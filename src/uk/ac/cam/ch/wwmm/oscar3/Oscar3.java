package uk.ac.cam.ch.wwmm.oscar3;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import nu.xom.Attribute;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Serializer;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import uk.ac.cam.ch.wwmm.opsin.NameToStructure;
import uk.ac.cam.ch.wwmm.oscar3.chemnamedict.ChemNameDictSingleton;
import uk.ac.cam.ch.wwmm.oscar3.dataparse.RParser;
import uk.ac.cam.ch.wwmm.oscar3.flow.OscarFlow;
import uk.ac.cam.ch.wwmm.oscar3.misc.InitScript;
import uk.ac.cam.ch.wwmm.oscar3.misc.MakeDirs;
import uk.ac.cam.ch.wwmm.oscar3.models.Model;
import uk.ac.cam.ch.wwmm.oscar3.newpc.FetchPubChem;
import uk.ac.cam.ch.wwmm.oscar3.newpc.NewPubChem;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.HyphenTokeniser;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.finder.DFANEFinder;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.finder.DFAONTCPRFinder;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.memm.MEMMSingleton;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.tokenanalysis.NGram;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.tokenanalysis.TLRHolder;
import uk.ac.cam.ch.wwmm.oscar3.resolver.NameResolver;
import uk.ac.cam.ch.wwmm.oscar3.subtypes.NESubtypes;
import uk.ac.cam.ch.wwmm.oscar3.terms.TermMaps;
import uk.ac.cam.ch.wwmm.oscar3.terms.TermSets;
import uk.ac.cam.ch.wwmm.oscar3.test.AllTests;
import uk.ac.cam.ch.wwmm.oscar3server.Oscar3Server;
import uk.ac.cam.ch.wwmm.oscar3server.ViewerServer;
import uk.ac.cam.ch.wwmm.ptclib.cdk.StructureConverter;
import uk.ac.cam.ch.wwmm.ptclib.io.FileTools;
import uk.ac.cam.ch.wwmm.ptclib.io.IOTools;
import uk.ac.cam.ch.wwmm.ptclib.io.ResourceGetter;
import uk.ac.cam.ch.wwmm.ptclib.scixml.PubXMLToSciXML;
import uk.ac.cam.ch.wwmm.ptclib.scixml.TextToSciXML;
import uk.ac.cam.ch.wwmm.ptclib.scixml.ToSciXML;
import uk.ac.cam.ch.wwmm.ptclib.scixml.XMLStrings;

/** The main class, call the main method on this to parse papers, start the server
 * or perform other tasks.
 * 
 * @author ptc24
 *
 */
public final class Oscar3 {

	public static String svnRevision = "$LastChangedRevision: 556 $";
		
	/** Initialised all of the important singletons, and prints out how long it took.
	 * 
	 * @throws Exception
	 */
 	private static void timedInitSingletons() throws Exception {
		System.out.println("Initialising singletons... Please hold on...");
		long start; long end; double seconds;
		start = System.currentTimeMillis();
		initSingletons();
		end = System.currentTimeMillis();
		seconds = (end - start) / 1000.0;
		System.out.printf("Singletons initialised in: %f seconds\n", seconds);
 	}

 	/** Initialises all of the important singletons, to avoid delays later
 	 * 
 	 * @throws Exception
 	 */
	private static void initSingletons() throws Exception {
		Model.loadModel();
		TermSets.init();
		TermMaps.init();
		TLRHolder.getInstance();
		ChemNameDictSingleton.hasName("acetone");
		NGram.getInstance();
		HyphenTokeniser.init();
		XMLStrings.init();
		if(Oscar3Props.getInstance().useMEMM) {
			DFAONTCPRFinder.getInstance();
		} else {
			DFANEFinder.getInstance();
		}
		NameResolver.init();
		StructureConverter.init();
		NameToStructure.getInstance();
		RParser.init();
	}
	
	/**A simple method for running Oscar3 - this takes a plain text input
	 * string, and returns a string serialisation of inline annotated XML.
	 * 
	 * @param input The input string.
	 * @return The string form of the inline annotated XML.
	 * @throws Exception
	 */
	public static String stringToString(String input) throws Exception {
		Document doc = TextToSciXML.textToSciXML(input);
		OscarFlow flow = new OscarFlow(doc);
		flow.processLite();
		return flow.getInlineXML().toXML();
	}

	
	/** Prints out a useful help message.
	 * 
	 *
	 */
	private static void printUsage() throws Exception {
		System.out.println(new ResourceGetter("uk/ac/cam/ch/wwmm/oscar3/resources/").getString("usage.txt"));
	}
	
