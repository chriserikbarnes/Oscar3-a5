package uk.ac.cam.ch.wwmm.oscar3server;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.BitSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.vecmath.Point2d;

import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Serializer;

import org.openscience.cdk.Atom;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.Molecule;
import org.openscience.cdk.fingerprint.Fingerprinter;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.smiles.SmilesParser;

import uk.ac.cam.ch.wwmm.ptclib.cdk.ConverterToInChI;
import uk.ac.cam.ch.wwmm.ptclib.cdk.Molecule2Png;
import uk.ac.cam.ch.wwmm.ptclib.cdk.MultiFragmentStructureDiagramGenerator;
import uk.ac.cam.ch.wwmm.ptclib.cdk.StereoInChIToMolecule;
import uk.ac.cam.ch.wwmm.ptclib.cdk.StructureConverter;
import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;

/** Draws a PNG, or makes SMILES, InChI or CML for a particular SMILES string, InChI or element symbol.
 * 
 * @author ptc24
 *
 */
@SuppressWarnings("serial")
public final class ViewMolServlet extends HttpServlet {

	Fingerprinter fingerprinter = new Fingerprinter();
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		String serviceType = "png";
		if("png".equals(request.getParameter("output"))) serviceType = "png";
		if("cml".equals(request.getParameter("output"))) serviceType = "cml";
		if(request.getServletPath().equals("/CML")) serviceType = "cml";
		if("smiles".equals(request.getParameter("output"))) serviceType = "smiles";
		if("inchi".equals(request.getParameter("output"))) serviceType = "inchi";
		if("inchikey".equals(request.getParameter("output"))) serviceType = "inchikey";		
		if("stdinchi".equals(request.getParameter("output"))) serviceType = "stdinchi";
		if("stdinchikey".equals(request.getParameter("output"))) serviceType = "stdinchikey";
		if("fingerprint".equals(request.getParameter("output"))) serviceType = "fingerprint";
		if("fingerprintint".equals(request.getParameter("output"))) serviceType = "fingerprintint";
		if("fingerprinthex".equals(request.getParameter("output"))) serviceType = "fingerprinthex";
		if("fingerprintbin".equals(request.getParameter("output"))) serviceType = "fingerprintbin";
		//if("SciXML".equals(request.getParameter("output"))) serviceType = "scixml";
		
		IMolecule mol = null;
		boolean makeStructure = true;
		if(request.getParameter("inchi") != null && (request.getParameter("inchi").length() > 0)) {
			makeStructure = false;
			if(mol == null) {
				StereoInChIToMolecule.primeCacheForInChI(request.getParameter("inchi"));
				mol = ConverterToInChI.getMolFromInChI(request.getParameter("inchi"));
				if(mol != null) {
					try {						
						StructureConverter.configureMolecule(mol);					
					} catch (Exception e) {
						mol = null;	
					}
					makeStructure = !MultiFragmentStructureDiagramGenerator.hasStructure(mol);
					if(mol.getAtomCount() == 1) makeStructure = true;					
				}
			}
		}
		
		if(mol == null && request.getParameter("smiles") != null && request.getParameter("smiles").length() > 0) {
			try {
				mol = new SmilesParser(DefaultChemObjectBuilder.getInstance()).parseSmiles(request.getParameter("smiles"));
				if(mol != null) {
					StructureConverter.configureMolecule(mol);					
					makeStructure = !MultiFragmentStructureDiagramGenerator.hasStructure(mol);
					if(mol.getAtomCount() == 1) makeStructure = true;					
				}
			} catch (Exception e) {
				mol = null;	
			}
		} 
		if(mol == null && request.getParameter("element") != null && (request.getParameter("element").length() > 0)) {
			String elemSymbol = request.getParameter("element");
			Atom a = new Atom(elemSymbol);
			a.setPoint2d(new Point2d(0.0, 0.0));
			mol = new Molecule();
			mol.addAtom(a);
			makeStructure = false;
		}
		
