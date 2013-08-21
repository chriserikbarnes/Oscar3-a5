package uk.ac.cam.ch.wwmm.oscar3server;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uk.ac.cam.ch.wwmm.oscar3.Oscar3;
import uk.ac.cam.ch.wwmm.oscar3.Oscar3Props;
import uk.ac.cam.ch.wwmm.oscar3.Traceability;
import uk.ac.cam.ch.wwmm.ptclib.scixml.PubmedToSciXML;

/**Servlet UI for getting PubMed abstracts into Oscar3
 * 
 * @author ptc24
 *
 */
@SuppressWarnings("serial")
public final class PubMedServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doPost(request, response);
	}
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String query = null;
		String name = null;
		String email = null;
		int abstracts = 0;
		int skip = 0;
		boolean process = false;
		try {
			query = request.getParameter("query");
			name = request.getParameter("name");
			email = request.getParameter("email");
			abstracts = Integer.parseInt(request.getParameter("abstracts"));
			skip = Integer.parseInt(request.getParameter("skip"));
			process = "true".equals(request.getParameter("process"));
		} catch (Exception e) {
			response.setContentType("text/plain");
			response.getWriter().write("Error: could not parse parameters");
		}
		try {
			File corpora = new File(Oscar3Props.getInstance().workspace, "corpora");
			File dir = new File(corpora, name);
			PubmedToSciXML.queryToCorpus(dir, query, email, abstracts, skip);
			if(process) {
				File [] subdirs = dir.listFiles();
				String traceability = Traceability.getTraceabilityInfo();
				for(int i=0;i<subdirs.length;i++) {
					if(subdirs[i].isDirectory()) {
						Oscar3.processDirectory(subdirs[i], traceability);
					}
				}				
			}
			response.setContentType("text/plain");
			response.getWriter().write("Pubmed abstracts fetched " + (process ? "and processed " : "") + "OK");			
		} catch (Exception e) {
			e.printStackTrace();
			response.setContentType("text/plain");
			response.getWriter().write("Error in getting pubmed abstracts");			
		}
		
	}
	
}
