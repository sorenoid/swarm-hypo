package gov.usgs.winston.monitor.server;

import gov.usgs.net.Command;
import gov.usgs.net.NetTools;
import gov.usgs.util.Util;
import gov.usgs.vdx.data.wave.Wave;
import gov.usgs.winston.monitor.MonitorServer;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 *
 * @author Tom Parker
 */
abstract public class BaseCommand implements Command
{
	protected NetTools netTools;
	protected MonitorServer mon;
	
	public BaseCommand(NetTools nt, MonitorServer mon)
	{
		netTools = nt;
		this.mon = mon;
	}
	
	protected void sendNoChannelResponse(String id, int pin, String s, String c, String n, String l, SocketChannel channel)
	{
		String loc = "";
		if (l != null)
			loc = " " + l;
		netTools.writeString(id + " " + id + " " + pin + " " + s + " " + c + " " + n + loc + " FN\n", channel);
	}

	protected int writeByteBuffer(String id, ByteBuffer bb, boolean compress, SocketChannel channel)
	{
		if (bb == null)
		{
			netTools.writeString(id + " 0\n", channel);
			return 0;
		}
		if (compress)
			bb = ByteBuffer.wrap(Util.compress(bb.array()));
		
		netTools.writeString(id + " " + bb.limit() + "\n", channel);
		return netTools.writeByteBuffer(bb, channel);
	}
	
	protected String getError(double[] d)
	{
		if (d == null || d.length != 2)
			return "";

		if (Double.isNaN(d[0]) && Double.isNaN(d[1]))
			return "FB";

		if (Double.isNaN(d[0]))
			return "FL s4 " + Double.toString(Util.j2KToEW(d[1]));

		if (Double.isNaN(d[1]))
			return "FR s4 " + Double.toString(Util.j2KToEW(d[0]));

		return "OK";
	}

	protected boolean allowTransaction(double[] d)
	{
		return !(d == null || d.length != 2 || Double.isNaN(d[0]) || Double.isNaN(d[1]));
	}

//		protected double[] checkTimes(int sid, double t1, double t2)
//		{
//			if (t1 >= t2)
//				return new double[] { Double.NaN, Double.NaN };
//
//			double[] tb = data.getTimeSpan(sid);
//
//			if (t1 < tb[0])
//				t1 = tb[0];
//
//			// only apply the later bounds check if there is an embargo otherwise we
//			// have to deal
//			// with other people's idea of what now is
//			if (t2 > tb[1])
//				t2 = tb[1];
//
//			if (t2 < tb[0])
//				return new double[] { Double.NaN, tb[0] };
//
//			if (t1 > tb[1])
//				return new double[] { tb[1], Double.NaN };
//
//			//		if (t2 - t1 > winstonWaveServer.getMaxDataSize())
//			//			return new double[] { Double.NaN, Double.NaN };
//
//			return new double[] { t1, t2 };
//		}
	
	public void writeWaveAsAscii(Wave wave, int sid, String id, String s, String c, String n, String l, double t1, double t2, String fill, SocketChannel channel)
	{
		NumberFormat numberFormat = new DecimalFormat("#.######");
		String sts = null;

		// find first sample time
		double ct = wave.getStartTime() - wave.getRegistrationOffset();
		double dt = 1 / wave.getSamplingRate();
		for (int i = 0; i < wave.numSamples(); i++)
		{
			if (ct >= (t1 - dt / 2))
				break;
			ct += dt;
		}
		sts = numberFormat.format(Util.j2KToEW(ct));
		ByteBuffer bb = ByteBuffer.allocate(wave.numSamples() * 13 + 256);
		bb.put(id.getBytes());
		bb.put((byte)' ');
		bb.put(Integer.toString(sid).getBytes());
		bb.put((byte)' ');
		bb.put(s.getBytes());
		bb.put((byte)' ');
		bb.put(c.getBytes());
		bb.put((byte)' ');
		bb.put(n.getBytes());
		if (l != null)
		{
			bb.put((byte)' ');
			bb.put(l.getBytes());
		}
		bb.put(" F s4 ".getBytes());
		bb.put(sts.getBytes());
		bb.put((byte)' ');
		bb.put(Double.toString(wave.getSamplingRate()).getBytes());
		//bb.put(" \n".getBytes());
		bb.put(" ".getBytes());
		int sample;
		ct = wave.getStartTime();
//		int samples = 0;
		for (int i = 0; i < wave.numSamples(); i++)
		{
			if (ct >= (t1 - dt / 2))
			{
//				samples++;
				sample = wave.buffer[i];
				if (sample == Wave.NO_DATA)
					bb.put(fill.getBytes());
				else
					bb.put(Integer.toString(wave.buffer[i]).getBytes());
				bb.put((byte)' ');
			}
			ct += dt;
			if (ct >= t2)
				break;
		}
		bb.put((byte)'\n');
		bb.flip();
		netTools.writeByteBuffer(bb, channel);
	}

}

