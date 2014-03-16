/*
 * This code is derived from:
 * 
 The TauP Toolkit: Flexible Seismic Travel-Time and Raypath Utilities.
 Copyright (C) 1998-2000 University of South Carolina

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

 The current version can be found at
 <A HREF="www.seis.sc.edu">http://www.seis.sc.edu</A>

 Bug reports and comments should be directed to
 H. Philip Crotwell, crotwell@seis.sc.edu or
 Tom Owens, owens@seis.sc.edu
 */

package gov.usgs.vdx.data.wave;

import gov.usgs.util.Log;
import gov.usgs.util.Util;

import java.io.*;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;

/**
 * A class to read WIN files, adapted from Fissures code, via WIN.java
 */
public class WIN
{
	protected final static Logger logger = Log.getLogger("gov.usgs.vdx.data.wave.WIN");

    public static class ChannelData {
        public int packetSize;
        public long year;
        public long month;
        public long day;
        public long hour;
        public long minute;
        public long second;
        public int channel_num;
        public int data_size;
        public float sampling_rate;
        public Vector<Integer> in_buf;
    }

    List<ChannelData> channelData = new ArrayList<ChannelData>();

//	public float[] y;
//	int yLen;
//	public float[] x;
//	public float[] real;
//	public float[] imaginary;
//	public float[] amp;
//	public float[] phase;
//	int baseVal;
	
//	public String kstnm = STRING8_UNDEF;  //
//	public String kcmpnm = STRING8_UNDEF;
//	public String knetwk = STRING8_UNDEF;

	// undef values for WIN
//	public static float FLOAT_UNDEF = -12345.0f;
//	public static int INT_UNDEF = -12345;
//	public static String STRING8_UNDEF = "-12345  ";
//	public static String STRING16_UNDEF = "-12345          ";

	/* TRUE and FALSE defined for convenience. */
//	public static final int TRUE = 1;
//	public static final int FALSE = 0;

	/* Constants used by WIN. */

	/**
     * reads the WIN file specified by the filename.
	 * @throws IOException if not found or some read error
	 */
	public List<ChannelData> read(String filename) throws IOException
	{
		FileInputStream fis = new FileInputStream(filename);
		BufferedInputStream buf = new BufferedInputStream(fis);
		DataInputStream dis = new DataInputStream(buf);
		while (dis.available() != 0)
		{
            ChannelData cur = new ChannelData();
            cur.in_buf = new Vector<Integer>();
            readHeader(cur, dis);
			readData(cur, dis);
            channelData.add(cur);
		}
		dis.close();
        return channelData;
	}

	static private int intFromSingleByte (byte b)
	{
		return b;
	}

	private static int intFromFourBytes (byte[] bites)
	{
		ByteBuffer wrapped = ByteBuffer.wrap(bites); 
		return wrapped.getInt ();
	}
	
	private static int intFromThreeBytes (byte[] bites)
	{
        // TODO: not sure about the endianness here
        byte[] padded = new byte[] { 0, bites[2], bites[1], bites[0] };
		ByteBuffer wrapped = ByteBuffer.wrap(padded);
		return wrapped.getInt ();
	}
	
	private static short shortFromTwoBytes (byte[] bites)
	{
		ByteBuffer wrapper = ByteBuffer.wrap(bites);
		return wrapper.getShort();
	
	}
	
	private static int decodeBcd(byte[] b) 
	{
	    StringBuffer buf = new StringBuffer(b.length * 2);
	    for (int i = 0; i < b.length; ++i) {
	        buf.append((char) (((b[i] & 0xf0) >> 4) + '0'));
	        if ((i != b.length) && ((b[i] & 0xf) != 0x0A)) 
	            buf.append((char) ((b[i] & 0x0f) + '0'));
	    }
	    return Integer.parseInt(buf.toString());
	}   
	/** reads the header from the given stream.	 
	 * @param dis DataInputStream to read WIN from
	 * @throws FileNotFoundException if the file cannot be found
	 * @throws IOException if it isn't a WIN file 
	 */
	public void readHeader(ChannelData c, DataInputStream dis) throws FileNotFoundException, IOException
	{
		//read first 4 byte: file size
		byte[] fourBytes = new byte[4];
		byte[] oneByte   = new byte[1];
		
		dis.readFully(fourBytes);	
		c.packetSize = intFromFourBytes (fourBytes);
	
		
		//read next 6 bytes: yy mm dd hh mi ss
		dis.readFully (oneByte);
		c.year = 2000 + decodeBcd(oneByte);
		
		dis.readFully(oneByte);
		c.month = decodeBcd(oneByte);
		
		dis.readFully(oneByte);
		c.day = decodeBcd(oneByte);
		
		dis.readFully(oneByte);
		c.hour = decodeBcd(oneByte);
		
		dis.readFully(oneByte);
		c.minute = decodeBcd(oneByte);
		
		dis.readFully(oneByte);
		c.second = decodeBcd(oneByte);
	}

