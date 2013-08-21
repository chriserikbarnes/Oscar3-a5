package uk.ac.cam.ch.wwmm.ptc.experimental.indexersearcher;

import java.util.List;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import uk.ac.cam.ch.wwmm.oscar3.indexersearcher.LuceneIndexerSearcher;
import uk.ac.cam.ch.wwmm.oscar3.indexersearcher.VectorCollector;
import uk.ac.cam.ch.wwmm.oscar3.terms.TermSets;
import uk.ac.cam.ch.wwmm.ptclib.datastruct.Bag;

public class EntropySplit {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		LuceneIndexerSearcher lis = new LuceneIndexerSearcher(false);
		IndexSearcher is = lis.getIndexSearcher();
		IndexReader ir = lis.getIndexReader();
		Bag<String> tfBag = new Bag<String>();
		for(int i=0;i<ir.maxDoc();i++) {
			TermFreqVector tv = ir.getTermFreqVector(i, "txt");
			String [] terms = tv.getTerms();
			int [] freqs = tv.getTermFrequencies();
			for(int k=0;k<tv.size();k++) {
				String term = terms[k];
				if("In".equals(term)) continue;
				if(TermSets.getClosedClass().contains(term)) continue;
				if(!term.matches(".*[A-Za-z].*")) continue;
				tfBag.add(term, freqs[k]);
			}
		}
		double overallEntropy = tfBag.entropy();
		double totalEntropy = overallEntropy * tfBag.totalCount();
		System.out.println(totalEntropy);
		
		List<String> termList = tfBag.getList().subList(0, 2000);
		for(String splitTerm : termList) {
			Query q = new TermQuery(new Term("txt", splitTerm));
			VectorCollector vc = new VectorCollector();
			is.search(q, vc);
			Bag<String> inBag = new Bag<String>();
			Bag<String> outBag = new Bag<String>();
			for(int i=0;i<ir.maxDoc();i++) {
				Bag<String> bag = inBag;
				if(!vc.getResultsVector().containsKey(i)) continue;
				
				//Bag<String> bag = outBag;
				//if(vc.getResultsVector().containsKey(i)) bag = inBag;
				TermFreqVector tv = ir.getTermFreqVector(i, "txt");
				String [] terms = tv.getTerms();
				int [] freqs = tv.getTermFrequencies();
				for(int k=0;k<tv.size();k++) {
					String term = terms[k];
					if("In".equals(term)) continue;
					if(TermSets.getClosedClass().contains(term)) continue;
					if(!term.matches(".*[A-Za-z].*")) continue;
					bag.add(term, freqs[k]);
				}
			}
			//double splitEntropy = (inBag.entropy() * inBag.totalCount()) + (outBag.entropy() * outBag.totalCount());
			//System.out.println(splitTerm + "\t" + (totalEntropy - splitEntropy));
			double subSetEntropy = (inBag.entropy() * inBag.totalCount());
			double subSetCrossEntropy = (inBag.crossEntropy(tfBag) * inBag.totalCount());
			double subSetMaybeEntropy = (overallEntropy * inBag.totalCount());
			//System.out.println(splitTerm + "\t" + (subSetMaybeEntropy - subSetEntropy));
			System.out.println(splitTerm + "\t" + (subSetCrossEntropy - subSetEntropy) + "\t" + (subSetMaybeEntropy - subSetEntropy));
		}
		
		
	}

}
