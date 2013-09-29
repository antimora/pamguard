package PamController;

import java.io.Serializable;
import java.util.ArrayList;

public class StorageParameters implements Cloneable, Serializable {


	public static final long serialVersionUID = 1L;
	
	private ArrayList<StoreChoice> storeChoices = new ArrayList<StoreChoice>();

	@Override
	public StorageParameters clone() {
		try {
			return (StorageParameters) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public void setStorageOptions(String dataName, boolean storeDatabase, boolean storeBinary) {
		StoreChoice storeChoice = findStoreChoice(dataName);
		if (storeChoice == null) {
			storeChoice = new StoreChoice(dataName);
			storeChoices.add(storeChoice);
		}
		storeChoice.binaryStore = storeBinary;
		storeChoice.database = storeDatabase;
	}
	
	public boolean isStoreDatabase(String dataName, boolean def) {
		StoreChoice storeChoice = findStoreChoice(dataName);
		if (storeChoice == null) {
			return def;
		}
		return storeChoice.database;
	}

	public boolean isStoreBinary(String dataName, boolean def) {
		StoreChoice storeChoice = findStoreChoice(dataName);
		if (storeChoice == null) {
			return def;
		}
		return storeChoice.binaryStore;
	}
	
	private StoreChoice findStoreChoice(String dataName) {
		for (int i = 0; i < storeChoices.size(); i++) {
			if (storeChoices.get(i).dataName.equals(dataName)) {
				return storeChoices.get(i);
			}
		}
		return null;
	}

	private class StoreChoice implements Cloneable, Serializable {
		private static final long serialVersionUID = 1L;
		public StoreChoice(String dataName) {
			this.dataName = dataName;
		}
		private String dataName;
		private boolean database;
		private boolean binaryStore;
	}
}
