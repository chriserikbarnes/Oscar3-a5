package uk.ac.cam.ch.wwmm.oscar3.recogniser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Node;
import nu.xom.Nodes;
import nu.xom.Text;
import uk.ac.cam.ch.wwmm.oscar3.Oscar3Props;
import uk.ac.cam.ch.wwmm.oscar3.misc.NETypes;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.NamedEntity;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.ProcessingDocument;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.ProcessingDocumentFactory;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.Token;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.TokenSequence;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.finder.DFANEFinder;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.finder.DFAONTCPRFinder;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.memm.MEMMSingleton;
import uk.ac.cam.ch.wwmm.ptclib.saf.ResolvableStandoff;
import uk.ac.cam.ch.wwmm.ptclib.saf.SafTools;
import uk.ac.cam.ch.wwmm.ptclib.saf.StandoffResolver;
import uk.ac.cam.ch.wwmm.ptclib.scixml.XMLStrings;
import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;
import uk.ac.cam.ch.wwmm.ptclib.xml.XOMTools;

/**Co-ordinates the overall recognition of chemical names in a document.
 * 
 * @author ptc24
 *
 */
public final class NameRecogniser {
	
	private Pattern dataStartPattern = Pattern.compile("[mMbB]\\.?p|\\[\u03b1\\]\\d*D|" +
	"IR|[\u03bd\u03bb]max(\\(.*)?|\u03b4[CHP](\\(.*)?|Rf");

	private NameRecogniser() {
		
	}

	/** Recognises named entities and data in a document.
	 * 
	 * @param sourceDoc The SciXML document to parse, now unlikely to be altered.
	 * @param safDoc A standoff annotation document.
	 * @throws Exception
	 */
	public static void processDocument(Document sourceDoc, Document safDoc) throws Exception {
		new NameRecogniser().processDocumentInternal(sourceDoc, safDoc);
	}
	
	/** Recognises named entities and data in a document.
	 * 
	 * @param sourceDoc The SciXML document to parse, now unlikely to be altered.
	 * @param safDoc An (empty) SAF XML document.
	 * @throws Exception
	 */
	private void processDocumentInternal(Document sourceDoc, Document safDoc) throws Exception {
		ProcessingDocument procDoc = ProcessingDocumentFactory.getInstance().makeTokenisedDocument(new Document((Element)XOMTools.safeCopy(sourceDoc.getRootElement())), false, false, false);
		List<NamedEntity> neList;
		if(Oscar3Props.getInstance().useMEMM) {
			neList = findNEsByMEMM(procDoc);			
		} else {
			neList = findNEsByPatterns(procDoc);
		}
		reportChemicals(neList, safDoc.getRootElement());
		wallOffExperimentalData(procDoc, safDoc.getRootElement());
	}
	
