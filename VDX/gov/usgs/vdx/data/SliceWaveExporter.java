package gov.usgs.vdx.data;

import gov.usgs.vdx.data.wave.Wave;

import gov.usgs.vdx.data.wave.plot.SliceWaveRenderer;
import gov.usgs.vdx.data.Exportable;


/**
 * Add methods to SliceWaveRenderer to yield plot data for exporting
 *
 * @author Scott Hunter
 */
public class SliceWaveExporter extends SliceWaveRenderer implements Exportable {

    private double step = 0, time = 0, bias = 0;

	/**
	 * Get data about next wave for export
	 * @return next wave
	 */
	public Double[] getNextExportRow() {
		if ( wave == null )
			return null;
		double value = 0;
		if ( step == 0 ) {
			resetExport();
			step = (1 / wave.getSamplingRate());
			time = wave.getStartTime();
			if ( removeBias )
				bias = wave.mean();
		}
		if ( wave.hasNext() ) {
			value = wave.next();
			time += step;
			Double[] retval = new Double[2];
			retval[0] = time - step;
			if ( value == Wave.NO_DATA ) {
				retval[1] = value;
			} else {
				retval[1] = value - bias;
			}
			return retval;
		}
		return null;
	}

    /**
     * Reset export to beginning of wave
     */
	public void resetExport() {
		if ( wave != null )
			wave.reset();
	}
	
	/**
	 * Return the number of samples
	 * @return number of samples
	 */
	public int length() {
		return wave.samples();
	}
		
}
