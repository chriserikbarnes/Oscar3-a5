package uk.ac.cam.ch.wwmm.ptc.experimental;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math.stat.regression.SimpleRegression;

import nu.xom.Builder;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.ProcessingDocument;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.ProcessingDocumentFactory;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.Token;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.TokenSequence;
import uk.ac.cam.ch.wwmm.oscar3.terms.TermSets;
import uk.ac.cam.ch.wwmm.ptclib.datastruct.Bag;
import uk.ac.cam.ch.wwmm.ptclib.io.FileTools;
import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;

public class CrossEntropy {

	public static double mutualInformation(Bag<String> bigBag, Bag<String> subBag) {
		double mi = 0.0;
		
		double bigTotalCount = bigBag.totalCount();
		double subTotalCount = subBag.totalCount();
		double pSub = subTotalCount / bigTotalCount;
		double pRem = 1.0 - pSub;
		if(pSub == 0.0 || pRem == 0.0) return 0.0;
		for(String s : bigBag.getSet()) {
			double bigCount = bigBag.getCount(s);
			double subCount = subBag.getCount(s);
			double remCount = bigCount - subCount;
			if(subCount > 0.0) {
				//mi += (subCount / bigTotalCount) * (Math.log((subCount / bigTotalCount) / (pSub * bigCount / bigTotalCount)) / Math.log(2));
				mi += (subCount / bigTotalCount) * (Math.log(subCount / (pSub * bigCount)) / Math.log(2));
			}
			if(remCount > 0.0) {
				mi += (remCount / bigTotalCount) * (Math.log(remCount / (pRem * bigCount)) / Math.log(2));				
			}
		}
		
		double omi = (-pSub * Math.log(pSub) / Math.log(2)) + (-pRem * Math.log(pRem) / Math.log(2)); 
		
		return mi;
		//return mi - omi;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		List<File> files = new ArrayList<File>();
		files.addAll(FileTools.getFilesFromDirectoryByName(new File("/home/ptc24/newows/corpora/BioIE"), "source.xml"));
		//files = files.subList(0, 50);
		
		List<Bag<String>> bags = new ArrayList<Bag<String>>();
		Bag<String> bigBag = new Bag<String>();
		Map<String,Bag<String>> index = new HashMap<String,Bag<String>>();
		
		
		
		for(File file : files) {
			System.out.println(file);
			ProcessingDocument procDoc = ProcessingDocumentFactory.getInstance().makeTokenisedDocument(new Builder().build(file), false, false, false);
			Bag<String> b = new Bag<String>();
			for(TokenSequence ts : procDoc.getTokenSequences()) {
				for(Token t : ts.getTokens()) {
					String s = t.getValue().intern();
					if(!TermSets.getClosedClass().contains(s.toLowerCase())) b.add(s);
				}
			}
			bags.add(b);
			bigBag.addAll(b);
			for(String s : b.getSet()) {
				if(!index.containsKey(s)) index.put(s, new Bag<String>());
				index.get(s).addAll(b);
			}
		}
		
		System.out.println(bigBag.entropy());
		Map<String,Double> saves = new HashMap<String,Double>();
		SimpleRegression sr = new SimpleRegression();
		for(String s : bigBag.getList()) {
			Bag<String> b = index.get(s);
			//double entropy = b.entropy();
			//double crossEntropy = b.crossEntropy(bigBag);
			//double compressionSaves = (crossEntropy - entropy);
			//System.out.println(s + "\t" + bigBag.getCount(s) + "\t" + entropy + "\t" + compressionSaves);
			//saves.put(s, mutualInformation(bigBag, b));
			//Bag<String> bb = new Bag<String>(b);
			//bb.discardInfrequent(10);
			//if(bb.size() == 0) continue;
			System.out.println(s + "\t" + b.totalCount() + "\t" + b.size());
			sr.addData(Math.log(b.totalCount()), Math.log(b.size()));
		}
		System.out.println(sr.getRSquare());
		double m = sr.getSlope();
		double c = sr.getIntercept();
		for(String s : bigBag.getList()) {
			Bag<String> b = index.get(s);
			saves.put(s, m * Math.log(b.totalCount()) + c - Math.log(b.size()));
		}
		
		for(String s : StringTools.getSortedList(saves)) {
			System.out.println(s + "\t" + saves.get(s) + "\t" + index.get(s).totalCount());
		}
		
	}

}