	public List<NamedEntity> findNEsByPatterns(ProcessingDocument procDoc) throws Exception {
	 	List<NamedEntity> stopNeList;

		//String text = doc.getValue();
		
	 	List<NamedEntity> neList = new ArrayList<NamedEntity>();
		
	 	Map<Integer,Token> tokensByStart = new HashMap<Integer,Token>();
	 	Map<Integer,Token> tokensByEnd = new HashMap<Integer,Token>();
		
		for(TokenSequence t : procDoc.getTokenSequences()) {
			neList.addAll(DFANEFinder.getInstance().getNEs(t));
		}
				
		// Make sure all NEs at a position share their ontIds
		Map<String,Set<String>> ontIdsForNePos = new HashMap<String,Set<String>>(); 
		Map<String,Set<String>> custTypesForNePos = new HashMap<String,Set<String>>(); 
		for(NamedEntity ne : neList) {
			String posStr = ne.getStart() + ":" + ne.getEnd();
			Set<String> ontIds = ne.getOntIds();
			if(ontIds != null) {
				if(ontIdsForNePos.containsKey(posStr)) {
					ontIdsForNePos.get(posStr).addAll(ontIds);
				} else {
					ontIdsForNePos.put(posStr, new HashSet<String>(ontIds));
				}
			}
			Set<String> custTypes = ne.getCustTypes();
			if(custTypes != null) {
				if(custTypesForNePos.containsKey(posStr)) {
					custTypesForNePos.get(posStr).addAll(custTypes);
				} else {
					custTypesForNePos.put(posStr, new HashSet<String>(custTypes));
				}				
			}
		}
		
		List<NamedEntity> preserveNes = new ArrayList<NamedEntity>();

		for(NamedEntity ne : neList) {
			if(NETypes.ONTOLOGY.equals(ne.getType()) || NETypes.LOCANTPREFIX.equals(ne.getType()) || NETypes.CUSTOM.equals(ne.getType())) {
				preserveNes.add(ne);
			}
			String posStr = ne.getStart() + ":" + ne.getEnd();
			Set<String> ontIds = ontIdsForNePos.get(posStr);
			if(ontIds != null) ne.setOntIds(ontIds);
			Set<String> custTypes = custTypesForNePos.get(posStr);
			if(custTypes != null) ne.setCustTypes(custTypes);
		}
		
		List<ResolvableStandoff> rsList = StandoffResolver.resolveStandoffs(neList);
		neList.clear();
		for(ResolvableStandoff rs : rsList) {
			neList.add((NamedEntity)rs);
		}
		
		//for(NamedEntity ne : neList) System.out.println(ne);
		
		/*
		Collections.sort(neList, new NEComparator());
		
		// Filter NEs
		if(neList.size() > 0) {
			NamedEntity activeNe = neList.get(0);
			int j = 1;
			while(j < neList.size()) {
				if(activeNe.overlapsWith(neList.get(j))) {
					neList.remove(j);
				} else {
					activeNe = neList.get(j);
					j++;
				}
			}
		}*/
		
		Map<String,String> acroMap = new HashMap<String,String>();
		
		Map<Integer,NamedEntity> endToNe = new HashMap<Integer,NamedEntity>();
		Map<Integer,NamedEntity> startToNe = new HashMap<Integer,NamedEntity>();
		
		for(NamedEntity ne : neList) {
			endToNe.put(ne.getEnd(), ne);
			startToNe.put(ne.getStart(), ne);
		}
		
		// Potential acronyms
		for(NamedEntity ne : neList) {
			if(ne.getType().equals(NETypes.POTENTIALACRONYM)) {
				int start = ne.getStart();
				//int end = ne.getEnd();
				
				Token t = tokensByStart.get(start);
				//if(t != null) System.out.println("AHA: " + t.getValue());
				if(t != null && t.getNAfter(-2) != null && t.getNAfter(1) != null) {
					Token prev = t.getNAfter(-1);
					Token next = t.getNAfter(1);
					Token prevPrev = t.getNAfter(-2);
					if(prev.getValue().equals("(") && next.getValue().endsWith(")")) {
						//boolean matched = false;
						if(endToNe.containsKey(prevPrev.getEnd())) {
							NamedEntity acronymOf = endToNe.get(prevPrev.getEnd());
							if(StringTools.testForAcronym(ne.getSurface(), acronymOf.getSurface())) {
								//System.out.println(ne.getSurface() + " is " + acronymOf.getSurface());
								if(acronymOf.getType().equals(NETypes.ASE) || acronymOf.getType().equals(NETypes.ASES)) {
									//System.out.println("Skip ASE acronym");
								} else {
									//matched = true;
									if (acroMap.containsKey(ne.getSurface())) {
										String newValue = ne.getType();
										String oldValue = acroMap.get(ne.getSurface());
										if (newValue == NETypes.POLYMER) acroMap.put(ne.getSurface(), acronymOf.getType());
										else if (newValue == NETypes.COMPOUND && !oldValue.equals(NETypes.POLYMER)) acroMap.put(ne.getSurface(), acronymOf.getType());
									}
									else {
										acroMap.put(ne.getSurface(), acronymOf.getType());
									}
								}
							}							
						}
					}
				}
				
				/*int index = neList.indexOf(ne);
				if(index == 0) continue;
				NamedEntity previous = neList.get(index-1);
				int prevEnd = previous.getEnd();
				String inBetween = text.substring(prevEnd, start);
				try {
					String afterWards = text.substring(end);
					if(afterWards != null && afterWards.length() > 0 && 
							inBetween.matches("\\s*\\(\\s*") && 
							afterWards.startsWith(")") && 
							StringTools.testForAcronym(ne.getSurface(), previous.getSurface())) {
						System.out.println(ne.getSurface() + " is " + previous.getSurface());
						if(previous.getType(this).equals(NETypes.ASE) || previous.getType(this).equals(NETypes.ASES)) {
							System.out.println("Skip ASE acronym");
						} else {
							acroMap.put(ne.getSurface(), previous.getType(this));							
						}
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}*/
				
			}
		}
		
		stopNeList = new ArrayList<NamedEntity>();
		
		int i = 0;
		while(i < neList.size()) {
			NamedEntity ne = neList.get(i);
			if(ne.getType().equals(NETypes.POTENTIALACRONYM)) {
				if(acroMap.containsKey(ne.getSurface())) {
					ne.setType(acroMap.get(ne.getSurface()));
					i++;
				} else {
					neList.remove(i);
				}
			} else if(ne.getType().equals(NETypes.STOP)) {
				//System.out.println("STOP: " + neList.get(i).getSurface());
				neList.remove(i);
				stopNeList.add(ne);
			} else {
				i++;
			}
		}

		// Some CPRs and ONTs will have been lost in the stopwording process
		// Re-introduce them, and do the resolution process again
		//for(NamedEntity ne : preserveNes) System.out.println(ne);
		neList.addAll(preserveNes);
		setPseudoConfidences(neList);
		rsList = StandoffResolver.resolveStandoffs(neList);
		neList.clear();
		for(ResolvableStandoff rs : rsList) {
			neList.add((NamedEntity)rs);
		}
		
		return neList;
	}

