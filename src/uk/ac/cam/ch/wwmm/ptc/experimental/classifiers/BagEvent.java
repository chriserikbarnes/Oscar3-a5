package uk.ac.cam.ch.wwmm.ptc.experimental.classifiers;

import uk.ac.cam.ch.wwmm.ptclib.datastruct.Bag;

public class BagEvent {

	private String classLabel;
	private Bag<String> features;
	
	public BagEvent(String classLabel, Bag<String> features) {
		this.classLabel = classLabel;
		this.features = features;
	}
	
	public String getClassLabel() {
		return classLabel;
	}
	
	public Bag<String> getFeatures() {
		return features;
	}
}
