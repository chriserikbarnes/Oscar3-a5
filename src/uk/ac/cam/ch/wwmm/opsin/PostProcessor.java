package uk.ac.cam.ch.wwmm.opsin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;
import uk.ac.cam.ch.wwmm.ptclib.xml.XOMTools;

import nu.xom.Attribute;
import nu.xom.Element;
import nu.xom.Elements;
import nu.xom.Node;
import nu.xom.Nodes;

/**Does destructive procedural parsing on parser results.
 *
 * @author ptc24/dl387
 *
 */
class PostProcessor {

	/**
	 * Sort bridges such as the highest priority secondary bridges come first
	 * e.g. 1^(6,3).1^(15,13)
	 * rearranged to 1^(15,13).1^(6,3)
	 * @author dl387
	 *
	 */
	private class VonBaeyerSecondaryBridgeSort implements Comparator<HashMap<String, Integer>> {

	    public int compare(HashMap<String, Integer> bridge1, HashMap<String, Integer> bridge2){
	    	//first we compare the larger coordinate, due to an earlier potential swapping of coordinates this is always in  "AtomId_Larger"
	    	int largerCoordinate1 = bridge1.get("AtomId_Larger");
	    	int largerCoordinate2 = bridge2.get("AtomId_Larger");
			if (largerCoordinate1 >largerCoordinate2) {
				return -1;
			}
			else if (largerCoordinate2 >largerCoordinate1) {
				return 1;
			}
			//tie
	    	int smallerCoordinate1 = bridge1.get("AtomId_Smaller");
	    	int smallerCoordinate2 = bridge2.get("AtomId_Smaller");
			if (smallerCoordinate1 >smallerCoordinate2) {
				return -1;
			}
			else if (smallerCoordinate2 >smallerCoordinate1) {
				return 1;
			}
			//tie
	    	int bridgelength1 = bridge1.get("Bridge Length");
	    	int bridgelength2 = bridge2.get("Bridge Length");
			if (bridgelength1 >bridgelength2) {
				return -1;
			}
			else if (bridgelength2 >bridgelength1) {
				return 1;
			}
			else{
				return 0;
			}
	    }
	}

	//match a fusion bracket with only numerical locants. If this is followed by a HW group it probably wasn't a fusion bracket
	private Pattern matchNumberLocantsOnlyFusionBracket = Pattern.compile("\\[\\d+[a-z]?(,\\d+[a-z]?)*\\]");
	private Pattern matchVonBaeyer = Pattern.compile("([0-9]+\\^?[\\({]?[0-9]*,?[0-9]*[\\)}]?\\^?\\^?)");
	private Pattern matchSpiro = Pattern.compile("spiro\\[([0-9]+)\\.([0-9]+)\\]");
	private Pattern matchAnnulene = Pattern.compile("\\[([1-9][0-9]*)\\]annulen");
	private NameToStructure n2s;

	PostProcessor(NameToStructure parent) {
		n2s=parent;
	}

	/** The master method, postprocesses a parse result.
	 *
	 * @param elem The element to postprocess.
	 * @return The postprocessed element. The same as elem.
	 * @throws Exception
	 */
	Element postProcess(Element elem) throws Exception {
		/*For clarity removes tags which represent letters such as e and o which have no effect on meaning */
		removeMeaninglessTags(elem);
		/* Throws exceptions for occurrences that are ambiguous and this parse has picked the incorrect interpretation */
		resolveAmbiguities(elem);

		Nodes substituentAndRootNodes = XQueryUtil.xquery(elem,".//substituent|.//root");
		ArrayList<Element> substituentsAndRoot =  OpsinTools.nodesToElementArrayList(substituentAndRootNodes);

		for (Element subOrRoot: substituentsAndRoot) {
			processHeterogenousHydrides(subOrRoot);
			processIndicatedHydrogens(subOrRoot);
			processStereochemistry(subOrRoot);
		}
		ArrayList<Element> groups =  OpsinTools.nodesToElementArrayList(XQueryUtil.xquery(elem,".//group"));

		processAnnulenes(elem);
		for (Element group : groups) {
			processRings(group);//processes cyclo, von baeyer and simple spiro tokens
			handleIrregularities(group);//handles benzyl
		}

		/* Converts open/close bracket elements to bracket elements and
		 *  places the elements inbetween within the newly created bracket */
		while(findAndStructureBrackets(substituentAndRootNodes));
		
		for (Element group : groups) {
			processHydroSubstituents(group);//this REMOVES hydro substituents
		}

		return elem;
	}

