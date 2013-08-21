package uk.ac.cam.ch.wwmm.oscar3.deployment;

import java.io.File;

import uk.ac.cam.ch.wwmm.oscar3.Oscar3Props;
import uk.ac.cam.ch.wwmm.ptclib.io.ResourceGetter;

/**Classes for deploying files contained on the classpath into the current working directory.
 * 
 * @author ptc24
 *
 */
public final class Deployment {
	
	/**Selects which InChI executable file is appropriate for the platform,
	 * deploys it in the file system and updates the Oscar3 properties
	 * appropriately.
	 * 
	 * @throws Exception
	 */
	public static void deployInChI() throws Exception {
		ResourceGetter rg = new ResourceGetter("uk/ac/cam/ch/wwmm/oscar3/deployment/resources/");
		File inchiFile = null;
		if(System.getProperty("os.name").equals("Linux")) {
			inchiFile = new File("cInChI-1");
			rg.writeToFile("cInChI-1.bin", inchiFile);
			Runtime.getRuntime().exec("chmod a+x " + inchiFile.getAbsolutePath());
		} else if(System.getProperty("os.name").startsWith("Windows")) {
			inchiFile = new File("cInChI-1.exe");
			rg.writeToFile("cInChI-1.exe", inchiFile);
		} 
		if(inchiFile != null) {
			Oscar3Props.setProperty("InChI", inchiFile.getAbsolutePath());
		} else {
			Oscar3Props.setProperty("InChI", "none");
		}
	
		Oscar3Props.saveProperties();
	}

}
