package es.um.redes.nanoFiles.udp.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import es.um.redes.nanoFiles.application.NanoFiles;
import es.um.redes.nanoFiles.tcp.server.NFServer;
import es.um.redes.nanoFiles.udp.message.DirMessage;
import es.um.redes.nanoFiles.udp.message.DirMessageOps;
import es.um.redes.nanoFiles.util.FileInfo;

public class NFDirectoryServer {

	public static final int DIRECTORY_PORT = 6868;

	private DatagramSocket socket = null;

	private Map<InetSocketAddress,Set<FileInfo>> serversTCP;

	private Set<FileInfo> fileList;

	private double messageDiscardProbability;

	public NFDirectoryServer(double corruptionProbability) throws SocketException {

		messageDiscardProbability = corruptionProbability;
		socket = new DatagramSocket(DIRECTORY_PORT);

		fileList = new HashSet<FileInfo>();
		serversTCP = new HashMap<InetSocketAddress, Set<FileInfo>>();

		if (NanoFiles.testModeUDP) {
			if (socket == null) {
				System.err.println("NFDirectoryServer: UDP socket is not initialized.");
				System.exit(-1);
			}
		}
	}

	public DatagramPacket receiveDatagram() throws IOException {
		DatagramPacket datagramReceivedFromClient = null;
		boolean datagramReceived = false;
		while (!datagramReceived) {

			byte[] recvBuf = new byte[DirMessage.PACKET_MAX_SIZE];

			datagramReceivedFromClient = new DatagramPacket(recvBuf, recvBuf.length);
			socket.receive(datagramReceivedFromClient);

			if (datagramReceivedFromClient == null) {
				System.err.println("NFDirectoryServer: no datagram was received.");
				System.exit(-1);
			} else {

				double rand = Math.random();
				if (rand < messageDiscardProbability) {
					System.err.println(
							"Directory ignored datagram from " + datagramReceivedFromClient.getSocketAddress());
				} else {
					datagramReceived = true;
					System.out
							.println("Directory received datagram from " + datagramReceivedFromClient.getSocketAddress()
									+ " of size " + datagramReceivedFromClient.getLength() + " bytes.");
				}
			}

		}

		return datagramReceivedFromClient;
	}

	public void runTest() throws IOException {

		System.out.println("[testMode] Directory starting...");

		System.out.println("[testMode] Attempting to receive 'ping' message...");
		DatagramPacket rcvDatagram = receiveDatagram();
		sendResponseTestMode(rcvDatagram);

		System.out.println("[testMode] Attempting to receive 'ping&PROTOCOL_ID' message...");
		rcvDatagram = receiveDatagram();
		sendResponseTestMode(rcvDatagram);
	}

	private void sendResponseTestMode(DatagramPacket pkt) throws IOException {

		String messageFromClient = new String(pkt.getData(), 0, pkt.getLength());
		System.out.println("Data received: " + messageFromClient);

		InetSocketAddress clientAddr = (InetSocketAddress) pkt.getSocketAddress();
		String messageToClient = new String();
		if (messageFromClient.equals("ping")) {
			messageToClient = messageFromClient+"ok";
		}
		else if (messageFromClient.contains("ping&")) {
			int posición = messageFromClient.indexOf("&");
			String idProtocol = messageFromClient.substring(posición+1);
			if (idProtocol.equals(NanoFiles.PROTOCOL_ID)) {
				messageToClient = "welcome";
			}
			else {
				messageToClient = "denied";
			}

		}
		else {
			messageToClient = "invalid";
		}

		byte [] dataToClient = messageToClient.getBytes();
		DatagramPacket packetToClient = new DatagramPacket(dataToClient, dataToClient.length, clientAddr);
		socket.send(packetToClient);

	}

	public void run() throws IOException {

		System.out.println("Directory starting...");

		while (true) {
			DatagramPacket rcvDatagram = receiveDatagram();

			sendResponse(rcvDatagram);

		}
	}

	public Set<FileInfo> getFilelist(){
		Set<FileInfo> fileList = new HashSet<FileInfo>();
		Set<String> conjuntoSoporte = new HashSet<String>();
		for(InetSocketAddress i : serversTCP.keySet()) {
			for(FileInfo f : serversTCP.get(i)) {
				if(!conjuntoSoporte.contains(f.fileHash)){
					conjuntoSoporte.add(f.fileHash);
					FileInfo fileInicial = new FileInfo(f.fileHash, f.fileName, f.fileSize, f.filePath);
					fileInicial.servers.add(i);
					fileList.add(fileInicial);
				}
				else {
					for (FileInfo file : fileList) {
						if(file.fileHash.equals(f.fileHash)) {
							file.servers.add(i);
							break;
						}
					}
				}
			}
		}
		return fileList;
	}

