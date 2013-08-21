package uk.ac.cam.ch.wwmm.oscar3server.scrapbook;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Serializer;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.NamedEntity;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.ProcessingDocument;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.ProcessingDocumentFactory;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.Token;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.TokenSequence;
import uk.ac.cam.ch.wwmm.oscar3.terms.TermSets;
import uk.ac.cam.ch.wwmm.ptclib.saf.SafTools;
import uk.ac.cam.ch.wwmm.ptclib.scixml.SAFToInline;
import uk.ac.cam.ch.wwmm.ptclib.xml.XMLSpanTagger;

/**A (somewhat experimental) class to annotate text for Potential Reaction
 * Words.
 * 
 * @author ptc24
 *
 */
final class ReactWords {

	private static ReactWords myInstance;
	private Set<String> prwStrings;
	
	static ReactWords getInstance() throws Exception {
		if(myInstance == null) myInstance = new ReactWords();
		return myInstance;
	}
	
	ReactWords() throws Exception {
		//List<String> strings = FileTools.getStrings(new File("/home/ptc24/tmp/reactwords.txt"));
		prwStrings = new HashSet<String>();
		for(String string : TermSets.getReactWords()) {
			prwStrings.add(string);
			if(string.contains("homo")) prwStrings.add(string.replaceFirst("homo", "hetero"));
			if(string.contains("polymer")) {
				prwStrings.add(string.replaceFirst("polymer", "dimer"));
				prwStrings.add(string.replaceFirst("polymer", "trimer"));
				prwStrings.add(string.replaceFirst("polymer", "tetramer"));
				prwStrings.add(string.replaceFirst("polymer", "pentamer"));
				prwStrings.add(string.replaceFirst("polymer", "hexamer"));
				prwStrings.add(string.replaceFirst("polymer", "heptamer"));
				prwStrings.add(string.replaceFirst("polymer", "octamer"));
				prwStrings.add(string.replaceFirst("polymer", "oligomer"));
			}
		}
	}
	
	Document annotateDoc(Document doc) throws Exception {
		ProcessingDocument procDoc = ProcessingDocumentFactory.getInstance().makeTokenisedDocument(doc, false, false, false);
		//NameRecogniser nr = new NameRecogniser();
		//nr.halfProcess(doc);
		//nr.makeTokenisers(false);
		Element safholder = new Element("saf");
		Document safDoc = new Document(safholder);
		for(TokenSequence t : procDoc.getTokenSequences()) {
			for(Token token : t.getTokens()) {
				//System.out.println(token.getValue());
				String value = token.getValue();
				value = value.toLowerCase();
				if(prwStrings.contains(value)) {
					List<Token> neTokens = new ArrayList<Token>();
					neTokens.add(token);
					NamedEntity ne = new NamedEntity(neTokens, token.getValue(), "PRW");
					safholder.appendChild(ne.toSAF());
					//System.out.println("**********");
				}
			}
			//System.out.println();
		}
		XMLSpanTagger.deTagElement(doc.getRootElement());
		
		SafTools.numberSaf(safDoc, "oscar", "o");
				
		return SAFToInline.safToInline(safDoc, doc, false);
	}

}
