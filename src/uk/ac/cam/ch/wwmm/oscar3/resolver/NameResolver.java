package uk.ac.cam.ch.wwmm.oscar3.resolver;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import nu.xom.Attribute;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;
import nu.xom.Node;
import nu.xom.Nodes;
import nu.xom.XPathContext;

import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.smiles.SmilesParser;

import uk.ac.cam.ch.wwmm.opsin.NameToStructure;
import uk.ac.cam.ch.wwmm.oscar3.Oscar3Props;
import uk.ac.cam.ch.wwmm.oscar3.chemnamedict.ChemNameDictSingleton;
import uk.ac.cam.ch.wwmm.oscar3.misc.NETypes;
import uk.ac.cam.ch.wwmm.oscar3.newpc.NewPubChem;
import uk.ac.cam.ch.wwmm.oscar3.pcsql.PubChemSQL;
import uk.ac.cam.ch.wwmm.oscar3.resolver.extension.ExtensionNameResolver;
import uk.ac.cam.ch.wwmm.oscar3.resolver.extension.Results;
import uk.ac.cam.ch.wwmm.oscar3.terms.OntologyTerms;
import uk.ac.cam.ch.wwmm.ptclib.cdk.ConverterToInChI;
import uk.ac.cam.ch.wwmm.ptclib.cdk.StereoInChIToMolecule;
import uk.ac.cam.ch.wwmm.ptclib.cdk.StructureConverter;
import uk.ac.cam.ch.wwmm.ptclib.io.ResourceGetter;
import uk.ac.cam.ch.wwmm.ptclib.saf.SafTools;
import uk.ac.cam.ch.wwmm.ptclib.scixml.XMLStrings;
import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;
import uk.ac.cam.ch.wwmm.ptclib.xml.XOMTools;

/** Attaches structures to names in SAF documents.
 * 
 * @author ptc24
 *
 */
public final class NameResolver {

	private class CacheObject {
		String SMILES;
		String InChI;
		Element CML;
		
		CacheObject(String s, String i, Element c) {
			SMILES = s;
			InChI = i;
			CML = c;
		}
	}
	
	private class ProcessState {
		private List<String> args;
		
		private String inchi;
		private String smiles;
		private Element cmlMol;
		private HashMap<String, Element> InChItoCML; 
		private HashMap<String, String> xRefIDtoCmlID;
		private Element ne;
		private Element cmlPile;
	}

	private static SmilesGenerator generator = new SmilesGenerator();
	private static SmilesParser smilesParser = new SmilesParser(DefaultChemObjectBuilder.getInstance());
	private static ResourceGetter rg = new ResourceGetter("uk/ac/cam/ch/wwmm/oscar3/resolver/resources/");
	private Document elementLexiconDoc = rg.getXMLDocument("lexicon.xml");
	private HashMap<String, Element> elementLexiconEntries;
	private HashMap<String, CacheObject> cache;
	private PubChemSQL pcsql;
	private boolean makeCML;
	private ExtensionNameResolver extensionNameResolver;
	
	private static Builder XMLBuilder = new Builder();
	
	private static NameResolver myInstance;
	
	private NameResolver() throws Exception {
		makeCML = Oscar3Props.getInstance().makeCML;
		elementLexiconDoc = rg.getXMLDocument("lexicon.xml");
		elementLexiconEntries = new HashMap<String, Element>();
		cache = new HashMap<String, CacheObject>();
		pcsql = PubChemSQL.getInstance();
		Nodes n = elementLexiconDoc.query("//chem");
		for(int i=0;i<n.size();i++) {
			Element e = (Element)n.get(i);
			elementLexiconEntries.put(e.getAttributeValue("name"), e);
		}
		String enrStr = Oscar3Props.getInstance().extensionNameResolver;
		extensionNameResolver = null;
		if(enrStr != null && !enrStr.toLowerCase().equals("none")) {
			try {
				extensionNameResolver = (ExtensionNameResolver)Class.forName(enrStr).getConstructor(new Class[0]).newInstance(new Object[0]);
			} catch (Exception e) {
				System.err.println("Could not initiate extension name resolver!");
				System.err.println("Details:");
				e.printStackTrace();
			}
		}
	}

