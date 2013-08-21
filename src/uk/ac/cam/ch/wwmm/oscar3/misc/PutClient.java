package uk.ac.cam.ch.wwmm.oscar3.misc;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/** A standalone class for PUTting a file to a specific URL.
 * 
 * @author ptc24
 *
 */
public final class PutClient {

	/** PUTS a file to a specific URL
	 * 
	 * @param args First, the filename of the file to PUT, second, the URL to PUT it to.
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		File inFile = new File(args[0]);

		String fname = inFile.getName();
		
		String urlStr = args[1];
		String urlEnd = urlStr.substring(urlStr.lastIndexOf("/")+1);
		if(urlEnd.equals("")) {
			urlStr += fname;
		} else if(!urlEnd.contains(".")) {
			urlStr += "/" + fname;
		}
		
		URL url = new URL(urlStr);
		
		HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
		urlCon.setRequestMethod("PUT");
		urlCon.setDoOutput(true);
		OutputStream os = urlCon.getOutputStream();
		FileInputStream fis = new FileInputStream(inFile);
		for(int i=fis.read();i!=-1;i=fis.read()) {
			os.write(i);
		}
		//osw.write("Hello, world!");
		os.close();
		
		urlCon.connect();
		
		InputStream is = urlCon.getInputStream();
		int b = is.read();
		while(b != -1) {
			//System.out.write(b);
			b = is.read();
		}
		System.out.printf("File %s succesfully PUT to %s", fname, urlStr);
	}

}
