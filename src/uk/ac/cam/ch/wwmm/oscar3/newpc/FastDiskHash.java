package uk.ac.cam.ch.wwmm.oscar3.newpc;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import uk.ac.cam.ch.wwmm.oscar3.Oscar3Props;

final class FastDiskHash {
	
	static class StringHashComparator implements Comparator<String> {
		int hashSize;
		
		public StringHashComparator(int hashSize) {
			this.hashSize = hashSize;
		}
		
		int hashCode(String s) {
			return Math.abs(s.hashCode()) % hashSize;
		}
		public int compare(String o1, String o2) {
			//System.out.println(o1 + " vs " + o2);
			int i = o1.indexOf("\t");
			if(i > -1) o1 = o1.substring(0, i);
			i = o2.indexOf("\t");
			if(i > -1) o2 = o2.substring(0, i);
			int h1 = hashCode(o1);
			int h2 = hashCode(o2);
			return h1 - h2;
		}
	}
	
	Comparator<String> namesLookupComparator = new Comparator<String>() {
		public int compare(String o1, String o2) {
			int i = o1.indexOf("\t");
			if(i > -1) o1 = o1.substring(0, i);
			return o1.compareTo(o2);
		}
	};
	
	int ptr;
	int lastHash;
	DataOutputStream hashOut;
	DataOutputStream dataOut;
	
	private byte [] toByteArray(String key, String value) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream daos = new DataOutputStream(baos);
			daos.writeUTF(key);
			daos.writeUTF(value);
			return baos.toByteArray();			
		} catch (Exception e) {
			//This should never throw
			throw new Error(e);
		}
	}
	
	private void handle(List<String> keys, List<String> values, int hash) throws Exception {
		while(lastHash<(hash-1)) {
			hashOut.writeInt(-1);
			lastHash++;
		}
		hashOut.writeInt(ptr);
		for(int j=0;j<keys.size();j++) {
			byte [] kv = toByteArray(keys.get(j), values.get(j));
			int nextPtr = ptr + 4 + kv.length;
			if(j == keys.size()-1) {
				dataOut.writeInt(-1);
			} else {
				dataOut.writeInt(nextPtr);
			}
			dataOut.write(kv);
			ptr = nextPtr;
		}
		lastHash = hash;
	}
	
	public void createHashInternal(File input, File output) throws Exception {
		long time = System.currentTimeMillis();
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(input), "UTF-8"));
		int lines = 0;
		for(String line = br.readLine();line != null;line = br.readLine()) lines++;
		//System.out.println(lines + " names to hash: " + (System.currentTimeMillis() - time));

		int hashSize = (lines * 4) / 3;

		StringHashComparator fc = new StringHashComparator(hashSize);
		File tmpFile = File.createTempFile("hash", "txt");
		//System.out.println(tmpFile);
		SortFile.sortFile(input, tmpFile, fc, true);
		//System.out.println("Sorted:" + (System.currentTimeMillis() - time));
		tmpFile.deleteOnExit();

		br = new BufferedReader(new InputStreamReader(new FileInputStream(tmpFile), "UTF-8"));
		int lc = 0;
		int currentHashVal = -1;
		List<String> keys = new ArrayList<String>();
		List<String> values = new ArrayList<String>();

		//File baseHashTableFile = File.createTempFile("hash", ".hash");
		//baseHashTableFile.deleteOnExit();
		hashOut = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(output)));
		hashOut.writeInt(hashSize);
		lastHash = -1;
		
		//baseHashTable = new int[hashSize];
		//for(int i=0;i<baseHashTable.length;i++) baseHashTable[i] = -1;
		ptr = (hashSize + 1) * 4;

		File postTable = File.createTempFile("hash", ".hash");
		postTable.deleteOnExit();
		dataOut = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(postTable)));

		for(String line = br.readLine();line != null;line = br.readLine()) {
			//System.out.println(line);
			int i = line.indexOf("\t");
			String key = line;
			String value = line;
			if(i != -1) {
				key = line.substring(0, i);
				value = line.substring(i+1);
			} else {
				if(Oscar3Props.getInstance().verbose) System.out.println(line + "???");
			}
			int hash = Math.abs(key.hashCode()) % hashSize;
			if(hash != currentHashVal) {
				if(currentHashVal != -1) {
					handle(keys,values,currentHashVal);
					keys.clear();
					values.clear();
				}
				currentHashVal = hash;
			}
			keys.add(key);
			values.add(value);
		}
		handle(keys, values, currentHashVal);
		while(lastHash<(hashSize-1)) {
			hashOut.writeInt(-1);
			lastHash++;
		}
		//System.out.println(hashSize);
		//System.out.println(baseHashOut.size());
		//baseHashOut.close();
		//System.out.println(baseHashTableFile.length());
		//System.out.println(hashSize);
		
		dataOut.close();
		//System.out.println("Linkedlists:" + (System.currentTimeMillis() - time));

		//DataOutputStream daos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(output)));
		//daos.writeInt(hashSize);
		/*InputStream is = new BufferedInputStream(new FileInputStream(baseHashTableFile));
		byte[] buffer = new byte[1024];
		int i = 0;
		while ((i = is.read(buffer)) != -1) {
			daos.write(buffer, 0, i);
		}
		is.close();
		daos.flush();
		System.out.println(output.length());*/
		InputStream is = new BufferedInputStream(new FileInputStream(postTable));
		byte [] buffer = new byte[1024];
		int i = 0;
		while ((i = is.read(buffer)) != -1) {
			hashOut.write(buffer, 0, i);
		}
		is.close();
		hashOut.close();
		//System.out.println("Done:" + (System.currentTimeMillis() - time));

		//DiskHash dh = new DiskHash(outHash);
		//br = new BufferedReader(new FileReader(nameFile));
		//for(String line = br.readLine();line != null;line = br.readLine()) {
		//	System.out.println(line + " -> " + dh.get(line.split("\t")[0]));
		//}
	}
	
	public static void createHash(File input, File output) throws Exception{
		new FastDiskHash().createHashInternal(input, output);
	}


	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		File nameFile = new File("/home/ptc24/newows/npc/smallnames.txt");
		File ff = File.createTempFile("foo", ".hash");
		ff.deleteOnExit();
		createHash(nameFile, ff);
		DiskHash dh = new DiskHash(ff);
		BufferedReader br = new BufferedReader(new FileReader(nameFile));
		int count = 0;
		for(String line = br.readLine();line != null;line = br.readLine()) {
			int i = line.indexOf("\t");
			String key = line.substring(0,i);
			String value = line.substring(i+1);
			if(!value.equals(dh.get(key))) System.out.println("Eeep!" + line);
			//System.out.println(line + " -> " + dh.get(line.split("\t")[0]));
			//if(count++ % 10000 == 0) {
			//	System.out.println(line + " -> " + dh.get(line.split("\t")[0]));
			//}
		}

	}
}