	public List<NamedEntity> findNEsByMEMM(ProcessingDocument procDoc) throws Exception {
	 	//String text = doc.getValue();
		
		List<NamedEntity> neList = new ArrayList<NamedEntity>();
		
		Map<Integer,Token> tokensByStart = new HashMap<Integer,Token>();
		Map<Integer,Token> tokensByEnd = new HashMap<Integer,Token>();
		
		for(TokenSequence t : procDoc.getTokenSequences()) {
			neList.addAll(MEMMSingleton.getInstance().findNEs(t, null).keySet());
		}
		if(Oscar3Props.getInstance().rescoreMEMM) MEMMSingleton.getInstance().rescore(neList);
		
		List<NamedEntity> filteredNeList = new ArrayList<NamedEntity>();
		for(NamedEntity ne : neList) {
			if(ne.getConfidence() > Oscar3Props.getInstance().neThreshold) {
				filteredNeList.add(ne);
			}
		}
		neList = filteredNeList;
		
		
		for(TokenSequence t : procDoc.getTokenSequences()) {
			neList.addAll(DFAONTCPRFinder.getInstance().getNEs(t));
		}

		
		// Make sure all NEs at a position share their ontIds and custTypes
		Map<String,Set<String>> ontIdsForNePos = new HashMap<String,Set<String>>(); 
		Map<String,Set<String>> custTypesForNePos = new HashMap<String,Set<String>>(); 
		for(NamedEntity ne : neList) {
			String posStr = ne.getStart() + ":" + ne.getEnd();
			Set<String> ontIds = ne.getOntIds();
			if(ontIds != null) {
				if(ontIdsForNePos.containsKey(posStr)) {
					ontIdsForNePos.get(posStr).addAll(ontIds);
				} else {
					ontIdsForNePos.put(posStr, new HashSet<String>(ontIds));
				}				
			}
			Set<String> custTypes = ne.getCustTypes();
			if(custTypes != null) {
				if(custTypesForNePos.containsKey(posStr)) {
					custTypesForNePos.get(posStr).addAll(custTypes);
				} else {
					custTypesForNePos.put(posStr, new HashSet<String>(custTypes));
				}
			}
		}
		
		for(NamedEntity ne : neList) {
			String posStr = ne.getStart() + ":" + ne.getEnd();
			Set<String> ontIds = ontIdsForNePos.get(posStr);
			if(ontIds != null) ne.setOntIds(ontIds);
			Set<String> custTypes = custTypesForNePos.get(posStr);
			if(custTypes != null) ne.setCustTypes(custTypes);
		}
		
		setPseudoConfidences(neList);
		List<ResolvableStandoff> rsList = StandoffResolver.resolveStandoffs(neList);
		for(NamedEntity ne : neList) {
			ne.setBlocked(true);
		}
		for(ResolvableStandoff rs : rsList) {
			((NamedEntity)rs).setBlocked(false);
		}
		
		return neList;
	}
	
