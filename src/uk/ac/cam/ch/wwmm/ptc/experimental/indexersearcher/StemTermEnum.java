package uk.ac.cam.ch.wwmm.ptc.experimental.indexersearcher;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.FilteredTermEnum;

import uk.ac.cam.ch.wwmm.ptclib.misc.Stemmer;

public class StemTermEnum extends FilteredTermEnum {

	Term searchTerm;
	Stemmer stemmerTools;
	String searchStem;
	IndexReader indexReader;
	
	public StemTermEnum(IndexReader ir, Term term, Stemmer st) throws IOException {
		searchTerm = term;
		stemmerTools = st;
		searchStem = stemmerTools.getStem(searchTerm.text());
	    setEnum(ir.terms());
	}
	
	@Override
	protected boolean termCompare(Term term) {
		if(!term.field().equals(searchTerm.field())) return false;
		return stemmerTools.getStem(term.text()).equals(searchStem);
	}

	@Override
	public float difference() {
		// TODO Auto-generated method stub
		return 1.0f;
	}

	@Override
	protected boolean endEnum() {
		// TODO Auto-generated method stub
		return false;
	}

}
