package uk.ac.cam.ch.wwmm.ptc.experimental.termsimilarity;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nu.xom.Element;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.Token;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.TokenSequence;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.TokenSequenceSource;
import uk.ac.cam.ch.wwmm.oscar3.terms.OBOOntology;
import uk.ac.cam.ch.wwmm.oscar3.terms.TermSets;
import uk.ac.cam.ch.wwmm.ptclib.datastruct.Bag;
import uk.ac.cam.ch.wwmm.ptclib.saf.SafTools;
import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;

public class InlineFVE extends FeatureVectorExtractor {

	Map<String, Bag<String>> featureVectors;
	Bag<String> features;
	Bag<String> terms;
	boolean useOntology = false;
	boolean useType = true;
	boolean useSubtype = true;
	
	private void addFeature(String term, String feature) {
		feature = feature.intern();
		features.add(feature);
		featureVectors.get(term).add(feature);
	}
	
	public InlineFVE(List<File> files) {
		terms = new Bag<String>();

		/*StringSource stringSource = new StringSource(files);
		
		stringSource.reset();
		for(String s : stringSource) {
			//System.out.println(s);
			Tokeniser t = new Tokeniser(null);
			t.tokenise(s);
			for(String word : t.getTokenStringList()) {
				terms.add(StringTools.normaliseName(word));
			}
		}*/
		
		TokenSequenceSource tokSeqSource = new TokenSequenceSource(files);
		tokSeqSource.reset();
		for(TokenSequence t : tokSeqSource) {
			for(String word : t.getTokenStringList()) {
				word = StringTools.normaliseName(word);
				word = word.replaceAll("\\s+", "_");
				terms.add(word.intern());
			}	
		}
		
		System.out.println("Tallied terms");
		System.out.println(terms.size() + " in corpus");
		//terms.discardInfrequent(5);
		System.out.println("Discarded infrequent");
		System.out.println(terms.size() + " terms remain");
		
		//for(String s : terms.getWordList().subList(0, 500)) {
		//	System.out.println(s + " " + terms.getCount(s));
		//}
		//if(true) return;
		
		//Set<String> stopWords = new HashSet<String>(terms.getWordList().subList(0, 17));
		
		Set<String> stopWords = new HashSet<String>();
		stopWords.addAll(TermSets.getClosedClass());
		for(String s : terms.getList()) {
			if(TermSets.getClosedClass().contains(s)) stopWords.add(s);
			if(!s.matches(".*[A-Za-z].*")) stopWords.add(s);
		}
		
		//for(String s : stopWords) {
		//	System.out.println(s + " " + terms.getCount(s));
		//}
		System.out.println("Extracted " + stopWords.size() + " stopterms...");
		
		featureVectors = new HashMap<String,Bag<String>>();
		for(String s : terms.getSet()) {
			if(!stopWords.contains(s)) {
				featureVectors.put(s, new Bag<String>());
			}
		}
		System.out.println("Initialised feature vectors");
		
		
		features = new Bag<String>();

		for(TokenSequence t : tokSeqSource) {
			List<String> currentStops = new ArrayList<String>();
			String prevWord = null;
			Set<String> prevOntIDs = null;
			String prevType = null;
			for(Token token : t.getTokens()) {
				String word = token.getValue();
				word = StringTools.normaliseName(word);
				word = word.replaceAll("\\s+", "_");
				if(word == null || word.length() == 0) continue;
				
				Set<String> ontIDs = null;
				String type = null;
				Element ne = token.getNeElem();
				if(ne != null && useOntology) {
					String ontIDString = SafTools.getSlotValue(ne, "ontIDs");
					if(ontIDString != null && ontIDString.length() > 0) {
						ontIDs = new HashSet<String>();
						for(String ontID : StringTools.arrayToList(ontIDString.split("\\s+"))) {
							ontIDs.addAll(OBOOntology.getInstance().getIdsForIdWithAncestors(ontID));
						}
						//System.out.println(ontIDs);
					}
				}
				if(ne != null && useType) {
					type = SafTools.getSlotValue(ne, "type");
					String subtype = SafTools.getSlotValue(ne, "subtype");
					if(subtype != null && useSubtype) {
						type = type + ":" + subtype;
					}
				}
				
				// Infrequent terms have been dropped.
				if(terms.getCount(word) == 0) {
					currentStops.clear();
					prevWord = null;
					prevOntIDs = null;
					prevType = null;
				} else if(".".equals(word)) {
					currentStops.clear();
					prevWord = null;
					prevOntIDs = null;
					prevType = null;
				} else if(stopWords.contains(word)) {
					currentStops.add(word);
				} else {
					if(prevWord != null) {
						if(currentStops.size() > 0) {							
							StringBuffer innerTerm = new StringBuffer();
							for(String sw : currentStops) {
								innerTerm.append("_" + sw);
							}
							String inner = innerTerm.toString();
							addFeature(prevWord, "R" + inner + "_" + word);
							addFeature(word, "L_" + prevWord + inner);
							if(type != null) {
								addFeature(prevWord, "R" + inner + "_" + type);
							}
							if(prevType != null) {
								addFeature(word, "L_" + prevType + inner);
							}							
							if(useOntology) {
								if(ontIDs != null) {
									for(String ontID : ontIDs) {
										addFeature(prevWord, "R" + inner + "_" + ontID);
									}
								}
								if(prevOntIDs != null) {
									for(String prevOntID : prevOntIDs) {
										addFeature(word, "L_" + prevOntID + inner);
									}
								}
							}
						} else {
							addFeature(prevWord, "R_" + word);
							addFeature(word, "L_" + prevWord);
							if(type != null) {
								addFeature(prevWord, "R_" + type);
							}
							if(prevType != null) {
								addFeature(word, "L_" + prevType);
							}														
							if(useOntology) {
								if(ontIDs != null) {
									for(String ontID : ontIDs) {
										addFeature(prevWord, "R_" + ontID);
									}
								}
								if(prevOntIDs != null) {
									for(String prevOntID : prevOntIDs) {
										addFeature(word, "L_" + prevOntID);
									}
								}
							}
						}
					} 
					prevWord = word;
					prevOntIDs = ontIDs;
					prevType = type;
					currentStops.clear();
				}
			}
		}
		System.out.println("Feature vectors computed");
		System.out.println(features.getSet().size() + " features in total");
		int minBreadth = 5;
		int minDepth = 2;
		
		Map<String,Integer> deepFCounts = new HashMap<String,Integer>();
		for(String f : features.getSet()) deepFCounts.put(f, 0);
		for(String word : featureVectors.keySet()) {
			for(String feature : featureVectors.get(word).getSet()) {
				if(featureVectors.get(word).getCount(feature) >= minDepth) {
					deepFCounts.put(feature, deepFCounts.get(feature) + 1); 
				}
			}
		}
		
		Set<String> badFeatures = new HashSet<String>();
		for(String feature : deepFCounts.keySet()) {
			if(deepFCounts.get(feature) < minBreadth) {
				badFeatures.add(feature);
			}
		}
		System.out.println("Chosen features to remove");
		
		for(String bf : badFeatures) {
			features.remove(bf);
		}
		
		for(String word : featureVectors.keySet()) {
			for(String feature : new ArrayList<String>(featureVectors.get(word).getSet())) {
				if(badFeatures.contains(feature)) {
					featureVectors.get(word).remove(feature);
				}
			}
		}
		System.out.println("Features filtered");
		System.out.println(features.getSet().size() + " features remain");
		
		for(String term : terms.getList()) {
			if(!featureVectors.containsKey(term) || featureVectors.get(term).totalCount() == 0) terms.remove(term);
		}
		System.out.println("Terms filtered");
		System.out.println(terms.getSet().size() + " terms remain");
		
	}
	
	@Override
	public Map<String, Bag<String>> getFeatureVectors() {
		// TODO Auto-generated method stub
		return featureVectors;
	}

	@Override
	public Bag<String> getFeatures() {
		// TODO Auto-generated method stub
		return features;
	}

	@Override
	public Bag<String> getTerms() {
		// TODO Auto-generated method stub
		return terms;
	}

}
