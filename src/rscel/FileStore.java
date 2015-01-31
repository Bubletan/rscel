package rscel;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Consumer;

import rscel.util.ZipUtils;

public final class FileStore {
	
	private static final Queue<Object> entries = new LinkedList<>();
	private static final Thread thread = new Thread(() -> {
		do {
			Object obj;
			synchronized (entries) {
				if (entries.isEmpty()) {
					running = false;
					break;
				}
				obj = entries.remove();
			}
			if (obj instanceof ReadEntry) {
				ReadEntry e = (ReadEntry) obj;
				e.listener.accept(e.store.readFile(e.id));
			} else {
				WriteEntry e = (WriteEntry) obj;
				e.store.writeFile(e.id, e.data);
			}
		} while (true);
	});
	static {
		thread.setPriority(2);
	}
	private static boolean running;
	
	private final int id;
	private final RandomAccessFile dat;
	private final RandomAccessFile idx;
	private final boolean gzip;
	private final byte[] buffer;
	
	protected FileStore(int id, RandomAccessFile dat, RandomAccessFile idx, boolean gzip, byte[] buffer) {
		this.id = id + 1;
		this.dat = dat;
		this.idx = idx;
		this.gzip = gzip;
		this.buffer = buffer;
	}
	
	public void readFileInBackground(int id, Consumer<byte[]> listener) {
		synchronized (entries) {
			entries.add(new ReadEntry(this, id, listener));
			if (!running) {
				running = true;
				thread.start();
			}
		}
	}
	
	private static final class ReadEntry {
		
		private final FileStore store;
		private final int id;
		private final Consumer<byte[]> listener;
		
		private ReadEntry(FileStore store, int id, Consumer<byte[]> listener) {
			this.store = store;
			this.id = id;
			this.listener = listener;
		}
	}
	
	public byte[] readFile(int id) {
		byte[] buf;
		synchronized (buffer) {
			try {
				seek(idx, id * 6);
				int fileSize;
				for (int i = 0; i < 6; i += fileSize) {
					fileSize = idx.read(buffer, i, 6 - i);
					if (fileSize == -1) {
						return null;
					}
				}
				fileSize = ((buffer[0] & 0xff) << 16)
						+ ((buffer[1] & 0xff) << 8) + (buffer[2] & 0xff);
				int frag = ((buffer[3] & 0xff) << 16)
						+ ((buffer[4] & 0xff) << 8) + (buffer[5] & 0xff);
				if (fileSize < 0) {
					return null;
				}
				if (frag <= 0 || frag > dat.length() / 520L) {
					return null;
				}
				buf = new byte[fileSize];
				int nRead = 0;
				int fragCount = 0;
				while (nRead < fileSize) {
					if (frag == 0) {
						return null;
					}
					seek(dat, frag * 520);
					int size = 0;
					int nToRead = fileSize - nRead;
					if (nToRead > 512) {
						nToRead = 512;
					}
					int fileId;
					for (; size < nToRead + 8; size += fileId) {
						fileId = dat.read(buffer, size, nToRead + 8 - size);
						if (fileId == -1) {
							return null;
						}
					}
					fileId = ((buffer[0] & 0xff) << 8) + (buffer[1] & 0xff);
					int fragId = ((buffer[2] & 0xff) << 8) + (buffer[3] & 0xff);
					int nextFrag = ((buffer[4] & 0xff) << 16)
							+ ((buffer[5] & 0xff) << 8) + (buffer[6] & 0xff);
					int nextStoreId = buffer[7] & 0xff;
					if (fileId != id || fragId != fragCount
							|| nextStoreId != this.id) {
						return null;
					}
					if (nextFrag < 0 || nextFrag > dat.length() / 520L) {
						return null;
					}
					for (int i = 0; i < nToRead; i++) {
						buf[nRead++] = buffer[i + 8];
					}
					frag = nextFrag;
					fragCount++;
				}
			} catch (IOException e) {
				return null;
			}
		}
		if (gzip) {
			buf = ZipUtils.ungzip(buf);
		}
		return buf;
	}
	
	public void writeFileInBackground(int id, byte[] data) {
		synchronized (entries) {
			entries.add(new WriteEntry(this, id, data));
			if (!running) {
				running = true;
				thread.start();
			}
		}
	}
	
	private static final class WriteEntry {
		
		private final FileStore store;
		private final int id;
		private final byte[] data;
		
