package uk.ac.cam.ch.wwmm.ptclib.test;

import static junit.framework.Assert.assertEquals;

import org.junit.Test;

import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;

public class RegexToolsTest {

	@Test
	public void testExpandRegex() {
		assertEquals("[]", StringTools.expandRegex(null).toString());
		assertEquals("[]", StringTools.expandRegex("").toString());
		assertEquals("[12345]", StringTools.expandRegex("12345").toString());
		assertEquals("[12345]", StringTools.expandRegex("(12345)?").toString());
		assertEquals("[1267, 1234567]", StringTools.expandRegex("12(345)?67").toString());
		assertEquals("[12 67 , 12 345 67 ]", StringTools.expandRegex("12 (345 )?67 ").toString());
		assertEquals("[34, 12, 1234]", StringTools.expandRegex("(12)?(34)?").toString());
		assertEquals("[34, 3456, 1234, 123456]", StringTools.expandRegex("(12)?34(56)?").toString());
		assertEquals("[034, 03456, 01234, 0123456]", StringTools.expandRegex("0(12)?34(56)?").toString());
		assertEquals("[347, 34567, 12347, 1234567]", StringTools.expandRegex("(12)?34(56)?7").toString());
		assertEquals("[127, 12347, 1234567]", StringTools.expandRegex("12(34(56)?)?7").toString());		
		assertEquals("[127, 12567, 1234567]", StringTools.expandRegex("12((34)?56)?7").toString());		
		assertEquals("[127, 1278, 12567, 125678, 1234567, 12345678]", StringTools.expandRegex("12((34)?56)?7(8)?").toString());		
		assertEquals("[12(34)+56]", StringTools.expandRegex("12(34)+56").toString());
		assertEquals("[12(34)*56]", StringTools.expandRegex("12(34)*56").toString());
	}
	
}
