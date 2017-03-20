package network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.Random;

public class sendingThread extends Thread {
	byte[] outbuf;
	long time;
	InetAddress source_address;
	int source_port; 
	int seqno;
	DatagramSocket socket = null;

	public sendingThread(byte[] outbuf,long time, InetAddress source_address, int source_port, int seqno){
     this.outbuf=outbuf;
     this.time=time;
     this.source_address=source_address;
     this.source_port=source_port;
     this.seqno=seqno;		
	}
	
	public void run() {
		DatagramPacket inputPacket = null; // receiving packet
		DatagramPacket outputPacket = null; // sending packet
		byte[] inbuf;
		ack ackn = null;
		Data datap = null;
		System.out.println(  seqno);


		try {
		     socket=new DatagramSocket();
			outputPacket = new DatagramPacket(outbuf, 0, outbuf.length, source_address, source_port);
			Random random = new Random();
			int chance = random.nextInt(100);
			
			if (chance < 90) {
				System.out.println("file packet from the first try");
				socket.send(outputPacket);
			} else {
				System.out.println("file packet not sent");
			}
			
			socket.setSoTimeout((int) time);

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
						System.out.println("resending the file packet");
						socket.send(outputPacket);
					} else {
						System.out.println("file packet lost");

					}

					continue;
				}

				

					break;

			}
			socket.close();
          this.join(1);		
		} catch (Exception e) {
			e.printStackTrace();

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
 public long[] timeout(long sample, long devrtt, long estimated ){
 	
 	 long alpha = (long)0.125;
      long beta = (long) 0.25;
      long[] times = new long[3];

      estimated = alpha * sample + (1 - alpha) * estimated;
      devrtt = (1 - beta) * devrtt + beta * Math.abs(sample - estimated);
      times[0] = estimated + 4 * devrtt;
      times[1] = estimated;
      times[2] = devrtt;
      return  times;

 }
}
