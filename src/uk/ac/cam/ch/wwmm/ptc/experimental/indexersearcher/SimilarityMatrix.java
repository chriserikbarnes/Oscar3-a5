package uk.ac.cam.ch.wwmm.ptc.experimental.indexersearcher;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.openscience.cdk.fingerprint.Fingerprinter;
import org.openscience.cdk.interfaces.IMolecule;

import uk.ac.cam.ch.wwmm.ptclib.cdk.ConverterToInChI;
import uk.ac.cam.ch.wwmm.ptclib.datastruct.Bag;

/**Calculates all pairwise chemical similarity scores (Tanimoto coefficient) 
 * for a set of InChIs.
 * @author ptc24
 *
 */
public class SimilarityMatrix {

	Set<String> inchis;
	Map<String,IMolecule> molecules;
	Map<String,BitSet> fingerprints;
	
	public SimilarityMatrix(Set<String> inchis) throws Exception {
		Fingerprinter fingerprinter = new Fingerprinter();
		this.inchis = inchis;
		molecules = new HashMap<String,IMolecule>();
		fingerprints = new HashMap<String,BitSet>();
		for(String inchi : inchis) {
			IMolecule mol = ConverterToInChI.getMolFromInChI(inchi);
			if(mol != null) {
				molecules.put(inchi, mol);
				fingerprints.put(inchi, fingerprinter.getFingerprint(mol));
			}
		}
	}
	
	public double getSimilarity(String inchiA, String inchiB) {
		if(fingerprints.containsKey(inchiA) && fingerprints.containsKey(inchiB)) {
			BitSet fpa = fingerprints.get(inchiA);
			BitSet fpb = fingerprints.get(inchiB);
			BitSet union = (BitSet)fpa.clone();
			union.or(fpb);
			BitSet intersection = (BitSet)fpa.clone();
			intersection.and(fpb);
			double similarity = intersection.cardinality() * 1.0 / union.cardinality();
			if(inchiA.equals(inchiB) && similarity != 1.0) System.out.println("Error 1!");
			if(similarity > 1.0) System.out.println("Error 2");
			if(similarity < 0.0) System.out.println("Error 3");
			//System.out.println(union.cardinality() + "\t" + intersection.cardinality() + "\t" + fpa.cardinality() + "\t" + fpb.cardinality());
			//System.out.println(intersection.cardinality() * 1.0 / union.cardinality());
			return similarity;
		} else {
			return 0.0;
		}
	}
	
	public Bag<String> similarityEvents(Bag<String> inchiBag) {
		Bag<String> results = new Bag<String>();
		for(String inchiA : inchis) {
			int likePoint5 = 0;
			int likePoint6 = 0;
			int likePoint7 = 0;
			int likePoint8 = 0;
			int likePoint9 = 0;
			int likePoint95 = 0;
			//System.out.println(inchiA);
			for(String inchiB : inchiBag.getSet()) {
				double similarity = getSimilarity(inchiA, inchiB);
				if(similarity == 1.0) continue;
				if(similarity > 0.5) {
					//System.out.println("\t" + similarity + "\t" + inchiB);
					likePoint5 += inchiBag.getCount(inchiB);
				}
				if(similarity > 0.6) likePoint6 += inchiBag.getCount(inchiB);
				if(similarity > 0.7) likePoint7 += inchiBag.getCount(inchiB);
				if(similarity > 0.8) likePoint8 += inchiBag.getCount(inchiB);
				if(similarity > 0.9) likePoint9 += inchiBag.getCount(inchiB);
				if(similarity > 0.95) likePoint95 += inchiBag.getCount(inchiB);
			}
			if(likePoint5 > 0) {
				results.add("0.5 like " + inchiA, likePoint7);
			}
			if(likePoint6 > 0) {
				results.add("0.6 like " + inchiA, likePoint7);
			}
			if(likePoint7 > 0) {
				results.add("0.7 like " + inchiA, likePoint7);
			}
			if(likePoint8 > 0) {
				results.add("0.8 like " + inchiA, likePoint8);
			}
			if(likePoint9 > 0) {
				results.add("0.9 like " + inchiA, likePoint9);
			}
			if(likePoint95 > 0) {
				results.add("0.95 like " + inchiA, likePoint9);
			}
		}
		return results;
	}
}
