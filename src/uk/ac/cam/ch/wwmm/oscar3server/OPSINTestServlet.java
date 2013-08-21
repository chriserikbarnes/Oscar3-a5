package uk.ac.cam.ch.wwmm.oscar3server;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nu.xom.Element;

import org.openscience.cdk.interfaces.IMolecule;

import uk.ac.cam.ch.wwmm.opsin.NameToStructure;
import uk.ac.cam.ch.wwmm.ptclib.cdk.ConverterToInChI;
import uk.ac.cam.ch.wwmm.ptclib.cdk.StructureConverter;
import uk.ac.cam.ch.wwmm.ptclib.io.ResourceGetter;

public final class OPSINTestServlet extends HttpServlet  {

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		ResourceGetter rg = new ResourceGetter("uk/ac/cam/ch/wwmm/oscar3server/resources/");
		response.setContentType("text/plain");
		PrintWriter out = response.getWriter();
		List<String> testSuite;
		try {
			testSuite = rg.getStrings("opsinTest.txt");			
		} catch (Exception e) {
			e.printStackTrace(out);
			return;
		}
		int total = 0;
		int correct = 0;
		int incorrect = 0;
		int correctReject = 0;
		int failedToReject = 0;
		int fail = 0;
		
		for(String s : testSuite) {
			if(!s.matches(".*\\s+(InChI=1/.*|REJECT\\s*)")) continue;

			String inchi = null;
			String name = null;
			if(s.matches(".*\\s+InChI=1/.*")) {
				String [] ss = s.split("\\s+InChI=1");
				if(ss.length != 2) {
					out.println("Bad line: " + s);
					continue;
				} 
				name = ss[0].trim();
				inchi = "InChI=1" + ss[1].trim();				
			} else {
				name = s.split("\\s+REJECT")[0];
			}
			String opsinInchi = null;
			try {
				Element cmlMol = cmlMol = NameToStructure.getInstance().parseToCML(name);
				IMolecule outputMol = StructureConverter.cmlToMolecule(cmlMol);					
				opsinInchi = ConverterToInChI.getInChI(outputMol);
			} catch (Exception e) {
				// Fail silently for now. Maybe we need to do something better.
			}
			total++;
			if(inchi == null && opsinInchi == null) {
				out.println(name + "\tRejected OK");
				correctReject++;
			} else if(inchi == null) {
				out.println(name + "\tFailed To Reject, Gave\t" + opsinInchi);
				failedToReject++;
			} else if(inchi.equals(opsinInchi)) {
				out.println(name + "\tOK\t" + inchi + "\t");
				correct++;
			} else if(opsinInchi == null) {
				out.println(name + "\tNo Output\tExpected: " + inchi);
				fail++;
			} else {
				out.println(name + "\tFail\t" + opsinInchi + "\tshould be\t" + inchi);
				incorrect++;
			}
		}
		out.println();
		out.println("Total: " + total);
		out.println("Correct: " + correct);
		out.println("Incorrect: " + incorrect);
		out.println("Failed To Process: " + fail);
		out.println("Correctly Rejected: " + correctReject);
		out.println("Failed To Reject: " + failedToReject);
		out.println("Precision: " + (correct * 1.0 / (correct + incorrect + failedToReject)));
		out.println("Recall: " + (correct * 1.0 / (correct + incorrect + fail)));
	}

}