	/**
	 * Removes elements that have no meaning associated with them. These elements such as <e>e</e> are typically either optional
	 * or in the case of <o>o</o> allowed assignment of a group as a fusion ring.
	 * @param elem
	 */
	private void removeMeaninglessTags(Element elem){
		Nodes nodes =XQueryUtil.xquery(elem, ".//e|.//o|.//optionalOpenBracket|.//optionalCloseBracket");
		for (int i = 0; i < nodes.size(); i++) {
			nodes.get(i).detach();
		}
	}

	/**
	 * Resolves common ambiguities e.g. tetradeca being 4x10carbon chain rather than 14carbon chain
	 * @param elem
	 * @throws PostProcessingException
	 */
	private void resolveAmbiguities(Element elem) throws PostProcessingException {
		Nodes nodes =XQueryUtil.xquery(elem, ".//multiplier");
		for (int i = 0; i < nodes.size(); i++) {
			Node apparentMultiplier = nodes.get(i);
			Element nextEl = (Element)XOMTools.getNextSibling(apparentMultiplier);
			if(nextEl !=null && nextEl.getLocalName().equals("group")){
				String multiplierAndGroup =((Element)apparentMultiplier).getValue() + nextEl.getValue();
				HashMap<String, HashMap<Character, Token>> tokenDict =n2s.parser.tokenManager.tokenDict;
				HashMap<Character,Token> tokenMap = tokenDict.get(multiplierAndGroup);
				if (tokenMap !=null){
					Element isThisALocant =(Element)XOMTools.getPreviousSibling(apparentMultiplier);
					if (isThisALocant == null ||
							!isThisALocant.getLocalName().equals("locant") ||
							isThisALocant.getValue().split(",").length < 3){
						throw new PostProcessingException(multiplierAndGroup +" should not have been lexed as two tokens!");
					}
				}
			}
		}

		nodes =XQueryUtil.xquery(elem, ".//fusion");
		for (int i = 0; i < nodes.size(); i++) {
			Node fusion = nodes.get(i);
			if (matchNumberLocantsOnlyFusionBracket.matcher(fusion.getValue()).matches()){
				Element nextElement = (Element)XOMTools.getNextSibling(fusion);
				while(nextElement != null) {
					if(nextElement.getLocalName().equals("group")){
						if (nextElement.getAttributeValue("subType").equals("hantzschWidman")){
							throw new PostProcessingException("This fusion bracket is in fact more likely to be a description of the locants of a HW ring");
						}
						else{
							break;
						}
					}
					nextElement= (Element)XOMTools.getNextSibling(nextElement);
				}
			}
		}
	}

