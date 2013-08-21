package uk.ac.cam.ch.wwmm.ptc.experimental.indexersearcher;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.index.TermFreqVector;

import uk.ac.cam.ch.wwmm.oscar3.indexersearcher.LuceneIndexerSearcher;
import uk.ac.cam.ch.wwmm.oscar3.terms.TermSets;
import uk.ac.cam.ch.wwmm.ptc.experimental.termsimilarity.QTClusterer;
import uk.ac.cam.ch.wwmm.ptc.experimental.termsimilarity.SimilarityMatrix;
import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;
import Jama.Matrix;

public class LSA {

	private static double cosine(Matrix m, int row1, int row2, double [] svals) {
		double r11 = 0.0;
		double r12 = 0.0;
		double r22 = 0.0;
		for(int i=0;i<Math.min(svals.length-1, m.getRowDimension()-1);i++) {
			double v1 = m.get(row1, i);// * svals[i];
			double v2 = m.get(row2, i);// * svals[i];
			//System.out.println(v1 + "\t" + v2);
			r11 += v1 * v1;
			r12 += v1 * v2;
			r22 += v2 * v2;
		}
		return r12 / (Math.sqrt(r11) * Math.sqrt(r22));
	}
	
	private static double tanimoto(Matrix m, int row1, int row2, double [] svals) {
		double r11 = 0.0;
		double r12 = 0.0;
		double r22 = 0.0;
		for(int i=0;i<Math.min(svals.length-1, m.getRowDimension()-1);i++) {
			double v1 = m.get(row1, i);// * svals[i];
			double v2 = m.get(row2, i);// * svals[i];
			//System.out.println(v1 + "\t" + v2);
			r11 += v1 * v1;
			r12 += v1 * v2;
			r22 += v2 * v2;
		}
		return r12 / (r11 + r22 - r12);
	}
	
	private static double [] getVector(Matrix m, int rowNo) {
		double [] vector = new double[m.getColumnDimension()];
		for(int i=0;i<vector.length;i++) vector[i] = m.get(rowNo, i);
		return vector;
	}
	
	private static double [] combinedVector(List<Integer> points, Matrix m) {
		double [] vector = new double[m.getColumnDimension()];
		for(int i=0;i<vector.length;i++) vector[i] = 0.0;
		for(Integer point : points) {
			for(int i=0;i<vector.length;i++) vector[i] += m.get(point, i) / points.size();
		}
		return vector;
	}

	private static double [] combinedVector(Collection<Integer> points, Matrix m, double [] svals) {
		double [] vector = new double[Math.min(m.getColumnDimension(), svals.length)];
		for(int i=0;i<vector.length;i++) vector[i] = 0.0;
		for(Integer point : points) {
			for(int i=0;i<vector.length;i++) vector[i] += m.get(point, i) / svals[i];
		}
		return vector;
	}

	private static double [] vectorToPoints(double [] vector, Matrix m, double [] svals) {
		double [] outVector = new double[m.getRowDimension()];
		for(int i=0;i<outVector.length;i++) {
			double val = 0.0;
			for(int j=0;j<vector.length;j++) val += m.get(i, j) * vector[j] * svals[j];
			outVector[i] = val;
		}
		return outVector;
	}

	
	private static double [] combinedVector(Collection<Integer> points, List<Integer> antiPoints, Matrix m) {
		int size = points.size() + antiPoints.size();
		double [] vector = new double[m.getColumnDimension()];
		for(int i=0;i<vector.length;i++) vector[i] = 0.0;
		for(Integer point : points) {
			for(int i=0;i<vector.length;i++) vector[i] += m.get(point, i) / size;
		}
		for(Integer antiPoint : antiPoints) {
			for(int i=0;i<vector.length;i++) vector[i] -= m.get(antiPoint, i) / size;
		}
		return vector;
	}
	
	
	private static void describeVector(double [] vector, Matrix lm, double [] svals, List<String> dfl) {
		double termScore = 0.0;
		for(int i=0;i<svals.length;i++) {
			termScore += Math.pow(vector[i], 2);
		}
		Map<String,Double> cosines = new HashMap<String,Double>();
		Map<String,Double> products = new HashMap<String,Double>();
		for(int i=0;i<dfl.size();i++) {
			double otherTermScore = 0.0;
			double product = 0.0;
			for(int j=0;j<svals.length;j++) {
				double tVal = vector[j];
				double otVal = lm.get(i,j);
				otherTermScore += otVal * otVal;
				product += tVal * otVal;
			}
			double cosine = product / (Math.sqrt(termScore) * Math.sqrt(otherTermScore));
			products.put(dfl.get(i), product);
			cosines.put(dfl.get(i), cosine);
		}
		for(String s : StringTools.getSortedList(cosines).subList(0,20)) {
			System.out.println("\t" + s + "\t" + cosines.get(s));
		}
		/*for(String s : StringTools.getSortedList(products).subList(0,20)) {
			System.out.println("\t" + s + "\t" + products.get(s));
		}*/		
	}
	
