package uk.ac.cam.ch.wwmm.oscar3.misc;

import nu.xom.Document;
import nu.xom.Nodes;
import nu.xom.xslt.XSLTransform;
import uk.ac.cam.ch.wwmm.ptclib.io.ResourceGetter;

/** A class for running an XSL stylesheet over inline-annotated SciXML. For use within
 * OscarFlow.
 * 
 * @author ptc24
 *
 */
public final class XSLOnInline {

	/**Runs an XSLT stylesheet over an inline-annotated SciXML document.
	 * 
	 * @param inline The inline XML document to modify.
	 * @param sheetName The filename of the XSLT stylesheet to use.
	 * @return The results of the XSL transform.
	 */
	public static Document runXSLOnInline(Document inline, String sheetName) {
		try {
			Document xslDoc = new ResourceGetter("uk/ac/cam/ch/wwmm/oscar3/misc/resources/").getXMLDocument(sheetName);
			XSLTransform xslt = new XSLTransform(xslDoc);
			Nodes n = xslt.transform(inline);
			return XSLTransform.toDocument(n);
		} catch (Exception e) {
			e.printStackTrace();
			return inline;
		}
	}
	
}
