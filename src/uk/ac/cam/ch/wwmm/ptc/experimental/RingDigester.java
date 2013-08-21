package uk.ac.cam.ch.wwmm.ptc.experimental;

import java.util.List;

import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.Molecule;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.interfaces.IRingSet;
import org.openscience.cdk.ringsearch.AllRingsFinder;
import org.openscience.cdk.ringsearch.RingPartitioner;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.smiles.SmilesParser;

public class RingDigester {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		SmilesParser sp = new SmilesParser(DefaultChemObjectBuilder.getInstance());
		IMolecule mol = sp.parseSmiles("C1CC12CC2CC1CC1");
		AllRingsFinder ar = new AllRingsFinder();
		IRingSet ringSet = ar.findAllRings(mol);
		List l = RingPartitioner.partitionRings(ringSet);
		for(Object o : l) {
			IAtomContainer ac = RingPartitioner.convertToAtomContainer((IRingSet)o);
			IMolecule nMol = new Molecule(ac);
			System.out.println(new SmilesGenerator().createSMILES(nMol));
		}
	}

}
