package uk.ac.cam.ch.wwmm.ptc.experimental;

import java.io.File;
import java.util.List;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Nodes;
import uk.ac.cam.ch.wwmm.ptclib.datastruct.Bag;
import uk.ac.cam.ch.wwmm.ptclib.io.FileTools;

public class CountNEs {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception  {
		List<File> files = FileTools.getFilesFromDirectoryByName(new File("/home/ptc24/papers/lbm2007/pubmedscrap"), "scrapbook.xml");
		Bag<String> types = new Bag<String>();
		for(File f : files) {
			Document doc = new Builder().build(f);
			Nodes n = doc.query("//ne/@type");
			for(int i=0;i<n.size();i++) {
				String type = n.get(i).getValue();
				types.add(type);
			}
		}
		for(String s : types.getList()) {
			System.out.println(s + "\t" + types.getCount(s));
		}
	}

}
