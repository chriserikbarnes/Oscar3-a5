package uk.ac.cam.ch.wwmm.opsin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

/** A builder for fragments specified as SSMILES. A custom SMILES dialect is used:
 *
 * Allowed:
 * Organic elements B,C,N,O,P,S,F,Cl,Br,I (square brackets not required)
 * Aromatic elements c,n,o,p,s,as,se (square brackets not required)
 * =, # for bond orders
 * . for disconnection
 * (, ) for branching
 * [, ] for placing inorganic elements within
 * +, - for charge. Use multiple symbols to specify multiple charges e.g. ++ is a charge of +2
 * Square brackets are recommended to avoid confusion especially as a - at the end of the string has a special meaning
 *
 * 012345679 - ring closures
 * %1 %2 %10 %11 etc - more ring closures
 * Use only standard ring closures or these to avoid confusion
 * e.g. %101 is ring closure 101, not ring closure 10 and 1 as it would be in normal SMILES
 *
 * |3 |5 etc. can be used to set the valency of an atom e.g. P|5
 * (| looks slightly like an l as in the lambda convention)
 *
 * Also, an = or # at the start of the string indicates that the group attaches to its parent group via a double or triple bond.
 *
 * A -,=,# on the end indicates that in the absence of locants, other groups attach to
 * *it* via the atom at the end of the string, not at the start of the string with -,=,# meaning single,double or triple bond
 * This behaviour is overridden for certain suffixes to give different meanings to the atom the -,=,# is referring to
 *
 * @author ptc24/dl387
 *
 */
class SSMILESFragmentBuilder {

	/**A "struct" to hold information on the parsing stack
	 *
	 * @author ptc24
	 *
	 */
	private class StackFrame {
		/**The ID of the atom currently under consideration.*/
		int atomID;
		/**The order of the bond about to be formed.*/
		int bondOrder;

		/**Creates a stack frame with given parameters.
		 *
		 * @param atomIDval The value for atomID.
		 * @param bondOrderVal The value for bondOrder.
		 */
		StackFrame(int atomIDval, int bondOrderVal) {
			atomID = atomIDval;
			bondOrder = bondOrderVal;
		}

		/**Creates a copy of an existing StackFrame.
		 *
		 * @param sf The stackframe to copy.
		 */
		StackFrame(StackFrame sf) {
			atomID = sf.atomID;
			bondOrder = sf.bondOrder;
		}
	}

	/**Upper case letters.*/
	static String upperLetters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	/**Lower case letters.*/
	static String lowerLetters = "abcdefghijklmnopqrstuvwxyz";
	/**Numerical digits.*/
	static String digits = "0123456789";
	/**Organic Atoms.*/
	static String organicAtoms = "B,C,N,O,P,S,F,Cl,Br,I";
	/**Aromatic Atoms.*/
	static String aromaticAtoms = "c,n,o,p,s,as,se";


	/**Build a Fragment based on a SSMILES string, with a null type/subType.
	 *
	 * @param SSMILES The SSMILES string to build from.
	 * @return The built fragment.
	 * @throws StructureBuildingException 
	 */
	Fragment build(String SSMILES, IDManager idManager) throws StructureBuildingException {
		return build(SSMILES, null, null, null, idManager);
	}

