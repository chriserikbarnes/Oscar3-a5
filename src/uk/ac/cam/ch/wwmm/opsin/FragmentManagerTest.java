package uk.ac.cam.ch.wwmm.opsin;

import static junit.framework.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;


public class FragmentManagerTest {

	FragmentManager fragManager;
	IDManager idm;

	@Before
	public void setUp() throws Exception {
		fragManager = new FragmentManager(new SSMILESFragmentBuilder(), new CMLFragmentBuilder(), new IDManager());
	}

	@Test
	public void testGetUnifiedFrags() throws Exception {
		Fragment frag1 = fragManager.buildSMILES("CC");
		Fragment frag2 = fragManager.buildSMILES("CNC");

		fragManager.attachFragments(frag1.getAtomByLocant("1"), frag2.getAtomByLocant("1"), 1);
		Fragment frag = fragManager.getUnifiedFrags();
		assertEquals("Frag has five atoms", 5, frag.getAtomList().size());
		assertEquals("Frag has four bonds", 4, frag.getBondList().size());
	}

	@Test
	public void testRelabelFusedRingSystem() throws StructureBuildingException {
		SSMILESFragmentBuilder ssBuilder = new SSMILESFragmentBuilder();
		Fragment naphthalene = ssBuilder.build("C1=CC=CC2=CC=CC=C12", new IDManager());
		fragManager.relabelFusedRingSystem(naphthalene);
		assertEquals("Locant 1 = atom 1", 1, naphthalene.getIDFromLocant("1"));
		assertEquals("Locant 4a = atom 5", 5, naphthalene.getIDFromLocant("4a"));
		assertEquals("Locant 8 = atom 9", 9, naphthalene.getIDFromLocant("8"));
		assertEquals("Locant 8a = atom 10", 10, naphthalene.getIDFromLocant("8a"));
		assertEquals("No locant 9", 0, naphthalene.getIDFromLocant(""));
	}

}
