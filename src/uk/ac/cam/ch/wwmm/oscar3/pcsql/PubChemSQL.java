package uk.ac.cam.ch.wwmm.oscar3.pcsql;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.apache.commons.net.ftp.FTPClient;

import uk.ac.cam.ch.wwmm.oscar3.Oscar3Props;
import uk.ac.cam.ch.wwmm.ptclib.io.FileTools;
import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;

/**Manages a local database based on PubChem.
 * 
 * @author ptc24
 * @deprecated PubChemSQL functionality has been replaced by NewPubChem.
 *
 */
public class PubChemSQL {

	Connection con;
	Statement stmt;
	
	File pcDir;
	
	PreparedStatement insertName;
	PreparedStatement insertStructure;
	PreparedStatement insertMol;
	PreparedStatement selectSmiles;
	PreparedStatement selectSmilesAndInChI;
	PreparedStatement selectCidFromName;
	PreparedStatement selectNameFromCid;
	PreparedStatement selectCidByInChI;
	PreparedStatement selectMolFromInChI;
	PreparedStatement selectSmilesAndInChIFromCid;

	String rdbms;
	
	boolean fullPubChem;

	public static Pattern sevenCapsPattern = Pattern.compile(".*([A-Z].*){7,}");
	public static Pattern twoLowerPattern = Pattern.compile("[a-z][a-z]");

	private static PubChemSQL myInstance;
	
	public static void reinitialise() throws Exception {
		myInstance = null;
		getInstance();
	}
	
	public static PubChemSQL getInstance() throws Exception {
		if(myInstance != null) return myInstance;
		if(Oscar3Props.getInstance().dbname == null || "none".equals(Oscar3Props.getInstance().dbname)) return null;
		String passwd = "";
		if(Oscar3Props.getInstance().dbpasswd != null) passwd = Oscar3Props.getInstance().dbpasswd;
		boolean full = false;
		if(Oscar3Props.getInstance().fulldb) full = true;
		File f = null;
		if(Oscar3Props.getInstance().pcdir != null && !"none".equals(Oscar3Props.getInstance().pcdir))
			f = new File(Oscar3Props.getInstance().pcdir);
		PubChemSQL pcs = new PubChemSQL(
				Oscar3Props.getInstance().dbaddress,
				Oscar3Props.getInstance().dbname,
				Oscar3Props.getInstance().dbusername,
				passwd,
				f,
				full
		);	
		return pcs;
	}
	
	public PubChemSQL(String address, String dbName, String userName, String passwd, File pcDir) throws Exception {
		this(address, dbName, userName, passwd, pcDir, false);
	}
	
