package main;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.AbstractList;

import engine.data.serializations.FastSerializable;
import engine.data.util.ByteHelper;

public class SerializedArray<T extends FastSerializable<T>> extends AbstractList<T> {

	private final int lengthChunk, size = 0;
	private final String folderPath;
	
	public SerializedArray(String folderPath, int lengthChunk) {
		this.folderPath = folderPath; 
		this.lengthChunk = lengthChunk;
	}

	@Override
	public boolean add(T object) {
		String file = "";
		
		try {
			Files.write(
				Paths.get(this.folderPath+"/chunk"+".mdb"), 
				object.serialize(), 
				StandardOpenOption.CREATE, 
				StandardOpenOption.APPEND
			);
		} 
		catch (IOException e) {e.printStackTrace();}
		return false;
	}
	
	@Override
	public T set(int index, T object) {
		return null;
	}

	@Override
	public T get(int index) {
		return null;
	}
	
	@Override
	public int size() {
		return this.size;
	}

}
