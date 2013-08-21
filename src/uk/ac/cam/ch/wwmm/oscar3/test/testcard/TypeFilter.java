package uk.ac.cam.ch.wwmm.oscar3.test.testcard;

import uk.ac.cam.ch.wwmm.ptclib.saf.SafTools;
import nu.xom.Element;

public class TypeFilter extends AbstractFilter {

	private String type;
	
	private TypeFilter() {
		
	}
	
	public TypeFilter(String type) {
		super();
		this.type = type;
	}
	
	@Override
	public boolean testElement(Element elem) {
		return type.equals(SafTools.getSlotValue(elem, "type"));
	}

}
