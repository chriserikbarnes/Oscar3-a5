package uk.ac.cam.ch.wwmm.ptc.experimental;

import java.io.File;
import java.io.FileOutputStream;

import nu.xom.Element;
import nu.xom.Serializer;
import uk.ac.cam.ch.wwmm.oscar3.terms.OBOOntology;
import uk.ac.cam.ch.wwmm.oscar3.terms.OntologyTerm;
import uk.ac.cam.ch.wwmm.ptclib.scixml.SciXMLDocument;

public class GOToSciXML {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		OBOOntology ontology = new OBOOntology();
		ontology.read(new File("/home/ptc24/gene_ontology.obo"));
		System.out.println(ontology.getTerms().size());
		int termID = 0;
		SciXMLDocument sxd = new SciXMLDocument();
		sxd.setTitle("GO terms");
		int reacts = 0;
		for(String id : ontology.getTerms().keySet()) {
			OntologyTerm term = ontology.getTerms().get(id);
			//SciXMLDocument sxd = new SciXMLDocument();
			//sxd.setTitle(term.getName());
			sxd.getNewDiv(term.getId());
			sxd.addPara().appendChild(term.getName());
			
			if(term.getDef() != null) {
				sxd.addPara().appendChild(term.getDef());
				if(term.getDef().matches(".*[Rr]eaction.*")) reacts++;
			}
			termID++;
			if(termID % 100 == 0) {
				System.out.println(termID);
				//Serializer ser = new Serializer(new FileOutputStream(new File("/home/ptc24/tmp/goscixml", "g" + termID + ".xml")));
				//ser.write(sxd);
				sxd = new SciXMLDocument();
				sxd.setTitle("GO terms");
			}
		}
		System.out.println(termID);
		//Serializer ser = new Serializer(new FileOutputStream(new File("/home/ptc24/tmp/goscixml", "g" + termID + ".xml")));
		//ser.write(sxd);
		System.out.println(reacts + "reactions");
	}
	
}
