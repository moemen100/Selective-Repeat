package network;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Array;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Random;

public class server extends Thread {
	int port;
	InetAddress source_address;
	int source_port;
	int seqno = 0;
	int windowSize = 4;

	public server(int port, int source_port, InetAddress source_address) {
		this.port = port;
		this.source_address = source_address;
		this.source_port = source_port;

	}

	public void run() {
		DatagramSocket socket = null;
		DatagramPacket inputPacket = null; // receiving packet
		DatagramPacket outputPacket = null; // sending packet
		byte[] inbuf, outbuf; // input buffer output buffer
		ack ackn = null;
		Data datap = null;
		long sample = (long) 1;
		long devrtt = (long) 1;
		long estimated = (long) 1;
		long time[] = new long[3];
		long now = 0;

		try {
			socket = new DatagramSocket(port);

			String dirname = "/home/moemen/server";

			File filel = new File(dirname);
			File file[] = filel.listFiles();

			StringBuilder stringbuild = new StringBuilder("\n");
			int counter = 0;

			for (int i = 0; i < file.length; i++) {

				if (file[i].canRead())
					counter++;

			}
			// send the files list
			stringbuild.append(counter + " files found \n");

			for (int i = 0; i < file.length; i++) {
				stringbuild.append(file[i].getName() + "  " + file[i].length() + " " + " Bytes\n");
			}

			stringbuild.append("enter file name for downloading");
			outbuf = (stringbuild.toString()).getBytes();
			datap = new Data(outbuf.length + 8, seqno, outbuf);
			outbuf = serialize(datap);
			outputPacket = new DatagramPacket(outbuf, 0, outbuf.length, source_address, source_port);
			socket.send(outputPacket);

			inbuf = new byte[1024];
			inputPacket = new DatagramPacket(inbuf, inbuf.length);
			socket.receive(inputPacket);

			seqno++;
			inbuf = new byte[1024];
			inputPacket = new DatagramPacket(inbuf, inbuf.length);
			socket.receive(inputPacket);

			String filename = new String(inputPacket.getData(), 0, inputPacket.getLength());
			System.out.println("user Requsted File: " + filename);

			boolean filelis = false;
			int index = -1;
			stringbuild = new StringBuilder("");
			for (int i = 0; i < file.length; i++) {
				if (((file[i].getName()).toString()).equalsIgnoreCase(filename)) {
					index = i;
					filelis = true;
				}
			}
			if (!filelis) {
				// sending error for the not found files

				System.out.println("Error");
				stringbuild.append("Error");
				outbuf = (stringbuild.toString()).getBytes();
				datap = new Data(outbuf.length + 8, seqno, outbuf);
				outbuf = serialize(datap);
				outputPacket = new DatagramPacket(outbuf, 0, outbuf.length, source_address, source_port);
				Random random = new Random();
				int chance = random.nextInt(100);

				time = timeout(sample, devrtt, estimated);
				devrtt = time[1];
				estimated = time[2];
				now = System.currentTimeMillis();
				if (chance < 90) {
					System.out.println(" file not exist sent from the first try");
					socket.send(outputPacket);
				} else {
					System.out.println(" file not exist not sent");
				}
				if ((int) time[0] == 0)
					time[0] = 1;
				socket.setSoTimeout((int) time[0]);

				while (true) {
					inbuf = new byte[1024];
					inputPacket = new DatagramPacket(inbuf, inbuf.length);

					try {
						socket.receive(inputPacket);
						ackn = (ack) deserialize(inputPacket.getData());

					} catch (SocketTimeoutException e) {
						// resend
						random = new Random();
						chance = random.nextInt(100);
						if (chance < 90) {
							System.out.println("resending file not exist");
							socket.send(outputPacket);
						}

						continue;
					}

					if (ackn.getLen() == 8 && ackn.getAckno() == seqno) {
						break;
					}
				}
				sample = System.currentTimeMillis() - now;
				now = System.currentTimeMillis();

			} else {
				try {
					// sending file
					File filepath = new File(file[index].getAbsolutePath());
					FileReader fileread = new FileReader(filepath);
					BufferedReader buffer = new BufferedReader(fileread);
					String s = null;
					stringbuild = new StringBuilder();
					while ((s = buffer.readLine()) != null) {
						stringbuild.append(s);
						if ((stringbuild.toString()).getBytes().length > 40000) {
							int length = (stringbuild.toString()).getBytes().length;
							byte tempbytes[] = (stringbuild.toString()).getBytes();
							outbuf = new byte[500];

							int j = 0;
							ArrayList<byte[]> q = new ArrayList<byte[]>();
							for (int i = 0; i < length; i++) {

								if (i % 500 == 0) {
									j = 0;
								}
								outbuf[j] = tempbytes[i];
								j++;

								if (i != 0 && (i % 499 == 0 || i == length - 1)) {
									datap = new Data(outbuf.length + 8, seqno, outbuf);
									if (i % 499 != 0) {
										byte temp[] = new byte[j + 1];
										System.arraycopy(outbuf, 0, temp, 0, j + 1);
										outbuf = temp;
										datap = new Data(outbuf.length + 8, seqno, outbuf);
									}
									outbuf = serialize(datap);
									q.add(outbuf);
									seqno++;
									if (i == length - 1) {
										outbuf = "finsh".getBytes();
										datap = new Data(outbuf.length + 8, seqno, outbuf);
										outbuf = serialize(datap);
										q.add(outbuf);
									}
									outbuf = new byte[500];
								}
							}
							boolean first = true;
							seqno = 1;
							int start = seqno;
							Thread window[] = new Thread[windowSize];
							while (!q.isEmpty()) {

								time = timeout(sample, devrtt, estimated);
								devrtt = time[1];
								estimated = time[2];
								now = System.currentTimeMillis();
								if (first)
									for (int i = 0; i < windowSize; i++) {
										outbuf = q.get(seqno - start);
										window[i] = new sendingThread(outbuf, time[0], source_address, source_port,
												seqno);
										window[i].start();
										seqno++;
									}
								else {
									if (window[0].isAlive()) {

										continue;
									} else {
										for (int i = 1; i < windowSize; i++) {
											try {
												if (window[i].isAlive()) {
													int temp = 0;
													for (int I = i; I < windowSize; I++) {
														window[temp] = window[I];
														temp++;

													}
													for (int I = temp; I < windowSize; I++) {
														if (seqno - start < q.size()) {
															outbuf = q.get(seqno - start);
															window[I] = new sendingThread(outbuf, time[0],
																	source_address, source_port, seqno);
															window[I].start();
															seqno++;
														}
													}

													break;
												}

												if (i == windowSize - 1) {
													for (int I = 0; I < windowSize; I++) {
														if (seqno - start < q.size()) {
															outbuf = q.get(seqno - start);
															window[I] = new sendingThread(outbuf, time[0],
																	source_address, source_port, seqno);
															window[I].start();
															seqno++;
														}
													}
												}

											} catch (ArrayIndexOutOfBoundsException exception) {
												break;
											}
										}

									}

								}
								first = false;
								sample = System.currentTimeMillis() - now;
								if (seqno - start == q.size()) {
									break;
								}
							}
							stringbuild = new StringBuilder();
						}
					}
					if (buffer.readLine() == null)
						System.out.println("file read successful");

					socket.close();
				} catch (IOException e) {
					System.out.println(e);
				}
			}

		}

		catch (Exception e) {
			e.printStackTrace();
			System.out.println("Erroooooor");
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

	public long[] timeout(long sample, long devrtt, long estimated) {

		long alpha = (long) 0.125;
		long beta = (long) 0.25;
		long[] times = new long[3];

		estimated = alpha * sample + (1 - alpha) * estimated;
		devrtt = (1 - beta) * devrtt + beta * Math.abs(sample - estimated);
		times[0] = estimated + 4 * devrtt;
		times[1] = estimated;
		times[2] = devrtt;
		return times;

	}
}
