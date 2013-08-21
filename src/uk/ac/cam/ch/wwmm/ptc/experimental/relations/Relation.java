package uk.ac.cam.ch.wwmm.ptc.experimental.relations;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import nu.xom.Attribute;
import nu.xom.Element;

public class Relation {

	String type;
	Map<String,List<LatticeCell>> dict;
	Map<String,List<String>> oldDict;
	Map<String,List<Element>> entityDict;
	String sentence;
	File file;
	List<String> pattern;
	
	public Relation(String type, Map<String,List<LatticeCell>> dict, String sentence, File evidence, List<String> pattern) {
		this.type = type;
		this.dict = dict;
		//this.entityDict = entityDict;
		this.sentence = sentence;
		this.file = evidence;
		this.pattern = pattern;
	}
	
	public String getType() {
		return type;
	}
	
	public Map<String,List<LatticeCell>> getDict() {
		return dict;
	}
	
	public String getSentence() {
		return sentence;
	}
	
	public File getFile() {
		return file;
	}
	
	public Map<String,List<Element>> getEntityDict() {
		return entityDict;
	}
	
	public List<String> getPattern() {
		return pattern;
	}
	
	@Override
	public String toString() {
		return "[" + type + ":" + dict + "]";
	}
	
	public Element toXML() {
		Element elem = new Element("relation");
		elem.addAttribute(new Attribute("type", type));
		elem.addAttribute(new Attribute("pattern", pattern.toString()));
		for(String role : dict.keySet()) {
			List<LatticeCell> cells = dict.get(role);
			for(LatticeCell cell : cells) {
				Element item = new Element("item");
				item.addAttribute(new Attribute("role", role));
				item.appendChild(cell.getValue());
				if(cell.getNeElem() != null && cell.getNeElem().getAttribute("id") != null) item.addAttribute(new Attribute("itemid", cell.getNeElem().getAttributeValue("id")));
				elem.appendChild(item);
			}
		}
		return elem;
	}
	
}
