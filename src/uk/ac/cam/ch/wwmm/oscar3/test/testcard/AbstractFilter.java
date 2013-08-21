package uk.ac.cam.ch.wwmm.oscar3.test.testcard;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import nu.xom.Element;

public abstract class AbstractFilter implements SAFElementFilter {

	protected SAFElementFilter prevFilter;
	protected boolean negate;
	
	public AbstractFilter() {
		prevFilter = null;
		negate=false;
	}
		
	public void setPrevFilter(SAFElementFilter prevFilter) {
		this.prevFilter = prevFilter;
	}
	
	public SAFElementFilter setNegate(boolean negate) {
		this.negate = negate;
		return this;
	}
		
	public SAFElementFilter chain(SAFElementFilter chainFilter) {
		chainFilter.setPrevFilter(this);
		return chainFilter;
	}
	
	public Set<Element> filter(Collection<Element> annotElems) {
		if(prevFilter != null) {
			annotElems = prevFilter.filter(annotElems);
		}
		Set<Element> results = new LinkedHashSet<Element>();
		for(Element elem : annotElems) {
			if(testElement(elem) ^ negate) results.add(elem);
		}
		return results;
	}

	abstract public boolean testElement(Element elem);
	
}
