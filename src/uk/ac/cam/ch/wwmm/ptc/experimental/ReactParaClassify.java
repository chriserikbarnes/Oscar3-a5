package uk.ac.cam.ch.wwmm.ptc.experimental;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Nodes;
import opennlp.maxent.DataIndexer;
import opennlp.maxent.Event;
import opennlp.maxent.EventCollectorAsStream;
import opennlp.maxent.GIS;
import opennlp.maxent.GISModel;
import opennlp.maxent.TwoPassDataIndexer;

import org.apache.commons.math.distribution.ChiSquaredDistribution;
import org.apache.commons.math.distribution.ChiSquaredDistributionImpl;
import org.apache.commons.math.stat.inference.ChiSquareTest;
import org.apache.commons.math.stat.inference.ChiSquareTestImpl;
import org.tartarus.snowball.ext.PorterStemmer;

import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.ProcessingDocument;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.ProcessingDocumentFactory;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.Token;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.TokenSequence;
import uk.ac.cam.ch.wwmm.oscar3server.scrapbook.ScrapBook;
import uk.ac.cam.ch.wwmm.ptc.experimental.classifiers.BagEvent;
import uk.ac.cam.ch.wwmm.ptc.experimental.classifiers.DecisionList;
import uk.ac.cam.ch.wwmm.ptc.experimental.classifiers.DecisionTree;
import uk.ac.cam.ch.wwmm.ptc.experimental.classifiers.MultinomialNaiveBayes;
import uk.ac.cam.ch.wwmm.ptclib.datastruct.Bag;
import uk.ac.cam.ch.wwmm.ptclib.io.FileTools;
import uk.ac.cam.ch.wwmm.ptclib.misc.ClassificationEvaluator;
import uk.ac.cam.ch.wwmm.ptclib.misc.SimpleEventCollector;
import uk.ac.cam.ch.wwmm.ptclib.misc.Stemmer;
import uk.ac.cam.ch.wwmm.ptclib.saf.SafTools;
import uk.ac.cam.ch.wwmm.ptclib.scixml.InlineToSAF;
import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;
import uk.ac.cam.ch.wwmm.ptclib.xml.XOMTools;

public class ReactParaClassify {

	static Stemmer stemmer = new Stemmer(new PorterStemmer());
	static Map<Bag<String>,String> bagsToSentences;
	
	static Set<String> allowableFeatures = new HashSet<String>(StringTools.arrayToList("reaction devic microfluid synthesi cellular scheme experiment aba h cell signal analogu gradient acid perform yield prepar catalyst their abil respons flow level format differ resolut synthes ni 1 = order the menthol tetralon volum . intracellular local dynam mixtur select data investig channel metabol group product".split("\\s+")));
	
	static class ParaRawFeatures {
		TokenSequence tokSeq;
		Bag<String> prws;
		boolean hasReact;
		
		@Override
		public String toString() {
			return "[" + tokSeq + ", " + prws + ", " + hasReact + "]";
		}
	}

	private static double bagSimilarity(Bag<String> b1, Bag<String> b2) {
		Set<String> s1 = b1.getSet();
		Set<String> s2 = b2.getSet();
		
		Set<String> union = new HashSet<String>(s1);
		union.addAll(s2);
		Set<String> intersection  = new HashSet<String>(s1);
		intersection.retainAll(s2);
		
		return intersection.size() * 1.0 / union.size();
	}
	
	private static BagEvent prfToMNBFeatures(ParaRawFeatures prf) {
		Bag<String> features = new Bag<String>();
		Set<String> prwf = new HashSet<String>();
		for(String s : prf.prws.getSet()) {
			String ss = s.split(":")[0];
			prwf.add(ss);
		}
		for(String s : prf.tokSeq.getTokenStringList()) {
			String ss = s.toLowerCase();
			//if(!ss.matches(".*[a-zA-Z0-9].*")) continue;
			//if(TermSets.getClosedClass().contains(ss)) continue;
			//if(prwf.contains(ss)) {
			//	features.add(ss.intern());				
			//} else {
				ss = stemmer.getStem(ss);
				//if(!allowableFeatures.contains(ss)) continue;
				features.add(ss.intern());								
			//}
			
			//if(!prwf.contains(ss)) ss = stemmer.getStem(ss);
		}
		bagsToSentences.put(features, prf.tokSeq.getSourceString());
		return new BagEvent(prf.hasReact? "TRUE" : "FALSE", features);
	}
	
