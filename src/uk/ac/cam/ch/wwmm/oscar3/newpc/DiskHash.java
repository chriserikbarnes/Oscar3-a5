package uk.ac.cam.ch.wwmm.oscar3.newpc;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;

import uk.ac.cam.ch.wwmm.oscar3.Oscar3Props;
import uk.ac.cam.ch.wwmm.oscar3.terms.TermSets;

final class DiskHash implements Iterable<Entry<String,String>> {

	int hashSize;
	int intSize = 4;
	RandomAccessFile raf;
	
	class DHEntry implements Entry<String,String> {
		String key;
		String value;
		public DHEntry(String key, String value) {
			this.key = key;
			this.value = value;
		}
		public String getKey() {
			return key;
		}
		public String getValue() {
			return value;
		}
		public String setValue(String value) {
			this.value = value;
			return this.value;
		}
	}
	
	class DHIterator implements Iterator<Entry<String,String>> {
		int ptr;
		int hashNo;
		Entry<String,String> next;

		public DHIterator() {
			hashNo = 0;
			ptr = -1;
			primeNext();
		}

		public boolean hasNext() {
			return next != null;
		}

		public Entry<String, String> next() {
			Entry<String,String> thisNext = next;
			if(thisNext == null) return null;
			primeNext();
			return thisNext;
		}

		private void primeNext() {
			try {
				synchronized (raf) {
					while(ptr == -1) {
						if(hashNo == hashSize) {
							next = null;
							return;
						}
						raf.seek((hashNo + 1) * 4);
						ptr = raf.readInt();
						if(ptr == -1) hashNo++;
					}
					raf.seek(ptr);
					ptr = raf.readInt();
					if(ptr == -1) hashNo++;
					next = new DHEntry(raf.readUTF(), raf.readUTF());
					return;
				}
			} catch (Exception e) {
				throw new Error(e);
			}
		}

		public void remove() {
			// Stub for optional method
		}

	}

	public DiskHash(File f, int hashSize) throws Exception {
		this.hashSize = hashSize;
		raf = new RandomAccessFile(f, "rw");
		raf.setLength(0);
		raf.writeInt(hashSize);
		for(int i=0;i<hashSize;i++) {
			raf.writeInt(-1);
		}
	}

	public DiskHash(File f) throws Exception {
		raf = new RandomAccessFile(f, "rw");
		this.hashSize = raf.readInt();
	}

	public Iterator<Entry<String, String>> iterator() {
		return new DHIterator();
	}


	public void put(String key, String value) throws Exception {
		synchronized (raf) {
			int hash = Math.abs(key.hashCode()) % hashSize;		
			raf.seek((hash+1) * 4);
			int pp = raf.readInt();
			if(pp == -1) {
				long p = raf.length();
				raf.seek(p);
				raf.writeInt(-1);
				raf.writeUTF(key);
				raf.writeUTF(value);
				raf.seek((hash + 1) * 4);
				raf.writeInt((int)p);
				return;
			} else {
				int prev = (hash + 1) * 4;
				while(true) {
					raf.seek(pp);
					int next = raf.readInt();
					String key2 = raf.readUTF();
					if(key2.equals(key)) {
						if(next == -1) {
							long p = raf.length();
							raf.seek(p);
							raf.writeInt(-1);
							raf.writeUTF(key);
							raf.writeUTF(value);
							raf.seek(prev);
							raf.writeInt((int)p);
							return;						
						} else {
							raf.seek(prev);
							raf.writeInt(next);
							pp = prev;
						}
					}
					if(next == -1) {
						long p = raf.length();
						raf.seek(p);
						raf.writeInt(-1);
						raf.writeUTF(key);
						raf.writeUTF(value);
						raf.seek(pp);
						raf.writeInt((int)p);
						return;
					} else {
						prev = pp;
						pp = next;
					}
				}
			}
		}
	}

	public void remove(String key) throws Exception {
		synchronized (raf) {
			int hash = Math.abs(key.hashCode()) % hashSize;		
			raf.seek((hash + 1) * 4);
			int pp = raf.readInt();
			if(pp == -1) return;
			int prev = (hash + 1) * 4;
			while(true) {
				raf.seek(pp);
				int next = raf.readInt();
				String key2 = raf.readUTF();
				if(key2.equals(key)) {
					if(next == -1) {
						raf.seek(prev);
						raf.writeInt(-1);
						return;						
					} else {
						raf.seek(prev);
						raf.writeInt(next);
						pp = prev;
					}
				}
				if(next == -1) {
					return;
				} else {
					prev = pp;
					pp = next;
				}
			}
		}
	}

