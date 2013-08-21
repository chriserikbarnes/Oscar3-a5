package uk.ac.cam.ch.wwmm.ptclib.scixml;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;

import nu.xom.Attribute;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;
import nu.xom.Nodes;
import nu.xom.ParentNode;
import nu.xom.ProcessingInstruction;
import nu.xom.xslt.XSLTransform;
import uk.ac.cam.ch.wwmm.ptclib.io.ResourceGetter;
import uk.ac.cam.ch.wwmm.ptclib.xml.XOMTools;

/** Used for sorting lists/whatever of XPoints. Long before short, otherwise
 * lexical. Somewhat experimental.
 * 
 * @author ptc24
 *
 */
class XPointComparator implements Comparator<String> {

	public int compare(String o1, String o2) {
		if(o1.length() < o2.length()) {
			return 1;
		} else if(o1.length() > o2.length()) {
			return -1;
		} else {
			return o1.compareTo(o2);
		}
	}
	
}

/** Converts a Publisher's XML (PubXML) file to SciXML. Also supports backporting
 * of annotations. Needs a stylesheet - supplied separately.
 * 
 * @author ptc24
 *
 */
public class PubXMLToSciXML {
	
	private static ResourceGetter rg = new ResourceGetter("uk/ac/cam/ch/wwmm/ptclib/scixml/");
	
	private Document pubXMLDoc;
	private Document sciXMLDoc;
	private Document cleanSciXMLDoc;
	private XSLTransform xslt;
	private TreeMap<String, String> xpc;
	private List<ProcessingInstruction> procInstructions;
	//private String namespace;
	
	public PubXMLToSciXML(Document pubXMLDoc) throws Exception {
		this(pubXMLDoc, true);
	}
	
	public PubXMLToSciXML(Document pubXML, boolean cleanup) throws Exception {
		procInstructions = new ArrayList<ProcessingInstruction>();
		this.pubXMLDoc = pubXML;
		XOMTools.setNamespaceURIRecursively(pubXML.getRootElement(), null);
		addNodeIds(pubXML.getRootElement(), pubXML.getRootElement());
		Document stylesheet = rg.getXMLDocument("rsc_art2paper_rng.xsl");
		xslt = new XSLTransform(stylesheet);
		sciXMLDoc = XSLTransform.toDocument(xslt.transform(pubXML));
		cleanSciXMLDoc = (Document)XOMTools.safeCopy(sciXMLDoc);
		xpc = new TreeMap<String, String>(new XPointComparator());
		Nodes n = sciXMLDoc.query("//*[@nodeID]");
		for(int i=0;i<n.size();i++) {
			Element e = (Element)n.get(i);
			xpc.put(XOMTools.getXPointToNode(e, sciXMLDoc.getRootElement()), e.getAttributeValue("nodeID"));
		}
		if(cleanup) {
			n = sciXMLDoc.query("//*/@nodeID");
			for(int i=0;i<n.size();i++) n.get(i).detach();
			n = pubXML.query("//*/@nodeID");
			for(int i=0;i<n.size();i++) n.get(i).detach();
			this.pubXMLDoc = new Document((Element)
					XOMTools.safeCopy(pubXML.getRootElement()));
			sciXMLDoc = new Document((Element)XOMTools.safeCopy(sciXMLDoc.getRootElement()));
		} else {
			addNewNodeIds(sciXMLDoc.getRootElement(), sciXMLDoc.getRootElement());
		}
	}

	public PubXMLToSciXML(Document pubXML, Document sciXML, Document cleanSciXML, Document convDoc) throws Exception {
		this.pubXMLDoc = pubXML;
		removeProcessingInstructions();
		this.sciXMLDoc = sciXML;
		this.cleanSciXMLDoc = cleanSciXML;
		docToXpc(convDoc);
	}
	
	public void removeProcessingInstructions() {
		procInstructions = new ArrayList<ProcessingInstruction>();
		Nodes piNodes = pubXMLDoc.getRootElement().query(".//processing-instruction()");
		for(int i=0;i<piNodes.size();i++) {
			ProcessingInstruction pi = (ProcessingInstruction)piNodes.get(i);
			ParentNode piParent = pi.getParent();
			int index = piParent.indexOf(pi);
			Element e = new Element("pi-proxy");
			e.addAttribute(new Attribute("pinumber", Integer.toString(i)));
			pi.detach();
			piParent.insertChild(e, index);
			procInstructions.add(pi);
		}		
	}
	
