package gov.usgs.swarm.calculation;


/**
 * Computes azimuth calculations required in determining events while markers are been placed.
 * 
 * Chirag Patel
 */
public class ThreeComponentParametersCalculator {
    private static final double RADDEG = 57.2958;
    private static final double PSRAT = 1.73;

    private double rmsamp;
    private double azimuth;
    private double coherence;
    private double velocity;

    
    public static void main(String[] args){
    	//trying to check why azimuth does not change for some range of values.
    	double [][] data50 = {
			    			{1.509668992E9,3.85464384E8,-2.52200896E8,4.18822208E8,-1.477134336E9,-1.477330944E9,-5.1398592E7,2.67302976E8,1.575860224E9,1.86100736E9,-1.880377344E9,-1.22606592E9,-2.048149504E9,-1.326729216E9,8.2068896E8,-3.5014604E7,-1.595295744E9,-5.8866272E8,1.66311984E8,2.16643632E8,-2.048280576E9,1.776924672E9,-7.22880448E8,2.07891456E9,5.35410752E8,-1.830176768E9,1.642706944E9,2.095691776E9,6.02519616E8,-1.94761728E9,-1.964394496E9,-2.53118416E8,1.760147456E9,-2.53118416E8,-1.94761728E9,-1.897220096E9,-1.3771264E9,3.84481312E8,-5.55042752E8,1.324005376E9,-1.813334016E9,-1.813334016E9,6.529168E8,-1.007962048E9,1.6932352E9,-1.226000384E9,2.112600064E9,4.3481296E8,1.441445888E9,-1.192577024E9},
			    			{1.509668992E9,3.85464384E8,-2.52200896E8,4.18822208E8,-1.477134336E9,-1.477330944E9,-5.1398592E7,2.67302976E8,1.575860224E9,1.86100736E9,-1.880377344E9,-1.22606592E9,-2.048149504E9,-1.326729216E9,8.2068896E8,-3.5014604E7,-1.595295744E9,-5.8866272E8,1.66311984E8,2.16643632E8,-2.048280576E9,1.776924672E9,-7.22880448E8,2.07891456E9,5.35410752E8,-1.830176768E9,1.642706944E9,2.095691776E9,6.02519616E8,-1.94761728E9,-1.964394496E9,-2.53118416E8,1.760147456E9,-2.53118416E8,-1.94761728E9,-1.897220096E9,-1.3771264E9,3.84481312E8,-5.55042752E8,1.324005376E9,-1.813334016E9,-1.813334016E9,6.529168E8,-1.007962048E9,1.6932352E9,-1.226000384E9,2.112600064E9,4.3481296E8,1.441445888E9,-1.192577024E9},
			    			{1.509668992E9,3.85464384E8,-2.52200896E8,4.18822208E8,-1.477134336E9,-1.477330944E9,-5.1398592E7,2.67302976E8,1.575860224E9,1.86100736E9,-1.880377344E9,-1.22606592E9,-2.048149504E9,-1.326729216E9,8.2068896E8,-3.5014604E7,-1.595295744E9,-5.8866272E8,1.66311984E8,2.16643632E8,-2.048280576E9,1.776924672E9,-7.22880448E8,2.07891456E9,5.35410752E8,-1.830176768E9,1.642706944E9,2.095691776E9,6.02519616E8,-1.94761728E9,-1.964394496E9,-2.53118416E8,1.760147456E9,-2.53118416E8,-1.94761728E9,-1.897220096E9,-1.3771264E9,3.84481312E8,-5.55042752E8,1.324005376E9,-1.813334016E9,-1.813334016E9,6.529168E8,-1.007962048E9,1.6932352E9,-1.226000384E9,2.112600064E9,4.3481296E8,1.441445888E9,-1.192577024E9}
    						};
    	
    	double [][] data24 = {
				    			{1.99931936E8,-8.23478208E8,-5.55042752E8,-1.662339072E9,-5.21488352E8,-3.4949084E7,1.089124352E9,1.49534752E8,-6.5590272E8,1.05524224E9,1.40749824E9,7.19566848E8,-1.15954688E9,-9.24731392E8,-5.22143712E8,1.088403456E9,-1.562462208E9,-1.56259328E9,-1.562724352E9,-2.049329152E9,5.51139328E8,-1.713784832E9,1.31708944E8,-2.066106368E9},
				    			{1.99931936E8,-8.23478208E8,-5.55042752E8,-1.662339072E9,-5.21488352E8,-3.4949084E7,1.089124352E9,1.49534752E8,-6.5590272E8,1.05524224E9,1.40749824E9,7.19566848E8,-1.15954688E9,-9.24731392E8,-5.22143712E8,1.088403456E9,-1.562462208E9,-1.56259328E9,-1.562724352E9,-2.049329152E9,5.51139328E8,-1.713784832E9,1.31708944E8,-2.066106368E9},
				    			{1.99931936E8,-8.23478208E8,-5.55042752E8,-1.662339072E9,-5.21488352E8,-3.4949084E7,1.089124352E9,1.49534752E8,-6.5590272E8,1.05524224E9,1.40749824E9,7.19566848E8,-1.15954688E9,-9.24731392E8,-5.22143712E8,1.088403456E9,-1.562462208E9,-1.56259328E9,-1.562724352E9,-2.049329152E9,5.51139328E8,-1.713784832E9,1.31708944E8,-2.066106368E9}
    						};
    	
    	
    	ThreeComponentParametersCalculator tt = new ThreeComponentParametersCalculator();
    	ThreeComponentParametersCalculator tt2 = new ThreeComponentParametersCalculator();
    	System.out.println(tt.calculate(data24, 0, 24, 1.86));
    	System.out.println(tt2.calculate(data50, 0, 50, 1.86));
    }
    

