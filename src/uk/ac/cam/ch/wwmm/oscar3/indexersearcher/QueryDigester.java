package uk.ac.cam.ch.wwmm.oscar3.indexersearcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.index.TermPositionVector;
import org.apache.lucene.index.TermVectorOffsetInfo;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.BooleanClause.Occur;

public class QueryDigester {

	Set<Term> simpleTerms;
	List<List<Term>> phraseTerms;
	
	public QueryDigester(Query q) {
		simpleTerms = new HashSet<Term>();
		phraseTerms = new ArrayList<List<Term>>();
		if(q instanceof BooleanQuery) {
			BooleanClause [] clauses = ((BooleanQuery)q).getClauses();
			for(int i=0;i<clauses.length;i++) {
				if(!clauses[i].getOccur().equals(Occur.MUST_NOT)) {
					QueryDigester qd = new QueryDigester(clauses[i].getQuery());
					simpleTerms.addAll(qd.simpleTerms);
					phraseTerms.addAll(qd.phraseTerms);
				}
			}
		} else if(q instanceof PhraseQuery) {
			PhraseQuery pq = (PhraseQuery)q;
			if(pq.getSlop() == 0) {
				List<Term> phraseTerm = new ArrayList<Term>();
				for(int i=0;i<pq.getTerms().length;i++) {
					phraseTerm.add(pq.getTerms()[i]);
				}
				phraseTerms.add(phraseTerm);
			} else {
				for(int i=0;i<pq.getTerms().length;i++) {
					simpleTerms.add(pq.getTerms()[i]);
				}
			}
		} else if(q instanceof TermQuery) {
			simpleTerms.add(((TermQuery)q).getTerm());
		}
	}
	
	public List<TermVectorOffsetInfo> getOffsets(IndexReader ir, int docNo, String fieldName) throws Exception {
		Set<String> ptt = new HashSet<String>();
		for(List<Term> phraseTerm : phraseTerms) {
			for(Term t : phraseTerm) {
				if(t.field().equals(fieldName)) ptt.add(t.text());
			}
		}
		
		List<TermVectorOffsetInfo> results = new ArrayList<TermVectorOffsetInfo>();
		TermFreqVector tfv = ir.getTermFreqVector(docNo, fieldName);
		Map<String,Integer> termToID = new HashMap<String,Integer>();
		if(tfv instanceof TermPositionVector) {
			TermPositionVector tpv = (TermPositionVector)tfv;
			String [] terms = tpv.getTerms();
			for(int i=0;i<tpv.getTerms().length;i++) {
				if(simpleTerms.contains(new Term(fieldName, terms[i]))) {
					//System.out.println(terms[i]);
					TermVectorOffsetInfo[] tps = tpv.getOffsets(i);
					int[] positions = tpv.getTermPositions(i);
					for(int j=0;j<tps.length;j++) {
						results.add(tps[j]);
						//System.out.println(terms[i] + "\t" + positions[j] + "\t" + tps[j].getStartOffset() + "\t" + tps[j].getEndOffset());
					}					
				}
				if(ptt.contains(terms[i])) {
					termToID.put(terms[i], i);
				}
			}
			for(List<Term> phraseTerm : phraseTerms) {
				String firstWord = phraseTerm.get(0).text();
				if(termToID.containsKey(firstWord)) {
					//System.out.println(firstWord);
					Map<Integer,List<TermVectorOffsetInfo>> phrases = new HashMap<Integer,List<TermVectorOffsetInfo>>();
					int id = termToID.get(firstWord);
					TermVectorOffsetInfo[] tps = tpv.getOffsets(id);
					int[] positions = tpv.getTermPositions(id);
					for(int i=0;i<positions.length;i++) {
						List<TermVectorOffsetInfo> tvoil = new ArrayList<TermVectorOffsetInfo>();
						tvoil.add(tps[i]);
						phrases.put(positions[i], tvoil);
						//System.out.println(firstWord + "\t" + positions[i]);
					}
					for(int i=1;i<phraseTerm.size();i++) {
						String word = phraseTerm.get(i).text();
						if(!termToID.containsKey(word)) {
							phrases = null;
							break;
						}
						int cid = termToID.get(word);
						TermVectorOffsetInfo[] ctps = tpv.getOffsets(cid);
						int[] cpositions = tpv.getTermPositions(cid);
						for(int j=0;j<cpositions.length;j++) {
							//System.out.println(word + "\t" + cpositions[j]);
							if(phrases.containsKey(cpositions[j] - i)) {
								//System.out.println(phrases.get(cpositions[j] - i));
								phrases.get(cpositions[j] - i).add(ctps[j]);
							}
						}
					}
					if(phrases == null) continue;
					for(Integer i : phrases.keySet()) {
						List<TermVectorOffsetInfo> phrase = phrases.get(i);
						if(phrase.size() == phraseTerm.size()) {
							for(int j=0;j<phrase.size();j++) {
								//System.out.println(phraseTerm.get(j).text() + "\t" + phrase.get(j).getStartOffset() + "\t" + phrase.get(j).getEndOffset());
								results.addAll(phrase);
							}
						}
					}
				}
			}
		}
		return results;
	}
	@Override
	public String toString() {
		return "[simpleTerms: " + simpleTerms + ", phraseTerms: " + phraseTerms + "]";
	}
	
	
	
}
