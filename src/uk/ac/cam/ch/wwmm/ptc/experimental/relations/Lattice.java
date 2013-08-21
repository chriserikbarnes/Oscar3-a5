package uk.ac.cam.ch.wwmm.ptc.experimental.relations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nu.xom.Element;
import nu.xom.Elements;
import uk.ac.cam.ch.wwmm.oscar3.misc.NewGeniaRunner;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.NamedEntity;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.ProcessingDocument;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.SentenceSplitter;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.Token;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.TokenSequence;

public class Lattice {

	List<LatticeCell> startCells;
	List<LatticeCell> allCells;
	Map<Token,LatticeCell> tokensToTokenCells;
	Map<Token,List<LatticeCell>> tokensToAllCells;
	TokenSequence tokSeq;
	static boolean runGenia = true;
	
	public static List<Lattice> buildLattices(ProcessingDocument procDoc, Element safElem) throws Exception {
		List<Lattice> lattices = new ArrayList<Lattice>();
		List<TokenSequence> tokSeqs = procDoc.getTokenSequences();
		
		if(runGenia) {
			for(TokenSequence tokSeq : tokSeqs) {
				List<Token> tokens = tokSeq.getTokens();
				List<List<Token>> sentences = SentenceSplitter.makeSentences(tokens);
				for(List<Token> sentence : sentences) {
					NewGeniaRunner.runGenia(sentence);
					List<NamedEntity> bioNEs = NewGeniaRunner.getGeniaNEs(sentence);
					for(NamedEntity bioNE : bioNEs) {
						safElem.appendChild(bioNE.toSAF());
					}
				}
			}
		}
		
		Elements annots = safElem.getChildElements("annot");
		Map<Integer,List<Element>> startsToNEs = new HashMap<Integer,List<Element>>();
		Map<Integer,List<Element>> endsToNEs = new HashMap<Integer,List<Element>>();
		for(int i=0;i<annots.size();i++) {
			Element elem = annots.get(i);
			if(!elem.getAttributeValue("type").equals("oscar")) continue;
			String startXPoint = elem.getAttributeValue("from");
			String endXPoint = elem.getAttributeValue("to");
			int start = procDoc.getStandoffTable().getOffsetAtXPoint(startXPoint);
			int end = procDoc.getStandoffTable().getOffsetAtXPoint(endXPoint);
			if(!startsToNEs.containsKey(start)) startsToNEs.put(start, new ArrayList<Element>());
			startsToNEs.get(start).add(elem);
			if(!endsToNEs.containsKey(end)) endsToNEs.put(end, new ArrayList<Element>());
			endsToNEs.get(end).add(elem);
		}
		
		for(TokenSequence tokSeq : tokSeqs) {
			lattices.add(new Lattice(tokSeq, procDoc, startsToNEs, endsToNEs));
		}
		return lattices;
	}
	
	private Lattice(TokenSequence tokSeq, ProcessingDocument procDoc, Map<Integer,List<Element>> startsToNEs, Map<Integer,List<Element>> endsToNEs) throws Exception {
		startCells = new ArrayList<LatticeCell>();
		allCells = new ArrayList<LatticeCell>();
		tokensToTokenCells = new HashMap<Token,LatticeCell>();
		tokensToAllCells = new HashMap<Token,List<LatticeCell>>();
		this.tokSeq = tokSeq;
		Map<Token,LatticeCell> tokenToPrev = new HashMap<Token,LatticeCell>();
		LatticeCell prevCell = null;
		// Build basic token chain
		//System.out.println("**************************");
		for(Token token : tokSeq.getTokens()) {
			//System.out.println(">" + token.getValue() + "<");
			LatticeCell cell = new LatticeCell(token);
			tokensToTokenCells.put(token,cell);
			tokensToAllCells.put(token, new ArrayList<LatticeCell>());
			tokensToAllCells.get(token).add(cell);
			allCells.add(cell);
			if(prevCell != null) {
				prevCell.addNext(cell);
				tokenToPrev.put(token, prevCell);
			} else {
				startCells.add(cell);
			}
			prevCell = cell;
		}
		//System.out.println("**************************");
		// Parenthetical phrases
		Token beforeBracket = null;
		Token prevToken = null;
		for(Token token : tokSeq.getTokens()) {
			if(prevToken == null) {
				prevToken = token;
				continue;
			}
			if(beforeBracket == null) {
				if(token.getValue().equals("(")) beforeBracket = prevToken;
			} else if(token.getValue().equals(")")) {
				LatticeCell beforeBracketCell = tokensToTokenCells.get(beforeBracket);
				LatticeCell bracketCell = tokensToTokenCells.get(token);
				if(beforeBracketCell != null || bracketCell != null) {
					beforeBracketCell.addInheritance(bracketCell);
					//System.out.println("Parenthetical: " + tokSeq.getSubstring(beforeBracket.getId(), token.getId()));
				}
				beforeBracket = null;
			}
			prevToken = token;
		}
		
		// Build in named entities
		for(Token token : tokSeq.getTokens()) {
			if(startsToNEs.containsKey(token.getStart())) {
				for(Element neElem : startsToNEs.get(token.getStart())) {
					Token endToken = procDoc.getTokenByEnd(neElem.getAttributeValue("to"));
					if(endToken == null) continue;
					prevCell = tokenToPrev.get(token);
					LatticeCell endTokenCell = tokensToTokenCells.get(endToken);
					if(endTokenCell == null) continue;
					LatticeCell neCell = new LatticeCell(neElem, endTokenCell);
					allCells.add(neCell);
					tokensToAllCells.get(token).add(neCell);
					if(prevCell == null) {
						startCells.add(neCell);
					} else {
						prevCell.addNext(neCell);
					}
				}
			}
		}
		
		prevToken = null;
		for(Token token : tokSeq.getTokens()) {
			if(prevToken != null && prevToken.getGeniaData() != null 
					&& prevToken.getGeniaData()[3].endsWith("-NP")
					&& token.getGeniaData() != null
					&& !token.getGeniaData()[3].equals("I-NP")) {
				LatticeCell cell = tokensToTokenCells.get(prevToken);
				cell.addNext(LatticeCell.endNPCell());
			}
			prevToken = token;
		}
		
		for(LatticeCell cell : allCells) {
			cell.recieveInheritance();
		}
		
		for(LatticeCell cell : allCells) {
			cell.nextToPrev();
		}
	}
	
	public List<LatticeCell> getAllCells() {
		return allCells;
	}
	
	public List<LatticeCell> getCellsForTokens(List<Token> tokens) {
		List<LatticeCell> cells = new ArrayList<LatticeCell>();
		for(Token t : tokens) cells.addAll(tokensToAllCells.get(t));
		return cells;
	}
	
}
