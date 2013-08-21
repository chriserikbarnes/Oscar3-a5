package uk.ac.cam.ch.wwmm.ptc.experimental.indexersearcher;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;

import uk.ac.cam.ch.wwmm.oscar3.indexersearcher.LuceneIndexerSearcher;
import uk.ac.cam.ch.wwmm.oscar3.indexersearcher.VectorCollector;
import uk.ac.cam.ch.wwmm.oscar3.terms.TermSets;
import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;

public class Split {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		LuceneIndexerSearcher lis = new LuceneIndexerSearcher(false);
		IndexSearcher is = lis.getIndexSearcher();
		IndexReader ir = lis.getIndexReader();
		
		TermEnum textEnum = ir.terms();
		Map<String,Integer> docFreqs = new HashMap<String,Integer>();
		float nd = ir.numDocs() * 1.0f;
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
		Map<String,Integer> termMap = new HashMap<String,Integer>();
		Map<Integer,Float> dff = new HashMap<Integer,Float>();
		for(int i=0;i<dfl.size();i++) {
			termMap.put(dfl.get(i), i);
			dff.put(i, docFreqs.get(dfl.get(i)) / nd);
		}
		
		for(int i=0;i<50;i++) {
			TermQuery tq = new TermQuery(new Term("txt", dfl.get(i)));
			VectorCollector vc = new VectorCollector();
			is.search(tq, vc);
			float vcs = vc.getResultsVector().size();
			Map<Integer,Float> stf = new HashMap<Integer,Float>();
			
			for(Integer j : vc.getResultsVector().keySet()) {
				TermFreqVector tv = ir.getTermFreqVector(j, "txt");
				String [] terms = tv.getTerms();
				for(int k=0;k<tv.size();k++) {
					String term = terms[k];
					if(termMap.containsKey(term)) {
						int termId = termMap.get(term);
						if(!stf.containsKey(termId)) stf.put(termId, 0.0f);
						stf.put(termId, stf.get(termId) + 1.0f/vcs);						
					}
				}
			}
			float excess = 0.0f;
			for(Integer k : stf.keySet()) {
				float excessOnTerm = stf.get(k) - dff.get(k);
				if(excessOnTerm > 0.0f) excess += excessOnTerm;
			}
			System.out.println(dfl.get(i) + "\t" + vcs + "\t" + excess * vcs);
		}
	}

}