	public void replaceProcessingInstructions(Document newPubDoc) {
		Nodes proxyNodes = newPubDoc.query("//pi-proxy");
		for(int i=0;i<proxyNodes.size();i++) {
			Element e = (Element)proxyNodes.get(i);
			ParentNode eParent = e.getParent();
			int index = eParent.indexOf(e);
			int pin = Integer.parseInt(e.getAttributeValue("pinumber"));
			ProcessingInstruction pi = procInstructions.get(pin);
			e.detach();
			eParent.insertChild(pi, index);
		}				
	}
	
	public Document getSciXML() {
		return sciXMLDoc;
	}
	
	public void setSciXMLDoc(Document sciXMLDoc) {
		this.sciXMLDoc = sciXMLDoc;
	}
	
	public Document getSourceXML() {
		return pubXMLDoc;
	}
	
	public Document getCleanSciXML() {
		return cleanSciXMLDoc;
	}
	
	public Document getAnnotatedPubXML() throws Exception {
		Document safDoc = InlineToSAF.extractSAFs(sciXMLDoc, cleanSciXMLDoc, "dummy");
		return getAnnotatedPubXML(safDoc);
	}

	public Document getAnnotatedPubXML(Document safDoc) throws Exception {
		transformSAFs(safDoc);
		Document outDoc = SAFToInline.safToInline(safDoc, pubXMLDoc, false);
		replaceProcessingInstructions(outDoc);
		checkNes(outDoc);
		return outDoc;
	}
	
	public static Document getAnnotatedPubXML(Document pubXML, Document sciXML, Document cleanSciXML, Document convDoc) throws Exception {
		PubXMLToSciXML ptsx = new PubXMLToSciXML(pubXML, sciXML, cleanSciXML, convDoc);
		return ptsx.getAnnotatedPubXML();
	}
	
	public String transformXPoint(String sciXMLXPoint) throws Exception {
		for(String prefix : xpc.keySet()) {
			if(sciXMLXPoint.startsWith(prefix)) return xpc.get(prefix) + sciXMLXPoint.substring(prefix.length());
		}
		return null;
	}
	
	public Document xpcToDoc() throws Exception {
		Element xpcElem = new Element("xpc"); 
		for(String s : xpc.keySet()) {
			Element conv = new Element("convert");
			conv.addAttribute(new Attribute("from", s));
			conv.addAttribute(new Attribute("to", xpc.get(s)));
			xpcElem.appendChild(conv);
		}
		return new Document(xpcElem);
	}
	
	public void docToXpc(Document doc) throws Exception {
		xpc = new TreeMap<String, String>(new XPointComparator());
		Elements elems = doc.getRootElement().getChildElements();
		for(int i=0;i<elems.size();i++) {
			Element e = elems.get(i);
			xpc.put(e.getAttributeValue("from"), e.getAttributeValue("to"));
		}
	}
	
	public void transformSAFs(Document safDoc) throws Exception {
		Nodes annots = safDoc.query("//annot");
		for(int i=0;i<annots.size();i++) {
			Element annot = (Element)annots.get(i);
			String fromXPoint = annot.getAttributeValue("from");
			String toXPoint = annot.getAttributeValue("to");
			
			fromXPoint = transformXPoint(fromXPoint);
			toXPoint = transformXPoint(toXPoint);
			if(fromXPoint == null || toXPoint == null) {
				annot.detach(); 
			} else {
				annot.addAttribute(new Attribute("from", fromXPoint));
				annot.addAttribute(new Attribute("to", toXPoint));
			}
		}
	}
	
	public static void addNodeIds(Element elem, Element root) {
		elem.addAttribute(new Attribute("nodeID", XOMTools.getXPointToNode(elem, root)));
		Elements children = elem.getChildElements();
		for(int i=0;i<children.size();i++) addNodeIds(children.get(i), root);
	}

	public static void addNewNodeIds(Element elem, Element root) {
		elem.addAttribute(new Attribute("newNodeID", XOMTools.getXPointToNode(elem, root)));
		Elements children = elem.getChildElements();
		for(int i=0;i<children.size();i++) addNewNodeIds(children.get(i), root);
	}
	
	public static boolean checkNes(Document doc) {
		Nodes n = doc.query("//ne");
		int errorCount = 0;
		for(int i=0;i<n.size();i++) {
			if(!(n.get(i).getValue().equals(((Element)n.get(i)).getAttributeValue("surface")))) {
				System.err.println("Warning: bad NE deleted: " + n.get(i).toXML());
				XOMTools.removeElementPreservingText((Element)n.get(i));
				errorCount++;
			}
		}
		return errorCount > 0;
	}
	
	public static boolean isRSCDoc(Document doc) {
		if(doc.getDocType() != null && doc.getDocType().getSystemID().equals("http://www.rsc.org/dtds/rscart37.dtd")) return true;
		if("RSCART3.8".equals(doc.getRootElement().getAttributeValue("dtd"))) return true;
		return false;
	}

}
