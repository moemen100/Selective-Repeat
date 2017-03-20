package network;

import java.io.Serializable;

public class ack implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int len;
	private int ackno;
	public ack(int len,int ackno) {
     this.setLen(len);
     this.setAckno(ackno);
	}
	public int getAckno() {
		return ackno;
	}
	public void setAckno(int ackno) {
		this.ackno = ackno;
	}
	public int getLen() {
		return len;
	}
	public void setLen(int len) {
		this.len = len;
	}

}
