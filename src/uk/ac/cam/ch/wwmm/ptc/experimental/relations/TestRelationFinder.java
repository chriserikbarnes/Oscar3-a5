package uk.ac.cam.ch.wwmm.ptc.experimental.relations;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Serializer;
import uk.ac.cam.ch.wwmm.oscar3.misc.NewGeniaRunner;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.HyphenTokeniser;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.ProcessingDocument;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.ProcessingDocumentFactory;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.SentenceSplitter;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.Token;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.TokenSequence;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.tokenanalysis.TLRHolder;
import uk.ac.cam.ch.wwmm.oscar3.terms.OBOOntology;
import uk.ac.cam.ch.wwmm.ptclib.io.FileTools;
import uk.ac.cam.ch.wwmm.ptclib.saf.SafTools;

public class TestRelationFinder {

	List<Relation> allRelations;
	RNEffects rnEffects;
		
	public TestRelationFinder() throws Exception {
		allRelations = new ArrayList<Relation>();
		rnEffects = new RNEffects();
	}
	
	public Document analysePaper(File paperDir) throws Exception {
		Element rootElem = new Element("relations");
		Document relationDoc = new Document(rootElem);
		
		File inlineFile = new File(paperDir, "source.xml");
		File safFile = new File(paperDir, "saf.xml");
		Document doc = new Builder().build(inlineFile);
		Document safDoc = new Builder().build(safFile);
		ProcessingDocument procDoc = ProcessingDocumentFactory.getInstance().makeTokenisedDocument(doc, false, false, false);
		//NameRecogniser nr = new NameRecogniser();
		//nr.halfProcess(doc);
		//Nodes n = doc.query(XMLStrings.CHEMICAL_PLACES_XPATH);
		DFARelationFinder relf = DFARelationFinder.getInstance();
		List<Lattice> lattices = Lattice.buildLattices(procDoc, safDoc.getRootElement());
		
		for(Lattice lattice : lattices) {
			/*for(LatticeCell cell : lattice.getAllCells()) {
				System.out.println(cell);
				for(LatticeCell next : cell.next) {
					System.out.println("\t->\t" + next);
				}
				for(LatticeCell prev : cell.prev) {
					System.out.println("\t<-\t" + prev);
				}
			}*/
			
			TokenSequence tokSeq = lattice.tokSeq;
			List<Token> tokens = tokSeq.getTokens();
			List<List<Token>> sentences = SentenceSplitter.makeSentences(tokens);
			Set<String> existingRelations = new HashSet<String>();
			for(List<Token> sentence : sentences) {
				/*NewGeniaRunner.runGenia(sentence);
				List<NamedEntity> bioNEs = NewGeniaRunner.getGeniaNEs(sentence);
				for(NamedEntity bioNE : bioNEs) {
					//System.out.println(bioNE);
					safDoc.getRootElement().a
				}*/
				String sentenceStr = paperDir.getName() + ": " + SentenceSplitter.sentenceString(sentence);
				//boolean alreadyPrintedSentence = false;
				List<Relation> relations = relf.getRelations(sentence, sentenceStr, lattice, paperDir);
				System.out.println(sentenceStr);
				if(relations.size() > 0) {
					for(Relation r : relations) {
						String rStr = r.toXML().toXML();
						//if(existingRelations.contains(rStr)) continue;
						//existingRelations.add(rStr);
						//System.out.println(r.toXML().toXML());
						System.out.println(r.toString());
						System.out.println(r.getPattern());
						//rootElem.appendChild(r.toXML());
						//allRelations.add(r);
					}
				}
				//NewGeniaRunner.runGenia(sentence);
				//List<Element> neList = new ArrayList<Element>();
				/*for(Token t : sentence) {
					if(t.getNeElem() != null) neList.add(t.getNeElem());
				}
				for(Element ei : neList) {
					String eiOntIds = SafTools.getSlotValue(ei, "ontIDs");
					if(eiOntIds == null || eiOntIds.length() == 0) continue;
					List<String> ontIDs = StringTools.arrayToList(eiOntIds.split("\\s+"));
					String iSurf = SafTools.getSlotValue(ei, "surface");
					for(String ontID : ontIDs) {
						if(!ontID.startsWith("CHEBI")) continue;
						for(Element ej : neList) {
							if(ei == ej) continue;
							String ejOntIds = SafTools.getSlotValue(ej, "ontIDs");
							if(ejOntIds == null || ejOntIds.length() == 0) continue;
							List<String> jontIDs = StringTools.arrayToList(ejOntIds.split("\\s+"));
							String jSurf = SafTools.getSlotValue(ej, "surface");
							if(iSurf.toLowerCase().equals(jSurf.toLowerCase())) continue;
							for(String jontID : jontIDs) {
								String rel = "is_a";
								if(jontID.equals(ontID)) {
									rel = "synonym_of";
									if(neList.indexOf(ei) > neList.indexOf(ej)) continue;
								}
								//if(OntologyResolver.getInstance().isa(ontID, jontID)) {
								//	System.out.printf("%s (%s) %s %s (%s)\n", iSurf, ontID, rel, jSurf, jontID);
								//}
							}
						}
					}
				}*/
			}
		}
		return relationDoc;
	}
	
