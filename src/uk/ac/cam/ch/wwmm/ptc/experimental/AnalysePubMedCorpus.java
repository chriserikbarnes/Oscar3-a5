package uk.ac.cam.ch.wwmm.ptc.experimental;

import java.io.File;
import java.util.List;

import nu.xom.Builder;
import nu.xom.Document;
import uk.ac.cam.ch.wwmm.ptclib.datastruct.Bag;
import uk.ac.cam.ch.wwmm.ptclib.io.FileTools;

public class AnalysePubMedCorpus {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		//sbFiles.addAll();

		List<File> files = FileTools.getFilesFromDirectoryByName(new File("/home/ptc24/tmp/pubmedscrap"), "scrapbook.xml");
		
		
		for(File f : files) {
			Document doc = new Builder().build(f);
			int cmc = doc.query("//ne[@type='CM']").size();
			int rnc = doc.query("//ne[@type='RN']").size();
			int cjc = doc.query("//ne[@type='CJ']").size();
			int asec = doc.query("//ne[@type='ASE']").size();
			System.out.println(cmc + "\t" + rnc + "\t" + cjc + "\t" + asec);
		}
		
	}

}
