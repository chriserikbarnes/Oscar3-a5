package uk.ac.cam.ch.wwmm.oscar3.indexersearcher;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;

public class NETokenStream extends TokenStream {

	Queue<Token> tokenQueue;
	
	public NETokenStream() {
		tokenQueue = new LinkedList<Token>();
	}
	
	public void addToken(String val, int startOffset, int endOffset) {
		Token t = new Token();
		t.reinit(val, startOffset, endOffset);
		tokenQueue.add(t);
	}
	
	@Override
	public Token next(Token token) throws IOException {
		if(tokenQueue.size() == 0) return null;
		if(token == null) token = new Token();
		Token fromQueue = tokenQueue.poll();
		token.reinit(fromQueue.term(), fromQueue.startOffset(), fromQueue.endOffset());
		return token;
	}
	
	@Override
	public Token next() throws IOException {
		// TODO Auto-generated method stub
		return next(new Token());
	}
	
}
