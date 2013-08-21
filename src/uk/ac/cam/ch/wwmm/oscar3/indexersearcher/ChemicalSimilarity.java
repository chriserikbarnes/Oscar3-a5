package uk.ac.cam.ch.wwmm.oscar3.indexersearcher;

import org.apache.lucene.search.Similarity;

/**Similarity metric for Lucene for indexing of fingerprints. This isn't quite
 * the Tanimoto coefficient, but it works quite well and potentially we can play
 * some games with making the fingerprinting algorithm aware of the index as a whole.
 * For example, we're already giving a higher weighting to rare fingerprint bits.
 * 
 */
final class ChemicalSimilarity extends Similarity {

	private static final long serialVersionUID = -5089450122170257888L;

	@Override
	public float lengthNorm(String arg0, int numTokens) {
		return (float)(1.0 / numTokens);
	}

	@Override
	public float queryNorm(float arg0) {
		return (float)1.0;
	}

	@Override
	public float sloppyFreq(int distance) {
		//return (float)1.0;
		return (float)1.0 / (float)(distance + 1);
	}

	@Override
	public float tf(float freq) {
		//return (float)1.0;
		return (float)Math.sqrt(freq);
	}

	@Override
	public float idf(int docFreq, int numDocs) {
		//return (float)1.0;
		return (float)Math.log((float)numDocs/((float)docFreq+1)) + 1;
	}

	@Override
	public float coord(int overlap, int maxOverlap) {
		return (float)overlap / (float)maxOverlap;
	}

}
