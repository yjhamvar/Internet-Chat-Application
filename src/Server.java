import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.ServerSocket;

public class Server {

	private static ServerSocket serverSocket = null;
	private static Socket clientSocket = null;

	private static final int maxClientCount = 10;
	private static final ChatServerHandler[] threads = new ChatServerHandler[maxClientCount];

	public static void main(String args[]) {

		int portNumber = 8000;
		System.out.println("Server running on port number: " + portNumber);

		try {
			serverSocket = new ServerSocket(portNumber);
		} catch (IOException e) {

			e.printStackTrace();
		}

		while (true) {
			try {
				clientSocket = serverSocket.accept();
				int i = 0;
				for (i = 0; i < maxClientCount; i++) {
					if (threads[i] == null) {
						(threads[i] = new ChatServerHandler(clientSocket, threads)).start();
						break;
					}
				}
				if (i == maxClientCount) {
					PrintStream ps = new PrintStream(clientSocket.getOutputStream());
					ps.println("Server is busy, Please try again later.");
					ps.close();
					clientSocket.close();
				}
			} catch (IOException e) {
				System.out.println(e);
			}
		}
	}
}

class ChatServerHandler extends Thread {
	public final static String RECEIVED_FILEPATH = System.getProperty("user.dir");
	public final static int FILE_SIZE = 16022386;
	private String clientName = null;
	private DataInputStream datainpstream = null;
	private PrintStream pstream = null;
	private Socket clientSocket = null;
	private final ChatServerHandler[] threads;
	private int maxClientsCount;
	private OutputStream outputstream = null;

	public ChatServerHandler(Socket clientSocket, ChatServerHandler[] threads) {
		this.clientSocket = clientSocket;
		this.threads = threads;
		maxClientsCount = threads.length;
	}

	@SuppressWarnings("deprecation")
	public void run() {
		int maxClientsCount = this.maxClientsCount;
		ChatServerHandler[] threads = this.threads;

		try {
			datainpstream = new DataInputStream(clientSocket.getInputStream());
			pstream = new PrintStream(clientSocket.getOutputStream());
			outputstream = clientSocket.getOutputStream();
			String name;
			while (true) {
				pstream.println("Enter your name: ");
				name = datainpstream.readLine().trim();
				if (name.indexOf('@') == -1) {
					break;
				} else {
					pstream.println("<Server Message> The name cannot contain '@' character. Please enter a new name!");
				}
			}

			pstream.println(
					"<Server Message> Welcome to the chat " + name + ".\nTo leave the chat type #Exit in a new line.");
			synchronized (this) {
				for (int i = 0; i < maxClientsCount; i++) {
					if (threads[i] != null && threads[i] == this) {
						clientName = "@" + name;
						break;
					}
				}
				for (int i = 0; i < maxClientsCount; i++) {
					if (threads[i] != null && threads[i] != this) {
						threads[i].pstream
								.println("<Server Message> A new user " + name + " has entered the chat room !!!");
					}
				}
			}

			while (true) {
				String line = datainpstream.readLine();
				if (line.startsWith("#Exit")) {
					break;
				}
				if (line.startsWith("@")) {
					privateMessage(line, name);
				} else if (line.startsWith("#File")) {
					takeFile(line, name);
				} else if (line.startsWith("!@")) {
					blockMessage(line, name);
				} else {
					broadcastMessage(line, name);
				}
			}
			clientExit(name);

			synchronized (this) {
				for (int i = 0; i < maxClientsCount; i++) {
					if (threads[i] == this) {
						threads[i] = null;
					}
				}
			}

			System.out.println("Closing streams at server");
			datainpstream.close();
			pstream.close();
			clientSocket.close();
		} catch (IOException e) {
		}
	}

