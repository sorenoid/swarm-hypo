package gov.usgs.math;

/**
 * From:
 * http://williams.best.vwh.net/sunrise_sunset_algorithm.htm
 *  
 * Source:
 * 	Almanac for Computers, 1990
 * 	published by Nautical Almanac Office
 * 	United States Naval Observatory
 * 	Washington, DC 20392
 * 
 * Inputs:
 * 	day, month, year:      date of sunrise/sunset
 * 	latitude, longitude:   location for sunrise/sunset
 * 	zenith:                Sun's zenith for sunrise/sunset
 * 	  offical      = 90 degrees 50'
 * 	  civil        = 96 degrees
 * 	  nautical     = 102 degrees
 * 	  astronomical = 108 degrees
 * 	
 * 	NOTE: longitude is positive for East and negative for West
 * 
 * 1. first calculate the day of the year
 * 
 * 	N1 = floor(275 * month / 9)
 * 	N2 = floor((month + 9) / 12)
 * 	N3 = (1 + floor((year - 4 * floor(year / 4) + 2) / 3))
 * 	N = N1 - (N2 * N3) + day - 30
 * 
 * 2. convert the longitude to hour value and calculate an approximate time
 * 
 * 	lngHour = longitude / 15
 * 	
 * 	if rising time is desired:
 * 	  t = N + ((6 - lngHour) / 24)
 * 	if setting time is desired:
 * 	  t = N + ((18 - lngHour) / 24)
 * 
 * 3. calculate the Sun's mean anomaly
 * 	
 * 	M = (0.9856 * t) - 3.289
 * 
 * 4. calculate the Sun's true longitude
 * 	
 * 	L = M + (1.916 * sin(M)) + (0.020 * sin(2 * M)) + 282.634
 * 	NOTE: L potentially needs to be adjusted into the range [0,360) by adding/subtracting 360
 * 
 * 5a. calculate the Sun's right ascension
 * 	
 * 	RA = atan(0.91764 * tan(L))
 * 	NOTE: RA potentially needs to be adjusted into the range [0,360) by adding/subtracting 360
 * 
 * 5b. right ascension value needs to be in the same quadrant as L
 * 
 * 	Lquadrant  = (floor( L/90)) * 90
 * 	RAquadrant = (floor(RA/90)) * 90
 * 	RA = RA + (Lquadrant - RAquadrant)
 * 
 * 5c. right ascension value needs to be converted into hours
 * 
 * 	RA = RA / 15
 * 
 * 6. calculate the Sun's declination
 * 
 * 	sinDec = 0.39782 * sin(L)
 * 	cosDec = cos(asin(sinDec))
 * 
 * 7a. calculate the Sun's local hour angle
 * 	
 * 	cosH = (cos(zenith) - (sinDec * sin(latitude))) / (cosDec * cos(latitude))
 * 	
 * 	if (cosH >  1) 
 * 	  the sun never rises on this location (on the specified date)
 * 	if (cosH < -1)
 * 	  the sun never sets on this location (on the specified date)
 * 
 * 7b. finish calculating H and convert into hours
 * 	
 * 	if if rising time is desired:
 * 	  H = 360 - acos(cosH)
 * 	if setting time is desired:
 * 	  H = acos(cosH)
 * 	
 * 	H = H / 15
 * 
 * 8. calculate local mean time of rising/setting
 * 	
 * 	T = H + RA - (0.06571 * t) - 6.622
 * 
 * 9. adjust back to UTC
 * 	
 * 	UT = T - lngHour
 * 	NOTE: UT potentially needs to be adjusted into the range [0,24) by adding/subtracting 24
 * 
 * 10. convert UT value to local time zone of latitude/longitude
 * 	
 * 	localT = UT + localOffset
 * 
 * $Log: not supported by cvs2svn $
 * @author Dan Cervelli
 * @version $Id: SunriseSunset.java,v 1.1 2007-04-24 08:31:38 dcervelli Exp $
 */
public class SunriseSunset
{
	public static final float OFFICIAL = 90f + 50.0f / 60.0f; 
	public static final float CIVIL = 96f;
	public static final float NAUTICAL = 102f;
	public static final float ASTRONOMICAL = 108f;

	/**
	 * Gets sunrise and sunset times for a given place and time.
	 * 
	 * @param day 
	 * @param month
	 * @param year
	 * @param zenith sun's zenith, probably use one of the final constants
	 * @param lon 
	 * @param lat
	 * @return first time in sunrise in decimal hours, second is sunset
	 */
	public static float[] getSunriseSunset(int day, int month, int year, float zenith, float lon, float lat)
	{
		float N1 = (float)Math.floor(275 * (month / 9.0f));
		float N2 = (float)Math.floor((month + 9.0f) / 12.0f);
		float N3 = (float)(1.0f + Math.floor((year - 4.0f * Math.floor(year / 4.0f) + 2.0f) / 3.0f));
		float N = N1 - (N2 * N3) + day - 30;

		float lngHour = lon / 15.0f;
		
		float tRise= N + ((6.0f - lngHour) / 24.0f);
		float tSet = N + ((18.0f - lngHour) / 24.0f);
		
		float sunrise = getTime(tRise, zenith, lat, lngHour, true);
		float sunset = getTime(tSet, zenith, lat, lngHour, false);
		return new float[] { sunrise, sunset };
	}
	
	private static float getTime(float t, float zenith, float lat, float lngHour, boolean rise)
	{
		float M = (0.9856f * t) - 3.289f;
		float L = M + (float)((1.916 * Math.sin(Math.toRadians(M))) +
				(0.020 * Math.toRadians(2 * M)) + 282.634);
		while (L >= 360)
			L -= 360;
		while (L < 0)
			L += 360;
		float RA = (float)Math.toDegrees(Math.atan(0.91764 * Math.tan(Math.toRadians(L))));
		while (RA >= 360)
			RA -= 360;
		while (L < 0)
			RA += 360;
		float Lquadrant  = (float)(Math.floor( L/90.0f)) * 90.0f;
		float RAquadrant = (float)(Math.floor(RA/90.0f)) * 90.0f;
		RA = RA + (Lquadrant - RAquadrant);
		RA = RA / 15;
		
		float sinDec = 0.39782f * (float)Math.sin(Math.toRadians(L));
		float cosDec = (float)Math.cos(Math.asin(sinDec));
		
		float cosH = (float)(Math.cos(Math.toRadians(zenith)) - (sinDec * Math.sin(Math.toRadians(lat))) / (cosDec * Math.cos(Math.toRadians(lat))));
		float H;
		if (rise)
			H = 360.0f - (float)Math.toDegrees(Math.acos(cosH));
		else
			H = (float)Math.toDegrees(Math.acos(cosH));
		H = H / 15;
		float T = H + RA - (0.06571f * t) - 6.622f;
		
		float UT = T - lngHour;
		while (UT >= 24)
			UT -= 24;
		while (UT < 0)
			UT += 24;
	
		return UT;
	}
	
	public static void main(String[] args)
	{
		float lat = 37.4611111f;
		float lon = -122.1150556f;
		float[] t = getSunriseSunset(24, 4, 2007, OFFICIAL, lon, lat);
		System.out.println(t[0] - 7);
		System.out.println(t[1] - 7 + 24);
	}
}
