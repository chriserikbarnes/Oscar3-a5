package uk.ac.cam.ch.wwmm.opsin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uk.ac.cam.ch.wwmm.ptclib.io.ResourceGetter;
import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;

import nu.xom.Elements;
import dk.brics.automaton.Automaton;
import dk.brics.automaton.RegExp;
import dk.brics.automaton.RunAutomaton;

/**Performs finite-state allocation of roles ("annotations") to tokens.
 * TODO: standardise the nomenclature about what an annotation is.
 *
 * @author ptc24
 *
 */
class ParseRules {

	/** A "struct" containing bits of state needed during finite-state parsing. */
	private class AnnotatorState {
		/** The current state of the DFA. */
		int state;
		/** The annotation so far. */
		List<Character> annot;
		/** The strings these annotations correspond to. */
		ArrayList<String> tokens;
	}

	/**
	 * Wrapper class for returning multiple objects
	 */
	final class TwoReturnValues {
		final List<ParseTokens> first;
		final boolean second;

		public TwoReturnValues(List<ParseTokens> first, boolean second) {
			this.first = first;
			this.second = second;
		}

		public List<ParseTokens> getFirst() {
			return first;
		}

		public boolean getSecond() {
			return second;
		}
	}

	/** Maps regular expression names to regular expressions. */
	private HashMap<String, String> regexDict;
	/** A DFA for "root" words in chemical names. */
	private RunAutomaton chemAutomaton;
	/** A DFA for "substituent" words in chemical names. */
	private RunAutomaton subAutomaton;
	/** Which group in the regex represents the root of a chemical name. */
	private int rootGroupNumber;
	/** Compiled substituent regex. */
	private Pattern substituentsRegex;
	/** Compiled chemical regex. */
	Pattern chemicalRegex;

	private TokenManager tokenManager;

	//private String largestLex="";//holds the smallest amount of unlexed/unlexable text so far

	private static ResourceGetter resourceGetter = NameToStructure.resourceGetter;

	/** Initialises the finite-state parser, reading in the rules from regexes.xml.
	 * @param tokenManager
	 *
	 * @throws Exception If the rules file can't be read properly.
	 */
	ParseRules(TokenManager tokenManager) throws Exception {
		this.tokenManager =tokenManager;
		regexDict = new HashMap<String, String>();
		Elements regexes = resourceGetter.getXMLDocument("regexes.xml").getRootElement().getChildElements("regex");
		Pattern p = Pattern.compile("%.*?%");
		for(int i=0;i<regexes.size();i++) {
			String name = regexes.get(i).getAttributeValue("name");
			String value = regexes.get(i).getAttributeValue("value");
			Matcher m = p.matcher(value);
			String newValue = "";
			int position = 0;
			while(m.find()) {
				newValue += value.substring(position, m.start());
				if (regexDict.get(m.group())==null){
					throw new ParsingException("Regex entry for: " + m.group() + " missing! Check regexes.xml");
				}
				newValue += regexDict.get(m.group());
				position = m.end();
			}
			newValue += value.substring(position);
			regexDict.put(name, newValue);
		}

		chemAutomaton = getChemicalAutomaton();
		subAutomaton = getSubstituentAutomaton();
		rootGroupNumber = 1 + StringTools.countOpenBrackets(regexDict.get("%chemical%")) -
			StringTools.countOpenBrackets(regexDict.get("%mainGroup%"));

		substituentsRegex=Pattern.compile(regexDict.get("%substituent%") +"*");
		chemicalRegex=Pattern.compile(regexDict.get("%chemical%"));
	}

	/** Compiles the DFA for a "root" chemical name word.
	 *
	 * @return The DFA for a "root" chemical name word.
	 */
	RunAutomaton getChemicalAutomaton() {
		String re = regexDict.get("%chemical%");

		Automaton a = new RegExp(re).toAutomaton();
		a.determinize();
		return new RunAutomaton(a);
	}

