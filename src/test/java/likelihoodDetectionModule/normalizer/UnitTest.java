package likelihoodDetectionModule.normalizer;

import likelihoodDetectionModule.spectralEti.SpectralEtiDataUnit;

import static org.junit.Assert.*;

import org.junit.Test;

public class UnitTest {

    @Test
    public void testNormalizedDataClass() {

        // ("Testing NormalizedData class");
        NormalizedData nd = new NormalizedData(15, 3);
        assertTrue(15. / 3. == nd.snr());

    }

    @Test
    public void testExponentialAveragerAndSlidingAverager() {

        // ("Testing the Exponential Averager and Sliding Averager");

        double[] testData = new double[20];
        java.util.Arrays.fill(testData, 5.5);
        // Add a data spike.
        testData[11] = 14;
        testData[12] = 15;

        // ("Testing Exponential Averager class, alpha == 0");
        ExponentialAverager exp = new ExponentialAverager(0);

        double[] result = exp.Process(testData);
        assertTrue(result.length == testData.length);
        for (int i = 0; i < result.length; ++i) {
            assertTrue(testData[i] == result[i]);
        }


        // ("Testing Exponential Averager class, alpha == 0.5");
        exp = new ExponentialAverager(0.5);
        result = exp.Process(testData);

        int i = 0;
        assertTrue(result[i++] == 5.5);
        assertTrue(result[i++] == 5.5);
        assertTrue(result[i++] == 5.5);
        assertTrue(result[i++] == 5.5);
        assertTrue(result[i++] == 5.5);
        assertTrue(result[i++] == 5.5);
        assertTrue(result[i++] == 5.5);
        assertTrue(result[i++] == 5.5);
        assertTrue(result[i++] == 5.5);
        assertTrue(result[i++] == 5.5);
        assertTrue(result[i++] == 5.5);
        assertTrue(result[i++] == 9.75);
        assertTrue(result[i++] == 12.375);
        assertTrue(result[i++] == 8.9375);
        assertTrue(result[i++] == 7.21875);
        assertTrue(result[i++] == 6.359375);
        assertTrue(result[i++] == 5.9296875);
        assertTrue(result[i++] == 5.71484375);
        assertTrue(result[i++] == 5.607421875);
        assertTrue(result[i++] == 5.5537109375);


        // ("Testing Sliding Averager class, with divide");

        SlidingAverager slide = new SlidingAverager(4, true);
        // Test that it always makes the width odd.
        assertTrue(slide.getWidth() == 5);

        double[] slidingTestData = new double[2];
        java.util.Arrays.fill(slidingTestData, 5.5);

        slide.consume(slidingTestData);
        // There should be no output yet, it hasn't seen enough input
        assertTrue(slide.processedData.isEmpty());

        slide.consume(slidingTestData);
        // Now there should be some output
        assertTrue(slide.processedData.size() == 2);

        for (Double d : slide.processedData) {
            assertTrue(d == 5.5);
        }

        // Start with a new Sliding Averager

        // ("Testing Sliding Averager class, without divide");
        slide = new SlidingAverager(5, false);
        assertTrue(slide.getWidth() == 5);

        // Modify the testData to have a small spike at the beginning, to
        // test
        // that it
        // mirrors the predata history properly.
        testData[0] = 10;
        testData[1] = 8;

        // Since this module requires a lookahead, its output lags its input
        // by
        // (n-1)/2 samples, in this case 2
        // So we expect 20-2 = 18 samples
        slide.consume(testData);
        assertTrue(slide.processedData.size() == 18);

        i = 0;
        Double[] slideResults = slide.processedData.toArray(new Double[0]);
        assertTrue(slideResults[i++] == 37.0);
        assertTrue(slideResults[i++] == 37.0);
        assertTrue(slideResults[i++] == 34.5);
        assertTrue(slideResults[i++] == 30.0);
        assertTrue(slideResults[i++] == 27.5);
        assertTrue(slideResults[i++] == 27.5);
        assertTrue(slideResults[i++] == 27.5);
        assertTrue(slideResults[i++] == 27.5);
        assertTrue(slideResults[i++] == 27.5);
        assertTrue(slideResults[i++] == 36.0);
        assertTrue(slideResults[i++] == 45.5);
        assertTrue(slideResults[i++] == 45.5);
        assertTrue(slideResults[i++] == 45.5);
        assertTrue(slideResults[i++] == 45.5);
        assertTrue(slideResults[i++] == 37.0);
        assertTrue(slideResults[i++] == 27.5);
        assertTrue(slideResults[i++] == 27.5);
        assertTrue(slideResults[i++] == 27.5);

    }

