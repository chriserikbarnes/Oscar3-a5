package uk.ac.cam.ch.wwmm.ptclib.test;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;

import org.junit.Before;
import org.junit.Test;

import uk.ac.cam.ch.wwmm.ptclib.xml.XOMFormatter;
import static junit.framework.Assert.*;

public class XOMFormatterTest {
	
	Document testDoc;
	
	static String testXMLString = "<tokenList type=\"group\" name=\"aryl\">\r\n" +
	"    <tokens value=\"benzene\" name=\"benz\">benz_</tokens>\r\n" +
	"    <tokens value=\"naphthalene\" name=\"naphth\">naphth</tokens>\r\n" +
	"    <tokens value=\"trityl\" name=\"trityl\">trityl</tokens>\r\n" +
	"</tokenList>\r\n";

	@Before
	public void setUp() throws Exception {
		testDoc = new Builder().build(testXMLString, "//notimportant/");		
	}

	@Test
	public void testElemToString() {
		XOMFormatter testXOMFormatter = new XOMFormatter();
		assertNotNull("Created OK", testXOMFormatter);
		
		Element rootElem = testDoc.getRootElement();
		assertEquals("Get formatted XML", testXMLString, testXOMFormatter.elemToString(rootElem));
	}

}
