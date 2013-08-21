package uk.ac.cam.ch.wwmm.oscar3.indexersearcher;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import nu.xom.Attribute;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Node;
import nu.xom.Nodes;
import nu.xom.Serializer;
import nu.xom.Text;

import org.apache.lucene.index.TermVectorOffsetInfo;

import uk.ac.cam.ch.wwmm.oscar3.sciborg.ResultDbReader;
import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;
import uk.ac.cam.ch.wwmm.ptclib.xml.StandoffTable;
import uk.ac.cam.ch.wwmm.ptclib.xml.XMLInserter;
import uk.ac.cam.ch.wwmm.ptclib.xml.XOMTools;

/** Produces "snippets" for a document, with search terms highlighted.
 * 
 * @author ptc24
 *
 */
final class SnippetGetter {
	
	/** Compares to snippets based on their start positions.
	 * 
	 * @author ptc24
	 *
	 */
	static class SnippetComparator implements Comparator<Element> {
		public int compare(Element e1, Element e2) {
			int s1 = Integer.parseInt(e1.getAttributeValue("start"));
			int s2 = Integer.parseInt(e2.getAttributeValue("start"));
			if(s1 > s2) return 1;
			if(s1 == s2) return 0;
			return -1;
		}
	}
	
	static Element getSearchResult(Collection<String> inchis, List<TermVectorOffsetInfo> offsets, Collection<String> ontIDs, Document doc, String href, int docId, UserQuery lq, File parentFile) {
		double start; double end; double seconds;
		start = System.currentTimeMillis();

		int paperId = -1;
		try {
			paperId = ResultDbReader.getInstance().getPaperId(parentFile);
			System.out.println("paper ID: " + paperId);			
		} catch (Exception e) {
			paperId = -1;
		}
		
		//System.out.println(inchis);
		
		//if(strings == null) {
		//	strings = new ArrayList<String>();
		//	strings.add("ocean");
		//}
		Nodes cmlNodes = doc.query("//cmlPile");
		for(int i=0;i<cmlNodes.size();i++) cmlNodes.get(i).detach();

		XMLInserter.tagUpDocument(doc.getRootElement(), "a");
		
		List<Element> snippets = getNESnippets(inchis, ontIDs, doc);
		snippets.addAll(getWordSnippets(offsets, doc));
		
		Collections.sort(snippets, new SnippetComparator());
		snippets = discardOverlappingElements(snippets);
		snippets = mergeElements(snippets, doc);
		
		Element div = new Element("DIV");
		Element title = doc.getRootElement().getFirstChildElement("TITLE");
		if(title == null) title = doc.getRootElement().getFirstChildElement("CURRENT_TITLE");
		Element header = new Element("HEADER");
		div.appendChild(header);
		if(title != null) {
			header.appendChild(title.getValue());
		} else {
			header.appendChild("No title");
		}
		if(href != null) header.addAttribute(new Attribute("href", href));		
		for(Element e : snippets) {
			div.appendChild(e);
		}
		
		if(paperId != -1) {
			long pmt = System.currentTimeMillis();
			//Map<String,Integer> pointmap = ResultDbReader.getInstance().getPointMap(paperId);
			//System.out.println("Pointmap in " + (System.currentTimeMillis() - pmt));
			//for(String s : StringTools.getSortedList(pointmap)) {
			//	System.out.println(s + "\t" + pointmap.get(s));
			//}
			Nodes nn = div.query("//ne/@id");
			List<String> ids = new ArrayList<String>();
			for(int i=0;i<nn.size();i++) {
				ids.add(nn.get(i).getValue());
			}
			Document rdoc = ResultDbReader.getInstance().getNes(ids, paperId);
			
			try {
				Serializer ser = new Serializer(System.out);
				ser.setIndent(2);
				ser.write(rdoc);
				
				Document sdoc = (Document)XOMTools.safeCopy(doc);
				Nodes nes = sdoc.query("//ne|//datasection");
				for(int i=0;i<nes.size();i++) {
					XOMTools.removeElementPreservingText((Element)nes.get(i));
				}
				
				StandoffTable st = new StandoffTable(sdoc.getRootElement());
				
				if(offsets != null && offsets.size() > 0) {
					//String paperStr = doc.getValue();
					List<Integer> startOffsets = new ArrayList<Integer>();
					List<Integer> endOffsets = new ArrayList<Integer>();
					List<Integer> docIds = new ArrayList<Integer>();
					for(TermVectorOffsetInfo tvoi : offsets) {
						int textStart = tvoi.getStartOffset();
						int textEnd = tvoi.getEndOffset();
						startOffsets.add(textStart);
						endOffsets.add(textEnd);
						//System.out.println(st.getLeftPointAtOffset(textStart));
						//System.out.println(pointmap.get(st.getLeftPointAtOffset(textStart)));
						//int xmlStart = pointmap.get(st.getLeftPointAtOffset(textStart));
						//int xmlEnd = pointmap.get(st.getRightPointAtOffset(textEnd));
						//System.out.println(xmlStart + "\t" + xmlEnd + "\t");
						//startOffsets.add(xmlStart);
						//endOffsets.add(xmlEnd);
						docIds.add(paperId);
					}
					if(startOffsets.size() > 0) {
						rdoc = ResultDbReader.getInstance().getSentences(startOffsets, endOffsets, docIds);
						ser = new Serializer(System.out);
						ser.setIndent(2);
						ser.write(rdoc);						
					}
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		/*Element p = new Element("P");
		Element a = new Element("a");
		a.addAttribute(new Attribute("href", lq.getQueryURL(0, 5, true, docId)));
		a.appendChild("More like this");
		p.appendChild(a);
		div.appendChild(p);*/
		
		end = System.currentTimeMillis();
		seconds = (end - start) / 1000.0;
		System.out.printf("Page made in %f seconds.\n", seconds);
		
		return div;
	}
	
	private static List<Element> getWordSnippets(List<TermVectorOffsetInfo> offsets, Document doc) {
		List<Element> snippets = new ArrayList<Element>();

		if(offsets != null && offsets.size() > 0) {
			String paperStr = doc.getValue();
			for(TermVectorOffsetInfo tvoi : offsets) {
				int start = tvoi.getStartOffset();
				int end = tvoi.getEndOffset();
					String before = paperStr.substring(Math.max(0, start - 150), start);
					String after = paperStr.substring(end, 
							Math.min(end + 150, paperStr.length()));
					Element e = new Element("P");
					e.addAttribute(new Attribute("preStart", Integer.toString(Math.max(0, start - 150))));
					e.addAttribute(new Attribute("start", Integer.toString(start)));
					e.addAttribute(new Attribute("end", Integer.toString(end)));
					e.addAttribute(new Attribute("postEnd", Integer.toString(Math.min(end + 150, paperStr.length()))));
					e.appendChild(before);
					Element ee = new Element("B");
					ee.appendChild(paperStr.substring(start, end));
					e.appendChild(ee);
					e.appendChild(after);
					snippets.add(e);
			}
		}

		return snippets;
	}
	
	private static List<Element> getNESnippets(Collection<String> inchis, Collection<String> ontIDs, Document doc) {
		List<Element> snippets = new ArrayList<Element>();
		if(inchis == null) return snippets;
		Nodes chemNodes = doc.query("//ne[@InChI]|//ne[@ontIDs]");
		for(int i=0;i<chemNodes.size();i++) {
			Element e = (Element)chemNodes.get(i);
			String inchi = e.getAttributeValue("InChI");
			if(inchi != null && inchis.contains(inchi)) {
				snippets.add(getElementInContext(e));
			} else {
				String o = e.getAttributeValue("ontIDs");
				if(o != null) {
					String [] oo = o.split(" ");
					for(int j=0;j<oo.length;j++) {
						if(ontIDs.contains(oo[j])) {
							snippets.add(getElementInContext(e));							
						}
					}
				}
			}
		}
		return snippets;
	}
	
	private static Element getElementInContext(Element elem) {
		Element context = (Element)elem.getParent();
		int index = context.indexOf(elem);
		if(context.getLocalName().equals("formula")) {
			index = context.getParent().indexOf(context);
			context = (Element)context.getParent();
		}
		StringBuffer previous = new StringBuffer();
		for(int i=0;i<index;i++) {
			previous.append(context.getChild(i).getValue());
		}
		StringBuffer next = new StringBuffer();
		for(int i=index+1;i<context.getChildCount();i++) {
			next.append(context.getChild(i).getValue());
		}
		Element newContext = new Element("P");
		
		String prev = previous.toString();
		if(prev.length() > 150) prev = prev.substring(prev.length() - 150);
		//prev = prev.replaceAll("\\s+", " ");
		String nxt = next.toString();
		if(nxt.length() > 150) nxt = nxt.substring(0, 150);	
		//nxt = nxt.replaceAll("\\s+", " ");

		int start = Integer.parseInt(elem.getAttributeValue("xtspanstart"));
		int end = Integer.parseInt(elem.getAttributeValue("xtspanend"));
		
		newContext.addAttribute(new Attribute("preStart", Integer.toString(start-prev.length())));
		newContext.addAttribute(new Attribute("start", Integer.toString(start)));
		newContext.addAttribute(new Attribute("end", Integer.toString(end)));
		newContext.addAttribute(new Attribute("postEnd", Integer.toString(end+nxt.length())));
		
		newContext.appendChild(prev);
		newContext.appendChild(new Element(elem));
		newContext.appendChild(nxt.toString());
		return newContext;
	}
	
	private static List<Element> discardOverlappingElements(List<Element> elements) {
		List<Element> newElems = new LinkedList<Element>();
		for(Element e : elements) {
			if(newElems.size() == 0) {
				newElems.add(e);
			} else {
				//System.out.println("Comparing: ");
				//System.out.println(e.toXML());
				Element lastElem = newElems.get(newElems.size()-1);
				//System.out.println(lastElem.toXML());
				//System.out.println();
				int lastStart = Integer.parseInt(lastElem.getAttributeValue("start"));
				int lastEnd = Integer.parseInt(lastElem.getAttributeValue("end"));
				int thisStart = Integer.parseInt(e.getAttributeValue("start"));
				int thisEnd = Integer.parseInt(e.getAttributeValue("end"));
				
				if(lastEnd > thisStart) {
					/* We have overlap, or nesting at any rate */
					if(e.query("ne").size() > 0) {
						/* NEs get priority over words */
						newElems.remove(newElems.size()-1);
						newElems.add(e);
					} else if(lastElem.query("ne").size() > 0) {
						/* Do nothing, don't put e in */
					} else if(thisStart - thisEnd - (lastStart - lastEnd) > 0) {
						/* This one's larger, give it priority */
						newElems.remove(newElems.size()-1);
						newElems.add(e);						
					} else {
						/* Do nothing, don't put e in */						
					}

				} else {
					/* No overlap problem */
					newElems.add(e);
				}
			}			
		}
		return newElems;
	}
	
	private static List<Element> mergeElements(List<Element> elements, Document sourceDoc) {
		List<Element> newElems = new LinkedList<Element>();
		String docTxt = sourceDoc.getValue();
		
		for(Element e : elements) {
			if(newElems.size() == 0) {
				newElems.add(e);
			} else if(Integer.parseInt(e.getAttributeValue("preStart")) >
				Integer.parseInt(newElems.get(newElems.size()-1).getAttributeValue("postEnd"))) {
				newElems.add(e);
			} else {
				Element lastElem = newElems.get(newElems.size()-1);
				Node n = lastElem.getChild(lastElem.getChildCount()-1);
				assert(n instanceof Text);
				n.detach();
				n = e.getChild(0);
				assert(n instanceof Text);
				n.detach();
				lastElem.appendChild(docTxt.substring(Integer.parseInt(lastElem.getAttributeValue("end")),
						Integer.parseInt(e.getAttributeValue("start"))));
				while(e.getChildCount() > 0) {
					n = e.getChild(0);
					n.detach();
					lastElem.appendChild(n);
				}
				lastElem.addAttribute(new Attribute("end", e.getAttributeValue("end")));
				lastElem.addAttribute(new Attribute("postEnd", e.getAttributeValue("postEnd")));
			}
		}
		
		return newElems;
	}

}
