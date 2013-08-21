package uk.ac.cam.ch.wwmm.oscar3server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nu.xom.Serializer;
import uk.ac.cam.ch.wwmm.oscar3.misc.NETypes;
import uk.ac.cam.ch.wwmm.ptclib.scixml.SciXMLDocument;

/** Shows a list of Named Entity types recognised by Oscar.
 * 
 * @author ptc24
 *
 */
@SuppressWarnings("serial")
public final class NETypesServlet extends HttpServlet {

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		SciXMLDocument doc = NETypes.getSciXMLDoc();
		doc.addServerProcessingInstructions();
		response.setContentType("application/xml");
		new Serializer(response.getOutputStream()).write(doc);
		
	}
	
}
