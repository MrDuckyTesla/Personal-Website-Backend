package main;

import java.util.UUID;

import engine.data.serializations.FastSerializable;
import engine.data.util.ByteHelper;

public class Comment implements FastSerializable<Comment> {
	
	private final String userID;
	
	private final String id;
	private final String title;
	private final String message;
	
	private final long timeStamp;

	public Comment(User user, String title, String message) {
		this.userID = user.getUsername();
		this.title = title;
		this.message = message;
		this.id = UUID.randomUUID().toString();
		this.timeStamp = System.currentTimeMillis();
	}
	
	public Comment(String userID, String title, String message, String id, long timestamp) {
		this.userID = userID;
		this.title = title;
		this.message = message;
		this.id = id;
		this.timeStamp = timestamp;
	}
	
	public String getID() {return this.id;}
	public String getTitle() {return this.title;}
	public String getMessage() {return this.message;}
	public long getTimeStamp() {return this.timeStamp;}
	
	@Override
	public String toString() {
		return "title="+title+"&message="+message+"&author="+userID+"&time="+timeStamp;
	}

	@Override
	public byte[] serialize() {
		return ByteHelper.mergeBytes(
			ByteHelper.toBytes(this.userID),
			ByteHelper.toBytes(this.title),
			ByteHelper.toBytes(this.message),
			ByteHelper.toBytes(this.id),
			ByteHelper.toBytes(this.timeStamp)
		);
	}

	@Override
	public Comment deserialize(ByteHelper b, FastSerializable<?>... prototypes) {
		return new Comment(
			b.readString(), 
			b.readString(), 
			b.readString(), 
			b.readString(), 
			b.readLong()
		);
	}

	@Override
	public Comment[] getProtoArray(int length) {
		return new Comment[length];
	}

}
