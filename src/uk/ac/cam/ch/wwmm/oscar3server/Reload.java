package uk.ac.cam.ch.wwmm.oscar3server;

import java.io.PrintWriter;

import uk.ac.cam.ch.wwmm.opsin.NameToStructure;
import uk.ac.cam.ch.wwmm.oscar3.Oscar3Props;
import uk.ac.cam.ch.wwmm.oscar3.chemnamedict.ChemNameDictSingleton;
import uk.ac.cam.ch.wwmm.oscar3.dataparse.RParser;
import uk.ac.cam.ch.wwmm.oscar3.indexersearcher.OntologicalArguaments;
import uk.ac.cam.ch.wwmm.oscar3.misc.NETypes;
import uk.ac.cam.ch.wwmm.oscar3.models.Model;
import uk.ac.cam.ch.wwmm.oscar3.pcsql.PubChemSQL;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.HyphenTokeniser;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.finder.DFANEFinder;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.tokenanalysis.NGram;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.tokenanalysis.TLRHolder;
import uk.ac.cam.ch.wwmm.oscar3.resolver.NameResolver;
import uk.ac.cam.ch.wwmm.oscar3.terms.TermMaps;
import uk.ac.cam.ch.wwmm.oscar3.terms.TermSets;
import uk.ac.cam.ch.wwmm.ptclib.cdk.MultiFragmentStructureDiagramGenerator;

/** Reloads parts of Oscar3.
 * 
 * @author ptc24
 *
 */
public class Reload {

	/**Re-initialise one or more singletons.
	 * 
	 * @param name The name of the service to re-initialse, or "all".
	 * @param out A PrintWriter to write messages to.
	 * @throws Exception
	 */
	public static void reload(String name, PrintWriter out) throws Exception {
		if(name != null) name=name.toLowerCase();
		if(name.startsWith("/")) name=name.substring(1);
		if(name == null) {
			out.write("Please specify a singleton to reload!");
		} else if("opsin".equals(name)) {
			NameToStructure.reinitialise();
			out.write("Reloaded OPSIN");
		} else if("properties".equals(name)) {
			Oscar3Props.reloadProperties();
			out.write("Reloaded Oscar3 Properties");			
		} else if("chemnamedict".equals(name)) {
			ChemNameDictSingleton.reinitialise();
			DFANEFinder.reinitialise();
			NGram.reinitialise();
			out.write("Reloaded ChemNameDict");
		} else if("netypes".equals(name)) {
			NETypes.reinitialise();
			out.write("Reloaded Named Entity Types");
		} else if("pubchemsql".equals(name)) {
			PubChemSQL.reinitialise();
			out.write("Reloaded PubChem SQL");
		/*} else if("formulabayes".equals(name)) {
			FormulaBayes.reinitialise();
			out.write("Reloaded FormulaBayes");*/
		} else if("hyphentokeniser".equals(name)) {
			HyphenTokeniser.reinitialise();
			out.write("Reloaded hyphen tokeniser");
		} else if("ngram".equals(name)) {
			NGram.reinitialise();
			out.write("Reloaded nGram");
		} else if("tlrholder".equals(name)) {
			TLRHolder.reinitialise();
			out.write("Reloaded TLRHolder");
		} else if("dataparser".equals(name)) {
			RParser.reinitialise();
			out.write("Reloaded Data Parser");
		} else if("terms".equals(name)) {
			TermSets.reinitialise();
			TermMaps.reinitialise();
			DFANEFinder.reinitialise();
			NGram.reinitialise();
			OntologicalArguaments.reinitialise();
			out.write("Reloaded Terms");
		} else if("dfa".equals(name)) {
			DFANEFinder.reinitialise();
			out.write("Reloaded DFA NE Finder");
		} else if("nameres".equals(name)) {				
			NameResolver.reinitialise();
			out.write("Reloaded Name Resolver");
		} else if("structurediagram".equals(name)) {				
			MultiFragmentStructureDiagramGenerator.reinitialise();
			out.write("Reloaded Structure Diagram Generator");
		} else if("model".equals(name)) {				
			Model.loadModel();
			HyphenTokeniser.reinitialise();
			out.write("Reloaded Model File");
		} else if("all".equals(name)) {
			out.println("Reloading... please wait...");
			out.flush();
			long time = System.currentTimeMillis();
			Oscar3Props.reloadProperties();
			Model.loadModel();
			NameToStructure.reinitialise();
			ChemNameDictSingleton.reinitialise();
			NETypes.reinitialise();
			PubChemSQL.reinitialise();
			HyphenTokeniser.reinitialise();
			TLRHolder.reinitialise();
			RParser.reinitialise();
			//Rules.reinitialise();
			TermSets.reinitialise();

			NGram.reinitialise();
			//FormulaBayes.reinitialise();

			DFANEFinder.reinitialise();
			NameResolver.reinitialise();
			MultiFragmentStructureDiagramGenerator.reinitialise();
			
			long timeTaken = System.currentTimeMillis() - time;
			double seconds = timeTaken / 1000.0;
			
			out.printf("Reloaded everything reloadable in %.1f seconds\n", seconds);
		} else {
			out.write("I don't know how to reload " + name);
		}

	}
	
}
