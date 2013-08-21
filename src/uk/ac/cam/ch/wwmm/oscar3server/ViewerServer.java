package uk.ac.cam.ch.wwmm.oscar3server;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.servlet.Context;

import uk.ac.cam.ch.wwmm.oscar3.Oscar3Props;


/** A simple Jetty server that only runs the ViewMol servlet
 * 
 */
public final class ViewerServer {

	/**Start the server, and wait for requests.
	 * 
	 * @throws Exception
	 */
	public static void launchServer() throws Exception {

	    Server server=new Server();
	    SocketConnector conn = new SocketConnector();
	    conn.setPort(8182);
	    if(Oscar3Props.getInstance().lockdown) conn.setHost("127.0.0.1");
	    server.addConnector(conn);
	    
	    Context context = new Context(server, "/");
	    context.addServlet("uk.ac.cam.ch.wwmm.oscar3server.ViewMolServlet", "/ViewMol");
	    
	    System.out.println("Server ready - go to http://" + Oscar3Props.getInstance().hostname + ":8182/");

	    server.start();
	}
}
