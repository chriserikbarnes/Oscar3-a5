package uk.ac.cam.ch.wwmm.ptclib.scixml;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nu.xom.Builder;
import nu.xom.Document;
import uk.ac.cam.ch.wwmm.ptclib.xml.StandoffTable;

public class ValueCharPoint2XMLCharPoint {

	public static Map<Integer,Integer> valueCharPoint2XMLCharPoint(File f) throws Exception {
		Map<String,Integer> pm = Xpoint2Charpoint.parseFile(f);
		Document doc = new Builder().build(f);
		StandoffTable st = new StandoffTable(doc.getRootElement());
		
		int size = st.getSize();
		Map<Integer,Integer> m = new HashMap<Integer,Integer>();
		for(int i=0;i<=size;i++) {
			if(i < size) {
				int cpoint = pm.get(st.getLeftPointAtOffset(i));
				if(m.containsKey(cpoint) && m.get(cpoint) != i) System.out.println(i + "\t" + cpoint + "\t" + m.get(cpoint));
				m.put(cpoint, i);
			}
			if(i > 0) {
				int cpoint = pm.get(st.getRightPointAtOffset(i));
				if(m.containsKey(cpoint) && m.get(cpoint) != i) System.out.println(i + "\t" + cpoint + "\t" + m.get(cpoint));
				m.put(cpoint, i);
			}
		}
		return m;
	}
	
	public static void main(String[] args) throws Exception {
		long time = System.currentTimeMillis();
		Map<Integer,Integer> m = valueCharPoint2XMLCharPoint(new File("/home/ptc24/newows/corpora/big_oscarresults/b200198e/source.xml"));
		System.out.println(System.currentTimeMillis() - time);
		List<Integer> l = new ArrayList<Integer>(m.keySet());
		Collections.sort(l);
		for(Integer i : l) {
			System.out.println(i + "\t" + m.get(i));
		}
	}
	
}
