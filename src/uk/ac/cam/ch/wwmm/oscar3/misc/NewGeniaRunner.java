package uk.ac.cam.ch.wwmm.oscar3.misc;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import nu.xom.Document;
import nu.xom.Element;
import uk.ac.cam.ch.wwmm.oscar3.Oscar3Props;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.NamedEntity;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.ProcessingDocument;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.ProcessingDocumentFactory;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.Token;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.TokenSequence;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.tokenanalysis.TokenTypes;
import uk.ac.cam.ch.wwmm.ptclib.saf.SafTools;

/** An improved method of running the GENIA tagger over annotated SciXML.
 * 
 * @author ptc24
 *
 */
public final class NewGeniaRunner {

	private Writer writer;
	private BufferedReader bufferedReader;
	private static NewGeniaRunner myInstance;
	
	/**Gets the NewGeniaRunner singleton.
	 * 
	 * @return The NewGeniaRunner singleton, if this can be set up. 
	 * Otherwise null.
	 */
	public static NewGeniaRunner getInstance() {
		try {
			if("none".equals(Oscar3Props.getInstance().geniaPath)) return null;
			if(myInstance == null) myInstance = new NewGeniaRunner();
			return myInstance;
		} catch (Exception e) {
			return null;
		}
	}
	
	/**Tests whether the Oscar3 properties file mentions a path for the Genia
	 * tagger (version 3.0 or later)
	 * 
	 * @return Whether the Genia tagger is likely to be available.
	 */
	public static boolean canRunGenia() {
		return !"none".equals(Oscar3Props.getInstance().geniaPath);
	}
	
	private NewGeniaRunner() throws Exception {
		Process p = Runtime.getRuntime().exec(Oscar3Props.getInstance().geniaPath + " -nt", null, 
				new File(Oscar3Props.getInstance().geniaPath).getParentFile());
		
		bufferedReader = new BufferedReader(new InputStreamReader(p.getInputStream()));
		BufferedReader ebr = new BufferedReader(new InputStreamReader(p.getErrorStream()));

		writer = new OutputStreamWriter(p.getOutputStream());
		
		String eline;

		for(int i=0;i<4;i++) {
			eline = ebr.readLine();
			if(Oscar3Props.getInstance().verbose) System.out.println(eline);
		}		
	}
	
