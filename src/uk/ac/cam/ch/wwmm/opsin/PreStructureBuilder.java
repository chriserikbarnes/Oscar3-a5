package uk.ac.cam.ch.wwmm.opsin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uk.ac.cam.ch.wwmm.ptclib.io.ResourceGetter;
import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;
import uk.ac.cam.ch.wwmm.ptclib.xml.XOMTools;

import nu.xom.Attribute;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;
import nu.xom.Node;
import nu.xom.Nodes;

/**Performs structure-aware destructive procedural parsing on parser results.
*
* @author dl387
*
*/

public class PreStructureBuilder {

	private static ResourceGetter resourceGetter = NameToStructure.resourceGetter;
	private Pattern matchCompoundLocant =Pattern.compile("[\\[\\(\\{](\\d+[a-z]?'*)[\\]\\)\\}]");
	private Pattern matchIndicatedHydrogen =Pattern.compile("(\\d+[a-z]?'*)H");
	private Pattern matchBracketedEntryInLocant =Pattern.compile("[\\[\\(\\{].*[\\]\\)\\}]");
	private Pattern matchCisTransInLocants =Pattern.compile("[rct]-");
	private Pattern matchColon =Pattern.compile(":");
	private Pattern matchSemiColon =Pattern.compile(";");
	private Pattern matchComma =Pattern.compile(",");
	private Pattern matchSpace =Pattern.compile(" ");
	private Pattern matchElementSymbol = Pattern.compile("[A-Z].?");
	private Pattern matchInlineSuffixesThatAreAlsoGroups = Pattern.compile("carbonyl|oxy|sulfinyl|sulfonyl");
	private Pattern matchSuffixesThatGoAtEndOfChainsByDefault = Pattern.compile("al|amide|hydrazide|hydrazonic|hydroxamic|hydroximic|ic|imidic|nitrile|oate|ohydrazide|ohydrazonic|ohydroxamic|ohydroximic|oic|oyl");

	private NameToStructure n2s;
	private BuildState state;

	//rings that look like HW rings but have other meanings
	private HashSet<String> blockedHWRings;

	/*Holds the rules on how suffixes are interpreted.*/
	Document suffixRulesDoc;

	PreStructureBuilder(NameToStructure parent) throws Exception {
		n2s=parent;
		suffixRulesDoc = resourceGetter.getXMLDocument("suffixRules.xml");

		blockedHWRings = new HashSet<String>();
		blockedHWRings.add("oxine");
		blockedHWRings.add("oxin");
		blockedHWRings.add("azine");
		blockedHWRings.add("azin");
		blockedHWRings.add("thiol");

		blockedHWRings.add("boroxine");
		blockedHWRings.add("boroxin");
		blockedHWRings.add("borazine");
		blockedHWRings.add("borazin");
	}


	/** The master method, postprocesses a parse result. At this stage one can except all substituents/roots to have at least 1 group.
	 * Multiple groups are present in for example fusion nomenclature. By the end of this function there will be exactly 1 group
	 * associated with each substituent/root
	 *
	 * @param elem The element to postprocess.
	 * @return The postprocessed element. The same as elem.
	 * @throws Exception
	 */
	Element postProcess(Element elem, BuildState state) throws Exception {
		this.state=state;
		n2s.structureBuilder.setState(state);
		state.wordRule=elem.getAttributeValue("wordRule");
		Elements words =elem.getChildElements("word");
		for (int i = 0; i < words.size(); i++) {
			Element word =words.get(i);
			if (word.getAttributeValue("type").equals("literal")){
				continue;
			}

			ArrayList<Element> roots =  OpsinTools.nodesToElementArrayList(XQueryUtil.xquery(word, ".//root"));
			if (roots.size() >1){
				throw new PostProcessingException("Multiple roots, but only 0 or 1 were expected. Found: " +roots.size());
			}
			ArrayList<Element> substituents = OpsinTools.nodesToElementArrayList(XQueryUtil.xquery(word, ".//substituent"));
			ArrayList<Element> substituentsAndRoot = OpsinTools.combineElementLists(substituents, roots);
			ArrayList<Element> brackets =  OpsinTools.nodesToElementArrayList(XQueryUtil.xquery(word, ".//bracket"));
			ArrayList<Element> substituentsAndRootAndBrackets =OpsinTools.combineElementLists(substituentsAndRoot, brackets);
			ArrayList<Element> groups =  OpsinTools.nodesToElementArrayList(XQueryUtil.xquery(word, ".//group"));

			Element root =null;
			if (roots.size() ==1) root=roots.get(0);

			for (Element subOrBracketOrRoot : substituentsAndRootAndBrackets) {
				processLocants(subOrBracketOrRoot);
			}

			for (Element group : groups) {
				Fragment thisFrag = resolveGroup(state, group);
				state.xmlFragmentMap.put(group, thisFrag);
			}

			for (Element e : substituentsAndRootAndBrackets) {
				checkAndConvertToSingleLocants(e, root);
			}

			for (Element subOrRoot : substituentsAndRoot) {
				processMultipliers(subOrRoot);
				preliminaryProcessSuffixes(subOrRoot);//also handles labelling of atoms with element symbol locants
			}

			for (Element e : substituentsAndRootAndBrackets) {
				matchLocantsToDirectFeatures(e);
			}
			for (Element subOrRoot : substituentsAndRoot) {
				processHW(subOrRoot);//hantzch-widman rings
				processRingAssemblies(subOrRoot);
				n2s.frBuilder.processFusedRings(state, subOrRoot);
				processComplicatedSpiroNomenclature(subOrRoot);
			}

			//System.out.println(new XOMFormatter().elemToString(elem));
			while(findAndStructureImplictBrackets(substituents, state.xmlFragmentMap, brackets));

			substituentsAndRoot = OpsinTools.combineElementLists(substituents, roots);
			substituentsAndRootAndBrackets =OpsinTools.combineElementLists(substituentsAndRoot, brackets);

			for (Element e : substituentsAndRootAndBrackets) {
				matchLocantsToIndirectFeatures(e);
			}
			for (Element subOrRoot : substituentsAndRoot) {
				resolveRemainingSuffixes(subOrRoot);
			}

			ArrayList<Element> subsOrRootsToRemove =new ArrayList<Element>();
			for (Element subOrRoot : substituentsAndRoot) {
				assignOutIDsToTrivialMultiRadicals(subOrRoot, subsOrRootsToRemove);
			}
			substituentsAndRoot.removeAll(subsOrRootsToRemove);
			substituentsAndRootAndBrackets.removeAll(subsOrRootsToRemove);

			for (Element subOrRoot : substituentsAndRoot) {
				identifyMultiplicativeNomenclature(subOrRoot);
			}

			//initially there should only be at maximum one root but after there may be multiple roots! (which are added into roots)
			if (root!=null){
				roots =processMultiplicativeNomenclature(roots);
			}

			for (Element e : substituentsAndRootAndBrackets) {
				processGroupMultipliers(e);
			}
		}

		return elem;
	}

	/**Handles special features of locants e.g. ortho/meta/para, indicated hydrogen, cis/trans in locant
	 *
	 * @param elem The substituent/root/bracket to looks for locants in.
	 * @throws PostProcessingException
	 */
	private void processLocants(Element elem) throws PostProcessingException{
		Elements ompLocants = elem.getChildElements("orthoMetaPara");
		for(int i=0;i<ompLocants.size();i++) {
			Element locant = ompLocants.get(i);
			String locantText = locant.getValue();
			locantText = locantText.substring(0, 1);
			Element afterOmpLocant = (Element)XOMTools.getNextSibling(locant);
			locant.setLocalName("locant");
			locant.removeChildren();
			locant.addAttribute(new Attribute("type", "orthoMetaPara"));
			if(afterOmpLocant.getLocalName().equals("multiplier") || afterOmpLocant.getAttribute("outIDs")!=null) {
				if (locantText.matches("(?i)o")){
					locant.appendChild("1,2-");
				}
				if (locantText.matches("(?i)m")){
					locant.appendChild("1,3-");
				}
				if (locantText.matches("(?i)p")){
					locant.appendChild("1,4-");
				}
			}
			else{
				if (locantText.matches("(?i)o")){
					locant.appendChild("2-");
				}
				if (locantText.matches("(?i)m")){
					locant.appendChild("3-");
				}
				if (locantText.matches("(?i)p")){
					locant.appendChild("4-");

				}
			}
		}

		Elements locants = elem.getChildElements("locant");
		for(int i=0;i<locants.size();i++) {
			Element locant = locants.get(i);
			String locantText = OpsinTools.removeDashIfPresent(locant.getValue());

			//If the indicatedHydrogen has been specified create a tag for it and remove it from the list of locants
			//e.g. 1(9H),5,7 -->indicatedHydrogen tag value (9H) and 1,5,7
			//can get as complicated as 1,2(2H,7H)
			Matcher matches =matchIndicatedHydrogen.matcher(locantText);
			if (matches.find()==true){
				do {
					Element indicatedHydrogenElement=new Element("indicatedHydrogen");
					indicatedHydrogenElement.addAttribute(new Attribute("locant", matches.group(1)));
					XOMTools.insertBefore(locant, indicatedHydrogenElement);
				}
				while (matches.find());
				locantText =matchBracketedEntryInLocant.matcher(locantText).replaceAll("");
			}

			/*
			 * Strip out cis/trans information built into locant - currently unhandled
			 */
			matches =matchCisTransInLocants.matcher(locantText);
			if (matches.find()==true){
				do {
					//currently do nothing
				}
				while (matches.find());
				locantText =matches.replaceAll("");
			}
			OpsinTools.setTextChild(locant, locantText);

			Element afterLocants = (Element)XOMTools.getNextSibling(locant);
			if(afterLocants == null) throw new PostProcessingException("Nothing after locant tag: " + locant.toXML());
		}
	}


	/**Resolves the contents of a &lt;group&gt; tag.
	 *
	 * @param group The &lt;group&gt; tag.
	 * @return The fragment specified by the tag.
	 * @throws StructureBuildingException If the group can't be built.
	 * @throws PostProcessingException
	 */
	private Fragment resolveGroup(BuildState state, Element group) throws StructureBuildingException, PostProcessingException {
		String groupType = group.getAttributeValue("type");
		String groupSubType = group.getAttributeValue("subType");
		String groupValue = group.getAttributeValue("value");
		String groupValType = group.getAttributeValue("valType");
		Fragment thisFrag =null;
		if(groupValType.equals("chain")) {
			int alkaneLength = new Integer(groupValue);
			String SSMILES = "";
			for(int i=0;i<alkaneLength;i++) {
				SSMILES += "C";
			}
			thisFrag = state.fragManager.buildSMILES(SSMILES, groupType, groupSubType, null);
		} else if(groupValType.equals("ring") || groupValType.equals("partunsatring")) {
			int alkaneLength = new Integer(groupValue);
			String SSMILES = "C1";
			for(int i=0;i<alkaneLength-1;i++) {
				SSMILES += "C";
			}
			SSMILES += "1";
			thisFrag = state.fragManager.buildSMILES(SSMILES, groupType, groupSubType, null);
		} else if(groupValType.equals("unsatring")) {
			int alkaneLength = new Integer(groupValue);
			String SSMILES = "C1";
			for(int i=0;i<alkaneLength-1;i++) {
				SSMILES += "C";
				if(i % 2 == 0) SSMILES += "=";
			}
			SSMILES += "1";
			thisFrag = state.fragManager.buildSMILES(SSMILES, groupType, groupSubType, null);
		} else if(groupValType.equals("SMILES")) {
			thisFrag = state.fragManager.buildSMILES(groupValue, groupType, groupSubType, group.getAttributeValue("labels"));
		} else if(groupValType.equals("dbkey")) {
			thisFrag = state.fragManager.buildCML(groupValue, groupType, groupSubType);
		} else if(groupValType.equals("atom")) {
			thisFrag = state.fragManager.buildSMILES("[" + groupValue + "]", groupType, groupSubType, null);
		}
		else{
			throw new StructureBuildingException("Group tag has bad or missing valType: " + group.toXML());
		}
		if (thisFrag ==null){
			throw new StructureBuildingException("null fragment returned from the following xml: " + group.toXML());
		}

		if (group.getAttribute("defaultInLocant")!=null){
			thisFrag.setDefaultInID(thisFrag.getAtomByLocantOrThrow(group.getAttributeValue("defaultInLocant")).getID());
		}
		else if (group.getAttribute("defaultInID")!=null){
			thisFrag.setDefaultInID(thisFrag.getIdOfFirstAtom() + Integer.parseInt(group.getAttributeValue("defaultInID")) -1);
		}
		else if (group.getAttribute("usableAsAJoiner") != null && group.getAttributeValue("usableAsAJoiner").equals("yes")){//makes linkers by default attach end to end
			int chainLength =thisFrag.getChainLength();
			if (chainLength >1){
				group.addAttribute(new Attribute("defaultInID",String.valueOf(chainLength)));
				thisFrag.setDefaultInID(thisFrag.getIDFromLocantOrThrow(String.valueOf(chainLength)));
			}
		}

		if (thisFrag.getType().equals("ring")){
			thisFrag.convertHighOrderBondsToSpareValencies();
		}

		//processes groups like cymene and xylene whose structure is determined by the presence of a locant in front e.g. p-xylene
		thisFrag =processXyleneLikeNomenclature(group, thisFrag);

		if (group.getAttribute("outID")!=null){
			thisFrag.addOutID(thisFrag.getIdOfFirstAtom() + Integer.parseInt(group.getAttributeValue("outID")) -1, 1, true);
		}

		if (thisFrag.getOutIDs().size()==0 && group.getAttributeValue("type").equals("substituent") &&
				group.getAttributeValue("subType").equals("simpleSubstituent")){
			//simple substituents implicitly will be given an outID assuming the SSMILESbuilder hasn't already given them one
			thisFrag.addOutID(thisFrag.getIdOfFirstAtom(), 1, false);
		}

		return thisFrag;
	}

