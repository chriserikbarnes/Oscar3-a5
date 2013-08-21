package uk.ac.cam.ch.wwmm.ptc.experimental.termsimilarity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.index.TermFreqVector;

import uk.ac.cam.ch.wwmm.ptclib.datastruct.Bag;
import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;

public class SimilarityExtractor {

	private FeatureVectorExtractor fve;
	private Similarity similarity;
	private Weighting weighting;
	
	private int featureTotal;
	private int termTotal;
	
	private Map<String,Bag<String>> rawFeatureVectors;
	private Bag<String> features;
	private Bag<String> terms;
	private Map<String,Map<String,Double>> weightVectors;
	
	public SimilarityExtractor(FeatureVectorExtractor fve, Similarity similarity, Weighting weighting) {
		this.fve = fve;
		this.weighting = weighting;
		this.similarity = similarity;
		initialise();
	}
	
	private void initialise() {
		rawFeatureVectors = fve.getFeatureVectors();
		features = fve.getFeatures();
		terms = fve.getTerms();

		featureTotal = 0;
		for(String f : features.getSet()) {
			featureTotal += features.getCount(f);
		}
		
		termTotal = terms.totalCount();
				
		weightVectors = new HashMap<String,Map<String,Double>>();
		for(String t : rawFeatureVectors.keySet()) {
			weightVectors.put(t, makeWeightVector(rawFeatureVectors.get(t), terms.getCount(t)));
		}
	}
	
	private Map<String,Double> makeWeightVector(Bag<String> rawFeatureVector, int termCount) {
		Map<String,Double> weightVector = new HashMap<String,Double>();
		for(String f : rawFeatureVector.getSet()) {
			double weight = weighting.weight(features.getCount(f), featureTotal, 
					termCount, termTotal, rawFeatureVector.getCount(f));
			if(weight != 0.0) weightVector.put(f, weight);
		}
		return weightVector;
	}
	
	public double getSimilarity(String term1, String term2) {
		return similarity.similarity(weightVectors.get(term1), weightVectors.get(term2));
	}

	public double getSimilarity(Map<String,Double> vector, String term) {
		return similarity.similarity(vector, weightVectors.get(term));
	}
	
	public Map<String,Double> getSimilarToVector(Map<String,Double> vector) {
		Map<String,Double> similar = new HashMap<String,Double>();
		for(String t : weightVectors.keySet()) {
			double s = getSimilarity(vector, t);
			if(s > 0.0) similar.put(t, s);
		}
		return similar;
	}

	public Map<String,Double> getSimilarToTerm(String term) {
		return getSimilarToVector(weightVectors.get(term));
	}
	
	public Set<String> selectFeaturesForTermSet(Set<String> termSet) {
		//Map<String,Bag<String>> featureBags = new HashMap<String,Bag<String>>
		Map<String,Integer> featureCounts = new HashMap<String,Integer>();
		for(String term : termSet) {
			Bag<String> rawFV = rawFeatureVectors.get(term);
			for(String feature : rawFV.getSet()) {
				if(!featureCounts.containsKey(feature)) featureCounts.put(feature, 0);
				featureCounts.put(feature, featureCounts.get(feature) + 1);
			}
		}
		Set<String> selectedFeatures = new HashSet<String>();
		int threshold = 1;
		if(termSet.size() > 2) threshold = 2;
		for(String feature : featureCounts.keySet()) {
			if(featureCounts.get(feature) >= threshold) selectedFeatures.add(feature);
		}
		return selectedFeatures;
	}

	public Set<String> selectFeaturesForTermSetByEntropy(Set<String> termSet) {
		Map<String,Bag<String>> featureBags = new HashMap<String,Bag<String>>();
		for(String term : termSet) {
			Bag<String> rawFV = rawFeatureVectors.get(term);
			for(String feature : rawFV.getSet()) {
				if(!featureBags.containsKey(feature)) featureBags.put(feature, new Bag<String>());
				featureBags.get(feature).add(term, rawFV.getCount(feature));
			}
		}
		Map<String,Double> entropies = new HashMap<String,Double>();
		for(String feature : featureBags.keySet()) {
			entropies.put(feature, featureBags.get(feature).entropy());
		}
		List<String> sortedFeatures = StringTools.getSortedList(entropies);
		if(entropies.size() > 20) {
			return new HashSet<String>(sortedFeatures.subList(0, 20));
		} else {
			return new HashSet<String>(sortedFeatures);
		}
	}
	