	/**Form heterogeneous hydrides/substituents
	 * Note that the heteroStems are always single atom SMILES
	 * @param elem The root/brackets
	 * @throws PostProcessingException If there is a disagreement.
	 */
	private void processHeterogenousHydrides(Element elem) throws PostProcessingException {
		Elements multipliers = elem.getChildElements("multiplier");
		for(int i=0;i<multipliers.size();i++) {
			Element m = multipliers.get(i);
			Element multipliedElem = (Element)XOMTools.getNextSibling(m);
			Element possiblyALocant = (Element)XOMTools.getPreviousSibling(m);
			if(possiblyALocant !=null && possiblyALocant.getLocalName().equals("locant")){continue;}
			if(multipliedElem.getLocalName().equals("group") &&
					multipliedElem.getAttribute("subType")!=null &&
					multipliedElem.getAttributeValue("subType").equals("heteroStem")) {
				int mvalue = Integer.parseInt(m.getAttributeValue("value"));
				//chain of heteroatoms
				String smiles=multipliedElem.getAttributeValue("value");
				multipliedElem.getAttribute("value").setValue(StringTools.multiplyString(smiles, mvalue));
				m.detach();
			}
		}
		Elements groups = elem.getChildElements("group");

		if (groups.size()==0){
			for(int i=0;i<multipliers.size();i++) {
				Element m = multipliers.get(i);
				Element multipliedElem = (Element)XOMTools.getNextSibling(m);
				if(multipliedElem.getLocalName().equals("heteroatom")){
					Element possiblyAnotherHeteroAtom = (Element)XOMTools.getNextSibling(multipliedElem);
					if (possiblyAnotherHeteroAtom !=null && possiblyAnotherHeteroAtom.getLocalName().equals("heteroatom")){
						Element possiblyAnUnsaturator = (Element)XOMTools.getNextSibling(possiblyAnotherHeteroAtom);
						//I would expect the unsaturator to always be ane, but the structure would not be ambiguous if it was ene/yne e.g. disilazene
						if (possiblyAnUnsaturator !=null && possiblyAnUnsaturator.getLocalName().equals("unsaturator")){
							//chain of alternating heteroatoms
							int mvalue = Integer.parseInt(m.getAttributeValue("value"));
							String smiles="";
							Element possiblyARingFormingEl = (Element)XOMTools.getPreviousSibling(m);
							if (possiblyARingFormingEl!=null && (possiblyARingFormingEl.getLocalName().equals("cyclo") || possiblyARingFormingEl.getLocalName().equals("vonBaeyer") || possiblyARingFormingEl.getLocalName().equals("spiro"))){
								//will be cyclised later.
								//FIXME sort based on order in HW system (also add check to HW stuff that the heteroatoms in that are in the correct order so that incorrectly ordered systems may be rejected.
								for (int j = 0; j < mvalue; j++) {
									smiles+=possiblyAnotherHeteroAtom.getAttributeValue("value");
									smiles+=multipliedElem.getAttributeValue("value");
								}
							}
							else{
								for (int j = 0; j < mvalue -1; j++) {
									smiles+=multipliedElem.getAttributeValue("value");
									smiles+=possiblyAnotherHeteroAtom.getAttributeValue("value");
								}
								smiles+=multipliedElem.getAttributeValue("value");
							}
							multipliedElem.detach();

							Element addedGroup=new Element("group");
							addedGroup.addAttribute(new Attribute("value", smiles));
							addedGroup.addAttribute(new Attribute("valType", "SMILES"));
							addedGroup.addAttribute(new Attribute("type", "chain"));
							addedGroup.addAttribute(new Attribute("subType", "heteroStem"));
							if (possiblyARingFormingEl!=null && (possiblyARingFormingEl.getLocalName().equals("cyclo") || possiblyARingFormingEl.getLocalName().equals("vonBaeyer") || possiblyARingFormingEl.getLocalName().equals("spiro"))){
								addedGroup.addAttribute(new Attribute("usableAsAJoiner", "yes"));
							}
							addedGroup.appendChild(smiles);
							XOMTools.insertAfter(possiblyAnotherHeteroAtom, addedGroup);

							possiblyAnotherHeteroAtom.detach();
							m.detach();
						}
					}
				}
			}
		}
	}

	/** Handle 1H- in 1H-pyrrole etc.
	 *
	 * @param elem The substituent/root to looks for indicated hydrogens in.
	 */
	private void processIndicatedHydrogens(Element elem) {
		Elements hydrogens = elem.getChildElements("hydrogen");
		for(int i=0;i<hydrogens.size();i++) {
			Element hydrogen = hydrogens.get(i);
			String txt = hydrogen.getChild(0).getValue();
			String[] hydrogenLocants =txt.split(",");
			for (int j = 0; j < hydrogenLocants.length; j++) {
				if(hydrogenLocants[j].endsWith("H-")) {
					Element newHydrogenElement =new Element("hydrogen");
					newHydrogenElement.addAttribute(new Attribute("locant", hydrogenLocants[j].substring(0, hydrogenLocants[j].length()-2)));
					XOMTools.insertAfter(hydrogen, newHydrogenElement);
				}
				else if(hydrogenLocants[j].endsWith("H")) {
					Element newHydrogenElement =new Element("hydrogen");
					newHydrogenElement.addAttribute(new Attribute("locant", hydrogenLocants[j].substring(0, hydrogenLocants[j].length()-1)));
					XOMTools.insertAfter(hydrogen, newHydrogenElement);
				}
			}
			hydrogen.detach();
		}
	}

