package uk.ac.cam.ch.wwmm.ptc.experimental.indexersearcher;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import Jama.Matrix;

public class KMeans {

	public static void normalizeClusters(Matrix clusters) {
		int clusterNo = clusters.getRowDimension();
		int points = clusters.getColumnDimension();
		int nullClusters = 0;
		for(int i=0;i<clusterNo;i++) {
			double tot = 0.0;
			for(int j=0;j<points;j++) tot += clusters.get(i, j);
			for(int j=0;j<points;j++) clusters.set(i, j, clusters.get(i, j) / tot);
			if(tot == 0.0) nullClusters++;
		}
		System.out.println(nullClusters + " null clusters");
	}

	public static Matrix normalizedClusters(Matrix clusters, boolean removeLowMax, boolean kmeans, double minMax) {
		int clusterNo = clusters.getRowDimension();
		int points = clusters.getColumnDimension();
		int nullClusters = 0;
		double [] totals = new double[clusterNo];
		for(int i=0;i<clusterNo;i++) {
			double tot = 0.0;
			double max = 0.0;
			for(int j=0;j<points;j++) {
				double val = clusters.get(i, j);
				tot += val;
				max = Math.max(max, val);
			}
			//System.out.println(tot + "\t" + max);
			totals[i] = tot; 
			if((kmeans && tot <= 1.0) || (!kmeans && tot <= 3.0) || (!kmeans && removeLowMax && max < minMax)) {
				totals[i] = 0.0;
				nullClusters++;
			}
		}
		int newClusterNo = clusterNo - nullClusters;
		Matrix newClusters = new Matrix(newClusterNo, points, 0.0);
		int newCluster = 0;
		for(int i=0;i<clusterNo;i++) {
			if(totals[i] > 1.0) {
				double tot = totals[i];
				for(int j=0;j<points;j++) newClusters.set(newCluster, j, clusters.get(i, j) / tot);
				newCluster++;
			}
		}
		System.out.println(nullClusters + " null clusters, " + newClusterNo + " good clusters");
		return newClusters;
	}

	public static List<List<Integer>> kmc(Matrix m, int clusterNo) throws Exception {
		Matrix clusters = korcmeans(m, clusterNo, true, 1.0, 1.0);
		List<List<Integer>> foundClusters = new ArrayList<List<Integer>>();
		for(int i=0;i<clusterNo;i++) {
			foundClusters.add(new ArrayList<Integer>());
		}
		for(int i=0;i<clusters.getRowDimension();i++) {
			for(int j=0;j<clusters.getColumnDimension();j++) {
				if(clusters.get(i, j) > 0) {
					foundClusters.get(i).add(j);
				} 
			}
		}
		return foundClusters;
	}
	
	public static List<Map<Integer,Float>> cmc(Matrix m, int clusterNo, double fuzzy, double cutoff, double minMax) throws Exception {
		Matrix clusters = korcmeans(m, clusterNo, false, fuzzy, minMax);
		List<Map<Integer,Float>> foundClusters = new ArrayList<Map<Integer,Float>>();
		for(int i=0;i<clusters.getRowDimension();i++) {
			Map<Integer,Float> cluster = new HashMap<Integer,Float>();
			for(int j=0;j<clusters.getColumnDimension();j++) {
				if(clusters.get(i, j) > cutoff) {
					cluster.put(j, (float)clusters.get(i, j));
				} 
			}
			foundClusters.add(cluster);
		}
		return foundClusters;		
	}
	
	private static Matrix korcmeans(Matrix m, int clusterNo, boolean kmeans, double fuzzy, double minMax) throws Exception {
		System.out.println("Trying " + clusterNo + " clusters");
		int dimensions = m.getColumnDimension();
		int points = m.getRowDimension();
		
		Matrix clusters = new Matrix(clusterNo, points, 0.0);
		PrintWriter pw = new PrintWriter(System.out);
		//if(true) return;
		
		Random rand = new Random(3);
		for(int i=0;i<clusters.getRowDimension();i++) {
			for(int j=0;j<clusters.getColumnDimension();j++) {
				clusters.set(i, j, Math.pow(rand.nextDouble(), 3));
			}
		}
		//for(int i=0;i<points;i++) {
		//	clusters.set(rand.nextInt(clusterNo), i, 1.0);
		//}

		//clusters.print(pw, 6, 4);
		pw.flush();
		
		boolean converged = false;
		boolean firstRound = true;
		while(!converged) {
			Matrix oldClusters = clusters;
			boolean lostClusters = false;
			clusters = normalizedClusters(clusters, !firstRound, kmeans, minMax);
			if(clusterNo != clusters.getRowDimension()) {
				clusterNo = clusters.getRowDimension();
				lostClusters = true;
			}
			Matrix centroids = clusters.times(m);
			Matrix newClusters = new Matrix(clusterNo, points, 0.0);
			for(int i=0;i<points;i++) {
				// KMeans - always do on first round to get some clusters going
				if(kmeans || firstRound) {
					int bestCluster = -1;
					double shortestDistance = Double.POSITIVE_INFINITY;
					for(int j=0;j<clusterNo;j++) {
						// Euclidian distance
						double distSquared = 0.0;
						for(int k=0;k<dimensions;k++) {
							distSquared += Math.pow(centroids.get(j, k) - m.get(i, k), 2.0);
						}
						double dist = Math.sqrt(distSquared);
						if(dist < shortestDistance) {
							shortestDistance = dist;
							bestCluster = j;
						}
					}
					newClusters.set(bestCluster, i, 1.0);					
				} else {
					// Fuzzy c-means
					double [] dists = new double[clusterNo];
					
					for(int j=0;j<clusterNo;j++) {
						// Euclidian distance
						double distSquared = 0.0;
						for(int k=0;k<dimensions;k++) {
							distSquared += Math.pow(centroids.get(j, k) - m.get(i, k), 2.0);
						}
						double dist = Math.sqrt(distSquared);
						dists[j] = dist;
					}
					double titid = 0.0;
					for(int j=0;j<clusterNo;j++) {
						double tid = 0.0;
						for(int k=0;k<clusterNo;k++) {
							tid += Math.pow(dists[j] / dists[k], 2.0 / (fuzzy - 1.0));
						}
						newClusters.set(j, i, 1 / tid);
						titid += 1 / tid;
					}
				}
			}
			pw.flush();
			boolean different = false;
			if(firstRound) {
				firstRound = false;
				different = true;
			} else if(lostClusters) {
				different = true;
			} else {
				double totsqrdiff = 0.0;
				
				for(int i=0;i<clusters.getRowDimension();i++) {
					for(int j=0;j<clusters.getColumnDimension();j++) {
						totsqrdiff += Math.pow(oldClusters.get(i,j) - newClusters.get(i, j), 2);
					}
				}
				double rmsd = Math.sqrt(totsqrdiff / clusters.getRowDimension() / clusters.getColumnDimension());
				System.out.println("RMSD: " + rmsd);
				if((kmeans && rmsd > 0.0) || (!kmeans && rmsd > 1.0E-3)) different = true;
			}
			clusters = newClusters;
			if(!different) {
				converged = true;
			}
		}	
		return clusters;		
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
