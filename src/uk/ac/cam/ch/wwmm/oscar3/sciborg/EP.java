package uk.ac.cam.ch.wwmm.oscar3.sciborg;

public class EP {
	String lemma;
	String pos;
	String sense;
	String gpred;
	String carg;
	
	int cfrom;
	int cto;
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof EP) {
			EP ep = (EP)obj;
			return (lemma == null ? ep.lemma == null : lemma.equals(ep.lemma)) &&
			(pos == null ? ep.pos == null : pos.equals(ep.pos)) &&
			(sense == null ? ep.sense == null : sense.equals(ep.sense)) &&
			(gpred == null ? ep.gpred == null : gpred.equals(ep.gpred)) &&
			(carg == null ? ep.carg == null : carg.equals(ep.carg));
		} else {
			return false;
		}
	}
	
	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		return (lemma == null ? 0 : 923521 * lemma.hashCode()) +
		(pos == null ? 0 : 29791 * pos.hashCode()) +
		(sense == null ? 0 : 961 * sense.hashCode()) +
		(gpred == null ? 0 : 31 * gpred.hashCode()) +
		(carg == null ? 0 : carg.hashCode());
	}
	
	@Override
	public String toString() {
		return String.format("ep:[%s, %s, %s, %s, %s, %d, %d]", lemma, pos, sense, gpred, carg, cfrom, cto);
	}
}
