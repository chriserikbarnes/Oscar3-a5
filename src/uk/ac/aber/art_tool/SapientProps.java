package uk.ac.aber.art_tool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Properties;

import uk.ac.cam.ch.wwmm.oscar3.chemnamedict.ChemNameDictSingleton;
import uk.ac.cam.ch.wwmm.oscar3.deployment.Deployment;
import uk.ac.cam.ch.wwmm.ptclib.io.ResourceGetter;

/** Sets up and maintains a properties file for Oscar3.
 * 
 * @author Maria Liakata
 * @author ptc24
 * @author jat45
 *
 */
public class SapientProps {

	public boolean fulldb;
	public boolean lockdown;
	public boolean makeCML;
	public boolean splitOnEnDash;
	public boolean useONT;
	public boolean useDSO;
	public boolean deprioritiseONT;
	public boolean useFormulaRegex;
	public boolean useWordShapeHeuristic;
	public boolean minimizeDFA;
	public boolean useJNIInChI;
	public boolean useMEMM;
	public boolean rescoreMEMM;
	public boolean verbose;
	public boolean interpretPoly;
	public boolean dataOnlyInExperimental;
		
	public double ngramThreshold;
	public double ontProb;
	public double cprProb;
	public double custProb;
	public double neThreshold;

	public int dfaSize;
	
	public String dbname;
	public String dbaddress;
	public String dbusername;
	public String dbpasswd;
	public String rdbms;
	
	public String serverType;
	
	public String hostname;
	
	public String oscarFlow;
	
	public String workspace;
	public String geniaPath;
	public String pcdir;
	public String InChI;
	public String svdlibc;
	public String openBabel;
	public String model;
	public String yahooKey;
	
	private Properties myProperties;
	private static ResourceGetter rg = new ResourceGetter("uk/ac/cam/ch/wwmm/oscar3/resources/", true);
	
	private static SapientProps myInstance;
	
	public static SapientProps getInstance() {
		if(myInstance == null) {
			myInstance = new SapientProps();
			myInstance.initialise();
		}
		return myInstance;
	}
	
	public static void reloadProperties() {
		myInstance = null;
		getInstance();
	}
	
	private SapientProps() {
	}
	
	private void initialise() {
		try {
			if(myProperties == null) {
				myProperties = new Properties(getDefaults());
				File propsFile = new File("SapientProperties.dat");
				if(!propsFile.exists()) {
					myProperties = getDefaults();
					savePropertiesInternal();
					Deployment.deployInChI();
				}
				myProperties.load(new FileInputStream(new File("SapientProperties.dat")));
			}
			propsToVariables();
		} catch (Exception e) {
			throw new Error("Problem loading properties!");
		}		
	}
		
