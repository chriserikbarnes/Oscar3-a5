package uk.ac.cam.ch.wwmm.oscar3.indexersearcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.search.HitCollector;

/** Converts Lucene Hits into a Map<Integer,Float>.
 * 
 * @author ptc24
 *
 */
public final class VectorCollector extends HitCollector {

	private Map<Integer,Float> resultsVector;
	
	/**Initialise a new, empty VectorCollector.
	 * 
	 */
	public VectorCollector() {
		resultsVector = new HashMap<Integer,Float>();
	}
	
	@Override
	public void collect(int arg0, float arg1) {
		resultsVector.put(arg0, arg1);
	}
	
	/**Gets the mapping from document number to score.
	 * 
	 * @return A mapping from document number to score.
	 */
	public Map<Integer, Float> getResultsVector() {
		return resultsVector;
	}
	
	/**Gets a list of document numbers, sorted by score, highest score
	 * first.
	 * 
	 * @return The document numbers.
	 */
	public List<Integer> hitsByScore() {
		List<Integer> results = new ArrayList<Integer>(resultsVector.keySet());
		final Map<Integer,? extends Comparable> fmap = resultsVector;
		Collections.sort(results, Collections.reverseOrder(new Comparator<Integer>() {
			@SuppressWarnings("unchecked")
			public int compare(Integer o1, Integer o2) {
				// TODO Auto-generated method stub
				return fmap.get(o1).compareTo(fmap.get(o2));
			}
		}));
		return results;
	}
	
}
