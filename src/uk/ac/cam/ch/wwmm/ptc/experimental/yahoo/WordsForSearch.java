package uk.ac.cam.ch.wwmm.ptc.experimental.yahoo;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import nu.xom.Attribute;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Serializer;
import uk.ac.cam.ch.wwmm.oscar3.chemnamedict.ChemNameDictSingleton;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.StringSource;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.TokenSequence;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.Tokeniser;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.tokenanalysis.NGram;
import uk.ac.cam.ch.wwmm.oscar3.terms.TermSets;
import uk.ac.cam.ch.wwmm.ptclib.datastruct.Bag;
import uk.ac.cam.ch.wwmm.ptclib.io.FileTools;
import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;

public class WordsForSearch {

	public static Element listToElement(String name, List<String> words, int maxLen, boolean follow) throws Exception {
		Element overall = new Element(name);
		if(words.size() > maxLen) words = words.subList(0, maxLen);
		for(String word : words) {
			Element results = new Element("results");
			results.addAttribute(new Attribute("word", word));
			List<String> lineList = YahooSearch.getLines(word, 1, follow);
			for(String line : lineList) {
				line = line.trim();
				if(!line.toLowerCase().contains(word.toLowerCase())) continue;
				Element lineElem = new Element("line");
				lineElem.appendChild(line);
				results.appendChild(lineElem);
			}
			overall.appendChild(results);
		}
		return overall;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		NGram.getInstance();
		Set<String> chemWords = new HashSet<String>(NGram.getInstance().chemSet);
		Set<String> engWords = new HashSet<String>(NGram.getInstance().engSet);
		
		List<File> files = new ArrayList<File>();
		files = FileTools.getFilesFromDirectoryByName(new File("/home/ptc24/newows/corpora/roughPubMed"), "source.xml");

		StringSource ss = new StringSource(files, false);
		
		Bag<String> wordCounts = new Bag<String>();
		
		ss.reset();
		for(String s : ss) {
			TokenSequence t = Tokeniser.getInstance().tokenise(s);
			for(String word : t.getTokenStringList()) {
				if(!word.matches(".*[a-z][a-z].*")) continue;
				word = StringTools.normaliseName(word);
				wordCounts.add(word);
			}
		}	
		
		List<String> chemList = new ArrayList<String>();
		List<String> engList = new ArrayList<String>();
		List<String> unknownList = new ArrayList<String>();
		
		wordCounts.discardInfrequent(2);
		
		for(String word : wordCounts.getList()) {
			String pw = NGram.parseWord(word);
			if(TermSets.getClosedClass().contains(pw)) {
				
			} else if(TermSets.getUsrDictWords().contains(word)) {
			
			} else if(ChemNameDictSingleton.hasName(word)) {
				
			} else if(chemWords.contains(pw)) {
				System.out.println(word + "\t" + "C" + "\t" + wordCounts.getCount(word));
				chemList.add(word);
			} else if(engWords.contains(pw)) {
				System.out.println(word + "\t" + "E" + "\t" + wordCounts.getCount(word));				
				engList.add(word);
			} else {
				System.out.println(word + "\t" + "U" + "\t" + wordCounts.getCount(word));
				unknownList.add(word);
			}
		}
		System.out.println(chemList.size());
		System.out.println(engList.size());
		System.out.println(unknownList.size());
		
		//if(true) return;
		
		Element overall = new Element("overall");
		Document doc = new Document(overall);
		overall.appendChild(listToElement("chemical", chemList, 150, false));
		overall.appendChild(listToElement("nonchemical", engList, 150, false));
		overall.appendChild(listToElement("unknown", unknownList, 150, false));
		
		File f = new File("/home/ptc24/tmp/searchTest.xml");
		FileOutputStream fos = new FileOutputStream(f);
		
		Serializer ser = new Serializer(fos);
		ser.setIndent(2);
		ser.write(doc);
	}

}
