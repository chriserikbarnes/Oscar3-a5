package uk.ac.cam.ch.wwmm.ptclib.cdk;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import net.sf.jniinchi.INCHI_OPTION;

import org.openscience.cdk.Molecule;
import org.openscience.cdk.inchi.InChIGeneratorFactory;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.io.MDLReader;
import org.openscience.cdk.io.MDLWriter;

import sun.misc.Lock;
import uk.ac.cam.ch.wwmm.oscar3.Oscar3Props;
import uk.ac.cam.ch.wwmm.oscar3.deployment.Deployment;
import uk.ac.cam.ch.wwmm.ptclib.datastruct.CacheMap;

/** Converts CDK molecules to InChI strings, using the InChI executable.
 * 
 * @author ptc24
 *
 */
public final class ConverterToInChI {
	
	private InChIGeneratorFactory igf;
	private CacheMap<String, IMolecule> molCache;
	private ReentrantLock lock;
	
	private static ConverterToInChI myInstance;
	
	private static ConverterToInChI getInstance() {
		if(myInstance == null) myInstance = new ConverterToInChI();
		return myInstance;
	}
	
	private ConverterToInChI() {
		lock = new ReentrantLock();
		molCache = new CacheMap<String, IMolecule>(500);
		try {
			if(Oscar3Props.getInstance().useJNIInChI) {
				igf = new InChIGeneratorFactory();
			} else {
				igf = null;
			}
		} catch (Exception e) {
			//e.printStackTrace();
			System.err.println("Falling back to using non-JNI InChI - please ignore the above!");
			igf = null;
		}
		if(!"none".equals(Oscar3Props.getInstance().InChI) && !new File(Oscar3Props.getInstance().InChI).exists()) {
			try {
			Deployment.deployInChI();
			} catch (Exception e) {
				throw new Error(e);
			}
		}
	}
	
	/**Gets the InChI string for a CDK molecule, or null.
	 * 
	 * @param mol The CDK molecule.
	 * @return The InChI string.
	 */
	public static String getInChI(IMolecule mol) {
		return getInChI(mol, false);
	}
	
	/**Gets the InChI string for a CDK molecule, or null.
	 * 
	 * @param mol The CDK molecule.
	 * @param stdInChI Whether to use stdInChI
	 * @return The InChI string.
	 */
	public static String getInChI(IMolecule mol, boolean stdInChI) {
		return getInstance().getInChIInternal(mol, stdInChI);
	}

	/**Gets the InChIKey string for a CDK molecule, or null.
	 * 
	 * @param mol The CDK molecule.
	 * @return The InChIKey string.
	 */
	public static String getInChIKey(IMolecule mol) {
		return getInstance().getInChIUsingApplicationInternal(mol, true, false);
	}
	
	/**Gets the InChIKey string for a CDK molecule, or null.
	 * 
	 * @param mol The CDK molecule.
	 * @return The InChIKey string.
	 */
	public static String getInChIKey(IMolecule mol, boolean stdInChI) {
		return getInstance().getInChIUsingApplicationInternal(mol, true, stdInChI);
	}

	private String getInChIInternal(IMolecule mol, boolean stdInChI) {
		if(stdInChI) {
			return getInChIUsingApplicationInternal(mol, false, true);
		} 
		if(igf != null) {
			try {
				lock.lock();
				List<INCHI_OPTION> options = new ArrayList<INCHI_OPTION>();
				options.add(INCHI_OPTION.FixedH);
				return igf.getInChIGenerator(mol, options).getInchi();
				//return igf.getInChIGenerator(mol).getInchi();
			} catch (Exception e) {
				return null;
			} finally {
				lock.unlock();
			}
		}
		return getInChIUsingApplicationInternal(mol);
	}
	
	/**Gets the InChI string for a CDK molecule, using the InChI application 
	 * (never the JNI InChI).
	 * 
	 * @param mol The CDK molecule.
	 * @return The InChI string.
	 */
	public static String getInChIUsingApplication(IMolecule mol) {
		return getInstance().getInChIUsingApplicationInternal(mol);
	}
	
