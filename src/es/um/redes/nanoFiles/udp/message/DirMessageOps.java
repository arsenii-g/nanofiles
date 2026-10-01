package es.um.redes.nanoFiles.udp.message;

public class DirMessageOps {

	public static final String OPERATION_INVALID = "invalid_operation";
	public static final String OPERATION_VALID = "valid_operation";

	public static final String OPERATION_PING = "ping";
	public static final String PING_INVALID = "denied";
	public static final String PING_VALID = "welcome";

	public static final String OPERATION_FILE_LIST = "fileList";
	public static final String FILE_LIST_RESPONSE = "filesAvailable";
	public static final String FILE_LIST_EMPTY = "directoryEmpty";

	public static final String OPERATION_SERVE = "serve";
	public static final String OPERATION_STOP_SERVE = "stop";
	public static final String SERVE_VALID = "acceptServe";
	public static final String SERVE_INVALID = "deniedServe";
	public static final String STOP_SERVE_VALID = "acceptStopServe";
	public static final String STOP_SERVE_INVALID = "deniedStopServe";

	public static final String OPERATION_DOWLOAD = "download";
	public static final String DOWLOAD_VALID = "availableServers";
	public static final String DOWLOAD_INVALID = "differentHash";

	public static final String OPERATION_UPLOAD = "serversToUpload";
	public static final String UPLOAD_SERVERS = "serversAvailables";

}
