package uk.ac.cam.ch.wwmm.oscar3server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uk.ac.cam.ch.wwmm.oscar3.Oscar3Props;

/**Experimental: a convenient web-wrapper for OpenBabel's InChI to canonical SMILES functionality.
 * 
 * @author ptc24
 *
 */
public final class OpenBabelServlet extends HttpServlet {

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String inchi = request.getParameter("inchi");
		try {
			String smiles = getSMILESFromInChI(inchi);
			response.setContentType("text/plain");
			response.getWriter().println(smiles);
		} catch (Exception e) {
			response.setContentType("text/plain");
			e.printStackTrace(response.getWriter());
		}
	}
	
	public static String getSMILESFromInChI(String inchi) throws Exception {
		String ob = Oscar3Props.getInstance().openBabel;
		if("none".equals(ob)) return null;
		if(ob.matches(".*\\s+.*")) ob = "\"" + ob + "\"";
		Process p = Runtime.getRuntime().exec(ob + " -iinchi -ocan");
		InputStream is = p.getInputStream();
		OutputStream os = p.getOutputStream();
		PrintWriter pw = new PrintWriter(new OutputStreamWriter(os));
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		pw.println(inchi);
		pw.close();
		String smiles = br.readLine().trim();
		return smiles;
	}

}
