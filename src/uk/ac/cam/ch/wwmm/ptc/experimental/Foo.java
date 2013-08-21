package uk.ac.cam.ch.wwmm.ptc.experimental;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import uk.ac.cam.ch.wwmm.oscar3.Oscar3Props;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.StringSource;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.Token;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.TokenSequence;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.TokenSequenceSource;
import uk.ac.cam.ch.wwmm.ptclib.datastruct.Bag;
import uk.ac.cam.ch.wwmm.ptclib.io.FileTools;

public class Foo {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		Properties props = new Properties();
		props.setProperty("neThreshold", "0.01");
		Oscar3Props.initialiseWithProperties(props);
		System.out.println(Oscar3Props.getInstance().neThreshold);
	}

}
