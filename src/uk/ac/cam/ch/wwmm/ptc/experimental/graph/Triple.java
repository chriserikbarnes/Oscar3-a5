package uk.ac.cam.ch.wwmm.ptc.experimental.graph;

public class Triple {

	String subject;
	String predictate;
	String object;
	
	public Triple(String subject, String predicate, String object) {
		this.subject = subject;
		this.predictate = predicate;
		this.object = object;
	}
	
	public String getSubject() {
		return subject;
	}
	
	public String getPredictate() {
		return predictate;
	}

	public String getObject() {
		return object;
	}
	
	@Override
	public String toString() {
		return "{Triple:" + subject + "," + predictate + "," + object + "}";
	}
	
	@Override
	public int hashCode() {
		return toString().hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof Triple) {
			return obj.toString().equals(toString());
		} else {
			return false;
		}
	}
	
	
}
