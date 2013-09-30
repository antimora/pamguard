package likelihoodDetectionModule.linearAverageSpectra;

import static org.junit.Assert.*;

import org.junit.Test;

public class UnitTest {

    @Test
    public void testfftManagerComplex() {

        fftManager.Complex[] d = new fftManager.Complex[2];

        d[0] = new fftManager.Complex();
        d[0].real = 10;
        d[0].imag = 15;

        d[1] = new fftManager.Complex();
        d[1].real = 23;
        d[1].imag = 45;

        long timestamp = 0;
        long startSample = 100;

        final int recordsToAverage = 5;
        // ("Test 1 with constant data");
        RealBlockAverage rba = new RealBlockAverage(recordsToAverage);
        // ("Created the RealBlockAverage object with nAvg == " + recordsToAverage);
        assertTrue(rba.NAvg() == recordsToAverage);

        // ("Checking no output is ready yet");
        assertTrue(rba.averageReady() == false);

        rba.processData(timestamp++, startSample, d);
        startSample += 100;
        rba.processData(timestamp++, startSample, d);
        startSample += 100;

        // ("Checking that nothing is ready after 2 data points");
        assertTrue(rba.averageReady() == false);


        rba.processData(timestamp++, startSample, d);
        startSample += 100;
        rba.processData(timestamp++, startSample, d);
        startSample += 100;

        // ("Checking that nothing is ready after 4 data points");
        assertTrue(rba.averageReady() == false);


        // Do the 5th input, this should produce an average
        rba.processData(timestamp++, startSample, d);
        startSample += 100;

        // ("Checking after 5 data points, should have an average");
        assertTrue(rba.averageReady() == true);

        double[] result = rba.average();
        // ("Making sure the output record is the same width as the input records");
        assertTrue(result.length == 2);

        // ("Checking timestamp of average");
        assertTrue(0 == rba.averageTimestamp());
        assertTrue(100 == rba.averageStartSample());

        // ("Checking output");
        for (double x : result) {
            // (x);
        }
        assertTrue(result[ 0] == 325.);
        assertTrue(result[ 1] == 2554.);


        // ("Making sure another average is not ready");
        assertTrue(rba.averageReady() == false);

        // ("Test 2 with changing data");
        d[0].real = 2;
        d[0].imag = 3;
        d[1].real = 5;
        d[1].imag = 6;
        for (int index = 0; index < rba.NAvg(); ++index) {
            rba.processData(timestamp++, startSample, d);
            startSample += 100;
            d[0].real += d[0].real;
            d[0].imag += d[0].imag;
            d[1].real += d[1].real;
            d[1].imag += d[1].imag;
        }
        // ("Checking after 5 data points, should have an average");
        assertTrue(rba.averageReady() == true);


        result = rba.average();
        // ("Making sure the output record is the same width as the input records");
        assertTrue(result.length == 2);

        // ("Checking timestamp of average");
        assertTrue(5 == rba.averageTimestamp());
        assertTrue(600 == rba.averageStartSample());

        assertTrue(result[ 0] == 886.6);
        assertTrue(result[ 1] == 4160.2);


        // ("Making sure another average is not ready");
        assertTrue(rba.averageReady() == false);


    }
}
