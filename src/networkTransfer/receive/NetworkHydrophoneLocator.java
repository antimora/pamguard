package networkTransfer.receive;

import java.util.ListIterator;
import java.util.Vector;

import Array.HydrophoneLocator;
import Array.PamArray;
import GPS.GpsData;
import PamController.PamController;
import PamUtils.LatLong;

public class NetworkHydrophoneLocator implements HydrophoneLocator {

	private NetworkReceiver networkReceiver;
	
	public NetworkHydrophoneLocator(PamArray pamArray) {
		// TODO Auto-generated constructor stub
	}

	private NetworkReceiver findNetworkReceiver() {
		if (networkReceiver == null) {
			networkReceiver = (NetworkReceiver) PamController.getInstance().findControlledUnit(NetworkReceiver.unitTypeString);
		}
		return networkReceiver;
	}
	@Override
	public double getArrayHeading(long timeMilliseconds, int phoneNo) {
		GpsData gpsData = findChannelGPSData(phoneNo);
		if (gpsData == null) {
			return 0;
		}
		return gpsData.getHeading();
//		if (th != null) {
//			return th;
//		}
//		else {
//			return 0;
//		}
	}

	@Override
	public double getPairAngle(long timeMilliseconds, int phone1, int phone2,
			int angleType) {
		GpsData g1 = findChannelGPSData(phone1);
		GpsData g2 = findChannelGPSData(phone2);
		if (g1 == null || g2 == null) {
			return Double.NaN;
		}
		return g1.bearingTo(g2);
	}

	@Override
	public double getPairSeparation(long timeMilliseconds, int phone1,
			int phone2) {
		GpsData g1 = findChannelGPSData(phone1);
		GpsData g2 = findChannelGPSData(phone2);
		if (g1 == null || g2 == null) {
			return Double.NaN;
		}
		return g1.distanceToMetres(g2);
	}

	@Override
	public double getPhoneHeight(long timeMilliseconds, int phoneNo) {
		return 0;
	}

	@Override
	public LatLong getPhoneLatLong(long timeMilliseconds, int phoneNo) {
		return findChannelGPSData(phoneNo);
	}

	@Override
	public double getPhoneTilt(long timeMilliseconds, int phoneNo) {
		return 0;
	}

	@Override
	public LatLong getReferenceLatLong(long timeMilliseconds) {
		/**
		 * Return the average of all buoy positions. 
		 */
		networkReceiver = findNetworkReceiver();
		if (networkReceiver == null) {
			return null;
		}

		double meanLat = 0;
		double meanLong = 0;
		int n = 0;
		GpsData gpsData;
		BuoyStatusDataUnit du;
		ListIterator<BuoyStatusDataUnit> li = networkReceiver.getBuoyStatusDataBlock().getListIterator(0);
		while (li.hasNext()) {
			du = li.next();
			gpsData = du.getGpsData();
			if (gpsData == null) {
				continue;
			}
			meanLat += gpsData.getLatitude();
			meanLong += gpsData.getLongitude();
			n++;
		}
		
		if (n > 0) {
			return new LatLong(meanLat/n, meanLong/n);
		}
		return null;
	}
		
	/**
	 * Find the gps data associated with a given hydrophone
	 * @param iChannel
	 * @return gpsData
	 */
	private GpsData findChannelGPSData(int iChannel) {
		networkReceiver = findNetworkReceiver();
		if (networkReceiver == null) {
			return null;
		}
		BuoyStatusDataUnit du = networkReceiver.getBuoyStatusDataBlock().findDataUnit(1<<iChannel);
		if (du == null) {
			return null;
		}
		return du.getGpsData();
		
	}

	@Override
	public void notifyModelChanged(int changeType) {
		// TODO Auto-generated method stub

	}

}