	public void setPseudoConfidences(List<NamedEntity> neList) {
		for(NamedEntity ne : neList) {
			double pseudoConf = Double.NaN;
			String type = ne.getType();
			if(type.equals(NETypes.ONTOLOGY)) pseudoConf = Oscar3Props.getInstance().ontProb;
			if(type.equals(NETypes.LOCANTPREFIX)) pseudoConf = Oscar3Props.getInstance().cprProb;
			if(type.equals(NETypes.CUSTOM)) pseudoConf = Oscar3Props.getInstance().custProb;
			ne.setPseudoConfidence(pseudoConf);
			ne.setPriorityOnt(!Oscar3Props.getInstance().deprioritiseONT);
		}
	}
	
	public void reportChemicals(List<NamedEntity> neList, Element safHolder) {
		for(NamedEntity ne : neList) {
			if(ne.isEmpty()) continue;
			safHolder.appendChild(ne.toSAF());
		}
	}
	
	//TODO: Make this work again.
	void wallOffExperimentalData(ProcessingDocument procDoc, Element safHolder) throws Exception {
		Nodes n = getExperimentalParagraphs(procDoc);
		for(int i=0;i<n.size();i++) {
			wallOffExperimentalDataInPara(procDoc, safHolder, (Element)n.get(i));
		}
	}
	
	Nodes getExperimentalParagraphs(ProcessingDocument procDoc) {
		if(Oscar3Props.getInstance().dataOnlyInExperimental) {
			return procDoc.getDoc().query(XMLStrings.getInstance().EXPERIMENTAL_PARAS_XPATH, XMLStrings.getInstance().getXpc());
		} else {
			return procDoc.getDoc().query(XMLStrings.getInstance().ALL_PARAS_XPATH, XMLStrings.getInstance().getXpc());
		}
	}