	/** Handles stereoChemistry (R/S currently)
	 *
	 * @param elem The substituent/root to looks for stereoChemistry in.
	 * @throws PostProcessingException
	 */
	private void processStereochemistry(Element elem) throws PostProcessingException {
		Elements stereoChemistryElements = elem.getChildElements("stereoChemistry");
		for(int i=0;i<stereoChemistryElements.size();i++) {
			Element stereoChemistryElement = stereoChemistryElements.get(i);
			if (stereoChemistryElement.getAttributeValue("type").equals("stereochemistryBracket")){
				String txt = stereoChemistryElement.getValue();
				if (txt.startsWith("rel-") || txt.contains("*")){
					//currently unsupported
				}
				else{
					txt =txt.substring(1, txt.length()-1);//remove opening and closing bracket.
					String[] stereoChemistryDescriptors =txt.split(",");
					for (int j = 0; j < stereoChemistryDescriptors.length; j++) {
						String stereoChemistryDescriptor =stereoChemistryDescriptors[j];
						if (stereoChemistryDescriptor.length()>1){
							if (stereoChemistryDescriptor.contains("E") || stereoChemistryDescriptor.contains("Z")){
								//currently unsupported
							}
							else if (stereoChemistryDescriptor.contains("R") || stereoChemistryDescriptor.contains("S")){
								Element stereoChemEl =new Element("stereoChemistry");
								stereoChemEl.addAttribute(new Attribute("type", "RorS"));
								stereoChemEl.addAttribute(new Attribute("locant", stereoChemistryDescriptor.substring(0, stereoChemistryDescriptor.length()-1)));
								stereoChemEl.addAttribute(new Attribute("value", stereoChemistryDescriptor.substring(stereoChemistryDescriptor.length()-1, stereoChemistryDescriptor.length())));
								stereoChemEl.appendChild(stereoChemistryDescriptor);
								XOMTools.insertAfter(stereoChemistryElement, stereoChemEl);
							}
							else{
								throw new PostProcessingException("Malformed stereochemistry element: " + stereoChemistryElement.getValue());
							}
						}
						else{
							//currently unsupported
						}
					}
				}
				stereoChemistryElement.detach();
			}
		}
	}

	/**Looks for annulen tags and replaces them with a group with appropriate SMILES.
	 * @param elem The element to look for annulen tags in
	 * @throws PostProcessingException
	 */
	private void processAnnulenes(Element elem) throws PostProcessingException {
		Nodes annulens = XQueryUtil.xquery(elem, ".//annulen");
		for (int i = 0; i < annulens.size(); i++) {
			Element annulen =(Element)annulens.get(i);
			String annulenValue =annulen.getValue();
	        Matcher match = matchAnnulene.matcher(annulenValue);
	        match.matches();
	        if (match.groupCount() !=1){
	        	throw new PostProcessingException("Invalid annulen tag");
	        }

	        int annulenSize=Integer.valueOf(match.group(1));
	        if (annulenSize <3){
	        	throw new PostProcessingException("Invalid annulen tag");
	        }

			//build [annulenSize]annulene ring as SMILES
			String SMILES="C1";
			int counter =(annulenSize -1)/2;
			for (int a = 0; a < counter; a++) {
				 SMILES +="=CC";
			}
			if (annulenSize %2 ==0){
				SMILES +="=C";
			}
			SMILES +="1";

			Element group =new Element("group");
			group.addAttribute(new Attribute("value", SMILES));
			group.addAttribute(new Attribute("valType", "SMILES"));
			group.addAttribute(new Attribute("type", "ring"));
			group.addAttribute(new Attribute("subType", "annulene"));
			group.appendChild(annulenValue);
			annulen.getParent().replaceChild(annulen, group);
		}
	}



