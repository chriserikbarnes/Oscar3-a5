package uk.ac.cam.ch.wwmm.ptc.experimental.yahoo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Nodes;
import nu.xom.XPathContext;

import org.apache.commons.lang.StringUtils;

import uk.ac.cam.ch.wwmm.oscar3.Oscar3Props;
import uk.ac.cam.ch.wwmm.ptclib.datastruct.Bag;
import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;

public class YahooSearch {

	static boolean verbose = false;
	
	public static double wordSimilarity(String s1, String s2) {
		int ld = StringUtils.getLevenshteinDistance(s1, s2);
		return 1.0 - (ld / (1.0 * Math.max(s1.length(), s2.length())));
	}
	
	// http://developer.yahoo.com/search/web/V1/webSearch.html is the reference
	
	public static int getNumber(String query) throws Exception {
		String yahooURL = "http://search.yahooapis.com/WebSearchService/V1/webSearch?";

		yahooURL += "appid=" + Oscar3Props.getInstance().yahooKey;
		yahooURL += "&type=phrase";
 		yahooURL += "&query=" + StringTools.urlEncodeUTF8NoThrow("\"" + query + "\"");
		//System.out.println(yahooURL);
		Document doc = null;
		
		try {
	 		doc = new Builder().build(yahooURL);			
		} catch (Exception e) {
			try {
				//System.err.println("Retrying...");
				e.printStackTrace();
		 		doc = new Builder().build(yahooURL);							
			} catch (Exception ee) {
				//System.err.println("Not trying a third time!");
				ee.printStackTrace();
			}
		}
		if(doc == null) return 0;
		
		int available = Integer.parseInt(doc.getRootElement().getAttributeValue("totalResultsAvailable"));
		return available;
	}
	
	public static List<String> getLines(String query, int start, boolean follow) throws Exception {
		
		List<String> results = new ArrayList<String>();
		String yahooURL = "http://search.yahooapis.com/WebSearchService/V1/webSearch?";
		yahooURL += "appid=" + Oscar3Props.getInstance().yahooKey;
		yahooURL += "&results=100";
		yahooURL += "&type=phrase";
		if(start > 1) yahooURL += "&start=" + start;
 		yahooURL += "&query=" + StringTools.urlEncodeUTF8NoThrow("\"" + query + "\"");
		System.out.println(yahooURL);

		Document doc = null;
		
		try {
	 		doc = new Builder().build(yahooURL);			
		} catch (Exception e) {
			try {
				System.err.println("Retrying...");
				e.printStackTrace();
		 		doc = new Builder().build(yahooURL);							
			} catch (Exception ee) {
				System.err.println("Not trying a third time!");
				ee.printStackTrace();
			}
		}
		
		if(doc == null) return results;
		
		XPathContext xpc = new XPathContext("y", "urn:yahoo:srch");
		
		Nodes n = doc.query("//y:Summary", xpc);

		for(int i=0;i<n.size();i++) {
			String resStr = n.get(i).getValue();
			List<String> resParts = StringTools.arrayToList(resStr.split("\\.\\.\\."));
			for(String res : resParts) {
				res = " " + res.trim() + " ";
				res = res.replaceAll("\\s+", " ");
				res = res.replaceAll("&gt;", ">");
				res = res.replaceAll("&lt;", "<");
				res = res.replaceAll("&amp;", "&");
				if(res.trim().length() > 0)	results.add(res);
			}
		}

		int available = Integer.parseInt(doc.getRootElement().getAttributeValue("totalResultsAvailable"));
		System.out.println(available);
		
		if(follow && available - start > 100 && start < 802) {
			results.addAll(getLines(query, start+100, true));
		}
		return results;
	}
	
	public static List<String> deDuplicate(List<String> results) {
		List<String> toCheck = new ArrayList<String>(results);
		List<String> newResults = new ArrayList<String>();
		for(String s : toCheck) {
			boolean isTooSimilar = false;
			for(String r : newResults) {
				double similarity = wordSimilarity(s, r);
				if(similarity > 0.75) {
					System.out.println("*****");
					System.out.println(s);
					System.out.println(r);
					System.out.println(similarity);
					System.out.println("*****");
					isTooSimilar = true;
					break;
				}
			}
			if(!isTooSimilar) newResults.add(s);
		}
		return newResults;
	}
	
