package uk.ac.cam.ch.wwmm.oscar3.misc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nu.xom.Attribute;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Nodes;
import uk.ac.cam.ch.wwmm.ptclib.saf.SafTools;
import uk.ac.cam.ch.wwmm.ptclib.xml.StandoffTable;
import uk.ac.cam.ch.wwmm.ptclib.xml.XMLSpanTagger;
import uk.ac.cam.ch.wwmm.ptclib.xml.XOMTools;

/** Analyses the results of the GeniaRunner class, assigning POS tags to words and suchlike.
 * 
 * @author ptc24
 *
 */
public final class GeniaProcessor {

	/**Analyses the results of the GeniaRunner class, assigning POS tags to named entities.
	 * 
	 * @param sourceXML The source document.
	 * @param geniaSafDoc The GENIA SAF document, produced by NewGeniaRunner.
	 * @param oscarSafDoc The Oscar3 SAF document.
	 * @throws Exception
	 */
	public static void processGeniaAndOscarSafs(Document sourceXML, Document geniaSafDoc, Document oscarSafDoc) throws Exception {
		XMLSpanTagger.tagUpDocument(sourceXML.getRootElement(), "a");
		StandoffTable st = new StandoffTable(sourceXML.getRootElement());
		
		String chunkStart = null;
		String chunkType = null;
		String neStart = null;
		String neType = null;
		String lastEnd = null;
		String docStr = sourceXML.getValue();
		
		List<Element> chunks = new ArrayList<Element>();
		List<Element> nes = new ArrayList<Element>();
		
		Map<String, Element> toToElem = new HashMap<String, Element>();
		
		for(int i=0;i<geniaSafDoc.getRootElement().getChildCount();i++) {
			Element e = (Element)geniaSafDoc.getRootElement().getChild(i);
			if(e.getAttributeValue("type").equals("sentence")) continue;
			toToElem.put(e.getAttributeValue("to"), e);
			String chunk = e.query("slot[@name='chunk']").get(0).getValue();
			String ne = e.query("slot[@name='geniane']").get(0).getValue();
			if(chunk.startsWith("B")) {
				if(chunkStart != null) {
					chunks.add(SafTools.makeAnnot(chunkStart, lastEnd, "chunk", chunkType, docStr.substring(st.getOffsetAtXPoint(chunkStart), st.getOffsetAtXPoint(lastEnd))));
					chunkStart = null;
				}
				chunkStart = e.getAttributeValue("from");
				chunkType = chunk.substring(2);
			} else if(chunk.startsWith("O")) {
				if(chunkStart != null) {
					chunks.add(SafTools.makeAnnot(chunkStart, lastEnd, "chunk", chunkType, docStr.substring(st.getOffsetAtXPoint(chunkStart), st.getOffsetAtXPoint(lastEnd))));
					chunkStart = null;
				}				
			}
			if(ne.startsWith("B")) {
				if(neStart != null) {
					nes.add(SafTools.makeAnnot(neStart, lastEnd, "ne", neType, docStr.substring(st.getOffsetAtXPoint(neStart), st.getOffsetAtXPoint(lastEnd))));
					neStart = null;
				}
				neStart = e.getAttributeValue("from");
				neType = ne.substring(2);
				
				
			} else if(ne.startsWith("O")) {
				if(neStart != null) {
					nes.add(SafTools.makeAnnot(neStart, lastEnd, "ne", neType, docStr.substring(st.getOffsetAtXPoint(neStart), st.getOffsetAtXPoint(lastEnd))));
					neStart = null;
				}				
			}
			
			lastEnd = e.getAttributeValue("to");
		}
		if(chunkStart != null) {
			chunks.add(SafTools.makeAnnot(chunkStart, lastEnd, "chunk", chunkType, docStr.substring(st.getOffsetAtXPoint(chunkStart), st.getOffsetAtXPoint(lastEnd))));
			chunkStart = null;
		}
		if(neStart != null) {
			nes.add(SafTools.makeAnnot(neStart, lastEnd, "ne", neType, docStr.substring(st.getOffsetAtXPoint(neStart), st.getOffsetAtXPoint(lastEnd))));
			neStart = null;
		}

		for(Element e : chunks) geniaSafDoc.getRootElement().appendChild(e);
		for(Element e : nes) geniaSafDoc.getRootElement().appendChild(e);
				
		if(oscarSafDoc == null) return;
		
		
		for(int i=0;i<oscarSafDoc.getRootElement().getChildCount();i++) {
			Element e = (Element)oscarSafDoc.getRootElement().getChild(i);

			String neEnd = e.getAttributeValue("to");
			//Nodes n = geniaSafDoc.query("/saf/annot[@type='genia'][@to='" + neEnd + "']");
			if(toToElem.containsKey(neEnd)) {
				Element ee = toToElem.get(neEnd);
				String tag = SafTools.getSlotValue(ee, "tag");
				//String tag = ee.query("slot[@name='tag']").get(0).getValue();

				SafTools.setSlot(e, "tag", tag);
				/*Element tagSlot = new Element("slot");
				tagSlot.addAttribute(new Attribute("name", "tag"));
				tagSlot.appendChild(tag);
				e.appendChild(tagSlot);*/
			}
		}	
	}
	
