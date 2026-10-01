package es.um.redes.nanoFiles.logic;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

import es.um.redes.nanoFiles.tcp.client.NFConnector;
import es.um.redes.nanoFiles.tcp.message.PeerMessage;
import es.um.redes.nanoFiles.tcp.message.PeerMessageOps;
import es.um.redes.nanoFiles.application.NanoFiles;

import es.um.redes.nanoFiles.tcp.server.NFServer;
import es.um.redes.nanoFiles.util.FileInfo;

public class NFControllerLogicP2P {
	private NFServer fileServer = null;
	private Thread t = null;
	private String directoryAddress;

	protected NFControllerLogicP2P(String directoryAdd) {
		directoryAddress = directoryAdd;
	}

	protected boolean startFileServer() {
		boolean serverRunning = false;

		if (fileServer != null) {
			System.err.println("File server is already running");
		}
		else {
			assert (fileServer == null);
			try {

				fileServer = new NFServer();
				t = new Thread(fileServer);
				t.start();
				serverRunning = true;

			} catch (IOException e1) {
				e1.printStackTrace();
				System.err.println("Cannot start the file server");
				fileServer = null;
			}

		}
		return serverRunning;

	}

	protected void testTCPServer() {
		assert (NanoFiles.testModeTCP);

		assert (fileServer == null);
		try {

			fileServer = new NFServer();
			fileServer.test();

		} catch (IOException e1) {
			e1.printStackTrace();
			System.err.println("Cannot start the file server");
			fileServer = null;
		}
	}

