package uk.ac.cam.ch.wwmm.ptc.experimental;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Node;
import nu.xom.Nodes;
import uk.ac.cam.ch.wwmm.oscar3server.scrapbook.ScrapBook;
import uk.ac.cam.ch.wwmm.ptclib.io.FileTools;
import uk.ac.cam.ch.wwmm.ptclib.scixml.XMLStrings;
import uk.ac.cam.ch.wwmm.ptclib.xml.XOMTools;

public class CompositeDoc {

	public static List<List<Integer>> getFolds(int items, int folds) {
		int foldSize = items / folds;
		int remainder = items % folds;
		List<List<Integer>> foldList = new ArrayList<List<Integer>>();
		List<Integer> fold = new ArrayList<Integer>();
		foldList.add(fold);
		for(int i=0;i<items;i++) {
			if(fold.size() == foldSize+1) {
				remainder--;
				fold = new ArrayList<Integer>();
				foldList.add(fold);
			} else if(remainder == 0 && fold.size() == foldSize) {
				fold = new ArrayList<Integer>();
				foldList.add(fold);
			}
			fold.add(i);
		}
		return foldList;
	}
	
	public static void handleElems(List<Element> elems, String suffix) throws Exception {
		System.out.println("Elems: " + elems.size());
		ScrapBook sb = new ScrapBook("paragraphs" + suffix, true);
		for(Element e : elems) {
			sb.addScrap(e, null);
		}
	}
	
	public static void handleFold(List<File> files, int foldNo) throws Exception {
		System.out.println("Files: " + files.size());

		List<Element> elems = new ArrayList<Element>();
		for(File f : files) {
			String name = f.getParentFile().getName();
			Document doc = new Builder().build(f);
			Nodes n = doc.query("//P|//ABSTRACT");
			
			Set<Node> excludeNodes = new HashSet<Node>();
			Nodes experimentalSections = doc.query(XMLStrings.getInstance().EXPERIMENTAL_SECTION_XPATH, XMLStrings.getInstance().getXpc());
			for(int i=0;i<experimentalSections.size();i++) {				
				Nodes subNodes = experimentalSections.get(i).query(".//P|.//HEADER");
				for(int j=0;j<subNodes.size();j++) excludeNodes.add(subNodes.get(j));
			}

			for(int i=0;i<n.size();i++) {
				Element e = (Element)n.get(i);
				if(excludeNodes.contains(e)) continue;
				if(e.getValue().trim().length() == 0) {
					System.out.println("Skipping");
					continue;
				}
				e.setLocalName("P");
				e.insertChild(name + ":" + i + ": ", 0);
				XOMTools.normalise(e);
				e.detach();
				elems.add(e);
			}
		}
				
		Collections.shuffle(elems);
		
		List<List<Integer>> folds = getFolds(elems.size(), 10);
		for(int i=0;i<10;i++) {
			List<Integer> f = folds.get(i);
			List<Element> elemsInFold = new ArrayList<Element>();
			for(Integer n : f) {
				elemsInFold.add(elems.get(n));
			}
			handleElems(elemsInFold, foldNo + "" + (char)('a' + i));
		}
		
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		List<File> files = FileTools.getFilesFromDirectoryByName(new File("/home/ptc24/oscarworkspace/corpora/paperset1"), "source.xml");

		Collections.shuffle(files);
		
		List<List<Integer>> folds = getFolds(files.size(), 10);
		for(int i=0;i<10;i++) {
			List<Integer> f = folds.get(i);
			List<File> filesInFold = new ArrayList<File>();
			for(Integer n : f) {
				filesInFold.add(files.get(n));
			}
			handleFold(filesInFold, i);
		}
		

	}

}
