package rscel.codec.zip;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

import rscel.codec.Encoder;

public final class GZipEncoder implements Encoder<byte[]> {
	
	@Override
	public byte[] encode(byte[] data) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			GZIPOutputStream gzos = new GZIPOutputStream(baos);
			gzos.write(data);
			gzos.close();
			return baos.toByteArray();
		} catch (IOException e) {
			throw new RuntimeException("error zipping");
		}
	}
}
