package uk.ac.cam.ch.wwmm.ptc.experimental;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import uk.ac.cam.ch.wwmm.ptclib.io.FileTools;
import uk.ac.cam.ch.wwmm.ptclib.io.ResourceGetter;

public class ScrapbookToSubstituted {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		ResourceGetter rg = new ResourceGetter("uk/ac/cam/ch/wwmm/ptc/experimental/resources/");
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer t = tf.newTransformer(new StreamSource(rg.getStream("scrapbookToText.xsl")));

		List<File> fl = new ArrayList<File>();
		fl.addAll(FileTools.getFilesFromDirectoryByName(new File("/home/ptc24/cleanOscar/oscar3-chem/scrapbookfold1_pruned/"), "scrapbook.xml"));
		fl.addAll(FileTools.getFilesFromDirectoryByName(new File("/home/ptc24/cleanOscar/oscar3-chem/scrapbookfold2_pruned/"), "scrapbook.xml"));
		fl.addAll(FileTools.getFilesFromDirectoryByName(new File("/home/ptc24/cleanOscar/oscar3-chem/scrapbookfold3_pruned/"), "scrapbook.xml"));
		for(File f : fl) {
			String s = f.getParentFile().getName();
			if("default".equals(s)) continue;
			File out = new File("/home/ptc24/tmp/subs/", s + ".txt");
			t.transform(new StreamSource(new FileInputStream(f)), new StreamResult(out));
		}
		//File f = new File("/home/ptc24/cleanOscar/oscar3-chem/scrapbookfold1_pruned/b303244b/scrapbook.xml/");
		
	}

}
