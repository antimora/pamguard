package likelihoodDetectionModule;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * The unit test for Signal and Guard Bands.
 */
public class UnitTest {

    /**
     * Testing Signal Band
     */
    @Test
    public void testTestingSignalBand() {

        // Create a fake AcquisitionSettings and fft_params.  These are typically used to validate
        // the signal/guard bands in the UI, but here we just need them to construct.  We will set the
        // values we wish to test by hand.
        AcquisitionSettings acq = new AcquisitionSettings();
        final double freq_res = 1.d;
        final double time_res = 2.d;
        final int channel_map = 0x1;
        LikelihoodFFTParameters fft_params = new LikelihoodFFTParameters(acq, channel_map, freq_res, time_res);

        SignalBand sb = new SignalBand(acq, fft_params);
        sb.inBandThresholdDb = 10.;
        sb.guardBandThresholdDb = 20.;
        assertTrue(sb.GuardBandAsRatio() == 100.);
        assertTrue(sb.InBandAsRatio() == 10);
    }

    /**
     * Testing LikelihoodFFTParameters #1
     */
    @Test
    public void testTestingLikelihoodFFTParameters1() {

        AcquisitionSettings acq = new AcquisitionSettings();
        acq.samplingRateHz = 48000.0f;
        acq.sourceNumber = 1;

        final double freqRes = 100;
        final double timeRes = 0.01;
        final int channelMap = 0x3;
        LikelihoodFFTParameters params = new LikelihoodFFTParameters(acq, channelMap, freqRes, timeRes);

        assertTrue(params.getActualFrequencyResolution() == 93.75);
        // No fuzzy compare, this will do
        assertTrue(params.getActualTimeResolution() > 0.01);
        assertTrue(params.getActualTimeResolution() < 0.0107);

        assertTrue(acq.sourceNumber == params.getdataSourceNumber());
        assertTrue(channelMap == params.getChannelMap());
        assertTrue(256 == params.getFFTHop());
        assertTrue(512 == params.getFFTSize());
        assertTrue(0.5 == params.getOverlap());
        assertTrue(2 == params.getNumberAverages());

    }

    /**
     * Testing LikelihoodFFTParameters #2
     */
    @Test
    public void testTestingLikelihoodFFTParameters2() {
        AcquisitionSettings acq = new AcquisitionSettings();
        acq.samplingRateHz = 48000.0f;
        acq.sourceNumber = 1;

        final double freqRes = 788;
        final double timeRes = 1;
        final int channelMap = 0x5;
        LikelihoodFFTParameters params = new LikelihoodFFTParameters(acq, channelMap, freqRes, timeRes);


        assertTrue(750.0 == params.getActualFrequencyResolution());
        assertTrue(1.0 == params.getActualTimeResolution());
        assertTrue(acq.sourceNumber == params.getdataSourceNumber());
        assertTrue(channelMap == params.getChannelMap());
        assertTrue(32 == params.getFFTHop());
        assertTrue(64 == params.getFFTSize());
        assertTrue(0.5 == params.getOverlap());
        assertTrue(1500 == params.getNumberAverages());


    }

    /**
     * Testing LikelihoodFFTParameters #3
     */
    @Test
    public void testTestingLikelihoodFFTParameters3() {
        AcquisitionSettings acq = new AcquisitionSettings();
        acq.samplingRateHz = 96000.0f;
        acq.sourceNumber = 1;

        final double freqRes = 10;
        final double timeRes = 0.01;
        final int channelMap = 0x5;
        LikelihoodFFTParameters params = new LikelihoodFFTParameters(acq, channelMap, freqRes, timeRes);

        assertTrue(11.71875 == params.getActualFrequencyResolution());
        assertTrue(0.01 == params.getActualTimeResolution());
        assertTrue(acq.sourceNumber == params.getdataSourceNumber());
        assertTrue(channelMap == params.getChannelMap());
        assertTrue(960 == params.getFFTHop());
        assertTrue(8192 == params.getFFTSize());
        assertTrue(0.8828125 == params.getOverlap());
        assertTrue(1 == params.getNumberAverages());

    }

    /**
     * Testing LikelihoodFFTParameters #4
     */
    @Test
    public void testTestingLikelihoodFFTParameters4() {
        AcquisitionSettings acq = new AcquisitionSettings();
        acq.samplingRateHz = 4096;
        acq.sourceNumber = 1;

        final double freqRes = 32;
        final double timeRes = 0.00390625;
        final int channelMap = 0x5;
        LikelihoodFFTParameters params = new LikelihoodFFTParameters(acq, channelMap, freqRes, timeRes);
        assertTrue(128 == params.getFFTSize());
        assertTrue(16 == params.getFFTHop());
        assertTrue(1 == params.getNumberAverages());

    }

    /**
     * Testing LikelihoodFFTParameters #5
     */
    @Test
    public void testTestingLikelihoodFFTParameters5() {
        AcquisitionSettings acq = new AcquisitionSettings();
        acq.samplingRateHz = 4096;
        acq.sourceNumber = 1;

        final double freqRes = 32;
        final double timeRes = 0.0625;
        final int channelMap = 0x5;
        LikelihoodFFTParameters params = new LikelihoodFFTParameters(acq, channelMap, freqRes, timeRes);
        assertTrue(128 == params.getFFTSize());
        assertTrue(64 == params.getFFTHop());
        assertTrue(4 == params.getNumberAverages());
    }

    /**
     * Testing LikelihoodFFTParameters #6
     */
    @Test
    public void testTestingLikelihoodFFTParameters6() {
        AcquisitionSettings acq = new AcquisitionSettings();
        acq.samplingRateHz = 4096;
        acq.sourceNumber = 1;

        // Ask for a time resolution that would require > 90% overlap, and ensure it
        // clamps at 90
        final double freqRes = 1;
        final double timeRes = 0.085;
        final int channelMap = 0x5;
        LikelihoodFFTParameters params = new LikelihoodFFTParameters(acq, channelMap, freqRes, timeRes);
        assertTrue(410 == params.getFFTHop());
        assertTrue(4096 == params.getFFTSize());
        assertTrue(1 == params.getNumberAverages());
        assertTrue(0.9 == params.getOverlap());

    }
}
