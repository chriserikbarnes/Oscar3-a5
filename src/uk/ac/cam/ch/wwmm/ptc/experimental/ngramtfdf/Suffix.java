package uk.ac.cam.ch.wwmm.ptc.experimental.ngramtfdf;

import java.util.ArrayList;
import java.util.List;

public class Suffix implements Comparable<Suffix> {

	private int docID;
	private List<String> suffix;
	
	public Suffix(int docID, List<String> suffix) {
		this.docID = docID;
		this.suffix = suffix;
	}
	
	public int compareTo(Suffix s) {
		int minLen = Math.min(s.suffix.size(), suffix.size());
		for(int i=0;i<minLen;i++) {
			String a = suffix.get(i);
			String b = s.suffix.get(i);
			if(a != b) {
				return a.compareTo(b);
			} 
		}
		if(s.suffix.size() < suffix.size()) return 1;
		if(s.suffix.size() > suffix.size()) return -1;
		if(s.docID < docID) return -1;
		if(s.docID > docID) return 1;
		return 0;
	}
	
	public int longestCommonPrefixLength(Suffix s) {
		int minLen = Math.min(s.suffix.size(), suffix.size());
		for(int i=0;i<minLen;i++) {
			String a = suffix.get(i);
			String b = s.suffix.get(i);
			if(a != b) {
				return i;
			} 
		}
		return minLen;
	}
	
	public Suffix longestCommonPrefix(Suffix s) {
		List<String> res = suffix.subList(0, longestCommonPrefixLength(s));
		return new Suffix(-1, res);
	}
	
	public int getDocID() {
		return docID;
	}
	
	public List<String> getSuffix() {
		return suffix;
	}

	public List<String> getStrings(int minLen) {
		List<String> strings = new ArrayList<String>();
		StringBuffer sb = new StringBuffer();
		for(int i=0;i<suffix.size();i++) {
			if(i > 0) sb.append(" ");
			sb.append(suffix.get(i));
			if(i >= minLen - 1) strings.add(sb.toString());
		}
		return strings;
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		for(String s : suffix) {
			sb.append(s);
			sb.append(" ");
		}
		sb.deleteCharAt(sb.length()-1);
		return sb.toString();
	}

}
