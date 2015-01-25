package rscel.codec;

public final class TypedEncoderFactory<T> {
	
	private final Class<Encoder<T>> type;
	
	public TypedEncoderFactory(Class<Encoder<T>> type) {
		this.type = type;
	}
	
	public Encoder<T> newEncoder() {
		try {
			return type.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new RuntimeException("error creating encoder");
		}
	}
}
