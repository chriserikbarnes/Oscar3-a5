package uk.ac.cam.ch.wwmm.ptc.experimental;

import java.io.File;
import java.io.FileOutputStream;

import uk.ac.cam.ch.wwmm.ptclib.io.ResourceGetter;
import uk.ac.cam.ch.wwmm.ptclib.xml.XOMTools;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Nodes;
import nu.xom.Serializer;
import nu.xom.XPathContext;
import nu.xom.xslt.XSLTransform;

public class PMCOAI {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		//ResourceGetter rg = new ResourceGetter("uk/ac/cam/ch/wwmm/ptc/experimental/resources/");
		//Document xslDoc = rg.getXMLDocument("nlmORplos.xsl");
		//XSLTransform xslt = new XSLTransform(xslDoc);
		
		File testDir = new File("/home/ptc24/pmctest");
		if(!testDir.exists()) testDir.mkdir();
		
		// TODO Auto-generated method stub
		String url = "http://www.pubmedcentral.nih.gov/oai/oai.cgi?verb=ListRecords&metadataPrefix=pmc";
		url = url + "&set=" + "beilstein";
		System.out.println("Fetching " + url);
		Document doc = new Builder().build(url);
		System.out.println("Fetched!");
		//new Serializer(System.out).write(doc);
		XPathContext xpc = new XPathContext();
		xpc.addNamespace("oai", "http://www.openarchives.org/OAI/2.0/");
		xpc.addNamespace("art", "http://dtd.nlm.nih.gov/2.0/xsd/archivearticle");
		
		while(doc != null) {
			Nodes paperNodes = doc.query("/oai:OAI-PMH/oai:ListRecords/oai:record/oai:metadata/art:article", xpc);
			System.out.println("Found " + paperNodes.size() + " papers");
			for(int i=0;i<paperNodes.size();i++) {
				Element e = (Element)paperNodes.get(i);
				Nodes idNodes = e.query("art:front/art:article-meta/art:article-id[@pub-id-type='publisher-id']", xpc);
				String name = idNodes.get(0).getValue();
				File outFile = new File(testDir, name + ".xml");
				e.detach();
				Document outDoc = new Document(e);
				new Serializer(new FileOutputStream(outFile)).write(outDoc);
				//XOMTools.clearNamespaces(outDoc.getRootElement());
				//Document sciXML = XSLTransform.toDocument(xslt.transform(outDoc));
				//File sciXMLFile = new File(testDir, name + ".sci.xml");
				//new Serializer(new FileOutputStream(sciXMLFile)).write(sciXML);				
			}
			Nodes rtokNodes = doc.query("/oai:OAI-PMH/oai:ListRecords/oai:resumptionToken", xpc);
			if(rtokNodes.size() == 0) {
				doc = null;
			} else {
				String rtok = rtokNodes.get(0).getValue();
				if(rtok == null || rtok.length() == 0) {
					doc = null;
				} else {
					System.out.println("Being polite...");
					Thread.sleep(3000);
					System.out.println("Fetching more records...");
					url = "http://www.pubmedcentral.nih.gov/oai/oai.cgi?verb=ListRecords&resumptionToken=" + rtok;
					doc = new Builder().build(url);
					System.out.println("Fetched");
				}
			}
		}
		System.out.println("Got all content");
	}

}
