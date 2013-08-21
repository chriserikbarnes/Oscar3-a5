package uk.ac.cam.ch.wwmm.oscar3server;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uk.ac.cam.ch.wwmm.oscar3.recogniser.finder.DFANEFinder;

/**An experimental servlet for loading and saving the DFAs used in named entity
 * recognition.
 * 
 * @author ptc24
 *
 */

@SuppressWarnings("serial")
public final class DFALoadSaveServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String action = request.getParameter("action");
		response.setContentType("text/plain");
		PrintWriter out = response.getWriter();
		if("load".equals(action)) {
			DFANEFinder.readFromWorkspace();
			out.println("Loaded DFAs OK");
		} else if("save".equals(action)) {
			DFANEFinder.writeToWorkspace();
			out.println("Wrote DFAs OK");			
		}
		
	}
	
}
