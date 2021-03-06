package uk.ac.cam.ch.wwmm.ptclib.scixml;

import java.util.Collection;

import nu.xom.Attribute;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Node;
import nu.xom.Nodes;
import nu.xom.ProcessingInstruction;
import nu.xom.Text;
import uk.ac.cam.ch.wwmm.oscar3.Oscar3Props;

/** A subclass of Document, contains methods for generating SciXML and extensions.
 * 
 * @author ptc24
 *
 */
public final class SciXMLDocument extends Document {
	
	/**Makes an new SciXMLDocument from a document. The old document is
	 * effectively destroyed in the process (the root element is replaced
	 * with a dummy element).
	 * 
	 * @param doc The old Document. 
	 * @return The new SciXMLDocument.
	 */
	public static SciXMLDocument makeFromDoc(Document doc) {
		Element dummy = new Element("dummy");
		Element root = doc.getRootElement();
		doc.setRootElement(dummy);
		return new SciXMLDocument(root);
	}
	
	/**Makes a new SciXMLDocument from the given root element.
	 * 
	 * @param arg0 The root element.
	 */
	public SciXMLDocument(Element arg0) {
		super(arg0);
	}

	/**Creates an empty new SciXMLDocument.
	 * 
	 */
	public SciXMLDocument() {
		super(new Element("PAPER"));
	}
	
	/**Removes all processing instructions from the SciXML document.
	 * 
	 */
	public void removeProcessingInstructions() {
		Nodes n = query("processing-instruction()");
		for(int i=0;i<n.size();i++) {
			n.get(i).detach();
		}
	}
	
	/**Removes processing instructions from the document that specify a molecule
	 * viewer.
	 * 
	 */
	public void removeViewerProcessingInstruction() {
		Nodes n = query("processing-instruction('viewer')");
		for(int i=0;i<n.size();i++) {
			n.get(i).detach();
		}		
	}
	
	/**Add processing instructions, to assist with the viewing of the document
	 * when served up by a webserver.
	 * 
	 */
	public void addServerProcessingInstructions() {
		removeProcessingInstructions();
	    String serverRoot = Oscar3Props.getInstance().serverRoot;
	    if(serverRoot == null || serverRoot.equals("none")) serverRoot = "";
		
		ProcessingInstruction pi = new ProcessingInstruction("xml-stylesheet", "type=\"text/xsl\" href=\"" + serverRoot + "/toHTMLJS.xsl\"");
		insertChild(pi, 0);
		pi = new ProcessingInstruction("jarpath", "/");
		insertChild(pi, 1);
		pi = new ProcessingInstruction("host", Oscar3Props.getInstance().hostname + ":" + Oscar3Props.getInstance().port);
		insertChild(pi, 2);
		pi = new ProcessingInstruction("path", serverRoot);
		insertChild(pi, 3);
		pi = new ProcessingInstruction("viewer", "picture");
		insertChild(pi, 4);
		if(Oscar3Props.getInstance().polymerMode) {
			pi = new ProcessingInstruction("polymermode", "true");
			insertChild(pi, 5);			
		}
	}
	
	/**Sets the title of the document, creating a new TITLE element, or 
	 * replacing the contents of an existing element, as appropriate. 
	 * 
	 * @param s The title.
	 * @return The title element.
	 */
	public Element setTitle(String s) {
		return setTitle(new Text(s));
	}

	/**Sets the title of the document, creating a new TITLE element, or 
	 * replacing the contents of an existing element, as appropriate. 
	 * 
	 * @param n Either a text node, or an element whose contents are to be
	 * transplanted into the TITLE element.
	 * @return The title element.
	 */
	public Element setTitle(Node n) {
		if(query("/TITLE").size() == 0) {
			Element title = new Element("TITLE");
			title.appendChild(n);
			getRootElement().appendChild(title);
		return title;
		} else {
			Element title = (Element)query("/TITLE").get(0);
			title.removeChildren();
			title.appendChild(n);			
			return title;
		}
	}

