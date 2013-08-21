package uk.ac.cam.ch.wwmm.oscar3.test.testcard;

import uk.ac.cam.ch.wwmm.ptclib.saf.SafTools;
import nu.xom.Element;

public class HasOntIDsFilter extends AbstractFilter {

	@Override
	public boolean testElement(Element elem) {
		String ontIDs = SafTools.getSlotValue(elem, "ontIDs");
		if(ontIDs != null && ontIDs.length() > 0) return true;
		return false;
	}

}