	private void propsToVariables() {
		fulldb = "yes".equals(myProperties.getProperty("fulldb"));
		lockdown = "yes".equals(myProperties.getProperty("lockdown"));
		makeCML = "yes".equals(myProperties.getProperty("makeCML"));
		splitOnEnDash = "yes".equals(myProperties.getProperty("splitOnEnDash"));
		useONT = "yes".equals(myProperties.getProperty("useONT"));
		useDSO = "yes".equals(myProperties.getProperty("useDSO"));
		deprioritiseONT = "yes".equals(myProperties.getProperty("deprioritiseONT"));
		useFormulaRegex = "yes".equals(myProperties.getProperty("useFormulaRegex"));
		useWordShapeHeuristic = "yes".equals(myProperties.getProperty("useWordShapeHeuristic"));
		minimizeDFA = "yes".equals(myProperties.getProperty("minimizeDFA"));
		useJNIInChI = "yes".equals(myProperties.getProperty("useJNIInChI"));
		useMEMM = "yes".equals(myProperties.getProperty("useMEMM"));
		rescoreMEMM = "yes".equals(myProperties.getProperty("rescoreMEMM"));
		verbose = "yes".equals(myProperties.getProperty("verbose"));
		interpretPoly = "yes".equals(myProperties.getProperty("interpretPoly"));
		dataOnlyInExperimental = "yes".equals(myProperties.getProperty("dataOnlyInExperimental"));
			
		ngramThreshold = Double.parseDouble(myProperties.getProperty("ngramThreshold"));
		neThreshold = Double.parseDouble(myProperties.getProperty("neThreshold"));
		ontProb = Double.parseDouble(myProperties.getProperty("ontProb"));
		cprProb = Double.parseDouble(myProperties.getProperty("cprProb"));
		custProb = Double.parseDouble(myProperties.getProperty("custProb"));

		dfaSize = Integer.parseInt(myProperties.getProperty("dfaSize"));
		
		dbname = getPropertyOrNone("dbname");
		dbaddress = getPropertyOrNone("dbaddress");
		dbusername = getPropertyOrNone("dbusername");
		dbpasswd = getPropertyOrNone("dbpasswd");
		rdbms = getPropertyOrNone("rdbms");
		serverType = getPropertyOrNone("serverType");
		hostname = getPropertyOrNone("hostname");
		oscarFlow = getPropertyOrNone("oscarFlow");
		workspace = getPropertyOrNone("workspace");
		geniaPath = getPropertyOrNone("geniaPath");
		pcdir = getPropertyOrNone("pcdir");
		InChI = getPropertyOrNone("InChI");
		openBabel = getPropertyOrNone("openBabel");
		svdlibc = getPropertyOrNone("svdlibc");
		model = getPropertyOrNone("model");
		yahooKey = getPropertyOrNone("yahooKey");
	}
	
	
	private String getPropertyOrNone(String propName) {
		String prop = myProperties.getProperty(propName);
		if(propName == null) return "none";
		return prop;
	}
	
	private Properties getDefaults() throws Exception {
		Properties def = new Properties();
		def.load(rg.getStream("DefaultProperties.dat"));
		return def;
	}
		
	public static void configureServer() throws Exception {
		getInstance().configureServerInternal();
	}
	
	private void configureServerInternal() throws Exception {
		BufferedReader stdinReader = new BufferedReader(new InputStreamReader(System.in));

		System.out.println("Configuring the Sapient web server");
		System.out.println();
		String serverType = "full";
		/*while(serverType == null) {
			System.out.println("Would you like to set up a full web server,");
			System.out.println("or a cut-down demonstration server?");
			System.out.println("Please enter 'full' or 'cutdown'");
			serverType = stdinReader.readLine();
			if(!"full".equals(serverType) && !"cutdown".equals(serverType)) {
				System.out.println("Invalid answer. Please try again.");
				System.out.println();
			}
		}*/
		setProperty("serverType", serverType);
		savePropertiesInternal();
		propsToVariables();
		
		if(serverType.equals("full")) {
			if(myProperties.get("workspace").equals("none")) {
				System.out.println("To do this you'll need to set up a workspace: ");
				System.out.println();
				configureWorkspaceInternal();
				System.out.println();
				System.out.println("Continuing with server configuration");
				System.out.println();
			}
		} 
		
		/*System.out.println("Would you like to lock the server down so that it can only be accessed from the machine it is running on (y/n)?");
		System.out.println("Please select 'yes', unless you know that your computer is behind a firewall that blocks incoming traffic on port 8181");
		System.out.println("If you select 'no', then people will be able to access and alter the contents of your Oscar3 workspace - this may cause problems if it will contain material that is confidential or someone else's copyright.");
		System.out.println("If in doubt, just answer 'yes'");*/
		//boolean lockdown = true;//IOTools.askYN();
		//if(lockdown) {
			setProperty("lockdown", "yes");
			setProperty("hostname", "127.0.0.1");
		//} else {
		/*	System.out.println("Setting up the server to be accessible from anywhere that your firewall doesn't protect you from, as you request...");

			setProperty("lockdown", "no");
			setProperty("hostname", InetAddress.getLocalHost().getCanonicalHostName());
		}*/
		propsToVariables();
		savePropertiesInternal();
	}
	
