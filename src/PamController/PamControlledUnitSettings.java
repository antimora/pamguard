/*	PAMGUARD - Passive Acoustic Monitoring GUARDianship.
 * To assist in the Detection Classification and Localisation 
 * of marine mammals (cetaceans).
 *  
 * Copyright (C) 2006 
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package PamController;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class PamControlledUnitSettings implements Serializable {

	private static final long serialVersionUID = 6793059135083717980L; // never change this !!!

	long versionNo;

	private String unitType;

	private String unitName;

	private Object settings;

	//	private PamSettings owner;

	public PamControlledUnitSettings(String unitType, String unitName,
			long versionNo, Object settings) {
		this.versionNo = versionNo;
		this.unitType = unitType;
		this.unitName = unitName;
		this.settings = settings;
	}

	public PamControlledUnitSettings(byte[] data) {
		// TODO Auto-generated constructor stub
	}

	/**
	 * 
	 * @return The unit settings. 
	 */
	public Object getSettings() {
		return settings;
	}

	/**
	 * @param settings the settings to set
	 */
	public void setSettings(Object settings) {
		this.settings = settings;
	}

	/**
	 * 
	 * @return The unit name
	 */
	public String getUnitName() {
		return unitName;
	}

	/**
	 * 
	 * @return The unit type
	 */
	public String getUnitType() {
		return unitType;
	}

	/**
	 * 
	 * @return the version number for these unit settings. 
	 */
	public long getVersionNo() {
		return versionNo;
	}

	/**
	 * Find out if this settings unit is that for the given type and name
	 * @param unitType Unit Type
	 * @param unitName Unit Name
	 * @return true if these settings correspond to that unit. 
	 */
	public boolean isSettingsOf(String unitType, String unitName) {
		return (this.unitType.equals(unitType) && this.unitName.equals(unitName));
	}

	/**
	 * Find out if a set of settings are compatible with another set. 
	 * @param p another set of PamControlledUnitSettings. 
	 * @return true if they have the same name type and version number
	 */
	public boolean isSame(PamControlledUnitSettings p) {
		return (this.versionNo == p.versionNo
				&& this.unitType.equals(p.unitType) && this.unitName
				.equals(p.unitName));
	}

	/**
	 * Get a byte array of the serialised data in this object. 
	 * @return a byte array of the serialised data in this object
	 */
	public byte[] getSerialisedByteArray() {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutputStream oos = null;
		try {
			oos = new ObjectOutputStream(bos);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

		try {
			oos.writeObject(this);
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		byte[] byteArray = bos.toByteArray();

		try {
			oos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return byteArray;
	}

	/**
	 * Get a byte array of the serialised data but with a small
	 * header giving the unitType, unitName, versionNO and the size
	 * of the serialised data object
	 * @return byte array. 
	 */
	public byte[] getNamedSerialisedByteArray() {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream oos = null;
		oos = new DataOutputStream(bos);

		byte[] serialisedData = getSerialisedByteArray();
		try {
			oos.writeUTF(unitType);
			oos.writeUTF(unitName);
			oos.writeLong(versionNo);
			oos.writeInt(serialisedData.length);
			oos.write(serialisedData);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}



		byte[] byteArray = bos.toByteArray();

		try {
			oos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return byteArray;
	}

	public static PamControlledUnitSettings createFromNamedByteArray(byte[] byteArray) {
		DataInputStream dis = new DataInputStream(new ByteArrayInputStream(byteArray));
		//		oos.writeUTF(unitType);
		//		oos.writeUTF(unitName);
		//		oos.writeLong(versionNo);
		//		oos.writeInt(serialisedData.length);
		String unitType;
		String unitName;
		long versionNumber;
		int dataLength;
		byte[] data;
		try {
			unitType = dis.readUTF();
			unitName = dis.readUTF();
			versionNumber = dis.readLong();
			dataLength = dis.readInt();
			data = new byte[dataLength];
			dis.read(data);
			return createFromByteArray(data);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Create an object from a serialised byte array
	 * @param byteArray byte array
	 * @return new object (or null if invalid byte array)
	 */
	public static PamControlledUnitSettings createFromByteArray(byte[] byteArray) {
		ByteArrayInputStream bis = new ByteArrayInputStream(byteArray);
		ObjectInputStream ois;
		try {
			ois = new ObjectInputStream(bis);
			PamControlledUnitSettings pcus = (PamControlledUnitSettings) ois.readObject();
			ois.close();
			return pcus;
		} 
		catch (InvalidClassException e) {
			System.out.println("Invalid class in Control setting");
			return null;
		}
		catch (IOException e) {
			e.printStackTrace();
			return null;
		} catch (ClassNotFoundException e) {
			System.out.println("Class not found: " + e.getMessage());
			//			e.printStackTrace();
			return null;
		}
		//		return null;
	}
}