	/** Compiles the DFA for a "substituent" chemical name word.
	 *
	 * @return The DFA for a "substituent" chemical name word.
	 */
	RunAutomaton getSubstituentAutomaton() {
		String re = regexDict.get("%substituent%");

		Automaton a = new RegExp(re+"*").toAutomaton();
		a.determinize();
		return new RunAutomaton(a);
	}

	/** Compiles the DFA for a chemical name word.
	 *
	 * @param wr The word rule for the word.
	 * @return The DFA for the chemical name word.
	 */
	RunAutomaton getAutomatonForWordRule(String wr) {
		if(wr.equals("substituent")) return subAutomaton;
		return chemAutomaton;
	}

	/**Determines the possible annotations for a chemical word.
	 *
	 * @param possibleAnnotations A list of (list of possible annotations)s for each token in the word
	 * @param wordRule The word rule for the word.
	 * @return A list of possible annotations for the word.
	 */
	TwoReturnValues getParses(String chemicalWord, String wordRule) {
		//largestLex=chemicalWord;
		RunAutomaton automaton = getAutomatonForWordRule(wordRule);
		AnnotatorState as = new AnnotatorState();
		as.state = automaton.getInitialState();
		as.annot = new ArrayList<Character>();
		as.tokens = new ArrayList<String>();
		List<AnnotatorState> states = new ArrayList<AnnotatorState>();
		moveToNextAnnotation(chemicalWord, chemicalWord.toLowerCase(), as, states, automaton, automaton.getCharIntervals());
		List<ParseTokens> outputList = new ArrayList<ParseTokens>();
		boolean lexable = false;
		if (states.size() > 0){
			lexable = true;
		}
		for(AnnotatorState aas : states) {
//			System.out.println("finalstate: "+ aas.state);
//			System.out.println("finalannote: "+ aas.annot);
			if(automaton.isAccept(aas.state)) {
				ParseTokens pt =new ParseTokens();
				pt.tokens=aas.tokens;
				pt.annotations=aas.annot;
				outputList.add(pt);
			}
		}
		if (outputList.size()==0){
			//System.out.println(largestLex);
		}
		TwoReturnValues output =new TwoReturnValues(outputList, lexable);
		return output;
	}

