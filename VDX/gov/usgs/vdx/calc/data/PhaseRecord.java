package gov.usgs.vdx.calc.data;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Data structure used in Hypo71 algorithm.
 * 
 * @author Oleg Shepelev
 */
@XmlRootElement
public class PhaseRecord {
	/**
	 * Station name.
	 */
	private String MSTA;	
	private String PRMK;
	/**
	 * Year, month and day of P-arrival
	 */
	private double W;
	/**
	 * Time.
	 */
	private int JTIME;
	/**
	 * 
	 */
	private int JMIN;
	private double P;
	private double S;
	private String SRMK;
	private double WS;
	/** 
	 * Maximum peak-to-peak amplitude in mm.
	 */
	private double AMX;
	/**
	 * Period of the maximum amplitude in sec. 
	 */
	private double PRX;
	/** 
	 * Normally not used except as noted in next item.
	 */
	private double CALP;
	/** 
	 * Peak-to-peak amplitude of 10mv calibration signal in mm.
	 */
	private double CALX;
	/** 
	 * Remark for this phase card.
	 */
	private String RMK;
	/**
	 * Time correction in sec.
	 */
	private double DT;
	/**
	 * F-P time in sec. This is the duration time of earthquake.
	 */
	private double FMP;
	private String AZRES;
	private char SYM;
	private String AS;
	private String ICARD;
	private char QRMK;
	/**
	 * Normally blank.
	 */
	private String IPRO;
	public PhaseRecord(){
		
	}
	public PhaseRecord(String mSTA, String pRMK, double w, int jTIME, int jMIN, double p, double s, String sRMK, double wS, double aMX, double pRX, double cALP, double cALX, String rMK, double dT,
			double fMP, String aZRES, char sYM, String aS, String iCARD, char qRMK, String iPRO) {
		MSTA = mSTA;
		PRMK = pRMK;
		W = w;
		JTIME = jTIME;
		JMIN = jMIN;
		P = p;
		S = s;
		SRMK = sRMK;
		WS = wS;
		AMX = aMX;
		PRX = pRX;
		CALP = cALP;
		CALX = cALX;
		RMK = rMK;
		DT = dT;
		FMP = fMP;
		AZRES = aZRES;
		SYM = sYM;
		AS = aS;
		ICARD = iCARD;
		QRMK = qRMK;
		IPRO = iPRO;
	}
	@XmlElement
	public String getMSTA() {
		return MSTA;
	}
	public void setMSTA(String mSTA) {
		MSTA = mSTA;
	}
	@XmlElement
	public String getPRMK() {
		return PRMK;
	}
	public void setPRMK(String pRMK) {
		PRMK = pRMK;
	}
	@XmlElement
	public double getW() {
		return W;
	}
	public void setW(double w) {
		W = w;
	}
	@XmlElement
	public int getJTIME() {
		return JTIME;
	}
	public void setJTIME(int jTIME) {
		JTIME = jTIME;
	}
	@XmlElement
	public int getJMIN() {
		return JMIN;
	}
	public void setJMIN(int jMIN) {
		JMIN = jMIN;
	}
	@XmlElement
	public double getP() {
		return P;
	}
	public void setP(double p) {
		P = p;
	}
	@XmlElement
	public double getS() {
		return S;
	}
	public void setS(double s) {
		S = s;
	}
	@XmlElement
	public String getSRMK() {
		return SRMK;
	}
	public void setSRMK(String sRMK) {
		SRMK = sRMK;
	}
	@XmlElement
	public double getWS() {
		return WS;
	}
	public void setWS(double wS) {
		WS = wS;
	}
	@XmlElement
	public double getAMX() {
		return AMX;
	}
	public void setAMX(double aMX) {
		AMX = aMX;
	}
	@XmlElement
	public double getPRX() {
		return PRX;
	}
	public void setPRX(double pRX) {
		PRX = pRX;
	}
	@XmlElement
	public double getCALP() {
		return CALP;
	}
	public void setCALP(double cALP) {
		CALP = cALP;
	}
	@XmlElement
	public double getCALX() {
		return CALX;
	}
	public void setCALX(double cALX) {
		CALX = cALX;
	}
	@XmlElement
	public String getRMK() {
		return RMK;
	}
	public void setRMK(String rMK) {
		RMK = rMK;
	}
	@XmlElement
	public double getDT() {
		return DT;
	}
	public void setDT(double dT) {
		DT = dT;
	}
	@XmlElement
	public double getFMP() {
		return FMP;
	}
	public void setFMP(double fMP) {
		FMP = fMP;
	}
	@XmlElement
	public String getAZRES() {
		return AZRES;
	}
	public void setAZRES(String aZRES) {
		AZRES = aZRES;
	}
	@XmlElement
	public char getSYM() {
		return SYM;
	}
	public void setSYM(char sYM) {
		SYM = sYM;
	}
	@XmlElement
	public String getAS() {
		return AS;
	}
	public void setAS(String aS) {
		AS = aS;
	}
	@XmlElement
	public String getICARD() {
		return ICARD;
	}
	public void setICARD(String iCARD) {
		ICARD = iCARD;
	}
	@XmlElement
	public char getQRMK() {
		return QRMK;
	}
	public void setQRMK(char qRMK) {
		QRMK = qRMK;
	}
	@XmlElement
	public String getIPRO() {
		return IPRO;
	}
	public void setIPRO(String iPRO) {
		IPRO = iPRO;
	}

	
}