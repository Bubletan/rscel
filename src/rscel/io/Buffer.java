package rscel.io;

import java.nio.ByteBuffer;

public final class Buffer {
	
	private ByteBuffer buf;
	
	public Buffer() {
		buf = ByteBuffer.allocate(32);
	}
	
	public Buffer(int capacity) {
		if (capacity < 0) {
			throw new IllegalArgumentException("capacity < 0");
		}
		buf = ByteBuffer.allocate(capacity);
	}
	
	public Buffer(byte[] data) {
		if (data == null) {
			throw new IllegalArgumentException("data == null");
		}
		buf = ByteBuffer.wrap(data);
	}
	
	public int getPosition() {
		return buf.position();
	}
	
	public void setPosition(int pos) {
		buf.position(pos);
	}
	
	public int getCapacity() {
		return buf.capacity();
	}
	
	public byte[] getData() {
		return buf.array();
	}
	
	public byte[] getShrunkData() {
		byte[] bytes = new byte[buf.position()];
		System.arraycopy(buf.array(), 0, bytes, 0, bytes.length);
		return bytes;
	}
	
	private void ensureCapacity(int n) {
		if (buf.remaining() >= n) {
			return;
		}
		int used = buf.capacity() - buf.remaining();
		int newCap = buf.capacity() << 1;
		if (newCap == 0) {
			newCap = 1;
		}
		while (newCap - used < n) {
			newCap += buf.capacity();
		}
		ByteBuffer newBuf = ByteBuffer.allocate(newCap);
		buf.flip();
		newBuf.put(buf);
		buf = newBuf;
	}
	
	public void putByte(int value) {
		ensureCapacity(1);
		buf.put((byte) value);
	}
	
	public void putShort(int value) {
		ensureCapacity(2);
		buf.putShort((short) value);
	}
	
	public void putMedium(int value) {
		ensureCapacity(3);
		buf.put((byte) (value >> 16)).put((byte) (value >> 8)).put((byte) value);
	}
	
	public void putInt(int value) {
		ensureCapacity(4);
		buf.putInt(value);
	}
	
	public void putLong(long value) {
		ensureCapacity(8);
		buf.putLong(value);
	}
	
	public void putLine(String value) {
		ensureCapacity(value.length() + 1);
		buf.put(value.getBytes()).put((byte) 10);
	}
	
	public void putString(String value) {
		ensureCapacity(value.length() + 1);
		buf.put(value.getBytes()).put((byte) 0);
	}
	
	public void putBytes(byte[] src, int pos, int len) {
		ensureCapacity(len);
		buf.put(src, pos, len);
	}
	
	public byte getByte() {
		return buf.get();
	}
	
	public int getUByte() {
		return buf.get() & 0xff;
	}
	
	public short getShort() {
		return buf.getShort();
	}
	
	public int getUShort() {
		return buf.getShort() & 0xffff;
	}
	
	public int getSmart() {
		int i = buf.get() & 0xff;
		if (i < 0x80) {
			return i - 0x40;
		} else {
			return (i << 8) + (buf.get() & 0xff) - 0xc000;
		}
	}
	
	public int getUSmart() {
		int i = buf.get() & 0xff;
		if (i < 0x80) {
			return i;
		} else {
			return (i << 8) + (buf.get() & 0xff) - 0x8000;
		}
	}
	
	public int getMedium() {
		int value = ((buf.get() & 0xff) << 16) | ((buf.get() & 0xff) << 8) | (buf.get() & 0xff);
		if (value > 0x7fffff) {
			value -= 0x1000000;
		}
		return value;
	}
	
	public int getUMedium() {
		return ((buf.get() & 0xff) << 16) | ((buf.get() & 0xff) << 8) | (buf.get() & 0xff);
	}
	
	public int getInt() {
		return buf.getInt();
	}
	
	public long getLong() {
		return buf.getLong();
	}
	
	public String getLine() {
		StringBuilder sb = new StringBuilder();
		byte b;
		while (buf.hasRemaining() && (b = buf.get()) != 10) {
			sb.append((char) b);
		}
		return sb.toString();
	}
	
	public String getString() {
		StringBuilder sb = new StringBuilder();
		byte b;
		while (buf.hasRemaining() && (b = buf.get()) != 0) {
			sb.append((char) b);
		}
		return sb.toString();
	}
	
	public void getBytes(byte[] dst, int pos, int len) {
		buf.get(dst, pos, len);
	}
}
