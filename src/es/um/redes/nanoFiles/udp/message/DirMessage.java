package es.um.redes.nanoFiles.udp.message;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import es.um.redes.nanoFiles.util.FileInfo;

public class DirMessage {
	public static final int PACKET_MAX_SIZE = 65507;

	private static final char DELIMITER = ':';
	private static final char END_LINE = '\n';

	private static final String FIELDNAME_OPERATION = "operation";

	private static final String FIELDNAME_IP_PROTOCOL = "id_protocol";

	private static final String FIELDNAME_FILE_NAME  =  "file";

	private static final String FIELDNAME_FILE_HASH  =  "hash";

	private static final String FIELDNAME_FILE_SIZE  =  "size";

	private static final String FIELDNAME_SERVER_IP  =  "ip_server";

	private static final String FIELDNAME_SERVER_PORT  =  "port";

	private String operation = DirMessageOps.OPERATION_INVALID;

	private String protocolId;

	private Set<FileInfo> listFile;

	private Set<InetSocketAddress> serversTCP;

	private int serverPort;

	private String fileDownload;

	public DirMessage(String op) {
		operation = op;
	}

	public DirMessage(String op, String protocolIdent) {
		operation = op;
		protocolId = protocolIdent;
	}

	public	DirMessage(String op, List<FileInfo> listFiles, int puerto) {
		operation = op;
		listFile = new HashSet<FileInfo>();
		for(FileInfo file : listFiles) {
			listFile.add(file);
		}
		serverPort = puerto;
	}

	public	DirMessage(String op, Set<InetSocketAddress> serversProvaider) {
		operation = op;
		serversTCP = new HashSet<InetSocketAddress>();
		for(InetSocketAddress server : serversProvaider) {
			serversTCP.add(server);
		}
	}

	public String getOperation() {
		return operation;
	}

	public void setProtocolID(String protocolIdent) {
		if (!operation.equals(DirMessageOps.OPERATION_PING)) {
			throw new RuntimeException(
					"DirMessage: setProtocolId called for message of unexpected type (" + operation + ")");
		}
		protocolId = protocolIdent;
	}

	public String getProtocolId() {
		return protocolId;
	}

	public	void setServetFileList(Set<FileInfo> listFiles) {
		if (!operation.equals(DirMessageOps.FILE_LIST_RESPONSE) && !operation.equals(DirMessageOps.OPERATION_SERVE)) {
			throw new RuntimeException(
					"DirMessage: setFileList called for message of unexpected type (" + operation + ")");
		}
		this.listFile = new HashSet<FileInfo>();
		for(FileInfo file : listFiles) {
			listFile.add(file);
		}
	}

	public Set<FileInfo> getFileList() {
		return new HashSet<FileInfo>(listFile);
	}

	public void setServerPort(int puertoServidor) {
		if (!operation.equals(DirMessageOps.OPERATION_SERVE) && !operation.equals(DirMessageOps.OPERATION_STOP_SERVE)) {
			throw new RuntimeException(
					"DirMessage: setServerPort called for message of unexpected type (" + operation + ")");
		}
		serverPort = puertoServidor;
	}

	public Integer getServerPort() {
		return serverPort;
	}

	public void setFileDownload(String fileSelect) {
		if (!operation.equals(DirMessageOps.OPERATION_DOWLOAD)) {
			throw new RuntimeException(
					"DirMessage: setFileList called for message of unexpected type (" + operation + ")");
		}
		fileDownload = fileSelect;
	}

	public String getFileDownload() {
		return fileDownload;
	}

	public void setServersTCP(List<InetSocketAddress> listaServers){
		if (!operation.equals(DirMessageOps.DOWLOAD_VALID)) {
			throw new RuntimeException(
					"DirMessage: setFileList called for message of unexpected type (" + operation + ")");
		}
		serversTCP = new HashSet<InetSocketAddress>(listaServers);
	}

	public Set<InetSocketAddress> getServersTCP(){
		return new HashSet<InetSocketAddress>(serversTCP);
	}

