package uk.ac.cam.ch.wwmm.ptc.experimental.relations;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.Token;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.TokenSequence;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.finder.AutomatonState;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.finder.DFAFinder;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.finder.ResultsCollector;
import uk.ac.cam.ch.wwmm.oscar3.terms.TermMaps;
import dk.brics.automaton.RunAutomaton;

/** An experimental subclass of DFAFinder, used for simple information
 * extraction tasks.
 * 
 * @author ptc24
 *
 */

@SuppressWarnings("serial")
public class DFARelationFinder extends DFAFinder {

	private Map<String, List<String>> termVariants;
	
	private static Pattern tailPattern = Pattern.compile("(.*?)<([a-z]+)>");
	
	private static DFARelationFinder myInstance;
	
	public static DFARelationFinder getInstance() {
		if(myInstance == null) {
			myInstance = new DFARelationFinder();
		}
		return myInstance;
	}

	List<Relation> relations;
	String sentenceStr;
	File file;
	
	public DFARelationFinder() {
		super.init();
	}
	
	/*public List<Relation> getRelations(List<Token> sentence, String sentenceStr, File file) {
		relations = new ArrayList<Relation>();
		this.sentenceStr = sentenceStr;
		this.file = file;
		Tokeniser t = sentence.get(0).getTokeniser();
		findItems(t, sentence.get(0).getId(), sentence.get(sentence.size()-1).getId());
		return relations;
	}*/
	
	public List<Relation> getRelations(List<Token> sentence, String sentenceStr, Lattice lattice, File file) {
		relations = new ArrayList<Relation>();
		this.sentenceStr = sentenceStr;
		this.file = file;

		for(LatticeCell cell : lattice.getCellsForTokens(sentence)) {
			for(String type : runAuts.keySet()) {
				RunAutomaton runAut = runAuts.get(type);
				int start = runAut.getInitialState();
				List<LatticeCell> cellPath = new ArrayList<LatticeCell>();
				List<String> repPath = new ArrayList<String>();
				//System.out.println("Starting with: " + type + "\t" + cell.getValue());
				seekAlongPath(type, start, cell, cellPath, repPath);
			}
		}
		return relations;
	}

	
	@Override
	protected void addTerms() {
		termVariants = new HashMap<String, List<String>>();
		for(String s : TermMaps.getIePatterns().keySet()) addNE(s, TermMaps.getIePatterns().get(s), true);
		for(String s : tokenToRep.keySet()) {
			Matcher m = tailPattern.matcher(s);
			if(m.matches()) {
				String subTerm = m.group(1);
				if(!termVariants.containsKey(subTerm)) termVariants.put(subTerm, new ArrayList<String>());
				termVariants.get(subTerm).add(s);
			}
		}
	}

	/*@Override
	protected List<String> getTokenReps(Token t) {
		List<String> reps = new ArrayList<String>();
		for(String s : t.ieReps(this)) {
			reps.add(s);
			if(termVariants.containsKey(s)) reps.addAll(termVariants.get(s));
		}
		return reps;
	}*/

	/*@Override
	protected void handleNe(AutomatonState a, int endToken, Tokeniser t) {
		throw new Error("This method is no longer supported");
		// TODO Auto-generated method stub
		String type = a.type;
		if(type.contains("_")) {
			type = type.split("_")[0];
		}
		List<String> reps = a.getReps();
		Map<String,List<String>> dict = new HashMap<String,List<String>>();
		Map<String,List<Element>> entityDict = new HashMap<String,List<Element>>();
		for(int i=0;i<reps.size();i++) {
			Matcher m = tailPattern.matcher(reps.get(i));
			if(m.matches()) {
				String role = m.group(2);
				Token token = t.getToken(a.startToken+i);
				Element neElem = token.getNeElem();
				if(neElem != null) {
					if(!entityDict.containsKey(role)) entityDict.put(role, new ArrayList<Element>());
					entityDict.get(role).add(neElem);
				}
				if(!dict.containsKey(role)) dict.put(role, new ArrayList<String>());
				dict.get(role).add(t.getToken(a.startToken+i).getValue());
			}
		}
		relations.add(new Relation(type, dict, entityDict, sentenceStr, file, reps));
	}*/

	protected void handleNe(String type, List<LatticeCell> cellPath, List<String> reps) {
		// TODO Auto-generated method stub
		if(type.contains("_")) {
			type = type.split("_")[0];
		}
		//Map<String,List<String>> dict = new HashMap<String,List<String>>();
		//Map<String,List<Element>> entityDict = new HashMap<String,List<Element>>();
		Map<String,List<LatticeCell>> dict = new HashMap<String,List<LatticeCell>>();
		for(int i=0;i<reps.size();i++) {
			Matcher m = tailPattern.matcher(reps.get(i));
			if(m.matches()) {
				String role = m.group(2);
				LatticeCell cell = cellPath.get(i);
				if(!dict.containsKey(role)) dict.put(role, new ArrayList<LatticeCell>());
				dict.get(role).add(cell);
			}
		}
		relations.add(new Relation(type, dict, sentenceStr, file, reps));
	}
		
	public void seekAlongPath(String type, int state, LatticeCell cell, List<LatticeCell> cellPath, List<String> repPath) {
		//System.out.println("At state " + state);
		Set<String> reps = cell.getReps(this);
		RunAutomaton runAut = runAuts.get(type);
		Set<String> newReps = new HashSet<String>();
		for(String rep : reps) {
			newReps.add(rep);
			if(termVariants.containsKey(rep)) {
				//System.out.println(rep + "\t" + termVariants.get(rep));
				newReps.addAll(termVariants.get(rep));
			}
		}
		reps = newReps;
		//System.out.println("At state " + state);
		//System.out.println(reps);
		for(String rep : reps) {
			//System.out.println("Now at state " + state);
			String repCode = getRepForTokenOrNull(rep);
			//System.out.println("NowNow at state " + state);
			if(repCode != null) {
				//System.out.println("Trying: " + type + "\t" + rep + "\t" + repCode + "\tat\t" + state);
				int newState = state;
				for(int i=0;i<repCode.length();i++) {
					char c = repCode.charAt(i);
					newState = runAut.step(newState, c);
					//System.out.println("\t" + newState);
					if(newState == -1) break;
				}
				if(newState == -1) continue;
				//System.out.println("Added: " + type + "\t" + cell.getValue());
				//System.out.println("Added: " + type + "\t" + repCode);
				List<LatticeCell> newCellPath = new ArrayList<LatticeCell>(cellPath);
				newCellPath.add(cell);
				List<String> newRepPath = new ArrayList<String>(repPath);
				newRepPath.add(rep);
				if(runAut.isAccept(newState)) {
					//System.out.println("Making new relation");
					handleNe(type, newCellPath, newRepPath);// Handle the relation
				}
				if(cell.hasNext()) {
					for(LatticeCell nextCell : cell.getNext()) {
						//System.out.println("Seeking from " + rep + " to state " + newState);
						seekAlongPath(type, newState, nextCell, newCellPath, newRepPath);
					}
				}
			}
		}
		//System.out.println("Leaving reps");
	}

	@Override
	protected void handleNe(AutomatonState a, int endToken, TokenSequence t, ResultsCollector collector) {
		throw new Error("This method is no longer supported");
	}

}
