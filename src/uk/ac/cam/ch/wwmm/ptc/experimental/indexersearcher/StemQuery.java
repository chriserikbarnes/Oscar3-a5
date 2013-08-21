package uk.ac.cam.ch.wwmm.ptc.experimental.indexersearcher;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.FilteredTermEnum;
import org.apache.lucene.search.MultiTermQuery;

import uk.ac.cam.ch.wwmm.ptclib.misc.Stemmer;

public class StemQuery extends MultiTermQuery {

	Term searchTerm;
	Stemmer stemmerTools;
	
	public StemQuery(Term term, Stemmer stemmerTools) {
		super(term);
		searchTerm = term;
		this.stemmerTools = stemmerTools;
	}
	
	@Override
	protected FilteredTermEnum getEnum(IndexReader ir) throws IOException {
		return new StemTermEnum(ir, searchTerm, stemmerTools);
	}
	

	
	
}
