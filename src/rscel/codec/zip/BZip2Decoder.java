package rscel.codec.zip;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import rscel.codec.Decoder;
import rscel.deps.bzip2.CBZip2InputStream;

public final class BZip2Decoder implements Decoder<byte[]> {
	
	@Override
	public byte[] decode(byte[] data) {
		try {
			CBZip2InputStream cbz2is = new CBZip2InputStream(new ByteArrayInputStream(data));
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			byte[] buf = new byte[256];
			int len;
			while ((len = cbz2is.read(buf)) != -1) {
				baos.write(buf, 0, len);
			}
			cbz2is.close();
			baos.close();
			return baos.toByteArray();
		} catch (IOException e) {
			throw new RuntimeException("error unzipping");
		}
	}
}
