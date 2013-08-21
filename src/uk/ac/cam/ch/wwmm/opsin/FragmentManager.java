package uk.ac.cam.ch.wwmm.opsin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import nu.xom.Element;
import nu.xom.Nodes;

import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;

/** Holds the Fragments during the construction of the molecule,
 * and handles the building of new fragments.
 *
 * @author ptc24
 *
 */
class FragmentManager {

	/** All of the atom-containing fragments in the molecule */
	private List<Fragment> fragPile;
	/** A dummy fragment, holding all inter-fragment bonds */
	private Fragment bondPile;
	/** A builder for fragments specified as SSMILES */
	private SSMILESFragmentBuilder ssBuilder;
	/** A builder for fragments specified as references to a CML data file */
	private CMLFragmentBuilder cmlBuilder;
	/** A source of unique integers */
	private IDManager idManager;

	/** Builds a fragment, based on a reference to a CML data file
	 *
	 * @param idStr The name of the fragment in the CML file
	 * @param type The fragment type
	 * @param subType The fragment subType
	 * @return The fragment
	 * @throws StructureBuildingException If the fragment can't be built
	 */
	Fragment buildCML(String idStr, String type, String subType) throws StructureBuildingException {
		Fragment newFrag = cmlBuilder.build(idStr, type, subType, idManager);
		fragPile.add(newFrag);
		return newFrag;
	}

	/** Builds a fragment, based on an SSMILES string
	 *
	 * @param SSMILES The fragment to build
	 * @return The built fragment
	 * @throws StructureBuildingException
	 */
	Fragment buildSMILES(String SSMILES) throws StructureBuildingException {
		return buildSMILES(SSMILES, null, null);
	}

	/** Builds a fragment, based on an SSMILES string
	 *
	 * @param SSMILES The fragment to build
	 * @param type The fragment type
	 * @return The built fragment
	 * @throws StructureBuildingException
	 */
	Fragment buildSMILES(String SSMILES, String type, String labelMapping) throws StructureBuildingException {
		return buildSMILES(SSMILES, type, null, labelMapping);
	}

	/** Builds a fragment, based on an SSMILES string
	 *
	 * @param SSMILES The fragment to build
	 * @param type The fragment type
	 * @param subType The fragment subType
	 * @return The built fragment
	 * @throws StructureBuildingException
	 */
	Fragment buildSMILES(String SSMILES, String type, String subType, String labelMapping) throws StructureBuildingException {
		Fragment newFrag = ssBuilder.build(SSMILES, type, subType, labelMapping, idManager);
		fragPile.add(newFrag);
		return newFrag;
	}

	/** Sets up a new Fragment mananger, containing no fragments.
	 *
	 * @param ssBuilder A SSMILESFragmentBuilder - dependency injection.
	 * @param cmlBuilder A CMLFragmentBuilder - dependency injection.
	 * @param idManager An IDManager.
	 *
	 * @throws Exception If the CML fragment file can't be found or otherwise used
	 */
	FragmentManager(SSMILESFragmentBuilder ssBuilder, CMLFragmentBuilder cmlBuilder, IDManager idManager) {
		this.ssBuilder = ssBuilder;
		this.cmlBuilder = cmlBuilder;
		this.idManager = idManager;
		fragPile = new LinkedList<Fragment>();
		bondPile = new Fragment();
	}

	/**Creates a new fragment, containing all of the atoms and bonds
	 * of all of the other fragments - ie the whole molecule. This is non-destructive,
	 * and does not update which fragment the Atoms think they are in. Atoms and Bonds
	 * are not copied.
	 *
	 * @return The unified fragment
	 */
	Fragment getUnifiedFrags() {
		Fragment outFrag = new Fragment();
		for(Fragment f : fragPile) {
			outFrag.importFrag(f);
		}
		outFrag.importFrag(bondPile);
		return outFrag;
	}

	/** Joins two fragments together, by creating a bond between them
	 *
	 * @param fromAtom The identity of an atom on one fragment
	 * @param toAtom The identity of an atom on another fragment
	 * @param bondOrder The order of the joining bond
	 */
	void attachFragments(Atom fromAtom, Atom toAtom, int bondOrder) {
		int fromID =fromAtom.getID();
		int toID = toAtom.getID();
		Bond b =new Bond(fromID, toID, bondOrder);
		bondPile.addBond(b, false);
		fromAtom.addBond(b);
		toAtom.addBond(b);
	}

