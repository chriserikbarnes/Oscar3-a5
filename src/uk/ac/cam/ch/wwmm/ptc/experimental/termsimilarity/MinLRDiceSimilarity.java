package uk.ac.cam.ch.wwmm.ptc.experimental.termsimilarity;

import java.util.Map;

public class MinLRDiceSimilarity extends Similarity {

	@Override
	public double similarity(Map<String, Double> v1, Map<String, Double> v2) {
		// TODO Auto-generated method stub
		return Math.min(diceSimilarity(v1, v2, "L_"), diceSimilarity(v1, v2, "R_"));
	}

	private double diceSimilarity(Map<String, Double> v1, Map<String, Double> v2, String prefix) {
		double sumMin = 0.0;
		double sum1 = 0.0;
		double sum2 = 0.0;
		for(String f : v1.keySet()) {
			if(prefix != null && !f.startsWith(prefix)) continue;
			sum1 += v1.get(f);
			if(v2.containsKey(f)) {
				sumMin += Math.min(v1.get(f), v2.get(f));
			}
		}
		for(String f : v2.keySet()) {
			if(prefix != null && !f.startsWith(prefix)) continue;
			sum2 += v2.get(f);
		}
		if(sum1 + sum2 == 0) return 0.0;
		return (2*sumMin/(sum1 + sum2));
	}

	
}
