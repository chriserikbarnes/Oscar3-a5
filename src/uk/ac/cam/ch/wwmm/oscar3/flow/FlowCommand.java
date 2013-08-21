package uk.ac.cam.ch.wwmm.oscar3.flow;

import java.util.List;

/**A command to be invoked by an OscarFlow.
 * 
 * @author ptc24
 *
 */
public interface FlowCommand {

	/** Perform the action specified by the command.
	 * 
	 * @param flow The OscarFlow calling this command.
	 * @param args The arguments to be passed to the command.
	 * @throws Exception
	 */
	public void call(OscarFlow flow, List<String> args) throws Exception;
	
}
