package uk.ac.cam.ch.wwmm.oscar3server.scrapbook;

import java.util.HashSet;
import java.util.Set;

import nu.xom.Attribute;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Node;
import nu.xom.Nodes;
import uk.ac.cam.ch.wwmm.ptclib.scixml.PubXMLToSciXML;
import uk.ac.cam.ch.wwmm.ptclib.scixml.XMLStrings;
import uk.ac.cam.ch.wwmm.ptclib.xml.XMLInserter;
import uk.ac.cam.ch.wwmm.ptclib.xml.XMLSpanTagger;
import uk.ac.cam.ch.wwmm.ptclib.xml.XOMTools;

/** Converts a PubXML or SciXML file into ScrapBook format, for hand-editing.
 * 
 * @author ptc24
 *
 */
public final class PaperToScrapBook {

	/**Makes a new scrapbook from a SciXML paper, placing it in the scrapbook
	 * directory, using the NoExperimental option.
	 * 
	 * @param doc The document to be converted to a scrapbook.
	 * @param name The name to be given to the scrapbook.
	 * @throws Exception
	 */
	public static void makeScrapBook(Document doc, String name) throws Exception {
		makeScrapBook(doc, name, true, false);
	}
	
	/**Makes a new scrapbook from a SciXML paper, placing it in the scrapbook
	 * directory.
	 * 
	 * @param doc The document to be converted to a scrapbook.
	 * @param name The name to be given to the scrapbook.
	 * @param noExperimental Whether to exclude the experimental section and captions.
	 * @param noCaptions Whether to exclude the captions.
	 * @throws Exception
	 */
	public static void makeScrapBook(Document doc, String name, boolean noExperimental, boolean noCaptions) throws Exception {
		PubXMLToSciXML ptsx = null;
		
		if(doc.getDocType() != null && doc.getDocType().getSystemID().equals("http://www.rsc.org/dtds/rscart37.dtd")) {
			ptsx = new PubXMLToSciXML(doc);
			doc = ptsx.getSciXML();
		}
		
		doc = (Document)doc.copy();
		
		Nodes n = null;
		Set<Node> excludeNodes = new HashSet<Node>();
		
		if(noExperimental) {
			Nodes experimentalSections = doc.query(XMLStrings.getInstance().EXPERIMENTAL_SECTION_XPATH, XMLStrings.getInstance().getXpc());
			for(int i=0;i<experimentalSections.size();i++) {				
				Nodes subNodes = experimentalSections.get(i).query(".//P|.//HEADER");
				for(int j=0;j<subNodes.size();j++) excludeNodes.add(subNodes.get(j));
			}
			n = doc.query(XMLStrings.getInstance().SMALL_CHEMICAL_PLACES_XPATH, XMLStrings.getInstance().getXpc());
		} else if(noCaptions) {
			n = doc.query(XMLStrings.getInstance().SMALL_CHEMICAL_PLACES_XPATH, XMLStrings.getInstance().getXpc());			
		} else {
			n = XMLStrings.getInstance().getChemicalPlaces(doc);
		}
		
		ScrapBook sb = new ScrapBook(name, true);
		sb.addSourceDoc(doc);
		
		for(int i=0;i<n.size();i++) {
			Element e = (Element)n.get(i);
			if(excludeNodes.contains(e)) continue;
			if(e.getValue().length() == 0) continue;
			String xPoint = XOMTools.getXPointToNode(e, doc.getRootElement());
			String a = e.toXML();
			String b = XOMTools.getNodeAtXPoint(doc.getRootElement(), xPoint).toXML();
			if(!a.equals(b)) {
				System.err.println("Yikes!");
				System.err.println(a);
				System.err.println(b);
			}
			e = (Element)e.copy();
			e.addAttribute(new Attribute("XPoint", xPoint));
			sb.addScrap(e, null);
		}
				
		if(ptsx != null) sb.addPubXMLDoc(ptsx);
	}
	
	static void importAnnotations(ScrapBook sb, Document doc) throws Exception {
		Document scrapDoc = sb.getDoc();
		Nodes snippetNodes = scrapDoc.query("//snippet");
		for(int i=0;i<snippetNodes.size();i++) {
			try {
			Element snippet = (Element)snippetNodes.get(i);
			Element targetElem = (Element)XOMTools.getNodeAtXPoint(doc.getRootElement(), snippet.getAttributeValue("XPoint"));

			XMLInserter xi = new XMLInserter(targetElem, "a", "c");
			
			XMLSpanTagger xst = new XMLSpanTagger(snippet, "b");
			Nodes neNodes = snippet.query(".//ne");
			for(int j=0;j<neNodes.size();j++) {
				xi.insertTaggedElement((Element)neNodes.get(j));
			}
			xst.deTagDocument();
			xi.deTagDocument();
			} catch (Exception e) {
				
			}
		}
	}
		
}
