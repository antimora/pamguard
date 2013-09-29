package IshmaelDetector;


import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;
import PamguardMVC.PamObservable;
import fftManager.Complex;
import fftManager.FFTDataBlock;
import fftManager.FFTDataUnit;
import IshmaelDetector.IshDetFnDataUnit;

/* @author Dave Mellinger and Hisham Qayum
 */
public class EnergySumProcess extends IshDetFnProcess
{
	int savedGramHeight = -1;		//-1 forces recalculation
	int loBin, hiBin;	//bin index to start/stop summing
	int sumLength = 0;
	boolean useDB;
	
	public EnergySumProcess(EnergySumControl energySumControl, 
			PamDataBlock parentDataBlock) 
	{
		super(energySumControl, parentDataBlock);
	}
	
	@Override
	public String getLongName() {
		return "Energy sum detector data";
	}
	
	public String getNumberName() {
		EnergySumParams p = (EnergySumParams)ishDetControl.ishDetParams;
		return "Energy Sum: " + p.f0 + " to " + p.f1 + " Hz";
	}

    /** Return the rate at which detection samples arrive, which for this detector
     * is the FFT frame rate.  Abstractly declared in IshDetFnProcess.
     */
	@Override
	public float getDetSampleRate() {
		FFTDataBlock fftDataSource = (FFTDataBlock)getInputDataBlock();
		return sampleRate / fftDataSource.getFftHop();
	}
	
	@Override
	public Class inputDataClass() { return FFTDataUnit.class; }
	
	
	//Calculate any subsidiary values needed for processing.  These get recalculated
	//whenever the sample rate changes (via setSampleRate, which is also called after 
	//the params dialog box closes).
	//Note that during initialization, this gets called with params.inputDataSource
	//still null.
	@Override
	protected void prepareMyParams() {
		EnergySumParams p = (EnergySumParams)ishDetControl.ishDetParams;
		PamDataBlock inputDataBlock = getInputDataBlock();		//might be null
		
		if (inputDataBlock != null && inputDataBlock.getUnitsCount() > 0) {
			savedGramHeight = ((FFTDataUnit)inputDataBlock.getLastUnit()).getFftData().length;	
			int len = savedGramHeight;
			//Should be max(1,...) here, but FFT bin 0 has 0's in it.
			loBin = Math.max(1,     (int)Math.floor(len * p.f0 / (sampleRate/2)));
			hiBin = Math.min(len-1, (int)Math.ceil (len * p.f1 / (sampleRate/2)));
		} else {
			savedGramHeight = -1;	//special case: force recalculation later
		}
		useDB = p.useLog;
		setProcessName("Energy sum: " + p.f0 + " to " + p.f1);
	}
	
	@Override
	public float getHiFreq() {
		EnergySumParams p = (EnergySumParams)ishDetControl.ishDetParams;
		return (float) p.f1;
	}

	@Override
	public float getLoFreq() {
		EnergySumParams p = (EnergySumParams)ishDetControl.ishDetParams;
		return (float) p.f0;
	}

	/* EnergySumProcess uses recycled data blocks. The length of the data unit should
	 * correspond to the output of the detector function: Just one double.
	 */
	@Override
	public void newData(PamObservable o, PamDataUnit arg1) {  //called from PamProcess
		FFTDataUnit fftDataUnit = (FFTDataUnit)arg1;
		Complex[] inputData = fftDataUnit.getFftData();     //data from FFT is Complex[]

		//See if the channel is one we want before doing anything.
		if ((fftDataUnit.getChannelBitmap() & ishDetControl.ishDetParams.channelList) == 0)
			return;
		//See if loBin and hiBin need recalculating.
		if (inputData.length != savedGramHeight)	//in theory, should never happen
			prepareMyParams();				//in practice, might happen on 1st unit
		
		IshDetFnDataUnit outputUnit = getOutputDataUnit(fftDataUnit);  //get a fresh unit

		//The actual sum calculation.
		double sum = 0.0;
		if (useDB) {		//keep this test outside the loop for speed
			for (int i = loBin; i <= hiBin; i++) {
				double mag = inputData[i].magsq();
				sum += Math.log10(Math.max(mag, 1.0e-9));   //apply floor
			} 
		} else {
			for (int i = loBin; i <= hiBin; i++)
				sum += inputData[i].magsq();
		}
		double result = sum / (hiBin - loBin + 1);

		//Set up structure for depositing the result -- a vector of length 1 --
		//and append the new data to the end of the data stream.
		outputUnit.detData = new double[1];
		outputUnit.detData[0] = result;
		outputDataBlock.addPamData(outputUnit);
	}
}
