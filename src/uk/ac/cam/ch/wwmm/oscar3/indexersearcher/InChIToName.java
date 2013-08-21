package uk.ac.cam.ch.wwmm.oscar3.indexersearcher;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Nodes;
import uk.ac.cam.ch.wwmm.ptclib.datastruct.Bag;
import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;

/**Collects InChIs and names from a set of papers, and holds the most common
 * name for each InChI.
 * 
 * @author ptc24
 *
 */

final class InChIToName {

	Map<String,Bag<String>> namesByInChI;
	Map<String,String> inchiToName;
	
	InChIToName() {
		namesByInChI = new HashMap<String,Bag<String>>();
		inchiToName = new HashMap<String,String>();
	}
	
	void analyse(List<File> files) throws Exception {
		for(File f : files) {
			if(f.isFile()) {
				f = f.getParentFile();
			}
			File safFile = new File(f, "saf.xml");
			if(safFile.exists()) {
				Document safDoc = new Builder().build(safFile);
				Nodes n = safDoc.query("/saf/annot[slot[@name='InChI']]");
				for(int i=0;i<n.size();i++) {
					String surf = n.get(i).query("slot[@name='surface']").get(0).getValue();
					String inchi = n.get(i).query("slot[@name='InChI']").get(0).getValue();
					//System.out.println(surf + " -> " + inchi);
					if(!namesByInChI.containsKey(inchi)) namesByInChI.put(inchi, new Bag<String>());
					namesByInChI.get(inchi).add(StringTools.normaliseName(surf));
				}				
			}
		}
		for(String inchi : namesByInChI.keySet()) {
			inchiToName.put(inchi, namesByInChI.get(inchi).mostCommon());
		}
	}
	
	Set<String> getInChIs() {
		return inchiToName.keySet();
	}
	
	String nameForInChI(String inchi) {
		return inchiToName.get(inchi);
	}

	Bag<String> namesForInChI(String inchi) {
		return namesByInChI.get(inchi);
	}
	
	boolean hasInchI(String inchi) {
		return inchiToName.containsKey(inchi);
	}

}