	private void shutdown() {
		try {
			writer.close();
			bufferedReader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**Creates a SAF XML document containing data produced by the Genia tagger
	 * when fed the contents of the source SciXML document.
	 * 
	 * @param sourceDoc The source SciXML document.
	 * @return A SAF XML document containing various information.
	 * @throws Exception
	 */
	public Document runGenia(Document sourceDoc) throws Exception {
		ProcessingDocument procDoc = ProcessingDocumentFactory.getInstance().makeTokenisedDocument(sourceDoc, false, false, true);
		Document safDoc = new Document(new Element("saf"));
		for(List<Token> sentence : procDoc.getSentences()) {
			if(sentence.size() > 0) {
				Token first = sentence.get(0);
				Token last = sentence.get(sentence.size()-1);
				Element sentenceAnnot = SafTools.makeAnnot(first.getStartXPoint(), last.getEndXPoint(),	"sentence");
				safDoc.getRootElement().appendChild(sentenceAnnot);
			}
		}
		for(TokenSequence ts : procDoc.getTokenSequences()) {
			for(Token t : ts.getTokens()) {
				Element safElem = SafTools.makeAnnot(t.getStartXPoint(), t.getEndXPoint(), "genia");
				SafTools.setSlot(safElem, "surface", t.getValue());
				SafTools.setSlot(safElem, "stem", t.getGeniaData()[1]);
				SafTools.setSlot(safElem, "tag", t.getGeniaData()[2]);
				SafTools.setSlot(safElem, "chunk", t.getGeniaData()[3]);
				SafTools.setSlot(safElem, "geniane", t.getGeniaData()[4]);
				safDoc.getRootElement().appendChild(safElem);
			}
		}
		return safDoc;
	}
	
	/**Runs the Genia tagger over a sentence, adding information to the
	 * individual Token objects that constitute the sentence.
	 * 
	 * @param sentence The sentence to feed to the Genia tagger.
	 */
	public static void runGenia(List<Token> sentence) {
		NewGeniaRunner instance = getInstance();
		try {
			if(instance == null) return;
			instance.runGeniaInternal(sentence);
		} catch (Exception e) {
			e.printStackTrace();
			if(instance != null) {
				instance.shutdown();
				myInstance = null;
			}
		}
	}
	
	private void runGeniaInternal(List<Token> sentence) throws Exception {
		List<Token> oldSentence = sentence;
		sentence = new ArrayList<Token>();
		for(Token t : oldSentence) {
			if(!TokenTypes.isRef(t)) sentence.add(t);
		}
		
		StringBuffer sb = new StringBuffer();
		boolean first = true;
		for(Token t : sentence) {
			if(!first) sb.append(" ");
			String val = t.getValue();
			val = val.replaceAll("\\s+", "_");
			sb.append(val);
			first = false;
		}
		//System.out.println(sb.toString());
		writer.write(sb.toString());
		writer.write("\n");
		writer.flush();
		String line = bufferedReader.readLine();
		int index = 0;
		while(line != null) {
			if(line.matches("\\s*")) {
				writer.flush();
				line = null;
			} else {
				String [] sss = line.split("\t");
				Token t = sentence.get(index);
				t.setGeniaData(sss);
				line = bufferedReader.readLine();				
				index++;
			}
		}

	}
	
	/**Assigns each Token in a sentence to a chunk. This should be run after
	 * the tagger has been run over the sentence, and updates the information
	 * stored in the Token objects.
	 * 
	 * @param sentence The sentence.
	 */
	public static void assignChunks(List<Token> sentence) {
		List<Token> chunk = null;
		String chunkType = null;
		for(Token t : sentence) {
			if(t.getGeniaData() == null) continue;
			String chunkData = t.getGeniaData()[3];
			if(chunkData.equals("O")) {
				//if(chunk != null) {
				//	for(Token tt : chunk) System.out.print(tt.getValue() + " ");
				//	System.out.println();
				//}
				chunk = null;
				chunkType = null;
			} else if(chunkData.equals("I-" + chunkType)) {
				chunk.add(t);
				t.setChunk(chunk);
				t.setChunkType(chunkType);
				// We occasionally get things like I-NP I-VP, so treat I-VP as N-VP
			} else if(chunkData.startsWith("B") || chunkData.startsWith("I")) {
				//if(chunk != null) {
				//	for(Token tt : chunk) System.out.print(tt.getValue() + " ");
				//	System.out.println();
				//}
				chunk = new ArrayList<Token>();
				chunkType = chunkData.substring(2);
				chunk.add(t);
				t.setChunk(chunk);
				t.setChunkType(chunkType);
			} else {
				throw new Error();
				//chunk = null;
				//chunkType = null;
			}
		}
		//if(chunk != null) {
		//	for(Token tt : chunk) System.out.print(tt.getValue() + " ");
		//	System.out.println();
		//}
	}
	
	private static NamedEntity makeNE(List<Token> neTokens, String neType) {
		Token firstToken = neTokens.get(0);
		Token lastToken = neTokens.get(neTokens.size()-1);
		String surf = firstToken.getTokenSequence().getSubstring(firstToken.getId(), lastToken.getId());
		return new NamedEntity(neTokens, surf, "GENIA-" + neType);
	}

	/**Looks through a list of Token objects that have previously been tagged
	 * by the Genia tagger, and produces NamedEntity objects corresponding
	 * to the NEs detected by the tagger.
	 * 
	 * @param sentence The tokens to analyse.
	 * @return The NamedEntity objects created.
	 */
	public static List<NamedEntity> getGeniaNEs(List<Token> sentence) {
		List<Token> neTokens = null;
		String neType = null;
		List<NamedEntity> neList = new ArrayList<NamedEntity>();
		for(Token t : sentence) {
			if(t.getGeniaData() == null) continue;
			String neData = t.getGeniaData()[4];
			if(neData.equals("O")) {
				if(neType != null) neList.add(makeNE(neTokens, neType));
				neTokens = null;
				neType = null;
			} else if(neData.equals("I-" + neType)) {
				neTokens.add(t);
			} else if(neData.startsWith("B") || neData.startsWith("I")) {
				if(neType != null) neList.add(makeNE(neTokens, neType));
				neTokens = new ArrayList<Token>();
				neType = neData.substring(2);
				neTokens.add(t);
			} else {
				throw new Error();
			}
		}
		if(neType != null) neList.add(makeNE(neTokens, neType));
		return neList;
	}


	
}
