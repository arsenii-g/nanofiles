package es.um.redes.nanoFiles.shell;

public class NFCommands {

	public static final byte COM_INVALID = 0;
	public static final byte COM_QUIT = 1;
	public static final byte COM_PING = 2;
	public static final byte COM_FILELIST = 4;
	public static final byte COM_MYFILES = 6;
	public static final byte COM_SERVE = 11;
	public static final byte COM_DOWNLOAD = 25;
	public static final byte COM_UPLOAD = 30;
	public static final byte COM_SERVERS = 35;
	public static final byte COM_HELP = 50;
	public static final byte COM_SOCKET_IN = 100;

	private static final Byte[] _valid_user_commands = {
		COM_QUIT,
		COM_PING,
		COM_FILELIST,
		COM_MYFILES,
		COM_SERVE,
		COM_DOWNLOAD,
		COM_UPLOAD,
		COM_SERVERS,
		COM_HELP,
		COM_SOCKET_IN
		};

	private static final String[] _valid_user_commands_str = {
			"quit",
			"ping",
			"filelist",
			"myfiles",
			"serve",
			"download",
			"upload",
			"servers",
			"help"
		};

	private static final String[] _valid_user_commands_help = {
			"quit the application",
			"ping directory to check protocol compatibility",
			"show list of files tracked by the directory",
			"show contents of local folder (files that may be served)",
			"run file server and publish served files to directory",
			"download file from all available server(s)",
			"upload file to server",
			"direction of each server",
			"shows this information"
			};

	public static byte stringToCommand(String comStr) {

		for (int i = 0;
		i < _valid_user_commands_str.length; i++) {
			if (_valid_user_commands_str[i].equalsIgnoreCase(comStr)) {
				return _valid_user_commands[i];
			}
		}

		return COM_INVALID;
	}

	public static String commandToString(byte command) {
		for (int i = 0;
		i < _valid_user_commands.length; i++) {
			if (_valid_user_commands[i] == command) {
				return _valid_user_commands_str[i];
			}
		}
		return null;
	}

	public static void printCommandsHelp() {
		System.out.println("List of commands:");
		for (int i = 0; i < _valid_user_commands_str.length; i++) {
			System.out.println(String.format("%1$15s", _valid_user_commands_str[i]) + " -- "
					+ _valid_user_commands_help[i]);
		}
	}
}