	/**
	 * Checks for groups with the addGroup attribute and adds the group defined by the SMILES described within
	 * e.g. for xylene  this function would add a methyl. Xylene is initially generated using the structure of toluene!
	 * @param group: The group element
	 * @param parentFrag: The fragment that has been generated from the group element
	 * @throws StructureBuildingException
	 * @throws PostProcessingException
	 */
	private Fragment processXyleneLikeNomenclature(Element group, Fragment parentFrag) throws StructureBuildingException, PostProcessingException {
		if(group.getAttributeValue("addGroup")!=null) {
			String addGroupInformation=group.getAttributeValue("addGroup");
			String[] groupsToBeAdded = matchSemiColon.split(addGroupInformation);//typically only one, but 2 in the case of xylene and quinones
			ArrayList<HashMap<String, String>> allGroupInformation = new ArrayList<HashMap<String, String>>();
			for (int i = 0; i < groupsToBeAdded.length; i++) {//populate allGroupInformation list
				String groupToBeAdded = groupsToBeAdded[i];
				String[] tempArray =matchSpace.split(groupToBeAdded);
				HashMap<String, String> groupInformation = new HashMap<String, String>();
				if (tempArray.length!=2){
					throw new PostProcessingException("malformed addGroup tag");
				}
				groupInformation.put("SMILES", tempArray[0]);
				if (tempArray[1].startsWith("id")){
					groupInformation.put("atomReferenceType", "id");
					groupInformation.put("atomReference", tempArray[1].substring(2));
				}
				else if (tempArray[1].startsWith("locant")){
					groupInformation.put("atomReferenceType", "locant");
					groupInformation.put("atomReference", tempArray[1].substring(6));
				}
				else{
					throw new PostProcessingException("malformed addGroup tag");
				}
				allGroupInformation.add(groupInformation);
			}
			Element previousEl =(Element) XOMTools.getPreviousSibling(group);
			if (previousEl !=null && previousEl.getLocalName().equals("locant")){//has the name got specified locants to override the default ones
				List<String> locantValues =StringTools.arrayToList(matchComma.split(previousEl.getValue()));
				boolean assignlocants =true;
				if (locantValues.size()<groupsToBeAdded.length){
					if (locantValues.size() +1 <groupsToBeAdded.length ){//only one locant can be implicit
						assignlocants=false;
					}
					else {//check that the firstGroup by default will be added to the atom with locant 1. If this is not the case then as many locants as there were groups should of been specified
						//or no locants should have been specified, which is what will be assumed (i.e. the locants will be left unassigned)
						HashMap<String, String> groupInformation =allGroupInformation.get(0);
						String locant;
						if (groupInformation.get("atomReferenceType").equals("locant")){
							locant =parentFrag.getAtomByLocantOrThrow(groupInformation.get("atomReference")).getFirstLocant();
						}
						else if (groupInformation.get("atomReferenceType").equals("id") ){
							locant =parentFrag.getAtomByIDOrThrow(parentFrag.getIdOfFirstAtom() + Integer.parseInt(groupInformation.get("atomReference")) -1 ).getFirstLocant();
						}
						else{
							throw new PostProcessingException("malformed addGroup tag");
						}
						if (locant ==null || !locant.equals("1")){
							assignlocants=false;
						}
					}

				}
				if (assignlocants==true){
					for (int i = groupsToBeAdded.length -1; i >=0 ; i--) {
						//if less locants than expected are specified the locants of only the later groups will be changed
						//e.g. 4-xylene will transform 1,2-xylene to 1,4-xylene
						HashMap<String, String> groupInformation =allGroupInformation.get(i);
						if (locantValues.size() >0){
							groupInformation.put("atomReferenceType", "locant");
							groupInformation.put("atomReference", locantValues.get(locantValues.size()-1));
							locantValues.remove(locantValues.size()-1);
						}
						else{
							break;
						}
					}
					if (locantValues.size() ==0){
						previousEl.detach();
					}
					else{
						OpsinTools.setTextChild(previousEl, StringTools.objectListToString(locantValues, ","));
					}
				}
			}

			for (int i = 0; i < groupsToBeAdded.length; i++) {
				HashMap<String, String> groupInformation =allGroupInformation.get(i);
				String smilesOfGroupToBeAdded = groupInformation.get("SMILES");
				Fragment newFrag = state.fragManager.buildSMILES(smilesOfGroupToBeAdded, parentFrag.getType(), parentFrag.getSubType(), "none");
				Atom atomOnParentFrag =null;
				if (groupInformation.get("atomReferenceType").equals("locant")){
					atomOnParentFrag=parentFrag.getAtomByLocantOrThrow(groupInformation.get("atomReference"));
				}
				else if (groupInformation.get("atomReferenceType").equals("id") ){
					atomOnParentFrag= parentFrag.getAtomByIDOrThrow(parentFrag.getIdOfFirstAtom() + Integer.parseInt(groupInformation.get("atomReference")) -1);
				}
				else{
					throw new PostProcessingException("malformed addGroup tag");
				}
				if (newFrag.getOutIDs().size() >1){
					throw new PostProcessingException("too many outIDs on group to be added");
				}
				if (newFrag.getOutIDs().size() ==1) {
					OutID newFragOutID = newFrag.getOutID(0);
					state.fragManager.incorporateFragment(newFrag, newFragOutID.id, parentFrag, atomOnParentFrag.getID(), newFragOutID.valency);
				}
				else{
					Atom atomOnNewFrag =newFrag.getAtomByIDOrThrow(newFrag.getDefaultInID());
					state.fragManager.incorporateFragment(newFrag, atomOnNewFrag.getID(), parentFrag, atomOnParentFrag.getID(), 1);
				}
			}
		}
		
		if(group.getAttributeValue("addHeteroAtom")!=null) {
			String addHeteroAtomInformation=group.getAttributeValue("addHeteroAtom");
			String[] heteroAtomsToBeAdded = matchSemiColon.split(addHeteroAtomInformation);
			ArrayList<HashMap<String, String>> allHeteroAtomInformation = new ArrayList<HashMap<String, String>>();
			for (int i = 0; i < heteroAtomsToBeAdded.length; i++) {//populate allHeteroAtomInformation list
				String heteroAtomToBeAdded = heteroAtomsToBeAdded[i];
				String[] tempArray =matchSpace.split(heteroAtomToBeAdded);
				HashMap<String, String> heteroAtomInformation = new HashMap<String, String>();
				if (tempArray.length!=2){
					throw new PostProcessingException("malformed addHeteroAtom tag");
				}
				heteroAtomInformation.put("SMILES", tempArray[0]);
				if (tempArray[1].startsWith("id")){
					heteroAtomInformation.put("atomReferenceType", "id");
					heteroAtomInformation.put("atomReference", tempArray[1].substring(2));
				}
				else if (tempArray[1].startsWith("locant")){
					heteroAtomInformation.put("atomReferenceType", "locant");
					heteroAtomInformation.put("atomReference", tempArray[1].substring(6));
				}
				else{
					throw new PostProcessingException("malformed addHeteroAtom tag");
				}
				allHeteroAtomInformation.add(heteroAtomInformation);
			}
			Element previousEl =(Element) XOMTools.getPreviousSibling(group);
			if (previousEl !=null && previousEl.getLocalName().equals("locant")){//has the name got specified locants to override the default ones
				List<String> locantValues =StringTools.arrayToList(matchComma.split(previousEl.getValue()));
				if (locantValues.size() >=heteroAtomsToBeAdded.length){
					for (int i = heteroAtomsToBeAdded.length -1; i >=0 ; i--) {//all heteroatoms must have a locant or default locants will be used
						HashMap<String, String> groupInformation =allHeteroAtomInformation.get(i);
						groupInformation.put("atomReferenceType", "locant");
						groupInformation.put("atomReference", locantValues.get(locantValues.size()-1));
						locantValues.remove(locantValues.size()-1);
					}
					if (locantValues.size() ==0){
						previousEl.detach();
					}
					else{
						OpsinTools.setTextChild(previousEl, StringTools.objectListToString(locantValues, ","));
					}
				}
			}

			for (int i = 0; i < heteroAtomsToBeAdded.length; i++) {
				HashMap<String, String> heteroAtomInformation =allHeteroAtomInformation.get(i);
				Atom atomOnParentFrag =null;
				if (heteroAtomInformation.get("atomReferenceType").equals("locant")){
					atomOnParentFrag=parentFrag.getAtomByLocantOrThrow(heteroAtomInformation.get("atomReference"));
				}
				else if (heteroAtomInformation.get("atomReferenceType").equals("id") ){
					atomOnParentFrag= parentFrag.getAtomByIDOrThrow(parentFrag.getIdOfFirstAtom() + Integer.parseInt(heteroAtomInformation.get("atomReference")) -1);
				}
				else{
					throw new PostProcessingException("malformed addHeteroAtom tag");
				}
				state.fragManager.makeHeteroatom(atomOnParentFrag, heteroAtomInformation.get("SMILES"), parentFrag);
			}
		}
		
		if(group.getAttributeValue("addBond")!=null) {
			String addBondInformation=group.getAttributeValue("addBond");
			String[] bondsToBeAdded = matchSemiColon.split(addBondInformation);
			ArrayList<HashMap<String, String>> allBondInformation = new ArrayList<HashMap<String, String>>();
			for (int i = 0; i < bondsToBeAdded.length; i++) {//populate allBondInformation list
				String bondToBeAdded = bondsToBeAdded[i];
				String[] tempArray =matchSpace.split(bondToBeAdded);
				HashMap<String, String> bondInformation = new HashMap<String, String>();
				if (tempArray.length!=2){
					throw new PostProcessingException("malformed addBond tag");
				}
				bondInformation.put("bondOrder", tempArray[0]);
				if (tempArray[1].startsWith("id")){
					bondInformation.put("atomReferenceType", "id");
					bondInformation.put("atomReference", tempArray[1].substring(2));
				}
				else if (tempArray[1].startsWith("locant")){
					bondInformation.put("atomReferenceType", "locant");
					bondInformation.put("atomReference", tempArray[1].substring(6));
				}
				else{
					throw new PostProcessingException("malformed addBond tag");
				}
				allBondInformation.add(bondInformation);
			}
			Element previousEl =(Element) XOMTools.getPreviousSibling(group);
			if (previousEl !=null && previousEl.getLocalName().equals("locant")){//has the name got specified locants to override the default ones
				List<String> locantValues =StringTools.arrayToList(matchComma.split(previousEl.getValue()));
				if (locantValues.size() >=bondsToBeAdded.length){
					for (int i = bondsToBeAdded.length -1; i >=0 ; i--) {//all heteroatoms must have a locant or default locants will be used
						HashMap<String, String> bondInformation =allBondInformation.get(i);
						bondInformation.put("atomReferenceType", "locant");
						bondInformation.put("atomReference", locantValues.get(locantValues.size()-1));
						locantValues.remove(locantValues.size()-1);
					}
					if (locantValues.size() ==0){
						previousEl.detach();
					}
					else{
						OpsinTools.setTextChild(previousEl, StringTools.objectListToString(locantValues, ","));
					}
				}
			}

			for (int i = 0; i < bondsToBeAdded.length; i++) {
				HashMap<String, String> bondInformation =allBondInformation.get(i);
				Atom atomOnParentFrag =null;
				if (bondInformation.get("atomReferenceType").equals("locant")){
					atomOnParentFrag=parentFrag.getAtomByLocantOrThrow(bondInformation.get("atomReference"));
				}
				else if (bondInformation.get("atomReferenceType").equals("id") ){
					atomOnParentFrag= parentFrag.getAtomByIDOrThrow(parentFrag.getIdOfFirstAtom() + Integer.parseInt(bondInformation.get("atomReference")) -1);
				}
				else{
					throw new PostProcessingException("malformed addBond tag");
				}
				state.fragManager.unsaturate(atomOnParentFrag.getID(), Integer.parseInt(bondInformation.get("bondOrder")) , parentFrag);
			}
		}
		return parentFrag;
	}


