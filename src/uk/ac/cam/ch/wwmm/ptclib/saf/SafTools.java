package uk.ac.cam.ch.wwmm.ptclib.saf;

import nu.xom.Attribute;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Nodes;

/** Generic routines for handling standoff annotations in SAF format.
 * 
 * @author ptc24
 *
 */
public final class SafTools {
	
	/**Sets the value of a slot in an annotation.
	 * 
	 * @param annot The annot element.
	 * @param slotName The slot name.
	 * @param slotVal The slot value.
	 */
	public static void setSlot(Element annot, String slotName, String slotVal) {
		Nodes n = annot.query("slot[@name=\"" + slotName + "\"]");
		for(int i=0;i<n.size();i++) n.get(i).detach();

		Element slot = new Element("slot");
		slot.addAttribute(new Attribute("name", slotName));
		slot.appendChild(slotVal);
		annot.appendChild(slot);
	}
	
	/**Gets the value of a slot, or null.
	 * 
	 * @param annot The annot element that (potentially) contains the slot.
	 * @param slotName The slot name.
	 * @return The slot value, or null.
	 */
	public static String getSlotValue(Element annot, String slotName) {
		Nodes n = annot.query("slot[@name=\"" + slotName + "\"]");
		if(n.size() > 0) {
			return n.get(0).getValue();
		} else {
			return null;
		}
	}
	
	/**Deletes a slot (if it exists) from an annot.
	 * 
	 * @param annot The annot element that (potentially) contains the slot.
	 * @param slotName The slot name.
	 */
	public static void removeSlot(Element annot, String slotName) {
		Nodes n = annot.query("slot[@name=\"" + slotName + "\"]");
		if(n.size() > 0) {
			for(int i=0;i<n.size();i++) {
				n.get(i).detach();
			}
		} else {
			return;
		}
	}

	/**Makes an empty annot element.
	 * 
	 * @param start The start XPoint.
	 * @param end The end XPoint.
	 * @param annotType The annot type.
	 * @return The annot element.
	 */
	public static Element makeAnnot(String start, String end, String annotType) {
		Element safElem = new Element("annot");
		safElem.addAttribute(new Attribute("from", start));
		safElem.addAttribute(new Attribute("to", end));
		safElem.addAttribute(new Attribute("type", annotType));
		return safElem;
	}
		
	/**Makes an annot element, with type and surface slots.
	 * 
	 * @param start The start XPoint.
	 * @param end The end XPoint.
	 * @param annotType The annot type.
	 * @param type The contents of the type slot.
	 * @param surface The contents of the surface slot.
	 * @return The annot element.
	 */
	public static Element makeAnnot(String start, String end, String annotType, String type, String surface) {
		Element safElem = makeAnnot(start, end, annotType);
		setSlot(safElem, "surface", surface);
		setSlot(safElem, "type", type);

		return safElem;
	}

	/**Goes through a SAF document, and assigns id numbers to all annots of a
	 * given type.
	 * 
	 * @param safDoc The SAF document.
	 * @param type The annot type.
	 * @param prefix This goes in front of the id numbers.
	 */
	public static void numberSaf(Document safDoc, String type, String prefix) {
		Nodes annots = safDoc.query("/saf/annot");
		int safID = 1;
		for(int i=0;i<annots.size();i++) {
			Element annot = (Element)annots.get(i);
			String annotType = annot.getAttributeValue("type");
			if(annotType.equals(type)) {
				annot.addAttribute(new Attribute("id", prefix + Integer.toString(safID++)));
			}
		}		
	}
}
