package gov.usgs.vdx.calc.data;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Data structure used in Hypo71 algorithm.
 * 
 * @author Oleg Shepelev
 */
@XmlRootElement
public class Station  implements Serializable{
	/**
	 * If IW = *, then this station has zero weight assigned to its P and/or S reading(s).
	 */
	char IW;
	/**
	 * Station name.
	 */
	String NSTA;
	/**
	 * Degree portion of latitude.
	 */
	int LAT1;
	/**
	 * Minute portion of latitude.
	 */
	float LAT2;
	/**
	 * Punch N or leave this column blank for stations in northern hemisphere. Punch S for stations in sourthern hemisphere.
	 */
	char INS;
	/**
	 * Degree portion of longitude.
	 */
	int LON1;
	/**
	 * Minute portion of longitude.
	 */
	float LON2;
	/**
	 * Punch E for eastern longitude. W or blank for western.
	 */
	char IEW;
	/**
	 * Elevation in meters. This data is not used in the program.
	 */
	int IELV;
	float dly;
	/**
	 * Station delay in seconds.
	 */
	float FMGC;
	/**
	 * Station correction for FMAG.
	 */
	float XMGC;
	/**
	 * System number is assigned for each station so that the frequency response curve of the seismometer and preamp is specified for the amplitude magnitude calculation (XMAG).
	 */
	int KLAS;
	/**
	 * Standard period for XMAG.
	 */
	float PRR;
	/**
	 * Standard calibration for XMAG.
	 */
	float CALR;
	/**
	 * Calibration indicator.
	 */
	int ICAL;
	/**
	 * Year, month and day.
	 */
	int NDATE;
	/**
	 * Hour and minute.
	 */
	int NHRMN;

	// Missing special fields
	// NSTA[I] - NSTA, DELI - dly, AZI, EXGAP, RDGAP
	float AZI;
	float EXGAP;
	float RDGAP;

	// Station output
	// String STN - NSTA
	/**
	 * Epicentral distance in km.
	 */
	float DIST;
	// AZM - AZI
	/**
	 * Angle of incidence measured with respect to downward vertical.
	 */
	int AIN;
	/**
	 * PRMK from input data.
	 */
	String PRMK;
	/**
	 * Hour of arrival time from input data.
	 */
	int HR;
	/**
	 * Minute of arraival time from inout data.
	 */
	int MN;
	/**
	 * The second's portial of P-arrival time from input data.
	 */
	float PSEC;
	/**
	 * Observed P-travet time in sec.
	 */
	float TPOBS;
	/**
	 * Calculated travel time in sec/
	 */
	float TPCAL;
	/**
	 * If the Station Delay Model is used, then DLY means the station delay in
	 * sec from the input list. If the Variable First-Layer Model is used, then
	 * H1 means the thickness of the first layer in km at this station.
	 */
	float DLYH1;
	float PRES;
	float PWT;
	float AMX;
	float PRX;
	float CALX;
	int K;
	float XMAG;
	float RMK;
	float FMP;
	float FMAG;
	String SRMK;
	float SSEC;
	float TSOBS;
	float SRES;
	float SWT;
	String DT;

	// Field for summary
	TravelTimeResiduals model1;
	TravelTimeResiduals model2;
	int NXM;
	float AVXM;
	float SDXM;
	int NFM;
	float AVFM;
	float SDFM;

	@XmlRootElement
	public static class TravelTimeResiduals {
		private int NRES;
		private float SRWT;
		private float AVRES;
		private float SDRES;

		@XmlElement
		public int getNRES() {
			return NRES;
		}

		public void setNRES(int nRES) {
			NRES = nRES;
		}

		@XmlElement
		public float getSRWT() {
			return SRWT;
		}

		public void setSRWT(float sRWT) {
			SRWT = sRWT;
		}

		@XmlElement
		public float getAVRES() {
			return AVRES;
		}

		public void setAVRES(float aVRES) {
			AVRES = aVRES;
		}

		@XmlElement		
		public float getSDRES() {
			return SDRES;
		}

		public void setSDRES(float sDRES) {
			SDRES = sDRES;
		}