	/** Incorporates a fragment, usually a suffix, into a parent fragment, creating a bond between them.
	 *
	 * @param suffixFrag The fragment to be incorporated
	 * @param fromID An id on that fragment
	 * @param toFrag The parent fragment
	 * @param toID An id on that fragment
	 * @param bondOrder The order of the joining bond
	 */
	void incorporateFragment(Fragment suffixFrag, int fromID, Fragment toFrag, int toID, int bondOrder) throws StructureBuildingException {
		toFrag.importFrag(suffixFrag);
		toFrag.addBond(new Bond(fromID, toID, bondOrder));
		fragPile.remove(suffixFrag);
	}

	/** Adjusts the order of a bond in a fragment.
	 *
	 * @param fromAtomID The id of the lower-numbered atom in the bond
	 * @param bondOrder The new bond order
	 * @param fragment The fragment
	 */
	void unsaturate(int fromAtomID, int bondOrder, Fragment fragment) throws StructureBuildingException {
		int toAtomID = fromAtomID + 1;
		if (fragment.getAtomByID(toAtomID)==null){//allows something like cyclohexan-6-ene, something like butan-4-ene will still fail
			List<Atom> neighbours =fragment.getAtomByIDOrThrow(fromAtomID).getAtomNeighbours();
			if (neighbours.size() >=2){
				int firstID =fragment.getIdOfFirstAtom();
				for (Atom a : neighbours) {
					if (a.getID() ==firstID){
						toAtomID=firstID;
						break;
					}
				}
			}
		}
		Bond b = fragment.findBondOrThrow(fromAtomID, toAtomID);
		b.setOrder(bondOrder);
	}

	/** Adjusts the order of a bond in a fragment.
	 *
	 * @param fromAtomID The id of the first atom in the bond
	 * @param locantTo The locant of the other atom in the bond
	 * @param bondOrder The new bond order
	 * @param fragment The fragment
	 */
	void unsaturate(int fromAtomID, String locantTo, int bondOrder, Fragment fragment) throws StructureBuildingException {
		int toAtomID = fragment.getIDFromLocantOrThrow(locantTo);
		Bond b = fragment.findBondOrThrow(fromAtomID, toAtomID);
		b.setOrder(bondOrder);
	}

	/** Converts an atom in a fragment to a different atomic symbol.
	 * Charged atoms can also be specified using a SSMILES formula eg. [N+]
	 *
	 * @param a The atom to change to a heteroatom
	 * @param atomSymbol The atomic symbol to be used
	 * @param fragment The fragment containing the atom
	 * @throws StructureBuildingException if the atom could not be found
	 */
	void makeHeteroatom(Atom a, String atomSymbol, Fragment fragment) throws StructureBuildingException {
		if(atomSymbol.startsWith("[")) {
			Fragment f = ssBuilder.build(atomSymbol, idManager);
			fragPile.remove(f);
			Atom referenceAtom = f.getAtomList().get(0);
			atomSymbol =referenceAtom.getElement();
			a.setCharge(referenceAtom.getCharge());
		}
		a.setElement(atomSymbol);
		a.removeElementSymbolLocants();
		if (a.getFrag().getAtomByLocant(atomSymbol) ==null){//if none of that element currently present add element symbol locant
			a.addLocant(atomSymbol);
		}
	}

	/** Works out where to put an "one", if this is unspecified. position 2 for propanone
	 * and higher, else 1. Position 2 is assumed to be 1 higher than the ID given.
	 *
	 * @param fragment The fragment
	 * @return the integer value of the desired locant
	 */
	int findKetoneLocant(Fragment fragment, int ID) {
		if(fragment.getChainLength() < 3)
			return ID;
		else
			return ID +1;
	}

	/** Gets an atom, given an id number
	 *
	 * @param id The id of the atom
	 * @return The atom, or null if no such atom exists.
	 */
	Atom getAtomByID(int id) {
		for(Fragment f : fragPile) {
			Atom a = f.getAtomByID(id);
			if(a != null) return a;
		}
		return null;
	}

	/** Gets an atom, given an id number, throwing if fails.
	 *
	 * @param id The id of the atom
	 * @return The atom
	 */
	Atom getAtomByIDOrThrow(int id) throws StructureBuildingException {
		Atom a = getAtomByID(id);
		if(a == null) throw new StructureBuildingException("Couldn't get atom by id");
		return a;
	}

