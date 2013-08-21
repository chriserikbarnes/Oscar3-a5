package uk.ac.cam.ch.wwmm.ptc.experimental;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import uk.ac.cam.ch.wwmm.oscar3.Oscar3;
import uk.ac.cam.ch.wwmm.oscar3.Traceability;
import uk.ac.cam.ch.wwmm.oscar3.models.Model;
import uk.ac.cam.ch.wwmm.ptclib.io.FileTools;

public class CrossTrain {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		Model.loadModel();
		List<File> files = FileTools.getFilesFromDirectoryByName(new File("/home/ptc24/tmp/autoAnnot/"), "source.xml");
		//List<File> files = FileTools.getFilesFromDirectoryByName(new File("/home/ptc24/newows/corpora/CrossTrainPatents"), "source.xml");
		List<List<File>> folds = new ArrayList<List<File>>();
		for(int i=0;i<3;i++) {
			folds.add(new ArrayList<File>());
		}
		
		for(int i=0;i<3;i++) {
			List<File> trainFiles = new ArrayList<File>();
			List<File> runFiles = new ArrayList<File>();
			for(File f : files) {
				if(f.toString().contains("default")) continue;
				int foldNo = Integer.parseInt(f.toString().split("fold")[1].substring(0,1)) - 1;
				if(i == foldNo) {
					runFiles.add(f);
				} else {
					trainFiles.add(new File(f.getParentFile(), "scrapbook.xml"));
					//trainFiles.add(new File(f.getParentFile(), "handannot.xml"));
				}
			}
			System.out.println(trainFiles);
			System.out.println(runFiles);
			Model.makeModel("tmp", trainFiles, true);			
			String traceability = Traceability.getTraceabilityInfo();
			System.out.println("Running");
			for(File f : runFiles) {
				Oscar3.processDirectory(f.getParentFile(), traceability);				
			}
		}
		
	}

}
