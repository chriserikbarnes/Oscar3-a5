package uk.ac.cam.ch.wwmm.oscar3server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Serializer;
import uk.ac.cam.ch.wwmm.opsin.NameToStructure;
import uk.ac.cam.ch.wwmm.oscar3.Oscar3Props;

public class SimpleOPSINServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		
		String name = request.getParameter("name");
		if(name == null) {
			response.setContentType("text/plain");
			response.getWriter().println("No name parameter!");
			return;
		}
		try {
			Element cml = NameToStructure.getInstance().parseToCML(name);
			if(cml == null) {
				response.setContentType("text/plain");
				response.getWriter().println("Name did not parse!");
				return;				
			}
			Document doc = new Document(cml);
			response.setContentType("application/xml");
			Serializer ser = new Serializer(response.getOutputStream());
			ser.write(doc);
		} catch (StackOverflowError e) {
			response.setContentType("text/plain");
			response.getWriter().println("Name did not parse!");
			if(Oscar3Props.getInstance().verbose) System.err.println("Stack overflow for OPSIN on: " + name);
			return;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
}
