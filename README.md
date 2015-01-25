# RSCEL

RSCEL is a lightweight RuneScape cache editing library for the revision 317. It aims for providing an easy-to-use API to access and edit the files in the cache directly.

## Examples

Accessing a cache:

	String dir = System.getProperty("user.home") + ".myCache/";
	Cache cache = new Cache(dir);
	
Using a file store:
	
	FileStore fs = cache.getFileStore(Cache.MODEL_FILE_STORE);
	byte[] data = fs.readGZipFile(123);
	fs.writeGZipFile(123, data);
	
Using a file system:

	FileSystem fs = cache.getFileSystem(Cache.CONFIG_FILE_SYSTEM);
	byte[] data = fs.readFile("flo.dat");
	fs.writeFile("flo.dat", data);

## Downloads

* [rscel-1.0.0.jar @ MediaFire](http://www.mediafire.com/download/x1qwu3klctzdkzs/rscel-1.0.0.jar)