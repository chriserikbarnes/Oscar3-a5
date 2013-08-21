package uk.ac.cam.ch.wwmm.ptclib.scixml;

import java.io.File;

import uk.ac.cam.ch.wwmm.ptclib.io.FileTools;
import uk.ac.cam.ch.wwmm.ptclib.io.ResourceGetter;
import uk.ac.cam.ch.wwmm.ptclib.string.HtmlCleaner;

import nu.xom.Builder;
import nu.xom.Document;

/** Converts files to SciXML, doing some autodetection.
 * 
 * @author ptc24
 *
 */
public final class ToSciXML {

	private static ResourceGetter rg = new ResourceGetter("uk/ac/cam/ch/wwmm/ptclib/scixml/");
	
	/** Produces a SciXML document from a file. Autodetects according to filname suffix
	 * and docType declaration. Currently recognises .xml (SciXML + PubXML if you have the
	 * stylesheets installed), .html, .htm, .src (for BioIE abstracts), otherwise assumes
	 * plain text.
	 * 
	 * @param f The File to parse.
	 * @return A SciXML document, ready for the recognition of named entities and data in.
	 * @throws Exception
	 */
	public static Document fileToSciXML(File f) throws Exception {
		String name = f.getName();
		if(name.endsWith(".xml")) {
			Document doc = new Builder().build(f);
			/* is this an RSC document? */
			if(PubXMLToSciXML.isRSCDoc(doc)) {
				PubXMLToSciXML rtsx = new PubXMLToSciXML(doc);
				doc = rtsx.getSciXML();
			}
			return doc;
		} else if(name.endsWith(".html") || name.endsWith(".htm")) {
			return TextToSciXML.textToSciXML(HtmlCleaner.cleanHTML(FileTools.readTextFile(f)));
		} else if(name.matches("source_file_\\d+_\\d+.src")) {
			return TextToSciXML.bioIEAbstractToXMLDoc(FileTools.readTextFile(f));
		} else {
			/* Assume plain text */
			return TextToSciXML.textToSciXML(FileTools.readTextFile(f));
		}
	}
	
}
