package uk.ac.cam.ch.wwmm.oscar3server;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uk.ac.cam.ch.wwmm.oscar3.pcsql.DetailsForCid;
import uk.ac.cam.ch.wwmm.oscar3.pcsql.PubChemSQL;

/**Allows PubChem mirrors to be queried. Now deprecated.
 * 
 * @author ptc24
 *
 */
@SuppressWarnings("serial")
public class PubChemMirrorServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setContentType("text/plain");
		PrintWriter out = response.getWriter();
		List<DetailsForCid> results = null;
		try {
			if(request.getParameter("inchi") != null) {
				results = PubChemSQL.getInstance().getDetailsForInchi(request.getParameter("inchi"));
			} else if(request.getParameter("name") != null) {
				results = PubChemSQL.getInstance().getDetailsForName(request.getParameter("name"));
			} else if(request.getParameter("cid") != null) {
				results = PubChemSQL.getInstance().getDetailsForCid(Integer.parseInt(request.getParameter("cid")));
			} else {
				out.println("Bad query, try inchi= or name= or cid= something");
			}
			if(results != null) {
				for(DetailsForCid dfc : results) {
					out.println(dfc);
					out.flush();
				}
			}
		} catch (Exception e) {
			e.printStackTrace(out);
		}
		
	}
	
}
