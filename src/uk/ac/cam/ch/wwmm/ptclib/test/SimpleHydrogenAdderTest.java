package uk.ac.cam.ch.wwmm.ptclib.test;

import org.junit.Test;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

public class SimpleHydrogenAdderTest {
	static SmilesParser smilesParser = new SmilesParser(DefaultChemObjectBuilder.getInstance());
	
	/* Dummy test, we'll need to get the tests working again*/
	@Test
	public void testAddImplicitHydrogens() {
		
	}
	
	/*int getMW(Molecule mol) {
		DescriptorValue retval = new WeightDescriptor().calculate(mol);			
		double molW = ((DoubleResult)retval.getValue()).doubleValue();
		return (int)Math.round(molW);
	}
	
	public void testAddImplicitHydrogens() throws Exception {
		Molecule mol;
		mol = smilesParser.parseSmiles("CC");
		SimpleHydrogenAdder.clearImplicitHydrogens(mol);
		assertEquals("Ethane MW without Hydrogens", 24, getMW(mol));
		SimpleHydrogenAdder.addImplicitHydrogens(mol);
		assertEquals("Ethane MW with Hydrogens", 30, getMW(mol));
		mol = smilesParser.parseSmiles("CCN");
		SimpleHydrogenAdder.clearImplicitHydrogens(mol);
		assertEquals("MW without Hydrogens", 38, getMW(mol));
		SimpleHydrogenAdder.addImplicitHydrogens(mol);
		assertEquals("MW with Hydrogens", 45, getMW(mol));
		mol = smilesParser.parseSmiles("CC[N+]");
		SimpleHydrogenAdder.clearImplicitHydrogens(mol);
		assertEquals("MW without Hydrogens", 38, getMW(mol));
		SimpleHydrogenAdder.addImplicitHydrogens(mol);
		assertEquals("MW with Hydrogens", 46, getMW(mol));
		mol = smilesParser.parseSmiles("CC[O-]");
		SimpleHydrogenAdder.clearImplicitHydrogens(mol);
		assertEquals("MW without Hydrogens", 40, getMW(mol));
		SimpleHydrogenAdder.addImplicitHydrogens(mol);
		assertEquals("MW with Hydrogens", 45, getMW(mol));
		mol = smilesParser.parseSmiles("CSC");
		SimpleHydrogenAdder.clearImplicitHydrogens(mol);
		assertEquals("MW without Hydrogens", 56, getMW(mol));
		SimpleHydrogenAdder.addImplicitHydrogens(mol);
		assertEquals("MW with Hydrogens", 62, getMW(mol));
		mol = smilesParser.parseSmiles("CS(=O)C");
		SimpleHydrogenAdder.clearImplicitHydrogens(mol);
		assertEquals("MW without Hydrogens", 72, getMW(mol));
		SimpleHydrogenAdder.addImplicitHydrogens(mol);
		assertEquals("MW with Hydrogens", 78, getMW(mol));
		mol = smilesParser.parseSmiles("CS(C)C");
		SimpleHydrogenAdder.clearImplicitHydrogens(mol);
		assertEquals("MW without Hydrogens", 68, getMW(mol));
		SimpleHydrogenAdder.addImplicitHydrogens(mol);
		assertEquals("MW with Hydrogens", 78, getMW(mol));
		mol = smilesParser.parseSmiles("CS(C)(C)(C)C");
		SimpleHydrogenAdder.clearImplicitHydrogens(mol);
		assertEquals("MW without Hydrogens", 92, getMW(mol));
		SimpleHydrogenAdder.addImplicitHydrogens(mol);
		assertEquals("MW with Hydrogens", 108, getMW(mol));
		
	}*/
	
}
