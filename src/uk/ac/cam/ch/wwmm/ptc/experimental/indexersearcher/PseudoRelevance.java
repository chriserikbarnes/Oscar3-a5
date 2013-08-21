package uk.ac.cam.ch.wwmm.ptc.experimental.indexersearcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.BooleanClause.Occur;
import org.tartarus.snowball.ext.EnglishStemmer;

import uk.ac.cam.ch.wwmm.oscar3.indexersearcher.LuceneIndexerSearcher;
import uk.ac.cam.ch.wwmm.oscar3.indexersearcher.VectorCollector;
import uk.ac.cam.ch.wwmm.ptclib.misc.Stemmer;
import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;

public class PseudoRelevance {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		LuceneIndexerSearcher lis = new LuceneIndexerSearcher(false);
		IndexSearcher is = lis.getIndexSearcher();

		Stemmer stemmerTools = new Stemmer(new EnglishStemmer());
		
		//QueryParser qp = new Oscar3QueryParser("txt", new Oscar3Analyzer(), lis, false);
		//Query q = qp.parse("NaCl");
		
		String queryTerm = "lipid";
		//PhraseQuery pq = new PhraseQuery();
		//pq.add(new Term("txt", "aromatase"));
		//pq.add(new Term("txt", "inhibitors"));
		Query q = new TermQuery(new Term("txt", queryTerm));
		//Query q = new StemQuery(new Term("txt", queryTerm), stemmerTools);

		for(int i=0;i<100;i++) {
			VectorCollector vc = new VectorCollector();
			is.search(q, vc);
			for(Integer j : new ArrayList<Integer>(vc.getResultsVector().keySet())) {
				if(vc.getResultsVector().get(j) < 0.2) vc.getResultsVector().remove(j);
			}
			Map<String,Double> scores = ClusterAnalyser.simpleExcessAnalyseCluster(vc.getResultsVector(), lis.getIndexReader(), 0.1);
			BooleanQuery bq = new BooleanQuery(false);
			List<String> terms = StringTools.getSortedList(scores);
			if(terms.size() > 10) terms = terms.subList(0, 10);
			for(String s : terms) {
				System.out.println(s + "\t" + scores.get(s));
				TermQuery tq = new TermQuery(new Term("txt", s));
				tq.setBoost(scores.get(s).floatValue());
				bq.add(new BooleanClause(tq, Occur.SHOULD));
			}
			q = bq;
			System.out.println();
		}
		
		/*Hits hits = is.search(q);
		int maxHit = Math.min(hits.length(), 50);
		System.out.println(maxHit);
		Bag<String> termBag = new Bag<String>();
		for(int i=0;i<maxHit;i++) {
			File f = new File(hits.doc(i).get("filename").replaceAll("markedup.xml", "source.xml"));
			System.out.println(f);
			TermFreqVector tv = is.getIndexReader().getTermFreqVector(hits.id(i), "txt");
			String [] terms = tv.getTerms();
			int [] counts = tv.getTermFrequencies();
			for(int j=0;j<terms.length;j++) {
				String term = terms[j];
				if(!TermSets.getClosedClass().contains(term) && term.matches(".*[A-Za-z].*")) termBag.add(term, counts[j]);
			}
		}
		
		BooleanQuery bq = new BooleanQuery();
		//termBag.discardInfrequent(Math.max(2, (int)Math.sqrt(maxHit)));
		List<String> termList = termBag.getList();
		if(termList.size() > 100) termList = termList.subList(0, 100);
		for(String term : termList) {
			TermQuery tq = new TermQuery(new Term("txt", term));
			tq.setBoost(termBag.getCount(term));
			bq.add(new BooleanClause(tq, Occur.SHOULD));
			System.out.println(term + "\t" + termBag.getCount(term));
		}
		//bq.add(new BooleanClause(q, Occur.MUST_NOT));
		
		hits = is.search(bq);
		maxHit = Math.min(hits.length(), 50);

		for(int i=0;i<maxHit;i++) {
			File f = new File(hits.doc(i).get("filename").replaceAll("markedup.xml", "source.xml"));
			System.out.println(f);
			TermFreqVector tv = is.getIndexReader().getTermFreqVector(hits.id(i), "txt");
			String [] terms = tv.getTerms();
			int [] counts = tv.getTermFrequencies();
			boolean hasTerm = false;
			for(int j=0;j<terms.length;j++) {
				String term = terms[j];
				if(term.equals(queryTerm)) {
					hasTerm = true;
					break;
				}
			}
			if(!hasTerm) System.out.println("Does not have query term!");
		}*/
		
	}

}
