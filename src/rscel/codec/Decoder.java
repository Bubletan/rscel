package rscel.codec;

@FunctionalInterface
public interface Decoder<T> {
	
	public T decode(byte[] data);
}