	/**Converts locantgroups to individual locant tags, and checks for agreement
	 * between the number of locants, and multipliers. If there is disagreement the locant is checked against some special cases.
	 * If this fails an excpetion is thrown.
	 *
	 * @param elem The substituent/root/bracket to looks for locants in.
	 * @param root : used to check if a locant is referring to the root as in multiplicative nomenclature (root can be null for substituents)
	 * @throws PostProcessingException
	 * @throws StructureBuildingException
	 * @throws PostProcessingException If there is a disagreement.
	 */
	private void checkAndConvertToSingleLocants(Element subOrBracketOrRoot, Element root) throws StructureBuildingException, PostProcessingException {
		Elements locants = subOrBracketOrRoot.getChildElements("locant");
		Element group =subOrBracketOrRoot.getFirstChildElement("group");
		for(int i=0;i<locants.size();i++) {
			Element locant = locants.get(i);
			String locantText = locant.getValue();
			if(locantText.endsWith("-")) {
				locantText = locantText.substring(0, locantText.length()-1);
			}
			String [] locantValues = locantText.split(",");

			Element afterLocants = (Element)XOMTools.getNextSibling(locant);
			if(locantValues.length > 1) {
				while (afterLocants !=null){
					if(afterLocants.getLocalName().equals("multiplier") || afterLocants.getLocalName().equals("locant")) {
						break;
					}
					afterLocants = (Element)XOMTools.getNextSibling(afterLocants);
				}
				if(afterLocants != null && afterLocants.getLocalName().equals("multiplier")) {
					if(Integer.parseInt(afterLocants.getAttributeValue("value")) == locantValues.length ) {
						// number of locants and multiplier agree
						if (locantValues[locantValues.length-1].endsWith("'") && group!=null && group.getParent().indexOf(group) > group.getParent().indexOf(locant)){//quite possible that this is referring to a multiplied root

							if (group.getAttribute("outIDs")!=null){
								determineLocantMeaning(locant, locantValues, root);
							}
							else{
								Element afterGroup = (Element)XOMTools.getNextSibling(group);
								int inlineSuffixCount =0;
								int multiplier=1;
								while (afterGroup !=null){
									if(afterGroup.getLocalName().equals("multiplier")){
										multiplier =Integer.parseInt(afterGroup.getAttributeValue("value"));
									}
									else if(afterGroup.getLocalName().equals("suffix") && afterGroup.getAttributeValue("type").equals("inline")){
										inlineSuffixCount +=(multiplier);
										multiplier=1;
									}
									afterGroup = (Element)XOMTools.getNextSibling(afterGroup);
								}
								if (inlineSuffixCount >=2){
									determineLocantMeaning(locant, locantValues, root);
								}
							}
						}
					} else {
						if(!determineLocantMeaning(locant, locantValues, root)) throw new PostProcessingException("Mismatch between locant and multiplier counts (" +
								Integer.toString(locantValues.length) + " and " + afterLocants.getAttributeValue("value") + "):" + locant.toXML());
					}
				} else {
					/* Multiple locants without a multiplier */
					if(!determineLocantMeaning(locant, locantValues, root)) throw new PostProcessingException("Multiple locants without a multiplier: " + locant.toXML());
				}
			}

			//checks that determineLocantMeaning hasn't moved the locant or already assigned the locant a special meaning and detached it
			if (locant.getParent()!=null && locant.getParent().equals(subOrBracketOrRoot)){

				for(int j=0;j<locantValues.length;j++) {
					Element singleLocant = new Element("locant");
					String locantType = null;
					if (locant.getAttribute("type")!=null){
						locantType =locant.getAttributeValue("type");
						singleLocant.addAttribute(new Attribute("type", locantType));
					}
					singleLocant.addAttribute(new Attribute("value", locantValues[j]));
					XOMTools.insertBefore(locant, singleLocant);
				}
				locant.detach();
			}
		}
	}


	/**Looks for Hantzch-Widman systems, and sees if the number of locants
	 * agrees with the number of heteroatoms.
	 * If this is not the case alternative possibilities are tested:
	 * 	The locants could be intended to indicate the position of outIDs e.g. 1,4-phenylene
	 * 	The locants could be intended to indicate the attachement points of the root groups in multiplicative nomenclature e.g. 4,4'-methylenedibenzoic acid
	 * @param locant The element corresponding to the locant group before the HW system.
	 * @param locantValues The locant values;
	 * @param root : used to check if a locant is referring to the root as in multiplicative nomenclature (root can be null for substituents)
	 * @return true if there's a HW system, and agreement; or if the locants conform to one of the alternative possibilities, otherwise false.
	 * @throws StructureBuildingException
	 */
	private boolean determineLocantMeaning(Element locant, String[] locantValues, Element root) throws StructureBuildingException {
		if (locant.getAttribute("type")!=null && locant.getAttributeValue("type").equals("multiplicativeNomenclature")) return true;//already known function
		int count =locantValues.length;
		Element currentElem = (Element)XOMTools.getNextSibling(locant);
		int heteroCount = 0;
		while(currentElem != null && !currentElem.getLocalName().equals("group")){
			if(currentElem.getLocalName().equals("heteroatom")) {
				heteroCount++;
			} else if (currentElem.getLocalName().equals("multiplier")){
				heteroCount += Integer.parseInt(currentElem.getAttributeValue("value")) -1;
			}
			currentElem = (Element)XOMTools.getNextSibling(currentElem);
		}
		if(currentElem != null && currentElem.getLocalName().equals("group")){
			if (currentElem.getAttributeValue("subType").equals("hantzschWidman")) {
				if(heteroCount == count) {
					return true;
				} else {
					return false;//there is a case where locants don't apply to heteroatoms in a HW system, but in that case only one locant is expected so this function would not be called
				}
			}
			else if (heteroCount==0 && currentElem.getAttribute("outIDs")!=null) {//e.g. phenylene
				String[] outIDs = matchComma.split(currentElem.getAttributeValue("outIDs"), -1);
				int flag =0;
				for (int i = 0; i < outIDs.length; i++) {
					if (outIDs[i].endsWith("?")){
						flag=1;
						break;
					}
				}
				if (flag==0 && count ==outIDs.length){
					Fragment groupFragment =state.xmlFragmentMap.get(currentElem);
					int idOfFirstAtomInFrag =groupFragment.getIdOfFirstAtom();
					for (int i = outIDs.length-1; i >=0; i--) {
						Atom a =groupFragment.getAtomByLocant(locantValues[i]);
						if (a==null){
							flag=1;
							break;
						}
						outIDs[i]=String.valueOf(a.getID() -idOfFirstAtomInFrag +1);//convert to relative id
					}
					if (flag==0){
						currentElem.getAttribute("outIDs").setValue(StringTools.arrayToString(outIDs, ","));
						locant.detach();
						return true;
					}
				}
			}
			else if(currentElem.getValue().equals("benz") || currentElem.getValue().equals("benzo")){
				Node potentialGroupAfterBenzo = XOMTools.getNextSibling(currentElem, "group");//need to make sure this isn't benzyl
				if (potentialGroupAfterBenzo!=null){
					return true;//e.g. 1,2-benzothiazole
				}
			}
		}
		if (root!=null){
			Element multiplier =(Element) root.getChild(0);
			if (!multiplier.getLocalName().equals("multiplier") && ((Element)root.getParent()).getLocalName().equals("bracket")){//e.g. 1,1'-ethynediylbis(1-cyclopentanol)
				multiplier =(Element) root.getParent().getChild(0);
			}
			Node commonParent =locant.getParent().getParent();//this should be a common parent of the multiplier in front of the root. If it is not, then this locant is in a different scopt
			Node parentOfMultiplier =multiplier.getParent();
			while (parentOfMultiplier!=null){
				if (commonParent.equals(parentOfMultiplier)){
					if (locantValues[count-1].endsWith("'")  &&
							multiplier.getLocalName().equals("multiplier") && multiplier.getAttribute("locantsAssigned")==null &&
							Integer.parseInt(multiplier.getAttributeValue("value")) == count ){//multiplicative nomenclature
						multiplier.addAttribute(new Attribute ("locantsAssigned",""));
						locant.detach();
						for(int i=locantValues.length-1; i>=0; i--) {
							Element singleLocant = new Element("multiplicativeLocant");
							singleLocant.addAttribute(new Attribute("value", locantValues[i]));
							XOMTools.insertAfter(multiplier, singleLocant);
						}
						return true;
					}
				}
				parentOfMultiplier=parentOfMultiplier.getParent();
			}
		}
		return false;
	}


	/** Look for multipliers, and multiply out suffixes/unsaturators/heteroatoms/hydros
	 * associated with them. Eg. triol - > ololol.
	 * @param elem The substituent/root to looks for multipliers in.
	 */
	private void processMultipliers(Element elem) {
		Elements multipliers = elem.getChildElements("multiplier");
		for(int i=0;i<multipliers.size();i++) {
			Element m = multipliers.get(i);
			Element nextElem = (Element)XOMTools.getNextSibling(m);
			String nextName = nextElem.getLocalName();
			if(nextName.equals("unsaturator") ||
					nextName.equals("suffix") ||
					nextName.equals("heteroatom") ||
					nextName.equals("hydro")) {
				int mvalue = Integer.parseInt(m.getAttributeValue("value"));
				for(int j=0;j<mvalue-1;j++) {
					XOMTools.insertAfter(m, new Element(nextElem));
				}
				m.detach();
			}
		}
	}


	/**
	 * Handles suffixes, passes them to resolveGroupAddingSuffixes.
	 * Processes the suffixAppliesTo command which multiplies a suffix and attaches the suffixes to the atoms described by the given IDs
	 * @param subOrRoot
	 * @throws PostProcessingException
	 * @throws StructureBuildingException
	 */
	private void preliminaryProcessSuffixes(Element subOrRoot) throws PostProcessingException, StructureBuildingException{
		Nodes groupsOfSubOrRoot = XQueryUtil.xquery(subOrRoot, "group");
		Element lastGroupElementInSubOrRoot =(Element)groupsOfSubOrRoot.get(groupsOfSubOrRoot.size()-1);
		Fragment suffixableFragment =state.xmlFragmentMap.get(lastGroupElementInSubOrRoot);
		ArrayList<Fragment> suffixFragments =resolveGroupAddingSuffixes(XQueryUtil.xquery(subOrRoot, "suffix"), suffixableFragment);

		//instructions for number/positions of suffix
		//this is of the form comma sepeated ids with the number of ids corresponding to the number of instances of the suffix
		if (lastGroupElementInSubOrRoot.getAttribute("suffixAppliesTo")!=null){
			Element suffix =OpsinTools.getNextNonChargeSuffix(lastGroupElementInSubOrRoot);
			if (suffix ==null){
				throw new PostProcessingException("No suffix where suffix was expected");
			}
			String suffixInstruction =lastGroupElementInSubOrRoot.getAttributeValue("suffixAppliesTo");
			String[] suffixInstructions=suffixInstruction.split(",");
			ArrayList<Fragment> newSuffixFragments =new ArrayList<Fragment>();
			int firstIdInFragment=suffixableFragment.getIdOfFirstAtom();
			for (int i = 0; i < suffixInstructions.length; i++) {
				if (i !=0){
					Element newSuffix =new Element(suffix);
					XOMTools.insertAfter(suffix, newSuffix);

					for (Fragment s : suffixFragments) {
						newSuffixFragments.add(state.fragManager.copyAndRelabel(s));
					}
					newSuffix.addAttribute(new Attribute("locantID", String.valueOf(firstIdInFragment + Integer.parseInt(suffixInstructions[i]) -1)));
				}
				else{
					suffix.addAttribute(new Attribute("locantID", String.valueOf(firstIdInFragment + Integer.parseInt(suffixInstructions[i]) -1)));
				}
			}
			suffixFragments.addAll(newSuffixFragments);
		}

		state.fragManager.assignElementLocants(suffixableFragment, suffixFragments);
		state.xmlSuffixMap.put(lastGroupElementInSubOrRoot, suffixFragments);
	}


