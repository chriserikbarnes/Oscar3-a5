package uk.ac.cam.ch.wwmm.oscar3server;
import java.io.IOException;
import java.io.OutputStream;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Serializer;
import uk.ac.cam.ch.wwmm.oscar3.flow.OscarFlow;
import uk.ac.cam.ch.wwmm.oscar3.resolver.NameResolver;
import uk.ac.cam.ch.wwmm.ptclib.scixml.SciXMLDocument;
import uk.ac.cam.ch.wwmm.ptclib.scixml.TextToSciXML;
import uk.ac.cam.ch.wwmm.ptclib.string.HtmlCleaner;
import uk.ac.cam.ch.wwmm.ptclib.xml.XOMTools;

/** Will process a plain text/HTML document POSTed to it.
 * 
 * @author ptc24
 *
 */
@SuppressWarnings("serial")
public final class ProcessServlet extends HttpServlet {
		
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		doPost(request, response);
	}
	
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		request.setCharacterEncoding("UTF-8");
		String in = request.getParameter("contents");
		SciXMLDocument doc = null;
		if(in == null) {
			in = request.getParameter("SciXML");
			try	{
				Document d = new Builder().build(in, "/");
				doc = SciXMLDocument.makeFromDoc(d);
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
		} else {
			if(Pattern.matches("(?is)\\w*<.*", in)) in = HtmlCleaner.cleanHTML(in);			
			doc = TextToSciXML.textToSciXML(in);
		}		
		produceOutput(request, response, doc);
	}
	
	private void produceOutput(HttpServletRequest request,
			HttpServletResponse response, SciXMLDocument doc) throws IOException {

		String outputMode = "default";
		if(request.getParameter("output") != null) outputMode = request.getParameter("output"); 
		
		try {
			NameResolver.purgeCache();
			Document plainDoc = new Document((Element)XOMTools.safeCopy(doc.getRootElement()));
			
			if(outputMode.equalsIgnoreCase("source")) {
				OutputStream out = response.getOutputStream();
				
				new Serializer(out).write(plainDoc);
				out.close();
				return;
			}
			/* Process the document */			

			OscarFlow oscarFlow = new OscarFlow(plainDoc);
			
			if(request.getParameter("flowcommand") != null &&
					request.getParameter("flowcommand").trim().length() > 0) {
				oscarFlow.runFlow(request.getParameter("flowcommand"));
			} else if(outputMode.equals("data")) {
				oscarFlow.parseData();
			} else {
				oscarFlow.processLite();
			}
			if(outputMode.equals("default") && request.getParameter("flowcommand") != null &&
					!request.getParameter("flowcommand").toLowerCase().contains("inline")) {
				outputMode = "saf";
			}
			
			if(outputMode.equalsIgnoreCase("custom")) {
				String name = request.getParameter("name");
				if(name.endsWith(".xml")) {
					response.setContentType("application/xml");
					response.setCharacterEncoding("UTF-8");					
				} else if(name.endsWith(".htm") || name.endsWith(".html")) {
					response.setContentType("text/html");
					response.setCharacterEncoding("UTF-8");
				} else {
					response.setContentType("text/plain");
					response.setCharacterEncoding("UTF-8");
				}
			} else {
				response.setContentType("application/xml");
				response.setCharacterEncoding("UTF-8");
			}
			
			OutputStream out = response.getOutputStream();
			if(outputMode.equalsIgnoreCase("saf")) {
				new Serializer(out).write(oscarFlow.getSafXML());				
			} else if(outputMode.equalsIgnoreCase("data")) {
				new Serializer(out).write(oscarFlow.getDataXML());				
			} else if(outputMode.equalsIgnoreCase("genia")) {
				new Serializer(out).write(oscarFlow.getGeniaSAF());
			} else if(outputMode.equalsIgnoreCase("custom")) {
				String name = request.getParameter("name");
				oscarFlow.writeCustomeOutputToStream(name, out);
			} else {
				// default to markedup
				doc = SciXMLDocument.makeFromDoc(oscarFlow.getInlineXML());
				doc.addServerProcessingInstructions();
				new Serializer(out).write(doc);
				out.close();
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