	/**Turns all of the spare valencies in the framents into double bonds.
	 *
	 * @throws Exception
	 */
	void convertSpareValenciesToDoubleBonds() throws StructureBuildingException {
		for(Fragment f : fragPile) {
			f.convertSpareValenciesToDoubleBonds(this);
		}
	}

	void checkValencies() throws StructureBuildingException {
		for(Fragment f : fragPile) {
			f.checkValencies();
		}
	}

	Fragment getBondPile() {
		return bondPile;
	}

	List<Fragment> getFragPile() {
		return fragPile;
	}

	/**
	 * Removes a fragment from the fragPile. Throws an exception if fragment wasn't present
	 * @param frag
	 * @throws StructureBuildingException
	 */
	void removeFragment(Fragment frag) throws StructureBuildingException {
		if (!fragPile.remove(frag)){
			throw new StructureBuildingException("Fragment not found in fragPile");
		}
	}

	void setCharge(int Id, String charge, Fragment fragment) throws StructureBuildingException {
		Atom atom= fragment.getAtomByIDOrThrow(Id);
		atom.setCharge(Integer.parseInt(charge));
	}

	int getOverallCharge() {
		int totalCharge=0;
		for (Fragment frag : fragPile) {
			totalCharge+=frag.getCharge();
		}
		return totalCharge;
	}

	/**
	 * Fixes things like quaternary nitrogen not being given positive charges
	 * @throws StructureBuildingException
	 */
	void tidyUpFragments() throws StructureBuildingException {
		for(Fragment f : fragPile) {
			List<Atom> atomsInFrag=f.getAtomList();
			for(Atom a : atomsInFrag){
				if (a.getElement().equals("N") && a.getIncomingValency()==4 && a.getCharge()==0){
					a.setCharge(1);
				}
			}
		}
	}



	/**
	 * Creates a copy of a fragment by copying data
	 * labels the atoms using new ids from the idManager and adds to the fragManager in state
	 * @param originalFragment
	 * @return
	 */
	Fragment copyAndRelabel(Fragment originalFragment) {
		return copyAndRelabel(originalFragment, null);
	}


	/**
	 * Creates a copy of a fragment by copying data
	 * labels the atoms using new ids from the idManager and adds to the fragManager in state
	 * @param originalFragment
	 * @param stringToAddToAllLocants: typically used to append primes to all locants, can be null
	 * @return
	 */
	Fragment copyAndRelabel(Fragment originalFragment, String stringToAddToAllLocants) {
		Fragment newFragment =new Fragment(originalFragment.getType(), originalFragment.getSubType());
		HashMap<Integer, Integer> idMap = new HashMap<Integer, Integer>();//maps old ID to new ID
		List<Atom> atomList =originalFragment.getAtomList();
		newFragment.setIndicatedHydrogen(originalFragment.getIndicatedHydrogen());
		newFragment.setInValency(originalFragment.getInValency());
		List<OutID> outIDs =originalFragment.getOutIDs();
		List<Integer> functionalIDs =originalFragment.getFunctionalIDs();
		int defaultInId =originalFragment.getDefaultInID();
		for (Atom atom : atomList) {
			int ID = idManager.getNextID();
			ArrayList<String> newLocants = new ArrayList<String>(atom.getLocants());
			if (stringToAddToAllLocants !=null){
				for (int i = 0; i < newLocants.size(); i++) {
					newLocants.set(i, newLocants.get(i) + stringToAddToAllLocants);
				}
			}
			Atom newAtom =new Atom(ID, newLocants, atom.getElement(), newFragment, atom.getType());
			newAtom.setCharge(atom.getCharge());
			newAtom.setSpareValency(atom.getSpareValency());
			newAtom.setStereochemistry(atom.getStereochemistry());
			newAtom.setExplicitHydrogens(atom.getExplicitHydrogens());
			newAtom.setValency(atom.getValency());
			newAtom.setNotes(new HashMap<String, String>(atom.getNotes()));
			newFragment.addAtom(newAtom);
			idMap.put(atom.getID(),ID);
		}
		for (int i = 0; i < outIDs.size(); i++) {
			newFragment.addOutID(idMap.get(outIDs.get(i).id), outIDs.get(i).valency, outIDs.get(i).setExplicitly);
		}
		for (Integer ID : functionalIDs) {
			newFragment.addFunctionalID(idMap.get(ID));
		}
		newFragment.setDefaultInID(idMap.get(defaultInId));
		List<Bond> bondList =originalFragment.getBondList();
		for (Bond bond : bondList) {
			Bond newBond=new Bond(idMap.get(bond.getFrom()), idMap.get(bond.getTo()), bond.getOrder());
			newFragment.addBond(newBond);
		}
		fragPile.add(newFragment);
		return newFragment;
	}

