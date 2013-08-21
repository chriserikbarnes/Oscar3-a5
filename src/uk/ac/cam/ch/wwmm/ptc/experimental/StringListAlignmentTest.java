package uk.ac.cam.ch.wwmm.ptc.experimental;

import java.util.List;

import org.junit.Test;

import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;

import static junit.framework.Assert.*;

public class StringListAlignmentTest {

	@Test
	public void testStringListAlignment1() {
		List<String> s1 = StringTools.arrayToList("Hello world".split(" "));
		List<String> s2 = StringTools.arrayToList("Hello world".split(" "));

		StringListAlignment sla = new StringListAlignment(s1, s2);
		assertEquals("[[0], [1]]", sla.getAligned1().toString());
		assertEquals("[[0], [1]]", sla.getAligned2().toString());
	}

	@Test
	public void testStringListAlignment2() {
		List<String> s1 = StringTools.arrayToList("Hello insertion world".split(" "));
		List<String> s2 = StringTools.arrayToList("Hello world".split(" "));

		StringListAlignment sla = new StringListAlignment(s1, s2);
		assertEquals("[[0], [2]]", sla.getAligned1().toString());
		assertEquals("[[0], [1]]", sla.getAligned2().toString());
	}

	@Test
	public void testStringListAlignment3() {
		List<String> s1 = StringTools.arrayToList("Hello world".split(" "));
		List<String> s2 = StringTools.arrayToList("Hello insertion world".split(" "));

		StringListAlignment sla = new StringListAlignment(s1, s2);
		assertEquals("[[0], [1]]", sla.getAligned1().toString());
		assertEquals("[[0], [2]]", sla.getAligned2().toString());
	}

	@Test
	public void testStringListAlignment4() {
		List<String> s1 = StringTools.arrayToList("Hello  world".split(" "));
		List<String> s2 = StringTools.arrayToList("Hello world".split(" "));

		StringListAlignment sla = new StringListAlignment(s1, s2);
		assertEquals("[[0], [2]]", sla.getAligned1().toString());
		assertEquals("[[0], [1]]", sla.getAligned2().toString());
	}

	@Test
	public void testStringListAlignment5() {
		List<String> s1 = StringTools.arrayToList("Hello world".split(" "));
		List<String> s2 = StringTools.arrayToList("Hello  world".split(" "));

		StringListAlignment sla = new StringListAlignment(s1, s2);
		assertEquals("[[0], [1]]", sla.getAligned1().toString());
		assertEquals("[[0], [2]]", sla.getAligned2().toString());
	}
	
	@Test
	public void testStringListAlignment6() {
		List<String> s1 = StringTools.arrayToList("Hello foo bar baz and abc world".split(" "));
		List<String> s2 = StringTools.arrayToList("Hello foobarbaz and a b c world".split(" "));

		StringListAlignment sla = new StringListAlignment(s1, s2);
		assertEquals("[[0], [1, 2, 3], [4], [5], [6]]", sla.getAligned1().toString());
		assertEquals("[[0], [1], [2], [3, 4, 5], [6]]", sla.getAligned2().toString());
	}

	// We'll need to optimise our algorithms to make this work, but in the time being
	/*public void testStringListAlignment7() {
		List<String> s1 = StringTools.arrayToList("Hello foo bar baz abc world".split(" "));
		List<String> s2 = StringTools.arrayToList("Hello foobarbaz and a b c world".split(" "));

		StringListAlignment sla = new StringListAlignment(s1, s2);
		assertEquals("[[0], [1, 2, 3], [4], [5]]", sla.getAligned1().toString());
		assertEquals("[[0], [1], [3, 4, 5], [6]]", sla.getAligned2().toString());
	}*/

}
