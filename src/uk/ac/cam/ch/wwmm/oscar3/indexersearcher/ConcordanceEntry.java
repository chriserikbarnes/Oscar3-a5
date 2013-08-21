package uk.ac.cam.ch.wwmm.oscar3.indexersearcher;

/**An entry for a Concordance.
 * 
 * @author ptc24
 *
 */
public final class ConcordanceEntry {

	/**The filename the entry was taken from*/
	public String file;
	/**The start offset of the key word*/
	public String start;
	/**The end offset of the key word*/
	public String end;
	/**The string to display, showing the key word and the context*/
	public String text;
	
}