    /**
     * @param dataMatrix       data matrix :
     *                         [0, *] - vertical channel (positive direction up)
     *                         [1, *] - north channel
     *                         [2, *] - east channel
     * @param firstSampleIndex first sample in time interval
     * @param lastSampleIndex  last sample in time interval
     * @param pvel             local p-wave velocity at site
     * @return
     */
    public double calculate(double[][] dataMatrix, int firstSampleIndex, int lastSampleIndex, double pvel) {
        if (dataMatrix == null || firstSampleIndex > lastSampleIndex) {
            throw new IllegalArgumentException("Wrong data window specified");
        }
        double[] dc = new double[3];
        for (int i = firstSampleIndex; i < lastSampleIndex; i++) {
            for (int l = 0; l < 3; l++) {
                dc[l] += dataMatrix[l][i];
            }
        }
        for (int i = 0; i < 3; i++) {
            dc[i] = dc[i] / (lastSampleIndex - firstSampleIndex + 1);
            for (int l = firstSampleIndex; l < lastSampleIndex; l++) {
                dataMatrix[i][l] -= dc[i];
            }
        }

        // Model 8 - p waves only
        //Calculate the auto and cross correlations.
        int lenwin = lastSampleIndex - firstSampleIndex + 1;
        double xx = cross(dataMatrix[1], dataMatrix[1], firstSampleIndex, lastSampleIndex);
        double xz = cross(dataMatrix[1], dataMatrix[0], firstSampleIndex, lastSampleIndex);
        double yy = cross(dataMatrix[2], dataMatrix[2], firstSampleIndex, lastSampleIndex);
        double yz = cross(dataMatrix[2], dataMatrix[0], firstSampleIndex, lastSampleIndex);
        double zz = cross(dataMatrix[0], dataMatrix[0], firstSampleIndex, lastSampleIndex);
        
        double power = (xx + yy + zz) / lenwin;
        rmsamp = Math.sqrt(power);

        // Calculate azimuth and vertical/radial amplitude ratio
        double azi = Math.atan2(-yz, -xz);
        azimuth = azi * RADDEG;
        if (azimuth < 0) {
            azimuth += 360;
        }
        double zoverr = zz / Math.sqrt(xz * xz + yz * yz);
        double a = -zoverr * Math.cos(azi);
        double b = -zoverr * Math.sin(azi);

        // Calculate predicted coherence
        double err = 0;
        for (int i = firstSampleIndex; i < lastSampleIndex; i++) {
            double cc = dataMatrix[0][i] - a * dataMatrix[1][i] - b * dataMatrix[2][i];
            err += cc * cc;
        }

        coherence = 1 - err / zz;

        // Simple biased velocity estimation (to obtain an unbiased estimate one need to know the noise amplitude)
        double svel = pvel / PSRAT;
        double ai = Math.atan(1 / zoverr);
        velocity = svel / Math.sin(ai / 2);

        return azimuth;
    }

    /**
     * Get root mean square amplitude. calculate method needs to be called first.
     * @return amplitude
     */
    public double getRootMeanSquareAmplitude() {
        return rmsamp;
    }

    /**
     * Get P-phase azimuth (towards event) in degrees. calculate method needs to be called first.
     * @return azimuth
     */
    public double getAzimuth() {
        return azimuth;
    }

    /**
     * Get predicted coherence, should be positive and larger than about 0.1 for P-phase. calculate method needs to be called first.
     * @return coherence
     */
    public double getCoherence() {
        return coherence;
    }

    /**
     * Get P-wave apparent velocity in km/sec. calculate method needs to be called first.
     * @return
     */
    public double getVelocity() {
        return velocity;
    }

    /**
     * Calculates unnormalised cross correlation between the two real time series x and y of length l.
     *
     * @param x
     * @param y
     * @param firstSampleIndex
     * @param lastSampleIndex
     * @return
     */
    private static double cross(double[] x, double[] y, int firstSampleIndex, int lastSampleIndex) {
        double a = 0;
        for (int i = firstSampleIndex; i < lastSampleIndex; i++) {
            a += x[i] * y[i];
        }
        return a;
    }

}