    @Test
    public void testSplitWindowNormalizer() {


        // ("Testing Split Window Normalizer");

        java.util.ArrayList<SplitWindowNormWidths> splitWidths = new java.util.ArrayList<SplitWindowNormWidths>();
        splitWidths.add(new SplitWindowNormWidths(1, 5));
        splitWidths.add(new SplitWindowNormWidths(3, 7));
        SplitWindowNorm split = new SplitWindowNorm(splitWidths);

        double[] splitData = new double[2];
        splitData[0] = 1;
        splitData[1] = 2;

        long time = 0;
        final int bitmap = 0x3;
        long startSample = 100;
        final int duration = 166;
        SpectralEtiDataUnit splitDataUnit = new SpectralEtiDataUnit(time++,
                bitmap,
                startSample,
                duration);

        startSample += 50;
        splitDataUnit.setData(splitData);

        NormalizedDataUnit splitOutputUnit = split.Process(splitDataUnit);
        assertTrue(splitOutputUnit == null);

        splitDataUnit.setTimeMilliseconds(time++);
        splitDataUnit.setStartSample(startSample);
        startSample += 50;
        splitOutputUnit = split.Process(splitDataUnit);
        assertTrue(splitOutputUnit == null);

        splitDataUnit.setTimeMilliseconds(time++);
        splitDataUnit.setStartSample(startSample);
        startSample += 50;
        splitOutputUnit = split.Process(splitDataUnit);
        assertTrue(splitOutputUnit == null);

        long outputTimeStamp = 0;
        long outputStartSample = 100;
        for (int i = 0; i < 10; ++i) {

            splitDataUnit.setTimeMilliseconds(time++);
            splitDataUnit.setStartSample(startSample);
            startSample += 50;

            splitOutputUnit = split.Process(splitDataUnit);
            assertTrue(splitOutputUnit != null);

            // Test that the timestamps coming out are delayed as well. They
            // should be 0, 1, 2 ...
            // even though this is the 3rd, 4th, 5th input records.
            assertTrue(splitOutputUnit.getTimeMilliseconds() == outputTimeStamp++);
            assertTrue(splitOutputUnit.getStartSample() == outputStartSample);
            assertTrue(splitOutputUnit.getDuration() == duration);
            outputStartSample += 50;
        }

        assertTrue(splitOutputUnit.getData()[0].signal == 1.);
        assertTrue(splitOutputUnit.getData()[0].noise == 1.);
        assertTrue(splitOutputUnit.getData()[1].signal == 2.);
        assertTrue(splitOutputUnit.getData()[1].noise == 2.);

        splitData[0] = 5;
        splitData[1] = 10;

        splitDataUnit.setData(splitData);

        splitDataUnit.setTimeMilliseconds(time++);
        splitDataUnit.setStartSample(startSample);
        startSample += 50;

        splitOutputUnit = split.Process(splitDataUnit);
        assertTrue(splitOutputUnit.getData()[0].signal == 1.);
        assertTrue(splitOutputUnit.getData()[0].noise == 1.);
        assertTrue(splitOutputUnit.getData()[1].signal == 2.);
        assertTrue(splitOutputUnit.getData()[1].noise == 4.);

        assertTrue(splitOutputUnit.getTimeMilliseconds() == outputTimeStamp++);
        assertTrue(splitOutputUnit.getStartSample() == outputStartSample);
        outputStartSample += 50;

        splitDataUnit.setTimeMilliseconds(time++);
        splitDataUnit.setStartSample(startSample);
        startSample += 50;

        splitOutputUnit = split.Process(splitDataUnit);
        assertTrue(splitOutputUnit.getData()[0].signal == 1.);
        assertTrue(splitOutputUnit.getData()[0].noise == 2.);
        assertTrue(splitOutputUnit.getData()[1].signal == 2.);
        assertTrue(splitOutputUnit.getData()[1].noise == 6.);

        assertTrue(splitOutputUnit.getTimeMilliseconds() == outputTimeStamp++);
        assertTrue(splitOutputUnit.getStartSample() == outputStartSample);
        outputStartSample += 50;

        splitDataUnit.setTimeMilliseconds(time++);
        splitDataUnit.setStartSample(startSample);
        startSample += 50;

        splitOutputUnit = split.Process(splitDataUnit);
        assertTrue(splitOutputUnit.getData()[0].signal == 1.);
        assertTrue(splitOutputUnit.getData()[0].noise == 3.);
        assertTrue(splitOutputUnit.getData()[1].signal == 4. + 2. / 3.);
        assertTrue(splitOutputUnit.getData()[1].noise == 6.);

        assertTrue(splitOutputUnit.getTimeMilliseconds() == outputTimeStamp++);
        assertTrue(splitOutputUnit.getStartSample() == outputStartSample);
        outputStartSample += 50;

        splitDataUnit.setTimeMilliseconds(time++);
        splitDataUnit.setStartSample(startSample);
        startSample += 50;
        splitOutputUnit = split.Process(splitDataUnit);
        assertTrue(splitOutputUnit.getData()[0].signal == 5.);
        assertTrue(splitOutputUnit.getData()[0].noise == 3.);
        assertTrue(splitOutputUnit.getData()[1].signal == 7. + 1. / 3.);
        assertTrue(splitOutputUnit.getData()[1].noise == 6.);

        assertTrue(splitOutputUnit.getTimeMilliseconds() == outputTimeStamp++);
        assertTrue(splitOutputUnit.getStartSample() == outputStartSample);
        outputStartSample += 50;

        splitDataUnit.setTimeMilliseconds(time++);
        splitDataUnit.setStartSample(startSample);
        startSample += 50;
        splitOutputUnit = split.Process(splitDataUnit);
        assertTrue(splitOutputUnit.getData()[0].signal == 5.);
        assertTrue(splitOutputUnit.getData()[0].noise == 4.);
        assertTrue(splitOutputUnit.getData()[1].signal == 10.);
        assertTrue(splitOutputUnit.getData()[1].noise == 6.);

        assertTrue(splitOutputUnit.getTimeMilliseconds() == outputTimeStamp++);
        assertTrue(splitOutputUnit.getStartSample() == outputStartSample);
        outputStartSample += 50;

        splitDataUnit.setTimeMilliseconds(time++);
        splitDataUnit.setStartSample(startSample);
        startSample += 50;
        splitOutputUnit = split.Process(splitDataUnit);
        assertTrue(splitOutputUnit.getData()[0].signal == 5.);
        assertTrue(splitOutputUnit.getData()[0].noise == 5.);
        assertTrue(splitOutputUnit.getData()[1].signal == 10.);
        assertTrue(splitOutputUnit.getData()[1].noise == 8.);

        assertTrue(splitOutputUnit.getTimeMilliseconds() == outputTimeStamp++);
        assertTrue(splitOutputUnit.getStartSample() == outputStartSample);
        outputStartSample += 50;

        splitDataUnit.setTimeMilliseconds(time++);
        splitDataUnit.setStartSample(startSample);
        startSample += 50;
        splitOutputUnit = split.Process(splitDataUnit);
        assertTrue(splitOutputUnit.getData()[0].signal == 5.);
        assertTrue(splitOutputUnit.getData()[0].noise == 5.);
        assertTrue(splitOutputUnit.getData()[1].signal == 10.);
        assertTrue(splitOutputUnit.getData()[1].noise == 10.);

        assertTrue(splitOutputUnit.getTimeMilliseconds() == outputTimeStamp++);
        assertTrue(splitOutputUnit.getStartSample() == outputStartSample);
        outputStartSample += 50;

    }

