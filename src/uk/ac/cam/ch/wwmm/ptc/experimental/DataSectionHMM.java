package uk.ac.cam.ch.wwmm.ptc.experimental;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Nodes;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.ProcessingDocument;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.ProcessingDocumentFactory;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.Token;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.TokenSequence;
import uk.ac.cam.ch.wwmm.ptclib.datastruct.Bag;
import uk.ac.cam.ch.wwmm.ptclib.io.FileTools;

public class DataSectionHMM {

	Map<String,Bag<String>> counts;
	Map<String,Bag<String>> transitionCounts;
	Bag<String> totalCounts;
	Set<String> commonWords;
	int grandTotal;
	
	int truePos;
	int falsePos;
	int falseNeg;
	
	private String repForToken(TokenSequence t, int i) {
		Token token = t.getToken(i);
		String s = token.getValue();
		//if(s.equals("(")) {
		//	Token tt = token.getNAfter(1);
		//	if(tt != null && tt.getValue().equals("Found")) s += tt.getValue();
		//}
		//if(token.isCompRef()) return "$CR";
		//if(s.matches("\\[\u03b1\\]\\d*D.*")) s = "$alphaD";
		//if(s.matches("[\u03bd\u03bb]max(\\(.*)?")) s = "$max";
		//if(s.matches("\u03b4[CHP](\\(.*)?")) s = "$NMR";
		return s;
	}
	
	private String convertWord(String word) {
		//if(word.matches("[mMbB]\\.?p|\\[\u03b1\\]\\d*D|" +
		//"IR|[\u03bd\u03bb]max(\\(.*)?|\u03b4[CHP](\\(.*)?|Rf")) return "$DataStart";
		if(commonWords.contains(word)) return word;
		//word.replaceAll("\\d", "0");
		if(word.matches("\\d+")) return word.replaceAll("\\d", "0");
		if(word.matches("\\d+%")) return word.replaceAll("\\d", "0");
		if(word.matches("\\d+\\.\\d+")) return word.replaceAll("\\d", "0");
		if(word.matches("\\d+\\.\\d+%")) return word.replaceAll("\\d", "0");
		//if(word.matches("[a-z]+")) return "[a-z]+";
		//if(word.matches(".*[a-z][aeiou].*|.*[aeiou][a-z].*")) return "$PROPERWORD";
		return "RARE";
	}
	
	private double getEmissionProbability(String word, String type) {
		//double cf = totalCounts.getCount(type) * 1.0 / grandTotal;
		int count = counts.get(type).getCount(word);
		double prob = Math.log((count + 0.001) / (totalCounts.getCount(type) + (0.001 + totalCounts.size())));
		//double prob = Math.log((count + cf) / (totalCounts.getCount(type) + (1.0)));
		return prob;
	}
	
	private double getTransitionProbability(String prevType, String type) {
		return Math.log((transitionCounts.get(prevType).getCount(type) + 0.0) / (transitionCounts.get(prevType).totalCount()));
	}
	
	public DataSectionHMM() {
		counts = new HashMap<String,Bag<String>>();
		transitionCounts = new HashMap<String,Bag<String>>();
		
		truePos = 0;
		falsePos = 0;
		falseNeg = 0;
	}
	
	public void trainOnSentence(TokenSequence t) {
		String prev = "START";
		String type;
		//t.toBIOEW();
		t.toRichTags();
		for(Token token : t.getTokens()) {
			type = token.getBioTag();
			String str = repForToken(t, token.getId());
			//String str = convertWord(token.getValue());
			if(!counts.containsKey(type)) counts.put(type, new Bag<String>());
			counts.get(type).add(str);
			if(!transitionCounts.containsKey(prev)) transitionCounts.put(prev, new Bag<String>());
			transitionCounts.get(prev).add(type);
			prev = type;
		}
		type = "END";
		if(!transitionCounts.containsKey(prev)) transitionCounts.put(prev, new Bag<String>());
		transitionCounts.get(prev).add(type);
	}
	
	public void trainOnFile(File file) throws Exception {
		long time = System.currentTimeMillis();
		System.out.print("Train on: " + file + "... ");
		Document doc = new Builder().build(file);
		Nodes n = doc.query("//cmlPile");
		for(int i=0;i<n.size();i++) n.get(i).detach();
		
		/*NameRecogniser nr = new NameRecogniser();
		nr.halfProcess(doc);
		//if(patternFeatures) {
		//	nr.findForReps(true);
		//} else {
			nr.makeTokenisers(true);
		//}*/
		ProcessingDocument procDoc = ProcessingDocumentFactory.getInstance().makeTokenisedDocument(doc, false, false, false);
		
		for(TokenSequence t : procDoc.getTokenSequences()) {
			trainOnSentence(t);
		}
		System.out.println(System.currentTimeMillis() - time);
	}
	
