package uk.ac.cam.ch.wwmm.oscar3.test.testcard;

import java.util.regex.Pattern;

import uk.ac.cam.ch.wwmm.ptclib.saf.SafTools;

import nu.xom.Element;

public class SurfaceRegexFilter extends AbstractFilter {

	private Pattern pattern;
	
	private SurfaceRegexFilter() {
		
	}
	
	public SurfaceRegexFilter(String regex) {
		this.pattern = Pattern.compile(regex);
	}
	
	@Override
	public boolean testElement(Element elem) {
		return pattern.matcher(SafTools.getSlotValue(elem, "surface")).matches();
	}

}