	/**Reinitialises the name resolver singleton.
	 * 
	 * @throws Exception
	 */
	public static void reinitialise() throws Exception {
		myInstance = null;
		getInstance();
	}
	
	/**Initialise the name resolver singleton, if this has not already been
	 * done.
	 * 
	 * @throws Exception
	 */
	public static void init() throws Exception {
		if(myInstance == null) myInstance = new NameResolver();
	}
		
	private static NameResolver getInstance() throws Exception {
		if(myInstance == null) myInstance = new NameResolver();
		return myInstance;
	}
	
	/** Resolves names in a document. This should be done after using the name
	 * recogniser.
	 * 
	 * @param doc The document to parse. This will be altered to include the extra information.
     * @param args Arguments from OscarFlow, passed to the extension name resolver.
	 * @throws Exception
	 */
	public static void parseDoc(Document doc, List<String> args) throws Exception {
		getInstance().processDoc(doc, args);
	}
	
	/**Clear the resolver's name cache.
	 * 
	 * @throws Exception
	 */
	public static void purgeCache() throws Exception {
		getInstance().purgeCacheInternal();
	}
	
	/**Gets the ExtensionNameResolver (if any) for the Name Resolver singleton.
	 * 
	 * @return The ExtensionNameResolver, or null.
	 * @throws Exception
	 */
	public static ExtensionNameResolver getExtensionNameResolver() throws Exception {
		return getInstance().extensionNameResolver;
	}
	
	private List<Element> getNEs(Document doc) {
		List<Element> nes = new ArrayList<Element>();
		Nodes annotNodes = doc.query("/saf/annot");
		for(int i=0;i<annotNodes.size();i++) {
			Element annot = (Element)annotNodes.get(i);
			String type = SafTools.getSlotValue(annot, "type");
			if(NETypes.COMPOUND.equals(type)) nes.add(annot);
		}
		return nes;
	}
	
	String getNEName(Element ne) {
		return ne.query("slot[@name=\"surface\"]").get(0).getValue();
	}
	
	void setNEAttribute(Element ne, String attrName, String attrVal) {
		SafTools.setSlot(ne, attrName, attrVal);
	}
	
	private String getNEAttribute(Element ne, String attrName) {
		return SafTools.getSlotValue(ne, attrName);
	}
	
	private void removeNEAttribute(Element ne, String attrName) {
		SafTools.removeSlot(ne, attrName);
	}

	private void purgeCacheInternal() {
		cache = new HashMap<String, CacheObject>();		
	}
	
	private void cacheEntry(String name, String s, String i, Element c) {
		cache.put(name, new CacheObject(s, i, c));
	}

	private boolean resolveVsCache(ProcessState state, String name) {
		if(cache.containsKey(name) || cache.containsKey(name.toLowerCase())) {
			CacheObject co = cache.get(name);
			if(co == null) co = cache.get(name.toLowerCase());
			if(co != null) {
				if(co.SMILES != null) {
					state.smiles = co.SMILES;
					setNEAttribute(state.ne, "SMILES", co.SMILES);
				}
				if(co.InChI != null) {
					state.smiles = co.InChI;
					setNEAttribute(state.ne, "InChI", co.InChI);
					if(makeCML && co.CML != null) {
						if(!state.InChItoCML.containsKey(co.InChI)) {
							state.cmlMol = new Element(co.CML);
							state.cmlPile.appendChild(state.cmlMol);
							state.InChItoCML.put(co.InChI, state.cmlMol);
						}
					}
				}
			} else {
				//Syname.out.println("Skipping " + name);
			}
			return true;
		} else {
			return false;
		}
	}
	
	private boolean resolveVsLexicon(ProcessState state, String name) {
		Element lexEntry = elementLexiconEntries.get(name);
		if(lexEntry == null) lexEntry = elementLexiconEntries.get(name.toLowerCase());
		if(lexEntry != null) {
			if(lexEntry.getAttribute("SMILES") != null) {
				state.smiles = lexEntry.getAttributeValue("SMILES");
				setNEAttribute(state.ne, "SMILES", state.smiles);
				try {
					IMolecule mol = smilesParser.parseSmiles(state.smiles);
					state.inchi = ConverterToInChI.getInChI(mol);
					if(state.inchi != null) {
						setNEAttribute(state.ne, "InChI", state.inchi);
						cacheEntry(name, state.smiles, state.inchi, null);
					} else {
						cacheEntry(name, state.smiles, null, null);
					}
				} catch (Exception e) {
					
				}
			} else if(lexEntry.getAttributeValue("type").equals("element")) {
				setNEAttribute(state.ne, "Element", lexEntry.getAttributeValue("atomicSymbol"));
			}
			return true;
		} else {
			return false;
		}
	}
	
