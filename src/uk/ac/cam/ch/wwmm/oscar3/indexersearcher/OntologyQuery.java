package uk.ac.cam.ch.wwmm.oscar3.indexersearcher;

import java.util.Set;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

/** A Query for an ontology term or all terms below it.
 * 
 * @author ptc24
 *
 */
final class OntologyQuery {
	private Set<String> ids;
	private BooleanQuery bq;
	
	OntologyQuery(Set<String> ids) {
		this.ids = ids;
		bq = new BooleanQuery();
		for(String id : ids) {
			TermQuery tq = new TermQuery(new Term("Ontology", id));
			/* We need *at least one of* these */
			bq.add(new BooleanClause(tq, BooleanClause.Occur.SHOULD));
		}
	}
		
	Query getLuceneQuery() {
		return bq;
	}
	
	Set<String> getIDs() {
		return ids;
	}	

}
