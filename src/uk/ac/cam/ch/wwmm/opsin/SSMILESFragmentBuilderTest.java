package uk.ac.cam.ch.wwmm.opsin;

import static junit.framework.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;


public class SSMILESFragmentBuilderTest {

	Fragment fragment;
	SSMILESFragmentBuilder builder;
	
	@Before
	public void setUp() throws Exception {
		builder = new SSMILESFragmentBuilder();
	}

	@Test
	public void testBuild() throws StructureBuildingException {		
		fragment = builder.build("CC", new IDManager());
		assertNotNull("Got a fragment", fragment);
	}
}
