package uk.ac.aber.art_tool;

import java.util.HashSet;

import nu.xom.Element;

/** Strings and methods to specific to SciXML. Will need changing to work on other schemas.
 * 
 * @author Maria Liakata
 */
public class ARTXMLStrings {
	
	private static ARTXMLStrings myInstance = null;
	
	public static String PAPER = "PAPER";
	public static String TITLE = "TITLE";
	public static String ABSTRACT = "ABSTRACT";
	public static String BODY = "BODY";
	public static String HEADER = "HEADER";
	public static String PARAGRAPH = "P"; /* NB "P" is hardcoded into regexes.xml, too. Be careful when changing this! */
	public static String ITALICS = "IT";
	public static String DIV = "DIV";
	public static String COMPOUNDREF_ID_ATTRIBUTE = "ID";
	public static String EQN = "EQN";
	
	public static String FORMATTING_XPATH = "//IT|//SB|//SP|//B|//SCP|//SANS";
	//public static String TEST_ONE_SNIPPET = "//BODY"; //cim
	//public static String TEST_PARAS_ONLY = "//P"; //cim
	public static String CHEMICAL_PLACES_XPATH = "//P|//ABSTRACT|//TITLE|//CURRENT_TITLE|//HEADER";
	public static String SMALL_CHEMICAL_PLACES_XPATH = "//P|//ABSTRACT|/PAPER/CURRENT_TITLE|/PAPER/TITLE|//HEADER";
	public static String EXPERIMENTAL_SECTION_XPATH = "/PAPER/BODY/DIV[HEADER[starts-with(text(),'Experimental')]]";
	public static String EXPERIMENTAL_PARAS_XPATH = "/PAPER/BODY/DIV/HEADER[starts-with(text(),'Experimental')]/..//P";
	public static String ALL_PARAS_XPATH = "/PAPER/BODY/DIV/..//P";
	public static String COMPOUNDREF_XPATH = "XREF[@TYPE=\"COMPOUND\"]";	
	public static String TITLE_XPATH ="/PAPER/TITLE|/PAPER/CURRENT_TITLE";
	public static String JOURNAL_NAME_XPATH = "/PAPER/METADATA/JOURNAL/NAME";
	
	String [] styleMarkupArray = {"IT", "B", "SB", "SP", "LATEX", "B", "SCP", "SANS", "ROMAN", "TYPE", "UN", "DUMMY", "ne"};
	HashSet<String> styleMarkup = new HashSet<String>();

	String [] blockMarkupArray = {"P", "HEADER", "TITLE", "ABSTRACT", "CURRENT_TITLE"};
	HashSet<String> blockMarkup = new HashSet<String>();
	
	/* These aren't part of SciXML, but are here for convenience */
	String [] specPropMarkupArray = {"spectrum", "property"};
	HashSet<String> specPropMarkup = new HashSet<String>();

	private ARTXMLStrings() {
		for(int i=0;i<styleMarkupArray.length;i++) styleMarkup.add(styleMarkupArray[i]);
		for(int i=0;i<blockMarkupArray.length;i++) blockMarkup.add(blockMarkupArray[i]);
		for(int i=0;i<specPropMarkupArray.length;i++) specPropMarkup.add(specPropMarkupArray[i]);
	}
	
	private static ARTXMLStrings getInstance() {
		if(myInstance == null) {
			myInstance = new ARTXMLStrings();
		}
		return myInstance;
	}
	
	/** Initialises the singleton associated with this class. For convenience at startup.
	 */
	public static void init() {
		getInstance();
	}

}
