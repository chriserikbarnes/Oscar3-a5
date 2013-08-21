package uk.ac.cam.ch.wwmm.ptc.experimental;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.UnivariateStatistic;
import org.apache.commons.math.stat.descriptive.moment.Mean;
import org.apache.commons.math.stat.descriptive.moment.StandardDeviation;

import uk.ac.cam.ch.wwmm.oscar3.models.ExtractTrainingData;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.tokenanalysis.NGram;
import uk.ac.cam.ch.wwmm.ptc.experimental.indexersearcher.SVDHarness;
import uk.ac.cam.ch.wwmm.ptclib.datastruct.Bag;
import uk.ac.cam.ch.wwmm.ptclib.misc.ClassificationEvaluator;
import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;
import Jama.Matrix;

public class NGramSVD {

	public static double pdf(double x, double m, double s) {
		return Math.exp(-(Math.pow(x-m,2)/(2*Math.pow(s,2)))) / (s * Math.PI); 
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		Bag<String> allFeatures = new Bag<String>();
		//for(String word : new ArrayList<String>(TermSets.getUsrDictWords()).subList(0,100)) {

		//List<String> terms = new ArrayList<String>(TermSets.getUsrDictWords());
		Set<String> chemSet = NGram.getInstance().chemSet;
		Set<String> engSet = NGram.getInstance().engSet;	
		Set<String> wordSet = new HashSet<String>();
		wordSet.addAll(chemSet);
		wordSet.addAll(engSet);
		
		List<String> terms = new ArrayList<String>(wordSet);
		Collections.shuffle(terms, new Random(7));		
		terms = terms.subList(0, 1000);
		
		//List<String> terms = new ArrayList<String>(ExtractTrainingData.getInstance().chemicalWords);
		//terms.addAll(ExtractTrainingData.getInstance().nonChemicalWords);
		//System.out.println(terms.size());
		for(String word : terms) {
			Bag<String> ngrams = new Bag<String>();
			String fooWord = "^^^" + word.toLowerCase() + "$$$";
			for(int i=0;i<fooWord.length() - 3;i++) {
				ngrams.add(fooWord.substring(i,i+4));
			}
			//System.out.println(ngrams);
			allFeatures.addAll(ngrams);
		}
		int termNo = terms.size();
		int featureNo = 0;//allFeatures.size();
		List<String> featureList = allFeatures.getList();
		Map<String,Integer> featureIndex = new HashMap<String,Integer>();
		for(String feature : featureList) {
			featureIndex.put(feature, featureNo);
			featureNo++;
		}

		SVDHarness svdh = new SVDHarness(termNo, featureNo);
		int wordNo = 0;
		for(String word : terms) {
			Bag<String> ngrams = new Bag<String>();
			String fooWord = "^^^" + word.toLowerCase() + "$$$";
			for(int i=0;i<fooWord.length() - 3;i++) {
				ngrams.add(fooWord.substring(i,i+4));
			}
			for(String f : ngrams.getSet()) {
				int fno = featureIndex.get(f);
				svdh.set(fno, wordNo, ngrams.getCount(f));
			}
			wordNo++;
		}
		System.out.println("Harness ready...");
		int values = 50;
		svdh.svd(values);
		Matrix ut = svdh.getUt(); // Terms
		//Matrix vt = svdh.getVt(); // Features
		
		//System.out.println(ut.getColumnDimension() + "\t" + ut.getRowDimension());
		//System.out.println(vt.getColumnDimension() + "\t" + vt.getRowDimension());
		//System.out.println(wordNo);
		//System.out.println(featureList.size());
		
		UnivariateStatistic mean = new Mean();
		UnivariateStatistic stdev = new StandardDeviation();
		List<Double> cmeans = new ArrayList<Double>();
		List<Double> cstdevs = new ArrayList<Double>();
		List<Double> ncmeans = new ArrayList<Double>();
		List<Double> ncstdevs = new ArrayList<Double>();
		int ccount = 0;
		int nccount = 0;
		for(int i=0;i<values;i++) {
			Map<String,Double> results = new HashMap<String,Double>();
			for(int j=0;j<wordNo;j++) {
				results.put(terms.get(j), ut.get(i,j));
			}
			List<String> resultsList = StringTools.getSortedList(results);
			for(String s : resultsList.subList(0, 20)) {
				System.out.println(s + "\t" + results.get(s));
			}
			System.out.println("...");
			for(String s : resultsList.subList(resultsList.size()-20, resultsList.size())) {
				System.out.println(s + "\t" + results.get(s));
			}
			List<Double> chemScores = new ArrayList<Double>();
			List<Double> nonChemScores = new ArrayList<Double>();
			List<Double> allScores = new ArrayList<Double>();
			for(int j=500;j<1000;j++) {
				boolean isChem = chemSet.contains(terms.get(j));
				//System.out.println(isChem);
				List<Double> list = isChem ? chemScores : nonChemScores;
				list.add(ut.get(i,j));
				allScores.add(ut.get(i,j));
			}
			System.out.println(chemScores.size() + "\t" + nonChemScores.size());
			double [] cs = new double[chemScores.size()];
			for(int j=0;j<chemScores.size();j++) cs[j] = chemScores.get(j);
			double [] ncs = new double[nonChemScores.size()];
			for(int j=0;j<nonChemScores.size();j++) ncs[j] = nonChemScores.get(j);
			double [] as = new double[allScores.size()];
			for(int j=0;j<allScores.size();j++) as[j] = allScores.get(j);
			System.out.println(mean.evaluate(cs) + "\t" + stdev.evaluate(cs));
			System.out.println(mean.evaluate(ncs) + "\t" + stdev.evaluate(ncs));
			cmeans.add(mean.evaluate(cs));
			cstdevs.add(stdev.evaluate(cs));
			ncmeans.add(mean.evaluate(ncs));
			ncstdevs.add(stdev.evaluate(ncs));
			System.out.println();
			ccount = cs.length;
			nccount = ncs.length;
		}
		double cp = ccount / (0.0 + ccount + nccount);
		double ncp = 1.0 - cp;
		
		ClassificationEvaluator ce = new ClassificationEvaluator();
		for(int j=0;j<500;j++) {
			double thiscp = Math.log(cp);
			double thisncp = Math.log(ncp);
			for(int i=0;i<values;i++) {
				thiscp += Math.log(pdf(ut.get(i,j), cmeans.get(i), cstdevs.get(i)));
				thisncp += Math.log(pdf(ut.get(i,j), ncmeans.get(i), ncstdevs.get(i)));
			}
			String refClass = chemSet.contains(terms.get(j)) ? "CHEM" : "NONCHEM";
			String respClass = thiscp > thisncp ? "CHEM" : "NONCHEM";
			System.out.println(terms.get(j) + "\t" + (thiscp - thisncp));
			ce.logEvent(refClass, respClass);
		}
		System.out.println(ce.getAccuracy());
		System.out.println(ce.getKappa());
		ce.pprintConfusionMatrix();
		ce.pprintPrecisionRecallEval();
	}

}