	public PubChemSQL(String address, String dbName, String userName, String passwd, File pcDir, boolean full) throws Exception {
		this.pcDir = pcDir;
		
		fullPubChem = full;
		
		rdbms = Oscar3Props.getInstance().rdbms;
			
		String url = null;
		if("MySQL".equals(rdbms)) {
			Class.forName("com.mysql.jdbc.Driver");
			url = "jdbc:mysql://" + address + "/" + dbName;			
		} else {
			/* Default to postgres */
			Class.forName("org.postgresql.Driver");
			url = "jdbc:postgresql://" + address + "/" + dbName;			
		}
		Properties props = new Properties();
		props.setProperty("user",userName);
		props.setProperty("password",passwd);
		con = DriverManager.getConnection(url, props);
		stmt = con.createStatement();		
		
		insertName = con.prepareStatement("INSERT INTO name (name, cid) VALUES (?, ?)");
		
		insertStructure = con.prepareStatement("INSERT INTO structure(smiles, inchi, cid) " +
		"VALUES (?, ?, ?)");
		
		insertMol = con.prepareStatement("INSERT INTO molFiles (inchi, mol) VALUES (?, ?)");
		
		if(!fullPubChem){
			selectSmiles = con.prepareStatement("SELECT smiles FROM structure WHERE cid IN " +
			"(SELECT cid FROM name WHERE name = ?)");		

			selectSmilesAndInChI = con.prepareStatement("SELECT smiles, inchi FROM structure WHERE cid IN " +
			"(SELECT cid FROM name WHERE name = ?)");		

			selectCidFromName = con.prepareStatement("SELECT cid FROM name WHERE name = ?");
			selectNameFromCid = con.prepareStatement("SELECT name FROM name WHERE cid = ?");
			
			selectSmilesAndInChIFromCid = con.prepareStatement("SELECT smiles, inchi FROM structure WHERE cid = ?");
			
			if("MySQL".equals(rdbms)) {
				selectCidByInChI = con.prepareStatement("SELECT cid FROM structure WHERE inchi = ?");				
			} else {
				selectCidByInChI = con.prepareStatement("SELECT cid FROM structure WHERE " +
						"substring(inchi for 100) = substring(? for 100) " +
						"AND inchi = ?");								
			}

			selectMolFromInChI = con.prepareStatement("SELECT mol FROM molFiles WHERE inchi = ?");
		} else {
			/* Full PubChem database */
			selectSmiles = con.prepareStatement("SELECT openeye_can_smiles FROM pubchem_compound WHERE cid IN " +
			"(SELECT cid FROM pubchem_synonym WHERE LOWER(synonym) = LOWER(?))");		

			selectSmilesAndInChI = con.prepareStatement("SELECT openeye_can_smiles, nist_inchi FROM pubchem_compound WHERE cid IN " +
			"(SELECT cid FROM pubchem_synonym WHERE LOWER(synonym) = (?))");		

			selectCidFromName = con.prepareStatement("SELECT cid FROM pubchem_synonym WHERE LOWER(synonym) = LOWER(?)");			
			selectNameFromCid = con.prepareStatement("SELECT synonym FROM pubchem_synonym WHERE cid = ?");			

			selectSmilesAndInChIFromCid = con.prepareStatement("SELECT openeye_can_smiles, nist_inchi FROM pubchem_compound WHERE cid = ?");
		}

	}
	
	public Connection getConnection() {
		return con;
	}
	
	public void setUpTables() throws Exception {		
	    stmt.executeUpdate("CREATE TABLE name " +
		"(name TEXT, cid INTEGER)");

	    stmt.executeUpdate("CREATE TABLE structure " +
		"(smiles TEXT, inchi TEXT, cid INTEGER)");
	    setUpMolFileCache();
	}
	
	public void setUpMolFileCache() throws Exception {
	    if("MySQL".equals(rdbms)) {
			stmt.executeUpdate("CREATE TABLE molFiles " +
			"(inchi TEXT, mol MEDIUMTEXT)");
			stmt.executeUpdate("CREATE INDEX inchi_mol_index " +
			"ON molFiles (inchi(50))");
	    } else {
			stmt.executeUpdate("CREATE TABLE molFiles " +
			"(inchi TEXT, mol TEXT)");
			stmt.executeUpdate("CREATE INDEX inchi_mol_index " +
			"ON molFiles USING HASH(inchi)");
	    }
	}
	
	public String getMolForInChI(String inchi) throws Exception {
		if(fullPubChem) return null;
		selectMolFromInChI.setString(1, inchi);
		ResultSet rs = selectMolFromInChI.executeQuery();
		if(rs.next()) {
			return rs.getString(1);
		} else {
			selectCidByInChI.setString(1, inchi);
			if(!"MySQL".equals(rdbms)) selectCidByInChI.setString(2, inchi);
			rs = selectCidByInChI.executeQuery();
			if(rs.next()) {
				int cid = rs.getInt(1);
				String mol = ExtractSDFFromGZip.extractSDFFromGzip(pcDir, cid);
				if(mol != null) {
					mol = ExtractSDFFromGZip.molFileFromSDFEntry(mol);
					insertMol.setString(1, inchi);
					insertMol.setString(2, mol);
					insertMol.executeUpdate();
				} 
				return mol;
			} else {
				return null;
			}
		}
	}
	
