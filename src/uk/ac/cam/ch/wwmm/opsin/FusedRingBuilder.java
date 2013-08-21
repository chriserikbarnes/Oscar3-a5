package uk.ac.cam.ch.wwmm.opsin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uk.ac.cam.ch.wwmm.ptclib.xml.XOMTools;

import nu.xom.Element;
import nu.xom.Elements;
import nu.xom.Nodes;

public class FusedRingBuilder {


	/**
	 * Sorts by atomSequences by the IUPAC rules for determining the preferred labelling
	 * The most preferred will be sorted to the back (0th position)
	 * @author dl387
	 *
	 */
	class SortAtomSequences implements Comparator<ArrayList<Atom>> {

	    public int compare(ArrayList<Atom> sequenceA, ArrayList<Atom> sequenceB){
	    	if (sequenceA.size() != sequenceB.size()){
	    		//Error in fused ring building. Identified ring sequences not the same lengths!
	    		return 0;
	    	}

	    	int i=0;
	    	int j=0;
	    	//Give low numbers for the heteroatoms as a set.
	    	while(i < sequenceA.size()){
				Atom atomA=sequenceA.get(i);
				boolean isAaHeteroatom =!atomA.getElement().equals("C");


				//bridgehead carbon do not increment numbering
				if (isAaHeteroatom ==false && atomA.getIncomingValency()>=3){
					i++;
					continue;
				}
				
				Atom atomB=sequenceB.get(j);
				boolean isBaHeteroatom =!atomB.getElement().equals("C");
				if (isBaHeteroatom ==false && atomB.getIncomingValency()>=3){
					j++;
					continue;
				}

				if (isAaHeteroatom ==true && isBaHeteroatom ==false){
					return -1;
				}
				if (isBaHeteroatom ==true && isAaHeteroatom ==false){
					return 1;
				}
	    		i++;j++;
	    	}

	    	i=0;
	    	j=0;
	    	//Give low numbers for heteroatoms when considered in the order: O, S, Se, Te, N, P, As, Sb, Bi, Si, Ge, Sn, Pb, B, Hg
	    	while(i < sequenceA.size()){
				Atom atomA=sequenceA.get(i);

				//bridgehead carbon do not increment numbering
				if (atomA.getElement().equals("C")&& atomA.getIncomingValency()>=3){
					i++;
					continue;
				}
				
				Atom atomB=sequenceB.get(j);
				if (atomB.getElement().equals("C") && atomB.getIncomingValency()>=3){
					j++;
					continue;
				}

				int atomAElementValue, atomBElementValue;
				if (heteroAtomValues.containsKey(atomA.getElement())){
					atomAElementValue = heteroAtomValues.get(atomA.getElement());
				}
				else{
					atomAElementValue=0;
				}
				if (heteroAtomValues.containsKey(atomB.getElement())){
					atomBElementValue = heteroAtomValues.get(atomB.getElement());
				}
				else{
					atomBElementValue=0;
				}
				if (atomAElementValue > atomBElementValue){
					return -1;
				}
				if (atomAElementValue < atomBElementValue){
					return 1;
				}
				i++;j++;
	    	}

	    	//Give low numbers to fusion carbon atoms.
	    	for ( i = 0; i < sequenceA.size(); i++) {
				Atom atomA=sequenceA.get(i);
				Atom atomB=sequenceB.get(i);
				if (atomA.getIncomingValency()>=3 && atomA.getElement().equals("C")){
					if (!(atomB.getIncomingValency()>=3 && atomB.getElement().equals("C"))){
						return -1;
					}
				}
				if (atomB.getIncomingValency()>=3 && atomB.getElement().equals("C")){
					if (!(atomA.getIncomingValency()>=3 && atomA.getElement().equals("C"))){
						return 1;
					}
				}
			}
	    	//Note that any sequences still unsorted at this step will have fusion carbon atoms in the same places
	    	//which means you can go through both sequences without constantly looking for fusion carbons i.e. the variable j is no longer needed

	    	//Give low numbers to fusion rather than non-fusion atoms of the same heteroelement.
	    	for (i = 0; i < sequenceA.size(); i++) {
				Atom atomA=sequenceA.get(i);
				Atom atomB=sequenceB.get(i);
				if (atomA.getIncomingValency()>=3){
					if (!(atomB.getIncomingValency()>=3)){
						return -1;
					}
				}
				if (atomB.getIncomingValency()>=3){
					if (!(atomA.getIncomingValency()>=3)){
						return 1;
					}
				}
			}
	    	return 0;
	    }
	}


