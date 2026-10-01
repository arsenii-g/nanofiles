package es.um.redes.nanoFiles.udp.client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import es.um.redes.nanoFiles.application.NanoFiles;
import es.um.redes.nanoFiles.udp.message.DirMessage;
import es.um.redes.nanoFiles.udp.message.DirMessageOps;
import es.um.redes.nanoFiles.util.FileInfo;

public class DirectoryConnector {

	private static final int DIRECTORY_PORT = 6868;

	private static final int TIMEOUT = 1000;

	private static final int MAX_NUMBER_OF_ATTEMPTS = 5;

	private DatagramSocket socket;

	private InetSocketAddress directoryAddress;

	private String directoryHostname;

	public DirectoryConnector(String hostname) throws IOException {

		directoryHostname = hostname;
		InetAddress serverIP = InetAddress.getByName(hostname);
		directoryAddress = new InetSocketAddress(serverIP, DIRECTORY_PORT);
		socket = new DatagramSocket();

	}

	private byte[] sendAndReceiveDatagrams(byte[] requestData) {
		byte responseData[] = new byte[DirMessage.PACKET_MAX_SIZE];
		byte response[] = null;
		if (directoryAddress == null) {
			System.err.println("DirectoryConnector.sendAndReceiveDatagrams: UDP server destination address is null!");
			System.err.println(
					"DirectoryConnector.sendAndReceiveDatagrams: make sure constructor initializes field \"directoryAddress\"");
			System.exit(-1);

		}
		if (socket == null) {
			System.err.println("DirectoryConnector.sendAndReceiveDatagrams: UDP socket is null!");
			System.err.println(
					"DirectoryConnector.sendAndReceiveDatagrams: make sure constructor initializes field \"socket\"");
			System.exit(-1);
		}

		DatagramPacket packetToServer = new DatagramPacket(requestData, requestData.length, directoryAddress);
		DatagramPacket packetFromServer = new DatagramPacket(responseData, responseData.length);
		int intentos;
		for(intentos = 0; intentos <= MAX_NUMBER_OF_ATTEMPTS; intentos++) {
			try {
				socket.send(packetToServer);
				socket.setSoTimeout(TIMEOUT);
				socket.receive(packetFromServer);

				intentos += 7;
			} catch (SocketTimeoutException e) {

				System.out.println("Se perdio el paquete, se procede a reenviarlo");
			}
			catch (IOException e) {
				System.err.println("Error while sending/receiving UDP datagram: " + e.getMessage());
			}
		}
		System.out.println(" ");
		if (intentos>6) {
			String messageFromServer = new String(responseData,0, packetFromServer.getLength());
			response = messageFromServer.getBytes();
		}

		if (response != null && response.length == responseData.length) {
			System.err.println("Your response is as large as the datagram reception buffer!!\n"
					+ "You must extract from the buffer only the bytes that belong to the datagram!");
		}
		return response;
	}

	public boolean testSendAndReceive() {
		boolean success = false;

		String messaggeToServer = "ping";
		byte[] dataToServer = messaggeToServer.getBytes();
		byte[] responseFromServer = sendAndReceiveDatagrams(dataToServer);
		String messaggeFromServer = new String(responseFromServer);
		success = messaggeFromServer.equals("pingok");

		return success;
	}

	public String getDirectoryHostname() {
		return directoryHostname;
	}

	public boolean pingDirectoryRaw() {
		boolean success = false;

		String messaggeToServer = "ping&"+NanoFiles.PROTOCOL_ID;
		byte[] dataToServer = messaggeToServer.getBytes();
		byte[] dataFromServer = sendAndReceiveDatagrams(dataToServer);
		String messageFromServer = new String(dataFromServer);
		if(messageFromServer.equals("welcome")) {
			System.out.println("Se ha recibido la respuesta esperada del servidor");
			success = true;
		}
		else {
			System.err.println("No se hea recibido la respuesta esperada del servidor");
		}

		return success;
	}

	public boolean pingDirectory() {
		boolean success = false;

		DirMessage messageToServer = new DirMessage(DirMessageOps.OPERATION_PING);
		messageToServer.setProtocolID(NanoFiles.PROTOCOL_ID);

		String stringToServer = messageToServer.toString();
		byte[] dataToServer = stringToServer.getBytes();

		byte[] dataFromServer = sendAndReceiveDatagrams(dataToServer);
		String stringFromServer = new String(dataFromServer);
		DirMessage messageFromServer = DirMessage.fromString(stringFromServer);

		String operation = messageFromServer.getOperation();
		if (operation.equals("welcome")) {
			System.out.println("Se ha recibido la respuesta esperada del servidor");
			success = true;
		}
		else if (operation.equals("invalid")) {
			System.err.println("No se ha podido realizar la conexión con el servidor");
		}
		else if (operation.equals("denied")) {
			System.err.println("Protcolo ID incorrecto, no se ha podido establecer conexión");
		}
		return success;
	}

