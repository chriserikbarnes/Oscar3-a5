package uk.ac.cam.ch.wwmm.ptc.experimental.termsimilarity;

import java.util.Map;

public class DiceSimilarity extends Similarity {

	@Override
	public double similarity(Map<String, Double> v1, Map<String, Double> v2) {
		double sumMin = 0.0;
		double sum1 = 0.0;
		double sum2 = 0.0;
		for(String f : v1.keySet()) {
			sum1 += v1.get(f);
			if(v2.containsKey(f)) {
				sumMin += Math.min(v1.get(f), v2.get(f));
			}
		}
		for(String f : v2.keySet()) {
			sum2 += v2.get(f);
		}
		if(sum1 + sum2 == 0) return 0.0;
		return (2*sumMin/(sum1 + sum2));
	}

	
}
