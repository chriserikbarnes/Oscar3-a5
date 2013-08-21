package uk.ac.cam.ch.wwmm.ptc.experimental.termsimilarity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class KMeansClusterer {

	SimilarityExtractor similarityExtractor;
	Set<String> terms;
	
	public KMeansClusterer(SimilarityExtractor se) {
		similarityExtractor = se;
		terms = new LinkedHashSet<String>(se.getTerms());
	}
	
	public Set<String> getInitialCenters(double threshold) {
		Set<String> centers = new LinkedHashSet<String>();
		//termList - holds all terms that lack a center, most frequent first
		List<String> termList = similarityExtractor.getTerms();
		termList.retainAll(terms);
		while(termList.size() > 0) {
			String center = termList.get(0);
			//System.out.println(termList.size() + "\t" + center);
			boolean addTerm = false;
			for(String term : new ArrayList<String>(termList)) {
				if(term.equals(center)) {
					termList.remove(term);					
				} else if(similarityExtractor.getSimilarity(term, center) > threshold) {
					//System.out.println("\t" + term + "\t" + similarityExtractor.getSimilarity(term, center));
					termList.remove(term);
					addTerm = true;
				}
			}
			if(addTerm)	centers.add(center);
		}
		return centers;
	}
	
	public Set<Set<String>> centersToClusters(Set<String> centers) {
		Set<Set<String>> clusters = new HashSet<Set<String>>();
		for(String center : centers) {
			Set<String> cluster = new HashSet<String>();
			cluster.add(center);
			clusters.add(cluster);
		}
		return clusters;
	}
	
	public Set<Set<String>> KMeansIteration(Set<Set<String>> clusters, double threshold) {
		List<Map<String,Double>> centroids = new ArrayList<Map<String,Double>>();
		List<Set<String>> clusterList = new ArrayList<Set<String>>();
		List<Set<String>> oldClusterList = new ArrayList<Set<String>>();
		for(Set<String> cluster : clusters) {
			Map<String,Double> centroid = similarityExtractor.getVectorForTermSet(cluster);
			centroids.add(centroid);
		}
		for(int i=0;i<centroids.size();i++) {
			clusterList.add(new HashSet<String>());
		}
		for(String term : terms) {
			int bestCluster = -1;
			double bestSimilarity = threshold;
			for(int i=0;i<centroids.size();i++) {
				double similarity = similarityExtractor.getSimilarity(centroids.get(i), term);
				//if(similarity > 0.0) {
				//	System.out.println(term + " " + )
				//}
				if(similarity > bestSimilarity) {
					bestCluster = i;
					bestSimilarity = similarity;
				}
			}
			if(bestCluster >= 0) {
				clusterList.get(bestCluster).add(term);
			}
		}
		return new HashSet<Set<String>>(clusterList);
	}
	
}
