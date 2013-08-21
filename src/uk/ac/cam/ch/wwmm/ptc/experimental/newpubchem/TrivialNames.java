package uk.ac.cam.ch.wwmm.ptc.experimental.newpubchem;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import uk.ac.cam.ch.wwmm.oscar3.Oscar3Props;
import uk.ac.cam.ch.wwmm.oscar3.chemnamedict.ChemNameDictSingleton;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.tokenanalysis.NGram;
import uk.ac.cam.ch.wwmm.oscar3.terms.TermSets;

public class TrivialNames {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		if(false) {
			System.out.println(ChemNameDictSingleton.getAllNames().contains("butane"));
			return;
		}
		
		
		List<String> names = new ArrayList<String>();
		Pattern p = Pattern.compile(".*[a-z][a-z].*");
		//NGram.getInstance();
		//System.out.println(Runtime.getRuntime().freeMemory());
		//System.out.println(Runtime.getRuntime().maxMemory());
		//System.out.println(Runtime.getRuntime().totalMemory());
		System.out.println("ready");
		File npcDir = new File(Oscar3Props.getInstance().workspace, "npc");
		File namesFile = new File(npcDir, "names.txt");
		BufferedReader br = new BufferedReader(new FileReader(namesFile));
		for(String line=br.readLine();line!=null;line=br.readLine()) {
			if(!line.contains("\t")) continue;
			String name = line.substring(0, line.indexOf("\t"));
			if(p.matcher(name).matches()) {
				double score = 20.0;
				//if(TermSets.getUsrDictWords().contains(name) && !TermSets.getElements().contains(name) && !ChemNameDictSingleton.hasName(name)) score = -20.0;
				if(name.matches(".*\\s+.*")) {
					String [] ss = name.split("\\s+");
					boolean allUDW = true;
					for(int i=0;i<ss.length;i++) {
						if(!TermSets.getUsrDictWords().contains(ss[i])) {
							allUDW = false;
						}
					}
					if(allUDW) score = -10.0;
				}
				
				//if(name.length() > 15) score = 100.0;
				//double score = NGram.getInstance().testWord(name);
				if(score < 10.0) {
					System.out.println(name);
					names.add(name.intern());
				}
			}
		}
		System.out.println(names.size());
		//System.gc();
		//System.out.println(Runtime.getRuntime().freeMemory());
		//System.out.println(Runtime.getRuntime().maxMemory());
		//System.out.println(Runtime.getRuntime().totalMemory());
	}

}
