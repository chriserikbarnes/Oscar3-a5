package uk.ac.cam.ch.wwmm.oscar3.resolver.extension;

import java.util.List;

/**An interface for extending the name resolution capabilities of Oscar3. This
 * could potentially be used to write adaptors for external name-to-structure
 * converters, or to access databases other than PubChem.
 * <br><br>
 * To use this interface, produce a class that implements this interface. This
 * class should have a public constructor that takes no arguments (an
 * implicit public constructor will do, as in the ExampleResolver class). Then 
 * ensure
 * that your class is on the classpath, and set the "extensionNameResolver"
 * property in the Oscar3 properties file.<br><br>
 * 
 * An example, is given in the form of the ExampleResolver class, in this
 * package. The example is accompanied by some advice.
 * 
 * @author ptc24
 *
 */
public interface ExtensionNameResolver {

	/**Get the SMILES and InChI strings for a chemical name.
	 * 
	 * @param name The chemical name to query.
	 * @param args Arguments from OscarFlow.
	 * @return The SMILES and InChI strings, or null.
	 */
	public Results resolve(String name, List<String> args);
	
}
