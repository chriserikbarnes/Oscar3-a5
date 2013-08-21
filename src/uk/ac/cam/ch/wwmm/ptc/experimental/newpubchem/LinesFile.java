package uk.ac.cam.ch.wwmm.ptc.experimental.newpubchem;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import sun.security.action.GetLongAction;
import uk.ac.cam.ch.wwmm.oscar3.Oscar3Props;
import uk.ac.cam.ch.wwmm.ptclib.datastruct.CacheMap;

public class LinesFile implements Iterable<String> {

	class LinesFileIterator implements Iterator<String> {
		int pos;
		public LinesFileIterator() {
			pos = 0;
		}
		
		public boolean hasNext() {
			return pos < positions.length;
		}
		
		public String next() {
			try {
				return getLine(pos++);				
			} catch (Exception e) {
				throw new Error(e);
			}
		}
		
		public void remove() {
			
		}
		
	}
	
	private RandomAccessFile raf;
	private int [] positions;
	private CacheMap<Integer, String> cache;
	
	public static void indexLines(File f, DataOutputStream daos) throws Exception {
		List<Integer> l = new ArrayList<Integer>();
		RandomAccessFile raf = new RandomAccessFile(f, "r");
		String line = null;
		int pos = 0;
		do {
			line = raf.readLine();
			if(line != null) l.add(pos);
			pos = (int)(raf.getFilePointer());
			//while(line != null && line.length() == 0) line = raf.readLine();
		} while(line != null);
		daos.writeInt(l.size());
		for(Integer i : l) daos.writeInt(i);
	}
	
	public LinesFile(File f, DataInputStream dais) throws Exception {
		positions = new int[dais.readInt()];
		for(int i=0;i<positions.length;i++) positions[i] = dais.readInt();
		raf = new RandomAccessFile(f, "r");
		cache = new CacheMap<Integer, String>(1000);
	}
	
	public String getLine(int position) throws Exception {
		if(position < 0 || position >= positions.length) return null;
		raf.seek(positions[position]);
		if(true) {
			if(cache.containsKey(position)) return cache.get(position);
			String line;
			if(position == positions.length-1) {
				line = raf.readLine();
			} else {
				byte[] buffer = new byte[(positions[position+1] - positions[position]) - 1];
				raf.read(buffer);
				line = new String(buffer);
			}

			
			//= raf.readLine();
			cache.put(position, line);
			return line;
		}
		
		//if(true) return raf.readLine();
		
		if(position == positions.length-1) {
			return raf.readLine();
		} else {
			byte[] buffer = new byte[(positions[position+1] - positions[position]) - 1];
			raf.read(buffer);
			return new String(buffer);
		}
		//byte [] buffer = new byte[100];
		//raf.read(buffer);
		//BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(buffer)));
		//return br.readLine();
		//return raf.readLine();
	}
	
	public String getLine(String key, Comparator<String> comparator) throws Exception {
		//key = key + "\t";
		String result = null;
		int lower = 0;
		int upper = positions.length-1;
		String lowerStr = getLine(lower);
		String upperStr = getLine(upper);
		//System.out.println(lowerStr);
		//System.out.println(upperStr);
		while(true) {
			if(comparator.compare(lowerStr, key) == 0) {
				result = lowerStr;
				break;
			} else if(comparator.compare(upperStr, key) == 0) {
				result = upperStr;
				break;
			}
			if(lower == upper || lower == upper-1) break;
			int mid = (lower + upper) / 2;
			//System.out.println(lower + "/" + mid + "/" + upper);
			String midStr = getLine(mid);
			//System.out.println(lowerStr + "\t" + midStr + "\t" + upperStr);
			int comp = comparator.compare(midStr, key);
			//System.out.println(comp);
			if(comp > 0) {
				upperStr = midStr;
				upper = mid;
			} else if(comp == 0) {
				result = midStr;
				break;
			} else {
				lowerStr = midStr;
				lower = mid;
			}
		}
		return result;
	}
	
	public int size() {
		return positions.length;
	}
	
	public Iterator<String> iterator() {
		return new LinesFileIterator();
	}
	
	public static void main(String[] args) throws Exception {
		
	}

}
