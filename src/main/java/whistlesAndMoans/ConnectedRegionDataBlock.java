package whistlesAndMoans;

import staticLocaliser.StaticLocaliserControl;
import staticLocaliser.StaticLocaliserProvider;
import staticLocaliser.panels.AbstractLocaliserControl;
import staticLocaliser.panels.WhistleLocaliserControl;

public class ConnectedRegionDataBlock extends AbstractWhistleDataBlock  {

	private WhistleToneConnectProcess parentProcess;
		
	public ConnectedRegionDataBlock(String dataName,
			WhistleToneConnectProcess parentProcess, int channelMap) {
		super(ConnectedRegionDataUnit.class, dataName, parentProcess, channelMap);
		this.parentProcess = parentProcess;
		// TODO Auto-generated constructor stub
	}

	@Override
	public WhistleToneConnectProcess getParentProcess() {
		return parentProcess;
	}

	
	
	
}
