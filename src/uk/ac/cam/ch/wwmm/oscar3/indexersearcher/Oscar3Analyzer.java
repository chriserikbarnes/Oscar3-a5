package uk.ac.cam.ch.wwmm.oscar3.indexersearcher;

import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;

import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.TokenSequence;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.Tokeniser;

/** A Lucence wrapper which combines various filters with a wrapper for the
 * Oscar3 tokeniser.
 * 
 * @author ptc24
 *
 */
public final class Oscar3Analyzer extends Analyzer {

	@Override
	public TokenStream tokenStream(String arg0, Reader reader) {
		StringBuffer sb = new StringBuffer();
		try {
			int c = reader.read();
			while(c != -1) {
				sb.append((char)c);
				c = reader.read();
			}			
		} catch (Exception e) {
			throw new Error(e);
		}
		TokenSequence t = Tokeniser.getInstance().tokenise(sb.toString());
		
		TokenStream ts = new Oscar3TokenStream(t);
		ts = new Oscar3Filters(ts);
		return ts;
	}

}
