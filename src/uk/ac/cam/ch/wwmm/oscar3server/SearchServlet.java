package uk.ac.cam.ch.wwmm.oscar3server;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nu.xom.Element;
import nu.xom.Serializer;
import uk.ac.cam.ch.wwmm.oscar3.Oscar3Props;
import uk.ac.cam.ch.wwmm.oscar3.indexersearcher.Concordance;
import uk.ac.cam.ch.wwmm.oscar3.indexersearcher.ConcordanceEntry;
import uk.ac.cam.ch.wwmm.oscar3.indexersearcher.LuceneIndexerSearcher;
import uk.ac.cam.ch.wwmm.oscar3.indexersearcher.UserQuery;
import uk.ac.cam.ch.wwmm.ptclib.scixml.SciXMLDocument;
import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;

/** Servlet interface for searching of annotated papers.
 * 
 * @author ptc24
 *
 */
@SuppressWarnings("serial")
public final class SearchServlet extends HttpServlet {
	
	@Override
	protected void doPost(HttpServletRequest arg0, HttpServletResponse arg1) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(arg0, arg1);
	}
	
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		
		LuceneIndexerSearcher indexerSearcher;
		try {
			indexerSearcher = new LuceneIndexerSearcher(false);			
		} catch (Exception e) {
			PrintWriter out = response.getWriter();
			e.printStackTrace(out);
			return;
		}
		
		if(request.getParameter("suggest") != null) {
			try {
				String start = request.getParameter("suggest");
				boolean ontological = "yes".equals(request.getParameter("ontological"));
				String json = indexerSearcher.suggestJSON(start, ontological);
				response.setContentType("application/x-suggestions+json");
				response.getWriter().write(json);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			return;
		}
		
		if(request.getParameter("selectcorpus") != null) {
			SciXMLDocument doc = new SciXMLDocument();
			doc.setTitle("Select corpus for search");
			Element list = doc.addList();
			File [] dirs = new File(Oscar3Props.getInstance().workspace, "corpora").listFiles();
			for(int i=0;i<dirs.length;i++) {
				String name = dirs[i].getName();
				if(dirs[i].isDirectory() && !name.startsWith("."))	{
					Element a = doc.makeLink("Search?setcorpus=" + URLEncoder.encode(name, "UTF-8"), name);
					list.appendChild(doc.makeListItem(a));
				}
			}

			Element a = doc.makeLink("Search?settoscrap", "ScrapBook");
			list.appendChild(doc.makeListItem(a));

			a = doc.makeLink("Search?indexall", "Index entire workspace");
			list.appendChild(doc.makeListItem(a));

			doc.addServerProcessingInstructions();
			response.setContentType("application/xml");
			new Serializer(response.getOutputStream()).write(doc);
			return;
		}
		
		if(request.getParameter("settoscrap") != null) {
			response.setContentType("text/plain");
			PrintWriter out = response.getWriter();
			try {
				indexerSearcher = new LuceneIndexerSearcher(true);
				indexerSearcher.addScrapBook();
				out.println("Corpus changed to scrapbook OK!");
			} catch (Exception e) {
				out.println("Yikes, that didn't work!");
			}
			return;		
		}
		
		if(request.getParameter("indexall") != null) {
			response.setContentType("text/plain");
			PrintWriter out = response.getWriter();
			try {
				indexerSearcher = new LuceneIndexerSearcher(true);
				indexerSearcher.addScrapBook();
				indexerSearcher.addDirectory(new File(Oscar3Props.getInstance().workspace, "corpora"), out);
				out.println("Indexed entire workspace OK");
			} catch (Exception e) {
				out.println("Yikes, that didn't work!");
			}
			return;		
			
		}
		
		if(request.getParameter("setcorpus") != null) {
			response.setContentType("text/plain");
			PrintWriter out = response.getWriter();
			try {
				File activeCorpus = new File(new File(Oscar3Props.getInstance().workspace, "corpora"), request.getParameter("setcorpus"));
				indexerSearcher = new LuceneIndexerSearcher(true);
				indexerSearcher.addDirectory(activeCorpus, new PrintWriter(System.out, true));
				out.println("Corpus changed to " + request.getParameter("setcorpus") + " OK!");
			} catch (Exception e) {
				e.printStackTrace();
				out.println("Yikes, that didn't work!");
			}
			return;
		}

		if(request.getParameter("concordanceresults") != null) {
			response.setContentType("text/plain");
			PrintWriter out = response.getWriter();
			int number = Integer.parseInt(request.getParameter("number"));
			for(int i=0;i<number;i++) {
				out.println(request.getParameter("s" + i));
				out.println(URLDecoder.decode(request.getParameter("s" + i + "start"), "UTF-8"));
				out.println(URLDecoder.decode(request.getParameter("s" + i + "end"), "UTF-8"));
				out.println(URLDecoder.decode(request.getParameter("s" + i + "file"), "UTF-8"));
				out.println();
			}
			return;
		}
		
		if(request.getParameter("concordance") != null) {
			try {
				String word = request.getParameter("concordance");
				String word1 = null;
				String word2 = null;
				if(word.matches("\\S+\\s+\\S+")) {
					String [] ww = word.split("\\s+");
					word1 = ww[0];
					word2 = ww[1];
				}
				String mode = request.getParameter("mode");
				if("form".equals(mode)) {
					String optstr = request.getParameter("types");
					List<String> options = StringTools.arrayToList(optstr.split("\\s+"));
					List<ConcordanceEntry> ces = Concordance.makeConcordance(indexerSearcher.filesForWord(word), word, 150, mode);									
					response.setContentType("text/html");
					PrintWriter out = response.getWriter();
					out.println("<html><head><title>foo</title></head><body><form method='POST' action='Search'>");
					out.println("<input name='concordanceresults' type='hidden' value='foo'>");					
					out.println("<table>");
					
					boolean shade = false;
					int i=0;
					for(ConcordanceEntry ce : ces) {
						out.print("<tr" + (shade ? " style='background-color: lightgrey'" : "") + ">");
						out.print("<td><tt>" + ce.text + "</tt></td>");
						out.print("<td><select name='s" + i + "'>");
						for(String option : options) {
							out.print("<option value='" + StringTools.urlEncodeUTF8NoThrow(option) + "'>");
							out.print(option);
							out.print("</option>");
						}
						out.print("</select></td>");
						out.println("</tr>");
						out.println("<input type='hidden' name='s" + i + "start' value='" + StringTools.urlEncodeUTF8NoThrow(ce.start) + "'>");
						out.println("<input type='hidden' name='s" + i + "end' value='" + StringTools.urlEncodeUTF8NoThrow(ce.end) + "'>");
						out.println("<input type='hidden' name='s" + i + "file' value='" + StringTools.urlEncodeUTF8NoThrow(ce.file) + "'>");
						shade = !shade;
						i++;
					}			
					out.println("</table>");
					out.println("<input type='hidden' name='number' value='" + i + "'>");
					out.println("<input type='submit' value='Submit'>");
					out.println("</form></body></html>");
				} else {
					response.setContentType("text/plain");	
					PrintWriter out = response.getWriter();
					long time = System.currentTimeMillis();
					List<String> concordance;
					if(word1 == null) {
						List<ConcordanceEntry> ces = Concordance.makeConcordance(indexerSearcher.filesForWord(word), word, 160, mode);									
						concordance = new ArrayList<String>();
						for(ConcordanceEntry ce : ces) {
							concordance.add(ce.text);// + "\t" + ce.file + "\t" + ce.start + "\t" + ce.end);
						}
					} else {
						concordance = Concordance.biConcordance(indexerSearcher.filesForWordPair(word1, word2), word1, word2, 160, mode);														
					}
					if(Oscar3Props.getInstance().verbose) System.out.println(System.currentTimeMillis() - time);
					for(String line : concordance) out.println(line);					
				}
			} catch (Exception e) {
				e.printStackTrace();
			}	
			return;
		}
		
		SciXMLDocument resultsDoc = null;
		
		if(request.getParameter("resultsType").equals("bigCompoundsList")) {
			try {
				resultsDoc = indexerSearcher.getBigCompoundsList();
			} catch (Exception e) {
				e.printStackTrace();
			}
		/*} else if(request.getParameter("resultsType").equals("pubchem")) {
			try {
				resultsDoc = indexerSearcher.getBigShoppingList();
				FetchFromPubChem.fillCNDFromCompoundsList(resultsDoc);
			} catch (Exception e) {
			 	e.printStackTrace();
			}*/
		} else {
			UserQuery.ResultsType rt;
			
			if(request.getParameter("resultsType").equals("compoundsList")) {
				rt = UserQuery.ResultsType.COMPOUNDSLIST;
			} else if(request.getParameter("resultsType").equals("hitsList")) {
				rt = UserQuery.ResultsType.HITSLIST;
			} else if(request.getParameter("resultsType").equals("assoc")) {
				rt = UserQuery.ResultsType.ASSOC;
			} else {
				rt = UserQuery.ResultsType.SNIPPETS;			
			}
			
			int size = 5;
			int skip = 0;
			
			if(request.getParameter("size") != null) size = Integer.parseInt(request.getParameter("size"));
			if(request.getParameter("skip") != null) skip = Integer.parseInt(request.getParameter("skip"));
			
			UserQuery uq = new UserQuery(rt, size, skip);
			if(request.getParameter("morelikethis") != null) {
				uq.setToMoreLikeThis(Integer.parseInt(request.getParameter("morelikethis")));
			} 
			if(request.getParameter("query") != null) {
				String query = request.getParameter("query");
				String queryType = request.getParameter("type");
				String parameter = request.getParameter("parameter");
				
				uq.addTerm(query, queryType, parameter);
				
				for(int i=2;request.getParameter("query" + Integer.toString(i)) != null;i++) {
					query = request.getParameter("query" + Integer.toString(i));
					queryType = request.getParameter("type" + Integer.toString(i));
					parameter = request.getParameter("parameter" + Integer.toString(i));
					uq.addTerm(query, queryType, parameter);			
				}
				
			}
		
			try {
				resultsDoc = indexerSearcher.getResultsByUserQuery(uq);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		if(resultsDoc != null) {
			response.setContentType("application/xml");
			resultsDoc.addServerProcessingInstructions();
			new Serializer(response.getOutputStream()).write(resultsDoc);
		}
	}
}
