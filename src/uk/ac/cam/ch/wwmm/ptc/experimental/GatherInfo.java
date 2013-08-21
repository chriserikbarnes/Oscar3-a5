package uk.ac.cam.ch.wwmm.ptc.experimental;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Nodes;
import nu.xom.xslt.XSLTransform;
import uk.ac.cam.ch.wwmm.oscar3.chemnamedict.ChemNameDictSingleton;
import uk.ac.cam.ch.wwmm.ptclib.io.FileTools;
import uk.ac.cam.ch.wwmm.ptclib.io.ResourceGetter;

public class GatherInfo {

	public static ResourceGetter rg = new ResourceGetter("uk/ac/cam/ch/wwmm/ptc/experimental/resources/");

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		XSLTransform xslt = new XSLTransform(rg.getXMLDocument("relations.xsl"));
		
		String inchiStr = ChemNameDictSingleton.getInChIForShortestSmiles("morphine");
		String ontID = "CHEBI:23888";
		File dir = new File("/home/ptc24/newows/corpora/BioIE");
		List<File> sourceFiles = FileTools.getFilesFromDirectoryByName(dir, "source.xml");
		long totalTime = 0;
		Set<String> relStrs = new HashSet<String>();
		for(File f : sourceFiles) {
			boolean hasRel = false;
			long time = System.currentTimeMillis();
			File paperDir = f.getParentFile();
			Document safDoc = new Builder().build(new File(paperDir, "saf.xml"));
			Document relDoc = new Builder().build(new File(paperDir, "relations.xml"));
			//Nodes annots = safDoc.query("/saf/annot[slot[@name='InChI'][.='" + inchiStr + "']]");
			Nodes annots = safDoc.query("/saf/annot[slot[@name='ontIDs'][contains(.,'" + ontID + "')]]");
			for(int i=0;i<annots.size();i++) {
				Element e = (Element)annots.get(i);
				System.out.println(e.toXML());
				String id = e.getAttributeValue("id");
				Nodes rels = relDoc.query("/relations/relation[item/@itemid='" + id + "']");
				for(int j=0;j<rels.size();j++) {
					hasRel = true;
					System.out.println("\t" + rels.get(j).toXML());
					Element rel = (Element)rels.get(j);
					//Document doc = XSLTransform.toDocument(xslt.transform(new Document(new Element(rel))));
					//System.out.println(doc.getRootElement().toXML());
				}
			}
			if(hasRel) totalTime += System.currentTimeMillis() - time;
		}
		for(String relStr : relStrs) {
			System.out.println(relStr);
		}
		System.out.println(totalTime);
	}

}
