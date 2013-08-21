package uk.ac.cam.ch.wwmm.ptc.experimental.indexersearcher;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.search.TermQuery;

import uk.ac.cam.ch.wwmm.oscar3.indexersearcher.LuceneIndexerSearcher;
import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;

public class QuerySimilarity {

	private static Map<Map<Integer,Float>,Double> cache = new HashMap<Map<Integer,Float>,Double>();

	private static double getSqrtMagnitude(Map<Integer,Float> v) {
		if(cache.containsKey(v)) return cache.get(v);
		double weightSumSquare = 0.0;
		for(int f : v.keySet()) {
			weightSumSquare += v.get(f) * v.get(f);
		}
		double result = Math.sqrt(weightSumSquare);
		cache.put(v, result);
		return result;
	}

	public static double cosine(Map<Integer,Float> v1, Map<Integer,Float> v2) {
		/*double weightSumSquare1 = 0.0;
		for(int f : v1.keySet()) {
			weightSumSquare1 += v1.get(f) * v1.get(f);
		}
		double weightSumSquare2 = 0.0;
		for(int f : v2.keySet()) {
			weightSumSquare2 += v2.get(f) * v2.get(f);
		}*/
		double coWeightSum = 0.0;
		for(int f : v1.keySet()) {
				if(v2.containsKey(f)) {
					coWeightSum += (v1.get(f) * v2.get(f));				
				}
		}
		return coWeightSum / (getSqrtMagnitude(v1) * getSqrtMagnitude(v2));
		//return coWeightSum / (Math.sqrt(weightSumSquare1) * Math.sqrt(weightSumSquare2));
	}
	
	public static double tanimoto(Map<Integer,Float> v1, Map<Integer,Float> v2) {
		double weightSumSquare1 = 0.0;
		for(int f : v1.keySet()) {
			weightSumSquare1 += v1.get(f) * v1.get(f);
		}
		double weightSumSquare2 = 0.0;
		for(int f : v2.keySet()) {
			weightSumSquare2 += v2.get(f) * v2.get(f);
		}
		double coWeightSum = 0.0;
		for(int f : v1.keySet()) {
				if(v2.containsKey(f)) {
					coWeightSum += (v1.get(f) * v2.get(f));				
				}
		}
		return coWeightSum / (weightSumSquare1 + weightSumSquare2 - coWeightSum);
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		//System.out.println(StringTools.arrayToList(StandardAnalyzer.STOP_WORDS));
		//if(true) return;
		
		LuceneIndexerSearcher lis = new LuceneIndexerSearcher(false);

		String queryTerm = "cyp2d6";
		
		/*List<String> queryList = StringTools.arrayToList(new String[] {
		"cyp2d6", 
		"cyp3a4", 
		"dextromethorphan"});*/
		List<String> queryList = new ArrayList<String>();
		long time = System.currentTimeMillis();
		IndexReader ir = lis.getIndexReader();
		TermEnum termEnum = ir.terms();
		while(termEnum.next()) {
			Term t = termEnum.term();
			if(t.field().equals("txt") && termEnum.docFreq() > 20) queryList.add(t.text());
		}
		//queryList.addAll(lis.termsFromQuery(new TermQuery(new Term("txt", queryTerm))));
		System.out.println("All terms loaded: " + (System.currentTimeMillis() - time));
		
		time = System.currentTimeMillis();
		Map<String,Map<Integer,Float>> vectors = new HashMap<String,Map<Integer,Float>>();
		for(String query : queryList) {
			//System.out.println(query);
			vectors.put(query, lis.getScoresVectorForQuery(new TermQuery(new Term("txt", query))));
		}
		/*PhraseQuery pq = new PhraseQuery();
		pq.add(new Term("txt", "cyp2d6"));
		pq.add(new Term("txt", "inhibitors"));
		vectors.put("cyp2d6 inhibitors", lis.getScoresVectorForQuery(pq));*/
		System.out.println("All vectors calculated: " + (System.currentTimeMillis() - time));

		time = System.currentTimeMillis();
		Map<String,Double> cosines = new HashMap<String,Double>();
		for(String qt1 : queryList) {
		//String qt1 = queryTerm;
		for(String qt2 : queryList) {
				if(qt1.compareTo(qt2) < 1) continue;
				double cosine = cosine(vectors.get(qt1), vectors.get(qt2));
				if(cosine > 0.05) cosines.put(qt1 + " -> " + qt2, cosine);
			}
		}
		System.out.println("All cosines calculated: " + (System.currentTimeMillis() - time));

		for(String sim : StringTools.getSortedList(cosines)) {
			System.out.println(sim + "\t" + cosines.get(sim));
		}
	}

}
