package uk.ac.cam.ch.wwmm.ptc.experimental.indexersearcher;

import java.util.Map;

public interface DocVectorSimilarity {

	public double similarity(Map<Integer,Float> v1, Map<Integer,Float> v2);
	
}
