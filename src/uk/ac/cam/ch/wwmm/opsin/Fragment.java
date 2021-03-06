package uk.ac.cam.ch.wwmm.opsin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import uk.ac.cam.ch.wwmm.ptclib.xml.XOMTools;

import nu.xom.Attribute;
import nu.xom.Element;

/**A fragment of a molecule, holds bonds and atoms.
 *
 * @author ptc24/dl387
 *
 */
class Fragment {

	/**A mapping between IDs and the atoms in this fragment, also keeps track of the order atoms are added*/
	private LinkedHashMap<Integer, Atom> atomMapFromId;

	/**Equivalent to and synced to atomMapFromId.values() */
	private Collection<Atom> atomCollection;

	/**A mapping between locants and the atoms in this fragment*/
	private HashMap<String, Atom> atomMapFromLocant;

	/**The bonds in the fragment*/
	private List<Bond> bondList;

	/**The type of the fragment, for the purpose of resolving suffixes*/
	private String type;

	/**The subType of the fragment, for the purpose of resolving suffixes*/
	private String subType;

	/**The IDs of atoms that are used when this fragment is connected to another fragment. Unused outIDs means that the name is a radical or an error has occured
	 * Initially empty */
	private LinkedList<OutID> outIDs;

	/**The IDs of atoms that are used on this fragment to form things like esters
	 * Initially empty */
	private LinkedList<Integer> functionalIDs;

	/**The ID of the atom that fragments connecting to the fragment connect to if a locant has not been specified
	 * Set by the first atom to be added to the fragment. This is typically the one with locant 1
	 * but allows for fragments with no locants. Can be overridden*/
	private int defaultInID;

	/**
	 * The bond order with which the defaultInID should connect
	 */
	private int inValency;

	/**The id of the indicated Hydrogen on the fragment.
	 * Defaults to null.*/
	private Integer indicatedHydrogen;

	/**Makes an empty Fragment with a given type and subType.
	 *
	 * @param type The type of the fragment
	 */
	Fragment(String type, String subType) {
		this();
		this.type = type;
		this.subType = subType;
	}

	/**Makes an empty fragment with no specified type.*/
	Fragment() {
		atomMapFromId = new LinkedHashMap<Integer, Atom>();
		atomCollection =atomMapFromId.values();
		atomMapFromLocant = new HashMap<String, Atom>();
		bondList = new ArrayList<Bond>();
		outIDs = new LinkedList<OutID>();
		functionalIDs = new LinkedList<Integer>();
		defaultInID =0;
	}

	/**Produces a CML cml element, corresponding to the molecule. The cml element contains
	 * a molecule, which contains an atomArray and bondArray filled with atoms and bonds.
	 * The molecule element has a dummy id of m1.
	 *
	 * @return The CML element.
	 * @see Atom
	 * @see Bond
	 */
	Element toCMLMolecule() {
		Element cml = new Element("cml");
		Element molecule = new Element("molecule");
		molecule.addAttribute(new Attribute("id", "m1"));
		Element atomArray = new Element("atomArray");
		for(Atom atom : atomCollection) {
			atomArray.appendChild(atom.toCMLAtom());
		}
		Element bondArray = new Element("bondArray");
		for(Bond bond : bondList) {
			bondArray.appendChild(bond.toCMLBond());
		}

		//add explicit hydrogen
		for(Atom atom : atomCollection) {
			Integer explicitHydrogen =atom.getExplicitHydrogens();
			if (explicitHydrogen != null){
				for (int i = 1; i <= explicitHydrogen; i++) {
					Element atomElem = new Element("atom");
					String hydrogenId ="a" +atom.getID() +"_h" +i;
					atomElem.addAttribute(new Attribute("id", hydrogenId));
					atomElem.addAttribute(new Attribute("elementType", "H"));
					atomArray.appendChild(atomElem);
					Element bondElem = new Element("bond");
					bondElem.addAttribute(new Attribute("atomRefs2", "a" + atom.getID() +" " +hydrogenId));
					bondElem.addAttribute(new Attribute("order", "1"));
					bondArray.appendChild(bondElem);
				}
			}
		}
		molecule.appendChild(atomArray);
		molecule.appendChild(bondArray);
		cml.appendChild(molecule);
		XOMTools.setNamespaceURIRecursively(cml, "http://www.xml-cml.org/schema");
		return cml;
	}

