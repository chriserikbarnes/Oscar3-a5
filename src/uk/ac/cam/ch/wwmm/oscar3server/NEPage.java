package uk.ac.cam.ch.wwmm.oscar3server;

import java.net.URLEncoder;
import java.util.Collection;

import nu.xom.Attribute;
import nu.xom.Element;
import uk.ac.cam.ch.wwmm.oscar3.chemnamedict.ChemNameDictSingleton;
import uk.ac.cam.ch.wwmm.oscar3.terms.OBOOntology;
import uk.ac.cam.ch.wwmm.ptclib.scixml.SciXMLDocument;

/** Generates a SciXML document giving details about a named entity.
 * 
 * @author ptc24
 *
 */
public final class NEPage {

	/**Makes an extended SciXML page giving information about a named entity.
	 * 
	 * @param name The named entity name.
	 * @param type The named entity type.
	 * @param smiles The SMILES for the named entity, or null.
	 * @param inchi The InChI for the named entity, or null.
	 * @param ontIDs Ontology identifiers for the named entity, as a
	 * space-separated list, or null.
	 * @param cutDown Whether the page is to be generated as a page for the
	 * full web server (false) or the cut-down version (true).
	 * @return The XML document thus produced.
	 * @throws Exception
	 */
	public static SciXMLDocument makeNEPage(String name, String type, String smiles, String inchi, String ontIDs, boolean cutDown) throws Exception {
		SciXMLDocument doc = new SciXMLDocument();
		
		Element ne = new Element("ne");
		ne.appendChild(name);
		ne.addAttribute(new Attribute("type", type));
		if(smiles != null) ne.addAttribute(new Attribute("SMILES", smiles));
		if(inchi != null) ne.addAttribute(new Attribute("InChI", inchi));
		doc.setTitle(ne);
		Element list = doc.addList();
					
		list.appendChild(doc.makeListItem("Name: " + name));
		list.appendChild(doc.makeListItem("Type: " + type));
		if(smiles != null) list.appendChild(doc.makeListItem("SMILES: " + smiles));
		if(inchi != null) list.appendChild(doc.makeListItem("InChI: " + inchi));
		
		if(inchi != null) { 
			Collection<String> synonyms = ChemNameDictSingleton.getNamesFromInChI(inchi);
			if(synonyms != null) {
				Element li = doc.makeListItem("Synonyms:");
				li.appendChild(doc.makeList(synonyms));
				list.appendChild(li);
			}
		}
		
		String scrubbedName = name;
		if(scrubbedName.endsWith("s")) scrubbedName = scrubbedName.substring(0, scrubbedName.length()-1);
		scrubbedName = scrubbedName.replaceAll("\u00ad", "");
		String urlName = URLEncoder.encode(scrubbedName, "UTF-8");
		
		list.appendChild(doc.makeListItem(doc.makeLink("http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?CMD=search&amp;db=pccompound&amp;term=" + 
		    URLEncoder.encode("\"" + scrubbedName + "\"[CSYN]", "UTF-8"), "Search PubChem for " + scrubbedName)));
		//list.appendChild(doc.makeListItem(doc.makeLink("PubChem?name=" + urlName, "Fetch structures for " + scrubbedName + " from PubChem")));
		if(!cutDown) list.appendChild(doc.makeListItem(doc.makeLink("Search?query=" + urlName + 
				"&type=word&resultsType=snippets", "Search local by name"))); 
		if(!cutDown) list.appendChild(doc.makeListItem(doc.makeLink("/ChemNameDict?action=stop&word=" + urlName, "Add " + scrubbedName + " as stopword")));

		if(inchi != null) {
			String urlInchi = URLEncoder.encode(inchi, "UTF-8");
			list.appendChild(doc.makeListItem(doc.makeLink("http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?CMD=search&amp;db=pccompound&amp;term=" + 
				    URLEncoder.encode("\"" + inchi + "\"[InChI]", "UTF-8"), "Search PubChem by InChI")));				
			if(!cutDown) list.appendChild(doc.makeListItem(doc.makeLink("Search?query=" + urlInchi + 
					"&type=inchi&resultsType=snippets", "Search local by InChI"))); 
			if(!cutDown) list.appendChild(doc.makeListItem(doc.makeLink("Search?query=" + urlInchi + 
					"&type=inchi&resultsType=compoundsList", "Search local for co-occuring compounds"))); 
		}
		
		if(ontIDs != null) {
			String [] oo = ontIDs.split("\\s+");
			for(int i=0;i<oo.length;i++) {
				String ontID = oo[i];
				String urlOntID = URLEncoder.encode(ontID, "UTF-8");
				list.appendChild(doc.makeListItem("Ontology ID: " + ontID));
				String def = OBOOntology.getInstance().getDefinitionForID(ontID);
				if(def != null) {
					list.appendChild(doc.makeListItem(def));
				}
				
				if (ontID.startsWith("PID")) {
					
					String pid = ontID.substring(4, ontID.length());
					String lastDigit = pid.substring(pid.length()-1, pid.length());
					String penultimateDigit = pid.substring(pid.length()-2, pid.length()-1);
					
					list.appendChild(doc.makeListItem(doc.makeLink("http://wwmm.ch.cam.ac.uk/polymers/polyinfo/" +
							penultimateDigit + "/" + lastDigit + "/" + pid + "/" + pid + ".html", "Get polymer data for " + ontID)));
					
				}
				
				
				else {

					list.appendChild(doc.makeListItem(doc.makeLink("http://www.ebi.ac.uk/ontology-lookup/browse.do?termId=" +
						urlOntID, "Look up " + ontID + " using EBI Ontology Lookup Service")));
				}
			}
		}
		
		//list.appendChild(doc.makeListItem(doc.makeLink("http://localhost:8181/foo.jsp", "foo")));
		
		return doc;
	}
	
}
