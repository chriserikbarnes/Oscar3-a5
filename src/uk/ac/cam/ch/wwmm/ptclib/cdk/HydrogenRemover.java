package uk.ac.cam.ch.wwmm.ptclib.cdk;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openscience.cdk.Bond;
import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IMolecule;

/** Removes explict hydrogens from a molecule, leaving hydrogens that attach
 * to heteroatoms or that indicate stereochemistry at bridgeheads.
 * 
 * @author ptc24
 *
 */
public final class HydrogenRemover {

	private Map<Integer,Integer> valencies;
	
	private HydrogenRemover() {
		/* A small dictionary of problem atoms.
		 * These disobey normal conventions, in that negative charges boost their
		 * valency, and positives subtract from it, rather than vice versa.
		 */
		valencies = new HashMap<Integer, Integer>();
		valencies.put(1, 1);
		valencies.put(3, 1);
		valencies.put(11, 1);
		valencies.put(19, 1);
		valencies.put(37, 1);
		valencies.put(55, 1);
		valencies.put(87, 1);
		
		valencies.put(5, 3);
		valencies.put(13, 3);
		valencies.put(31, 3);
	}

	/**Removes insignificant hydrogen atoms from a molecule, including 
	 * bridgehead hydrogens.
	 * 
	 * @param mol The molecule.
	 */
	public static void removeInsignificantHydrogens(IMolecule mol) {
		removeInsignificantHydrogens(mol, true);
	}

	/**Removes insignificant hydrogen atoms from a molecule.
	 * 
	 * @param mol The molecule.
	 * @param removeBridgeHeads Whether to remove bridgehead hydrogens.
	 */
	public static void removeInsignificantHydrogens(IMolecule mol, boolean removeBridgeHeads) {
		boolean verbose = false;
		for(int i=0;i<mol.getAtomCount();i++) {			
			if(mol.getAtom(i).getAtomicNumber() == 1) {
				if(verbose) System.out.println("Uncharged?");
				if(mol.getAtom(i).getFormalCharge() != 0) continue;
				List l = mol.getConnectedBondsList(mol.getAtom(i));
				if(verbose) System.out.println("One bond?");
				if(l.size() != 1) continue;
				Bond b = (Bond)l.get(0);
				if(verbose) System.out.println("Not a stereo bond?");
				if(b.getStereo() != CDKConstants.STEREO_BOND_NONE) continue;
				IAtom otherAtom = b.getConnectedAtom(mol.getAtom(i));
				// Special cases
				/*if(getInstance().valencies.containsKey(otherAtom.getAtomicNumber())) {
					int valency = getInstance().valencies.get(otherAtom.getAtomicNumber());
					if(valency == 1) {
						if(otherAtom.getFormalCharge() != 0) valency = 0;
					}
					System.out.println(valency);
					if(valency < mol.getConnectedAtomsCount(otherAtom)) {
						System.out.println("removing");
						System.out.println(mol.getAtomCount());
						mol.removeAtomAndConnectedElectronContainers(mol.getAtom(i));
						System.out.println(mol.getAtomCount());
						i--;
						continue;
					}
				}*/
				if(verbose) System.out.println("Connect to carbon?");
				if(verbose) System.out.println(otherAtom.getAtomicNumber());
				if(otherAtom.getAtomicNumber() != 6) continue;
				if(verbose) System.out.println("Bridgehead?");
				if(!removeBridgeHeads && mol.getConnectedAtomsCount(otherAtom) == 4) {
					List ll = mol.getConnectedAtomsList(otherAtom);
					boolean isBridgehead = true;
					for(Object o : ll) {
						IAtom aa = (IAtom)o;
						if(aa == mol.getAtom(i)) continue;
						// Detect cyclicity
						if(!isRingBond(mol, aa, otherAtom)) {
							// ie if it isn't cyclic
							isBridgehead = false;
							break;
						}
					}
					if(isBridgehead) continue;
				}
				mol.removeAtomAndConnectedElectronContainers(mol.getAtom(i));
				i--;
			}
		}	
	}

	private static boolean isRingBond(IMolecule mol, IAtom a1, IAtom a2) {
		//IBond b = mol.getBond(a1, a2);
		//if(b == null) throw new Exception();
		Set<IAtom> atoms = new HashSet<IAtom>();
		
		return scanForRingBonds(mol, a1, a1, a2, atoms);
	}
	
	private static boolean scanForRingBonds(IMolecule mol, IAtom targetAtom, IAtom a1, IAtom a2, Set<IAtom> atoms) {
		boolean isRingBond = false;
		List l = mol.getConnectedAtomsList(targetAtom);
		for(Object o : l) {
			IAtom a = (IAtom)o;
			if(a2.equals(a) && !a1.equals(targetAtom)) {
				isRingBond = true;
				//throw new Exception("Cyclic double bond");
			} else if(!a1.equals(a) && !a2.equals(a) && !atoms.contains(a)) {
				atoms.add(a);
				isRingBond |= scanForRingBonds(mol, a, a1, a2, atoms);
			}
		}
		return isRingBond;
	}
	
}
