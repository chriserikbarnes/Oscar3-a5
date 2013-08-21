package uk.ac.cam.ch.wwmm.oscar3.test.testcard;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;

public class SAFTester {

	private Document safDoc;
	private List<Element> annots;
	
	public SAFTester(Document safDoc) {
		this.safDoc = safDoc;
		annots = new ArrayList<Element>();
		Elements annotElems = safDoc.getRootElement().getChildElements("annot");
		for(int i=0;i<annotElems.size();i++) {
			annots.add(annotElems.get(i));
		}
	}
	
	public boolean atLeastOne(SAFElementFilter filter) {
		Set<Element> elems = filter.filter(annots);
		return elems.size() > 0;
	}

	public boolean no(SAFElementFilter filter) {
		Set<Element> elems = filter.filter(annots);
		return elems.size() == 0;
	}
	
}
