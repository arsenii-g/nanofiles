package es.um.redes.nanoFiles.tcp.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import es.um.redes.nanoFiles.application.NanoFiles;
import es.um.redes.nanoFiles.tcp.message.PeerMessage;
import es.um.redes.nanoFiles.tcp.message.PeerMessageOps;
import es.um.redes.nanoFiles.util.FileInfo;

public class NFServer implements Runnable {

	public static final int PORT = 10000;

	private ServerSocket serverSocket = null;

	public NFServer() throws IOException {
		InetSocketAddress socketAddress = new InetSocketAddress(0);
		try {
			serverSocket = new ServerSocket();
			serverSocket.bind(socketAddress);
			System.out.println("Servidor creado en el puerto : " + serverSocket.getLocalPort());
		} catch (IOException ex) {
			System.out.println("Server exception: " + ex.getMessage());
			ex.printStackTrace();
		}
	}

	public void test() {
		if (serverSocket == null || !serverSocket.isBound()) {
			System.err.println(
					"[fileServerTestMode] Failed to run file server, server socket is null or not bound to any port");
			return;
		} else {
			System.out
					.println("[fileServerTestMode] NFServer running on " + serverSocket.getLocalSocketAddress() + ".");
		}

		while (true) {
			try {
				Socket socket = serverSocket.accept();
				System.out.println("\nNew client connected: " +
						socket.getInetAddress().toString() + ":" + socket.getPort());
				serveFilesToClient(socket);

			} catch (IOException ex) {
				System.out.println("Server exception: " + ex.getMessage());
				ex.printStackTrace();
				break;
			}
		}
	}

	public void run() {
		if (serverSocket == null || !serverSocket.isBound()) {
			System.err.println(
					"Failed to run file server, server socket is null or not bound to any port");
			return;
		} else {
			System.out
					.println("NFServer running on " + serverSocket.getLocalSocketAddress() + ".");
		}

		while (true) {
			try {
				Socket socket = serverSocket.accept();
				System.out.println("\nNew client connected: " +
						socket.getInetAddress().toString() + ":" + socket.getPort());
				NFServerThread nfTh = new NFServerThread(socket);
				nfTh.start();
			} catch (IOException ex) {
				System.out.println("Server exception: " + ex.getMessage());
				break;
			}
		}
	}

	public boolean isServing() {
		return !serverSocket.isClosed();
	}

	public void stopServer() {
		if(serverSocket !=null && !serverSocket.isClosed()) {
			try {

				serverSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public int getPort() {
		if(serverSocket != null) {
			return serverSocket.getLocalPort();
		}
		return -1;
	}

	public static void serveFilesToClient(Socket socket) {
		try {
			int cuenta = 0;
			boolean end = false;

			DataInputStream dis = new DataInputStream(socket.getInputStream());
			DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

			do {

			PeerMessage peerCliente = PeerMessage.readMessageFromInputStream(dis);

			byte opCodeRequest = peerCliente.getOpcode();
			if(opCodeRequest == PeerMessageOps.OPCODE_DOWNLOAD_CHUNK) {

				FileInfo[] filesAvailable= NanoFiles.db.getFiles();
				FileInfo[] filesSimilar = FileInfo.lookupFilenameSubstring(filesAvailable, peerCliente.getFileName());
				int numFiles = filesSimilar.length;
				if(numFiles != 0) {
					FileInfo fileSelect = filesSimilar[cuenta%numFiles];
					cuenta++;
					String filePath = NanoFiles.db.lookupFilePath(fileSelect.fileHash);

					RandomAccessFile archivo = new RandomAccessFile(filePath, "r");
					int posChunk = peerCliente.getPosChunk();
					int sizeRead = peerCliente.getSizeChunk();
					archivo.seek(posChunk);
					int size = (int) fileSelect.fileSize;
					int resto = size-posChunk;
					int div = resto/sizeRead;
					if(div==0) {
						sizeRead = resto;
					}
					byte[] bytes = new byte[sizeRead];
					archivo.readFully(bytes);
					archivo.close();
					PeerMessage peerServer = new PeerMessage(PeerMessageOps.OPCODE_FILE_CHUNK, (short) bytes.length, bytes);

					peerServer.writeMessageToOutputStream(dos);
					}
					else{
						PeerMessage peerServer = new PeerMessage(PeerMessageOps.OPCODE_FILE_NOT_FOUND);
						peerServer.writeMessageToOutputStream(dos);
					}
				}
				else if( opCodeRequest == PeerMessageOps.OPCODE_DOWNLOAD_END){
					end = true;
					System.out.println("Se ha terminado la descarga del cliente:"+ socket.getInetAddress().toString() + ":" + socket.getPort());
				}
			}
			while(!end);

		} catch (IOException ex) {
			System.out.println("Server exception: " + ex.getMessage());
			ex.printStackTrace();
		}
	}
}