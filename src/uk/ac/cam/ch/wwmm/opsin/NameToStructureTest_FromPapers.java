package uk.ac.cam.ch.wwmm.opsin;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import nu.xom.Element;

import org.junit.Test;


public class NameToStructureTest_FromPapers {

	@Test
	public void testNameToStructure() throws Exception {
		NameToStructure nts = new NameToStructure();
		assertNotNull("Got a name to structure convertor", nts);
	}

	@Test
	public void testParseToCML() throws Exception {
		NameToStructure nts = new NameToStructure();
		Element cml = nts.parseToCML("ethane");
		assertEquals("Parsed ethane OK", "<cml xmlns=\"http://www.xml-cml.org/schema\"><molecule id=\"m1\"><atomArray>" +
				"<atom id=\"a1\" elementType=\"C\"><label value=\"1\" /></atom>" +
				"<atom id=\"a2\" elementType=\"C\"><label value=\"2\" /></atom>" +
				"<atom id=\"a1_h1\" elementType=\"H\" />" +
				"<atom id=\"a1_h2\" elementType=\"H\" />" +
				"<atom id=\"a1_h3\" elementType=\"H\" />" +
				"<atom id=\"a2_h1\" elementType=\"H\" />" +
				"<atom id=\"a2_h2\" elementType=\"H\" />" +
				"<atom id=\"a2_h3\" elementType=\"H\" />" +
				"</atomArray><bondArray>" +
				"<bond atomRefs2=\"a1 a2\" order=\"1\" />" +
	            "<bond atomRefs2=\"a1 a1_h1\" order=\"1\" />" +
	            "<bond atomRefs2=\"a1 a1_h2\" order=\"1\" />" +
	            "<bond atomRefs2=\"a1 a1_h3\" order=\"1\" />" +
	            "<bond atomRefs2=\"a2 a2_h1\" order=\"1\" />" +
	            "<bond atomRefs2=\"a2 a2_h2\" order=\"1\" />" +
	            "<bond atomRefs2=\"a2 a2_h3\" order=\"1\" />" +
				"</bondArray></molecule></cml>", cml.toXML());
		assertNull("Won't parse helloworld", nts.parseToCML("helloworld"));
	}
}
