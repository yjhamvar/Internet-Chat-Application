import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.UnknownHostException;

public class Client implements Runnable {
	private static Socket clientSocket = null;
	private static PrintStream pstream = null;
	private static DataInputStream datainpstream = null;
	private static BufferedReader inputLine = null;
	private static boolean closed = false;
	private static OutputStream outputstream = null;
	private static FileInputStream fileinpstream = null;
	private static BufferedInputStream bufferinpstream = null;
	public final static int FILE_SIZE = 16022386;
	public final static String RECEIVED_FILEPATH = System.getProperty("user.dir");
	private static InputStream ist = null;
	public static int nameFlag = 1;
	public static String clientName = null;

	public static void main(String[] args) {
		int portNumber = 8000;
		String host = "localhost";

		System.out.println("Client initialising\n" + "with host: " + host + ", portNumber: " + portNumber);

		try {
			clientSocket = new Socket(host, portNumber);
			inputLine = new BufferedReader(new InputStreamReader(System.in));
			pstream = new PrintStream(clientSocket.getOutputStream());
			datainpstream = new DataInputStream(clientSocket.getInputStream());
			outputstream = clientSocket.getOutputStream();
			ist = clientSocket.getInputStream();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (clientSocket != null && pstream != null && datainpstream != null) {
			try {
				new Thread(new Client()).start();
				while (!closed) {
					String temp = inputLine.readLine().trim();
					if (temp.startsWith("#File")) {
						try {
							String split[] = temp.split("\\s", 4);
							File myFile;
							if (split[1].equals("broadcast")) {
								myFile = new File(split[2]);
							} else {
								myFile = new File(split[3]);
							}
							String filename = myFile.getName();
							long fileSize = myFile.length();
							byte[] mybytearray = new byte[(int) myFile.length()];
							fileinpstream = new FileInputStream(myFile);
							bufferinpstream = new BufferedInputStream(fileinpstream);
							bufferinpstream.read(mybytearray, 0, mybytearray.length);

							System.out.println("Sending " + myFile.getPath() + "(" + mybytearray.length + " bytes)");
							pstream.println("#File " + split[1] + " " + split[2] + " " + filename + " " + fileSize);
							outputstream.write(mybytearray, 0, mybytearray.length);
							System.out.println("File sent successfully!");
						} catch (Exception ex) {
							System.err.println("Exception:  " + ex);
							System.out.println("File NOT sent successfully!");
							ex.printStackTrace();
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
					} else {
						if (nameFlag == 1) {
							clientName = temp;
						}
						nameFlag = 0;
						pstream.println(temp);
						System.out.println("Sent: " + temp);
					}
				}

				pstream.close();
				datainpstream.close();
				clientSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
				System.err.println("IOException:  " + e);

			}
		}
	}

	private void takeFile(String line) throws IOException {
		int bytesRead;
		int current = 0;
		FileOutputStream fileoutstream = null;
		BufferedOutputStream bufferoutstream = null;

		try {
			String split[] = line.split("\\s", 4);
			byte[] mybytearray = new byte[FILE_SIZE];
			File clientDir = new File(RECEIVED_FILEPATH + "\\" + clientName);
			if (!clientDir.exists()) {
				if (clientDir.mkdir()) {
					System.out.println("Client directory created successfully at path: " + clientDir.getAbsolutePath());
				} else {
					System.out.println("Client directory NOT created!");
				}
			}
			String temp = clientDir.getPath() + "\\" + split[1];
			long fileSize = Long.parseLong(split[2]);
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
			System.out.println("File " + split[1] + " received of size (" + current + " bytes) from @" + split[3]);
		} finally {
			if (fileoutstream != null)
				fileoutstream.close();
			if (bufferoutstream != null)
				bufferoutstream.close();
		}
	}

	public void run() {
		String responseLine;
		try {
			while ((responseLine = datainpstream.readLine()) != null) {
				if (responseLine.startsWith("#File")) {
					takeFile(responseLine);
				} else {
					System.out.println(responseLine);
					if (responseLine.indexOf("<Server Message> bye ") != -1) {
						break;
					}
				}
			}
			closed = true;
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("IOException:  " + e);
		}
	}
}