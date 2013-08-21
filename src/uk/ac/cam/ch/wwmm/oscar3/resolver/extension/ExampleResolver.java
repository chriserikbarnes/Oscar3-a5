package uk.ac.cam.ch.wwmm.oscar3.resolver.extension;

import java.util.List;


/**An example to how to use the ExtensionNameResolver interface. To use this
 * example, set the "extensionNameResolver"
 * property in the Oscar3 properties file to 
 * "uk.ac.cam.ch.wwmm.oscar3.resolver.extension.ExampleResolver".<br><br>
 * Note that this class is only an example, showing how to use the mechanism.
 * In particular, there is no such chemical as "methylfoobarane" - that name
 * was merely chosen to provide an example that can be tested.<br><br>
 * Note that this example actually contains code that directly resolves
 * chemical names! This may not be the best approach to take when writing
 * extensions; it would be better to write your core name resolution logic
 * in classes that do not implement this interface (as that allows you much more
 * control over the initialisation of those classes etc.), and to use the
 * class that implements the ExtensionNameResolver as a simple wrapper. You
 * may use the getExtensionNameResolver() method of NameResolver to get the
 * freshly-created class - this may be useful during initialisation as a way
 * of feeding it the information it needs to work. 
 * 
 * @author ptc24
 *
 */
public class ExampleResolver implements ExtensionNameResolver {

	/*There's no explicit constructor here. The implict constructor, equivalent
	 * to:
	 * 
	 * public ExampleResolver() {
	 * 
	 * }
	 * 
	 */
	
	public Results resolve(String name, List<String> args) {
		if(name.toLowerCase().matches("methylfoobarane")) {
			return new Results("C1=CC=CC=C1", "InChI=1/C6H6/c1-2-4-6-5-3-1/h1-6H");
		} else {
			return null;
		}
	}

}
