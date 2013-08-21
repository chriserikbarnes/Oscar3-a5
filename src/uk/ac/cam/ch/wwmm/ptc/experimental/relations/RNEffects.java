package uk.ac.cam.ch.wwmm.ptc.experimental.relations;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nu.xom.Element;

import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.tools.HydrogenAdder;

import uk.ac.cam.ch.wwmm.opsin.NameToStructure;
import uk.ac.cam.ch.wwmm.oscar3.chemnamedict.ChemNameDictSingleton;
import uk.ac.cam.ch.wwmm.oscar3.pcsql.PubChemSQL;
import uk.ac.cam.ch.wwmm.ptclib.cdk.ConverterToInChI;
import uk.ac.cam.ch.wwmm.ptclib.cdk.StereoInChIToMolecule;
import uk.ac.cam.ch.wwmm.ptclib.cdk.StructureConverter;

public class RNEffects {

	PubChemSQL pcsql;

	public RNEffects() throws Exception {
		pcsql = PubChemSQL.getInstance();
	}
	
	Pattern dealkylpattern = Pattern.compile("((O|N)-)?de(methyl|ethyl|propyl|butyl|pentyl|hexyl)(ations?|ases?)");
	Pattern hydroxypattern = Pattern.compile("(.+-hydroxy)l(ations?|ases?)");
	
	public String applyRN(String inchi, String rn, String name) throws Exception {
		Matcher dealkylmatcher = dealkylpattern.matcher(rn);
		Matcher hydroxymatcher = hydroxypattern.matcher(rn);
		if(hydroxymatcher.matches()) {
			if(!hydroxymatcher.matches()) return null;
			String hydroxyname = hydroxymatcher.group(1) + name;
			if(ChemNameDictSingleton.hasName(hydroxyname)) {
				return ChemNameDictSingleton.getInChIForShortestSmiles(hydroxyname);
			} else if(pcsql != null && pcsql.hasName(hydroxyname)) {
				return pcsql.getShortestSmilesAndInChI(hydroxyname)[1];
			} else {
				Element cml = NameToStructure.getInstance().parseToCML(hydroxyname);
				if(cml != null) {
					return StructureConverter.cmlToInChI(cml);
				}
			}
		} else if(dealkylmatcher.matches()) {
			StereoInChIToMolecule.primeCacheForInChI(inchi);
			IMolecule mol = ConverterToInChI.getMolFromInChI(inchi);
			StructureConverter.configureMolecule(mol);
			//System.out.println(mol.getAtomCount());
			mol = (IMolecule)mol.clone();

			String atomStr = dealkylmatcher.group(2);
			//if(atomStr == null) return null;
			int alkylSize = 0;
			String groupStr = dealkylmatcher.group(3);
			if(groupStr.equals("methyl")) {
				alkylSize = 1;
			} else if(groupStr.equals("ethyl")) {
				alkylSize = 2;
			} else if(groupStr.equals("propyl")) {
				alkylSize = 3;
			} else if(groupStr.equals("butyl")) {
				alkylSize = 4;
			} else if(groupStr.equals("pentyl")) {
				alkylSize = 5;
			} else if(groupStr.equals("hexyl")) {
				alkylSize = 6;
			}
			if(alkylSize == 0) return null;
			//StructureConverter.configureMolecule(mol);

			List<List<IAtom>> alkyl = getXAlkyl(mol, atomStr, alkylSize);
			if(alkyl.size() == 2 && alkyl.get(0).size() == alkyl.get(1).size()) {
				Set<IAtom> touching = new HashSet<IAtom>();
				for(IAtom atom : alkyl.get(0)) {
					touching.addAll((List<IAtom>)mol.getConnectedAtomsList(atom));
				}
				for(IAtom atom : alkyl.get(1)) {
					touching.addAll((List<IAtom>)mol.getConnectedAtomsList(atom));
				}
				touching.removeAll(alkyl.get(0));
				touching.removeAll(alkyl.get(1));
				if(touching.size() == 1) alkyl.remove(1);
			}
			if(alkyl.size() != 1) return null;
			Set<IAtom> touching = new HashSet<IAtom>();
			for(IAtom atom : alkyl.get(0)) {
				touching.addAll((List<IAtom>)mol.getConnectedAtomsList(atom));
				//System.out.println(mol.getAtomCount());
				mol.removeAtomAndConnectedElectronContainers(atom);
				//System.out.println(mol.getAtomCount());
			}

			touching.removeAll(alkyl);
			for(IAtom atom : touching) {
				//new HydrogenAdder().addExplicitHydrogensToSatisfyValency(mol, atom);
				//atom.setHydrogenCount(atom.getHydrogenCount()+1);
			}
			//mol.removeAtomAndConnectedElectronContainers(.get(0));
			StructureConverter.configureMolecule(mol);
//			/System.out.println(new SmilesGenerator().createSMILES(mol));
			//System.out.println(mol.getAtomCount());
			//System.out.println(ConverterToInChI.getInChI(mol));
			return ConverterToInChI.getInChI(mol);
		}
		return null;
	}
	
	public static List<List<IAtom>> getXAlkyl(IMolecule mol, String atomStr, int alkylLength) {
		List<List<IAtom>> groups = new ArrayList<List<IAtom>>(); 
		for(int i=0;i<mol.getAtomCount();i++) {
			IAtom a = mol.getAtom(i);
			if(a.getAtomicNumber() != 6) continue;
			if(mol.getBondOrderSum(a) != 1.0) continue;
			// OK, found a "methyl", let's see what's further on
			List<IAtom> connected = mol.getConnectedAtomsList(a);
			if(connected.size() != 1) continue;
			IAtom connectedAtom = connected.get(0);
			List<IAtom> atoms = new ArrayList<IAtom>();
			atoms.add(a);
			if(alkylLength == 1) {
				if(atomStr != null && !connectedAtom.getSymbol().equals(atomStr)) continue;
				if(connectedAtom.getSymbol().equals("C") && mol.getConnectedAtomsCount(connectedAtom) == 2) continue;
				groups.add(atoms);
				continue;
			} else {
				IAtom nextAtom = connectedAtom;
				IAtom prevAtom = a;
				boolean ok = true;
				for(int stepsToGo = alkylLength-1;stepsToGo>0;stepsToGo--) {
					if(!nextAtom.getSymbol().equals("C") || mol.getConnectedAtomsCount(nextAtom) != 2) {
						ok = false;
						break;
					}
					atoms.add(nextAtom);
					IAtom nextNextAtom = null;
					for(IAtom otherAtom : (List<IAtom>)mol.getConnectedAtomsList(nextAtom)) {
						if(otherAtom == prevAtom) continue;
						nextNextAtom = otherAtom;
					}
					prevAtom = nextAtom;
					nextAtom = nextNextAtom;
				}
				if(!ok) continue;
				if(atomStr != null && !nextAtom.getSymbol().equals(atomStr)) continue;
				if(nextAtom.getSymbol().equals("C") && mol.getConnectedAtomsCount(connectedAtom) == 2) continue;
				groups.add(atoms);
				continue;
			}
		}
		return groups;
	}

}
