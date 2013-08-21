package uk.ac.cam.ch.wwmm.oscar3.misc;

import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Nodes;
import uk.ac.cam.ch.wwmm.oscar3.Oscar3Props;
import uk.ac.cam.ch.wwmm.ptclib.saf.SafTools;
import uk.ac.cam.ch.wwmm.ptclib.xml.XOMTools;

/** Does SciBorg-related post-processing - does basic POS tagging of NEs
 * and creates near-duplicate entries for ambiguous words.
 * 
 * @author ptc24
 *
 */
public final class SciBorgPostProcess {

	/** Does SciBorg-related post-processing - does basic POS tagging of NEs
	 * and creates near-duplicate entries for ambiguous words.
	 * 
	 * @param safDoc The SAF document to postprocess.
	 */
	public static void postProcess(Document safDoc) {
		postProcess(safDoc, true);
	}
	
	/** Does SciBorg-related post-processing - does basic POS tagging of NEs, optionally
	 * creating near-duplicate entries for ambiguous words.
	 * 
	 * @param safDoc The SAF document to postprocess.
	 * @param splitAtes Whether or not to produce two seperate annots for words ending in -ate or -ates.
	 */
	public static void postProcess(Document safDoc, boolean splitAtes) {
		if(Oscar3Props.getInstance().useMEMM) splitAtes = false;
		Nodes neNodes = safDoc.query("/saf/annot");
		for(int i=0;i<neNodes.size();i++) {
			Element e = (Element)neNodes.get(i);
			String type = SafTools.getSlotValue(e, "type");
			//String type = e.query("slot[@name='type']").get(0).getValue();
			if("CM".equals(type)) {
				String surface = SafTools.getSlotValue(e, "surface");
				//String surface = e.query("slot[@name='surface']").get(0).getValue();
				if(surface.endsWith("ates")) {
					if("yes".equals(SafTools.getSlotValue(e, "singular"))) {
						SafTools.setSlot(e, "tag", "NN1");												
					} else {
						SafTools.setSlot(e, "tag", "NN2");						
					}
					if(splitAtes) {
						Element ee = (Element)e.copy();
						XOMTools.insertAfter(e, ee);
						SafTools.setSlot(ee, "type", "RN");
						SafTools.setSlot(ee, "tag", "VVZ");
					}
				} else if(surface.endsWith("ate")) {
					SafTools.setSlot(e, "tag", "NN1");
					if(splitAtes) {
						Element ee = (Element)e.copy();
						XOMTools.insertAfter(e, ee);
						SafTools.setSlot(ee, "type", "RN");
						SafTools.setSlot(ee, "tag", "VVI VVB");
					}
				} else if(surface.endsWith("s")) {
					if("yes".equals(SafTools.getSlotValue(e, "singular"))) {
						SafTools.setSlot(e, "tag", "NN1");												
					} else {
						SafTools.setSlot(e, "tag", "NN2");						
					}
				} else {
					SafTools.setSlot(e, "tag", "NN1");					
				}
			} else if("RN".equals(type)) {
				String surface = SafTools.getSlotValue(e, "surface");
				//String surface = e.query("slot[@name='surface']").get(0).getValue();
				if(surface.endsWith("ates") || surface.endsWith("ifies") || surface.endsWith("ifys") || surface.endsWith("ises") || surface.endsWith("izes")) {
					SafTools.setSlot(e, "tag", "VVZ");					
				} else if(surface.endsWith("ate") || surface.endsWith("ify") || surface.endsWith("ize") || surface.endsWith("ise")) {
					SafTools.setSlot(e, "tag", "VVI VVB");					
				} else if(surface.endsWith("ed")) {
					SafTools.setSlot(e, "tag", "VVD VVN AJ0");					
				} else if(surface.endsWith("ing")) {
					SafTools.setSlot(e, "tag", "VVG NN1 AJ0");					
				} else if(surface.endsWith("tion") || surface.endsWith("ment") || surface.endsWith("lysis")) {
					SafTools.setSlot(e, "tag", "NN1");					
				} else if(surface.endsWith("tions") || surface.endsWith("ments") || surface.endsWith("lyses")) {
					SafTools.setSlot(e, "tag", "NN2");					
				} else if(surface.endsWith("ive") || surface.endsWith("ic")) {
					SafTools.setSlot(e, "tag", "AJ0");					
				} else if(surface.endsWith("ly")) {
					SafTools.setSlot(e, "tag", "AV0");					
				} else {
					SafTools.setSlot(e, "tag", "NN1 NN2 AJ0 AV0 VVB VVD VVN VVI VVZ");
				}
			} else if("CJ".equals(type)) {
				SafTools.setSlot(e, "tag", "AJ0 AV0");					
			} else if("ASE".equals(type)) {
				String surface = SafTools.getSlotValue(e, "surface");
				//String surface = e.query("slot[@name='surface']").get(0).getValue();
				if(surface.endsWith("s")) {
					SafTools.setSlot(e, "tag", "NN2");					
				} else {
					SafTools.setSlot(e, "tag", "NN1");					
				}				
			} else if("CPR".equals(type)) {
				SafTools.setSlot(e, "tag", "AJ0 AV0");									
			} 
		}
	}
	
}