	public void reportRelations() throws Exception {
		Map<String,List<Relation>> relationsForTerms = new HashMap<String,List<Relation>>();
		
		final Map<Relation,Double> relationConfidences = new HashMap<Relation,Double>();
		for(Relation relation : allRelations) {
			double minConf = 1.0;
			double prodConf = 1.0;
			for(String role : relation.getEntityDict().keySet()) {
				for(Element annot : relation.getEntityDict().get(role)) {
					String confStr = SafTools.getSlotValue(annot, "confidence");
					if(confStr != null) {
						double conf = Double.parseDouble(confStr);
						minConf = Math.min(minConf, conf);
						prodConf *= conf;
					}
				}
			}
			relationConfidences.put(relation, prodConf);
		}
		
		Collections.sort(allRelations, new Comparator<Relation>() {
			public int compare(Relation o1, Relation o2) {
				return relationConfidences.get(o1).compareTo(relationConfidences.get(o2));
			}
		});
		//MiscTools.sortList(allRelations, relationConfidences);
		for(Relation relation : allRelations) {
			System.out.println(relation);
			System.out.println("\t" + relation.pattern);
			System.out.println("\t" + relationConfidences.get(relation));
		}
		
		if(true) return;
		
		/*for(Relation relation : allRelations) {
			for(String role : relation.getDict().keySet()) {
				for(String term : relation.getDict().get(role)) {
					if(!relationsForTerms.containsKey(term)) relationsForTerms.put(term, new ArrayList<Relation>());
					relationsForTerms.get(term).add(relation);
				}
			}
		}*/
		for(String term : relationsForTerms.keySet()) {
			System.out.println(term);
			for(Relation relation : relationsForTerms.get(term)) {
				//List<Double> confidences = new ArrayList<Double>();
				double minConf = 1.0;
				double prodConf = 1.0;
				for(String role : relation.getEntityDict().keySet()) {
					for(Element annot : relation.getEntityDict().get(role)) {
						String confStr = SafTools.getSlotValue(annot, "confidence");
						if(confStr != null) {
							double conf = Double.parseDouble(confStr);
							minConf = Math.min(minConf, conf);
							prodConf *= conf;
						}
					}
				}
				System.out.println("\t" + relation);
				if(minConf < 1.0) {
					System.out.println("\t" + minConf + "\t" + prodConf);
				}
				System.out.println("\t" + relation.getPattern());
				if(false && (relation.getType().equals("REACTION") || relation.getType().equals("ENZYME"))) {
					/*String reaction = null;
					Element substrateElem = null;
					if(relation.getType().equals("REACTION")) {
						reaction = relation.getDict().get("reaction");					
						substrateElem = relation.getEntityDict().get("substrate");
					} else {
						reaction = relation.getDict().get("enzyme");										
						substrateElem = relation.getEntityDict().get("chemical");
					}
					if(reaction != null && substrateElem != null) {
						String inchi = SafTools.getSlotValue(substrateElem, "InChI");
						String name = SafTools.getSlotValue(substrateElem, "surface");
						if(inchi != null) {
							StereoInChIToMolecule.primeCacheForInChI(inchi);
							IMolecule mol = ConverterToInChI.getMolFromInChI(inchi);
							if(mol != null) {
								System.out.println(rnEffects.applyRN(mol, reaction, name));
							} 
						}
					}*/
				}
				//System.out.println(relation.getSentence());
			}
		}
		
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		File dir = new File("/home/ptc24/newows/corpora/BioIE");
		//File dir = new File("/home/ptc24/newows/corpora/combined_corpus/BioIE");
		//File dir = new File("/home/ptc24/newows/corpora/combined_corpus/bigBactMet");
		//File dir = new File("C:\\newows\\corpora\\newGrapefruit");
		//File dir = new File("C:\\newows\\corpora\\bigCancerPapers");
		//File dir = new File("C:\\newows\\corpora\\headache");
		//File dir = new File("C:\\newows\\corpora\\borane");
		//File dir = new File("C:\\newows\\corpora\\morersc");
		//File dir = new File("C:\\newows\\corpora\\totallysynthetic");
		List<File> sourceFiles = FileTools.getFilesFromDirectoryByName(dir, "source.xml");
		TestRelationFinder trf = new TestRelationFinder();
		NewGeniaRunner.getInstance();
		HyphenTokeniser.init();
		TLRHolder.reinitialise();
		OBOOntology.getInstance();
		System.out.println("Gathering relations...");
		long time = System.currentTimeMillis();
		for(File f : sourceFiles) {
			//if(!f.getParentFile().getName().equals("11477318")) continue;
			//if(!f.getParentFile().getName().equals("16233876")) continue;
			System.out.println(f);
			Document relDoc =  trf.analysePaper(f.getParentFile());
			Serializer ser = new Serializer(System.out);
			ser.setIndent(2);
			ser.write(relDoc);
		}
		System.out.println("Done in " + (System.currentTimeMillis() - time) + " milliseconds");
		double perPaper = (System.currentTimeMillis() - time) * 1.0 / (1000.0 * sourceFiles.size());
		System.out.println(perPaper + " seconds per abstract");
		trf.reportRelations();
		
	}

}
