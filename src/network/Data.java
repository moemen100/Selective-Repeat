package network;

import java.io.Serializable;

public class Data implements Serializable {
	
/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
private int len;
private int seqno;
private byte data[];

public Data(int len,int seqno,byte data[]) {
	this.setData(data);
	this.setLen(len);
	this.setSeqno(seqno);
	
}

public int getLen() {
	return len;
}

public void setLen(int len) {
	this.len = len;
}

public int getSeqno() {
	return seqno;
}

public void setSeqno(int seqno) {
	this.seqno = seqno;
}

public byte[] getData() {
	return data;
}

public void setData(byte data[]) {
	this.data = data;
}

}