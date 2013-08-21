package uk.ac.cam.ch.wwmm.ptclib.cdk;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.vecmath.Point2d;

import org.openscience.cdk.Atom;
import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.Molecule;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.tools.HydrogenAdder;
import org.openscience.cdk.tools.ValencyHybridChecker;

import uk.ac.cam.ch.wwmm.oscar3.Oscar3Props;


/**Converts InChI Strings containing stereochemical informations into Molecules with 2D co-ordinates
 * and the correct stereochemistry. Both E/Z and R/S stereochemistry is supported.
 * @author ptc24
 *
 */
public final class StereoInChIToMolecule {

	private static class PriorityInformation implements Comparable<PriorityInformation> {
		boolean isCyclic;
		boolean isNotSingle;
		int atomicNumber;
		int groupSize;
		
		public PriorityInformation(boolean isCyclic, boolean isDouble, int atomicNumber, int groupSize) {
			this.isCyclic = isCyclic;
			this.isNotSingle = isDouble;
			this.atomicNumber = atomicNumber;
			this.groupSize = groupSize;
		}
		
		public int compareTo(PriorityInformation o) {
			if(!isNotSingle && o.isNotSingle) return 1;
			if(isNotSingle && o.isNotSingle) return -1;
			if(!isCyclic && o.isCyclic) return 1;
			if(isCyclic && o.isCyclic) return -1;
			if(atomicNumber == 1 && o.atomicNumber != 1) return -1;
			if(atomicNumber != 1 && o.atomicNumber == 1) return 1;
			if(groupSize > o.groupSize) return -1;
			if(groupSize < o.groupSize) return 1;
			if(atomicNumber > o.atomicNumber) return -1;
			if(atomicNumber < o.atomicNumber) return 1;
			return 0;
		}
	}
	
	private static Pattern bondLayerPattern = Pattern.compile("/b((\\d+-\\d+[+-])(,\\d+-\\d+[+-])*)");
	private static Pattern bondPattern = Pattern.compile("(\\d+-\\d+)([+-])");

	private static Pattern atomLayerPattern = Pattern.compile("/t((\\d+[+-\\?u])(,\\d+[+-\\?u])*)");
	private static Pattern atomPattern = Pattern.compile("(\\d+)([+-\\?u])");
	
	private static Pattern mPattern = Pattern.compile("/m([01])");
	
	private String targetInChI;
	private String resultingInChI;
	private IMolecule mol;
	private Map<String,IAtom> atomMap;
	private boolean verbose = false;

	/**Takes the InChI, generates the molecule, works out 2D coordinates
	 * including stereochemical information, and places the result in the
	 * ConverterToInChI cache. All exceptions thrown will be caught, silently
	 * if verbose is not set, with a message to system.err if set.
	 * 
	 * @param inchi The InChI.
	 */
	public static void primeCacheForInChINoThrow(String inchi) {
		try {
			primeCacheForInChI(inchi);
		} catch (Exception e) {
			if(Oscar3Props.getInstance().verbose) {
				System.err.println("Warning: couldn't generate stereochemistry for " + inchi);
			}
		}
	}

	
	/**Takes the InChI, generates the molecule, works out 2D coordinates
	 * including stereochemical information, and places the result in the
	 * ConverterToInChI cache.
	 * 
	 * @param inchi The InChI.
	 */
	public static void primeCacheForInChI(String inchi) {
		IMolecule mol = null;
		boolean done = false;
		if(isStereoInChI(inchi)) {
			mol = ConverterToInChI.getMolFromInChICache(inchi);
			boolean generate = false;
			if(mol == null) {
				generate = true;
				// Small
			} else if(mol.getAtomCount() <= 1) {
				generate = false;
				// Layout-free? 
			} else if(MultiFragmentStructureDiagramGenerator.hasStructure(mol)) {
				generate = true;
			}
			//System.out.println(generate);
			if(generate) {
				try {
					mol = getMolecule(inchi);
					if(mol != null) ConverterToInChI.cacheInChI(inchi, mol);
					done = true;
				} catch (Exception e) {
					if(Oscar3Props.getInstance().verbose) {
						System.err.println("Error in InChI stereochemical parsing for: " + inchi);
					}
					//e.printStackTrace();
				}
			}
		} 
		if(!done) {
			mol = ConverterToInChI.getMolFromInChI(inchi);
			if(mol != null) { 
				mol = new Molecule(mol);
				try {
					StructureConverter.configureMolecule(mol);
					new HydrogenAdder(new ValencyHybridChecker()).addExplicitHydrogensToSatisfyValency(mol);
					StructureConverter.addHydrogensForSilicon(mol);
				} catch (Exception e) {
					throw new Error(e);
				}
				HydrogenRemover.removeInsignificantHydrogens(mol);
				ConverterToInChI.cacheInChI(inchi, mol);
			}
		}
	}
	
