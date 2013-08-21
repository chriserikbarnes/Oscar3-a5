package uk.ac.cam.ch.wwmm.oscar3.newpc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**A class for on-disk mergesorting. The files to be mergesorted are text files,
 * and the unit of sorting is the line.
 * 
 * @author ptc24
 *
 */
public final class SortFile {

	private static int bufferSize = 1000;
	private boolean uniq;
	
	private List<File> toDelete;
	private Comparator<String> comparator;
	
	private SortFile(Comparator<String> comparator, boolean uniq) {
		this.uniq = uniq;
		this.comparator = comparator;
	}
	
	private void mergeFiles(File f1, File f2, File merged) throws Exception {
		BufferedReader br1 = new BufferedReader(new FileReader(f1));
		BufferedReader br2 = new BufferedReader(new FileReader(f2));
		String line1 = br1.readLine();
		String line2 = br2.readLine();
		String lastLine = null;
		FileWriter fw = new FileWriter(merged);
		while(line1 != null && line2 != null) {
			int cmpVal = comparator == null ?
					line1.compareTo(line2) :
					comparator.compare(line1, line2);
			if(cmpVal <= 0) {
				if(!uniq || !line1.equals(lastLine)) fw.write(line1 + "\n");
				lastLine = line1;
				line1 = br1.readLine();
			} else {
				if(!uniq || !line2.equals(lastLine)) fw.write(line2 + "\n");
				lastLine = line2;
				line2 = br2.readLine();
			}
		}
		while(line1 != null) {
			fw.write(line1 + "\n");
			line1 = br1.readLine();
		}
		while(line2 != null) {
			fw.write(line2 + "\n");
			line2 = br2.readLine();
		}
		br1.close();
		br2.close();
		fw.close();
	}
	
	private File mergeFiles(List<File> toMerge, File out) throws Exception {		
		if(toMerge.size() == 1)  {
			if(out != null) {
				BufferedReader br = new BufferedReader(new FileReader(toMerge.get(0)));
				FileWriter fw = new FileWriter(out);
				for(String line=br.readLine();line!=null;line=br.readLine()) fw.write(line + "\n");
				br.close();
				fw.close();
				return out;
			} else {
				return toMerge.get(0);				
			}
		}
		if(toMerge.size() == 2) {
			if(out == null) {
				out = File.createTempFile("sorttmp", ".txt");
				out.deleteOnExit();
				toDelete.add(out);
			}
			mergeFiles(toMerge.get(0), toMerge.get(1), out);
			return out;
		}
		List<File> tma = toMerge.subList(0, toMerge.size() / 2);
		List<File> tmb = toMerge.subList(toMerge.size() / 2, toMerge.size());
		List<File> pair = new ArrayList<File>();
		pair.add(mergeFiles(tma, null));
		pair.add(mergeFiles(tmb, null));
		return mergeFiles(pair, out);
	}
	
	private void sortFileInternal(File in, File out) throws Exception {
		BufferedReader br = new BufferedReader(new FileReader(in));
		ArrayList<String> lineBuffer = new ArrayList<String>(bufferSize);
		String line = null;
		for(int i=0;i<bufferSize;i++) {
			line = br.readLine();
			if(line == null) break;
			lineBuffer.add(line);
		}
		if(comparator == null) {
			Collections.sort(lineBuffer);			
		} else {
			Collections.sort(lineBuffer, comparator);
		}
		if(line == null) {
			FileWriter fw = new FileWriter(out);
			String lastLine = null;
			for(String s : lineBuffer) {
				if(!uniq || !s.equals(lastLine)) fw.write(s);
				lastLine = s;
				fw.write("\n");
			}
			return;
		}
		List<File> tmpFiles = new ArrayList<File>();
		do {
			File tmpFile = File.createTempFile("sorttmp", ".txt");
			tmpFile.deleteOnExit();
			FileWriter fw = new FileWriter(tmpFile);
			for(String s : lineBuffer) {
				fw.write(s);
				fw.write("\n");
			}
			fw.close();
			tmpFiles.add(tmpFile);
			lineBuffer.clear();
			for(int i=0;i<bufferSize;i++) {
				line = br.readLine();
				if(line == null) break;
				lineBuffer.add(line);
			}
			if(comparator == null) {
				Collections.sort(lineBuffer);			
			} else {
				Collections.sort(lineBuffer, comparator);
			}
		} while(line != null);
		if(lineBuffer.size() > 0) {
			File tmpFile = File.createTempFile("sorttmp", ".txt");
			tmpFile.deleteOnExit();
			FileWriter fw = new FileWriter(tmpFile);
			for(String s : lineBuffer) {
				fw.write(s);
				fw.write("\n");
			}
			fw.close();
			tmpFiles.add(tmpFile);
		}
		toDelete = new ArrayList<File>(tmpFiles);
		File f = mergeFiles(tmpFiles, out);
		for(File tmpFile : toDelete) {
			if(tmpFile.exists()) tmpFile.delete();
		}
		
	}
	
	/**Sorts a file.
	 * 
	 * @param in The input file (will not be changed).
	 * @param out The output file (will be created).
	 * @param comparator The ordering of the lines.
	 * @param uniq Whether to merge lines if they are exactly equal.
	 * @throws Exception
	 */
	public static void sortFile(File in, File out, Comparator<String> comparator, boolean uniq) throws Exception {
		new SortFile(comparator, uniq).sortFileInternal(in, out);
	}
	
}
