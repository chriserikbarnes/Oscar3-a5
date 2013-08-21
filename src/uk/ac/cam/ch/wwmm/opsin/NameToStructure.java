package uk.ac.cam.ch.wwmm.opsin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import uk.ac.cam.ch.wwmm.ptclib.io.ResourceGetter;
import uk.ac.cam.ch.wwmm.ptclib.xml.XOMFormatter;

import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Serializer;

/** The "master" class, to turn a name into a structure.
 *
 * @author ptc24
 *
 */
public class NameToStructure {

	class SortParses implements Comparator<Element>{
		public int compare(Element el1, Element el2){
			int elementsInEl1 = XQueryUtil.xquery(el1, ".//*").size();
			int elementsInEl2 = XQueryUtil.xquery(el2, ".//*").size();
			if ( elementsInEl1> elementsInEl2){
				return 1;
			}
			else if (elementsInEl1 < elementsInEl2){
				return -1;
			}
			else{
				return 0;
			}
		}
	}


	/**Does finite-state non-destructive parsing on chemical names.*/
	Parser parser;
	/**Does destructive procedural parsing on parser results.*/
	PostProcessor postProcessor;
	/**Does structure-aware destructive procedural parsing on parser results.*/
	PreStructureBuilder preStructureBuilder;
	/**Constructs the CML molecule from the postprocessor results.*/
	StructureBuilder structureBuilder;
	/**Constructs fused Rings*/
	FusedRingBuilder frBuilder;
	/**Resources, visible to other package members */
	static ResourceGetter resourceGetter = new ResourceGetter("uk/ac/cam/ch/wwmm/opsin/resources/");

	private static NameToStructure myInstance;

	public static void reinitialise() throws Exception {
		myInstance = null;
		getInstance();
	}

	public static NameToStructure getInstance() throws Exception {
		if(myInstance == null) myInstance = new NameToStructure();
		return myInstance;
	}


	/**Initialises the name-to-structure convertor.
	 *
	 * @throws Exception If the convertor cannot be initialised, most likely due to bad or missing data files.
	 */
	public NameToStructure() throws Exception {
		System.out.println("Initialising OPSIN... ");
		//try {
			parser = new Parser();
			postProcessor = new PostProcessor(this);
			preStructureBuilder = new PreStructureBuilder(this);
			structureBuilder = new StructureBuilder();
			frBuilder = new FusedRingBuilder(this);
//		} catch (Exception e) {
//			throw new NameToStructureException(e.getMessage());
//		}
		System.out.println("OPSIN initialised");
	}

	public Element parseToCML(String name) {
		return parseToCML(name, false);
	}

	/**Parses a chemical name, returning an unambiguous CML representation of the molecule.
	 *
	 * @param name The chemical name to parse.
	 * @param verbose Whether to print lots of debugging information to stdin and stderr or not.
	 * @return A CML element, containing the parsed molecule, or null if the molecule would not parse.
	 */
	public synchronized Element parseToCML(String name, boolean verbose) {
		name=name.trim();//remove leading and trailing whitespace
		if("amine".equalsIgnoreCase(name)) return null; //One tiny annoying exception...
		if("thiol".equalsIgnoreCase(name)) return null; //OK a few more, not enough to merit a file
		if("carboxylic acid".equalsIgnoreCase(name)) return null;
		try {
			if(verbose) System.out.println(name);
			List<Element> p = parser.parse(name);
			//if(verbose) for(Element e : p) System.out.println(new XOMFormatter().elemToString(e));
			Comparator<Element> sortParses= new SortParses();
			Collections.sort(p, sortParses);//less tokens preferred
			Element cml =null;
			for(Element pe : p) {
				try {
					if(verbose) System.out.println(new XOMFormatter().elemToString(pe));
					Element pp = postProcessor.postProcess(pe);
					if(pp != null) {
						if(verbose) System.out.println(new XOMFormatter().elemToString(pp));
						BuildState state = new BuildState(structureBuilder.ssBuilder, structureBuilder.cmlBuilder);
						Element psb = preStructureBuilder.postProcess(pp, state);
						if(psb != null) {
							if(verbose) System.out.println(new XOMFormatter().elemToString(psb));
							cml = structureBuilder.buildCML(psb, state);
							if(verbose) System.out.println(new XOMFormatter().elemToString(cml));
							break;
						}
					}
				} catch (Exception e) {
					if(verbose) e.printStackTrace();
				}
			}
			if(cml ==null) throw new Exception();
			return cml;
		} catch (Exception e) {
			if(verbose) e.printStackTrace();
			return null;
		}
	}

	public static void interact() throws Exception {
		NameToStructure nts = new NameToStructure();
		Serializer serializer = new Serializer(System.out);
		serializer.setIndent(2);
		boolean end = false;
		BufferedReader stdinReader = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("OPSIN Prealpha: enter chemical name:");
		while(!end) {
			//stdinReader.
			//while(!stdinReader.ready());
			String name = stdinReader.readLine();
			if(name == null) {
				System.err.println("Disconnected!");
				end = true;
			} else if(name.equals("END")) {
				end = true;
			} else {
				Element output = nts.parseToCML(name);
				if(output == null) {
					System.out.println("Did not parse.");
					System.out.flush();
				} else {
					serializer.write(new Document(output));
					System.out.flush();
				}
			}
		}
	}

	/**Run OPSIN as a standalone component.
	 *
	 * @param args
	 * @throws Exception
	 */
	public static void main(String [] args) throws Exception {
		interact();
	}
}
