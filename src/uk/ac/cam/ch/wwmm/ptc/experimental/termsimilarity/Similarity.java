package uk.ac.cam.ch.wwmm.ptc.experimental.termsimilarity;

import java.util.Map;

public abstract class Similarity {

	public abstract double similarity(Map<String,Double> v1, Map<String,Double> v2);
	
}
