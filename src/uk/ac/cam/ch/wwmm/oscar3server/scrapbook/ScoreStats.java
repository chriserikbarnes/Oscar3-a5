package uk.ac.cam.ch.wwmm.oscar3server.scrapbook;

/**Holds information about precision and recall.
 * 
 * @author ptc24
 *
 */
public final class ScoreStats implements Cloneable {

	int falsepos;
	int falseneg;
	int matches;
	int matchesWithPunctuation;
	int falsenegWithPunctuation;
	int simpleFp;
	int simpleFn;
	
	public ScoreStats() {
		falsepos = 0;
		falseneg = 0;
		matches = 0;
		matchesWithPunctuation = 0;
		falsenegWithPunctuation = 0;
		simpleFp = 0;
		simpleFn = 0;
	}
	
	/**Gets the number of false negatives observed.
	 * 
	 * @return The number of false negatives observed.
	 */
	public int getFalseneg() {
		return falseneg;
	}
	
	/**Gets the number of false positives observed.
	 * 
	 * @return The number of false positives observed.
	 */
	public int getFalsepos() {
		return falsepos;
	}
	
	/**Gets the number of true positives observed.
	 * 
	 * @return The number of true positives observed.
	 */
	public int getMatches() {
		return matches;
	}
	
	/**Gets the number of true positives that contain punctuation.
	 * 
	 * @return The number of true positives that contain punctuation.
	 */
	public int getMatchesWithPunctuation() {
		return matchesWithPunctuation;
	}
	
	/**Gets the number of false positives that contain punctuation.
	 * 
	 * @return The number of false positives that contain punctuation.
	 */
	public int getFalsenegWithPunctuation() {
		return falsenegWithPunctuation;
	}
	
	void incMatches() {
		matches++;
	}
	
	void incFalsepos() {
		falsepos++;
	}
	
	void incFalseneg() {
		falseneg++;
	}

	void incMatchesWithPunctuation() {
		matchesWithPunctuation++;
	}
		
	void incFalsenegWithPunctuation() {
		falsenegWithPunctuation++;
	}
	
	void decFalsenegWithPunctuation() {
		falsenegWithPunctuation--;
	}
	
	void setFalsenegViaPossibleMatches(int possibleMatches) {
		falseneg = possibleMatches - matches;
	}
	
	/**Add all of the totals in another ScoreStats to those in this one.
	 * 
	 * @param ss The other ScoreStats.
	 */
	public void addScoreStats(ScoreStats ss) {
		falsepos += ss.falsepos;
		falseneg += ss.falseneg;
		matches += ss.matches;
		simpleFn += ss.simpleFn;
		simpleFp += ss.simpleFp;
		matchesWithPunctuation += ss.matchesWithPunctuation;
		falsenegWithPunctuation += ss.falsenegWithPunctuation;
	}
	
	/**Calculates precision as a percentage.
	 * 
	 * @return Precision.
	 */
	public float getPrecision() {
		return 100.0f * ((float)matches) / ((float)(matches + falsepos));
	}
	
	/**Calculates recall as a percentage.
	 * 
	 * @return Recall.
	 */
	public float getRecall() {
		return 100.0f * ((float)matches) / ((float)(matches + falseneg));
	}
	
	/**Calculates F (the harmonic average of precision and recall) as a
	 * percentage.
	 * @return F.
	 */
	public float getF() {
		return 100.0f * (2.0f * matches) / ((2.0f * matches) + falsepos + falseneg);
	}
	
	/**Produces a string that summarises precision and recall.
	 * 
	 * @return a string that summarises precision and recall.
	 */
	public String getPrecAndRecallString() {
		return "Precision:\t" + Float.toString(getPrecision()) +
		"\tRecall:\t"	+ Float.toString(getRecall()) +
		"\tF:\t"	+ Float.toString(getF()) +
			"\tTrue Positives:\t" + Integer.toString(matches) +
			"\tFalse Positives:\t" + Integer.toString(falsepos) +
			"\tFalse Negatives:\t" + Integer.toString(falseneg) +
			"\tsimpleFp:\t" + Integer.toString(simpleFp) +
			"\tsimpleFn:\t" + Integer.toString(simpleFn);
	}
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		ScoreStats ss = new ScoreStats();
		ss.falseneg = falseneg;
		ss.falsepos = falsepos;
		ss.matches = matches;
		return ss;
	}
		
}