	public void indexForLookup() throws Exception {
	    stmt.executeUpdate("CREATE INDEX structure_index ON structure (cid)");
	    stmt.executeUpdate("CREATE INDEX name_cid_index ON name (cid)");
	    if("MySQL".equals(rdbms)) {
	    	/* MySQL needs a prefix length */
			stmt.executeUpdate("CREATE INDEX name_index ON name (name(20))");	    		    	
		    stmt.executeUpdate("CREATE INDEX inchi_index ON structure (inchi(50))");
	    } else {
			stmt.executeUpdate("CREATE INDEX name_index ON name (name)");	    	
		    stmt.executeUpdate("CREATE INDEX inchi_index ON structure((substring(inchi for 100)))");
	    }
	}
	
	public void addChemical(Collection<String> names, String smiles, String inchi, int cid) throws Exception {
		if(fullPubChem) throw new Exception("This class cannot add entries to full pubchem mirrors");
		con.setAutoCommit(false);
		putStructure(smiles, inchi, cid);
		for(String name : names) {
			putName(name, cid);
		}
		con.commit();
		con.setAutoCommit(true);
	}
	
	public void addSynonyms(Collection<String> names, int cid) throws Exception {
		if(fullPubChem) throw new Exception("This class cannot add entries to full pubchem mirrors");
		con.setAutoCommit(false);
		for(String name : names) {
			putName(name, cid);
		}
		con.commit();			
		con.setAutoCommit(true);		
	}
	
	public void putName(String name, int cid) throws Exception {
		if(fullPubChem) throw new Exception("This class cannot add entries to full pubchem mirrors");
		insertName.setString(1, name);
		insertName.setInt(2, cid);
		insertName.executeUpdate();
	}
	
	public void putStructure(String smiles, String inchi, int cid) throws Exception {	    
		if(fullPubChem) throw new Exception("This class cannot add entries to full pubchem mirrors");
		insertStructure.setString(1, smiles);
		insertStructure.setString(2, inchi);
		insertStructure.setInt(3, cid);
		insertStructure.execute();
	}
	
	private Set<Integer> getCidsForName(String name) throws Exception {
		Set<Integer> cids = new HashSet<Integer>();
		if(name == null) return cids;
		name = StringTools.unicodeToLatin(name);
		if(name == null) return cids;
		if(!fullPubChem) name = StringTools.normaliseName(name);
		selectCidFromName.setString(1, name);
		ResultSet rs = selectCidFromName.executeQuery();
		while(rs.next()) {
			cids.add(rs.getInt(1));
		}
		return cids;
	}
	
	private Set<Integer> getCidsForInchi(String inchi) throws Exception {
		Set<Integer> cids = new HashSet<Integer>();
		selectCidByInChI.setString(1, inchi);
		if(!"MySQL".equals(rdbms)) selectCidByInChI.setString(2, inchi);
		ResultSet rs = selectCidByInChI.executeQuery();
		if(rs.next()) {
			cids.add(rs.getInt(1));
		} 
		return cids;
	}
	
	public List<String []> getSmilesAndInChIsForName(String name) throws Exception {
		List<String []> results = new ArrayList<String []>();
		// Ultimately I'd like to seperate out the way that relies on SQL doing
		// the right thing with nested selects, but for now I'll avoid them.
		if(true || "MySQL".equals(rdbms)) {
			Set<Integer> cids = getCidsForName(name);
			for(int i : cids) {
				selectSmilesAndInChIFromCid.setInt(1, i);
				ResultSet rs = selectSmilesAndInChIFromCid.executeQuery();
				while(rs.next()) {
					String [] resultsArray = new String[2];
					resultsArray[0] = rs.getString(1);
					resultsArray[1] = rs.getString(2);
					results.add(resultsArray);
				}
			}
		}
		return results;
	}
	