	String toSMILES() throws StructureBuildingException {
		StringBuffer sb = new StringBuffer();
		IDManager idm = new IDManager();
		getAtomList().get(0).visitForSMILES(sb, null, idm);

		sb.deleteCharAt(sb.length()-1);
		return sb.toString();
	}

	/**Adds an atom to the fragment. Does not tell the atom that it is in the fragment.*/
	void addAtom(Atom atom) {
		if (defaultInID==0){//the first atom becomes the defaultInID
			defaultInID =atom.getID();
		}
		List<String> locants =atom.getLocants();
		for (String locant: locants) {
			atomMapFromLocant.put(locant, atom);
		}
		atomMapFromId.put(atom.getID(), atom);
	}

	/**Gets atomList.*/
	List<Atom> getAtomList() {
		return new ArrayList<Atom>(atomCollection);
	}

	/**
	 * Adds a bond to the fragment.
	 * associateWithAtoms is false when adding to the dummyFragment, bondpile, which does not contain any atoms
	 * or if the atoms are known to already have the bond associated with them
	 * @param Bond bond
	 * @param Boolean associateWithAtoms
	 */
	void addBond(Bond bond, boolean associateWithAtoms) {
		bondList.add(bond);
		if (associateWithAtoms ==true){
			atomMapFromId.get(bond.getFrom()).addBond(bond);
			atomMapFromId.get(bond.getTo()).addBond(bond);
		}
	}

	/**Adds a bond to the fragment.*/
	void addBond(Bond bond) {
		addBond(bond,true);
	}

	/**Gets bondList.*/
	List<Bond> getBondList() {
		return bondList;
	}

	/**Imports all of the atoms and bonds from another fragment into this one.
	 *
	 * @param frag The fragment from which the atoms and bonds are to be imported.
	 */
	void importFrag(Fragment frag) {
		for(Atom atom : frag.getAtomList()) {
			addAtom(atom);
			atom.setFrag(this);
		}
		for(Bond bond : frag.getBondList()) {
			addBond(bond, false);
		}
	}

	/**Gets the id of the atom in the fragment with the specified locant.
	 *
	 * @param locant The locant to look for
	 * @return The id of the found atom, or 0 if it is not found
	 */
	int getIDFromLocant(String locant) {
		Atom a =atomMapFromLocant.get(locant);
		if (a != null){
			return a.getID();
		}
		return 0;
	}

	/**Gets the id of the atom in the fragment with the specified locant, throwing if this fails.
	 *
	 * @param locant The locant to look for
	 * @return The id of the found atom
	 */
	int getIDFromLocantOrThrow(String locant) throws StructureBuildingException {
		int id = getIDFromLocant(locant);
		if(id == 0) throw new StructureBuildingException("Couldn't find id from locant " + locant + ".");
		return id;
	}

	/**Gets the atom in the fragment with the specified locant.
	 *
	 * @param locant The locant to look for
	 * @return The found atom, or null if it is not found
	 */
	Atom getAtomByLocant(String locant) {
		Atom a =atomMapFromLocant.get(locant);
		if (a != null){
			return a;
		}
		return null;
	}

	/**Gets the atom in the fragment with the specified locant, throwing if this fails.
	 *
	 * @param locant The locant to look for
	 * @return The found atom
	 */
	Atom getAtomByLocantOrThrow(String locant) throws StructureBuildingException {
		Atom a = getAtomByLocant(locant);
		if(a == null) throw new StructureBuildingException("Could not find the atom with locant " + locant + ".");
		return a;
	}

	/**Gets the atom in the fragment with the specified ID.
	 *
	 * @param id The id of the atom.
	 * @return The found atom, or null.
	 */
	Atom getAtomByID(int id) {
		Atom a = atomMapFromId.get(id);
		if (a !=null){
			return a;
		}
		return null;
	}

	/**Gets the atom in the fragment with the specified ID, throwing if this fails.
	 *
	 * @param id The id of the atom.
	 * @return The found atom
	 */
	Atom getAtomByIDOrThrow(int id) throws StructureBuildingException {
		Atom a = getAtomByID(id);
		if(a == null) throw new StructureBuildingException("Couldn't find atom with id " + id + ".");
		return a;
	}

	/**Reduces the spare valency of the atom at the given locant.
	 *
	 * @param locant The locant of the atom.
	 * @throws StructureBuildingException If the atom cannot be found, or has no spare valency.
	 */
	void reduceSpareValency(String locant) throws StructureBuildingException {
		Atom a =getAtomByLocantOrThrow(locant);
		if(a.getSpareValency() < 1) throw new StructureBuildingException("Atom at locant " +
				locant + " has no spare valency to reduce.");
		a.subtractSpareValency(1);
	}

