package uk.ac.aber.art_tool;

import java.util.HashSet;
import java.util.Set;

import nu.xom.Attribute;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Node;
import nu.xom.Nodes;
import uk.ac.cam.ch.wwmm.ptclib.xml.XMLInserter;
import uk.ac.cam.ch.wwmm.ptclib.xml.XMLSpanTagger;
import uk.ac.cam.ch.wwmm.ptclib.xml.XOMTools;

/** Converts a SciXML file into ScrapBook format, for hand-editing.
 * 
 * @author Maria Liakata
 * @author ptc24
 *
 */
public class ConvertToAnnotatedPaper {

	public static AnnotatedPaper makeAnnotatedPaper(Document doc, String name) throws Exception {
		return makeAnnotatedPaper(doc, name, true, false);
	}
	
	public static AnnotatedPaper makeAnnotatedPaper(Document doc, String name, boolean noExperimental, boolean noCaptions) throws Exception {
		System.err.println("makeAnnotatedPaper");
		
		doc = (Document)doc.copy();
		
		Nodes n = null;
		Set<Node> excludeNodes = new HashSet<Node>();
		
		if(noExperimental) {
			Nodes experimentalSections = doc.query(ARTXMLStrings.EXPERIMENTAL_SECTION_XPATH);
			for(int i=0;i<experimentalSections.size();i++) {				
				Nodes subNodes = experimentalSections.get(i).query(".//P|.//HEADER");
				for(int j=0;j<subNodes.size();j++) excludeNodes.add(subNodes.get(j));
			}
			System.err.println("noexperimental");
			n = doc.query(ARTXMLStrings.SMALL_CHEMICAL_PLACES_XPATH);
		} else if(noCaptions) {
			System.err.println("noCaptions");
			//n = doc.query(XMLStrings.TEST_ONE_SNIPPET);
			n = doc.query(ARTXMLStrings.SMALL_CHEMICAL_PLACES_XPATH);			
		} else {
			System.err.println("no nothing");
			//n = doc.query(XMLStrings.TEST_ONE_SNIPPET);
			n = doc.query(ARTXMLStrings.CHEMICAL_PLACES_XPATH);
		}
		
		AnnotatedPaper ap = new AnnotatedPaper(name, true);
		ap.addSourceDoc(doc);

		//for(int i=0;i<n.size();i++) {
			//Element e = (Element)n.get(i);
			//if(excludeNodes.contains(e)) continue;
			//if(e.getValue().length() == 0) continue;
			Element e = (Element)doc.query("//PAPER").get(0);
			String xPoint = XOMTools.getXPointToNode(e, doc.getRootElement());
			String a = e.toXML();
			String b = XOMTools.getNodeAtXPoint(doc.getRootElement(), xPoint).toXML();
			if(!a.equals(b)) {
				System.out.println("Yikes!");
				System.out.println(a);
				System.out.println(b);
			}
			e = (Element)e.copy();
			e.addAttribute(new Attribute("XPoint", xPoint));
			ap.addScrap(e, null);
		return ap;
	}
	
	public static void importAnnotations(AnnotatedPaper ap, Document doc) throws Exception {
		Document scrapDoc = ap.getDoc();
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