		private WriteEntry(FileStore store, int id, byte[] data) {
			this.store = store;
			this.id = id;
			this.data = data;
		}
	}
	
	public boolean writeFile(int id, byte[] data) {
		if (data == null) {
			throw new IllegalArgumentException("data == null");
		}
		if (gzip) {
			data = ZipUtils.gzip(data);
		}
		return writeFile(id, data, data.length);
	}
	
	private boolean writeFile(int id, byte[] buf, int len) {
		synchronized (buffer) {
			boolean success = writeFile(id, buf, len, true);
			if (!success) {
				success = writeFile(id, buf, len, false);
			}
			return success;
		}
	}
	
	private boolean writeFile(int id, byte[] buf, int len, boolean first) {
		try {
			int sector;
			if (first) {
				seek(idx, id * 6);
				int size;
				for (int i = 0; i < 6; i += size) {
					size = idx.read(buffer, i, 6 - i);
					if (size == -1) {
						return false;
					}
				}
				sector = ((buffer[3] & 0xff) << 16) + ((buffer[4] & 0xff) << 8)
						+ (buffer[5] & 0xff);
				if (sector <= 0 || sector > dat.length() / 520L) {
					return false;
				}
			} else {
				sector = (int) ((dat.length() + 519L) / 520L);
				if (sector == 0) {
					sector = 1;
				}
			}
			buffer[0] = (byte) (len >> 16);
			buffer[1] = (byte) (len >> 8);
			buffer[2] = (byte) len;
			buffer[3] = (byte) (sector >> 16);
			buffer[4] = (byte) (sector >> 8);
			buffer[5] = (byte) sector;
			seek(idx, id * 6);
			idx.write(buffer, 0, 6);
			int written = 0;
			for (int zero = 0; written < len; zero++) {
				int nextSector = 0;
				if (first) {
					seek(dat, sector * 520);
					int currentFile;
					int idx;
					for (idx = 0; idx < 8; idx += currentFile) {
						currentFile = dat.read(buffer, idx, 8 - idx);
						if (currentFile == -1) {
							break;
						}
					}
					if (idx == 8) {
						currentFile = ((buffer[0] & 0xff) << 8)
								+ (buffer[1] & 0xff);
						int currentPart = ((buffer[2] & 0xff) << 8)
								+ (buffer[3] & 0xff);
						nextSector = ((buffer[4] & 0xff) << 16)
								+ ((buffer[5] & 0xff) << 8)
								+ (buffer[6] & 0xff);
						int currentCache = buffer[7] & 0xff;
						if (currentFile != id || currentPart != zero
								|| currentCache != this.id) {
							return false;
						}
						if (nextSector < 0 || nextSector > dat.length() / 520L) {
							return false;
						}
					}
				}
				if (nextSector == 0) {
					first = false;
					nextSector = (int) ((dat.length() + 519L) / 520L);
					if (nextSector == 0) {
						nextSector++;
					}
					if (nextSector == sector) {
						nextSector++;
					}
				}
				if (len - written <= 512) {
					nextSector = 0;
				}
				buffer[0] = (byte) (id >> 8);
				buffer[1] = (byte) id;
				buffer[2] = (byte) (zero >> 8);
				buffer[3] = (byte) zero;
				buffer[4] = (byte) (nextSector >> 16);
				buffer[5] = (byte) (nextSector >> 8);
				buffer[6] = (byte) nextSector;
				buffer[7] = (byte) this.id;
				seek(dat, sector * 520);
				dat.write(buffer, 0, 8);
				int remaining = len - written;
				if (remaining > 512) {
					remaining = 512;
				}
				dat.write(buf, written, remaining);
				written += remaining;
				sector = nextSector;
			}
			return true;
		} catch (IOException e) {
			return false;
		}
	}
	
	private void seek(RandomAccessFile file, int pos) throws IOException {
		if (pos < 0 || pos > 62914560) {
			System.err.println("bad seek: pos=" + pos + " len=" + file.length());
			pos = 62914560;
			try {
				Thread.sleep(1000L);
			} catch (Exception e) {
			}
		}
		file.seek(pos);
	}
	
	public int getFileCount() {
		synchronized (buffer) {
			try {
				return (int) (idx.length() / 6);
			} catch (IOException e) {
				throw new RuntimeException("error getting file count");
			}
		}
	}
}
