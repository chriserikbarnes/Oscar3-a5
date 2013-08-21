package uk.ac.cam.ch.wwmm.ptc.experimental;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import nu.xom.Attribute;
import nu.xom.Element;
import nu.xom.Nodes;
import nu.xom.Serializer;
import uk.ac.cam.ch.wwmm.ptclib.datastruct.Bag;
import uk.ac.cam.ch.wwmm.ptclib.io.FileTools;
import uk.ac.cam.ch.wwmm.ptclib.scixml.SciXMLDocument;
import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;
import uk.ac.cam.ch.wwmm.ptclib.xml.XMLInserter;
import uk.ac.cam.ch.wwmm.ptclib.xml.XOMTools;

public class BioCreative {

	static class Gene {
		int start;
		int end;
		String contents;
		
		@Override
		public String toString() {
			return start + " " + end + " " + contents;
		}
	}
	
	public static int adjustForWhitespace(String s, int pos, boolean atStart) {
		int newpos = pos;
		int posWithoutSpaces = s.substring(0,newpos).replaceAll("\\s+", "").length();
		while(posWithoutSpaces < pos) {
			newpos += (pos - posWithoutSpaces);
			posWithoutSpaces = s.substring(0,newpos).replaceAll("\\s+", "").length();
		}
		while(atStart && newpos < s.length() && s.substring(newpos, newpos+1).matches("\\s|/")) newpos++;
		return newpos;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		List<String> strings = FileTools.getStrings(new FileInputStream(new File("/local/scratch/ptc24/biocreative2/bc2geneMention/train/train.in")), false);
		List<String> idList = new ArrayList<String>();
		Map<String,String> sentencesById = new HashMap<String,String>();
		for(String s : strings) {
			int idpos = s.indexOf(" ");
			String id = s.substring(0, idpos);
			String sentence = s.substring(idpos+1);
			idList.add(id);
			sentencesById.put(id, sentence);
		}
		
		Random r = new Random("biocreative".hashCode());
		Collections.shuffle(idList, r);
		
		List<String> genes = FileTools.getStrings(new FileInputStream(new File("/local/scratch/ptc24/biocreative2/bc2geneMention/train/GENE.eval")), false);
		Map<String,List<Gene>> genesById = new HashMap<String,List<Gene>>();
		for(String gene : genes) {
			String [] geneInfo = gene.split("\\|");
			String [] startEnd = geneInfo[1].split(" ");
			String id = geneInfo[0];
			String sentence = sentencesById.get(id);
			List<Gene> geneList = genesById.get(id);
			if(geneList == null) {
				geneList = new ArrayList<Gene>();
				genesById.put(id, geneList);
			}
			Gene g = new Gene();
			//System.out.println(id + " " + sentence);
			g.start = adjustForWhitespace(sentence, Integer.parseInt(startEnd[0]), true);
			g.end = adjustForWhitespace(sentence, Integer.parseInt(startEnd[1]) + 1, false);
			g.contents = geneInfo[2];
			geneList.add(g);
		}
		List<Element> sentenceElems = new ArrayList<Element>();
		Bag<Integer> geneLocs = new Bag<Integer>();
		for(int i=0;i<idList.size();i++) {
			String id = idList.get(i);
			if(genesById.containsKey(id)) {
				for(Gene g : genesById.get(id)) {
					geneLocs.add(i / 100);
					if(!sentencesById.get(id).substring(g.start, g.end).equals(g.contents)) {
						System.out.println(id);
						System.out.println(sentencesById.get(id));
						System.out.println(g);
						System.out.println("ERROR: " + sentencesById.get(id).substring(g.start, g.end));						
					}
				}
			}
			Element p = new Element("P");
			p.addAttribute(new Attribute("ID", id));
			p.appendChild(sentencesById.get(id));
			if(genesById.containsKey(id)) {
				XMLInserter xi = new XMLInserter(p, "a", "b");
				for(Gene g : genesById.get(id)) {
					Element ne = new Element("ne");
					ne.addAttribute(new Attribute("type", "GENE"));
					ne.appendChild(g.contents);
					xi.insertElement(ne, g.start, g.end);
				}
				xi.deTagDocument();
			}
			sentenceElems.add(p);
			//System.out.println(p.toXML());
		}
		
		for(int i=0;i<150;i++) {
			System.out.println(i + "\t" + StringTools.multiplyString("*", geneLocs.getCount(i)/10));
		}
		
		File sxDir = new File("/local/scratch/ptc24/biocreative2/bc2sciXML");
		if(true) {
			for(int i=0;i<150;i++) {
				List<Element> batch = sentenceElems.subList(i*100, (i+1)*100);
				SciXMLDocument sxd = new SciXMLDocument();
				Element div = sxd.getDiv();
				for(Element p : batch) {
					div.appendChild(p);
					div.appendChild("\n");
				}
				File outDir = new File(sxDir, i + "");
				outDir.mkdir();
				File outFile = new File(outDir, "markedup.xml");
				new Serializer(new FileOutputStream(outFile)).write(sxd);
				Nodes n = sxd.query("//ne");
				for(int j=0;j<n.size();j++) {
					XOMTools.removeElementPreservingText((Element)n.get(j));
				}
				outFile = new File(outDir, "source.xml");
				new Serializer(new FileOutputStream(outFile)).write(sxd);
			}			
		}
		
	}

}
