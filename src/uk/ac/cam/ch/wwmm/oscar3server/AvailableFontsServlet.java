package uk.ac.cam.ch.wwmm.oscar3server;

import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;

/** A simple servlet to list the fonts available.
 * 
 * @author ptc24
 *
 */
@SuppressWarnings("serial")
public final class AvailableFontsServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		List<String> fontNames = StringTools.arrayToList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
		response.setContentType("text/plain");
		PrintWriter out = response.getWriter();
		for(String fontName : fontNames) {
			out.println(fontName);
		}
		out.close();
	}
	
}