		public TravelTimeResiduals(int nRES, float sRWT, float aVRES,
				float sDRES) {
			super();
			NRES = nRES;
			SRWT = sRWT;
			AVRES = aVRES;
			SDRES = sDRES;
		}

		public TravelTimeResiduals(){
			
		}
	}
	public Station(){
		
	}
	public Station(String NSTA, int NRES, float SRWT, float AVRES,
			float SDRES, int NRES1, float SRWT1, float AVRES1,
			float SDRES1, int NXM, float AVXM, float SDXM, int NFM,
			float AVFM, float SDFM) {
		model1 = new TravelTimeResiduals(NRES, SRWT, AVRES, SDRES);
		model2 = new TravelTimeResiduals(NRES1, SRWT1, AVRES1, SDRES1);
		this.NXM = NXM;
		this.AVXM = AVXM;
		this.SDXM = SDXM;
		this.NFM = NFM;
		this.AVFM = AVFM;
		this.SDFM = SDFM;
	}

	public Station(String string, float d, int iAZ, int iAIN, String string2,
			int jHR, int i, float e, float tPK, float f, float dLYK,
			String x4kout, String string3, float g, int iAMX, int iPRX,
			float h, int j, String xMAGOU, char rMK3, String string4,
			String fMPOUT, String fMAGOU, char rMK4, String string5,
			String sKOUT, String tSKOUT, String sRESOU, String rMK5,
			String sWTOUT, String dTKOUT, char c) {
		// TODO Auto-generated constructor stub
	}

	public Station(String nSTA, float dIST, int aIN, String pRMK, int hR,
			int mN, float pSEC, float tPOBS, float tPCAL, float dLYH1,
			String pRES, String pWT, float aMX, float pRX, float cALX,
			int k, float xMAG, float rMK, float fMP, float fMAG,
			String sRMK, float sSEC, float tSOBS, float sRES, float sWT,
			String dT) {
		super();
		NSTA = nSTA;
		DIST = dIST;
		AIN = aIN;
		PRMK = pRMK;
		HR = hR;
		MN = mN;
		PSEC = pSEC;
		TPOBS = tPOBS;
		TPCAL = tPCAL;
		DLYH1 = dLYH1;
		PRES = Float.valueOf(pRES);
		PWT = Float.valueOf(pWT);
		AMX = aMX;
		PRX = pRX;
		CALX = cALX;
		K = k;
		XMAG = xMAG;
		RMK = rMK;
		FMP = fMP;
		FMAG = fMAG;
		SRMK = sRMK;
		SSEC = sSEC;
		TSOBS = tSOBS;
		SRES = sRES;
		SWT = sWT;
		DT = dT;
	}

	public Station(String nSTA, float dly, float aZI, float eXGAP,
			float rDGAP) {
		this.NSTA = nSTA;
		this.dly = dly;
		this.AZI = aZI;
		this.EXGAP = eXGAP;
		this.RDGAP = rDGAP;
	}
	
	public Station(char iW, String nSTA, int lAT1, float lAT2, char iNS,
			int lON1, float lON2, char iEW, int iELV, float dly, float fMGC,
			float xMGC, int kLAS, float pRR, float cALR, int iCAL,
			int nDATE, int nHRMN) {
		IW = iW;
		NSTA = nSTA;
		LAT1 = lAT1;
		LAT2 = lAT2;
		INS = iNS;
		LON1 = lON1;
		LON2 = lON2;
		IEW = iEW;
		IELV = iELV;
		this.dly = dly;
		FMGC = fMGC;
		XMGC = xMGC;
		KLAS = kLAS;
		PRR = pRR;
		CALR = cALR;
		ICAL = iCAL;
		NDATE = nDATE;
		NHRMN = nHRMN;
	}
	
	public Station(char iW, String nSTA, int lAT1, float lAT2, char iNS,
			int lON1, float lON2, char iEW, int iELV, int mNO, float dly, float dly1,
			float xMGC, float fMGC, int kLAS, float pRR, int iCAL,
			int nDATE, int nHRMN) {
		IW = iW;
		NSTA = nSTA;
		LAT1 = lAT1;
		LAT2 = lAT2;
		INS = iNS;
		LON1 = lON1;
		LON2 = lON2;
		IEW = iEW;
		IELV = iELV;
		this.dly = dly;
		FMGC = fMGC;
		XMGC = xMGC;
		KLAS = kLAS;
		PRR = pRR;
		NDATE = nDATE;
		NHRMN = nHRMN;
		ICAL = iCAL;
	}