	/** Loads in a file, gets it into SciXML, and parses it.
	 * 
	 * @param filename The name of the file to parse.
	 * @param mode "Process", "SAF", "Data" or "RoundTrip".
	 * @return A XOM Document corresponding to the output.
	 * @throws Exception
	 */
	private static Document processFile(String filename, String mode) throws Exception {		
		// First make sure the properties file is loaded...
		Oscar3Props.getInstance();
		Document doc;

		PubXMLToSciXML ptsx = null;
		
		if(mode.equals("Process") || mode.equals("SAF") || mode.equals("Data")) {
			System.out.println("Loading file...");
			doc = ToSciXML.fileToSciXML(new File(filename));
		} else if(mode.equals("RoundTrip")) {
			ptsx = new PubXMLToSciXML(new Builder().build(new File(filename)));
			doc = ptsx.getSciXML();
		} else {
			throw new Error("Mode not recognised");
		}
		
		OscarFlow oscarFlow = new OscarFlow(doc);

		if(mode.equals("Process")) {
			oscarFlow.processLite();
			return oscarFlow.getInlineXML();
		} else if(mode.equals("SAF")) {
			oscarFlow.processToSAF();
			return oscarFlow.getSafXML();
		} else if(mode.equals("Data")) {
			oscarFlow.parseData();
			return oscarFlow.getDataXML();
		} else if(mode.equals("RoundTrip")) {
			oscarFlow.processLite();
			ptsx.setSciXMLDoc(oscarFlow.getInlineXML());
			return ptsx.getAnnotatedPubXML();
		}
		return null;
	}
	