	/**
	 * Recursively attempts to find all valid annotations for the given chemical name
	 * This is done by a depth first search
	 * Successful annotations are stored in successfulAnnotations
	 * @param chemicalWord The chemical name with any part of the name that has been lexed already removed from the front
	 * @param chemicalWordLowerCase The same chemical name but lower case, used for token matching but not regex matching
	 * @param as An AnnotatorState, this contains the current state of the automaton.
	 * @param succesfulAnnotations
	 */
	void moveToNextAnnotation(String chemicalWord, String chemicalWordLowerCase, AnnotatorState as, List<AnnotatorState> succesfulAnnotations, RunAutomaton automaton, char[] stateSymbols ){
		int wordLength = chemicalWordLowerCase.length();
		if (wordLength ==0){
			succesfulAnnotations.add(as);
		}
		String firstTwoLetters =null;
		if (wordLength >=2){
			firstTwoLetters=chemicalWordLowerCase.substring(0,2);
		}

		for (int j = 0; j < stateSymbols.length; j++) {
			char annotationCharacter =stateSymbols[j];
			int potentialNextState = automaton.step(as.state, annotationCharacter);
			if (potentialNextState != -1) {//-1 means this state is not accessible from the previous state
				HashMap<String, List<String>> possibleTokenisationsMap = tokenManager.symbolTokenNamesDict.get(annotationCharacter);
				if (possibleTokenisationsMap!=null){
					List<String> possibleTokenisations =null;
					if (firstTwoLetters!=null){
						possibleTokenisations= possibleTokenisationsMap.get(firstTwoLetters);
					}
					if (possibleTokenisations!=null){//next could be a token
						for (String possibleTokenisation : possibleTokenisations) {
							if (chemicalWordLowerCase.startsWith(possibleTokenisation)){
								AnnotatorState newAs =new AnnotatorState();
								String newchemicalWord=chemicalWord.substring(possibleTokenisation.length());
								String newchemicalWordLowerCase=chemicalWordLowerCase.substring(possibleTokenisation.length());
								newAs.tokens =new ArrayList<String>(as.tokens);
								newAs.tokens.add(possibleTokenisation);
								newAs.annot = new ArrayList<Character>(as.annot);
								newAs.annot.add(annotationCharacter);
								newAs.state=potentialNextState;
								//System.out.println("tokened " +newchemicalWord);
								moveToNextAnnotation(newchemicalWord, newchemicalWordLowerCase, newAs, succesfulAnnotations, automaton, stateSymbols);
							}
						}
					}
				}
				List<Pattern> possibleRegexes =tokenManager.symbolRegexesDict.get(annotationCharacter);
				if (possibleRegexes!=null){//next could be a regex
					for (Pattern pattern : possibleRegexes) {
						Matcher mat =pattern.matcher(chemicalWord);
						if (mat.lookingAt()){//match at start
							AnnotatorState newAs =new AnnotatorState();
							String newchemicalWord=chemicalWord.substring(mat.group(0).length());
							String newchemicalWordLowerCase=chemicalWordLowerCase.substring(mat.group(0).length());
							newAs.tokens =new ArrayList<String>(as.tokens);
							newAs.tokens.add(mat.group(0));
							newAs.annot = new ArrayList<Character>(as.annot);
							newAs.annot.add(annotationCharacter);
							newAs.state=potentialNextState;
							//System.out.println("neword regex " +newchemicalWord);
							moveToNextAnnotation(newchemicalWord, newchemicalWordLowerCase, newAs, succesfulAnnotations, automaton, stateSymbols);
						}
					}
				}
			}
		}
//		if (chemicalWord.length() < largestLex.length()){
//			largestLex=chemicalWord;
//		}
	}


	/**Groups the token annotations for a given "substituent" word, such that each annotation
	 * represents a chemical fragment.
	 *
	 * @param annots The annotation for a word.
	 * @return The list of annotations produced.
	 */
	List<List<Character>> chunkAnnotationsForSubstituentWord(List<Character> annots) {
		LinkedList<List<Character>> chunkList = new LinkedList<List<Character>>();
		String previous = StringTools.charListToString(annots);
		Matcher m;
		while(previous.length() > 0) {
			m = substituentsRegex.matcher(previous);
			m.matches();
			String subAnnot = m.group(1);
			chunkList.addFirst(StringTools.stringToList(subAnnot));
			previous = previous.substring(0, previous.length() - subAnnot.length());
		}
		return chunkList;
	}

	/**Groups the token annotations for a given "root" word, such that each annotation
	 * represents a chemical fragment.
	 *
	 * @param annots The annotation for a word.
	 * @return The list of annotations produced.
	 */
	List<List<Character>> chunkAnnotationsForRootWord(List<Character> annots) {
		LinkedList<List<Character>> chunkList = new LinkedList<List<Character>>();
		String annotStr = StringTools.charListToString(annots);
		Matcher m = chemicalRegex.matcher(annotStr);
		m.matches();
		String previous =annotStr;
		String rootAnnot = m.group(rootGroupNumber);
		if (rootAnnot !=null){
			chunkList.addFirst(StringTools.stringToList(rootAnnot));
			previous = annotStr.substring(0, annotStr.length() - rootAnnot.length());
		}
		while(previous.length() > 0) {
			m = substituentsRegex.matcher(previous);
			m.matches();
			String subAnnot = m.group(1);
			chunkList.addFirst(StringTools.stringToList(subAnnot));
			previous = previous.substring(0, previous.length() - subAnnot.length());
		}
		return chunkList;
	}

}