	/**
	 * Sorts by number, then by letter e.g. 4,3,3b,5,3a,2 -->2,3,3a,3b,4,5
	 * @author dl387
	 *
	 */
	static class SortLocants implements Comparator<Atom> {

	    public int compare(Atom atoma, Atom atomb){
	    	String locanta =atoma.getFirstLocant();
	    	String locantb =atomb.getFirstLocant();

	    	Pattern matchdigits = Pattern.compile("([0-9]+).*");

	    	Matcher m1  =matchdigits.matcher(locanta);
	    	int locantaNumber=0;
	    	if (m1.matches()){
	    		locantaNumber=Integer.parseInt(m1.group(1));
	    	}
	    	else{
	    		return 0;//invalid locant (could be intentionally invalid)
	    	}

	    	Matcher m2  =matchdigits.matcher(locantb);
	    	int locantbNumber=0;
	    	if (m2.matches()){
	    		locantbNumber=Integer.parseInt(m2.group(1));
	    	}
	    	else{
	    		return 0;//invalid locant (could be intentionally invalid)
	    	}

	        if (locantaNumber >locantbNumber) {
	            return 1;//e.g. 3 vs 2 or 3a vs 2
	        } else if (locantbNumber >locantaNumber) {
	            return -1;//e.g. 2 vs 3 or 2 vs 3a
	        }
	        else{
	        	Pattern matchletters = Pattern.compile(".*([a-z]+)");
	        	m1  =matchletters.matcher(locanta);
	        	String locantaLetter="";
	        	if (m1.matches()){
	        		locantaLetter=m1.group(1);
	        	}
	        	else{
	        		return -1;// e.g. 1 vs 1a
	        	}

	        	m2  =matchletters.matcher(locantb);
	        	String locantbLetter="";
	        	if (m2.matches()){
	        		locantbLetter=m2.group(1);
	        	}
	        	else{
	        		return 1;//e.g. 1a vs 1
	        	}

	            if (locantaLetter.compareTo(locantbLetter)>=1) {
	                return 1;//e.g. 1b vs 1a
	            } else if (locantbLetter.compareTo(locantaLetter)>=1) {
	                return -1;//e.g. 1a vs 1b
	            }
	            return 0;
	        }
	    }
	}

	/** A builder for fragments specified as references to a CML data file */
	private CMLFragmentBuilder cmlBuilder;
	private BuildState state;
	private HashMap<Fragment,HashMap<String, ArrayList<String>>> ringsInformation;
	private ArrayList<Fragment> rings;
	private StructureBuilder structureBuilder;

	private HashMap<String, Integer> heteroAtomValues =new HashMap<String, Integer>();

	FusedRingBuilder(NameToStructure n2s) {
		structureBuilder=n2s.structureBuilder;
		cmlBuilder = structureBuilder.cmlBuilder;
		//unknown heteroatoms or carbon are given a value of 0
		heteroAtomValues.put("Hg",2);
		heteroAtomValues.put("B",3);
		heteroAtomValues.put("Pb",4);
		heteroAtomValues.put("Sn",5);
		heteroAtomValues.put("Ge",6);
		heteroAtomValues.put("Si",7);
		heteroAtomValues.put("Bi",8);
		heteroAtomValues.put("Sb",9);
		heteroAtomValues.put("As",10);
		heteroAtomValues.put("P",12);
		heteroAtomValues.put("N",13);
		heteroAtomValues.put("Te",14);
		heteroAtomValues.put("Se",15);
		heteroAtomValues.put("S",16);
		heteroAtomValues.put("O",17);
	}