	/**Processes a suffix and returns any fragment the suffix intends to add to the molecule
	 *
	 * @param suffixes The suffix nodes for a fragment.
	 * @param frag The fragment to which the suffix will be applied
	 * @return An arrayList containing the generated fragments
	 * @throws StructureBuildingException If the suffixes can't be resolved properly.
	 */
	private ArrayList<Fragment> resolveGroupAddingSuffixes(Nodes suffixes, Fragment frag) throws StructureBuildingException {
		ArrayList<Fragment> suffixFragments =new ArrayList<Fragment>();
		String groupType = frag.getType();
		String subgroupType = frag.getSubType();
//TODO optimize the Xquery as groupType is a constant
		for(int i=0;i<suffixes.size();i++) {
			Element suffix =(Element) suffixes.get(i);
			String suffixValue = suffix.getAttributeValue("value");

			Nodes suffixRuleNodes = XQueryUtil.xquery(suffixRulesDoc,"/suffixRulesList/"+
					"suffixRules[@type=\"" + groupType + "\"]/" +
					"rule[@value=\"" + suffixValue + "\" " +
							"and (not(@subType) or @subType=\"" + subgroupType +"\")]");
			if(suffixRuleNodes == null) throw new StructureBuildingException();

			Element suffixRule;
			if(suffixRuleNodes.size() == 1 &&
					suffixRuleNodes.get(0) instanceof Element) {
				suffixRule = (Element) suffixRuleNodes.get(0);
			} else {
				throw new StructureBuildingException();
			}

			Elements suffixRuleTags = suffixRule.getChildElements();
			for(int j=0;j<suffixRuleTags.size();j++) {
				Element suffixRuleTag = suffixRuleTags.get(j);
				if(suffixRuleTag.getLocalName().equals("addgroup")) {
					String bondOrderStr = suffixRuleTag.getAttributeValue("bondOrder");
					int bondOrder = 1;
					if(bondOrderStr != null) bondOrder = Integer.parseInt(bondOrderStr);
					String labels="none";
					if (suffixRuleTag.getAttribute("labels")!=null){
						labels =suffixRuleTag.getAttributeValue("labels");
					}
					Fragment suffixFrag;

					if(suffixRuleTag.getAttribute("setsOutID") != null) {
						suffixFrag= state.fragManager.buildSMILES(suffixRuleTag.getAttributeValue("SMILES"),"suffix", "outSuffix", labels);
						if(suffixFrag.getOutIDs().size() == 0) {
							if(suffixRuleTag.getAttribute("outValency") != null) {
								suffixFrag.addOutID(suffixFrag.getIdOfFirstAtom(), Integer.parseInt(suffixRuleTag.getAttributeValue("outValency")), true);
							}
							else{
								suffixFrag.addOutID(suffixFrag.getIdOfFirstAtom(), 1, true);
							}
						}
					}
					else if(suffixRuleTag.getAttribute("setsDefaultInID") != null) {
						suffixFrag= state.fragManager.buildSMILES(suffixRuleTag.getAttributeValue("SMILES"),"suffix", "inSuffix", labels);
					}
					else if	(suffixRuleTag.getAttribute("setsFunctionalID") != null) {
						suffixFrag= state.fragManager.buildSMILES(suffixRuleTag.getAttributeValue("SMILES"),"suffix", "functionalSuffix", labels);
						if(suffixFrag.getFunctionalIDs().size() == 0) {
							suffixFrag.addFunctionalID(suffixFrag.getIdOfFirstAtom());
						}
					}
					else{
						suffixFrag= state.fragManager.buildSMILES(suffixRuleTag.getAttributeValue("SMILES"),"suffix", "suffix", labels);
					}
					suffixFrag.setInValency(bondOrder);
					suffixFragments.add(suffixFrag);
				}
			}
		}
		return suffixFragments;
	}


	/** Match each locant to the next applicable "feature". Assumes that processLocants
	 * has done a good job and rejected cases where no match can be made.
	 * Handles cases where the locant is next to the feature it refers to
	 *
	 * @param elem The substituent/root/bracket to look for locants in.
	 * @throws PostProcessingException
	 * @throws StructureBuildingException
	 */
	private void matchLocantsToDirectFeatures(Element elem) throws PostProcessingException, StructureBuildingException {
		ArrayList<Element> locants = OpsinTools.nodesToElementArrayList(XQueryUtil.xquery(elem, "./locant"));

		Elements groups = elem.getChildElements("group");
		if(groups.size() >= 1) {//may be a bracket
			Element group = groups.get(0);
			if (group.getAttributeValue("subType").equals("hantzschWidman")){
				if (group.getAttributeValue("valType")!=null && group.getAttributeValue("valType").equals("partunsatring")){
					//exception for where a locant is supposed to indicate the location of a double bond...
					Elements deltas = elem.getChildElements("delta");
					if (deltas.size()==0){
						Element delta =new Element("delta");
						if (locants.size()>0 && elem.indexOf(locants.get(0))< elem.indexOf(group)){//locant is in front of group
							Element locant=locants.get(0);
							delta.appendChild(locant.getAttributeValue("value"));
							XOMTools.insertBefore(locant, delta);
							locant.detach();
							locants.remove(locant);
						}
						else{
							delta.appendChild("");
							elem.insertChild(delta, 0);//no obvious attempt to set double bond position, potentially ambiguous, valency will be used to choose later
						}
					}
					group.getAttribute("valType").setValue("ring");
				}
				if (locants.size()>0 ){//detect a solitary locant in front of a HW system and prevent it being assigned.
					//something like 1-aziridin-1-yl never means the N is at position 1 as it is at position 1 by convention
					int heteroAtomCount =0;
					ArrayList<Element> locantsBeforeHWSystem = new ArrayList<Element>();
					int indexOfGroup =elem.indexOf(group);
					for (int i = indexOfGroup -1; i >= 0; i--) {
						String elName=((Element)elem.getChild(i)).getLocalName();
						if (elName.equals("locant")){
							locantsBeforeHWSystem.add((Element)elem.getChild(i));
						}
						else if(elName.equals("heteroatom")){
							heteroAtomCount++;
						}
						else if (elName.equals("hyphen")){}
						else{
							break;
						}
					}
					if (locantsBeforeHWSystem.size()!=0){
						if (locantsBeforeHWSystem.size() ==1){
							locants.remove(locantsBeforeHWSystem.get(0));//don't assign this locant
						}
						else if (locantsBeforeHWSystem.size() != heteroAtomCount){
							throw new PostProcessingException("Mismatch between number of locants and HW heteroatoms");
						}
					}
				}
			}
		}

		for (Element locant : locants) {
			String locantValue =locant.getAttributeValue("value");
			if (matchElementSymbol.matcher(locantValue).matches()){//element symbol locant
				continue;
			}
			Element referent = (Element)XOMTools.getNextSibling(locant);
			while(referent.getLocalName().equals("locant") ||
					referent.getAttribute("locant") != null) {
				referent = (Element)XOMTools.getNextSibling(referent);
			}
			String refName = referent.getLocalName();
			if(refName.equals("unsaturator") ||
					refName.equals("suffix") ||
					refName.equals("heteroatom") ||
					refName.equals("hydro")) {

				//If a compound Locant e.g. 1(6) is detected add a compound locant attribute
				Matcher matches =matchCompoundLocant.matcher(locantValue);
				if (matches.find()==true){
					referent.addAttribute(new Attribute("compoundLocant", matches.group(1)));
					locantValue = matches.replaceAll("");
				}
				referent.addAttribute(new Attribute("locant", locantValue));
				locant.detach();
			}
		}
	}


	/**
	 * Checks through the groups accesible from the currentElement taking into account bracket
	 * i.e. those that it is feasible that the group of the currentElement could substitute onto
	 * @param currentElement
	 * @param locant: the locant string to check for the presence of
	 * @return whether the locant was found
	 */
	private boolean checkLocantPresentOnPotentialRoot(Element currentElement, String locant) {
		Element parent = (Element)currentElement.getParent();
		int indexOfCurrentElement =parent.indexOf(currentElement);
		Nodes siblings = XQueryUtil.xquery(parent, "./bracket|./substituent|./root");
		boolean locantFound =false;
		for (int i = 0; i < siblings.size(); i++) {
			Element bracketOrSub = (Element)siblings.get(i);
			if (parent.indexOf(bracketOrSub )<=indexOfCurrentElement){
				continue;
			}
			if (bracketOrSub.getLocalName().equals("bracket")){
				locantFound =checkLocantPresentOnPotentialRootRecursive(bracketOrSub, locant);
				if (locantFound==true){
					return true;
				}
			}
			else{
				Element group = bracketOrSub.getFirstChildElement("group");
				Fragment groupFrag =state.xmlFragmentMap.get(group);
				if (groupFrag.hasLocant(locant)){
					return true;
				}
				ArrayList<Fragment> suffixes =state.xmlSuffixMap.get(group);
				if (suffixes!=null){
					for (Fragment suffix : suffixes) {
						if (suffix.hasLocant(locant)){
							return true;
						}
					}
				}
			}
		}

		return locantFound;
	}

	/**
	 * Goes through all the substituents of the currentElement and recursively investigates brackets
	 * @param currentElement
	 * @param locant
	 * @return
	 */
	private boolean checkLocantPresentOnPotentialRootRecursive(Element currentElement, String locant) {
		Nodes siblings = XQueryUtil.xquery(currentElement, "./bracket|./substituent|./root");
		boolean locantFound =false;

		for (int i = 0; i < siblings.size(); i++) {
			Element bracketOrSub = (Element)siblings.get(i);
			if (bracketOrSub.getLocalName().equals("bracket")){
				locantFound =checkLocantPresentOnPotentialRootRecursive(bracketOrSub, locant);
				if (locantFound==true){
					return true;
				}
			}
			else{
				Element group = bracketOrSub.getFirstChildElement("group");
				Fragment groupFrag =state.xmlFragmentMap.get(group);
				if (groupFrag.hasLocant(locant)){
					return true;
				}
			}
		}

		return locantFound;
	}



	/**
	 * Handles Hantzsch-Widman rings. Adds SMILES to the group corresponding to the ring's structure
	 * @param elem
	 * @throws StructureBuildingException
	 * @throws PostProcessingException
	 */
	private void processHW(Element subOrRoot) throws StructureBuildingException, PostProcessingException{
		Nodes n = XQueryUtil.xquery(subOrRoot, "./group[@subType=\"hantzschWidman\"]");
		for(int i=0;i<n.size();i++) {
			Element e = (Element)n.get(i);
			String ringType =e.getAttributeValue("valType");
			int ringSize = Integer.parseInt(e.getAttributeValue("value"));
			Node pn = XOMTools.getPreviousSibling(e);
			Element prev = null;
			if(pn instanceof Element) {
				prev = (Element)pn;
			}
			ArrayList<Element> prevs = new ArrayList<Element>();
			boolean noLocants = true;
			while(prev != null && prev.getLocalName().equals("heteroatom")) {
				prevs.add(prev);
				if(prev.getAttribute("locant") != null) {
					noLocants = false;
				}
				pn = XOMTools.getPreviousSibling(prev);
				prev = null;
				if(pn instanceof Element) {
					prev = (Element)pn;
				}
			}
			boolean hasNitrogen = false;
			boolean hasSiorGeorSborPb=false;
			for(Element p : prevs){
				String heteroAtomElement =p.getAttributeValue("value");
				if (heteroAtomElement.startsWith("[") && heteroAtomElement.endsWith("]")){
					heteroAtomElement=heteroAtomElement.substring(1, heteroAtomElement.length()-1);
				}
				if (heteroAtomElement.equals("N")){
					hasNitrogen=true;
				}
				if (heteroAtomElement.equals("Si") ||
					heteroAtomElement.equals("Ge") ||
					heteroAtomElement.equals("Sb") ||
					heteroAtomElement.equals("Pb") ){
					hasSiorGeorSborPb =true;
				}
			}
			if (ringSize == 6 && ringType.equals("ring") && hasNitrogen ==false && hasSiorGeorSborPb ==true && (e.getValue().equals("in") ||e.getValue().equals("an"))){
				throw new PostProcessingException("Blocked HW system (6 member saturated ring with no nitrogen but has Si/Ge/Sb/Pb)");
			}
			String name = "";
			Collections.reverse(prevs);
			for(Element p : prevs) name += p.getValue();
			name += e.getValue();
			name = name.toLowerCase();
			if(noLocants && prevs.size() > 0) {
				if(blockedHWRings.contains(name)) {
					throw new PostProcessingException("Blocked HW system");
				}
				if (prevs.size()==2 && ringSize ==5){
				//by convention for unlocanted 5 member rings the heteroatoms are 1,3
					prevs.get(0).addAttribute(new Attribute("locant", "1"));
					prevs.get(1).addAttribute(new Attribute("locant", "3"));
				}
			}
			Fragment hwRing =state.xmlFragmentMap.get(e);
			HashSet<Element> elementsToRemove =new HashSet<Element>();
			for(Element p : prevs){//add locanted heteroatoms
				if (p.getAttribute("locant") !=null){
					String locant =p.getAttributeValue("locant");
					String elementReplacement =p.getAttributeValue("value");
					if (elementReplacement.startsWith("[") && elementReplacement.endsWith("]")){
						elementReplacement=elementReplacement.substring(1, elementReplacement.length()-1);
					}
					Atom a =hwRing.getAtomByLocantOrThrow(locant);
					a.setElement(elementReplacement);
					p.detach();
					elementsToRemove.add(p);
				}
			}
			for(Element p : elementsToRemove){
				prevs.remove(p);
			}

			//add unlocanted heteroatoms
			int defaultLocant=1;
			for(Element p : prevs){
				String elementReplacement =p.getAttributeValue("value");
				if (elementReplacement.startsWith("[") && elementReplacement.endsWith("]")){
					elementReplacement=elementReplacement.substring(1, elementReplacement.length()-1);
				}

				while (!hwRing.getAtomByLocantOrThrow(String.valueOf(defaultLocant)).getElement().equals("C")){
					defaultLocant++;
				}
				Atom a =hwRing.getAtomByLocantOrThrow(String.valueOf(defaultLocant));
				a.setElement(elementReplacement);
				p.detach();
			}


			Elements deltas = subOrRoot.getChildElements("delta");//add specified double bonds
			for (int j = 0; j < deltas.size(); j++) {
				String locantOfDoubleBond = deltas.get(j).getValue();
				Atom firstInDoubleBond;
				Atom secondInDoubleBond;
				if (locantOfDoubleBond.equals("")){
					int defaultId=hwRing.getIdOfFirstAtom();
					firstInDoubleBond =hwRing.getAtomByIDOrThrow(defaultId);
					secondInDoubleBond =hwRing.getAtomByIDOrThrow(defaultId +1);
					while (firstInDoubleBond.getSpareValency() != 0 || ValencyChecker.checkValencyAvailableForBond(firstInDoubleBond, 1) != true ||
							secondInDoubleBond.getSpareValency() != 0 || ValencyChecker.checkValencyAvailableForBond(secondInDoubleBond, 1) != true){
						defaultId++;
						firstInDoubleBond =hwRing.getAtomByIDOrThrow(defaultId);
						secondInDoubleBond =hwRing.getAtomByIDOrThrow(defaultId +1);
						if (firstInDoubleBond.getType().equals("suffix") || secondInDoubleBond.getType().equals("suffix")){
							throw new StructureBuildingException("No suitable atom found");
						}
					}
				}
				else{
					firstInDoubleBond = hwRing.getAtomByLocantOrThrow(locantOfDoubleBond);
					secondInDoubleBond = hwRing.getAtomByIDOrThrow(firstInDoubleBond.getID() +1);
				}
				Bond b =hwRing.findBond(firstInDoubleBond.getID(), secondInDoubleBond.getID());
				b.setOrder(2);
				deltas.get(j).detach();
			}
			OpsinTools.setTextChild(e, name);
		}
	}


