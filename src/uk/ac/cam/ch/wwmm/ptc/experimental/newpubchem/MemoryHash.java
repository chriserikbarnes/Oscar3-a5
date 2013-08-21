package uk.ac.cam.ch.wwmm.ptc.experimental.newpubchem;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;

public class MemoryHash {
	
	static DataInputStream dis;
	static int hashSize; 
	
	public static String get(String key) throws Exception {
			int hash = Math.abs(key.hashCode()) % hashSize;

			dis.reset();
			dis.skip((hash + 1) * 4);
			int hp = dis.readInt();
			if(hp == -1) return null;
			dis.reset();
			dis.skip(hp);
			while(true) {
				int next = dis.readInt();
				String key2 = dis.readUTF();
				if(key2.equals(key)) return dis.readUTF();
				if(next == -1) return null;
				dis.reset();
				dis.skip(next);
			}
	}
	public static void main(String[] args) throws Exception {
		File f = new File("/home/ptc24/newows/npc/names.hash");
		InputStream is = new BufferedInputStream(new FileInputStream(f));
		byte [] bytes = new byte[(int)f.length()];
		System.out.println(is.read(bytes) == f.length());
		dis = new DataInputStream(new ByteArrayInputStream(bytes));

		/*ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte [] buffer = new byte[1024];
		int i = 0;
		while ((i = is.read(buffer)) != -1) {
			baos.write(buffer, 0, i);
		}
		is.close();
		//bais = new ByteArrayInputStream(baos.toByteArray());
		dis = new DataInputStream(new ByteArrayInputStream(baos.toByteArray()));*/
		hashSize = dis.readInt();
		System.out.println(hashSize);
		
		BufferedReader br = new BufferedReader(new FileReader(new File("/home/ptc24/newows/npc/names.txt")));
		long totalTime = 0;
		int totalTests = 0;
		for(String line = br.readLine();line!=null;line = br.readLine()) {
			totalTests++;
			String name = line.split("\t")[0];
			String val = line.split("\t", 2)[1];
			long time = System.nanoTime();
			if(!get(name).equals(val)) System.out.println("Eeep!");
			totalTime += System.nanoTime() - time;
		}
		System.out.println(totalTime / totalTests / 1000.0);
		System.out.println(get("morphine"));
	}
	
}
