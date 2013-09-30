package likelihoodDetectionModule.spectralEti;

import org.junit.Before;
import static org.junit.Assert.*;

import org.junit.Test;

public class UnitTest {

    double[] data = new double[10];

    @Before
    public void beforeTest() {
        // ("Instantiated SpectralBand object");
        for (int i = 0; i < 10; ++i) {
            data[i] = i + 1;
        }
    }

    @Test
    public void testSpectralBand() {

        // ("Test #1 with bands inside of the spectral range");
        SpectralBand band = new SpectralBand(1., 2., 10.);
        double result = band.calculate(data);
        assertTrue(result == 4.);

    }

    @Test
    public void testWithBandsOutsideOfTheSpectralRangeToTestClipping() {
        // ("Test #2 with bands outside of the spectral range to test clipping");
        SpectralBand band = new SpectralBand(-1, 6, 10.);
        double result = band.calculate(data);
        assertTrue(result == 5.5);

    }
}