	/** Visits a directory containing a SciXML file, parses it, and adds files as appropriate.
	 * 
	 * @param directory The directory to visit.
	 * @throws Exception
	 */
	public static void processDirectory(File directory, String traceability) {
		try {
			if(Oscar3Props.getInstance().verbose) System.out.println("Processing: " + directory);
			Writer w = new FileWriter(new File(directory, "traceability.txt"));
			w.write(traceability);
			w.close();
			
			/* Get "side streams" for output ready */
			Document safDoc = new Document(new Element("dummy"));
			/* Get the document */
			Document doc = new Builder().build(new File(directory, "source.xml"));
			
			OscarFlow oscarFlow = new OscarFlow(doc);
			oscarFlow.processFull();
			
			/* Output time! */
			new Serializer(new FileOutputStream(new File(directory, "markedup.xml"))).write(oscarFlow.getInlineXML());
			safDoc.getRootElement().addAttribute(new Attribute("document", directory.getName()));
			new Serializer(new FileOutputStream(new File(directory, "saf.xml"))).write(oscarFlow.getSafXML());
			if(oscarFlow.getGeniaSAF() != null) {
				new Serializer(new FileOutputStream(new File(directory, "geniasaf.xml"))).write(oscarFlow.getGeniaSAF());				
			}
			if(oscarFlow.getRelationXML() != null) {
				new Serializer(new FileOutputStream(new File(directory, "relations.xml"))).write(oscarFlow.getRelationXML());				
			}
			if(oscarFlow.getDataXML() != null) {
				new Serializer(new FileOutputStream(new File(directory, "data.xml"))).write(oscarFlow.getDataXML());				
			}

			/* PubXML present? Then round-trip it */
			if(new File(directory, "pubxml-source.xml").exists()) {
				Builder b = new Builder();
				Document pubXML = b.build(new File(directory, "pubxml-source.xml"));
				Document convDoc = b.build(new File(directory, "conv.xml"));
				Document outDoc = PubXMLToSciXML.getAnnotatedPubXML(pubXML, oscarFlow.getInlineXML(), oscarFlow.getSourceXML(), convDoc);
				new Serializer(new FileOutputStream(new File(directory, "pubxml-annotated.xml"))).write(outDoc);
			}
			/* Write out custom files, if present */
			for(String filename : oscarFlow.getCustomOutputNames()) {
				//InputStream is = oscarFlow.customInputStream(filename);
				OutputStream os = new FileOutputStream(new File(directory, filename));
				/*byte[] buffer = new byte[1024];
				int i = 0;
				while ((i = is.read(buffer)) != -1) {
					os.write(buffer, 0, i);
				}
				is.close();
				os.close();*/
				oscarFlow.writeCustomeOutputToStream(filename, os);
				os.close();
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Argh. Better luck next time");
		}
	}
		
	public static void crossProcessDirectory(File inputDirectory, File outputDirectory, String traceability) throws Exception {
		if(!outputDirectory.exists()) outputDirectory.mkdir();
		File [] subdirs = inputDirectory.listFiles();
		List<File> sdl = new ArrayList<File>();
		for(int i=0;i<subdirs.length;i++) {
			if(subdirs[i].isDirectory() && FileTools.getFilesFromDirectoryByName(subdirs[i], "scrapbook.xml").size() > 1) {
				sdl.add(subdirs[i]);
			}
		}
		if(sdl.size() < 2) {
			System.out.println("Input directory must contain at least two subdirectories");
			System.out.println("that in turn contain at least two subdirectories each");
			System.out.println("that in turn contain scrapbook files.");
			return;
		}
		System.out.println("Ready to go...");
		for(int i=0;i<sdl.size();i++) {
			List<File> files = new ArrayList<File>();
			for(int j=0;j<sdl.size();j++) {
				if(i == j) continue;
				files.addAll(FileTools.getFilesFromDirectoryByName(sdl.get(j), "scrapbook.xml"));
			}
			System.out.println(files);
			MEMMSingleton.train(files, true); // This also trains the ETD
			//NESubtypes.trainOnFiles(files);
			System.out.println("Trained OK");
			List<File> sourceFiles = FileTools.getFilesFromDirectoryByName(sdl.get(i), "source.xml");
			System.out.println(sourceFiles);
			File tld = null;
			for(File f : sourceFiles) {
				System.out.println(f);
				List<String> dirNames = new ArrayList<String>();
				for(File cf = f.getParentFile();!cf.equals(inputDirectory);cf=cf.getParentFile()) {
					dirNames.add(cf.getName());
				}
				Collections.reverse(dirNames);
				File outDir = outputDirectory;
				for(String dirName : dirNames) {
					File newOutDir = new File(outDir, dirName);
					if(!newOutDir.exists()) newOutDir.mkdir();
					if(tld == null) tld = newOutDir;
					outDir = newOutDir;
				}
				copyFile(f, new File(outDir, "source.xml"));
				processDirectory(outDir, traceability);
				
				//if(f.getParentFile().getParentFile().equals(inputDirectory)) {
				//	
				//} else {
				//	
				//}
			}
			
		}
	}
	
	private static void copyFile(File from, File to) throws Exception {
		FileChannel in = new FileInputStream(from).getChannel();
		FileChannel out = new FileOutputStream(to).getChannel();
		in.transferTo(0, in.size(), out);
		in.close();
		out.close();
	}
	/**
	 * Grand unified main method - call this!
	 * 
	 * @param args Command-line arguaments.
	 */
	public static void main(String[] args) throws Exception {
		if(args.length == 0) {
			printUsage();
			return;
		}
		
		if(args[0].startsWith("-P=")) {
			String [] newArgs = new String[args.length-1];
			for(int i=0;i<newArgs.length;i++) newArgs[i] = args[i+1];
			String a0 = args[0];
			String after = a0.substring(3);
			Oscar3Props.initialiseWithFile(new File(after));
			args = newArgs;
		}

		String mode = args[0];
		long mem = Runtime.getRuntime().maxMemory();
		mem /= (1024 * 1024);
		//System.out.println(mem);
		// Running Xmx512 on my AMD64 box causes only 455M to be reported. how strange.
		if(mem < 455 && !"ViewerServer".equals(mode)) {
			System.out.println("WARNING: Oscar3 needs a large amount of memory to run");
			System.out.println("Try running with the command-line option -Xmx512m");
			//return;
		}

		// Try loading the properties, you might want to configure.
		Oscar3Props.getInstance();

		if(!Oscar3Props.getInstance().initScript.equals("none")) {
			Object o = Class.forName(Oscar3Props.getInstance().initScript).getConstructor(new Class[0]).newInstance(new Object[0]);
			((InitScript)o).call();
		}
		//if(!Oscar3Props.checkVersion()) Oscar3Props.configure();
		
		if(mode.equals("SortProps")) {
			Oscar3Props.saveProperties();
			return;
		}
		
		if(mode.equals("Test")) {
			Result result = JUnitCore.runClasses(new Class[]{AllTests.class});
			if(result.wasSuccessful()) {
				System.out.println("All " + result.getRunCount() + " tests ran correctly!");
			} else {
				System.out.println("Tests showed " + result.getFailureCount() + " failures");
				for(Failure failure : result.getFailures()) {
					System.out.println(failure);
				}
			}
			return;
		}
		
		if(mode.equals("FetchPubChem")) {
			System.out.println("Fetching the compound files from PubChem will take several hours");
			System.out.println("and use several GB of hard disk space, and a lot of bandwidth.");
			System.out.println("Proceed (yes/no)?");
			if(IOTools.askYN()) {
				FetchPubChem.setupPcDir();
				FetchPubChem.fetchFromPubChem("Compound", false);
			}
			return;
		}
		
		if(mode.equals("BuildPubChem")) {
			new NewPubChem().initialise();
			return;
		}
		
		if(mode.equals("Server")) {
			if(Oscar3Props.getInstance().serverType.equals("none")) {
				System.out.println("Currently there is no server configured for Oscar3");
				System.out.println("Would you like me to configure it now (yes/no)?");
				if(IOTools.askYN()) {
					Oscar3Props.configureServer();
				} else {
					System.out.println("OK, quitting.");
					return;					
				}
			}
			Oscar3Server.launchServer();
			return;
		} else if(mode.equals("ReConfigureServer")) {
			Oscar3Props.configureServer();
			return;
		} else if(mode.equals("ConfigureWorkspace")) {
			Oscar3Props.configureWorkspace();
			return;
		} else if(mode.equals("MakeModel")) {
			if(args.length != 2) {
				System.out.println("You need a name (1 word, suitable for filenames) for the model");
				return;
			}
			if(Oscar3Props.getInstance().workspace.equals("none")) {
				System.out.println("You should set up a workspace");
				return;
			} 
			if(!new File(Oscar3Props.getInstance().workspace, "scrapbook").exists()) {
				System.out.println("You don't currently have a scrapbook");
				return;
			}
			new File(args[1] + ".xml");
			Model.makeModel(args[1]);
			System.out.println("Made model " + args[1] + " OK!");
		}
		
		
		if(mode.equals("ViewerServer")) {
			ViewerServer.launchServer();
			return;
		}
		
		if(!(mode.equals("Import") || mode.equals("Data"))) {
			timedInitSingletons();
		}
		
		if(mode.equals("Import") || mode.equals("ImportAndProcess") || mode.equals("ProcessInWorkspace")) {
			if("none".equals(Oscar3Props.getInstance().workspace)) {			
				System.out.println("You do not have a workspace configured at the moment.");
				System.out.println("As an alternative, I suggest using:");
				System.out.println("");
				System.out.println("ProcessInto <input directory> <output directory>");
				System.out.println("");
				System.out.println("This will find parsable files in the input directory");
				System.out.println("and create an output directory structure containing");
				System.out.println("the results of the parse.");
				return;
			}
		}	
				
		File outDir = null;		
		if(mode.equals("Import") || mode.equals("ImportAndProcess") || mode.equals("ProcessInto")) {
			File inDir = new File(args[1]);
			if(mode.equals("ProcessInto")) {
				outDir = new File(args[2]);
			} else {
				outDir = new File(new File(Oscar3Props.getInstance().workspace, "corpora"), inDir.getName());				
			}
			MakeDirs.makeDirs(inDir, outDir);
			System.out.println("Directory " + outDir.getAbsolutePath() + " made.");
		}

		if(mode.equals("Process") || mode.equals("SAF") || mode.equals("RoundTrip") || mode.equals("Data")) {
			String inFile = args[1];
			String outFile = args[2];
		
			System.out.println("Input: " + inFile);
			Document doc = processFile(inFile, mode);
			System.out.println("Writing output: " + outFile);
			new Serializer(new FileOutputStream(outFile)).write(doc);
			System.out.println("Document processed OK!");
		} else if(mode.equals("ProcessInWorkspace") || mode.equals("ImportAndProcess") || mode.equals("ProcessInto")) {
			File rootDir;
			
			if(outDir != null) {
				rootDir = outDir;
			} else {			
				rootDir = new File(args[1]);
				if(!rootDir.exists() || !rootDir.isDirectory()) {
					rootDir = new File(new File(Oscar3Props.getInstance().workspace, "corpora"), args[1]);
				}
			}

			List<File> files = FileTools.getFilesFromDirectoryByName(rootDir, "source.xml");
			System.out.println("****");
						
			long start; long end; double seconds;

			start = System.currentTimeMillis();
			String traceability = Traceability.getTraceabilityInfo();
			for(File f : files) {
				processDirectory(f.getParentFile(), traceability);
			}
			end = System.currentTimeMillis();
			seconds = (end - start) / 1000.0;
			int papers = files.size();
			double perpaper = seconds / papers;
			System.out.printf("%d papers processed in %f seconds\nAverage time: %f seconds", papers, seconds, perpaper);
		} else if(mode.equals("CrossProcess")) {
			String traceability = Traceability.getTraceabilityInfo();
			File inputDirectory = new File(args[1]);
			File outputDirectory = new File(args[2]);
			crossProcessDirectory(inputDirectory, outputDirectory, traceability);
		}
		
	}

}
