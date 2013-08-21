package uk.ac.cam.ch.wwmm.oscar3server.scrapbook;

import nu.xom.Builder;
import nu.xom.Element;
import nu.xom.xslt.XSLTransform;
import uk.ac.cam.ch.wwmm.ptclib.io.ResourceGetter;
import uk.ac.cam.ch.wwmm.ptclib.xml.XOMTools;

/**Used in ScrapBook. Only useful for HTML generated with my XSLT stylesheet
 * 
 * @author ptc24
 *
 */
final class HTMLToSciXML {

	private static ResourceGetter rg = new ResourceGetter("uk/ac/cam/ch/wwmm/oscar3server/scrapbook/resources/");
	
	private static HTMLToSciXML myInstance;
	
	private XSLTransform xslt;
	
	private HTMLToSciXML() throws Exception {
		xslt = new XSLTransform(rg.getXMLDocument("htmlToSciXML.xsl"));
	}
	
	static Element makeSciXML(String html) throws Exception {
		html = "<html>" + html + "</html>";
		HTMLToSciXML hts = getInstance();
		Element e = XSLTransform.toDocument(hts.xslt.transform(new Builder().build(html, "/"))).getRootElement();
		XOMTools.normalise(e);
		return new Element(e);
	}
	
	//public static void reinitialise() throws Exception {
	//	myInstance = null;
	//	getInstance();
	//}
	
	private static HTMLToSciXML getInstance() throws Exception {
		if(myInstance == null) myInstance = new HTMLToSciXML();
		return myInstance;
	}

}
