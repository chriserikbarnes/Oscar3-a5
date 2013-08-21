package uk.ac.cam.ch.wwmm.ptc.experimental;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Nodes;
import nu.xom.Serializer;

import org.openscience.cdk.Molecule;
import org.openscience.cdk.interfaces.IMolecule;

import uk.ac.cam.ch.wwmm.oscar3server.NEPage;
import uk.ac.cam.ch.wwmm.ptclib.cdk.ConverterToInChI;
import uk.ac.cam.ch.wwmm.ptclib.cdk.Molecule2Png;
import uk.ac.cam.ch.wwmm.ptclib.cdk.StructureConverter;
import uk.ac.cam.ch.wwmm.ptclib.io.FileTools;

public class MakeImages {

	public static void makeImages(File paperDir) throws Exception {
		Transformer trans = TransformerFactory.newInstance().newTransformer(new StreamSource(new File("/home/ptc24/eclipseworkspace/Oscar3/src/uk/ac/cam/ch/wwmm/oscar3server/resources/sharedweb/toHTMLJS.xsl"))); 
		trans.setParameter("viewer", "file");
		Document doc = new Builder().build(new File(paperDir, "markedup.xml"));
		Nodes n = doc.query("//ne");
		for(int i=0;i<n.size();i++) {
			Element e = (Element)n.get(i);
			if(e.getAttribute("id") == null || e.getAttribute("InChI") == null) continue;
			String inchi = e.getAttributeValue("InChI");
			String id = e.getAttributeValue("id");
			String smiles = e.getAttributeValue("SMILES");
			String ontIDs = e.getAttributeValue("ontIDs");
			String name = e.getValue();
			String type = e.getAttributeValue("type");
			Document neDoc = NEPage.makeNEPage(name, type, smiles, inchi, ontIDs, true);
			try {
				File tmpFile = new File("tmp.xml");
				new Serializer(new FileOutputStream(tmpFile)).write(neDoc);
				trans.transform(new StreamSource(tmpFile), new StreamResult(new File(paperDir, id+".html")));
			} catch (Exception ee) {
				ee.printStackTrace();
			}
			try {
				IMolecule mol = ConverterToInChI.getMolFromInChI(inchi);
				StructureConverter.configureMolecule(mol);
				Molecule2Png m2p = new Molecule2Png();
				m2p.fixedWidthAndHeight = true;
				m2p.height = 300;
				m2p.width = 300;
				m2p.renderMolecule(mol, new File(paperDir, id+".png").toString());
			} catch (Exception ee) {
				
			}
		}
		try {
			Molecule blank = new Molecule();
			Molecule2Png m2p = new Molecule2Png();
			m2p.fixedWidthAndHeight = true;
			m2p.height = 300;
			m2p.width = 300;
			m2p.renderMolecule(blank, new File(paperDir, "blank.png").toString());			
		} catch (Exception ee) {
			
		}
		trans.transform(new StreamSource(new File(paperDir, "markedup.xml")), new StreamResult(new File(paperDir, "markedup.html")));
		System.out.println(paperDir);
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		List<File> files = FileTools.getFilesFromDirectoryByName(new File("/home/ptc24/newows/corpora/smallGrapefruit"), "markedup.xml");
		files.addAll(FileTools.getFilesFromDirectoryByName(new File("/home/ptc24/newows/corpora/active_corpus/rsc"), "markedup.xml"));
		for(File f : files) {
			File dir = f.getParentFile();
			makeImages(dir);
		}
		
		
	}

}
