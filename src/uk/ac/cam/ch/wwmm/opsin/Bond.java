package uk.ac.cam.ch.wwmm.opsin;

import nu.xom.Attribute;
import nu.xom.Element;

/**A bond, between two atoms.
 * 
 * @author ptc24
 *
 */
class Bond {
	/** The id of the atom the bond comes from */
	private int from;
	/** The id of the atom the bond goes to */
	private int to;
	/** The bond order */
	private int order;
	
	private int smilesNumber;

	/**Creates a new Bond.
	 * 
	 * @param from The ID of the atom the bond comes from.
	 * @param to The ID of the atom the bond goes to.
	 * @param order The bond order.
	 */
	Bond(int from, int to, int order) {
		this.from = from;
		this.to = to;
		this.order = order;
		smilesNumber = 0;
	}
	
	/**Produces a nu.xom.Element corresponding to a CML bond tag.
	 * Has attibutes of atomRefs2 and order.
	 * 
	 * @return The CML element.
	 */
	Element toCMLBond() {
		Element elem = new Element("bond");
		elem.addAttribute(new Attribute("atomRefs2", "a" + Integer.toString(from)
				+ " a" + Integer.toString(to)));
		elem.addAttribute(new Attribute("order", Integer.toString(order)));
		return elem;
	}
	
	/**Gets from.*/
	int getFrom() {
		return from;
	}

	/**Gets to.*/
	int getTo() {
		return to;
	}

	/**Gets order.*/
	int getOrder() {
		return order;
	}
	
	/**Sets order.*/
	void setOrder(int o) {
		order = o;
	}
	
	/**Adds to the bond order.
	 * 
	 * @param o The value to be added to the bond order.
	 */
	void addOrder(int o) {
		order += o;
	}
	
	int getSMILESNumber() {
		return smilesNumber;
	}

	int assignSMILESNumber(IDManager idm) {
		smilesNumber = idm.getNextID();
		return smilesNumber;
	}


}
