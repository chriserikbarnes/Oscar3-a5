package uk.ac.cam.ch.wwmm.ptc.experimental.termsimilarity;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Nodes;
import uk.ac.cam.ch.wwmm.ptclib.datastruct.Bag;
import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;

public class EntityPaperFVE extends FeatureVectorExtractor {

	Map<String, Bag<String>> featureVectors;
	Bag<String> features;
	Bag<String> terms;
	
	public EntityPaperFVE(List<File> files) throws Exception {
		features = new Bag<String>();
		terms = new Bag<String>();
		featureVectors = new HashMap<String, Bag<String>>();
		
		for(File f : files) {
			Document doc = new Builder().build(f);
			Nodes n = doc.query("//ne");
			Set<String> entities = new HashSet<String>();
			for(int i=0;i<n.size();i++) {
				entities.add(StringTools.normaliseName(n.get(i).getValue()).replaceAll("\\s+", "_"));
			}
			
			if(entities.size() == 1) continue;
			
			for(String e : entities) {
				terms.add(e);
				if(!featureVectors.containsKey(e)) featureVectors.put(e, new Bag<String>());
				featureVectors.get(e).add(f.getParentFile().getName());
				features.add(f.getParentFile().getName());
			}
			
		}
		
		//for(String term : featureVectors.keySet()) {
		//	System.out.println(term);
		//	for(String feature : featureVectors.get(term).getList()) {
		//		System.out.println("\t" + feature + "\t" + featureVectors.get(term).getCount(feature));
		//	}
		//	System.out.println();
		//}
		
		
	}
	
	@Override
	public Map<String, Bag<String>> getFeatureVectors() {
		return featureVectors;
	}

	@Override
	public Bag<String> getFeatures() {
		return features;
	}

	@Override
	public Bag<String> getTerms() {
		return terms;
	}

}
