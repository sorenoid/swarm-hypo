package gov.usgs.util;

import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * DriveSwamp, created Sep 2, 2004.
 * Test class to check disk performance
 *
 * @author Dan Cervelli
 */
public class DriveSwamp
{
	private Thread readThread;
	private Thread writeThread;
	private FileChannel[] files;

	/**
	 * Constructor. Create set of test files, read them, then write into them.
	 * 
	 * @param numFiles quantity of files in the test case
	 * @param size size of each test file, in bytes
     * @param readSize how many bytes we want to read from each file
     * @param writeSize how many bytes we want to write in each file 
	 * @param sleep pause between test files processing during reading and writing, in ms
	 * @throws Exception
	 */
	@SuppressWarnings("resource")
	public DriveSwamp(final int numFiles, final int size, final int readSize, final int writeSize, final int sleep) throws Exception
	{
		files = new FileChannel[numFiles];
		ByteBuffer bb = ByteBuffer.allocateDirect(size);
		for (int i = 0; i < size; i++)
			bb.put((byte)i);
		
		// first create test files
		for (int i = 0; i < numFiles; i++)
		{
			bb.flip();
			FileChannel out = new FileOutputStream("swamp_" + i + ".tmp").getChannel();
			while (bb.hasRemaining())
				out.write(bb);
			
			files[i] = out;
		}
		
		readThread = new Thread(new Runnable()
				{
					public void run()
					{
						ByteBuffer bb = ByteBuffer.allocateDirect(readSize);
						while (true)
						{
							try
							{
								CodeTimer ct = new CodeTimer("read");
								int i = (int)Math.floor(Math.random() * numFiles);
								FileChannel in = new RandomAccessFile("swamp_" + i + ".tmp", "r").getChannel();
								int p = (int)Math.floor(Math.random() * (size - readSize));
								in.position(p);
								bb.clear();
								in.read(bb, readSize);
								in.close();
								ct.stopAndReport();
								Thread.sleep(sleep);
							}
							catch (Exception e)
							{
								e.printStackTrace();
							}
						}
					}
				});
		readThread.start();
		
		writeThread = new Thread(new Runnable()
				{
					public void run()
					{
						ByteBuffer bb = ByteBuffer.allocateDirect(writeSize);
						for (int i = 0; i < writeSize; i++)
							bb.put((byte)i);
						while (true)
						{
							try
							{
								CodeTimer ct = new CodeTimer("write");
								int i = (int)Math.floor(Math.random() * numFiles);
								FileChannel out = files[i];
								bb.flip();
								out.write(bb);
								ct.stopAndReport();
								Thread.sleep(sleep);
							}
							catch (Exception e)
							{
								e.printStackTrace();
							}
						}
					}
				});
		writeThread.start();
	}

	/**
	 * Built-in testcase
	 * @param args command-line arguments: number of test files, file size, reading size, writing size, pause.
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception
	{
		int numFiles = Integer.parseInt(args[0]);
		int size = Integer.parseInt(args[1]);
		int readSize = Integer.parseInt(args[2]);
		int writeSize = Integer.parseInt(args[3]);
		int sleep = Integer.parseInt(args[4]);
		new DriveSwamp(numFiles, size, readSize, writeSize, sleep);
	}
}