	/**Finds a bond between two specified atoms within the fragment.
	 *
	 * @param ID1 The id of one atom
	 * @param ID2 The id of the other atom
	 * @return The bond found, or null
	 */
	Bond findBond(int ID1, int ID2) {
		Atom a = atomMapFromId.get(ID1);
		if (a!=null){
			for (Bond b : a.getBonds()) {
				if((b.getFrom() == ID1 && b.getTo() == ID2) ||
						(b.getTo() == ID1 && b.getFrom() == ID2)) {
					return b;
				}
			}
		}
		return null;
	}

	/**Finds a bond between two specified atoms within the fragment, throwing if fails.
	 *
	 * @param ID1 The id of one atom
	 * @param ID2 The id of the other atom
	 * @return The bond found, or null
	 */
	Bond findBondOrThrow(int ID1, int ID2) throws StructureBuildingException {
		Bond b = findBond(ID1, ID2);
		if(b == null) throw new StructureBuildingException("Couldn't find specified bond");
		return b;
	}

	/**Works out how many atoms there are in the fragment there are
	 * with consecutive locants, starting from 1;
	 *
	 * @return The number of atoms in the locant chain.
	 */
	int getChainLength() {
		int length=0;
		while(getIDFromLocant(Integer.toString(length+1)) > 0) {
			length++;
		}
		return length;
	}

	/**Gets type.*/
	String getType() {
		return type;
	}

	/**Gets subType.*/
	String getSubType() {
		return subType;
	}

	/**Gets the linkedList of outIDs*/
	List<OutID> getOutIDs() {
		return outIDs;
	}

	/**
	 * Gets the outID at a specific index of the outIDs linkedList
	 * @param i
	 * @return
	 */
	OutID getOutID(int i) {
		return outIDs.get(i);
	}

	/**Adds an outID*/
	void addOutID(int id, int valency, Boolean setExplicitly) {
		outIDs.add(new OutID(id,valency,setExplicitly, this, null));
	}

	/**Adds an outID*/
	void addOutID(OutID outID) {
		outIDs.add(new OutID(outID));
	}

	/**
	 * Removes the outID at a specific index of the outIDs linkedList
	 * @param i
	 */
	void removeOutID(int i) {
		outIDs.remove(i);
	}
	
	/**
	 * Removes the specified outID from the outIDs linkedList
	 * @param outID
	 */
	void removeOutID(OutID outID) {
		outIDs.remove(outID);
	}

	/**Gets the linkedList of functionalIDs*/
	List<Integer> getFunctionalIDs() {
		return functionalIDs;
	}

	/**
	 * Gets the functionalID at a specific index of the functionalIDs linkedList
	 * @param i
	 * @return
	 */
	int getFunctionalID(int i) {
		return functionalIDs.get(i);
	}

	/**Adds a functionalID*/
	void addFunctionalID(int id) {
		functionalIDs.add(id);
	}

	/**Adds a list of functionalIDs*/
	void addFunctionalIDs(List<Integer> functionalIDs) {
		this.functionalIDs.addAll(functionalIDs);
	}

	/**
	 * Removes the functionalID at a specific index of the functionalIDs linkedList
	 * @param i
	 */
	void removeFunctionalID(int i) {
		functionalIDs.remove(i);
	}

	/** Looks for the atom that would have had a hydrogen indicated,
	 * adds a spareValency to that atom, and sets indicatedHydrogen.
	 */
	void pickUpIndicatedHydrogen() {
		int svCount = 0;
		int dvCount = 0; /* Divalent, like O */
		for(Atom a : atomCollection) {
			svCount += a.getSpareValency();
			if(a.getElement().equals("O")) dvCount++;
			if(a.getElement().equals("S")) dvCount++;
			if(a.getElement().equals("Se")) dvCount++;
			if(a.getElement().equals("Te")) dvCount++;
		}
		if(svCount == atomCollection.size() - (1 + dvCount) && svCount > 0) {
			// Now we're looking for a trivalent C, or failing that a divalent N
			Atom nCandidate = null;
			for(Atom a : atomCollection) {
				if(a.getElement().equals("C") && a.getSpareValency() == 0) {
					indicatedHydrogen = a.getID();
					a.addSpareValency(1);
					return;
				} else if(a.getElement().equals("N") && a.getSpareValency() == 0) {
					nCandidate = a;
				}
			}
			if(nCandidate != null) {
				indicatedHydrogen = nCandidate.getID();
				nCandidate.addSpareValency(1);
				return;
			}
		}
	}

