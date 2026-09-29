import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.UUID;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import engine.data.serializations.FastSerializable;
import engine.data.util.ByteHelper;

public class User implements FastSerializable<User> {
	
	private final String username;
	private final String id;
	private final byte[] pwHash;
	private final long timeCreated;
	
	private String githubID = "";
	private String authKey = "";
	private String email = "";
	
	private long lastLogin;
	private long lastMessage;
	private long lastThread;
	private short powerLevel;

	public User(String username, String pw) throws RuntimeException {
		this.username = username;
		
		byte[] salt = new byte[16];
		new SecureRandom().nextBytes(salt);
		
		this.pwHash = this.hashPw(pw, salt);
		this.id = UUID.randomUUID().toString();
		this.timeCreated = System.currentTimeMillis();
		this.lastMessage = -1L;
		this.lastThread = -1L;
		this.powerLevel = 0;
	}
	
	public User(String username, String id, byte[] pwHash, long timeCreated) {
		this.username = username;
		this.id = id;
		this.pwHash = pwHash;
		this.timeCreated = timeCreated;
	}
	
	private byte[] hashPw(String password, byte[] salt) throws RuntimeException {
		try {
			PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 600_000, 256);
			byte[] hash = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
			return ByteHelper.mergeBytes(salt, hash);
		} catch (InvalidKeySpecException | NoSuchAlgorithmException e) {e.printStackTrace();}
		throw new RuntimeException();
	}
	
	public boolean passwordsMatch(String password) {
		return MessageDigest.isEqual(this.hashPw(password, Arrays.copyOf(this.pwHash, 16)), this.pwHash);
	}
	
	public boolean authKeyMatch(String authKey) {
		if (this.authKey == null || authKey == null) {return false;}
		long curr = System.currentTimeMillis();
		if (MessageDigest.isEqual(this.authKey.getBytes(StandardCharsets.UTF_8), authKey.getBytes(StandardCharsets.UTF_8))) {
			if (curr - this.lastLogin >= 43200000L) {
				this.authKey = UUID.randomUUID().toString(); 
				this.lastLogin = curr; return false;
			} return true;
		} return false;
	}
	
	public void createAuthKey() {
		this.authKey = UUID.randomUUID().toString();
		this.lastLogin = System.currentTimeMillis();
	}
	
	public String getUsername() {return this.username;}
	public String getEmail() {return this.email;}
	public String getUserID() {return this.id;}
	public String getUserAuth() {return this.authKey;}
	public String getGithubID() {return this.githubID;}
	
	public void setGithubID(String githubID) {
		this.githubID = githubID;
	}
	
	public void setEmail(String email) {
		this.email = email;
	}

	@Override
	public byte[] serialize() {
		return ByteHelper.mergeBytes(
			ByteHelper.toBytes(this.username), 
			ByteHelper.toBytes(this.id),
			ByteHelper.toBytes(this.pwHash),
			ByteHelper.toBytes(this.timeCreated),
			ByteHelper.toBytes(this.githubID),
			ByteHelper.toBytes(this.authKey),
			ByteHelper.toBytes(this.email),  
			ByteHelper.toBytes(this.lastLogin),
			ByteHelper.toBytes(this.lastMessage),
			ByteHelper.toBytes(this.lastThread),
			ByteHelper.toBytes(this.powerLevel)
		);
	}

	@Override
	public User deserialize(ByteHelper b, FastSerializable<?>... prototypes) {
		User u = new User(
			b.readString(), 
			b.readString(), 
			b.readByteArr(),
			b.readLong()
		); 
		u.githubID = b.readString();
		u.authKey = b.readString();
		u.email = b.readString();
		u.lastLogin = b.readLong();
		u.lastMessage = b.readLong();
		u.lastThread = b.readLong();
		u.powerLevel = b.readShort();
		return u;
	}

	@Override
	public User[] getProtoArray(int length) {
		return new User[length];
	}
	
	public String toString() {return "username=" + this.username + "&email=" + this.email;}

}
