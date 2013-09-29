package GPS;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;

import PamguardMVC.PamDataUnit;
import binaryFileStorage.BinaryDataSource;
import binaryFileStorage.BinaryHeader;
import binaryFileStorage.BinaryObjectData;
import binaryFileStorage.ModuleFooter;
import binaryFileStorage.ModuleHeader;
import binaryFileStorage.PackedBinaryObject;

public class GPSBinaryDataSource extends BinaryDataSource {

	public GPSBinaryDataSource(GPSDataBlock gpsDataBlock) {
		super(gpsDataBlock);
		// TODO Auto-generated constructor stub
	}

	@Override
	public byte[] getModuleHeaderData() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getModuleVersion() {
		// TODO Auto-generated method stub
		return 1;
	}

	@Override
	public String getStreamName() {
		// TODO Auto-generated method stub
		return "GPS";
	}

	@Override
	public int getStreamVersion() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void newFileOpened(File outputFile) {
		// TODO Auto-generated method stub

	}

	@Override

	public PackedBinaryObject getPackedData(PamDataUnit pamDataUnit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PamDataUnit sinkData(BinaryObjectData binaryObjectData,
			BinaryHeader bh, int moduleVersion) {
		ByteArrayInputStream bis = new ByteArrayInputStream(binaryObjectData.getData(), 
				0, binaryObjectData.getDataLength());
		DataInputStream dis = new DataInputStream(bis);
		float latitude = 0; 
		float longitude = 0;

		try {
			latitude = dis.readFloat();
			longitude = dis.readFloat();
			dis.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//		System.out.println(String.format("GPS data received at %s", PamCalendar.formatDateTime(binaryObjectData.getTimeMillis())));
		
		GpsData gpsData = new GpsData(latitude, longitude, 0, binaryObjectData.getTimeMillis());
		GpsDataUnit du = new GpsDataUnit(binaryObjectData.getTimeMillis(), gpsData);
		
		return du;
	}

	@Override
	public ModuleFooter sinkModuleFooter(BinaryObjectData binaryObjectData,
			BinaryHeader bh, ModuleHeader moduleHeader) {
		return null;
	}

	@Override
	public ModuleHeader sinkModuleHeader(BinaryObjectData binaryObjectData,
			BinaryHeader bh) {
		// TODO Auto-generated method stub
		return null;
	}

}
