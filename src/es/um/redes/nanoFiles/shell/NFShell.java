package es.um.redes.nanoFiles.shell;

import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.Vector;

import es.um.redes.nanoFiles.application.NanoFiles;

public class NFShell {

	private Scanner reader;

	byte command = NFCommands.COM_INVALID;
	String[] commandArgs = new String[0];

	boolean enableComSocketIn = false;
	private boolean skipValidateArgs;

	public static final String FILENAME_TEST_SHELL = ".nanofiles-test-shell";
	public static boolean enableVerboseShell = false;

	public NFShell() {
		reader = new Scanner(System.in);

		System.out.println("NanoFiles shell");
		System.out.println("For help, type 'help'");
	}

	public byte getCommand() {
		return command;
	}

	public String[] getCommandArguments() {
		return commandArgs;
	}

	public void readGeneralCommand() {
		boolean validArgs;
		do {
			commandArgs = readGeneralCommandFromStdIn();

			validArgs = validateCommandArguments(commandArgs);
		} while (!validArgs);
	}

	public String chooseDirectory(String defaultDirectory) {
		char response;
		String directory = null;
		do {
			System.out.print(
					"Do you want to use '" + defaultDirectory + "' as location of the directory server? (y/n): ");
			String input = reader.nextLine().trim().toLowerCase();
			if (input.length() == 1) {
				response = input.charAt(0);
				if (response == 'y') {
					directory = defaultDirectory;
				} else if (response == 'n') {
					System.out.print("Enter the directory hostname/IP:");
					directory = reader.nextLine().trim().toLowerCase();
				} else {
					System.out.println("Invalid key! Please, answer 'y' or 'n'.");
				}
			}
		} while (directory == null);
		System.out.println("Using directory location: " + directory);
		return directory;
	}

	private String[] readGeneralCommandFromStdIn() {
		String[] args = new String[0];
		Vector<String> vargs = new Vector<String>();
		while (true) {
			System.out.print("(nanoFiles@" + NanoFiles.sharedDirname + ") ");

			String input = reader.nextLine();
			StringTokenizer st = new StringTokenizer(input);

			if (st.hasMoreTokens() == false) {
				continue;
			}

			command = NFCommands.stringToCommand(st.nextToken());
			if (enableVerboseShell) {
				System.out.println(input);
			}
			skipValidateArgs = false;

			switch (command) {
			case NFCommands.COM_INVALID:

				System.out.println("Invalid command");
				continue;
			case NFCommands.COM_HELP:

				NFCommands.printCommandsHelp();
				continue;
			case NFCommands.COM_SERVERS:
			case NFCommands.COM_QUIT:
			case NFCommands.COM_FILELIST:
			case NFCommands.COM_MYFILES:
			case NFCommands.COM_SERVE:
			case NFCommands.COM_PING:

				break;
			case NFCommands.COM_DOWNLOAD:
			case NFCommands.COM_UPLOAD:

				while (st.hasMoreTokens()) {
					vargs.add(st.nextToken());
				}
				break;
			default:
				skipValidateArgs = true;
				System.out.println("Invalid command");
				;
			}
			break;
		}
		return vargs.toArray(args);
	}

	private boolean validateCommandArguments(String[] args) {
		if (skipValidateArgs)
			return false;
		switch (this.command) {
		case NFCommands.COM_DOWNLOAD:
			if (args.length != 2) {
				System.out.println(
						"Correct use:" + NFCommands.commandToString(command) + " <filename_substring> <local_filename>");
				return false;
			}
			break;
		case NFCommands.COM_UPLOAD:
			if (args.length != 2) {
				System.out.println(
						"Correct use:" + NFCommands.commandToString(command) + " <filename_substring> <remote_server>");
				return false;
			}
			break;
		default:
		}

		return true;
	}

	public static void enableVerboseShell() {
		enableVerboseShell = true;
	}
}