	/**Looks for double and higher bonds, converts them to single bonds
	 * and adds corresponding spareValencies to the atoms they join.
	 * @throws StructureBuildingException
	 */
	void convertHighOrderBondsToSpareValencies() throws StructureBuildingException {
		for(Bond b : bondList) {
			if(b.getOrder() > 1) {
				int orderExtra = b.getOrder() - 1;
				b.setOrder(1);
				getAtomByIDOrThrow(b.getFrom()).addSpareValency(orderExtra);
				getAtomByIDOrThrow(b.getTo()).addSpareValency(orderExtra);
			}
		}
		pickUpIndicatedHydrogen();
	}

	/**Increases the order of bonds joining atoms with spareValencies,
	 * and uses up said spareValencies.
	 * @throws StructureBuildingException If the algorithm can't work out where to put the bonds
	 */
	void convertSpareValenciesToDoubleBonds(FragmentManager fragManager) throws StructureBuildingException {
		/* pick atom, getAtomNeighbours, decideIfTerminal, resolve */

		/*
		 * Correct spare valency by looking at valencyState of atom
		 *
		 */
		for(Atom a : atomCollection) {
			a.ensureSVIsConsistantWithValency(true);
		}

		/*
		 * Remove spare valency on atoms which may not form higher order bonds
		 */
		atomLoop: for(Atom a : atomCollection) {
			if(a.getSpareValency() > 0) {
				for(Atom aa : getAtomNeighbours(a)) {
					if(aa.getSpareValency() > 0 || aa.getNote("Possibly Should Be Charged")!=null) {
						continue atomLoop;
					}
				}
				a.setSpareValency(0);
				a.setNote("Possibly Should Be Charged", null);
			}
		}

		int svCount = 0;
		for(Atom a : atomCollection) {
			svCount += a.getSpareValency();
		}

		if (svCount %2 ==1){//look for the special case where a Nitrogen could be charged to maintain aromaticity
			for(Atom a : atomCollection) {
				if (a.getNote("Possibly Should Be Charged")!=null){
					a.setCharge(1);
					a.setSpareValency(1);
					svCount++;
					break;
				}
			}
		}

		/*
		 Reduce valency of atoms which cannot possibly have any of their bonds converted to double bonds
		 pick an atom which definitely does have spare valency to be the indicated hydrogen.
		*/
		Atom atomToReduceValencyAt =null;
		if (indicatedHydrogen!=null){
			if (getAtomByID(indicatedHydrogen)==null || getAtomByID(indicatedHydrogen).getSpareValency()==0){
				indicatedHydrogen = null;
			}
			else{
				atomToReduceValencyAt=getAtomByID(indicatedHydrogen);
			}
		}

		if((svCount % 2) == 1) {
			if (indicatedHydrogen == null){
				for(Atom a : atomCollection) {
					if(a.getSpareValency() > 0) {
						atomToReduceValencyAt=a;
						int atomsWithSV =0;
						for(Atom aa : getAtomNeighbours(a)) {
							if(aa.getSpareValency() > 0) {
								atomsWithSV++;
							}
						}
						if (atomsWithSV==1){
							break;
						}
					}
				}
			}
			atomToReduceValencyAt.subtractSpareValency(1);
			svCount--;
		}

		while(svCount > 0) {
			boolean foundTerminalFlag = false;
			boolean foundNonBridgeHeadFlag = false;
			for(Atom a : atomCollection) {
				if(a.getSpareValency() > 0) {
					int count = 0;
					for(Atom aa : getAtomNeighbours(a)) {
						if(aa.getSpareValency() > 0) {
							count++;
						}
					}
					if(count == 1) {
						for(Atom aa : getAtomNeighbours(a)) {
							if(aa.getSpareValency() > 0) {
								foundTerminalFlag = true;
								a.subtractSpareValency(1);
								aa.subtractSpareValency(1);
								findBondOrThrow(a.getID(), aa.getID()).addOrder(1);
								svCount -= 2;
								break;
							}
						}
					}
				}
			}
			if(!foundTerminalFlag) {
				for(Atom a : atomCollection) {
					List<Atom> neighbours =getAtomNeighbours(a);
					if(a.getSpareValency() > 0 && neighbours.size() < 3) {
						for(Atom aa : neighbours) {
							if(aa.getSpareValency() > 0) {
								foundNonBridgeHeadFlag = true;
								a.subtractSpareValency(1);
								aa.subtractSpareValency(1);
								findBondOrThrow(a.getID(), aa.getID()).addOrder(1);
								svCount -= 2;
								break;
							}
						}
					}
					if(foundNonBridgeHeadFlag) break;
				}
				if(!foundNonBridgeHeadFlag) throw new StructureBuildingException("Could not assign all higher order bonds.");
			}
		}
	}

