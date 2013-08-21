package uk.ac.cam.ch.wwmm.oscar3.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import uk.ac.cam.ch.wwmm.oscar3.test.testcard.TestCardTest;

/** The master JUnit test suite. Currently coverage is really very low.
 * 
 * @author ptc24
 *
 */
@RunWith(Suite.class)
@SuiteClasses({QuickTests.class, TestCardTest.class})
public class AllTests {

}
