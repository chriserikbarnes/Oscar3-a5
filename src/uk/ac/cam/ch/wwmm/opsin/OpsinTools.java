package uk.ac.cam.ch.wwmm.opsin;

import java.util.ArrayList;

import uk.ac.cam.ch.wwmm.ptclib.xml.XOMTools;

import nu.xom.Element;
import nu.xom.Elements;
import nu.xom.Node;
import nu.xom.Nodes;
import nu.xom.ParentNode;
import nu.xom.Text;

/**
 * A set of useful methods to assist OPSIN
 * Some of these may be duplicates of the useful functions in StringTools/XOMTools in OSCAR
 * @author dl387
 *
 */
class OpsinTools {

	/**
	 * Returns the next sibling suffix node which is not related to altering charge (ium/ide/id)
	 * @param group
	 */
	public static Element getNextNonChargeSuffix(Element current) {
		Element matchedElement =null;
		while (true) {
			Element next = (Element) XOMTools.getNextSibling(current);
			if (next != null) {
				if (next.getLocalName().equals("suffix")){
					if (next.getAttribute("subType")==null || !next.getAttributeValue("subType").equals("charge")){
						matchedElement=next;
						break;
					}
				}
				current = next;
			} else {
				break;
			}
		}
		return matchedElement;
	}

	/**
	 * Returns an arrayList of elements corresponding to the nodes given
	 * @param nodes
	 * @return The new arrayList
	 */
	public static ArrayList<Element> nodesToElementArrayList(Nodes nodes) {
		ArrayList<Element> elementList =new ArrayList<Element>(nodes.size());
		for (int i = 0, n=nodes.size(); i < n; i++) {
			elementList.add((Element) nodes.get(i));
		}
		return elementList;
	}

	/**
	 * Returns a new list containing the elements of list1 followed by list2
	 * @param list1
	 * @param list2
	 * @return The new list
	 */
	public static ArrayList<Element> combineElementLists(ArrayList<Element> list1, ArrayList<Element> list2) {
		ArrayList<Element> elementList =new ArrayList<Element>(list1);
		elementList.addAll(list2);
		return elementList;
	}

	/**
	 * Same as getPreviousSibling, but ignores hyphens
	 * @return
	 */
	public static Node getPreviousSiblingNonHyphen(Node node) {
		ParentNode parent = node.getParent();
		int i = parent.indexOf(node);
		if (i==0) return null;
		Element previous =(Element)parent.getChild(i-1);
		if (previous.getLocalName().equals("hyphen")){
			return getPreviousSiblingNonHyphen(previous);
		}
		return previous;
	}

	/**
	 * Same as getNextSibling, but ignores hyphens
	 * @return
	 */
	public static Node getNextSiblingNonHyphen(Node node) {
		ParentNode parent = node.getParent();
		int i = parent.indexOf(node);
		if (i+1 >= parent.getChildCount()) return null;
		Element next =(Element)parent.getChild(i+1);
		if (next.getLocalName().equals("hyphen")){
			return getNextSiblingNonHyphen(next);
		}
		return next;
	}
	
	
	/**
	 * Gets the next node which is not a hyphen. This element need not be a sibling
	 * @param current: starting node
	 * @return
	 */
	public static Node getNextNonHyphen(Node node) {
		Element parent = (Element) node.getParent();
		if (parent.getLocalName().equals("molecule")){
			return null;
		}
		int index = parent.indexOf(node);
		if (index +1 >=parent.getChildCount()) return getNextNonHyphen(parent);//reached end of element
		Element next =(Element) parent.getChild(index+1);
		Elements children =next.getChildElements();
		while (children.size()!=0){
			next =children.get(0);
			children =next.getChildElements();
		}
		if (next.getLocalName().equals("hyphen")){
			return getNextNonHyphen(next);
		}
		return next;
	}
	
