package es.um.redes.nanoFiles.application;

import es.um.redes.nanoFiles.logic.NFController;
import es.um.redes.nanoFiles.util.FileDatabase;

public class NanoFiles {

	public static final String DEFAULT_SHARED_DIRNAME = "nf-shared";

	public static final String PROTOCOL_ID = "NANOFILES_V1";
	private static final String DEFAULT_DIRECTORY_HOSTNAME = "localhost";
	public static String sharedDirname = DEFAULT_SHARED_DIRNAME;
	public static FileDatabase db;

	public static boolean testModeUDP = false;

	public static boolean testModeTCP = false;

	public static void main(String[] args) {

		if (args.length > 1) {
			System.out.println("Usage: java -jar NanoFiles.jar [<local_shared_directory>]");
			return;
		} else if (args.length == 1) {

			sharedDirname = args[0];
		}

		db = new FileDatabase(sharedDirname);

		NFController controller = new NFController(DEFAULT_DIRECTORY_HOSTNAME);

		if (testModeUDP) {
			controller.testCommunication();
		} else {

			do {
				controller.readGeneralCommandFromShell();
				controller.processCommand();
			} while (controller.shouldQuit() == false);
			System.out.println("Bye.");
		}
	}
}