	/**Looks (multiplier)cyclo/spiro/cyclo tags before chain
	 * and replaces them with a group with appropriate SMILES
	 * Note that only simple spiro tags are handled at this stage i.e. not dispiro
	 * @param elem A group which is potentially a chain
	 * @throws PostProcessingException
	 */
	private void processRings(Element group) throws PostProcessingException {
		Element previous = (Element)XOMTools.getPreviousSibling(group);
		if(previous != null) {
			if(previous.getLocalName().equals("spiro")) {
				//int chainlen = Integer.parseInt(group.getAttributeValue("value"));
				Matcher m = matchSpiro.matcher(previous.getChild(0).getValue());
				m.matches();
				int shortLen = Integer.parseInt(m.group(1));
				int longLen = Integer.parseInt(m.group(2));
				// TODO check chainlen vs shortLen+longLen+1;
				String SMILES = "C1" + StringTools.multiplyString("C", shortLen) + "11" + StringTools.multiplyString("C", longLen) + "1";
				group.addAttribute(new Attribute("value", SMILES));
				group.addAttribute(new Attribute("valType", "SMILES"));
				group.getAttribute("type").setValue("ring");
				previous.detach();
			} else if(previous.getLocalName().equals("vonBaeyer")) {
				String vonBaeyerBracket = previous.getValue();
				Matcher m = matchVonBaeyer.matcher(vonBaeyerBracket);

				Element multiplier =(Element)XOMTools.getPreviousSibling(previous);
				int numberOfRings=Integer.parseInt(multiplier.getAttributeValue("value"));
				multiplier.detach();
				
				int alkylChainLength;
				LinkedList<String> elementSymbolArray = new LinkedList<String>();
				if (group.getAttributeValue("valType").equals("chain")){
					alkylChainLength=Integer.parseInt(group.getAttributeValue("value"));
					for (int i = 0; i < alkylChainLength; i++) {
						elementSymbolArray.add("C");
					}
				}
				else if (group.getAttributeValue("valType").equals("SMILES")){
					String smiles =group.getAttributeValue("value");
					char[] smilesArray =smiles.toCharArray();
					for (int i = 0; i < smilesArray.length; i++) {//only able to interpret the SMILES that should be in an unmodified unbranched chain
						char currentChar =smilesArray[i];
						if (currentChar == '['){
							if ( smilesArray[i +2]==']'){
								elementSymbolArray.add("[" +String.valueOf(smilesArray[i+1]) +"]");
								i=i+2;
							}
							else{
								elementSymbolArray.add("[" + String.valueOf(smilesArray[i+1]) +String.valueOf(smilesArray[i+2]) +"]");
								i=i+3;
							}
						}
						else{
							elementSymbolArray.add(String.valueOf(currentChar));
						}
					}
					smiles.split("]");
					alkylChainLength=elementSymbolArray.size();
				}
				else{
					throw new PostProcessingException("unexpected group valType: " + group.getAttributeValue("valType"));
				}
				

				int totalLengthOfBridges=0;
				int bridgeLabelsUsed=3;//start labelling from 3 upwards
				//3 and 4 will the atoms on each end of one secondary bridge, 5 and 6 for the next etc.

				ArrayList<HashMap<String, Integer>> bridges = new ArrayList<HashMap<String, Integer>>();
				HashMap<Integer, ArrayList<Integer>> bridgeLocations = new HashMap<Integer, ArrayList<Integer>>(alkylChainLength);
				while(m.find()) {
					String[] lengthOfBridgeArray=m.group(0).split(",");
					HashMap<String, Integer> bridge = new HashMap<String, Integer>();
					int bridgeLength =0;
					if (lengthOfBridgeArray.length > 1){//secondary bridge

						String coordinatesStr1;
						String coordinatesStr2 =lengthOfBridgeArray[1].replaceAll("[^0-9]", "");
						String[] tempArray = lengthOfBridgeArray[0].split("[^0-9]+");

						if (tempArray.length ==1){
							//there is some ambiguity as the superscripted locants will not be superscripted
							//so we assume that it is more likely that it will be referring to an atom of label >10
							//rather than a secondary bridge of length > 10
							char[] tempCharArray = lengthOfBridgeArray[0].toCharArray();
							if (tempCharArray.length ==2){
								bridgeLength= Character.getNumericValue(tempCharArray[0]);
								coordinatesStr1= Character.toString(tempCharArray[1]);
							}
							else if (tempCharArray.length ==3){
								bridgeLength= Character.getNumericValue(tempCharArray[0]);
								coordinatesStr1=Character.toString(tempCharArray[1]) +Character.toString(tempCharArray[2]);
							}
							else if (tempCharArray.length ==4){
								bridgeLength = Integer.parseInt(Character.toString(tempCharArray[0]) +Character.toString(tempCharArray[1]));
								coordinatesStr1 = Character.toString(tempCharArray[2]) +Character.toString(tempCharArray[3]);
							}
							else{
								throw new PostProcessingException("Unsupported Von Baeyer locant description: " + m.group(0) );
							}
						}
						else{//bracket detected, no ambiguity!
							bridgeLength= Integer.parseInt(tempArray[0]);
							coordinatesStr1= tempArray[1];
						}

						bridge.put("Bridge Length", bridgeLength );
						int coordinates1=Integer.parseInt(coordinatesStr1);
						int coordinates2=Integer.parseInt(coordinatesStr2);
						if (coordinates1 > alkylChainLength || coordinates2 > alkylChainLength){
							throw new PostProcessingException("Indicated bridge position is not on chain: " +coordinates1 +"," +coordinates2);
						}
						if (coordinates2>coordinates1){//makes sure that bridges are built from highest coord to lowest
							int swap =coordinates1;
							coordinates1=coordinates2;
							coordinates2=swap;
						}
						if (bridgeLocations.get(coordinates1)==null){
							bridgeLocations.put(coordinates1, new ArrayList<Integer>());
						}
						if (bridgeLocations.get(coordinates2)==null){
							bridgeLocations.put(coordinates2, new ArrayList<Integer>());
						}
						bridgeLocations.get(coordinates1).add(bridgeLabelsUsed);
						bridge.put("AtomId_Larger_Label", bridgeLabelsUsed);
						bridgeLabelsUsed++;
						if (bridgeLength==0){//0 length bridge, hence want atoms with the same labels so they can join together without a bridge
							bridgeLocations.get(coordinates2).add(bridgeLabelsUsed -1);
							bridge.put("AtomId_Smaller_Label", bridgeLabelsUsed -1);
						}
						else{
							bridgeLocations.get(coordinates2).add(bridgeLabelsUsed);
							bridge.put("AtomId_Smaller_Label", bridgeLabelsUsed);
						}
						bridgeLabelsUsed++;

						bridge.put("AtomId_Larger", coordinates1);
						bridge.put("AtomId_Smaller", coordinates2);
					}
					else{
						bridgeLength= Integer.parseInt(lengthOfBridgeArray[0]);
						bridge.put("Bridge Length", bridgeLength);
					}
					totalLengthOfBridges += bridgeLength;
					bridges.add(bridge);
				}
				if (totalLengthOfBridges + 2 !=alkylChainLength ){
					throw new PostProcessingException("Disagreement between lengths of bridges and alkyl chain length");
				}
				if (numberOfRings +1 != bridges.size()){
					throw new PostProcessingException("Disagreement between number of rings and number of bridges");
				}

				String SMILES="";
				int atomCounter=1;
				int bridgeCounter=1;
				//add standard bridges
				for (HashMap<String, Integer> bridge : bridges) {
					if (bridgeCounter==1){
						SMILES += elementSymbolArray.removeFirst() +"%1";
						if (bridgeLocations.get(atomCounter)!=null){
							for (Integer bridgeAtomLabel : bridgeLocations.get(atomCounter)) {
								SMILES +="%"+ bridgeAtomLabel;
							}
						}
						SMILES += "(";
					}
					int bridgeLength =bridge.get("Bridge Length");

					for (int i = 0; i < bridgeLength; i++) {
						atomCounter++;
						SMILES +=elementSymbolArray.removeFirst();
						if (bridgeLocations.get(atomCounter)!=null){
							for (Integer bridgeAtomLabel : bridgeLocations.get(atomCounter)) {
								SMILES +="%"+ bridgeAtomLabel;
							}
						}
					}
					if (bridgeCounter==1){
						atomCounter++;
						SMILES += elementSymbolArray.removeFirst() +"%2";
						if (bridgeLocations.get(atomCounter)!=null){
							for (Integer bridgeAtomLabel : bridgeLocations.get(atomCounter)) {
								SMILES +="%"+ bridgeAtomLabel;
							}
						}
					}
					if (bridgeCounter==2){
						SMILES += "%1)";
					}
					if (bridgeCounter==3){
						SMILES += "%2";
					}
					bridgeCounter++;
					if (bridgeCounter >3){break;}
				}

				//create list of secondary bridges that need to be added
				//0 length bridges and the 3 main bridges are dropped
				ArrayList<HashMap<String, Integer>> secondaryBridges = new ArrayList<HashMap<String, Integer>>();
				for (HashMap<String, Integer> bridge : bridges) {
					if(bridge.get("AtomId_Larger")!=null && bridge.get("Bridge Length")!=0){
						secondaryBridges.add(bridge);
					}
				}

				Comparator<HashMap<String, Integer>> sortBridges= new VonBaeyerSecondaryBridgeSort();
				Collections.sort(secondaryBridges, sortBridges);

				ArrayList<HashMap<String, Integer>> dependantSecondaryBridges;
				//add secondary bridges, recursively add dependent secondary bridges
				do{
					dependantSecondaryBridges = new ArrayList<HashMap<String, Integer>>();
					for (HashMap<String, Integer> bridge : secondaryBridges) {
						int bridgeLength =bridge.get("Bridge Length");
						if (bridge.get("AtomId_Larger") > atomCounter){
							dependantSecondaryBridges.add(bridge);
							continue;
						}
						SMILES+=".";
						for (int i = 0; i < bridgeLength; i++) {
							atomCounter++;
							SMILES +=elementSymbolArray.removeFirst();
							if (i==0){SMILES+="%"+ bridge.get("AtomId_Larger_Label");}
							if (bridgeLocations.get(atomCounter)!=null){
								for (Integer bridgeAtomLabel : bridgeLocations.get(atomCounter)) {
									SMILES += "%"+ bridgeAtomLabel;
								}
							}
						}
						SMILES+= "%"+  bridge.get("AtomId_Smaller_Label");
					}
					if (dependantSecondaryBridges.size() >0 && dependantSecondaryBridges.size()==secondaryBridges.size()){
						throw new PostProcessingException("Unable to resolve all dependant bridges!!!");
					}
					secondaryBridges=dependantSecondaryBridges;
				}
				while(dependantSecondaryBridges.size() > 0);

				group.addAttribute(new Attribute("value", SMILES));
				group.addAttribute(new Attribute("valType", "SMILES"));
				group.getAttribute("type").setValue("ring");
				previous.detach();
			}
			else if(previous.getLocalName().equals("cyclo")) {
				if (!group.getAttributeValue("subType").equals("heteroStem")){
					int chainlen = Integer.parseInt(group.getAttributeValue("value"));
					if (chainlen < 3){
						throw new PostProcessingException("Alkane chain too small to create a cyclo alkane: " + chainlen);
					}
					Element next = (Element)XOMTools.getNextSibling(group);
					String SMILES;
					if (next != null && next.getLocalName().equals("fusion") || next.getLocalName().equals("group")){
						//will have conjugated double bonds as is dictated by fusion nomenclature
						SMILES = "C1=" +StringTools.multiplyString("CC=", chainlen/2 -1);
						if (chainlen % 2 !=0){SMILES += "C";}
						SMILES += "C1";
					}
					else{
						SMILES = "C1" + StringTools.multiplyString("C", chainlen - 1) + "1";
					}
					group.addAttribute(new Attribute("value", SMILES));
					group.addAttribute(new Attribute("valType", "SMILES"));
				}
				else{
					String smiles=group.getAttributeValue("value");
					smiles+="1";
					if (Character.isUpperCase(smiles.charAt(1))){//element is 1 letter long
						smiles= smiles.substring(0,1) +"1" + smiles.substring(1);
					}
					else{
						smiles= smiles.substring(0,2) +"1" + smiles.substring(2);
					}
					group.getAttribute("value").setValue(smiles);
				}
				group.getAttribute("type").setValue("ring");
				previous.detach();
			}
		}
	}

