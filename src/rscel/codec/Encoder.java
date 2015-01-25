package rscel.codec;

@FunctionalInterface
public interface Encoder<T> {
	
	public byte[] encode(T t);
}
