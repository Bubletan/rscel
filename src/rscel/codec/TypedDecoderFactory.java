package rscel.codec;

public final class TypedDecoderFactory<T> {
	
	private final Class<Decoder<T>> type;
	
	public TypedDecoderFactory(Class<Decoder<T>> type) {
		this.type = type;
	}
	
	public Decoder<T> newDecoder() {
		try {
			return type.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new RuntimeException("error creating decoder");
		}
	}
}
