package uk.ac.cam.ch.wwmm.ptc.experimental.indexersearcher;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.ac.cam.ch.wwmm.oscar3.Oscar3;
import uk.ac.cam.ch.wwmm.oscar3.Oscar3Props;

import Jama.Matrix;

public class SVDHarness {

	private List<Map<Integer,Double>> matrix;
	private int nonzero;
	private int columns;
	private int rows;
	
	private double [] s;
	private Matrix Ut;
	private Matrix Vt;
	
	public SVDHarness(int rows, int columns) {
		this.columns = columns;
		this.rows = rows;
		matrix = new ArrayList<Map<Integer,Double>>();
		for(int i=0;i<columns;i++) {
			matrix.add(new HashMap<Integer,Double>());
		}
		nonzero = 0;
	}
	
	public void set(int column, int row, double value) {
		if(column >= columns || row >= rows) {
			throw new Error();
		}
		Map<Integer,Double> c = matrix.get(column);
		if(value != 0.0) {
			if(!c.containsKey(row)) nonzero++;
			c.put(row, value);			
		} else if(c.containsKey(row)) {
			c.remove(row);
			nonzero--;
		}
	}
	
	public double get(int column, int row) {
		Map<Integer,Double> c = matrix.get(column);
		if(!c.containsKey(row)) {
			return 0.0;
		} else {
			return c.get(row);
		}
	}
	
	public void printDense(PrintWriter pw) {
		pw.println(rows + " " + columns);
		for(int i=0;i<rows;i++) {
			for(int j=0;j<columns;j++) {
				if(j > 0) pw.print(" ");
				pw.print(get(j, i));
			}
			pw.println();
		}
		pw.flush();
	}
	
	// http://tedlab.mit.edu/~dr/SVDLIBC/SVD_F_ST.html
	public void printSparse(PrintWriter pw) {
		pw.println(rows + " " + columns + " " + nonzero);
		for(int i=0;i<columns;i++) {
			Map<Integer,Double> c = matrix.get(i);
			pw.println(c.size());
			for(Integer row : c.keySet()) {
				pw.println(row + " " + c.get(row));
			}
		}
		pw.flush();
	}
	
	private Matrix getMatrix(BufferedReader br) throws Exception {
		String line = br.readLine();
		String [] params = line.split("\\s+");
		int mrows = Integer.parseInt(params[0]);
		int mcols = Integer.parseInt(params[1]);
		Matrix m = new Matrix(mrows, mcols);
		line = br.readLine();
		for(int i=0;i<mrows;i++) {
			String [] vals = line.split("\\s+");
			for(int j=0;j<vals.length;j++) {
				m.set(i, j, Double.parseDouble(vals[j]));
			}
			line = br.readLine();
		}
		assert(line == null);
		return m;
	}

	public void svd() throws Exception {
		svd(0);
	}
	
	public void svd(int dimensions) throws Exception {
		File f = File.createTempFile("sparse", "matrix");
		f.deleteOnExit();
		PrintWriter pw = new PrintWriter(new FileWriter(f));
		printSparse(pw);
		pw.close();
		String cmd = Oscar3Props.getInstance().svdlibc;
		//String cmd = "C:\\SVDLIBC\\svd.exe";
		//String cmd = "/home/ptc24/apps/SVDLIBC/linux/svd";
		//String cmd = "/local/scratch/ptc24/bin/SVDLIBC/linux/svd";
		if(dimensions > 0) cmd += " -d " + dimensions;
		cmd += " -o " + f.getAbsolutePath();
		cmd += " " + f.getAbsolutePath();
		System.out.println("Matrix marshalled OK...");
		Process proc = Runtime.getRuntime().exec(cmd);
		BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
		String line = br.readLine();
		while(line != null) {
			System.out.println(line);
			line = br.readLine();
		}
		File sFile = new File(f.getAbsolutePath() + "-S");
		File utFile = new File(f.getAbsolutePath() + "-Ut");
		File vtFile = new File(f.getAbsolutePath() + "-Vt");
		if(sFile.exists() && utFile.exists() && vtFile.exists()) {
			sFile.deleteOnExit();
			utFile.deleteOnExit();
			vtFile.deleteOnExit();
			br = new BufferedReader(new FileReader(sFile));
			line = br.readLine();
			int svals = Integer.parseInt(line);
			s = new double[svals]; 
			line = br.readLine();
			for(int i=0;i<svals;i++) {
				s[i] = Double.parseDouble(line);
				line = br.readLine();
			}
			assert(line == null);
			Ut = getMatrix(new BufferedReader(new FileReader(utFile)));
			Vt = getMatrix(new BufferedReader(new FileReader(vtFile)));
		}
	}
	
	public double[] getS() {
		return s;
	}
	
	public Matrix getUt() {
		return Ut;
	}
	
	public Matrix getVt() {
		return Vt;
	}
	
	public static void main(String[] args) throws Exception {
		SVDHarness h = new SVDHarness(4, 3);
		h.set(1, 2, 1.1);		
		h.set(0, 2, 1.1);
		h.set(0, 3, -1.4);
		h.set(2, 1, 1.1);
		h.set(2, 0, 1.1);
		h.printSparse(new PrintWriter(System.out));
				
		h.svd(2);
		h.Ut.print(7,5);
		h.Vt.print(7,5);
		
		double [] s = h.getS();
		for(int i=0;i<s.length;i++) {
			System.out.println(s[i]);
		}
	}
	
}
