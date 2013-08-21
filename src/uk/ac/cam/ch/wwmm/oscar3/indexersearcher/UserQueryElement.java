package uk.ac.cam.ch.wwmm.oscar3.indexersearcher;

import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;

/** A data structure to hold a term in a UserQuery.
 *
 * @author ptc24
 *
 */

final class UserQueryElement {

	String queryStr;
	String queryType;
	String parameter;
	
	UserQueryElement(String queryStr, String queryType, String parameter) {
		this.queryStr = queryStr;
		this.queryType = queryType;
		this.parameter = parameter;
	}
	
	String getQueryURL(int i) {
		String qnum = "";
		if(i > 1) qnum = Integer.toString(i);
		return "&type" + qnum + "=" + queryType + "&query" + qnum + "=" + StringTools.urlEncodeUTF8NoThrow(queryStr) + "&parameter" + qnum + "=" + parameter;
	}
}
