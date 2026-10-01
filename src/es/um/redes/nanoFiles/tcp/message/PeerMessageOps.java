package es.um.redes.nanoFiles.tcp.message;

import java.util.Map;
import java.util.TreeMap;

public class PeerMessageOps {

	public static final byte OPCODE_INVALID_CODE = 0;

	public static final byte OPCODE_DOWNLOAD_CHUNK = 1;

	public static final byte OPCODE_FILE_NOT_FOUND = 2;

	public static final byte OPCODE_FILE_CHUNK = 3;

	public static final byte OPCODE_DOWNLOAD_END= 4;

	public static final byte OPCODE_UPLOAD_REQUEST= 5;

	public static final byte OPCODE_UPLOAD_ACCEPT= 6;

	public static final byte OPCODE_UPLOAD_DENIED= 7;

	public static final byte OPCODE_UPLOAD_CHUNK= 8;

	private static final Byte[] _valid_opcodes = { OPCODE_INVALID_CODE, OPCODE_DOWNLOAD_CHUNK, OPCODE_FILE_NOT_FOUND, OPCODE_FILE_CHUNK, OPCODE_DOWNLOAD_END, OPCODE_UPLOAD_REQUEST, OPCODE_UPLOAD_ACCEPT, OPCODE_UPLOAD_DENIED, OPCODE_UPLOAD_CHUNK

	};
	private static final String[] _valid_operations_str = { "INVALID_OPCODE", "DOWNLOAD_CHUNK", "FILE_NOT_FOUND", "FILE_CHUNK", "DOWNLOAD_END", "UPLOAD_REQUEST", "UPLOAD_ACCEPT", "UPLOAD_DENIED", "UPLOAD_CHUNK"

	};

	private static Map<String, Byte> _operation_to_opcode;
	private static Map<Byte, String> _opcode_to_operation;

	static {
		_operation_to_opcode = new TreeMap<>();
		_opcode_to_operation = new TreeMap<>();
		for (int i = 0; i < _valid_operations_str.length; ++i) {
			_operation_to_opcode.put(_valid_operations_str[i].toLowerCase(), _valid_opcodes[i]);
			_opcode_to_operation.put(_valid_opcodes[i], _valid_operations_str[i]);
		}
	}

	protected static byte operationToOpcode(String opStr) {
		return _operation_to_opcode.getOrDefault(opStr.toLowerCase(), OPCODE_INVALID_CODE);
	}

	public static String opcodeToOperation(byte opcode) {
		return _opcode_to_operation.getOrDefault(opcode, null);
	}
}
