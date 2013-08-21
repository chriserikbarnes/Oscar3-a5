package uk.ac.cam.ch.wwmm.ptc.experimental.indexersearcher;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import uk.ac.cam.ch.wwmm.ptclib.datastruct.Bag;
import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;
import Jama.EigenvalueDecomposition;
import Jama.Matrix;

public class TfIdfSimilarity {

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
			termTotal += counts[i];
		}
		for(int i=0;i<tv.size();i++) {
			String term = terms[i].intern();
			double tf = counts[i] / termTotal;
			double idf = Math.log(docTotal / ir.docFreq(new Term(field, term)));
			tfIdf.put(term, tf * idf);
			//tfIdf.put(term, tf);
		}
		return tfIdf;
	}
	
	private static void laplacianise(Matrix m) {
		int d = m.getColumnDimension();
		for(int i=0;i<d;i++) {
			double degree = 0.0;
			for(int j=0;j<d;j++) degree += m.get(i, j);
			for(int j=0;j<d;j++) {
				m.set(i, j, (i==j ? degree : 0) - m.get(i, j));
			}
		}
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
		/*LuceneIndexerSearcher lis = new LuceneIndexerSearcher(false);

		IndexReader ir = lis.getIndexReader();*/
		
		System.out.println(ir.numDocs());
		System.out.println(ir.maxDoc());
		
		List<Map<String,Double>> vectors = new ArrayList<Map<String,Double>>(ir.numDocs());
		long time = System.currentTimeMillis();
		for(int i=0;i<ir.maxDoc();i++) {
		//for(int i=0;i<50;i++) {
			//System.out.println(i);
			Map<String,Double> vector = getTfIdfVector(ir, i, "txt");
			//vector.putAll(getTfIdfVector(ir, i, "InChI"));
			//if(vector.size() < 10) vector = new HashMap<String,Double>();
			vectors.add(vector);
		}
		System.out.println("Read vectors in: " + (System.currentTimeMillis() - time));
		time = System.currentTimeMillis();
		
		Matrix matrix = new Matrix(vectors.size(), vectors.size());
		
		//Map<String,Double> cosines = new HashMap<String,Double>();
		for(int i=0;i<vectors.size();i++) {
		//for(int i=0;i<50;i++) {
			//System.out.println(i);
			matrix.set(i, i, 1.0);
			for(int j=i+1;j<vectors.size();j++) {
				double cosine = cosine(vectors.get(i), vectors.get(j));
				//if(cosine > 0.05) cosines.put(i + " -> " + j, cosine);
				matrix.set(i, j, cosine);
				matrix.set(j, i, cosine);
				//cosines.put(i + " -> " + j, cosine);
			}
		}
		
		System.out.println("Cosines calculated in: " + (System.currentTimeMillis() - time));
		time = System.currentTimeMillis();

		//laplacianise(matrix);
		
		
		EigenvalueDecomposition evd = matrix.eig();
		System.out.println("evd in: " + (System.currentTimeMillis() - time));
		double [] evr = evd.getRealEigenvalues();		
		Matrix evm = evd.getV();

		int evnum = evr.length;

		for(int i=0;i<evr.length;i++) {
			if(evr[i] <= 1.0) {
				evnum--;
			} else {
				break;
			}
		}
		
		System.out.println("Using " + evnum + " eigenvectors");
		
		Matrix sm = evm.getMatrix(0, evm.getRowDimension()-1, evm.getColumnDimension()-evnum, evm.getColumnDimension()-1);
		
		for(int i=0;i<evnum;i++) {
			for(int j=0;j<evr.length;j++) {
				sm.set(j, evnum-i-1, sm.get(j, evnum-i-1) * (evr[evr.length-i-1] - 1.0));
			}
		}
		// 1.14 = moderate fuzzy
		List<Map<Integer,Float>> clusters = KMeans.cmc(sm, evnum, 1.05, 0.1, 0.5);
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
		
		/*List<List<Integer>> clusters = KMeans.kmc(sm, evnum);
		for(List<Integer> cluster : clusters) {
			for(Integer item : cluster) {
				System.out.println(ir.document(item).getField("filename").stringValue().replaceAll("markedup", "source"));
			}
			analyseCluster(cluster, ir);
			System.out.println();
		}*/
		
		if(true) return;
		//evm.print(6, 2);
		//evd.getD().print(6, 2);

		Map<String,Double> termTots = new HashMap<String,Double>();
		for(int i=0;i<evr.length;i++) {
			for(String term : vectors.get(i).keySet()) {
				if(ir.docFreq(new Term("txt", term)) < 10) continue;
				if(!termTots.containsKey(term)) termTots.put(term, 0.0);
				termTots.put(term, termTots.get(term) + (vectors.get(i).get(term)));
			}
		}
		//System.out.println("termTots: " + termTots.size());
		
		
		Map<String,Double> evect = new HashMap<String,Double>();

		Map<String,List<Double>> etvs = new HashMap<String,List<Double>>();
		
		Map<String,Double> etv = new HashMap<String,Double>();
		Map<String,Double> etv2 = new HashMap<String,Double>();
		for(int i=0;i<evr.length;i++) {
			System.out.println(evr[i]);
		}
		
		int nev = Math.min(evr.length, 20);
		
		for(int i=0;i<evr.length;i++) {
			//System.out.println(evr[i]);
			double val = evm.get(i, evr.length - 2);
			double val2 = evm.get(i, evr.length - 3);
			evect.put("\tfile://" + ir.document(i).getField("filename").stringValue().replaceAll("markedup", "source"), val);
			new File(ir.document(i).getField("filename").stringValue()).getParentFile().getName();
			System.out.println(val + "\t" + val2 + "\t" + new File(ir.document(i).getField("filename").stringValue()).getParentFile().getName());
			for(String term : vectors.get(i).keySet()) {
				if(!termTots.containsKey(term)) continue;
				List<Double> coords = etvs.get(term);
				if(coords == null) {
					coords = new ArrayList<Double>(nev);
					for(int j=0;j<nev;j++) {
						coords.add(0.0);
					}
					etvs.put(term, coords);
				}
				for(int j=0;j<nev;j++) {
					int idx = evr.length - 1 - j;
					coords.set(j, coords.get(j) + (evm.get(i, idx) * vectors.get(i).get(term) / termTots.get(term)));
				}
					//new ArrayList<Double>();
				
				/*if(!etv.containsKey(term)) etv.put(term, 0.0);
				etv.put(term, etv.get(term) + (val * vectors.get(i).get(term) / termTots.get(term)));
				if(!etv2.containsKey(term)) etv2.put(term, 0.0);
				etv2.put(term, etv2.get(term) + (val2 * vectors.get(i).get(term) / termTots.get(term)));*/
			}
		}
		
		File outFile = new File("/home/ptc24/tmp/ev.csv");
		PrintWriter out = new PrintWriter(new FileWriter(outFile));
		
		for(String term : etvs.keySet()) {
			if(term.matches("\\s*")) continue;
			out.print(term);
			for(Double d : etvs.get(term)) {
				out.print("\t" + d);
			}
			out.println();
		}
		
		out.close();
		
		/*for(String s : StringTools.getSortedList(evect)) {
			System.out.println(s + "\t" + evect.get(s));
		}*/
		
		/*for(String term : StringTools.getSortedList(etv)) {
			if(ir.docFreq(new Term("txt", term)) < 2) continue;
			String tr = term;
			if(tr.matches("\\s*")) tr = "FOOO";
			System.out.println(etv.get(term) + "\t" +  etv2.get(term) + "\t" + tr);
		}*/
		
		
		/*for(int i=0;i<evr.length;i++) {
			//System.out.println(evr[i] + "\t" + evi[i]);
			for(int j=0;j<evr.length;j++) {
				System.out.print(evm.get(i, j) + "\t");
			}
			System.out.println(evr[i]);
		}*/
		
		System.out.println(System.currentTimeMillis() - time);

		/*for(String cosStr : StringTools.getSortedList(cosines)) {
			System.out.println(cosStr + "\t" + cosines.get(cosStr));
			String [] foo = cosStr.split(" -> ");
			int doc1 = Integer.parseInt(foo[0]);
			int doc2 = Integer.parseInt(foo[1]);
			System.out.println("file://" + ir.document(doc1).getField("filename").stringValue().replaceAll("markedup", "source"));
			System.out.println("file://" + ir.document(doc2).getField("filename").stringValue().replaceAll("markedup", "source"));
			Map<String,Double> combined = combine(vectors.get(doc1), vectors.get(doc2));
			double total = 0.0;
			for(String s : combined.keySet()) {
				total += combined.get(s);
			}
			double cumulative = 0.0;
			for(String s : StringTools.getSortedList(combined)) {
				cumulative += combined.get(s);
				System.out.println("\t" + s + "\t" + combined.get(s) + "\t" + (100.0 * cumulative / total) + "%");
			}
		}*/
		
		/*Map<String,Double> tfIdf = getTfIdfVector(ir, 1);
		for(String term : StringTools.getSortedList(tfIdf)) {
			System.out.println(term + "\t" + tfIdf.get(term));
		}*/
		
		ir.close();
	}

}
