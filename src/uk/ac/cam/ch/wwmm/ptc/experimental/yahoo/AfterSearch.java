package uk.ac.cam.ch.wwmm.ptc.experimental.yahoo;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;
import opennlp.maxent.DataIndexer;
import opennlp.maxent.Event;
import opennlp.maxent.EventCollectorAsStream;
import opennlp.maxent.GIS;
import opennlp.maxent.GISModel;
import opennlp.maxent.TwoPassDataIndexer;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.TokenSequence;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.Tokeniser;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.tokenanalysis.NGram;
import uk.ac.cam.ch.wwmm.ptclib.misc.ClassificationEvaluator;
import uk.ac.cam.ch.wwmm.ptclib.misc.SimpleEventCollector;
import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;

public class AfterSearch {

	public static double jaccard(Collection<String> s1, Collection<String> s2) {
		Set<String> intersection = new HashSet<String>(s1);
		intersection.retainAll(s2);
		Set<String> union = new HashSet<String>(s1);
		union.addAll(s2);
		return intersection.size() * 1.0 / union.size();
	}
	
	public static Map<String,Collection<String>> getFeatures(Element elem) {
		Elements elems = elem.getChildElements();
		
		Map<String,Collection<String>> features = new HashMap<String,Collection<String>>();
		
		for(int i=0;i<elems.size();i++) {
			String name = elems.get(i).getAttributeValue("word");
			System.out.println(name);
			if(name.endsWith("ase") || name.endsWith("ases")) continue;
			//if(name.endsWith("in")) continue;
			Elements lineElems = elems.get(i).getChildElements();
			//List<String> wordsForWord = new ArrayList<String>();
			Set<String> wordsForWord = new HashSet<String>();
			for(int j=0;j<lineElems.size();j++) {
				String lineVal = lineElems.get(j).getValue();
				TokenSequence t = Tokeniser.getInstance().tokenise(lineVal);
				boolean found = false;
				List<String> tl = new ArrayList<String>();
				for(String s : t.getTokenStringList()) {
					if(s.compareToIgnoreCase(name) == 0) {
						tl.add("*WORD*");
						found = true;
					} else {
						//tl.add(StringTools.normaliseName2(s));
						//tl.add(s);
						//tl.add(nGram.parseWord(s));
						tl.add(s.toLowerCase());
					}
				}
				if(found || !found) {
					//System.out.println(tl);
					for(String word : tl) {
						wordsForWord.add(word);
					}
					//for(int k=0;k<tl.size()-1;k++) {
					//	wordsForWord.add(tl.get(k) + "_" + tl.get(k+1));
					//}
					
					if(true) {
						int ws = 2; //window size
						for(int k=0;k<tl.size();k++) {
							if(tl.get(k).equals("*WORD*")) {
								int start = Math.max(k-ws, 0);
								int end = Math.min(k+ws, tl.size()-1);
								for(int l=start;l<=k;l++) {
									for(int m=Math.max(l+ws-1,k);m<=end;m++) {
										wordsForWord.add(StringTools.collectionToString(tl.subList(l, m+1), "_"));
									}
								}
								
								//if(k > 0) wordsForWord.add(tl.get(k-1) + " *WORD*");
								//if(k+1 < tl.size()) wordsForWord.add("*WORD* " + tl.get(k+1));
								//if(k > 0 && k+1 < tl.size()) wordsForWord.add(tl.get(k-1) + " *WORD* " + tl.get(k+1));
							}
						}
					}
				}
				//if(name.length() > 3) wordsForWord.add("SUFFIX=" + name.substring(name.length()-3));
				/*String decName = "^^^" + name + "$$$";
				for(int k=0;k<decName.length()-3;k++) {
					wordsForWord.add("NGRAM=" + decName.substring(k+1,k+3));
				}*/
			}
			features.put(name, wordsForWord);
			//for(String word : wordsForWord) words.add(word);
			//System.out.println();
		}
		return features;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		Document doc = new Builder().build(new File("/home/ptc24/tmp/searchTest.xml"));
		
		Element root = doc.getRootElement();
		
		Map<String,Collection<String>> overallFeatures = new HashMap<String,Collection<String>>();
		Map<String,Collection<String>> chemFeatures = getFeatures(root.getFirstChildElement("chemical"));
		Map<String,Collection<String>> nonchemFeatures = getFeatures(root.getFirstChildElement("nonchemical"));
		Map<String,Collection<String>> unknownFeatures = getFeatures(root.getFirstChildElement("unknown"));
		
		overallFeatures.putAll(chemFeatures);
		overallFeatures.putAll(nonchemFeatures);
		
		//List<String> chemWords = new ArrayList<String>(chemFeatures.keySet());
		//List<String> nonChemWords = new ArrayList<String>(nonchemFeatures.keySet());
		
		List<String> words = new ArrayList<String>(overallFeatures.keySet());
		Collections.shuffle(words, new Random(0));
		
		List<String> trainSet = words.subList(0, 2*words.size()/4);
		List<String> testSet = words.subList(2*words.size()/4, words.size());
		
		List<Event> events = new ArrayList<Event>();
		for(String word : trainSet) {
			Collection<String> f = overallFeatures.get(word);
			//System.out.println(f);
			String type = chemFeatures.containsKey(word) ? "CHEM" : "NONCHEM";

			Event event = new Event(type, f.toArray(new String[0]));
			events.add(event);
		}
		if(events.size() == 1) events.add(events.get(0));
		DataIndexer di = new TwoPassDataIndexer(new EventCollectorAsStream(new SimpleEventCollector(events)), 3);
		GISModel gm = GIS.trainModel(100, di);
		
		ClassificationEvaluator ce = new ClassificationEvaluator();

		List<Double> chemList = new ArrayList<Double>();
		List<Double> engList = new ArrayList<Double>();

		for(String word : testSet) {
			Collection<String> f = overallFeatures.get(word);
			String type = chemFeatures.containsKey(word) ? "CHEM" : "NONCHEM";
			
			double [] results = gm.eval(f.toArray(new String[0]));
			System.out.println(word + "\t" + gm.getAllOutcomes(results));
			ce.logEvent(type, gm.getBestOutcome(results));
			if(!gm.getBestOutcome(results).equals(type)) System.out.println("*");
			if(type.equals("CHEM")) {
				chemList.add(results[gm.getIndex("CHEM")]);
			} else {
				engList.add(results[gm.getIndex("CHEM")]);
			}

		}
		ce.pprintPrecisionRecallEval();
		ce.pprintConfusionMatrix();
		
		for(String word : unknownFeatures.keySet()) {
			Collection<String> f = unknownFeatures.get(word);
			double [] results = gm.eval(f.toArray(new String[0]));
			System.out.println(word + "\t" + results[gm.getIndex("CHEM")] + "\t" + NGram.getInstance().testWordProb(word) + "\t" + NGram.getInstance().testWordSuffixProb(word));			
		}
		
		/*Bag<String> chemWords = getBag(root.getFirstChildElement("chemical"));
		Bag<String> nonChemWords = getBag(root.getFirstChildElement("nonchemical"));
		Bag<String> words = new Bag<String>();
		words.add(chemWords);
		words.add(nonChemWords);
		words.discardInfrequent(5);
		
		
		double overallRatio = chemWords.size() * 1.0 / nonChemWords.size();
		
		Map<String,Double> wbg = new HashMap<String,Double>();
		
		for(String word : words.getList()) {
			int chemcount = chemWords.getCount(word);
			int nonchemcount = nonChemWords.getCount(word);
			int totalcount = chemcount + nonchemcount;
			double ratio = chemcount * 1.0 / nonchemcount;
			double score = ratio / overallRatio;

			double g = 0;
			if(chemcount > 0) g += 2 * (chemcount * Math.log(chemcount / (totalcount * overallRatio)));
			if(nonchemcount > 0) g += 2 * (nonchemcount * Math.log(nonchemcount / (totalcount *(1.0-overallRatio))));
			//wbg.put(word, g);
			wbg.put(word, score);
			//if(score < 0.5 && word.contains(" ")) System.out.println(word + "\t" + chemWords.getCount(word) + "\t" + nonChemWords.getCount(word) + "\t" + score);
		}
		for(String word : StringTools.getSortedList(wbg)) {
			System.out.println(word + "\t" + chemWords.getCount(word) + "\t" + nonChemWords.getCount(word) + "\t" + wbg.get(word));
		}*/
		Collections.sort(chemList, Collections.reverseOrder());
		Collections.sort(engList, Collections.reverseOrder());
		int chemCount = 0;
		int engCount = 0;
		double map = 0.0;
		while(chemCount < chemList.size() && engCount < engList.size()) {
			if(engList.get(engCount) >= chemList.get(chemCount)) {
				engCount++;
			} else {
				chemCount++;
				double precision = chemCount * 1.0 / (chemCount + engCount);
				map += precision / chemList.size();
				if(true) {
					System.out.println((chemCount * 1.0 / chemList.size()) + "\t" + precision);
				}
			}
		}
		System.out.println(map);
		
		
		if(false) {
			ClassificationEvaluator ce2 = new ClassificationEvaluator();
			for(String word : testSet) {
				String bestWord = null;
				double bestScore = 0.0;
				for(String testWord : trainSet) {
					double score = jaccard(overallFeatures.get(word), overallFeatures.get(testWord));
					if(score > bestScore) {
						bestScore = score;
						bestWord = testWord;
					}
				}
				if(bestScore < 0.12) continue;
				String type = chemFeatures.containsKey(word) ? "CHEM" : "NONCHEM";
				String testType = chemFeatures.containsKey(bestWord) ? "CHEM" : "NONCHEM";
				System.out.println(word + "\t" + bestWord);
				if(!type.equals(testType)) {
					System.out.println("*");
				}
				
				ce2.logEvent(type, testType);
			}
			ce2.pprintPrecisionRecallEval();			
		}
	}

}