	/**Build a Fragment based on a SSMILES string.
	 *
	 * @param SSMILES The SSMILES string to build from.
	 * @param type The type of fragment being built.
	 * @return The built fragment.
	 * @throws StructureBuildingException 
	 */
	Fragment build(String SSMILES, String type, String subType, String labelMapping, IDManager idManager) throws StructureBuildingException {
		List<String> labelMap = null;
		if(labelMapping != null && !labelMapping.equals("none")) {
			labelMap = new ArrayList<String>();
			String [] mappingTmp = labelMapping.split("/", -1);
			for(int i=0;i<mappingTmp.length;i++) {
				labelMap.add(mappingTmp[i]);
			}
		}
		int currentNumber = 1;
		Fragment currentFrag = new Fragment(type, subType);
		Stack<StackFrame> stack = new Stack<StackFrame>();
		stack.push(new StackFrame(0, 1));
		HashMap<String, StackFrame> closures = new HashMap<String, StackFrame>();
		String tmpString = new String(SSMILES);
		char firstCharacter =tmpString.charAt(0);
		if(firstCharacter == '=' || firstCharacter == '#') {
			tmpString = tmpString.substring(1);
		}
		char lastCharacter =tmpString.charAt(tmpString.length()-1);
		if(lastCharacter == '-' || lastCharacter == '=' || lastCharacter == '#') {
			tmpString = tmpString.substring(0, tmpString.length()-1);
		}

		int inSquareBrackets=0;
		while(tmpString.length() > 0) {
			String nextChar = tmpString.substring(0, 1);
			tmpString = tmpString.substring(1);
			if(nextChar.equals("(")) {
				stack.push(new StackFrame(stack.peek()));
			} else if(nextChar.equals(")")) {
				stack.pop();
			} else if(nextChar.equals("=")){
				stack.peek().bondOrder = 2;
			} else if(nextChar.equals("#")){
				stack.peek().bondOrder = 3;
			} else if(nextChar.equals(".")){
				stack.peek().atomID = 0;
			} else if(upperLetters.contains(nextChar)) {//normal atoms
				if(tmpString.length() > 0 && lowerLetters.contains(tmpString.substring(0,1))) {
					if (organicAtoms.contains(nextChar + tmpString.substring(0,1)) || inSquareBrackets==1){
						nextChar += tmpString.substring(0,1);
						tmpString = tmpString.substring(1);
					}
				}
				if (nextChar.equals("H")){//temporary cludge to allow stereochemistry in SMILES to be ignored
					continue;
				}
				int ID = idManager.getNextID();
				Atom atom = new Atom(ID, nextChar, currentFrag, type);
				if(labelMapping == null) {
					atom.addLocant(Integer.toString(currentNumber));
				} else if (!labelMapping.equals("none")){
					String labels[] = labelMap.get(currentNumber-1).split(",");
					for(int i=0;i<labels.length;i++) {
						atom.addLocant(labels[i]);
					}
				}
				currentFrag.addAtom(atom);
				if(stack.peek().atomID > 0) {
					currentFrag.addBond(new Bond(stack.peek().atomID,
							ID, stack.peek().bondOrder));
				}
				stack.peek().atomID = ID;
				stack.peek().bondOrder = 1;
				currentNumber += 1;
			} else if(lowerLetters.contains(nextChar)) {//aromatic atoms
				if(tmpString.length() > 0 && lowerLetters.contains(tmpString.substring(0,1))) {
					if (aromaticAtoms.contains(nextChar + tmpString.substring(0,1))){
						nextChar += tmpString.substring(0,1);
						tmpString = tmpString.substring(1);
					}
				}
				if (!aromaticAtoms.contains(nextChar)){
					throw new StructureBuildingException("Invalid aromatic atom: " + nextChar);
				}
				nextChar= Character.toUpperCase(nextChar.charAt(0)) + nextChar.substring(1);
				int ID = idManager.getNextID();
				Atom atom = new Atom(ID, nextChar, currentFrag, type);
				atom.addSpareValency(1);
				if(labelMapping == null) {
					atom.addLocant(Integer.toString(currentNumber));
				} else if (!labelMapping.equals("none")){
					String labels[] = labelMap.get(currentNumber-1).split(",");
					for(int i=0;i<labels.length;i++) {
						atom.addLocant(labels[i]);
					}
				}
				currentFrag.addAtom(atom);
				if(stack.peek().atomID > 0) {
					currentFrag.addBond(new Bond(stack.peek().atomID,
							ID, stack.peek().bondOrder));
				}
				stack.peek().atomID = ID;
				stack.peek().bondOrder = 1;
				currentNumber += 1;
			} else if(nextChar.equals("[")) {
				inSquareBrackets=1;
			} else if(nextChar.equals("]")) {
				inSquareBrackets=0;
			} else if(nextChar.equals("+")) {
				if(stack.peek().atomID > 0) {
					Atom a = currentFrag.getAtomByIDOrThrow(stack.peek().atomID);
					a.setCharge(a.getCharge() +1);
				}
				else{
					throw new StructureBuildingException("+ found in SMILES string at unexpected position");
				}
			} else if(nextChar.equals("-")) {
				if(stack.peek().atomID > 0) {
					Atom a = currentFrag.getAtomByIDOrThrow(stack.peek().atomID);
					a.setCharge(a.getCharge() -1);
				}
				else{
					throw new StructureBuildingException("- found in SMILES string at unexpected position");
				}
			} else if(digits.contains(nextChar) ||
					nextChar.equals("%")) {
				if(nextChar.equals("%")) {
					nextChar = "";
					while(tmpString.length() > 0 &&
							digits.contains(tmpString.substring(0,1))) {
						nextChar += tmpString.substring(0,1);
						tmpString = tmpString.substring(1);
					}
				}
				if(closures.containsKey(nextChar)) {
					StackFrame sf = closures.remove(nextChar);
					int bondOrder = 1;
					if(sf.bondOrder > 1) {
						bondOrder = sf.bondOrder;
					} else if(stack.peek().bondOrder > 1) {
						bondOrder = stack.peek().bondOrder;
					}
					currentFrag.addBond(new Bond(stack.peek().atomID, sf.atomID, bondOrder));
					stack.peek().bondOrder = 1;
				} else {
					StackFrame sf = new StackFrame(stack.peek());
					closures.put(nextChar, sf);
					stack.peek().bondOrder = 1;
				}
			} else if(nextChar.equals("|")) {
				nextChar = "";
				while(tmpString.length() > 0 &&
						digits.contains(tmpString.substring(0,1))) {
					nextChar += tmpString.substring(0,1);
					tmpString = tmpString.substring(1);
				}
				if(stack.peek().atomID > 0) {
					Atom a = currentFrag.getAtomByIDOrThrow(stack.peek().atomID);
					a.setValency(Integer.parseInt(nextChar));
				}
				else{
					throw new StructureBuildingException("| found in SMILES string at unexpected position");
				}
			}
		}
		if (labelMap != null && labelMap.size() >= currentNumber ){
			throw new StructureBuildingException("Group numbering has been invalidly defined in resource file: labels: " +labelMap.size() + ", atoms: " + (currentNumber -1) );
		}
		
		if(lastCharacter == '-' || lastCharacter == '=' || lastCharacter == '#') {
			List<Atom> aList =currentFrag.getAtomList();
			int lastAtomID =aList.get(aList.size()-1).getID();
			if (subType.equals("inSuffix")){
				currentFrag.setDefaultInID(lastAtomID);
			}
			else if (subType.equals("functionalSuffix")){
				currentFrag.addFunctionalID(lastAtomID);
			}
			else{
				if (lastCharacter == '#'){
					currentFrag.addOutID(lastAtomID,3, true);
				}
				else if (lastCharacter == '='){
					currentFrag.addOutID(lastAtomID,2, true);
				}
				else{
					currentFrag.addOutID(lastAtomID,1, true);
				}
			}
		}
		
		if(firstCharacter == '='){
			currentFrag.addOutID(currentFrag.getIdOfFirstAtom(),2, true);
		}
		if (firstCharacter == '#'){
			currentFrag.addOutID(currentFrag.getIdOfFirstAtom(),3, true);
		}

		return currentFrag;
	}

}
