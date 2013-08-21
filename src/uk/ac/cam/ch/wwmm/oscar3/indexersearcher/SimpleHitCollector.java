package uk.ac.cam.ch.wwmm.oscar3.indexersearcher;

import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.search.HitCollector;

final class SimpleHitCollector extends HitCollector {

	private List<Integer> hits; 
	
	SimpleHitCollector() {
		hits = new ArrayList<Integer>();
	}

	List<Integer> getHits() {
		return hits;
	}
	
	@Override
	public void collect(int docNo, float score) {
		hits.add(docNo);
	}

}
