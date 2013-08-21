package uk.ac.cam.ch.wwmm.ptc.experimental.newpubchem;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import uk.ac.cam.ch.wwmm.oscar3.newpc.NewPubChem;
import uk.ac.cam.ch.wwmm.oscar3.newpc.SortFile;
import uk.ac.cam.ch.wwmm.ptclib.datastruct.Bag;

public class AnalyseSubstances {

	public static void reportLines(List<String> lines, PrintWriter out, Map<String,Integer> votesCast, Map<String,Double> totalFractions) throws Exception {
		Bag<String> cids = new Bag<String>();
		Map<String,String> dToC = new HashMap<String,String>();
		Set<String> badDepositors = new HashSet<String>();
		for(String line : lines) {
			String [] ls = line.split("\t");
			String cid = ls[1];
			cids.add(cid);
			if(NewPubChem.getInstance().lookupCid(cid) == null) continue;
			String depositor = ls[4];
			if(badDepositors.contains(depositor)) {
				
			} else if(dToC.containsKey(depositor)) {
				dToC.remove(depositor);
				badDepositors.add(depositor);
			} else {
				dToC.put(depositor, cid);
			}
		}
		Bag<String> newCids = new Bag<String>();
		for(String d : dToC.keySet()) newCids.add(dToC.get(d));
		if(newCids.size() > 1) {
			double totalCount = newCids.totalCount();
			for(String d : dToC.keySet()) {
				if(!votesCast.containsKey(d)) {
					votesCast.put(d, 0);
					totalFractions.put(d, 0.0);
				}
				votesCast.put(d, votesCast.get(d) + 1);
				totalFractions.put(d, totalFractions.get(d) + (newCids.getCount(dToC.get(d)) / totalCount));
			}
		}
		/*if(cids.size() > 1) {
			for(String cid : cids.getList()) {
				out.println(cid + "\t" + cids.getCount(cid));
				String [] smilesAndInChI = NewPubChem.getInstance().lookupCid(cid);
				if(smilesAndInChI != null) {
					out.println(smilesAndInChI[0] + "\t" + smilesAndInChI[1]);
				}	
			}
			for(String line : lines) {
				out.println(line);
			}
			out.println();
		}*/
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		Map<String,Integer> votesCast = new HashMap<String,Integer>();
		Map<String,Double> totalFractions = new HashMap<String,Double>();
		if(false) {
			File in = new File("/home/ptc24/tmp/pcsnames.txt");
			File out = new File("/home/ptc24/tmp/pcsnamessorted.txt");
			SortFile.sortFile(in, out, new Comparator<String>() {
				public int compare(String o1, String o2) {
					return o1.toLowerCase().compareTo(o2.toLowerCase());
				}
			}, true);			
		}
		long time = System.currentTimeMillis();
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File("/home/ptc24/tmp/pcsnamessorted.txt"))));
		PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File("/home/ptc24/tmp/pcsanalysis2.txt"))));
		List<String> lines = new ArrayList<String>();
		String name = null;
		for(String line=br.readLine();line!=null;line=br.readLine()) {
			String [] ss = line.split("\t");
			if(!ss[0].equals(name)) {
				reportLines(lines, out, votesCast, totalFractions);
				lines.clear();
			}
			lines.add(line);
			name = ss[0];
		}
		reportLines(lines, out, votesCast, totalFractions);
		lines.clear();
		br.close();
		out.close();
		System.out.println(System.currentTimeMillis() - time);

		for(String s : votesCast.keySet()) {
			System.out.println(s + "\t" + votesCast.get(s) + "\t" + (totalFractions.get(s) / votesCast.get(s)));
		}
	}

}
