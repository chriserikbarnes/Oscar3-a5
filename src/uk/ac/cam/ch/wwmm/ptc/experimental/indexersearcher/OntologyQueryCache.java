package uk.ac.cam.ch.wwmm.ptc.experimental.indexersearcher;

import java.util.Collection;
import java.util.Map;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.BooleanClause.Occur;

import uk.ac.cam.ch.wwmm.oscar3.indexersearcher.VectorCollector;
import uk.ac.cam.ch.wwmm.ptclib.datastruct.CacheMap;

public class OntologyQueryCache {

	Map<String,VectorCollector> cache;
	static OntologyQueryCache myCache;

	public static VectorCollector getResultsStatic(String ontQ, Collection<String> queryItems, IndexSearcher is) throws Exception {
		if(myCache == null) myCache = new OntologyQueryCache();
		return myCache.getResults(ontQ, queryItems, is);
	}
	
	public OntologyQueryCache() {
		cache = new CacheMap<String,VectorCollector>(10000);
	}
	
	public VectorCollector getResults(String ontQ, Collection<String> queryItems, IndexSearcher is) throws Exception {
		if(cache.containsKey(ontQ)) return cache.get(ontQ);
		
		BooleanQuery bq = new BooleanQuery(true);
		if(queryItems.size() <= BooleanQuery.getMaxClauseCount()) {
			for(String ont : queryItems) {
				bq.add(new BooleanClause(new TermQuery(new Term("Ontology", ont)), Occur.SHOULD));
			}
			VectorCollector vc = new VectorCollector();
			is.search(bq, vc);
			cache.put(ontQ, vc);
			return vc;
		}
		return new VectorCollector();
	}
	
}
