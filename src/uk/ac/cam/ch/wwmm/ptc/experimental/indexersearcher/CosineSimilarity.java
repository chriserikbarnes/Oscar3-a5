package uk.ac.cam.ch.wwmm.ptc.experimental.indexersearcher;

import java.util.Map;

public class CosineSimilarity implements DocVectorSimilarity {

	public double similarity(Map<Integer, Float> v1, Map<Integer, Float> v2) {
		double weightSumSquare1 = 0.0;
		for(int f : v1.keySet()) {
			weightSumSquare1 += v1.get(f) * v1.get(f);
		}
		double weightSumSquare2 = 0.0;
		for(int f : v2.keySet()) {
			weightSumSquare2 += v2.get(f) * v2.get(f);
		}
		double coWeightSum = 0.0;
		for(int f : v1.keySet()) {
				if(v2.containsKey(f)) {
					coWeightSum += (v1.get(f) * v2.get(f));				
				}
		}
		return coWeightSum / (Math.sqrt(weightSumSquare1) * Math.sqrt(weightSumSquare2));
	}

}
