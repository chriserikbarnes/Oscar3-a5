package uk.ac.cam.ch.wwmm.ptc.experimental.relations;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import nu.xom.Document;
import nu.xom.Element;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.SentenceSplitter;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.Token;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.ProcessingDocument;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.ProcessingDocumentFactory;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.TokenSequence;

public class Relations {
	
	public static Document analysePaper(Document sourceDoc, Document safDoc) throws Exception {
		Element rootElem = new Element("relations");
		Document relationDoc = new Document(rootElem);

		Document doc = sourceDoc;
		//Document doc = new Document((Element)(sourceDoc.getRootElement().copy()));
		safDoc = new Document((Element)(safDoc.getRootElement().copy()));
		
		//NameRecogniser nr = new NameRecogniser();
		//nr.halfProcess(doc);
		ProcessingDocument procDoc = ProcessingDocumentFactory.getInstance().makeTokenisedDocument(doc, false, false, false);
		
		DFARelationFinder relf = DFARelationFinder.getInstance();
		List<Lattice> lattices = Lattice.buildLattices(procDoc, safDoc.getRootElement());
		
		for(Lattice lattice : lattices) {
			TokenSequence tokSeq = lattice.tokSeq;
			List<Token> tokens = tokSeq.getTokens();
			List<List<Token>> sentences = SentenceSplitter.makeSentences(tokens);
			Set<String> existingRelations = new HashSet<String>();
			for(List<Token> sentence : sentences) {
				List<Relation> relations = relf.getRelations(sentence, "", lattice, null);
				if(relations.size() > 0) {
					for(Relation r : relations) {
						String rStr = r.toXML().toXML();
						if(existingRelations.contains(rStr)) continue;
						existingRelations.add(rStr);
						rootElem.appendChild(r.toXML());
					}
				}
			}
		}
		return relationDoc;
	}

	
}