	private boolean resolveVsChemNameDict(ProcessState state, String name) {
		try	{
			if(ChemNameDictSingleton.hasName(name)) {
				state.smiles = ChemNameDictSingleton.getShortestSmiles(name);
				state.inchi = ChemNameDictSingleton.getInChIForShortestSmiles(name);
				if(state.smiles != null) setNEAttribute(state.ne, "SMILES", state.smiles);
				if(state.inchi != null) setNEAttribute(state.ne, "InChI", state.inchi);
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	private boolean resolveVsDatabase(ProcessState state, String name) {
		try {
			if(pcsql != null && pcsql.hasName(name)) {
				String [] results = pcsql.getShortestSmilesAndInChI(name);
				state.smiles = results[0];
				state.inchi = results[1];
				if(state.smiles != null) setNEAttribute(state.ne, "SMILES", state.smiles);
				if(state.inchi != null) setNEAttribute(state.ne, "InChI", state.inchi);
				if(state.smiles != null) {
					cacheEntry(name, state.smiles, state.inchi, null);
				}
				return true;
			} else {
				return false;
			}		
		} catch (Exception e) {
			e.printStackTrace();
			try {
				PubChemSQL.reinitialise();
			} catch (Exception ee) {
				ee.printStackTrace();
			}
			return false;
		}		
	}

	private boolean resolveVsNewPubChem(ProcessState state, String name) {
		try {
			NewPubChem npc = NewPubChem.getInstance();
			if(npc != null) {
				String [] results = npc.getShortestSmilesAndInChI(name);
				if(results == null) return false;
				state.smiles = results[0];
				state.inchi = results[1];
				if(state.smiles != null) setNEAttribute(state.ne, "SMILES", state.smiles);
				if(state.inchi != null) setNEAttribute(state.ne, "InChI", state.inchi);
				if(state.smiles != null) {
					cacheEntry(name, state.smiles, state.inchi, null);
				}
				return true;
			} else {
				return false;
			}		
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}		
	}
	
	private boolean resolveVsExternal(ProcessState state, String name) {
		if(extensionNameResolver == null) return false;
		try {
			Results results = extensionNameResolver.resolve(name, state.args);
			if(results != null) {
				state.smiles = results.getSmiles();
				state.inchi = results.getInchi();
				state.cmlMol = results.getCml();
				if(state.smiles != null) setNEAttribute(state.ne, "SMILES", state.smiles);
				if(state.inchi != null) setNEAttribute(state.ne, "InChI", state.inchi);
				if(state.smiles != null && Oscar3Props.getInstance().cacheExtensionNameResolver) {
					cacheEntry(name, state.smiles, state.inchi, state.cmlMol);
				}
			}
			return (state.smiles != null || state.inchi != null);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	private boolean resolveVsOPSIN(ProcessState state, String name) {
		if(Oscar3Props.getInstance().useOPSIN == false) return false;
		try {
			boolean synonymInDoc = false;
			state.cmlMol = NameToStructure.getInstance().parseToCML(name);
			if(state.cmlMol != null) {
				StructureConverter.enhanceCMLMolecule(state.cmlMol, name);
				IMolecule outputMol = StructureConverter.cmlToMolecule(state.cmlMol);
				state.smiles = generator.createSMILES(outputMol);
				setNEAttribute(state.ne, "SMILES", state.smiles);
				state.inchi = ConverterToInChI.getInChI(outputMol);
				if(state.inchi != null) setNEAttribute(state.ne, "InChI", state.inchi);
				if(!makeCML) {
					state.cmlMol = null;
					cacheEntry(name, state.smiles, state.inchi, null);
				} else {
					if(!state.InChItoCML.containsKey(state.inchi)) {
						state.cmlPile.appendChild(state.cmlMol);
						state.InChItoCML.put(state.inchi, state.cmlMol);
					} else {
						synonymInDoc = true;
					}
					cacheEntry(name, state.smiles, state.inchi, new Element(state.cmlMol));								
				}
				if(synonymInDoc) state.cmlMol = null;
				return true;
			} else {
				return false;
				//cache.put(name, null);
			}
		} catch (Exception e) {
			return false;
		}
	}
	
	private void processDoc(Document doc, List<String> args) throws Exception {
		ProcessState state = new ProcessState();
		state.args = args;
		//SmilesParser parser = new SmilesParser(DefaultChemObjectBuilder.getInstance());
		//generator.setRingFinder(new NullRingFinder());
		state.InChItoCML = new HashMap<String, Element>();
		state.xRefIDtoCmlID = new HashMap<String, String>();
		int cmlId = 0;
		int nextCmlId = 1;
		Nodes cmlPiles = doc.query("//cmlPile");
		for(int i=0;i<cmlPiles.size();i++) {
			cmlPiles.get(i).detach();
		}
		state.cmlPile = null;
		if(makeCML) {
			state.cmlPile = new Element("cmlPile");
			doc.getRootElement().appendChild(state.cmlPile);
		}
		List<Element> neElements = getNEs(doc);
		for(Element neElem : neElements) {
			state.ne = neElem;
			if(state.ne.getAttribute("InChI") != null) state.ne.removeAttribute(state.ne.getAttribute("InChI"));
			if(state.ne.getAttribute("SMILES") != null) state.ne.removeAttribute(state.ne.getAttribute("SMILES"));
			if(state.ne.getAttribute("Element") != null) state.ne.removeAttribute(state.ne.getAttribute("Element"));
			String rawName = getNEName(state.ne);
			try {
				if(NETypes.REACTION.equals(getNEAttribute(state.ne, "type"))) {
					ReactionNameParser.parseRN(this, state.ne);
				} else if(NETypes.ASE.equals(getNEAttribute(state.ne, "type"))) {
					ReactionNameParser.parseASE(this, state.ne);
				}
				
				//System.out.println(rawName);
				
				//String stem = StringTools.removeTerminalS(StringTools.scrubWord(rawName));
				String stem = StringTools.removeTerminalS(rawName);
				stem = stem.replaceAll("(\\s|[\n\r])+", " ");
				
				boolean isPoly = false;
				String polyStem = null;
				if(stem.matches("poly\\(.*\\)")) {
					isPoly = true;
					polyStem = stem.substring(5, stem.length()-1);
				} else if(stem.matches("poly.*")) {
					isPoly = true;
					polyStem = stem.substring(4);
				}
				if(Oscar3Props.getInstance().interpretPoly && isPoly) {
					stem = polyStem;
				}
				
				String longStem = null;
				if(rawName.endsWith("s")) longStem = stem + "s";
				boolean is_s_singular = false;
				
				state.inchi = null;
				state.smiles = null;
				state.cmlMol = null;
				
				// Try resolution methods in turn, stop when there's a success
				// Try short stem first, then with an "s"
				if(resolveVsCache(state, stem)) {
					
				} else if(longStem != null && resolveVsCache(state, longStem)) {
					is_s_singular = true;
				} else if(resolveVsLexicon(state, stem)) {
					//No further action
				} else if(longStem != null && resolveVsLexicon(state, longStem)) {
					is_s_singular = true;
				} else if(resolveVsChemNameDict(state, stem)) {
					//No further action
				} else if(longStem != null && resolveVsChemNameDict(state, longStem)) {
					is_s_singular = true;
				} else if(resolveVsNewPubChem(state, stem)) {
					//No further action
				} else if(longStem != null && resolveVsNewPubChem(state, longStem)) {
					is_s_singular = true;
				} else if(resolveVsDatabase(state, stem)) {
					//No further action
				} else if(longStem != null && resolveVsDatabase(state, longStem)) {
					is_s_singular = true;
				} else if(resolveVsExternal(state, stem)) {
					//No further action
				} else if(longStem != null && resolveVsExternal(state, longStem)) {
					is_s_singular = true;						
				} else if(resolveVsOPSIN(state, stem)) {
					//No further action
				} else if(longStem != null && resolveVsOPSIN(state, longStem)) {
					is_s_singular = true;						
				} else {
					cache.put(stem, null);
					if(longStem != null) cache.put(longStem, null);
				}
				
				if(state.smiles != null && "X".equals(state.smiles)) {
					state.smiles = null;
					state.inchi = null;
					state.cmlMol = null;
					removeNEAttribute(state.ne, "SMILES");
					removeNEAttribute(state.ne, "InChI");
					is_s_singular = false;
				}
				
				if(is_s_singular) {
					setNEAttribute(state.ne, "singular", "yes");
				} else if(longStem != null && longStem.endsWith("s")) {
					setNEAttribute(state.ne, "plural", "yes");
				}
				
				if(isPoly) {
					setNEAttribute(state.ne, "poly", polyStem);
				}
				
				String name = stem;
				if(is_s_singular) name = longStem;
				
				/* If no CML, look for it by InChI, or make it from the SMILES */
				if(makeCML && state.cmlMol == null && state.smiles != null && state.inchi != null) {
					/* If the CML doesn't already exist */
					if(!state.InChItoCML.containsKey(state.inchi)) {
						try {
							StereoInChIToMolecule.primeCacheForInChINoThrow(state.inchi);
							IMolecule mol = ConverterToInChI.getMolFromInChI(state.inchi);
							StructureConverter.configureMolecule(mol);
							//IMolecule mol = parser.parseSmiles(smiles);
							state.cmlMol = StructureConverter.molToCml(mol, name);
							state.cmlPile.appendChild(state.cmlMol);
							state.InChItoCML.put(state.inchi, state.cmlMol);
							/* InChI checking, do something useful with this */
							/*
							 System.out.println();
							 System.out.println(inchi);
							 System.out.println(cmlMol.query(".//cml:identifier/text()", new XPathContext("cml", "http://www.xml-cml.org/schema")).get(0).toXML());
							 */
							
						} catch (UnsupportedEncodingException e) {
							throw new Error(e);
						} catch (InvalidSmilesException e) {
							//The Smiles parser should return null rather than throwing. Ho hum.
						} catch (Exception e) {
							e.printStackTrace();
						}
						/* If it does exist */
					} else {
						state.cmlMol = state.InChItoCML.get(state.inchi);
						Nodes n = state.cmlMol.query(".//cml:name/text()", new XPathContext("cml", "http://www.xml-cml.org/schema"));
						boolean hasName = false;
						for(int j=0;j<n.size();j++) {
							if(n.get(j).getValue().equals(name)) hasName = true;
						}
						if(!hasName) {
							Element cmlMolElem = state.cmlMol.getFirstChildElement("molecule", "http://www.xml-cml.org/schema");
							Element nameElem = new Element("name", "http://www.xml-cml.org/schema");
							nameElem.appendChild(name);
							cmlMolElem.insertChild(nameElem, 0);
						}
						
					}
				}
				
				/* Associate molecule with ID, and CML with ID if possible */
				if(state.cmlMol != null) {
					String id = state.cmlMol.getAttributeValue("id");
					if(id == null) {
						cmlId = nextCmlId;
						nextCmlId++;
						id = "cml" + Integer.toString(cmlId);
						state.cmlMol.addAttribute(new Attribute("id", id));
					} else {
						cmlId = Integer.parseInt(id.substring(3));
					}
					setNEAttribute(state.ne, "cmlRef", id);
				} else {
					cmlId = 0;
				}
				
				if((state.cmlMol != null) && 
						((Element)state.ne.getParent()).getLocalName().equals(XMLStrings.getInstance().HEADER)) {
					/* We might be above an experimental paragraph here */
					Node headerNode = state.ne.getParent();
					Node nextNode = XOMTools.getNextSibling(headerNode);
					while(nextNode != null && !(nextNode instanceof Element)) nextNode = XOMTools.getNextSibling(nextNode);
					if(nextNode != null && nextNode.query("datasection") != null) {
						/* OK, so nextNode is an experimental paragraph */
						Nodes n = state.ne.getParent().query("./" + XMLStrings.getInstance().COMPOUNDREF_XPATH, XMLStrings.getInstance().getXpc());
						if(n.size() == 1) {
							String xRefID = ((Element)n.get(0)).getAttributeValue(XMLStrings.getInstance().COMPOUNDREF_ID_ATTRIBUTE);
							if(!xRefID.contains(" ")) {
								/* Good, we have a single compound */
								//System.out.printf("%s %d\n", xRefID, cmlId);
								//System.out.println(headerNode.getValue());
								if(cmlId != 0) state.xRefIDtoCmlID.put(xRefID, "cml" + Integer.toString(cmlId));
								Nodes mpNodes = nextNode.query(".//property[@type=\"mp\"]");
								if(mpNodes.size() == 1) {
									Node mpNode = mpNodes.get(0);
									Nodes tempValNodes = mpNode.query("./quantity[@type=\"temp\"]/value");
									if(tempValNodes.size() == 1) {
										/* Make this first, then namespace */
										Element cmlMp = new Element("property");
										cmlMp.addAttribute(new Attribute("dictRef", "cml:mpt"));
										Element cmlScalar = new Element("scalar");
										cmlMp.appendChild(cmlScalar);
										/* FIXME - blatant hack */
										cmlScalar.addAttribute(new Attribute("units", "units:celsius"));
										
										Element tempValElem = (Element)tempValNodes.get(0);
										Element min = tempValElem.getFirstChildElement("min");
										Element point = tempValElem.getFirstChildElement("point");
										if(min == null) {
											cmlScalar.appendChild(point.getValue());
										} else {
											String minStr = min.getValue();
											String maxStr = point.getValue();
											while(!Character.isDigit(maxStr.charAt(0))) maxStr = maxStr.substring(1);
											if(maxStr.length() == (minStr.length() - 1)) {
												maxStr = minStr.substring(0,1) + maxStr;
											}
											cmlScalar.addAttribute(new Attribute("minValue", minStr));
											cmlScalar.addAttribute(new Attribute("maxValue", maxStr));
										}
										XOMTools.setNamespaceURIRecursively(cmlMp, "http://www.xml-cml.org/schema");
										state.cmlMol.getFirstChildElement("molecule", "http://www.xml-cml.org/schema").appendChild(cmlMp);
									} 
								}
							}
						}
					}
				}

				String ontIds = "";
				
				if(state.inchi != null) {
					Set<String> ontologyIds = ChemNameDictSingleton.getOntologyIdsFromInChI(state.inchi);
					if(ontologyIds != null && ontologyIds.size() > 0) {
						ontIds += StringTools.collectionToString(ontologyIds, " ") + " ";
					}
				}
				
				String normName = StringTools.normaliseName(rawName);
				if(OntologyTerms.hasTerm(normName)) {
					ontIds += OntologyTerms.idsForTerm(normName) + " ";
				}
				
				if(ontIds.length() > 0) {
					String oldOntIds = getNEAttribute(state.ne, "ontIDs");
					if(oldOntIds == null || oldOntIds.length() == 0) {
						setNEAttribute(state.ne, "ontIDs", ontIds.substring(0, ontIds.length()-1));
					} else {
						setNEAttribute(state.ne, "ontIDs", StringTools.mergeSpaceSeparatedSets(
								oldOntIds, ontIds.substring(0, ontIds.length()-1)));						
					}
				}
				
							
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		//System.out.println(xRefIDtoCmlID);
		
		// TODO - make more general for SAF
		if(makeCML) {
			
			Nodes xRefNodes = doc.query(".//" + XMLStrings.getInstance().COMPOUNDREF_XPATH);
			for(int i=0;i<xRefNodes.size();i++) {
				Element xRefElem = (Element)xRefNodes.get(i);
				String id = xRefElem.getAttributeValue(XMLStrings.getInstance().COMPOUNDREF_ID_ATTRIBUTE);
				if(state.xRefIDtoCmlID.containsKey(id)) {
					xRefElem.addAttribute(new Attribute("cmlRef", state.xRefIDtoCmlID.get(id)));
				}
			}
			
			Elements cmlElems = state.cmlPile.getChildElements("cml", "http://www.xml-cml.org/schema");
			for(int i=0;i<cmlElems.size();i++) {
				Element cml = cmlElems.get(i);
				String id = cml.getAttributeValue("id");
				Element cmlUrlEnc = new Element("cmlUrlEnc");
				cmlUrlEnc.addAttribute(new Attribute("idRef", id));
				cmlUrlEnc.appendChild(StringTools.urlEncodeLongString(cml.toXML()));
				state.cmlPile.appendChild(cmlUrlEnc);
			}
			
		}
	}

	
}
