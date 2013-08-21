package uk.ac.cam.ch.wwmm.ptc.experimental;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Nodes;

import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.BinomialDistribution;
import org.apache.commons.math.distribution.BinomialDistributionImpl;
import org.apache.commons.math.distribution.ChiSquaredDistribution;
import org.apache.commons.math.distribution.ChiSquaredDistributionImpl;
import org.apache.commons.math.stat.inference.ChiSquareTest;
import org.apache.commons.math.stat.inference.ChiSquareTestImpl;

import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.Token;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.ProcessingDocument;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.ProcessingDocumentFactory;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.TokenSequence;
import uk.ac.cam.ch.wwmm.oscar3server.scrapbook.ScrapBook;
import uk.ac.cam.ch.wwmm.ptclib.datastruct.Bag;
import uk.ac.cam.ch.wwmm.ptclib.io.FileTools;
import uk.ac.cam.ch.wwmm.ptclib.saf.SafTools;
import uk.ac.cam.ch.wwmm.ptclib.scixml.InlineToSAF;
import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;
import uk.ac.cam.ch.wwmm.ptclib.xml.XOMTools;

public class ReactCollocations {

	public static double binomialProbEqualGreater(int trials, int success, double prob) throws MathException {
		BinomialDistribution bd = new BinomialDistributionImpl(trials, prob);
		double score = 0;
		for(int i=success;i<=trials;i++) {
			score += bd.probability(i);
		}
		//double score = bd.cumulativeProbability(success-1);
		System.out.println("bd:\t" + trials + "\t" + success + "\t" + prob + "\t" + (1.0-score) + "\t" + bd.cumulativeProbability(success-1));
		return 1.0 - score;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		if(true) {
			long time = System.currentTimeMillis();
			BinomialDistribution bd = new BinomialDistributionImpl(10000000, 0.5);
			double d = bd.cumulativeProbability(3456789);
			//for(int i=-1;i<=100000;i++) {
			//	double d = bd.cumulativeProbability(i);
			//	//System.out.println(i + "\t" + bd.cumulativeProbability(i) + "\t" + (1.0 - bd.cumulativeProbability(i)));
			//}
			System.out.println(System.currentTimeMillis() - time);
		}
		
		List<File> files = FileTools.getFilesFromDirectoryByName(new File("/home/ptc24/newows/reactnewpubmed"), "scrapbook.xml");
		
		Map<String,Bag<String>> collocsForSense = new HashMap<String,Bag<String>>();
		
		Bag<String> wordsWithReact = new Bag<String>();
		Bag<String> wordsWithoutReact = new Bag<String>();
		
		for(File f : files) {
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
			Set<String> tokenSet = new HashSet<String>();
			Bag<String> tokenBag = new Bag<String>();
			for(TokenSequence t : procDoc.getTokenSequences()) {
				//System.out.println(t.getSourceString());
				for(Token token : t.getTokens()) {
					tokenSet.add(token.getValue().toLowerCase());
					tokenBag.add(token.getValue().toLowerCase());
				}
			}
			//for(String t : tokenBag.getList()) {
			//	System.out.println(t + "\t" + tokenBag.getCount(t));
			//}
			
			//File safFile = new File(f.getParentFile(), "saf.xml");
			//Document safDoc = new Builder().build(safFile);
			Nodes n = safDoc.query("/saf/annot[slot[@name='type']['PRW']]");
			Set<String> wpss = new HashSet<String>();
			boolean hasReact = false;
			for(int i=0;i<n.size();i++) {
				Element annot = (Element)n.get(i);
				String s = SafTools.getSlotValue(annot, "surface").toLowerCase();
				String subtype = SafTools.getSlotValue(annot, "subtype");
				if("REACT".equals(subtype)) hasReact = true;
				String wps = s+"_"+subtype;
				wpss.add(wps);
			}
			for(String wps : wpss) {
				if(!collocsForSense.containsKey(wps)) collocsForSense.put(wps, new Bag<String>());
				//collocsForSense.get(wps).add(tokenBag);
				for(String t : tokenSet) collocsForSense.get(wps).add(t);
			}
			if(hasReact) {
				for(String t : tokenSet) wordsWithReact.add(t);				
			} else {
				for(String t : tokenSet) wordsWithoutReact.add(t);				
			}
		}
		
		Map<String,Set<String>> sensesForWords = new HashMap<String,Set<String>>();
		for(String sense : collocsForSense.keySet()) {
			String [] ss = sense.split("_");
			if(!sensesForWords.containsKey(ss[0])) sensesForWords.put(ss[0], new HashSet<String>());
			sensesForWords.get(ss[0]).add(ss[1]);
		}
		
		for(String word : sensesForWords.keySet()) {
			if(sensesForWords.get(word).size() < 2) continue;
			Bag<String> overall = new Bag<String>();
			System.out.println(word);
			for(String sense : sensesForWords.get(word)) {
				overall.addAll(collocsForSense.get(word+"_"+sense));
			}
			double overallTotal = overall.totalCount();
			for(String sense : sensesForWords.get(word)) {
				System.out.println(sense);
				double sensTotal = collocsForSense.get(word+"_"+sense).totalCount();
				for(String c : collocsForSense.get(word+"_"+sense).getList()) {
					if(collocsForSense.get(word+"_"+sense).getCount(c) < 3) break;
					int sc = collocsForSense.get(word+"_"+sense).getCount(c);
					double sfreq = sc / sensTotal;
					double ofreq = overall.getCount(c) / overallTotal;					
					System.out.println("\t" + c + "\t" + sc + "\t" + sfreq + "\t" + ofreq + "\t" + (sfreq / ofreq));
				}
			}
		}
		
		double overallRatio = wordsWithReact.totalCount() * 1.0 / (wordsWithReact.totalCount() + wordsWithoutReact.totalCount());
		
		int totalReactWords = wordsWithReact.totalCount();
		
		Map<String,Double> scores = new HashMap<String,Double>();
		
		Set<String> words = new HashSet<String>();
		words.addAll(wordsWithReact.getSet());
		words.addAll(wordsWithoutReact.getSet());
		
		ChiSquaredDistribution csd = new ChiSquaredDistributionImpl(1);
		ChiSquareTest cst = new ChiSquareTestImpl();
		
		
		for(String word : wordsWithReact.getList()) {
			int observed = wordsWithReact.getCount(word);
			int observedElsewhere = wordsWithoutReact.getCount(word);
			//if(observed + observedElsewhere < 20) continue;
			double expected = overallRatio * (observed + observedElsewhere);
			double expectedElsewhere = (1.0 - overallRatio) * (observed + observedElsewhere);
			double g = 0.0;
			if(observed > 0) g += 2 * observed * Math.log(observed/expected);
			if(observedElsewhere > 0) g += 2 * observedElsewhere * Math.log(observedElsewhere/expectedElsewhere); 
			
			long [] obsArray = new long[]{observed, observedElsewhere};
			double [] expectArray = new double[]{expected, expectedElsewhere};
			double cs = cst.chiSquare(expectArray, obsArray);
			
			double manualcs = 0.0;
			manualcs += Math.pow(observed - expected, 2.0) / expected;
			manualcs += Math.pow(observedElsewhere - expectedElsewhere, 2.0) / expectedElsewhere;

			double manualcscc = 0.0;
			manualcscc += Math.pow(Math.abs(observed - expected) - 0.5, 2.0) / expected;
			manualcscc += Math.pow(Math.abs(observedElsewhere - expectedElsewhere) - 0.5, 2.0) / expectedElsewhere;
			
			
			//int notObserved = totalReactWords - observed;
			
			//Is this a total bastardisation?
			//double mcnemar = Math.pow(observedElsewhere - notObserved, 2.0) / (observedElsewhere + notObserved);
			//System.out.println(mcnemar);
			//System.out.println(csd.cumulativeProbability(mcnemar));
			
			
			double ratio = observed / (observed + observedElsewhere + 0.0);

			System.out.println(word + "\t" + (ratio < overallRatio));
			double bp;
			if(ratio < overallRatio) {
				bp = binomialProbEqualGreater(observed + observedElsewhere, observedElsewhere, 1.0 - overallRatio);				
			} else {
				bp = binomialProbEqualGreater(observed + observedElsewhere, observed, overallRatio);
			}
			//System.out.println(word + "\t" + bp);
			
			//if(ratio < overallRatio) g = -g;
			//scores.put(word,csd.cumulativeProbability(manualcscc));
			scores.put(word,bp);
			//if(wordsWithReact.getCount(word) < 3) break;
			//double ratio = wordsWithReact.getCount(word) * 1.0 / (wordsWithReact.getCount(word) + wordsWithoutReact.getCount(word));
			//scores.put(word, ratio / overallRatio);
		}
		
		int ss = scores.size();
		System.out.println(ss);
		System.out.println(0.05 / ss);
		int c = 0;
		
		for(String word : StringTools.getSortedList(scores)) {
			c++;
			double p = c * 1.0 / ss;
			double pr = (1.0 - scores.get(word)) / p;
			System.out.println(word + "\t" + wordsWithReact.getCount(word) + "\t" + wordsWithoutReact.getCount(word) + "\t" + (1.0 - scores.get(word)) + "\t" + p + "\t" + pr);
		}
		
		//List<String> senses = new ArrayList<String>(collocsForSense.keySet());
		//Collections.sort(senses);
 		//for(String s : senses) {
 		//	System.out.println(s);
 		//	for(String c : collocsForSense.get(s).getList()) {
 		//		System.out.println("\t" + c + "\t" + collocsForSense.get(s).getCount(c));
 		//	}
 		//}
	}

}
