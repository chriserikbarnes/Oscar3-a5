package uk.ac.cam.ch.wwmm.oscar3.indexersearcher;

import java.io.IOException;

import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;

import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;

/**Performs case-normalisation during indexing.
 * 
 * @author ptc24
 *
 */
final class Oscar3CaseFilter extends TokenFilter {

	Oscar3CaseFilter(TokenStream in) {
		super(in);
	}
	
	@Override
	public Token next(Token token) throws IOException {
		Token t = input.next(token);
		if (t == null) return null;
		t.setTermBuffer(StringTools.normaliseName2(t.term()));
		return t;
	}

	@Override
	public Token next() throws IOException {
		return next(new Token());
	}
}
