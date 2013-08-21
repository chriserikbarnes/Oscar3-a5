package uk.ac.cam.ch.wwmm.ptc.experimental;

public class DisjointSetItem<X> {

	private X payload;
	private DisjointSetItem<X> parent;
	private int rank;
	
	private DisjointSetItem() {
		
	}
	
	public DisjointSetItem(X payload) {
		this.payload = payload;
		parent = this;
		rank = 0;
	}
	
	public DisjointSetItem<X> find() {
		if(parent == this) {
			return this;
		} else {
			parent = parent.find();
			return parent;
		}
	}
	
	public void union(DisjointSetItem<X> other) {
		DisjointSetItem<X> thisRoot = find();
		DisjointSetItem<X> otherRoot = other.find();
	    if(thisRoot.rank > otherRoot.rank) {
	    	otherRoot.parent = thisRoot;
	    } else if(thisRoot.rank < otherRoot.rank) {
	    	thisRoot.parent = otherRoot;
	    } else if(thisRoot != otherRoot) {
	    	otherRoot.parent = thisRoot;
	    	thisRoot.rank++;
	    }
	}
	
	public X getPayload() {
		return payload;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
