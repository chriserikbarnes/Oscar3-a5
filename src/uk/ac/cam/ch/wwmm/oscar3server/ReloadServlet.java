package uk.ac.cam.ch.wwmm.oscar3server;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**Allows re-loading of singletons in Oscar3.
 * 
 * @author ptc24
 *
 */
@SuppressWarnings("serial")
public final class ReloadServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String name = request.getPathInfo();
		response.setContentType("text/plain");
		PrintWriter out = response.getWriter();
		
		try {
			Reload.reload(name, out);
		} catch (Exception e) {
			e.printStackTrace(response.getWriter());
		}
	}
	
}
