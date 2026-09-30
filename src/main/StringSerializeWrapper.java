package main;

import engine.data.serializations.FastSerializable;
import engine.data.util.ByteHelper;

public class StringSerializeWrapper implements FastSerializable<StringSerializeWrapper> {
	
	private final String payload;

	public StringSerializeWrapper(String payload) {
		this.payload = payload;
	}
	
	public String get() {return this.payload;}

	@Override
	public byte[] serialize() {
		return ByteHelper.toBytes(payload);
		
	}

	@Override 
	public StringSerializeWrapper deserialize(ByteHelper b, FastSerializable<?>... prototypes) {
		return new StringSerializeWrapper(b.readString());
	}

	@Override 
	public StringSerializeWrapper[] getProtoArray(int length) {
		return new StringSerializeWrapper[length];
	}


}
