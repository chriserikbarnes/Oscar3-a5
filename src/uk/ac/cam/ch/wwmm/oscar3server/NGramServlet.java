package uk.ac.cam.ch.wwmm.oscar3server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import uk.ac.cam.ch.wwmm.oscar3.Oscar3Props;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.tokenanalysis.NGram;
import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;

public final class NGramServlet extends HttpServlet {

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doGet(req, resp);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		resp.setContentType("text/plain");
		PrintWriter out = resp.getWriter();
		if(req.getParameter("word") != null) {
			out.println(NGram.getInstance().testWord(req.getParameter("word")));
		} else if(req.getParameter("words") != null) {
			List<String> words = StringTools.arrayToList(req.getParameter("words").split("\\s+"));
			for(String word : words) {
				out.println(word + "\t" + NGram.getInstance().testWord(word));
			}
		} else if(req.getContentType() != null && 
				req.getContentType().startsWith("multipart/form-data;")) {
			try {
				FileItemFactory fif = new DiskFileItemFactory();
				ServletFileUpload sfu = new ServletFileUpload(fif);
				List<String> chemical = new ArrayList<String>();
				List<String> english = new ArrayList<String>();
				List itemList = sfu.parseRequest(req);
				for(Object o : itemList) {
					FileItem item = (FileItem)o;
					if(Oscar3Props.getInstance().verbose) System.out.println(item);
					if(item.getFieldName().equals("chemical")) {
						BufferedReader br = new BufferedReader(new InputStreamReader(item.getInputStream(), "UTF-8"));
						for(String line=br.readLine();line!=null;line=br.readLine()) {
							chemical.addAll(StringTools.arrayToList(line.split("\\s+")));
						}
					}
					if(item.getFieldName().equals("english")) {
						BufferedReader br = new BufferedReader(new InputStreamReader(item.getInputStream(), "UTF-8"));
						for(String line=br.readLine();line!=null;line=br.readLine()) {
							english.addAll(StringTools.arrayToList(line.split("\\s+")));
						}
					}
				}
				if(chemical.size() > 0 && english.size() > 0) {
					NGram.getInstance().reinitialise(chemical, english, true);
					out.println("Trained OK");
				} else {
					out.println("Not trained OK");					
				}
			} catch (Exception e) {
				e.printStackTrace(out);
			}
		} else if(req.getParameter("chemical") != null && req.getParameter("english") != null) {
			try {
				List<String> chemical = StringTools.arrayToList(req.getParameter("chemical").trim().split("\\s+"));
				List<String> english = StringTools.arrayToList(req.getParameter("english").trim().split("\\s+"));
				NGram.reinitialise(chemical, english, true);				
			} catch (Exception e) {
				e.printStackTrace(out);
			}
		}

		
		
	}
	
}
