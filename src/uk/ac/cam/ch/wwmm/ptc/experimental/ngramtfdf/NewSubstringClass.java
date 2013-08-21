package uk.ac.cam.ch.wwmm.ptc.experimental.ngramtfdf;

import java.util.List;

public class NewSubstringClass {
	private int sil;
	private int lbl;
	private int tf;
	private int df;
	private int fo;
	
	public NewSubstringClass(int sil, int lbl, int tf, int df, int fo) {
		this.sil = sil;
		this.lbl = lbl;
		this.tf = tf;
		this.df = df;
		this.fo = fo;
	}

	/*public int compareTo(SubstringClass o) {
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
	}*/
	
	public int getSil() {
		return sil;
	}
	
	public int getLbl() {
		return lbl;
	}
	
	public int getTf() {
		return tf;
	}
	
	public int getDf() {
		return df;
	}
	
	public int getFo() {
		return fo;
	}
	
	public double ridf(int totalDocs) {
		return -(Math.log(df*1.0/totalDocs)) + Math.log(1 - Math.exp(-tf*1.0/totalDocs));
	}
	
	public double rawCVal() {
		//if(true) return sil;
		return Math.log(sil) / Math.log(2) * tf;
	}
	
	/*@Override
	public String toString() {
		return (longestSuffix + "\t" + sil + "\t" + lbl + "\t" + tf + "\t" + df);
	}*/
}