	/**
	 * Processes constructs such as biphenyl, 1,1':4',1''-Terphenyl, 2,2'-Bipyridylium, m-Quaterphenyl
	 * @param subOrRoot
	 * @throws PostProcessingException
	 * @throws StructureBuildingException
	 */
	private void processRingAssemblies(Element subOrRoot) throws PostProcessingException, StructureBuildingException {
		Nodes ringAssemblyMultipliers = XQueryUtil.xquery(subOrRoot, "./ringAssemblyMultiplier");
		for (int i = 0; i < ringAssemblyMultipliers.size(); i++) {
			Element multiplier =(Element) ringAssemblyMultipliers.get(i);
			int mvalue = Integer.parseInt(multiplier.getAttributeValue("value"));

			/*
			 * Populate locants with locants. Two locants are required for every pair of rings to be joined.
			 * e.g. bi requires 2, ter requires 4 etc.
			 */
			ArrayList<List<String>> ringJoiningLocants =new ArrayList<List<String>>();
			Element previousEl =(Element)XOMTools.getPreviousSibling(multiplier);
			Element group =(Element)XOMTools.getNextSibling(multiplier, "group");
			if (previousEl!=null && previousEl.getLocalName().equals("ringAssemblyLocant")){
				String locantText =OpsinTools.removeDashIfPresent(previousEl.getValue());
				//special cases where often locants are meant to apply to suffixes rather than being a description of where the rings connect to each other
				if (group.getValue().equals("phen") || group.getValue().equals("hex")){
					//Find elements that can have locants but don't currently
					Nodes locantAble =  XQueryUtil.xquery(subOrRoot, "./suffix|./unsaturator|./heteroatom|./hydro");
					int locantAbleElements=locantAble.size();
					for(int j=locantAbleElements -1;j >= 0;j--) {
						if (((Element)locantAble.get(j)).getAttribute("locant") !=null){
							locantAble.remove(j);
						}
					}
					if(2 <= locantAble.size()) {
						throw new PostProcessingException("Most likely the ringAssemblyLocant: " + previousEl.getValue() + " is actually a normal locant that is supposed to apply to elements after the ring assembly");
					}
				}
				String[] perRingLocantArray =matchColon.split(locantText);
				if (perRingLocantArray.length !=(mvalue -1)){
					throw new PostProcessingException("Disagreement between number of locants(" + locantText +") and ring assembly multiplier: " + mvalue);
				}
				for (int j = 0; j < perRingLocantArray.length; j++) {
					String[] locantArray = matchComma.split(perRingLocantArray[j]);
					if (locantArray.length !=2){
						throw new PostProcessingException("missing locant, expected 2 locants: " + perRingLocantArray[j]);
					}
					ringJoiningLocants.add(Arrays.asList(locantArray));
				}
				previousEl.detach();
			}
			else if (previousEl!=null && previousEl.getLocalName().equals("locant")){
				if (previousEl.getAttribute("type")!=null && previousEl.getAttributeValue("type").equals("orthoMetaPara")){
					String locant2 =previousEl.getAttributeValue("value");
					String locant1 ="1";
					ArrayList<String> locantArrayList =new ArrayList<String>();
					locantArrayList.add("1");
					locantArrayList.add("1'");
					ringJoiningLocants.add(locantArrayList);
					for (int j = 1; j < mvalue -1; j++) {
						locantArrayList =new ArrayList<String>();
						locantArrayList.add(locant2 + StringTools.multiplyString("'", j));
						locantArrayList.add(locant1 + StringTools.multiplyString("'", j+1));
						ringJoiningLocants.add(locantArrayList);
					}
					previousEl.detach();
				}
			}

			Element elementToResolve = new Element("substituent");//temporary element containing elements that should be resolved before the ring is duplicated
			Element nextEl =(Element) XOMTools.getNextSibling(multiplier);
			if (nextEl.getLocalName().equals("structuralOpenBracket")){
				Element currentEl =nextEl;
				nextEl = (Element) XOMTools.getNextSibling(currentEl);
				currentEl.detach();
				while (nextEl !=null && !nextEl.getLocalName().equals("structuralCloseBracket")){
					currentEl =nextEl;
					nextEl = (Element) XOMTools.getNextSibling(currentEl);
					currentEl.detach();
					elementToResolve.appendChild(currentEl);
				}
				if (nextEl!=null){
					nextEl.detach();
				}
			}
			else{
				int groupFound =0;
				int inlineSuffixSeen=0;
				while (nextEl !=null){
					Element currentEl =nextEl;
					nextEl = (Element) XOMTools.getNextSibling(currentEl);
					if (groupFound==0 ||
							currentEl.getLocalName().equals("hyphen")||
							(inlineSuffixSeen ==0 && currentEl.getLocalName().equals("suffix") && currentEl.getAttributeValue("type").equals("inline") && currentEl.getAttribute("locant")==null)||
							(currentEl.getLocalName().equals("suffix") && currentEl.getAttribute("subType")!=null && currentEl.getAttributeValue("subType").equals("charge"))){
						currentEl.detach();
						elementToResolve.appendChild(currentEl);
					}
					else{
						break;
					}
					if (currentEl.getLocalName().equals("group")){
						groupFound=1;
					}
					if ((currentEl.getLocalName().equals("suffix") && currentEl.getAttributeValue("type").equals("inline"))){
						inlineSuffixSeen=1;
					}
				}
			}

			Nodes suffixes =XQueryUtil.xquery(elementToResolve, "./suffix");
			Fragment orginalFragment =state.xmlFragmentMap.get(group);
			resolveSuffixes(orginalFragment, suffixes, group);
			BuildResults buildResults =n2s.structureBuilder.resolveRootOrSubstituent(elementToResolve, null, new Nodes(), new LinkedHashSet<Fragment>());
			Fragment fragmentToDuplicate =buildResults.getMainFragment();
			group.detach();
			XOMTools.insertAfter(multiplier, group);

			int bondOrder =1;
			if (fragmentToDuplicate.getOutIDs().size()>0){
				bondOrder =fragmentToDuplicate.getOutID(0).valency;
				fragmentToDuplicate.removeOutID(0);
			}

			ArrayList<Fragment> clonedFragments = new ArrayList<Fragment>();
			for (int j = 1; j < mvalue; j++) {
				clonedFragments.add(state.fragManager.copyAndRelabel(fragmentToDuplicate, StringTools.multiplyString("'", j)));
			}
			for (int j = 0; j < mvalue-1; j++) {
				Fragment clone =clonedFragments.get(j);
				Atom atomOnParent;
				Atom atomOnLatestClone;
				if (ringJoiningLocants.size()>0){
					atomOnParent = fragmentToDuplicate.getAtomByLocantOrThrow(ringJoiningLocants.get(j).get(0));
					atomOnLatestClone = clone.getAtomByLocantOrThrow(ringJoiningLocants.get(j).get(1));
				}
				else{
					atomOnParent =fragmentToDuplicate.getAtomByIdOrNextSuitableAtomOrThrow(fragmentToDuplicate.getDefaultInID(), bondOrder);
					atomOnLatestClone = clone.getAtomByIdOrNextSuitableAtomOrThrow(clone.getDefaultInID(), bondOrder);
				}
				state.fragManager.incorporateFragment(clone, atomOnLatestClone.getID(), fragmentToDuplicate, atomOnParent.getID(), bondOrder);
			}
			OpsinTools.setTextChild(group, multiplier.getValue() +group.getValue());
			multiplier.detach();
		}
	}


	private void processComplicatedSpiroNomenclature(Element subOrRoot) {
		// TODO Auto-generated method stub

	}

