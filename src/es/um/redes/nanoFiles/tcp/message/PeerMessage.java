package es.um.redes.nanoFiles.tcp.message;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class PeerMessage {

	private byte opcode;

	private int nameSize;

	private String fileName;

	private int hashSize;

	private String fileHash;

	private byte[] chunk;

	private int posChunk;

	private short sizeChunk;

	public PeerMessage() {
		opcode = PeerMessageOps.OPCODE_INVALID_CODE;
	}

	public PeerMessage(byte op) {
		opcode = op;
	}

	public PeerMessage(byte op, String fName, String fHash) {
		opcode = op;
		fileName = fName;
		fileHash = fHash;
	}

	public PeerMessage(byte op, short sChunk, byte[] ch) {
		opcode = op;
		sizeChunk = sChunk;
		chunk = ch;
	}

	public PeerMessage(byte op, String fName, int pChunk, short sChunk) {
		opcode = op;
		fileName = fName;
		posChunk = pChunk;
		sizeChunk = sChunk;
	}

	public PeerMessage(byte op, String fHash, short sChunk, byte[] ch) {
		opcode = op;
		fileHash = fHash;
		sizeChunk = sChunk;
		chunk = ch;
	}

	public byte getOpcode() {
		return opcode;
	}

	public void setOpcode(byte opcode) {
		this.opcode = opcode;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		if(opcode!=PeerMessageOps.OPCODE_DOWNLOAD_CHUNK&&opcode!=PeerMessageOps.OPCODE_FILE_CHUNK) {
			throw new RuntimeException(
					"PeerMessage: setFileName called for message of unexpected type (" + opcode + ")");
		}
		this.fileName = fileName;
	}

	public String getFileHash() {
		return fileHash;
	}

	public void setFileHash(String fileHash) {
		if(opcode!=PeerMessageOps.OPCODE_FILE_CHUNK) {
			throw new RuntimeException(
					"PeerMessage: setFileHash called for message of unexpected type (" + opcode + ")");
		}
		this.fileHash = fileHash;
	}

	public byte[] getChunk() {
		return chunk;
	}

	public void setChunk(byte[] chunk) {
		if(opcode!=PeerMessageOps.OPCODE_FILE_CHUNK) {
			throw new RuntimeException(
					"PeerMessage: setChunk called for message of unexpected type (" + opcode + ")");
		}
		this.chunk = chunk;
	}

	public int getPosChunk() {
		return posChunk;
	}

	public void setPosChunk(int posChunk) {
		if(opcode!=PeerMessageOps.OPCODE_DOWNLOAD_CHUNK) {
			throw new RuntimeException(
					"PeerMessage: setPosChunk called for message of unexpected type (" + opcode + ")");
		}
		this.posChunk = posChunk;
	}

	public short getSizeChunk() {
		return sizeChunk;
	}

	public void setSizeChunk(short sizeChunk) {
		if(opcode!=PeerMessageOps.OPCODE_DOWNLOAD_CHUNK&&opcode!=PeerMessageOps.OPCODE_FILE_CHUNK&&opcode!=PeerMessageOps.OPCODE_UPLOAD_CHUNK) {
			throw new RuntimeException(
					"PeerMessage: setSizeChunk called for message of unexpected type (" + opcode + ")");
		}
		this.sizeChunk = sizeChunk;
	}

	public static PeerMessage readMessageFromInputStream(DataInputStream dis) throws IOException {

		PeerMessage message = new PeerMessage();
		byte opcode = dis.readByte();
		message.setOpcode(opcode);
		switch (opcode) {
		case PeerMessageOps.OPCODE_DOWNLOAD_CHUNK:{
			message.nameSize = dis.readInt();
			byte[] name = new byte[message.nameSize];
			dis.readFully(name);
			message.fileName = new String(name,0, name.length);
			message.posChunk = dis.readInt();
			message.sizeChunk = dis.readShort();
			break;
		}
		case PeerMessageOps.OPCODE_FILE_CHUNK:{
			message.sizeChunk = dis.readShort();
			message.chunk = new byte[message.sizeChunk];
			dis.readFully(message.chunk);
			break;
		}
		case PeerMessageOps.OPCODE_UPLOAD_REQUEST:{
			message.hashSize = dis.readInt();
			byte[] hash = new byte[message.hashSize];
			dis.readFully(hash);
			message.fileHash = new String(hash,0,hash.length);
			message.nameSize = dis.readInt();
			byte[] name = new byte[message.nameSize];
			dis.readFully(name);
			message.fileName = new String(name,0, name.length);
			break;
		}
		case PeerMessageOps.OPCODE_UPLOAD_CHUNK:{
			message.sizeChunk = dis.readShort();
			message.chunk = new byte[message.sizeChunk];
			dis.readFully(message.chunk);
			break;
		}
		case PeerMessageOps.OPCODE_DOWNLOAD_END:
		case PeerMessageOps.OPCODE_FILE_NOT_FOUND:
		case PeerMessageOps.OPCODE_UPLOAD_DENIED:
		case PeerMessageOps.OPCODE_UPLOAD_ACCEPT:{

			break;
		}
		default:
			System.err.println("PeerMessage.readMessageFromInputStream doesn't know how to parse this message opcode: "
					+ PeerMessageOps.opcodeToOperation(opcode));
			System.exit(-1);
		}
		return message;
	}

	public void writeMessageToOutputStream(DataOutputStream dos) throws IOException {

		dos.writeByte(this.opcode);
		switch (this.opcode) {
		case 1:{
			byte[] name = fileName.getBytes();
			nameSize = name.length;
			dos.writeInt(nameSize);
			dos.write(name);
			dos.writeInt(posChunk);
			dos.writeShort(sizeChunk);
			break;
		}
		case 3:{
			sizeChunk = (short) chunk.length;
			dos.writeShort(sizeChunk);
			dos.write(chunk);
			break;
		}
		case 5:{
			byte[] hash = fileHash.getBytes();
			hashSize = hash.length;
			dos.writeInt(hashSize);
			dos.write(hash);
			byte[] name = fileName.getBytes();
			nameSize = name.length;
			dos.writeInt(nameSize);
			dos.write(name);
			break;
		}
		case 8:{
			sizeChunk = (short) chunk.length;
			dos.writeShort(sizeChunk);
			dos.write(chunk);
			break;
		}
		case 7:
		case 6:
		case 4:
		case 2:{
			break;
		}

		default:
			System.err.println("PeerMessage.writeMessageToOutputStream found unexpected message opcode " + opcode + "("
					+ PeerMessageOps.opcodeToOperation(opcode) + ")");
		}
	}
}
