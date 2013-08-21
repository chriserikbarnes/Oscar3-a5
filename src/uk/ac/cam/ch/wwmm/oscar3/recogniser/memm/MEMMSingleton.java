package uk.ac.cam.ch.wwmm.oscar3.recogniser.memm;

import java.io.File;
import java.util.List;

import nu.xom.Element;
import uk.ac.cam.ch.wwmm.oscar3.models.Model;

/**A MEMM singleton, for access by other components.
 * 
 * @author ptc24
 *
 */
public class MEMMSingleton {

	private static MEMM memm;

	/**Initialise the singleton, by loading the MEMM from an XML Element.
	 * 
	 * @param elem
	 */
	public static void load(Element elem) {
		try {
			memm = new MEMM();
			memm.readModel(elem);
		} catch (Exception e) {
			throw new Error(e);
		}
	}
	
	/**Remove the MEMM singleton.
	 * 
	 */
	public static void clear() {
		memm = null;
	}
	
	/**Train a new MEMM, based on a list of ScrapBook files, and make it
	 * be the new singleton.
	 * 
	 * @param files The files to use as training data.
	 * @param rescore Whether to train a rescorer as well as a MEMM.
	 */
	public static void train(List<File> files, boolean rescore) {
		try {
			memm = new MEMM();
			if(rescore) {
				memm.trainOnSbFilesWithRescore(files, null);
			} else {
				memm.trainOnSbFiles(files, null);
			}
		} catch (Exception e) {
			throw new Error(e);
		}
	}
	
	/**Gets the MEMM singleton instance, loading if necessary.
	 * 
	 * @return The MEMM singleton.
	 */
	public static MEMM getInstance() {
		if(memm == null) {
			Model.loadModel();
		}
		return memm;
	}

}