    @Test
    public void testTwoSpeedExponentialAverager() {

        // ("Testing Two Speed Exponential Averager");

        TwoSpeedExponentialAverager twospeed = new TwoSpeedExponentialAverager(
                0.5, 0.);

        double[] expData = new double[1];
        double[] expRefData = new double[1];
        expData[0] = 10;
        expRefData[0] = 20;

        double[] expResult = twospeed.Process(expData, expRefData);
        assertTrue(expResult.length == 1);
        assertTrue(expResult[0] == 10.);

        expData = new double[6];
        expRefData = new double[6];
        expData[0] = 10;
        expRefData[0] = 20;
        expData[1] = 10;
        expRefData[1] = 20;
        expData[2] = 12;
        expRefData[2] = 20;
        expData[3] = 8;
        expRefData[3] = 20;
        expData[4] = 8;
        expRefData[4] = 5;
        expData[5] = 10;
        expRefData[5] = 20;

        expResult = twospeed.Process(expData, expRefData);
        assertTrue(expResult.length == 6);
        assertTrue(expResult[0] == 10.);
        assertTrue(expResult[1] == 10.);
        assertTrue(expResult[2] == 11.);
        assertTrue(expResult[3] == 9.5);
        assertTrue(expResult[4] == 8.);
        assertTrue(expResult[5] == 9.);
    }

