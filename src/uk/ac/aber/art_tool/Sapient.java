package uk.ac.aber.art_tool;

import uk.ac.cam.ch.wwmm.oscar3.Oscar3Props;
import uk.ac.cam.ch.wwmm.ptclib.io.IOTools;

/** The main class, call the main method on this to parse papers, start the server
 * or perform other tasks.
 * 
 * @author Maria Liakata
 * @author ptc24
 *
 */
public class Sapient {
						
	/**
	 * Grand unified main method - call this!
	 * 
	 * @param args Command-line arguments.
	 */
	public static void main(String[] args) throws Exception {
		if(args.length == 0) {
			return;
		}

		String mode = args[0];
		long mem = Runtime.getRuntime().maxMemory();
		mem /= (1024 * 1024);
		//System.out.println(mem);
		// Running Xmx512 on my AMD64 box causes only 455M to be reported. how strange.
		if(mem < 455 && !"ViewerServer".equals(mode)) {
			System.out.println("WARNING: Sapient needs a large amount of memory to run");
			System.out.println("Try running with the command-line option -Xmx512m");
			//return;
		}
		
		// Try loading the properties, you might want to configure.
		Oscar3Props.getInstance();
		
		if(mode.equals("Server")) {
			if(Oscar3Props.getInstance().serverType.equals("none")) {
				System.out.println("Currently there is no server configured for Sapient");
				System.out.println("Would you like me to configure it now (yes/no)?");
				if(IOTools.askYN()) {
					Oscar3Props.configureServer();
				} else {
					System.out.println("OK, quitting.");
					return;					
				}
			}
			SapientServer.launchServer();
			return;
		} else if(mode.equals("ReConfigureServer")) {
			Oscar3Props.configureServer();
			return;
		} else if(mode.equals("ConfigureWorkspace")) {
			Oscar3Props.configureWorkspace();
			return;
		}	
	}
}