	private String getInChIUsingApplicationInternal(IMolecule mol) {
		return getInChIUsingApplicationInternal(mol, false, false);
	}
	
	private String getInChIUsingApplicationInternal(IMolecule mol, boolean key, boolean stdInChI) {
		String inchiBinary = stdInChI ? Oscar3Props.getInstance().stdInChI : Oscar3Props.getInstance().InChI;
		if("none".equals(inchiBinary)) return null;
		try {
			lock.lock();
			File tmpMolFile = File.createTempFile("m2i", ".mol");
			File tmpInChIFile = File.createTempFile("m2i", ".mol.txt");
			
			MDLWriter mdlWriter = new MDLWriter(new FileWriter(tmpMolFile));
			mdlWriter.writeMolecule(mol);
			new File(inchiBinary).exists();
			//Process proc = 
			String cmd = inchiBinary + " " + 
			tmpMolFile.getAbsolutePath() + " " + tmpInChIFile.getAbsolutePath();
			if(key) {
				if(inchiBinary.endsWith(".exe")) {
					cmd += " /Key";
				} else {
					cmd += " -Key";					
				}
			}
			// Fixed hydrogens
			if(!stdInChI) {
				if(inchiBinary.endsWith(".exe")) {
					cmd += " /FixedH";
				} else {
					cmd += " -FixedH";					
				}				
			}
			
			//System.out.println(cmd);
			Runtime.getRuntime().exec(cmd);
			//proc.waitFor();
			/* This loop seems to work better under windows than proc.waitFor(), which hangs */
			while(tmpInChIFile.length() == 0);
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(tmpInChIFile), "UTF-8"));
			br.readLine();
			br.readLine();				
			if(key) {
				br.readLine();
				br.readLine();				
			}
			String InChI = br.readLine();
			br.close();
			tmpMolFile.delete();
			tmpInChIFile.delete();
			return InChI;
		} catch (Exception e) {
			return null;	
		} finally {
			lock.unlock();
		}
	}

	/**Places an association between an InChI and a molecule in the cache.
	 * 
	 * @param InChI The InChI string.
	 * @param mol The molecule.
	 */
	public static void cacheInChI(String InChI, IMolecule mol) {
		getInstance().cacheInChIInternal(InChI, mol);		
	}

	private void cacheInChIInternal(String InChI, IMolecule mol) {
		try {
			lock.lock();
			molCache.put(InChI, mol);		
		} catch (Exception e) {
			e.printStackTrace();
			return;
		} finally {
			lock.unlock();
		}
	}
	
	/**Retrieves a molecule from the cache.
	 * 
	 * @param InChI The InChI string to query.
	 * @return The molecule, or null.
	 */
	public static IMolecule getMolFromInChICache(String InChI) {
		return getInstance().getMolFromInChICacheInternal(InChI);
	}
	
	private IMolecule getMolFromInChICacheInternal (String InChI) {
		try {
			lock.lock();		
			if(molCache.containsKey(InChI)) {
				return molCache.get(InChI);
			} else {
				return null;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			lock.unlock();
		}
	}
	
	/**Removes an InChI string (and the corresponding molecule) from the
	 * cache.
	 * 
	 * @param inchi The InChI string to remove.
	 */
	public static void clearInChIFromCache(String inchi) {
		getInstance().clearInChIFromCacheInternal(inchi);
	}
	
	private void clearInChIFromCacheInternal(String inchi) {
		try {
			lock.lock();		
			molCache.remove(inchi);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		} finally {
			lock.unlock();
		}
		
	}
	
	/** Gets a molecule given an InChI. WARNING: the molecule returned is also kept in
	 * a cache. Routines that modify the resulting molecule are advised to make a deep
	 * copy of the molecule.
	 * 
	 * @param InChI The InChI to convert to an IMolecule
	 * @return The resulting molecule, or null.
	 */
	public static IMolecule getMolFromInChI(String InChI) {
		return getInstance().getMolFromInChIInternal(InChI);
	}
	
	private IMolecule getMolFromInChIInternal(String InChI) {
		try {
			lock.lock();
			if(molCache.containsKey(InChI)) {
				return molCache.get(InChI);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			lock.unlock();
		}
		
		if(igf != null) {
			//System.out.println("Use IGF!");
			try {
				lock.lock();
				//getInstance().igf.
				IAtomContainer iac = igf.getInChIToStructure(InChI).getAtomContainer();
				IMolecule mol = new Molecule(iac);
				molCache.put(InChI, mol);

				return mol;
				//return getInstance().igf.getInChIGenerator(mol).getInchi();
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			} finally {
				lock.unlock();
			}
		}
		return getMolFromInChIUsingApplicationInternal(InChI);
	}

	/** Gets a molecule given an InChI, using the InChI binary, never the JNI 
	 * InChI. WARNING: the molecule returned is also kept in a cache. Routines
	 * that modify the resulting molecule are advised to make a deep copy of 
	 * the molecule. 
	 * 
	 * @param InChI The InChI to convert to an IMolecule
	 * @return The resulting molecule, or null.
	 */
	public static IMolecule getMolFromInChIUsingApplication(String InChI) {
		return getInstance().getMolFromInChIUsingApplicationInternal(InChI);
	}
	
	private IMolecule getMolFromInChIUsingApplicationInternal(String InChI) {
		if("none".equals(Oscar3Props.getInstance().InChI)) return null;
		try {
			lock.lock();
			File tmpMolFile = File.createTempFile("m2i", ".mol");
			File tmpInChIAuxFile = File.createTempFile("m2i", ".aux.txt");
			File tmpInChIFile = File.createTempFile("m2i", ".txt");
			//System.out.println(tmpMolFile);
			//System.out.println(tmpInChIAuxFile);
			//System.out.println(tmpInChIFile);
			Writer w = new FileWriter(tmpInChIFile);
			w.write(InChI);
			w.write("\n");
			w.close();

			Process proc;
			
			boolean onWindows = Oscar3Props.getInstance().InChI.endsWith(".exe");
			
			if(onWindows) {
				proc = Runtime.getRuntime().exec(Oscar3Props.getInstance().InChI + " " +
						tmpInChIFile.getAbsolutePath() + " " + tmpInChIAuxFile.getAbsolutePath() +
						" /InChI2Struct ");
			} else {
				proc = Runtime.getRuntime().exec(Oscar3Props.getInstance().InChI + " -InChI2Struct " + 
						tmpInChIFile.getAbsolutePath() + " " + tmpInChIAuxFile.getAbsolutePath());				
			}

			if(onWindows) {
				while(tmpInChIAuxFile.length() == 0);				
			} else {
				proc.waitFor();				
			}
						
			if(Oscar3Props.getInstance().InChI.endsWith(".exe")) {
				proc = Runtime.getRuntime().exec(Oscar3Props.getInstance().InChI + " " +
						tmpInChIAuxFile.getAbsolutePath() + " " + tmpMolFile.getAbsolutePath()
						 + " /OutputSDF ");
			} else {
				proc = Runtime.getRuntime().exec(Oscar3Props.getInstance().InChI + " -OutputSDF " + 
						tmpInChIAuxFile.getAbsolutePath() + " " + tmpMolFile.getAbsolutePath());
			}

			if(onWindows) {
				while(tmpMolFile.length() == 0);				
			} else {
				proc.waitFor();				
			}
			
			//proc.waitFor();
			/* This loop seems to work better under windows than proc.waitFor(), which hangs */
			
			//System.out.println(tmpMolFile.getAbsolutePath());
			
			IMolecule mol = (IMolecule)new MDLReader(new InputStreamReader(new FileInputStream(tmpMolFile), "UTF-8")).read(new Molecule());
			
			//System.out.println(FileTools.readTextFile(tmpMolFile));
			
			tmpMolFile.delete();
			tmpInChIAuxFile.delete();
			tmpInChIFile.delete();
			
			molCache.put(InChI, mol);
			
			return mol;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			lock.unlock();
		}
	}	

}
