package uk.ac.cam.ch.wwmm.oscar3.misc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Serializer;
import uk.ac.cam.ch.wwmm.ptclib.scixml.PubXMLToSciXML;
import uk.ac.cam.ch.wwmm.ptclib.scixml.ToSciXML;

/** Constructs Oscar3 corpus directory structures from other directories.
 * 
 * This will take a directory with files in HTML, plain text, SciXML or (if you
 * have the stylesheets installed) a Publisher's XML, and convert these into a
 * directory structure, where each paper resides in a subdirectory of its own, in
 * a SciXML file called source.xml. The original file is also kept. This class also
 * does the right thing when the source directory contains subdirectories of its
 * own.
 * 
 * @author ptc24
 *
 */
public final class MakeDirs {

	private static void copyFile(File from, File to) throws Exception {
		FileChannel in = new FileInputStream(from).getChannel();
		FileChannel out = new FileOutputStream(to).getChannel();
		in.transferTo(0, in.size(), out);
		in.close();
		out.close();
	}
	
	/**Constructs a directory structure containing SciXML files from a directory
	 * containing input files in various formats.
	 * 
	 * @param dir The directory containing input files.
	 * @param destDir The directory in which to place the Oscar3 SciXML 
	 * directory structures.
	 * @throws Exception
	 */
	public static void makeDirs(File dir, File destDir) throws Exception {
		if(!dir.exists()) throw new Exception("Directory does not exist!");
		if(!dir.isDirectory()) throw new Exception ("Not a directory!");
		
		if(!destDir.exists()) destDir.mkdirs();
		
		File [] children = dir.listFiles();
		for(int i=0;i<children.length;i++) {
			if(children[i].isDirectory()) {
				makeDirs(children[i], new File(destDir, children[i].getName()));
			} else {
				String name = children[i].getName();
				if(name.endsWith(".txt") || name.matches("source_file_\\d+_\\d+.src")) {
					File paperDir = new File(destDir, name.substring(0, name.length()-4));
					paperDir.mkdir();
					File paperFile = new File(paperDir, "source.txt");
					copyFile(children[i], paperFile);
					File xmlFile = new File(paperDir, "source.xml");
					new Serializer(new FileOutputStream(xmlFile)).write(ToSciXML.fileToSciXML(children[i]));
				} else if(name.endsWith(".html")) {
					File paperDir = new File(destDir, name.substring(0, name.length()-5));
					paperDir.mkdir();
					File paperFile = new File(paperDir, "source.html");
					copyFile(children[i], paperFile);
					File xmlFile = new File(paperDir, "source.xml");
					new Serializer(new FileOutputStream(xmlFile)).write(ToSciXML.fileToSciXML(children[i]));					
				} else if(name.endsWith(".src.ann")) {
					//BioIE related - ignore
				} else if(name.endsWith(".xml")) {
					File paperDir = new File(destDir, name.substring(0, name.length()-4));
					paperDir.mkdir();
					Document doc = new Builder().build(children[i]);
					/* is this an RSC document? */
					if(PubXMLToSciXML.isRSCDoc(doc)) {
						PubXMLToSciXML ptsx = new PubXMLToSciXML(doc);
						doc = ptsx.getSciXML();
						File paperFile = new File(paperDir, "pubxml-source.xml");
						copyFile(children[i], paperFile);
						File xmlFile = new File(paperDir, "source.xml");
						new Serializer(new FileOutputStream(xmlFile)).write(ptsx.getSciXML());
						File convFile = new File(paperDir, "conv.xml");
						new Serializer(new FileOutputStream(convFile)).write(ptsx.xpcToDoc());
					} else {
						File paperFile = new File(paperDir, "source.xml");
						copyFile(children[i], paperFile);
					}					
				} else {
					System.out.println("Ignoring: " + name);
					//throw new Exception("Filename extension of: " + name + " not supported!");
				}
			}
		}	
	}

}
