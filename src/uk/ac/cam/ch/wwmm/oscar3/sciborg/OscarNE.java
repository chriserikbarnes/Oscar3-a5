package uk.ac.cam.ch.wwmm.oscar3.sciborg;

import java.util.Map;

public class OscarNE implements Comparable<OscarNE> {
	int cfrom;
	int cto;
	int id;
	String safId;
	Map<String,String> content;
	
	public int compareTo(OscarNE o) {
		if(cfrom != o.cfrom) return cfrom - o.cfrom;
		return cto - o.cto;
	}
	
	public boolean overlapsWith(OscarNE o) {
		return cfrom <= o.cto && cto >= o.cfrom; 
	}
	
	public int getId() {
		return id;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	public String getSafId() {
		return safId;
	}
	
	public void setSafId(String safId) {
		this.safId = safId;
	}
	
	@Override
	public String toString() {
		return String.format("ne:[%d->%d, %s]", cfrom, cto, content.toString());
	}
}