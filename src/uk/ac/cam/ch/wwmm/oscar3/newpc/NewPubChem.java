package uk.ac.cam.ch.wwmm.oscar3.newpc;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import uk.ac.cam.ch.wwmm.oscar3.Oscar3Props;
import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;

/**Storage of chemical names and stuctures from PubChem. This class co-ordinates
 * the fetching of PubChem files, analyses them to find the names, SMILES
 * and InChIs, places them in fast-to-read on-disk hash tables, and fetches the
 * structures given the names as appropriate.
 * 
 * @author ptc24
 *
 */
public final class NewPubChem {

	private File pcdir;
	private File npcdir;
	private File compoundFile;
	private File nameFile;
	private File namesDHFile;
	private File compoundsDHFile;
	private DiskHash namesDiskHash;
	private DiskHash compoundsDiskHash;
	
	Comparator<String> namesLookupComparator = new Comparator<String>() {
		public int compare(String o1, String o2) {
			int i = o1.indexOf("\t");
			if(i > -1) o1 = o1.substring(0, i);
			return o1.compareTo(o2);
		}
	};

	private Comparator<String> compoundsLookupComparator = new Comparator<String>() {
		public int compare(String o1, String o2) {
			int i = o1.indexOf("\t");
			if(i > -1) o1 = o1.substring(0, i);
			int i1 = Integer.parseInt(o1);
			int i2 = Integer.parseInt(o2);
			return i1 - i2;
		}
	};
	
	private static NewPubChem myInstance;
	private static boolean foundNotToExist = false;
	
	/**Gets a NewPubChem instance, if one has been set up.
	 * 
	 * @return The NewPubChem instance, or null.
	 */
	public static NewPubChem getInstance() {
		if(foundNotToExist) return null;
		if(myInstance == null) {
			myInstance = new NewPubChem();
			boolean loaded = myInstance.load();
			if(!loaded) {
				foundNotToExist = true;
				return null;
			}
		}
		return myInstance;
	}
	
	/**Creates a NewPubChem instance.
	 * 
	 */
	public NewPubChem() {
		pcdir = new File(Oscar3Props.getInstance().pcdir);
		npcdir = new File(Oscar3Props.getInstance().workspace, "npc");
	}
	
	/**Analyses files that have previously been fetched from PubChem, and makes
	 * the various hash tables etc. that store the data.
	 * 
	 * @throws Exception
	 */
	public void initialise() throws Exception {
		System.out.println("Initialising compounds database from PubChem...");
		System.out.println("This may take a long time...");
		if(!npcdir.exists()) npcdir.mkdir();
		File tmpFile1 = File.createTempFile("npc", ".txt");
		tmpFile1.deleteOnExit();
		Writer fw = new BufferedWriter(new FileWriter(tmpFile1));
		long time = System.currentTimeMillis();
		System.out.println("Digesting CID-MESH File: " + (System.currentTimeMillis() - time));
		digestCIDMesh(fw);
		System.out.println("Digesting CID-Synonym File: " + (System.currentTimeMillis() - time));
		digestCIDSyn(fw);
		fw.close();
		File tmpFile2 = File.createTempFile("npc", ".txt");
		tmpFile2.deleteOnExit();
		System.out.println("Sorting Names File: " + (System.currentTimeMillis() - time));
		SortFile.sortFile(tmpFile1, tmpFile2, null, true);
		if(tmpFile1.exists()) tmpFile1.delete();
		nameFile = new File(npcdir, "names.txt");
		System.out.println("Grouping Names: " + (System.currentTimeMillis() - time));
		groupCIDs(tmpFile2, nameFile);
		File tmpCIDFile = File.createTempFile("npc", ".txt");
		tmpCIDFile.deleteOnExit();
		fw = new BufferedWriter(new FileWriter(tmpCIDFile));
		System.out.println("Extracting CIDs: " + (System.currentTimeMillis() - time));
		extractCIDs(tmpFile2, fw);
		if(tmpFile2.exists()) tmpFile2.delete();
		fw.close();
		File tmpCIDFile2 = File.createTempFile("npc", ".txt");
		tmpCIDFile2.deleteOnExit();
		System.out.println("Sorting CIDs: " + (System.currentTimeMillis() - time));
		SortFile.sortFile(tmpCIDFile, tmpCIDFile2, new Comparator<String>() {
			public int compare(String o1, String o2) {
				int i1 = Integer.parseInt(o1);
				int i2 = Integer.parseInt(o2);
				return i1 - i2;
			}
		}, true);
		if(tmpCIDFile.exists()) tmpCIDFile.delete();
		compoundFile = new File(npcdir, "compounds.txt");
		//fw = new BufferedWriter(new FileWriter(compoundFile));
		fw = new FileWriter(compoundFile);
		System.out.println("Extracting Compounds: " + (System.currentTimeMillis() - time));
		extractCompounds(tmpCIDFile2, fw);
		if(tmpCIDFile2.exists()) tmpCIDFile2.delete();
		fw.close();
		System.out.println("Making hash tables: " + (System.currentTimeMillis() - time));
		makeDiskHashes();
		System.out.println("Done!");
	}
	
