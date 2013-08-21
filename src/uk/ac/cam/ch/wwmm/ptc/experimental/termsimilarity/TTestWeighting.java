package uk.ac.cam.ch.wwmm.ptc.experimental.termsimilarity;

public class TTestWeighting extends Weighting {

	@Override
	public double weight(int featureCount, int totalFeatureCount, int termCount, int totalWordCount, int coCount) {
		double coProb = (double)coCount / totalFeatureCount;
		double wProb = (double)termCount / totalWordCount;
		double fProb = (double)featureCount / totalFeatureCount;
		//System.out.println(coProb + " " + wProb + " " + fProb);
		double value = (coProb - (fProb * wProb)) / Math.sqrt(fProb * wProb);
		if(value < 0.0) return 0.0;
		return value;
	}

}
