package uk.ac.cam.ch.wwmm.ptc.experimental;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.Query;

import uk.ac.cam.ch.wwmm.oscar3.indexersearcher.LuceneIndexerSearcher;
import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;

public class Fuzzy {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		LuceneIndexerSearcher lis = new LuceneIndexerSearcher(false);
		
		Query q = new FuzzyQuery(new Term("txt", "adrenaline"), 0.1f);
		Query qq = lis.getIndexSearcher().rewrite(q);
		
		if(qq instanceof BooleanQuery) {
			BooleanQuery bq = (BooleanQuery)qq;
			BooleanClause [] clauses = bq.getClauses();
			final HashMap<String,Float> hm = new HashMap<String,Float>();
			
			for(int i=0;i<clauses.length;i++) {
				Set s = new HashSet();		
				clauses[i].getQuery().extractTerms(s);
				Term t = (Term)s.toArray()[0];
				hm.put(t.text(), clauses[i].getQuery().getBoost());
			}
			
			for(String s : StringTools.getSortedList(hm)) {
				System.out.println(s + "\t" + hm.get(s));
			}
			
			/*List<String> al = new ArrayList<String>(hm.keySet());
			Collections.sort(al, Collections.reverseOrder(new Comparator<String>() {
				public int compare(String arg0, String arg1) {
					return hm.get(arg0).compareTo(hm.get(arg1));
				}
			}));*/
		}
	}

}