	/**Looks for places where brackets should have been, and does the same
	 * as findAndStructureBrackets. E.g. dimethylaminobenzene -> (dimethylamino)benzene.
	 * @param brackets
	 * @param xmlFragmentMap: mapping between XML and fragments
	 * @param substituents: An arraylist of substituent elements
	 * @return Whether the method did something, and so needs to be called again.
	 */
	private boolean findAndStructureImplictBrackets(ArrayList<Element> substituents, HashMap<Element, Fragment> xmlFragmentMap, ArrayList<Element> brackets) throws PostProcessingException {

		for (Element theSubstituent : substituents) {
			String firstElInSubName =((Element)theSubstituent.getChild(0)).getLocalName();
			if (firstElInSubName.equals("locant") ||firstElInSubName.equals("multiplier")){
				continue;
			}

			Element theSubstituentGroup=theSubstituent.getFirstChildElement("group");
			String theSubstituentSubType = theSubstituentGroup.getAttributeValue("subType");
			String theSubstituentType = theSubstituentGroup.getAttributeValue("type");

			//Only some substituents are valid joiners (e.g. no rings are valid joiners). Need to be atleast bivalent
			if (theSubstituentGroup.getAttribute("usableAsAJoiner")==null){
				continue;
			}
			Fragment theSubstituentGroupFragment =xmlFragmentMap.get(theSubstituentGroup);

			//prevents the function getting stuck in a loop in which it recursively brackets the same elements
			Element parent = (Element)theSubstituent.getParent();
			if (parent.getLocalName().equals("bracket") &&
					parent.getAttribute("type") !=null &&
					parent.getAttributeValue("type").equals("implicit")){
				continue;
			}

			//there must be an element after the substituent for the implicit bracket to be required
			Element elementAftersubstituent =(Element)XOMTools.getNextSibling(theSubstituent);
			if (elementAftersubstituent ==null ||
					!elementAftersubstituent.getLocalName().equals("substituent") &&
					!elementAftersubstituent.getLocalName().equals("bracket") &&
					!elementAftersubstituent.getLocalName().equals("root")){
				continue;
			}

			//checks that the element before is a substituent or a bracket which will obviously include substituent/s
			//this makes sure there's more than just a substituent in the bracket
			Element elementBeforeSubstituent =(Element)XOMTools.getPreviousSibling(theSubstituent);
			if (elementBeforeSubstituent ==null||
					!elementBeforeSubstituent.getLocalName().equals("substituent") &&
					!elementBeforeSubstituent.getLocalName().equals("bracket")){
				continue;
			}

			//look for hyphen between substituents, this indicates implicit bracketing was not desired
			Element tryingToFindPreviousElement =elementBeforeSubstituent;
			while (tryingToFindPreviousElement.getChildCount()!=0){
				if (tryingToFindPreviousElement.getChild(tryingToFindPreviousElement.getChildCount()-1) instanceof Element){
					tryingToFindPreviousElement = (Element)tryingToFindPreviousElement.getChild(tryingToFindPreviousElement.getChildCount()-1);
				}
				else{
					break;
				}
			}
			if (tryingToFindPreviousElement.getLocalName().equals("hyphen")){
				continue;
			}

			//prevents alkyl chains being bracketed together e.g. ethylmethylamine
			//...unless it's something like 2-methylethyl where the first appears to be locanted onto the second
			Nodes groupNodes = XQueryUtil.xquery(elementBeforeSubstituent, ".//group");//one for a substituent, possibly more for a bracket
			Element group =((Element)groupNodes.get(groupNodes.size()-1));
			if (group==null){throw new PostProcessingException("No group where group was expected");}
			if (theSubstituentType.equals("chain") && theSubstituentSubType.equals("alkaneStem") &&
					group.getAttributeValue("type").equals("chain") && (group.getAttributeValue("subType").equals("alkaneStem") || group.getAttributeValue("subType").equals("alkaneStem-irregular"))){
				int placeInImplicitBracket =0;

				Element elementAfterGroup=(Element)XOMTools.getNextSibling(group, "suffix");
				//if the alkane ends in oxy or amine it's not a pure alkane (other suffixes don't need to be considered as they would produce silly structures)
				if (elementAfterGroup !=null && matchInlineSuffixesThatAreAlsoGroups.matcher(elementAfterGroup.getValue()).matches()){
					placeInImplicitBracket =1;
				}
				//look for locants and check whether they appear to be referring to the other chain
				if (placeInImplicitBracket==0){
					Elements childrenOfElementBeforeSubstituent  =elementBeforeSubstituent.getChildElements();
					Integer foundLocantNotReferringToChain =null;
					for (int i = 0; i < childrenOfElementBeforeSubstituent.size(); i++) {
						String currentElementName = childrenOfElementBeforeSubstituent.get(i).getLocalName();
						if (currentElementName.equals("hyphen") || currentElementName.equals("stereoChemistry")){
						}
						else if (currentElementName.equals("locant")){
							String locantText =childrenOfElementBeforeSubstituent.get(i).getAttributeValue("value");
							if(!theSubstituentGroupFragment.hasLocant(locantText)){
								foundLocantNotReferringToChain=1;
								break;
							}
							else{
								foundLocantNotReferringToChain=0;
							}
						}
						else{
							break;
						}
					}
					if (foundLocantNotReferringToChain !=null && foundLocantNotReferringToChain==0){//a locant was found a it referred to the other chain
						placeInImplicitBracket=1;
					}
				}
				if (placeInImplicitBracket==0){
					continue;
				}
			}

			Element bracket = new Element("bracket");
			bracket.addAttribute(new Attribute("type", "implicit"));

			/*
			 * locant may need to be moved. This occurs when the group in elementBeforeSubstituent is not supposed to be locanted onto
			 *  theSubstituentGroup
			 *  e.g. 2-aminomethyl-1-chlorobenzene where the 2 refers to the benzene NOT the methyl
			 */
			ArrayList<Element> locantElements =new ArrayList<Element>();
			Elements childrenOfElementBeforeSubstituent  =elementBeforeSubstituent.getChildElements();
			int nonStereoChemistryLocants =0;
			for (int i = 0; i < childrenOfElementBeforeSubstituent.size(); i++) {
				String currentElementName = childrenOfElementBeforeSubstituent.get(i).getLocalName();
				if (currentElementName.equals("hyphen")){
				}
				else if (currentElementName.equals("stereoChemistry")){
					locantElements.add(childrenOfElementBeforeSubstituent.get(i));
				}
				else if (currentElementName.equals("locant")){
					locantElements.add(childrenOfElementBeforeSubstituent.get(i));
					nonStereoChemistryLocants++;
				}
				else{
					break;
				}
			}

			//either all locants will be moved, or none
			Integer moveLocants=null;
			int flag=0;
			for (Element locant : locantElements) {
				if (!locant.getLocalName().equals("locant")){
					continue;//ignore stereochemistry elements, atleast for the moment
				}
				String locantText = locant.getAttributeValue("value");

				if (theSubstituentGroupFragment.hasLocant("2")){//if only has locant 1 then assume substitution onto it not intended
					if(!theSubstituentGroupFragment.hasLocant(locantText)){
						flag=1;
					}
				}
				else{
					flag=1;
				}
			}
			if (flag==0){
				moveLocants =0;//if the locant applies to the theSubstituentGroup then don't move
			}

			flag =0;
			if (moveLocants ==null){
				for (Element locant : locantElements) {
					if (!locant.getLocalName().equals("locant")){
						continue;//ignore stereochemistry elements, atleast for the moment
					}
					String locantText = locant.getAttributeValue("value");
					if (!checkLocantPresentOnPotentialRoot(theSubstituent, locantText)){
						flag =1;
					}
				}
				if (flag==0){
					moveLocants = 1;//if the locant applies to a group which is not theSubstituentGroup then move
				}
				else{
					moveLocants =0;
				}
			}
			if (moveLocants==1 && nonStereoChemistryLocants>1){
				Element shouldBeAMultiplierNode = (Element)XOMTools.getNextSibling(locantElements.get(locantElements.size()-1));
				if (shouldBeAMultiplierNode instanceof Element){
					Element shouldBeAGroupOrSubOrBracket = (Element)XOMTools.getNextSibling(shouldBeAMultiplierNode);
					if (shouldBeAGroupOrSubOrBracket instanceof Element && shouldBeAMultiplierNode.getLocalName().equals("multiplier")
							&& shouldBeAGroupOrSubOrBracket.getLocalName().equals("group") ||
							shouldBeAGroupOrSubOrBracket.getLocalName().equals("substituent") ||
							shouldBeAGroupOrSubOrBracket.getLocalName().equals("bracket")){
						if (matchInlineSuffixesThatAreAlsoGroups.matcher(theSubstituentGroup.getValue()).matches()){//e.g. 4, 4'-dimethoxycarbonyl-2, 2'-bioxazole
							locantElements.add(shouldBeAMultiplierNode);
						}
						else{//don't bracket complex multiplied substituents
							continue;
						}
					}
					else{
						moveLocants =0;
					}
				}
				else{
					moveLocants =0;
				}
			}
			if (moveLocants==1){
				for (int i = 0; i < locantElements.size(); i++) {
					locantElements.get(i).detach();
					bracket.appendChild(locantElements.get(i));
				}
			}

			/*
			 * A special case when a multiplier should be moved
			 * e.g. tripropan-2-yloxyphosphane -->tri(propan-2-yloxy)phosphane
			 */
			if (locantElements.size()==0 && matchInlineSuffixesThatAreAlsoGroups.matcher(theSubstituentGroup.getValue()).matches()){
				for (int i = 0; i < childrenOfElementBeforeSubstituent.size(); i++) {
					String currentElementName = childrenOfElementBeforeSubstituent.get(i).getLocalName();
					if (currentElementName.equals("hyphen")){
					}
					else if (currentElementName.equals("multiplier")){
						if (i +1 < childrenOfElementBeforeSubstituent.size()){
							if (childrenOfElementBeforeSubstituent.get(i+1).getLocalName().equals("group")){
								childrenOfElementBeforeSubstituent.get(i).detach();
								bracket.appendChild(childrenOfElementBeforeSubstituent.get(i));
							}
						}
					}
					else{
						break;
					}
				}
			}

			int startIndex=parent.indexOf(elementBeforeSubstituent);
			int endIndex=parent.indexOf(theSubstituent);
			for(int i = 0 ; i <= (endIndex-startIndex);i++) {
				Node n = parent.getChild(startIndex);
				n.detach();
				bracket.appendChild(n);
			}
			parent.insertChild(bracket, startIndex);
			brackets.add(bracket);
			return true;
		}
		return false;
	}


	/** Match each locant to the next applicable "feature". Assumes that processLocants
	 * has done a good job and rejected cases where no match can be made.
	 * Handles cases where the locant is in front of the group but the feature is after the group
	 *
	 * @param elem The substituent/root/bracket to look for locants in.
	 * @throws PostProcessingException
	 * @throws StructureBuildingException
	 */
	private void matchLocantsToIndirectFeatures(Element elem) throws PostProcessingException, StructureBuildingException {
		Elements locants = elem.getChildElements("locant");

		/* Root fragments (or the root in a bracket) can have prefix-locants
		 * that work on suffixes - (2-furyl), 2-propanol, (2-propylmethyl), (2-propyloxy), 2'-Butyronaphthone.
		 */
		locants = elem.getChildElements("locant");
		Element group =elem.getFirstChildElement("group");
		if (locants.size()>0 && group!=null){
			Fragment thisFrag =state.xmlFragmentMap.get(group);
			Nodes locantAble =null;
			ArrayList<Element> locantsToAssignToIndirectFeatures = new ArrayList<Element>();
			for (int i = locants.size()-1; i >=0 ; i--) {
				Element locant =locants.get(i);
				Element temp =locant;
				while (temp!=null && temp.getLocalName().equals("locant")){
					temp = (Element) OpsinTools.getNextSiblingNonHyphen(temp);
				}
				if (temp!=null && temp.getLocalName().equals("multiplier")){//locant should not be followed by a multiplier. c.f. 1,2,3-tributyl 2-acetyloxypropane-1,2,3-tricarboxylate
					continue;
				}
				String locantValue =locant.getAttributeValue("value");
				if (!checkLocantPresentOnPotentialRoot(elem, locantValue)){
					if (thisFrag.hasLocant(locantValue)){//locant not available elsewhere and is available on the group associated with this element
						if (locantAble ==null){
							//Find elements that can have locants but don't currently
							locantAble = XQueryUtil.xquery(elem, "./suffix|./unsaturator|./heteroatom|./hydro");
							int locantAbleElements=locantAble.size();
							for(int j=locantAbleElements -1;j >= 0;j--) {
								if (((Element)locantAble.get(j)).getAttribute("locant") !=null){
									locantAble.remove(j);
								}
							}
						}
						if (locantsToAssignToIndirectFeatures.size() < locantAble.size()){
							locantsToAssignToIndirectFeatures.add(0, locant);//last in first out
						}
					}
					else{//usually indicates the name will fail unless the suffix has the locant or heteroatom replacement will create the locant
						List<Fragment> suffixes =state.xmlSuffixMap.get(group);
						//I do not want to assign element locants as in locants on the suffix as I currently know of no examples where this actually occurs
						if (matchElementSymbol.matcher(locantValue).matches()==true || matchCompoundLocant.matcher(locantValue).find()==true){
							continue;
						}
						for (Fragment suffix : suffixes) {
							if (suffix.hasLocant(locantValue)){
								suffix.setDefaultInID(suffix.getAtomByLocantOrThrow(locantValue).getID());
								locant.detach();
							}
						}
					}
				}
			}
			for (Element locant : locantsToAssignToIndirectFeatures) {
				Element locantAbleElement =(Element)locantAble.get(0);
				locantAble.remove(0);
				String locantValue =locant.getAttributeValue("value");
				//If a compound Locant e.g. 1(6) is detected add a compound locant attribute
				Matcher matches =matchCompoundLocant.matcher(locantValue);
				if (matches.find()==true){
					locantAbleElement.addAttribute(new Attribute("compoundLocant", matches.group(1)));
					locantValue = matches.replaceAll("");
				}
				locantAbleElement.addAttribute(new Attribute("locant", locantValue));
				locant.detach();
			}
		}

		//put di-carbon modifying suffixes e.g. oic acids, aldehydes on opposite ends of chain
		Nodes suffixNodes = XQueryUtil.xquery(elem, ".//suffix");
		for (int i = 0; i < suffixNodes.size()-1; i++) {
			Element diCarbonModifyingSuffix1 =(Element)suffixNodes.get(i);
			if (matchSuffixesThatGoAtEndOfChainsByDefault.matcher(diCarbonModifyingSuffix1.getAttributeValue("value")).matches()){
				if ((diCarbonModifyingSuffix1).getAttribute("locant")==null && XOMTools.getNextSibling(diCarbonModifyingSuffix1) != null){
					Element diCarbonModifyingSuffix2 =(Element)XOMTools.getNextSibling(diCarbonModifyingSuffix1);
					if (diCarbonModifyingSuffix2.getLocalName().equals("suffix") &&
							matchSuffixesThatGoAtEndOfChainsByDefault.matcher(diCarbonModifyingSuffix2.getAttributeValue("value")).matches() &&
							diCarbonModifyingSuffix2.getAttribute("locant")==null){
						Element hopefullyAChain = (Element) XOMTools.getPreviousSibling((Element)diCarbonModifyingSuffix1, "group");
						if (hopefullyAChain != null && hopefullyAChain.getAttributeValue("type").equals("chain")){
							((Element)diCarbonModifyingSuffix1).addAttribute(new Attribute("locant", "1"));
							((Element)diCarbonModifyingSuffix2).addAttribute(new Attribute("locant", String.valueOf(state.xmlFragmentMap.get(hopefullyAChain).getChainLength())));
							break;
						}
					}
				}
			}
		}
	}


	/**
	 * Resolves the effects of remaining suffixes and attaches resolved suffixes.
	 * @param subOrRoot: The sub/root to look in
	 * @throws StructureBuildingException
	 */
	private void resolveRemainingSuffixes(Element subOrRoot) throws StructureBuildingException{
		Element group =subOrRoot.getFirstChildElement("group");
		Fragment thisFrag = state.xmlFragmentMap.get(group);
		Nodes suffixes =XQueryUtil.xquery(subOrRoot, "./suffix");
		resolveSuffixes(thisFrag, suffixes, group);
	}

