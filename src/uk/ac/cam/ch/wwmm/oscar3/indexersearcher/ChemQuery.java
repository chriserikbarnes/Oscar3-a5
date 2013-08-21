package uk.ac.cam.ch.wwmm.oscar3.indexersearcher;

import java.util.Map;
import java.util.Set;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

/** A data structure to hold a list of InChIs for a chemical query. This also holds
 * a boost factor for each InChI, to generate a BooleanQuery that can be weighted, for
 * example towards compounds that are more similar to the query molecule. Note that this
 * does NOT subclass Query.
 * 
 * @author ptc24
 *
 */
final class ChemQuery {

	private Map<String, Float> inchis;
	private BooleanQuery bq;
	
	public ChemQuery(Map<String, Float> inchis) {
		this.inchis = inchis;
		bq = new BooleanQuery();
		for(String inchi : inchis.keySet()) {
			TermQuery tq = new TermQuery(new Term("InChI", inchi));
			tq.setBoost(inchis.get(inchi));
			/* We need *at least one of* these */
			bq.add(new BooleanClause(tq, BooleanClause.Occur.SHOULD));
		}
	}
		
	public Query getLuceneQuery() {
		return bq;
	}
	
	public Set<String> getInChIs() {
		return inchis.keySet();
	}	
}
