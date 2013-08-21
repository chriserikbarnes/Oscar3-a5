package uk.ac.cam.ch.wwmm.oscar3.newpc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uk.ac.cam.ch.wwmm.oscar3.Oscar3Props;

/**Routines to fetch files from PubChem.
 * 
 * @author ptc24
 *
 */
public final class FetchPubChem {

	/**Checks to see whether Oscar3 has been configured with a place to put
	 * PubChem files, and if not, creates one.
	 * 
	 * @return Whether there is now a directory in which to put PubChem files.
	 */
	public static boolean setupPcDir() {
		String pcdir = Oscar3Props.getInstance().pcdir;
		if(pcdir == null || "none".equals(pcdir)) {
			if("none".equals(Oscar3Props.getInstance().workspace)) {
				System.out.println("Must create a workspace before downloading PubChem files");
				return false;
			}
			File workspace = new File(Oscar3Props.getInstance().workspace);
			if(!workspace.exists()) {
				System.out.println("Must create a workspace before downloading PubChem files");
				return false;				
			}
			if(!workspace.isDirectory()) {
				System.out.println("Your workspace must be a directory, not a file");
				return false;				
			}

			File f;
			try {
				f = new File(workspace, "npc");
				if(f.exists() && !f.isDirectory()) throw new Exception();
				if(!f.exists()) f.mkdir();
				f = new File(f, "PubChemFiles");
				f.mkdir();
			} catch (Exception e) {
				System.out.println("Could not create directory for PubChem files");
				return false;								
			}
			Oscar3Props.setProperty("pcdir", f.getAbsolutePath());
			try {
				Oscar3Props.saveProperties();
			} catch (Exception e) {
				System.out.println("Could not save Oscar3 Properties file");
				return false;								
			}
			return true;
		} else {
			try {
				if(!new File(pcdir).exists()) new File(pcdir).mkdir();
			} catch (Exception e) {
				System.out.println("Could not create directory for PubChem files");
				return false;												
			}
			return true;
		}
	}
	
	/**Fetches the files from PubChem.
	 * 
	 * @param recordType Compound or Substance.
	 * @param inCambridge Whether you can use the Cambridge HTTP/FTP proxy.
	 */
	public static void fetchFromPubChem(String recordType, boolean inCambridge) throws Exception {
		String pcdir = Oscar3Props.getInstance().pcdir;
		if("Compound".equals(recordType)) {
			File f = new File(pcdir, "CID-Synonym.gz");
			if(!f.exists())	fetchFile("ftp://ftp.ncbi.nlm.nih.gov/pubchem/Compound/Extras/CID-Synonym.gz",
					f, inCambridge);
			f = new File(pcdir, "CID-MeSH");
			if(!f.exists()) fetchFile("ftp://ftp.ncbi.nlm.nih.gov/pubchem/Compound/Extras/CID-MeSH",
					f, inCambridge);	
		}
		
		List<String> names = new ArrayList<String>();
		Pattern p = Pattern.compile(recordType + "_\\d+_\\d+\\.sdf\\.gz");
		URL url = new URL("ftp://ftp.ncbi.nlm.nih.gov/pubchem/" + recordType + "/CURRENT-Full/SDF/");
		BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
		for(String line = br.readLine();line != null;line = br.readLine()) {
			//System.out.println(line);
			Matcher m = p.matcher(line);
			if(m.find()) {
				names.add(m.group());
			}
		}
		br.close();	
		
		for(String name : names) {
			File outFile = new File(pcdir, name);
			if(outFile.exists()) continue;
			if(true) continue;
			fetchFile("ftp://ftp.ncbi.nlm.nih.gov/pubchem/" + recordType + "/CURRENT-Full/SDF/" +
					name, outFile, inCambridge);			
		}
		System.out.println("All " + recordType + " files fetched OK!");
		new NewPubChem().initialise();
	}
	
	private static boolean fetchFile(String urlSpec, File outFile, boolean inCambridge) throws Exception {
		try	{
			System.out.println("Fetching: " + urlSpec);
			URL url = new URL(urlSpec);
			URLConnection con;
			if(inCambridge){ 
				Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("wwwcache.cam.ac.uk", 8080));
				con = url.openConnection(proxy);
			} else {
				con = url.openConnection();
			}
			//if(con == null) return false;
			File tmp = new File(outFile.getParentFile(), "tmp");
			
			OutputStream os = new FileOutputStream(tmp);
			InputStream is = con.getInputStream();
			//if(is == null) return false;
			//if(true) return true;
			
			byte[] buf = new byte[1024];
			int len;
			while ((len = is.read(buf)) > 0) {
				os.write(buf, 0, len);
			}
			is.close();
			os.close();
			
			tmp.renameTo(outFile);
			
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Didn't fetch: " + urlSpec);
			return false;
		}

	}
	
}
