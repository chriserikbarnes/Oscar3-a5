package uk.ac.cam.ch.wwmm.oscar3.indexersearcher;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import uk.ac.cam.ch.wwmm.oscar3.terms.OntologyTerms;

/** Autosuggest for ontology queries and JSON.
 * 
 * @author ptc24
 *
 */
public final class OntologicalArguaments {

	private static OntologicalArguaments myInstance;
	private Map<String, Set<String>> prefixToTerms;
	
	private static OntologicalArguaments getInstance() {
		if(myInstance == null) myInstance = new OntologicalArguaments();
		return myInstance;
	}
	
	public static void reinitialise() {
		myInstance = null;
		getInstance();
	}
	
	public static void destroyInstance() {
		myInstance = null;
	}
	
	private OntologicalArguaments() {
		prefixToTerms = new HashMap<String, Set<String>>();
		try {
			for(String term : OntologyTerms.getAllTerms()) {
				int minPrefixLength = term.length() / 4;
				if(minPrefixLength == 0) minPrefixLength = 1;
				for(int i=minPrefixLength;i<=term.length();i++) {
					addTerm(term.substring(0,i), term);
				}
			}

		
		} catch (Exception e) {
		}
	}
	
	private void addTerm(String prefix, String term) {
		Set<String> terms = prefixToTerms.get(prefix);
		if(terms == null) {
			terms = new HashSet<String>();
			prefixToTerms.put(prefix, terms);
		}
		terms.add(term);
	}
	
	/**Gets a set of possible terms corresponding to a given prefix.
	 * 
	 * @param prefix The first few letters of a potential term.
	 * @return The terms that correspond to the prefix.
	 */
	public static Set<String> getTerms(String prefix) {
		return getInstance().prefixToTerms.get(prefix);
	}
	
}