	/**
	 * Gets the next node which is not a hyphen. 
	 * The siblings of the given node will be surveyed. 
	 * Then the first node of the node following the given node will be returned (children are recursively evaluated)
	 * @param current: starting node
	 * @return
	 */
	public static Node getNextNonHyphenAtSameOrLowerLevel(Node node) {
		Element parent = (Element) node.getParent();
		if (parent.getLocalName().equals("molecule")){
			return null;
		}
		int index = parent.indexOf(node);
		if (index +1 >=parent.getChildCount()) return null;
		Element next =(Element) parent.getChild(index+1);
		Elements children =next.getChildElements();
		while (children.size()!=0){
			next =children.get(0);
			children =next.getChildElements();
		}
		if (next.getLocalName().equals("hyphen")){
			return getNextNonHyphenAtSameOrLowerLevel(next);
		}
		return next;
	}
	
	
	/**
	 * Gets the previous node which is not a hyphen. This element need not be a sibling
	 * @param current: starting node
	 * @return
	 */
	public static Node getPreviousNonHyphen(Node node) {
		Element parent = (Element) node.getParent();
		if (parent.getLocalName().equals("molecule")){
			return null;
		}
		int index = parent.indexOf(node);
		if (index ==0) return getPreviousNonHyphen(parent);//reached beginning of element
		Element previous =(Element) parent.getChild(index-1);
		Elements children =previous.getChildElements();
		while (children.size()!=0){
			previous =children.get(children.size()-1);
			children =previous.getChildElements();
		}
		if (previous.getLocalName().equals("hyphen")){
			return getNextNonHyphen(previous);
		}
		return previous;
	}

	/**
	 * Returns the previous group. This group element need not be a sibling
	 * @param current: starting node
	 * @return
	 */
	public static Node getPreviousGroup(Element current) {
	  if (current.getLocalName().equals("group")){//can start with a group or the sub/root the group is in
		  current=(Element)current.getParent();
	  }
	  Element parent = (Element) current.getParent();
	  if (parent.getLocalName().equals("molecule")){
		  return null;
	  }
	  int index = parent.indexOf(current);
	  if (index ==0) return getPreviousGroup(parent);//no group found
	  Element previous =(Element) parent.getChild(index-1);
	  Elements children =previous.getChildElements();
	  while (children.size()!=0){
		  previous =children.get(children.size()-1);
		  children =previous.getChildElements();
	  }
	  Elements groups =((Element)previous.getParent()).getChildElements("group");
	  if (groups.size()==0){
		  return getPreviousGroup(previous);
	  }
	  else{
		  return groups.get(groups.size()-1);//return last group if multiple exist e.g. fused ring
	  }
	}


	/**
	 * If a dash is the last character it is removed
	 * @param locantText
	 * @return
	 */
	public static String removeDashIfPresent(String locantText){
		if(locantText.endsWith("-")) {
			locantText = locantText.substring(0, locantText.length()-1);
		}
		return locantText;
	}

	/**
	 * Sets the first text child of the group to the newName
	 * Throws an exception if the first child is not a Text node
	 * @param group
	 * @param newName
	 * @throws PostProcessingException 
	 */
	public static void setTextChild(Element group, String newName) throws PostProcessingException {
		Node textNode =group.getChild(0);
		if (textNode instanceof Text){
			((Text)textNode).setValue(newName);
		}
		else{
			throw new PostProcessingException("No Text Child Found!");
		}
	}

	/**
	 * Finds the word element that encloses the given element.
	 * Returns the word element or throws an exception
	 * @param Element el
	 * @return word Element
	 * @throws PostProcessingException 
	 */
	public static Element getParentWord(Element el) throws PostProcessingException {
		Element parent=(Element)el.getParent();
		while(parent !=null && !parent.getLocalName().equals("word")){
			parent =(Element)parent.getParent();
		}
		if (parent==null){
			throw new PostProcessingException("Cannot find enclosing word element");
		}
		else{
			return parent;
		}
	}
}