	void relabelFusedRingSystem(Fragment fusedring){
		relabelFusedRingSystem(fusedring, fusedring.getAtomList());
	}

	/**Adjusts the labeling on a fused ring system, such that bridgehead atoms
	 * have locants endings in 'a' or 'b' etc. Example: naphthalene
	 * 1,2,3,4,5,6,7,8,9,10->1,2,3,4,4a,5,6,7,8,8a
	 */
	void relabelFusedRingSystem(Fragment fusedring, List<Atom> atomList) {
		int locantVal = 0;
		char locantLetter = 'a';
		for (Atom atom : atomList) {
			atom.clearLocants();
		}
		for (Atom atom : atomList) {
			if(!atom.getElement().equals("C") || atom.getBonds().size() < 3) {
				locantVal++;
				locantLetter = 'a';
				atom.addLocant(Integer.toString(locantVal));
			} else {
				atom.addLocant(Integer.toString(locantVal) + locantLetter);
				locantLetter++;
			}
		}
		assignElementLocants(fusedring, new ArrayList<Fragment>());
	}

	/**
	 * Assign element locants to groups/suffixes. These are in addition to any numerical locants that are present.
	 * Adds primes to make each locant unique.
	 * For groups a locant is not given to carbon atoms
	 * If an element appears in a suffix then element locants are not assigned to occurrences of that element in the parent group
	 * @param suffixableFragment
	 * @param suffixFragments
	 */
	void assignElementLocants(Fragment suffixableFragment, ArrayList<Fragment> suffixFragments) {
		HashMap<String,Integer> elementCount =new HashMap<String,Integer>();//keeps track of how many times each element has been seen
		for (Fragment fragment : suffixFragments) {
			List<Atom> atomList =fragment.getAtomList();
			for (Atom atom : atomList) {
				String element =atom.getElement();
				if (elementCount.get(element)==null){
					atom.addLocant(element);
					elementCount.put(element,1);
				}
				else{
					int count =elementCount.get(element);
					atom.addLocant(element + StringTools.multiplyString("'", count));
					elementCount.put(element, count +1);
				}
			}
		}
		HashSet<String> elementToIgnore = new HashSet<String>(elementCount.keySet());
		elementToIgnore.add("C");
		elementCount =new HashMap<String,Integer>();
		List<Atom> atomList =suffixableFragment.getAtomList();
		for (Atom atom : atomList) {
			String element =atom.getElement();
			if (elementToIgnore.contains(element)){
				continue;
			}
			if (elementCount.get(element)==null){
				atom.addLocant(element);
				elementCount.put(element,1);
			}
			else{
				int count =elementCount.get(element);
				atom.addLocant(element + StringTools.multiplyString("'", count));
				elementCount.put(element, count +1);
			}
		}
	}

	/**
	 * Takes an element and produces a copy of it. Groups and suffixes are copied so that the new element
	 * has it's own group and suffix fragments
	 * @param elementToBeCloned
	 * @param state The current buildstate
	 * @return
	 */
	Element cloneElement(Element elementToBeCloned, BuildState state) {
		Element clone = new Element(elementToBeCloned);
		Nodes originalGroups =XQueryUtil.xquery(elementToBeCloned, ".//group");
		Nodes clonedGroups =XQueryUtil.xquery(clone, ".//group");
		for (int j = 0; j < originalGroups.size(); j++) {
			Fragment originalFragment =state.xmlFragmentMap.get(originalGroups.get(j));
			state.xmlFragmentMap.put((Element)clonedGroups.get(j), state.fragManager.copyAndRelabel(originalFragment));
			ArrayList<Fragment> originalSuffixes =state.xmlSuffixMap.get(originalGroups.get(j));
			ArrayList<Fragment> newSuffixFragments =new ArrayList<Fragment>();
			for (Fragment suffix : originalSuffixes) {
				newSuffixFragments.add(state.fragManager.copyAndRelabel(suffix));
			}
			state.xmlSuffixMap.put((Element)clonedGroups.get(j), newSuffixFragments);
		}
		return clone;
	}
}