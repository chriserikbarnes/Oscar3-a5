package uk.ac.cam.ch.wwmm.opsin;

import java.util.ArrayList;
import java.util.HashMap;

import nu.xom.Element;

/**
 * Used to pass the current IDManager, FragmentManager and wordRule around as well as a mapping between the XML and fragments
 *
 * Also included is the first element which has multiple outIDs as detected by the preStructureBuilder in a particular word.
 * Usually no groups have multiple outIDs so this hash will have entries
 * If it does have entries this indicates that either the structure is a radical or that it is an example of multiplicative nomenclature
 * Multiplicative nomenclature is typically resolved left to right rather than right to left so from that element onwards in that word
 * the name will be resolved left to right in the structureBuilder. Once it reaches the end of the word it will resolve anything before
 * the detected multi radical in the conventional right to left manner.
 * @author dl387
 *
 */
public class BuildState {
	IDManager idManager;
	FragmentManager fragManager;
	String wordRule;
	HashMap<Element, Fragment> xmlFragmentMap;
	HashMap<Element, ArrayList<Fragment>> xmlSuffixMap;
	HashMap<Element, Element> firstMultiRadical;//hash of word element against substituent/root element with multi radical

	BuildState(SSMILESFragmentBuilder ssBuilder, CMLFragmentBuilder cmlBuilder) {
		idManager = new IDManager();
		fragManager = new FragmentManager(ssBuilder, cmlBuilder, idManager);
		wordRule =null;
		xmlFragmentMap = new HashMap<Element, Fragment>();
		xmlSuffixMap = new HashMap<Element, ArrayList<Fragment>>();
		firstMultiRadical = new HashMap<Element, Element>();
	}
}
