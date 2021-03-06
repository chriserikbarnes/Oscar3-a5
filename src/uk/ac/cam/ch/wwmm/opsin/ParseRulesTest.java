package uk.ac.cam.ch.wwmm.opsin;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import uk.ac.cam.ch.wwmm.opsin.ParseRules.TwoReturnValues;


public class ParseRulesTest {

	@Test
	public void testParseRules() throws Exception {
		ParseRules pr = new ParseRules(new TokenManager());
		assertNotNull("Got ParseRules", pr);
		String regex = pr.chemicalRegex.toString();
		assertNotNull("Got regex", regex);
		assertTrue("Regex is a decent size", regex.length() > 10);
		assertFalse("Regex contains no %", regex.contains("%"));

		TwoReturnValues returned = pr.getParses("hexane", "simple");
		List<ParseTokens> parseTokenList =returned.getFirst();
		boolean lexable =returned.getSecond();
		assertEquals("One lex", 1, parseTokenList.size());
		assertEquals("Two tokens", 2, parseTokenList.get(0).tokens.size());
		assertEquals("First token: hex", "hex", parseTokenList.get(0).tokens.get(0));
		assertEquals("Second token: ane", "ane", parseTokenList.get(0).tokens.get(1));
		assertEquals(true, lexable);

		returned = pr.getParses("hexachlorohexane", "simple");
		parseTokenList =returned.getFirst();
		lexable =returned.getSecond();
		assertEquals("Four tokens", 4, parseTokenList.get(0).tokens.size());
		assertEquals("First token: hexa", "hexa", parseTokenList.get(0).tokens.get(0));
		assertEquals("Second token: chloro", "chloro", parseTokenList.get(0).tokens.get(1));
		assertEquals("Third token: hex", "hex", parseTokenList.get(0).tokens.get(2));
		assertEquals("Fourth token: ane", "ane", parseTokenList.get(0).tokens.get(3));
		assertEquals(true, lexable);

		returned = pr.getParses("hexachlorohexaneeeeeee", "simple");
		parseTokenList =returned.getFirst();
		lexable =returned.getSecond();
		assertEquals("No Parses", 0, parseTokenList.size());
		assertEquals(false, lexable);

		returned = pr.getParses("(hexachloro)hexane", "simple");
		parseTokenList =returned.getFirst();
		lexable =returned.getSecond();
		assertEquals("One lex", 1, parseTokenList.size());
		assertEquals("Six tokens", 6, parseTokenList.get(0).tokens.size());
		assertEquals("token", "(", parseTokenList.get(0).tokens.get(0));
		assertEquals("token", "hexa", parseTokenList.get(0).tokens.get(1));
		assertEquals("token", "chloro", parseTokenList.get(0).tokens.get(2));
		assertEquals("token", ")", parseTokenList.get(0).tokens.get(3));
		assertEquals("token", "hex", parseTokenList.get(0).tokens.get(4));
		assertEquals("token", "ane", parseTokenList.get(0).tokens.get(5));
		assertEquals(true, lexable);

		returned = pr.getParses("methyl", "substituent");
		parseTokenList =returned.getFirst();
		lexable =returned.getSecond();
		assertEquals("One lex", 1, parseTokenList.size());
		assertEquals("Two tokens", 2, parseTokenList.get(0).tokens.size());
		assertEquals("token", "meth", parseTokenList.get(0).tokens.get(0));
		assertEquals("token", "yl", parseTokenList.get(0).tokens.get(1));
		assertEquals(true, lexable);

		returned = pr.getParses("methyl", "simple");
		parseTokenList =returned.getFirst();
		lexable =returned.getSecond();
		assertEquals("No Lexes", 0, parseTokenList.size());
		assertEquals(true, lexable);
	}
}
