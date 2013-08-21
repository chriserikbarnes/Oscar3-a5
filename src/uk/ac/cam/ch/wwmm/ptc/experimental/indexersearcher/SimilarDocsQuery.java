package uk.ac.cam.ch.wwmm.ptc.experimental.indexersearcher;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.DefaultSimilarity;
import org.apache.lucene.search.Hit;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

import Jama.Matrix;

import uk.ac.cam.ch.wwmm.oscar3.indexersearcher.LuceneIndexerSearcher;
import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;

public class SimilarDocsQuery {
	
	private static Map<String,Integer> docFreqs = new HashMap<String,Integer>();
	
	private static int getDocFreq(IndexReader ir, String field, String s) throws Exception {
		if(docFreqs.containsKey(s)) return docFreqs.get(s);
		int df = ir.docFreq(new Term(field, s));
		docFreqs.put(s, df);
		return df;
	}
	
	private static class MySimilarity extends DefaultSimilarity {
		@Override
		public float coord(int arg0, int arg1) {
			return 1.0f;
		}
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
			//double idf = 1.0;
			double idf = Math.log(docTotal / ir.docFreq(new Term(field, term)));
			//double idf = Math.log(docTotal / getDocFreq(ir, field, term));
			tfIdf.put(term, tf * idf);
			//tfIdf.put(term, tf);
		}
		return tfIdf;
	}
	public static Query myMoreLikeThisQuery(IndexReader ir, int doc) throws Exception {
		BooleanQuery bq = new BooleanQuery();
		Map<String,Double> tfIdf = getTfIdfVector(ir, doc, "txt");
		double totalTfIdf = 0.0;
		for(String t : tfIdf.keySet()) totalTfIdf += (tfIdf.get(t) * tfIdf.get(t));
		double cumulative = 0.0;
		int foo;
		for(String t : StringTools.getSortedList(tfIdf)) {
			double fractional = (tfIdf.get(t) * tfIdf.get(t)) / totalTfIdf;
			cumulative += fractional;
			if(cumulative > 0.95) break;
			Term term = new Term("txt", t);
			//if(ir.docFreq(term) == 1) foo=1;
			//System.out.println(t + "\t" + cumulative + "\t" + fractional);
			TermQuery tq = new TermQuery(term);
			tq.setBoost((float)fractional);
			bq.add(new BooleanClause(tq, BooleanClause.Occur.SHOULD));
		}
		return bq;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		LuceneIndexerSearcher lis = new LuceneIndexerSearcher(false);
		IndexReader ir = lis.getIndexReader();
		Directory dir = new RAMDirectory(ir.directory());
		ir.close();
		IndexSearcher is = new IndexSearcher(dir);
		//IndexSearcher is = lis.getIndexSearcher();
		is.setSimilarity(new MySimilarity());
		ir = is.getIndexReader();
		long time = System.currentTimeMillis();
		Matrix matrix = new Matrix(ir.numDocs(), ir.numDocs());
		int nz = 0;
		for(int i=0;i<ir.numDocs();i++) {
			System.out.println(i);
			//nz++;
			matrix.set(i, i, 1.0);
			Query q = myMoreLikeThisQuery(ir, i);
			//time = System.currentTimeMillis();
			Hits h = is.search(q);
			//System.out.println(System.currentTimeMillis() - time);
			//time = System.currentTimeMillis();
			Iterator iterator = h.iterator();
			while(iterator.hasNext()) {
				Hit hit = (Hit)iterator.next();
				int id = hit.getId();
				float score = hit.getScore();
				//if(score < 0.025) break;
				if(i != id) {
					//matrix.set(i, id, score);
					matrix.set(i, id, matrix.get(i, id) + score/2);
					matrix.set(id, i, matrix.get(id, i) + score/2);
					//nz++;
				}
				//System.out.println(i + "\t" + hit.getId() + "\t" + hit.getScore());
			}
			//System.out.println(System.currentTimeMillis() - time);
			//System.out.println(((double)h.length()) / ir.numDocs());
			
		}
		for(int i=0;i<matrix.getColumnDimension();i++) {
			for(int j=0;j<matrix.getRowDimension();j++) {
				if(matrix.get(i, j) != 0.0) nz++;
			}
		}
		System.out.println(System.currentTimeMillis() - time);
		time = System.currentTimeMillis();
		//matrix.eig();
		System.out.println(System.currentTimeMillis() - time);
		System.out.println(nz);
		
		
		
		int newnz = 0;
		System.out.println(matrix.getColumnDimension() + " " + matrix.getRowDimension() + " " + nz);
		for(int i=0;i<matrix.getColumnDimension();i++) {
			int nonzero = 0;
			for(int j=0;j<matrix.getRowDimension();j++) {
				if(matrix.get(i, j) != 0.0) nonzero++;
			}
			System.out.println(nonzero);
			for(int j=0;j<matrix.getRowDimension();j++) {
				if(matrix.get(i, j) != 0.0) System.out.println(j + " " + matrix.get(i, j));
			}
			newnz += nonzero;
		}
		System.out.println(newnz);
	}

}
