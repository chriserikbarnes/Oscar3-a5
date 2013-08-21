package uk.ac.cam.ch.wwmm.ptclib.cdk;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;

import nu.xom.Attribute;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Nodes;
import nu.xom.XPathContext;

import org.openscience.cdk.Atom;
import org.openscience.cdk.Bond;
import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.ChemFile;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.Molecule;
import org.openscience.cdk.config.IsotopeFactory;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IChemFile;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.io.CMLReader;
import org.openscience.cdk.io.CMLWriter;
import org.openscience.cdk.io.MDLReader;
import org.openscience.cdk.io.MDLWriter;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.HydrogenAdder;
import org.openscience.cdk.tools.ValencyHybridChecker;

import uk.ac.cam.ch.wwmm.ptclib.xml.XOMTools;

/** Routines for converting between various molecule structure formats.
 * 
 * @author ptc24
 *
 */
public final class StructureConverter {

	private static StructureConverter myInstance = null;
	static SmilesGenerator generator = new SmilesGenerator();
	ValencyHybridChecker checker; 
	HydrogenAdder hydrogenAdder; 

	private static StructureConverter getInstance() throws Exception {
		if(myInstance == null) myInstance = new StructureConverter();
		return myInstance;
	}
	
	private StructureConverter() throws Exception {
		checker = new ValencyHybridChecker();
		hydrogenAdder = new HydrogenAdder(checker);		
	}
	
	/** Initialises the singleton associated with this class. For convenience at startup.
	 */
	public static void init() throws Exception {
		getInstance();
	}
	
	/**Adds hydrogens for a molecule, and sets up the isotope information
	 * correctly.
	 * 
	 * @param mol The molecule to modify.
	 * @throws Exception
	 */
	public static void configureMolecule(IMolecule mol) throws Exception {
		configureMoleculeIsotopes(mol);
		getInstance().hydrogenAdder.addImplicitHydrogensToSatisfyValency(mol);
		addHydrogensForSilicon(mol);
	}
	
	/**Sets up the isotope information for a molecule.
	 * 
	 * @param mol The molecule to modify.
	 * @throws Exception
	 */
	public static void configureMoleculeIsotopes(IMolecule mol) throws Exception {
		IsotopeFactory isotopeFactory = IsotopeFactory.getInstance(DefaultChemObjectBuilder.getInstance());
		isotopeFactory.configureAtoms(mol);		
	}
	
	/**Converts a CML molecule to a CDK molecule.
	 * 
	 * @param cmlMol The CML molecule.
	 * @return The CDK molecule.
	 * @throws Exception
	 */
	public static IMolecule cmlToMolecule(Element cmlMol) throws Exception {
		ByteArrayInputStream bais = new ByteArrayInputStream(cmlMol.toXML().getBytes());
		IChemFile cf = (IChemFile) new CMLReader(bais).read(new ChemFile());
		IMolecule mol = cf.getChemSequence(0).getChemModel(0).getMoleculeSet().getMolecule(0);
		configureMolecule(mol);
		return mol;
	}
	
	static IMolecule cmlToMolecule(InputStream is) throws Exception {
		IChemFile cf = (IChemFile) new CMLReader(is).read(new ChemFile());
		IMolecule mol = cf.getChemSequence(0).getChemModel(0).getMoleculeSet().getMolecule(0);
		configureMolecule(mol);
		return mol;		
	}
	
	/**Converts a CML molecule to a SMILES string.
	 * 
	 * @param cmlMol The CML molecule.
	 * @return The SMILES string.
	 * @throws Exception
	 */
	public static String cmlToSMILES(Element cmlMol) throws Exception {
		return generator.createSMILES(cmlToMolecule(cmlMol));
	}
	
	/**Converts a CML molecule to an InChI string.
	 * 
	 * @param cmlMol The CML molecule.
	 * @return The InChI string.
	 * @throws Exception
	 */	public static String cmlToInChI(Element cmlMol) throws Exception {
		return ConverterToInChI.getInChI(cmlToMolecule(cmlMol));
	}
	
	 /**Converts a CDK molecule to CML, with details such as the name and InChI.
	  * 
	  * @param mol The CDK molecule.
	  * @param name The name of the compound (may be null).
	  * @return The CML molecule.
	  * @throws Exception
	  */
	public static Element molToCml(IMolecule mol, String name) throws Exception {
		deAromatise(mol);
		StringWriter output = new StringWriter();
		CMLWriter cmlw = new CMLWriter(output);
		cmlw.write(mol);
		Document doc = new Builder().build(output.toString(), "/localhost");
		Element e = (Element)doc.getRootElement();
		Nodes n = e.query(".//bond/@order");
		for(int i=0;i<n.size();i++) {
			Attribute a = (Attribute)n.get(i);
			if(a.getValue().equals("S")) a.setValue("1");
			else if(a.getValue().equals("D")) a.setValue("2");
			else if(a.getValue().equals("T")) a.setValue("3");
		}
		
		enhanceCMLMolecule(mol, e, name);
		
		Element cml = new Element("cml");
		e = new Element(e);
		cml.appendChild(e);
		XOMTools.setNamespaceURIRecursively(cml, "http://www.xml-cml.org/schema");
		return cml;
	}
	
