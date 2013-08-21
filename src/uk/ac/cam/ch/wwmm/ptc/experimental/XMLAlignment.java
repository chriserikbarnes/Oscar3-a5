package uk.ac.cam.ch.wwmm.ptc.experimental;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nu.xom.Attribute;
import nu.xom.Builder;
import nu.xom.Comment;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Node;
import nu.xom.Nodes;
import nu.xom.ProcessingInstruction;
import nu.xom.Serializer;
import nu.xom.Text;
import nu.xom.xslt.XSLTransform;
import uk.ac.cam.ch.wwmm.ptclib.io.FileTools;
import uk.ac.cam.ch.wwmm.ptclib.scixml.PubXMLToSciXML;
import uk.ac.cam.ch.wwmm.ptclib.scixml.SAFToInline;
import uk.ac.cam.ch.wwmm.ptclib.scixml.XMLStrings;
import uk.ac.cam.ch.wwmm.ptclib.xml.StandoffTable;
import uk.ac.cam.ch.wwmm.ptclib.xml.XMLSpanTagger;
import uk.ac.cam.ch.wwmm.ptclib.xml.XOMTools;

public class XMLAlignment {

	Document xml1;
	Document xml2;
	Map<String,List<Element>> xmlMap1;
	Map<String,List<Element>> xmlMap2;
	
	Map<Element,Element> elemToElem;
	List<Integer> charPosToCharPos;
	StandoffTable st1;
	StandoffTable st2;
	
	long startTime;
	boolean verbose = false;
	
	public XMLAlignment(Document xml1, Document xml2, boolean useNodeIds) throws Exception {
		this.xml1 = xml1;
		this.xml2 = xml2;
		startTime = System.currentTimeMillis();
		if(verbose) System.out.println("Started");
		if(useNodeIds) {
			makeElemToElemMapByNodeId();
		} else {
			makeElemToElemMapByText();
		}
		if(verbose) System.out.println("Making charmap at " + (System.currentTimeMillis() - startTime));
		makeCharToCharMap();
		if(verbose) System.out.println("Ready at " + (System.currentTimeMillis() - startTime));
	}
	
	public void printAligned() {
		Set<String> valSet = new LinkedHashSet<String>();
		for(String val : xmlMap1.keySet()) valSet.add(val);
		for(String val : xmlMap2.keySet()) valSet.add(val);
		for(String val : valSet) {
			if(!xmlMap1.containsKey(val) || !xmlMap2.containsKey(val) || 
					xmlMap1.get(val).size() > 1 ||
					xmlMap2.get(val).size() > 1) continue;
			String fp1 = makeFingerPrint(xmlMap1.get(val).get(0));
			String fp2 = makeFingerPrint(xmlMap2.get(val).get(0));
			if(fp1.equals(fp2)) continue;
			String tag2 = xmlMap2.get(val).get(0).getLocalName();
			if("PLACE".equals(tag2) || 
					"DL".equals(tag2) || 
					"NAME".equals(tag2) || 
					"SURNAME".equals(tag2) || 
					"AUTHOR".equals(tag2) || 
					"TGROUP".equals(tag2) || 
					"TABLE".equals(tag2) || 
					"REFERENCE".equals(tag2) || 
					"REFERENCELIST".equals(tag2) || 
					"PAGES".equals(tag2) || 
					"DIV".equals(tag2) || 
					"EQN".equals(tag2) || 
					"FIGURE".equals(tag2)) continue;
			
			System.out.println("Document 1");
			if(xmlMap1.containsKey(val)) {
				for(Element e : xmlMap1.get(val)) {
					System.out.println(e.toXML());
					String fp = makeFingerPrint(e);
					System.out.println(fp.hashCode() + "\t" + fp);
				}
			}
			System.out.println("Document 2");
			if(xmlMap2.containsKey(val)) {
				for(Element e : xmlMap2.get(val)) {
					System.out.println(e.toXML());
					String fp = makeFingerPrint(e);
					System.out.println(fp.hashCode() + "\t" + fp);
				}
			}
			System.out.println("====================================================");
		}
	}
	
	private Map<String,List<Element>> makeMap(Document doc) {
		Map<String,List<Element>> map = new HashMap<String,List<Element>>();
		addElementToMap(doc.getRootElement(), map);
		return map;
	}
	