	public void finishTraining() {
		commonWords = new HashSet<String>();
		Bag<String> totalWords = new Bag<String>();
		for(String type : counts.keySet()) {
			totalWords.addAll(counts.get(type));
		}
		Set<String> privWords = new HashSet<String>(counts.get("B-ONT").getSet());
		//privWords.addAll(counts.get("E-ONT").getSet());
		//privWords.addAll(counts.get("O-B-ONT").getSet());
		for(String word : totalWords.getSet()) {
			if(totalWords.getCount(word) > (privWords.contains(word) ? 2 : 15)) commonWords.add(word);
		}
//		commonWords.addAll();
		
		totalCounts = new Bag<String>();
		for(String type : counts.keySet()) {
			counts.put(type, convert(counts.get(type)));
			//addOne(counts.get(type));
			//hapaxify(counts.get(type));
			totalCounts.add(type, counts.get(type).totalCount());
		}
		grandTotal = totalCounts.totalCount();
	}
	
	private Bag<String> convert(Bag<String> b) {
		Bag<String> newBag = new Bag<String>();
		for(String w : b.getSet()) {
			newBag.add(convertWord(w), b.getCount(w));
		}
		return newBag;
	}
		
	public void printModel() {
		for(String type : counts.keySet()) {
			System.out.println("Counts for type " + type);
			for(String word : counts.get(type).getList()) {
				System.out.println("\t" + word + "\t" + counts.get(type).getCount(word));
			}
		}
		for(String type : transitionCounts.keySet()) {
			for(String typeTo : transitionCounts.get(type).getList()) {
				System.out.println(type + "\t" + typeTo + "\t" + transitionCounts.get(type).getCount(typeTo));
			}
		}
	}
	
	public void testOnFile(File file) throws Exception {
		long time = System.currentTimeMillis();
		System.out.println("Test on: " + file);
		Document doc = new Builder().build(file);
		Nodes n = doc.query("//cmlPile");
		for(int i=0;i<n.size();i++) n.get(i).detach();
		
		//NameRecogniser nr = new NameRecogniser();
		//nr.halfProcess(doc);
		//nr.makeTokenisers(true);
		Set<String> testDataSections = new LinkedHashSet<String>();

		ProcessingDocument procDoc = ProcessingDocumentFactory.getInstance().makeTokenisedDocument(doc, false, false, false);
		
		for(TokenSequence tokSeq : procDoc.getTokenSequences()) {
			Nodes neNodes = tokSeq.getElem().query(".//ne");
			for(int k=0;k<neNodes.size();k++) {
				Element neElem = (Element)neNodes.get(k);
				String neStr = "["+ neElem.getAttributeValue("xtspanstart") + ":" + neElem.getAttributeValue("xtspanend") + "]";
				testDataSections.add(neStr);
			}
		}
		List<String> tdsl = new ArrayList<String>(testDataSections);
		Collections.sort(tdsl);
		System.out.println(tdsl);
			
		Set<String> results = new HashSet<String>();
		for(TokenSequence t : procDoc.getTokenSequences()) {
			results.addAll(testOnSentence(t));
		}
		List<String> rl = new ArrayList<String>(results);
		Collections.sort(rl);
		System.out.println(rl);
		
		Set<String> tp = new HashSet<String>();
		tp.addAll(results);
		tp.retainAll(testDataSections);
		truePos += tp.size();
		falsePos += results.size() - tp.size();
		falseNeg += testDataSections.size() - tp.size();
		
		System.out.println("Tested in: " + (System.currentTimeMillis() - time));
	}
	