	/**Handles special cases in IUPAC nomenclature.
	 * Benzyl etc.
	 * @param elem The group to look for irregularities in.
	 */
	private void handleIrregularities(Element group) throws PostProcessingException {
		String groupValue =group.getValue();
		/* Benzyl - make the -yl special, so that the builder knows to add a methylene */
		if(group.getValue().equals("benz")) {
			Element suffix = (Element)XOMTools.getNextSibling((Node)group);
			if (suffix != null && suffix.getLocalName().equals("suffix")){
				String suffixVal = suffix.getAttributeValue("value");
				if(suffixVal.equals("yl")) {
					suffix.addAttribute(new Attribute("value", "benzyl-yl"));
				} else if(suffixVal.equals("ylidene")) {
					suffix.addAttribute(new Attribute("value", "benzyl-ylidene"));
				} else if(suffixVal.equals("ylidyne")) {
					suffix.addAttribute(new Attribute("value", "benzyl-ylidyne"));
				} else if(suffixVal.equals("oxy")) {
					suffix.addAttribute(new Attribute("value", "benzyloxy-yloxy"));
				}
			}
		}
		/*In the case of carbonate changes the SMILES to C and sets the suffix to the carbonate suffix */
		if (groupValue.equals("carbon")){
			Element suffix =(Element) XOMTools.getNextSibling(group);
			if (suffix !=null && suffix.getValue().equals("ate")){
				group.getAttribute("value").setValue("C");
				suffix.getAttribute("value").setValue("carbonate");
				OpsinTools.setTextChild(suffix, "carbonate");
			}
		}
	}
	
