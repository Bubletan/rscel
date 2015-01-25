package rscel.codec.zip;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

import rscel.codec.Decoder;

public final class GZipDecoder implements Decoder<byte[]> {
	
	@Override
	public byte[] decode(byte[] data) {
		try {
			GZIPInputStream gzis = new GZIPInputStream(new ByteArrayInputStream(data));
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			byte[] buf = new byte[256];
			int len;
			while ((len = gzis.read(buf)) != -1) {
				baos.write(buf, 0, len);
			}
			baos.close();
			return baos.toByteArray();
		} catch (IOException e) {
			throw new RuntimeException("error unzipping");
		}
	}
}
