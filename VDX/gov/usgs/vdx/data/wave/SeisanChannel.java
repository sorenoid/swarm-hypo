package gov.usgs.vdx.data.wave;

import java.util.Calendar;

/**
 * This class holds meta data for a Seisan channel
 * 
 * @author Chirag Patel
 *
 */
public class SeisanChannel {
    
	public final SimpleChannel channel;
	private String firstLocationCode;
	private Integer year;
	private String secondLocationCode;
	private String doy;
	private Integer month;
	private Integer day;
	private Integer hour;
	private Integer minute;
	private Float second;
	private Float sampleRate;
	private Integer numberOfSamples;
	private int[] data;
	private double startTime; 
	
	/**
	 * parses and generates channel info from a String
	 * 
	 * @param header
	 */
	public SeisanChannel(String header){
        channel = new SimpleChannel(null,
                header.substring(16,17),
                header.substring(0,5).trim(),
                header.substring(5,7).trim(),
                header.substring(7,8).trim());
        
		year = header.substring(9,12).trim().length() == 0?null:Integer.parseInt(header.substring(9,12).trim());
		secondLocationCode = header.substring(12,13).trim();
		doy  = header.substring(13,16).trim();
		month = header.substring(17,19).trim().length() == 0?null:Integer.parseInt(header.substring(17,19).trim());
		day  = header.substring(20,22).trim().length() == 0?null:Integer.parseInt(header.substring(20,22).trim());
		hour  = header.substring(23,25).trim().length() == 0?null:Integer.parseInt(header.substring(23,25).trim());
		minute  = header.substring(26,28).trim().length() == 0?null:Integer.parseInt(header.substring(26,28).trim());
		second  = header.substring(29,35).trim().length() == 0?null:Float.parseFloat(header.substring(29,35).trim());

		sampleRate = header.substring(36,43).trim().length()==0?null:Float.parseFloat(header.substring(36,43).trim());
		numberOfSamples = header.substring(43,50).trim().length()==0?null:Integer.parseInt(header.substring(43,50).trim());
		
		Calendar c  = Calendar.getInstance();
		c.set(Calendar.YEAR, (year + 1900));
		c.set(Calendar.MONTH, month-1);
		c.set(Calendar.DAY_OF_MONTH, day);
		c.set(Calendar.HOUR_OF_DAY, hour);
		c.set(Calendar.MINUTE, minute);
		c.set(Calendar.SECOND, second.intValue());
		startTime  = c.getTimeInMillis();
		
		
		System.out.println(c.getTime().toGMTString());
		System.out.println(year + 1900+ " - " + month + " - " + day + "  " + hour + " : " + minute + ":" + ":"+second);
		
	}

	public Float getSampleRate() {
		return sampleRate;
	}



	public Integer getNumberOfSamples() {
		return numberOfSamples;
	}


	public int[] getData() {
		return data;
	}

	public void setData(int[] data) {
		this.data = data;
	}
	
	public Wave toWave()
	{
		Wave sw = new Wave();
		sw.setSamplingRate(sampleRate);
		sw.setStartTime(startTime);
		sw.buffer = data;
		return sw;
	}
	
	
	public String toString(){
		return channel.stationCode + ", " + channel.firstTwoComponentCode + ", "+ firstLocationCode + ","+channel.lastComponentCode+","+sampleRate+","+numberOfSamples;
	}
	
	
	public static class SimpleChannel {
        public final String networkName;
        public final String stationCode;
        public final String firstTwoComponentCode;
        public final String lastComponentCode;
        public final String toString;


        public SimpleChannel(String toString, String networkName, String stationCode, String firstTwoComponentCode, String lastComponentCode) {
            this.networkName = networkName;
            this.stationCode = stationCode;
            this.firstTwoComponentCode = firstTwoComponentCode;
            this.lastComponentCode = lastComponentCode;
            this.toString = toString != null ? toString : generateString();
        }

        public String toString() {
            return toString;
        }

        // TODO: should do generateString for display only. Should see how SCNL does parsing and follow that convention
        // for toString and parse
        public String generateString() {
            return (((networkName == null || networkName.trim().length() == 0) ? "--"
                    : networkName)
                    + "  "
                    + ((stationCode == null || stationCode.trim().length() == 0) ? "--"
                    : stationCode)
                    + "  "
                    + ((firstTwoComponentCode == null || firstTwoComponentCode.trim().length() == 0) ? ((lastComponentCode == null || lastComponentCode
                    .trim().length() == 0) ? "--"
                    : lastComponentCode)
                    : ((lastComponentCode == null || lastComponentCode
                    .trim().length() == 0) ? firstTwoComponentCode
                    : firstTwoComponentCode + lastComponentCode)));
        }

        public static SimpleChannel parse(String channel) {
            try {
                String compressed = channel.replace("  ", " ");
                String[] split = compressed.split(" ");
                String network = "--".equals(split[0]) ? null : split[0];
                int len = split[2].length();
                String c1 = split[2].substring(0, len-1);
                String c2 = split[2].substring(len-1, len);
                return new SimpleChannel(channel, network, split[1], c1, c2);
            } catch (Exception e) {
                System.out.println("Could not parse channel: "+channel);
                return new SimpleChannel(channel, null, null, null, null);
            }
        }

        public void populateSAC(SAC sac) {
            sac.kstnm = stationCode;
            sac.kcmpnm = firstTwoComponentCode+lastComponentCode;
            sac.knetwk = networkName;
        }

        public String showStationCode() {
            return stationCode == null ? "" : stationCode;
        }

        public String showNetworkName() {
            return networkName == null ? "" : networkName;
        }

        public String showFirstTwoComponent() {
            return firstTwoComponentCode == null ? "" : firstTwoComponentCode;
        }

        public boolean isPopulated() {
            return hasData(stationCode) && hasData(firstTwoComponentCode) && hasData(lastComponentCode);
        }

        // TODO: move to general place and use
        public static boolean hasData(String s) {
            return s != null && !s.isEmpty();
        }

        public String fullComponent() {
            return firstTwoComponentCode+lastComponentCode;
        }
    }

    

	
}
