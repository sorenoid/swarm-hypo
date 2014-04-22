package gov.usgs.vdx.data.wave;


import gov.usgs.util.Util;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;


/**
 * 
 * Holds data representing a Seisan file. This includes channel headers and channel data
 * 
 * @author Chirag Patel
 *
 */
public class SeisanFile {
	
	private ArrayList<SeisanChannel> channels = new ArrayList<SeisanChannel>();
	private int noOfChannels;
	private Integer year;
	private Integer month;
	private Integer day;
	private Integer hour;
	private Integer minute;
	private Float second;
	private String fileName;
	private Date startDate;
	private FileFlag fileFlag;
	private LengthFlag lengthFlag;
	
	
	/**
	 * Tries to read data from seisan file.
	 * <br />
	 * Function reads both headers and channel header and data for wave file.
	 * 
	 * 
	 * @param filename
	 * @throws IOException
	 */
	public void read(String filename) throws IOException{
		fileName = filename;
		FileInputStream fis = new FileInputStream(filename);
		BufferedInputStream buf = new BufferedInputStream(fis);
		DataInputStream dis = new DataInputStream(buf);
		
		String data  = readFileHeader(dis,80);
		System.out.println(data);
		noOfChannels = Integer.parseInt(data.substring(30,33).trim());
		
		year =  data.substring(33,36).trim().length() == 0?null:Integer.parseInt(data.substring(33,36).trim());
		month = data.substring(41,43).trim().length() == 0?null:Integer.parseInt(data.substring(41,43).trim());
		day  =  data.substring(44,46).trim().length() == 0?null:Integer.parseInt(data.substring(44,46).trim());
		hour  = data.substring(48,49).trim().length() == 0?null:Integer.parseInt(data.substring(48,49).trim());
		minute  = data.substring(50,52).trim().length() == 0?null:Integer.parseInt(data.substring(50,52).trim());
		second  = data.substring(53,59).trim().length() == 0?null:Float.parseFloat(data.substring(53,59).trim());

		Calendar c  = Calendar.getInstance();
		c.set(Calendar.YEAR, (year));
		c.set(Calendar.MONTH, month-1);
		c.set(Calendar.DAY_OF_MONTH, day);
		c.set(Calendar.HOUR_OF_DAY, hour);
		c.set(Calendar.MINUTE, minute);
		c.set(Calendar.SECOND, second.intValue());
		startDate= c.getTime();
		
	    
	    int number_of_lines = (noOfChannels/ 3) + (noOfChannels % 3 + 1);
	    
	    if (number_of_lines < 10){
	        number_of_lines = 10;
	    }
	    dis.skipBytes(88);
	    dis.skipBytes((number_of_lines)*88);
	    
	    for(int i = 0 ; i < noOfChannels; i++){
	    	readChannel(dis);
	    }
	    dis.close();
	}
	

	/**
	 * Reads a single channel from the InputStream and adds it to the list of channels for this seisan file.
	 * 
	 * @param dis
	 * @throws IOException
	 */
	private void readChannel(DataInputStream dis) throws IOException{
		String channelHeader = readLine(dis,1040);
		SeisanChannel channel = new SeisanChannel(channelHeader);
		readChannelData(dis,channel);
		channels.add(channel);
	}

	
	/**
	 * 
	 * reads channel wave data from the InputStream for the specified {@link SeisanChannel}
	 * 
	 * @param dis
	 * @param channel
	 * @throws IOException
	 */
	private void readChannelData(DataInputStream dis, SeisanChannel channel) throws IOException{
		dis.skipBytes(4);
		int[] wave_data = new int[channel.getNumberOfSamples()];
	    
	   
		int index = 0;
		while(index != channel.getNumberOfSamples()){
			byte[] bytes = new byte[4];
			dis.read(bytes);
			int val = byteArrayToInt(bytes);
		    wave_data[index] = val;
		    index ++;
		}	
		dis.skipBytes(4);
	    channel.setData(wave_data);
	}
	
	
	private int byteArrayToInt(byte[] b) {
	    final ByteBuffer bb = ByteBuffer.wrap(b);
	    bb.order(ByteOrder.LITTLE_ENDIAN);
	    return bb.getInt();
	}
	
	
	
	/**
	 * Reads a single line with a specified length of information  from the InputStream
	 * @param dis
	 * @param length
	 * @return
	 * @throws IOException
	 */
	private String readLine(DataInputStream dis, int length) throws IOException{
		    int len = lengthFlag.getLength();
			byte[] bytes  = new byte[length + len];
		    dis.read(bytes);
	        int end = length + len;
	        int start = len;
	        String data = new String (bytes).trim();
	        if(data.length()<(length + len))
	        	return data;
	        else
	        	 return data.substring(start,end);
	}
	
	private String readFileHeader(DataInputStream dis, int length) throws IOException{
	    byte[] bytes  = detectParameters(dis,length);
	    //dis.read(bytes);
        int byteLength = lengthFlag.getLength();
		int end = length + byteLength;
        int start = byteLength;
        String data = new String (bytes).trim();
        if(data.length()<(length + byteLength))
        	return data;
        else
        	 return data.substring(start,end);
	}
	
	private byte[] detectParameters(DataInputStream dis,int length){
		byte[] bytes = new byte[length+8];
		try {
			dis.read(bytes);
		} catch (IOException e) {
			e.printStackTrace();
		}
		int i = 0;
		int PFLAG = -1;
		do{
			if(bytes[i] == 80){
				PFLAG = i;
			}
			i++;
		}while(bytes[i]==0);
		
		if(3 == i || 4 == i){
			lengthFlag = lengthFlag.FOUR;
		}else if(7 == i || 8 == i){
			lengthFlag = lengthFlag.EIGHT;
		}		

		if(-1 == PFLAG){
			fileFlag = fileFlag.PC;
		}else{
			fileFlag = fileFlag.SUN;
		}
		
		return bytes;
	}
	
	

	public ArrayList<SeisanChannel> getChannels() {
		return channels;
	}


	
	/**
	 * Return a {@link Wave} as a summation of all wave data for each SeisanChannel 
	 * 
	 * @return
	 */
	public Wave toWave()
	{
		Wave sw = new Wave();
		int size = 0;
		Float sampleRate = channels.get(0).getSampleRate();
		
		for(SeisanChannel c : channels){
			if(sampleRate >= c.getSampleRate()){
				sampleRate = c.getSampleRate();
			}
			size += c.getData().length;
		}
		sw.setStartTime(Util.dateToJ2K(startDate));
		sw.setSamplingRate(sampleRate);
		sw.buffer = new int[size];
		int index = 0;
		for (int i = 0; i < channels.size(); i++)
		{
			SeisanChannel c = channels.get(i);
			int[] data = c.getData();
			for(int j = 0; j < data.length; j++){
				sw.buffer[index] = data[j];
				index ++;
			}
			
		}

		return sw;
	}


	public String getStationInfo() {
		return (new File(fileName)).getName();
	}
	
	
}
