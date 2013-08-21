package uk.ac.cam.ch.wwmm.oscar3server;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uk.ac.cam.ch.wwmm.oscar3.chemnamedict.ChEBIImportFromOBO;
import uk.ac.cam.ch.wwmm.oscar3.chemnamedict.ChemNameDictSingleton;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.tokenanalysis.NGram;

/** Allows the ChemNameDict to be queried and updated online.
 * 
 * @author ptc24
 *
 */
@SuppressWarnings("serial")
public final class ChemNameDictServlet extends HttpServlet {

	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}
	
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		response.setContentType("text/plain");
		PrintWriter out = response.getWriter();
		
		
		String action = request.getParameter("action");
		if(action.equals("put")) {
			try {
				String name = request.getParameter("name");
				String smiles = request.getParameter("smiles");
				String inchi = request.getParameter("inchi");
				if(name.equals("")) name = null;
				if(smiles.equals("")) smiles = null;
				if(inchi.equals("")) inchi = null;
				ChemNameDictSingleton.addChemical(name, smiles, inchi);
				out.printf("%s wrote to ChemNameDict as %s, %s", name, smiles, inchi);
			} catch (Exception e) {
				out.write("Couldn't add to ChemNameDict!");
			}
		} else if(action.equals("get")) {
			try {
			String name = request.getParameter("name");
			String result = "Couldn't fetch from ChemNameDict!";
			if(request.getParameter("type").equals("smiles")) {
				result = ChemNameDictSingleton.getSpaceSeparatedSmiles(name);
			} else if(request.getParameter("type").equals("inchi")) {
				result = ChemNameDictSingleton.getSpaceSeparatedInchis(name);
			}
			out.write(result);
			} catch (Exception e) {
				e.printStackTrace(out);
			}
		} else if(action.equals("save")) {
			try {	
				ChemNameDictSingleton.save();
				out.write("Saved OK!");
			} catch (Exception e) {
				out.write("Save failed!");
			}
		} else if(action.equals("stop")) {
			try {
				String word = request.getParameter("word");
				ChemNameDictSingleton.addStopWord(word);
				response.sendRedirect("stopwordOK.html");
				//out.println("Added stopword OK");
				//out.println("NB: You will need to save your ChemNameDict (see the ChemNameDict form) if you " +
				//		"want this stopword to persist after you shut the server down");
			} catch (Exception e) {
				out.write("Adding stopword failed!");
			}			
/*		} else if(action.equals("importChEBI")) {
			try {
				ChEBIImport.importChEBI();
				out.write("Imported OK");
			} catch (Exception e) {
				out.write("ChEBI import failed!");
				e.printStackTrace();
			}	*/
		} else if(action.equals("importChEBIFromOBO")) {
			try {
				ChEBIImportFromOBO.importChEBI();
				out.write("Imported OK");
			} catch (Exception e) {
				out.write("ChEBI import failed!");
				e.printStackTrace();
			}	
		} else if(action.equals("retrain")) {
			try {
				NGram.reinitialise();
				out.write("Retrained OK");
			} catch (Exception e) {
				out.write("Retraining failed!");
			}
		} else if(action.equals("viewstop")) {
			try {
				for(String s : ChemNameDictSingleton.getStopWords()) {
					out.println(s);
				}
			} catch (Exception e) {
				out.write("Viewing stopwords failed!");
			}
		}
	}

}
