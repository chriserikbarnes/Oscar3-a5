package uk.ac.cam.ch.wwmm.ptc.experimental.yahoo;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;

import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.TokenSequence;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.Tokeniser;
import uk.ac.cam.ch.wwmm.oscar3.terms.TermSets;
import uk.ac.cam.ch.wwmm.ptclib.datastruct.Bag;
import uk.ac.cam.ch.wwmm.ptclib.io.FileTools;
import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;

public class Collocates {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		String word = "mucosa";
		boolean window = true;
		
		File f = new File("/home/ptc24/tmp/yahoo/" + word + ".txt");
		List<String> strings = FileTools.getStrings(new FileInputStream(f));
		Bag<String> collocs = new Bag<String>();
		for(String s : strings) {
			TokenSequence t = Tokeniser.getInstance().tokenise(s);
			List<String> tokens = t.getTokenStringList();
			boolean hasCell = false;
			for(String token : tokens) {
				if(token.toLowerCase().equals(word)) {
					hasCell = true;
					break;
				}
			}
			String prev = "";
			if(hasCell) {
				for(String token : tokens) {
					String norm = StringTools.normaliseName2(token);
					if(TermSets.getClosedClass().contains(norm) || !norm.matches(".*[A-Za-z0-9].*")) {
						prev = "";
						continue;
					}
					if(norm.equals(word)) {
						if(!prev.equals("")) collocs.add(prev + " " + word);
					} else {
						if(window) collocs.add(norm);
					}
					if(prev.equals(word)) collocs.add(word + " " + norm);
					prev = norm;
				}
			}
		}
		for(String s : collocs.getList()) {
			System.out.println(s + "\t" + collocs.getCount(s));
		}
	}

}
