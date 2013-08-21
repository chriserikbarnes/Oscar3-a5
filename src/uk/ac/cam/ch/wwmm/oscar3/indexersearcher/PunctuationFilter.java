package uk.ac.cam.ch.wwmm.oscar3.indexersearcher;

import java.io.IOException;

import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;

/**Removes punctuation during indexing.
 * 
 * @author ptc24
 *
 */
final class PunctuationFilter extends TokenFilter {

	PunctuationFilter(TokenStream in) {
		super(in);
	}
	
	@Override
	public Token next() throws IOException {
		return next(new Token());
	}
	
	@Override
	public Token next(Token token) throws IOException {
		boolean readNext = true;
		Token t = null;
		while(readNext) {
			t = input.next(token);
			if (t == null) return null;
			readNext = !t.term().matches(".*[a-zA-Z0-9].*");
		}
		return t;
	}
	
}
