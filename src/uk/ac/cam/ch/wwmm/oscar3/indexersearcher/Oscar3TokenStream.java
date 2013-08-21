package uk.ac.cam.ch.wwmm.oscar3.indexersearcher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;

import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.TokenSequence;

/**A Lucene wrapper for Oscar3 token sequences.
 * 
 * @author ptc24
 *
 */
final class Oscar3TokenStream extends TokenStream {

	private TokenSequence tokSeq;
	private List<TokenSequence> tokSeqs;
	private int place;
	private int tokSeqPlace;

	Oscar3TokenStream(TokenSequence tokSeq) {
		this.tokSeqs = new ArrayList<TokenSequence>();
		tokSeqs.add(tokSeq);
		tokSeqPlace = 0;
		place = 0;
		
	}
	
	Oscar3TokenStream(List<TokenSequence> tokSeqs) {
		this.tokSeqs = tokSeqs;
		tokSeqPlace = 0;
		place = 0;
	}
	
	@Override
	public Token next(Token token) throws IOException {
		if(token == null) token = new Token();
		if(tokSeqPlace == tokSeqs.size()) return null;
		tokSeq = tokSeqs.get(tokSeqPlace);
		while(place >= tokSeq.size()) {
			place = 0;
			tokSeqPlace++;
			if(tokSeqPlace == tokSeqs.size()) return null;
			tokSeq = tokSeqs.get(tokSeqPlace);
		}
		uk.ac.cam.ch.wwmm.oscar3.recogniser.document.Token oscarToken = tokSeq.getToken(place);
		place++;
		return token.reinit(oscarToken.getValue(), oscarToken.getStart(), oscarToken.getEnd());
	}

	@Override
	public Token next() throws IOException {
		return next(new Token());
	}

}