	@XmlElement
	public char getIW() {
		return IW;
	}

	public void setIW(char iW) {
		IW = iW;
	}

	@XmlElement
	public String getNSTA() {
		return NSTA;
	}

	public void setNSTA(String nSTA) {
		NSTA = nSTA;
	}

	@XmlElement
	public int getLAT1() {
		return LAT1;
	}

	public void setLAT1(int lAT1) {
		LAT1 = lAT1;
	}

	@XmlElement
	public float getLAT2() {
		return LAT2;
	}

	public void setLAT2(float lAT2) {
		LAT2 = lAT2;
	}

	@XmlElement
	public char getINS() {
		return INS;
	}

	public void setINS(char iNS) {
		INS = iNS;
	}

	@XmlElement
	public int getLON1() {
		return LON1;
	}

	public void setLON1(int lON1) {
		LON1 = lON1;
	}

	@XmlElement
	public float getLON2() {
		return LON2;
	}

	public void setLON2(float lON2) {
		LON2 = lON2;
	}

	@XmlElement
	public char getIEW() {
		return IEW;
	}

	public void setIEW(char iEW) {
		IEW = iEW;
	}

	@XmlElement
	public int getIELV() {
		return IELV;
	}

	public void setIELV(int iELV) {
		IELV = iELV;
	}

	@XmlElement
	public float getDly() {
		return dly;
	}

	public void setDly(float dly) {
		this.dly = dly;
	}

	@XmlElement
	public float getFMGC() {
		return FMGC;
	}

	public void setFMGC(float fMGC) {
		FMGC = fMGC;
	}

	@XmlElement
	public float getXMGC() {
		return XMGC;
	}

	public void setXMGC(float xMGC) {
		XMGC = xMGC;
	}

	@XmlElement
	public int getKLAS() {
		return KLAS;
	}

	public void setKLAS(int kLAS) {
		KLAS = kLAS;
	}

	@XmlElement
	public float getPRR() {
		return PRR;
	}

	public void setPRR(float pRR) {
		PRR = pRR;
	}

	@XmlElement
	public float getCALR() {
		return CALR;
	}

	public void setCALR(float cALR) {
		CALR = cALR;
	}

	@XmlElement
	public int getICAL() {
		return ICAL;
	}

	public void setICAL(int iCAL) {
		ICAL = iCAL;
	}

	@XmlElement
	public int getNDATE() {
		return NDATE;
	}

	public void setNDATE(int nDATE) {
		NDATE = nDATE;
	}

	@XmlElement
	public int getNHRMN() {
		return NHRMN;
	}

	public void setNHRMN(int nHRMN) {
		NHRMN = nHRMN;
	}

	@XmlElement
	public float getAZI() {
		return AZI;
	}

	public void setAZI(float aZI) {
		AZI = aZI;
	}

	@XmlElement
	public float getEXGAP() {
		return EXGAP;
	}

	public void setEXGAP(float eXGAP) {
		EXGAP = eXGAP;
	}

	@XmlElement
	public float getRDGAP() {
		return RDGAP;
	}

	public void setRDGAP(float rDGAP) {
		RDGAP = rDGAP;
	}

	@XmlElement
	public float getDIST() {
		return DIST;
	}

	public void setDIST(float dIST) {
		DIST = dIST;
	}

	@XmlElement
	public int getAIN() {
		return AIN;
	}

	public void setAIN(int aIN) {
		AIN = aIN;
	}

	@XmlElement
	public String getPRMK() {
		return PRMK;
	}

	public void setPRMK(String pRMK) {
		PRMK = pRMK;
	}

	@XmlElement
	public int getHR() {
		return HR;
	}

	public void setHR(int hR) {
		HR = hR;
	}

	@XmlElement
	public int getMN() {
		return MN;
	}

	public void setMN(int mN) {
		MN = mN;
	}

	@XmlElement
	public float getPSEC() {
		return PSEC;
	}

