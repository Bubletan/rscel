# RSCEL

RSCEL is a lightweight RuneScape cache editing library for the revision 317. It aims for providing an easy-to-use API to access and edit the files in the cache directly.

## Examples

Accessing a cache:

```java
String dir = System.getProperty("user.home") + ".myCache/";
Cache cache = new Cache(dir);
```

Using a file store:

```java
FileStore fs = cache.getFileStore(Cache.MODEL_FILE_STORE);
byte[] data = fs.readFile(123);
fs.writeFile(123, data);
```

Using a file system:

```java
FileSystem fs = cache.getFileSystem(Cache.CONFIG_FILE_SYSTEM);
byte[] data = fs.readFile("flo.dat");
fs.writeFile("flo.dat", data);
```

Using background IO with file stores:

```java
fs.readFileInBackground(123, data -> {
    // do something with data
});
fs.writeFileInBackground(123, data);
```

## Downloads

* [rscel-2.1.0.jar @ MediaFire](http://www.mediafire.com/download/a8ucepckowtfeer/rscel-2.1.0.jar)
* [rscel-2.0.0.jar @ MediaFire](http://www.mediafire.com/download/6mgfokzl4odfgbd/rscel-2.0.0.jar)
* [rscel-1.0.0.jar @ MediaFire](http://www.mediafire.com/download/x1qwu3klctzdkzs/rscel-1.0.0.jar)