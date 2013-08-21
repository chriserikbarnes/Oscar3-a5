package uk.ac.cam.ch.wwmm.oscar3.misc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

/** A class for submitting a file to the Oscar3 parse servlet for parsing.
 * 
 * @author ptc24
 *
 */
public final class OscarClient {

	//Replicating code from ResourceGetter, to keep a small OscarClient jar
	private static void printUsage() {
		try {
			ClassLoader l = Thread.currentThread().getContextClassLoader();
			URL url = l.getResource("uk/ac/cam/ch/wwmm/oscar3/misc/resources/oscarclientusage.txt");
			if(url == null) throw new Exception("Could not load usage instructions");
			InputStream i = url.openStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(i));
			String line;
			while((line = br.readLine()) != null) {
				System.out.println(line);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new Error("Could not load usage instructions");
		}

	}
	
	/**The main method - call this. Command line arguaments:
	 * 
	 * <p><tt>inputfilename outputfilename url [oscarflow]</tt></p>
	 * 
	 * Where url is the url of the Oscar3 parse servlet. The optional
	 * oscarflow commands should be quoted eg "recognise resolve inline".
	 * 
	 * @param args Command line arguaments.
	 */
	public static void main(String[] args) throws Exception {
		if(args.length < 3) {
			printUsage();
			return;
		}
		
		File inFile = new File(args[0]);
		File outFile = new File(args[1]);		
		URL url = new URL(args[2]);
		
		String flow = null;
		
		if(args.length > 3) flow = args[3]; 
		
		HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
		//urlCon.setRequestMethod("PUT");
		//urlCon.addRequestProperty("contents", "we like acetone, yes we do");
		urlCon.setDoOutput(true);
		OutputStreamWriter osw = new OutputStreamWriter(urlCon.getOutputStream());
		if(args[0].endsWith(".xml")) {
			osw.write("SciXML=");
		} else {
			osw.write("contents=");
		}
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(inFile), "UTF-8"));
		String line = br.readLine();
		while(line != null) {
			osw.write(URLEncoder.encode(line + "\n", "UTF-8"));
			line = br.readLine();
		}
		if(flow != null && flow.equalsIgnoreCase("data")) {
			osw.write("&output=data");
		} 
		if(flow != null) {
			osw.write("&flowcommand=" + URLEncoder.encode(flow, "UTF-8"));
		}
		//osw.write(URLEncoder.encode(new InputStreamReader(new FileInputStream(file), "UTF-8");
		osw.close();
		
		//System.out.println(urlCon.getRequestMethod());
		System.out.println(urlCon.getRequestMethod());
		
		urlCon.connect();
		
		InputStream is = urlCon.getInputStream();
		FileOutputStream os = new FileOutputStream(outFile);
		int b = is.read();
		while(b != -1) {
			os.write(b);
			b = is.read();
		}
		os.close();
		//br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
		//PrintWriter pw = new PrintWriter(outFile, "UTF-8");
		//line = br.readLine();
		//while(line != null) {
		//	pw.println(line);
		//	line = br.readLine();
		//}
		//pw.close();

	}

}
