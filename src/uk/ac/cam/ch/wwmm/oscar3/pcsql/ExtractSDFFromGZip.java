package uk.ac.cam.ch.wwmm.oscar3.pcsql;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.zip.GZIPInputStream;

/**Pulls MOL files out of .sdf.gz files from PubChem.
 * 
 * @author ptc24
 * @deprecated PubChemSQL functionality has been replaced by NewPubChem.
 *
 */
public class ExtractSDFFromGZip {

	public static String molFileFromSDFEntry(String sdf) {
		return sdf.split("M END")[0];
	}
	
	public static String extractSDFFromGzip(File pubChemDir, int cid) throws Exception {
		StringBuffer sb = new StringBuffer();
		int fileno = cid / 10000;
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		pw.printf("Compound_%08d_%08d.sdf.gz", (fileno*10000)+1, (fileno+1)*10000);
		File f = new File(pubChemDir, sw.toString());
		if(!f.exists()) return null;
		String cidStr = Integer.toString(cid);
		BufferedReader br = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(f))));
		String line = br.readLine();
		boolean atStart = true;
		while(line != null) {
			if(line.equals("$$$$") || atStart) {
				if(!atStart) line = br.readLine();
				if(line == null) break;
				if(line.equals(cidStr)) {
					line = br.readLine();
					sb.append("\n");
					while(line != null && !line.equals("$$$$")) {
						sb.append(line + "\n");
						line = br.readLine();
					}
					break;
				}
			}
			atStart = false;
			line = br.readLine();
		}
		
		String s = sb.toString();
		if(s.length() == 0) return null;
		return s;
	}
	
}
