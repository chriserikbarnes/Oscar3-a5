package uk.ac.cam.ch.wwmm.oscar3server.scrapbook;

import uk.ac.cam.ch.wwmm.ptclib.scixml.SciXMLDocument;

/**Holds the results of a test on a paper - a SciXMLDocument and a ScoreStats.
 * 
 * @author ptc24
 *
 */
public final class RegTestResults {

	private SciXMLDocument doc;
	private ScoreStats scoreStats;
	
	RegTestResults(SciXMLDocument doc, ScoreStats scoreStats) {
		this.doc = doc;
		this.scoreStats = scoreStats;
	}
	
	/**Gets the document that displays the regression test results.
	 * 
	 * @return The document that displays the regression test results.
	 */ 
	public SciXMLDocument getDoc() {
		return doc;
	}
	
	/**Gets the ScoreStats object that contains the results.
	 * 
	 * @return The ScoreStats object that contains the results.
	 */
	public ScoreStats getScoreStats() {
		return scoreStats;
	}
	
}
