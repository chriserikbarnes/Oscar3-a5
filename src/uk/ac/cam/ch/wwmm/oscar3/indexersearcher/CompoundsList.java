package uk.ac.cam.ch.wwmm.oscar3.indexersearcher;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nu.xom.Attribute;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Nodes;
import uk.ac.cam.ch.wwmm.oscar3.Oscar3Props;
import uk.ac.cam.ch.wwmm.ptclib.scixml.SciXMLDocument;
import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;
import uk.ac.cam.ch.wwmm.ptclib.xml.XOMTools;

/** Creates a list of compounds based on a set of hits from Lucene, and produces
 * a SciXML+Oscar results page. 
 * 
 * @author ptc24
 *
 */
final class CompoundsList {
	
	Map<String, Integer> counts;
	
	class CLComparator implements Comparator<String> {
		public int compare(String s1, String s2) {
			return counts.get(s1).compareTo(counts.get(s2));
		}
	}
	
	static SciXMLDocument makeCompundsList(Collection<File> files) throws Exception {
		return new CompoundsList().makeCompoundsListInternal(files, null, null, null);
	}	

	static SciXMLDocument makeCompoundsList(Collection<File> files, Collection<String> filterInchis, Collection<String> filterWords, Collection<String> filterOntIds) throws Exception {
		return new CompoundsList().makeCompoundsListInternal(files, filterInchis, filterWords, filterOntIds);
	}	
	
	private SciXMLDocument makeCompoundsListInternal(Collection<File> files, Collection<String> filterInchis, Collection<String> filterWords, Collection<String> filterOntIds) throws Exception {		
		String xp = "//ne";
		
		counts = new HashMap<String, Integer>();
		
		Map<String, Map<String,Integer>> countsPerInChI = new HashMap<String, Map<String,Integer>>();
		
		Map<String, Element> elemTable = new HashMap<String, Element>();
		
		if(filterOntIds != null) {
			if(Oscar3Props.getInstance().verbose) System.out.println(filterOntIds);
		}
		
		for(File f : files) {
			Document d = new Builder().build(f);
			Nodes n = d.query(xp);
			for(int i=0;i<n.size();i++) {
				Element e = (Element)n.get(i);
				Attribute a = e.getAttribute("cmlRef");
				if(a != null) e.removeAttribute(a);
				String s;
				if(e.getAttribute("InChI") != null) {
					s = e.getAttributeValue("InChI");
					String o = e.getAttributeValue("ontIDs");
					if(filterInchis != null || filterOntIds != null) {
						boolean accept = false;
						if(filterInchis != null && filterInchis.contains(s)) {
							accept = true;
						} else if (filterOntIds != null && o != null) {
							//System.out.println("Trying: " + o);
							String [] onts = o.split("\\s+");
							for(int j=0;j<onts.length;j++) {
								if(filterOntIds.contains(onts[j])) {
									if(Oscar3Props.getInstance().verbose) System.out.println("Found: " + onts[j]);
									accept = true;
									break;
								}
							}
						}
						if(accept == false) continue;
					}
					
					if(!countsPerInChI.containsKey(s)) countsPerInChI.put(s, new HashMap<String,Integer>());
					Map<String, Integer> subCounts = countsPerInChI.get(s);
					String name = StringTools.normaliseName(e.getValue());
					if(subCounts.containsKey(name)) {
						subCounts.put(name, subCounts.get(name)+1);
					} else {
						subCounts.put(name, 1);
						elemTable.put(s + " " + name, (Element)XOMTools.safeCopy(e));
					}
				} else if(filterOntIds != null && e.getAttribute("ontIDs") != null) { 
					String o = e.getAttributeValue("ontIDs");
					s = null;
					boolean accept = false;
					if(filterInchis != null && filterInchis.contains(s)) {
						accept = true;
					} else if (filterOntIds != null && o != null) {
						//System.out.println("Trying: " + o);
						String [] onts = o.split("\\s+");
						for(int j=0;j<onts.length;j++) {
							if(filterOntIds.contains(onts[j])) {
								s = onts[j];
								if(Oscar3Props.getInstance().verbose) System.out.println("Found: " + onts[j]);
								accept = true;
								break;
							}
						}
					}
					if(accept == false) continue;
					if(!countsPerInChI.containsKey(s)) countsPerInChI.put(s, new HashMap<String,Integer>());
					Map<String, Integer> subCounts = countsPerInChI.get(s);
					String name = StringTools.normaliseName(e.getValue());
					if(subCounts.containsKey(name)) {
						subCounts.put(name, subCounts.get(name)+1);
					} else {
						subCounts.put(name, 1);
						elemTable.put(s + " " + name, (Element)XOMTools.safeCopy(e));
					}

				} else {
					s = StringTools.normaliseName(e.getValue());
					if(filterWords != null && !filterWords.contains(s)) continue;
				}
				if(counts.containsKey(s)) {
					counts.put(s, counts.get(s)+1);
				} else {
					counts.put(s, 1);
					elemTable.put(s, (Element)XOMTools.safeCopy(e));
				}
			}
		}
		
		List<String> keys = new ArrayList<String>();		
		keys.addAll(counts.keySet());
		
		Collections.sort(keys, Collections.reverseOrder(new CLComparator()));
		
		SciXMLDocument doc = new SciXMLDocument();
		
		Element list = doc.addList();
		
		for(String k : keys) {
			//System.out.printf("%s: %d\n", k, counts.get(k));
			Element li = doc.makeListItem();
			list.appendChild(li);
			if(countsPerInChI.containsKey(k)) {
				String s = "";
				int tot = 0;
				for(String kk : countsPerInChI.get(k).keySet()) {
					li.appendChild(elemTable.get(k + " " + kk));
					li.appendChild(" " + countsPerInChI.get(k).get(kk).toString() + " ");
					tot += countsPerInChI.get(k).get(kk);
				}
				if(countsPerInChI.get(k).size() > 1) {
					s = s + "Total: " + Integer.toString(tot);
				}
				li.appendChild(s);
			} else {
				li.appendChild(elemTable.get(k));
				li.appendChild(" " + counts.get(k).toString());				
			}
		}
		
		return doc;				
	}
}
