package es.um.redes.nanoFiles.logic;

import java.io.IOException;
import java.net.InetSocketAddress;

import es.um.redes.nanoFiles.application.NanoFiles;
import es.um.redes.nanoFiles.udp.client.DirectoryConnector;
import es.um.redes.nanoFiles.util.FileInfo;

public class NFControllerLogicDir {

	private DirectoryConnector directoryConnector;

	protected NFControllerLogicDir(String directoryHostname) {

		try {
			directoryConnector = new DirectoryConnector(directoryHostname);
		} catch (IOException e1) {
			System.err.println(
					"* Check your connection, the directory server at " + directoryHostname + " is not available.");
			System.exit(-1);
		}
	}

	protected void testCommunicationWithDirectory() {
		assert (NanoFiles.testModeUDP);
		System.out.println(
				"[testMode] Testing communication with directory: " + this.directoryConnector.getDirectoryHostname());

		if (directoryConnector.testSendAndReceive()) {
			System.out.println("[testMode] testSendAndReceived - TEST PASSED!");

			if (directoryConnector.pingDirectoryRaw()) {
				System.out.println("[testMode] pingDirectoryRaw - SUCCESS!");
			} else {
				System.err.println("[testMode] pingDirectoryRaw - FAILED!");
			}
		} else {
			System.err.println("[testMode] testSendAndReceived - TEST FAILED!");
		}
	}

	protected boolean ping() {
		boolean result = false;
		System.out.println(
				"* Checking if the directory at " + directoryConnector.getDirectoryHostname() + " is available...");
		result = directoryConnector.pingDirectory();
		if (result) {
			System.out.println("* Directory is active and uses compatible protocol " + NanoFiles.PROTOCOL_ID);
		} else {
			System.err.println("* Ping failed");
		}
		return result;
	}

	protected void getAndPrintFileList() {

		FileInfo[] trackedFiles = directoryConnector.getFileList();
		System.out.println(
				"* These are the files tracked by the directory at " + directoryConnector.getDirectoryHostname());
		FileInfo.printToSysout(trackedFiles);
	}

	protected boolean registerFileServer(int serverPort, FileInfo[] filelist) {

		boolean result = false;
		if (this.directoryConnector.registerFileServer(serverPort, filelist)) {
			System.out.println("* File server successfully registered with the directory");
			result = true;
		} else {
			System.err.println("* File server failed to register with the directory");
		}
		return result;
	}

	protected InetSocketAddress[] getServerAddressesSharingThisFile(String filenameSubstring) {

		return directoryConnector.getServersSharingThisFile(filenameSubstring);
	}

	protected boolean unregisterFileServer(int port) {

		boolean result = false;
		if (this.directoryConnector.unregisterFileServer(port)) {
			System.out.println("* File server successfully unregistered with the directory");
			result = true;
		} else {
			System.err.println("* File server failed to unregister with the directory");
		}
		return result;
	}

	protected String getDirectoryHostname() {
		return directoryConnector.getDirectoryHostname();
	}

	protected void getAndPrintServerAddresses() {

		InetSocketAddress[] serversAvailables = directoryConnector.getServersSharing();
		System.out.println("Servers capable of receiving a file via \"Upload\":");
		for(InetSocketAddress server : serversAvailables) {
			System.out.println("Ip: " + server.getAddress() + " Port: " + server.getPort() );
		}
	}

}
