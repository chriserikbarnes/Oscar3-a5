package uk.ac.aber.art_tool;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.handler.ResourceHandler;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.resource.Resource;

import uk.ac.cam.ch.wwmm.oscar3.Oscar3Props;


/** The main server class, this is where the Jetty embedding takes place.
 * 
 * @author Maria Liakata
 */
public class SapientServer {

	public static void launchServer() throws Exception {

		if(Oscar3Props.getInstance().serverType.equals("none")) {
			return;
		}
			
		System.setProperty("org.mortbay.http.HttpRequest.maxFormContentSize", "1000000");
		
	    // Create the server
	    Server server=new Server();
	    
	    SocketConnector conn = new SocketConnector();
	    conn.setPort(8181);
	    if(Oscar3Props.getInstance().lockdown) conn.setHost("127.0.0.1");
	    server.addConnector(conn);

	    Resource r = Resource.newClassPathResource("uk/ac/aber/art_tool/art_tool_web");
	    //if(Oscar3Props.getInstance().serverType.equals("full")) {
		   // r = 
	   // } else {
		//    r = Resource.newClassPathResource("uk/ac/cam/ch/wwmm/oscar3server/resources/cutdownweb/");	    	
	   // }
	    r.encode("UTF-8");
	    ResourceHandler rh = new ResourceHandler();
	    rh.setBaseResource(r);
	    server.addHandler(rh);
	    /*r = Resource.newClassPathResource("uk/ac/aber/art_tool/art_tool_web/javascript");
	    r.encode("UTF-8");
	    rh = new ResourceHandler();
	    rh.setBaseResource(r);
	    server.addHandler(rh);
	    r = Resource.newClassPathResource("uk/ac/aber/art_tool/art_tool_web/xsl");
	    r.encode("UTF-8");
	    rh = new ResourceHandler();
	    rh.setBaseResource(r);
	    server.addHandler(rh);*/
	    
	    // Adds the servlets associated with the server
	    
	    Context context = new Context(server, "/");

	    context.addServlet("uk.ac.cam.ch.wwmm.oscar3server.ProcessServlet", "/Parse");
	    context.addServlet("uk.ac.cam.ch.wwmm.oscar3server.ProcessServlet", "/Process");
	    context.addServlet("uk.ac.cam.ch.wwmm.oscar3server.ViewMolServlet", "/ViewMol");
	    context.addServlet("uk.ac.cam.ch.wwmm.oscar3server.NEViewerServlet", "/NEViewer");
	    
	    if(Oscar3Props.getInstance().serverType.equals("full")) {
	    	//context.addServlet("com.acme.Dump", "/Dump/*");
	    	context.addServlet("uk.ac.aber.art_tool.ARTServlet","/");
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
	    	if(!"none".equals(Oscar3Props.getInstance().openBabel)) context.addServlet("uk.ac.cam.ch.wwmm.oscar3server.OpenBabelServlet", "/OpenBabel");
	    } 
	    
	    System.out.println("Server ready - go to http://" + Oscar3Props.getInstance().hostname + ":8181/");

	    server.start();
	}
}