	/**Process the effects of suffixes upon a fragment.
	 *
	 * @param frag The fragment to which to add the suffixes
	 * @param suffixes The suffix elements for a fragment.
	 * @param group The element for the group that the fragment will attach to
	 * @throws StructureBuildingException If the suffixes can't be resolved properly.
	 */
	private void resolveSuffixes(Fragment frag, Nodes suffixes,  Element group) throws StructureBuildingException {
		int firstAtomID =frag.getIdOfFirstAtom();//typically equivalent to locant 1
		int defaultIdToAddTo= firstAtomID;
		String groupType = frag.getType();
		String subgroupType = frag.getSubType();
		List<Fragment> suffixList =state.xmlSuffixMap.get(group);
		for(int i=0;i<suffixes.size();i++) {
			Element suffix = (Element)suffixes.get(i);
			String suffixValue = suffix.getAttributeValue("value");

			Nodes suffixRuleNodes = XQueryUtil.xquery(suffixRulesDoc ,"/suffixRulesList/"+
					"suffixRules[@type=\"" + groupType + "\"]/" +
					"rule[@value=\"" + suffixValue + "\" " +
							"and (not(@subType) or @subType=\"" + subgroupType +"\")]");
			if(suffixRuleNodes == null) throw new StructureBuildingException();

			Element suffixRule;
			if(suffixRuleNodes.size() == 1 &&
					suffixRuleNodes.get(0) instanceof Element) {
				suffixRule = (Element) suffixRuleNodes.get(0);
			} else {
				throw new StructureBuildingException();
			}

			String locant = StructureBuilder.getLocant(suffix);
			int idOnParentFragToUse=0;
			if (!locant.equals("0")){
				idOnParentFragToUse =frag.getIDFromLocantOrThrow(locant);
			}
			if (idOnParentFragToUse==0 && suffix.getAttribute("locantID")!=null){
				idOnParentFragToUse = Integer.parseInt(suffix.getAttributeValue("locantID"));
			}

			String compoundLocant = null;
			if (suffix.getAttribute("compoundLocant")!=null){
				compoundLocant=suffix.getAttributeValue("compoundLocant");
			}
			Elements suffixRuleTags = suffixRule.getChildElements();
			for(int j=0;j<suffixRuleTags.size();j++) {
				Element suffixRuleTag = suffixRuleTags.get(j);

				if(suffixRuleTag.getLocalName().equals("addgroup")) {
					//take the first suffix out of the list, it should of been added in the same order that it is now being read.
					Fragment addFrag = suffixList.get(0);
					suffixList.remove(0);

					int bondOrder = addFrag.getInValency();

					if(idOnParentFragToUse==0) {
						if(suffixRuleTag.getAttributeValue("ketoneLocant") != null && suffixRuleTag.getAttributeValue("ketoneLocant").equals("yes")) {
							if(defaultIdToAddTo == frag.getDefaultInID()) defaultIdToAddTo = state.fragManager.findKetoneLocant(frag, defaultIdToAddTo);
							idOnParentFragToUse =defaultIdToAddTo;
							defaultIdToAddTo++;
						}
						else{
							idOnParentFragToUse =defaultIdToAddTo;
						}
						idOnParentFragToUse =frag.getAtomByIdOrNextSuitableAtomOrThrow(idOnParentFragToUse, bondOrder).getID();
					}

					state.fragManager.incorporateFragment(addFrag, addFrag.getDefaultInID(), frag, idOnParentFragToUse, bondOrder);
					if(suffixRuleTag.getAttribute("setsOutID") != null) {
						List<OutID> outIDs = addFrag.getOutIDs();
						for (OutID outID : outIDs) {
							frag.addOutID(outID);
						}
					}
					else if(suffixRuleTag.getAttribute("setsDefaultInID") != null) {
						frag.setDefaultInID(addFrag.getDefaultInID());
					}
					else if	(suffixRuleTag.getAttribute("setsFunctionalID") != null) {
						frag.addFunctionalIDs(addFrag.getFunctionalIDs());
					}
				} else if(suffixRuleTag.getLocalName().equals("doublebond")) {
					if(idOnParentFragToUse==0){
						idOnParentFragToUse =defaultIdToAddTo;
						defaultIdToAddTo +=2;
					}
					if (compoundLocant!=null){
						state.fragManager.unsaturate(idOnParentFragToUse, compoundLocant, 2, frag);
					}
					else{
						state.fragManager.unsaturate(idOnParentFragToUse, 2, frag);
					}
				} else if(suffixRuleTag.getLocalName().equals("triplebond")) {
					if(idOnParentFragToUse==0){
						idOnParentFragToUse =defaultIdToAddTo;
						defaultIdToAddTo +=2;
					}
					if (compoundLocant!=null){
						state.fragManager.unsaturate(idOnParentFragToUse, compoundLocant, 3, frag);
					}
					else{
						state.fragManager.unsaturate(idOnParentFragToUse, 3, frag);
					}
				} else if(suffixRuleTag.getLocalName().equals("changecharge")) {
					if(idOnParentFragToUse==0){
						idOnParentFragToUse =defaultIdToAddTo;
						defaultIdToAddTo++;
					}
					state.fragManager.setCharge(idOnParentFragToUse, suffixRuleTag.getAttributeValue("charge"), frag);
				}else if(suffixRuleTag.getLocalName().equals("setOutID")) {
					if(suffixRuleTag.getAttribute("outValency") != null) {
						if(idOnParentFragToUse!=0){
							frag.addOutID(idOnParentFragToUse, Integer.parseInt(suffixRuleTag.getAttributeValue("outValency")), true);
						}
						else{
							frag.addOutID(firstAtomID, Integer.parseInt(suffixRuleTag.getAttributeValue("outValency")), false);
						}
					}
					else{
						if(idOnParentFragToUse!=0){
							frag.addOutID(idOnParentFragToUse, 1, true);
						}
						else{
							frag.addOutID(firstAtomID, 1, false);
						}
					}
				}
			}
		}
	}

	/**
	 * Uses the number of outIDs that are definitely present to assign the number of outIDs on substituents that can have a variable number of outIDs
	 * Hence at this point it can be determined if a multi radical susbtituent is present in the name
	 * This would be expected in multiplicative nomenclature and is noted in the state so that the StructureBuilder knows to resolve the
	 * section of the name from that point onwards in a left to right manner rather than right to left
	 * @param subsOrRootsToRemove
	 * @param subOrRoot: The sub/root to look in
	 * @return boolean whether multiplicative nomenclature was encountered
	 * @throws StructureBuildingException
	 * @throws PostProcessingException
	 */
	private void assignOutIDsToTrivialMultiRadicals(Element subOrRoot, ArrayList<Element> subsOrRootsToRemove) throws StructureBuildingException, PostProcessingException{
		Element group =subOrRoot.getFirstChildElement("group");
		Fragment thisFrag = state.xmlFragmentMap.get(group);
		Element previousGroup =(Element) OpsinTools.getPreviousGroup(group);
		Element possibleGroupMultiplier =(Element)subOrRoot.getChild(0);
		Element possibleSubstituentMultiplier = (Element)group.getParent().getParent().getChild(0);
		Element firstElInNextSilbingSubOrRoot =(Element) OpsinTools.getNextNonHyphenAtSameOrLowerLevel(subOrRoot);
		boolean groupMultiplier =false;
		if (possibleGroupMultiplier.getLocalName().equals("multiplier") && !possibleGroupMultiplier.getAttributeValue("value").equals("1")){
			groupMultiplier =true;
		}
		boolean substituentMultiplier =false;
		if (possibleSubstituentMultiplier != null && possibleSubstituentMultiplier.getLocalName().equals("multiplier") && !possibleSubstituentMultiplier.getAttributeValue("value").equals("1")){
			substituentMultiplier =true;
		}

		boolean followingSiblingElStartsWithAMultiplier =false;
		if (firstElInNextSilbingSubOrRoot!=null && firstElInNextSilbingSubOrRoot.getLocalName().equals("multiplier") && !firstElInNextSilbingSubOrRoot.getAttributeValue("value").equals("1")){
			followingSiblingElStartsWithAMultiplier =true;
		}

		//A multiradical which is multiplied and is not preceded by locants (otherwise locants would be present before the multiplier and hence groupMultiplier would be false)
		if (group.getAttribute("outIDs") !=null){
			if (group.getValue().equals("amine")){//amine is a special case as it shouldn't technically be allowed but is allowed due to it's common usage in EDTA
				if (previousGroup==null || state.xmlFragmentMap.get(previousGroup).getOutIDs().size() < 2){//must be preceded by a multi radical
					throw new PostProcessingException("Invalid use of amine as a substituent!");
				}
			}
			group.addAttribute(new Attribute ("isAMultiRadical", "yes"));

			String[] radicalPositions = matchComma.split(group.getAttributeValue("outIDs"));
			int firstIdInFrag =thisFrag.getIdOfFirstAtom();


			int multiplier =1;
			if (followingSiblingElStartsWithAMultiplier ==true){
				multiplier= Integer.parseInt(firstElInNextSilbingSubOrRoot.getAttributeValue("value"));
			}

			for (int i = radicalPositions.length -1; i >=0 ; i--) {
				String radicalID =radicalPositions[i];
				if (radicalID.endsWith("?")){//outID is context dependant e.g. oxy in methyloxy vs oxydibenzene
					radicalID=radicalID.substring(0, radicalID.length()-1);
					if (multiplier >1 && groupMultiplier ==false  && substituentMultiplier ==false){
						multiplier--;
						thisFrag.addOutID(firstIdInFrag + Integer.parseInt(radicalID) -1, 1, true);//c.f. oxydicyclohexane, 3-methyloxydicyclohexane, methyleneoxydicyclohexane
					}
					else{
						if (previousGroup==null){//something like carbonyl dichloride
							thisFrag.addOutID(firstIdInFrag + Integer.parseInt(radicalID) -1, 1, true);
						}
						else{
							thisFrag.setDefaultInID(firstIdInFrag + Integer.parseInt(radicalID) -1);
							//e.g. 3,3'-tetramethylenedioxydibenzoic acid, the oxy only has 1 outID
						}
					}
				}
				else if (i!=0 || (groupMultiplier ==false && substituentMultiplier ==false)  ) {
					thisFrag.addOutID(firstIdInFrag + Integer.parseInt(radicalID) -1, 1, true);
				}
				else{
					thisFrag.setDefaultInID(firstIdInFrag + Integer.parseInt(radicalID) -1);
				}
			}
		}

		//resolves for example trimethylene to propan-1,3-diyl
		if ((thisFrag.getOutIDs().size()>=2 || group.getAttribute("outIDs") !=null) && groupMultiplier ==true && (previousGroup==null || Integer.parseInt(possibleGroupMultiplier.getAttributeValue("value"))>2)){
			int multiplierValue =Integer.parseInt(possibleGroupMultiplier.getAttributeValue("value"));
			List<OutID> outIDs =thisFrag.getOutIDs();
			if (outIDs.size()==1 ){
				thisFrag.addOutID(new OutID(outIDs.get(0)));
			}
			Fragment clone = state.fragManager.copyAndRelabel(thisFrag);
			clone.removeOutID(0);
			for (int i = 1; i < multiplierValue; i++) {
				Fragment tempFrag = state.fragManager.copyAndRelabel(clone);
				OutID outID =outIDs.get(outIDs.size()-1);
				Atom outAtom =thisFrag.getAtomByIDOrThrow(outID.id);
				Atom inAtom =tempFrag.getAtomByIDOrThrow(tempFrag.getDefaultInID());
				state.fragManager.incorporateFragment(tempFrag, inAtom.getID(), thisFrag, outAtom.getID(), outID.valency);
				outIDs.addAll(tempFrag.getOutIDs());
				outIDs.remove(outID);
			}
			state.fragManager.removeFragment(clone);
			OpsinTools.setTextChild(group, possibleGroupMultiplier.getValue() +group.getValue());
			possibleGroupMultiplier.detach();
		}
		possibleGroupMultiplier =(Element)subOrRoot.getChild(0);
		groupMultiplier =false;
		if (possibleGroupMultiplier.getLocalName().equals("multiplier") && !possibleGroupMultiplier.getAttributeValue("value").equals("1")){
			groupMultiplier =true;
		}

		//resolves for example ethyleneoxy into a single structure with outIDs on the first carbon and on the oxygen.
		//The two substituents must share the same parent e.g. not carbonyl)ethylene
		if (previousGroup != null && groupMultiplier==false && previousGroup.getParent().getParent()==subOrRoot.getParent()){
			Fragment previousFrag = state.xmlFragmentMap.get(previousGroup);
			Element previousSubstituent =(Element) previousGroup.getParent();
			if (thisFrag.getOutIDs().size()>=2 && (previousFrag.getOutIDs().size()>=2 || previousGroup.getAttribute("outIDs") !=null)
					&& previousSubstituent.getChildElements().size()==1){
				List<OutID> outIDs =previousFrag.getOutIDs();
				OutID outID =outIDs.get(outIDs.size()-1);
				state.fragManager.incorporateFragment(thisFrag, outID.id, previousFrag, thisFrag.getOutID(0).id, outID.valency);
				outIDs.remove(outID);
				thisFrag.removeOutID(0);
				outIDs.addAll(thisFrag.getOutIDs());
				OpsinTools.setTextChild(previousGroup, previousGroup.getValue() +group.getValue());
				state.xmlFragmentMap.remove(group);
				subsOrRootsToRemove.add(subOrRoot);
				subOrRoot.getParent().removeChild(subOrRoot);
			}
		}

		//check on whether this multiradical is used in multiplicative nomenclature.
		//If it is then it may need its first outID removed. e.g. [ethylenebis(sulfane-diyl)]dicyclohexane
		if (thisFrag.getOutIDs().size()>1 &&  group.getAttribute("outIDs") ==null){
			group.addAttribute(new Attribute ("isAMultiRadical", "yes"));
			//Preceded by a multiplier
			if (groupMultiplier==true || substituentMultiplier ==true){
				thisFrag.setDefaultInID(thisFrag.getOutID(0).id);
				thisFrag.removeOutID(0);
			}
		}
	}

