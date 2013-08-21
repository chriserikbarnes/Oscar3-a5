package uk.ac.cam.ch.wwmm.oscar3.resolver.extension;

import nu.xom.Element;

/**The results of looking up a name using an extension name resolver.
 * 
 * @author ptc24
 *
 */
public final class Results {

	private String smiles;
	private String inchi;
	private Element cml;
	
	/**Create a new results object with a given SMILES and InChI string.
	 * 
	 * @param smiles The SMILES string.
	 * @param inchi The InChI string.
	 */
	public Results(String smiles, String inchi) {
		this.smiles = smiles;
		this.inchi = inchi;
	}
	
	/**Create a new results object with a given SMILES, InChI and CML.
	 * 
	 * @param smiles The SMILES string.
	 * @param inchi The InChI string.
	 * @param cml CML for the object.
	 */
	public Results(String smiles, String inchi, Element cml) {
		this.smiles = smiles;
		this.inchi = inchi;
		this.cml = cml;
	}
	
	/**Gets the SMILES string.
	 * 
	 * @return The SMILES string.
	 */
	public String getSmiles() {
		return smiles;
	}
	
	/**Gets the InChI string.
	 * 
	 * @return The InChI string.
	 */
	public String getInchi() {
		return inchi;
	}
	
	/**Gets the CML elements.
	 * 
	 * @return The CML Element.
	 */
	public Element getCml() {
		return cml;
	}
 
	/**Sets the SMILES string.
	 * 
	 * @param smiles The SMILES string.
	 */
	public void setSmiles(String smiles) {
		this.smiles = smiles;
	}
	
	/**Sets the InChI string.
	 * 
	 * @param inchi The InChI string.
	 */
	public void setInchi(String inchi) {
		this.inchi = inchi;
	}
	
	/**Sets the CML element.
	 * 
	 * @param cml The CML element.
	 */
	public void setCml(Element cml) {
		this.cml = cml;
	}
	
	/**Gets a representation of the SMILES/INCHI pair for debugging purposes.
	 * 
	 */
	@Override
	public String toString() {
		return "{" + smiles + ", " + inchi + "}";
	}
	
}