	/**Finds short (fewer than three character) named entities at the start of
	 * sentences, and removes them. For example, this removes sentence-initial
	 * "In", "As", "Be" etc.
	 * 
	 * @param oscarSaf The Oscar3 SAF document.
	 * @param geniaSaf The GENIA SAF document, produced by NewGeniaRunner.
	 */
	public static void removeElementsAtSentenceStart(Document oscarSaf, Document geniaSaf) {
		// Look for starts of sentences to remove elements (In, As etc).
		Nodes sentenceNodes = geniaSaf.query("/saf/annot[@type='sentence']");
		for(int i=0;i<sentenceNodes.size();i++) {
			String from = ((Element)sentenceNodes.get(i)).getAttributeValue("from");
			Nodes outNodes = oscarSaf.query("/saf/annot[@from='" + from + "']");
			for(int j=0;j<outNodes.size();j++) {
				String val = SafTools.getSlotValue((Element)outNodes.get(j), "surface");
				//Nodes nn = outNodes.get(j).query("slot[@name='surface']");
				//if(nn.size() > 0) {
				//	String val = nn.get(0).getValue();
				//System.out.println(val);
				if(val != null && val.length() < 3) outNodes.get(j).detach();					
				//}
			}
		}
	}
	
	/**Detects elements of type CM that have been tagged as verbs, and re-types
	 * them to RN.
	 * 
	 * @param oscarSaf The Oscar3 SAF document.
	 * @param geniaSaf The GENIA SAF document, produced by NewGeniaRunner.
	 */
	public static void adjustNETypeUsingPartOfSpeech(Document oscarSaf, Document geniaSaf) {	
		Nodes neNodes = oscarSaf.query("/saf/annot[@type='oscar']");
		for(int i=0;i<neNodes.size();i++) {
			Element e = (Element)neNodes.get(i);
			String type = SafTools.getSlotValue(e, "type");
			//String type = e.query("slot[@name='type']").get(0).getValue();
			//Nodes tagNodes = e.query("slot[@name='tag']");
			String tag = SafTools.getSlotValue(e, "tag");
			if(tag != null) {
			//if(tagNodes.size() > 0) {
			//	String tag = tagNodes.get(0).getValue();
				//System.out.println(tag);
				if(type.startsWith("CM") && tag.startsWith("VB")) {
					SafTools.setSlot(e, "type", NETypes.REACTION);
					//Element typeSlot = (Element)(e.query("slot[@name='type']").get(0));
					//typeSlot.getChild(0).detach();
					//typeSlot.appendChild("RN");
				}
			}
		}
	}

	/**Imports the named entites recognised by the Genia tagger into the
	 * Oscar3 SAF document.
	 * 
	 * @param oscarSaf The Oscar3 SAF document.
	 * @param geniaSaf The GENIA SAF document, produced by NewGeniaRunner.
	 */
	public static void importGeniaNEs(Document oscarSaf, Document geniaSaf) {	
		Nodes geniaNeNodes = geniaSaf.query("/saf/annot[@type='ne']");
		for(int i=0;i<geniaNeNodes.size();i++) {
			Element e = (Element)XOMTools.safeCopy(geniaNeNodes.get(i));
			e.addAttribute(new Attribute("type", "oscar"));
			oscarSaf.getRootElement().appendChild(e);
		}
		
		//Collections.b
	}

}
