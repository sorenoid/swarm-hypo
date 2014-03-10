package gov.usgs.winston.monitor;

import gov.usgs.util.Util;
import gov.usgs.vdx.data.wave.Wave;


/**
 * Decode and hold a McVCO Calibration pulse
 * see OFR99-361
 * 
 * @author Tom Parker
 * 
 */

public class McVcoCalPulse {

	private final double startTime;
	private int gain;
	private int unitId;
	private boolean dataFound;
	private double voltage;
	private Wave wave;
	
	private double endPreambleSignal;
	private String endPreambleString;
	private double endPreambleQuiet;
	private double beginGain;
	private double beginId;
	private double beginVolt;
	
	public McVcoCalPulse(Wave w)
	{
		wave = w;
		startTime = wave.getStartTime();
		try
		{
			decode();
			dataFound = true;
		} catch (ArrayIndexOutOfBoundsException e)
		{
			dataFound = false;
		}
		System.out.println("endPreSig: " + Util.j2KToDateString(endPreambleSignal) +
				"; endPreQuiet: " + Util.j2KToDateString(endPreambleQuiet) + "; beginGain: " +
				Util.j2KToDateString(beginGain) + "; beginId: " + Util.j2KToDateString(beginId) +
				"; beginVolt: " + Util.j2KToDateString(beginVolt));
		System.out.println(endPreambleString);
	}
	
	public boolean dataFound()
	{
		return dataFound;
	}
	
	private int decodeBit(int i)
	{
		return Math.signum(wave.buffer[i]) == 1 ? 1 : 0;
	}

	public double getStartTime() 
	{
		return startTime;
	}
	
	public int getGain() 
	{
		return gain;
	}

	public int getUnitId() 
	{
		return unitId;
	}

	public double getVoltage() 
	{
		return voltage;
	}
	// find the end of preamble. Assume wave starts during 21.25hz signal
	private void decode()
	{
		double st = wave.getStartTime();
		double et = wave.getEndTime();
		double sr = wave.getSamplingRate();
		
		// search, in .25 second chunks, for the start of low amplitude period after 21.25hz signal
		Wave w;
		
		do
		{
			st += 0.25;
			w = wave.subset(st, st+0.25);
//			System.out.print(":: " + w.max() + "-" + w.mean() + "=" +Math.abs(w.max() - w.mean()) +"@"+Util.j2KToDateString(st));
		}		
		while (st < et - 0.5 && Math.abs(w.max() - w.mean()) > 200);
		
		endPreambleSignal = st;
		endPreambleString = "abs (max: " + w.max() + "- mean: " + w.mean() + ") = " + Math.abs(w.max() - w.mean());

		wave = wave.subset(st, et);
		double mean = wave.mean();
		
		for (int i=0; i<wave.buffer.length; i++)
			wave.buffer[i] -= mean;

		wave.invalidateStatistics();
		
		// search for end of low amplitude period
		double min = wave.subset(st, st+.25).min();
		int i = 0;
		while (wave.buffer[i] > 2*min)
			i++;
		while (wave.buffer[i] < wave.buffer[i-1])
			i--;
		i++;
		endPreambleQuiet = st + i/sr;
		
		// skip 17 seconds of instrument response pulses
		i += sr * 17;

		// search for first bit
		while (Math.abs(wave.buffer[i]) < 500)
			i++;
		
		beginGain = st+i/sr;
		// decode gain
		for (int j=0; j<3; j++) 
		{
			gain += decodeBit(i) * Math.pow(2, j);
			i += sr;
		}
		gain *= 10;

		beginId = st+i/sr;
		// decode unit id
		for (int j=0; j<10; j++)
		{
			unitId += decodeBit(i) * Math.pow(2, 9-j);
			i += sr;
		}
		
		beginVolt=st+i/sr;
		// voltage pulses present?
		double previous2Seconds = 0;
		double next2Seconds = 0;
		for (int j=0; j<2*sr; j++) {
			previous2Seconds += Math.abs(wave.buffer[i-j]);
			next2Seconds += Math.abs(wave.buffer[i+j]);
		}
		
		// decode voltage
		if (next2Seconds > previous2Seconds * .9)
		{
			voltage = 0;
			for (int j=0; j<12; j++)
			{
				voltage += decodeBit(i) * Math.pow(2, (11-j));
				i += sr;
			}
			voltage /= 80;
		}
	}
}