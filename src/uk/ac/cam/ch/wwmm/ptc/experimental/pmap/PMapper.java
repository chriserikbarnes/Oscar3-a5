package uk.ac.cam.ch.wwmm.ptc.experimental.pmap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class PMapper<I,O> {

	public PMapper() {
		
	}

	public List<O> map(final List<I> inputs, final PMapFunction<I,O> f) {
		List<O> out = new ArrayList<O>(inputs.size());
		for(int i=0;i<inputs.size();i++) {
			out.add(f.call(inputs.get(i)));
		}
		return out;
	}
	
	public List<O> pmap(final List<I> inputs, final PMapFunction<I,O> f) {
		int processors = Runtime.getRuntime().availableProcessors();
		ExecutorService threadPool = Executors.newFixedThreadPool(processors);
		List<O> out = new ArrayList<O>(inputs.size());
		final List<O> syncOut = Collections.synchronizedList(out);
		for(int i=0;i<inputs.size();i++) {
			syncOut.add(null);
		}
		for(int i=0;i<inputs.size();i++) {
			final int ii = i;
			Runnable r = new Runnable() {
				public void run() {
					I input = inputs.get(ii);
					O output = f.call(input);
					syncOut.set(ii, output);
				}
			};
			threadPool.execute(r);
		}
		try {
			threadPool.shutdown();
			boolean result = threadPool.awaitTermination(60, TimeUnit.SECONDS);
			if(result) {
				return syncOut;
			} else {
				return null;
			}
		} catch (InterruptedException e) {
			return null;
		}
	}
	
	public static void main(String[] args) {
		List<Integer> ints = new ArrayList<Integer>();
		for(int i=0;i<1000000;i++) {
			ints.add(i);
		}
		final Random r = new Random(0);
		PMapFunction<Integer,String> iToS = new PMapFunction<Integer, String>() {
			public String call(Integer input) {
				double d = 0.0;
				int ii = (int)(10000 * r.nextDouble());
				for(int i=0;i<ii;i++) {
					d += 1.0;
				}
				return "\"" + Integer.toString(input) + "\"";
			}
		};
		long time = System.currentTimeMillis();
		List<String> strs = new PMapper<Integer,String>().map(ints, iToS);
		System.out.println(System.currentTimeMillis() - time);
		System.out.println(strs.subList(0, 10));
	}
	
}
