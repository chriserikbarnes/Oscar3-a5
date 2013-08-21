package uk.ac.cam.ch.wwmm.ptclib.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import uk.ac.cam.ch.wwmm.ptc.experimental.StringListAlignmentTest;

@RunWith(Suite.class)
@SuiteClasses({StringToolsTest.class,
	RegexToolsTest.class, 
	SimpleHydrogenAdderTest.class,
	StandoffTableTest.class,
	XMLInserterTest.class,
	XOMFormatterTest.class
	})
public class AllTests {

}
