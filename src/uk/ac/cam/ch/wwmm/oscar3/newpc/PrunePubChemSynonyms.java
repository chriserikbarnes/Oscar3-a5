package uk.ac.cam.ch.wwmm.oscar3.newpc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import uk.ac.cam.ch.wwmm.ptclib.io.ResourceGetter;

/**Discards obviously bad compound names from PubChem, by testing them against
 * regexes, or a list of known bad names.
 * 
 * @author ptc24
 *
 */
public final class PrunePubChemSynonyms {

	List<Pattern> patterns;
	Set<String> stopNames;
	
	/**Creates a PrunePubChemSynonyms instance, loading and intitalising the
	 * regexes and bad names.
	 * 
	 * @throws Exception
	 */
	public PrunePubChemSynonyms() throws Exception {
		ResourceGetter rg = new ResourceGetter("uk/ac/cam/ch/wwmm/oscar3/newpc/resources/");
		patterns = new ArrayList<Pattern>();
		for(String line: rg.getStrings("stopRegexes.txt")) {
			patterns.add(Pattern.compile(line));
		}
		stopNames = new HashSet<String>();
		for(String s : rg.getStrings("stopNames.txt")) stopNames.add(s.trim());
	}
	
	/**Checks to see whether a name is acceptable.
	 * 
	 * @param name The name to check.
	 * @return Whether it is acceptable.
	 */
	public boolean isGoodName(String name) {
		if(stopNames.contains(name)) return false;
		for(Pattern p : patterns) {
			if(p.matcher(name).matches()) return false;
		}
		return true;
	}
	
}