	public void setPSEC(float pSEC) {
		PSEC = pSEC;
	}

	@XmlElement
	public float getTPOBS() {
		return TPOBS;
	}

	public void setTPOBS(float tPOBS) {
		TPOBS = tPOBS;
	}

	@XmlElement
	public float getTPCAL() {
		return TPCAL;
	}

	public void setTPCAL(float tPCAL) {
		TPCAL = tPCAL;
	}

	@XmlElement
	public float getDLYH1() {
		return DLYH1;
	}

	public void setDLYH1(float dLYH1) {
		DLYH1 = dLYH1;
	}

	@XmlElement
	public float getPRES() {
		return PRES;
	}

	public void setPRES(float pRES) {
		PRES = pRES;
	}

	@XmlElement
	public float getPWT() {
		return PWT;
	}

	public void setPWT(float pWT) {
		PWT = pWT;
	}

	@XmlElement
	public float getAMX() {
		return AMX;
	}

	public void setAMX(float aMX) {
		AMX = aMX;
	}

	@XmlElement
	public float getPRX() {
		return PRX;
	}

	public void setPRX(float pRX) {
		PRX = pRX;
	}

	@XmlElement
	public float getCALX() {
		return CALX;
	}

	public void setCALX(float cALX) {
		CALX = cALX;
	}

	@XmlElement
	public int getK() {
		return K;
	}

	public void setK(int k) {
		K = k;
	}

	@XmlElement
	public float getXMAG() {
		return XMAG;
	}

	public void setXMAG(float xMAG) {
		XMAG = xMAG;
	}

	@XmlElement
	public float getRMK() {
		return RMK;
	}

	public void setRMK(float rMK) {
		RMK = rMK;
	}

	@XmlElement
	public float getFMP() {
		return FMP;
	}

	public void setFMP(float fMP) {
		FMP = fMP;
	}

	@XmlElement
	public float getFMAG() {
		return FMAG;
	}

	public void setFMAG(float fMAG) {
		FMAG = fMAG;
	}

	@XmlElement
	public String getSRMK() {
		return SRMK;
	}

	public void setSRMK(String sRMK) {
		SRMK = sRMK;
	}

	@XmlElement
	public float getSSEC() {
		return SSEC;
	}

	public void setSSEC(float sSEC) {
		SSEC = sSEC;
	}

	@XmlElement
	public float getTSOBS() {
		return TSOBS;
	}

	public void setTSOBS(float tSOBS) {
		TSOBS = tSOBS;
	}

	@XmlElement
	public float getSRES() {
		return SRES;
	}

	public void setSRES(float sRES) {
		SRES = sRES;
	}

	@XmlElement
	public float getSWT() {
		return SWT;
	}

	public void setSWT(float sWT) {
		SWT = sWT;
	}

	@XmlElement
	public String getDT() {
		return DT;
	}

	public void setDT(String dT) {
		DT = dT;
	}

	@XmlElement
	public TravelTimeResiduals getModel1() {
		return model1;
	}

	public void setModel1(TravelTimeResiduals model1) {
		this.model1 = model1;
	}

	@XmlElement
	public TravelTimeResiduals getModel2() {
		return model2;
	}

	public void setModel2(TravelTimeResiduals model2) {
		this.model2 = model2;
	}

	@XmlElement
	public int getNXM() {
		return NXM;
	}

	public void setNXM(int nXM) {
		NXM = nXM;
	}

	@XmlElement
	public float getAVXM() {
		return AVXM;
	}

	public void setAVXM(float aVXM) {
		AVXM = aVXM;
	}

	@XmlElement
	public float getSDXM() {
		return SDXM;
	}

	public void setSDXM(float sDXM) {
		SDXM = sDXM;
	}

	@XmlElement
	public int getNFM() {
		return NFM;
	}

	public void setNFM(int nFM) {
		NFM = nFM;
	}

	@XmlElement
	public float getAVFM() {
		return AVFM;
	}

	public void setAVFM(float aVFM) {
		AVFM = aVFM;
	}

	@XmlElement
	public float getSDFM() {
		return SDFM;
	}

	public void setSDFM(float sDFM) {
		SDFM = sDFM;
	}
	
	
}