	private static List<ParaRawFeatures> fileToRawFeatures(File f) throws Exception {
		//System.out.println();
		System.out.println(f);
		//System.out.println(StringTools.multiplyString("=", f.toString().length()));
		//System.out.println();
		
		ScrapBook sb = new ScrapBook(f.getParentFile());
		Document doc = (Document)sb.getDoc().copy();
		Nodes nodes = doc.query("//cmlPile");
		for(int i=0;i<nodes.size();i++) nodes.get(i).detach();
		Document sourceDoc = (Document)doc.copy();
		nodes = sourceDoc.query("//ne");
		for(int i=0;i<nodes.size();i++) {
			XOMTools.removeElementPreservingText((Element)nodes.get(i));
		}
		Document safDoc = InlineToSAF.extractSAFs(doc, sourceDoc, "foo");

		ProcessingDocument procDoc = ProcessingDocumentFactory.getInstance().makeTokenisedDocument(sourceDoc, false, false, false);

		//NameRecogniser nr = new NameRecogniser();
		//nr.halfProcess(sourceDoc);
		//nr.makeTokenisers(false);

		Nodes n = safDoc.query("/saf/annot[slot[@name='type']['PRW']]");

		Map<TokenSequence,Boolean> tokSeqs = new HashMap<TokenSequence,Boolean>();
		Map<TokenSequence,Bag<String>> tokSeqPRWs = new HashMap<TokenSequence,Bag<String>>();
		
		for(int i=0;i<n.size();i++) {
			Element e = (Element)n.get(i);
			Token token = procDoc.getTokenByStart(e.getAttributeValue("from"));
			if(token == null) token = procDoc.getTokenByEnd(e.getAttributeValue("to"));
			if(token == null) {
				System.out.println("Eeep!");
			} else {
				TokenSequence tokSeq = token.getTokenSequence();
				boolean isReact = "REACT".equals(SafTools.getSlotValue(e, "subtype"));
				if(tokSeqs.containsKey(tokSeq)) {
					if(isReact) tokSeqs.put(tokSeq, true);
				} else {
					tokSeqs.put(tokSeq, isReact);
					tokSeqPRWs.put(tokSeq, new Bag<String>());
				}
				String prwStr = SafTools.getSlotValue(e, "surface").toLowerCase() + ":" + SafTools.getSlotValue(e, "subtype");
				tokSeqPRWs.get(tokSeq).add(prwStr);
			}
		}
		
		List<ParaRawFeatures> prfs = new ArrayList<ParaRawFeatures>();
		for(TokenSequence tokSeq : tokSeqs.keySet()) {
			ParaRawFeatures prf = new ParaRawFeatures();
			prf.tokSeq = tokSeq;
			prf.hasReact = tokSeqs.get(tokSeq);
			prf.prws = tokSeqPRWs.get(tokSeq);
			prfs.add(prf);
		}
		return prfs;

	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		bagsToSentences = new HashMap<Bag<String>,String>();
	
		//List<File> files = FileTools.getFilesFromDirectoryByName(new File("/home/ptc24/newows/reactmiscrsceasy"), "scrapbook.xml");
		//List<File> files = FileTools.getFilesFromDirectoryByName(new File("/home/ptc24/newows/reactgoodrsc"), "scrapbook.xml");
		//List<File> files = FileTools.getFilesFromDirectoryByName(new File("/home/ptc24/newows/reactnewpubmed"), "scrapbook.xml");
	
		List<File> files = new ArrayList<File>();
		files.addAll(FileTools.getFilesFromDirectoryByName(new File("/home/ptc24/newows/reactmiscrsceasy"), "scrapbook.xml"));
		files.addAll(FileTools.getFilesFromDirectoryByName(new File("/home/ptc24/newows/reactgoodrsc"), "scrapbook.xml"));
		files.addAll(FileTools.getFilesFromDirectoryByName(new File("/home/ptc24/newows/reactnewpubmed"), "scrapbook.xml"));
		Collections.shuffle(files, new Random(0));
		
		List<File> trainFiles = files.subList(0, files.size() / 2);
		List<File> testFiles = files.subList(files.size() / 2, files.size());
		
		List<ParaRawFeatures> prfs = new ArrayList<ParaRawFeatures>();
		for(File f : trainFiles) {
			prfs.addAll(fileToRawFeatures(f));
		}
		List<BagEvent> bagEvents = new ArrayList<BagEvent>();
		for(ParaRawFeatures prf : prfs) {
			bagEvents.add(prfToMNBFeatures(prf));
		}
		
		
		int minOccur = 1;
		
		Bag<String> overallFCs = new Bag<String>();
		for(BagEvent be : bagEvents) {
			for(String f : be.getFeatures().getSet()) overallFCs.add(f);
		}
		for(BagEvent be : bagEvents) {
			for(String f : new ArrayList<String>(be.getFeatures().getSet())) {
				if(overallFCs.getCount(f) < minOccur) be.getFeatures().remove(f);
			}
		}
		
		//for(String f : overallFCs.getList()) {
		//	System.out.println(f + "\t" + overallFCs.getCount(f));
		//}
		
		List<ParaRawFeatures> testPrfs = new ArrayList<ParaRawFeatures>();
		for(File f : testFiles) {
			testPrfs.addAll(fileToRawFeatures(f));
		}
		List<BagEvent> testBagEvents = new ArrayList<BagEvent>();
		for(ParaRawFeatures prf : testPrfs) {
			testBagEvents.add(prfToMNBFeatures(prf));
		}
		for(BagEvent be : testBagEvents) {
			for(String f : new ArrayList<String>(be.getFeatures().getSet())) {
				if(overallFCs.getCount(f) < minOccur) be.getFeatures().remove(f);
			}
		}

		
		ClassificationEvaluator ce = new ClassificationEvaluator();
	
		if(true) {
			DecisionTree dt = new DecisionTree(bagEvents);
			dt.printTree();
			for(int i=0;i<testBagEvents.size();i++) {
				BagEvent be = testBagEvents.get(i);
				String result = dt.testBag(be.getFeatures());
				ce.logEvent(be.getClassLabel(), result);
			}
			System.out.println(ce.getAccuracy());
			System.out.println(ce.getKappa());			
			ce.pprintConfusionMatrix();
			ce.pprintPrecisionRecallEval();
			//return;
		}
		
		if(true) {
			ce = new ClassificationEvaluator();
			DecisionList dl = new DecisionList(bagEvents);
			for(int i=0;i<testBagEvents.size();i++) {
				BagEvent be = testBagEvents.get(i);
				String result = dl.testBag(be.getFeatures());
				ce.logEvent(be.getClassLabel(), result);
			}
			System.out.println(ce.getAccuracy());
			System.out.println(ce.getKappa());			
			ce.pprintConfusionMatrix();
			ce.pprintPrecisionRecallEval();
			//return;
		}
		
		ce = new ClassificationEvaluator();
		MultinomialNaiveBayes mnb = new MultinomialNaiveBayes(bagEvents);
		for(int i=0;i<testBagEvents.size();i++) {
			BagEvent be = testBagEvents.get(i);
			//Map<String,Double> results = mnb.testBag(be.getClassLabel(), be.getFeatures());
			Map<String,Double> results = mnb.testBag(be.getFeatures());
			System.out.println(be.getClassLabel() + "\t" + mnb.testBag(be.getFeatures()));
			ce.logEvent(be.getClassLabel(), mnb.bestResult(results));
		}
		System.out.println(ce.getAccuracy());
		System.out.println(ce.getKappa());			
		ce.pprintConfusionMatrix();
		ce.pprintPrecisionRecallEval();

		ce = new ClassificationEvaluator();
		List<Event> trainEvents = new ArrayList<Event>();
		List<Event> testEvents = new ArrayList<Event>();
		for(BagEvent be : bagEvents) {
			trainEvents.add(new Event(be.getClassLabel(), be.getFeatures().getSet().toArray(new String[0])));
		}
		for(BagEvent be : testBagEvents) {
			testEvents.add(new Event(be.getClassLabel(), be.getFeatures().getSet().toArray(new String[0])));
		}
		DataIndexer di = new TwoPassDataIndexer(new EventCollectorAsStream(new SimpleEventCollector(trainEvents)), 1);
		GISModel gm = GIS.trainModel(100, di);
		
		//ClassificationEvaluator ce = new ClassificationEvaluator();
		
		for(Event event : testEvents) {
			double [] results = gm.eval(event.getContext());
			String result = results[gm.getIndex("TRUE")] > 0.5 ? "TRUE" : "FALSE";
			//String result = gm.getBestOutcome(results);
			//System.out.println(event.getOutcome() + "\t" + result + "\t" + results[gm.getIndex(event.getOutcome())] + "\t" + StringTools.arrayToList(event.getContext()));
			ce.logEvent(event.getOutcome(), result);
		}
		System.out.println(ce.getAccuracy());
		System.out.println(ce.getKappa());			
		ce.pprintConfusionMatrix();
		ce.pprintPrecisionRecallEval();
		
		if(false) {
			
			List<Bag<String>> trueBags = new ArrayList<Bag<String>>();
			List<Bag<String>> falseBags = new ArrayList<Bag<String>>();
			for(BagEvent be : bagEvents) {
				if("TRUE".equals(be.getClassLabel())) {
					trueBags.add(be.getFeatures());
				} else {
					falseBags.add(be.getFeatures());
				}
			}
			Random r = new Random(0);
			Collections.shuffle(trueBags, r);
			Collections.shuffle(falseBags, r);
			if(trueBags.size() > falseBags.size()) {
				trueBags = trueBags.subList(0, falseBags.size());
			} else if(falseBags.size() > trueBags.size()) {
				falseBags = falseBags.subList(0, trueBags.size());
			}
			
			double [][] similarities = new double[trueBags.size()][trueBags.size()];
			
			for(int i=0;i<trueBags.size();i++) {
				for(int j=0;j<trueBags.size();j++) {
					//similarities[i][j] = r.nextDouble(); 
					similarities[i][j] = bagSimilarity(trueBags.get(i), falseBags.get(j));
				}
			}
			
			Set<Integer> remainingTrues = new LinkedHashSet<Integer>();
			Set<Integer> remainingFalses = new LinkedHashSet<Integer>();
			Bag<String> onlyTrues = new Bag<String>();
			Bag<String> onlyFalses = new Bag<String>();
			
			Bag<String> trues = new Bag<String>();
			Bag<String> falses = new Bag<String>();
			
			
			for(int i=0;i<trueBags.size();i++) {
				remainingTrues.add(i);
				remainingFalses.add(i);
			}
			while(remainingTrues.size() > 0) {
				int bestFalse = -1;
				int bestTrue = -1;
				double bestSim = -1.0;
				for(Integer i : remainingTrues) {
					for(Integer j : remainingFalses) {
						if(similarities[i][j] > bestSim) {
							bestTrue = i;
							bestFalse = j;
							bestSim = similarities[i][j];
						}
					}
				}
				remainingTrues.remove(bestTrue);
				remainingFalses.remove(bestFalse);
				System.out.println(bestTrue + "\t" + bestFalse + "\t" + bestSim);
				Bag<String> t = trueBags.get(bestTrue);
				Bag<String> f = falseBags.get(bestFalse);
				System.out.println(bagsToSentences.get(t));
				System.out.println(bagsToSentences.get(f));
				Bag<String> c = new Bag<String>();
				c.addAll(t);
				c.addAll(f);
				for(String s : c.getSet()) {
					if(t.getCount(s) > 0 && f.getCount(s) == 0) {
						onlyTrues.add(s);
					} else if(t.getCount(s) == 0) {
						onlyFalses.add(s);
					}
				}
				for(String s : t.getSet()) {
					trues.add(s);
				}
				for(String s : f.getSet()) {
					falses.add(s);
				}
				//for(String s : c.getList()) {
				//	System.out.println(s + "\t" + t.getCount(s) + "\t" + f.getCount(s));
				//}
			}
			
			double ratio = trues.size() / (0.0 + trues.size() + falses.size());
			ChiSquaredDistribution csd = new ChiSquaredDistributionImpl(1);
			ChiSquareTest cst = new ChiSquareTestImpl();
			
			Bag<String> combined = new Bag<String>();
			combined.addAll(onlyTrues);
			combined.addAll(onlyFalses);
			Bag<String> tpf = new Bag<String>();
			tpf.addAll(trues);
			tpf.addAll(falses);
			tpf.discardInfrequent(5);
			//combined.discardInfrequent(8);
			Map<String,Double> mcNemarScores = new HashMap<String,Double>();
			for(String s : tpf.getList()) {
				int b = onlyTrues.getCount(s);
				int c = onlyFalses.getCount(s);
				double score = Math.pow(b-c, 2) / (b+c);
				int t = trues.getCount(s);
				int f = falses.getCount(s);
				double et = (t + f) * ratio;
				double ef = (t + f) * (1.0 - ratio);
				long [] obsArray = new long[]{t, f};
				double [] expectArray = new double[]{et, ef};
				double cs = cst.chiSquare(expectArray, obsArray);
				//score = cs;
				if(Double.isNaN(score)) score = 0.0;
				mcNemarScores.put(s, score);
			}

			int ss = mcNemarScores.size();
			int count = 0;
			
			boolean beforeCutOff = true;
			for(String s : StringTools.getSortedList(mcNemarScores)) {
				count++;
				double foo = count * 1.0 / ss;
				int b = onlyTrues.getCount(s);
				int c = onlyFalses.getCount(s);
				
				int t = trues.getCount(s);
				int f = falses.getCount(s);
				double et = (t + f) * ratio;
				double ef = (t + f) * (1.0 - ratio);
				long [] obsArray = new long[]{t, f};
				double [] expectArray = new double[]{et, ef};
				double cs = cst.chiSquare(expectArray, obsArray);
				if(beforeCutOff && ((1.0 - csd.cumulativeProbability(mcNemarScores.get(s))) / foo) > 0.05) {
					System.out.println(count - 1);
					beforeCutOff = false;
					//break;
				}
				System.out.println(s + "\t" + b + "\t" + c + "\t" + t + "\t" + f + "\t" + mcNemarScores.get(s)
						 + "\t" + (1.0 - csd.cumulativeProbability(mcNemarScores.get(s)))
						 + "\t" + ((1.0 - csd.cumulativeProbability(mcNemarScores.get(s))) / foo)
						 + "\t" + csd.cumulativeProbability(cs));
			}			
		}
	}

}
