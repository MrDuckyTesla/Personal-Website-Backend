package main;

import java.util.UUID;

import engine.data.serializations.FastSerializable;
import engine.data.util.ByteHelper;

public class State implements FastSerializable<State> {
	
	private final User user;
	private String id;

	public State(User user) {
		this.user = user;
		this.id = UUID.randomUUID().toString();
	}
	
	public User getUser() {return this.user;}
	public String getID() {return this.id;}

	@Override
	public byte[] serialize() {
		return ByteHelper.mergeBytes(
			ByteHelper.toBytes(user), 
			ByteHelper.toBytes(id)
		);
	}

	@Override
	public State deserialize(ByteHelper b, FastSerializable<?>... prototypes) {
		State s = new State((User) b.readObject((prototypes[0])));
		s.id = b.readString(); return s;
	}

	@Override
	public State[] getProtoArray(int length) {
		return new State[length];
	}

}
