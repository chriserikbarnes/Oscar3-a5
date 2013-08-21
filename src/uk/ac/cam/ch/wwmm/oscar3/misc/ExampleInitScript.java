package uk.ac.cam.ch.wwmm.oscar3.misc;

import java.io.PrintWriter;
import java.util.List;

import uk.ac.cam.ch.wwmm.oscar3.flow.FlowCommand;
import uk.ac.cam.ch.wwmm.oscar3.flow.FlowRunner;
import uk.ac.cam.ch.wwmm.oscar3.flow.OscarFlow;

/**An example of the InitScript class. This example prints a message, and
 * adds the command "helloworld" to the OscarFlow language.
 * @author ptc24
 *
 */
public final class ExampleInitScript implements InitScript {

	/**Performs the example on-initialisation tasks.
	 * 
	 */
	public void call() throws Exception {
		System.out.println("Hello init world!");
		FlowRunner.getInstance().addCommand("helloworld",  new FlowCommand() {
			public void call(OscarFlow flow, List<String> args) throws Exception {
				System.out.println("Hello command world!");
				PrintWriter pw = flow.customPrintWriter("hello.txt");
				pw.println("Hello custom output world!");
				pw.close();
			}
		});
	}

}
