package uk.ac.aber.art_tool;


import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Serializer;
import nu.xom.xslt.XSLTransform;

import org.json.JSONObject;

import uk.ac.cam.ch.wwmm.ptclib.io.ResourceGetter;

/** The servlet side of ScrapBook.
 * This class receives all POST and GET requests sent to /ScrapBook and responds accordingly.
 * @author Maria Liakata, cim, ptc24
 *
 */
@SuppressWarnings("serial")
public class ARTServlet extends HttpServlet {
	//a sorted map of the ScrapBook objects
	private SortedMap<String, AnnotatedPaper> papers;
	//The root directory for the folders containing scrapbook.xml and source.xml files 
	private File fileRoot;
	
	//used to retrieve html files on the server
	private static ResourceGetter rg;
	private static ResourceGetter SAPIENTrg = new ResourceGetter("uk/ac/aber/art_tool/art_tool_web/");
	private static ResourceGetter OSCARrg = new ResourceGetter("uk/ac/cam/ch/wwmm/oscar3server/resources/fullweb/art_tool_web/");
	private static String serverType = ""; 
	
	//creates an instance of the servlet, populating the Map with annotated papers for each file in the directory
	public ARTServlet() throws Exception {
		fileRoot = AnnotatedPaper.getScrapBookFile();
		if(!fileRoot.exists()) fileRoot.mkdir();
		papers = new TreeMap<String,AnnotatedPaper>();		
		File [] fileArr = fileRoot.listFiles();
		for(int i=0;i<fileArr.length;i++) {
			if(fileArr[i].isDirectory()) {
				String filename = fileArr[i].getName();
				papers.put(filename, new AnnotatedPaper(filename));
			}
		}
	}
	
	/**Calls doGet() with whatever parameters the post had
	 * @param HttpServletRequest the http POST request
	 * @param HttpServletResponse the http POST response
	 */
	public void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {		
		doGet(request,response);
	}
		
