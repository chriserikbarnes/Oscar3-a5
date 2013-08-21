package uk.ac.cam.ch.wwmm.ptc.experimental;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Nodes;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.TokenSequence;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.Tokeniser;
import uk.ac.cam.ch.wwmm.oscar3.terms.TermSets;
import uk.ac.cam.ch.wwmm.ptclib.scixml.XMLStrings;
import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;

public class PatentFilter {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		Document doc = new Builder().build(new File("/home/ptc24/corpora/patents/WO2006007854/source.xml"));
		Nodes n = XMLStrings.getInstance().getChemicalPlaces(doc);
		Set<String> boring = new HashSet<String>();
		boring.addAll(TermSets.getUsrDictWords());
		
		int nb = 0;
		int b = 0;
		
		for(int i=0;i<n.size();i++) {
			TokenSequence t = Tokeniser.getInstance().tokenise(n.get(i).getValue());
			boolean isBoring = true;
			for(String s : t.getTokenStringList()) {
				s = StringTools.normaliseName(s);
				if(!s.matches(".*[A-Za-z].*")) continue;
				if(boring.contains(s)) continue;
				boring.add(s);
				isBoring = false;
				break;
			}
			if(isBoring) {
				System.out.println("BORING:");
				b++;
			} else {
				System.out.println("NEW:");
				nb++;
			}
			System.out.println(n.get(i).toXML());
		}
		System.out.println(b + "\t" + nb);
	}

}