	private static boolean isStereoInChI(String inchi) {
		if(bondLayerPattern.matcher(inchi).find()) return true;
		if(atomLayerPattern.matcher(inchi).find()) return true;
		return false;
	}
	
	private static IMolecule getMolecule(String inchi) throws Exception {
		StereoInChIToMolecule sm = new StereoInChIToMolecule(inchi);
		sm.prepareMolecule();
		sm.resultingInChI = ConverterToInChI.getInChI(sm.mol);
		//System.out.println(sm.resultingInChI);
		sm.invertRelevantDoubleBonds();
		//System.out.println(sm.resultingInChI);
		sm.invertRelevantStereoCenters();
		//System.out.println(sm.resultingInChI);
		if(!inchi.startsWith(sm.resultingInChI) && !sm.resultingInChI.startsWith(inchi)) throw new Exception("InChI mismatch - supplied + " +
				inchi + " generated " + sm.resultingInChI);
		//System.out.println(MyStructureDiagramGenerator.hasStructure(sm.mol));
		//System.out.println(sm.resultingInChI);
		return sm.mol;
	}
		
	private StereoInChIToMolecule(String targetInChI) {
		this.targetInChI = targetInChI;
		//System.out.println("Target Stereo InChI: " + targetInChI);
	}
	
	private void prepareMolecule() throws Exception {
		ConverterToInChI.clearInChIFromCache(targetInChI);
		mol = ConverterToInChI.getMolFromInChI(targetInChI);
		mol = new Molecule(mol);
		
		new HydrogenAdder().addExplicitHydrogensToSatisfyValency(mol);
		StructureConverter.addHydrogensForSilicon(mol);

		//System.out.println(mol);
		StructureConverter.configureMoleculeIsotopes(mol);
		HydrogenRemover.removeInsignificantHydrogens(mol, false);
		//System.out.println(mol);

		for(int i=0;i<mol.getAtomCount();i++) {
			mol.getAtom(i).setID(Integer.toString(i+1));
		}
		mol = MultiFragmentStructureDiagramGenerator.getMoleculeWith2DCoords(mol);
		atomMap = new HashMap<String,IAtom>();
 		for(int i=0;i<mol.getAtomCount();i++) {
 			atomMap.put(mol.getAtom(i).getID(), mol.getAtom(i));
		}
	}
	
	private Map<String,String> getBondTypeMap(String inchi) {
		Map<String,String> bondTypes = new HashMap<String,String>();		
		Matcher m = bondLayerPattern.matcher(inchi);
		if(m.find()) {
			String [] ss = m.group(1).split(",");
			for(int i=0;i<ss.length;i++) {
				Matcher mm = bondPattern.matcher(ss[i]);
				if(mm.matches()) {
					bondTypes.put(mm.group(1), mm.group(2));
				}
			}
		}
		return bondTypes;
	}
	
	private boolean isBondStereoMatch() {
		Matcher m = bondLayerPattern.matcher(targetInChI);
		String targetBondLayer = null;
		if(m.find()) {
			targetBondLayer = m.group(1);
			if(verbose) System.out.println(targetBondLayer);
		} else {
			return true;
		}
		m = bondLayerPattern.matcher(resultingInChI);
		if(m.find() && m.group(1).equals(targetBondLayer)) {
			if(verbose) System.out.println(m.group(1));
			return true;
		}
		return false;
	}
		
	private void invertRelevantDoubleBonds() throws Exception {
		Map<String,String> targetBondTypes = getBondTypeMap(targetInChI);
		int allowableIters = targetBondTypes.size();		
		while(!isBondStereoMatch()) {
			if(allowableIters-- < 0) throw new Exception("Not converging");
			if(verbose) System.out.println("T> " + targetInChI);
			if(verbose) System.out.println("R> " + resultingInChI);
			Map<String,String> resultingBondTypes = getBondTypeMap(resultingInChI);
			for(String bondStr : targetBondTypes.keySet()) {
				if(!resultingBondTypes.containsKey(bondStr)) continue;
				if(verbose) System.out.println(bondStr + " " + targetBondTypes.get(bondStr) + " " + resultingBondTypes.get(bondStr));
				if(!resultingBondTypes.containsKey(bondStr)) throw new Exception();
				if(!targetBondTypes.get(bondStr).equals(resultingBondTypes.get(bondStr))) {
					if(verbose) System.out.println("flip!");
					String [] ss = bondStr.split("-");
					flipAboutBond(ss[0], ss[1]);
				}
			}
			resultingInChI = ConverterToInChI.getInChIUsingApplication(mol);
		}
	}
	
