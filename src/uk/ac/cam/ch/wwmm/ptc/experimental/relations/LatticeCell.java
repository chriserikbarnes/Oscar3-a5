package uk.ac.cam.ch.wwmm.ptc.experimental.relations;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import nu.xom.Element;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.Token;
import uk.ac.cam.ch.wwmm.oscar3.terms.OBOOntology;
import uk.ac.cam.ch.wwmm.ptclib.saf.SafTools;
import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;

public class LatticeCell {

	Token token;
	Element neElem;
	Set<LatticeCell> next;
	Set<LatticeCell> prev;
	Set<LatticeCell> inheritFrom;
	LinkedHashSet<String> reps;
	Token endToken;
	boolean haveInheritance;
	boolean isEndNP;
	
	public static LatticeCell endNPCell() {
		LatticeCell cell = new LatticeCell();
		cell.next = new HashSet<LatticeCell>();
		cell.prev = new HashSet<LatticeCell>();
		cell.inheritFrom = new HashSet<LatticeCell>();
		cell.reps = null;
		cell.haveInheritance = true;
		cell.isEndNP = true;
		return cell;
	}
	
	private LatticeCell() {
		
	}
	
	public LatticeCell(Token token) {
		this.token = token;
		this.neElem = null;
		this.next = new HashSet<LatticeCell>();
		this.prev = new HashSet<LatticeCell>();
		this.inheritFrom = new HashSet<LatticeCell>();
		reps = null;
		haveInheritance = false;
		isEndNP = false;
	}
	
	public LatticeCell(Element neElem, LatticeCell endTokenCell) {
		this.neElem = neElem;
		this.token = null;
		this.prev = new HashSet<LatticeCell>();
		this.next = endTokenCell.next;
		this.endToken = endTokenCell.token;
		reps = null;
		haveInheritance = true;
		isEndNP = false;
	}
	
	public void addNext(LatticeCell nextCell) {
		next.add(nextCell);
	}
	
	public void addInheritance(LatticeCell inheritance) {
		inheritFrom.add(inheritance);
	}
	
	public LinkedHashSet<String> getReps(DFARelationFinder finder) {
		if(reps != null) return reps;
		reps = new LinkedHashSet<String>();
		if(isEndNP) {
			reps.add("$endNP");
		} else if(token != null) {
			reps.add(token.getValue());
			reps.add(StringTools.normaliseName(token.getValue()));
			if(token.getGeniaData() != null) {
				String [] geniaData = token.getGeniaData();
				reps.add("$pos=" + geniaData[2]);
			}
			if(neElem != null) System.out.println("FOOOOOOOOOOO!");
		} else {
			String neType = SafTools.getSlotValue(neElem, "type");
			reps.add("$" + neType);

			String subType = SafTools.getSlotValue(neElem, "subtype");
			if(subType != null) {
				reps.add("$" + neType + ":" + subType);
			}
			
			String ontIDs = SafTools.getSlotValue(neElem, "ontIDs");
			if(ontIDs != null) {
				for(String ontID : StringTools.arrayToList(ontIDs.split("\\s+"))) {
					Set<String> ids = OBOOntology.getInstance().getIdsForIdWithAncestors(ontID);
					if(ids != null) {
						for(String id : ids) {
							reps.add("$ONT=" + id);
							String name = OBOOntology.getInstance().getNameForID(id);
							//System.out.println("\t" + "$ONT=" + id.split(":")[0] + ":\"" + name.replaceAll("\\s+", "_") + "\"");
							reps.add("$ONT=" + id.split(":")[0] + ":\"" + name.replaceAll("\\s+", "_") + "\"");
							if(OBOOntology.getInstance().isCMType(id)) reps.add("$CMTYPE");
						}
					}
				}
			}
			if(endToken.getGeniaData() != null) {
				String [] geniaData = endToken.getGeniaData();
				String posPrefix = "$pos=" + geniaData[2] + ":";
				for(String rep : new ArrayList<String>(reps)) {
					reps.add(posPrefix + rep);
					//System.out.println("\t" + SafTools.getSlotValue(neElem, "surface") + "\t" + posPrefix + rep);
				}
			}
			
		}
		return reps;
	}
	
	public boolean hasNext() {
		return next.size() > 0;
	}
	
	public Set<LatticeCell> getNext() {
		return next;
	}
	
	public String getValue() {
		if(isEndNP) {
			return "endNP";
		} else if (token != null) {
			return token.getValue();
		} else {
			return SafTools.getSlotValue(neElem, "surface");
		}
	}
	
	public Element getNeElem() {
		return neElem;
	}
	
	public Set<LatticeCell> recieveInheritance() {
		if(haveInheritance) {
			return next;
		} else {
			for(LatticeCell cell : inheritFrom) {
				next.addAll(cell.recieveInheritance());
			}
			haveInheritance = true;
			return next;
		}
	}
	
	public void nextToPrev() {
		for(LatticeCell cell : next) {
			cell.prev.add(this);
		}
	}
	
	@Override
	public String toString() {
		return getValue();
	}
}
