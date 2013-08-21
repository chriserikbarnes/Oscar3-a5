package uk.ac.cam.ch.wwmm.ptc.experimental.termsimilarity;

public abstract class Weighting {

	public abstract double weight(int featureCount, int totalFeatureCount, int termCount, int totalWordCount, int coCount);
	
}
