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

public class QTClusterer {

	SimilarityMatrix similarityMatrix;
	Set<Integer> unclustered;
	Map<Integer, Set<Integer>> clusters;
	List<Set<Integer>> finalClusters;
	
	public QTClusterer(SimilarityMatrix sm) {
		similarityMatrix = sm;
	}
	
	public void makeClusters(double threshold) {
		clusters = new HashMap<Integer,Set<Integer>>();
		finalClusters = new ArrayList<Set<Integer>>();
		Set<Integer> stopMembers = new HashSet<Integer>();
		Set<Integer> newStopMembers = new HashSet<Integer>();
		unclustered = new HashSet<Integer>();
		
		while(stopMembers.size() < similarityMatrix.getTermListSize()) {
			Set<Integer> bestCluster = new HashSet<Integer>();
			float bestDiameter = 0.0f;
			for(int i=0;i<similarityMatrix.getTermListSize();i++) {
				if(stopMembers.contains(i)) continue;
				Set<Integer> cluster = makeCluster(i, threshold, stopMembers, newStopMembers);
				if(cluster.size() > bestCluster.size()) {
					bestCluster = cluster;
					bestDiameter = getDiameter(cluster);
				} else if(cluster.size() == bestCluster.size()) {
					float diameter = getDiameter(cluster);
					if(diameter > bestDiameter) {
						bestCluster = cluster;
						bestDiameter = diameter;						
					}
				}
			}
			if(bestCluster.size() < 2) break;
			newStopMembers.clear();
			for(Integer i : bestCluster) {
				stopMembers.add(i);
				newStopMembers.add(i);
			}
			finalClusters.add(bestCluster);
			//System.out.println(bestCluster + "\t" + bestDiameter);
		}

		//System.out.println("Made clusters");
		
		for(int i=0;i<similarityMatrix.getTermListSize();i++) {
			if(!stopMembers.contains(i)) {
				unclustered.add(i);
			}
		}
	}

	public Set<Integer> makeCluster(final int center, double threshold, Set<Integer> stopMembers, Set<Integer> newStopMembers) {
		Set<Integer> oldCluster = clusters.get(center);
		boolean needToUpdate = false;
		if(oldCluster != null) {
			for(Integer i : oldCluster) {
				if(newStopMembers.contains(i)) {
					needToUpdate = true;
					break;
				}
			}
		} else {
			needToUpdate = true;
		}
		if(needToUpdate) {
			final List<Integer> potentialMembers = new ArrayList<Integer>();
			for(int i=0;i<similarityMatrix.getTermListSize();i++) {
				if(stopMembers.contains(i)) continue;
				if(similarityMatrix.getSimilarity(center,i) > threshold) potentialMembers.add(i);
			}
			Collections.sort(potentialMembers, Collections.reverseOrder(new Comparator<Integer>() {
				public int compare(Integer o1, Integer o2) {
					// TODO Auto-generated method stub
					return Double.compare(similarityMatrix.getSimilarity(center,o1), similarityMatrix.getSimilarity(center,o2));
				}
			}));
			
			Set<Integer> growingCluster = new LinkedHashSet<Integer>();
			for(Integer i : potentialMembers) {
				boolean isOK = true;
				for(Integer j : growingCluster) {
					if(similarityMatrix.getSimilarity(i,j) < threshold) {
						isOK = false;
						break;
					}
				}
				if(isOK) {
					growingCluster.add(i);
				} 
			}
			clusters.put(center, growingCluster);
			return growingCluster;
		} else {
			return clusters.get(center);
		}
	}
	
	private float getDiameter(Set<Integer> cluster) {
		float diameter = 1.0f;
		for(Integer i : cluster) {
			for(Integer j : cluster) {
				if(i > j) {
					float similarity = similarityMatrix.getSimilarity(i, j);
					if(similarity < diameter) diameter = similarity;
				}
			}
		}
		return diameter;
	}
	
	public List<Set<Integer>> getFinalClusters() {
		return finalClusters;
	}
	
	public List<Set<Integer>> getClustersBySize() {		
		final List<Set<Integer>> clustersBySize = new ArrayList<Set<Integer>>(finalClusters);
		Collections.sort(clustersBySize, Collections.reverseOrder(new Comparator<Set<Integer>>() {
			public int compare(Set<Integer> o1, Set<Integer> o2) {
				// TODO Auto-generated method stub
				if(o1.size() > o2.size()) return 1;
				if(o2.size() > o1.size()) return -1;
				return 0;
			}
		}));
		return clustersBySize;
	}
	
	public List<Set<String>> getClustersOfNames() {
		List<Set<Integer>> clustersBySize = getClustersBySize();
		List<Set<String>> clustersOfNames = new ArrayList<Set<String>>();
		for(Set<Integer> cluster : clustersBySize) {
			Set<String> clusterOfNames = new LinkedHashSet<String>();
			for(Integer i : cluster) {
				clusterOfNames.add(similarityMatrix.getNameForNumber(i));
			}
			clustersOfNames.add(clusterOfNames);
		}
		return clustersOfNames;
	}
	
	public Set<Integer> getUnclustered() {
		return unclustered;
	}

	
}