	private void makeDiskHashes() throws Exception {
		namesDHFile = new File(npcdir, "names.hash");
		FastDiskHash.createHash(nameFile, namesDHFile);
		compoundsDHFile = new File(npcdir, "compounds.hash");
		FastDiskHash.createHash(compoundFile, compoundsDHFile);
		
		namesDiskHash = new DiskHash(namesDHFile);
		compoundsDiskHash = new DiskHash(compoundsDHFile);
	}
	
	private boolean load() {
		try {
			compoundFile = new File(npcdir, "compounds.txt");
			nameFile = new File(npcdir, "names.txt");
			File indexFile = new File(npcdir, "index");
			if(!compoundFile.exists() || !nameFile.exists() || !indexFile.exists()) return false;
			DataInputStream dais = new DataInputStream(new BufferedInputStream(new FileInputStream(indexFile)));
			namesDHFile = new File(npcdir, "names.hash");
			namesDiskHash = new DiskHash(namesDHFile);
			compoundsDHFile = new File(npcdir, "compounds.hash");
			compoundsDiskHash = new DiskHash(compoundsDHFile);

			
			dais.close();
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	private void digestCIDSyn(Writer w) throws Exception {
		PrunePubChemSynonyms ppcs = new PrunePubChemSynonyms();
		File f = new File(pcdir, "CID-Synonym.gz");
		BufferedReader br = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(f))));
		for(String line = br.readLine();line!=null;line=br.readLine()) {
			if(!line.contains("\t")) continue;
			String [] ss = line.split("\t");
			String name = ss[1];
			String cid = ss[0];
			if(ppcs.isGoodName(name)) {
				name = normaliseName(name);
				if(name != null) w.write(name + "\t" + cid + "\n");
			}
		}
	}
	
	private void digestCIDMesh(Writer w) throws Exception {
		File f = new File(pcdir, "CID-MeSH");
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));
		for(String line = br.readLine();line!=null;line=br.readLine()) {
			String [] ss = line.split("\t");
			String cid = ss[0];
			for(int i=1;i<ss.length;i++) {
				String name = normaliseName(ss[i]);
				if(name != null) w.write(name + "\t" + cid + "\n");
			}
		}
	}
	
	private void extractCIDs(File f, Writer w) throws Exception {
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));
		for(String line = br.readLine();line!=null;line=br.readLine()) {
			String [] ss = line.split("\t");
			String cid = ss[1];
			w.write(cid + "\n");
		}
	}
	
	private void extractCompounds(File numberFile, Writer w) throws Exception {
		long time = System.currentTimeMillis();
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(numberFile), "UTF-8"));
		String oldFileName = null;
		BufferedReader compoundsBr = null;
		boolean warned = false;
		int increment = 25000;
		if(new File(pcdir, "Compound_00000001_00010000.sdf.gz").exists()) increment = 10000;
		for(String line = br.readLine();line!=null;line=br.readLine()) {
			int cid = Integer.parseInt(line);
			int cfn = (cid-1) / increment;
			String filename = String.format("Compound_%08d_%08d.sdf.gz", (cfn * increment) + 1, (cfn + 1) * increment);
			if(!filename.equals(oldFileName)) {
				//System.out.println(System.currentTimeMillis() - time);
				time = System.currentTimeMillis();
				if(compoundsBr != null) compoundsBr.close();
				oldFileName = filename;
				File f = new File(pcdir, filename);
				if(f.exists()) {
					System.out.println("Extracting compound data from: " + f);
					compoundsBr = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(f))));
				} else {
					if(!warned) {
						System.out.println("Warning: you are missing some PubChem compound files, such as:");
						System.out.println(f);						
						warned = true;
					}
					compoundsBr = null;
				}
			}
			//System.out.println("cid:\t" + cid);
			if(compoundsBr != null) {
				String smiles = null;
				String inchi = null;
				int ccid = -1;
				String cLine = null;
				cLine = compoundsBr.readLine();
				while(cLine != null && ccid <= cid) {
					//System.out.println(cLine);
					if("> <PUBCHEM_COMPOUND_CID>".equals(cLine)) {
						cLine = compoundsBr.readLine();
						if(cLine.matches("\\d+")) ccid = Integer.parseInt(cLine);
					} else if("> <PUBCHEM_NIST_INCHI>".equals(cLine)) {
						cLine = compoundsBr.readLine();
						inchi = cLine;
					} else if("> <PUBCHEM_OPENEYE_CAN_SMILES>".equals(cLine)) {
						cLine = compoundsBr.readLine();
						smiles = cLine;
						//} else if(line.matches("> <PUBCHEM_IUPAC_.*_NAME>")) {
						//	line = br.readLine();
						//	names.add(StringTools.normaliseName(line));
					} else if("$$$$".equals(cLine)) {
						ccid = -1;
						smiles = null;
						inchi = null;
					}
					if(ccid == cid && smiles != null && inchi != null) {
						w.write(cid + "\t" + smiles + "\t" + inchi + "\n");
						break;
					}
					cLine = compoundsBr.readLine();
				}
				if(cLine == null) compoundsBr = null;
			}
		}
		if(compoundsBr != null) compoundsBr.close();
	}
	
	private void groupCIDs(File inputFile, File outFile) throws Exception {
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile), "UTF-8"));
		FileWriter fw = new FileWriter(outFile);
		String prevName = null;
		Set<Integer> cids = new HashSet<Integer>();
		for(String line=br.readLine();line!=null;line=br.readLine()) {
			if(!line.contains("\t")) continue;
			String [] ss = line.split("\t");
			if(ss[0].equals(prevName)) {
				cids.add(Integer.parseInt(ss[1]));
			} else if(prevName != null) {
				List<Integer> cidList = new ArrayList<Integer>(cids);
				Collections.sort(cidList);
				fw.write(prevName);
				for(Integer i : cidList) fw.write("\t" + i);
				fw.write("\n");
				
				cids.clear();
				cids.add(Integer.parseInt(ss[1]));
				prevName = ss[0];
			} else {
				cids.add(Integer.parseInt(ss[1]));				
				prevName = ss[0];
			}
		}
		if(prevName != null) {
			List<Integer> cidList = new ArrayList<Integer>(cids);
			Collections.sort(cidList);
			fw.write(prevName);
			for(Integer i : cidList) fw.write("\t" + i);
			fw.write("\n");			
		}
		fw.close();
		br.close();
	}
	
	/**Looks up a compound, by name.
	 * 
	 * @param compoundName The name to look up.
	 * @return A list of [SMILES, InChI] pairs, possibly empty.
	 * @throws Exception
	 */
	public List<String []> lookup(String compoundName) throws Exception {
		List<String []> results = new ArrayList<String []>();
		compoundName = normaliseName(compoundName);
		if(compoundName == null) return results;
		String s = namesDiskHash.get(compoundName);
		if(s == null) {
			return results;
		} else {
			String [] ss = s.split("\t");
			for(int i=0;i<ss.length;i++) {
				String l = compoundsDiskHash.get(ss[i]);
				if(l != null) {
					String [] sss = l.split("\t");
					String [] result = new String []{sss[0], sss[1]};
					results.add(result);
				}
			}
		}
		return results;
	}
	
	/**Looks up a compound, by name, selecting the SMILES and InChI according
	 * to a heuristic. The approach is to select the entry with the shortest
	 * SMILES string (this means that counterions etc. tend to be excluded).
	 * 
	 * @param name The name to look up.
	 * @return A [SMILES, InChI] pair, or null.
	 * @throws Exception
	 */
	public String [] getShortestSmilesAndInChI(String name) throws Exception {
		List<String []> smilesAndInChIs = lookup(name);
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
	}
	
	/**Looks up the SMILES and InChI for a given CID.
	 * 
	 * @param cid
	 * @return An array - SMILES then InChI - or null.
	 * @throws Exception
	 */
	public String [] lookupCid(String cid) throws Exception {
		String l = compoundsDiskHash.get(cid);
		if(l != null) {
			String [] sss = l.split("\t");
			String [] result = new String []{sss[0], sss[1]};
			return result;
		}
		return null;
	}
	
	/**Normalises a name for lookup. This converts Unicode to ISO-8859-1, 
	 * changing Greek characters to their names, and then normalises the 
	 * case of the name.
	 * 
	 * @param name The name to normalise.
	 * @return The normalised name.
	 */
	public static String normaliseName(String name) {
		if(name == null) return null;
		name = StringTools.unicodeToLatin(name);
		if(name == null) return null;
		name = StringTools.normaliseName(name);
		return name;
	}
	
	public List<String> cidsForInchi(String inchi) throws Exception {
		List<String> cids = new ArrayList<String>();
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(npcdir, "compounds.txt")), "UTF-8"));
		for(String line=br.readLine();line!=null;line=br.readLine()) {
			if(line.contains("\t"));
			String [] ss = line.split("\t");
			if(ss[2].equals(inchi)) {
				cids.add(ss[0]);
			}
		}
		return cids;
	}

	public List<String> namesForCids(List<String> cids) throws Exception {
		Set<String> cidSet = new HashSet<String>(cids);
		List<String> nfc = new ArrayList<String>();
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(npcdir, "names.txt")), "UTF-8"));
		for(String line=br.readLine();line!=null;line=br.readLine()) {
			if(line.contains("\t"));
			String [] ss = line.split("\t");
			for(int i=1;i<ss.length;i++) {
				if(cidSet.contains(ss[i])) {
					nfc.add(line);
				}
			}
		}
		return nfc;
	}
	
	public static void main(String[] args) throws Exception {
		long time = System.currentTimeMillis();
		if(false) {
			NewPubChem npc = new NewPubChem();
			npc.load();
			npc.makeDiskHashes();
		}
		
		//new NewPubChem().initialise();

		NewPubChem npc = new NewPubChem();
		npc.load();
		
		time = System.nanoTime();
		System.out.println(npc.lookup("morphine"));
		System.out.println(System.nanoTime() - time);
		time = System.nanoTime();
		System.out.println(npc.lookup("foobarane"));
		System.out.println(System.nanoTime() - time);
		time = System.nanoTime();
		System.out.println(npc.lookup("morphine"));
		System.out.println(System.nanoTime() - time);
		time = System.nanoTime();
		System.out.println(npc.lookup("1,3,5(10)-estratriene-3,15 beta,16 beta,17 beta-tetrol"));
		System.out.println(System.nanoTime() - time);
		//new NewPubChem().initialise();
		//System.out.println(String.format("%04d", 63));
		System.out.println(npc.getShortestSmilesAndInChI("oripavine")[1]);
		
		time = System.nanoTime();
		List<String> cids = npc.cidsForInchi("InChI=1/C18H19NO3/c1-19-8-7-18-11-4-6-14(21-2)17(18)22-16-13(20)5-3-10(15(16)18)9-12(11)19/h3-6,12,17,20H,7-9H2,1-2H3/t12-,17+,18+/m1/s1");
		System.out.println(npc.namesForCids(cids));
		System.out.println((System.nanoTime() - time) / 1000000000.0);
	}

}
