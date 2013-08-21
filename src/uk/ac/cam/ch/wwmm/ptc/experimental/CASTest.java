package uk.ac.cam.ch.wwmm.ptc.experimental;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;

import nu.xom.Element;
import uk.ac.cam.ch.wwmm.opsin.NameToStructure;
import uk.ac.cam.ch.wwmm.ptclib.cdk.StructureConverter;
import uk.ac.cam.ch.wwmm.ptclib.io.FileTools;

public class CASTest {

	public static String invertCAS(String name) {
		if(!name.contains(", ")) return name;
		if(name.matches(".* acid, .*-, .* ester")) {
			int commapos = name.lastIndexOf(", ");
			String afterComma = name.substring(commapos+2);
			String beforeComma = name.substring(0, commapos);
			return invertCAS(beforeComma) + " " + afterComma;
		}
		if(name.matches(".* acid, .* ester")) {
			int commapos = name.lastIndexOf(", ");
			String afterComma = name.substring(commapos+2);
			String beforeComma = name.substring(0, commapos);
			return invertCAS(beforeComma) + " " + afterComma;
		}
		if(name.endsWith("-")) {
			int commapos = name.lastIndexOf(", ");
			String afterComma = name.substring(commapos+2);
			//if(afterComma.matches("[A-Z][a-z].*")) {
			//	afterComma = afterComma.substring(0,1).toLowerCase() + afterComma.substring(1);
			//}
			String beforeComma = name.substring(0, commapos);
			if(beforeComma.matches("[A-Z][a-z].*")) {
				beforeComma = beforeComma.substring(0,1).toLowerCase() + beforeComma.substring(1);
			}
			return invertCAS(afterComma + beforeComma);
		}
		return "FOOO!";
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		int tp = 0;
		int fp = 0;
		int fn = 0;
		
		List<String> stringList = FileTools.getStrings(new FileInputStream(new File("/home/ptc24/tmp/ninthCIonly_SMILES.txt")));
		for(String s : stringList) {
			String [] split = s.split("\\|");
			String name = split[1];
			String smiles = split[2];
			String inverted = invertCAS(name);
			Element cml = NameToStructure.getInstance().parseToCML(inverted);
			
			String testInChI = null;
			/*try {
			 IMolecule testMol = new SmilesParser(DefaultChemObjectBuilder.getInstance()).parseSmiles(smiles);
			 testInChI = ConverterToInChI.getInChI(testMol);
			 } catch (Exception e) {
			 
			 }*/
			testInChI = "foo";
			
			if(cml != null && testInChI != null) {
				String newInChI = StructureConverter.cmlToInChI(cml);
				testInChI = newInChI;
				if(newInChI.equals(testInChI)) {
					tp++;
				} else {
					System.out.println(name + "\t" + inverted);
					fp++;
					fn++;
				}
			} else if(testInChI == null) {
				
			} else if(cml == null) {
				System.out.println(name);
				System.out.println(inverted);
				System.out.println();
				fn++;
			}
		}
		System.out.println(tp*1.0/(tp+fp));
		System.out.println(tp*1.0/(tp+fn));
	}
	
}