	public Set<String> testOnSentence(TokenSequence t) {
		Set<String> results = new HashSet<String>();
		
		if(t.getTokens().size() < 2) return results;
		// NB all logprobs here
		List<Map<String,String>> prevsByLabel = new ArrayList<Map<String,String>>();
		//List<Map<String,Double>> probsByPrev = new ArrayList<Map<String,Double>>();
		Map<String,Double> probByPrev = new HashMap<String,Double>();
		probByPrev.put("START", 0.0);
		for(Token token : t.getTokens()) {
			String v = convertWord(repForToken(t, token.getId()));
			Map<String,Double> newProbByPrev = new HashMap<String,Double>();
			Map<String,String> newPrevByLabel = new HashMap<String,String>();
			for(String prevType : counts.keySet()) {
				newPrevByLabel.put(prevType, "OOPS");
				newProbByPrev.put(prevType, Double.NEGATIVE_INFINITY);
			}
			for(String type : counts.keySet()) {
				double prob = getEmissionProbability(v, type);
				for(String prevType : probByPrev.keySet()) {
					double transProb = getTransitionProbability(prevType, type);
					double pp = probByPrev.get(prevType) + transProb + prob;
					//System.out.println(type + "\t" + prevType + "\t" + transitionCounts.get(prevType).getCount(type) + "\t" + transitionCounts.get(prevType).totalCount() + "\t" + transProb + "\t" + pp);
					
					double prevProb = newProbByPrev.get(type);
					if(pp > prevProb) {
						newProbByPrev.put(type, pp);
						newPrevByLabel.put(type, prevType);
					}
				}
			}
			//System.out.println(v + "\t" + newProbByPrev + "\t" + newPrevByLabel);
			prevsByLabel.add(newPrevByLabel);
			probByPrev = newProbByPrev;
		}
		String bestLast = "OOPS";
		double finalProb = Double.NEGATIVE_INFINITY;
		for(String type : counts.keySet()) {
			//System.out.println(probByPrev);
			//System.out.println(probByPrev.get(type));
			//System.out.println(transitionCounts.get(type));
			double tfp = probByPrev.get(type) + getTransitionProbability(type, "END");
			if(tfp > finalProb) {
				finalProb = tfp;
				bestLast = type;
			}
		}
		List<String> tags = new ArrayList<String>(t.size());
		for(int i=0;i<t.size();i++) { 
			tags.add("");
		}
		String tag = bestLast;
		for(int i=t.size()-1;i>=0;i--) {
			tags.set(i, tag);
			tag = prevsByLabel.get(i).get(tag);
		}
		Token startToken = null;
		for(int i=0;i<t.size();i++) {
			//System.out.println(t.getToken(i).getValue() + "\t" + tags.get(i));
			if(startToken != null && !tags.get(i).matches("[IE]-ONT.*")) {
				//System.out.println(t.getSubstring(startToken.getId(), t.getToken(i-1).getId()));
				results.add("[" + startToken.getStart() + ":" + t.getToken(i-1).getEnd() + "]");
				startToken = null;
			}
			if(tags.get(i).equals("B-ONT")) startToken = t.getToken(i);
		}
		if(startToken != null) {
			results.add("[" + startToken.getStart() + ":" + t.getToken(t.size()-1).getEnd() + "]");
			//System.out.println(t.getSubstring(startToken.getId(), t.size()-1));
		}
		return results;
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		//List<File> files = FileTools.getFilesFromDirectoryByName(new File("/home/ptc24/oscarworkspace/datascrap/"), "scrapbook.xml");
		List<File> files = FileTools.getFilesFromDirectoryByName(new File("/home/ptc24/oscarworkspace/scrapbook/"), "scrapbook.xml");
		Collections.reverse(files);
		//Collections.shuffle(files);
		List<File> trainFiles = files;
		//List<File> trainFiles = files.subList(0, files.size() / 2);
		//trainFiles.addAll(FileTools.getFilesFromDirectoryByName(new File("/home/ptc24/oscarworkspace/datascrap/"), "scrapbook.xml"));
		//trainFiles.addAll(FileTools.getFilesFromDirectoryByName(new File("/home/ptc24/oscarworkspace/scrapbook/"), "scrapbook.xml"));
		//List<File> testFiles = files.subList(files.size() / 2, (files.size() / 2) + 1);
		//List<File> testFiles = files.subList(files.size() / 2, files.size());
		//List<File> testFiles = FileTools.getFilesFromDirectoryByName(new File("/home/ptc24/oscarworkspace/scrapbook/"), "scrapbook.xml");
		List<File> testFiles = FileTools.getFilesFromDirectoryByName(new File("/home/ptc24/oscarworkspace/scrapbook/"), "scrapbook.xml");
		DataSectionHMM dsh = new DataSectionHMM();
		for(File f : trainFiles) {
			dsh.trainOnFile(f);
		}
		dsh.finishTraining();
		dsh.printModel();
		for(File f : testFiles) {
			dsh.testOnFile(f);
		}
		
		double prec = dsh.truePos / (0.0 + dsh.truePos + dsh.falsePos);
		double rec = dsh.truePos / (0.0 + dsh.truePos + dsh.falseNeg);
		double f = (dsh.truePos * 2.0) / (dsh.truePos + dsh.truePos + dsh.falsePos + dsh.falseNeg);
		
		System.out.println(prec + "\t" + rec + "\t" + f);
		System.out.println(dsh.truePos + "\t" + dsh.falsePos + "\t" + dsh.falseNeg);
		
	}

}