	/**Gets a list of atoms in the fragment that connect to a specified atom
	 *
	 * @param atom The reference atom
	 * @return The list of atoms connected to the atom
	 */
	List<Atom> getAtomNeighbours(Atom atom) {
		List<Atom> results = new ArrayList<Atom>();
		int id = atom.getID();
		for(Bond b :  atom.getBonds()) {
			//recalled atoms will be null if they are not part of this fragment
			if(b.getFrom() == id) {
				Atom a =getAtomByID(b.getTo());
				if (a!=null){
					results.add(a);
				}
			} else if(b.getTo() == id) {
				Atom a =getAtomByID(b.getFrom());
				if (a!=null){
					results.add(a);
				}
			}
		}
		return results;
	}
	
	/**Calculates the number of bonds connecting to the atom, excluding bonds to implicit
	 * hydrogens. Double bonds count as
	 * two bonds, etc. Eg ethene - both C's have an incoming valency of 2.
	 * 
	 * Only bonds to atoms within the fragment are counted. Suffix atoms are excluded
	 *
	 * @return Incoming Valency
	 */
	int getIntraFragmentIncomingValency(Atom atom) {
		int id = atom.getID();
		int v = 0;
		for(Bond b :  atom.getBonds()) {
			//recalled atoms will be null if they are not part of this fragment
			if(b.getFrom() == id) {
				Atom a =getAtomByID(b.getTo());
				if (a!=null && !a.getType().equals("suffix")){
					v += b.getOrder();
				}
			} else if(b.getTo() == id) {
				Atom a =getAtomByID(b.getFrom());
				if (a!=null && !a.getType().equals("suffix")){
					v += b.getOrder();
				}
			}
		}
		return v;
	}

	/**
	 * Checks valencies are sensible
	 * @throws StructureBuildingException
	 */
	void checkValencies() throws StructureBuildingException {
		for(Atom a : atomCollection) a.checkIncomingValency();
	}

	/**
	 * Removes an atom and the bonds joined to it
	 * @param atom
	 * @throws StructureBuildingException
	 */
	void removeAtomByLocant(String locant) throws StructureBuildingException {
		Atom atom =getAtomByLocantOrThrow(locant);
		ArrayList<Bond> bondsToBeRemoved=new ArrayList<Bond>(atom.getBonds());
		for (Bond bond : bondsToBeRemoved) {
			bondList.remove(bond);
			atomMapFromId.get(bond.getFrom()).removeBond(bond);
			atomMapFromId.get(bond.getTo()).removeBond(bond);
		}
		int atomID =atom.getID();
		atomMapFromId.remove(atomID);
		for (String l : atom.getLocants()) {
			atomMapFromLocant.remove(l);
		}
	}

	/**
	 * Retrieves the overall charge of the fragment by querying all its atoms
	 * @return
	 */
	int getCharge() {
		int charge=0;
		for (Atom a : atomCollection) {
			charge+=a.getCharge();
		}
		return charge;
	}

	/**
	 * Sets the type of the fragment e.g. aromaticStem
	 * @param type
	 */
	void setType(String type) {
		this.type = type;
	}

	int getDefaultInID() {
		return defaultInID;
	}

	void setDefaultInID(int defaultInID) {
		this.defaultInID=defaultInID;
	}

	/**
	 * Adds a mapping between the locant and atom object
	 * @param locant A locant as a string
	 * @param a An atom
	 */
	void addMappingToAtomLocantMap(String locant, Atom a){
		atomMapFromLocant.put(locant, a);
	}

	/**
	 * Removes a mapping between a locant
	 * @param locant A locant as a string
	 */
	void removeMappingFromAtomLocantMap(String locant){
		atomMapFromLocant.remove(locant);
	}

	/**
	 * Checks to see whether a locant is present on this fragment
	 * @param string
	 * @return
	 */
	boolean hasLocant(String locant) {
		//FIXME Support all locant types
		return atomMapFromLocant.containsKey(locant);
	}

