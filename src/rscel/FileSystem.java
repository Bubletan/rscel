package rscel;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import rscel.deps.bzip2.BZip2Decompressor;
import rscel.io.Buffer;
import rscel.util.FileSystemUtils;
import rscel.util.ZipUtils;

public final class FileSystem {
	
	private final int id;
	private final FileStore fs;
	private final boolean zippedAtOneTime;
	
	private final List<File> files = new LinkedList<>();
	
	private static final class File {
		
		private int hash;
		private int origSize;
		private int onDiskSize;
		private byte[] data;
	}
	
	protected FileSystem(int id, FileStore fs, byte[] data) {
		this.id = id;
		this.fs = fs;
		
		Buffer buf = new Buffer(data);
		int origSize = buf.getUMedium();
		int onDiskSize = buf.getUMedium();
		
		if (origSize != onDiskSize) {
			byte[] tmp = new byte[origSize];
			BZip2Decompressor.decompress(data, 6, tmp, onDiskSize, origSize);
			data = tmp;
			buf = new Buffer(data);
			zippedAtOneTime = true;
		} else {
			zippedAtOneTime = false;
		}
		
		int nFiles = buf.getUShort();
		Buffer altBuf = new Buffer(data);
		altBuf.setPosition(buf.getPosition() + nFiles * 10);
		
		for (int i = 0; i < nFiles; i++) {
			
			File file = new File();
			
			file.hash = buf.getInt();
			file.origSize = buf.getUMedium();
			file.onDiskSize = buf.getUMedium();
			
			file.data = new byte[file.onDiskSize];
			altBuf.getBytes(file.data, 0, file.onDiskSize);
			
			files.add(file);
		}
	}
	
	private synchronized boolean repack() {
		
		int size = 2 + files.size() * 10;
		for (File file : files) {
			size += file.onDiskSize;
		}
		
		Buffer buf;
		if (!zippedAtOneTime) {
			buf = new Buffer(size + 6);
			buf.putMedium(size);
			buf.putMedium(size);
		} else {
			buf = new Buffer(size);
		}
		
		buf.putShort(files.size());
		
		for (File file : files) {
			buf.putInt(file.hash);
			buf.putMedium(file.origSize);
			buf.putMedium(file.onDiskSize);
		}
		
		for (File file : files) {
			buf.putBytes(file.data, 0, file.onDiskSize);
		}
		
		byte[] data;
		if (!zippedAtOneTime) {
			data = buf.getData();
		} else {
			byte[] unzipped = buf.getData();
			byte[] zipped = ZipUtils.bzip2(unzipped);
			if (unzipped.length == zipped.length) {
				throw new RuntimeException("error zipped size matches original");
			}
			buf = new Buffer(zipped.length + 6);
			buf.putMedium(unzipped.length);
			buf.putMedium(zipped.length);
			buf.putBytes(zipped, 0, zipped.length);
			data = buf.getData();
		}
		
		return fs.writeFile(id, data);
	}
	
	public boolean writeFile(String name, byte[] data) {
		return writeFile(FileSystemUtils.fileNameToHash(name), data);
	}
	
	public synchronized boolean writeFile(int hash, byte[] data) {
		File file = null;
		for (File f : files) {
			if (f.hash == hash) {
				file = f;
				break;
			}
		}
		if (file == null) {
			file = new File();
			file.hash = hash;
			files.add(file);
		}
		file.origSize = data.length;
		if (!zippedAtOneTime) {
			data = ZipUtils.bzip2(data);
		}
		file.onDiskSize = data.length;
		file.data = data;
		return repack();
	}
	
	public byte[] readFile(String name) {
		return readFile(FileSystemUtils.fileNameToHash(name));
	}
	
	public synchronized byte[] readFile(int hash) {
		for (File file : files) {
			if (file.hash == hash) {
				byte[] data = new byte[file.origSize];
				if (zippedAtOneTime) {
					System.arraycopy(file.data, 0, data, 0, data.length);
				} else {
					BZip2Decompressor.decompress(file.data, 0, data,
							file.onDiskSize, file.origSize);
				}
				return data;
			}
		}
		throw new RuntimeException("file not found");
	}
	
	public boolean removeFile(String name) {
		return removeFile(FileSystemUtils.fileNameToHash(name));
	}
	
	public synchronized boolean removeFile(int hash) {
		for (Iterator<File> i = files.iterator(); i.hasNext();) {
			File file = i.next();
			if (file.hash == hash) {
				i.remove();
				return repack();
			}
		}
		throw new RuntimeException("file not found");
	}
	
	public boolean containsFile(String name) {
		return containsFile(FileSystemUtils.fileNameToHash(name));
	}
	
	public synchronized boolean containsFile(int hash) {
		for (File file : files) {
			if (file.hash == hash) {
				return true;
			}
		}
		return false;
	}
	
	public synchronized int getFileCount() {
		return files.size();
	}
	
	public synchronized int[] getFileList() {
		int size = files.size();
		int[] a = new int[size];
		for (int i = 0; i < size; i++) {
			a[i] = files.get(i).hash;
		}
		return a;
	}
}
