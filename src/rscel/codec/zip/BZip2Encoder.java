package rscel.codec.zip;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import rscel.codec.Encoder;
import rscel.deps.bzip2.CBZip2OutputStream;

public final class BZip2Encoder implements Encoder<byte[]> {
	
	@Override
	public byte[] encode(byte[] data) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
	        CBZip2OutputStream cbz2os = new CBZip2OutputStream(baos, 1);
	        cbz2os.write(data);
	        cbz2os.close();
	        return baos.toByteArray();
		} catch (IOException e) {
			throw new RuntimeException("error zipping");
		}
	}
}
