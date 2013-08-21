package uk.ac.cam.ch.wwmm.oscar3server;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Serializer;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import uk.ac.cam.ch.wwmm.oscar3.Oscar3Props;
import uk.ac.cam.ch.wwmm.oscar3.models.Model;
import uk.ac.cam.ch.wwmm.oscar3server.scrapbook.PaperToScrapBook;
import uk.ac.cam.ch.wwmm.oscar3server.scrapbook.ScoreStats;
import uk.ac.cam.ch.wwmm.oscar3server.scrapbook.ScrapBook;
import uk.ac.cam.ch.wwmm.ptclib.io.ResourceGetter;
import uk.ac.cam.ch.wwmm.ptclib.scixml.SciXMLDocument;
import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;

/** The servlet side of ScrapBook.
 * 
 * @author ptc24
 *
 */
@SuppressWarnings("serial")
public final class ScrapBookServlet extends HttpServlet {

	private ScrapBook scrapBook;
	private Map<String, ScrapBook> scrapBooks;
	private File fileRoot;
	
	private static ResourceGetter rg = new ResourceGetter("uk/ac/cam/ch/wwmm/oscar3server/resources/");
		
	private void initialise() throws ServletException {
		if(fileRoot == null) {
			try {
				fileRoot = ScrapBook.getScrapBookFile();
				scrapBooks = new HashMap<String,ScrapBook>();
				scrapBook = null;			
			} catch (Exception e) {
				throw new ServletException(e);
			}
		}
	}
	
