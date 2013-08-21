package uk.ac.cam.ch.wwmm.opsin;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import nu.xom.Element;

import org.junit.Test;


public class BondTest {
	
	@Test
	public void testBond() {
		Bond bond = new Bond(1, 2, 1);
		assertNotNull("Got bond", bond);
		assertEquals("From = 1", 1, bond.getFrom());
		assertEquals("To = 2", 2, bond.getTo());
		assertEquals("Order = 1", 1, bond.getOrder());
	}
	
	@Test
	public void testToCMLBond() {
		Bond bond = new Bond(1, 2, 1);
		Element elem = bond.toCMLBond();
		assertNotNull("Got XOM Element", elem);
		assertEquals("Correct XML", "<bond atomRefs2=\"a1 a2\" order=\"1\" />", elem.toXML());
	}
	
}
