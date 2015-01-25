package rscel.util;

import java.util.zip.CRC32;

import rscel.codec.zip.BZip2Decoder;
import rscel.codec.zip.BZip2Encoder;
import rscel.codec.zip.GZipDecoder;
import rscel.codec.zip.GZipEncoder;

public final class ZipUtils {
	
	private static final CRC32 crc32 = new CRC32();
	
	private static final GZipEncoder GZIP_ENCODER = new GZipEncoder();
	private static final GZipDecoder GZIP_DECODER = new GZipDecoder();
	private static final BZip2Encoder BZIP2_ENCODER = new BZip2Encoder();
	private static final BZip2Decoder BZIP2_DECODER = new BZip2Decoder();
	
	private ZipUtils() {
	}
	
	public static int crc(byte[] data) {
		synchronized (crc32) {
			crc32.reset();
			crc32.update(data);
			return (int) crc32.getValue();
		}
	}
	
	public static byte[] gzip(byte[] data) {
		return GZIP_ENCODER.encode(data);
	}
	
	public static byte[] ungzip(byte[] data) {
		return GZIP_DECODER.decode(data);
	}
	
	public static byte[] bzip2(byte[] data) {
		return BZIP2_ENCODER.encode(data);
	}
	
	public static byte[] unbzip2(byte[] data) {
		return BZIP2_DECODER.decode(data);
	}
}
