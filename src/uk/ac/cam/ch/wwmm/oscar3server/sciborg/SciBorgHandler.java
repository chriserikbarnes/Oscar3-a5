package uk.ac.cam.ch.wwmm.oscar3server.sciborg;

import nu.xom.Document;

public interface SciBorgHandler {

	public Document handle(String [] path, boolean endsInSlash);
	
}
