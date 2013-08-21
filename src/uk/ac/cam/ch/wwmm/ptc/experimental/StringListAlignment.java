package uk.ac.cam.ch.wwmm.ptc.experimental;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;

/**Experimental: Takes two lists of strings, and aligns them.
 * 
 * @author ptc24
 *
 */
public final class StringListAlignment {

	List<String> strings1;
	List<String> strings2;
	
	List<List<Integer>> aligned1;
	List<List<Integer>> aligned2;
	
	boolean success;
	boolean verbose;
	
	public StringListAlignment(List<String> strings1, List<String> strings2) {
		this.strings1 = strings1;
		this.strings2 = strings2;
		
		aligned1 = new ArrayList<List<Integer>>();
		aligned2 = new ArrayList<List<Integer>>();
	
		success = true;
		
		int s1p = 0;
		int s2p = 0;
		if(verbose) System.out.println(strings1);
		if(verbose) System.out.println(strings2);
		
		while(s1p < strings1.size() && s2p < strings2.size()) {
			String s1 = strings1.get(s1p);
			String s2 = strings2.get(s2p);
			if(s1.equals(s2)) {
				if(verbose) System.out.println("Match: " + strings1.get(s1p));
				aligned1.add(oneMemberList(s1p));
				aligned2.add(oneMemberList(s2p));
				s1p++;
				s2p++;
				// Skip empty in strings1;
			} else if(s1.length() == 0) {
				if(verbose) System.out.println("Skip empty in 1");
				s1p++;
				// Skip empty in strings2;
			} else if(s2.length() == 0) {
				if(verbose) System.out.println("Skip empty in 2");
				s2p++;
				// Look for supersets; 
			} else if(lengthOfFragmentedString(s1p, s2p) > 0) {
				int l = lengthOfFragmentedString(s1p, s2p);
				if(s1.length() > s2.length()) {
					if(verbose) System.out.println("Match: " + s1 + " with " + strings2.subList(s2p, s2p+l));
					aligned1.add(oneMemberList(s1p));
					aligned2.add(nMemberList(s2p, l));
					s1p++;
					s2p += l;
				} else {
					if(verbose) System.out.println("Match: " + strings1.subList(s1p, s1p+l) + " with " + s2);
					aligned2.add(oneMemberList(s2p));
					aligned1.add(nMemberList(s1p, l));
					s2p++;
					s1p += l;					
				}
				// Insertion in strings1;
			} else if(s1p + 1 < strings1.size() && s2.equals(strings1.get(s1p+1))) {
				if(verbose) System.out.println("Skip in 1: " + s1);
				s1p++;
				// Insertion in strings2;
			} else if(s2p + 1 < strings2.size() && s1.equals(strings2.get(s2p+1))) {
				if(verbose) System.out.println("Skip in 2: " + s2);
				s2p++;
				// string1 is whitespace?;
			} else if(s1.matches("\\s+")) {
				s1p++;
				// string2 is whitespace?;
			} else if(s2.matches("\\s+")) {
				s2p++;
			} else {
				String sls1 = StringTools.stringListToString(strings1.subList(s1p, strings1.size()));
				String sls2 = StringTools.stringListToString(strings2.subList(s2p, strings2.size()));
				if(sls1.equals(sls2)) {
					if(verbose) System.out.println("Matching all the rest");
					aligned1.add(nMemberList(s1p, strings1.size() - s1p));
					aligned2.add(nMemberList(s1p, strings2.size() - s2p));
					s1p = strings1.size();
					s2p = strings2.size();
				} else if(sls1.endsWith(sls2)) {
					if(verbose) System.out.println("Skip (on suffix) in 1: " + s1);
					s1p++;
				} else if(sls2.endsWith(sls1)) {
					if(verbose) System.out.println("Skip (on suffix) in 2: " + s2);
					s2p++;
				} else {
					String slss1 = StringTools.stringListToString(strings1.subList(s1p+1, strings1.size()));
					String slss2 = StringTools.stringListToString(strings2.subList(s2p+1, strings2.size()));
					int ld1 = StringUtils.getLevenshteinDistance(slss1, sls2);
					int ld2 = StringUtils.getLevenshteinDistance(slss2, sls1);
					if(ld1 < ld2) {
						if(verbose) System.out.println("Skip (on edit distance) in 1: " + s1);
						s1p++;
					} else {
						if(verbose) System.out.println("Skip (on edit distance) in 2: " + s2);
						s2p++;						
					}
				}
			}
		}
	}

	
	private int lengthOfFragmentedString(int s1p, int s2p) {
		if(verbose) System.out.println("Looking...");
		int l1 = strings1.get(s1p).length();
		int l2 = strings2.get(s2p).length();
		
		List<String> sl1 = strings1;
		List<String> sl2 = strings2;
		
		//Doesn't work for equal length 
		if(l1 == l2) {
			if(verbose) System.out.println("l1: " + l1 + " " + l2);
			return 0;
		//Assume string 1 is longer. If not, switch the labels so that it is
		} else if(l1 < l2) {
			if(verbose) System.out.println("Switch");
			int tmp = s1p;
			s1p = s2p;
			s2p = tmp;
			sl1 = strings2;
			sl2 = strings1;
		}
		if(verbose) System.out.println("Looking2...");

		
		String toMatch = sl1.get(s1p);
		if(verbose) System.out.println("To match: " + toMatch);
		int i=1;
		while(s2p+1 < sl2.size()) {
			List<String> subList = sl2.subList(s2p, s2p+i);
			String subListStr = StringTools.stringListToString(subList);
			if(verbose) System.out.println("Match with: " + subListStr);
			if(subListStr.equals(toMatch)) return i;
			if(!toMatch.startsWith(subListStr)) return 0;
			i++;
		}
			
		return 0;
	}
	
	private List<Integer> oneMemberList(int i) {
		List<Integer> l = new ArrayList<Integer>();
		l.add(i);
		return l;
	}

	private List<Integer> nMemberList(int i, int n) {
		List<Integer> l = new ArrayList<Integer>();
		for(int j=i;j<i+n;j++) l.add(j);
		return l;
	}
	
	public List<List<Integer>> getAligned1() {
		return aligned1;
	}
	
	public List<List<Integer>> getAligned2() {
		return aligned2;
	}
	
	public boolean isSuccess() {
		return success;
	}
}
