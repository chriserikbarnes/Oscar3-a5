package uk.ac.cam.ch.wwmm.oscar3.misc;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import uk.ac.cam.ch.wwmm.ptclib.saf.SafTools;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;

/**A class for looking through a SAF XML Document, discarding named entities
 * that are within a dataSection.
 * 
 * @author ptc24
 *
 */
public final class DiscardInDataSection {

	static class XPointComparator implements Comparator<String> {
		public int compare(String o1, String o2) {
			String [] o1p = o1.split("[/\\.]");
			String [] o2p = o2.split("[/\\.]");
			for(int i=1;i<Math.min(o1p.length, o2p.length);i++) {
				int o1i = Integer.parseInt(o1p[i]);
				int o2i = Integer.parseInt(o2p[i]);
				if(o1i > o2i) return 1;
				if(o1i < o2i) return -1;
			}
			return 0;
		}
	}
	
	/**Looks through a SAF XML Document, discarding named entities that are
	 * within a dataSection.
	 * 
	 * @param safDoc The SAF XML document to be altered.
	 */
	public static void discard(Document safDoc) {
		Elements elements = safDoc.getRootElement().getChildElements();
		XPointComparator xpc = new XPointComparator();
		List<String> xpoints = new ArrayList<String>();
		Set<String> dataStarts = new HashSet<String>();
		Set<String> dataStops = new HashSet<String>();
		Map<String,List<Element>> elementsByXPoints = new HashMap<String,List<Element>>();
		for(int i=0;i<elements.size();i++) {
			Element e = elements.get(i);
			String from = e.getAttributeValue("from");
			String to = e.getAttributeValue("to");
			xpoints.add(from);
			xpoints.add(to);
			if(SafTools.getSlotValue(e, "type").equals("dataSection")) {
				dataStarts.add(from);
				dataStops.add(to);
			} else {
				if(!elementsByXPoints.containsKey(from)) elementsByXPoints.put(from, new ArrayList<Element>());
				if(!elementsByXPoints.containsKey(to)) elementsByXPoints.put(to, new ArrayList<Element>());
				elementsByXPoints.get(from).add(e);
				elementsByXPoints.get(to).add(e);
			}
		}
		Collections.sort(xpoints, xpc);
		boolean inData = false;
		for(String xpoint : xpoints) {
			if(dataStarts.contains(xpoint)) {
				inData = true; 
			} else if(dataStops.contains(xpoint)) {
				inData = false;
			}
			if(inData) {
				List<Element> elems = elementsByXPoints.get(xpoint);
				if(elems != null) {
					for(Element elem : elems) {
						if(elem.getParent() != null) {
							elem.detach();
						}
					}					
				}
			}
		}
	}

}
