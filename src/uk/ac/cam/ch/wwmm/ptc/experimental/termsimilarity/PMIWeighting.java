package uk.ac.cam.ch.wwmm.ptc.experimental.termsimilarity;

public class PMIWeighting extends Weighting {

	@Override
	public double weight(int featureCount, int totalFeatureCount,
			int termCount, int totalWordCount, int coCount) {
		double observed = (double)coCount;
		double expected = ((double)termCount/totalWordCount) * featureCount;
		double pmi = Math.log((observed)/(expected));
		return pmi;		

	}

}