	private void takeFile(String line, String name) throws IOException {
		int bytesRead;
		int current = 0;
		FileOutputStream fileoutstream = null;
		BufferedOutputStream bufferoutstream = null;

		try {
			String split[] = line.split("\\s", 5);
			InputStream ist = this.clientSocket.getInputStream();
			byte[] mybytearray = new byte[FILE_SIZE];
			File path = new File(RECEIVED_FILEPATH + "\\ServerTemp");
			if (!path.exists()) {
				if (path.mkdir()) {
					System.out.println("Server directory created successfully at path: " + path.getAbsolutePath());
				} else {
					System.out.println("Failed to create Temp Server directory!");

				}
			}

			String temp = path.getPath() + "\\" + split[3];
			// System.out.println("FilePath: " + temp);
			long fileSize = Long.parseLong(split[4]);
			// System.out.println("FileSize: " + fileSize);
			fileoutstream = new FileOutputStream(temp);
			bufferoutstream = new BufferedOutputStream(fileoutstream);
			bytesRead = ist.read(mybytearray, 0, mybytearray.length);
			current = bytesRead;

			do {
				if (current == fileSize)
					break;
				bytesRead = ist.read(mybytearray, current, (mybytearray.length - current));
				if (bytesRead >= 0) {
					current += bytesRead;
				}
			} while (bytesRead > -1);

			bufferoutstream.write(mybytearray, 0, current);
			bufferoutstream.flush();
		} finally {
			if (fileoutstream != null)
				fileoutstream.close();
			if (bufferoutstream != null)
				bufferoutstream.close();
		}

		if (line.startsWith("#File unicast")) {
			privateFile(line, name);
		}

		if (line.startsWith("#File blockcast")) {
			blockcastFile(line, name);
		}

		if (line.startsWith("#File broadcast")) {
			broadcastFile(line, name);
		}
	}

	public void privateFile(String line, String name) throws IOException {
		FileInputStream fileinpstream = null;
		BufferedInputStream bufferinpstream = null;

		String split[] = line.split("\\s", 5);

		synchronized (this) {
			for (int i = 0; i < maxClientsCount; i++) {
				if (threads[i] != null && threads[i] != this && threads[i].clientName != null
						&& threads[i].clientName.equals(split[2])) {
					try {
						File myFile = new File(RECEIVED_FILEPATH + "\\ServerTemp\\" + split[3]);
						String filename = myFile.getName();
						long fileSize = myFile.length();
						byte[] mybytearray = new byte[(int) myFile.length()];
						fileinpstream = new FileInputStream(myFile);
						bufferinpstream = new BufferedInputStream(fileinpstream);
						bufferinpstream.read(mybytearray, 0, mybytearray.length);
						threads[i].pstream.println("#File " + filename + " " + fileSize + " " + name);

						threads[i].outputstream.write(mybytearray, 0, mybytearray.length);

						System.out.println(
								"Unicast File sent successfully from @" + name + " to " + this.threads[i].clientName);
						fileinpstream.close();
						bufferinpstream.close();
						threads[i].outputstream.flush();
						threads[i].pstream.flush();
						myFile.delete();
						break;
					} catch (IOException e) {
						e.printStackTrace();
					} finally {
						if (fileinpstream != null) {
							fileinpstream.close();
						}
						if (bufferinpstream != null) {
							bufferinpstream.close();
						}
						if (outputstream != null) {
							outputstream.flush();
						}
					}
				}
			}
		}
	}

	public void blockcastFile(String line, String name) throws IOException {
		FileInputStream fileinpstream = null;
		BufferedInputStream bufferinpstream = null;

		String split[] = line.split("\\s", 5);

		synchronized (this) {
			File myFile = new File(RECEIVED_FILEPATH + "\\ServerTemp\\" + split[3]);
			for (int i = 0; i < maxClientsCount; i++) {

				if (threads[i] != null && threads[i] != this && threads[i].clientName != null
						&& !threads[i].clientName.equals(split[2])) {
					try {

						String filename = myFile.getName();
						long fileSize = myFile.length();

						byte[] mybytearray = new byte[(int) myFile.length()];
						fileinpstream = new FileInputStream(myFile);
						bufferinpstream = new BufferedInputStream(fileinpstream);
						bufferinpstream.read(mybytearray, 0, mybytearray.length);

						threads[i].pstream.println("#File " + filename + " " + fileSize + " " + name);
						threads[i].outputstream.write(mybytearray, 0, mybytearray.length);
						fileinpstream.close();
						bufferinpstream.close();
						threads[i].outputstream.flush();
						threads[i].pstream.flush();

					} catch (IOException e) {

						e.printStackTrace();
					} finally {
						if (fileinpstream != null) {
							fileinpstream.close();
						}
						if (bufferinpstream != null) {
							bufferinpstream.close();
						}
						if (outputstream != null) {
							outputstream.flush();
						}
					}
				}
			}
			myFile.delete();
			System.out.println("Blockcast File Sent Successfully from @" + name + " to everyone EXCEPT " + split[2]);
		}
	}