    @Test
    public void testDecayingAverageNorm() {

        // ("Testing Decaying Average Norm");

        java.util.ArrayList<DecayingAverageNormAlphas> alphas = new java.util.ArrayList<DecayingAverageNormAlphas>();
        alphas.add(new DecayingAverageNormAlphas(0.5, 0.6));
        alphas.add(new DecayingAverageNormAlphas(0.0, 0.5));

        final double referenceGain = 1.2;

        DecayingAverageNorm decay = new DecayingAverageNorm(alphas, referenceGain);
        final int bitmap = 0x3;
        final int duration = 142;
        long time = 0;
        long startSample = 100;

        SpectralEtiDataUnit decayUnit = new SpectralEtiDataUnit(time++,
                bitmap,
                startSample,
                duration);
        startSample += 50;

        double[] decayData = new double[2];
        decayData[0] = 10;
        decayData[1] = 12;
        decayUnit.setData(decayData);


        int outputTime = 0;
        long outputStartSample = 100;

        NormalizedDataUnit decayOutputUnit = decay.Process(decayUnit);
        assertTrue(decayOutputUnit.getData()[0].signal == 10.);
        assertTrue(decayOutputUnit.getData()[0].noise == 10.);
        assertTrue(decayOutputUnit.getData()[1].signal == 12.);
        assertTrue(decayOutputUnit.getData()[1].noise == 12.);
        assertTrue(decayOutputUnit.getTimeMilliseconds() == outputTime++);
        assertTrue(decayOutputUnit.getStartSample() == outputStartSample);
        assertTrue(decayOutputUnit.getChannelBitmap() == bitmap);
        assertTrue(decayOutputUnit.getDuration() == duration);
        outputStartSample += 50;



        decayData[0] = 100;
        decayData[1] = 1;
        decayUnit.setData(decayData);
        decayUnit.setTimeMilliseconds(time++);
        decayUnit.setStartSample(startSample);
        startSample += 50;

        decayOutputUnit = decay.Process(decayUnit);
        assertTrue(decayOutputUnit.getData()[0].signal == 55.);
        assertTrue(decayOutputUnit.getData()[0].noise == 46.);
        assertTrue(decayOutputUnit.getData()[1].signal == 1.);
        assertTrue(decayOutputUnit.getData()[1].noise == 1.);
        assertTrue(decayOutputUnit.getTimeMilliseconds() == outputTime++);
        assertTrue(decayOutputUnit.getStartSample() == outputStartSample);
        assertTrue(decayOutputUnit.getDuration() == duration);
        outputStartSample += 50;


        decayData[0] = 1;
        decayData[1] = 100;
        decayUnit.setData(decayData);
        decayUnit.setTimeMilliseconds(time++);
        decayUnit.setStartSample(startSample);
        startSample += 50;

        decayOutputUnit = decay.Process(decayUnit);
        assertTrue(decayOutputUnit.getData()[0].signal == 28.);
        assertTrue(decayOutputUnit.getData()[0].noise == 23.5);
        assertTrue(decayOutputUnit.getData()[1].signal == 100.);
        assertTrue(decayOutputUnit.getData()[1].noise == 50.5);
        assertTrue(decayOutputUnit.getTimeMilliseconds() == outputTime++);
        assertTrue(decayOutputUnit.getStartSample() == outputStartSample);
        assertTrue(decayOutputUnit.getDuration() == duration);
        outputStartSample += 50;

    }

    @Test
    public void testDecayingAverageNormAlphas() {


        final double timeResolution = 2.2;
        // ("Testing DecayingAverageNormAlphas");
        DecayingAverageNormAlphas decayNormAlphas = new DecayingAverageNormAlphas(
                10, 1000, timeResolution);
        assertTrue(decayNormAlphas.signalAlpha == 0.78);
        assertTrue(decayNormAlphas.backgroundAlpha == 0.9978);

    }

    @Test
    public void testSplitWindowNormWidths() {


        // ("Testing SplitWindowNormWidths");
        final double timeResolution = 2.2;
        SplitWindowNormWidths splitWindowWidths = new SplitWindowNormWidths(
                10, 1000, timeResolution);
        assertTrue(splitWindowWidths.signalWidth == 4);
        assertTrue(splitWindowWidths.backgroundWidth == 454);
        try {
            new SplitWindowNormWidths(1000, 10, timeResolution);
            // Should not get here, if we do, FAIL
            assertTrue(false);
        } catch (java.lang.Exception e) {
            // Do nothing, we are supposed to throw above
        }
    }
}