	private void addElementToMap(Element e, Map<String,List<Element>> map) {
		String val = e.getValue();
		if(val != null && val.length() > 0) {
			if(!map.containsKey(val)) map.put(val, new ArrayList<Element>());
			map.get(val).add(e);
		}
		for(int i=0;i<e.getChildCount();i++) {
			if(e.getChild(i) instanceof Element) {
				addElementToMap((Element)e.getChild(i), map);
			}
		}
	}
	
	private void cleanMaps() {
		Set<String> valSet = new LinkedHashSet<String>();
		for(String val : xmlMap1.keySet()) valSet.add(val);
		for(String val : xmlMap2.keySet()) valSet.add(val);
		for(String val : valSet) {
			if(!xmlMap1.containsKey(val)) {
				xmlMap2.remove(val);
				continue;
			}
			if(!xmlMap2.containsKey(val)) {
				xmlMap1.remove(val);
				continue;
			}
			cleanElementList(xmlMap1.get(val));
			cleanElementList(xmlMap2.get(val));
		}
	}
	
	private void cleanElementList(List<Element> elems) {
		for(int i=elems.size()-2;i>=0;i--) {
			for(int j=0;j<elems.get(i).getChildCount();j++) {
				if(elems.get(i).getChild(j) == elems.get(i+1)) {
					//elems.remove(i+1);
					elems.remove(i);
					//i--;
					break;
				}
			}
		}
	}
	
	public void mappable() {
		Set<Element> mappable = new HashSet<Element>();
		for(String val : xmlMap2.keySet()) {
			List<Element> elems = xmlMap2.get(val);
			if(elems.size() != 1) continue;
			mappable.add(elems.get(0));
		}
		int fullLength = xml1.getValue().length();
		int mappableLength = mappableOfElement(xml2.getRootElement(), mappable);
		System.out.println(fullLength + " " + mappableLength + " " + ((double)mappableLength)/fullLength);
		fullLength = xml2.getValue().length();
		System.out.println(fullLength + " " + mappableLength + " " + ((double)mappableLength)/fullLength);
		
	}
	
	private int mappableOfElement(Element e, Set<Element> mappable) {
		if(mappable.contains(e)) return e.getValue().length();
		int mappableLength = 0;
		for(int i=0;i<e.getChildCount();i++) {
			if(e.getChild(i) instanceof Element) {
				mappableLength += mappableOfElement((Element)e.getChild(i), mappable);
			}
		}
		return mappableLength;
	}
	
	private String makeFingerPrint(Element e) {
		StringBuffer fp = new StringBuffer();
		for(int i=0;i<e.getChildCount();i++) {
			Node n = e.getChild(i);
			if(n instanceof Element) {
				fp.append("E");
			} else if(n instanceof ProcessingInstruction) {
				fp.append("P");
			} else if(n instanceof Comment) {
				fp.append("C");
			} else if(n instanceof Text) {
				fp.append("T(");
				fp.append(n.getValue().length());
				fp.append(")");
			} else {
				fp.append("???");
			}
		}
		return fp.toString();
	}
	
	private void makeElemToElemMapByText() {
		xmlMap1 = makeMap(xml1);
		xmlMap2 = makeMap(xml2);
		cleanMaps();
		elemToElem = new HashMap<Element,Element>();
		Set<String> valSet = new LinkedHashSet<String>();
		for(String val : xmlMap1.keySet()) valSet.add(val);
		for(String val : xmlMap2.keySet()) valSet.add(val);
		for(String val : valSet) {
			if(!xmlMap1.containsKey(val) || !xmlMap2.containsKey(val) || 
					xmlMap1.get(val).size() > 1 ||
					xmlMap2.get(val).size() > 1) continue;
			elemToElem.put(xmlMap1.get(val).get(0), xmlMap2.get(val).get(0));
		}

	}

	private void makeElemToElemMapByNodeId() {
		elemToElem = new HashMap<Element,Element>();
		Map<String,Element> nodeIdToElem1 = new HashMap<String,Element>();
		Map<String,Element> nodeIdToElem2 = new HashMap<String,Element>();
		harvestNodeIDs(xml1.getRootElement(), nodeIdToElem1);
		harvestNodeIDs(xml2.getRootElement(), nodeIdToElem2);
		if(verbose) System.out.println("Harvested Node IDs at " + (System.currentTimeMillis() - startTime));
		for(String id : nodeIdToElem1.keySet()) {
			if(!nodeIdToElem2.containsKey(id)) continue;
			elemToElem.put(nodeIdToElem1.get(id), nodeIdToElem2.get(id));
		}
	}
	
