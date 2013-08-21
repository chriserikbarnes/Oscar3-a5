package uk.ac.cam.ch.wwmm.oscar3server;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uk.ac.cam.ch.wwmm.oscar3.models.ExtractTrainingData;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.StringSource;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.Tokeniser;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.TokenSequence;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.tokenanalysis.NGram;
import uk.ac.cam.ch.wwmm.ptclib.datastruct.Bag;
import uk.ac.cam.ch.wwmm.ptclib.io.FileTools;
import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;

public final class UnknownWordsServlet extends HttpServlet {

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String mode = request.getParameter("mode");
		
		if(mode == null) {
			response.setContentType("text/html");
			
			PrintWriter out = response.getWriter();
			
			Set<String> knownWords = new HashSet<String>();
			knownWords.addAll(NGram.getInstance().chemSet);
			knownWords.addAll(NGram.getInstance().engSet);
			knownWords.addAll(ExtractTrainingData.getInstance().polysemous);
			
			List<File> files = new ArrayList<File>();
			//files = FileTools.getFilesFromDirectoryByName(new File("/home/ptc24/oscarworkspace/corpora/paperset1"), "source.xml");
			//files = FileTools.getFilesFromDirectoryByName(new File("/home/ptc24/newows/corpora/paperset1"), "source.xml");
			//files = FileTools.getFilesFromDirectoryByName(new File("/home/ptc24/newows/corpora/BioIE"), "source.xml");
			files = FileTools.getFilesFromDirectoryByName(new File("/home/ptc24/newows/corpora/roughPubMed"), "source.xml");
			//files = FileTools.getFilesFromDirectoryByName(new File("/scratch/pubmed/2005"), "source.xml");
			StringSource ss = new StringSource(files, false);
			
			Bag<String> wordCounts = new Bag<String>();
			
			ss.reset();
			for(String s : ss) {
				TokenSequence t = Tokeniser.getInstance().tokenise(s);
				for(String word : t.getTokenStringList()) {
					if(!word.matches(".*[a-z][a-z].*")) continue;
					word = StringTools.normaliseName(word);
					if(!knownWords.contains(NGram.parseWord(word))) wordCounts.add(word);
				}
			}	
			
			Map<String,Double> wordScores = new HashMap<String,Double>();
			double totalScore = 0.0;
			for(String word : wordCounts.getSet()) {
				double prob = NGram.getInstance().testWordSuffixProb(word);
				if(prob > 0.5) prob = 1.0 - prob;
				wordScores.put(word,prob * wordCounts.getCount(word));
				totalScore += prob * wordCounts.getCount(word);
			}

			double cumulative = 0.0;
			out.println("<html><head><title>Foo Bar</title></head><body><form action='UnknownWords' method='POST'><table>");
			boolean shade = false;
			List<String> words = StringTools.getSortedList(wordScores);
			if(words.size() > 1000) words = words.subList(0, 1000);
			for(String word : words) {
				cumulative += wordScores.get(word);
				String enc = StringTools.urlEncodeUTF8NoThrow(word);
				out.println("<tr"
						+ (shade ? " style='background-color: lightgrey'" : "" ) 
						+ "><td>" + word + "</td>" +
						"<td>Chemical: <input type='checkbox' name='chem' value='" + enc + "'></td>" +
						"<td>Nonchemical: <input type='checkbox' name='eng' value='" + enc + "'></td>" +
						"<td>" +
						"<a href='http://www.google.co.uk/search?q=" + enc + "' target='_blank'>Google</a> " +
						"<a href='http://en.wikipedia.org/wiki/Special:Search?search=" + enc + "' target='_blank'>Wikipedia</a> " +
						"<a href='http://www.ncbi.nlm.nih.gov/sites/entrez?db=pccompound&term=" + enc + "' target='_blank'>PubChem</a> " +
						"<a href='http://www.ncbi.nlm.nih.gov/sites/entrez?db=pubmed&term=" + enc + "' target='_blank'>PubMed</a> " +
						"</td></tr>");
				/*out.println(word + "\t" + wordCounts.getCount(word)
						+ "\t" + nGram.getInstance().testWordSuffixProb(word)
						+ "\t" + nGram.getInstance().testWordProb(word)
						+ "\t" + wordScores.get(word)
						+ "\t" + (cumulative / totalScore));*/
				shade = !shade;
			}
			out.println("</table>");
			out.println("<input type='hidden' name='mode' value='process'>");
			out.println("<input type='submit' value='Submit'>");
			out.println("</form></body></html>");
		} else {
			response.setContentType("text/plain");
			PrintWriter out = response.getWriter();
			String [] chem = request.getParameterValues("chem");
			for(int i=0;i<chem.length;i++) out.println(URLDecoder.decode(chem[i], "UTF-8"));
			out.println();
			String [] eng = request.getParameterValues("eng");
			for(int i=0;i<eng.length;i++) out.println(URLDecoder.decode(eng[i], "UTF-8"));
			out.println();
		}
		
		
	}
	
}
