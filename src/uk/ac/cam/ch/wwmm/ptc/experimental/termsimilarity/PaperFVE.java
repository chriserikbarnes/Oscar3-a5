package uk.ac.cam.ch.wwmm.ptc.experimental.termsimilarity;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nu.xom.Builder;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.Token;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.ProcessingDocument;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.ProcessingDocumentFactory;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.TokenSequence;
import uk.ac.cam.ch.wwmm.oscar3.terms.TermSets;
import uk.ac.cam.ch.wwmm.ptclib.datastruct.Bag;
import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;

public class PaperFVE extends FeatureVectorExtractor {

	Map<String, Bag<String>> featureVectors;
	Bag<String> features;
	Bag<String> terms;
	
	public PaperFVE(List<File> files) throws Exception {
		features = new Bag<String>();
		terms = new Bag<String>();
		featureVectors = new HashMap<String, Bag<String>>();
		
		for(File f : files) {
			String fileName = f.getParentFile().getName();
			terms.add(fileName);
			Bag<String> fVect = new Bag<String>();
			featureVectors.put(fileName, fVect);
			
			//NameRecogniser nr = new NameRecogniser();
			
			//nr.halfProcess(new Builder().build(new File(f.getParentFile(), "source.xml")));
			//List<Tokeniser> tokSeqs = nr.buildTokenTables(new Builder().build(new File(f.getParentFile(), "saf.xml")).getRootElement(), false, true);
			
			ProcessingDocument procDoc = ProcessingDocumentFactory.getInstance().makeTokenisedDocument(new Builder().build(new File(f.getParentFile(), "source.xml")), true, true, false,
					new Builder().build(new File(f.getParentFile(), "saf.xml")));
			
			for(TokenSequence tokSeq : procDoc.getTokenSequences()) {
				for(Token t : tokSeq.getTokens()) {
					String tokVal = t.getValue();
					tokVal = StringTools.normaliseName(tokVal);
					tokVal.replaceAll("\\s+", "_");
					
					if(!tokVal.matches(".*[A-Za-z].*")) continue;
					if(TermSets.getClosedClass().contains(tokVal.toLowerCase())) continue;
					
					tokVal = tokVal.intern();
					features.add(tokVal);
					fVect.add(tokVal);
				}
			}
			
		}		
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