	private void harvestNodeIDs(Element elem, Map<String,Element> map) {
		if(elem.getAttribute("nodeID") != null && elem.getAttributeValue("nodeID").length() > 0) {
			map.put(elem.getAttributeValue("nodeID"), elem);
		}
		for(int i=0;i<elem.getChildCount();i++) {
			if(elem.getChild(i) instanceof Element) {
				harvestNodeIDs((Element)elem.getChild(i), map);
			}
		}
	}
	
	private void makeCharToCharMap() throws Exception {
		int xmlLength = xml1.getValue().length();
		charPosToCharPos = new ArrayList<Integer>(xml1.getValue().length());
		for(int i=0;i<xmlLength;i++) {
			charPosToCharPos.add(-1);
		}
		if(verbose) System.out.println("Initialised charmap at " + (System.currentTimeMillis() - startTime));
		XMLSpanTagger.tagUpDocument(xml1.getRootElement(), "a");
		XMLSpanTagger.tagUpDocument(xml2.getRootElement(), "a");
		st1 = new StandoffTable(xml1.getRootElement());
		st2 = new StandoffTable(xml2.getRootElement());
		if(verbose) System.out.println("Tags and tables at " + (System.currentTimeMillis() - startTime));
		
		List<Element> alignable = new ArrayList<Element>(elemToElem.keySet());
		Collections.sort(alignable, Collections.reverseOrder(new Comparator<Element>() {
			public int compare(Element o1, Element o2) {
				// TODO Auto-generated method stub
				Integer o1l = o1.getValue().length();
				Integer o2l = o2.getValue().length();
				return o1l.compareTo(o2l);
			}
		}));
		if(verbose) System.out.println("Sorted alignable at " + (System.currentTimeMillis() - startTime));


		for(Element elem1 : alignable) {
			Element elem2 = elemToElem.get(elem1);
			int e1start = Integer.parseInt(elem1.getAttributeValue("xtspanstart"));
			int e2start = Integer.parseInt(elem2.getAttributeValue("xtspanstart"));
			String e1v = elem1.getValue();
			String e2v = elem2.getValue();
			if(e1v.equals(e2v)) {
				//System.out.println(e1v + "\t" + e1start + "\t" + e2start);
				for(int i=0;i<e1v.length();i++) {
					charPosToCharPos.set(e1start+i, e2start+i);
				}
			} else {
				if(verbose) System.out.println("******************************************");
				if(verbose) System.out.println("Eeek: couldn't align");
				if(verbose) System.out.println(elem1.toXML());
				if(verbose) System.out.println("with");
				if(verbose) System.out.println(elem2.toXML());
				if(verbose) System.out.println("******************************************");
				List<String> strings1 = stringsForElement(elem1);
				List<String> strings2 = stringsForElement(elem2);
				StringListAlignment sla = new StringListAlignment(strings1, strings2);
				if(sla.isSuccess()) {
					if(verbose) System.out.println("Alignable!");
					List<List<Integer>> ll1 = sla.getAligned1();
					List<List<Integer>> ll2 = sla.getAligned2();
					for(int i=0;i<ll1.size();i++) {
						int start1 = ll1.get(i).get(0);
						int end1 = ll1.get(i).get(ll1.get(i).size()-1);
						int start2 = ll2.get(i).get(0);
						int end2 = ll2.get(i).get(ll2.get(i).size()-1);
						int startPos1 = startOfNode(elem1.getChild(start1));
						int endPos1 = endOfNode(elem1.getChild(end1));
						int startPos2 = startOfNode(elem2.getChild(start2));
						int endPos2 = endOfNode(elem2.getChild(end2));
						
						if(startPos1 != -1 && startPos2 != -1 && endPos1 != -1 && endPos2 != -1 && 
								startPos1 < endPos1 && startPos2 < endPos2 && (endPos1 - startPos1 == endPos2 - startPos2)) {
							//if(verbose) System.out.println("Aligning: " + dv1.substring(startPos1, endPos1));
							for(int j=startPos1;j<endPos1;j++) {
								charPosToCharPos.set(j, j - startPos1 + startPos2);
							}
						} else {
							//if(verbose) System.out.println("Argh: " + startPos1 + " " + endPos1 + " " + startPos2 + " " + endPos2);
						}
					}
				} else {
					if(verbose) System.out.println("Unalignable!");
				}
			}
		}
		
	}
	
