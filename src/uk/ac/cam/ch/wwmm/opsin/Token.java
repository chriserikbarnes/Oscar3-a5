package uk.ac.cam.ch.wwmm.opsin;

import uk.ac.cam.ch.wwmm.ptclib.xml.XOMTools;
import nu.xom.Attribute;
import nu.xom.Element;

/**A token in a chemical name. hex, yl, ane, chloro etc.
 * Stores information about the XML element that will be produced for the token.
 *
 * @author ptc24
 *
 */
class Token {

	/**A reference copy of the XML element to produce*/
	private Element elem;

	/**Makes a new Token based on a tagname. To be used for regex tokens.
	 *
	 * @param tagName The localName of the XML tag to be produced.
	 */
	Token(String tagName) {
		elem = new Element(tagName);
	}

	/**Makes a new Token based on a tagname. To be used for regex tokens.
	 *
	 * @param tagName The localName of the XML tag to be produced.
	 * @param type A string to be used as a type attribute
	 */
	Token(String tagName, String type) {
		elem = new Element(tagName);
		elem.addAttribute(new Attribute("type",type));
	}

	/**Makes a new Token based on reference elements from an XML file.
	 *
	 * @param tokenElement The token element in the XML tokens file.
	 * @param tokenList The tokenList element the token was taken from.
	 */
	Token(Element tokenElement, Element tokenList) {
		elem = XOMTools.shallowCopy(tokenElement);
		elem.setLocalName(tokenList.getAttributeValue("tagname"));
		if(tokenList.getAttribute("type") != null) {
			elem.addAttribute(new Attribute("type", tokenList.getAttributeValue("type")));
		}
		if(tokenList.getAttribute("subType") != null) {
			elem.addAttribute(new Attribute("subType", tokenList.getAttributeValue("subType")));
		}
	}

	/**Makes an XML element of the token.
	 *
	 * @param text The string to go in the Text node contained within the Element.
	 * @return The element produced.
	 */
	Element makeElement(String text) {
		Element tokenElement = XOMTools.shallowCopy(elem);
		tokenElement.appendChild(text);
		return tokenElement;
	}
}
