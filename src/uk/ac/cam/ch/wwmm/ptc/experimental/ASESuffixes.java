package uk.ac.cam.ch.wwmm.ptc.experimental;

import java.util.Set;

import uk.ac.cam.ch.wwmm.oscar3.terms.TermSets;
import uk.ac.cam.ch.wwmm.ptclib.datastruct.Bag;

public class ASESuffixes {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Set<String> aseStrs = TermSets.getChemAses();
		Bag<String> subStrCounts = new Bag<String>();
		for(String s : aseStrs) {
			for(int i=0;i<s.length();i++) {
				subStrCounts.add(s.substring(i));
			}
		}
		for(String s : subStrCounts.getList()) {
			int count = subStrCounts.getCount(s);
			if(count == 1) break;
			System.out.println(s + "\t" + count);
		}
	}

}