	public void displayCharMap() {
		String dv1 = xml1.getValue();
		String dv2 = xml2.getValue();
		
		int oldCp = -50;
		for(int i=0;i<dv1.length();i++) {
			int cp = charPosToCharPos.get(i);
			if(cp != -1 && (cp != oldCp + 1 || oldCp == -1)) {
				System.out.println();
				System.out.print("Align at " + i + " to " + cp + ": ");
			} else if(cp == -1 && oldCp != -1) {
				System.out.println();
				System.out.print("Unalign at " + i + ": ");				
			}
			String s = dv1.substring(i, i+1);
			if(s.matches("\\s")) s = " ";
			System.out.print(s);// + "\t" + charPosToCharPos.get(i));
			if(cp != -1) {
				if(!dv1.substring(i, i+1).equals(dv2.substring(cp, cp+1))) {
					System.out.println();
					System.out.println("Error!");
				}
			}
			oldCp = cp;
		}
		
		//System.out.println(xml1.toXML());		
	}
	
	private List<String> stringsForElement(Element e) {
		List<String> strings = new ArrayList<String>();
		for(int i=0;i<e.getChildCount();i++) {
			Node n = e.getChild(i);
			if(n instanceof Element || n instanceof Text) {
				strings.add(e.getChild(i).getValue());				
			} else {
				strings.add("");								
			}
		}
		return strings;
	}
	
	private int startOfNode(Node n) {
		if(n instanceof Element) {
			Element e = (Element)n;
			return Integer.parseInt(e.getAttributeValue("xtspanstart"));
		}
		int index = n.getParent().indexOf(n);
		if(index == 0) {
			return Integer.parseInt(((Element)n.getParent()).getAttributeValue("xtspanstart"));
		} else {
			Node prev = XOMTools.getPreviousSibling(n);
			if(prev instanceof Element) {
				return Integer.parseInt(((Element)prev).getAttributeValue("xtspanend"));				
			} else if(prev instanceof Text) {
				return startOfNode(prev) + prev.getValue().length();
			} else {
				return startOfNode(prev);
			}
		}
	}

	private int endOfNode(Node n) {
		if(n instanceof Element) {
			Element e = (Element)n;
			return Integer.parseInt(e.getAttributeValue("xtspanend"));
		}
		int index = n.getParent().indexOf(n);
		if(index == n.getParent().getChildCount() - 1) {
			return Integer.parseInt(((Element)n.getParent()).getAttributeValue("xtspanend"));
		} else {
			Node next = XOMTools.getNextSibling(n);
			if(next instanceof Element) {
				return Integer.parseInt(((Element)next).getAttributeValue("xtspanstart"));				
			} else if(next instanceof Text) {
				return endOfNode(next) - next.getValue().length();
			} else {
				return endOfNode(next);
			}
		}
	}
	