	public static DirMessage fromString(String message) {

		String[] lines = message.split(END_LINE + "");

		DirMessage m = null;
		String fileName="";
		String hash;
		long size = -1;

		String ipValue = "";
		FileInfo lastFile = null;
		for (String line : lines) {
			int idx = line.indexOf(DELIMITER);
			String fieldName = line.substring(0, idx).toLowerCase();
			String value = line.substring(idx + 1).trim();

			switch (fieldName) {
			case FIELDNAME_OPERATION: {
				assert (m == null);
				m = new DirMessage(value);
				if(value.equals(DirMessageOps.FILE_LIST_RESPONSE) || value.equals(DirMessageOps.OPERATION_SERVE)) {
					m.listFile = new HashSet<FileInfo>();
				}
				else if (value.equals(DirMessageOps.DOWLOAD_VALID) || value.equals(DirMessageOps.UPLOAD_SERVERS)) {
					m.serversTCP = new HashSet<InetSocketAddress>();
				}

				break;
			}
			case FIELDNAME_FILE_NAME:{
				fileName = value;
				m.fileDownload = value;
				break;
			}
			case FIELDNAME_FILE_SIZE:{
				size = Long.parseLong(value);;
				break;
			}
			case FIELDNAME_FILE_HASH:{
				hash = value;
				lastFile = new FileInfo(hash, fileName, size, "");
				m.listFile.add(lastFile);
				break;
			}
			case FIELDNAME_SERVER_IP:{
				ipValue = value;
				break;
			}
			case FIELDNAME_SERVER_PORT:{
				if(m.getOperation().equals(DirMessageOps.DOWLOAD_VALID) || m.getOperation().equals(DirMessageOps.UPLOAD_SERVERS)) {
					int	puerto = Integer.parseInt(value);
					InetSocketAddress socket = new InetSocketAddress(ipValue, puerto);
					m.serversTCP.add(socket);
					break;
				}
				else if(m.getOperation().equals(DirMessageOps.FILE_LIST_RESPONSE)) {
					int	puerto = Integer.parseInt(value);
					InetSocketAddress inet = new InetSocketAddress(ipValue, puerto);
					lastFile.servers.add(inet);
					break;
				}
				else {
					m.serverPort = Integer.parseInt(value);
					break;
				}
			}
			case FIELDNAME_IP_PROTOCOL:{
				m.protocolId = value;
				break;
			}

			default:
				System.err.println("PANIC: DirMessage.fromString - message with unknown field name " + fieldName);
				System.err.println("Message was:\n" + message);
				System.exit(-1);
			}
		}

		return m;
	}

	public String toString() {

		StringBuffer sb = new StringBuffer();
		sb.append(FIELDNAME_OPERATION + DELIMITER + operation + END_LINE);
		if(operation.equals(DirMessageOps.OPERATION_PING)) {
			sb.append(FIELDNAME_IP_PROTOCOL + DELIMITER + protocolId + END_LINE);
			}
		else if(operation.equals(DirMessageOps.FILE_LIST_RESPONSE)){
			for(FileInfo file : listFile) {
				String cadena = file.toString();
				String subcadena;

				subcadena = cadena.substring(0, 30).trim();
				sb.append(FIELDNAME_FILE_NAME + DELIMITER + subcadena + END_LINE);
				subcadena = cadena.substring(30, 40).trim();
				sb.append(FIELDNAME_FILE_SIZE + DELIMITER + subcadena + END_LINE);
				subcadena = cadena.substring(41,85).trim();
				sb.append(FIELDNAME_FILE_HASH + DELIMITER + subcadena + END_LINE);
				for(InetSocketAddress serv : file.servers) {
					String ip = serv.getHostString();
					String puerto = Integer.toString(serv.getPort());
					sb.append(FIELDNAME_SERVER_IP + DELIMITER + ip + END_LINE);
					sb.append(FIELDNAME_SERVER_PORT + DELIMITER + puerto + END_LINE);
				}
			}
		}
		else if(operation.equals(DirMessageOps.DOWLOAD_VALID) || operation.equals(DirMessageOps.UPLOAD_SERVERS)) {
			for(InetSocketAddress serv : serversTCP) {
				String ip = serv.getHostString();
				String puerto = Integer.toString(serv.getPort());
				sb.append(FIELDNAME_SERVER_IP + DELIMITER + ip + END_LINE);
				sb.append(FIELDNAME_SERVER_PORT + DELIMITER + puerto + END_LINE);
			}
		}
		else if(operation.equals(DirMessageOps.OPERATION_SERVE)) {
			sb.append(FIELDNAME_SERVER_PORT + DELIMITER + serverPort + END_LINE);
			for(FileInfo file : listFile) {
				String cadena = file.toString();
				String subcadena;

				subcadena = cadena.substring(0, 30).trim();
				sb.append(FIELDNAME_FILE_NAME + DELIMITER + subcadena + END_LINE);
				subcadena = cadena.substring(30, 40).trim();
				sb.append(FIELDNAME_FILE_SIZE + DELIMITER + subcadena + END_LINE);
				subcadena = cadena.substring(41, 85).trim();
				sb.append(FIELDNAME_FILE_HASH + DELIMITER + subcadena + END_LINE);
			}

		}
		else if(operation.equals(DirMessageOps.OPERATION_DOWLOAD)) {
			sb.append(FIELDNAME_FILE_NAME + DELIMITER + fileDownload + END_LINE);
		}
		else if(operation.equals(DirMessageOps.OPERATION_STOP_SERVE)) {
			sb.append(FIELDNAME_SERVER_PORT + DELIMITER + serverPort + END_LINE);
		}

		sb.append(END_LINE);
		return sb.toString();
	}

}
