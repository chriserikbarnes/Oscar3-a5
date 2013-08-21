package uk.ac.cam.ch.wwmm.oscar3.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import uk.ac.cam.ch.wwmm.oscar3.chemnamedict.ChemNameDictTest;

@RunWith(Suite.class)
@SuiteClasses({uk.ac.cam.ch.wwmm.ptclib.test.AllTests.class,
	uk.ac.cam.ch.wwmm.opsin.AllTests.class,
	ChemNameDictTest.class
})
public class QuickTests {

}