		if("png".equals(serviceType)) {
			response.setContentType("image/png");
			Molecule2Png m2p = new Molecule2Png();
			m2p.fixedWidthAndHeight = true;
			m2p.height = 300;
			m2p.width = 300;
			try	{
				String fixed = request.getParameter("fixed");
				if("true".equals(fixed) || "yes".equals(fixed) || fixed == null) {
					if(request.getParameter("size") != null) {
						int size = Integer.parseInt(request.getParameter("size"));
						m2p.height = size;
						m2p.width = size;
					}
					if(request.getParameter("width") != null) {
						m2p.width = Integer.parseInt(request.getParameter("width"));
					}
					if(request.getParameter("height") != null) {
						m2p.height = Integer.parseInt(request.getParameter("height"));
					}					
					if(request.getParameter("occupationFactor") != null) {
						m2p.occupationFactor = Double.parseDouble(request.getParameter("occupationFactor"));
					}					
				} else {
					m2p.fixedWidthAndHeight = false;
					if(request.getParameter("scaleFactor") != null) {
						m2p.scaleFactor = Integer.parseInt(request.getParameter("scaleFactor"));
					}
					if(request.getParameter("borderWidth") != null) {
						m2p.borderWidth = Integer.parseInt(request.getParameter("borderWidth"));
					}
				}
				if(request.getParameter("fontSize") != null) {
					m2p.fontSize = Integer.parseInt(request.getParameter("fontSize"));
				}
				if(request.getParameter("fontName") != null) {
					m2p.fontName = request.getParameter("fontName");
				}
				if(request.getParameter("colourAtoms") != null && 
						("false".equals(request.getParameter("colourAtoms")) ||
								"no".equals(request.getParameter("colourAtoms")))) {
					m2p.colourAtoms = false;
				}

				OutputStream os = response.getOutputStream();
				m2p.renderMolecule(mol, os, makeStructure);
				os.close();
			} catch (Exception e) {
				response.setContentType("image/png");
				try {
					m2p.renderMolecule(null, response.getOutputStream());				
				} catch (Exception ee) {
					//e.printStackTrace();
				}				
			}
			m2p = null;
		} else if("cml".equals(serviceType)) {
			try	{
				StructureConverter.configureMolecule(mol);
				Element cml = StructureConverter.simpleMolToCml(mol);
				Document cmlDoc = new Document(cml);
				response.setContentType("application/xml");
				new Serializer(response.getOutputStream()).write(cmlDoc);
			} catch (Exception e) {
				e.printStackTrace();
				response.setContentType("text/plain");
				response.getWriter().println("Error in CML Servlet");
			}			
		} else if("smiles".equals(serviceType)) {
			SmilesGenerator sg = new SmilesGenerator();
			String smiles = sg.createSMILES(mol);
			response.setContentType("text/plain");
			response.getWriter().println(smiles);			
		} else if("inchi".equals(serviceType)) {
			//try {
			//	new HydrogenAdder(new ValencyHybridChecker()).addImplicitHydrogensToSatisfyValency(mol);				
			//} catch (Exception e) {
			//	e.printStackTrace();
			//}
			String inchi = ConverterToInChI.getInChI(mol);
			response.setContentType("text/plain");
			response.getWriter().println(inchi);			
		} else if("stdinchi".equals(serviceType)) {
			String inchi = ConverterToInChI.getInChI(mol, true);
			response.setContentType("text/plain");
			response.getWriter().println(inchi);			
		} else if("inchikey".equals(serviceType)) {
			String inchikey = ConverterToInChI.getInChIKey(mol);
			response.setContentType("text/plain");
			response.getWriter().println(inchikey);			
		} else if("stdinchikey".equals(serviceType)) {
			String inchikey = ConverterToInChI.getInChIKey(mol, true);
			response.setContentType("text/plain");
			response.getWriter().println(inchikey);			
		} else if("fingerprint".equals(serviceType)) {
			try {
				BitSet fp = fingerprinter.getFingerprint(mol);
				response.setContentType("text/plain");
				response.getWriter().println(fp.toString());
			} catch (Exception e) {
				e.printStackTrace();
				response.setContentType("text/plain");
				response.getWriter().println("Error in fingerprint generation");
			}	
			
		} else if("fingerprintint".equals(serviceType)) {
			try {
				BitSet fp = fingerprinter.getFingerprint(mol);
				BigInteger bigInteger = BigInteger.ZERO;
				for(int i=fp.nextSetBit(0);i>=0;i=fp.nextSetBit(i+1)) {
					bigInteger = bigInteger.setBit(i);
				}			
				response.setContentType("text/plain");
				response.getWriter().println(bigInteger.toString());
			} catch (Exception e) {
				e.printStackTrace();
				response.setContentType("text/plain");
				response.getWriter().println("Error in fingerprint generation");
			}
		} else if("fingerprinthex".equals(serviceType)) {
			try {
				BitSet fp = fingerprinter.getFingerprint(mol);
				BigInteger bigInteger = BigInteger.ZERO;
				for(int i=fp.nextSetBit(0);i>=0;i=fp.nextSetBit(i+1)) {
					bigInteger = bigInteger.setBit(i);
				}
				String fpStr = bigInteger.toString(16);
				fpStr = StringTools.multiplyString("0", (fingerprinter.getSize() / 4) - fpStr.length()) + fpStr;
				response.setContentType("text/plain");
				response.getWriter().println(fpStr);
			} catch (Exception e) {
				e.printStackTrace();
				response.setContentType("text/plain");
				response.getWriter().println("Error in fingerprint generation");
			}		
		} else if("fingerprintbin".equals(serviceType)) {
			try {
				BitSet fp = fingerprinter.getFingerprint(mol);
				BigInteger bigInteger = BigInteger.ZERO;
				for(int i=fp.nextSetBit(0);i>=0;i=fp.nextSetBit(i+1)) {
					bigInteger = bigInteger.setBit(i);
				}
				String fpStr = bigInteger.toString(2);
				fpStr = StringTools.multiplyString("0", fingerprinter.getSize() - fpStr.length()) + fpStr;
				response.setContentType("text/plain");
				response.getWriter().println(fpStr);
			} catch (Exception e) {
				e.printStackTrace();
				response.setContentType("text/plain");
				response.getWriter().println("Error in fingerprint generation");
			}		
		}
	}
}