	public String [] translateXPoints(String xpStart1, String xpEnd1) {
		try {
			int charPosStart1 = st1.getOffsetAtXPoint(xpStart1);
			int charPosEnd1 = st1.getOffsetAtXPoint(xpEnd1);
		
			int charPosStart2 = charPosToCharPos.get(charPosStart1);
			int charPosEnd2;
			if(charPosStart1 == charPosEnd1) {
				charPosEnd2 = charPosEnd1;
			} else {
				// offsets refer to spaces between characters, whereas charPosToCharPos refers to
				// characters. Therefore we need to look at the final character of the span
				charPosEnd2 = charPosToCharPos.get(charPosEnd1-1)+1;
			}
			// Sanity-check the results
			if(charPosEnd1 - charPosStart1 != charPosEnd2 - charPosStart2) {
				System.out.println("Length doesn't match up!");
				return null;
			}
			String xpStart2 = st2.getLeftPointAtOffset(charPosStart2);
			String xpEnd2 = st2.getRightPointAtOffset(charPosEnd2);
			
			String [] results = new String[2];
			results[0] = xpStart2;
			results[1] = xpEnd2;
			return results;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public Document translateSafDoc(Document safDoc1) {
		Document safDoc2 = (Document)safDoc1.copy();
		Nodes annotNodes = safDoc2.query("//annot");
		for(int i=0;i<annotNodes.size();i++) {
			Element e = (Element)annotNodes.get(i);
			String from = e.getAttributeValue("from");
			String to = e.getAttributeValue("to");
			String [] translated = translateXPoints(from, to);
			if(translated == null) {
				System.out.println("Couldn't translate annot:");
				System.out.println(e.toXML());
				System.out.println();
				e.detach();
			} else {
				e.addAttribute(new Attribute("from", translated[0]));
				e.addAttribute(new Attribute("to", translated[1]));
			}
		}
		
		return safDoc2;
	}
	
	public void testXML1ForOscar() {
		Nodes n = XMLStrings.getInstance().getChemicalPlaces(xml1);
		for(int i=0;i<n.size();i++) {
			Element e = (Element)n.get(i);
			int start = Integer.parseInt(e.getAttributeValue("xtspanstart"));
			int end = Integer.parseInt(e.getAttributeValue("xtspanend"));
			if(start == end) continue;
			for(int j=start; j<end; j++) {
				if(charPosToCharPos.get(j) == -1) {
					System.out.println(e.toXML());
					break;
				}
			}
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		String paperNo = "av1125";
		//File paper = new File("/home/ptc24/corpora/iuc/c030100/" + paperNo + ".xml");
		//Document xsltDoc = new Builder().build(new File("/local/scratch/ptc24/tmp/npg_art2paper_rng.xsl"));
		Document xsltDoc = new Builder().build(new File("/local/scratch/ptc24/base_corpus/rsc/rsc_art2paper_rng.xsl"));
		//Document xsltDoc = new Builder().build(new File("/local/scratch/ptc24/base_corpus/nature/npg_art2paper_rng.xsl"));
		//Document xsltDoc = new Builder().build(new File("/local/scratch/ptc24/tmp_corpus/iuc/iucr_art2paper_rng.xsl"));
		//Document xsltDoc = new Builder().build(new File("/local/scratch/ptc24/base_corpus/iuc/iucr_art2paper_rng.xsl"));
		//Document xsltDoc = new Builder().build(new File("/home/ptc24/iucr_art2paper_rng.xsl"));
		XSLTransform xslt = new XSLTransform(xsltDoc);
		
		//List<File> files = FileTools.getFilesFromDirectoryBySuffix(new File("/usr/groups/sciborg/texts/iuc"), ".xml");
		List<File> files = FileTools.getFilesFromDirectoryBySuffix(new File("/usr/groups/sciborg/texts/rsc/paperset1"), ".xml");
		//List<File> files = FileTools.getFilesFromDirectoryBySuffix(new File("/usr/groups/sciborg/texts/nature/nature_2"), ".xml");
		//List<File> files = FileTools.getFilesFromDirectoryBySuffix(new File("/local/scratch/ptc24/rscorig"), ".xml");
		//files=files.subList(13,14);
		System.out.println(files);
		int i=0;
		for(File paper : files) {

			System.out.println(paper.getName());
			System.out.println(i++);
			try {
			Document xml1;
			Document xml2;
			XMLAlignment xa;
			if(true) {
				xml1 = new Builder().build(paper);
			
				PubXMLToSciXML.addNodeIds(xml1.getRootElement(), xml1.getRootElement());
			
				Nodes n = xslt.transform(xml1);
				xml2 = XSLTransform.toDocument(n);
				xa = new XMLAlignment(xml2, xml1, true);
				xa.testXML1ForOscar();
				//new Serializer(System.out).write(xml1);
				//new Serializer(System.out).write(xml2);
			} else {
				xml1 = new Builder().build(new File("/usr/groups/sciborg/texts/ne_annotated_301106/handAnnot/b110865b/source.xml"));
				xml2 = new Builder().build(new File("/usr/groups/sciborg/texts/ne_annotated_110107/autoAnnot/b110865b/source.xml"));
				xa = new XMLAlignment(xml1, xml2, false);				
			}
			
			xa.displayCharMap();
			System.out.println();
			//new Serializer(System.out).write(xml2);
			if(true) {
				
			} else {
				Document safDoc = new Builder().build(new File("/usr/groups/sciborg/texts/ne_annotated_301106/handAnnot/b110865b/saf.xml"));
				Document transSaf = xa.translateSafDoc(safDoc);
				Document inlineXML = SAFToInline.safToInline(transSaf, xml2, false);
				new Serializer(System.out).write(inlineXML);
				
			}
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("Skipping that paper");
			}
			System.out.println("*****************************************");
		}
	}

}