	/**
	 * Converts hydro substituents to properties of the next group of type ring
	 * @param Element group
	 * @throws PostProcessingException 
	 */
	private void processHydroSubstituents(Element group) throws PostProcessingException {
		if (group.getValue().equals("hydro")){
			Element hydroSubstituent =(Element) group.getParent();
			Node next =	OpsinTools.getNextSiblingNonHyphen(hydroSubstituent);
			Element targetRing =null;
			while (next!=null){
				Element potentialRing =((Element)next).getFirstChildElement("group");
				if (potentialRing!=null && potentialRing.getAttributeValue("type").equals("ring")){
					targetRing =potentialRing;
					break;
				}
				else{
					next =	OpsinTools.getNextSiblingNonHyphen(next);
				}
			}
			if (targetRing ==null){
				throw new PostProcessingException("Cannot find ring for hydro substituent to apply to");
			}
			//move the children of the hydro substituent, and change the group tag into a hydro tag
			Elements children =hydroSubstituent.getChildElements();
			for (int i = children.size()-1; i >=0 ; i--) {
				Element child =children.get(i);
				if (!child.getLocalName().equals("hyphen")){
					child.detach();
					if (child.getLocalName().equals("group")){
						child =new Element("hydro");
						child.appendChild("hydro");
					}
					targetRing.getParent().insertChild(child, 0);
				}
			}
			hydroSubstituent.detach();
		}
	}

