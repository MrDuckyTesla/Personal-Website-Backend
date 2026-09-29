import java.util.Map;
import java.util.TreeMap;

import engine.data.serializations.FastSerializable;
import engine.data.util.ByteHelper;

public class Database<T extends FastSerializable<T>> {
	
	private final TreeMap<String, T> database;

	public Database() {
		this.database = new TreeMap<String, T>();
	}
	
	public T get(String key) {
		return this.database.get(key);
	}
	
	public boolean put(String key, T payload) {
		return this.database.put(key, payload) != null;
	}
	
	public boolean remove(String key) {
		return this.database.remove(key) != null;
	}
	
	public int size() {
		return this.database.size();
	}
	
	public String firstKey() {
		return this.database.firstKey();
	}
	
	public String lastKey() {
		return this.database.lastKey();
	}

	public byte[] serialize() {
		byte[][] bytes = new byte[database.size()][]; int i = 0;
		for (Map.Entry<String, T> entry : database.entrySet()) {
			bytes[i++] = ByteHelper.mergeBytes(ByteHelper.toBytes(entry.getKey()), ByteHelper.toBytes(entry.getValue()));
		} return ByteHelper.mergeBytes(bytes);
	}

	@SuppressWarnings("unchecked")
	public Database<T> deserialize(ByteHelper b, FastSerializable<?>... prototypes) {
		Database<T> d = new Database<>();
		while (b.hasRemaining()) {d.database.put(b.readString(), b.readObject((T) prototypes[0]));}
		return d;
	}

}
