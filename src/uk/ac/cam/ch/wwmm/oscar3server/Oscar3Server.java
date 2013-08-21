package uk.ac.cam.ch.wwmm.oscar3server;

import java.io.File;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.handler.ResourceHandler;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.resource.Resource;

import uk.ac.cam.ch.wwmm.oscar3.Oscar3Props;

/** The main server class, this is where the Jetty embedding takes place.
 * 
 */
public final class Oscar3Server {

	private static void loadExperimentalServlet(Context context, String servlet, String path) throws Exception {
		try {
			Class.forName(servlet);
		} catch (ClassNotFoundException e) {
			// Failing silently is in fact the correct behaviour here...
			return;
		}
		context.addServlet(servlet, path);
	}
	
	/**Launches the Oscar3 server, and waits for requests.
	 * 
	 * @throws Exception
	 */
	public static void launchServer() throws Exception {

		if(Oscar3Props.getInstance().serverType.equals("none")) {
			return;
		}
			
		System.setProperty("org.mortbay.http.HttpRequest.maxFormContentSize", "1000000");
		
	    // Create the server
	    Server server=new Server();
	    
	    SocketConnector conn = new SocketConnector();
	    conn.setPort(Oscar3Props.getInstance().port);
	    if(Oscar3Props.getInstance().lockdown) conn.setHost("127.0.0.1");
	    server.addConnector(conn);

	    String serverRoot = Oscar3Props.getInstance().serverRoot;
	    Context context;
	    if(serverRoot == null || serverRoot.equals("none")) {
	    	context = new Context(server, "/");
	    } else {
	    	context = new Context(server, serverRoot);
	    }

	    Resource r = null;
	    if(Oscar3Props.getInstance().serverType.equals("full")) {
		    r = Resource.newClassPathResource("uk/ac/cam/ch/wwmm/oscar3server/resources/fullweb/");
	    } else {
		    r = Resource.newClassPathResource("uk/ac/cam/ch/wwmm/oscar3server/resources/cutdownweb/");	    	
	    }
	    r.encode("UTF-8");
	    ResourceHandler rh = new ResourceHandler();
	    rh.setBaseResource(r);
	    //server.addHandler(rh);
	    context.setHandler(rh);

	    if(serverRoot == null || serverRoot.equals("none")) {
	    	context = new Context(server, "/");
	    } else {
	    	context = new Context(server, serverRoot);
	    }
    
	    r = Resource.newClassPathResource("uk/ac/cam/ch/wwmm/oscar3server/resources/sharedweb");
	    rh = new ResourceHandler();
	    rh.setBaseResource(r);


	    
	    context.setHandler(rh);
	    //server.addHandler(rh);
	    
	    if(serverRoot == null || serverRoot.equals("none")) {
	    	context = new Context(server, "/");
	    } else {
	    	context = new Context(server, serverRoot);
	    }

	    context.addServlet("uk.ac.cam.ch.wwmm.oscar3server.ProcessServlet", "/Parse");
	    context.addServlet("uk.ac.cam.ch.wwmm.oscar3server.ProcessServlet", "/Process");
	    context.addServlet("uk.ac.cam.ch.wwmm.oscar3server.ViewMolServlet", "/ViewMol");
	    context.addServlet("uk.ac.cam.ch.wwmm.oscar3server.NEViewerServlet", "/NEViewer");
	    
	    if(Oscar3Props.getInstance().serverType.equals("full")) {
	    	//context.addServlet("com.acme.Dump", "/Dump/*");
	    	context.addServlet("uk.ac.cam.ch.wwmm.oscar3server.SearchServlet", "/Search");
	    	context.addServlet("uk.ac.cam.ch.wwmm.oscar3server.ChemNameDictServlet", "/ChemNameDict");
	    	context.addServlet("uk.ac.cam.ch.wwmm.oscar3server.ScrapBookServlet", "/ScrapBook");
	    	context.addServlet("uk.ac.cam.ch.wwmm.oscar3server.NETypesServlet", "/NETypes");
	    	context.addServlet("uk.ac.cam.ch.wwmm.oscar3server.ViewPaperServlet", "/ViewPaper/*");
	    	context.addServlet("uk.ac.cam.ch.wwmm.oscar3server.PubMedServlet", "/PubMed");
	    	context.addServlet("uk.ac.cam.ch.wwmm.oscar3server.CMLServlet", "/CML");
	    	context.addServlet("uk.ac.cam.ch.wwmm.oscar3server.ResourceServlet", "/resources/*");
	    	context.addServlet("uk.ac.cam.ch.wwmm.oscar3server.ReloadServlet", "/Reload/*");	    	
	    	context.addServlet("uk.ac.cam.ch.wwmm.oscar3server.PubChemMirrorServlet", "/PubChemMirror");
	    	context.addServlet("uk.ac.cam.ch.wwmm.oscar3server.AvailableFontsServlet", "/AvailableFonts");
	    	context.addServlet("uk.ac.cam.ch.wwmm.oscar3server.DFALoadSaveServlet", "/DFALoadSave");
	    	context.addServlet("uk.ac.cam.ch.wwmm.oscar3server.UnknownWordsServlet", "/UnknownWords");
	    	context.addServlet("uk.ac.cam.ch.wwmm.oscar3server.OPSINTestServlet", "/OPSINTest");
	    	context.addServlet("uk.ac.cam.ch.wwmm.oscar3server.SimpleOPSINServlet", "/OPSIN");
	    	context.addServlet("uk.ac.cam.ch.wwmm.oscar3server.NGramServlet", "/NGram");
	    	context.addServlet("uk.ac.aber.art_tool.ARTServlet", "/ART");
	    	
		    loadExperimentalServlet(context, "uk.ac.cam.ch.wwmm.ptc.experimental.termsimilarity.TermSimilarityServlet", "/TermSimilarity");
		    loadExperimentalServlet(context, "uk.ac.cam.ch.wwmm.oscar3server.sciborg.SciBorgServlet", "/SciBorg/*");
	    	
	    	if(!"none".equals(Oscar3Props.getInstance().openBabel)) context.addServlet("uk.ac.cam.ch.wwmm.oscar3server.OpenBabelServlet", "/OpenBabel");
	    } 
	    
	    System.out.println("Server ready - go to http://" + Oscar3Props.getInstance().hostname + ":" + Oscar3Props.getInstance().port + "/");

	    server.start();
	}
}
