package uk.ac.cam.ch.wwmm.oscar3.resolver.extension;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.List;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;

import org.openscience.cdk.Molecule;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.io.MDLReader;
import org.openscience.cdk.smiles.SmilesGenerator;

import uk.ac.cam.ch.wwmm.oscar3.Oscar3Props;
import uk.ac.cam.ch.wwmm.ptclib.cdk.ConverterToInChI;
import uk.ac.cam.ch.wwmm.ptclib.cdk.StructureConverter;

/**Another example; resolves names via HTTP requests. For example, using the
 * OPSIN servlet on the Oscar3 server.
 * 
 * @author ptc24
 *
 */
public class HTTPExtensionNameResolver implements ExtensionNameResolver {

	private static SmilesGenerator generator = new SmilesGenerator();

	public Results resolve(String name, List<String> args) {
		Builder builder = new Builder();
		try {
			// Default to the OPSIN servlet
			String urlStart = "http://localhost:8181/OPSIN?name=";
			if(null != args && args.size() > 0 && args.get(0) != null && args.get(0).length() > 0) {
				urlStart = args.get(0);
			}
			URL url = new URL(urlStart + URLEncoder.encode(name, "UTF-8"));
			URLConnection conn = url.openConnection();
			String type = conn.getContentType();
			if(type != null && type.contains(";")) {
				type = type.substring(0, type.indexOf(";"));
			}
			//System.out.println(type);
			if("application/xml".equals(type)) {
				Document cml = builder.build(conn.getInputStream());
				Element cmlMol = cml.getRootElement();
				StructureConverter.enhanceCMLMolecule(cmlMol, name);
				IMolecule outputMol = StructureConverter.cmlToMolecule(cmlMol);
				String smiles = generator.createSMILES(outputMol);
				String inchi = ConverterToInChI.getInChI(outputMol);
				Results results = new Results(smiles, inchi, cmlMol);
				return results;				
			} else if("text/plain".equals(type) && "Name did not parse!".equals(new BufferedReader(new InputStreamReader(conn.getInputStream())).readLine())) { 
				// Failure from SimpleOPSINServlet
				return null;
			} else if("chemical/x-mdl-molfile".equals(type)) {
				MDLReader mdlr = new MDLReader(conn.getInputStream());
				IMolecule outputMol = (IMolecule)mdlr.read(new Molecule());
				StructureConverter.configureMolecule(outputMol);
				String smiles = generator.createSMILES(outputMol);
				String inchi = ConverterToInChI.getInChI(outputMol);
				Results results = new Results(smiles, inchi, null);
				return results;	
			} else if(type == null) {
				return null;
			} else {
				System.err.println("HTTPExtensionNameResolver didn't recognise content type: " + type);
				if(Oscar3Props.getInstance().verbose) {
					BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
					for(String line = br.readLine();line != null;line = br.readLine()) {
						System.err.println(line);
					}
				}
				return null;				
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
	}

}