	private void flipAboutBond(String atomId1, String atomId2) throws Exception {
		Set<IAtom> atoms = new HashSet<IAtom>();
		boolean isRingBond = getAtomsAfterAtom(mol, atomMap.get(atomId1), atomMap.get(atomId2), atoms);
		if(isRingBond) throw new Exception("Trying to flip about a ring bond");
		for(IAtom a : atoms) {
			reflectAtom(a, atomMap.get(atomId1), atomMap.get(atomId2));
		}

	}
	
	private boolean getAtomsAfterAtom(IMolecule mol, IAtom a1, IAtom a2, Set<IAtom> atoms) throws Exception {
		IBond b = mol.getBond(a1, a2);
		if(b == null) throw new Exception();

		return getAtomsAfterAtom(mol, a1, a1, a2, atoms);
	}
	
	private boolean getAtomsAfterAtom(IMolecule mol, IAtom targetAtom, IAtom a1, IAtom a2, Set<IAtom> atoms) throws Exception {
		boolean isRingBond = false;
		List l = mol.getConnectedAtomsList(targetAtom);
		for(Object o : l) {
			IAtom a = (IAtom)o;
			if(a2.equals(a) && !a1.equals(targetAtom)) {
				isRingBond = true;
				//throw new Exception("Cyclic double bond");
			} else if(!a1.equals(a) && !a2.equals(a) && !atoms.contains(a)) {
				atoms.add(a);
				isRingBond |= getAtomsAfterAtom(mol, a, a1, a2, atoms);
			}
		}
		return isRingBond;
	}
	
	private void reflectAtom(IAtom a, IAtom refAtom1, IAtom refAtom2) {
		Point2d refCoords = Reflection.reflect(a.getPoint2d(), refAtom1.getPoint2d(), refAtom2.getPoint2d());
		a.setPoint2d(refCoords);
	}
	
	/* Atomistic (sp3) stereochemistry */
	
	private void makeStereoCenterAtAtom(String atomId, boolean undefined) throws Exception {
		IAtom a = atomMap.get(atomId);
		List l = mol.getConnectedAtomsList(a);

		// First up - add a hydrogen to a bridgehead
		if(mol.getConnectedAtomsCount(a) == 3) {
			boolean isBridgehead = true;
			for(Object o : l) {
				IAtom aa = (IAtom)o;
				// Detect cyclicity
				Set<IAtom> danglingAtoms = new HashSet<IAtom>();
				if(!getAtomsAfterAtom(mol, aa, a, danglingAtoms)) {
					// ie if it isn't cyclic
					isBridgehead = false;
					break;
				}
			}
			if(isBridgehead) {
				IAtom newH = new Atom("H");
				mol.addAtom(newH);
				
			}
		}
		
		// Find the best bond
		PriorityInformation bestPi = null;
		IBond bestBond = null;
		for(Object o : l) {
			IAtom aa = (IAtom)o;
			IBond bond = mol.getBond(a, aa);
			if(bond.getStereo() != CDKConstants.STEREO_BOND_NONE) continue;
			Set<IAtom> danglingAtoms = new HashSet<IAtom>();
			boolean isCyclic = getAtomsAfterAtom(mol, aa, a, danglingAtoms);
			PriorityInformation pi = new PriorityInformation(isCyclic, bond.getOrder() != CDKConstants.BONDORDER_SINGLE, aa.getAtomicNumber(), danglingAtoms.size());
			if(bestPi == null || pi.compareTo(bestPi) > 0) {
				bestBond = bond;
				bestPi = pi;
			}
		}
		
		if(undefined) {
			bestBond.setStereo(CDKConstants.STEREO_BOND_UNDEFINED);
		} else if(bestBond.getAtom(0) == a) {
			bestBond.setStereo(CDKConstants.STEREO_BOND_UP);			
		} else {
			bestBond.setStereo(CDKConstants.STEREO_BOND_UP_INV);
		}
	}
	
