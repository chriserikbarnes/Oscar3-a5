package uk.ac.cam.ch.wwmm.oscar3.test.testcard;

import java.util.Collection;
import java.util.Set;

import nu.xom.Element;

public interface SAFElementFilter {

	public Set<Element> filter(Collection<Element> annotElems);
	public SAFElementFilter setNegate(boolean negate);
	public SAFElementFilter chain(SAFElementFilter chainFilter);
	public void setPrevFilter(SAFElementFilter prevFilter);

}
