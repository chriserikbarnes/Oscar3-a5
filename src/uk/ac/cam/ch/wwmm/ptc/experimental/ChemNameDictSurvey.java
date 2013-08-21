package uk.ac.cam.ch.wwmm.ptc.experimental;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uk.ac.cam.ch.wwmm.oscar3.chemnamedict.ChemNameDictSingleton;
import uk.ac.cam.ch.wwmm.oscar3.terms.TermSets;
import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;

public class ChemNameDictSurvey {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		//String inchi = ChemNameDictSingleton.getInChIForShortestSmiles("CHCl3");
		//System.out.println(ChemNameDictSingleton.getNamesFromInChI(inchi));
		Set<String> names = ChemNameDictSingleton.getAllNames();
		System.out.println(names.size());
		//for(String name : names) {
		//if(name.matches(".*\\sacetal")) {
		//		System.out.println(name);
		//	}
		//}
		/*Set<String> udw = TermSets.getUsrDictWords();
		Map<String,Set<String>> iffyWords = new HashMap<String,Set<String>>();
		Set<String> wordSet = new HashSet<String>();
		for(String name : names) {
			if(udw.contains(name)) {
				if(!iffyWords.containsKey(name)) iffyWords.put(name, new HashSet<String>());
				iffyWords.get(name).add(name);
			}
			if(name.matches(".*\\s+.*")) {
				List<String> words = StringTools.arrayToList(name.split("\\s+"));
				for(String word : words) {
					if("acid".equals(word)) continue;
					if(udw.contains(word) || word.matches("\\([a-z]+\\)")) {
						if(!iffyWords.containsKey(word)) iffyWords.put(word, new HashSet<String>());
						iffyWords.get(word).add(name);
					}
					wordSet.add(word);
				}
			} else {
				wordSet.add(name);
			}
		}
		for(String word: iffyWords.keySet()) {
			System.out.println(word + "\t\t" + iffyWords.get(word));
		}
		Map<String,Set<String>> prefixes = new HashMap<String,Set<String>>();
		for(String name : names) {
			Pattern p = Pattern.compile("(.*\\s+)?([a-z][a-z]+)-[a-z][a-z].*");
			Matcher m = p.matcher(name);
			if(m.matches()) {
				System.out.println(m.group(2) + "\t\t" + name);
				if(!prefixes.containsKey(m.group(2))) prefixes.put(m.group(2), new HashSet<String>());
				prefixes.get(m.group(2)).add(name);
			}
		}
		Set<String> nsps = TermSets.getNoSplitPrefixes();

		for(String prefix : prefixes.keySet()) {
			if(nsps.contains(prefix)) continue;
			System.out.println(prefix + "\t\t" + prefixes.get(prefix));			
		}
		
		System.out.println(wordSet.size());*/
	}

}
