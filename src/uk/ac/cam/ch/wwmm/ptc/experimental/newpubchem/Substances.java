package uk.ac.cam.ch.wwmm.ptc.experimental.newpubchem;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import uk.ac.cam.ch.wwmm.oscar3.Oscar3Props;
import uk.ac.cam.ch.wwmm.oscar3.newpc.NewPubChem;
import uk.ac.cam.ch.wwmm.oscar3.newpc.PrunePubChemSynonyms;
import uk.ac.cam.ch.wwmm.ptclib.io.FileTools;

public class Substances {

	public static void analyseFile(PrintStream out, File f) throws Exception {
		BufferedReader br = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(f))));
		String sid = null;
		String cid = null;
		List<String> comments = new ArrayList<String>();
		List<String> synonyms = new ArrayList<String>();
		String dataSource = null;
		String genericRegistryName = null;
		boolean nonStandardBond = false;
		PrunePubChemSynonyms ppcs = new PrunePubChemSynonyms();
		for(String line=br.readLine();line!=null;line=br.readLine()) {
			//System.out.println(line);
			if(line.matches("> <PUBCHEM_CID_ASSOCIATIONS>")) {
				//System.out.println("*****");
				for(line=br.readLine();line.length()>0;line=br.readLine()) {
					if(line.matches("\\d+\\s+1")) {
						if(cid != null) System.out.println("Yikes!");
						cid = line.split("\\s+")[0];
						//System.out.println("****" + cid);
					}
				}
			} else if(line.matches("> <PUBCHEM_SUBSTANCE_SYNONYM>")) {
					//System.out.println("*****");
					for(line=br.readLine();line.length()>0;line=br.readLine()) {
						String s = line.trim();
						if(s.length() > 0 && ppcs.isGoodName(s)) synonyms.add(line.trim());
					}
			} else if(line.matches("> <PUBCHEM_SUBSTANCE_ID>")) {
				//System.out.println("*****");
				sid = br.readLine().trim();
				//System.out.println("****" + sid);
			} else if(line.matches("> <PUBCHEM_EXT_DATASOURCE_NAME>")) {
				dataSource = br.readLine().trim();
			} else if(line.matches("> <PUBCHEM_NONSTANDARDBOND>")) {
				nonStandardBond = true;
			} else if(line.equals("$$$$")) {
				//System.out.println("*****");
				if(cid != null && sid != null && synonyms.size() > 0) {
					out.print(cid);
					out.print("\t" + sid);
					out.print("\t" + nonStandardBond);
					out.print("\t" + dataSource);
					for(String synonym : synonyms) {
						out.print("\t" + synonym);
					}
					out.println();
				}
				cid = null;
				sid = null;
				comments.clear();
				synonyms.clear();
				dataSource = null;
				genericRegistryName = null;
				nonStandardBond = false;
			}
		}		
	}
	
	public static void invertToNames(File inFile, PrintStream out) throws Exception {
		BufferedReader br = new BufferedReader(new FileReader(inFile));
		for(String line = br.readLine();line!=null;line=br.readLine()) {
			String [] ss = line.split("\t");
			for(int i=4;i<ss.length;i++) {
				out.println(NewPubChem.normaliseName(ss[i]) + "\t" + ss[0] + "\t" + ss[1] + "\t" + ss[2] + "\t" + ss[3]); 
			}
		}
	}
	
	/**
	 * 
	 * @param inCambridge Whether you can use the Cambridge HTTP/FTP proxy
	 */
	public static void fetchFromPubChem(boolean inCambridge) throws Exception {
		List<String> names = new ArrayList<String>();
		Pattern p = Pattern.compile("Substance_\\d+_\\d+\\.sdf\\.gz");
		URL url = new URL("ftp://ftp.ncbi.nlm.nih.gov/pubchem/Substance/CURRENT-Full/SDF/");
		BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
		for(String line = br.readLine();line != null;line = br.readLine()) {
			System.out.println(line);
			Matcher m = p.matcher(line);
			if(m.find()) {
				names.add(m.group());
			}
		}
		br.close();	
		
		for(String name : names) {
			File outFile = new File(Oscar3Props.getInstance().pcdir, name);
			if(outFile.exists()) continue;
			System.out.println("Fetching: " + name);
			fetchFile("ftp://ftp.ncbi.nlm.nih.gov/pubchem/Substance/CURRENT-Full/SDF/" +
					name, outFile, inCambridge);			
		}
	}
	
	public static boolean fetchFile(String urlSpec, File outFile, boolean inCambridge) throws Exception {
		try	{
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

	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		//fetchFromPubChem(true);
		File f = new File("/home/ptc24/tmp/pcs.txt");
		PrintStream out;
		out = new PrintStream(new FileOutputStream(f));
		List<File> files = FileTools.getFilesFromDirectoryByRegex(new File(Oscar3Props.getInstance().pcdir), "Substance.*\\.sdf\\.gz");
		for(File file : files) {
			System.out.println(file);
			analyseFile(out, file);			
		}
		out.close();
		File ff = new File("/home/ptc24/tmp/pcsnames.txt");
		out = new PrintStream(new FileOutputStream(ff));
		invertToNames(f, out);
		out.close();
	}

}
