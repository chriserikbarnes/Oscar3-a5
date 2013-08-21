/**
 * 
 */
package uk.ac.cam.ch.wwmm.oscar3.sciborg;

public class Sentence implements Comparable<OscarNE> {
	String id;
	int cfrom;
	int cto;
	String content;
	int resultDbId;
	
	public int compareTo(OscarNE o) {
		int result;
		if(cfrom <= o.cfrom) {
			if(cto >= o.cto) {
				result = 0;
			} else {
				result = -1;
			}
		} else {
			result = 1;
		}
		//System.out.println(result + "\t" + this);
		return result;
	}
	
	@Override
	public String toString() {
		return "[" + id + ", " + cfrom + "->" + cto + ", " + content + "]";
	}
}