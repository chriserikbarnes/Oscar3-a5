package uk.ac.cam.ch.wwmm.ptc.experimental.termsimilarity;

import java.util.Map;

import uk.ac.cam.ch.wwmm.ptclib.datastruct.Bag;

public class MapBagFVE extends FeatureVectorExtractor {

	Map<String, Bag<String>> featureVectors;
	Bag<String> features;
	Bag<String> terms;
	
	public MapBagFVE(Map<String, Bag<String>> fv) throws Exception {
		featureVectors = fv;
		features = new Bag<String>();
		terms = new Bag<String>();
		for(String term : featureVectors.keySet()) {
			terms.add(term);
			for(String feature : featureVectors.get(term).getSet()) {
				features.add(feature, featureVectors.get(term).getCount(feature));
			}
		}
	}
	
	@Override
	public Map<String, Bag<String>> getFeatureVectors() {
		return featureVectors;
	}

	@Override
	public Bag<String> getFeatures() {
		return features;
	}

	@Override
	public Bag<String> getTerms() {
		return terms;
	}
}
