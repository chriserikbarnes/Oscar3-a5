package uk.ac.cam.ch.wwmm.ptc.experimental.yahoo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.List;

import uk.ac.cam.ch.wwmm.ptclib.datastruct.Bag;
import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;

public class AndOtherClassifier {

	public static Bag<String> andOtherForTerm(String term) throws Exception {
		Bag<String> extracted = new Bag<String>();
		List<String> results = YahooSearch.getLines(term + " and other *", 1, false);
		extracted.addAll(YahooSearch.extractTerms(results,term + " and other *", true));
		return extracted;
	}
	
	public static void gather() throws Exception {
		List<String> chemicals = StringTools.arrayToList(new String[] {
				//oripavine, bergamottin, 
				"verapamil", "tacrolimus", "calcium"});
		List<String> nonChemicals = StringTools.arrayToList(new String[] {
				"prostate", "mucosa", "P450", "CYP2D6", "lactogen", "gonadotropin"});
		//Map<String,Bag<String>> bagsForTerm = new HashMap<String,Bag<String>>();
		PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File("/home/ptc24/tmp/ao.txt"))));
		
		for(String chemical : chemicals) {
			Bag<String> b = andOtherForTerm(chemical);
			out.println(chemical + "\t" + "CHEM");
			for(String s : b.getList()) {
				out.println(s + "\t" + b.getCount(s));
			}
			out.println();
		}
		for(String nonChemical : nonChemicals) {
			Bag<String> b = andOtherForTerm(nonChemical);
			out.println(nonChemical + "\t" + "NONCHEM");
			for(String s : b.getList()) {
				out.println(s + "\t" + b.getCount(s));
			}
			out.println();
		}
		out.close();
		//System.out.println(bagsForTerm);
		
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		File f = new File("/home/ptc24/tmp/ao.txt");
		BufferedReader br = new BufferedReader(new FileReader(f));
		String line = br.readLine();
		String currentWord = null;
		boolean isChem = false;
		Bag<String> termBag = null;
		while(line != null) {
			if(currentWord == null && line.length() > 0) {
				String [] parts = line.split("\t");
				currentWord = parts[0];
				isChem = "CHEM".equals(parts[1]);
				termBag = new Bag<String>();
			} else if(line.length() > 0) {
				String [] parts = line.split("\t");
				termBag.add(parts[0], Integer.parseInt(parts[1]));
			} else {
				System.out.println(currentWord + "\t" + isChem + "\t" + termBag);
				currentWord = null;
				termBag = null;
			}
			line = br.readLine();
		}
	}

}
