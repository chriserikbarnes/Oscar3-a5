package uk.ac.cam.ch.wwmm.ptc.experimental.termsimilarity;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nu.xom.Element;
import nu.xom.Serializer;
import uk.ac.cam.ch.wwmm.oscar3.Oscar3Props;
import uk.ac.cam.ch.wwmm.oscar3server.AjaxResponse;
import uk.ac.cam.ch.wwmm.ptclib.io.FileTools;
import uk.ac.cam.ch.wwmm.ptclib.scixml.SciXMLDocument;
import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;

/** An experimental servlet for performing thesaurus extraction-related 
 * techinques.
 * 
 * @author ptc24
 *
 */
@SuppressWarnings("serial")
public class TermSimilarityServlet extends HttpServlet {

	private SimilarityExtractor similarityExtractor;
	private File similarityCorpus;
	
	public TermSimilarityServlet() throws Exception {
		similarityExtractor = null;
	}
	
	@Override
	protected void doPost(HttpServletRequest arg0, HttpServletResponse arg1) throws ServletException, IOException {
		doGet(arg0, arg1);
	}
	
	public void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
			
		/*System.out.println(request);
		BufferedReader br = new BufferedReader(new InputStreamReader(request.getInputStream()));
		String line = br.readLine();
		while(line !=  null) {
			System.out.println(line);
			line = br.readLine();
		}*/
		
		// Handle AJAX
		if(request.getParameter("function") != null) {
			System.out.println("AJAX!");
			StringWriter sw = new StringWriter();
			PrintWriter out = new PrintWriter(sw);
			if(request.getParameter("listTerms") != null) {
				for(String s : similarityExtractor.getTerms()) {
					out.println(s);
				}
			} else if(request.getParameter("similarTo") != null) {
				String similarTo = request.getParameter("similarTo");
				Set<String> words = new HashSet<String>();
				String [] lines = similarTo.split("\\s*\n\\s*");
				for(int i=0;i<lines.length;i++) {
					words.add(StringTools.normaliseName(lines[i]).replaceAll("\\s+", "_"));
				}
				Map<String,Double> similar = similarityExtractor.getSimilarToTermSetNearest(words);
				for(String s : StringTools.getSortedList(similar)) {
					out.println(s);// + "\t" + similar.get(s));
				}
			} else if(request.getParameter("startsWith") != null) {
				String startsWith = request.getParameter("startsWith");
				for(String s : similarityExtractor.getTerms()) {
					if(s.startsWith(startsWith)) out.println(s);
				}
			}
			try {
				AjaxResponse.doAjaxResponse(request, response, sw.toString());					
			} catch (Exception e) {
				e.printStackTrace();
			}			
			return;
		}
		
		if(request.getParameter("selectcorpus") != null) {
			SciXMLDocument doc = new SciXMLDocument();
			doc.setTitle("Select corpus to analyse");
			Element list = doc.addList();
			File [] dirs = new File(Oscar3Props.getInstance().workspace, "corpora").listFiles();
			for(int i=0;i<dirs.length;i++) {
				String name = dirs[i].getName();
				if(dirs[i].isDirectory() && !name.startsWith("."))	{
					Element a = doc.makeLink("TermSimilarity?setcorpus=" + URLEncoder.encode(name, "UTF-8"), name);
					list.appendChild(doc.makeListItem(a));
				}
			}

			//Element a = doc.makeLink("Search?settoscrap", "ScrapBook");
			//list.appendChild(doc.makeListItem(a));

			//a = doc.makeLink("Search?indexall", "Index entire workspace");
			//list.appendChild(doc.makeListItem(a));

			doc.addServerProcessingInstructions();
			response.setContentType("application/xml");
			new Serializer(response.getOutputStream()).write(doc);
			return;
		}

		if(request.getParameter("setcorpus") != null) {
			response.setContentType("text/plain");
			PrintWriter out = response.getWriter();
			try {
				similarityCorpus = new File(new File(Oscar3Props.getInstance().workspace, "corpora"), request.getParameter("setcorpus"));
				List<File> fileList = FileTools.getFilesFromDirectoryByName(similarityCorpus, "markedup.xml");
				FeatureVectorExtractor fve = new InlineFVE(fileList);
				//FeatureVectorExtractor fve = new EntityFVE(fileList);
				similarityExtractor = new SimilarityExtractor(fve, new MinLRDiceSimilarity(), new TTestWeighting());
				//similarityExtractor = new SimilarityExtractor(fve, new CosSimilarity(), new TTestWeighting());
				out.println("Corpus changed to " + request.getParameter("setcorpus") + " OK!");
				response.sendRedirect("TermSimilarity.html");
			} catch (Exception e) {
				e.printStackTrace();
				out.println("Yikes, that didn't work!");
			}
			return;
		}
		
		if(request.getParameter("similarTo") != null) {
			String similarTo = request.getParameter("similarTo");
			Set<String> words = new HashSet<String>();
			String [] lines = similarTo.split("\\s*\n\\s*");
			for(int i=0;i<lines.length;i++) {
				words.add(StringTools.normaliseName(lines[i]).replaceAll("\\s+", "_"));
			}
			response.setContentType("text/plain");
			PrintWriter out = response.getWriter();
			Map<String,Double> similar = similarityExtractor.getSimilarToTermSetNearest(words);
			for(String s : StringTools.getSortedList(similar)) {
				out.println(s);// + "\t" + similar.get(s));
			}
		}
		
		if(request.getParameter("listTerms") != null) {
			System.out.println("listing terms");
			response.setContentType("text/plain");
			PrintWriter out = response.getWriter();
			for(String s : similarityExtractor.getTerms()) {
				out.println(s);
			}
			out.close();
		}
	}

}
