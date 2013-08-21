package uk.ac.cam.ch.wwmm.oscar3.misc;

/**A mechanism for customising the startup of Oscar3. The Oscar3 main class
 * may be configured such that a call() method on a object with this interface
 * will be called on startup. In particular, this may be useful for adding
 * additional commands to the OscarFlow language via the 
 * <tt>FlowRunner.addCommand(String commandName, FlowCommand command)</tt>
 * method. 
 * 
 * @author ptc24
 *
 */
public interface InitScript {

	/**The code to be executed upon Oscar3 startup.
	 * 
	 * @throws Exception
	 */
	public void call() throws Exception;
}