	/**Finds matching open and close brackets, and places the
	 * elements contained within in a big &lt;bracket&gt; element.
	 *
	 * @param substituentsAndRoot: The substituent/root elements at the current level of the tree
	 * @return Whether the method did something, and so needs to be called again.
	 * @throws PostProcessingException
	 */
	private boolean findAndStructureBrackets(Nodes substituentsAndRoot) throws PostProcessingException {
		int blevel = 0;
		Element openBracket = null;
		Element closeBracket = null;
		for(int i=0;i<substituentsAndRoot.size();i++) {
			Element sub = (Element)substituentsAndRoot.get(i);
			Elements children = sub.getChildElements();
			for(int j=0;j<children.size();j++) {
				Element child = children.get(j);
				if(child.getLocalName().equals("openbracket")) {
					if(openBracket == null) {
						openBracket = child;
					}
					blevel++;
				} else if (child.getLocalName().equals("closebracket")) {
					blevel--;
					if(blevel == 0) {
						closeBracket = child;
						Element bracket = structureBrackets(openBracket, closeBracket);
						while(findAndStructureBrackets(XQueryUtil.xquery(bracket, ".//substituent")));
						return true;
					}
				}
			}
		}
		if (blevel!=0){
			throw new PostProcessingException("Matching closing bracket not found!");
		}
		return false;
	}

	/**Places the elements in substituents containing/between an open and close bracket
	 * in a &lt;bracket&gt; tag.
	 *
	 * @param openBracket The open bracket element
	 * @param closeBracket The close bracket element
	 * @return The bracket element thus created.
	 */
	private Element structureBrackets(Element openBracket, Element closeBracket) {
		Element bracket = new Element("bracket");
		XOMTools.insertBefore(openBracket.getParent(), bracket);
		/* Pick up everything in the substituent before the bracket*/
		while(!openBracket.getParent().getChild(0).equals(openBracket)) {
			Node n = openBracket.getParent().getChild(0);
			n.detach();
			bracket.appendChild(n);
		}
		/* Pick up all nodes from the one with the open bracket,
		 * to the one with the close bracket, inclusive.
		 */
		Node currentNode = openBracket.getParent();
		while(!currentNode.equals(closeBracket.getParent())) {
			Node nextNode = XOMTools.getNextSibling(currentNode);
			currentNode.detach();
			bracket.appendChild(currentNode);
			currentNode = nextNode;
		}
		currentNode.detach();
		bracket.appendChild(currentNode);
		/* Pick up nodes after the close bracket */
		currentNode = XOMTools.getNextSibling(closeBracket);
		while(currentNode != null) {
			Node nextNode = XOMTools.getNextSibling(currentNode);
			currentNode.detach();
			bracket.appendChild(currentNode);
			currentNode = nextNode;
		}
		openBracket.detach();
		closeBracket.detach();

		return bracket;
	}

}

