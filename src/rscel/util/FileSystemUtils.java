package rscel.util;

public final class FileSystemUtils {
	
	private FileSystemUtils() {
	}
	
	public static int fileNameToHash(String name) {
		int hash = 0;
		name = name.toUpperCase();
		for (int i = 0; i < name.length(); i++) {
			hash = (hash * 61 + name.charAt(i)) - 32;
		}
		return hash;
	}
}