	private static void describeTerm(int termNo, Matrix lm, double [] svals, List<String> dfl) {
		double [] vector = getVector(lm, termNo);
		describeVector(vector, lm, svals, dfl);
	}
	
	private static void addToList(List<Integer> points, String term, Map<String,Integer> termIndex) {
		if(termIndex.containsKey(term)) points.add(termIndex.get(term));
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		LuceneIndexerSearcher lis = new LuceneIndexerSearcher(false);
		IndexReader ir = lis.getIndexReader();
		
		long allTime = System.currentTimeMillis();
		
		int numDocs = ir.numDocs();
		TermEnum textEnum = ir.terms();
		Map<String,Integer> docFreqs = new HashMap<String,Integer>();
		while(textEnum.next()) {
			Term t = textEnum.term();
			if("txt".equals(t.field())) {
				String text = t.text();
				if(TermSets.getClosedClass().contains(text)) continue;
				if(!text.matches(".*[A-Za-z].*")) continue;
				int docFreq = ir.docFreq(t);
				if(docFreq > 1) {
					docFreqs.put(text, ir.docFreq(t));
				}
			}			
		}
		List<String> dfl = StringTools.getSortedList(docFreqs);
		
		Map<String,Integer> termIndex = new HashMap<String,Integer>();
		for(int i=0;i<dfl.size();i++) {
			termIndex.put(dfl.get(i), i);
		}
		
		System.out.println("Documents: " + numDocs);
		System.out.println("Terms: " + dfl.size());
		
		SVDHarness svdh = new SVDHarness(dfl.size(), numDocs);
		
		for(int i=0;i<ir.numDocs();i++) {
			TermFreqVector tv = ir.getTermFreqVector(i, "txt");
			String [] terms = tv.getTerms();
			int [] counts = tv.getTermFrequencies();
			double termTotal = 0.0;
			for(int j=0;j<tv.size();j++) {
				termTotal += counts[j];
			}
			for(int j=0;j<tv.size();j++) {
				if(termIndex.containsKey(terms[j])) {
					svdh.set(i, termIndex.get(terms[j]), counts[j] * (1.0 / termTotal) * Math.log(numDocs * 1.0 / docFreqs.get(terms[j])));
				}
			}
		}
		System.out.println("Doing SVD...");
		long time = System.currentTimeMillis();

		svdh.svd(300);
		System.out.println("SVD Done in: " + (System.currentTimeMillis() - time));
		double [] svals = svdh.getS();
		
		System.out.println(svdh.getUt().getColumnDimension());
		System.out.println(svdh.getUt().getRowDimension());
		System.out.println(svdh.getVt().getColumnDimension());
		System.out.println(svdh.getVt().getRowDimension());
		
		time = System.currentTimeMillis();
		
		System.out.println("All Done in: " + (System.currentTimeMillis() - allTime));
		
		List<String> docFiles = new ArrayList<String>();
		for(int i=0;i<ir.numDocs();i++) {
			docFiles.add(ir.document(i).getField("filename").stringValue().replaceAll("markedup", "source"));
		}
		
		time = System.currentTimeMillis();
		if(true) {		
			Set<Integer> assigned = new HashSet<Integer>();
			Matrix docMatrix = svdh.getVt().transpose();
			SimilarityMatrix simMatrix = new SimilarityMatrix(docFiles); 
			for(int i=0;i<docFiles.size();i++) {
				for(int j=i+1;j<docFiles.size();j++) {
					simMatrix.setSimilarity(i, j, (float)cosine(docMatrix, i, j, svals));
					//simMatrix.setSimilarity(i, j, (float)tanimoto(docMatrix, i, j, svals));
				}
			}
			//System.out.println(Math.min(svals.length, docMatrix.getRowDimension()));
			//VirtualSimilarityMatrix simMatrix = new VirtualSimilarityMatrix(docMatrix, docFiles, Math.min(svals.length, docMatrix.getRowDimension()-1));
			System.out.println("Similarity matrix: " + (System.currentTimeMillis() - time));
			time = System.currentTimeMillis();			
			QTClusterer qt = new QTClusterer(simMatrix);
			qt.makeClusters(0.05);
			System.out.println("Made clusters in: " + (System.currentTimeMillis() - time));
			for(Set<Integer> cluster : qt.getClustersBySize()) {
				/*double[] fuzzyVector = vectorToPoints(combinedVector(cluster, docMatrix, svals), docMatrix, svals);
				double fvt = 0.0;
				Map<Integer,Float> fc = new HashMap<Integer,Float>();
				for(int i=0;i<fuzzyVector.length;i++) {
					fvt += fuzzyVector[i];
					//if(cluster.contains(i)) System.out.println(docFiles.get(i) + "\t" + fuzzyVector[i]);
					//if(fuzzyVector[i] > 0.1 && !cluster.contains(i)) System.out.println(docFiles.get(i) + "\t" + fuzzyVector[i]);
					//if(fuzzyVector[i] > 0.05 && !cluster.contains(i)) {
					//	cluster.add(i);
					//	assigned.add(i);
					//}
					if(fuzzyVector[i] > 0.25) fc.put(i, (float)fuzzyVector[i]);
				}*/
				//System.out.println(fvt);
				for(Integer i : cluster) {
					System.out.println(docFiles.get(i));
				}
				Map<Integer,Float> clusterMap = new HashMap<Integer,Float>();
				for(Integer i : cluster) {
					clusterMap.put(i, 1.0f);
				}

				//ClusterAnalyser.analyseCluster(fc, ir);
				//ClusterAnalyser.analyseCluster(new ArrayList<Integer>(cluster), ir, new TanimotoSimilarity(), 0.05);
				ClusterAnalyser.excessAnalyseCluster(clusterMap, ir, 0.2, true);
				System.out.println();
			}
			
			for(Integer i : qt.getUnclustered()) {
				if(!assigned.contains(i)) System.out.println(docFiles.get(i));
			}
			return;
		}
		
		Matrix lm = svdh.getUt().transpose();
		Matrix sm = svdh.getVt().transpose();

		if(true) {
			Map<Integer,List<Integer>> clusters = new HashMap<Integer,List<Integer>>();
			for(int i=0;i<docFiles.size();i++) {
				double maxScore = 0.0;
				int cnum = 0;
				for(int j=0;j<svals.length;j++) {
					double score = sm.get(i, j) * svals[j];
					if(score > maxScore) {
						cnum = j;
						maxScore = score;
					} else if(-score > maxScore) {
						cnum = -j;
						maxScore = -score;
					}
				}
				if(!clusters.containsKey(cnum)) clusters.put(cnum, new ArrayList<Integer>());
				clusters.get(cnum).add(i);
			}
			for(int i=0;i<svals.length;i++) {
				if(clusters.containsKey(i)) {
					System.out.println(i);
					for(Integer j : clusters.get(i)) {
						System.out.println(docFiles.get(j));
					}
					ClusterAnalyser.analyseCluster(clusters.get(i), ir, new TanimotoSimilarity(), 0.05);
					System.out.println();
				}
				if(i > 0 && clusters.containsKey(-i)) {
					System.out.println(-i);
					for(Integer j : clusters.get(-i)) {
						System.out.println(docFiles.get(j));
					}
					ClusterAnalyser.analyseCluster(clusters.get(-i), ir, new TanimotoSimilarity(), 0.05);
					System.out.println();
				}
			}
		}
		
		if(false) {
			File outFile = new File("lsa.csv");
			PrintWriter out = new PrintWriter(new FileWriter(outFile));
			Map<String,Double> distances = new HashMap<String,Double>();
			
			for(int i=0;i<dfl.size();i++) {
				out.print(dfl.get(i));
				for(int j=0;j<svals.length;j++) {
					out.print("\t" + lm.get(i, j));
				}
				out.println();
				double squareLength = 0.0;
				for(int j=0;j<lm.getColumnDimension();j++) {
					squareLength += Math.pow(lm.get(i, j), 2);
				}
				double distance = Math.sqrt(squareLength);
				String term = dfl.get(i);
				int docFreq = ir.docFreq(new Term("txt", term));
				double score = Math.log(docFreq) * distance;
				if(score > 1.0) distances.put(dfl.get(i), score);
			}
			out.close();
			
			for(String topicWord : StringTools.getSortedList(distances)) {
				System.out.println(topicWord + "\t" + distances.get(topicWord));
				int termNo = termIndex.get(topicWord);
				describeTerm(termNo, lm, svals, dfl);
			}			
		}
		

		if(false) {
			for(int i=0;i<svals.length;i++) {
				//System.out.println(svals[i]);
				for(int j=0;j<sm.getRowDimension();j++) {
					sm.set(j, i, sm.get(j, i) * svals[i]);
				}
			}			
		}

		if(false) {
			for(int i=0;i<sm.getRowDimension();i++) {
				double [] v = getVector(sm, i);
				System.out.println(ir.document(i).getField("filename").stringValue().replaceAll("markedup", "source"));
				describeVector(v, lm, svals, dfl);
			}
		}
		
		
		if(false) {
			List<Map<Integer,Float>> clusters = KMeans.cmc(sm, 100, 1.01, 0.1, 0.5);
			for(Map<Integer,Float> cluster : clusters) {
				Map<String,Float> c = new HashMap<String,Float>();
				for(Integer i : cluster.keySet()) {
					c.put(ir.document(i).getField("filename").stringValue().replaceAll("markedup", "source"), cluster.get(i));
				}
				for(String s : StringTools.getSortedList(c)) {
					System.out.println(s);
					System.out.println(c.get(s));
				}
				ClusterAnalyser.analyseCluster(cluster, ir, new TanimotoSimilarity(), 0.05);
				System.out.println();
			}
			
		}
	}

}