	private void setScrapBook(String name) throws Exception {
		if(name == null || name.length() == 0) scrapBook = null;
		if(scrapBooks.containsKey(name)) {
			scrapBook = scrapBooks.get(name);
		} else {
			scrapBook = new ScrapBook(name);
			scrapBooks.put(name, scrapBook);
		}
	}
	
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}
		
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		initialise(); //This initialises if necessary.
		
		String name = request.getParameter("name");
		String action = request.getParameter("action");
		try {
			if(name != null) {
				setScrapBook(name);
			} else {
				if(action != null && !(action.equals("index") || action.equals("makemodel"))) {
					response.getWriter().println("No scrapbook set!");
					return;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			response.getWriter().println("Scrapbook error!");
			return;
		}

		
		try	{
			if(request.getContentType() != null &&
					request.getContentType().startsWith("multipart/form-data;")) {
				
				FileItemFactory fif = new DiskFileItemFactory();
				ServletFileUpload sfu = new ServletFileUpload(fif);
				List itemList = sfu.parseRequest(request);
				Document paperDoc = null;
				String paperName = null;
				for(Object o : itemList) {
					FileItem item = (FileItem)o;
					if(item.getFieldName().equals("paper")) {
						InputStream is = item.getInputStream();
						paperDoc = new Builder().build(is);
					} else if(item.getFieldName().equals("name")) {
						paperName = item.getString();
					}
				}
				if(paperDoc != null && paperName != null) {
					PaperToScrapBook.makeScrapBook(paperDoc, paperName);
					response.setContentType("text/plain");
					response.getWriter().printf("Paper %s added OK!", paperName);
				} else {
					response.setContentType("text/plain");
					response.getWriter().printf("Couldn't add that paper!");
				}
				return;
			}
			//if(action == null && mpr != null) action = mpr.getString("action");*/
			if(action == null) action = "index";
			
			if(action.equals("addpaper")) {
				/*String paperName = mpr.getString("name");
				Document paperDoc = new Builder().build(mpr.getInputStream("paper"));
				PaperToScrapBook.makeScrapBook(paperDoc, paperName);
				response.setContentType("text/plain");
				response.getWriter().printf("Paper %s added OK!", paperName);*/
			} else if(action.equals("show")) {
				response.setContentType("application/xml");
				SciXMLDocument doc = scrapBook.getDoc();
				doc.addServerProcessingInstructions();
				new Serializer(response.getOutputStream()).write(doc);
			} else if(action.equals("add")) {
				request.setCharacterEncoding("UTF-8");
				String fileno = request.getParameter("fileno");
				String html = request.getParameter("html");
				html = URLDecoder.decode(html, "UTF-8");
				response.setContentType("text/html");
				scrapBook.addScrap(html, fileno);
				PrintWriter out = response.getWriter();
				out.println("<html><head><title>Foo</title></head>");
				out.println("<body>");
				out.println("<script>var t=setTimeout('window.close()', 500)</script>");
				out.println("Added OK!");
				out.println("</body></html>");
			} else if(action.equals("delete")) {
				response.setContentType("text/plain");
				String sid = request.getParameter("sid");
				scrapBook.deleteScrap(sid);
				response.getWriter().println("snippet deleted OK");
			} else if(action.equals("deletebook")) {
				scrapBooks.remove(scrapBook.getName());
				scrapBook.delete();
				response.setContentType("text/plain");
				response.getWriter().printf("Scrapbook deleted OK!");
				scrapBook = null;
			} else if(action.equals("clear")) {
				scrapBook.clearAnnotations();
				response.setContentType("application/xml");
				SciXMLDocument doc = scrapBook.getDoc();
				doc.addServerProcessingInstructions();
				new Serializer(response.getOutputStream()).write(doc);				
			} else if(action.equals("autoannotate")) {
				scrapBook.autoAnnotate();
				response.setContentType("application/xml");
				SciXMLDocument doc = scrapBook.getDoc();
				doc.addServerProcessingInstructions();
				new Serializer(response.getOutputStream()).write(doc);				
			} else if(action.equals("autoannotatereactions")) {
				scrapBook.autoAnnotateReactions();
				response.setContentType("application/xml");
				SciXMLDocument doc = scrapBook.getDoc();
				doc.addServerProcessingInstructions();
				new Serializer(response.getOutputStream()).write(doc);				
			} else if(action.equals("regtest")) {
				response.setContentType("application/xml");
				SciXMLDocument doc = scrapBook.regtest().getDoc();
				doc.addServerProcessingInstructions();
				new Serializer(response.getOutputStream()).write(doc);				
			} else if(action.equals("index")) {
				String s = rg.getString("scrapbookindex.html");
				StringBuffer links = new StringBuffer();

				File [] fileArr = fileRoot.listFiles();
				for(int i=0;i<fileArr.length;i++) {
					if(fileArr[i].isDirectory()) {
						String sbn = fileArr[i].getName();
						links.append("<li><a href=\"ScrapBook?action=show&name=" + sbn);
						links.append("\">" + sbn + "</a>");
						links.append(" ");
						links.append("<a href=\"ScrapBook?action=deletebook&name=" + sbn);
						links.append("\">" + "Delete!" + "</a>");						
						links.append(" ");
						links.append("<a href=\"ScrapBook?action=selectedit&type=subtype&name=" + sbn);
						links.append("\">" + "Edit subtypes" + "</a>");						
						links.append(" ");
						links.append("</li>\n");
					}
				}
				s = s.replace("LINKS GO HERE", links.toString());
				response.setContentType("text/html");
				response.getWriter().print(s);
			} else if(action.equals("requestbooleanedit")) {
				String s = rg.getString("booleanedit.html");
				s = s.replace("PAPERNAME", name);
				response.setContentType("text/html");
				response.getWriter().print(s);				
			} else if(action.equals("regtests")) {
				//ToughWords.deTrain(10);
				response.setContentType("text/plain");
				PrintWriter out = response.getWriter();				
				File [] fileArr = fileRoot.listFiles();
				ScoreStats grandTotal = new ScoreStats();
				for(int i=0;i<fileArr.length;i++) {
					if(fileArr[i].isDirectory()) {
						String sbn = fileArr[i].getName();
						setScrapBook(sbn);
						ScoreStats score = scrapBook.regtest().getScoreStats();
						out.printf("%s %s\n", sbn, score.getPrecAndRecallString());
						grandTotal.addScoreStats(score);
						out.flush();
					}
				}
				out.println("Grand Total:");
				out.println(grandTotal.getPrecAndRecallString());
			} else if(action.equals("edit")) {
				String sid = request.getParameter("sid");
				SciXMLDocument editor = scrapBook.getEditor(sid);
				editor.addServerProcessingInstructions();
				response.setContentType("application/xml");
				new Serializer(response.getOutputStream()).write(editor);				
			} else if(action.equals("reledit")) {
				String sid = request.getParameter("sid");
				SciXMLDocument editor = scrapBook.getRelEditor(sid);
				editor.addServerProcessingInstructions();
				response.setContentType("application/xml");
				new Serializer(response.getOutputStream()).write(editor);				
			} else if(action.equals("selectedit")) {
				String editType = request.getParameter("type");
				SciXMLDocument editor = scrapBook.getSelectorEditor(editType);
				editor.addServerProcessingInstructions();
				response.setContentType("application/xml");
				new Serializer(response.getOutputStream()).write(editor);				
			} else if(action.equals("selecteditsubmit")) {
				if(Oscar3Props.getInstance().verbose) System.out.println("Editing via selects");
				String editType = request.getParameter("type");
				if(Oscar3Props.getInstance().verbose) System.out.println("Editing: editType");
				scrapBook.updateToSelections(request.getParameterMap(), editType);
				if(Oscar3Props.getInstance().verbose) System.out.println("Done: updateToSelections");
				response.setContentType("text/html");
				PrintWriter out = response.getWriter();
				out.println(editType + "s changed OK!");
				out.println("<a href='ScrapBook'>Back to Index</a>");
			} else if(action.equals("booleanedit")) {
				SciXMLDocument editor = scrapBook.getBooleanAttrEditor(request.getParameter("attrName"));
				editor.addServerProcessingInstructions();
				response.setContentType("application/xml");
				new Serializer(response.getOutputStream()).write(editor);				
			} else if(action.equals("booleaneditsubmit")) {
				scrapBook.submitBooleans(request.getParameterMap(), request.getParameter("attrName"));
				response.setContentType("text/plain");
				response.getWriter().println("Booleans submitted OK!");
			} else if(action.equals("textfieldedit")) {
				String editType = request.getParameter("type");
				SciXMLDocument editor = scrapBook.getTextFieldEditor(editType);
				editor.addServerProcessingInstructions();
				response.setContentType("application/xml");
				new Serializer(response.getOutputStream()).write(editor);				
			} else if(action.equals("delne")) {
				String sid = request.getParameter("sid");
				String neid = request.getParameter("neid");
				scrapBook.deleteNe(sid, neid);
				response.setContentType("text/plain");
				response.getWriter().println("Removed ne OK!");
			} else if(action.equals("addne")) {
				String sid = request.getParameter("sid");
				String start = request.getParameter("start");
				String end = request.getParameter("end");
				String type = request.getParameter("type");
				scrapBook.addNe(sid, start, end, type);
				response.setContentType("text/plain");
				response.getWriter().println("Added ne OK!");
			} else if(action.equals("movene")) {
				String sid = request.getParameter("sid");
				String start = request.getParameter("start");
				String end = request.getParameter("end");
				String neid = request.getParameter("neid");
				scrapBook.moveNe(sid, start, end, neid);
				response.setContentType("text/plain");
				response.getWriter().println("Moved ne OK!");
			} else if(action.equals("makepaper")) {
				SciXMLDocument doc = scrapBook.makePaper();
				doc.addServerProcessingInstructions();
				response.setContentType("application/xml");
				new Serializer(response.getOutputStream()).write(doc);								
			} else if(action.equals("makepubxmlpaper")) {
				Document doc = scrapBook.makePubXMLPaper();
				response.setContentType("application/xml");
				new Serializer(response.getOutputStream()).write(doc);								
			} else if(action.equals("attred")) {
				String sid = request.getParameter("sid");
				String neid = request.getParameter("neid");
				SciXMLDocument editor = scrapBook.getAttributeEditor(sid, neid);
				editor.addServerProcessingInstructions();
				response.setContentType("application/xml");
				new Serializer(response.getOutputStream()).write(editor);								
			} else if(action.equals("edattr")) {
				String sid = request.getParameter("sid");
				String neid = request.getParameter("neid");
				Map params = request.getParameterMap();
				scrapBook.editAttributes(sid, neid, params);
				response.sendRedirect("ScrapBook?name=" + name + "&action=edit&sid=" + sid);												
			} else if(action.equals("makemodel")) {
				response.setContentType("text/plain");
				PrintWriter out = response.getWriter();
				String modelname = request.getParameter("modelname");
				if(modelname == null || modelname.length() == 0) {
					out.println("Error: need a model name");
				}
				out.println("OK, making a model.");
				out.println("This may take some time - 15 minutes, an hour, maybe longer?");
				out.flush();
				Model.makeModel(modelname);
				out.println("Make the model OK");
				out.flush();
			} else if(action.equals("comment")) {
				String sid = request.getParameter("sid");
				String comment = request.getParameter("comment");
				scrapBook.setSnippetProperty(sid, comment, "comment");
				response.setContentType("text/plain");
				PrintWriter out = response.getWriter();
				out.println("Comment changed OK!");
			} else if(action.equals("submitrel")) {
				String sid = request.getParameter("sid");
				String relations = request.getParameter("relations");
				if(StringTools.isLackingCloseBracket(relations)) relations += ")";
				scrapBook.setSnippetProperty(sid, relations, "relations");
				response.setContentType("text/plain");
				PrintWriter out = response.getWriter();
				out.println("Relations changed OK!");
			} else if(action.equals("renderrel")) {
				scrapBook.renderRelations();
				response.setContentType("text/plain");
				PrintWriter out = response.getWriter();
				out.println("Relations rendered OK - to your console screen (This is an experimental feature)!");
			}
		} catch (Exception e) {
			e.printStackTrace();
			response.setContentType("text/plain");
			response.getWriter().println("Scrapbook error!");
		}	
	}
	
	

}
