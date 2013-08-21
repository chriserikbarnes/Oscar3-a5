package uk.ac.cam.ch.wwmm.ptc.experimental.ngramtfdf;

import java.util.List;

public class SubstringClass implements Comparable<SubstringClass> {

	private int sil;
	private int lbl;
	private int tf;
	private int df;
	private Suffix longestSuffix;
	
	public SubstringClass(int sil, int lbl, int tf, int df, Suffix longestSuffix) {
		this.sil = sil;
		this.lbl = lbl;
		this.tf = tf;
		this.df = df;
		this.longestSuffix = longestSuffix;
	}

	public int compareTo(SubstringClass o) {
		return longestSuffix.compareTo(o.longestSuffix);
	}
	
	public String getLongestSuffixString() {
		return longestSuffix.toString();
	}

	public List<String> getSuffixStrings(int minLen) {
		return longestSuffix.getStrings(minLen);
	}
	
	public Suffix getLongestSuffix() {
		return longestSuffix;
	}
	
	public int getTf() {
		return tf;
	}
	
	public int getDf() {
		return df;
	}
	
	public double ridf(int totalDocs) {
		return -(Math.log(df*1.0/totalDocs)) + Math.log(1 - Math.exp(-tf*1.0/totalDocs));
	}
	
	public double rawCVal() {
		//if(true) return sil;
		return Math.log(sil) / Math.log(2) * tf;
	}
	
	@Override
	public String toString() {
		return (longestSuffix + "\t" + sil + "\t" + lbl + "\t" + tf + "\t" + df);
	}
	
}
