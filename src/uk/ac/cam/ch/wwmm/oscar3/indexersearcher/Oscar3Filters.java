package uk.ac.cam.ch.wwmm.oscar3.indexersearcher;

import java.io.IOException;

import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.snowball.SnowballFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

final class Oscar3Filters extends TokenStream {

	TokenStream ts;
	
	Oscar3Filters(TokenStream ts) {
		if(false) ts = new StopFilter(ts, StandardAnalyzer.STOP_WORDS, true);
		ts = new Oscar3CaseFilter(ts);
		if(false) ts = new PunctuationFilter(ts);
		if(false) ts = new SnowballFilter(ts, "Porter");
		this.ts = ts;
	}
	
	@Override
	public Token next(Token token) throws IOException {
		return ts.next(token);
	}
	
	@Override
	public Token next() throws IOException {
		return next(new Token());
	}
	
}
