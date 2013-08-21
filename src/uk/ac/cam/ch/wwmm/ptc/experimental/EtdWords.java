package uk.ac.cam.ch.wwmm.ptc.experimental;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import uk.ac.cam.ch.wwmm.oscar3.models.ExtractTrainingData;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.tokenanalysis.NGram;
import uk.ac.cam.ch.wwmm.ptclib.io.FileTools;

public class EtdWords {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		ExtractTrainingData etd1 = ExtractTrainingData.getInstance();
		List<File> sbFiles = new ArrayList<File>();
		sbFiles.addAll(FileTools.getFilesFromDirectoryByName(new File("/home/ptc24/newows/goodrsc"), "scrapbook.xml"));
		ExtractTrainingData etd2 = new ExtractTrainingData(sbFiles);
		Set<String> chem = new HashSet<String>(etd2.chemicalWords);
		//chem.removeAll(etd1.chemicalWords);
		for(String w : chem) {
			if(!NGram.getInstance().chemSet.contains(NGram.parseWord(w))) {
				double score = NGram.getInstance().testWord(w);
				score = Math.max(-17.0, Math.min(13.0, score));
				System.out.println(score);
			}
		}
		System.out.println();
		Set<String> nonchem = new HashSet<String>(etd2.nonChemicalWords);
		chem.removeAll(etd1.nonChemicalWords);
		for(String w : nonchem) {
			if(!NGram.getInstance().engSet.contains(NGram.parseWord(w))) {
				double score = NGram.getInstance().testWord(w);
				score = Math.max(-17.0, Math.min(13.0, score));
				System.out.println(score);
			}
		}
		
	}

}