	public static void configureWorkspace() throws Exception {
		getInstance().configureWorkspaceInternal();
	}
	
	private void configureWorkspaceInternal() throws Exception {
		
		// the workspace can be set in the properties file so check there first
		boolean done = false;
		String wsdir = myProperties.getProperty("workspace");
		
		try {
			if(!wsdir.equals("none")) {
				File f = new File(wsdir);
				if (f.exists() && f.isDirectory()) {
					done = true;
					System.out.println("OK, using an existing directory as your workspace.");
					System.out.println();
				}
				f.mkdir();
				wsdir = f.getCanonicalPath();
				done = true;
				System.out.println("Made your workspace at: " + wsdir);				
			}
		} catch (Exception e) {
			System.out.println("Can't create workspace automatically");
			done = false;
			wsdir = null;
		}

		// workspace is not valid, so prompt the user
		if (!done) {
			BufferedReader stdinReader = new BufferedReader(
					new InputStreamReader(System.in));

			System.out.println("Setting up a Sapient workspace.");
			System.out.println();
			System.out
					.println("This will be used for papers before and after annotation. ");
			/*System.out
					.println("indexes of those papers (so you can search them), ");
			System.out.println("an updatable dictionary of chemical names ");
			System.out.println("and a ScrapBook.");
			System.out.println();
			System.out
					.println("Please enter a directory to be your OSCAR workspace");
			System.out
					.println("or leave blank to use the current working directory");*/
			while (!done) {
				wsdir = stdinReader.readLine();
				try {
					File f = new File(wsdir);
					if (f.exists() && f.isDirectory()) {
						done = true;
						System.out
								.println("OK, using an existing directory as your workspace.");
						System.out.println();
					}
					f.mkdir();
					wsdir = f.getCanonicalPath();
					done = true;
					System.out.println("Made your workspace at: " + wsdir);
				} catch (Exception e) {
					System.out.println("That didn't work");
				}
				if (!done) {
					System.out.println("Please try again: enter a directory to be your Sapient workspace");
				}
			}
		}
		setProperty("workspace", wsdir);

		ChemNameDictSingleton.makeFromScratch();
		File chemnamedictdir = new File(wsdir, "chemnamedict");
		if (!chemnamedictdir.exists())
			chemnamedictdir.mkdir();
		ChemNameDictSingleton.save();

		File scrapbookdir = new File(wsdir, "scrapbook");
		if (!scrapbookdir.exists())
			scrapbookdir.mkdir();

		File resdir = new File(wsdir, "resources");
		if (!resdir.exists())
			resdir.mkdir();

		/*File modelsdir = new File(wsdir, "models");
		if (!modelsdir.exists())
			modelsdir.mkdir();*/
		
		File corporadir = new File(wsdir, "corpora");
		if (!corporadir.exists())
			corporadir.mkdir();
		propsToVariables();
		savePropertiesInternal();
	}	
		
	public static void setProperty(String name, String value) {
		getInstance().setPropertyInternal(name, value);
	}

	private synchronized void setPropertyInternal(String name, String value) {
		myProperties.setProperty(name, value);
		propsToVariables();
	}
	
	public static void saveProperties() throws Exception {
		getInstance().savePropertiesInternal();
	}
		
	private synchronized void savePropertiesInternal() throws Exception {
		myProperties.store(new FileOutputStream(new File("SapientProperties.dat")), "Autogenerated by Sapient");
	}
	
	public static void writeProperties(OutputStream os) throws Exception {
		getInstance().writePropertiesInternal(os);
	}

	public void writePropertiesInternal(OutputStream os) throws Exception {
		myProperties.store(os, "");
	}
}