	public void broadcastFile(String line, String name) throws IOException {
		FileInputStream fileinpstream = null;
		BufferedInputStream bufferinpstream = null;
		String split[] = line.split("\\s", 5);

		synchronized (this) {
			File myFile = new File(RECEIVED_FILEPATH + "\\ServerTemp\\" + split[3]);
			for (int i = 0; i < maxClientsCount; i++) {
				if (threads[i] != null && threads[i].clientName != null
						&& !threads[i].clientName.equals(this.clientName)) {
					try {

						String filename = myFile.getName();
						long fileSize = myFile.length();
						byte[] mybytearray = new byte[(int) myFile.length()];
						fileinpstream = new FileInputStream(myFile);
						bufferinpstream = new BufferedInputStream(fileinpstream);
						bufferinpstream.read(mybytearray, 0, mybytearray.length);
						threads[i].pstream.println("#File " + filename + " " + fileSize + " " + name);

						threads[i].outputstream.write(mybytearray, 0, mybytearray.length);
						fileinpstream.close();
						bufferinpstream.close();
						threads[i].outputstream.flush();
						threads[i].pstream.flush();

					} catch (IOException e) {
						e.printStackTrace();
					} finally {
						if (fileinpstream != null) {
							fileinpstream.close();
						}
						if (bufferinpstream != null) {
							bufferinpstream.close();
						}
						if (outputstream != null) {
							outputstream.flush();
						}

					}
				}
			}
			System.out.println("Broadcast File Sent Successfully from @" + name + " to everyone");
			myFile.delete();
		}
	}

	public void privateMessage(String line, String name) {

		String[] words = line.split("\\s", 2);
		if (words.length > 1 && words[1] != null) {
			words[1] = words[1].trim();
			if (!words[1].isEmpty()) {
				synchronized (this) {
					for (int i = 0; i < maxClientsCount; i++) {
						if (threads[i] != null && threads[i] != this && threads[i].clientName != null
								&& threads[i].clientName.equals(words[0])) {
							threads[i].pstream.println("@" + name + ": " + words[1]);
							this.pstream.println("Message Sent to " + words[0]);
							System.out.println("Private Message sent from @" + name + " to " + threads[i].clientName);
							break;
						}
					}
				}
			}
		}
	}

	public void blockMessage(String line, String name) {

		String[] words = line.split("\\s", 2);
		String[] temp = words[0].split("!");

		if (words.length > 1 && words[1] != null) {
			words[1] = words[1].trim();
			if (!words[1].isEmpty()) {
				synchronized (this) {
					for (int i = 0; i < maxClientsCount; i++) {
						if (threads[i] != null && threads[i] != this && threads[i].clientName != null
								&& !(threads[i].clientName.equals(temp[1]))) {
							threads[i].pstream.println("@" + name + ": " + words[1]);
						}
					}
					this.pstream.println("Message sent to everyone except " + temp[1]);
					System.out.println("Blockcast Message sent from @" + name + " to everyone EXCEPT " + temp[1]);
				}
			}
		}
	}

	public void broadcastMessage(String line, String name) {
		synchronized (this) {
			for (int i = 0; i < maxClientsCount; i++) {
				if (threads[i] != null && threads[i].clientName != null) {
					threads[i].pstream.println("@" + name + ": " + line);
				}
			}
			this.pstream.println("Message sent to everyone");
			System.out.println("Broadcast Message sent from @" + name + " to everyone");
		}

	}

	public void clientExit(String name) {
		synchronized (this) {
			for (int i = 0; i < maxClientsCount; i++) {
				if (threads[i] != null && threads[i] != this && threads[i].clientName != null) {
					threads[i].pstream.println("<Server Message> The user @" + name + " is leaving the chat room!!!");
				}
			}
		}
		pstream.println("<Server Message> bye " + name + "!!!");
	}
}