	/*public Element setAbstract(String s) {
		return setAbstract(new Text(s));
	}

	public Element setAbstract(Node n) {
		if(query("/ABSTRACT").size() == 0) {
			Element title = new Element("ABSTRACT");
			title.appendChild(n);
			getRootElement().appendChild(title);
		return title;
		} else {
			Element title = (Element)query("/ABSTRACT").get(0);
			title.removeChildren();
			title.appendChild(n);			
			return title;
		}
	}*/
		
	/**Gets the body element of the document, creating one if it doesn't
	 * already exist.
	 * 
	 * @return The body element.
	 */
	public Element getBody() {
		Nodes n = query("/DIV");
		if(n.size() == 0) {
			Element body = new Element("BODY");
			getRootElement().appendChild(body);
			return body;
		} else {
			return (Element)n.get(0);
		}
	}
	
	/** Gets the last DIV in the document,
	 * creating if necessary.
	 * 
	 * @return The DIV Element.
	 */
	public Element getDiv() {
		Nodes n = query("//DIV");
		if(n.size() == 0) {
			Element div = new Element("DIV");
			getBody().appendChild(div);
			return div;
		} else {
			return (Element)n.get(n.size()-1);
		}
	}

	/** Creates and gets a DIV.
	 * 
	 * @return The DIV Element.
	 */
	public Element getNewDiv(String headStr) {
		Element div = new Element("DIV");
		getBody().appendChild(div);
		Element header = new Element("HEADER");
		div.appendChild(header);
		header.appendChild(headStr);
		return div;
	}
	
	/** Adds a list to the last DIV, creating
	 * backwards if necessary.
	 * @return A new LIST@TYPE=bullet element
	 */
	public Element addList() {
		Element list = makeList();
		getDiv().appendChild(list);
		return list;
	}
	
	/**Creates a new empty LIST element, configured for bullet points.
	 * 
	 * @return The new LIST element.
	 */
	public Element makeList() {
		Element list = new Element("LIST");
		list.addAttribute(new Attribute("TYPE", "bullet"));
		return list; 
	}
	
	/**Creates a new LIST element, configured for bullet points, containing
	 * the specified items.
	 * 
	 * @param items The strings to be turned into list items.
	 * @return The new LIST element.
	 */
	public Element makeList(Collection<String> items) {
		Element list = makeList();
		for(String item:items) {
			list.appendChild(makeListItem(item));
		}
		return list;
	}
	
	/** Make a new, empty, list item.
	 * 
	 * @return The list item;
	 */
	public Element makeListItem() {
		Element li = new Element("LI");
		return li;
	}
	
	/** Make a new list item, containing a string.
	 * 
	 * @param s The contents of the list item
	 * @return The list item.
	 */
	public Element makeListItem(String s) {
		Element li = makeListItem();
		li.appendChild(s);
		return li;
	}
	
	/** Make a new list item, containing a Node.
	 * 
	 * @param n The contents of the list item
	 * @return The list item
	 */
	public Element makeListItem(Node n) {
		Element li = makeListItem();
		li.appendChild(n);
		return li;		
	}
	
	/**Make a new, empty, link element.
	 * 
	 * @param href The HREF to link to.
	 * @return The new link.
	 */
	public Element makeLink(String href) {
		Element a = new Element("a");
		a.addAttribute(new Attribute("href", href));
		return a;
	}
	
	/**Make a new link element, with the specified contents.
	 * 
	 * @param href The HREF to link to.
	 * @param contents The link text.
	 * @return The new link.
	 */
	public Element makeLink(String href, String contents) {
		Element a = makeLink(href);
		a.appendChild(contents);
		return a;
	}
	
	/**Make a new paragraph element, and add it to the document.
	 * 
	 * @return The new paragraph element.
	 */
	public Element addPara() {
		Element p = new Element("P");
		getDiv().appendChild(p);
		return p;
	}
	
}
