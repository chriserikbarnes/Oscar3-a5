package uk.ac.cam.ch.wwmm.oscar3.indexersearcher;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.BooleanClause.Occur;

import uk.ac.cam.ch.wwmm.oscar3.terms.OBOOntology;

/**An extension of the Lucene QueryParser that detects chemical and ontology
 * search terms, translating and expanding them as appropriate.
 * 
 * @author ptc24
 *
 */
public final class Oscar3QueryParser extends QueryParser {

	private LuceneIndexerSearcher lis;
	private boolean strict;
	
	private Pattern similarPattern = Pattern.compile("(\\d+)\\s+(like|similar to)\\s+(\\S+.*)");
	private Pattern numberPattern = Pattern.compile("(\\d+)");
	private Pattern inchiPattern = Pattern.compile("(?<!\")InChI=\\S+(?!\")");
	
	public Oscar3QueryParser(String arg0, Analyzer arg1, LuceneIndexerSearcher lis, boolean strict) {
		super(arg0, arg1);
		this.lis = lis;
		this.strict = strict;
		setDefaultOperator(QueryParser.AND_OPERATOR);
		setAllowLeadingWildcard(true);
		setLowercaseExpandedTerms(false);
	}

	@Override
	public Query parse(String query) throws ParseException {
		if(strict) return super.parse(query);
		Matcher m = similarPattern.matcher(query);
		if(m.matches()) {
			Query q = expandTerm(query);
			if(q == null) return new BooleanQuery();
			return q;
		} else {
			m = inchiPattern.matcher(query);
			while(m.find()) {
				query = query.substring(0, m.start()) + "\"" + m.group() + "\"" + query.substring(m.end());
				m = inchiPattern.matcher(query);				
			}
			Query q = super.parse(query);
			return q;
		}
	}
	
	@Override
	protected Query getFieldQuery(String field, String queryText, int slop) throws ParseException {
		if(strict) return super.getFieldQuery(field, queryText, slop);
		try {
			if(queryText.matches(".*InChI=.*")) {
				return new TermQuery(new Term("InChI", queryText));
			} else if(field.equals("txt")) {
				Query ontQuery = expandTerm(queryText);
				Query q = super.getFieldQuery(field, queryText, slop);
				q = mergeQueries(q, ontQuery);
				return q;
			} else if(field.toLowerCase().startsWith("smiles")) {
				String fieldEnd = field.toLowerCase().substring(6);
				if(fieldEnd.equals("")) {
					return lis.makeChemQueryFromSMILES(queryText, "exact", "").getLuceneQuery();
				} else if(fieldEnd.startsWith("sub")) {
					return lis.makeChemQueryFromSMILES(queryText, "substructure", "").getLuceneQuery();
				} else if(fieldEnd.matches("\\d+(sim(ilar)|like)")) {
					Matcher m = numberPattern.matcher(fieldEnd);
					if(m.find()) {
						return lis.makeChemQueryFromSMILES(queryText, "similarity", m.group()).getLuceneQuery();						
					}
				}
			}			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new BooleanQuery();
	}
		
	@SuppressWarnings("unchecked")
	@Override
	protected void addClause(List clauses, int conj, int mods, Query q) {
		if(strict) {
			super.addClause(clauses, conj, mods, q);
			return;
		}
		if(clauses.size() == 0) {
			clauses.add(new BooleanClause(new BooleanQuery(), BooleanClause.Occur.SHOULD));
		}
		if(q instanceof TermQuery) {
			Term t = ((TermQuery)q).getTerm();
			if(t.field().equals("txt")) {
				String queryText = t.text();
				Query ontQuery = expandTerm(queryText);
				q = mergeQueries(q, ontQuery);
			} 
		}
		super.addClause(clauses, conj, mods, q);
	}
	
	private Query mergeQueries(Query q1, Query q2) {
		if(q2 != null) {
			BooleanQuery bq = new BooleanQuery();
			bq.add(new BooleanClause(q2, Occur.SHOULD));
			bq.add(new BooleanClause(q1, Occur.SHOULD));
			q1 = bq;
		}
		return q1;
	}
	
	private Query expandTerm(String term) {
		BooleanQuery bq = new BooleanQuery();
		Set<String> myOntIDs = OBOOntology.getInstance().getIdsForTermWithDescendants(term);
		if(myOntIDs != null && myOntIDs.size() > 0) {
			OntologyQuery oq = new OntologyQuery(myOntIDs);
			bq.add(new BooleanClause(oq.getLuceneQuery(), Occur.SHOULD));
		}
		Matcher m = similarPattern.matcher(term);
		try {
			if(m.matches()) {
				String name = m.group(3);
				String smiles = lis.getSmilesFromName(name);
				ChemQuery cq = lis.makeChemQueryFromSMILES(smiles, "similarity", m.group(1));
				bq.add(new BooleanClause(cq.getLuceneQuery(), BooleanClause.Occur.SHOULD));
			} else {
				String inchi = lis.getInchiFromName(term);
				if(inchi != null) {
					bq.add(new BooleanClause(new TermQuery(new Term("InChI", inchi)), BooleanClause.Occur.SHOULD));
				}
			}			
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		if(bq.clauses().size() > 0) {
			return bq;
		} else {
			return null;
		}
	}

	
}
