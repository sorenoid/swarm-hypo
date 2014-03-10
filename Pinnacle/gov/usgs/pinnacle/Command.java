package gov.usgs.pinnacle;

import gov.usgs.util.CurrentTime;

/**
 * <p>A class for constructing byte packets for commands to send to a 
 * Pinnacle Tiltmeter.</p>
 * 
 * $Log: not supported by cvs2svn $
 * @author Dan Cervelli
 */
public class Command
{
	/**
	 * <p>Compute byte array checksum</p>
	 */
	public static void setChecksum(byte[] buf)
	{
		int total = 0;
		for (int i = 0; i < buf.length - 1; i++)
			total += buf[i];
		
		buf[buf.length - 1] = (byte)((0xff & total)); 
	}
	
	/**
	 * <p>Construct ACK command</p>
	 * @param id device serial id
	 */
	public static byte[] getACK(int id)
	{
		byte[] cmd = new byte[5];
		cmd[0] = 0x16;
		cmd[1] = (byte)(0xff & id);
		cmd[2] = (byte)((0xff00 & id) >> 8);
		cmd[3] = 0x28;
		setChecksum(cmd);
		return cmd;
	}
	
	/**
	 * <p>Construct 38400Baud command</p>
	 * @param id device serial id
	 */
	public static byte[] get38400Baud(int id)
	{
		byte[] cmd = new byte[5];
		cmd[0] = 0x16;
		cmd[1] = (byte)(0xff & id);
		cmd[2] = (byte)((0xff00 & id) >> 8);
		cmd[3] = 0x03;
		setChecksum(cmd);
		return cmd;
	}
	
	/**
	 * <p>Construct status request command</p>
	 * @param id device serial id
	 */
	public static byte[] getStatus(int id)
	{
		byte[] cmd = new byte[5];
		cmd[0] = 0x16;
		cmd[1] = (byte)(0xff & id);
		cmd[2] = (byte)((0xff00 & id) >> 8);
		cmd[3] = 0x00;
		setChecksum(cmd);
		return cmd;
	}

	/**
	 * <p>Construct get current block index command</p>
	 * @param id device serial id
	 */
	public static byte[] getCurrentBlockIndex(int id)
	{
		byte[] cmd = new byte[5];
		cmd[0] = 0x16;
		cmd[1] = (byte)(0xff & id);
		cmd[2] = (byte)((0xff00 & id) >> 8);
		cmd[3] = 0x1c;
		setChecksum(cmd);
		return cmd;
	}
	
	/**
	 * <p>Construct Stop Data Acquisition command</p>
	 * @param id device serial id
	 */
	public static byte[] stopDA(int id)
	{
		byte[] cmd = new byte[5];
		cmd[0] = 0x16;
		cmd[1] = (byte)(0xff & id);
		cmd[2] = (byte)((0xff00 & id) >> 8);
		cmd[3] = 0x0b;
		setChecksum(cmd);
		return cmd;
	}
	
	/**
	 * <p>Construct Start Data Acquisition command</p>
	 * @param id device serial id
	 */
	public static byte[] startDA(int id)
	{
		byte[] cmd = new byte[5];
		cmd[0] = 0x16;
		cmd[1] = (byte)(0xff & id);
		cmd[2] = (byte)((0xff00 & id) >> 8);
		cmd[3] = 0x0a;
		setChecksum(cmd);
		return cmd;
	}
	
	/**
	 * <p>Construct Get Block command</p>
	 * @param id device serial id
	 * @param block block number
	 */
	public static byte[] getBlock(int id, int block)
	{
		byte[] cmd = new byte[7];
		cmd[0] = 0x16;
		cmd[1] = (byte)(0xff & id);
		cmd[2] = (byte)((0xff00 & id) >> 8);
		cmd[3] = 0x19;
		cmd[4] = (byte)(0xff & block);
		cmd[5] = (byte)((0xff00 & block) >> 8);
		setChecksum(cmd);
		return cmd;
	}
	
	/**
	 * <p>Construct Get Data Header command</p>
	 * @param id device serial id
	 * @param block block number
	 */
	public static byte[] getDataHeader(int id, int block)
	{
		byte[] cmd = new byte[7];
		cmd[0] = 0x16;
		cmd[1] = (byte)(0xff & id);
		cmd[2] = (byte)((0xff00 & id) >> 8);
		cmd[3] = 0x1b;
		cmd[4] = (byte)(0xff & block);
		cmd[5] = (byte)((0xff00 & block) >> 8);
		setChecksum(cmd);
		return cmd;
	}
	
	/**
	 * <p>Construct ReZero command</p>
	 * @param id device serial id
	 */
	public static byte[] getRezeroNow(int id)
	{
		byte[] cmd = new byte[5];
		cmd[0] = 0x16;
		cmd[1] = (byte)(0xff & id);
		cmd[2] = (byte)((0xff00 & id) >> 8);
		cmd[3] = 0x1f;
		setChecksum(cmd);
		return cmd;
	}
	
	/**
	 * <p>The same as startDA(int id)</p>
	 */
	public static byte[] getStartDataAcquisition(int id)
	{
		byte[] cmd = new byte[5];
		cmd[0] = 0x16;
		cmd[1] = (byte)(0xff & id);
		cmd[2] = (byte)((0xff00 & id) >> 8);
		cmd[3] = 0x0a;
		setChecksum(cmd);
		return cmd;
	}
	
	/**
	 * <p>Construct Set Gain command</p>
	 * @param id device serial id
	 * @param gain ampl gain (0,1,2,3)
	 */
	public static byte[] getSetGain(int id, int gain)
	{
		byte[] cmd = new byte[6];
		cmd[0] = 0x16;
		cmd[1] = (byte)(0xff & id);
		cmd[2] = (byte)((0xff00 & id) >> 8);
		cmd[3] = 0x22;
		cmd[4] = (byte)(0xff & gain);
		setChecksum(cmd);
		return cmd;
	}
	
	/**
	 * <p>Construct Set Sample Rate command</p>
	 * @param id device serial id
	 * @param sr sample rate to set (1-255 sec/sample)
	 */
	public static byte[] getSetSampleRate(int id, int sr)
	{
		byte[] cmd = new byte[6];
		cmd[0] = 0x16;
		cmd[1] = (byte)(0xff & id);
		cmd[2] = (byte)((0xff00 & id) >> 8);
		cmd[3] = 0x0f;
		cmd[4] = (byte)(0xff & sr);
		setChecksum(cmd);
		return cmd;
	}
	
	/**
	 * <p>Construct Set Clock command</p>
	 * @param id device serial id
	 */
	public static byte[] getSetClock(int id)
	{
		byte[] cmd = new byte[9];
		cmd[0] = 0x16;
		cmd[1] = (byte)(0xff & id);
		cmd[2] = (byte)((0xff00 & id) >> 8);
		cmd[3] = 0x14;
		int sec = (int)Math.round(CurrentTime.getInstance().now() / 1000.0);
		cmd[4] = (byte)((0xff000000 & sec) >> 24);
		cmd[5] = (byte)((0xff0000 & sec) >> 16);
		cmd[6] = (byte)((0xff00 & sec) >> 8);
		cmd[7] = (byte)(0xff & sec);
		setChecksum(cmd);
		return cmd;
	}
}
