package uk.ac.cam.ch.wwmm.oscar3.misc;

import java.util.Map;
import java.util.regex.Pattern;

import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Nodes;
import uk.ac.cam.ch.wwmm.oscar3.terms.TermMaps;
import uk.ac.cam.ch.wwmm.ptclib.saf.SafTools;

/**Attempts to assign chemicals as specifics/generics/fragments.
 * Currently a bit of a stub, and also obsolete. Kept for possible backwards
 * compatibility reasons.
 * 
 * @author ptc24
 *
 */
public final class StructureTypes {

	private static Pattern genericPattern = Pattern.compile("[Aa](cyl|lk(yl|an|en|yn))|hal(o|ide)");
	
	public static void doStructureTypes(Document safDoc) {
		Map<String, String> structureTypes;
		try {
			structureTypes = TermMaps.getStructureTypes();
		} catch (Exception e) {
			return;
		}
		Nodes chemNodes = safDoc.query("/saf/annot[slot[@name='type'][text()='" + NETypes.COMPOUND + "']]");
		for(int i=0;i<chemNodes.size();i++) {
			Element e = (Element)chemNodes.get(i);
			String value = SafTools.getSlotValue(e, "surface");
			//System.out.println(value);
			String st = null;
			if(structureTypes.containsKey(value)) {
				st = structureTypes.get(value);
			} else if(value.endsWith("s") && structureTypes.containsKey(value.substring(0, value.length()-1))) {
				st = structureTypes.get(value.substring(0, value.length()-1));				
			} else if(genericPattern.matcher(value).find()) {
				st = "generic";
			}
			if(st != null) {
				SafTools.setSlot(e, "structureType", st);
			}
			
			
		}
	}
		
}