	public Set<String> getSmilesForName(String name) throws Exception {
		List<String []> smilesAndInChIs = getSmilesAndInChIsForName(name);
		Set<String> smiles = new HashSet<String>();
		for(String [] results : smilesAndInChIs) {
			smiles.add(results[0]);
		}
		return smiles;
		/*Set<String> smiles = new HashSet<String>();
		if(name == null) return smiles;
		name = StringTools.unicodeToLatin(name);
		if(name == null) return smiles;
		
		if(!fullPubChem) name = StringTools.normaliseName(name);
		selectSmiles.setString(1, name);
		//selectSmiles.
		ResultSet rs = selectSmiles.executeQuery();
		while(rs.next()) {
			smiles.add(rs.getString(1));
		}
		return smiles;*/
	}
	
	public String [] getShortestSmilesAndInChI(String name) throws Exception {
		List<String []> smilesAndInChIs = getSmilesAndInChIsForName(name);
		if(smilesAndInChIs.size() == 0) return null;
		String smiles = null;
		String inchi = null;
		for(String [] result : smilesAndInChIs) {
			String tmpSmiles = result[0];
			String tmpInchi = result[1];
			if(tmpSmiles != null && tmpInchi != null && (smiles == null || inchi == null ||
					(smiles.length() == tmpSmiles.length() && inchi.length() > tmpInchi.length()) ||
							(smiles.length() > tmpSmiles.length()))) {
				smiles = tmpSmiles;
				inchi = tmpInchi;
			}		
		}
		String [] results = new String[2];
		results[0] = smiles;
		results[1] = inchi;
		return results;
		
		/*String [] results = new String[2];
		if(name == null) return results;
		name = StringTools.unicodeToLatin(name);
		if(name == null) return results;
		name = StringTools.normaliseName(name);
		selectSmilesAndInChI.setString(1, name);
		ResultSet rs = selectSmilesAndInChI.executeQuery();
		String smiles = null;
		String inchi = null;
		while(rs.next()) {
			String tmpSmiles = rs.getString(1);
			String tmpInchi = rs.getString(2);
			if(tmpSmiles != null && tmpInchi != null && (smiles == null || inchi == null ||
					(smiles.length() == tmpSmiles.length() && inchi.length() > tmpInchi.length()) ||
							(smiles.length() > tmpInchi.length()))) {
				smiles = tmpSmiles;
				inchi = tmpInchi;
			}
		}
		results[0] = smiles;
		results[1] = inchi;
		return results;*/
	}
	
	private DetailsForCid getDetailsForCidInternal(int cid) throws Exception {
		DetailsForCid dfc = new DetailsForCid(cid);
		selectSmilesAndInChIFromCid.setInt(1, cid);
		ResultSet rs = selectSmilesAndInChIFromCid.executeQuery();
		if(rs.next()) {
			dfc.setSmiles(rs.getString(1));
			dfc.setInchi(rs.getString(2));
		}
		
		selectNameFromCid.setInt(1,cid);
		rs = selectNameFromCid.executeQuery();
		while(rs.next()) {
			dfc.addName(rs.getString(1));
		}
		return dfc;
	}
	
	public List<DetailsForCid> getDetailsForCids(Set<Integer> cids) throws Exception {
		List<DetailsForCid> results = new ArrayList<DetailsForCid>();
		for(int cid : cids) {
			results.add(getDetailsForCidInternal(cid));
		}
		return results;
	}

	public List<DetailsForCid> getDetailsForCid(int cid) throws Exception {
		Set<Integer> cids = new HashSet<Integer>();
		cids.add(cid);
		return getDetailsForCids(cids);
	}

	public List<DetailsForCid> getDetailsForName(String name) throws Exception {
		Set<Integer> cids = getCidsForName(name);
		return getDetailsForCids(cids);
	}

	public List<DetailsForCid> getDetailsForInchi(String name) throws Exception {
		Set<Integer> cids = getCidsForInchi(name);
		return getDetailsForCids(cids);
	}
	
