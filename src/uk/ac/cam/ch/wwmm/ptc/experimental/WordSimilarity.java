package uk.ac.cam.ch.wwmm.ptc.experimental;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;

import uk.ac.cam.ch.wwmm.oscar3.indexersearcher.LuceneIndexerSearcher;
import uk.ac.cam.ch.wwmm.oscar3.terms.TermSets;
import uk.ac.cam.ch.wwmm.ptc.experimental.termsimilarity.QTClusterer;
import uk.ac.cam.ch.wwmm.ptc.experimental.termsimilarity.SimilarityMatrix;
import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;

public class WordSimilarity {

	public static double wordSimilarity(String s1, String s2) {
		int ld = StringUtils.getLevenshteinDistance(s1, s2);
		return 1.0 - (ld / (1.0 * Math.max(s1.length(), s2.length())));
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception{
		LuceneIndexerSearcher lis = new LuceneIndexerSearcher(false);
		IndexReader ir = lis.getIndexReader();
		
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
				if(docFreq > 0) {
					docFreqs.put(text, ir.docFreq(t));
				}
			}			
		}
		List<String> dfl = StringTools.getSortedList(docFreqs);
		int maxSize = 20000;
		if(dfl.size() > maxSize) dfl = dfl.subList(0, maxSize);
		System.out.println("Have " + dfl.size() + " terms");
		
		SimilarityMatrix sm = new SimilarityMatrix(dfl);
		for(int i=0;i<dfl.size();i++) {
			for(int j=i+1;j<dfl.size();j++) {
				sm.setSimilarity(i, j, (float)wordSimilarity(dfl.get(i), dfl.get(j)));
			}
		}
		System.out.println("Similarity matrix made");
		QTClusterer qt = new QTClusterer(sm);
		qt.makeClusters(0.5);
		for(Set<String> terms : qt.getClustersOfNames()) {
			System.out.println(terms);
		}
		
	}

}
