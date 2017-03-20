package network;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Scanner;

public class Client {

	static int windowSize = 4;

	public static void main(String[] args) {

		DatagramSocket socket = null;
		DatagramPacket inputPacket = null; // receiving packet
		DatagramPacket outputPacket = null; // sending packet
		byte[] inBuf, outBuf; // input buffer output buffer
		ack ackn = null;
		Data datap = null;
		String message;
		int port = 3000;
		int seqno = 0;
		BufferedWriter bufferwrite = null;
		Scanner src = new Scanner(System.in);
		try {
			InetAddress address = InetAddress.getByName("127.0.0.1");
			socket = new DatagramSocket();

			message = "";

			outBuf = message.getBytes();
			outputPacket = new DatagramPacket(outBuf, 0, outBuf.length, address, port);
			socket.send(outputPacket);

			inBuf = new byte[1024];
			inputPacket = new DatagramPacket(inBuf, inBuf.length);
			socket.receive(inputPacket);
			port = inputPacket.getPort();
			// ack
			ackn = new ack(8, seqno);
			outBuf = serialize(ackn);
			outputPacket = new DatagramPacket(outBuf, 0, outBuf.length, address, port);
			socket.send(outputPacket);
			seqno++;

			datap = (Data) deserialize(inputPacket.getData());
			String data = new String(datap.getData(), 0, (datap.getData()).length);

			// print file list

			System.out.println(data);

			// send file name

			String filename = src.nextLine();
			outBuf = filename.getBytes();
			outputPacket = new DatagramPacket(outBuf, 0, outBuf.length, address, port);
			socket.send(outputPacket);

			// Receive file
			StringBuilder stringbuild = new StringBuilder();
			Hashtable<Integer, String> window = new Hashtable<Integer, String>();
			int[] windowack = new int[windowSize];
			int windowcounter = 1;
			int last=1;
			boolean Break=false;
			while (true) {

				inBuf = new byte[1024];
				inputPacket = new DatagramPacket(inBuf, inBuf.length);
				socket.receive(inputPacket);
				datap = (Data) deserialize(inputPacket.getData());
				data = new String(datap.getData(), 0, (datap.getData()).length);
				if (datap.getSeqno() - windowcounter < windowSize) {
					if (window.get(datap.getSeqno()) == null&&data!=null) {
						window.put(datap.getSeqno(), data);
					}

					if (windowcounter <= datap.getSeqno()) {
						if (datap.getSeqno() - windowcounter < windowSize)
							windowack[datap.getSeqno() - windowcounter] = 1;
					}

					ackn = new ack(8, windowcounter);
					outBuf = serialize(ackn);
					outputPacket = new DatagramPacket(outBuf, 0, outBuf.length, inputPacket.getAddress(),
							inputPacket.getPort());
					socket.send(outputPacket);

					while (true) {
						if (windowack[0] == 1) {
							for (int i = 0; i < windowSize - 1; i++) {
								windowack[i] = windowack[i + 1];
							}
							windowack[windowSize - 1] = 0;
							windowcounter++;
						} else
							break;
					}
				}
				if (data.endsWith("Error")) {
					break;
				}
                 System.out.println(windowcounter);
                 if(data.equalsIgnoreCase("finsh")){
                	 Break=true;
                 }
                 if(windowcounter<=datap.getSeqno()){
                	 continue;
                 }
				if (Break) {
					for (int i = last; i <= datap.getSeqno(); i++) {
						if(!window.get(i).equalsIgnoreCase("finsh"))
						stringbuild.append(window.get(i));
					}
					last=datap.getSeqno();
					try {
						bufferwrite = new BufferedWriter(new FileWriter(filename + port, true));
						bufferwrite.write(stringbuild.toString().replace("\0", ""));
						bufferwrite.flush();
						stringbuild = new StringBuilder();
					} catch (IOException ioe) {
						ioe.printStackTrace();
					} finally { // always close the file
						if (bufferwrite != null)
							try {
								bufferwrite.close();
							} catch (IOException ioe2) {

							}
					}
					System.out.println("finshed");
					break;
				}

			}
       
			if (data.endsWith("Error")) {

				System.out.println("file not exist");
				socket.close();
			}

			else {

				System.out.println("file sent successfully");
				socket.close();

			}

		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("error");
		}
	}

	public static byte[] serialize(Object obj) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ObjectOutputStream os = new ObjectOutputStream(out);
		os.writeObject(obj);
		return out.toByteArray();
	}

	public static Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
		ByteArrayInputStream in = new ByteArrayInputStream(data);
		ObjectInputStream is = new ObjectInputStream(in);
		return is.readObject();
	}
}