	/**
	 * Master method for processing fused rings. If 2 groups are present will attempt to fuse them
	 * Returns the substituent/root with the 2 groups fused together into 1 group
	 * @param state: contains the current id and fragment manager
	 * @param e Element (substituent or root)
	 * @throws PostProcessingException
	 * @throws StructureBuildingException
	 */
	Element processFusedRings(BuildState state, Element e) throws PostProcessingException, StructureBuildingException {
		this.state=state;
		Elements groups =e.getChildElements("group");
		if (groups.size() < 2){return e;}//nothing to fuse
		if (groups.size() >2){
			throw new PostProcessingException("Unsupported fused ring system: more than 2 groups");
		}
		Elements fusions =e.getChildElements("fusion");
		if (fusions.size() >1){
			throw new PostProcessingException("Unsupported fused ring system: more than 1 fusion");
		}
		rings = new ArrayList<Fragment>();
		ringsInformation = new HashMap<Fragment,HashMap<String, ArrayList<String>>>();

		/*
		 * Resolve the Groups
		 */
		for(int i=0;i<groups.size();i++) {
			Element group=groups.get(i);
			String groupValue = group.getAttributeValue("value");
			String groupValType = group.getAttributeValue("valType");
			Fragment ring =state.xmlFragmentMap.get(group);
			if(groupValType.equals("dbkey")) {
				Nodes toBuildNodes = XQueryUtil.xquery(cmlBuilder.fragmentDoc, "/cml/molecule[@id=\""+groupValue + "_standardNumbering" +"\"]");
				if(toBuildNodes != null && toBuildNodes.size()==1 ){
					state.fragManager.removeFragment(ring);
					state.xmlFragmentMap.remove(group);
					String groupType = group.getAttributeValue("type");
					String groupSubType = group.getAttributeValue("subType");
					ring = state.fragManager.buildCML(groupValue + "_standardNumbering", groupType, groupSubType);
				}
			}
			ring.convertHighOrderBondsToSpareValencies();
			rings.add(ring);
		}

		int numbersMissing=0;
		int lettersMissing=0;
		ArrayList<String> numericalLocantsOfChild = new ArrayList<String>();
		ArrayList<String> letterLocantsOfParent = new ArrayList<String>();
		if (fusions.size() > 0){
			Element fusion = fusions.get(0);
			String[] fusionArray=fusion.getValue().split("-");
			if (fusionArray.length ==2){
				String[] locantsOfChildTemp = fusionArray[0].replaceFirst("\\[", "").split(",");
				for (int i = 0; i < locantsOfChildTemp.length; i++) {
					numericalLocantsOfChild.add(locantsOfChildTemp[i]);
				}
				char[] tempLetterLocantsOfParent = fusionArray[1].replaceFirst("\\]", "").toCharArray();
				for (int i = 0; i < tempLetterLocantsOfParent.length; i++) {
					letterLocantsOfParent.add(String.valueOf(tempLetterLocantsOfParent[i]));
				}
			}
			else{
				String tempContents = fusionArray[0].replaceFirst("\\[", "").replaceFirst("\\]", "");
				if (tempContents.contains(",")){//only has digits
					String[] numericalLocantsOfChildTemp =tempContents.split(",");
					for (int i = 0; i < numericalLocantsOfChildTemp.length; i++) {
						numericalLocantsOfChild.add(numericalLocantsOfChildTemp[i]);
					}
					lettersMissing=1;
				}
				else{//only has letters
					char[] tempLetterLocantsOfParentCharArray = tempContents.toCharArray();
					for (int i = 0; i < tempLetterLocantsOfParentCharArray.length; i++) {
						letterLocantsOfParent.add(String.valueOf(tempLetterLocantsOfParentCharArray[i]));
					}
					numbersMissing=1;
				}
			}
		}
		else{
			numbersMissing=1;
			lettersMissing=1;
		}

		if (numbersMissing==1){
			List<Atom> atomlist = rings.get(0).getAtomList();
			int foundCarbon=0;
			String locant="";
			atomlist.add(atomlist.get(0));//TODO use ringIterators
			for (Atom atom : atomlist) {
				if (atom.getElement().matches("C")){
					if (!atom.getFirstLocant().matches("[0-9]*")){
						continue;
					}
					if (foundCarbon ==1 ){//two in a row ->use this side
						numericalLocantsOfChild.add(locant);
						numericalLocantsOfChild.add(atom.getFirstLocant());
						break;
					}
					foundCarbon =1;
					locant =atom.getFirstLocant();
				}
				else{
					foundCarbon =0;
				}
			}
			atomlist.remove(atomlist.size()-1);
		}

		if (lettersMissing==1){
			List<Atom>  atomlist = rings.get(1).getAtomList();
			int foundCarbon=0;
			String locant ="";
			atomlist.add(0, atomlist.get(atomlist.size() -1));//TODO use ringIterators
			for (int i =atomlist.size() -1; i >=0; i--) {
				Atom atom =atomlist.get(i);
				if (atom.getElement().matches("C")){
					if (!atom.getFirstLocant().matches("[0-9]*")){
						continue;
					}
					locant =atom.getFirstLocant();
					if (foundCarbon ==1 ){//two in a row ->use this side
						letterLocantsOfParent.add(String.valueOf((char)(Integer.parseInt(locant) +96)));
						break;
					}
					foundCarbon =1;
				}
				else{
					foundCarbon =0;
				}
			}
			atomlist.remove(0);
		}


		HashMap <String, ArrayList<String>> tempHash = new HashMap<String, ArrayList<String>>();
		tempHash.put("numericalLocants",numericalLocantsOfChild);
		ringsInformation.put(rings.get(0), tempHash );
		tempHash = new HashMap<String, ArrayList<String>>();
		tempHash.put("letterLocants",letterLocantsOfParent);
		ringsInformation.put(rings.get(1), tempHash );

		Fragment fusedRing =fuseRings();//fuses the rings using the information contained with ringsInformation
		String fusedRingName=groups.get(0).getValue();
		if (fusedRingName.equals("benz") || fusedRingName.equals("benzo")){
			benzoSpecificAssignHeteroAtomsUsingLocants(state, groups.get(0), fusedRing);
		}
		for(int i=0;i<fusions.size();i++) {
			fusedRingName+=fusions.get(i).getValue();
		}
		fusedRingName+=groups.get(1).getValue();

		Element fusedRingEl =groups.get(groups.size()-1);//reuse this element to save having to remap suffixes...
		fusedRingEl.getAttribute("value").setValue(fusedRingName);
		fusedRingEl.getAttribute("valType").setValue("generatedFragment");
		fusedRingEl.getAttribute("type").setValue("ring");
		fusedRingEl.getAttribute("subType").setValue("fusedRing");
		fusedRingEl.removeChildren();
		fusedRingEl.appendChild(fusedRingName);

		state.xmlFragmentMap.put(fusedRingEl, fusedRing);

		for(int i=0;i<groups.size() -1;i++) {
			groups.get(i).detach();
		}
		for(int i=0;i<fusions.size();i++) {
			groups.get(i).detach();
		}
		return fusedRingEl;
	}