	public boolean registerFileServer(int serverPort, FileInfo[] files) {
		boolean success = false;

		List<FileInfo> filesServe = new ArrayList<FileInfo>();
		for(FileInfo f : files){
			filesServe.add(f);
		}
		DirMessage messageToServer = new DirMessage(DirMessageOps.OPERATION_SERVE, filesServe, serverPort);

		String stringToServer = messageToServer.toString();
		byte[] dataToServer = stringToServer.getBytes();

		byte[] dataFromServer = sendAndReceiveDatagrams(dataToServer);
		String stringFromServer = new String(dataFromServer);

		DirMessage messageFromServer = DirMessage.fromString(stringFromServer);
		if(messageFromServer.getOperation().equals(DirMessageOps.SERVE_VALID)) {
			success = true;
		}

		return success;
	}

	public FileInfo[] getFileList() {
		FileInfo[] filelist = new FileInfo[0];

		DirMessage messageToServer = new DirMessage(DirMessageOps.OPERATION_FILE_LIST);

		String stringToServer = messageToServer.toString();
		byte[] dataToServer = stringToServer.getBytes();

		byte[] dataFromServer = sendAndReceiveDatagrams(dataToServer);
		String stringFromServer = new String(dataFromServer);

		DirMessage messageFromServer = DirMessage.fromString(stringFromServer);

		if(messageFromServer.getOperation().equals(DirMessageOps.FILE_LIST_RESPONSE)) {
			Set<FileInfo> lista = messageFromServer.getFileList();
			filelist = new FileInfo[lista.size()];
			int i = 0;
			for(FileInfo serverISA : lista) {
				filelist[i]= serverISA;
				i+=1;
			}
		}
		else if(messageFromServer.getOperation().equals(DirMessageOps.FILE_LIST_EMPTY)) {
			return filelist;
		}

		return filelist;
	}

	public InetSocketAddress[] getServersSharingThisFile(String filenameSubstring) {
		InetSocketAddress[] serversList = new InetSocketAddress[0];

		DirMessage messageToServer = new DirMessage(DirMessageOps.OPERATION_DOWLOAD);
		messageToServer.setFileDownload(filenameSubstring);

		String stringToServer = messageToServer.toString();
		byte[] dataToServer = stringToServer.getBytes();

		byte[] dataFromServer = sendAndReceiveDatagrams(dataToServer);
		String stringFromServer = new String(dataFromServer);

		DirMessage messageFromServer = DirMessage.fromString(stringFromServer);
		if(messageFromServer.getOperation().equals(DirMessageOps.DOWLOAD_VALID)) {
			Set<InetSocketAddress> lista = messageFromServer.getServersTCP();
			serversList = new InetSocketAddress[lista.size()];
			int i = 0;
			for(InetSocketAddress serverISA : lista) {
				serversList[i]= serverISA;
				i+=1;
			}
		}
		else {
			serversList = null;
		}

		return serversList;
	}

	public boolean unregisterFileServer(int port) {
		boolean success = false;

		 DirMessage messageToServer = new DirMessage(DirMessageOps.OPERATION_STOP_SERVE);
		 messageToServer.setServerPort(port);

		String stringToServer = messageToServer.toString();
		byte[] dataToServer = stringToServer.getBytes();

		byte[] dataFromServer = sendAndReceiveDatagrams(dataToServer);
		String stringFromServer = new String(dataFromServer);

		DirMessage messageFromServer = DirMessage.fromString(stringFromServer);

		if(messageFromServer.getOperation().equals(DirMessageOps.STOP_SERVE_VALID)) {
			success = true;
		}

		return success;
	}

	public InetSocketAddress[] getServersSharing() {
		InetSocketAddress[] serversList = new InetSocketAddress[0];

		DirMessage messageToServer = new DirMessage(DirMessageOps.OPERATION_UPLOAD);

		String stringToServer = messageToServer.toString();
		byte[] dataToServer = stringToServer.getBytes();

		byte[] dataFromServer = sendAndReceiveDatagrams(dataToServer);
		String stringFromServer = new String(dataFromServer);

		DirMessage messageFromServer = DirMessage.fromString(stringFromServer);
		Set<InetSocketAddress> lista = messageFromServer.getServersTCP();

		if(!lista.isEmpty()) {
			serversList = new InetSocketAddress[lista.size()];
			int i = 0;
			for(InetSocketAddress serverISA : lista) {
				serversList[i]= serverISA;
				i+=1;
			}
		}
		return serversList;
	}

}