	public boolean hasName(String name) throws Exception {
		try {
			if(name == null) return false;
			name = StringTools.unicodeToLatin(name);
			if(name == null) return false;
			name = StringTools.normaliseName(name);
			selectCidFromName.setString(1, name);
			ResultSet rs = selectCidFromName.executeQuery();
			if(rs.next()) return true;
			return false;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * 
	 * @param emailAddress Your email address as anon FTP password
	 * @param inCambridge Whether you can use the Cambridge HTTP/FTP proxy
	 */
	public void fetchFromPubChem(String emailAddress, boolean inCambridge) throws Exception {
		FTPClient ftpc = new FTPClient();
		ftpc.connect("ftp.ncbi.nlm.nih.gov");
		ftpc.login("anonymous", emailAddress);
		ftpc.changeWorkingDirectory("/pubchem/Compound/CURRENT-Full/SDF/");
		
		String [] names = ftpc.listNames();
		ftpc.disconnect();
		
		for(int i=0;i<names.length;i++) {
			System.out.println(names[i]);
			File outFile = new File(pcDir, names[i]);
			if(outFile.exists()) continue;
			//System.out.println("Would fetch: " + names[i]);
			fetchFile("ftp://ftp.ncbi.nlm.nih.gov/pubchem/Compound/CURRENT-Full/SDF/" +
					names[i], outFile, inCambridge);
		}
		
		fetchFile("ftp://ftp.ncbi.nlm.nih.gov/pubchem/Compound/Extras/CID-Synonym.gz",
				new File(pcDir, "CID-Synonym.gz"), inCambridge);
		fetchFile("ftp://ftp.ncbi.nlm.nih.gov/pubchem/Compound/Extras/CID-MeSH",
				new File(pcDir, "CID-MeSH"), inCambridge);		
	}
	
	public void fetchFile(String urlSpec, File outFile, boolean inCambridge) throws Exception {
		try	{
			URL url = new URL(urlSpec);
		URLConnection con;
		if(inCambridge){ 
			Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("wwwcache.cam.ac.uk", 8080));
			con = url.openConnection(proxy);
		} else {
			con = url.openConnection();
		}
		OutputStream os = new FileOutputStream(outFile);
		InputStream is = con.getInputStream();
		
		byte[] buf = new byte[1024];
        int len;
        while ((len = is.read(buf)) > 0) {
            os.write(buf, 0, len);
        }
        is.close();
        os.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Didn't fetch: " + urlSpec);
		}

		}
	
	public void digestCompoundGzip(File f) throws Exception {
		BufferedReader br = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(f))));
		
		String line = br.readLine();
		String smiles = null;
		String inchi = null;
		int cid = -1;
		Set<String> names = new HashSet<String>();
		while(line != null) {
			if("> <PUBCHEM_COMPOUND_CID>".equals(line)) {
				line = br.readLine();
				if(line.matches("\\d+")) cid = Integer.parseInt(line);
			} else if("> <PUBCHEM_NIST_INCHI>".equals(line)) {
				line = br.readLine();
				inchi = line;
			} else if("> <PUBCHEM_OPENEYE_CAN_SMILES>".equals(line)) {
				line = br.readLine();
				smiles = line;
			} else if(line.matches("> <PUBCHEM_IUPAC_.*_NAME>")) {
				line = br.readLine();
				names.add(StringTools.normaliseName(line));
			} else if("$$$$".equals(line)) {
				if(cid != -1 && smiles != null && inchi != null) {
					addChemical(names, smiles, inchi, cid);
				}
				cid = -1;
				smiles = null;
				inchi = null;
				names = new HashSet<String>();
			}
			line = br.readLine();
		}		
	}
	
	public void digestCidSyn(long startTime) throws Exception {
		File f = new File(pcDir, "CID-Synonym.gz");
		BufferedReader br = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(f))));
		String line = br.readLine();
		Set<String> names = new HashSet<String>();
		int cid = -1;
		int added = 0;
		while(line != null) {
			String [] ss = line.split("\t");
			if(ss.length == 0 || !ss[0].matches("\\d+")) {
				System.out.println("Duff cid in CID-Synomyn.gz!");
				return;
			}
			int newCid = Integer.parseInt(ss[0]);
			if(newCid > cid) {
				if(cid != -1) {
					addSynonyms(names, cid);
					added++;
					if(added % 10000 == 0) System.out.println("Added synonyms for CID " + cid + " at " + (System.currentTimeMillis() - startTime) + " milliseconds");
					names = new HashSet<String>();
				}
				cid = newCid;
			}
			String synonym = ss[1];
			String ns = StringTools.normaliseName(synonym);
			names.add(ns);
			if(sevenCapsPattern.matcher(ns).matches()) {
				names.add(ns.toLowerCase());
			}
			line = br.readLine();
		}
		addSynonyms(names, cid);	
	}

	public void digestCidMeSH(long startTime) throws Exception {
		File f = new File(pcDir, "CID-MeSH");
		BufferedReader br = new BufferedReader(new FileReader(f));
		String line = br.readLine();
		Set<String> names = new HashSet<String>();
		int cid = -1;
		int added = 0;
		while(line != null) {
			String [] ss = line.split("\t");
			int newCid = Integer.parseInt(ss[0]);
			if(newCid > cid) {
				if(cid != -1) {
					//System.out.println(names);
					addSynonyms(names, cid);
					added++;
					if(added % 10000 == 0) System.out.println("Added MeSH Terms for CID " + cid + " at " + (System.currentTimeMillis() - startTime) + " milliseconds");
					names = new HashSet<String>();
				}
				cid = newCid;
			}
			for(int i=1;i<ss.length;i++) {
				String synonym = ss[i];
				String ns = StringTools.normaliseName(synonym);
				names.add(ns);
			}
			line = br.readLine();
		}
		addSynonyms(names, cid);	
	}

	public void setUpDatabase() throws Exception{
		long startTime = System.currentTimeMillis();
		List<File> sdfFiles = FileTools.getFilesFromDirectoryBySuffix(pcDir, ".sdf.gz");
		try {
			setUpTables();
		} catch (Exception e) {
			System.out.println("The foregoing may not be an error: maybe the tables already exist");
			System.out.println("When I have more time I'll work out how to ask MySQL what tables it has");
			e.printStackTrace();
		}
		for(File f : sdfFiles) {
			System.out.println("Adding " + f.getName() + " at " + (System.currentTimeMillis() - startTime) + " milliseconds");
			digestCompoundGzip(f);		
		}
		System.out.println("Adding synonyms at " + (System.currentTimeMillis() - startTime) + " milliseconds");
		digestCidSyn(startTime);
		System.out.println("Adding MeSH terms at " + (System.currentTimeMillis() - startTime) + " milliseconds");
		digestCidMeSH(startTime);
		System.out.println("Building indices at " + (System.currentTimeMillis() - startTime) + " milliseconds");
		
		try {
			indexForLookup();
		} catch (Exception e) {
			System.out.println("The foregoing may not be an error: maybe the indices already exist");
			System.out.println("When I have more time I'll work out how to ask MySQL what indices it has");
			e.printStackTrace();
		}
		System.out.println("Database complete at " + (System.currentTimeMillis() - startTime) + " milliseconds");		
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		PubChemSQL pcsql = PubChemSQL.getInstance();
		//for(DetailsForCid dfc : pcsql.getDetailsForInchi("InChI=1/C4H8O/c1-2-4-5-3-1/h1-4H2")) {
		//	System.out.println(dfc);
		//}
		// Modify this: the email address is being used as a password for anonymous FTP
		//pcsql.fetchFromPubChem("pcorbett+oscarftp@chiark.greenend.org.uk", false);
		pcsql.setUpDatabase();
		pcsql.setUpMolFileCache();
	}

}
