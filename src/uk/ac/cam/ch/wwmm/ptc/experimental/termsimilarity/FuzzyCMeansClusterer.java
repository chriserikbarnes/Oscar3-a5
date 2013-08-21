package uk.ac.cam.ch.wwmm.ptc.experimental.termsimilarity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;

public class FuzzyCMeansClusterer {

	SimilarityExtractor similarityExtractor;
	Set<String> terms;
	Set<String> alreadyUsedAsSeeds;
	
	public List<Map<String,Double>> doIterations(int iter, double thresh) {
		List<Map<String,Double>> clusters = new ArrayList<Map<String,Double>>();
		clusters = addNewCenters(clusters, thresh);
		for(int i=0;i<iter;i++) {
			clusters = CMeansIteration(clusters, thresh);
			clusters = mergeClusters(clusters, 0.8);
			clusters = filterClusters(clusters, 1.0);
		}
		return clusters;
	}
	
	public Set<Set<String>> cluster(int iter, double thresh, double memberthresh) {
		List<Map<String,Double>> rawClusters = doIterations(iter, thresh);
		Set<Set<String>> clusters = new HashSet<Set<String>>();
		for(Map<String,Double> cluster : rawClusters) {
			Set<String> newCluster = new HashSet<String>();
			for(String item : cluster.keySet()) {
				if(cluster.get(item) > memberthresh) newCluster.add(item);
			}
			if(newCluster.size() > 1) clusters.add(newCluster);
		}
		return clusters;
	}

	
	public FuzzyCMeansClusterer(SimilarityExtractor se) {
		similarityExtractor = se;
		terms = new LinkedHashSet<String>(se.getTerms());
		alreadyUsedAsSeeds = new HashSet<String>();
	}
	
	public Set<String> getInitialCenters(double threshold) {
		return getInitialCenters(threshold, new HashSet<String>());
	}

