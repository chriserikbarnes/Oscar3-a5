package uk.ac.cam.ch.wwmm.oscar3.indexersearcher;

import java.util.ArrayList;
import java.util.List;

/** A Query submitted by users of indexersearcher. You make one of these, and then
 * add terms to it. This gets translated into a big BooleanQuery, with the aid of
 * various parts of the code that transform chemical (and maybe later, ontology-based)
 * queries into Queries that Lucene understands.
 * 
 * @author ptc24
 *
 */
public final class UserQuery {

	/**An enum to store the type of results page requested by the user.
	 * 
	 * @author ptc24
	 *
	 */
	public enum ResultsType {
		SNIPPETS,
		COMPOUNDSLIST,
		HITSLIST,
		ASSOC
	};

	List<UserQueryElement> queryElems;
	ResultsType rt;
	int size;
	int skip;
	boolean moreLikeThis;
	int docId;
	
	/**Copy a UserQuery, switching to a different ResultsType.
	 * 
	 * @param uq The UserQuery to copy.
	 * @param r The new ResultsType.
	 */
	public UserQuery(UserQuery uq, ResultsType r) {
		this.rt = r;
		this.queryElems = new ArrayList<UserQueryElement>(uq.queryElems);
		this.size = uq.size;
		this.skip = 0;
		this.moreLikeThis = false;
		this.docId = -1;
	}
	
	/**Construct a new, empty UserQuery.
	 * 
	 * @param r The results type.
	 * @param size The number of results to get.
	 * @param skip The number of results to skip.
	 */
	public UserQuery(ResultsType r, int size, int skip) {
		rt = r;
		queryElems = new ArrayList<UserQueryElement>();
		this.size = size;
		this.skip = skip;
		moreLikeThis = false;
		docId = -1;
	}
	
	/**Makes this a MoreLikeThis query.
	 * 
	 * @param docId The document ID to find similar documents to.
	 */
	public void setToMoreLikeThis(int docId) {
		moreLikeThis = true;
		this.docId = docId;
	}
	
	/**Adds a search term to the query.
	 * 
	 * @param queryStr The query string.
	 * @param queryType The query type.
	 * @param parameter A parameter (irrelevant for most query types.
	 */
	public void addTerm(String queryStr, String queryType, String parameter) {
		queryElems.add(new UserQueryElement(queryStr, queryType, parameter));
	}
	
	/**Constructs the last part of a URL that describes this query.
	 * 
	 * @param newSkip The new number of documents to skip.
	 * @param newSize The new number of documents to fetch.
	 * @return The last part of the URL.
	 */
	public String getQueryURL(int newSkip, int newSize) {
		return getQueryURL(newSkip, newSize, false, 0);
	}
		
	private String getQueryURL(int newSkip, int newSize, boolean mlt, int myDocId) {
		String s =  "Search?resultsType=";
		switch(rt) {
		case SNIPPETS:
			s += "snippets";
			break;
		case COMPOUNDSLIST:
			s += "compoundsList";
			break;
		case HITSLIST:
			s += "hitsList";
			break;
		case ASSOC:
			s += "assoc";
			break;
		}
		for(int i=0;i<queryElems.size();i++) {
			s += queryElems.get(i).getQueryURL(i+1);
		}
		if(mlt) {
			s+= "&morelikethis=" + Integer.toString(myDocId);
		} else {
			if(moreLikeThis) s+= "&morelikethis=" + Integer.toString(docId);
		}
		if(newSize != 5) s+= "&size=" + Integer.toString(newSize);
		if(newSkip != 0) s+= "&skip=" + Integer.toString(newSkip);
		return s;
	}
	
	/**Gets the number of documents requested.
	 * 
	 * @return The number of documents requested.
	 */
	public int getSize() {
		return size;
	}
	
}