	Integer getIndicatedHydrogen() {
		return indicatedHydrogen;
	}

	void setIndicatedHydrogen(Integer indicatedHydrogen) {
		this.indicatedHydrogen = indicatedHydrogen;
	}


	Atom getAtomByIdOrNextSuitableAtomOrThrow(int id, int additionalValencyRequired) throws StructureBuildingException {
		Atom a =getAtomByIdOrNextSuitableAtom(id, additionalValencyRequired, false);
		if (a==null){
			throw new StructureBuildingException("No suitable atom found");
		}
		return a;
	}
	
	/**
	 * Takes an id and additional valency required. Returns the atom associated with that id if adding the specified valency will not violate
	 * that atom types maximum valency.
	 * If this is not possible it iterates sequentially through all atoms in the fragment till one is found
	 * Spare valency is initially taken into account so that the atom is not dearomatised
	 * If this is impossible to accomplish dearomatisation is done
	 * If an atom is still not found an exception is thrown
	 * atoms belonging to suffixes are never selected.
	 * @param id
	 * @param additionalValencyRequired The increase in valency that will be required on the desired atom
	 * @return Atom
	 * @throws StructureBuildingException
	 */
	Atom getAtomByIdOrNextSuitableAtom(int id, int additionalValencyRequired, boolean takeIntoAccountOutIDs) throws StructureBuildingException {
		List<Atom> atomList =getAtomList();
		int minId =atomList.get(0).getID();
		int maxId =atomList.get(atomList.size()-1).getID();
		int diff =maxId -minId;
		int startingId =id;
		int flag =0;
		Atom currentAtom=null;

		HashMap<Atom, Integer> valencyRequiredForConnections = new HashMap<Atom, Integer>();//takes into account valency required for connections as specified by the outIDs
		for (Atom atom : atomList) {
			valencyRequiredForConnections.put(atom, 0);
		}
		if (takeIntoAccountOutIDs){
			for (OutID outID : outIDs) {
				valencyRequiredForConnections.put(atomMapFromId.get(outID.id), valencyRequiredForConnections.get(atomMapFromId.get(outID.id)) +outID.valency);
			}
		}
		for (int i = 0; i <= diff; i++) {//aromaticity preserved
			if (id > maxId){
				id -=(diff+1);
			}
			currentAtom=getAtomByIDOrThrow(id);
			if (currentAtom.getType().equals("suffix")){
				id++;
				continue;
			}
			if(ValencyChecker.checkValencyAvailableForBond(currentAtom, additionalValencyRequired + currentAtom.getSpareValency() +valencyRequiredForConnections.get(currentAtom)) == true){
				flag=1;
				break;
			}
			id++;
		}
		id =startingId;
		if (flag==0){
			for (int i = 0; i <= diff; i++) {//aromaticity dropped
				if (id > maxId){
					id -=(diff+1);
				}
				currentAtom=getAtomByIDOrThrow(id);
				if (currentAtom.getType().equals("suffix")){
					id++;
					continue;
				}
				if(ValencyChecker.checkValencyAvailableForBond(currentAtom, additionalValencyRequired + valencyRequiredForConnections.get(currentAtom)) == true){
					flag=1;
					break;
				}
				id++;
			}
		}
		if (flag==0){
			currentAtom=null;
		}
		return currentAtom;
	}

	/**
	 * Returns the id of the first atom that was added to this fragment
	 * @return
	 * @throws StructureBuildingException
	 */
	int getIdOfFirstAtom() throws StructureBuildingException {
		for (Atom a: atomCollection) {
			return a.getID();
		}
		throw new StructureBuildingException("Fragment is empty");
	}

	int getInValency() {
		return inValency;
	}

	void setInValency(int valency) {
		this.inValency = valency;
	}

	/**
	 * Clears and recreates atomMapFromId (and hence AtomCollection) using the order of the atoms in atomList
	 * @param atomList
	 * @throws StructureBuildingException
	 */
	void reorderAtomCollection(ArrayList<Atom> atomList) throws StructureBuildingException {
		if (atomMapFromId.size()!=atomList.size()){
			throw new StructureBuildingException("atom list is not the same size as the number of atoms in the fragment");
		}
		atomMapFromId.clear();
		for (Atom atom : atomList) {
			atomMapFromId.put(atom.getID(), atom);
		}
	}
}



