package uk.ac.cam.ch.wwmm.ptclib.misc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.tartarus.snowball.SnowballProgram;

/**A handy wrapper for the Snowball stemmers included with Lucene.
 * 
 * @author ptc24
 *
 */
public final class Stemmer {
	private static final Object [] EMPTY_ARGS = new Object[0];

	private SnowballProgram stemmer;
	
	/**Makes a wrapper for the given SnowballProgram.
	 * 
	 * @param stemmer The SnowballProgram to wrap.
	 */
	public Stemmer(SnowballProgram stemmer) {
		this.stemmer = stemmer;
	}
	
	/**Gets the stem of a word. This also works for multi-word terms; the last 
	 * word will be stemmed, as whitespace is treated like any other character.
	 * 
	 * @param word The word.
	 * @return The stem.
	 */
	public synchronized String getStem(String word) {
		stemmer.setCurrent(word);
		try {
			stemmer.getClass().getMethod("stem", new Class[0]).invoke(stemmer, EMPTY_ARGS);			
		} catch (Exception e) {
			throw new Error(e);
		}
		return stemmer.getCurrent();
	}
	
	/**Takes a list of words, finds the stems, and groups the words together
	 * by stem. This also works for multi-word terms; the last word will be
	 * stemmed, as whitespace is treated like any other character.
	 * 
	 * @param words The words to stem.
	 * @return A mapping from the stems to the lists of words that correspond
	 * to the stems.
	 */
	public Map<String,List<String>> wordsToStems(Collection<String> words) {
		Map<String,List<String>> stems = new HashMap<String,List<String>>();
		for(String s : words) {
			String stem = getStem(s);
			if(!stems.containsKey(stem)) stems.put(stem, new ArrayList<String>());
			stems.get(stem).add(s);
		}
		return stems;
	}

}
