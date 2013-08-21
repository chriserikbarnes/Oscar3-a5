package uk.ac.cam.ch.wwmm.ptc.experimental;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;

import uk.ac.cam.ch.wwmm.ptclib.io.ResourceGetter;

import nu.xom.Builder;
import nu.xom.Element;
import nu.xom.NodeFactory;
import nu.xom.Nodes;
import nu.xom.Serializer;
import nu.xom.xslt.XSLTransform;

public class GENIACorpusNodeFactory extends NodeFactory {

	public static ResourceGetter rg = new ResourceGetter("uk/ac/cam/ch/wwmm/ptc/experimental/resources/");
	
	private XSLTransform xslt;
	private File papersDir;
	
	public GENIACorpusNodeFactory(File papersDir) throws Exception {
		super();
		xslt = new XSLTransform(rg.getXMLDocument("GENIAToSciXML.xsl"));
		this.papersDir = papersDir;
		if(!papersDir.exists()) papersDir.mkdir();
	}
	
	@Override
	public Nodes finishMakingElement(Element elem) {
		if(elem.getLocalName().equals("article")) {
			String id = elem.query("articleinfo/bibliomisc").get(0).getValue().trim();
			id = id.substring(id.lastIndexOf(":") + 1);
			File outDir = new File(papersDir, id);
			if(!outDir.exists()) outDir.mkdir();
			System.out.println(id);
			try	{
				xslt.setParameter("ne", true);
				Nodes n = xslt.transform(new Nodes(elem));
				Serializer ser = new Serializer(new FileOutputStream(new File(outDir, "genia-markedup.xml")));
				//ser.setIndent(2);
				ser.write(XSLTransform.toDocument(n));
				xslt.setParameter("ne", false);
				n = xslt.transform(new Nodes(elem));
				ser = new Serializer(new FileOutputStream(new File(outDir, "source.xml")));
				ser.write(XSLTransform.toDocument(n));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			//if(true) throw new RuntimeException("Stop!");
			return new Nodes();
		} else {
			return new Nodes(elem);
		}
	}
	
	public static void main(String [] args) throws Exception {
		new Builder(new GENIACorpusNodeFactory(new File("/local/scratch/ptc24/geniascixml"))).build(new File("/usr/groups/corpora/Genia/GENIAcorpus3.02.xml"));
	}
	
}
