package networkTransfer.receive;

import java.net.Socket;
import java.util.Arrays;

import Array.ArrayManager;
import Array.Streamer;
import GPS.GPSDataBlock;
import GPS.GpsData;
import GPS.GpsDataUnit;
import PamUtils.LatLong;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;

/**
 * Class for collecting data to do with individual buoys receiving data
 * over the network. 
 * @author Doug Gillespie
 *
 */
public class BuoyStatusDataUnit extends PamDataUnit {

	private int buoyId1;
	private int buoyId2;
	private int channel;
	private long creationTime;
	private long lastDataTime;
	private int totalPackets;
	private int gpsCount;
	private int[] blockPackets = new int[1];
	private NetworkReceiver networkReceiver;
	private Socket socket;
	private GpsData gpsData;
	private int commandStatus = NetworkReceiver.NET_PAM_COMMAND_STOP;
	private Streamer hydrophoneStreamer;
	private GPSDataBlock gpsDataBlock;
	private double[] compassData;
	public BuoyStatusDataUnit(NetworkReceiver networkReceiver, int buoyId1, int buoyId2, int channel) {
		super(System.currentTimeMillis());
		this.networkReceiver = networkReceiver;
		this.buoyId1 = buoyId1;
		this.buoyId2 = buoyId2;
		this.channel = channel;
		setChannelBitmap(1<<channel);
		gpsDataBlock = new GPSDataBlock(networkReceiver.getNetworkReceiveProcess());
	}
	
	public void newDataObject(PamDataBlock dataBlock, PamDataUnit dataUnit, int blockSeq) {
		lastDataTime = dataUnit.getTimeMilliseconds();
		if (creationTime == 0) {
			creationTime = lastDataTime;
		}
		totalPackets++;
		checkPacketAllocation(blockSeq);
		blockPackets[blockSeq] ++;
	}

	private void checkPacketAllocation(int blockSeq) {
		if (blockPackets == null) {
			blockPackets = new int[blockSeq+1];
		}
		else if (blockSeq >= blockPackets.length) {
			blockPackets = Arrays.copyOf(blockPackets, blockSeq+1);
		}
	}

	/**
	 * @return the buoyId1
	 */
	public int getBuoyId1() {
		return buoyId1;
	}

	/**
	 * @return the buoyId2
	 */
	public int getBuoyId2() {
		return buoyId2;
	}

	/**
	 * @return the channel
	 */
	public int getChannel() {
		return channel;
	}

	/**
	 * @return the creationTime
	 */
	public long getCreationTime() {
		return creationTime;
	}

	/**
	 * @return the lastDataTime
	 */
	public long getLastDataTime() {
		return lastDataTime;
	}

	/**
	 * @return the totalPackets
	 */
	public int getTotalPackets() {
		return totalPackets;
	}
	
	public int getBlockPacketCount(int iBlock) {
		if (iBlock < blockPackets.length) {
			return blockPackets[iBlock];
		}
		return -1;
	}

	public void initialise() {
		int nDataBlocks = networkReceiver.getRxDataBlocks().size();
		if (nDataBlocks != blockPackets.length) {
			blockPackets = new int[nDataBlocks];
		}
	}

	public void setSocket(Socket socket) {
		this.socket = socket;		
	}
	
	/**
	 * @return the socket
	 */
	public Socket getSocket() {
		return socket;
	}

	public String getIPAddr() {
		if (socket == null) {
			return null;
		}
		return socket.getInetAddress().toString();
	}
	
	public String getPort() {
		if (socket == null) {
			return null;
		}
		return new Integer(socket.getPort()).toString();
	}

	/**
	 * @return the gpsData
	 */
	public GpsData getGpsData() {
		if (gpsData == null) {
			return gpsData = getDefaultGpsData();
		}
		return gpsData;
	}

	/**
	 * Get a default location from the array manager. 
	 * @return a default location to be used in teh absence of any other gps data. 
	 */
	private GpsData getDefaultGpsData() {
//		ArrayManager arrayManager = ArrayManager.getArrayManager();
//		arrayManager.
		// for now return a default GPS position.
		return new GpsData();
	}

	/**
	 * @param gpsData the gpsData to set
	 */
	public void setGpsData(long timeMilliseconds, GpsData gpsData) {
		this.gpsData = gpsData;
		gpsCount++;
		gpsDataBlock.addPamData(new GpsDataUnit(timeMilliseconds, gpsData));
	}

	public void setCompassData(long timeInMillis, double[] compassData) {
		GpsData someData = gpsData;
		if (gpsData == null) {
			someData = getDefaultGpsData();
			gpsDataBlock.addPamData(new GpsDataUnit(timeInMillis, someData));
		}
		someData.setMagneticHeading(compassData[0]);
		this.compassData = compassData;
	}

	public double[] getCompassData() {
		return compassData;
	}

	public int getGpsCount() {
		return gpsCount;
	}
	
	public Object getPositionString() {
		if (gpsData == null) {
			return null;
		}
		return String.format("%s, %s", LatLong.formatLatitude(gpsData.getLatitude()), LatLong.formatLongitude(gpsData.getLongitude()));
	}

	/**
	 * @param commandStatus the commandStatus to set
	 */
	public void setCommandStatus(int commandStatus) {
		this.commandStatus = commandStatus;
	}

	/**
	 * @return the commandStatus
	 */
	public int getCommandStatus() {
		return commandStatus;
	}

	/**
	 * @return the networkReceiver
	 */
	public NetworkReceiver getNetworkReceiver() {
		return networkReceiver;
	}

	/**
	 * @param networkReceiver the networkReceiver to set
	 */
	public void setNetworkReceiver(NetworkReceiver networkReceiver) {
		this.networkReceiver = networkReceiver;
	}

	/**
	 * Set the hydrophone streamer for the buoy. Also sets a
	 * reference to the buoy stats in the streamer. 
	 * @param hydrophoneStreamer the hydrophoneStreamer to set
	 */
	public void setHydrophoneStreamer(Streamer hydrophoneStreamer) {
		this.hydrophoneStreamer = hydrophoneStreamer;
		hydrophoneStreamer.setBuoyStats(this);
	}

	/**
	 * @return the hydrophoneStreamer
	 */
	public Streamer getHydrophoneStreamer() {
		return hydrophoneStreamer;
	}
	
	
}
