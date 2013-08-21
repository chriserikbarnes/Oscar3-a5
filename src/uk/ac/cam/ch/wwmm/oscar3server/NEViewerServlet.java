package uk.ac.cam.ch.wwmm.oscar3server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nu.xom.Serializer;
import uk.ac.cam.ch.wwmm.oscar3.Oscar3Props;
import uk.ac.cam.ch.wwmm.ptclib.scixml.SciXMLDocument;

/** Provides a page with information about a named entity. Also provides useful links etc.
 * 
 * @author ptc24
 *
 */
@SuppressWarnings("serial")
public final class NEViewerServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String name = request.getParameter("name");
		String type = request.getParameter("type");
		String smiles = request.getParameter("smiles");
		String inchi = request.getParameter("inchi");
		String ontIDs = request.getParameter("ontids");
		
		boolean cutDown = Oscar3Props.getInstance().serverType.equals("cutdown");
		
		try {
			SciXMLDocument doc = NEPage.makeNEPage(name, type, smiles, inchi, ontIDs, cutDown);
			
			doc.addServerProcessingInstructions();
			new Serializer(response.getOutputStream()).write(doc);
		} catch (Exception e) {
			e.printStackTrace();
			response.setContentType("text/plain");
			response.getWriter().println("Error in Named Entity viewer!");
		}
	}
	
}