	private void sendResponse(DatagramPacket pkt) throws IOException {

		String messageFromClient = new String(pkt.getData(),0,pkt.getLength());
		System.out.println("Data received: \n" + messageFromClient);

		DirMessage dirMessageFromClient = DirMessage.fromString(messageFromClient);

		InetSocketAddress clientAddr = (InetSocketAddress) pkt.getSocketAddress();

		String operation = dirMessageFromClient.getOperation();

		DirMessage dirMessageToClient = null;

		switch (operation) {
		case DirMessageOps.OPERATION_PING: {
			String protocolID = dirMessageFromClient.getProtocolId();
			if(protocolID.equals(NanoFiles.PROTOCOL_ID)) {
				dirMessageToClient = new DirMessage(DirMessageOps.PING_VALID);
				System.out.println("El mensaje \"ping\" recibido del cliente tiene el ID_Protocol adecuado");
			}
			else {
				dirMessageToClient = new DirMessage(DirMessageOps.PING_INVALID);
				System.out.println("El mensaje \"ping\" recibido del cliente tiene un ID_Protocol contrario al esperado");
			}
			break;
		}

		case DirMessageOps.OPERATION_FILE_LIST:{
			fileList = getFilelist();
			if(!fileList.isEmpty()) {
				dirMessageToClient = new DirMessage(DirMessageOps.FILE_LIST_RESPONSE);
				dirMessageToClient.setServetFileList(fileList);
				System.out.println("La lista de ficheros no está vacía, y se le puede enviar al cliente");
			}
			else {
				dirMessageToClient = new DirMessage(DirMessageOps.FILE_LIST_EMPTY);
				System.out.println("La lista de ficheros está vacía");
			}
			break;
		}

		case DirMessageOps.OPERATION_DOWLOAD:{
			String subcadena = dirMessageFromClient.getFileDownload();
			FileInfo[] fileArray = getFilelist().toArray(FileInfo[]::new);
			FileInfo[] files = FileInfo.lookupFilenameSubstring(fileArray,subcadena);

			boolean equals = true;
			int lenFiles = files.length-1;
			for(int i = 0; i < lenFiles; i++) {
				if(!files[i].fileHash.equals(files[i+1].fileHash)) {
					equals = false;
					break;
				}
			}

			if(equals) {
				Set<InetSocketAddress> serversFile = new HashSet<InetSocketAddress>();
				for(FileInfo f : files) {
					for(InetSocketAddress inet : serversTCP.keySet()) {
						Set<FileInfo> ficheros = serversTCP.get(inet);
						for(FileInfo opcion : ficheros) {
							if(opcion.fileHash.equals(files[0].fileHash)) {
								serversFile.add(inet);
								break;
							}
						}
					}
				}
				dirMessageToClient = new DirMessage(DirMessageOps.DOWLOAD_VALID, serversFile);
				System.out.println("La lista de servidores que contienen el fichero se ha enviado");
				break;
			}
			else {
				dirMessageToClient = new DirMessage(DirMessageOps.DOWLOAD_INVALID);
				System.out.println("Los ficheros encontrados por el subnombre tienen diferentes hash entre ellos");
				break;
			}

		}

		case DirMessageOps.OPERATION_SERVE:{
			int port = dirMessageFromClient.getServerPort();
			InetAddress addressClient = pkt.getAddress();
			InetSocketAddress servidorTCP;
			if(addressClient.toString().contains("127.0.0.1")) {
				 servidorTCP = new InetSocketAddress("0.0.0.0", port);
			}
			else {
				 servidorTCP = new InetSocketAddress(addressClient, port);
			}
			if(!serversTCP.containsKey(servidorTCP)) {
					Set<FileInfo> listaFiles = dirMessageFromClient.getFileList();
					serversTCP.put(servidorTCP, listaFiles);
					dirMessageToClient = new DirMessage(DirMessageOps.SERVE_VALID);
					System.out.println("El cliente con dirección "+ clientAddr+ " ha subido sus ficheros y ahora tambien es un serve");
				}
			else {
				dirMessageToClient = new DirMessage(DirMessageOps.SERVE_INVALID);
				System.out.println("El cliente con dirección "+ clientAddr+ " no podido subir sus ficheros");
				}
			break;

		}
		case DirMessageOps.OPERATION_STOP_SERVE:{
			int port = dirMessageFromClient.getServerPort();
			InetAddress addressClient = pkt.getAddress();
			InetSocketAddress servidorTCP;
			if(addressClient.toString().contains("127.0.0.1")) {
				 servidorTCP = new InetSocketAddress("0.0.0.0", port);
			}
			else {
				 servidorTCP = new InetSocketAddress(addressClient, port);
			}
			if(serversTCP.containsKey(servidorTCP)) {
				serversTCP.remove(servidorTCP);
				dirMessageToClient = new DirMessage(DirMessageOps.STOP_SERVE_VALID);
			}
			else {
				dirMessageToClient = new DirMessage(DirMessageOps.STOP_SERVE_INVALID);
			}
			break;
		}
		case  DirMessageOps.OPERATION_UPLOAD:{
			dirMessageToClient = new DirMessage(DirMessageOps.UPLOAD_SERVERS, serversTCP.keySet());
			break;
		}
		default:
			System.err.println("Unexpected message operation: \"" + operation + "\"");
			System.exit(-1);
		}

		String messageToClient = dirMessageToClient.toString();
		byte[] dataToClient = messageToClient.getBytes();

		DatagramPacket packetToClient = new DatagramPacket(dataToClient, dataToClient.length, clientAddr);
		socket.send(packetToClient);
	}
}
