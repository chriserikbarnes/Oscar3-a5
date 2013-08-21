package uk.ac.cam.ch.wwmm.ptc.experimental.termsimilarity;

import java.util.Map;

import uk.ac.cam.ch.wwmm.ptclib.datastruct.Bag;

public abstract class FeatureVectorExtractor {

	public abstract Map<String,Bag<String>> getFeatureVectors();
	public abstract Bag<String> getFeatures();
	public abstract Bag<String> getTerms();
	
}
