package uk.ac.cam.ch.wwmm.oscar3;

import java.io.ByteArrayOutputStream;

import uk.ac.cam.ch.wwmm.oscar3.chemnamedict.ChemNameDictSingleton;
import uk.ac.cam.ch.wwmm.oscar3.models.ExtractTrainingData;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.tokenanalysis.TLRHolder;
import uk.ac.cam.ch.wwmm.oscar3.terms.TermMaps;
import uk.ac.cam.ch.wwmm.oscar3.terms.TermSets;

/** A class to generate a log of which versions of Oscar3 resources were used
 * in the analysis of a document.
 * 
 * @author ptc24
 *
 */

public class Traceability {
	
	public static String getTraceabilityInfo() {
		try {
			//long time = System.currentTimeMillis();
			StringBuffer sb = new StringBuffer();
			sb.append(Oscar3.svnRevision + "\n");
			sb.append("ChemNameDict: " + ChemNameDictSingleton.getCNDHash() + "\n");
			sb.append("Model: " + ExtractTrainingData.getInstance().makeHash() + "\n");
			sb.append("TermSets:\n" + TermSets.makeHashes() + "\n");
			sb.append("TermMaps:\n" + TermMaps.makeHashes() + "\n");
			sb.append("TLRs: " + TLRHolder.getInstance().makeHash() + "\n");
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			Oscar3Props.writeProperties(baos);
			sb.append(baos.toString());
			//System.out.println("Generated traceability in " + (System.currentTimeMillis() - time) + " milliseconds");
			return sb.toString();
		} catch (Exception e) {
			throw new Error(e);
		}
	}
	
	
}