	public Map<String,Double> getVectorForTermSet(Set<String> termSet) {
		if(termSet.size() == 1) {
			return weightVectors.get(termSet.toArray(new String[0])[0]);
		}
		Bag<String> composite = new Bag<String>();
		Set<String> selectedFeatures = selectFeaturesForTermSetByEntropy(termSet);
		int compositeTotal = 0;
		for(String s : termSet) {
			if(terms.getCount(s) > 0) {
				composite.addAll(rawFeatureVectors.get(s));
				compositeTotal += terms.getCount(s);				
			}
		}
		composite.discardNotInSet(selectedFeatures);
		return makeWeightVector(composite, compositeTotal);
	}
	
	public Map<String,Double> getVectorForFuzzyTermSet(Map<String,Double> fuzzyTermSet) {
		Map<String,Bag<String>> featureBags = new HashMap<String,Bag<String>>();
		Map<String,Double> composite = new HashMap<String,Double>();
		double compositeTotal = 0;
		for(String term : fuzzyTermSet.keySet()) {
			if(terms.getCount(term) > 0) {
				Bag<String> rawFV = rawFeatureVectors.get(term);
				for(String feature : rawFV.getSet()) {
					if(!composite.containsKey(feature)) composite.put(feature, 0.0);
					if(!featureBags.containsKey(feature)) featureBags.put(feature, new Bag<String>());
					double featureCount = rawFV.getCount(feature) / fuzzyTermSet.get(term);
					composite.put(feature, composite.get(feature) + featureCount);
					featureBags.get(feature).add(term, (int)featureCount);
				}
				compositeTotal += terms.getCount(term) * fuzzyTermSet.get(term);				
			}
		}
		Map<String,Double> entropies = new HashMap<String,Double>();
		for(String feature : featureBags.keySet()) {
			entropies.put(feature, featureBags.get(feature).entropy());
			//entropies.put(feature, Math.pow(2, featureBags.get(feature).entropy()) + (Math.log10(featureBags.get(feature).totalCount()) / 100000.0));
		}
		List<String> sortedFeatures = StringTools.getSortedList(entropies);
		Set<String> includeFeatures = null;
		if(entropies.size() > 50) {
			includeFeatures = new HashSet<String>(sortedFeatures.subList(0, 20));
		} else {
			includeFeatures = new HashSet<String>(sortedFeatures);
		}

		Bag<String> compositeBag = new Bag<String>();
		for(String feature : composite.keySet()) {
			compositeBag.add(feature, composite.get(feature).intValue());
		}
		compositeBag.discardNotInSet(includeFeatures);
		return makeWeightVector(compositeBag, (int)compositeTotal);
	}
	
	public Map<String,Double> getSimilarToTermSet(Set<String> termSet) {
		return getSimilarToVector(getVectorForTermSet(termSet));
	}

	public Map<String,Double> getSimilarToTermSetNearest(Set<String> termSet) {
		Map<String,Double> results = new HashMap<String,Double>();
		for(String term : termSet) {
			Map<String,Double> similar = getSimilarToTerm(term);
			for(String similarTerm : similar.keySet()) {
				if(results.containsKey(similarTerm)) {
					if(results.get(similarTerm) < similar.get(similarTerm)) results.put(similarTerm,similar.get(similarTerm));
				} else {
					results.put(similarTerm,similar.get(similarTerm));
				}
			}
		}
		return results;
	}
	
	public List<String> getTerms() {
		return terms.getList();
	}
	
	public List<String> getFeatures() {
		return features.getList();
	}
		
	public List<String> getMostFrequentTerms(int termCount) {
		List<String> termList = terms.getList();
		if(termList.size() > termCount) termList = termList.subList(0, termCount);
		return termList;
	}
	
	public Set<String> complementOfSet(Set<String> termSet) {
		Set<String> results = new HashSet<String>();
		results.addAll(terms.getSet());
		results.removeAll(termSet);
		return results;
	}
	
	public double averageFrequency(Set<String> termSet) {
		double totalFreq = 0.0;
		int termCount = 0;
		for(String term : termSet) {
			totalFreq += terms.getCount(term);
			termCount++;
		}
		return totalFreq / termCount;
	}
	
	public void explainWeightVector(Map<String,Double> vector) {
		List<String> foo = StringTools.getSortedList(vector);
		if(foo.size() > 10) foo = foo.subList(0, 10);
		for(String f : foo) {
			System.out.println(f + "\t" + vector.get(f));
		}
	}
	
	public int getFrequency(String term) {
		return terms.getCount(term);
	}
	
	public Map<String,Double> getWeightVector(String term) {
		return weightVectors.get(term);
	}
}