	void wallOffExperimentalDataInPara(ProcessingDocument procDoc, Element safHolder, Element e) throws Exception {
		Node prevNode = XOMTools.getPreviousSibling(e);
		while(prevNode instanceof Text) prevNode = XOMTools.getPreviousSibling(prevNode);
		if(prevNode instanceof Element) {
			Element prevElem = (Element)prevNode;
			if(prevElem.getLocalName().equals(XMLStrings.getInstance().HEADER)) {
				String s = prevElem.getValue();
				if(s.matches("(X-[Rr]ay|Crystal).*data.*")) {
					int start = Integer.parseInt(e.getAttributeValue("xtspanstart"));
					int end = Integer.parseInt(e.getAttributeValue("xtspanend"));
					putDatasectionIntoSaf(procDoc, safHolder, start, end);
					return;
				}
			}
		}
		double d = getWordFraction(e);
		if(d < 0.1) {
			int start = Integer.parseInt(e.getAttributeValue("xtspanstart"));
			int end = Integer.parseInt(e.getAttributeValue("xtspanend"));
			putDatasectionIntoSaf(procDoc, safHolder, start, end);
			return;			
		}
		TokenSequence tokSeq = null;
		for(TokenSequence t : procDoc.getTokenSequences()) {
			if(t.getElem() == e) tokSeq = t;
		}
		int dataSectionStartOffset = -1;
		if(tokSeq != null) {
			for(Token t : tokSeq.getTokens()) {
				//System.out.println(t.getValue());
				Matcher m = dataStartPattern.matcher(t.getValue());
				if(m.matches() || (t.getValue().matches("1H|13C") && t.getNAfter(1) != null && t.getNAfter(1).getValue().equals("NMR"))) {
					double checkVal = checkCut(tokSeq, t.getId());
					//System.out.println(checkVal);
					if(checkVal < 0.3) {
						dataSectionStartOffset = t.getStart();
						break;						
					}
				}
				if(t.getValue().equalsIgnoreCase("found") && t.getNAfter(-1) != null
						&& t.getNAfter(-1).getValue().matches("\\(|\\[|\\{")) {
					double checkVal = checkCut(tokSeq, t.getId()-1);
					//System.out.println(checkVal);
					if(checkVal < 0.3) {
						dataSectionStartOffset = t.getNAfter(-1).getStart();
						break;						
					}					
				}
			}
		}
		/*Nodes n = e.query(".//property[@type=\"elemAnal\"]|" +
				".//property[@type=\"hrms\"]|" +
				".//property[@type=\"mp\"]|" +
				".//property[@type=\"bp\"]|" +
				".//property[@type=\"optRot\"]|" +
				".//property[@type=\"rf\"]|" +
				".//property[@type=\"refractiveindex\"]|" +
				".//spectrum");
		if(n.size() == 0) return;*/
		if(dataSectionStartOffset != -1) {
			int start = dataSectionStartOffset;
			int end = Integer.parseInt(e.getAttributeValue("xtspanend"));
			putDatasectionIntoSaf(procDoc, safHolder, start, end);			
		}
	}
	
	private void putDatasectionIntoSaf(ProcessingDocument procDoc, Element safHolder, int start, int end) {
		if(start == end) return;
		Element safElem = SafTools.makeAnnot(procDoc.getStandoffTable().getLeftPointAtOffset(start), procDoc.getStandoffTable().getRightPointAtOffset(end), "oscar");
		SafTools.setSlot(safElem, "type", "dataSection");
		safHolder.appendChild(safElem);
	}
	
	private double checkCut(TokenSequence t, int breakID) {
		int wordsBeforeBreak = 0;
		int wordsAfterBreak = 0;
		int nonWordsBeforeBreak = 0;
		int nonWordsAfterBreak = 0;
		boolean prevIsWord = false;
		for(int i=0;i<t.size();i++) {
			String v = t.getToken(i).getValue();
			boolean isWord = v.matches("a|.*[a-z][aeiou].*|[aeiou][a-z].*|cm3");
			if(isWord) {
				if(i < breakID) wordsBeforeBreak++; else wordsAfterBreak++;
				//if(prevIsWord && i < breakID) wordsBeforeBreak++; else wordsAfterBreak++;
			} else {
				if(i < breakID) nonWordsBeforeBreak++; else nonWordsAfterBreak++;				
			}
			prevIsWord = isWord;
		}
		double fractionAfterBreak = wordsAfterBreak / (0.0 + wordsAfterBreak + nonWordsAfterBreak);
		return fractionAfterBreak;
	}
	
	/** Gets a fraction of word-like objects in an element. Word-like
	 * is defined as all lower-case, with at least one vowel.
	 * 
	 * @param e The element to examine
	 * @return The fraction, on a scale of 0.0 to 1.0. Less than 10 words -> 1.0
	 */
	double getWordFraction(Element e) {
		String s = e.getValue();
		double words = 0; 
		double nonWords = 0;
		Pattern p = Pattern.compile("\\S+");
		Matcher m = p.matcher(s);
		while(m.find()) {
			if(m.group().matches("[a-z]*[aeiou][a-z]*")) words++; else nonWords++;
		}
		if(words+nonWords < 10) return 1.0;
		return words / (words + nonWords);
	}
	
	
}