	/**
     * read the data portion of WIN format from the given stream
	 * @param dis DataInputStream to read WIN from
	 * @throws IOException if it isn't a WIN file
	 */
	public void readData(ChannelData c, DataInputStream dis) throws IOException
	{
		int bytesRead = 10;
		
		do 
		{	
			byte[] oneByte = new byte[1];
			dis.readFully(oneByte);
		
			dis.readFully(oneByte);
			c.channel_num = intFromSingleByte(oneByte[0]);
		
			dis.readFully(oneByte);
			c.data_size = intFromSingleByte(oneByte[0])>>4;
		
			dis.readFully(oneByte);
			c.sampling_rate = intFromSingleByte(oneByte[0]); // TODO: needs lower 4 bits of above byte included

			byte[] fourBytes = new byte[4];
			dis.readFully(fourBytes);
			int accum = intFromFourBytes (fourBytes);

			float[] d = new float[(int) c.sampling_rate - 1];
			System.out.println ("base: " + accum);
	
			bytesRead += 8;
            if (c.data_size == 0) {
                throw new RuntimeException("sample size of 0 is unimplemented");
            }
			else if (c.data_size == 1)
			{
				for (int ix = 0; ix < ((int) c.sampling_rate - 1); ix++)
				{
					accum += dis.readByte();
					c.in_buf.add(accum);
					bytesRead ++;
				}	
			}
			else if (c.data_size == 2)
			{
				byte[] twoBytes = new byte[2];
				for (int ix = 0; ix < ((int) c.sampling_rate - 1); ix++)
				{		
					dis.readFully(twoBytes);
					accum += shortFromTwoBytes (twoBytes);
					d[ix] = accum;
					c.in_buf.add (accum);
//					System.out.println ("data bytes: " + d[ix]);
					bytesRead += 2;
				}	
			}
			else if (c.data_size == 3)
			{
				byte[] threeBytes = new byte[3];
				for (int ix = 0; ix < ((int) c.sampling_rate - 1); ix++)
				{		
					dis.readFully(threeBytes);
					accum += intFromThreeBytes (threeBytes);
					d[ix] = accum;
					bytesRead += 3;
				}	
			}
			else if (c.data_size == 4)
			{
				for (int ix = 0; ix < ((int) c.sampling_rate - 1); ix++)
				{		
					dis.readFully (fourBytes);
					accum += intFromFourBytes (fourBytes);
					
					d[ix] = accum;
					bytesRead += 4;
				}	
			}
		} while (bytesRead < c.packetSize);
	}

	/**
	 * Create Wave object from WIN data
	 * @return wave created
	 */
	public Wave[] toWave()
	{
        Wave[] waves = new Wave[channelData.size()];
        for (int i=0; i<waves.length; i++) {
            Wave sw = new Wave();
            ChannelData c = channelData.get(i);
            sw.setStartTime(Util.dateToJ2K(getStartTime(c)));
            sw.setSamplingRate(c.sampling_rate);
            sw.buffer = new int[c.in_buf.size()];
            for (int j = 0; j < c.in_buf.size(); j++)
            {
                sw.buffer[j] = Math.round(c.in_buf.elementAt(j));
            }
            waves[i] = sw;
        }

		return waves;
	}

	/**
	 * Get start time of data
	 * @return start time of data
	 */
	public Date getStartTime(ChannelData c)
	{
		String ds = c.year + "," + c.day + "," + c.hour + "," + c.minute + "," + c.second + ",0.0";
		SimpleDateFormat format = new SimpleDateFormat("yyyy,DDD,HH,mm,ss,SSS");
		format.setTimeZone(TimeZone.getTimeZone("GMT"));
		Date d = null;
		try
		{
			d = format.parse(ds);
		}
		catch (ParseException e)
		{
			e.printStackTrace();
		}
		return d;
	}

//	/** just for testing. Reads the filename given as the argument,
//	 *  writes out some header variables and then
//	 *  writes it back out as "outWINfile".
//	 * @param args command line args
//	 */
//	public static void main(String[] args)
//	{
//		WIN data = new WIN();
//
//		if (args.length != 1)
//		{
//			System.out.println("Usage: java gov.usgs.vdx.data.wave.WIN WINsourcefile ");
//			//return;
//		}
//
//		try
//		{
//			data.read(args[0]);
//			data.printHeader();
//			System.out.println(Util.formatDate(data.getStartTime()));
//			System.out.println(data.getSamplingRate());
//			Wave sw = data.toWave();
//			System.out.println(sw);
//		}
//		catch (FileNotFoundException e)
//		{
//			System.out.println("File " + args[0] + " doesn't exist.");
//		}
//		catch (IOException e)
//		{
//			System.out.println("IOException: " + e.getMessage());
//		}
//	}
}