package uk.ac.cam.ch.wwmm.ptc.experimental;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.smiles.SmilesParser;

public class TimeoutSmilesParser {

	private static TimeoutSmilesParser myInstance;
	private SmilesParser smilesParser;
    
	// Stuff for timeout implemention
	// Gakked from http://mrfeinberg.com/blog/archives/000016.html
	private static final ExecutorService THREADPOOL = Executors.newCachedThreadPool();

	private static <T> T call(Callable<T> c, long timeout, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException
	{
		FutureTask<T> t = new FutureTask<T>(c);
		THREADPOOL.execute(t);
		return t.get(timeout, timeUnit);
	}
	
	private static TimeoutSmilesParser getInstance() {
		if(myInstance == null) myInstance = new TimeoutSmilesParser();
		return myInstance;
	}
	
	private TimeoutSmilesParser() {
		smilesParser = new SmilesParser(DefaultChemObjectBuilder.getInstance());
	}
	
	public static IMolecule parseSmiles(String smiles) {
		return parseSmiles(smiles, 0.5);
	}
	
	public static IMolecule parseSmiles(String smiles, double timeout) {
		long timeMicros = (long)(timeout * 1000000);
		return getInstance().parseSmilesInternal(smiles, timeMicros);
	}
	
	private IMolecule parseSmilesInternal(String smiles, long timeout) {
		final String finalSmiles = smiles;
		try {
			IMolecule mol = call (new Callable<IMolecule>() {
				public IMolecule call() throws Exception {
					return smilesParser.parseSmiles(finalSmiles);
				}
			}, timeout, TimeUnit.MICROSECONDS);
			return mol;
		} catch (TimeoutException e) {
			System.out.println("Timeout: " + smiles);
			return null;
		} catch (Exception e) {
			return null;
		}
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		SmilesGenerator sg = new SmilesGenerator();
		System.out.println(sg.createSMILES(parseSmiles("CC(=O)C")));
		System.out.println(sg.createSMILES(parseSmiles("C1=CSC(=C1)C(=O)C2=C(C(=C(C=C2)OCC(=O)O)Cl)Cl")));
		System.out.println(sg.createSMILES(parseSmiles("CCC1(C(=O)NC(=O)NC1=O)c2ccccc2")));
		System.out.println(sg.createSMILES(parseSmiles("CC(=C/CO)\\C=C\\C=C(C)\\C=C\\C1=C(C)CCCC1(C)C")));
		System.out.println(sg.createSMILES(parseSmiles("ClCCN(CCCl)P1(=O)NCCCO1")));
		System.out.println(sg.createSMILES(parseSmiles("Cc1ccc2cc3c(ccc4ccccc34)c5CCc1c25")));
		System.out.println(sg.createSMILES(parseSmiles("Cc1ccc2cc3c(ccc4ccccc34)c5CCc1c25")));
		System.out.println(sg.createSMILES(parseSmiles("c1ccc2c(c1)cc3ccc4cccc5ccc2c3c45")));
		System.out.println(sg.createSMILES(parseSmiles("c1ccc2cc3c(ccc4ccccc34)cc2c1")));
		System.out.println(sg.createSMILES(parseSmiles("[H][C@]12CN3C4=C([C@@H](COC(N)=O)[C@@]3(OC)[C@@]1([H])N2)C(=O)C(N)=C(C)C4=O")));
		System.out.println(sg.createSMILES(parseSmiles("Cc1ccc2cc3c(ccc4ccccc34)c5CCc1c25")));
		//System.out.println(sg.createSMILES(parseSmiles("c1ccc-2c(c1)-c3cccc4c3c-2cc5ccccc45")));
		//System.out.println(sg.createSMILES(parseSmiles("n1ccnc1C", 0.009)));
		/*System.out.println(sg.createSMILES(parseSmiles("")));
		System.out.println(sg.createSMILES(parseSmiles("")));
		System.out.println(sg.createSMILES(parseSmiles("")));
		System.out.println(sg.createSMILES(parseSmiles("")));
		System.out.println(sg.createSMILES(parseSmiles("")));
		System.out.println(sg.createSMILES(parseSmiles("")));
		System.out.println(sg.createSMILES(parseSmiles("")));
		System.out.println(sg.createSMILES(parseSmiles("")));
		System.out.println(sg.createSMILES(parseSmiles("")));
		System.out.println(sg.createSMILES(parseSmiles("")));
		System.out.println(sg.createSMILES(parseSmiles("")));*/
		
	}

}
