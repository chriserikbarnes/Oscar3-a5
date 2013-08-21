package uk.ac.cam.ch.wwmm.ptc.experimental.newpubchem;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import uk.ac.cam.ch.wwmm.oscar3.Oscar3Props;
import uk.ac.cam.ch.wwmm.ptclib.datastruct.Bag;

public class AnalyseNames {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		File npcDir = new File(Oscar3Props.getInstance().workspace, "npc");
		File namesFile = new File(npcDir, "names.txt");
		BufferedReader br = new BufferedReader(new FileReader(namesFile));
		Bag<String> components = new Bag<String>();
		for(String line=br.readLine();line!=null;line=br.readLine()) {
			if(!line.contains("\t")) continue;
			String name = line.substring(0, line.indexOf("\t"));
			if(!name.matches(".*[a-z][a-z].*")) continue;
			String [] ss = name.split("\\s+");
			for(int i=0;i<ss.length;i++) components.add(ss[i]);
		}
		
		components.discardInfrequent(2);
		
		for(String c : components.getList()) {
			System.out.println(c + "\t" + components.getCount(c));
		}
	}

}
