package uk.ac.cam.ch.wwmm.oscar3server.sciborg;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nu.xom.Document;
import nu.xom.Element;
import nu.xom.ProcessingInstruction;
import nu.xom.Serializer;
import uk.ac.cam.ch.wwmm.oscar3.Oscar3Props;
import uk.ac.cam.ch.wwmm.oscar3.sciborg.ResultDbReader;
import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;

public class SciBorgServlet extends HttpServlet {

	Map<String,SciBorgHandler> baseHandlers;
	SciBorgHandler rootHandler;
	
	public SciBorgServlet() {
		super();
		rootHandler = new SciBorgHandler() {
			public Document handle(String[] path, boolean endsInSlash) {
				Element elem = new Element("root");
				Document doc = new Document(elem);
				return doc;
			}
		};
		baseHandlers = new HashMap<String,SciBorgHandler>();
		baseHandlers.put("papers", new SciBorgHandler() {
			public Document handle(String[] path, boolean endsInSlash) {
				if(path.length == 2) {
					return ResultDbReader.getInstance().getPapers();					
				} else {
					String paperIdStr = path[2];
					int paperId = Integer.parseInt(paperIdStr);
					return ResultDbReader.getInstance().getPaperInfo(paperId);
				}
			}
		});
		baseHandlers.put("sentences", new SciBorgHandler() {
			public Document handle(String[] path, boolean endsInSlash) {
				if(path.length == 2) {
					return null;//ResultDbReader.getInstance().getPapers();					
				} else {
					String sentenceIdStr = path[2];
					int sentenceId = Integer.parseInt(sentenceIdStr);
					return ResultDbReader.getInstance().getSentence(sentenceId);
				}
			}
		});		
		baseHandlers.put("rmrs", new SciBorgHandler() {
			public Document handle(String[] path, boolean endsInSlash) {
				if(path.length == 2) {
					return null;//ResultDbReader.getInstance().getPapers();					
				} else {
					String rmrsIdStr = path[2];
					int rmrsId = Integer.parseInt(rmrsIdStr);
					return ResultDbReader.getInstance().getRmrs(rmrsId);
				}
			}
		});		
		baseHandlers.put("ne", new SciBorgHandler() {
			public Document handle(String[] path, boolean endsInSlash) {
				if(path.length == 2) {
					return null;//ResultDbReader.getInstance().getPapers();					
				} else {
					String neIdStr = path[2];
					int neId = Integer.parseInt(neIdStr);
					return ResultDbReader.getInstance().getNe(neId);
				}
			}
		});		
		//System.out.println(baseHandlers);
	}
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		String pathInfo = request.getPathInfo();
		String [] path = new String[0];
		boolean endsInSlash = false;
		if(pathInfo != null) {
			path = pathInfo.split("/");	
			if(pathInfo.endsWith("/")) endsInSlash = true;
		}
		
		Document responseDoc;
		if(path.length == 0) {
			responseDoc = rootHandler.handle(path, endsInSlash);
		} else if(path.length == 1) {
			responseDoc = null;
			// TODO: something sane. This should never happen.
		} else {
			SciBorgHandler handler = baseHandlers.get(path[1]);
			if(handler == null) {
				responseDoc = null;
			}
			responseDoc = handler.handle(path, endsInSlash);
		}
		if(responseDoc == null) {
			response.sendError(500);
		} else if(responseDoc.getRootElement().getLocalName().equals("error")) {
			Element elem = responseDoc.getRootElement();
			int code = Integer.parseInt(elem.getAttributeValue("code"));
			String str = elem.getValue();
			response.sendError(code, str);
		} else {
		    String serverRoot = Oscar3Props.getInstance().serverRoot;
		    if(serverRoot == null || serverRoot.equals("none")) serverRoot = "";
			ProcessingInstruction pi = new ProcessingInstruction("xml-stylesheet", "type=\"text/xsl\" href=\"" + serverRoot + "/sciborg.xsl\"");
			responseDoc.insertChild(pi, 0);
			response.setContentType("application/xml");
			new Serializer(response.getOutputStream()).write(responseDoc);			
		}
	}
		
}