	/**
	 * Performs ring fusions
	 * @return fused Fragment
	 * @throws StructureBuildingException
	 */
	private Fragment fuseRings() throws StructureBuildingException {
		ArrayList<List<Atom>> childAtomsToBeConnectedToParent = new ArrayList<List<Atom>>();
		ArrayList<Integer> parentAtomIds = new ArrayList<Integer>();
		Fragment childRing =rings.get(0);
		Fragment parentRing =rings.get(1);
		ArrayList<String> locantsOfChild = ringsInformation.get(childRing).get("numericalLocants");
		ArrayList<String> letterLocantsOfParent = ringsInformation.get(parentRing).get("letterLocants");
		String firstLocant =locantsOfChild.get(0);
		String secondLocant =locantsOfChild.get(1);

		List<Atom> sortedAtomsInChild =sortFragmentAtomListByLocant(childRing);
		int firstLocantIndex=-1;
		int nonBridgeHeads=0;
		for (int i = 0; i < sortedAtomsInChild.size(); i++) {
			if (firstLocant.equals(sortedAtomsInChild.get(i).getFirstLocant())){
				firstLocantIndex=i;
			}
			if (sortedAtomsInChild.get(i).getFirstLocant().matches("[0-9]*")){
				nonBridgeHeads++;
			}
		}

		if (Integer.parseInt(secondLocant) - Integer.parseInt(firstLocant) == 1 || (Integer.parseInt(secondLocant) + nonBridgeHeads ) - Integer.parseInt(firstLocant) ==1){ //indexes are ascending
			int endingIndex = firstLocantIndex + sortedAtomsInChild.size();
			int counter=0;
			for (int i = firstLocantIndex; i < endingIndex; i++) {
				int index= i;
				if (index >= sortedAtomsInChild.size()){
					index -=sortedAtomsInChild.size();
				}
				String locant = sortedAtomsInChild.get(index).getFirstLocant();

				if (counter<locantsOfChild.size()){
					if (!locant.equals(locantsOfChild.get(counter))){
						locantsOfChild.add(counter++, locant);
					}
					else{
						counter++;
					}
				}
			}
		}
		else{
			int endingIndex = firstLocantIndex - sortedAtomsInChild.size();
			int counter=0;
			for (int i = firstLocantIndex; i > endingIndex; i--) {
				int index= i;
				if (index < 0){
					index +=sortedAtomsInChild.size();
				}
				String locant = sortedAtomsInChild.get(index).getFirstLocant();

				if (counter<locantsOfChild.size()){
					if (!locant.equals(locantsOfChild.get(counter))){
						locantsOfChild.add(counter++, locant);
					}
					else{
						counter++;
					}
				}
			}
		}

		for (int i = 0; i < locantsOfChild.size(); i++) {
			Atom atom =childRing.getAtomByLocant(locantsOfChild.get(i));
			List<Atom> neighbours = childRing.getAtomNeighbours(atom);

			//remove neighbours that are going to be deleted as they are included in numericalLocantsOfChild
			List<Atom> neighboursToRemove = new ArrayList<Atom>();
			for (Atom neighbour : neighbours) {
				String neighbourLocant=neighbour.getFirstLocant();
				for (int j = 0; j < locantsOfChild.size(); j++) {
					if (neighbourLocant.equals(locantsOfChild.get(j))){
						neighboursToRemove.add(neighbour);
					}
				}
			}

			for (Atom atomToRemove : neighboursToRemove) {//neighbour atoms that are to be subsequently removed are dropped
				neighbours.remove(atomToRemove);
			}

			childAtomsToBeConnectedToParent.add(i, neighbours);
		}
		for (int i = 0; i < locantsOfChild.size(); i++) {
			childRing.removeAtomByLocant(locantsOfChild.get(i));
		}

		parentAtomIds.add(0, (int)letterLocantsOfParent.get(0).charAt(0) -97);
		for (int i = 0; i < letterLocantsOfParent.size(); i++) {
			int ringEdgeStartAtomNumber =(int)letterLocantsOfParent.get(i).charAt(0) -97;//convert from lower case character through ascii to 0-23
			if (ringEdgeStartAtomNumber +1!=parentRing.getAtomList().size()){
				parentAtomIds.add(parentAtomIds.size(), ringEdgeStartAtomNumber +1);
			}
			else{
				parentAtomIds.add(parentAtomIds.size(), 0);
			}
		}

		List<Atom> sortedAtomsInParent =sortFragmentAtomListByLocant(parentRing);
		parentRing.importFrag(childRing);
		for (int i = 0; i < parentAtomIds.size(); i++) {
			for (Atom atom : childAtomsToBeConnectedToParent.get(i)) {
				//System.out.println("Atom ID " + atom.getID() +" bonded to " +  parentAtomIds.get(i));
				parentRing.addBond(new Bond(atom.getID(), sortedAtomsInParent.get(parentAtomIds.get(i)).getID(), 1));
			}
		}
		state.fragManager.removeFragment(childRing);
		numberFusedRing(parentRing);//numbers the fused ring;
		Fragment fusedRing =state.fragManager.copyAndRelabel(parentRing);//makes sure the IDs are continuous
		state.fragManager.removeFragment(parentRing);
		return fusedRing;
	}


