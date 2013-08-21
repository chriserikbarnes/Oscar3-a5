package uk.ac.cam.ch.wwmm.ptc.experimental;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Nodes;
import uk.ac.cam.ch.wwmm.oscar3.terms.TermSets;
import uk.ac.cam.ch.wwmm.ptclib.io.FileTools;

public class IUPACNameFinder {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		List<File> sbFiles = new ArrayList<File>();
		sbFiles.addAll(FileTools.getFilesFromDirectoryByName(new File("/home/ptc24/cleanOscar/oscar3-chem/scrapbookfold1_pruned"), "scrapbook.xml"));
		sbFiles.addAll(FileTools.getFilesFromDirectoryByName(new File("/home/ptc24/cleanOscar/oscar3-chem/scrapbookfold2_pruned"), "scrapbook.xml"));
		sbFiles.addAll(FileTools.getFilesFromDirectoryByName(new File("/home/ptc24/cleanOscar/oscar3-chem/scrapbookfold3_pruned"), "scrapbook.xml"));			
		
		StringBuffer sb = new StringBuffer();
		sb.append("(");
		for(String s : TermSets.getLigands()) {
			sb.append(s);
			sb.append("|");
		}
		sb.append("o|i{1,4}|i{0,3}[xv]|[xv]i{0,4})");
		//sb.deleteCharAt(sb.length()-1);
		//sb.append(")");
		String ligandRe = sb.toString();
		String ligandOrLowerRe = "([a-z]|" + ligandRe + ")";
		Pattern formulaRe = Pattern.compile(ligandOrLowerRe + "?[^a-z]+(" + ligandOrLowerRe + "[^a-z]+)*" + ligandOrLowerRe + "?");
		System.out.println(formulaRe);
		
		for(File f : sbFiles) {
			Document doc = new Builder().build(f);
			Nodes n = doc.query("//ne[@type='CM']");
			for(int i=0;i<n.size();i++) {
				String v = n.get(i).getValue();
				boolean iupac = true;
				//System.out.println(n.get(i).getValue());
				if(!v.matches(".*[a-z][a-z].*")) {
					iupac = false;
				} else if(v.matches("([A-Za-z]|\\s)+")) {
					iupac = false;
				} else if(formulaRe.matcher(v).matches()) {
					iupac = false;
				}
				if(iupac) System.out.println(v);
				//System.out.println(iupac + "\t" + v);
			}
			
		}
	}

}