	 /**Converts a CDK molecule to CML, without enhancements.
	  * 
	  * @param mol The CDK molecule.
	  * @return The CML molecule.
	  * @throws Exception
	  */
	public static Element simpleMolToCml(IMolecule mol) throws Exception {
		//deAromatise(mol);
		StringWriter output = new StringWriter();
		CMLWriter cmlw = new CMLWriter(output);
		cmlw.write(mol);
		Document doc = new Builder().build(output.toString(), "/localhost");
		Element e = (Element)doc.getRootElement();
		Nodes n = e.query(".//bond/@order");
		for(int i=0;i<n.size();i++) {
			Attribute a = (Attribute)n.get(i);
			if(a.getValue().equals("S")) a.setValue("1");
			else if(a.getValue().equals("D")) a.setValue("2");
			else if(a.getValue().equals("T")) a.setValue("3");
		}
		
		//enhanceCMLMolecule(mol, e, name);
		
		Element cml = new Element("cml");
		e = new Element(e);
		cml.appendChild(e);
		XOMTools.setNamespaceURIRecursively(cml, "http://www.xml-cml.org/schema");
		return cml;
	}
	
	/*public static String molToMDL(IMolecule mol) {
		try	{
			StringWriter sw = new StringWriter();
			MDLWriter mdlw = new MDLWriter(sw);
			mdlw.dontWriteAromatic();
			mdlw.write(mol);
			return sw.toString();
		} catch (Exception e) {
			return null;
		}
	}*/
	
	/*public static IMolecule MDLToMol(String mdl) {
		try {
			StringReader sr = new StringReader(mdl);
			MDLReader mr = new MDLReader(sr);
			Molecule mol = (Molecule)mr.read(new Molecule());
			return mol;
		} catch(Exception e) {
			return new Molecule();
		}
	}*/
	
	/**Adds name and InChI information to a CML molecule. The InChI is generated
	 * from the CML.
	 *
	 * @param cmlMolElem The CML molecule.
	 * @param name The molecule name.
	 * 
	 */
	public static void enhanceCMLMolecule(Element cmlMolElem, String name) throws Exception {
		if(cmlMolElem.getLocalName().equals("cml")) {
			cmlMolElem = cmlMolElem.getFirstChildElement("molecule", "http://www.xml-cml.org/schema");
		}
		IMolecule mol = cmlToMolecule(cmlMolElem);
		//for(int i=0;i<mol.getAtomCount();i++) {
		//	System.out.println(mol.getAtom(i));
		//}
		StructureConverter.configureMolecule(mol);
		enhanceCMLMolecule(mol, cmlMolElem, name);
		XOMTools.setNamespaceURIRecursively(cmlMolElem, "http://www.xml-cml.org/schema");
	}
	
	/**Adds name and InChI information to a CML molecule.
	 * 
	 * @param mol The CDK molecule used to generate the InChI.
	 * @param cmlMolElem The CML molecule.
	 * @param name The molecule name.
	 */
	private static void enhanceCMLMolecule(IMolecule mol, Element cmlMolElem, String name) {
		String inchi = ConverterToInChI.getInChI(mol);
		if(inchi != null) {
			Element identifier = new Element("identifier");
			identifier.addAttribute(new Attribute("convention", "iupac:inchi"));
			identifier.appendChild(inchi);
			cmlMolElem.insertChild(identifier, 0);
		}
		
		if(name != null) {
			Element nameElem = new Element("name");
			nameElem.appendChild(name);
			cmlMolElem.insertChild(nameElem, 0);
		}
		
		Nodes n = cmlMolElem.query(".//cml:label", new XPathContext("cml", "http://www.xml-cml.org/schema"));
		for(int i=0;i<n.size();i++) n.get(i).detach();
	}
	
	private static void deAromatise(IMolecule mm) {
		for(int i=0;i<mm.getBondCount();i++) {
			IBond b = mm.getBond(i);
			b.setOrder(b.getOrder());
		}
		for(int i=0;i<mm.getBondCount();i++) {
			IBond b = mm.getBond(i);
			b.setFlag(CDKConstants.ISAROMATIC, false);
		}
		for(int i=0;i<mm.getAtomCount();i++) {
			IAtom a = mm.getAtom(i);
			a.setHybridization(0);
			a.setFlag(CDKConstants.ISAROMATIC, false);
		}
	}
	
	/*public static Molecule deAromatise2(Molecule mm) throws Exception {
		StringWriter sw = new StringWriter();
		MDLWriter mdlw = new MDLWriter(sw);
		mdlw.dontWriteAromatic();
		mdlw.write(mm);
		StringReader sr = new StringReader(sw.toString());
		MDLReader mr = new MDLReader(sr);
		Molecule mmm = (Molecule)mr.read(new Molecule());
		return mmm;
	}*/

	/*public static IMolecule reAromatise(IMolecule mm) throws Exception {
		try {
			SmilesGenerator sg = new SmilesGenerator();
			SmilesParser sp = new SmilesParser(DefaultChemObjectBuilder.getInstance());
			return sp.parseSmiles(sg.createSMILES(mm)); 
		} catch (InvalidSmilesException e) {
			return mm;
		}
	}*/
	
	static void addHydrogensForSilicon(IMolecule mol) {
		IsotopeFactory isotopeFactory;
		try {
			isotopeFactory = IsotopeFactory.getInstance(DefaultChemObjectBuilder.getInstance());			
		} catch (Exception e) {
			throw new Error(e);
		}
		for(int i=0;i<mol.getAtomCount();i++) {
			IAtom a = mol.getAtom(i);
			if(!a.getSymbol().equals("Si")) continue;
			int c = (int)mol.getBondOrderSum(a);
			if(a.getFormalCharge() == 1 || a.getFormalCharge() == -1) c += 1;
			int hr = 4 - c;
			for(int j=0;j<hr;j++) {
				IAtom ha = new Atom("H");
				isotopeFactory.configure(ha);
				IBond b = new Bond(a, ha);
				mol.addAtom(ha);
				mol.addBond(b);
			}
		}
	}
}

