package es.um.redes.nanoFiles.logic;

import java.net.InetSocketAddress;

import es.um.redes.nanoFiles.application.NanoFiles;
import es.um.redes.nanoFiles.shell.NFCommands;
import es.um.redes.nanoFiles.shell.NFShell;
import es.um.redes.nanoFiles.util.FileInfo;

public class NFController {

	private static final byte OFFLINE = 0;

	private static final byte JOINED = 1;

	private static final byte SERVE = 2;

	private NFShell shell;

	private byte currentCommand;

	private NFControllerLogicDir controllerDir;

	private NFControllerLogicP2P controllerPeer;

	private byte currentState;

	private String targetFilenameSubstring;
	private String downloadLocalFileName;
	private String uploadToServer;

	public NFController(String defaultDirectory) {
		shell = new NFShell();

		String directory = shell.chooseDirectory(defaultDirectory);

		controllerDir = new NFControllerLogicDir(directory);
		controllerPeer = new NFControllerLogicP2P(directory);

		currentState = OFFLINE;

	}

	public void testCommunication() {
		assert (NanoFiles.testModeUDP);
		System.out
				.println("[testMode] Attempting to reach directory server at " + controllerDir.getDirectoryHostname());
		controllerDir.testCommunicationWithDirectory();
		System.out.println("[testMode] Test terminated!");
		return;
	}

	public void processCommand() {

		if (!canProcessCommandInCurrentState()) {
			return;
		}

		boolean commandSucceeded = false;
		switch (currentCommand) {
		case NFCommands.COM_MYFILES:
			showMyLocalFiles();
			break;
		case NFCommands.COM_PING:

			commandSucceeded = controllerDir.ping();
			break;
		case NFCommands.COM_FILELIST:

			controllerDir.getAndPrintFileList();
			break;
		case NFCommands.COM_SERVE:

			if (NanoFiles.testModeTCP) {
				controllerPeer.testTCPServer();
			} else {
				boolean serverRunning = controllerPeer.startFileServer();
				if (serverRunning) {
					commandSucceeded = controllerDir.registerFileServer(controllerPeer.getServerPort(),
							NanoFiles.db.getFiles());
				} else {
					System.err.println("Cannot start file server");
				}
			}
			break;
		case NFCommands.COM_DOWNLOAD:

			if (NanoFiles.testModeTCP) {
				controllerPeer.testTCPClient();
			} else {
				InetSocketAddress[] serverAddressList = controllerDir
						.getServerAddressesSharingThisFile(targetFilenameSubstring);
				commandSucceeded = controllerPeer.downloadFileFromServers(serverAddressList, targetFilenameSubstring,
						downloadLocalFileName);
			}
			break;
		case NFCommands.COM_QUIT:

			if (controllerPeer.serving()) {
				controllerPeer.stopFileServer();
				int port = controllerPeer.getServerPort();
				commandSucceeded = controllerDir.unregisterFileServer(port);
			}
			break;
		case NFCommands.COM_UPLOAD:

			FileInfo[] matchingFiles = FileInfo.lookupFilenameSubstring(NanoFiles.db.getFiles(),
					targetFilenameSubstring);
			if (matchingFiles.length == 1) {
				commandSucceeded = controllerPeer.uploadFileToServer(matchingFiles[0], uploadToServer);
			} else if (matchingFiles.length == 0) {
				System.err.println("Cannot locate file to upload! No matching files found");
			} else {
				System.err.println("Ambiguos filename substring! Candidate files are:");
				FileInfo.printToSysout(matchingFiles);
			}
			break;
		case NFCommands.COM_SERVERS:

			controllerDir.getAndPrintServerAddresses();
			break;
		default:
		}

		updateCurrentState(commandSucceeded);
	}

	private boolean canProcessCommandInCurrentState() {

		boolean commandAllowed = true;
		switch (currentCommand) {
		case NFCommands.COM_UPLOAD:
		case NFCommands.COM_SERVERS:
		case NFCommands.COM_FILELIST:
		case NFCommands.COM_DOWNLOAD: {
			if(currentState==OFFLINE) {
				System.out.println("This command required to verify first the protocol ID is the same as the server, excute the command \"ping\" to verify");
				commandAllowed = false;
			}
			break;
		}
		case NFCommands.COM_SERVE:{
			if(currentState==OFFLINE) {
				System.out.println("This command required to verify first the protocol ID is the same as the server, excute the command \"ping\" to verify");
				commandAllowed = false;
			}

			else if(currentState==SERVE){
				commandAllowed = false;
			}
			break;
		}
		case NFCommands.COM_PING:{
			commandAllowed = true;
			break;
		}
		case NFCommands.COM_MYFILES:
		case NFCommands.COM_QUIT:{
			break;
		}

		default:
			 System.err.println("ERROR: undefined behaviour for " + currentCommand + " command!");
		}
		return commandAllowed;
	}

	private void updateCurrentState(boolean success) {

		if (!success) {
			return;
		}
		switch (currentCommand) {
		case NFCommands.COM_PING:
			if(currentState!=SERVE) {
				currentState = JOINED;
			}

			break;
		case NFCommands.COM_SERVE:
			currentState = SERVE;
			break;
		default:
		}

	}

	private void showMyLocalFiles() {
		System.out.println("List of files in local folder:");
		FileInfo.printToSysout(NanoFiles.db.getFiles());
	}

	public boolean shouldQuit() {
		return currentCommand == NFCommands.COM_QUIT;
	}

	private void setCurrentCommand(byte command) {
		currentCommand = command;
	}

	private void setCurrentCommandArguments(String[] args) {
		switch (currentCommand) {
		case NFCommands.COM_DOWNLOAD:
			targetFilenameSubstring = args[0];
			downloadLocalFileName = args[1];
			break;
		case NFCommands.COM_UPLOAD:
			targetFilenameSubstring = args[0];
			uploadToServer = args[1];
			break;
		default:
		}
	}

	public void readGeneralCommandFromShell() {

		shell.readGeneralCommand();

		setCurrentCommand(shell.getCommand());

		setCurrentCommandArguments(shell.getCommandArguments());
	}

}
