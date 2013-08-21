package uk.ac.cam.ch.wwmm.oscar3.pcsql;

import java.util.LinkedHashSet;
import java.util.Set;

/**Extracts and formats some details about a compound in a PubChem mirror.
 * 
 * @author ptc24
 * @deprecated PubChemSQL functionality has been replaced by NewPubChem.
 *
 */
public class DetailsForCid {
	public int cid;
	public String smiles;
	public String inchi;
	public Set<String> names;
	
	public DetailsForCid(int cid) {
		this.cid = cid;
		names = new LinkedHashSet<String>();
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(cid + "\n");
		sb.append(smiles + "\n");
		sb.append(inchi + "\n");
		for(String name : names) {
			sb.append(name + "\n");
		}
		return sb.toString();
	}
	
	public void setSmiles(String smiles) {
		this.smiles = smiles;
	}
	
	public void setInchi(String inchi) {
		this.inchi = inchi;
	}
	
	public void addName(String name) {
		names.add(name);
	}
}
