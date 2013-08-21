package uk.ac.cam.ch.wwmm.oscar3server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Serializer;

import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.smiles.SmilesParser;

import uk.ac.cam.ch.wwmm.ptclib.cdk.ConverterToInChI;
import uk.ac.cam.ch.wwmm.ptclib.cdk.MultiFragmentStructureDiagramGenerator;
import uk.ac.cam.ch.wwmm.ptclib.cdk.StereoInChIToMolecule;
import uk.ac.cam.ch.wwmm.ptclib.cdk.StructureConverter;

/**Obsolete: use ViewMolServlet instead.
 * 
 * @author ptc24
 *
 */
@SuppressWarnings("serial")
public final class CMLServlet extends HttpServlet {
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		IMolecule mol = null;
		try {
			if(request.getParameter("smiles") != null) {
				mol = new SmilesParser(DefaultChemObjectBuilder.getInstance()).parseSmiles(request.getParameter("smiles"));
				//StructureConverter.configureMolecule(mol);
			} else if(request.getParameter("inchi") != null) {
				/*try {
					PubChemSQL pcsql = PubChemSQL.getInstance();
					String mf = pcsql.getMolForInChI(request.getParameter("inchi"));
					mol = StructureConverter.MDLToMol(mf);
					HydrogenRemover.removeInsignificantHydrogens(mol);
				} catch (Exception e) {

				}*/
				if(mol == null) {
					StereoInChIToMolecule.primeCacheForInChI(request.getParameter("inchi"));
					mol = ConverterToInChI.getMolFromInChI(request.getParameter("inchi"));
					//mol = ConverterToInChI.getMolFromInChIUsingApplication(request.getParameter("inchi"));
				}
			}
			if(mol != null && !MultiFragmentStructureDiagramGenerator.hasStructure(mol)) {
				mol = MultiFragmentStructureDiagramGenerator.getMoleculeWith2DCoords(mol);
			}
			Element cml = StructureConverter.simpleMolToCml(mol);
			Document cmlDoc = new Document(cml);
			response.setContentType("application/xml");
			new Serializer(response.getOutputStream()).write(cmlDoc);
		} catch (Exception e) {
			e.printStackTrace();
			response.setContentType("text/plain");
			response.getWriter().println("Error in CML Servlet");
		}
		// TODO Auto-generated method stub
//		/super.doGet(arg0, arg1);
	}
	
}
