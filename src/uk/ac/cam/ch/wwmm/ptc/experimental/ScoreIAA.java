package uk.ac.cam.ch.wwmm.ptc.experimental;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Nodes;
import uk.ac.cam.ch.wwmm.oscar3server.scrapbook.ScoreStats;
import uk.ac.cam.ch.wwmm.ptclib.io.FileTools;
import uk.ac.cam.ch.wwmm.ptclib.xml.XOMTools;

public class ScoreIAA {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		//FIXME
		
		File myDir = new File("/home/ptc24/cleanOscar/oscar3-chem/scrapbookfold1_pruned");
		File crbDir = new File("/home/ptc24/cleanOscar/oscar3-chem/from_crb_pruned");
		
		//File myDir = new File("/home/ptc24/cleanOscar/oscar3-chem/scrapbookfold1_pruned");
		//File crbDir = new File("/home/ptc24/cleanOscar/oscar3-chem/from_dj");

		//File myDir = new File("/home/ptc24/cleanOscar/oscar3-chem/from_dj");
		//File crbDir = new File("/home/ptc24/cleanOscar/oscar3-chem/from_crb_pruned");

		List<File> files = FileTools.getFilesFromDirectoryByName(crbDir, "scrapbook.xml");
		ScoreStats grandTotal = new ScoreStats();
		String filterType = null;
		for(File f : files) {
			String subDir = f.getParentFile().getName();
			File myFile = new File(new File(myDir, subDir), "scrapbook.xml");
			
			ScoreStats ss = new ScoreStats();
			Document myDoc = new Builder().build(myFile);
			Document crbDoc = new Builder().build(f);
			
			Nodes ids = myDoc.query("//snippet/@id");
			Set<String> idStrs = new LinkedHashSet<String>();
			for(int i=0;i<ids.size();i++) idStrs.add(ids.get(i).getValue());

			int chars = 0;
			
			//System.out.println(f.getParentFile().getName());
			for(String id : idStrs) {
				try {
				//System.out.println(id);
				Element elem1 = (Element)myDoc.query("//snippet[@id='" + id + "']").get(0);
				chars += elem1.getValue().length();
				Element elem2 = (Element)crbDoc.query("//snippet[@id='" + id + "']").get(0);

				Nodes n = elem1.query(".//ne");
				for(int i=0;i<n.size();i++) {
					Element ne = (Element)n.get(i);
					if(filterType != null && !ne.getAttributeValue("type").equals(filterType)) {
						XOMTools.removeElementPreservingText(ne);
					}
				}
				n = elem2.query(".//ne");
				for(int i=0;i<n.size();i++) {
					Element ne = (Element)n.get(i);
					if(filterType != null && !ne.getAttributeValue("type").equals(filterType)) {
						XOMTools.removeElementPreservingText(ne);
					}
				}
				
				//FIXME - replace this with something that doesn't violate visibility
				//ScoreStats mySs = SnippetCompare.getPrecisionAndRecall(elem2, elem1, false);
				//ss.addScoreStats(mySs);
				
				
				} catch (Exception e) {
					System.out.println("Discrepancy!");
				}
			}
			//System.out.println("\t" + chars);
			System.out.println(f.getParentFile().getName() + "\t" + ss.getPrecAndRecallString());
			grandTotal.addScoreStats(ss);
		}
		
		System.out.println("Grand total:\t" + grandTotal.getPrecAndRecallString());

	}

}