	/**
	 * Numbers the fused ring
	 * Currently only works for a very limited selection of rings
	 * @param uniFrag
	 * @throws StructureBuildingException
	 */
	private void numberFusedRing(Fragment fusedRing) throws StructureBuildingException {
		List<Atom> atomList =fusedRing.getAtomList();
		ArrayList<Atom> bridgeheads =new ArrayList<Atom>();
		for (Atom atom : atomList) {
			if (fusedRing.getAtomNeighbours(atom).size()==3){
				bridgeheads.add(atom);
			}
		}
		/*
		 * General case where labelling cannot be done successfully.
		 */
		if (bridgeheads.size() !=2){
			int i=1;
			for (Atom atom : atomList) {
				atom.replaceLocant("X" + String.valueOf(i));
				i++;
			}
			return;
		}

		ArrayList<ArrayList<Atom>> atomSequences=new ArrayList<ArrayList<Atom>>();
		for (Atom bridgeheadAtom : bridgeheads) {
			List<Atom>  neighbours =fusedRing.getAtomNeighbours(bridgeheadAtom);
			for (Atom  neighbour :  neighbours) {
				if (!bridgeheads.contains(neighbour)){
					//found starting atom
					ArrayList<Atom> atomsVisited =new ArrayList<Atom>();
					atomsVisited.add(bridgeheadAtom);

					Atom nextAtom =neighbour;
					do{
						atomsVisited.add(nextAtom);
						List<Atom> possibleNextInRings =fusedRing.getAtomNeighbours( nextAtom);
						nextAtom=null;
						for (Atom nextInRing:  possibleNextInRings) {
							if (atomsVisited.contains(nextInRing)){
								continue;//already visited
							}
							else{
								nextAtom=nextInRing;
							}
						}
					}
					while (nextAtom != null);
					atomsVisited.remove(bridgeheadAtom);
					atomsVisited.add(bridgeheadAtom);//remove the bridgehead and then re-add it so that it is at the end of the list
					atomSequences.add(atomsVisited);
				}
			}
		}
		//find the preferred numbering scheme then relabel with this scheme
		Collections.sort( atomSequences, new SortAtomSequences());
		fusedRing.setDefaultInID(atomSequences.get(0).get(0).getID());
		state.fragManager.relabelFusedRingSystem(fusedRing, atomSequences.get(0));
		fusedRing.reorderAtomCollection(atomSequences.get(0));
	}


