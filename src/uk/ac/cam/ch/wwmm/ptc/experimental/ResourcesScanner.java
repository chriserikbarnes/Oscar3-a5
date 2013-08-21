package uk.ac.cam.ch.wwmm.ptc.experimental;

import java.util.HashSet;
import java.util.Set;

import nu.xom.Attribute;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Serializer;
import uk.ac.cam.ch.wwmm.ptclib.io.ResourceGetter;

public class ResourcesScanner {

	Document doc;
	Element root;
	Set<String> fns;
	
	void recursePlace(String place) throws Exception {
		ResourceGetter rg = new ResourceGetter(place);
		//System.out.println("IM IN UR " + place);
		for(String s : rg.getStrings("")) {
			//System.out.println(s);
			if(s.matches("[a-z0-9]+")) {
				if(place.length() > 0) {
					recursePlace(place + s + "/");
				} else {
					recursePlace(s + "/");
				}
			}
			if(place.endsWith("resources/")) {
				Element e = new Element("file");
				e.addAttribute(new Attribute("file", s));
				e.addAttribute(new Attribute("place", place));
				root.appendChild(e);
				if(fns.contains(s)) System.out.println("Conflict: " + s);
				fns.add(s);
			}
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		ResourcesScanner rs = new ResourcesScanner();
		rs.root = new Element("filePlaces");
		rs.doc = new Document(rs.root);
		rs.fns = new HashSet<String>();
		rs.recursePlace("");
		Serializer ser = new Serializer(System.out);
		ser.setIndent(2);
		ser.write(rs.doc);
	}

}
