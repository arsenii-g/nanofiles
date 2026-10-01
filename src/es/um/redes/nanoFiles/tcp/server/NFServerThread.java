package es.um.redes.nanoFiles.tcp.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.Socket;

import es.um.redes.nanoFiles.application.NanoFiles;
import es.um.redes.nanoFiles.tcp.message.PeerMessage;
import es.um.redes.nanoFiles.tcp.message.PeerMessageOps;
import es.um.redes.nanoFiles.util.FileInfo;

public class NFServerThread extends Thread {
	private Socket socket;
	public NFServerThread(Socket sockClient) {
		socket = sockClient;
	}

	public void run() {
		try {
			File upload = null;
			FileOutputStream fo = null;
			System.out.println("Hilo con socket: " + socket + " se está ejecutando");
			int cuenta = 0;
			boolean end = false;

			DataInputStream dis = new DataInputStream(socket.getInputStream());
			DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

			do {

			PeerMessage peerCliente = PeerMessage.readMessageFromInputStream(dis);

			byte opCodeRequest = peerCliente.getOpcode();
			switch(opCodeRequest) {
			case PeerMessageOps.OPCODE_DOWNLOAD_CHUNK:{

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
						PeerMessage peerServer = new PeerMessage(PeerMessageOps.OPCODE_FILE_CHUNK, fileSelect.fileHash, (short) bytes.length, bytes);

						peerServer.writeMessageToOutputStream(dos);
						}
						else{
							PeerMessage peerServer = new PeerMessage(PeerMessageOps.OPCODE_FILE_NOT_FOUND);
							peerServer.writeMessageToOutputStream(dos);
						}
						break;
					}
				case PeerMessageOps.OPCODE_DOWNLOAD_END:{
					end = true;

					if(fo==null) {
						System.out.println("Se ha terminado la descarga del cliente:"+ socket.getInetAddress().toString() + ":" + socket.getPort());
					}
					break;
					}
				case PeerMessageOps.OPCODE_UPLOAD_REQUEST:{
					FileInfo[] files = NanoFiles.db.getFiles();
					boolean accept = true;
					for(FileInfo f : files) {

						if(f.fileHash.equals(peerCliente.getFileHash()) || f.fileName.equals(peerCliente.getFileName())) {
							PeerMessage responseUpload = new PeerMessage(PeerMessageOps.OPCODE_UPLOAD_DENIED);
							responseUpload.writeMessageToOutputStream(dos);
							accept = false;
							break;
						}
					}
					if(accept) {
						PeerMessage responseUpload = new PeerMessage(PeerMessageOps.OPCODE_UPLOAD_ACCEPT);
						responseUpload.writeMessageToOutputStream(dos);
						upload = new File(NanoFiles.sharedDirname, peerCliente.getFileName());
						System.out.println("Se ha aceptado la descarga del fichero: " + peerCliente.getFileName());
					}
					break;
					}
				case PeerMessageOps.OPCODE_UPLOAD_CHUNK:{
					if(fo==null) {
						fo = new FileOutputStream(upload);
					}
					byte[] contenido = peerCliente.getChunk();
					fo.write(contenido);
					break;
					}
				default:{
					System.out.println("Unexpected value: " + opCodeRequest);
					}
				}
			}
			while(!end);
			if(fo!=null) {
				fo.close();
				System.out.println("Se termino la descarga con exito del fichero recibido del cliente con socket: " + socket.getInetAddress().toString() + ":" + socket.getLocalPort());
			}
		} catch (IOException ex) {
			System.out.println("Server : " + ex.getMessage());
			ex.printStackTrace();
		}
	}
}