	private void flipStereoCenterAtAtom(String atomId) throws Exception {
		IAtom a = atomMap.get(atomId);
		List l = mol.getConnectedAtomsList(a);
		// Find a pre-existing stereo bond
		for(Object o : l) {
			IAtom aa = (IAtom)o;
			IBond bond = mol.getBond(a, aa);
			if(bond.getStereo() != CDKConstants.STEREO_BOND_NONE) {
				boolean inv = (bond.getStereo() == CDKConstants.STEREO_BOND_UP_INV || bond.getStereo() == CDKConstants.STEREO_BOND_DOWN_INV);
				if(inv && (bond.getAtom(0) == a)) continue;
				if(!inv && !(bond.getAtom(0) == a)) continue;
				
				if(bond.getStereo() == CDKConstants.STEREO_BOND_UP) {
					bond.setStereo(CDKConstants.STEREO_BOND_DOWN);
				} else if(bond.getStereo() == CDKConstants.STEREO_BOND_DOWN) {
					bond.setStereo(CDKConstants.STEREO_BOND_UP);					
				} else if(bond.getStereo() == CDKConstants.STEREO_BOND_UP_INV) {
					bond.setStereo(CDKConstants.STEREO_BOND_DOWN_INV);
				} else if(bond.getStereo() == CDKConstants.STEREO_BOND_DOWN_INV) {
					bond.setStereo(CDKConstants.STEREO_BOND_UP_INV);					
				}
				return;
			}
		}
		throw new Exception("Couldn't invert the stereochemistry at a stereocenter");
	}
	
	private Map<String,String> getAtomTypeMap(String inchi) {
		Map<String,String> atomTypes = new HashMap<String,String>();		
		Matcher m = atomLayerPattern.matcher(inchi);
		if(m.find()) {
			String [] ss = m.group(1).split(",");
			for(int i=0;i<ss.length;i++) {
				Matcher mm = atomPattern.matcher(ss[i]);
				if(mm.matches()) {
					atomTypes.put(mm.group(1), mm.group(2));
				}
			}
		}
		return atomTypes;
	}
	
	private boolean isAtomStereoMatch(String resultingInChI) {
		Matcher m = atomLayerPattern.matcher(targetInChI);
		String targetAtomLayer = null;
		if(m.find()) {
			targetAtomLayer = m.group(1);
			if(verbose) System.out.println("target: " + targetAtomLayer);
		} else {
			return true;
		}
		//targetAtomLayer = targetAtomLayer.replaceAll("u", "?");
		m = atomLayerPattern.matcher(resultingInChI);
		if(m.find() && m.group(1).equals(targetAtomLayer)) {
			if(verbose) System.out.println("resulting: " + m.group(1));
			return true;
		}
		return false;
	}

	private void invertRelevantStereoCenters() throws Exception {
		Map<String,String> targetAtomTypes = getAtomTypeMap(targetInChI);
		int allowableIters = targetAtomTypes.size();
		while(!isAtomStereoMatch(resultingInChI)) {
			if(allowableIters-- < 0) throw new Exception("Not converging: " + targetInChI);
			if(verbose) System.out.println("T> " + targetInChI);
			if(verbose) System.out.println("R> " + resultingInChI);
			Map<String,String> resultingAtomTypes = getAtomTypeMap(resultingInChI);
			if(verbose) System.out.println(resultingAtomTypes);
			for(String atomStr : targetAtomTypes.keySet()) {
				if(!resultingAtomTypes.containsKey(atomStr) && !targetAtomTypes.get(atomStr).equals("?")) {
					if(verbose) System.out.println(atomStr);
					makeStereoCenterAtAtom(atomStr, targetAtomTypes.get(atomStr).equals("u"));
					continue;
				}
				if(verbose) System.out.println(atomStr + " " + targetAtomTypes.get(atomStr) + " " + resultingAtomTypes.get(atomStr));
				if(targetAtomTypes.get(atomStr).equals("?")) continue;
				if(!resultingAtomTypes.containsKey(atomStr) && !resultingAtomTypes.get(atomStr).equals("?")) throw new Exception();
				if(!targetAtomTypes.get(atomStr).equals(resultingAtomTypes.get(atomStr))) {
					if(verbose) System.out.println("flip!");
					flipStereoCenterAtAtom(atomStr);
				}
			}
			resultingInChI = ConverterToInChI.getInChIUsingApplication(mol);
		}
		if(verbose) System.out.println("Almost done, check enantisomerism");
		if(verbose) System.out.println("T> " + targetInChI);
		if(verbose) System.out.println("R> " + resultingInChI);
		
		Matcher m = mPattern.matcher(targetInChI);
		if(m.find()) {
			String tmVal = m.group(1);
			Matcher mm = mPattern.matcher(resultingInChI);
			if(!mm.find()) throw new Error();
			String rmVal = mm.group(1);
			if(!tmVal.equals(rmVal)) {
				for(String atomStr : targetAtomTypes.keySet()) {
					if(verbose) System.out.println("Final flip:" + atomStr);
					flipStereoCenterAtAtom(atomStr);
				}				
				resultingInChI = ConverterToInChI.getInChIUsingApplication(mol);
			}
		}
	}

}