	/**Deals with all post requests as well as all get requests
	 * @param HttpServletRequest Either the GET or POST request as received by the server
	 * @param HttpServletResponse Either the GET or POST response as received by the server
	 */
	public void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {		
		//DEBUG		
		System.out.println("USING ARTServlet");
		String name = request.getParameter("name");		
		String papername = request.getParameter("papername");		
		String paperfile = request.getParameter("paperfile");		
		String action = request.getParameter("action");
		String modestring = request.getParameter("mode");
		
		int mode = 0;
		if(modestring != null) {
			mode = Integer.parseInt(modestring);
		}
		System.err.println("action:" + action);
		
		try {
			if(name != null) {//action = show/deletebook
				//System.out.println("scrapbook set -- name");
				//setAnnotatedPaper(name);
			} else {
				//This may not be entirely necessary anymore -- cim
				if(action != null && 
				!(action.equals("savePaperMode2")) &&	
				!(action.equals("addlinks")) && 
				!(action.equals("addpaper")) && 
				!(action.equals("index")) &&
				!(action.equals("oscarindex")) && !(action.equals("help"))) {
					response.getWriter().println("No paper name passed in!");
					return;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			response.getWriter().println("ART error!");
			return;
		}
		
		try {
			//in case a request goes to <domain>/Scrapbook without an action
			if(action == null) action = "index";
			System.out.println("action = " + action);
			//For uploading a paper to the server and adding it to the scrapbook map
			if(action.equals("addpaper")) {	
				System.err.println("GOT HERE FIRST");
				String resp = addPaper(papername,paperfile);
				System.err.println("GOT HERE");
//				for(String s: papers.keySet()) {
//					System.err.println("ADD Map key: " + s);
//				}
				response.setContentType("text/plain");
				response.getWriter().print(resp);
			//For displaying a paper to the user
			} else if(action.equals("deletebook")) {
			
				AnnotatedPaper ap = papers.get(name);
				papers.remove(name);
//				for(String s: papers.keySet()) {
//					System.err.println("DELETE Map key: " + s);
//				}
				ap.delete();
				String filenames = addLinks();
				System.err.println(filenames);
				response.setContentType("text/plain");
				response.getWriter().print(filenames);
			} else if(action.equals("makepaper")) {
				ARTSciXMLDocument doc = papers.get(name).makePaper();
				doc.addServerProcessingInstructions();
				response.setContentType("application/xml");
				new Serializer(response.getOutputStream()).write(doc);								
			} else if(action.equals("showmode1")) {
				String sid = request.getParameter("sid");
				System.err.printf("sid: %s",sid);
				AnnotatedPaper ap = papers.get(name);
				ARTSciXMLDocument editor = ap.getEditor(sid);
				editor.addServerProcessingInstructions();
				response.setContentType("application/xml");
				new Serializer(response.getOutputStream()).write(editor);				
			//for clearing the annotations added by OSCAR
			} else if(action.equals("getcomments")) { 
				AnnotatedPaper ap = papers.get(name);
				String commentsString = ap.getCommentsAsString();
				response.setContentType("text/plain");
				response.getWriter().print(commentsString);
			} else if(action.equals("showmode2")) {
				//String sid = request.getParameter("sid");
				//if mode2 doc doesn't exist create & display it
				AnnotatedPaper ap = papers.get(name);
				ARTSciXMLDocument editor;
				if(ap.hasMode2Doc()) {
					editor = ap.getMode2Doc();
				} else {
					editor = ap.makeMode2Doc();
				}
				if(serverType.equals("oscar")) {
					editor.addMode2OSCARPI();
				} else if(serverType.equals("sapient")) {
					editor.addMode2SapientPI();
				}
				response.setContentType("application/xml");
				new Serializer(response.getOutputStream()).write(editor);
			    //if mode2 doc exists, retrieve & display it
			} else if(action.equals("savePaperMode2")) {
				 System.out.println("Got to the Servlet");
				 AnnotatedPaper ap = papers.get(name);
				 ap.writeComments(request.getParameter("comment"));
				 ARTSciXMLDocument mode2doc = ap.getMode2Doc();
				 //System.out.println("editor:" + mode2doc.toString());
				 JSONObject conceptJSON = new JSONObject(request.getParameter("conceptJSON"));
				 JSONObject subtypeJSON = new JSONObject(request.getParameter("subtypeJSON")); 
				  //String Message = objJSON.get("conceptHash").toString();
                  System.out.println("The conceptHash as string is :" + conceptJSON.toString());
                  System.out.println("The subtypeHash as string is :" + subtypeJSON.toString());
		        mode2doc.updateMode2XML(conceptJSON, subtypeJSON);
		        //ap.writeMode2Doc(mode2doc);
		        String mode2DocAsString = mode2doc.toXML();
		        //System.out.println("before\n"+mode2DocAsString);
		        String chevronned = addChevrons(mode2DocAsString);
		       // System.out.println("after\n"+chevronned);
		        ap.writeStringToMode2File(chevronned);
		        ap.getMode2Doc();
		        response.setContentType("text/plain");
				response.getWriter().print(conceptJSON.toString());
				response.getWriter().print(subtypeJSON.toString());
				//response.setContentType("text/plain");
				//response.getWriter().print("Hello!");
				
			} else if(action.equals("clearARTAnnotations")) {
				System.out.println("in Clear ART Annotations");
				AnnotatedPaper ap = papers.get(name);
				ARTSciXMLDocument mode2doc;
					mode2doc = ap.getMode2Doc();
					//Builder builder = new Builder();
					//System.out.println("mode2doc: " + mode2doc.toString());	
					System.out.println("rg: " + rg.toString());
					
				     Document stylesheet = rg.getXMLDocument("xsl/clearMode2.xsl");
				     System.out.println("Found clearMode2.xsl");
				     XSLTransform transform = new XSLTransform(stylesheet);
				     Document clearDoc = XSLTransform.toDocument(transform.transform(mode2doc));
				     ARTSciXMLDocument clearedDoc = ARTSciXMLDocument.makeFromDoc(clearDoc);
				     ap.writeMode2Doc(clearedDoc);
				     response.setContentType("text/plain");
				     response.getWriter().print(name);
				     /*mode2doc = ap.getMode2Doc();
				     if(serverType.equals("oscar")) {
				    	 mode2doc.addMode2OSCARPI();
					 } else if(serverType.equals("sapient")) {
						mode2doc.addMode2SapientPI();
					 }
				    response.setContentType("application/xml");
					new Serializer(response.getOutputStream()).write(mode2doc);*/
			} else if(action.equals("clear")) {
				AnnotatedPaper ap = papers.get(name);
				ARTSciXMLDocument doc;
				if(mode == 1) {
					ap.clearAnnotations();				
					doc = ap.getDoc();
					doc.addServerProcessingInstructions();
				} else {
					ap.clearAnnotations2();	
					doc = ap.getMode2Doc();
					if(serverType.equals("oscar")) {
						doc.addMode2OSCARPI();
					} else if(serverType.equals("sapient")) {
						doc.addMode2SapientPI();
					}					
				}
				response.setContentType("application/xml");
				new Serializer(response.getOutputStream()).write(doc);
			//for annotation using OSCAR
			} else if(action.equals("autoannotate")) {
				AnnotatedPaper ap = papers.get(name);
				

				ARTSciXMLDocument doc;
				if(mode == 2){					
					doc = ap.getMode2Doc();
					doc = ap.autoAnnotate2();
					if(serverType.equals("oscar")) {
						doc.addMode2OSCARPI();
					} else if(serverType.equals("sapient")) {
						doc.addMode2SapientPI();
					}
				} else {					
					doc = ap.getDoc();
					ap.autoAnnotate();
					doc.addServerProcessingInstructions();
				}
				response.setContentType("application/xml");
				new Serializer(response.getOutputStream()).write(doc);
			//for getting the list of available scrapbooks and returning them to the browser to be displayed
			} else if(action.equals("addlinks")) {
				serverType = request.getParameter("servertype");
				if(serverType.equalsIgnoreCase("oscar")) {
					rg = OSCARrg;
				} else if(serverType.equalsIgnoreCase("sapient")) {
					rg = SAPIENTrg;
				}
				System.err.println("Server Type is: " + serverType);
				String filenames = addLinks();
				response.setContentType("text/plain");
				response.getWriter().print(filenames);
			//for loading the scrapbook index page
			} else if (action.equals("oscarindex")) {
				serverType = "oscar";
				rg = OSCARrg;				
				response.getWriter().print(rg.getString("artindex.html"));
				
			} else if (action.equals("index")) {
				serverType = "sapient";
				rg = SAPIENTrg;
				System.err.println("Server Type is: " + serverType);
				response.getWriter().print(rg.getString("index.html"));
			} else if (action.equals("help")) {
				response.getWriter().print(rg.getString("SAPIENT_FAQ.html"));
			}
		//in case of error. At the moment, not very specific.
		} catch (Exception e) {
			e.printStackTrace();
			response.setContentType("text/plain");
			response.getWriter().println("ART error!");
		}	
	}
	
	/**
	 * @param papername a String containing the name of the paper to be uploaded
	 * @param paperfile a String containing the contents of the file 
	 * encoded UTF-8
	 * @return a String that is the list of filenames now in the Map
	 */
	public String addPaper(String papername, String paperfile) {
		try {
			//reads from a string that is the contents of the file
			StringReader sr = new StringReader(paperfile);
			//makes a xom.Document from the string
			Document paperDoc = new Builder().build(sr);
			//if there is a document created and if there is a name for the paper
			if(paperDoc != null && papername != null) {
				//Saves the paper to file and returns a scrapBook object
				AnnotatedPaper ap = ConvertToAnnotatedPaper.makeAnnotatedPaper(paperDoc, papername);
				//adds the returned scrapBook to the map (key=papername, value=scrapBook)
				papers.put(papername, ap);
				//retrieves the list of filenames
				String filenames = addLinks();
				//DEBUG
				//for(String s: papers.keySet()) {
				//	System.err.println("Map key: " + s);
				//}
				System.err.println("GOT TO THIS POINT");
				//returns the filenames list as a string
				return filenames;			
			//no name or no paper
			} else {
				//this should never occur, if so an Exception will be raised in doGet()
				return null;
			}
		//catch all exceptions. Perhaps this should be more specific.
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}		
	}
	
	/**
	 * facilitates the list of paper links at the top of the index page by getting a 
	 * list of available files from the Map
	 * @return the string containing comma separated filenames
	 */
	public String addLinks() {
		//malleable String that can be added to without creating a new String every time
		//for appending the filenames
		StringBuffer filenames = new StringBuffer();
		//appends each key from the map to the StringBuffer
		for(String s: papers.keySet()) {
			filenames.append(s);
			filenames.append(",");
		}
		//returns the final String
		return filenames.toString();
	}
	
	public String addChevrons(String docString) {
		docString = docString.replace("sapientOpenChevron", "<");
		docString = docString.replace("sapientCloseChevron", ">");
		docString = docString.replace("sapientAmpersand", "&");
		return docString;
	}
	

	
}
