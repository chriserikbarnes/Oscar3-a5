package uk.ac.cam.ch.wwmm.ptc.experimental.indexersearcher;

import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.BooleanClause.Occur;

import uk.ac.cam.ch.wwmm.oscar3.indexersearcher.LuceneIndexerSearcher;
import uk.ac.cam.ch.wwmm.oscar3.terms.TermSets;
import uk.ac.cam.ch.wwmm.ptclib.datastruct.Bag;

public class SimpleTreeClusterer {

	static class TreeNode {
		Set<String> includeTerms;
		Set<String> excludeTerms;
		
		TreeNode() {
			includeTerms = new LinkedHashSet<String>();
			excludeTerms = new LinkedHashSet<String>();
		}
		
		TreeNode(TreeNode tn) {
			this.includeTerms = new LinkedHashSet<String>(tn.includeTerms);
			this.excludeTerms = new LinkedHashSet<String>(tn.excludeTerms);
		}
		
		TreeNode includeBranch(String include) {
			TreeNode tn = new TreeNode(this);
			tn.includeTerms.add(include);
			return tn;
		}
		
		TreeNode excludeBranch(String exclude) {
			TreeNode tn = new TreeNode(this);
			tn.excludeTerms.add(exclude);
			return tn;			
		}
		
		Query getQuery() {
			if(includeTerms.size() == 0 && excludeTerms.size() == 0) {
				return new MatchAllDocsQuery();
			} else {
				BooleanQuery bq = new BooleanQuery();
				if(includeTerms.size() == 0) {
					bq.add(new BooleanClause(new MatchAllDocsQuery(), Occur.MUST));
				}
				for(String s : includeTerms) {
					bq.add(new BooleanClause(new TermQuery(new Term("txt", s)), Occur.MUST));
				}
				for(String s : excludeTerms) {
					bq.add(new BooleanClause(new TermQuery(new Term("txt", s)), Occur.MUST_NOT));					
				}
				return bq;
			}
		}
		
		String getSplitTerm(IndexSearcher is) throws Exception {
			Hits h = is.search(getQuery());
			if(h.length() < 20) return null;
			IndexReader ir = is.getIndexReader();
			Bag<String> termBag = new Bag<String>();
			for(int i=0;i<h.length();i++) {
				TermFreqVector tv = ir.getTermFreqVector(h.id(i), "txt");
				String [] terms = tv.getTerms();
				for(int k=0;k<tv.size();k++) {
					String term = terms[k];
					if("In".equals(term)) continue;
					if(TermSets.getClosedClass().contains(term)) continue;
					if(!term.matches(".*[A-Za-z].*")) continue;
					if(!includeTerms.contains(term) && !excludeTerms.contains(term)) termBag.add(terms[k]);
				}
			}
			String bestTerm = null;
			int bestTermScore = 0;
			int ldiv = (int)Math.sqrt(h.length());
			for(String term : termBag.getSet()) {
				int score = termBag.getCount(term);
				if(score > ldiv) score = (2 * ldiv) - score;
				if(score > bestTermScore) {
					bestTerm = term;
					bestTermScore = score;
				}
			}
			//System.out.println(h.length() + "\t" + bestTermScore);
			return bestTerm;
		}
		
		void subCluster(IndexSearcher is) throws Exception {
			String splitTerm = getSplitTerm(is);
			if(splitTerm == null) {
				System.out.println(includeTerms + "\t" + excludeTerms);
			} else {
				includeBranch(splitTerm).subCluster(is);
				excludeBranch(splitTerm).subCluster(is);
			}
		}
		
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		LuceneIndexerSearcher lis = new LuceneIndexerSearcher(false);
		IndexSearcher is = lis.getIndexSearcher();

		TreeNode tn = new TreeNode();
		
		tn.subCluster(is);
		
		//tn = tn.includeBranch("In").includeBranch("study");
		//tn = tn.excludeBranch("In").excludeBranch("metabolite").excludeBranch("possible");
		
		//System.out.println(tn.getSplitTerm(is));
	}

}
