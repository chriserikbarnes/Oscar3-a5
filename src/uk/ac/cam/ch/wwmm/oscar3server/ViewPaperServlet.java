package uk.ac.cam.ch.wwmm.oscar3server;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Node;
import nu.xom.Nodes;
import nu.xom.Serializer;
import uk.ac.cam.ch.wwmm.oscar3.Oscar3;
import uk.ac.cam.ch.wwmm.oscar3.Oscar3Props;
import uk.ac.cam.ch.wwmm.oscar3.Traceability;
import uk.ac.cam.ch.wwmm.oscar3.sciborg.ResultDbReader;
import uk.ac.cam.ch.wwmm.oscar3server.scrapbook.PaperToScrapBook;
import uk.ac.cam.ch.wwmm.ptclib.io.FileTools;
import uk.ac.cam.ch.wwmm.ptclib.scixml.SciXMLDocument;
import uk.ac.cam.ch.wwmm.ptclib.scixml.XMLStrings;
import uk.ac.cam.ch.wwmm.ptclib.xml.XOMTools;

/** Views a paper or paper directory in your corpora directory.
 * 
 * @author ptc24
 *
 */
@SuppressWarnings("serial")
public final class ViewPaperServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		if(request.getPathInfo() == null) {
			response.sendRedirect("ViewPaper/");
			return;
		}
		
		File f = new File(new File(Oscar3Props.getInstance().workspace, "corpora"), request.getPathInfo());
		File markedup = new File(f, "markedup.xml");
		if(!markedup.exists() && new File(f, "source.xml").exists()) {
			markedup = new File(f, "source.xml");
		}

		if(markedup.exists()) {
			try {
				if(request.getParameter("toscrapbook") != null) {
					String mode = request.getParameter("toscrapbook");
					boolean noExperimental = mode.equals("noexperimental");
					boolean noCaptions = mode.equals("nocaptions");
					Document sourceDoc = new Builder().build(new File(f, "source.xml"));					
					PaperToScrapBook.makeScrapBook(sourceDoc, f.getName(), noExperimental, noCaptions);
					response.setContentType("text/plain");
					response.getWriter().write("Paper added OK");
				} else {
					SciXMLDocument doc = SciXMLDocument.makeFromDoc(new Builder().build(markedup));
					if(request.getParameter("minConf") != null) {
						double minConf = Double.parseDouble(request.getParameter("minConf"));
						Nodes n = doc.query("//ne[@confidence]");
						for(int i=0;i<n.size();i++) {
							Element e = (Element)n.get(i);
							double conf = Double.parseDouble(e.getAttributeValue("confidence"));
							if(conf < minConf) {
								XOMTools.removeElementPreservingText(e);
							}
						}
					}
					
					Element p = doc.addPara();
					Element a = doc.makeLink(request.getRequestURI() + "?toscrapbook=full", "Put in scrapbook");
					p.appendChild(a);
					p.appendChild(" ");
					a = doc.makeLink(request.getRequestURI() + "?toscrapbook=noexperimental", "Put in scrapbook (No experimental or captions)");
					p.appendChild(a);
					p.appendChild(" ");
					a = doc.makeLink(request.getRequestURI() + "?toscrapbook=nocaptions", "Put in scrapbook (No captions)");
					p.appendChild(a);
					try {
						int paperId = ResultDbReader.getInstance().getPaperId(f);
						if(paperId > -1) {
							p.appendChild(" ");
							a = doc.makeLink("/SciBorg/papers/" + paperId, " SciBorg Paper " + paperId);						
							p.appendChild(a);
						}						
					} catch (RuntimeException e) {
						//e.printStackTrace();
					}
					
					if(markedup.getName().equals("source.xml")) {
						doc.addServerProcessingInstructions();
						doc.removeViewerProcessingInstruction();
					} else {
						doc.addServerProcessingInstructions();						
					}
					response.setContentType("application/xml");
					new Serializer(response.getOutputStream()).write(doc);
				}
			} catch (Exception e) {
				response.sendError(404);
			}		
		} else if(f.isDirectory()) {
			/* Make sure the browser treats it like one... */
			if(!request.getPathInfo().endsWith("/")) {
				String newPath = request.getPathInfo().substring(request.getPathInfo().lastIndexOf('/') + 1);
				response.sendRedirect(newPath + "/");
				return;
			}
			
			if(request.getParameter("reprocess") != null) {
				List<File> files = FileTools.getFilesFromDirectoryByName(f, "source.xml");
				String traceability = Traceability.getTraceabilityInfo();
				for(File ff : files) {
					if(Oscar3Props.getInstance().verbose) System.out.println(ff.getParentFile());
					Oscar3.processDirectory(ff.getParentFile(), traceability);
				}
				response.sendRedirect(".");
			} else if(request.getParameter("importall") != null) {
				String mode = request.getParameter("importall");
				boolean noExperimental = mode.equals("noexperimental");
				boolean noCaptions = mode.equals("nocaptions");
				List<File> files = FileTools.getFilesFromDirectoryByName(f, "source.xml");
				try {
					for(File ff : files) {
						Document sourceDoc = new Builder().build(ff);					
						PaperToScrapBook.makeScrapBook(sourceDoc, ff.getParentFile().getName(), noExperimental, noCaptions);					
					}					
				} catch (Exception e) {
					e.printStackTrace();
					return;
				}
				response.setContentType("text/plain");
				response.getWriter().write("Imported all files to scrapbook OK");
				return;
			}
			
			SciXMLDocument doc = new SciXMLDocument();
			doc.addServerProcessingInstructions();
			Element para = doc.addPara();
			para.appendChild(doc.makeLink("/ViewPaper" + request.getPathInfo() + "?reprocess", "Reprocess this directory"));
			
			Element list = doc.addList();
			
			File [] children = f.listFiles();
			for(int i=0;i<children.length;i++) {
				if(children[i].isDirectory()) {
					Element a = doc.makeLink("./" + children[i].getName());
					File cm = new File(children[i], "markedup.xml");
					if(!cm.exists()) cm = new File(children[i], "source.xml");
					if(cm.exists()) {
						try {
							Nodes nn = new Builder().build(cm).query(XMLStrings.getInstance().TITLE_XPATH, XMLStrings.getInstance().getXpc());
							if(nn.size() > 0){
								Node n = nn.get(0);
								n.detach();
								a.appendChild(n);
							} else {								
								a.appendChild(children[i].getName());						
							}
						} catch (Exception e) {
							e.printStackTrace();
							a.appendChild(children[i].getName());						
						}
					} else {
						a.appendChild(children[i].getName());												
					}
					list.appendChild(doc.makeListItem(a));
				}
			}
			response.setContentType("application/xml");
			new Serializer(response.getOutputStream()).write(doc);			
		}
		

	}
	
}
