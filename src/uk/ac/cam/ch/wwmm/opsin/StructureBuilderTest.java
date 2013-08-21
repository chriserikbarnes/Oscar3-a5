package uk.ac.cam.ch.wwmm.opsin;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import nu.xom.Builder;
import nu.xom.Document;

import org.junit.Test;


public class StructureBuilderTest {

	static Builder XMLBuilder = new Builder();
	Document testData;
	
	@Test
	public void testBuilderFromName() throws Exception {
		StructureBuilder structureBuilder = new StructureBuilder();
		assertNotNull("Got SuffixRules", structureBuilder.suffixRulesDoc);
		assertEquals("SuffixRules has root of suffixRulesList",
				structureBuilder.suffixRulesDoc.getRootElement().getLocalName(),
				"suffixRulesList");
	}
	
}
