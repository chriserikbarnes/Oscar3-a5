package uk.ac.cam.ch.wwmm.ptc.experimental.indexersearcher;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

import uk.ac.cam.ch.wwmm.oscar3.indexersearcher.LuceneIndexerSearcher;
import uk.ac.cam.ch.wwmm.oscar3.indexersearcher.VectorCollector;
import uk.ac.cam.ch.wwmm.ptc.experimental.ngramtfdf.NGramTfDf;
import uk.ac.cam.ch.wwmm.ptc.experimental.termsimilarity.QTClusterer;
import uk.ac.cam.ch.wwmm.ptc.experimental.termsimilarity.SimilarityMatrix;
import uk.ac.cam.ch.wwmm.ptclib.datastruct.Bag;
import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;
import Jama.Matrix;

public class QT {

	private static Map<Map<String,Double>,Double> cache = new HashMap<Map<String,Double>,Double>();

	private static double getSqrtMagnitude(Map<String,Double> v) {
		if(cache.containsKey(v)) return cache.get(v);
		double weightSumSquare = 0.0;
		for(String s : v.keySet()) {
			weightSumSquare += v.get(s) * v.get(s);
		}
		double result = Math.sqrt(weightSumSquare);
		cache.put(v, result);
		return result;
	}

	public static Map<String,Double> combine(Map<String,Double> v1, Map<String,Double> v2) {
		Map<String,Double> combined = new HashMap<String,Double>();
		for(String s : v1.keySet()) {
			if(v2.containsKey(s)) {
				combined.put(s, v1.get(s) * v2.get(s));
			}
		}
		return combined;
	}

	
	public static double cosine(Map<String,Double> v1, Map<String,Double> v2) {
		/*double weightSumSquare1 = 0.0;
		for(int f : v1.keySet()) {
			weightSumSquare1 += v1.get(f) * v1.get(f);
		}
		double weightSumSquare2 = 0.0;
		for(int f : v2.keySet()) {
			weightSumSquare2 += v2.get(f) * v2.get(f);
		}*/
		double coWeightSum = 0.0;
		for(String s : v1.keySet()) {
				if(v2.containsKey(s)) {
					coWeightSum += (v1.get(s) * v2.get(s));				
				}
		}
		return coWeightSum / (getSqrtMagnitude(v1) * getSqrtMagnitude(v2));
		//return coWeightSum / (Math.sqrt(weightSumSquare1) * Math.sqrt(weightSumSquare2));
	}

	private static Map<String,Double> getTfIdfVector(IndexReader ir, int doc, String field) throws Exception {
		Map<String,Double> tfIdf = new HashMap<String,Double>();
		TermFreqVector tv = ir.getTermFreqVector(doc, field);
		double termTotal = 0.0;
		if(tv == null) return tfIdf;
		String [] terms = tv.getTerms();
		int [] counts = tv.getTermFrequencies();
		double docTotal = ir.numDocs();
		for(int i=0;i<tv.size();i++) {
			String term = terms[i];
			//if(!term.matches(".*[A-Za-z].*")) continue;
			termTotal += counts[i];
		}
		for(int i=0;i<tv.size();i++) {
			String term = terms[i].intern();
			//if(!term.matches(".*[A-Za-z].*")) continue;
			double tf = counts[i] / termTotal;
			double idf = Math.log(docTotal / ir.docFreq(new Term(field, term)));
			tfIdf.put(term, tf * idf);
			//tfIdf.put(term, tf);
		}
		return tfIdf;
	}
			
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		LuceneIndexerSearcher lis = new LuceneIndexerSearcher(false);
		IndexReader ir = lis.getIndexReader();
		Directory dir = new RAMDirectory(ir.directory());
		ir.close();
		IndexSearcher is = new IndexSearcher(dir);
		ir = is.getIndexReader();

		List<String> docFiles = new ArrayList<String>();
		for(int i=0;i<ir.numDocs();i++) {
			docFiles.add(ir.document(i).getField("filename").stringValue().replaceAll("markedup", "source"));
		}
				
		List<Map<String,Double>> vectors = new ArrayList<Map<String,Double>>(ir.numDocs());
		long time = System.currentTimeMillis();
		for(int i=0;i<ir.maxDoc();i++) {
			Map<String,Double> vector = getTfIdfVector(ir, i, "txt");
			vectors.add(vector);
		}
		System.out.println("Read vectors in: " + (System.currentTimeMillis() - time));
		time = System.currentTimeMillis();

		SimilarityMatrix sm = new SimilarityMatrix(docFiles);
		
		int goodCosines = 0;
		int totalCosines = 0;
		for(int i=0;i<vectors.size();i++) {
			for(int j=i+1;j<vectors.size();j++) {
				double cosine = cosine(vectors.get(i), vectors.get(j));
				sm.setSimilarity(i, j, (float)cosine);
				if(cosine > 0.05) goodCosines++;
				totalCosines++;
			}
		}
		System.out.println("Fraction of pairwise similarities: " + goodCosines * 1.0 / totalCosines);
		
		System.out.println("Cosines calculated in: " + (System.currentTimeMillis() - time));
		time = System.currentTimeMillis();

		QTClusterer qt = new QTClusterer(sm);
		qt.makeClusters(0.05);
		System.out.println("Made clusters in: " + (System.currentTimeMillis() - time));
		for(Set<Integer> cluster : qt.getClustersBySize()) {
			List<File> clusterFiles = new ArrayList<File>();
			Map<Integer,Float> clusterMap = new HashMap<Integer,Float>();
 			for(Integer i : cluster) {
 				clusterMap.put(i, 1.0f);
				System.out.println(docFiles.get(i));
				clusterFiles.add(new File(docFiles.get(i)));
			}
			ClusterAnalyser.analyseCluster(new ArrayList<Integer>(cluster), ir, new TanimotoSimilarity(), 0.05);
			System.out.println();
		}
		
		for(Integer i : qt.getUnclustered()) {
			System.out.println(docFiles.get(i));
		}

		ir.close();
	}

}
