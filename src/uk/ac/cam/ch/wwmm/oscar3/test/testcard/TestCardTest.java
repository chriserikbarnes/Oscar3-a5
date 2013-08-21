package uk.ac.cam.ch.wwmm.oscar3.test.testcard;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import nu.xom.Document;

import org.junit.Test;

import uk.ac.cam.ch.wwmm.oscar3.flow.OscarFlow;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.finder.DFAONTCPRFinder;
import uk.ac.cam.ch.wwmm.ptclib.io.ResourceGetter;
import uk.ac.cam.ch.wwmm.ptclib.scixml.TextToSciXML;

/**Does overall testing of some Oscar3 functionality. This is a slight abuse
 * of JUnit but should be useful.
 * 
 * @author ptc24
 *
 */
public class TestCardTest {

	private static ResourceGetter rg = new ResourceGetter("uk/ac/cam/ch/wwmm/oscar3/test/testcard/resources/");
	
	@Test
	public void testTestCard() throws Exception {
		DFAONTCPRFinder.getInstance();
		
		String s = rg.getString("testcard.txt");
		assertTrue("Have testcard string", s != null && s.length() > 0);
		Document doc = TextToSciXML.textToSciXML(s);
		assertTrue("Have SciXML document", doc != null);
		OscarFlow flow = new OscarFlow(doc);
		flow.runFlow("recognise resolve numbersaf inline data");
		assertNotNull("Have source XML", flow.getSourceXML());
		assertNotNull("Have inline XML", flow.getInlineXML());
		assertNotNull("Have SAF XML", flow.getSafXML());
		assertNotNull("Have data XML", flow.getDataXML());
		
		SAFTester tester = new SAFTester(flow.getSafXML());
		assertTrue("At least one annot", tester.atLeastOne(new AllElementsFilter()));
		assertTrue("Null filter gives no annots", tester.no(new NoElementsFilter()));
		assertTrue("At least one annot", tester.atLeastOne(new NoElementsFilter().setNegate(true)));
		assertTrue("Null filter gives no annots", tester.no(new AllElementsFilter().setNegate(true)));
		assertTrue("At least one annot", tester.atLeastOne(new AllElementsFilter().chain(new AllElementsFilter())));
		assertTrue("Null filter gives no annots", tester.no(new AllElementsFilter().chain(new NoElementsFilter())));
		assertTrue("Null filter gives no annots", tester.no(new NoElementsFilter().chain(new AllElementsFilter())));
		assertTrue("At least one annot", tester.atLeastOne(new AllElementsFilter().chain(new NoElementsFilter().setNegate(true))));
		assertTrue("At least one annot", tester.atLeastOne(new NoElementsFilter().setNegate(true).chain(new NoElementsFilter().setNegate(true))));
		//assertTrue("This should break", tester.atLeastOne(new NoElementsFilter()));
		//assertTrue("As should this", tester.no(new AllElementsFilter()));

		assertTrue("At least one CM", tester.atLeastOne(new TypeFilter("CM")));
		assertTrue("No CJ", tester.no(new TypeFilter("CJ")));
		
		assertTrue("Acetone", tester.atLeastOne(new SurfaceRegexFilter("acetone")));
	}
	
	public void testTestCard2() throws Exception {
		String s = rg.getString("testcard2.txt");
		Document doc = TextToSciXML.textToSciXML(s);
		OscarFlow flow = new OscarFlow(doc);
		flow.runFlow("recognise resolve");
		assertNotNull("Have SAF XML", flow.getSafXML());
		
		SAFTester tester = new SAFTester(flow.getSafXML());
		assertTrue("Has a CM", tester.atLeastOne(new TypeFilter("CM")));
		assertTrue("Has an ONT", tester.atLeastOne(new TypeFilter("ONT")));
		
		assertTrue("Has a single-word ONT", tester.atLeastOne(new TypeFilter("ONT").chain(new SurfaceRegexFilter("[a-z]+"))));
		assertTrue("Has a multi-word ONT", tester.atLeastOne(new TypeFilter("ONT").chain(new SurfaceRegexFilter("([a-z]+ )+[a-z]+"))));

		assertTrue("Has a single-word ONT with OntIDs", tester
				.atLeastOne(new TypeFilter("ONT")
				.chain(new SurfaceRegexFilter("[a-z]+"))
				.chain(new HasOntIDsFilter())));
		assertTrue("Has a multi-word ONT with OntIDs", tester
				.atLeastOne(new TypeFilter("ONT")
				.chain(new SurfaceRegexFilter("([a-z]+ )+[a-z]+"))
				.chain(new HasOntIDsFilter())));
		
		
	}

	
}
