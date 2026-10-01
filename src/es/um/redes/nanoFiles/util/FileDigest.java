package es.um.redes.nanoFiles.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileDigest {

	public static final String algorithm = "SHA-1";

	public static int getFileDigestSize() {
		try {
			return getDigestSize(algorithm);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return 0;
		}
	}

	private static int getDigestSize(String algorithm) throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance(algorithm);
		String input = "";
		byte[] fileDigest = md.digest(input.getBytes());
		return fileDigest.length;
	}

	public static String computeFileChecksumString(String filename) {
		return FileDigest.getChecksumHexString(computeFileChecksum(filename));
	}

	private static byte[] computeFileChecksum(String filename) {
		MessageDigest md;
		try {
			md = MessageDigest.getInstance(algorithm);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}
		InputStream fis;
		try {
			fis = new FileInputStream(filename);
			int numRead;
			byte[] buffer = new byte[4096];
			do {
				numRead = fis.read(buffer);
				if (numRead > 0) {
					md.update(buffer, 0, numRead);
				}
			} while (numRead != -1);
			fis.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

		return md.digest();
	}

	private static String getChecksumHexString(byte[] digest) {

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < digest.length; i++) {
			sb.append(Integer.toString((digest[i] & 0xff) + 0x100, 16).substring(1));
		}

		return sb.toString();
	}
}