	public void testTCPClient() {

		assert (NanoFiles.testModeTCP);

		try {
			NFConnector nfConnector = new NFConnector(new InetSocketAddress(NFServer.PORT));
			nfConnector.test();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected boolean downloadFileFromServers(InetSocketAddress[] serverAddressList, String targetFileNameSubstring,
			String localFileName) {
		boolean downloaded = false;
		if (serverAddressList==null) {
			System.err.println("* Cannot start download - Ambiguous file name, further specify the name of the file to be downloaded");
			return false;
		}
		else if (serverAddressList.length == 0) {
			System.err.println("* Cannot start download - No list of server addresses provided");
			return false;
		}
		else {
			boolean sameFile = false;
			FileInfo[] files = NanoFiles.db.getFiles();
			for(FileInfo file : files) {
				if(file.fileName.equals(localFileName)) {
					sameFile = true;
					break;
				}
			}
			if(sameFile) {
				System.err.println("* Cannot start download - Same file to dowload is already download");
				return false;
			}
			else {
				NFConnector[] conectors = new NFConnector[serverAddressList.length];
				int pos = 0;
				try {
				if(directoryAddress.contains("localhost") || directoryAddress.contains("127.0.0.1")) {
					for(InetSocketAddress fserverAddr : serverAddressList) {

						NFConnector nf = new NFConnector(fserverAddr);
						conectors[pos] = nf;
						pos++;
						System.out.println("Conexión establecida con el servidor de ficheros en el socket: " + fserverAddr.toString());
					}
				}
				else {
					for(InetSocketAddress fserverAddr : serverAddressList) {
						if(fserverAddr.getAddress().toString().contains("0.0.0.0")) {
							InetSocketAddress fserverAddrRedirect = new InetSocketAddress(directoryAddress, fserverAddr.getPort());
							NFConnector nf = new NFConnector(fserverAddrRedirect);
							conectors[pos] = nf;
							System.out.println("Conexión establecida con el servidor de ficheros en el socket: " + nf.getServerAddr().toString());
							pos++;
						}
						else {
							NFConnector nf = new NFConnector(fserverAddr);
							conectors[pos] = nf;
							System.out.println("Conexión establecida con el servidor de ficheros en el socket: " + nf.getServerAddr().toString());
							pos++;
						}

					}
				}

				short sizeChunk = 1024;

				int length = 0;

				int	server = 0;

				File f = new File(NanoFiles.sharedDirname, localFileName);

					FileOutputStream fo = new FileOutputStream(f);
					boolean end = false;
					do {
						int s = server % pos;
						server++;
						NFConnector nf =  conectors[s];
						PeerMessage clientMessage = new PeerMessage(PeerMessageOps.OPCODE_DOWNLOAD_CHUNK, targetFileNameSubstring,length, sizeChunk);
						clientMessage.writeMessageToOutputStream(nf.dos);
						PeerMessage serverResponse = PeerMessage.readMessageFromInputStream(nf.dis);

						if(serverResponse.getOpcode()==PeerMessageOps.OPCODE_FILE_CHUNK) {
							short socket = serverResponse.getSizeChunk();
							if(socket<sizeChunk) {
								end = true;

							}
							byte[] contenido = serverResponse.getChunk();
							fo.write(contenido);
							length = length+sizeChunk;
						}
					} while (!end);
					fo.close();
					System.out.println("Descarga realizada con exito");

					for(NFConnector nf : conectors) {
						PeerMessage closeConnection = new PeerMessage(PeerMessageOps.OPCODE_DOWNLOAD_END);
						closeConnection.writeMessageToOutputStream(nf.dos);
						nf.closeConnection();
						System.out.println("Conexión cerrada con el servidor de ficheros en el socket: " + nf.getServerAddr().toString());
					}
					downloaded = true;
				} catch (IOException e) {
					System.err.println("Se ha producido un error al descargar el fichero de algún servidor");
				}
			}

		return downloaded;
		}
	}

	protected int getServerPort() {
		int port = 0;

		port = fileServer.getPort();

		return port;
	}

	protected void stopFileServer() {
		fileServer.stopServer();
	}

	protected boolean serving() {
		boolean result = false;
		if(fileServer!=null) {
		result = fileServer.isServing();
		}
		return result;

	}

	protected boolean uploadFileToServer(FileInfo matchingFile, String uploadToServer) {
		boolean result = false;

		int idx = uploadToServer.indexOf(":");
		String ipServer = uploadToServer.substring(0, idx).toLowerCase();
		String portString = uploadToServer.substring(idx + 1).trim();
		int portServer = Integer.parseInt(portString);
		InetSocketAddress idServer = new InetSocketAddress(ipServer, portServer);

		try {
			NFConnector nf = new NFConnector(idServer);

			PeerMessage peerProveedor = new PeerMessage(PeerMessageOps.OPCODE_UPLOAD_REQUEST, matchingFile.fileName, matchingFile.fileHash);
			peerProveedor.writeMessageToOutputStream(nf.dos);
			PeerMessage clientResponse = PeerMessage.readMessageFromInputStream(nf.dis);

			if(clientResponse.getOpcode()==PeerMessageOps.OPCODE_UPLOAD_ACCEPT) {
				System.out.println("Se ha aceptado la petición de \"upload\" por el cliente con socket :" + nf.getServerAddr().toString());
				String filePath = NanoFiles.db.lookupFilePath(matchingFile.fileHash);
				long posFich = 0;
				int sizeChunk = 1024;
				boolean end = false;
				do {
						RandomAccessFile fichero = new RandomAccessFile(filePath, "r");
						fichero.seek(posFich);

						long size =  matchingFile.fileSize;
						long resto = size-posFich;
						long div = resto/sizeChunk;
						if(div==0) {
							sizeChunk = (int) resto;
							end = true;
						}
						byte[] bytes = new byte[sizeChunk];
						fichero.readFully(bytes);
						posFich = posFich + sizeChunk;
						PeerMessage chunkUpload = new PeerMessage(PeerMessageOps.OPCODE_UPLOAD_CHUNK, (short) sizeChunk , bytes);
						chunkUpload.writeMessageToOutputStream(nf.dos);
						fichero.close();
				}
				while(!end);
				PeerMessage chunkUpload = new PeerMessage(PeerMessageOps.OPCODE_DOWNLOAD_END);
				chunkUpload.writeMessageToOutputStream(nf.dos);
				System.out.println("Se ha realizado el envío del fichero al destinatario sin problemas al socket: " + nf.getServerAddr().toString());
			}
			else {
				System.err.println("El fichero que se está intentando compartir ya está en el destinatario (con el mismo nombre o mismo hash)");
			}
		} catch (UnknownHostException e) {
			System.err.println("Error durante la conexión con: " + idServer);
		} catch (IOException e) {
			System.err.println("Error durante la crear conexión con: " + idServer);
		}

		return result;
	}

}