	/**
	 * Uses the number of outIDs that are present to assign the number of outIDs on substituents that can have a variable number of outIDs
	 * Hence at this point it can be determined if a multi radical susbtituent is present in the name
	 * This would be expected in multiplicative nomenclature and is noted in the state so that the StructureBuilder knows to resolve the
	 * section of the name from that point onwards in a left to right manner rather than right to left
	 * @param subOrRoot: The sub/root to look in
	 * @throws PostProcessingException
	 */
	private void identifyMultiplicativeNomenclature(Element subOrRoot) throws PostProcessingException{
		Element group =subOrRoot.getFirstChildElement("group");
		Fragment thisFrag = state.xmlFragmentMap.get(group);
		Element possibleMultiplier= (Element)OpsinTools.getNextNonHyphen(subOrRoot.getChild(subOrRoot.getChildCount()-1));
		int outIDsSize = thisFrag.getOutIDs().size();
//		System.out.println("OUDIDS" +thisFrag.getOutIDs().size());
//		System.out.println(subOrRoot.toXML());
		if (possibleMultiplier !=null){//i.e. this isn't the root
			if (outIDsSize >=2 && possibleMultiplier.getLocalName().equals("multiplier")){//multiplicative nomenclaturee
				Element parentWord =OpsinTools.getParentWord(group);
				if (state.firstMultiRadical.get(parentWord)==null){
					state.firstMultiRadical.put(parentWord, (Element)group.getParent());
				}
			}
			else if (outIDsSize>=2){ //e.g. methylenecyclohexane
				List<OutID> outIDs =thisFrag.getOutIDs();
				int flag=0;
				int id =0;
				for (OutID outID : outIDs) {
					if (id ==0){
						id =outID.id;
					}
					else{
						if (id!=outID.id){
							flag=1;
						}
					}
				}
				if (flag==1){
					if (!state.wordRule.equals("simple") && !state.wordRule.equals("binaryOrOther")){ //cludge to allow certain esters to still work
						//e.g. pentane-1,5-diyl bis(chloroformate)
					}
					else{
						throw new PostProcessingException("Currently unhandled use of multi radicals!");
					}
				}
				else{//gives, for example, the methylene of methylenecyclohexane one outID of valency 2
					outIDs.clear();
					thisFrag.addOutID(id, outIDsSize, true);
				}
			}
			else if (possibleMultiplier.getLocalName().equals("multiplier") && outIDsSize==1){//special case where something like benzylidene is being used as if it meant benzdiyl for multiplicative nomenclature
				//this is allowed in the IUPAC 79 recommendations but not recommended in the current recommendations
				OutID outID =thisFrag.getOutID(0);
				if (outID.valency == Integer.parseInt(possibleMultiplier.getAttributeValue("value"))){
					Element parentWord =OpsinTools.getParentWord(group);
					Element root =parentWord.getFirstChildElement("root");
					if (!parentWord.getAttributeValue("type").equals("full")|| ((Element)root.getChild(0)).getLocalName().equals("multiplier")){//checks that the name appears to be multiplicative
						int value =outID.valency;
						outID.valency=1;
						for (int i = 1; i < value; i++) {
							thisFrag.addOutID(outID.id, 1, outID.setExplicitly);
						}

						if (state.firstMultiRadical.get(parentWord)==null){
							state.firstMultiRadical.put(parentWord, (Element)group.getParent());
						}
					}
				}
			}
		}
//		System.out.println("OUDIDS" +thisFrag.getOutIDs().size());
//		System.out.println(subOrRoot.toXML());
	}

	/**
	 * Multiplies out groups in the root
	 * for example in ethylenedinitrilotetraacetic acid this function will clone the acetic acid root 3 times
	 * @param rootList A list containing one root element
	 * @return returns the rootList with more roots (if the function does anything)
	 * @throws PostProcessingException
	 * @throws StructureBuildingException
	 */
	private ArrayList<Element> processMultiplicativeNomenclature(ArrayList<Element> rootList) throws PostProcessingException, StructureBuildingException {
		Element root =rootList.get(0);
		Elements multipliers = root.getChildElements("multiplier");
		boolean rootInABracket =false;
		if (((Element)root.getParent()).getLocalName().equals("bracket")){
			rootInABracket=true;
		}
		if(multipliers.size() == 0 && rootInABracket){//e.g. 1,1'-ethynediylbis(1-cyclopentanol)
			multipliers = ((Element)root.getParent()).getChildElements("multiplier");
		}
		if(multipliers.size() == 1) {
			Element multiplier =multipliers.get(0);
			if (OpsinTools.getPreviousNonHyphen(multiplier)==null){
				return rootList;//The group was something inappropriate e.g. dichloride
			}
			Nodes locants =XQueryUtil.xquery(multiplier.getParent(), ".//multiplicativeLocant");
			multiplier.detach();
			int multiVal = Integer.parseInt(multiplier.getAttributeValue("value"));
			Nodes originalGroups =XQueryUtil.xquery(root, ".//group");
			if (originalGroups.size()!=1){
				throw new PostProcessingException("Root elements are expected to contain only one group");
			}
			Element group =(Element)originalGroups.get(0);
			BuildResults originalFragmentBuildResults =n2s.structureBuilder.resolveRootOrSubstituent(root, null, new Nodes(), new LinkedHashSet<Fragment>());
			Fragment builtFragment =originalFragmentBuildResults.getMainFragment();
			ArrayList<Fragment> clonedFragments = new ArrayList<Fragment>();

			for (int j = 1; j < multiVal; j++) {
				clonedFragments.add(state.fragManager.copyAndRelabel(builtFragment, StringTools.multiplyString("'", j)));
			}
			if (locants.size()==0){
				builtFragment.setDefaultInID(builtFragment.getAtomByIdOrNextSuitableAtomOrThrow(builtFragment.getDefaultInID(), 1).getID());
			}
			else if (locants.size() == clonedFragments.size() +1){
				Element locant =(Element)locants.get(0);
				String locantValue =locant.getAttributeValue("value");
				locant.detach();
				builtFragment.setDefaultInID(builtFragment.getAtomByLocantOrThrow(locantValue).getID());
				for (int i = 1; i <= clonedFragments.size(); i++) {
					locant =(Element)locants.get(i);
					locantValue = locant.getAttributeValue("value");
					locant.detach();
					Fragment clone =clonedFragments.get(i-1);
					clone.setDefaultInID(clone.getAtomByLocantOrThrow(locantValue).getID());
				}
			}
			else{
				throw new PostProcessingException("Mismatch between number of locants and number of roots");
			}

			Elements children =root.getChildElements();
			for (int i = 0; i < children.size(); i++) {
				Element child=children.get(i);
				if (!child.equals(group)){
					child.detach();
				}
			}

			for (int i = 0; i < clonedFragments.size(); i++) {
				Fragment clone =clonedFragments.get(i);
				Element newRoot =new Element("root");
				Element newGroup =new Element(group);
				state.xmlFragmentMap.put(newGroup, clone);
				state.xmlSuffixMap.put(newGroup, new ArrayList<Fragment>());
				newRoot.appendChild(newGroup);
				rootList.add(newRoot);
				if (rootInABracket){
					Element newBracket =new Element("bracket");
					newBracket.appendChild(newRoot);
					XOMTools.insertAfter(root.getParent(), newBracket);
				}else{
					XOMTools.insertAfter(root, newRoot);
				}
			}
		}
		return rootList;
	}


	/**Multiplies substituents and brackets. Eg. dimethylamine -> methylmethylamine,
	 * di(chloromethyl)amine -> (chloromethyl)(chloromethyl)amine
	 * @param elem The substituent/root/bracket to look for multipliers in.
	 * @throws PostProcessingException
	 */
	private void processGroupMultipliers(Element elem) throws PostProcessingException {
		/* There should now only be substituent locants and multipliers here */
		Elements locants = elem.getChildElements("locant");
		Elements multipliers = elem.getChildElements("multiplier");
		if(multipliers.size() == 1) {
			multipliers.get(0).detach();
			int multiVal = Integer.parseInt(multipliers.get(0).getAttributeValue("value"));
			for(int i=0;i<locants.size();i++) {//locants detached
				locants.get(i).detach();
			}
			/* Look for multipliers on whole words eg. _diethyl_ hexanedioate */
			Element parentElem =(Element)elem.getParent();
			if(parentElem.getLocalName().equals("word") &&
					XOMTools.getNextSibling(elem) == null &&
					XOMTools.getPreviousSibling(elem) == null) {
				int index =parentElem.indexOf(elem);
				for(int i=multiVal -1; i>=1; i--) {
					Element clone = state.fragManager.cloneElement(parentElem, state);
					if(locants.size() > 0 ) {
						((Element) clone.getChild(index)).insertChild(locants.get(i), 0);
					}
					XOMTools.insertAfter(elem.getParent(), clone);
					Nodes bracketsOrSubstituents = XQueryUtil.xquery(clone, ".//bracket|.//substituent");
					for (int j = 0; j < bracketsOrSubstituents.size(); j++) {
						assignSubOrBracketLocants((Element)bracketsOrSubstituents.get(j));
					}
					if(elem.getLocalName().equals("bracket")) {
						Nodes brackets = XQueryUtil.xquery(clone, ".//bracket");
						for(int j=0;j<brackets.size();j++) {
							processGroupMultipliers((Element)brackets.get(j));
						}
					}
				}
				if(locants.size() > 0 ) {//locant reattached to original group
					elem.insertChild(locants.get(0), 0);
				}
			} else {
				for(int i=multiVal -1; i>=1; i--) {
					Element clone = state.fragManager.cloneElement(elem, state);
					if(locants.size() > 0 ) {
						clone.insertChild(locants.get(i), 0);
					}
					XOMTools.insertAfter(elem, clone);
					Nodes bracketsOrSubstituents = XQueryUtil.xquery(clone, ".//bracket|.//substituent");
					for (int j = 0; j < bracketsOrSubstituents.size(); j++) {
						assignSubOrBracketLocants((Element)bracketsOrSubstituents.get(j));
					}
					assignSubOrBracketLocants(clone);
					if(elem.getLocalName().equals("bracket")) {
						Nodes brackets = XQueryUtil.xquery(clone, ".//bracket");
						for(int j=0;j<brackets.size();j++) {
							processGroupMultipliers((Element)brackets.get(j));
						}
					}
				}
				if(locants.size() > 0 ) {//locant reattached to original group
					elem.insertChild(locants.get(0), 0);
				}
			}
		}
		assignSubOrBracketLocants(elem);
	}

	/**
	 * Assigns locants to substituents/brackets
	 * @param subOrBracketOrRoot
	 * @throws PostProcessingException
	 */
	private void assignSubOrBracketLocants(Element subOrBracketOrRoot) throws PostProcessingException {
		Element locant =subOrBracketOrRoot.getFirstChildElement("locant");

		if(locant != null && subOrBracketOrRoot.getFirstChildElement("multiplier")==null) {
			if(locant.getAttribute("compoundLocant")!=null){
				throw new PostProcessingException("A compound locant cannot be used to locant a sub/bracket!");
			}
			if (subOrBracketOrRoot.getLocalName().equals("root")){
				throw new PostProcessingException("Unable to assign all locants");
			}
			subOrBracketOrRoot.addAttribute(new Attribute("locant", locant.getAttributeValue("value")));
			locant.detach();
			if (subOrBracketOrRoot.getFirstChildElement("locant")!=null){
				throw new PostProcessingException("Unable to assign all locants");
			}
		}
	}
}
