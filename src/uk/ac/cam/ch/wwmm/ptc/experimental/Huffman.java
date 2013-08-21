package uk.ac.cam.ch.wwmm.ptc.experimental;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.Token;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.TokenSequence;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.TokenSequenceSource;
import uk.ac.cam.ch.wwmm.ptclib.datastruct.Bag;
import uk.ac.cam.ch.wwmm.ptclib.io.FileTools;

public class Huffman {

	static class Node implements Comparable<Node> {
		String payload;
		Node branch0;
		Node branch1;
		int freq;
		
		public Node(String payload, int freq) {
			this.payload = payload;
			this.freq = freq;
		}
		
		public Node(Node branch0, Node branch1) {
			this.branch0 = branch0;
			this.branch1 = branch1;
			freq = branch0.freq + branch1.freq;
		}
		
		public int compareTo(Node o) {
			return freq - o.freq;
		}
		
		@Override
		public String toString() {
			if(payload != null) {
				return "{" + payload + " " + freq + "}";
			} else {
				return "{0:" + branch0.toString() + " 1:" + branch1.toString() + "}"; 
			}
		}
		
		public List<String> getTable(String prefix) {
			if(payload != null) {
				List<String> l = new ArrayList<String>();
				l.add(prefix + "\t" + payload + "\t" + freq);
				return l;
			} else {
				List<String> l = branch0.getTable(prefix + "0");
				l.addAll(branch1.getTable(prefix + "1"));
				return l;
			}
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		List<File> files = new ArrayList<File>();
		files.addAll(FileTools.getFilesFromDirectoryByName(new File("/home/ptc24/oscarworkspace/corpora/newEnzyme"), "source.xml"));
		//files = files.subList(0, 10);
		
		
		Bag<String> tokenBag = new Bag<String>();
		
		TokenSequenceSource tss = new TokenSequenceSource(files);
		int i=0;
		for(TokenSequence ts : tss) {
			for(Token t : ts.getTokens()) {
				tokenBag.add(t.getValue().intern());
			}
		}
		
		
		List<Node> nodes = new ArrayList<Node>();
		for(String s : tokenBag.getList()) {
			//System.out.println(s + "\t" + tokenBag.getCount(s));
			nodes.add(new Node(s, tokenBag.getCount(s)));
		}
		long time = System.currentTimeMillis();
		PriorityQueue<Node> pq = new PriorityQueue<Node>(nodes);
		while(pq.size() > 1) {
			Node n1 = pq.poll();
			Node n2 = pq.poll();
			pq.add(new Node(n2, n1));
		}
		System.out.println(System.currentTimeMillis() - time);
		//if(true) return;
		time = System.currentTimeMillis();
		Node root = pq.poll();
		int bits = 0;
		List<String> sl = root.getTable("");
		System.out.println(System.currentTimeMillis() - time);
		for(String s : sl) {
			String [] ss = s.split("\t");
			bits += ss[0].length() * Integer.parseInt(ss[2]);
		}
		System.out.println(bits);
		System.out.println(tokenBag.entropy() * tokenBag.totalCount());
		System.out.println((Math.log(tokenBag.size()) / Math.log(2)) * tokenBag.totalCount());
		Collections.sort(sl, new Comparator<String>() {
			public int compare(String o1, String o2) {
				int c = o1.split("\t")[0].length() - o2.split("\t")[0].length();
				if(c == 0) return o1.compareTo(o2);
				return c;
			}
		});
		double tc = tokenBag.totalCount();
		double inefficiency = 0.0;
		for(String s : sl) {
			String [] ss = s.split("\t");
			double p = Integer.parseInt(ss[2]) / tc;
			double elen = -Math.log(p) / Math.log(2);
			int hlen = ss[0].length();
			System.out.println(s + "\t" + elen + "\t" + (hlen - elen));
			inefficiency += (hlen - elen) * Integer.parseInt(ss[2]);
		}
		System.out.println(inefficiency);
	}

}