	public Set<String> getInitialCenters(double threshold, Set<String> excludeTerms) {
		Set<String> centers = new LinkedHashSet<String>();
		//termList - holds all terms that lack a center, most frequent first
		List<String> termList = similarityExtractor.getTerms();
		termList.removeAll(excludeTerms);
		termList.retainAll(terms);
		boolean retryIfEmpty = false;
		if(alreadyUsedAsSeeds.size() > 0) {
			termList.removeAll(alreadyUsedAsSeeds);
			retryIfEmpty = true;
		}
		while(termList.size() > 0) {
			String center = termList.get(0);
			//System.out.println(termList.size() + "\t" + center);
			boolean addTerm = false;
			//boolean addTerm = true; // Always add a term, even if it picks nothing up
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
		if(centers.size() > 0) {
			alreadyUsedAsSeeds.addAll(centers);
		} else if(centers.size() == 0 && retryIfEmpty) {
			System.out.println("Recycling seeds");
			alreadyUsedAsSeeds.clear();
			return getInitialCenters(threshold, excludeTerms);
		}
		return centers;
	}
	
	public List<Map<String,Double>> centersToFuzzyClusters(Set<String> centers) {
		List<Map<String,Double>> clusters = new ArrayList<Map<String,Double>>();
		for(String center : centers) {
			Map<String,Double> cluster = new HashMap<String,Double>();
			cluster.put(center, 1.0);
			clusters.add(cluster);
		}
		return clusters;
	}
	
	public List<Map<String,Double>> CMeansIteration(List<Map<String,Double>> clusters, double threshold) {
		List<Map<String,Double>> centroids = new ArrayList<Map<String,Double>>();
		List<Map<String,Double>> clusterList = new ArrayList<Map<String,Double>>();
		//Map<String,Map<String,Double>> similaritiesForTerm = new HashMap<String,Map<String,Double>>();
		for(Map<String,Double> cluster : clusters) {
			Map<String,Double> centroid = similarityExtractor.getVectorForFuzzyTermSet(cluster);
			centroids.add(centroid);
			clusterList.add(new HashMap<String,Double>());
		}
		//for(int i=0;i<centroids.size();i++) {
		//	clusterList.add(new HashSet<String>());
		//}
		for(String term : terms) {
			Map<Integer,Double> similarities = new HashMap<Integer,Double>();
			// Assume a similarity to the "null cluster"
			double totalSimilarity = threshold * 3;
			for(int i=0;i<centroids.size();i++) {
				double similarity = similarityExtractor.getSimilarity(centroids.get(i), term);
				if(similarity > threshold) {
					similarities.put(i,similarity);
					totalSimilarity += similarity;
				}
			}
			// Now transform similarities to memberships
			for(Integer i : similarities.keySet()) {
				double membership = similarities.get(i) / totalSimilarity;
				clusterList.get(i).put(term, membership);
			}
		}
		return clusterList;
	}
	
	public List<Map<String,Double>> filterClusters(List<Map<String,Double>> clusters, double threshold) {
		List<Map<String,Double>> newClusters = new ArrayList<Map<String,Double>>();
		for(Map<String,Double> cluster : clusters) {
			double totalMembership = 0.0;
			for(String term : cluster.keySet()) totalMembership += cluster.get(term);
			if(totalMembership > threshold) newClusters.add(cluster);
		}
		return newClusters;
	}

	public List<Map<String,Double>> mergeClusters(List<Map<String,Double>> clusters, double threshold) {
		List<Map<String,Double>> clusterList = new ArrayList<Map<String,Double>>(clusters);
		
		final Map<Integer,Double> memberships = new HashMap<Integer,Double>();
		for(int i=0;i<clusterList.size();i++) {
			double membership = 0.0;
			for(String term : clusterList.get(i).keySet()) membership += clusterList.get(i).get(term);
			memberships.put(i, membership);
		}
		List<Integer> sortedClusterIds = new ArrayList<Integer>();
		for(int i=0;i<clusterList.size();i++) {
			sortedClusterIds.add(i);
		}
		Collections.sort(sortedClusterIds, Collections.reverseOrder(new Comparator<Integer>() {
			public int compare(Integer o1, Integer o2) {
				// TODO Auto-generated method stub
				return Double.compare(memberships.get(o1), memberships.get(o2));
			}
		}));
		List<Map<String,Double>> newClusterList = new ArrayList<Map<String,Double>>();
		for(Integer i : sortedClusterIds) {
			newClusterList.add(clusterList.get(i));
		}
		clusterList = newClusterList;
		
		List<Map<String,Double>> newClusters = new ArrayList<Map<String,Double>>();
		for(int i=0;i<clusterList.size();i++) {
			for(int j=i+1;j<clusterList.size();j++) {
				double recall = recall(clusterList.get(j), clusterList.get(i));
				if(recall > threshold) {

					Map<String,Double> mergeInto = clusterList.get(i);
					Map<String,Double> mergeFrom = clusterList.get(j);

					/*System.out.println("Merging...");
					
					List<String> memberList = StringTools.getSortedList(mergeInto);
					for(String s : memberList) {
						System.out.printf("%s %.1f%%\n", s, mergeInto.get(s) * 100.0);
					}
					System.out.println();

					memberList = StringTools.getSortedList(mergeFrom);
					for(String s : memberList) {
						System.out.printf("%s %.1f%%\n", s, mergeFrom.get(s) * 100.0);
					}
					System.out.println();
					*/
					for(String term : mergeFrom.keySet()) {
						if(!mergeInto.containsKey(term)) mergeInto.put(term, 0.0);
						mergeInto.put(term, mergeInto.get(term) + mergeFrom.get(term));
					}
					clusterList.remove(j);
					j--;
				}
			}
			newClusters.add(clusterList.get(i));
		}
		
		return newClusters;
	}
	
	public boolean checkConvergence(List<Map<String,Double>> oldClusters, List<Map<String,Double>> newClusters) {
		if(oldClusters.size() != newClusters.size()) {
			System.out.println("Different number of clusters!");
			return false;
		}
		for(int i=0;i<oldClusters.size();i++) {
			if(oldClusters.get(i).size() != newClusters.get(i).size()) {
				System.out.println("Cluster " + i + " has changed size");
				return false;
			} else {
				Set<String> ts1 = oldClusters.get(i).keySet();
				Set<String> ts2 = newClusters.get(i).keySet();
				if(!ts1.equals(ts2)) {
					System.out.println("Cluster " + i + " has changed membership");
					return false;
				}
			}
		}
		return true;
	}
	
	public Set<String> getClusteredTerms(List<Map<String,Double>> oldClusters) {
		Set<String> clusteredTerms = new HashSet<String>();
		for(Map<String,Double> cluster : oldClusters) {
			clusteredTerms.addAll(cluster.keySet());
		}
		return clusteredTerms;
	}
	
	public List<Map<String,Double>> addNewCenters(List<Map<String,Double>> oldClusters, double threshold) {
		Set<String> centers = getInitialCenters(threshold, getClusteredTerms(oldClusters));
		List<Map<String,Double>> newClusters = new ArrayList<Map<String,Double>>(oldClusters);
		newClusters.addAll(centersToFuzzyClusters(centers));
		return newClusters;
	}
	
	private double recall(Map<String,Double> set1, Map<String,Double> set2) {
		double jointMembership = 0.0;
		double set1Membership = 0.0;
		for(String term : set1.keySet()) {
			set1Membership += set1.get(term);
			if(set2.containsKey(term)) jointMembership += set1.get(term);
		}
		return jointMembership / set1Membership;
	}
	
}
