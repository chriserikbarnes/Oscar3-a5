package uk.ac.cam.ch.wwmm.ptc.experimental;

import java.io.File;

import uk.ac.cam.ch.wwmm.ptclib.xml.XMLInserter;
import uk.ac.cam.ch.wwmm.ptclib.xml.XMLSpanTagger;
import uk.ac.cam.ch.wwmm.ptclib.xml.XOMTools;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Nodes;

public class BioCreativeMerge {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		Document doc1 = new Builder().build(new File("/home/ptc24/oscarworkspace/scrapbook/0/markedup.xml"));
		Document doc2 = new Builder().build(new File("/home/ptc24/oscarworkspace/corpora/bc2sciXML/0/markedup.xml"));
		Nodes p1n = doc1.query("//P");
		Nodes p2n = doc2.query("//P");
		for(int i=0;i<p1n.size();i++) {
			Element p1 = (Element)p1n.get(i);
			Element p2 = (Element)p2n.get(i);
			XMLInserter xi = new XMLInserter(p1, "a", "b");
			XMLSpanTagger.tagUpDocument(p2, "b");
			xi.incorporateElementsFromRetaggedDocument(p2, "b");
			xi.deTagDocument();
			System.out.println(p1.toXML());

			Nodes en = p1.query("ne/ne");
			for(int j=0;j<en.size();j++) {
				XOMTools.removeElementPreservingText((Element)en.get(j));
			}
			System.out.println(p1.toXML());

		}
	}

}