	public String get(String key) throws Exception {
		synchronized (raf) {
			int hash = Math.abs(key.hashCode()) % hashSize;

			raf.seek((hash + 1) * 4);
			int hp = raf.readInt();
			if(hp == -1) return null;
			raf.seek(hp);
			while(true) {
				int next = raf.readInt();
				String key2 = raf.readUTF();
				if(key2.equals(key)) return raf.readUTF();
				if(next == -1) return null;
				raf.seek(next);
			}

		}
	}
	
	public void export(DiskHash dh) throws Exception {
		for(Entry<String,String> e : this) {
			dh.put(e.getKey(), e.getValue());
		}
	}

	//TODO turn this into a unit test
	private void doDiskHash() throws Exception {
		Random random = new Random(0);



		//put("Hello world!", "this");
		//put("foo", "is a");
		//put("bar", "test");
		
		TermSets.getElements();
		long time = System.currentTimeMillis();
	
		for(String s : TermSets.getElements()) {
			put(s, s.toUpperCase());
		}

		Map<String,String> referenceMap = new HashMap<String,String>();
		for(int i=0;i<100;i++) {
			for(String s : TermSets.getElements()) {
				if(random.nextDouble() > 0.75) {
					referenceMap.put(s, s.toUpperCase() + "-" + i);
					put(s, s.toUpperCase() + "-" + i);
				}
			}			
			for(String s : TermSets.getElements()) {
				if(random.nextDouble() > 0.75) {
					//remove(s);
					//referenceMap.remove(s);
				}
			}			
		}
		
		if(Oscar3Props.getInstance().verbose) System.out.println(System.currentTimeMillis() - time);
		
		
		
		for(String s : TermSets.getElements()) {
			String s1 = get(s);
			String s2 = referenceMap.get(s);
			if(s1 == null && s2 != null) System.out.println("Eeeep!");
			if(s1 != null && !s1.equals(s2)) System.out.println("Eeeep!");
			//System.out.println(s + " -> " + get(s) + "->" + referenceMap.get(s));
		}

		
		List<String> el = new ArrayList<String>(TermSets.getElements());
		long totalTime = 0;
		int trials = 10000;
		for(int i=0;i<trials;i++) {
			String e = el.get(random.nextInt(el.size()));
			long nanoTime = System.nanoTime();
			get(e);
			totalTime += System.nanoTime() - nanoTime;
		}
		if(Oscar3Props.getInstance().verbose) System.out.println(totalTime / trials / 1000.0);
		
		int size = 0;
		for(Entry<String,String> e : this) {
			//System.out.println(e.getKey() + ", " + e.getValue());
			size++;
			if(!referenceMap.containsKey(e.getKey()) || !referenceMap.get(e.getKey()).equals(e.getValue())) {
				System.out.println("Eeep!");
			}
		}
		if(Oscar3Props.getInstance().verbose) System.out.println(size + "\t" + referenceMap.size());
		
		/*List<String> ll = new ArrayList<String>(TermSets.getElements());
		for(String s : ll) {
			System.out.println(s + "\t" + s.hashCode());
		}
		int n = 1;
		for(int i=0;i<1000;i++) {
			n *= 10;
			System.out.println(n);
		}*/
		

		
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		File f = File.createTempFile("foo", "bar");
		f.deleteOnExit();
		File ff = File.createTempFile("foo", "bar");
		ff.deleteOnExit();
		DiskHash dh = new DiskHash(f, TermSets.getElements().size() / 10);
		dh.doDiskHash();
		System.out.println(dh.raf.length());
		
		Map<String,String> refMap = new HashMap<String,String>();
		for(Entry<String,String> e : dh) {
			refMap.put(e.getKey(), e.getValue());
		}

		
		DiskHash dh2 = new DiskHash(ff, (TermSets.getElements().size() * 4) / 3);
		dh.export(dh2);
		List<String> el = new ArrayList<String>(TermSets.getElements());
		Random random = new Random(0);
		long totalTime = 0;
		int trials = 10000;
		for(int i=0;i<trials;i++) {
			String e = el.get(random.nextInt(el.size()));
			long nanoTime = System.nanoTime();
			dh2.get(e);
			totalTime += System.nanoTime() - nanoTime;
		}
		System.out.println(totalTime / trials / 1000.0);
		System.out.println(dh2.raf.length());
		int size = 0;
		for(Entry<String,String> e : dh2) {
			size++;
			if(!e.getValue().equals(refMap.get(e.getKey()))) System.out.println("Eeeep!");
		}
		System.out.println(size + "\t" + refMap.size());

		DiskHash dh3 = new DiskHash(ff);
		size = 0;
		for(Entry<String,String> e : dh3) {
			size++;
			if(!e.getValue().equals(refMap.get(e.getKey()))) System.out.println("Eeeep!");
		}
		System.out.println(size + "\t" + refMap.size());
		
		
	}


}