	public static Bag<String> extractTerms(List<String> results, String searchTerm, boolean expand) throws Exception {
		String stRe = searchTerm;
		stRe = stRe.replaceAll("^\\*", " *");
		stRe = stRe.replaceAll("\\*$", "* ");
		stRe = stRe.replaceAll("\\* \\* \\* \\* \\*", "(\\\\S+?[() ,\\\\-\\\\.]+\\\\S+?[() ,\\\\-\\\\.]+\\\\S+?[() ,\\\\-\\\\.]+\\\\S+?[() ,\\\\-\\\\.]+\\\\S+?)");
		stRe = stRe.replaceAll("\\* \\* \\* \\*", "(\\\\S+?[() ,\\\\-\\\\.]+\\\\S+?[() ,\\\\-\\\\.]+\\\\S+?[() ,\\\\-\\\\.]+\\\\S+?)");
		stRe = stRe.replaceAll("\\* \\* \\*", "(\\\\S+?[() ,\\\\-\\\\.]+\\\\S+?[() ,\\\\-\\\\.]+\\\\S+?)");
		stRe = stRe.replaceAll("\\* \\*", "(\\\\S+?[() ,\\\\-\\\\.]+\\\\S+?)");
		stRe = stRe.replaceAll("\\*", "(\\\\S+?)");
		stRe = stRe.replaceAll(" ", "[() ,\\\\-\\\\.]+");
		stRe = ".*" + stRe + ".*";
		System.out.println(stRe);
		Pattern replacePattern = Pattern.compile(stRe);	
		Pattern activePattern = replacePattern;
		
		Bag<String> extracted = new Bag<String>();
		
		
		for(String res : results) {
			if(verbose) System.out.println(res);
			Matcher m = activePattern.matcher(res);
			if(m.matches()) {
				if(verbose) System.out.println(m.group(1));
				extracted.add(m.group(1));
			}
		}
		
		if(expand) {
			Bag<String> newExtracted = new Bag<String>();
			for(String s : extracted.getList()) {
				String newQuery = searchTerm.replaceAll("\\*( \\*)*", s);
				int n = getNumber(newQuery);
				newExtracted.set(s, n);
			}
			return newExtracted;
		} else {
			return extracted;			
		}
		
		/*for(String s : extracted.getList()) {
			System.out.println(s + "\t" + extracted.getCount(s));
		}*/
	}
	
	public static void wordsToFiles(File outDir, List<String> terms) throws Exception {
		if(!outDir.exists()) outDir.mkdir();
		for(String term : terms) {
			List<String> results = getLines(term, 1, true);
			File outFile = new File(outDir, term + ".txt");
			Writer w = new OutputStreamWriter(new FileOutputStream(outFile), "UTF-8");
			for(String result : results) {
				if(result.toLowerCase().contains(term.toLowerCase())) {
					w.write(result);
					w.write("\n");					
				}
			}
			w.close();
		}
	}
	
	public static void foobar(List<String> terms, boolean expand) throws Exception {
		Bag<String> extracted = new Bag<String>();
		for(String term : terms) {
			List<String> results = getLines(term, 1, true);
			extracted.addAll(extractTerms(results, term, expand));
		}
		for(String s : extracted.getList()) {
			System.out.println(s + "\t" + extracted.getCount(s));
		}

	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		if(true) {
			//File dir = new File("/home/ptc24/tmp/yahoo");
			List<String> terms = StringTools.arrayToList(new String[]{
					"the * * * will have a field day",
					"the * * will have a field day",
					"the * will have a field day"});
					//"the * was catalyzed"});
							//"catalysed * of", "catalyzed * of", "catalysed the * of", "catalyzed the * of"});
					//"* and other bacteria", "bacteria such as *"});
			foobar(terms, true);
			return;
		}

		if(false) {
			File dir = new File("/home/ptc24/tmp/yahoo");
			List<String> words = StringTools.arrayToList(new String[]{
					//"demethylation", "emergence", "elimination"});
					//"addition", "substitution", "coupling", "condensation"});
					//"transport", "efflux", "diffusion"});
					//"oxidation", "reduction"});
					//"verapamil", "bergamottin"});
					"prostate", "mucosa", "epithelium"});
			wordsToFiles(dir, words);
			return;
		}
		if(true) {
			String searchTerm = "demethylation";
			List<String> results = getLines(searchTerm, 1, true);
			int maxPos = -1;
			for(String result : results) {
				if(!result.toLowerCase().contains(searchTerm)) continue; 
				maxPos = Math.max(maxPos, result.toLowerCase().indexOf(searchTerm));
			}
			for(String result : results) {
				if(!result.toLowerCase().contains(searchTerm)) continue; 
				int spaces = maxPos - result.toLowerCase().indexOf(searchTerm);
				System.out.println(StringTools.multiplyString(" ", spaces) + result);
			}
		}
	}

}
