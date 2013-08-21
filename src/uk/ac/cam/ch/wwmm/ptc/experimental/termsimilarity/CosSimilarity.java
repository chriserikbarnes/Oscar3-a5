package uk.ac.cam.ch.wwmm.ptc.experimental.termsimilarity;

import java.util.Map;

public class CosSimilarity extends Similarity {

	@Override
	public double similarity(Map<String, Double> v1, Map<String, Double> v2) {
		// TODO Auto-generated method stub
		double weightSumSquare1 = 0.0;
		for(String f : v1.keySet()) {
			weightSumSquare1 += v1.get(f) * v1.get(f);
		}
		double weightSumSquare2 = 0.0;
		for(String f : v2.keySet()) {
			weightSumSquare2 += v2.get(f) * v2.get(f);
		}
		double coWeightSum = 0.0;
		for(String f : v1.keySet()) {
				if(v2.containsKey(f)) {
					coWeightSum += (v1.get(f) * v2.get(f));				
				}
		}
		return coWeightSum / (Math.sqrt(weightSumSquare1) * Math.sqrt(weightSumSquare2));
	}

}
