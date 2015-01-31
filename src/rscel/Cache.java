package rscel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;

import rscel.io.Buffer;

public final class Cache {
	
	public static final int N_FILE_STORES = 5;
	
	public static final int FS_FILE_STORE = 0;
	public static final int MODEL_FILE_STORE = 1;
	public static final int ANIM_FILE_STORE = 2;
	public static final int MIDI_FILE_STORE = 3;
	public static final int MAP_FILE_STORE = 4;
	
	public static final int N_FILE_SYSTEMS = 9;
	
	public static final int TITLE_FILE_SYSTEM = 1;
	public static final int CONFIG_FILE_SYSTEM = 2;
	public static final int INTERFACE_FILE_SYSTEM = 3;
	public static final int MEDIA_FILE_SYSTEM = 4;
	public static final int VERSIONLIST_FILE_SYSTEM = 5;
	public static final int TEXTURES_FILE_SYSTEM = 6;
	public static final int WORDENC_FILE_SYSTEM = 7;
	public static final int SOUNDS_FILE_SYSTEM = 8;
	
	private final String dir;
	private final RandomAccessFile dat;
	
	private final Map<Integer, FileStore> stores = new HashMap<>();
	private final Map<Integer, FileSystem> systems = new HashMap<>();
	
	private final byte[] buffer = new byte[520];
	
	public Cache(String dir) {
		dir = formatDir(dir);
		File dirf = new File(dir);
		if (!dirf.exists()) {
			throw new RuntimeException("dir does not exist");
		} else if (!dirf.isDirectory()) {
			throw new RuntimeException("dir is not valid");
		}
		this.dir = dir;
		String path = datPath(dir);
		try {
			dat = new RandomAccessFile(path, "rw");
		} catch (FileNotFoundException e) {
			throw new RuntimeException("unable to access file: " + path);
		}
	}
	
	public FileStore getFileStore(int id) {
		if (id < 0 || id >= 0xff) {
			throw new IllegalArgumentException("id out of range: " + id);
		}
		FileStore fs = stores.get(id);
		if (fs != null) {
			return fs;
		}
		synchronized (this) {
			fs = stores.get(id);
			if (fs != null) {
				return fs;
			}
			String path = idxPath(dir, id);
			File file = new File(path);
			if (!file.exists()) {
				throw new IllegalStateException("file store does not exist");
			}
			try {
				RandomAccessFile idx = new RandomAccessFile(path, "rw");
				fs = new FileStore(id, dat, idx, id != FS_FILE_STORE, buffer);
				stores.put(id, fs);
				return fs;
			} catch (FileNotFoundException e) {
				throw new RuntimeException("unable to access file: " + path);
			}
		}
	}
	
	public FileStore createFileStore(int id) {
		if (id < 0 || id >= 0xff) {
			throw new IllegalArgumentException("id out of range: " + id);
		}
		synchronized (this) {
			boolean exists = false;
			if (stores.containsKey(id)) {
				exists = true;
			} else {
				File file = new File(idxPath(dir, id));
				if (file.exists()) {
					exists = true;
				}
			}
			if (exists) {
				throw new IllegalStateException("file store already exists");
			}
			String path = idxPath(dir, id);
			try {
				RandomAccessFile idx = new RandomAccessFile(path, "rw");
				FileStore fs = new FileStore(id, dat, idx, id != FS_FILE_STORE, buffer);
				stores.put(id, fs);
				return fs;
			} catch (FileNotFoundException e) {
				throw new RuntimeException("unable to access file: " + path);
			}
		}
	}
	
	public FileStore getOrCreateFileStore(int id) {
		if (id < 0 || id >= 0xff) {
			throw new IllegalArgumentException("id out of range: " + id);
		}
		FileStore fs = stores.get(id);
		if (fs != null) {
			return fs;
		}
		synchronized (this) {
			fs = stores.get(id);
			if (fs != null) {
				return fs;
			}
			String path = idxPath(dir, id);
			try {
				RandomAccessFile idx = new RandomAccessFile(path, "rw");
				fs = new FileStore(id, dat, idx, id != FS_FILE_STORE, buffer);
				stores.put(id, fs);
				return fs;
			} catch (FileNotFoundException e) {
				throw new RuntimeException("unable to access file: " + path);
			}
		}
	}
	
	public boolean containsFileStore(int id) {
		try {
			getFileStore(id);
		} catch (RuntimeException e) {
			return false;
		}
		return true;
	}
	
	public FileSystem getFileSystem(int id) {
		FileSystem fs = systems.get(id);
		if (fs != null) {
			return fs;
		}
		synchronized (this) {
			fs = systems.get(id);
			if (fs != null) {
				return fs;
			}
			FileStore store = getOrCreateFileStore(FS_FILE_STORE);
			byte[] data = null;
			try {
				data = store.readFile(id);
			} catch (Exception e) {
			}
			if (data == null) {
				throw new IllegalStateException("file system does not exist");
			}
			fs = new FileSystem(id, store, data);
			systems.put(id, fs);
			return fs;
		}
	}
	
	public FileSystem createFileSystem(int id) {
		synchronized (this) {
			if (systems.containsKey(id)) {
				throw new IllegalStateException("file system already exists");
			}
			FileStore store = getOrCreateFileStore(FS_FILE_STORE);
			byte[] data = null;
			try {
				data = store.readFile(id);
			} catch (Exception e) {
			}
			if (data != null) {
				FileSystem fs = new FileSystem(id, store, data);
				systems.put(id, fs);
				throw new IllegalStateException("file system already exists");
			}
			data = emptyFileSystemData();
			store.writeFile(id, data);
			FileSystem fs = new FileSystem(id, store, data);
			systems.put(id, fs);
			return fs;
		}
	}
	
	public FileSystem getOrCreateFileSystem(int id) {
		FileSystem fs = systems.get(id);
		if (fs != null) {
			return fs;
		}
		synchronized (this) {
			fs = systems.get(id);
			if (fs != null) {
				return fs;
			}
			FileStore store = getOrCreateFileStore(FS_FILE_STORE);
			byte[] data = null;
			try {
				data = store.readFile(id);
			} catch (Exception e) {
			}
			if (data == null) {
				data = emptyFileSystemData();
				store.writeFile(id, data);
			}
			fs = new FileSystem(id, store, data);
			systems.put(id, fs);
			return fs;
		}
	}
	
	private static byte[] emptyFileSystemData() {
		Buffer buf = new Buffer(8);
		buf.putMedium(2);
		buf.putMedium(2);
		buf.putShort(0);
		return buf.getData();
	}
	
	public boolean containsFileSystem(int id) {
		try {
			getFileSystem(id);
		} catch (RuntimeException e) {
			return false;
		}
		return true;
	}
	
	private static String formatDir(String dir) {
		return (dir + '/').replaceAll("\\+|/+", "/");
	}
	
	private static String datPath(String dir) {
		return dir + "main_file_cache.dat";
	}
	
	private static String idxPath(String dir, int id) {
		return dir + "main_file_cache.idx" + id;
	}
}
