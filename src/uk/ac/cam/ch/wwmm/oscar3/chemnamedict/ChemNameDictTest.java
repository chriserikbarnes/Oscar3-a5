package uk.ac.cam.ch.wwmm.oscar3.chemnamedict;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import org.junit.Test;

public class ChemNameDictTest {
	
	@Test
	public void testStopWords() throws Exception {
		ChemNameDict ncnd = new ChemNameDict();
		ncnd.addStopWord("stop");
		assertTrue(ncnd.hasStopWord("stop"));
		assertTrue(ncnd.hasStopWord("Stop"));
		assertFalse(ncnd.hasStopWord("word"));
		ncnd.addStopWord("Word");
		assertTrue(ncnd.hasStopWord("word"));
		ncnd.addStopWord("FOOBAR");
		assertTrue(ncnd.hasStopWord("FOOBAR"));
		assertFalse(ncnd.hasStopWord("FooBar"));
		ncnd.addStopWord("Foo bar");
		assertTrue(ncnd.hasStopWord("foo       Bar"));
		assertTrue(ncnd.hasStopWord("foo\nbar"));
		assertTrue(ncnd.hasStopWord("foo\tbar"));
		assertFalse(ncnd.hasStopWord("foo"));
		assertFalse(ncnd.hasStopWord("bar"));
		assertFalse(ncnd.hasStopWord("foobar"));
		ncnd.addStopWord("fish          and\tchips");
		assertTrue(ncnd.hasStopWord("Fish and Chips"));
		assertEquals(ncnd.getStopWords().size(), 5);
		ncnd.addStopWord("fish and chips");
		assertEquals(ncnd.getStopWords().size(), 5);
		ncnd.addStopWord("stops");
		assertEquals(ncnd.getStopWords().size(), 6);
		ncnd.addName("stops");
		assertTrue(ncnd.hasStopWord("stops"));
		assertEquals(ncnd.getStopWords().size(), 6);
		ncnd.addChemical("stop", "INCHI", "SMILES");
		assertTrue(ncnd.hasStopWord("stop"));
		assertEquals(ncnd.getStopWords().size(), 6);
		try { 
			ncnd.addStopWord(null);
			fail();
		} catch (Exception e) { 
			
		}
		try { 
			ncnd.addStopWord("");
			fail();
		} catch (Exception e) { 
			
		}	
	}
	
	@Test
	public void testOrphanNames() throws Exception {
		ChemNameDict ncnd = new ChemNameDict();
		ncnd.addName("name");
		assertTrue(ncnd.hasName("name"));
		assertTrue(ncnd.hasName("Name"));
		assertFalse(ncnd.hasName("word"));
		ncnd.addName("Word");
		assertTrue(ncnd.hasName("word"));
		ncnd.addName("FOOBAR");
		assertTrue(ncnd.hasName("FOOBAR"));
		assertFalse(ncnd.hasName("FooBar"));
		ncnd.addName("Foo bar");
		assertTrue(ncnd.hasName("foo       Bar"));
		assertTrue(ncnd.hasName("foo\nbar"));
		assertTrue(ncnd.hasName("foo\tbar"));
		assertFalse(ncnd.hasName("foo"));
		assertFalse(ncnd.hasName("bar"));
		assertFalse(ncnd.hasName("foobar"));
		ncnd.addName("fish          and\tchips");
		assertTrue(ncnd.hasName("Fish and Chips"));
		assertEquals(ncnd.getNames().size(), 5);
		ncnd.addName("fish and chips");
		assertEquals(ncnd.getNames().size(), 5);
		ncnd.addName("names");
		assertEquals(ncnd.getNames().size(), 6);
		ncnd.addStopWord("names");
		assertTrue(ncnd.hasName("names"));
		assertEquals(ncnd.getNames().size(), 6);
		ncnd.addChemical("name", "INCHI", "SMILES");
		assertTrue(ncnd.hasName("name"));
		assertEquals(ncnd.getNames().size(), 6);
		try { 
			ncnd.addName(null);
			fail();
		} catch (Exception e) { 
			
		}
		try { 
			ncnd.addName("");
			fail();
		} catch (Exception e) { 
			
		}
	}
	
}
