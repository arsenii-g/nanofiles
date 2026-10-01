package es.um.redes.nanoFiles.tcp.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class NFConnector {
	private Socket socket;
	private InetSocketAddress serverAddr;

	public DataInputStream dis;
	public DataOutputStream dos;

	public NFConnector(InetSocketAddress fserverAddr) throws UnknownHostException, IOException {
		serverAddr = fserverAddr;
		socket = new Socket(fserverAddr.getAddress(), fserverAddr.getPort());
		dis = new DataInputStream(socket.getInputStream());
		dos = new DataOutputStream(socket.getOutputStream());
	}

	public void test() {
		int i;
		try {
			dos.writeInt(10);
			i = dis.readInt();
			if(i==10) {
				System.out.println("Los mensajes concuerdan");
			}
		} catch (IOException e) {
			System.err.println("Error en la escritura");
			e.printStackTrace();
		}

	}

	public void closeConnection() {
		try {
			if(!socket.isClosed()) {
				socket.close();
			}
		} catch (IOException e) {
			System.err.println("Error al intentar cerrar el socket cliente");
			e.printStackTrace();
		}
	}

	public InetSocketAddress getServerAddr() {
		return serverAddr;
	}

}
