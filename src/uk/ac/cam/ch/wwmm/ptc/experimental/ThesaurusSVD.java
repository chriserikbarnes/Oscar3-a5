package uk.ac.cam.ch.wwmm.ptc.experimental;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.ac.cam.ch.wwmm.ptc.experimental.indexersearcher.SVDHarness;
import uk.ac.cam.ch.wwmm.ptc.experimental.termsimilarity.CosSimilarity;
import uk.ac.cam.ch.wwmm.ptc.experimental.termsimilarity.FeatureVectorExtractor;
import uk.ac.cam.ch.wwmm.ptc.experimental.termsimilarity.InlineFVE;
import uk.ac.cam.ch.wwmm.ptc.experimental.termsimilarity.SimilarityExtractor;
import uk.ac.cam.ch.wwmm.ptc.experimental.termsimilarity.TTestWeighting;
import uk.ac.cam.ch.wwmm.ptclib.io.FileTools;
import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;
import Jama.Matrix;

public class ThesaurusSVD {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		List<File> files = new ArrayList<File>();			
		//files = FileTools.getFilesFromDirectoryByName(new File("/home/ptc24/oscarworkspace/corpora/paperset1"), "markedup.xml");
		files = FileTools.getFilesFromDirectoryByName(new File("/home/ptc24/oscarworkspace/corpora/newEnzyme"), "markedup.xml");

		FeatureVectorExtractor fve = new InlineFVE(files);
		SimilarityExtractor se = new SimilarityExtractor(fve, new CosSimilarity(), new TTestWeighting());

		System.out.println("Indexing...");
		Map<String,Integer> termIndex = new HashMap<String,Integer>();
		int termNo = 0;
		List<String> terms = se.getMostFrequentTerms(5000);
		for(String term : terms) {
			termIndex.put(term, termNo);
			termNo++;
		}
		int featureNo = 0;
		List<String> features = se.getFeatures();
		Map<String,Integer> featureIndex = new HashMap<String,Integer>();
		for(String feature : features) {
			featureIndex.put(feature, featureNo);
			featureNo++;
		}
		System.out.println("Indexed...");
		SVDHarness svdh = new SVDHarness(termNo, featureNo);
		for(int i=0;i<termNo;i++) {
			Map<String,Double> wv = se.getWeightVector(terms.get(i));
			for(String feature : wv.keySet()) {
				int fno = featureIndex.get(feature);
				svdh.set(fno, i, wv.get(feature));
			}
		}
		System.out.println("Harness ready...");
		svdh.svd(10);
		Matrix lm = svdh.getUt().transpose();
		System.out.println(lm.getRowDimension());
		System.out.println(lm.getColumnDimension());
		System.out.println(termNo);
		System.out.println(featureNo);
		double [] svals = svdh.getS();
		if(true) {
			for(int i=0;i<svals.length;i++) {
				System.out.println(svals[i]);
				for(int j=0;j<lm.getRowDimension();j++) {
					lm.set(j, i, lm.get(j, i) * svals[i]);
				}
			}			
		}

		for(int tn=0;tn<termNo;tn++) {
			System.out.println(terms.get(tn));
			Map<String,Double> cosines = new HashMap<String,Double>();
			for(int i=0;i<terms.size();i++) {
				double termScore = 0.0;
				double otherTermScore = 0.0;
				double product = 0.0;
				
				for(int j=0;j<svals.length;j++) {
					double tVal = lm.get(tn,j);
					double otVal = lm.get(i,j);
					termScore += tVal * tVal;
					otherTermScore += otVal * otVal;
					product += tVal * otVal;
				}
				double cosine = product / (Math.sqrt(termScore) * Math.sqrt(otherTermScore));
				if(cosine > 0.4) cosines.put(terms.get(i), cosine);
			}
			for(String s : StringTools.getSortedList(cosines)) {
				System.out.println("\t" + s + "\t" + cosines.get(s) + "\t" + se.getSimilarity(terms.get(tn), s));
			}

		}
	}

}
