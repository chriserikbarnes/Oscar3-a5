package uk.ac.cam.ch.wwmm.ptc.experimental;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Serializer;
import nu.xom.xslt.XSLTransform;
import uk.ac.cam.ch.wwmm.oscar3.flow.OscarFlow;
import uk.ac.cam.ch.wwmm.ptclib.io.ResourceGetter;
import uk.ac.cam.ch.wwmm.ptclib.scixml.SAFToInline;

public class OnAnnotated {

	private static String testStr = "<text><SENT sid=\"0\" pm=\".\"><plain><z:chebi ids=\"15377\">water</z:chebi> </plain></SENT></text>";
	
	private static OnAnnotated myInstance;
	private XSLTransform xslt;
	
	private OnAnnotated() {
		try {
			ResourceGetter rg = new ResourceGetter("uk/ac/cam/ch/wwmm/ptc/experimental/resources/");
			Document xsltDoc = rg.getXMLDocument("toSciXML.xsl");
			xslt = new XSLTransform(xsltDoc);			
		} catch (Exception e) {
			throw new Error(e);
		}
	}
	
	private static XSLTransform getXSLT() {
		if(myInstance == null) myInstance = new OnAnnotated();
		return myInstance.xslt;
	}
	
	private static String runOscar(String inStr) throws Exception {
		inStr = "<wrap xmlns:z=\"http://foo.bar\">" + inStr + "</wrap>";
		Document doc = new Builder().build(inStr, "");
			
		Document sciXML = XSLTransform.toDocument(getXSLT().transform(doc));
		
		OscarFlow of = new OscarFlow(sciXML);
		of.processToSAF();
		Document saf = of.getSafXML();
		doc = SAFToInline.safToInline(saf, doc, false);
		
		return doc.getRootElement().getChild(0).toXML();
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		System.out.println(runOscar(testStr));
	}

}