	/**
	 * Given a fragment returns it's atom list sorted by locant. e.g. 1,2,3,3a,3b,4
	 * @param fragment
	 * @return
	 */
	private List<Atom> sortFragmentAtomListByLocant(Fragment frag) {
		List<Atom> atomsInFragment =frag.getAtomList();
		Comparator<Atom> sortLocants= new SortLocants();
		Collections.sort(atomsInFragment, sortLocants);
		return atomsInFragment;
	}
	
	/**
	 * Uses locants in front of the benz/benzo group to assign heteroatoms on the now numbered and used fused ring system
	 * @param state
	 * @param benzoEl
	 * @param fusedRing
	 * @throws StructureBuildingException 
	 * @throws PostProcessingException 
	 */
	private void benzoSpecificAssignHeteroAtomsUsingLocants(BuildState state, Element benzoEl, Fragment fusedRing) throws StructureBuildingException, PostProcessingException {
		Element previous = (Element) XOMTools.getPreviousSibling(benzoEl);
		LinkedList<Element> locants =new LinkedList<Element>();
		while(previous != null && previous.getLocalName().equals("locant")) {
			locants.add(previous);
			previous=(Element) XOMTools.getPreviousSibling(previous);
		}
		if (locants.size() >0){
			List<Atom> atomList =fusedRing.getAtomList();
			LinkedList<Atom> heteroatoms =new LinkedList<Atom>();
			LinkedList<String> elementOfHeteroAtom =new LinkedList<String>();
			for (Atom atom : atomList) {//this iterates in the same order as the numbering system
				if (!atom.getElement().equals("C")){
					heteroatoms.add(atom);
					elementOfHeteroAtom.add(atom.getElement());
				}
			}
			if (locants.size() >=heteroatoms.size()){//atleast as many locants as there are heteroatoms to assign
				for (Atom atom : heteroatoms) {
					atom.setElement("C");
				}
				fusedRing.pickUpIndicatedHydrogen();
				for (int i=0; i< heteroatoms.size(); i ++){
					String elementSymbol =elementOfHeteroAtom.removeLast();
					Element locant =locants.removeFirst();
					fusedRing.getAtomByLocantOrThrow(locant.getAttributeValue("value")).setElement(elementSymbol);
					locant.detach();
				}
			}
			else if (locants.size()>1){
				throw new PostProcessingException("Unable to assign all locants to benzo-fused ring or multiplier was mising");
			}
		}
	}
}
