package uk.ac.cam.ch.wwmm.ptc.experimental;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import opennlp.maxent.DataIndexer;
import opennlp.maxent.Event;
import opennlp.maxent.EventCollectorAsStream;
import opennlp.maxent.GIS;
import opennlp.maxent.GISModel;
import opennlp.maxent.TwoPassDataIndexer;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.tokenanalysis.NGram;
import uk.ac.cam.ch.wwmm.ptclib.misc.SimpleEventCollector;
import uk.ac.cam.ch.wwmm.ptclib.misc.StringGISModelReader;
import uk.ac.cam.ch.wwmm.ptclib.misc.StringGISModelWriter;

public class Attenuation {

	public static String[] getFeatures(String word) {
		List<String> features = new ArrayList<String>();
		for(int i=0;i<word.length()-3;i++) {
			features.add(word.substring(i, i+4));
		}
		return features.toArray(new String[0]);
	}
	
	public static double probToLogit(double p) {
		return Entropy.log2(p / (1-p));
	}
	
	public static double logitToProb(double l) {
		double pPrime = 1.0/(1+Math.pow(2,l));
		double p = 1.0 - pPrime;
		return p;
	}
	
	public static String[] probToFeatures(double p) {
		double l = probToLogit(p);
		String fStr = "+";
		if(l < 0) {
			fStr = "-";
			l = -l;
		}
		int ll = (int)(l * 10);
		ll = Math.min(ll, 1000);
		//System.out.println(p + "\t" + ll);
		String [] features = new String[ll];
		for(int i=0;i<ll;i++) features[i] = fStr;
		return features;
	}
	
	public static double boost(double p, double factor) {
		return logitToProb(probToLogit(p) * factor);
	}
	
	public static double logAdd(double a, double b) {
		if(a < b) return logAdd(b, a);
		double diff = a - b;
		double expa = Math.exp(diff);
		double expt = expa + 1;
		if(expt == Double.POSITIVE_INFINITY) return a;
		return Math.log(expt) + b;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
				
		//for(double p = 0.005;p<1;p+=0.01) {
		//	System.out.println(p + "\t" + probToLogit(p) + "\t" + logitToProb(probToLogit(p)*2));
		//	System.out.println(Math.pow(p, 2.0));
		//}

		//System.out.println(logAdd(-1000, -1000));
		
		//if(true) return;
		
		NGram ng = NGram.getInstance();
		ng.engSet.removeAll(ng.chemSet);
		Map<String,Boolean> isChem = new HashMap<String,Boolean>();
		for(String chem : ng.chemSet) {
			isChem.put(chem, true);
		}
		for(String eng : ng.engSet) {
			isChem.put(eng, false);
		}
		List<String> toClassify = new ArrayList<String>();
		toClassify.addAll(ng.chemSet);
		toClassify.addAll(ng.engSet);
		
		Random r = new Random("Hello world!".hashCode());
		Collections.shuffle(toClassify, r);
		
		List<String> batch1 = toClassify.subList(0, toClassify.size() / 3);
		List<String> batch2 = toClassify.subList(toClassify.size() / 3, (2*toClassify.size()) / 3);
		List<String> batch3 = toClassify.subList((2*toClassify.size()) / 3, toClassify.size());
		
		List<Event> events = new ArrayList<Event>();
		for(String s : batch1) {
			String outcome = "E";
			if(isChem.get(s)) outcome = "C";
			events.add(new Event(outcome, getFeatures(s)));
		}
		SimpleEventCollector sec = new SimpleEventCollector(events);
		DataIndexer di = new TwoPassDataIndexer(new EventCollectorAsStream(sec), 1);
		GISModel gis = GIS.trainModel(100, di);
		
		System.out.println(gis.getIndex("FOO"));
		if(true) return;
		
		
		long time = System.currentTimeMillis();
		StringGISModelWriter sgmw = new StringGISModelWriter(gis);
		sgmw.persist();
		String modelStr = sgmw.toString();

		//System.out.println(modelStr);
		
		StringGISModelReader sgmr = new StringGISModelReader(modelStr);
		gis = sgmr.getModel();
		System.out.println("Round trip in " + (System.currentTimeMillis() - time));
		System.out.println(modelStr.length());
		
		//if(true) return;
		
		GISModel gis2 = null;
		
		if(false) {
			List<Event> events2 = new ArrayList<Event>();
			for(String s : batch2) {
				double prob = gis.eval(getFeatures(s))[gis.getIndex("C")];
				String outcome = "E";
				if(isChem.get(s)) outcome = "C";
				events.add(new Event(outcome, probToFeatures(prob)));
			}
			sec = new SimpleEventCollector(events);
			di = new TwoPassDataIndexer(new EventCollectorAsStream(sec), 2);
			gis2 = GIS.trainModel(1000, di);			
		}
		
		double score = 0.0;
		List<Double> trialScores = new ArrayList<Double>();
		for(double i=-1;i<1;i+=0.02) trialScores.add(Math.pow(10, i));
		Map<Double,Double> scores = new HashMap<Double,Double>();
		for(double d : trialScores) scores.put(d, 0.0);
		for(String s : batch3) {
			String outcome = "E";
			if(isChem.get(s)) outcome = "C";
			//System.out.println(s + "\t" + outcome + "\t" + gis.getBestOutcome(gis.eval(getFeatures(s))));
			double prob;
			if(gis2 == null) {
				prob = gis.eval(getFeatures(s))[gis.getIndex(outcome)];				
			} else {
				prob = gis2.eval(probToFeatures(gis.eval(getFeatures(s))[gis.getIndex("C")]))[gis.getIndex(outcome)];				
			}

			//System.out.print(prob + "\t");
			//prob = logitToProb(probToLogit(prob));
			
			score += Entropy.log2(prob);
			
			for(double d : trialScores) scores.put(d, scores.get(d) + Entropy.log2(logitToProb(probToLogit(prob)/d)));
			//System.out.println(prob);
		}
		for(double d : trialScores) {
			System.out.println(d + "\t" + scores.get(d));
		}
		
		System.out.println(score);
	}

}
