package uk.ac.cam.ch.wwmm.oscar3.test.testcard;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import nu.xom.Element;

public class NoElementsFilter extends AbstractFilter {

	@Override
	public boolean testElement(Element elem) {
		return false;
	}	
	
}
