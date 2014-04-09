package gov.usgs.vdx.calc;

import gov.usgs.vdx.calc.data.AdjustmentIteration;
import gov.usgs.vdx.calc.data.ControlCard;
import gov.usgs.vdx.calc.data.CrustalModel;
import gov.usgs.vdx.calc.data.Hypocenter;
import gov.usgs.vdx.calc.data.PhaseRecord;
import gov.usgs.vdx.calc.data.Station;
import gov.usgs.vdx.calc.data.Stats;
import gov.usgs.vdx.calc.util.FortranFormat;

import java.io.*;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Queue;

import static java.lang.Math.*;

/**
 * Runs the hypo71 algorithm. This is a direct port of the fortran version.
 * 
 * @author Oleg Shepelev
 */
@SuppressWarnings("all")
public class Hypo71 {

	public static class Results {

		private List<AdjustmentIteration> adjustmentsOutput = new LinkedList<AdjustmentIteration>();
		private List<Hypocenter> hypocenterOuput = new LinkedList<Hypocenter>();
		private List<Station> missingStationsList = new LinkedList<Station>();
		private List<Station> stationsResultList = new LinkedList<Station>();
		private List<Station> summaryList = new LinkedList<Station>();
		private List<String> deletedStationsList = new LinkedList<String>();
		private Stats stats;
		private String printOutput = new String();
		private String punchOutput = new String();

		public String getPunchOutput() {
			return punchOutput;
		}

		public void setPunchOutput(String punchOutput) {
			this.punchOutput = punchOutput;
		}

		public String getPrintOutput() {
			return printOutput;
		}

		public void setPrintOutput(String printOutput) {
			this.printOutput = printOutput;
		}

		public Stats getStats() {
			return stats;
		}

		public void setStats(Stats stats) {
			this.stats = stats;
		}

		public void addAdjustmentIteration(AdjustmentIteration hypocenter) {
			adjustmentsOutput.add(hypocenter);
		}

		public List<AdjustmentIteration> getAdjustmentIterations() {
			return adjustmentsOutput;
		}

		public void addHypocenterOutput(Hypocenter hypocenter) {
			hypocenterOuput.add(hypocenter);
		}

		public List<Hypocenter> getHypocenterOutput() {
			return hypocenterOuput;
		}

		public void addMissingStation(Station station) {
			missingStationsList.add(station);
		}

		public void addStationToTheResultList(Station station) {
			stationsResultList.add(station);
		}

		public void addStationToSummaryList(Station station) {
			summaryList.add(station);
		}

		public void addToDeletedStationsList(String name) {
			deletedStationsList.add(name);
		}

		public List<Station> getMissingStationsList() {
			return missingStationsList;
		}

		public List<Station> getStationsResultList() {
			return stationsResultList;
		}

		public List<Station> getSummaryList() {
			return summaryList;
		}

		public List<String> getDeletedStationsList() {
			return deletedStationsList;
		}

		public String getOutput() {
			return printOutput;
		}

	}

	static BufferedReader FINPUT_READER = null;
	boolean readFromFile;

	char[] SYM = new char[101];
	char[] QRMK = new char[101];
	char[] IW = new char[151];
	char[] INS = new char[151];
	char[] IEW = new char[151];

	char SYM3 = ' ';

	String[] RMK = new String[101];
	String IPRO = "", ISW = "";
	String[] NSTA = new String[151];
	String[] MSTA = new String[101];
	String[] PRMK = new String[101];
	String[] SRMK = new String[101];
	String[] AZRES = new String[101];
	String[] WRK = new String[101];
	String FINPUT, FPRINT, FPUNCH;
	String AHEAD, BHEAD, SUCARD;

	{
		initStringArray(RMK);
		initStringArray(NSTA);
		initStringArray(MSTA);
		initStringArray(PRMK);
		initStringArray(SRMK);
		initStringArray(AZRES);
		initStringArray(WRK);
	}

	int[] KDX = new int[101];
	int[] LDX = new int[101];
	int[] JMIN = new int[101];
	int[] NXM = new int[151];
	int[] NFM = new int[151];
	int[] ICAL = new int[151];
	int[] JDX = new int[151];
	int[] IELV = new int[151];
	int[] MNO = new int[151];

	int[] KLAS = new int[151];
	int[] KLSS = new int[151];
	int[] KSMP = new int[151];
	int[][] NRES = new int[2][151];
	int[] MDATE = new int[151];
	int[] MHRMN = new int[151];
	int[] NDATE = new int[151];
	int[] NHRMN = new int[151];

	double[] V = new double[21];
	double[] D = new double[21];
	double[] THK = new double[21];
	double[] H = new double[21];
	double[] DEPTH = new double[21];
	double[] VSQ = new double[21];
	double[][] TID = new double[21][21];
	double[][] DID = new double[21][21];

	double[][] F = new double[21][21];
	double[][] G = new double[4][21];

	double[] XMAG = new double[101];
	double[] FMAG = new double[101];
	double[] AMX = new double[101];
	double[] PRX = new double[101];
	double[] CALX = new double[101];
	double[] FMP = new double[101];

	double[] W = new double[101];
	double[] P = new double[101];
	double[] TP = new double[101];
	double[] S = new double[101];
	double[] WS = new double[101];
	double[] TS = new double[101];
	double[] DT = new double[101];

	double[] QNO = new double[4];
	double[] TEST = new double[15];
	double[][] QSPA = new double[9][40];

	double[] SXM = new double[151];
	double[] SXMSQ = new double[151];
	double[] SFM = new double[151];
	double[] SFMSQ = new double[151];
	double[] CALS = new double[151];

	double[] LAT = new double[151];
	double[] LON = new double[151];
	double[] PRR = new double[151];
	double[] CALR = new double[151];
	double[] FMGC = new double[151];
	double[] XMGC = new double[151];

	double[][] SR = new double[2][151];
	double[][] SRSQ = new double[2][151];
	double[][] SRWT = new double[2][151];
	double[][] DLY = new double[2][151];
	double[][] FLT = new double[2][151];
	double MAG;
	double GAP;

	double[] Y = new double[4];

	double TIME1, TIME2;
	double SDXM, SDFM;
	int LMAX = 21;
	int MMAX = 101;
	int NMAX = 151;
	int NM, NF;
	int KSING;
	int KNO, AVXM, AVFM;

	double LAT1, LON1;
	double LAT2, LON2;

	String BLANKS = "              ";

	// From INPUT1()

	Results results = new Results();

	// ///////////////////////////////////////////
	int MJUMP = 0;

	COMMON_C1 C1 = new COMMON_C1();
	COMMON_C2 C2 = new COMMON_C2();
	COMMON_C3 C3 = new COMMON_C3();
	COMMON_C4 C4 = new COMMON_C4();
	COMMON_C5 C5 = new COMMON_C5();
	COMMON_O1 O1 = new COMMON_O1();
	COMMON_O2 O2 = new COMMON_O2();
	COMMON_O3 O3 = new COMMON_O3();

	// COMMON/C1/IQ,KMS,KFM,IPUN,IMAG,IR,IPRN,KPAPER,KTEST,KAZ,KSORT,KSEL
	public static class COMMON_C1 {
		int IQ;
		int KMS;
		int KFM;
		int IPUN;
		int IMAG;
		int IR;
		int IPRN;
		int KPAPER;
		int KTEST;
		int KAZ;
		int KSORT;
		int KSEL;
	}

	// COMMON/C2/ ZTR,XNEAR,XFAR,POS,LATR,LONR,ONF,FLIM
	public static class COMMON_C2 {

		double ZTR;
		double XNEAR;
		double XFAR;
		double POS;
		double LATR;
		double LONR;
		double ONF;
		double FLIM;
	}

	// COMMON/C3/ AHEAD,IPRO,ISW
	public static class COMMON_C3 {
		double AHEAD;
		int IPRO;
		int ISW;
	}

	// COMMON/C4/ NL,NS,KDATE,KHR,NEAR,IEXIT,IDXS
	public static class COMMON_C4 {
		int NL;
		int NS;
		int KDATE;
		int KHR;
		int NEAR;
		int IEXIT;
		int IDXS;
	}

	// COMMON/C5/ PMIN,XFN
	public static class COMMON_C5 {
		double PMIN;
		double XFN;
	}

	// COMMON/O1/ NI,INST,KNST,IPH,JPH,NDEC,JMAX,JAV,NR,NRP,KF,KP,KZ,KKF
	public static class COMMON_O1 {

		int NI;
		int INST;
		int KNST;
		int IPH;
		int JPH;
		int NDEC;
		int JMAX;
		int JAV;
		int NR;
		int NRP;
		int KF;
		int KP;
		int KZ;
		int KKF;

	}

	// COMMON/O2/ AVRPS,DMIN,RMSSQ,ADJSQ,LATEP,LONEP,Z,ZSQ,AVR,AAR,ORG
	public static class COMMON_O2 {

		double AVRPS;
		double DMIN;
		double RMSSQ;
		double ADJSQ;
		double LATEP;
		double LONEP;
		double Z;
		double ZSQ;
		double AVR;
		double AAR;
		double ORG;
	}

	// COMMON/O3/ SUCARD
	public static class COMMON_O3 {
		double SUCARD;
	}

	/*
	 * SUBROUTINE OUTPUT(TEST,KNO,IW,INS,IEW,DLY,FMGC,XMGC, &
	 * KLAS,PRR,CALR,ICAL,FLT,QSPA,MSTA, &
	 * PRMK,JMIN,P,S,SRMK,AMX,PRX,CALX,RMK,DT,FMP,AZRES,QRMK,KDX,LDX, &
	 * WT,TP,T,WRK,KSMP,TS,TIME1,TIME2,DELTA,DX,DY,AVXM, &
	 * XMAG,AVFM,FMAG,MAG,FNO,X,B,Y,SE,AF,AZ,AIN,ANIN,TEMP,KEY)
	 */
	@SuppressWarnings("boxing")
	public void OUTPUT(final char[] IW, final char[] INS, final char[] IEW, final double[][] DLY, final double[] FMGC, final double[] XMGC, final int[] KLAS, final double[] PRR, final double[] CALR, final int[] ICAL, final double[][] FLT, final double[][] QSPA,
			final String[] PRMK, final int[] JMIN, final double[] P, final double[] S, final String[] SRMK, final double[] AMX, final double[] PRX, final double[] CALX, final String[] RMK, final double[] DT, final double[] FMP, final String[] AZRES, final int[] KDX,
			final int[] LDX, final double[] WT, final double[] TP, final double[] T, final String[] WRK, final double[] TS, final double TIME1, double TIME2, final double[] DELTA, final double[] DX, final double[] DY, double AVXM, double AVFM, final double FNO,
			final double[][] X, final double[] B, final double[] SE, final double[] AF, final double[] AZ, final double[] AIN, final double[] ANIN, final double[] TEMP, final int[] KEY) throws IOException, ParseException {

		// CHARACTER*1 RMKO,RMK2,RMK3,RMK4,Q,QS,QD,SYM3
		char RMKO, RMK2;
		char RMK3;
		char RMK4;
		final char Q, QS, QD;

		// CHARACTER*1 CLASS(4),SYMBOL(5),QRMK(101),IW(151),INS(151),IEW(151)
		char[] CLASS = new char[4];
		char[] SYMBOL = new char[5];

		// CHARACTER*4 ISW,XMAGOU,FMAGOU,SWTOUT,FMPOUT,RMK5,IPRO
		final String ISW = null;
		String XMAGOU;
		String FMAGOU;
		String SWTOUT = "";
		String FMPOUT;
		String RMK5 = "";
		final String IPRO;

		// CHARACTER*5 ERHOUT,SE3OUT
		String ERHOUT = "", SE3OUT = "";

		// CHARACTER*6 MAGOUT,SKOUT,TSKOUT,SRESOU,DTKOUT,X4KOUT
		String MAGOUT = "";
		String SKOUT = "";
		String TSKOUT = "";
		String SRESOU = "";
		String DTKOUT = "";
		String X4KOUT = "";

		// REAL LAT2,LON2,LATEP,LONEP,MAG,LATR,LONR
		final double /* MAG, */LATR, LONR;
		final double[] CAL = new double[101];
		final double[] DEMP = new double[101];

		// REAL*4 FMGC(151),XMGC(151),PRR(151),CALR(151)
		final double[] FMCG = new double[151];

		// DATA CLASS/'A','B','C','D'/
		// DATA SYMBOL/' ','1','2','Q','*'/
		CLASS = new char[] { 'A', 'B', 'C', 'D' };
		SYMBOL = new char[] { ' ', '1', '2', 'Q', '*' };

		if (C1.IPRN >= 2 || O1.KP == 1) {
			/*
			 * CALL XFMAGS & (TEST,FMGC,XMGC,KLAS,PRR,CALR,ICAL,IMAG,IR,QSPA, &
			 * AMX,PRX,CALX,FMP,KDX,DELTA,ZSQ,NRP,CAL,NM,AVXM,SDXM,XMAG,NF, &
			 * AVFM,SDFM,FMAG,MAG)
			 */
			XFMAGS(FMGC, XMGC, KLAS, PRR, CALR, ICAL, QSPA, AMX, PRX, CALX, FMP, KDX, DELTA, CAL, AVXM, AVFM);
		}

		LAT1 = O2.LATEP / 60.0;
		LAT2 = O2.LATEP - 60d * (int) LAT1;
		LON1 = O2.LONEP / 60.0;
		LON2 = (O2.LONEP - 60. * (int) LON1);
		final double ADJ = Math.sqrt(O2.ADJSQ);
		final double RMS = Math.sqrt(O2.RMSSQ);
		final int JHR = C4.KHR;
		final double OSAVE = O2.ORG;
		if (O2.ORG < 0) {
			O2.ORG = O2.ORG + 3600;
			C4.KHR = C4.KHR - 1;
		}

		final int KMIN = (int) (O2.ORG / 60.0); // 5
		final double SEC = O2.ORG - 60.0 * KMIN;
		final double ERH = Math.sqrt(SE[0] * SE[0] + SE[1] * SE[1]);
		final double NO = FNO;
		RMK2 = ' ';
		RMKO = ' ';
		// KZ=1 FOR FIXED DEPTH; ONF=0 FOR ORIGIN TIME BASED ON SMP'S
		if (C2.ONF == 0) {
			RMKO = '*';
		}
		if (O1.KZ == 1) {
			RMK2 = '*';
		}
		O1.JMAX = 0;
		int KK = 0;
		for (int I = 0; I < O1.NRP; I++) { // DO 10 I=1,NRP
			double DXI = DX[I];
			double DYI = DY[I];
			final boolean goto6 = DXI == 0 && DYI == 0;
			if (!goto6) {
				final int JI = KDX[I];
				if (INS[JI - 1] == 'S') {
					DYI = -DYI;
				}
				if (IEW[JI - 1] == 'W') {
					DXI = -DXI;
				}
				AZ[I] = ((Math.atan2(DXI, DYI) * 57.29578 + 360) % 360);
			} else {
				AZ[I] = 999; // 6
			}

			/*
			 * USE THE FOLLOWING LINE FOR IBM MAINFRAME ONLY
			 * AIN(I)=ARSIN(ANIN(I))*57.29578 USE THE FOLLOWING LINE FOR IBM PC
			 * ONLY
			 */
			AIN[I] = (Math.asin(ANIN[I]) * 57.29578);
			if (AIN[I] < 0) {
				AIN[I] = (180. + AIN[I]);
			}
			AIN[I] = 180 - AIN[I];

			double SWT = 0;
			final boolean goto8 = LDX[I] == 0;
			if (!goto8) {
				KK = LDX[I];
				SWT = WT[KK - 1];
			}
			final boolean goto10 = WT[I] == 0 && SWT == 0;
			if (!goto10) {// 8
				O1.JMAX = O1.JMAX + 1;
				TEMP[O1.JMAX - 1] = AZ[I];
			}
		}// 10 CONTINUE

		// CALL SORT(TEMP,KEY,JMAX)
		SORT(TEMP, KEY, O1.JMAX);

		GAP = TEMP[0] + 360 - TEMP[O1.JMAX - 1];

		for (int I = 1; I < O1.JMAX; I++) {// DO 20 I=2,JMAX
			final double DTEMP = TEMP[I] - TEMP[I - 1];
			if (DTEMP > GAP) {
				GAP = DTEMP;
			}
		}// 20 CONTINUE

		final int IGAP = (int) (GAP + 0.5);
		for (int I = 0; I < O1.NRP; I++) {// DO 25 I=1,NRP
			DEMP[I] = DELTA[I];// 25
		}
		SORT(DEMP, KEY, O1.NRP);
		int I = 0;
		double SWT = 0;
		for (; I < O1.NRP; I++) {// DO 27 I=1,NRP
			final int K = KEY[I];
			if (LDX[K - 1] != 0) {
				KK = LDX[K - 1];
				SWT = WT[KK - 1];
			}
			// 26
			if (WT[K - 1] > 0 || SWT > 0) {
				break;
			}
		} // 27 CONTINUE

		// 28
		O2.DMIN = DEMP[I];
		final double IDMIN = (O2.DMIN + 0.5);
		double OFD = O2.Z;
		double TFD = 2 * O2.Z;

		if (OFD < 5) {
			OFD = 5;
		}
		if (TFD < 10) {
			TFD = 10;
		}
		int JS = 4;
		if (RMS < 0.50 && ERH <= 5.0) {
			JS = 3;
		}
		if (RMS < 0.30 && ERH <= 2.5 && SE[2] <= 5.0) {
			JS = 2;
		}
		if (RMS < 0.15 && ERH < 1.0 && SE[2] <= 2.0) {
			JS = 1;
		}
		int JD = 4;
		final boolean goto30 = NO < 6;
		if (!goto30) {
			if (GAP <= 180 && O2.DMIN <= 50) {
				JD = 3;
			}
			if (GAP <= 135 && O2.DMIN <= TFD) {
				JD = 2;
			}
			if (GAP <= 90 && O2.DMIN <= OFD) {
				JD = 1;
			}
		}
		// 30
		O1.JAV = (JS + JD + 1) / 2;
		Q = CLASS[O1.JAV - 1];
		QS = CLASS[JS - 1];
		QD = CLASS[JD - 1];
		TIME2 = SEC + 1.D + 02 * KMIN + 1.D + 04 * C4.KHR + 1.D + 06 * C4.KDATE;

		final boolean goto52 = C1.IPRN == 0;
		boolean goto60 = false;
		if (!goto52) {
			goto60 = O1.NI != 1 || O1.NDEC >= 1 || O1.JPH == 1;
		}
		if (!goto60) {
			// 52
			int KKYR = C4.KDATE / 10000;
			int KKMO = (C4.KDATE - 10000 * KKYR) / 100;
			int KKDAY = C4.KDATE - 10000 * KKYR - 100 * KKMO;

			O1.JPH = 1;

			final boolean goto505 = C1.KSEL > 0;
			boolean goto535 = false;
			if (!goto505) {
				write("FPRINT_WRITER", "\n\n\n");
				goto535 = true;
			}
			if (!goto535) {
				write("FPRINT_WRITER", "1");

				write("FPRINT_WRITER", data(AHEAD, (int) KKYR, '/', (int) KKMO, '/', (int) KKDAY, C4.KHR, ':', (int) KMIN), "(/,30X,A48,T112,I2,A,I2,A,I2,4X,I2,A,I2)");

			}
			// 535
			goto60 = TIME2 - TIME1 > -20;
			if (!goto60) {
				write("FPRINT_WRITER", " ***** FOLLOWING EVENT IS OUT OF ORDER *****");
			}
		}
		// 60
		final boolean goto67 = O1.KP == 1 && C1.IPRN == 0;
		boolean goto100 = false;
		if (!goto67) {
			final boolean goto62 = O1.IPH == 1;
			if (!goto62) {
				write("FPRINT_WRITER", data("  ADJUSTMENTS (KM)  PARTIAL F-VALUES  STANDARD ERRORS  ADJUSTMENTS TAKEN", "  I  ORIG  LAT ", INS[0], "    LONG ", IEW[0], "   DEPTH  DM  RMS AVRPS SKD   CF   DLAT  DLON    DZ  DLAT  DLON    DZ  DLAT  "
						+ "DLON    DZ  DLAT  DLON    DZ"), "(/,59X,A,/,A,A1,A,A1,A)");

				if (C1.IPRN == 1) {
					O1.IPH = 1;
				}
			}
			// Integration code goes here
			results.addAdjustmentIteration(new AdjustmentIteration(O1.NI, SEC, (int) LAT1, LAT2, (int) LON1, LON2, O2.Z, RMK2, (int) IDMIN, RMS, O2.AVRPS, QS, O1.KF, QD, C2.FLIM, B[1], B[0], B[2], AF[1], AF[0], AF[2], SE[1], SE[0], SE[2], Y[1], Y[0], Y[2]));

			write("FPRINT_WRITER", data(O1.NI, SEC, (int) LAT1, '-', LAT2, (int) LON1, '-', LON2, O2.Z, RMK2, (int) IDMIN, RMS, O2.AVRPS, QS, O1.KF, QD, C2.FLIM, B[1], B[0], B[2], AF[1], AF[0], AF[2], SE[1], SE[0], SE[2], Y[1], Y[0], Y[2]), "(I3," + "F6.2,I3,A,F5.2,I4," + "A,"
					+ "F5.2," + "F6.2,A1,I3,F5.2,F6.2, 1X, A1, I1, A1, 13F6.2)");

			goto100 = O1.KP == 0;

		}
		if (!goto100) {
			// 67
			final int JNST = O1.KNST * 10 + O1.INST;

			if (NM == 0) {
				AVXM = 0;
			}
			if (NF == 0) {
				AVFM = 0;
			}
			MAGOUT = "      ";
			if (MAG != 99.9) {
				MAGOUT = getFormattedString(data(MAG), "(F6.2)");
			}
			SE3OUT = "     ";
			if (SE[2] != 0) {
				SE3OUT = getFormattedString(data(SE[2]), "(F5.1)");
			}
			ERHOUT = "     ";
			if (ERH != 0.) {
				ERHOUT = getFormattedString(data(ERH), "(F5.1)");
			}

			write("FPRINT_WRITER", data("  DATE    ORIGIN    LAT ", INS[0], "    LONG ", IEW[0], "    DEPTH    MAG NO DM GAP M  RMS  ERH  ERZ Q SQD  ADJ IN NR  AVR  AAR NM A     2VXM SDXM NF AVFM SDFM I"), "(//,A,A1,A,A1,A)");

			// Integration code goes here
			results.addHypocenterOutput(new Hypocenter(C4.KDATE, RMKO, C4.KHR, (int) KMIN, SEC, (int) LAT1, LAT2, (int) LON1, LON2, O2.Z, RMK2, MAGOUT, (int) NO, (int) IDMIN, (int) IGAP, KNO, RMS, ERHOUT, SE3OUT, Q, QS, QD, ADJ, JNST, O1.NR, O2.AVR, O2.AAR, NM, AVXM, SDXM, NF,
					AVFM, SDFM, O1.NI));

			write("FPRINT_WRITER",
					data(C4.KDATE, RMKO, C4.KHR, (int) KMIN, SEC, (int) LAT1, '-', LAT2, (int) LON1, '-', LON2, O2.Z, RMK2, MAGOUT, (int) NO, (int) IDMIN, (int) IGAP, KNO, RMS, ERHOUT, SE3OUT, Q, QS, '|', QD, ADJ, JNST, O1.NR, O2.AVR, O2.AAR, NM, AVXM, SDXM, NF, AVFM, SDFM,
							O1.NI), "(1X,I6,A1,2I2,F6.2,I3,A,F5.2,I4,A,F5.2,1X,F6.2,A1,A6,2I3,I4,I2,F5.2,2A5,2(1X,A1),A,A1,F5.2,2I3,2F5.2,2(I3,2F5.1),I2)");

			if (QRMK[0] != SYMBOL[3] && QRMK[0] != SYMBOL[4]) {
				QRMK[0] = SYMBOL[0];
			}
			SYM3 = SYMBOL[KNO];
			if (C1.IPUN != 0) {
				// Integration code goes here
				results.addHypocenterOutput(new Hypocenter(C4.KDATE, C4.KHR, (int) KMIN, SEC, (int) LAT1, LAT2, (int) LON1, LON2, O2.Z, RMK2, MAGOUT, (int) NO, (int) IGAP, O2.DMIN, RMS, ERHOUT, SE3OUT, new String(new char[] { QRMK[0], Q, SYM3 })));

				write("FPUNCH_WRITER", data(C4.KDATE, C4.KHR, KMIN, SEC, (int) LAT1, "-", LAT2, (int) LON1, "-", LON2, O2.Z, RMK2, MAGOUT, (int) NO, IGAP, O2.DMIN, RMS, ERHOUT, SE3OUT, QRMK[0], Q, SYM3), "(I6,1X,2I2," + "F6.2,I3,A," + "F5.2,I4,A,F5.2,1X,F6.2,A1,A6,I3,I4,F5.1,"
						+ "" + "F5.2,2A5,3A1)");
			}
		}
		// 100 CONTINUE

		SUCARD = getFormattedString(data(C4.KDATE, C4.KHR, (int) KMIN, SEC, (int) LAT1, "-", LAT2, (int) LON1, "-", LON2, O2.Z, RMK2, MAGOUT, (int) NO, (int) IGAP, O2.DMIN, RMS, ERHOUT, SE3OUT, QRMK[0], Q, SYM3), "(I6,1X,2I2," + ""
				+ "F6.2,I3,A,F5.2,I4,A,F5.2,1X,F6.2,A1,A6,I3,I4,F5.1,F5.2,2A5,3A1)");

		if (O1.KP != 1) {
			if (C1.IPRN <= 1) {
				C4.KHR = JHR;// 300
				O2.ORG = OSAVE;
				return;
			}
		}

		write("FPRINT_WRITER", "\n  STN  DIST AZM AIN PRMK HRMN P-SEC TPOBS TPCAL DLY/H1 P-RES P-WT AMX PRX CALX K XMAG RMK FMP FMAG SRMK S-SEC TSOBS S-RES  S-WT    DT\n");

		for (I = 0; I < O1.NRP; I++) {// DO 200 I=1,NRP

			int K = I;
			if (C1.KSORT == 1) {
				K = KEY[I] - 1;
			}
			final int KJI = KDX[K];
			double TPK = TP[K] - O2.ORG;
			if (TPK < 0.) {
				TPK = TPK + 3600;
			}
			X4KOUT = getFormattedString(data(X[3][K]), "(F6.2)");

			if (!(AZRES[K] != " . " && AZRES[K] != " " && AZRES[K] != "0. ")) {
				X4KOUT = "      ";
			}
			RMK3 = ' '; // 114
			if (XMAG[K] != 99.9) {
				if (Math.abs(XMAG[K] - AVXM) >= 0.5) {
					RMK3 = '*';
				}
			}

			RMK4 = ' ';// 115
			if (FMAG[K] != 99.9) {
				if (Math.abs(FMAG[K] - AVFM) >= 0.5) {
					RMK4 = '*';
				}
			}

			XMAGOU = "    ";// 130
			if (XMAG[K] != 99.9) {
				XMAGOU = getFormattedString(data(XMAG[K]), "(F4.1)");
			}

			FMAGOU = "    ";
			if (FMAG[K] != 99.9) {
				FMAGOU = getFormattedString(data(FMAG[K]), "(F4.1)");
			}

			final double IAZ = AZ[K] + 0.5;
			final double IAIN = AIN[K] + 0.5;
			final double IAMX = AMX[K];
			final double IPRX = 100 * PRX[K] + 0.5;

			FMPOUT = "    ";
			final double IFMPK = FMP[K];
			if (FMP[K] != 0) {
				FMPOUT = getFormattedString(data((int) IFMPK), "(I4)");
			}

			double TSK = 0;
			boolean goto165 = false;
			boolean goto168 = false;

			if (LDX[K] == 0) {
				// CHECK FOR SMP DATA
				if (KSMP[K] != 0) {
					SRESOU = getFormattedString(data(X[3][K]), "(F6.2)");
					RMK5 = " ";
					SWTOUT = "****";
					TSK = S[K] - P[K];
					TSKOUT = getFormattedString(data(TSK), "(F6.2)");
					goto168 = true;
				} else {
					goto165 = true;
				}
			}

			if (!goto165 && !goto168) {

				KK = LDX[K];// 163
				RMK5 = WRK[KK - 1];
				SWT = WT[KK - 1];
				TSK = TS[K] - O2.ORG;
				SRESOU = getFormattedString(data(X[3][KK - 1]), "(F6.2)");
				SWTOUT = getFormattedString(data(SWT - 1), "(F4.1)");
				TSKOUT = getFormattedString(data(TSK - 1), "(F6.2)");
				goto168 = true;
			}

			if (!goto168) {
				SKOUT = " ";// 165
				TSKOUT = " ";
				SRESOU = " ";
				RMK5 = " ";
				SWTOUT = " ";

			}
			double DLYK = DLY[KNO - 1][KJI - 1];

			if (ISW == "1 ") {// 168
				DLYK = FLT[KNO - 1][KJI - 1];
			}
			DTKOUT = " ";

			if (DT[K] != 0) {
				DTKOUT = getFormattedString(data(DT[K]), "(F6.2)");
			}

			if (S[K] != 999.99) {
				SKOUT = getFormattedString(data(S[K]), "(F6.2)");
			}

			write("FPRINT_WRITER",
					data(MSTA[K], DELTA[K], (int) IAZ, (int) IAIN, PRMK[K], JHR, JMIN[K], P[K], TPK, T[K], DLYK, X4KOUT, WRK[K], WT[K], (int) IAMX, (int) IPRX, CAL[K], KLAS[KJI - 1], XMAGOU, RMK3, RMK[K], FMPOUT, FMAGOU, RMK4, SRMK[K], SKOUT, TSKOUT, SRESOU, RMK5, SWTOUT,
							DTKOUT, IW[KJI - 1]), "(1X,A4,F6.1,2I4,1X,A4,1X,2I2,4F6.2,A6,A2,F4.2,I4,I3,F6.2,I2,A4,A1,1X,A3,A4,A4,A1,1X,A4,3A6,A2,A4,A6,T6,A1)");

			results.addStationToTheResultList(new Station(MSTA[K], DELTA[K], (int) IAZ, (int) IAIN, PRMK[K], JHR, JMIN[K], P[K], TPK, T[K], DLYK, X4KOUT, WRK[K], WT[K], (int) IAMX, (int) IPRX, CAL[K], KLAS[KJI - 1], XMAGOU, RMK3, RMK[K], FMPOUT, FMAGOU, RMK4, SRMK[K], SKOUT,
					TSKOUT, SRESOU, RMK5, SWTOUT, DTKOUT, IW[KJI - 1]));

			if (C1.IPUN != 2) {
				continue;
			}
			final double ISEC = 100 * SEC;
			write("FPUNCH_WRITER", data(MSTA[K], DELTA[K], AZ[K], AIN[K], PRMK[K], TPK, X4KOUT, WT[K], XMAGOU, RMK[K], FMAGOU, C4.KDATE, C4.KHR, KMIN, ISEC, KJI, SYM3), "(A4,3F6.1,1X,A4,F6.2,A6,F5.1,A6,1X,A3,A6,I7," + "2I2,2I4," + "A1)");

		}// 200 CONTINUE

		if (C1.IPUN == 2) {
			write("FPUNCH_WRITER", " $$$");
		}
		C4.KHR = JHR;// 300
		O2.ORG = OSAVE;
	}

	/*
	 * SUBROUTINE TRVDRV(ISW,V,D,DEPTH,VSQ,NL,THK,H,G,F,TID,DID,FLT, &
	 * DELTA,DX,DY,NR,KDX,KNO,FLTEP,Z,ZSQ,X,T,ANIN)
	 */
	@SuppressWarnings("boxing")
	public void TRVDRV(final String ISW, final double[] V, final double[] D, final double[] DEPTH, final double[] VSQ, final double[] THK, final double[] H, final double[][] G, final double[][] F, final double[][] TID, final double[][] DID, final double[][] FLT,
			final double[] DELTA, final double[] DX, final double[] DY, final int[] KDX, final double FLTEP, final double[][] X, final double[] T, final double[] ANIN) throws IOException, ParseException {

		// REAL*4 TINJ(21),DIDJ(21),TR(21),TID(21,21),DID(21,21),F(21,21)
		final double[] TINJ = new double[21];
		final double[] DIDJ = new double[21];
		final double[] TR = new double[21];

		final boolean goto5 = ISW.equals("1   ");
		boolean goto2 = false;

		int L;
		int JL = 0;
		int JJ = 0;
		double TKJSQ = 0;
		double TKJ = 0;
		double XOVMAX = 0;

		if (!goto5) {
			// INITIALIZATION FOR FIXED LAYER MODEL
			for (L = 0; L < C4.NL; L++) {// DO 1 L=1,NL
				if (D[L] > O2.Z) {
					goto2 = true;
					break;
				}
			}// 1 CONTINUE
			if (!goto2) {
				JL = C4.NL - 1;
			} else {
				JJ = L; // 2
				JL = L - 1;
			}
			TKJ = O2.Z - D[JL];// 3
			TKJSQ = TKJ * TKJ + 0.000001;
			if (JL + 1 != C4.NL) {
				for (L = JJ; L < C4.NL; L++) {// DO 4 L=JJ,NL
					final double SQT = Math.sqrt(VSQ[L] - VSQ[JL]);
					TINJ[L] = (TID[JL][L] - TKJ * SQT / (V[L] * V[JL]));
					DIDJ[L] = (DID[JL][L] - TKJ * V[JL] / SQT); // 4
				}
				XOVMAX = V[JJ] * V[JL] * (TINJ[JJ] - TID[JL][JL]) / (V[JJ] - V[JL]);
			}
		}
		boolean goto45 = false;
		double SQT;
		double TIX;
		double DH1 = 0;
		double DH2 = 0;
		boolean goto100 = false;
		double DTDD = 0;
		double DTDH = 0;
		double TMIN = 0;
		boolean goto260 = false;
		boolean goto80 = false;
		double U = 0;
		double USQ = 0;
		int K = 0;
		for (int I = 0; I < O1.NR; I++) { // 5 DO 300 I=1,NR
			if (!goto80) {
				if (ISW != null && !ISW.equals("1   ")) {
					goto45 = true;
				}
				if (!goto45) {

					// INITIALIZATION FOR VARIABLE LAYER MODEL
					final int JI = KDX[I];
					DEPTH[1] = FLT[KNO - 1][JI - 1];
					if (O2.Z < FLTEP) {
						DEPTH[1] = (0.5 * (FLT[KNO - 1][JI - 1] + FLTEP));
					}

					THK[0] = DEPTH[1];
					THK[1] = D[2] - DEPTH[1];
					DH1 = THK[0] - H[0];
					DH2 = THK[1] - H[1];
					boolean goto20 = false;
					boolean goto30 = false;

					for (L = 0; L < C4.NL; L++) { // DO 10 L=1,NL
						if (DEPTH[L] > O2.Z) {
							goto20 = true;
							break;
						}
					}// 10 CONTINUE
					if (!goto20) {
						JL = C4.NL - 1;
						goto30 = true;
					}
					if (!goto30) {
						JJ = L;// 20
						JL = L - 1;
					}

					// check here..
					TKJ = O2.Z - DEPTH[JL];// 30
					TKJSQ = TKJ * TKJ + 0.000001;
					if (JL + 1 == C4.NL) {
						goto100 = true;
					}
					if (!goto100) {
						// CALCULATION FOR REFRACTED WAVES
						for (L = JJ; L < C4.NL; L++) {// DO 40 L=JJ,NL
							SQT = Math.sqrt(VSQ[L] - VSQ[JL]);
							TIX = F[0][JL] * DH1 * G[0][L] + F[1][JL] * DH2 * G[1][L] + TID[JL][L];
							final double DIX = F[0][JL] * DH1 * G[2][L] + F[1][JL] * DH2 * G[3][L] + DID[JL][L];
							TINJ[L] = (TIX - TKJ * SQT / (V[L] * V[JL]));
							DIDJ[L] = (DIX - TKJ * V[JL] / SQT); // 40
						}

						TIX = F[0][JL] * DH1 * G[0][JL] + F[1][JL] * DH2 * G[1][JL] + TID[JL][JL];
						XOVMAX = V[JJ] * V[JL] * (TINJ[JJ] - TIX) / (V[JJ] - V[JL]);
					}
				}
			}
			if (!goto100 || goto80) {
				boolean goto90 = false;
				if (!goto80) {
					if (goto45) {
						if (JL + 1 == C4.NL) {// 45
							goto100 = true;
						}
					}
					if (!goto100) {
						for (int M = JJ; M < C4.NL; M++) {// 50 DO 60 M=JJ,NL
							TR[M] = TINJ[M] + DELTA[I] / V[M];// 60
						}
						TMIN = 999.99;
						// K = 0;
						for (int M = JJ; M < C4.NL; M++) {// DO 70 M=JJ,NL
							if (TR[M] > TMIN) {
								continue;
							}
							if (DIDJ[M] > DELTA[I]) {
								continue;
							}
							K = M;
							TMIN = TR[M];
						}// 70 CONTINUE

						if (DELTA[I] < XOVMAX) {
							goto90 = true;
						}

						// C-----TRAVEL TIME & DERIVATIVES FOR REFRACTED WAVE
					}
				}
				goto80 = false;
				do {
					if (!goto90) {
						T[I] = TR[K];// 80
						DTDD = 1.0 / V[K];
						DTDH = -Math.sqrt(VSQ[K] - VSQ[JL]) / (V[K] * V[JL]);
						ANIN[I] = -V[JL] / V[K];
						goto260 = true;
						break;
					} else {
						goto90 = false;
					}

					// CALCULATION FOR DIRECT WAVE
					if (JL + 1 != 1) {// 90
						goto100 = true;
						break;
					}
					SQT = Math.sqrt(O2.ZSQ + DELTA[I] * DELTA[I]);
					final double TDJ1 = SQT / V[0];
					if (TDJ1 >= TMIN) {
						goto80 = true;
					}
					if (!goto80) {
						// C-----TRAVEL TIME & DERIVATIVES FOR DIRECT WAVE
						// IN FIRST LAYER
						T[I] = TDJ1;
						DTDD = DELTA[I] / (V[0] * SQT);
						DTDH = O2.Z / (V[0] * SQT);
						ANIN[I] = (DELTA[I] / SQT);
						goto260 = true;
						break;
						// C-----FIND A DIRECT WAVE THAT WILL EMERGE AT THE
						// STATION
					}
				} while (goto80);
			}

			goto100 = false;
			if (!goto260) {
				double XBIG = DELTA[I];// 100
				double XLIT = DELTA[I] * TKJ / O2.Z;
				final double UB = XBIG / Math.sqrt(XBIG * XBIG + TKJSQ);
				final double UL = XLIT / Math.sqrt(XLIT * XLIT + TKJSQ);
				final double UBSQ = UB * UB;
				final double ULSQ = UL * UL;
				double DELBIG = TKJ * UB / Math.sqrt(1.000001 - UBSQ);
				double DELLIT = TKJ * UL / Math.sqrt(1.000001 - ULSQ);
				final int J1 = JL - 1;
				for (L = 0; L <= J1; L++) { // DO 110 L=1,J1
					DELBIG = DELBIG + THK[L] * UB / Math.sqrt(VSQ[JL] / VSQ[L] - UBSQ);
					DELLIT = DELLIT + THK[L] * UL / Math.sqrt(VSQ[JL] / VSQ[L] - ULSQ);// 110
				}
				boolean goto190 = false;
				for (int LL = 0; LL < 25; LL++) { // DO 170 LL=1,25
					if (DELBIG - DELLIT < 0.02) {
						break;
					}
					final double XTR = XLIT + (DELTA[I] - DELLIT) * (XBIG - XLIT) / (DELBIG - DELLIT);
					U = XTR / Math.sqrt(XTR * XTR + TKJSQ);
					USQ = U * U;
					double DELXTR = TKJ * U / Math.sqrt(1.000001 - USQ);
					for (L = 0; L <= J1; L++) {// DO 120 L=1,J1
						DELXTR = DELXTR + THK[L] * U / Math.sqrt(VSQ[JL] / VSQ[L] - USQ); // 120
					}
					final double XTEST = DELTA[I] - DELXTR;
					if (Math.abs(XTEST) <= 0.02) {
						goto190 = true;
						break;
					}
					if (XTEST < 0) {
						XBIG = XTR;// 140
						DELBIG = DELXTR;
					} else if (XTEST == 0) {
						goto190 = true;
						break;
					} else {
						XLIT = XTR;// 150
						DELLIT = DELXTR;
					}
					if (LL + 1 < 10) {// 160
						continue;
					}
					if (1.0 - U < 0.0002) {
						goto190 = true;
						break;
					}
				}// 170 CONTINUE

				if (!goto190) {
					final double XTR = 0.5 * (XBIG + XLIT);// 180
					U = XTR / Math.sqrt(XTR * XTR + TKJSQ);
					USQ = U * U;
				}

				if (1.0 - U <= 0.0002) {// 190
					// if U IS TOO NEAR 1, COMPUTE TDIR AS WAVE ALONG THE TOP OF
					// LAYER JL

					double TDC = 0;
					if (!ISW.equals("1   ")) {
						TIX = F[0][JL] * DH1 * G[0][JL] + F[1][JL] * DH2 * G[1][JL] + TID[JL][JL]; // 195
						TDC = TIX + DELTA[I] / V[JL];
					} else {
						TDC = TID[JL][JL] + DELTA[I] / V[JL];
					}

					if (JL + 1 != C4.NL && TDC >= TMIN) {
						I--;
						goto80 = true;
						continue;
					}
					T[I] = TDC;// 210
					DTDD = 1.0 / V[JL];
					DTDH = 0.0;
					ANIN[I] = 0.9999999;
					goto260 = true;
					// C-----TRAVEL TIME & DERIVATIVES FOR DIRECT WAVE BELOW
					// FIRST LAYER
				}
				if (!goto260) {
					double TDIR = TKJ / (V[JL] * Math.sqrt(1.0 - USQ)); // 220
					for (L = 0; L <= J1; L++) { // DO 240 L=1,J1
						TDIR = TDIR + THK[L] * V[JL] / (VSQ[L] * Math.sqrt(VSQ[JL] / VSQ[L] - USQ));
					}// 240

					if (JL + 1 != C4.NL && TDIR >= TMIN) {
						I--;
						goto80 = true;
						continue;

					}
					T[I] = TDIR;// 245
					final double SRR = Math.sqrt(1 - USQ);
					final double SRT = SRR * SRR * SRR;
					double ALFA = TKJ / SRT;
					double BETA = TKJ * U / (V[JL] * SRT);
					for (L = 0; L <= J1; L++) {// DO 250 L=1,J1
						final double e = Math.sqrt(VSQ[JL] / VSQ[L] - USQ);
						final double STK = e * e * e;
						final double VTK = THK[L] / (VSQ[L] * STK);
						ALFA = ALFA + VTK * VSQ[JL];
						BETA = BETA + VTK * V[JL] * U;// 250
					}
					DTDD = BETA / ALFA;
					DTDH = (1.0 - V[JL] * U * DTDD) / (V[JL] * SRR);
					ANIN[I] = U;
				}
			}
			// C-----SET UP PARTIAL DERIVATIVES FOR REGRESSION ANALYSIS
			X[0][I] = (-DTDD * DX[I] / DELTA[I]);// 260
			X[1][I] = (-DTDD * DY[I] / DELTA[I]);
			X[2][I] = DTDH;
			goto260 = false;
		}// 300 CONTINUE
			// RETURN
			// END
	}

	/*
	 * SUBROUTINE AZWTOS(DX,DY,NR,WT,KDX,AZ,TEMP,KEY,INS,IEW)
	 */
	public void AZWTOS(final double[] DX, final double[] DY, final double[] WT, final int[] KDX, final double[] AZ, final double[] TEMP, final int[] KEY, final char[] INS, final char[] IEW) throws IOException, ParseException {

		final int[] KTX = new int[4];
		final int[] KEMP = new int[101];
		final double[] TX = new double[4];
		final double[] TXN = new double[4];
		int J = 0;

		for (int I = 0; I < O1.NR; I++) {// DO 10 I=1,
			if (WT[I] == 0.) {
				continue;
			}
			double DXI = DX[I];
			double DYI = DY[I];
			if (!(DXI == 0 && DYI == 0)) {
				final int JI = KDX[I];
				if (INS[JI - 1] == 'S') {
					DYI = -DYI;
				}
				if (IEW[JI - 1] == 'W') {
					DXI = -DXI;
				}
				AZ[I] = ((Math.atan2(DXI, DYI) * 57.29578 + 360) % 360);
			} else {
				AZ[I] = 999;
			}
			J = J + 1;// 7
			TEMP[J - 1] = AZ[I];
		} // 10 CONTINUE

		SORT(TEMP, KEY, J);

		GAP = TEMP[0] + 360 - TEMP[J - 1];
		int IG = 1;
		for (int I = 1; I < J; I++) {// DO 20 I=2,J
			final double DTEMP = TEMP[I] - TEMP[I - 1];
			if (DTEMP <= GAP) {
				continue;
			}
			GAP = DTEMP;
			IG = I;
		} // 20 CONTINUE

		TX[0] = (TEMP[IG] - 0.5 * GAP);
		TX[1] = TX[0] + 90;
		TX[2] = TX[0] + 180;
		TX[3] = TX[0] + 270;
		for (int I = 0; I < 4; I++) {// DO 124 I=1,4
			TXN[I] = 0;
			if (TX[I] < 0) {
				TX[I] = TX[I] + 360;
			}
			if (TX[I] > 360) {
				TX[I] = TX[I] - 360;
			}
		} // 124 CONTINUE

		// CALL SORT(TX,KTX,4)
		SORT(TX, KTX, 4);
		for (int I = 0; I < O1.NR; I++) {// DO 130 I=1,NR

			if (WT[I] == 0.) {
				continue;
			}
			if (AZ[I] <= TX[0]) {
				TXN[0] = TXN[0] + 1;// 125
				KEMP[I] = 1;
				continue;
			}

			if (AZ[I] <= TX[1]) {// 126
				TXN[1] = TXN[1] + 1;
				KEMP[I] = 2;
				continue;
			}
			if (AZ[I] <= TX[2]) {// 127
				TXN[2] = TXN[2] + 1;
				KEMP[I] = 3;
				continue;
			}
			if (AZ[I] > TX[3]) {// 128
				TXN[0] = TXN[0] + 1;// 125
				KEMP[I] = 1;
				continue;
			}
			TXN[3] = TXN[3] + 1;
			KEMP[I] = 4;
		}// 130 CONTINUE

		int XN = 4;
		if (TXN[0] == 0) {
			XN = XN - 1;
		}
		if (TXN[1] == 0) {
			XN = XN - 1;
		}
		if (TXN[2] == 0) {
			XN = XN - 1;
		}
		if (TXN[3] == 0) {
			XN = XN - 1;
			final int FJ = J / XN;
			for (int I = 0; I < O1.NR; I++) { // DO 150 I=1,NR
				if (WT[I] == 0) {
					continue;
				}
				final int KI = KEMP[I];
				WT[I] = WT[I] * FJ / TXN[KI - 1];
			}// 150 CONTINUE
				// RETURN
				// END
		}
	}

	/*
	 * SUBROUTINE MISING(NSTA,LAT,LON,NS,MAG,TEMP,DMIN,JDX,
	 * JMAX,O2.LATEP,LONEP,INS,IEW)
	 */
	public void MISING(final double MAG, final double[] TEMP) throws IOException, ParseException {

		int IHD = 0;
		final int NJ = O1.JMAX + 1;
		TEMP[NJ - 1] = TEMP[0] + 360;
		double TDEL = 25 * (MAG * MAG);
		if (MAG == 99.9) {
			TDEL = 100;
		}
		for (int I = 0; I < C4.NS; I++) { // DO 30 I=1,
			if (JDX[I] == 1) {
				continue;
			}
			final double PHI = 0.0174532 * ((LAT[I] + O2.LATEP) / 120);
			final double SINPHI = Math.sin(PHI);
			final double SINP2 = SINPHI * SINPHI;
			final double SINP4 = SINP2 * SINP2;
			final double CA = 1.8553654 + 0.0062792 * SINP2 + 0.0000319 * SINP4;
			final double CB = 1.8428071 + 0.0187098 * SINP2 + 0.0001583 * SINP4;
			double DXI = (LON[I] - O2.LONEP) * CA * Math.cos(PHI);
			double DYI = (LAT[I] - O2.LATEP) * CB;
			final double DELI = Math.sqrt(DXI * DXI + DYI * DYI) + 0.000001;
			if (DELI > TDEL) {
				continue;
			}
			// C CHECK LATITUDE AND LONGITUDE
			if (INS[I] == 'S') {
				DYI = -DYI;
			}
			if (IEW[I] == 'W') {
				DXI = -DXI;
			}
			double AZI = (Math.atan2(DXI, DYI) * 57.29578 + 360) % 360;
			if (AZI <= TEMP[0]) {
				AZI = AZI + 360;
			}
			int J;
			for (J = 0; J < NJ; J++) {// DO 10 J=2,
				if (AZI < TEMP[J]) {
					break;
				}
			}// 10 CONTINUE
				// J = NJ;
			final double EXGAP = TEMP[J] - TEMP[J - 1];// 20
			double RDGAP = TEMP[J] - AZI;
			final double TGAP = AZI - TEMP[J - 1];
			if (TGAP < RDGAP) {
				RDGAP = TGAP;
			}
			if (DELI > O2.DMIN && RDGAP < 30.) {
				continue;
			}
			if (AZI >= 360) {
				AZI = AZI - 360;
			}
			if (IHD != 1) {
				write("FPUNCH_WRITER", data("MISSING STATION DELTA AZIM EX-GAP RD-GAP"), "(/,10X,A)");
				IHD = 1;
			}
			// Integration code goes here
			results.addMissingStation(new Station(NSTA[I], DELI, AZI, EXGAP, RDGAP));
			write("FPUNCH_WRITER", data(NSTA[I], DELI, AZI, EXGAP, RDGAP), "(21X,A4,2F7.1,2F8.1)");
		}// 30 CONTINUE
			// RETURN
			// END
	}

	/*
	 * SUBROUTINE SUMOUT(ISW,NSTA,INS,IEW,IELV,DLY,FMGC,XMGC,KLAS,PRR, &
	 * CALR,ICAL,NDATE,NHRMN,LAT,LON,MDATE,MHRMN,KLSS,CALS,NS,QNO,IPUN, &
	 * MNO,NRES,NXM,NFM,SR,SRSQ,SRWT,SXM,SXMSQ,SFM,SFMSQ)
	 */
	public void SUMOUT() throws IOException, ParseException {

		// REAL*4 LAT2,LON2,QNO(4)

		// REAL*4 AVRES(4,151),SDRES(4,151)
		final double[][] AVRES = new double[4][151];
		final double[][] SDRES = new double[4][151];

		final double QSUM = QNO[0] + QNO[1] + QNO[2] + QNO[3];

		if (QSUM != 0.) {
			// Integration code goes here
			results.setStats(new Stats(QNO[0], QNO[1], QNO[2], QNO[3], QSUM, 100. * QNO[0] / QSUM, 100. * QNO[1] / QSUM, 100. * QNO[2] / QSUM, 100. * QNO[3] / QSUM));
			List dataList = convertArrayToListOfObjects(QNO);
			write("FPRINT_WRITER", data("1 ***** CLASS: A B C D TOTAL *****", "NUMBER", dataList.get(0), dataList.get(1), dataList.get(2), dataList.get(3), QSUM), "(A,//,7X,A,5F6.1)");
			for (int I = 0; I < 4; I++) {// DO 10 I=1,4
				// 10 QNO[I]=100.*QNO[I]/QSUM
				QNO[I] = 100. * QNO[I] / QSUM;
			}
			dataList = convertArrayToListOfObjects(QNO);
			write("FPRINT_WRITER", data("%:", dataList.get(0), dataList.get(1), dataList.get(2), dataList.get(3)), "(/,12X,A,4F6.1)");
			write("FPRINT_WRITER", data("TRAVELTIME RESIDUALS (MODEL=1)", "TRAVELTIME RESIDUALS (MODEL=2)", "X-MAGNITUDE RESIDUALS", "F-MAGNITUDE RESIDUALS", " STATION   NRES    SRWT   AVRES   S     3DRES       NRES    SRWT   AVRES   SDRES        "
					+ "NXM    AVXM    SDXM        NFM    AVFM    SDFM"), "(///,10X,A,5X,A,5X,A,6X,A,/,A)");

			for (int I = 0; I < C4.NS; I++) {// DO 70 I=1,NS
				for (int J = 0; J < 4; J++) {// DO 30 J=1,4
					AVRES[J][I] = 0;
					SDRES[J][I] = 0;// 30
				}
				if (NRES[0][I] != 0) {
					AVRES[0][I] = SR[0][I] / SRWT[0][I];
					SDRES[0][I] = Math.sqrt(SRSQ[0][I] / SRWT[0][I] - AVRES[0][I] * AVRES[0][I] + 0.000001);
				}
				if (NRES[1][I] != 0) {// 35
					AVRES[1][I] = SR[1][I] / SRWT[1][I];
					SDRES[1][I] = Math.sqrt(SRSQ[1][I] / SRWT[1][I] - AVRES[1][I] * AVRES[1][I] + 0.000001);
				}

				if (NXM[I] != 0) {// 40
					AVRES[2][I] = SXM[I] / NXM[I];
					SDRES[2][I] = Math.sqrt(SXMSQ[I] / NXM[I] - AVRES[2][I] * AVRES[2][I] + 0.000001);
				}
				if (NFM[I] != 0) {// 50
					AVRES[3][I] = SFM[I] / NFM[I];
					SDRES[3][I] = Math.sqrt(SFMSQ[I] / NFM[I] - AVRES[3][I] * AVRES[3][I] + 0.000001);
				}
				// Integration code goes here
				results.addStationToSummaryList(new Station(NSTA[I], NRES[0][I], SRWT[0][I], AVRES[0][I], SDRES[0][I], NRES[1][I], SRWT[1][I], AVRES[1][I], SDRES[1][I], NXM[I], AVRES[2][I], SDRES[2][I], NFM[I], AVRES[3][I], SDRES[3][I]));
				write("FPRINT_WRITER", data(NSTA[I], NRES[0][I], SRWT[0][I], AVRES[0][I], SDRES[0][I], NRES[1][I], SRWT[1][I], AVRES[1][I], SDRES[1][I], NXM[I], AVRES[2][I], SDRES[2][I], NFM[I], AVRES[3][I], SDRES[3][I]), "(4X," + ""
						+ "A4,2X,I5,3F8.2,6X,I5,3F8.2,2(6X,I5,2F8.2))");
			}// 70 CONTINUE

		}
		if (C1.IPUN == 3) {// 72
			// C------- PUNCH STATION LIST WITH REVISED DELAYS,XMGC,AND FMGC
			if (ISW != "1 ") {
				write("FPRINT_WRITER", data("1 ***** NEW STATION LIST *****", "I STN LAT ", INS[0], " LONG ", IEW[0], " ELV DELAY", "FMGC XMGC KL PRR CALR IC IS DATE HRMN"), "(A,///, 4X,A,A1,A,A1,A,5X,A)");
			} else {
				write("FPRINT_WRITER", data("1 ***** NEW STATION LIST *****", "I STN LAT ", INS[0], " LONG ", IEW[0], " ELV M DLY1DLY2 XMGC FMGC K CALR IC DATE HRMN"), "(A,///, 4X,A,A1,A,A1,A)");
			}

			for (int I = 0; I < C4.NS; I++) {// 90 DO 120 I=1,NS
				DLY[0][I] = DLY[0][I] + AVRES[0][I];
				if (ISW == "1 ") {
					DLY[1][I] = DLY[1][I] + AVRES[1][I];
				}
				XMGC[I] = XMGC[I] + AVRES[2][I];
				FMGC[I] = FMGC[I] + AVRES[3][I];
				LAT1 = (LAT[I] / 60.0);
				LAT2 = LAT[I] - 60 * (int) LAT1;
				LON1 = (LON[I] / 60.0);
				LON2 = LON[I] - 60 * (int) LON1;
				if (ISW != "1 ") {
					// Integration code goes here
					results.addStationToSummaryList(new Station((char) I, NSTA[I], (int) LAT1, LAT2, INS[I], (int) LON1, LON2, IEW[I], IELV[I], DLY[0][I], FMGC[I], XMGC[I], KLSS[I], PRR[I], CALS[I], ICAL[I], NDATE[I], NHRMN[I]));

					write("FPRINT_WRITER", data(I, NSTA[I], LAT1, LAT2, INS[I], LON1, LON2, IEW[I], IELV[I], DLY[0][I], FMGC[I], XMGC[I], KLSS[I], PRR[I], CALS[I], ICAL[I], NDATE[I], NHRMN[I]), "(I5,2X,A4,I2,F5.2,A1,I4,F5.2,A1," + ""
							+ "I5,F6.2,4X,F5.2,2X,F5.2,I2,1X,F4.2,1X,F6.2,I2,5X,I6,I4)");

					write("FPUNCH_WRITER", data(NSTA[I], LAT1, LAT2, INS[I], LON1, LON2, IEW[I], IELV[I], DLY[0][I], FMGC[I], XMGC[I], KLSS[I], PRR[I], CALS[I], ICAL[I], NDATE[I], NHRMN[I]), "(2X,A4,I2,F5.2,A1,I3,F5.2,A1,I4,F6.2," + ""
							+ "T38,F5.2,T45,F5.2,I2,1X,F4.2,1X,F6.2,I2,T71,I6,I4)");
				} else {
					// Integration code goes here
					results.addStationToSummaryList(new Station((char) I, NSTA[I], (int) LAT1, LAT2, INS[I], (int) LON1, LON2, IEW[I], IELV[I], MNO[I], DLY[0][I], DLY[1][I], XMGC[I], FMGC[I], KLSS[I], CALS[I], ICAL[I], NDATE[I], NHRMN[I]));

					write("FPRINT_WRITER", data(I, NSTA[I], LAT1, "-", LAT2, INS[I], LON1, "-", LON2, IEW[I], IELV[I], MNO[I], DLY[0][I], DLY[1][I], XMGC[I], FMGC[I], KLSS[I], CALS[I], ICAL[I], NDATE[I], NHRMN[I]), "(I5,2X,A4,I3," + "" + "A,F5.2,A1,I4,A,"
							+ "F5.2,A1,I5,I6,2F6.2,2F6.2,I2,F6.2,I2,2X,I6,I4)");

					write("FPUNCH_WRITER", data(NSTA[I], LAT1, "-", LAT2, INS[I], LON1, "-", LON2, IEW[I], IELV[I], MNO[I], DLY[0][I], DLY[1][I], XMGC[I], FMGC[I], KLSS[I], CALS[I], ICAL[I], NDATE[I], NHRMN[I]), "(A4,I3,A,F5.2," + "" + "A1,I3,A,F5.2,A1,"
							+ "I4,I6,2F6.2,2F6.2,I2,F6.2,I2,2X,I6,I4)");
				}

			}// 120 CONTINUE
		}
		if (C1.IPUN != 4) { // 200
			return;
		}
		if (ISW != "1 ") {
			write("FPRINT_WRITER", data("1 ***** NEW STATION LIST *****", "I STN LAT ", INS[0], " LONG ", IEW[0], " ELV DELAY", "FMGC XMGC KL PRR CALR IC IS DATE HRMN"), "(A,///, 4X,A,A1,A,A1,A,5X,A)");
		} else {
			write("FPRINT_WRITER", data("1 ***** NEW STATION LIST *****", "I STN LAT ", INS[0], " LONG ", IEW[0], " ELV M DLY1DLY2 XMGC FMGC K CALR IC DATE HRMN"), "(A,///, 4X,A,A1,A,A1,A)");
		}

		for (int I = 0; I < C4.NS; I++) { // 206 DO 220 I=1,NS

			LAT1 = (LAT[I] / 60.0);
			LAT2 = LAT[I] - 60 * (int) LAT1;
			LON1 = (LON[I] / 60);
			LON2 = LON[I] - 60 * (int) LON1;
			if (ISW != "1 ") {
				// Integration code goes here
				results.addStationToSummaryList(new Station((char) I, NSTA[I], (int) LAT1, LAT2, INS[I], (int) LON1, LON2, IEW[I], IELV[I], DLY[0][I], FMGC[I], XMGC[I], KLSS[I], PRR[I], CALS[I], ICAL[I], NDATE[I], NHRMN[I]));

				write("FPRINT_WRITER", data(I, NSTA[I], LAT1, LAT2, INS[I], LON1, LON2, IEW[I], IELV[I], DLY[0][I], FMGC[I], XMGC[I], KLSS[I], PRR[I], CALS[I], ICAL[I], NDATE[I], NHRMN[I]), "(I5,2X,A4,I2,F5.2,A1,I4,F5.2,A1," + ""
						+ "I5,F6.2,4X,F5.2,2X,F5.2,I2,1X,F4.2,1X,F6.2,I2,5X,I6,I4)");

				write("FPUNCH_WRITER", data(NSTA[I], LAT1, LAT2, INS[I], LON1, LON2, IEW[I], IELV[I], DLY[0][I], FMGC[I], XMGC[I], KLSS[I], PRR[I], CALS[I], ICAL[I], NDATE[I], NHRMN[I]), "(2X,A4,I2,F5.2,A1,I3,F5.2,A1,I4,F6.2," + ""
						+ "T38,F5.2,T45,F5.2,I2,1X,F4.2,1X,F6.2,I2,T71,I6,I4)");
			} else {
				// Integration code goes here
				results.addStationToSummaryList(new Station((char) I, NSTA[I], (int) LAT1, LAT2, INS[I], (int) LON1, LON2, IEW[I], IELV[I], MNO[I], DLY[0][I], DLY[1][I], XMGC[I], FMGC[I], KLSS[I], CALS[I], ICAL[I], NDATE[I], NHRMN[I]));

				write("FPRINT_WRITER", data(I, NSTA[I], LAT1, "-", LAT2, INS[I], LON1, "-", LON2, IEW[I], IELV[I], MNO[I], DLY[0][I], DLY[1][I], XMGC[I], FMGC[I], KLSS[I], CALS[I], ICAL[I], NDATE[I], NHRMN[I]), "(I5,2X,A4,I3," + "" + "A,F5.2,A1,I4,A,"
						+ "F5.2,A1,I5,I6,2F6.2,2F6.2,I2,F6.2,I2,2X,I6,I4)");

				write("FPUNCH_WRITER", data(NSTA[I], LAT1, "-", LAT2, INS[I], LON1, "-", LON2, IEW[I], IELV[I], MNO[I], DLY[0][I], DLY[1][I], XMGC[I], FMGC[I], KLSS[I], CALS[I], ICAL[I], NDATE[I], NHRMN[I]), "(A4,I3,A,F5.2," + "" + "A1,I3,A,F5.2,A1,I4,"
						+ "I6,2F6.2,2F6.2,I2,F6.2,I2,2X,I6,I4)");
			}
		}// 220 CONTINUE
			// RETURN
			// END
	}

	/**
	 * Returns array column with index i from jBeg to jEnd row
	 * 
	 * @param array
	 * @param i
	 * @param jBeg
	 * @param jEnd
	 * @return
	 */
	public String getColumn(char[][] array, int i, int jBeg, int jEnd) {
		char[] column = new char[jEnd - jBeg + 1];
		i--;
		for (int j = jBeg - 1; j < jEnd; j++) {
			column[j] = array[j][i];
		}
		return new String(column);
	}

	/*
	 * SUBROUTINE FMPLOT(KPAPER,KFM,FNO,NRP,AZ,AIN,SYM,SUCARD)
	 */
	public void FMPLOT(final int KPAPER, final int KFM, final double FNO, final double[] AZ, final double[] AIN, final char[] SYM, final String SUCARD) throws IOException, ParseException {

		// CHARACTER*1 GRAPH(107,59),SYM(101),TEMP
		final char[][] GRAPH = new char[107][59];
		// final char[] SYM = new char[101];
		char TEMP;

		// CHARACTER*2 K0
		String K0;

		// CHARACTER*4 K180
		String K180;

		// CHARACTER*80 SUCARD
		// final String SUCARD;

		// REAL*4 SE(4),AZ(101),AIN(101)
		final double[] SE = new double[4];
		// final double[] AZ = new double[101];
		// final double[] AIN = new double[101];

		// DATA NOY,IX,IY,NOY1,NOX2,NOY2/59,39,24,57,48,30/
		final int NOY = 59;
		final int IX = 39;
		final int IY = 24;
		final int NOY1 = 57;
		final int NOX2 = 48;
		final int NOY2 = 30;

		// DATA RMAX,XSCALE,YSCALE,ADD/3.937008,0.101064,0.169643,4.75/
		final double RMAX = 3.937008;
		final double XSCALE = 0.101064;
		final double YSCALE = 0.169643;
		final double ADD = 4.75;

		int NOX = 95;
		double XPAPER = 1.0;
		K0 = "0 ";
		K180 = "180 ";
		if (KPAPER != 0) {
			NOX = 107;
			XPAPER = 1.125;
			K0 = " 0";
			K180 = " 180";
		}

		int NFMR = 0; // 100

		final double NO = FNO;
		for (int I = 0; I < O1.NRP; I++) { // 1
			if (SYM[I] == 'N') {
				SYM[I] = ' ';
			}
			if (SYM[I] == ' ') {
				continue;
			}
			if (SYM[I] == 'U') {
				SYM[I] = 'C';
			}
			NFMR = NFMR + 1;
		} // 1 CONTINUE

		if (NFMR < KFM) {
			return;// final RETURN
		}

		write("FPRINT_WRITER", "  DATE    ORIGIN     LAT      LONG     DEPTH    MAG NO GAP DMIN  RMS  ERH  ERZ QM");
		write("FPRINT_WRITER", data(SUCARD), "(2X,A80)");

		for (int I = 0; I < NOX; I++) {
			for (int J = 0; J < NOY; J++) {
				GRAPH[I][J] = ' '; // 10 GRAPH(I,J)=' '
			}
		}
		for (int I = 0; I < 180; I++) {
			final double RI = I * 0.0349066;
			final double X = (RMAX * Math.cos(RI) + ADD);
			final double Y = (RMAX * Math.sin(RI) + ADD);
			int JX = (int) (X / XSCALE + 1.5);
			JX = (int) (XPAPER * JX + 0.5);
			int JY = (int) (Y / YSCALE + .5);
			JY = NOY - JY - 1;
			GRAPH[JX - 1][JY - 1] = '*';
		}// 20

		int IT = NOX2 - IX - 1;
		IT = (int) (XPAPER * IT + 0.5);
		GRAPH[IT - 1][NOY2 - 1] = '-';
		IT = NOX2 + IX + 1;
		IT = (int) (XPAPER * IT + 0.5);
		GRAPH[IT - 1][NOY2 - 1] = '-';

		int JX = (int) (XPAPER * NOX2 + 0.5);
		IT = NOY2 - IY - 1;
		GRAPH[JX - 1][IT - 1] = 'I';

		IT = NOY2 + IY + 1;
		GRAPH[JX - 1][IT - 1] = 'I';

		for (int I = 0; I < O1.NRP; I++) { // DO 50 I=1,NRP

			if (SYM[I] == ' ') {
				continue;
			}
			double ANN;
			double AZZ;
			if (AIN[I] <= 90) {
				ANN = AIN[I];
				AZZ = (AZ[I] * .0174533);
			} else {
				ANN = (180. - AIN[I]);// 31
				AZZ = ((180. + AZ[I]) * .0174533);
			}
			final double R = RMAX * 1.414214 * Math.sin(ANN * .0087266);// 32
			final double X = (R * Math.sin(AZZ) + ADD);
			final double Y = (R * Math.cos(AZZ) + ADD);
			JX = (int) (X / XSCALE + 1.5);
			JX = (int) (XPAPER * JX + 0.5);
			int JY = (int) (Y / YSCALE + .5);
			JY = NOY - JY - 1;
			TEMP = GRAPH[JX - 1][JY - 1];
			if (!(TEMP == ' ' || TEMP == '*' || TEMP == '+' || TEMP == '-' || TEMP == '.')) {
				// C-----TEMP IS OCCUPIED SO IF SYS[I]=+ OR - SKIP THIS STATION
				if (SYM[I] == '+' || SYM[I] == '-') {
					continue;
				}
				if (SYM[I] != 'C') {
					if (GRAPH[JX - 1][JY - 1] == 'D') {
						GRAPH[JX - 1][JY - 1] = 'E';
						continue;
					}
					// 35
					if (GRAPH[JX - 1][JY - 1] == 'E') {
						GRAPH[JX - 1][JY - 1] = 'F';
						continue;
					}
					// 37
					if (GRAPH[JX - 1][JY - 1] == 'F') {
						continue;
					}
					GRAPH[JX - 1][JY - 1] = 'X';
					continue;
				}
				// 40
				if (GRAPH[JX - 1][JY - 1] == 'C') {
					GRAPH[JX - 1][JY - 1] = 'B';
					continue;
				}
				// 43
				if (GRAPH[JX - 1][JY - 1] == 'B') {
					GRAPH[JX - 1][JY - 1] = 'A';
					continue;
				}
				// 45
				if (GRAPH[JX - 1][JY - 1] == 'A') {
					continue;
				}
				GRAPH[JX - 1][JY - 1] = 'X';
				continue;
			}
			// 47
			GRAPH[JX - 1][JY - 1] = SYM[I];
		}// 50 CONTINUE

		// C*** SCALE JX FOR DIFFERENT PRINTER PAPER SIZES
		JX = (int) (XPAPER * NOX2 + 0.5);
		GRAPH[JX - 1][NOY2 - 1] = '*';
		write("FPRINT_WRITER", data(' ', K0), "(A,67X,A2)");

		for (int I = 2; I < NOY1; I++) { // DO 80 I=3,NOY1
			if (I != NOY2 - 1) {
				if (KPAPER == 0) {
					write("FPRINT_WRITER", data(' ', getColumn(GRAPH, I, 1, NOX)), "(A ,20X,A95)");
				}
				if (KPAPER == 1) {
					write("FPRINT_WRITER", data(' ', getColumn(GRAPH, I, 1, NOX)), "(A ,15X,A107)");
				}
				continue;
			}
			if (KPAPER == 0) {// 70
				write("FPRINT_WRITER", data(' ', "270 ", getColumn(GRAPH, I, 1, NOX), " 90"), "(A ,16X,A,A95,A)");
			}
			if (KPAPER == 1) {
				write("FPRINT_WRITER", data(' ', "270 ", getColumn(GRAPH, I, 1, NOX), " 90"), "(A ,11X,A,A107,A)");
			}
		}// 80 CONTINUE

		write("FPRINT_WRITER", data(K180), "(67X,A4)");
	}

	/*
	 * SUBROUTINE SINGLE(TEST,KNO,IW,NSTA,INS,IEW,DLY,FMGC, &
	 * XMGC,KLAS,PRR,CALR,ICAL,LAT,LON,V,D,DEPTH,VSQ,THK,H,G,F, &
	 * TID,DID,FLT,QSPA,MSTA,PRMK,W,JMIN,P,S,SRMK,WS,AMX,PRX,CALX,RMK, &
	 * DT,FMP,AZRES,SYM,QRMK,KDX,LDX,JDX,TP,WRK,KSMP,TS,TIME1,TIME2, &
	 * AVXM,AVFM,XMAG,FMAG,NRES,SR,SRSQ,SRWT,QNO,MNO)
	 */
	public void SINGLE(boolean singmd) throws IOException, ParseException {

		// CHARACTER*4 ISW,IPRO,CHECK,NSTA(151)
		String CHECK = null;

		// CHARACTER*48 AHEAD

		// INTEGER*4 ISKP(4),LA(10),LO(10)
		final int[] ISKP = new int[4];
		int[] LA = new int[10];
		int[] LO = new int[10];
		int[] KEY = new int[101];

		// REAL*4 LATRT,LONRT,LATSV,LONSV
		double LATRT;
		double LONRT = 0;
		double LATSV = 0;
		double LONSV = 0;

		// REAL*4 LAT2,LON2,LATEP,LONEP,MAG,LATR,LONR
		double LATR = 0, LONR = 0;

		// REAL*4 QNO(4),XMEAN(4),SUM(5),WF(41),ALZ(10)
		final double[] XMEAN = new double[4];
		final double[] SUM = new double[5];
		double[] WF = new double[41];
		double[] ALZ = new double[10];

		// REAL*4 AF(3),B(4),Y(4),SE(4),TEST(15),X(4,101),QSPA(9,40)
		final double[] AF = new double[3];
		final double[] B = new double[4];
		final double[] SE = new double[4];
		final double[][] X = new double[4][101];

		final double[] TEMP = new double[101];

		// REAL*4 DELTA(101),DX(101),DY(101),ANIN(101),AIN(101),AZ(101)
		final double[] DELTA = new double[101];
		final double[] DX = new double[101];
		final double[] DY = new double[101];
		final double[] ANIN = new double[101];
		final double[] AIN = new double[101];
		final double[] AZ = new double[101];

		// REAL*4 WT(101),T(101),P(101),TP(101),DT(101),S(101),TS(101)
		final double[] WT = new double[101];
		final double[] T = new double[101];
		double RMSSV = 0;

		int NA = 0;

		double DELAT = 0;
		double DELON = 0;
		double DEZ = 0;
		double ZSV = 0;

		double PHI = 0;
		double SINPHI = 0;
		double SINP2 = 0;
		double SINP4 = 0;
		double CA = 0;
		double CB = 0;
		int K = 0;
		double ERLMT = 0.;
		double FLTEP = 0;
		double PRMSSQ = 0;
		double NIMAX = 0d;
		double SVY1 = 0.0;
		double SVY2 = 0.0;
		double SVY3 = 0.0;

		// DATA WF/.95,0.95,0.95,0.95,0.95,0.95,0.94,0.94,0.94,0.93,
		// 1 0.92,0.92,0.91,0.90,0.88,0.87,0.85,0.83,0.80,0.77,
		// 2 0.73,0.69,0.64,0.59,0.53,0.47,0.41,0.34,0.28,0.23,
		// 3 0.18,0.14,0.11,0.08,0.06,0.04,0.03,0.02,0.01,0.01,0./

		WF = new double[] { .95, 0.95, 0.95, 0.95, 0.95, 0.95, 0.94, 0.94, 0.94, 0.93, 0.92, 0.92, 0.91, 0.90, 0.88, 0.87, 0.85, 0.83, 0.80, 0.77, 0.73, 0.69, 0.64, 0.59, 0.53, 0.47, 0.41, 0.34, 0.28, 0.23, 0.18, 0.14, 0.11, 0.08, 0.06, 0.04, 0.03, 0.02, 0.01, 0.01, 0 };

		// DATA LA/1,1,1,1,0,0,-1,-1,-1,-1/,
		// 1 LO/+1,-1,+1,-1,0,0,+1,-1,+1,-1/,
		// 2 ALZ/-1.0,-1.0,+1.0,+1.0,-1.732,+1.732,-1.0,-1.0,+1.0,+1.0/
		LA = new int[] { 1, 1, 1, 1, 0, 0, -1, -1, -1, -1 };
		LO = new int[] { +1, -1, +1, -1, 0, 0, +1, -1, +1, -1 };
		ALZ = new double[] { -1.0, -1.0, +1.0, +1.0, -1.732, +1.732, -1.0, -1.0, +1.0, +1.0 };

		O2.AVRPS = 0.0;
		C4.IEXIT = 0;
		LATRT = 0;
		double ZRES = P[O1.NR];
		O1.KNST = JMIN[O1.NR] / 10;
		O1.INST = JMIN[O1.NR] - O1.KNST * 10;

		// Here

		O1.NRP = O1.NR;
		boolean goto96 = false;
		boolean goto111 = false;
		boolean goto110 = false;
		boolean goto575 = false;
		while (true) {
			if (!goto111 && !goto110) {
				if (C4.IDXS != 0) {// 30
					// C------- TREAT S DATA BY AUGMENTING P DATA;
					int NOS = 0;
					for (int I = 0; I < O1.NRP; I++) {// DO 65 I=1,O1.NRP;
						if (LDX[I] == 0) {
							continue;
						}
						NOS = NOS + 1;
						final int NRS = O1.NRP + NOS;
						TP[NRS - 1] = TS[I];
						W[NRS - 1] = WS[I];
						KSMP[NRS - 1] = 0;
						if (O1.KNST != 1 && O1.KNST != 6) {
							W[NRS - 1] = 0;
						}
						KDX[NRS - 1] = KDX[I];
						LDX[I] = NRS;
						WRK[NRS - 1] = "    ";
					}// 65 CONTINUE;

					O1.NR = O1.NRP + NOS;
				}
				// C------- INITIALIZE TRIAL HYPOCENTER;
				// -----------------------------------;
				K = KDX[C4.NEAR - 1];// 80
				for (int I = 0; I < 3; I++) {// DO 25 I = 1,3;
					ISKP[I] = 0;
				}// 25 CONTINUE;

				goto575 = false;
				if (O1.INST == 9) {
					double ORG1 = 0;
					double ORG2 = 0;

					if (readFromFile) {
						final ArrayList<Object> ar = read(FINPUT_READER, "(F5.0,F5.2,I5,F5.2,I5,2F5.2)");
						ORG1 = ((Double) (ar.get(0) == null ? 0d : ar.get(0)));
						ORG2 = ((Double) (ar.get(1) == null ? 0d : ar.get(1)));
						LAT1 = (Integer) (ar.get(2) != null ? ar.get(2) : 0);
						LAT2 = ((Double) (ar.get(3) != null ? ar.get(3) : 0d));
						LON1 = (Integer) (ar.get(4) != null ? ar.get(4) : 0);
						LON2 = ((Double) (ar.get(5) != null ? ar.get(5) : 0d));
						O2.Z = ((Double) (ar.get(6) != null ? ar.get(6) : 0d));
					}
					/*
					 * final ArrayList<Object> ar = read(FINPUT_READER,
					 * "(F5.0,F5.2,I5,F5.2,I5,2F5.2)"); ORG1 = ((Double)
					 * (ar.get(0) == null ? 0d : ar.get(0))); ORG2 = ((Double)
					 * (ar.get(1) == null ? 0d : ar.get(1))); LAT1 = (Integer)
					 * (ar.get(2) != null ? ar.get(2) : 0); LAT2 = ((Double)
					 * (ar.get(3) != null ? ar.get(3) : 0d)); LON1 = (Integer)
					 * (ar.get(4) != null ? ar.get(4) : 0); LON2 = ((Double)
					 * (ar.get(5) != null ? ar.get(5) : 0d)); O2.Z = ((Double)
					 * (ar.get(6) != null ? ar.get(6) : 0d));
					 */
					// TODO: The list of values need to be defined here (org1,
					// org2, lat1, lat2, lon1, lon2, z - depth)
					// READ(4,85) ORG1,ORG2,LAT1,LAT2,LON1,LON2,Z;
					// 85 FORMAT(F5.0,F5.2,I5,F5.2,I5,2F5.2);

					O2.ORG = 60 * ORG1 + ORG2;

					O2.LATEP = 60 * LAT1 + LAT2;
					O2.LONEP = 60 * LON1 + LON2;

				}
				if (O1.INST != 9 || goto96) {
					if (goto96 || O1.NR < 3) {// 90
						goto96 = false;
						write("FPRINT_WRITER", data(" ***** INSUFFICIENT DATA FOR LOCATING THIS QUAKE:"), "(///,A)");

						O1.KKF = 1;
						if (O1.NRP == 0) {
							O1.NRP = 1;
						}
						for (int L = 0; L < O1.NRP; L++) { // DO 98 L=1,O1.NRP;
							write("FPRINT_WRITER", data(MSTA[L], PRMK[L], C4.KDATE, C4.KHR, JMIN[L], P[L], S[L]), "(5X,2A4,1X,I6,2I2,F5.2,7X,F5.2)");
						}
						C4.IEXIT = 1;
						if (O1.NRP == 1) {
							return;
						}
						goto575 = true;
					}
					if (!goto575) {
						O2.Z = C2.ZTR;// 100
						if (!AZRES[O1.NRP + 1 - 1].equals("")) {
							O2.Z = ZRES;
						}
						O2.ORG = C5.PMIN - O2.Z / 5 - 1;
						if (LATRT != 0) {
							O2.LATEP = LATRT;
							O2.LONEP = LONRT;
						} else {
							if (LATR != 0.) {// 102
								O2.LATEP = LATR;
								O2.LONEP = LONR;
							} else {
								O2.LATEP = (LAT[K - 1] + 0.1);// 104
								O2.LONEP = (LON[K - 1] + 0.1);
							}
						}
					}
				}
			}
			if (!goto575 || goto111 || goto110) {
				if (!goto111 && !goto110) {
					O2.ADJSQ = 0;// 105
					O1.IPH = 0;
					O1.NDEC = 0;
					PRMSSQ = 100000;
					if (ISW.equals("1    ")) {
						KNO = MNO[K - 1];
					}
					if (ISW.equals("1    ")) {
						FLTEP = FLT[KNO - 1][K - 1];
					}
					NIMAX = TEST[10] + .0001;
					// C------- GEIGER'S ITERATION TO FIND HYPOCENTRAL
					// ADJUSTMENTS;
					O1.NI = 1;
					if (O1.INST == 9) {
						O1.NI = (int) NIMAX;
					}
				}
				goto111 = false;
				if (!goto110) {
					if (ERLMT != 0) {// 111
						O2.LATEP = LATSV + LA[NA - 1] * DELAT;
						O2.LONEP = LONSV + LO[NA - 1] * DELON;

						O2.Z = ZSV + ALZ[NA - 1] * DEZ;
						if (O2.Z < 0) {
							O2.Z = 0;
						}
					}
				}
				goto110 = false;
				double FMO = 0;// 110
				double FNO = 0;
				double DELMIN = 0;
				if (singmd) {
					DELMIN = 99999.f;
				}
				for (int I = 0; I < 5; I++) {// DO 112 I=1,5;
					SUM[I] = 0;// 112
				}
				int JI = 0;
				// C------- CALCULATE EPICENTRAL DISTANCE BY RICHTER'S METHOD;
				for (int I = 0; I < O1.NR; I++) {// DO 120 I=1,O1.NR;
					JI = KDX[I];
					PHI = 0.0174532 * ((LAT[JI - 1] + O2.LATEP) / 120);
					SINPHI = Math.sin(PHI);
					SINP2 = SINPHI * SINPHI;
					SINP4 = SINP2 * SINP2;
					CA = 1.8553654 + 0.0062792 * SINP2 + 0.0000319 * SINP4;
					CB = 1.8428071 + 0.0187098 * SINP2 + 0.0001583 * SINP4;
					DX[I] = ((LON[JI - 1] - O2.LONEP) * CA * Math.cos(PHI));
					DY[I] = ((LAT[JI - 1] - O2.LATEP) * CB);
					DELTA[I] = (Math.sqrt(DX[I] * DX[I] + DY[I] * DY[I]) + 0.000001);
					WT[I] = W[I];
					if (!singmd) {
						if (O1.NI > 1) {
							// C------- DISTANCE WEIGHTING;
							if (DELTA[I] > C2.XNEAR) {
								WT[I] = W[I] * (C2.XFAR - DELTA[I]) / C5.XFN;
								if (WT[I] < 0.005) {
									WT[I] = 0;
								}
							}
						}
					} else {
						DELMIN = Math.min(DELTA[I], DELMIN);
						double YFAR = Math.max(C2.XFAR, 3 * DELMIN);
						WT[I] = W[I];
						if (O1.NI > 3) {
							if (DELTA[I] > C2.XNEAR) {
								WT[I] = W[I] * (YFAR - DELTA[I]) / (YFAR - C2.XNEAR);
							}
							if (WT[I] < 0.005) {
								WT[I] = 0;
							}
						}
					}
					if (WT[I] != 0.) {// 115
						if (KSMP[I] == 1) {
							FMO = FMO + 1;
						}
						FNO = FNO + 1;
						SUM[3] = SUM[3] + WT[I];
					}
				}// 120 CONTINUE;

				if (FNO < 3) {
					goto96 = true;
					continue;
				}
				double AVWT = SUM[3] / FNO;
				// C------- NORMALIZE DISTANCE WEIGHTS;
				SUM[3] = 0.0;
				for (int I = 0; I < O1.NR; I++) {// DO 122 I=1,O1.NR;
					WT[I] = WT[I] / AVWT;// 122
				}
				if (!(O1.NI <= 2 || C1.KAZ == 0)) {
					// C------- AZIMUTHAL WEIGHTING;
					// CALL AZWTOS(DX,DY,O1.NR,WT,KDX,AZ,TEMP,KEY,INS,IEW);
					// C------- COMPUTE TRAVEL TIMES & DERIVATIVES;
					AZWTOS(DX, DY, WT, KDX, AZ, TEMP, KEY, INS, IEW);
				}

				O2.ZSQ = O2.Z * O2.Z;// 130

				// CALL TRVDRV(ISW,V,D,DEPTH,VSQ,NL,THK,H,G,F,TID,DID,FLT,;
				// & DELTA,DX,DY,O1.NR,KDX,KNO,FLTEP,Z,ZSQ,X,T,ANIN);
				TRVDRV(ISW, V, D, DEPTH, VSQ, THK, H, G, F, TID, DID, FLT, DELTA, DX, DY, KDX, FLTEP, X, T, ANIN);

				double FDLY = 1;
				if (ISW == "1   ") {
					FDLY = 0;
				}
				// C------- CALCULATE TRAVEL TIME RESIDUALS X(4,I) & MODIFY THE
				// DERIV'S;
				// ---;
				for (int I = 0; I < O1.NR; I++) {// DO 150 I=1,O1.NR;
					JI = KDX[I];
					if (I > O1.NRP - 1) {
						// C------- S PHASE DATA;
						// --------------------------------------------------;
						T[I] = C2.POS * T[I];
						X[0][I] = C2.POS * X[0][I];
						X[1][I] = C2.POS * X[1][I];
						X[2][I] = C2.POS * X[2][I];
						X[3][I] = TP[I] - T[I] - O2.ORG - C2.POS * DLY[KNO - 1][JI - 1] * FDLY;
					} else {
						if (KSMP[I] != 0) {// 145
							// C------- S-P DATA;
							// ------------------------------------------------------;
							X[0][I] = (C2.POS - 1) * X[0][I];
							X[1][I] = (C2.POS - 1) * X[1][I];
							X[2][I] = (C2.POS - 1) * X[2][I];
							X[3][I] = TS[I] - TP[I] - (C2.POS - 1) * (DLY[KNO - 1][JI - 1] * FDLY + T[I]);
						} else {
							// C------- P TRAVEL TIME RESIDUAL;
							// ----------------------------------------;
							X[3][I] = TP[I] - T[I] - O2.ORG - DLY[KNO - 1][JI - 1] * FDLY; // 146
						}
					}
				}// 150 CONTINUE;

				// C------- COMPUTE AVR, AAR, RMSSQ, & SDR;
				// --------------------------------;
				C2.ONF = 0.0;
				double XWT = 0.0;
				for (int I = 0; I < O1.NR; I++) {// DO 152 I=1,O1.NR;
					C2.ONF = C2.ONF + WT[I] * (1 - KSMP[I]);
					XWT = X[3][I] * WT[I];
					SUM[0] = SUM[0] + XWT;
					SUM[1] = SUM[1] + Math.abs(XWT);
					SUM[2] = SUM[2] + X[3][I] * XWT;
					SUM[4] = SUM[4] + XWT * (1 - KSMP[I]);
				}// 152 CONTINUE;

				if (FNO > FMO) {
					O2.AVRPS = SUM[4] / C2.ONF;
				}
				O2.AVR = SUM[0] / FNO;
				O2.AAR = SUM[1] / FNO;
				O2.RMSSQ = SUM[2] / FNO;
				if (singmd) {
					O2.RMSSQ = SUM[2] / Math.max(1, FNO - 4);
				}
				final double SDR = sqrt(Math.abs(O2.RMSSQ - O2.AVR * O2.AVR));
				for (int I = 0; I < 5; I++) {// DO 153 I=1,5;
					SUM[I] = 0.0;
				}// 153 CONTINUE;

				boolean goto167 = false;
				boolean goto501 = false;
				boolean goto502 = false;
				boolean goto165 = false;
				if ((O2.RMSSQ < TEST[0] && !singmd) || (singmd && (O2.RMSSQ < TEST[0] || O1.NI <= 3))) {
					if (ERLMT == 1) {
						goto167 = true;
					} else {
						if (O1.INST == 9) {
							goto501 = true;
						} else {
							if (O1.NI >= 2) {
								goto167 = true;
							}
							goto165 = true;
						}
					}
				}
				boolean goto169 = false;
				if (!goto167) {
					if (!goto165) {
						// JEFFREYS' WEIGHTING;
						FMO = 0;// 154
						FNO = 0;
						for (int I = 0; I < O1.NR; I++) { // DO 160 I=1,O1.NR;
							WRK[I] = "    ";
							if (WT[I] == 0) {
								continue;
							}
							K = (int) (10 * Math.abs(X[3][I] - O2.AVR) / SDR + 1.5);
							if (K > 41) {
								K = 41;
							}
							WT[I] = WT[I] * WF[K - 1];
							if (K > 30) {
								WRK[I] = "****";
							}
							if (WT[I] < 0.005) {
								WT[I] = 0;
							}
							if (WT[I] == 0.) {
								continue;
							}
							if (KSMP[I] == 1) {
								FMO = FMO + 1;
							}
							FNO = FNO + 1;
							SUM[3] = SUM[3] + WT[I];
						}// 160 CONTINUE;

						if (FNO < 3) {
							goto96 = true;
							continue;
						}
						AVWT = SUM[3] / FNO;
						SUM[3] = 0.0;
						C2.ONF = 0.0;
						for (int I = 0; I < O1.NR; I++) {// DO 164 I=1,O1.NR;
							WT[I] = WT[I] / AVWT;
							C2.ONF = C2.ONF + WT[I] * (1 - KSMP[I]);
							XWT = X[3][I] * WT[I];
							SUM[4] = SUM[4] + XWT * (1 - KSMP[I]);
						}// 164 CONTINUE;

						// C------- RECALCULATE O2.AVRPS;
						// ---------------------------------------------;
						if (!(ERLMT == 1 || O1.INST != 9)) {
							O2.AVRPS = 0.0;
							if (FNO != FMO) {
								O2.AVRPS = SUM[4] / C2.ONF;
							}

							goto501 = true;
						}
						if (!goto501) {
							if (FNO == FMO) {// 163
								O2.AVRPS = 0.0;
							}
							if (FNO == FMO) {
								goto167 = true;
							} else {
								O2.AVRPS = SUM[4] / C2.ONF;
								SUM[4] = 0.0;
								if (ERLMT == 1) {
									goto167 = true;
								} else {
									// C------- RESET FIRST ORIGIN TIME;
									// ---------------------------------------;
									if (O1.NI >= 2) {
										goto167 = true;
									}
								}
							}
						}
					}
					if (!goto501) {
						if (!goto167) {
							O2.ORG = O2.ORG + O2.AVRPS;// 165
							for (int I = 0; I < O1.NR; I++) {// DO 166
																// I=1,O1.NR;
								if (KSMP[I] == 0) {
									X[3][I] = X[3][I] - O2.AVRPS;
								}
								XWT = WT[I] * X[3][I];
								SUM[4] = SUM[4] + XWT * (1 - KSMP[I]);
								SUM[1] = SUM[1] + Math.abs(XWT);
								SUM[2] = SUM[2] + X[3][I] * XWT;
							}// 166 CONTINUE;

							if (FNO > FMO) {
								O2.AVRPS = SUM[4] / C2.ONF;
							}
							O2.AAR = SUM[1] / FNO;
							if (!singmd) {
								O2.RMSSQ = SUM[2] / FNO;
							} else {
								O2.RMSSQ = SUM[2] / Math.max(1, FNO - 4);
							}
							goto169 = true;

							// C------- FOR NI>1, COMPUTE AAR, & RMSSQ AS
							// ifAVRPS=0.;
							// -----------------;
						}
					}

				}
				boolean goto550 = false;
				if (!goto501) {
					if (!goto169) {
						for (int I = 0; I < O1.NR; I++) {// 167 DO 168
															// I=1,O1.NR;
							XWT = WT[I] * (X[3][I] - O2.AVRPS * (1 - KSMP[I]));
							SUM[1] = SUM[1] + Math.abs(XWT);
							SUM[2] = SUM[2] + (X[3][I] - O2.AVRPS * (1 - KSMP[I])) * XWT;
						}// 168 CONTINUE;

						O2.AAR = SUM[1] / FNO;
						if (!singmd) {
							O2.RMSSQ = SUM[2] / FNO;
						} else {
							O2.RMSSQ = SUM[2] / Math.max(1, FNO - 4);
						}
						if (ERLMT != 0) {
							// C------- OUTPUT RMS ERROR OF AUXILIARY POINTS;
							// --------------------------;

							double L = O2.LATEP / 60;
							final double ALA = O2.LATEP - 60. * L;
							L = O2.LONEP / 60;
							final double ALO = O2.LONEP - 60 * L;
							final double RMSX = sqrt(O2.RMSSQ);

							final double DRMS = RMSX - RMSSV;
							// go to (1,2,3,4,5,6,1,2,3,4), NA;
							switch (NA) {
							case 1:
							case 7:
								write("FPUNCH_WRITER", data(ALA, ALO, O2.Z, O2.AVRPS, RMSX, DRMS), "(5F10.2,10X,F6.2)");
								break;
							case 2:
							case 8:
								write("FPUNCH_WRITER", data(ALA, ALO, O2.Z, O2.AVRPS, RMSX, DRMS), "(5F10.2,28X,F6.2)");
								break;
							case 3:
							case 9:
								write("FPUNCH_WRITER", data(ALA, ALO, O2.Z, O2.AVRPS, RMSX, "(", DRMS, ")"), "(5F10.2,13X,A,F6.2,A)");
								break;
							case 4:
							case 10:
								write("FPUNCH_WRITER", data(ALA, ALO, O2.Z, O2.AVRPS, RMSX, "(", DRMS, ")"), "(5F10.2,31X,A,F6.2,A)");
								// IF(NA == 10)
								goto550 = true;
								break;
							case 5:
								write("FPUNCH_WRITER", data(ALA, ALO, O2.Z, O2.AVRPS, RMSX, DRMS), "(/5F10.2,19X,F6.2)");
								write("FPUNCH_WRITER", data(RMSSV, "0.00"), "(40X,F10.2,23X,A)");
								break;
							case 6:
								write("FPUNCH_WRITER", data(ALA, ALO, O2.Z, O2.AVRPS, RMSX, "(", DRMS, ")"), "(5F10.2,22X,A,F6.2,A)");
							}

							if (!goto550) {
								NA = NA + 1; // 174
								goto111 = true;
								continue;
							}
						}
					}
					if (!goto550) {
						// C------- CHECK ifSOLUTION IS BETTER THAN PREVIOUS
						// ONE;
						boolean goto170 = false;
						boolean goto325 = false;

						if ((O1.NI == 1 && O1.NDEC == 0) || PRMSSQ >= O2.RMSSQ) { // 169
							goto170 = true;
						}

						double XADJSQ = 0;
						if (!goto170) {
							O1.NDEC = O1.NDEC + 1;
							if (O1.NDEC <= 1) {
								for (int I = 0; I < 3; I++) {// DO 177 I= 1,3;
									B[I] = 0.0;
									AF[I] = -1.0;
									SE[I] = 0.0;
								}// 177 CONTINUE;

								O1.NI = O1.NI - 1;
								final double BM1 = Y[0];
								final double BM2 = Y[1];
								final double BM3 = Y[2];
								double BMAX = Math.abs(Y[0]);
								int IIMAX = 1;
								for (int I = 1; I < 3; I++) {// DO 176 I = 2,3;
									if (Math.abs(Y[I]) <= BMAX) {
										continue;
									}
									BMAX = Math.abs(Y[I]);
									IIMAX = I;
								}// 176 CONTINUE;

								ISKP[IIMAX] = 1;
								Y[0] = -BM1 / 5;
								Y[1] = -BM2 / 5;
								Y[2] = -BM3 / 5;
								Y[3] = -Y[0] * XMEAN[0] - Y[1] * XMEAN[1] - Y[2] * XMEAN[2];
								XADJSQ = Y[0] * Y[0] + Y[1] * Y[1] + Y[2] * Y[2];
								O1.KP = 0;
								if (XADJSQ < 4 * TEST[3] / 25) {
									goto170 = true;
								}
							}
							if (O1.NDEC == 5) {
								goto170 = true;
							}
							if (!goto170) {
								goto325 = true;
							}

							// STEPWISE MULTIPLE REGRESSION ANALYSIS OF TRAVEL
							// TIME RESIDUALS;
						}
						if (!goto325) {
							if (O1.NDEC >= 1) {// 170
								O1.NI = O1.NI + 1;
							}
							boolean goto300 = false;
							if (!(O1.INST == 1 || ISKP[2] == 1)) {
								if (O1.INST == 9) {
									goto501 = true;
								}
								if (!goto501) {
									if (!(FNO == 3 && FMO < 3)) {
										// C---- FREE SOLUTION;
										O1.KZ = 0;
										O1.KF = 0;

										SWMREG(FNO, X, WT, ISKP, XMEAN, B, SE, AF);
										// CALL
										// SWMREG(TEST,IPRN,O1.NR,KSMP,FNO,X,WT,ISKP,KF,KZ,XMEAN,;
										// & B,Y,SE,AF,ONF,FLIM);

										// AVOID CORRECTING DEPTH ifHORIZONTAL
										// CHANGE IS LARGE;

										if (Y[0] * Y[0] + Y[1] * Y[1] < TEST[1]) {
											goto300 = true;
										}
									}
									if (!goto300) {
										// C---- FIXED DEPTH SOLUTION;
										O1.KZ = 1;// 250
										O1.KF = 0;

										SWMREG(FNO, X, WT, ISKP, XMEAN, B, SE, AF);
										// CALL
										// SWMREG(TEST,IPRN,O1.NR,KSMP,FNO,X,WT,ISKP,KF,KZ,XMEAN,;
										// & B,Y,SE,AF,ONF,FLIM);

										// LIMIT FOCAL DEPTH CHANGE & AVOID
										// HYPOCENTER IN THE AIR;

									}
									int I;
									for (I = 0; I < 3; I++) {// 300 DO 275 I=
																// 1,3;
										ISKP[I] = 0;
									}// 275 CONTINUE;

									I++;
									final double OLDY1 = Y[0];
									final double OLDY2 = Y[1];
									final double OLDY3 = Y[2];
									final double ABSY1 = abs(Y[0]);
									final double ABSY2 = abs(Y[1]);
									final double ABSY3 = abs(Y[2]);
									double ABSGR;
									if (ABSY1 <= ABSY2) {
										ABSGR = ABSY2;

									} else {
										ABSGR = ABSY1;// 305
									}
									if (ABSY3 > TEST[4]) {// 308
										I = (int) (ABSY3 / TEST[4]);
										Y[2] = Y[2] / (I + 1);
									}
									if (O2.Z + Y[2] <= 0.0) {// 310
										Y[2] = (-O2.Z * TEST[11] + 0.000001);
										ISKP[2] = 1;
										// LIMIT HORIZONTAL ADJUSTMENT OF
										// EPICENTER;
									}
									if (ABSGR > TEST[9]) {// 315
										I = (int) (ABSGR / TEST[9]);
										Y[0] = Y[0] / (I + 1);
										Y[1] = Y[1] / (I + 1);
									}
									Y[3] = Y[3] - (Y[2] - OLDY3) * XMEAN[2] - (Y[0] - OLDY1) * XMEAN[0] - (Y[1] - OLDY2) * XMEAN[1]; // 320
									XADJSQ = Y[0] * Y[0] + Y[1] * Y[1] + Y[2] * Y[2];

									O1.KP = 0;
									O1.NDEC = 0;
									O1.JPH = 0;
								}
							}
							if (!goto501) {
								if (C1.IPRN >= 1) {// 325
									OUTPUT(IW, INS, IEW, DLY, FMGC, XMGC, KLAS, PRR, CALR, ICAL, FLT, QSPA, PRMK, JMIN, P, S, SRMK, AMX, PRX, CALX, RMK, DT, FMP, AZRES, KDX, LDX, WT, TP, T, WRK, TS, TIME1, TIME2, DELTA, DX, DY, AVXM, AVFM, FNO, X, B, SE, AF, AZ, AIN, ANIN, TEMP,
											KEY);
								}

								boolean goto500 = false;
								if (O1.NDEC < 1) {
									// TERMINATE ITERATION ifHYPOCENTER
									// ADJUSTMENT < TEST(4);
									if ((!singmd && XADJSQ < TEST[3]) || (singmd && (XADJSQ < TEST[3] && O1.NI > 4))) {
										goto500 = true;
									}
								}
								if (O1.NI == NIMAX) {
									goto500 = true;
								}
								if (!goto500) {
									// ADJUST HYPOCENTER;
									PHI = 0.0174532 * (O2.LATEP / 60);
									SINPHI = Math.sin(PHI);
									SINP2 = SINPHI * SINPHI;
									SINP4 = SINP2 * SINP2;
									CA = 1.8553654 + 0.0062792 * SINP2 + 0.0000319 * SINP4;
									CB = 1.8428071 + 0.0187098 * SINP2 + 0.0001583 * SINP4;
									O2.LATEP = (O2.LATEP + Y[1] / CB);
									O2.LONEP = (O2.LONEP + Y[0] / (CA * Math.cos(PHI)));
									O2.Z = O2.Z + Y[2];
									O2.ORG = O2.ORG + Y[3];
									SVY1 = Y[0];
									SVY2 = Y[1];
									SVY3 = Y[2];
									O2.ADJSQ = XADJSQ;
									if (O1.NDEC == 0) {
										PRMSSQ = O2.RMSSQ;
									}
									if (O1.NDEC >= 1) {
										goto110 = true;
										continue;
									}
									O1.NI = O1.NI + 1;
									if (O1.NI <= NIMAX) {
										goto111 = true;
										continue;
									}
									// RESET ORIGIN TIME;
								}
								O2.ORG = O2.ORG + XMEAN[3];// 500
								goto502 = true;
							}
						}
					}
					if (goto501 || !goto550 || goto502) {
						if (!goto502) {
							XMEAN[3] = 0.0;// 501
						}

						for (int i = 0; i < 5; i++) {// 502 DO 505 I=1,5;
							SUM[i] = 0.0;// 505
						}

						double SUMM = 0.0;
						for (int I = 0; I < O1.NR; I++) {// DO 510 I=1,O1.NR;
							if (KSMP[I] == 0) {
								X[3][I] = X[3][I] - XMEAN[3];
							}
							if (WT[I] == 0) {
								continue;
							}
							if (O1.INST == 9) {
								double XWTS = WT[I] * (X[3][I] * X[3][I]);
								if (KSMP[I] == 0) {
									final double val = X[3][I] - O2.AVRPS;
									XWTS = WT[I] * (val * val);
								}
								SUMM = SUMM + XWTS;
							}
							XWT = X[3][I] * WT[I];// 509
							SUM[0] = SUM[0] + XWT;
							SUM[1] = SUM[1] + Math.abs(XWT);
							SUM[2] = SUM[2] + X[3][I] * XWT;
							SUM[4] = SUM[4] + XWT * (1 - KSMP[I]);
						}// 510 CONTINUE;

						final double RM9SV = SUMM / FNO;
						O2.AVR = SUM[0] / FNO;
						O2.AVRPS = 0.0;
						if (FNO > FMO) {
							O2.AVRPS = SUM[4] / C2.ONF;
						}
						O2.AAR = SUM[1] / FNO;
						if (!singmd) {
							O2.RMSSQ = SUM[2] / FNO;
						} else {
							O2.RMSSQ = SUM[2] / Math.max(1, FNO - 4);
						}
						// COMPUTE ERROR ESTIMATES BY SOLVING FULL NORMAL
						// EQUATION;
						O1.KF = 2;
						O1.KP = 1;
						O1.KZ = 0;

						SWMREG(FNO, X, WT, ISKP, XMEAN, B, SE, AF);
						// CALL
						// SWMREG(TEST,IPRN,O1.NR,KSMP,FNO,X,WT,ISKP,KF,KZ,XMEAN,;
						// & B,Y,SE,AF,ONF,FLIM);

						for (int I = 0; I < 3; I++) {// DO 521 I =1,3;
							Y[I] = 0.0;// 521
						}
						if (O1.INST == 1) {
							O1.KZ = 1;
						}

						OUTPUT(IW, INS, IEW, DLY, FMGC, XMGC, KLAS, PRR, CALR, ICAL, FLT, QSPA, PRMK, JMIN, P, S, SRMK, AMX, PRX, CALX, RMK, DT, FMP, AZRES, KDX, LDX, WT, TP, T, WRK, TS, TIME1, TIME2, DELTA, DX, DY, AVXM, AVFM, FNO, X, B, SE, AF, AZ, AIN, ANIN, TEMP, KEY);
						// CALL OUTPUT(TEST,KNO,IW,INS,IEW,DLY,FMGC,XMGC,;
						// & KLAS,PRR,CALR,ICAL,FLT,QSPA,MSTA,;
						// &
						// PRMK,JMIN,P,S,SRMK,AMX,PRX,CALX,RMK,DT,FMP,AZRES,QRMK,KDX,LDX,;
						// & WT,TP,T,WRK,KSMP,TS,TIME1,TIME2,DELTA,DX,DY,AVXM,;
						// &
						// XMAG,AVFM,FMAG,MAG,FNO,X,B,Y,SE,AF,AZ,AIN,ANIN,TEMP,KEY);

						if (C1.KMS == 1) {
							MISING(MAG, TEMP);
							// CALL MISING;
							// & (NSTA,LAT,LON,NS,MAG,TEMP,DMIN,JDX,;
							// & JMAX,LATEP,LONEP,INS,IEW);
						}
						if (O1.KNST >= 5 || C1.KFM >= 1) {
							FMPLOT(C1.KPAPER, C1.KFM, FNO, AZ, AIN, SYM, SUCARD);
							// CALL FMPLOT;
							// & (KPAPER,KFM,FNO,O1.NRP,AZ,AIN,SYM,SUCARD);
						}
						QNO[O1.JAV - 1] = QNO[O1.JAV - 1] + 1;
						if (O1.JAV <= C1.IQ) {
							// COMPUTE SUMMARY OF TRAVEL TIME RESIDUALS;

							for (int I = 0; I < O1.NRP; I++) {// DO 522
																// I=1,O1.NRP;
								if (WT[I] == 0. || KSMP[I] == 1) {
									continue;
								}
								JI = KDX[I];
								NRES[KNO - 1][JI - 1] = NRES[KNO - 1][JI - 1] + 1;
								SR[KNO - 1][JI - 1] = SR[KNO - 1][JI - 1] + X[3][I] * WT[I];
								SRSQ[KNO - 1][JI - 1] = SRSQ[KNO - 1][JI - 1] + X[3][I] * X[3][I] * WT[I];
								SRWT[KNO - 1][JI - 1] = SRWT[KNO - 1][JI - 1] + WT[I];
							} // 522 CONTINUE;
						}
						if (C1.KTEST == 1) {// 523
							// COMPUTE RMS AT AUXILIARY POINTS;
							RMSSV = sqrt(O2.RMSSQ);
							if (O1.INST == 9) {
								RMSSV = sqrt(RM9SV);
							}
							ERLMT = 1;
							LATSV = O2.LATEP;
							LONSV = O2.LONEP;
							ZSV = O2.Z;
							PHI = 0.0174532 * (O2.LATEP / 60);
							SINPHI = sin(PHI);
							SINP2 = SINPHI * SINPHI;
							SINP4 = SINP2 * SINP2;
							CA = 1.8553654 + 0.0062792 * SINP2 + 0.0000319 * SINP4;
							CB = 1.8428071 + 0.0187098 * SINP2 + 0.0001583 * SINP4;
							DELAT = TEST[12] / CB;
							DELON = TEST[12] / (CA * cos(PHI));
							DEZ = TEST[12];

							write("FPUNCH_WRITER", data(" LAT LON Z O2.AVRPS RMS DRMS"), "(/,A,/)");
							NA = 1;
							goto111 = true;
							continue;
						}
					}

					TIME1 = TIME2;// 550

					// 575 CONTINUE;
				}
			}

			// CHECK FOR MULTIPLE SOLUTIONS OF THE SAME EARTHQUAKE;
			if (IPRO != " ** ") {
				return;
			}
			O1.NR = O1.NRP;
			final int NRP1 = O1.NR + 1;

			final ArrayList<Object> ar = read(FINPUT_READER, "(2A4,9X,2I1,F5.2,1X,2(I4,F6.2),T21,A)");
			CHECK = ((String) (ar.get(0) == null ? "" : ar.get(0)));
			IPRO = ((String) (ar.get(1) == null ? "" : ar.get(1)));
			O1.KNST = ((Integer) (ar.get(2) == null ? 0d : ar.get(2)));
			O1.INST = ((Integer) (ar.get(3) == null ? 0d : ar.get(3)));
			ZRES = ((Double) (ar.get(3) != null ? ar.get(3) : 0d));
			LAT1 = (Integer) (ar.get(4) != null ? ar.get(4) : 0);
			LAT2 = ((Double) (ar.get(5) != null ? ar.get(5) : 0d));
			LON1 = (Integer) (ar.get(6) != null ? ar.get(6) : 0);
			LON2 = ((Double) (ar.get(7) != null ? ar.get(7) : 0d));
			AZRES[NRP1 - 1] = ((String) (ar.get(8) == null ? "" : ar.get(8)));

			/*
			 * final ArrayList<Object> ar = read(FINPUT_READER,
			 * "(2A4,9X,2I1,F5.2,1X,2(I4,F6.2),T21,A)"); CHECK = ((String)
			 * (ar.get(0) == null ? "" : ar.get(0))); IPRO = ((String)
			 * (ar.get(1) == null ? "" : ar.get(1))); O1.KNST = ((Integer)
			 * (ar.get(2) == null ? 0d : ar.get(2))); O1.INST = ((Integer)
			 * (ar.get(3) == null ? 0d : ar.get(3))); ZRES = ((Double)
			 * (ar.get(3) != null ? ar.get(3) : 0d)); LAT1 = (Integer)
			 * (ar.get(4) != null ? ar.get(4) : 0); LAT2 = ((Double) (ar.get(5)
			 * != null ? ar.get(5) : 0d)); LON1 = (Integer) (ar.get(6) != null ?
			 * ar.get(6) : 0); LON2 = ((Double) (ar.get(7) != null ? ar.get(7) :
			 * 0d)); AZRES[NRP1 - 1] = ((String) (ar.get(8) == null ? "" :
			 * ar.get(8)));
			 */
			// TODO: another set of values needs to be defined here
			// READ(4,600)
			// CHECK,IPRO,O1.KNST,O1.INST,ZRES,LAT1,LAT2,LON1,LON2,;
			// 1 AZRES(NRP1);

			write("FPRINT_WRITER", data(CHECK, IPRO, O1.KNST, O1.INST, ZRES, LAT1, LAT2, LON1, LON2, "--- RUN AGAIN ---"), "(//2A4,9X,2I1,F5.2,1X,2(I4,F6.2),A)");

			LATRT = 60 * LAT1 + LAT2;
			LONRT = 60 * LON1 + LON2;

			if (CHECK != "    " && CHECK.length() != 0) {
				break;
			}

		}
	}

	/*
	 * SUBROUTINE SWMREG(TEST,IPRN,NR,KSMP,FNO,X,W,ISKP,KF,KZ,XMEAN, &
	 * B,Y,BSE,AF,ONF,FLIM)
	 */

	public void SWMREG(final double FNO, final double[][] X, final double[] W, final int[] ISKP, final double[] XMEAN, final double[] B, final double[] BSE, final double[] AF) throws IOException, ParseException {

		final int[] IDX = new int[4];

		final double[] V = new double[3];
		final double[] PF = new double[3];
		final double[][] A = new double[7][7];
		final double[][] T = new double[7][7];

		final double[] XSUM = new double[4];
		final double[] SIGMA = new double[4];
		final double[][] S = new double[4][4];

		// DATA L,M,MM,M1/3,4,7,5/
		final int L = 3;
		final int M = 4;
		final int MM = 7;
		final int M1 = 5;

		int KFLAG = 0;
		final double SVTEST = TEST[2];
		C2.ONF = 0;
		C2.FLIM = TEST[2];
		for (int I = 0; I < 3; I++) {// DO 2 I=1,3
			AF[I] = -1.00;
		}// 2 CONTINUE
		for (int I = 0; I < O1.NR; I++) { // DO 5 I=1,NR
			C2.ONF = C2.ONF + W[I] * (1 - KSMP[I]);
		}// 5 CONTINUE

		for (int I = 0; I < MM; I++) { // DO 10 I=1,MM
			for (int J = 0; J < MM; J++) { // DO 10 J=1,MM
				A[I][J] = 0;// 10
			}
		}
		// C-----COMPUTE MEANS,STANDARD DEVIATIONS,AND CORRECTED SUMS OF SQUARE
		for (int I = 0; I < M; I++) { // DO 40 I=1,M
			XSUM[I] = 0;
			XMEAN[I] = 0;
			for (int J = 0; J < M; J++) {// DO 40 J=1,M
				S[I][J] = 0;// 40
			}
		}
		for (int K = 0; K < O1.NR; K++) {// DO 50 K=1,NR
			for (int I = 0; I < M; I++) {// DO 50 I=1,M
				final double TEMP = X[I][K] * W[K];
				final double ETMP = TEMP * (1 - KSMP[K]);
				XSUM[I] = XSUM[I] + ETMP;
				for (int J = I; J < M; J++) {// DO 50 J=I,M
					S[I][J] = S[I][J] + TEMP * X[J][K];// 50
				}
			}
		}

		for (int I = 0; I < M; I++) {// DO 70 I=1,M
			if (C2.ONF != 0) {
				XMEAN[I] = XSUM[I] / C2.ONF;
				for (int J = I; J < M; J++) {// DO 60 J=I,M
					S[I][J] = S[I][J] - XSUM[I] * XSUM[J] / C2.ONF;// 60
				}
			}
			A[I][I] = 1;// 65
			if (S[I][I] < 0.000001) {
				S[I][I] = 0.000001;
			}
			SIGMA[I] = Math.sqrt(S[I][I]);
		}// 70 CONTINUE

		// C-----COMPUTE AND AUGMENT CORRELATION MATRIX A
		for (int I = 0; I < L; I++) {// DO 80 I=1,L
			final int I1 = I + 1;
			for (int J = I1; J < M; J++) { // DO 80 J=I1,M
				A[I][J] = S[I][J] / (SIGMA[I] * SIGMA[J]);
				A[J][I] = A[I][J];// 80
			}
		}
		double PHI = FNO - 1;
		for (int I = M1 - 1; I < MM; I++) {// DO 120 I=M1,MM
			A[I - M][I] = 1;
			A[I][I - M] = -1;// 120
		}
		for (int I = 0; I < M; I++) {// DO 140 I=1,M
			B[I] = 0;
			Y[I] = 0;
			BSE[I] = 0;
			IDX[I] = 0;// 140
		}
		if (C1.IPRN >= 3) {
			for (int K = 0; K < O1.NR; K++) {// DO 47 K=1,NR
			}// 47 CONTINUE
			for (int I = 0; I < M; I++) {// DO 78 I=1,M
			}
			for (int I = 0; I < M; I++) {// DO 90 I=1,M
			}
		}

		while (true) {
			for (int NSTEP = 0; NSTEP < L; NSTEP++) { // 150 DO 300 NSTEP=1,L

				int NU = 0;
				int MU = 0;
				if (C1.IPRN >= 3) {
					write("FPUNCH_WRITER", data(" ***** STEP NO.", NSTEP, " *****", "KZ =", O1.KZ, "KF =", O1.KF), "(//,A,I2,A,5X,A,I2,5X,A,I2)");
				}
				// FIND VARIABLE TO ENTER REGRESSION
				double VMAX = 0;// 155
				int MAX = NSTEP;
				for (int I = 0; I < L; I++) {// DO 160 I=1,L
					if (ISKP[I] == 1) {
						continue;
					}
					if (IDX[I] == 1) {
						continue;
					}
					if (I == 2 && O1.KZ == 1) {
						continue;
					}
					V[I] = A[I][M - 1] * A[M - 1][I] / A[I][I];
					if (V[I] <= VMAX) {
						continue;
					}
					VMAX = V[I];
					MAX = I;
				}// 160 CONTINUE

				double F = 0;
				if (VMAX != 0) {
					F = (PHI - 1) * VMAX / (A[M - 1][M - 1] - VMAX);
					if (F >= 1000) {
						F = 999.99;
					}
				}
				AF[MAX] = F;// 163
				boolean goto400 = false;
				if (O1.KF < 2) {
					if (F < TEST[2]) {
						goto400 = true;
					}
				}
				if (!goto400) {
					if (MAX == 2 && O1.KZ == 1) {// 165
						continue;
					}
					NU = MAX;
					IDX[NU] = 1;
					PHI = PHI - 1;
					// C-----COMPUTE MATRIX T FOR THE ENTRANCE OF VARIABLE X(NU)
					for (int J = 0; J < MM; J++) {// DO 170 J=1,MM
						T[NU][J] = A[NU][J] / A[NU][NU];// 170
					}
					for (int I = 0; I < MM; I++) { // DO 180 I=1,MM
						if (I == NU) {
							continue;
						}
						for (int J = 0; J < MM; J++) {// DO 175 J=1,MM
							T[I][J] = A[I][J] - A[I][NU] * A[NU][J] / A[NU][NU];// 175
						}
					} // 180 CONTINUE

					for (int I = 0; I < MM; I++) {// DO 190 I=1,MM
						for (int J = 0; J < MM; J++) {// DO 190 J=1,MM
							A[I][J] = T[I][J];// 190
						}
					}
					for (int I = 0; I < L; I++) { // DO 200 I=1,L
						if (IDX[I] == 0) {
							continue;
						}
						if (Math.abs(A[M - 1][M - 1] * A[I + M][I]) >= .000001) {
							PF[I] = PHI * A[I][M - 1] * A[I][M - 1] / (A[M - 1][M - 1] * A[I + M][I + M]);
							if (PF[I] >= 1000) {
								PF[I] = 999.99;
							}
							AF[I] = PF[I];
							continue;
						}
						PF[I] = 999.99;// 195
					}// 200 CONTINUE

					if (C1.IPRN >= 3) {
						ANSWER(A, S, XMEAN, SIGMA, IDX, PHI, L, M, MM, PF, NU, "ENTERING");
					}

					if (O1.KF == 2) {// 210
						continue;
					}
					if (O1.KF >= 3) {
						break;
					}

					// C-----FIND VARIABLE TO LEAVE REGRESSION
					for (int K = 0; K < L; K++) {// DO 250 K=1,L
						if (IDX[K] == 0) {
							continue;
						}
						if (PF[K] >= TEST[2]) {
							continue;
						}
						MU = K;
						F = PF[MU];
						IDX[MU] = 0;
						PHI = PHI + 1;
						for (int J = 0; J < MM; J++) { // DO 220 J=1,MM
							T[MU][J] = A[MU][J] / A[MU + M - 1][MU + M - 1];// 220
						}
						for (int I = 0; I < MM; I++) { // DO 230 I=1,MM
							if (I == MU) {
								continue;
							}
							for (int J = 0; J < MM; J++) {// DO 225 J=1,MM
								if (J == MU) {
									continue;
								}
								T[I][J] = A[I][J] - A[I][MU + M] * A[MU + M][J] / A[MU + M][MU + M];
							}// 225 CONTINUE
						}// 230 CONTINUE

						for (int I = 0; I < MM; I++) {// DO 240 I=1,MM
							if (I == MU) {
								continue;
							}
							T[I][MU] = A[I][MU] - A[I][MU + M] / A[MU + M][MU + M];
						}// 240 CONTINUE

						for (int I = 0; I < MM; I++) {// DO 245 I=1,MM
							for (int J = 0; J < MM; J++) {// DO 245 J=1,MM
								A[I][J] = T[I][J];// 245
							}
						}
						if (C1.IPRN < 3) {
							continue;
						}

						ANSWER(A, S, XMEAN, SIGMA, IDX, PHI, L, M, MM, PF, MU, "LEAVING");

					}// 250 CONTINUE
				}// 300 CONTINUE
			}

			// C-----CHECK TERMINATION CONDITION

			int KOUT = 0;// 400
			for (int I = 0; I < L; I++) {// DO 410 I=1,L
				KOUT = KOUT + IDX[I];// 410
			}
			B[3] = XMEAN[M - 1];
			if (KOUT != 0) {
				break;
			}
			if (O1.KF == 1) {
				O1.KF = 3;
				continue;
			}

			TEST[2] = TEST[2] / TEST[5];// 420
			C2.FLIM = TEST[2];// 420
			O1.KF = 1;
			KFLAG = 0;
			if (TEST[5] > 1) {
				continue;
			}
			KFLAG = 1;
			O1.KF = 4;
		}
		// C-----COMPUTE REGRESSION CONSTANT,COEFFICIENTS,AND STANDARD ERRORS
		double YSE = 77.7;// 450

		if (PHI >= 1) {
			YSE = SIGMA[M - 1] * Math.sqrt(Math.abs(A[M - 1][M - 1] / PHI));
		}
		for (int I = 0; I < L; I++) {// DO 500 I=1,L
			if (IDX[I] == 0) {
				continue;
			}
			B[I] = (A[I][M - 1] * Math.sqrt(S[M - 1][M - 1] / S[I][I]));
			BSE[I] = (YSE * Math.sqrt(Math.abs(A[I + M][I + M] / S[I][I])));
			if (O1.KF != 3) {
				Y[I] = B[I];
			}
			if (KFLAG != 0) {
				if (Math.abs(B[I]) <= TEST[5] * BSE[I]) {
					Y[I] = 0;
				}
			}
			if (PHI < 1) {
				BSE[I] = 0;// 480
			}
			B[3] = B[3] - Y[I] * XMEAN[I];
		}

		// 500 CONTINUE
		if (O1.KF != 3) {
			Y[3] = B[3];
		}
		TEST[2] = SVTEST;
		// RETURN
		// END
	}

	/*
	 * SUBROUTINE XFMAGS(TEST,FMGC,XMGC,KLAS,PRR,CALR,ICAL,IMAG,IR,QSPA, &
	 * AMX,PRX,CALX,FMP,KDX,DELTA,ZSQ,NRP,CAL,NM,AVXM,SDXM,XMAG,NF, &
	 * AVFM,SDFM,FMAG,MAG)
	 */
	@SuppressWarnings("boxing")
	public void XFMAGS(final double[] FMGC, final double[] XMGC, final int[] KLAS, final double[] PRR, final double[] CALR, final int[] ICAL, final double[][] QSPA, final double[] AMX, final double[] PRX, final double[] CALX, final double[] FMP, final int[] KDX,
			final double[] DELTA, final double[] CAL, double AVXM, double AVFM) throws IOException, ParseException {

		final double[][] RSPA = new double[8][20];

		// REAL*4 TEMP1(8,5),TEMP2(8,5),TEMP3(8,5),TEMP4(8,5)
		final double[][] TEMP1 = new double[8][5];
		final double[][] TEMP2 = new double[8][5];
		final double[][] TEMP3 = new double[8][5];
		final double[][] TEMP4 = new double[8][5];

		// DATA ZMC1,ZMC2,PWC1,PWC2/0.15,3.38,0.80,1.50/
		final double ZMC1 = 0.15;
		final double ZMC2 = 3.38;
		final double PWC1 = 0.80;
		final double PWC2 = 1.50;

		// DATA TEMP1/-0.02, 1.05,-0.15,-0.13, 0.66, 0.55, 0.17, 0.42,
		// 2 0.14, 1.18,-0.01, 0.01, 0.79, 0.66, 0.27, 0.64,
		// 3 0.30, 1.29, 0.12, 0.14, 0.90, 0.76, 0.35, 0.84,
		// 4 0.43, 1.40, 0.25, 0.27, 1.00, 0.86, 0.43, 0.95,
		// 5 0.55, 1.49, 0.38, 0.41, 1.08, 0.93, 0.49, 1.04/
		TEMP1[0] = new double[] { -0.02, 0.14, 0.30, 0.43, 0.55 };
		TEMP1[1] = new double[] { 1.05, 1.18, 1.29, 1.40, 1.49 };
		TEMP1[2] = new double[] { -0.15, -0.01, 0.12, 0.25, 0.38 };
		TEMP1[3] = new double[] { -0.13, 0.01, 0.14, 0.27, 0.41 };
		TEMP1[4] = new double[] { 0.66, 0.79, 0.90, 1.00, 1.08 };
		TEMP1[5] = new double[] { 0.55, 0.66, 0.76, 0.86, 0.93 };
		TEMP1[6] = new double[] { 0.17, 0.27, 0.35, 0.43, 0.49 };
		TEMP1[7] = new double[] { 0.42, 1.64, 0.84, 0.95, 1.04 };

		// DATA TEMP2/0.65, 1.57, 0.53, 0.57, 1.16, 1.00, 0.55, 1.13,
		// 7 0.74, 1.63, 0.71, 0.75, 1.23, 1.07, 0.63, 1.24,
		// 8 0.83, 1.70, 0.90, 0.95, 1.30, 1.15, 0.72, 1.40,
		// 9 0.92, 1.77, 1.07, 1.14, 1.38, 1.25, 0.83, 1.50,
		// A 1.01, 1.86, 1.23, 1.28, 1.47, 1.35, 0.95, 1.62/
		TEMP2[0] = new double[] { 0.65, 0.74, 0.83, 0.92, 1.01 };
		TEMP2[1] = new double[] { 1.57, 1.63, 1.70, 1.77, 1.86 };
		TEMP2[2] = new double[] { 0.53, 0.71, 0.90, 1.07, 1.23 };
		TEMP2[3] = new double[] { 0.57, 0.75, 0.95, 1.14, 1.28 };
		TEMP2[4] = new double[] { 1.16, 1.23, 1.30, 1.38, 1.47 };
		TEMP2[5] = new double[] { 1.00, 1.07, 1.15, 1.25, 1.35 };
		TEMP2[6] = new double[] { 0.55, 0.63, 0.72, 0.83, 0.95 };
		TEMP2[7] = new double[] { 1.13, 1.24, 1.40, 1.50, 1.62 };

		// DATA TEMP3/1.11, 1.96, 1.35, 1.40, 1.57, 1.46, 1.08, 1.73,
		// C 1.20, 2.05, 1.45, 1.49, 1.67, 1.56, 1.19, 1.84,
		// D 1.30, 2.14, 1.55, 1.58, 1.77, 1.66, 1.30, 1.94,
		// E 1.39, 2.24, 1.65, 1.67, 1.86, 1.76, 1.40, 2.04,
		// F 1.47, 2.33, 1.74, 1.76, 1.95, 1.85, 1.50, 2.14/
		TEMP3[0] = new double[] { 1.11, 1.20, 1.30, 1.39, 1.47 };
		TEMP3[1] = new double[] { 1.96, 2.05, 2.14, 2.24, 2.33 };
		TEMP3[2] = new double[] { 1.35, 1.45, 1.55, 1.65, 1.74 };
		TEMP3[3] = new double[] { 1.40, 1.49, 1.58, 1.67, 1.76 };
		TEMP3[4] = new double[] { 1.57, 1.67, 1.77, 1.86, 1.95 };
		TEMP3[5] = new double[] { 1.46, 1.56, 1.66, 1.76, 1.85 };
		TEMP3[6] = new double[] { 1.08, 1.19, 1.30, 1.40, 1.50 };
		TEMP3[7] = new double[] { 1.73, 1.84, 1.94, 2.04, 2.14 };

		// DATA TEMP4/1.53, 2.41, 1.81, 1.83, 2.03, 1.93, 1.58, 2.24,
		// H 1.56, 2.45, 1.85, 1.87, 2.07, 1.97, 1.62, 2.31,
		// I 1.53, 2.44, 1.84, 1.86, 2.06, 1.96, 1.61, 2.31,
		// J 1.43, 2.36, 1.76, 1.78, 1.98, 1.88, 1.53, 1.92,
		// K 1.25, 2.18, 1.59, 1.61, 1.82, 1.72, 1.37, 1.49/
		TEMP4[0] = new double[] { 1.53, 1.56, 1.53, 1.43, 1.25 };
		TEMP4[1] = new double[] { 2.41, 2.45, 2.44, 2.36, 2.18 };
		TEMP4[2] = new double[] { 1.81, 1.85, 1.84, 1.76, 1.59 };
		TEMP4[3] = new double[] { 1.83, 1.87, 1.86, 1.78, 1.61 };
		TEMP4[4] = new double[] { 2.03, 2.07, 2.06, 1.98, 1.82 };
		TEMP4[5] = new double[] { 1.93, 1.97, 1.96, 1.88, 1.72 };
		TEMP4[6] = new double[] { 1.58, 1.62, 1.61, 2.53, 1.37 };
		TEMP4[7] = new double[] { 2.24, 2.31, 2.31, 1.92, 1.49 };

		for (int I = 0; I < 8; I++) {// DO 2 I=1,8
			for (int J = 0; J < 5; J++) {// DO 1 J=1,5
				RSPA[I][J] = TEMP1[I][J];
			}// 1 CONTINUE
		}// 2 CONTINUE

		for (int I = 0; I < 8; I++) {// DO 4 I=1,8
			for (int J = 0; J < 5; J++) {// DO 3 J=1,5
				RSPA[I][J + 5] = TEMP2[I][J];
			}// 3 CONTINUE
		}// 4 CONTINUE

		for (int I = 0; I < 8; I++) {// DO 6 I=1,8
			for (int J = 0; J < 5; J++) {// DO 5 J=1,5
				RSPA[I][J + 10] = TEMP3[I][J];
			}// 5 CONTINUE
		}// 6 CONTINUE
		for (int I = 0; I < 8; I++) {// DO 8 I=1,8
			for (int J = 0; J < 5; J++) {// DO 7 J=1,5
				RSPA[I][J + 15] = TEMP4[I][J];
			}// 7 CONTINUE
		}// 8 CONTINUE

		// C
		NM = 0;
		AVXM = 0;
		SDXM = 0;
		NF = 0;
		AVFM = 0;
		SDFM = 0;

		for (int I = 0; I < O1.NRP; I++) {// DO 40 I=1,NRP
			XMAG[I] = 99.9;
			final double RAD2 = DELTA[I] * DELTA[I] + O2.ZSQ;
			int JI = 0;
			if (!(RAD2 < 1 || RAD2 > 360000)) {
				JI = KDX[I];
				final int K = KLAS[JI - 1];
				final double AMXI = Math.abs(AMX[I]);
				CAL[I] = CALX[I];
				if (CAL[I] < 0.01 || ICAL[JI - 1] == 1) {
					CAL[I] = CALR[JI - 1];
				}
				if (!(AMXI < 0.01 || CAL[I] < 0.01)) {
					if (!(K < 0 || K > 8)) {
						double XLMR = 0;
						double PRXI = 0;
						double FQ = 0;
						int IFQ = 0;

						boolean goto20 = false;
						boolean goto10 = false;
						if (K == 0) {
							goto20 = true;
						}
						if (!goto20) {
							PRXI = PRX[I];
							if (PRXI < 0.01) {
								PRXI = PRR[JI - 1];
							}
							if (C1.IR == 0) {
								goto10 = true;
							}
						}
						if (goto10 || goto20 || !(PRXI > 20 || PRXI < 0.033)) {
							if (!goto10 && !goto20) {
								FQ = (10 * Math.log10(1 / PRXI) + 20);
								IFQ = (int) FQ;
								XLMR = QSPA[K - 1][IFQ - 1] + (FQ - IFQ) * (QSPA[K - 1][IFQ + 1 - 1] - QSPA[K - 1][IFQ - 1]);
								goto20 = true;
							}
							if (goto20 || !(PRXI > 3.0 || PRXI < 0.05)) {// 10
								if (!goto20) {
									FQ = (10 * Math.log10(1 / PRXI) + 6);
									IFQ = (int) FQ;
									XLMR = RSPA[K - 1][IFQ - 1] + (FQ - IFQ) * (RSPA[K - 1][IFQ + 1 - 1] - RSPA[K - 1][IFQ - 1]);
								}

								final double BLAC = Math.log10(AMXI / (2 * CAL[I])) - XLMR; // 20
								final double RLD2 = Math.log10(RAD2);
								double BLNT = ZMC1 - PWC1 * RLD2;
								if (RAD2 >= 40000.0) {
									BLNT = ZMC2 - PWC2 * RLD2;
								}
								XMAG[I] = (BLAC - BLNT + XMGC[JI - 1]);
								NM = NM + 1;
								AVXM = AVXM + XMAG[I];
								SDXM = SDXM + XMAG[I] * XMAG[I];
							}
						}
					}
				}
			}
			FMAG[I] = 99.9;// 30
			if (FMP[I] == 0) {
				continue;
			}
			FMAG[I] = (TEST[6] + TEST[7] * Math.log10(FMP[I]) + TEST[8] * DELTA[I] + FMGC[JI - 1]);
			NF = NF + 1;
			AVFM = AVFM + FMAG[I];
			SDFM = SDFM + FMAG[I] * FMAG[I];

		}// 40 CONTINUE
		if (NM != 0) {
			AVXM = AVXM / NM;
			SDXM = Math.sqrt(SDXM / NM - AVXM * AVXM);
		}
		if (NF != 0) {// 50
			AVFM = AVFM / NF;
			SDFM = Math.sqrt(SDFM / NF - AVFM * AVFM);
		}
		if (NM == 0) {// 60
			AVXM = 99.9;
		}
		if (NF == 0) {
			AVFM = 99.9;
		}
		if (C1.IMAG - 1 < 0) {
			MAG = AVXM;// 70
			return;
		}
		if (C1.IMAG - 1 == 0) {
			MAG = AVFM;// 80
			return;
		}
		MAG = (0.5 * (AVXM + AVFM));// 90
		if (AVXM == 99.9) {
			MAG = AVFM;// 80
			return;
		}
		if (AVFM == 99.9) {
			MAG = AVXM;// 70
			return;
		}
		// RETURN
		// END
	}

	@SuppressWarnings("boxing")
	public void INPUT2(Queue<PhaseRecord> phaseRecordsList) throws IOException, ParseException {

		// CHARACTER*1 IW(151),SYM(101),QRMK(101)
		// final char[] IW = new char[151];
		// final char[] SYM = new char[101];
		// final char[] QRMK = new char[101];

		// CHARACTER*3 RMK(101)
		// final String[] RMK = new String[101];

		// CHARACTER*4 AS,IPRO,NSTA(151),ISW
		String AS = null;

		// CHARACTER*48 AHEAD
		// final String AHEAD;

		// CHARACTER*4 MSTA(101),PRMK(101),SRMK(101),AZRES(101),WRK(101)
		// final String[] MSTA = new String[101];
		// final String[] PRMK = new String[101];
		// final String[] SRMK = new String[101];
		// final String[] AZRES = new String[101];
		// final String[] WRK = new String[101];

		// CHARACTER*80 ICARD
		String ICARD = null;

		// INTEGER*4 JMIN(101),KDX(101),LDX(101)
		// final int[] JMIN = new int[101];
		// final int[] KDX = new int[101];
		// final int[] LDX = new int[101];

		// INTEGER*4 JDX(151),KSMP(151),KLAS(151),MDATE(151),MHRMN(151)
		// final int[] JDX = new int[151];
		// final int[] KSMP = new int[151];
		// final int[] KLAS = new int[151];
		// final int[] MDATE = new int[151];
		// final int[] MHRMN = new int[151];

		// REAL*4 W(101),P(101),S(101),WS(101),AMX(101),PRX(101),CALX(101)
		// final double[] W = new double[101];
		// final double[] P = new double[101];
		// final double[] S = new double[101];
		// final double[] WS = new double[101];
		// final double[] AMX = new double[101];
		// final double[] PRX = new double[101];
		// final double[] CALX = new double[101];

		// REAL*4 DT(101),FMP(101),TP(101),TS(101)
		// final double[] DT = new double[101];
		// final double[] FMP = new double[101];
		// final double[] TP = new double[101];
		// final double[] TD = new double[101];

		// REAL*4 CALR(151)
		// final double[] CALR = new double[151];

		// REAL*8 TIME1,TIME2
		double TIME1 = 0;
		double TIME2;

		boolean goto30 = false;
		boolean goto300 = false;
		boolean goto350 = false;
		int l;
		int L = 0;
		long JTIME = 0l;
		double CALP = 0;
		long KTIME = 0l;
		C4.KDATE = 0;

		do {// 30
			if (!goto30) {
				C5.PMIN = 9999.0;
				C4.IDXS = 0;

				for (int I = 0; I < C4.NS; I++) {// DO 20 I=1,NS
					KSMP[I] = 0;
					JDX[I] = 0;
				}
				L = 1;
			}
			goto30 = false;
			l = L - 1;
			if (readFromFile) {
				try {
					List ar = read(FINPUT_READER, "(2A4,T8,F1.0,T10,I8,I2,F5.2,T32,F5.2,A4,T40,F1.0,T44,F4.0,F3.2,F4.1,T59,F4.1,A3,F5.2,F5.0,T21,A4,T7,A1,T32,A4,T1,A80,T63,A1,T5,A4)");
					if (ar.size() == 0) {
						goto350 = true;
						break;
					}
					String temp = ((String) (ar.get(17) != null ? ar.get(17) : " "));
					phaseRecordsList.add(new PhaseRecord((String) (ar.get(0) != null ? ar.get(0) : ""), (String) (ar.get(1) != null ? ar.get(1) : ""), ((Double) (ar.get(2) != null ? ar.get(2) : 0d)), (Integer) (ar.get(3) != null ? ar.get(3) : 0),
							(Integer) (ar.get(4) != null ? ar.get(4) : 0), ((Double) (ar.get(5) != null ? ar.get(5) : 0d)), ((Double) (ar.get(6) != null ? ar.get(6) : 0d)), (String) (ar.get(7) != null ? ar.get(7) : 0), ((Double) (ar.get(8) != null ? ar.get(8) : 0d)),
							((Double) (ar.get(9) != null ? ar.get(9) : 0d)), ((Double) (ar.get(10) != null ? ar.get(10) : 0d)), ((Double) (ar.get(11) != null ? ar.get(11) : 0d)), ((Double) (ar.get(12) != null ? ar.get(12) : 0d)), (String) (ar.get(13) != null ? ar.get(13) : 0),
							((Double) (ar.get(14) != null ? ar.get(14) : 0d)), ((Double) (ar.get(15) != null ? ar.get(15) : 0d)), (String) (ar.get(16) != null ? ar.get(16) : 0), temp.length() != 0 ? temp.charAt(0) : ' ', (String) (ar.get(18) != null ? ar.get(18) : 0),
							(String) (ar.get(19) != null ? ar.get(19) : 0), ((String) (ar.get(20) != null ? ar.get(20) : " ")).length() != 0 ? ((String) (ar.get(20) != null ? ar.get(20) : " ")).charAt(0) : ' ', (String) (ar.get(21) != null ? ar.get(21) : 0)));
				} catch (IOException e) {
				}
			}
			// Integration code goes here
			if (phaseRecordsList.size() <= 0) {
				goto350 = true;
				break;
			}

			PhaseRecord phaseRecord = phaseRecordsList.poll();
			MSTA[l] = phaseRecord.getMSTA();
			PRMK[l] = phaseRecord.getPRMK();
			W[l] = phaseRecord.getW();
			JTIME = phaseRecord.getJTIME();
			JMIN[l] = phaseRecord.getJMIN();
			P[l] = phaseRecord.getP();
			S[l] = phaseRecord.getS();
			SRMK[l] = phaseRecord.getSRMK();
			WS[l] = phaseRecord.getWS();
			AMX[l] = phaseRecord.getAMX();
			PRX[l] = phaseRecord.getPRX();
			CALP = phaseRecord.getCALP();
			CALX[l] = phaseRecord.getCALX();
			RMK[l] = phaseRecord.getRMK();
			DT[l] = phaseRecord.getDT();
			FMP[l] = phaseRecord.getFMP();
			AZRES[l] = phaseRecord.getAZRES();
			SYM[l] = phaseRecord.getSYM();
			AS = phaseRecord.getAS();
			ICARD = phaseRecord.getICARD();
			QRMK[l] = phaseRecord.getQRMK();
			IPRO = phaseRecord.getIPRO();

			boolean endOfFileReached = false;
			/*
			 * try { List ar = read( FINPUT_READER,
			 * "(2A4,T8,F1.0,T10,I8,I2,F5.2,T32,F5.2,A4,T40,F1.0,T44,F4.0,F3.2,F4.1,T59,F4.1,A3,F5.2,F5.0,T21,A4,T7,A1,T32,A4,T1,A80,T63,A1,T5,A4)"
			 * ); if (ar.size() == 0) { goto350 = true; break; } MSTA[l] =
			 * (String) (ar.get(0) != null ? ar.get(0) : ""); PRMK[l] = (String)
			 * (ar.get(1) != null ? ar.get(1) : ""); W[l] = ((Double) (ar.get(2)
			 * != null ? ar.get(2) : 0d)); JTIME = (Integer) (ar.get(3) != null
			 * ? ar.get(3) : 0); JMIN[l] = (Integer) (ar.get(4) != null ?
			 * ar.get(4) : 0); P[l] = ((Double) (ar.get(5) != null ? ar.get(5) :
			 * 0d)); S[l] = ((Double) (ar.get(6) != null ? ar.get(6) : 0d));
			 * SRMK[l] = (String) (ar.get(7) != null ? ar.get(7) : 0); WS[l] =
			 * ((Double) (ar.get(8) != null ? ar.get(8) : 0d)); AMX[l] =
			 * ((Double) (ar.get(9) != null ? ar.get(9) : 0d)); PRX[l] =
			 * ((Double) (ar.get(10) != null ? ar.get(10) : 0d)); CALP =
			 * ((Double) (ar.get(11) != null ? ar.get(11) : 0d)); CALX[l] =
			 * ((Double) (ar.get(12) != null ? ar.get(12) : 0d)); RMK[l] =
			 * (String) (ar.get(13) != null ? ar.get(13) : 0); DT[l] = ((Double)
			 * (ar.get(14) != null ? ar.get(14) : 0d)); FMP[l] = ((Double)
			 * (ar.get(15) != null ? ar.get(15) : 0d)); AZRES[l] = (String)
			 * (ar.get(16) != null ? ar.get(16) : 0); String temp = ((String)
			 * (ar.get(17) != null ? ar.get(17) : " ")); SYM[l] = temp.length()
			 * != 0 ? temp.charAt(0) : ' '; AS = (String) (ar.get(18) != null ?
			 * ar.get(18) : 0); ICARD = (String) (ar.get(19) != null ?
			 * ar.get(19) : 0); temp = ((String) (ar.get(20) != null ?
			 * ar.get(20) : " ")); QRMK[l] = temp.length() != 0 ? temp.charAt(0)
			 * : ' '; IPRO = (String) (ar.get(21) != null ? ar.get(21) : 0); }
			 * catch (IOException e) { endOfFileReached = true; }
			 */

			if (MSTA[l].equals(" ***") || MSTA[l].equals(" $$$") || MSTA[l].equals(" ###") || endOfFileReached) {
				goto300 = true;
				break;
			}
			if (MSTA[l].equals("    ") || MSTA[l].length() == 0) {
				goto350 = true;
				break;
			}
			if (CALX[l] < 0.01) {
				CALX[l] = CALP;
			}
			if (AS.equals("    ") || AS.length() == 0) {
				S[l] = 999.99;
			}
			boolean goto50 = false;
			int I = 1;
			for (; I <= C4.NS; I++) { // DO 40 I=1,NS
				if (MSTA[l].equals(NSTA[I - 1])) {
					goto50 = true;
					break;
				}
			}
			if (!goto50) {
				// Integration code goes here
				results.addToDeletedStationsList(getFormattedString(data(" ***** ", ICARD, " ***** DELETED: ", MSTA[l], " NOT ON STATION LIST"), "(///,A,A80,A,A4,A)"));
				write("FPRINT_WRITER", data(" ***** ", ICARD, " ***** DELETED: ", MSTA[l], " NOT ON STATION LIST"), "(///,A,A80,A,A4,A)");
				goto30 = true;
				continue;
			}

			// Label 50
			KDX[l] = I;
			LDX[l] = 0;
			JDX[I - 1] = 1;

			if (FMP[l] < 0) {
				FMP[l] = 0;
			}
			if (L <= 1) {
				KTIME = JTIME;
				C4.KDATE = (int) (KTIME / 100);
				C4.KHR = (int) (KTIME - C4.KDATE * 100);
			}
			if (JTIME != KTIME) {
				// Label 65
				// Integration code goes here
				results.addToDeletedStationsList(getFormattedString(data(" ***** ", ICARD, " ***** DELETED: WRONG TIME"), "(///,A,A80,A)"));
				write("FPRINT_WRITER", data(" ***** ", ICARD, " ***** DELETED: WRONG TIME"), "(///,A,A80,A)");
				goto30 = true;
				continue;
			}

			// Label 70.
			if (!RMK[l].equals("CAL")) {

				W[l] = ((4. - W[l]) / 4);
				if (IW[I - 1] == '*') {
					W[l] = 0;
				}
				TP[l] = 60 * JMIN[l] + P[l] + DT[l];
				WRK[l] = "    ";
				boolean goto95 = false;
				if (W[l] != 0) {
					if (W[l] <= 0) {
						W[l] = (4 - WS[l]) / 4;
						KSMP[l] = 1;
						if (TP[l] < C5.PMIN) {
							C5.PMIN = TP[l];
							C4.NEAR = L;
						}
						goto95 = true;
					} else {
						// Label 89
						if (TP[l] < C5.PMIN) {
							C5.PMIN = TP[l];
							C4.NEAR = L;
						}
					}
				} else {
					goto95 = true;
				}
				if (!goto95) {
					// Label 90
					if (!AS.equals("    ") && AS.length() != 0) {
						C4.IDXS = 1;
						LDX[l] = 1;
						WS[l] = (4 - WS[l]) / 4;
						if (IW[I - 1] == '*') {
							WS[l] = 0;
						}
						goto95 = true;
					}
				}
				if (goto95) {
					// Label 95
					TS[l] = 60 * JMIN[l] + S[l] + DT[l];
				}

				// Label 100
				L = L + 1;
				if (L < MMAX) {
					goto30 = true;
					continue;
				}
				write("FPRINT_WRITER", data(""), "///,' ***** ERROR: PHASE LIST EXCEEDS ARRAY DIMENSION; EXTR A DATA " + "TREATED AS NEXT EARTHQUAKE");
				goto350 = true;
				break;
			}
			// Label 200
			if (P[l] != 0) {
				KLAS[I - 1] = (int) P[l];
			}
			CALR[I - 1] = CALX[l];
			TIME2 = 1.D + 06 * C4.KDATE + 1.D + 04 * C4.KHR + 1.D + 02 * JMIN[l];
			if (TIME2 < TIME1) {
				write("FPRINT_WRITER", data(""), "///,' ********** THE FOLLOWING EVENT IS OUT OF CHRONOLOGICA ORDER " + "**********");
			}
			// Label 250
			write("FPRINT_WRITER", data(C4.KDATE, C4.KHR, JMIN[l], MSTA[l], KLAS[I - 1], CALR[I - 1]), "///,' ***** ',I6,1X,2I2," + "' ***** CALIBRATION CHANGE " + "FOR : KLAS = ',I1,', CALR = ',F4.1");
			MDATE[I - 1] = C4.KDATE;
			MHRMN[I - 1] = (int) (100 * C4.KHR + JMIN[l]);
			TIME1 = TIME2;
		} while (L < MMAX);// IF (L .LT. MMAX)

		if (goto300) {
			MJUMP = 1; // 300
			O1.NR = L - 1;
		}
		if (goto350) {
			MJUMP = 0; // 350
			O1.NR = L - 1;
		}
	}

	// SUBROUTINE ANSWER(A,S,XMEAN,SIGMA,IDX,PHI,L,M,MM,PF,NDX,ADX)
	@SuppressWarnings("boxing")
	public void ANSWER(final double[][] A, final double[][] S, final double[] XMEAN, final double[] SIGMA, final int[] IDX, final double PHI, final int L, final int M, final int MM, final double[] PF, final int NDX, final String ADX) throws IOException, ParseException {

		final double[] B = new double[4];
		final double[] BSE = new double[4];

		for (int I = 0; I < MM; I++) {// DO 410 I=1,MM
			write("FPRINT_WRITER", data(convertArrayToListOfObjects(A[I]).subList(0, MM).toArray()), "(7E18.8)");
			// WRITE(8,400) (A(I,J),J=1,MM)
			// 400 FORMAT(7E18.8)
		} // 410 CONTINUE

		final double FVE = 1 - A[M - 1][M - 1];
		double B0 = XMEAN[M - 1];
		double YSE = 77.7;
		if (PHI >= 1) {
			YSE = SIGMA[M - 1] * Math.sqrt(Math.abs(A[M - 1][M - 1] / PHI));
		}
		for (int I = 0; I < L; I++) { // DO 5 I=1,L
			if (IDX[I] == 0) {
				continue;
			}
			B[I] = (A[I][M - 1] * Math.sqrt(Math.abs(S[M - 1][M - 1] / S[I][I])));
			BSE[I] = (YSE * Math.sqrt(Math.abs(A[I + M - 1][I + M - 1] / S[I][I])));
			B0 = B0 - B[I] * XMEAN[I];
		}// 5 CONTINUE

		write("FPRINT_WRITER", data(" VARIABLE ", ADX, "................", NDX, " FRACTION OF VARIATION EXPLAINED..", FVE, " STANDARD ERROR OF Y..............", YSE, " CONSTANT IN REGRESSION EQUATION..", B0), "(/,A,A8,A,I5,A," + "" + "E18.8,A,E18.8,A," + "E18.8)");

		write("FPRINT_WRITER", data(" VARIABLE COEFFICIENT STANDARD ERROR", " PARTIAL F-VALUE"), "(/,A,A)");

		for (int I = 0; I < L; I++) { // DO 40 I=1,L
			if (IDX[I] == 0) {
				continue;
			}
			write("FPRINT_WRITER", data(I + 1, B[I], BSE[I], PF[I]), "(I5,3E20.6)");
		}// 40 CONTINUE
			// RETURN
	}

	private static abstract class Wrapper<T> {
		public abstract void setValue(T value);

	}

	private <T> List<Object> convertArrayToListOfObjects(double[] array) {
		List<Object> list = new ArrayList<Object>();
		for (double element : array) {
			list.add(element);
		}
		return list;
	}

	private static ArrayList<Object> read(final BufferedReader reader, final String format) throws IOException, ParseException {
		final String readline = reader.readLine();
		if (readline == null) {
			throw new IOException("End of file");
		}
		if (readline.length() == 0) {
			return new ArrayList<Object>(0);
		}
		return FortranFormat.read(readline, format);
	}

	@SuppressWarnings("boxing")
	public void INPUT1(Queue<Station> stationsList, Queue<CrustalModel> crustalModelList, ControlCard controlCard) throws IOException, ParseException {
		int IFLAG = 0;

		write("FPRINT_WRITER", data("1"), "(A)");// line 211

		if (MJUMP < 1) {
			TEST[0] = 0.10;
			TEST[1] = 10.0;
			TEST[2] = 2.0;
			TEST[3] = 0.05;
			TEST[4] = 5.0;
			TEST[5] = 4.0;
			TEST[6] = -0.87;
			TEST[7] = 2.00;
			TEST[8] = 0.0035;
			TEST[9] = 100.0;
			TEST[10] = 8.0;
			TEST[11] = 0.5;
			TEST[12] = 1.0;

			if (readFromFile) {

				AHEAD = "                                                ";
				for (int I = 1; I <= 16; I++) {
					// Reading of the input file
					// header
					// The name of the input here - BHEAD and the test values
					// can be
					// resetted if necessary
					final ArrayList<Object> result = read(FINPUT_READER, "(A4,T12, I2,T16,F9.4,A48)");
					if (result.size() == 0) {
						break;
					}
					ISW = (String) result.get(0);
					int J = 0;
					double TESTJ = 0;
					if (result.get(1) != null) {
						J = (Integer) result.get(1);
					}
					if (result.get(2) != null) {
						TESTJ = (Double) result.get(2);
					}
					BHEAD = (String) result.get(3);
					if (ISW.equals("    ") || ISW.equals("1   ") || ISW.length() == 0) {
						break;
					}
					if (!ISW.equals("HEAD")) {
						IFLAG = 1;
						TEST[J - 1] = TESTJ;
					} else {
						AHEAD = BHEAD;
					}
				}
			}

			IFLAG = 0;

			write("FPRINT_WRITER", data(AHEAD), "(40X,A48)");
			write("FPRINT_WRITER", data(" ***** PROGRAM: HYPO71PC (Version 1: 11/29/85) *****", "TEST(1)  TEST(2)  TEST(3)  TEST(4)  TEST(5)  TEST(6)  TEST(7)  TEST(8)  TEST(9) TEST(10)" + " TEST(11) TEST(12) TEST(13)"), "(///,A," + "" + "///,13X,A)");
			write("FPRINT_WRITER", " STANDARD ");
			write("FPRINT_WRITER", convertArrayToListOfObjects(Arrays.copyOf(TEST, 13)), "(13F9.4)");

			if (IFLAG != 0) {
				write("FPRINT_WRITER", " RESET TO ");
				write("FPRINT_WRITER", convertArrayToListOfObjects(Arrays.copyOf(TEST, 13)), "(13F9.4)");
			}

			TEST[0] = TEST[0] * TEST[0];
			TEST[1] = TEST[1] * TEST[1];
			TEST[3] = TEST[3] * TEST[3];

			KNO = 0;

			if (!ISW.equals("1   ")) {
				KNO = 1;
				write("FPRINT_WRITER", data("L     STN     LAT     LONG    ELV DELAY", "FMGC  XMGC KL  PRR  CALR IC      DATE HRMN"), "(/,4X,A,5X,A)");
			} else {
				write("FPRINT_WRITER", data("L   STN    LAT      LONG      ELV     M  DLY1  DLY2  XMGC FMGC KL CALR IC   DATE HRMN"), "(/,4X,A)");
			}

			int i = 0;
			boolean goto55 = false;
			for (int L = 1; L <= NMAX; L++) {// DO 50 L=1,NMAX

				i = L - 1;

				if (readFromFile) {
					if (!ISW.equals("1   ")) {
						List ar = read(FINPUT_READER, "(1X,A1,A4,I2,F5.2,A1,I3,F5.2,A1,I4,F6.2,4X,F5.2,2X,F5.2,1X,I1,F5.2,F7.2,1X,I1,5X,I6,I4)");
						if (ar.size() == 0) {
							break;
						}
						stationsList
								.add(new Station(((String) (ar.get(0) != null ? ar.get(0) : " ")).charAt(0), (String) (ar.get(1) != null ? ar.get(1) : " "), (Integer) (ar.get(2) != null ? ar.get(2) : 0), ((Double) (ar.get(3) != null ? ar.get(3) : 0d)),
										((String) (ar.get(4) != null ? ar.get(4) : " ")).charAt(0), (Integer) (ar.get(5) != null ? ar.get(5) : 0), ((Double) (ar.get(6) != null ? ar.get(6) : 0d)), ((String) (ar.get(7) != null ? ar.get(7) : " ")).charAt(0),
										(Integer) (ar.get(8) != null ? ar.get(8) : 0), ((Double) (ar.get(9) != null ? ar.get(9) : 0d)), ((Double) (ar.get(10) != null ? ar.get(10) : 0d)), ((Double) (ar.get(11) != null ? ar.get(11) : 0d)), (Integer) (ar.get(12) != null ? ar
												.get(12) : 0), ((Double) (ar.get(13) != null ? ar.get(13) : 0d)), ((Double) (ar.get(14) != null ? ar.get(14) : 0d)), (Integer) (ar.get(15) != null ? ar.get(15) : 0), (Integer) (ar.get(16) != null ? ar.get(16) : 0), (Integer) (ar
												.get(17) != null ? ar.get(17) : 0)));

					} else {// 30
						List ar = read(FINPUT_READER, "(A4,A1,I2,1X,F5.2,A1,I3,1X,F5.2,A1,I4,5X,I1,4F6.2,1X,I1,F6.2,1X,I1,2X,I6,I4)");
						stationsList.add(new Station(((String) (ar.get(1) != null ? ar.get(1) : "")).charAt(0), (String) (ar.get(0) != null ? ar.get(0) : ""), (Integer) (ar.get(2) != null ? ar.get(2) : 0), ((Double) (ar.get(3) != null ? ar.get(3) : 0d)),
								((String) (ar.get(4) != null ? ar.get(4) : "")).charAt(0), (Integer) (ar.get(5) != null ? ar.get(5) : 0), ((Double) (ar.get(6) != null ? ar.get(6) : 0d)), ((String) (ar.get(7) != null ? ar.get(7) : "")).charAt(0), (Integer) (ar.get(8) != null ? ar
										.get(8) : 0), (Integer) (ar.get(9) != null ? ar.get(9) : 0), (Integer) (ar.get(10) != null ? ar.get(10) : 0), (Integer) (ar.get(11) != null ? ar.get(11) : 0), (Integer) (ar.get(12) != null ? ar.get(12) : 0),
								(Integer) (ar.get(13) != null ? ar.get(13) : 0), (Integer) (ar.get(14) != null ? ar.get(14) : 0), (Integer) (ar.get(15) != null ? ar.get(15) : 0), (Integer) (ar.get(16) != null ? ar.get(16) : 0), (Integer) (ar.get(17) != null ? ar.get(17) : 0),
								(Integer) (ar.get(18) != null ? ar.get(18) : 0)));
					}
				}
				// Integration code goes here
				if (stationsList.size() <= 0) {
					break;
				}
				Station station = stationsList.poll();
				IW[i] = station.getIW();
				NSTA[i] = station.getNSTA();
				LAT1 = station.getLAT1();
				LAT2 = station.getLAT2();
				INS[i] = station.getINS();
				LON1 = station.getLON1();
				LON2 = station.getLON2();
				IEW[i] = station.getIEW();
				IELV[i] = station.getIELV();
				DLY[0][i] = station.getDly();
				FMGC[i] = station.getFMGC();
				XMGC[i] = station.getXMGC();
				KLAS[i] = station.getKLAS();
				PRR[i] = station.getPRR();
				CALR[i] = station.getCALR();
				ICAL[i] = station.getICAL();
				NDATE[i] = station.getNDATE();
				NHRMN[i] = station.getNHRMN();
				if (INS[i] == ' ') {
					INS[i] = 'N';
				}
				if (IEW[i] == ' ') {
					IEW[i] = 'W';
				}

				if (!ISW.equals("1   ")) {
					write("FPRINT_WRITER", data(L, IW[i], NSTA[i], (int) LAT1, LAT2, INS[i], (int) LON1, LON2, IEW[i], IELV[i], DLY[0][i], FMGC[i], XMGC[i], KLAS[i], PRR[i], CALR[i], ICAL[i], NDATE[i], NHRMN[i]), "(I5,3X," + "A1,A4," + "I3,F5.2,A1,I4," + ""
							+ "F5.2,A1,I5,F6.2,4X,F5.2,2X,F5.2,1X,I1,F5.2,F7.2,1X,I1,5X,I6,I4)");
				} else {// 30
					write("FPRINT_WRITER", data(L, NSTA[i], IW[i], LAT1, LAT2, INS[i], LON1, LON2, IEW[i], IELV[i], MNO[i], DLY[0][i], DLY[1][i], XMGC[i], FMGC[i], KLAS[i], CALR[i], ICAL[i], NDATE[i], NHRMN[i]), "I5,2X,A4,A1,I2," + ""
							+ "1X,F5.2,A1,I4,1X,F5.2,A1,I5,5X,I1,4F6.2,1X,I1,F6.2,1X,I1,2X,I6,I4");
				}

				// 40
				LAT[i] = 60 * LAT1 + LAT2;
				LON[i] = (60 * LON1 + LON2);
				MDATE[i] = NDATE[i];
				MHRMN[i] = NHRMN[i];
				KLSS[i] = KLAS[i];
				CALS[i] = CALR[i];
			}// 50
			if (goto55) {
				write("FPRINT_WRITER", data(" ***** ERROR: STATION LIST EXCEEDS ARRAY DIMENSION"), "(///,A)");
				System.exit(-1);
			}
			C4.NS = i;

		}// IF (MJUMP-1) 1,100,200
		if (MJUMP <= 1) {// 100
			write("FPRINT_WRITER", data("CRUSTAL MODEL 1", "VELOCITY     DEPTH"), "(///,7X,A,/,5X,A)");

			int i = 0;
			boolean error = true;
			for (int L = 1; L < LMAX; L++) {// /DO 130 L=1,LMAX

				i = L - 1;
				if (readFromFile) {
					List ar = read(FINPUT_READER, "(2F7.3)");
					if (ar.size() == 0) {
						error = false;
						break;
					}
					crustalModelList.add(new CrustalModel(((Double) (ar.get(0) != null ? ar.get(0) : 0d)), ((Double) (ar.get(1) != null ? ar.get(1) : 0d))));
				}
				// Integration code goes here
				if (crustalModelList.size() <= 0) {
					error = false;
					break;
				}

				CrustalModel crustalModel = crustalModelList.poll();

				V[i] = crustalModel.getV();
				D[i] = crustalModel.getD();

				/*
				 * List ar = read(FINPUT_READER, "(2F7.3)"); if (ar.size() == 0)
				 * { error = false; break; } V[i] = ((Double) (ar.get(0) != null
				 * ? ar.get(0) : 0d)); D[i] = ((Double) (ar.get(1) != null ?
				 * ar.get(1) : 0d));
				 */
				if (V[i] < 0.01) {
					error = false;
					break;
				}
				write("FPRINT_WRITER", data(V[i], D[i]), "(3X,2F10.3)");
				DEPTH[i] = D[i];
				VSQ[i] = V[i] * V[i];
			}// 130 CONTINUE

			if (error) {
				write("FPRINT_WRITER", data(" ***** ERROR: CRUSTAL MODEL EXCEEDS ARRAY DIMENSION"), "(///,A)");
			}
			// 140
			C4.NL = i;
			final int N1 = C4.NL - 1;

			for (int L = 1; L <= N1; L++) {
				THK[L - 1] = D[L] - D[L - 1];
				H[L - 1] = THK[L - 1];
			}// 145

			for (int J = 1; J <= C4.NL; J++) { // DO 150 J=1,NL
				int j = J - 1;
				G[0][j] = (sqrt(abs(VSQ[j] - VSQ[0])) / (V[0] * V[j]));
				G[1][j] = (sqrt(abs(VSQ[j] - VSQ[1])) / (V[1] * V[j]));
				G[2][j] = (V[0] / sqrt(abs(VSQ[j] - VSQ[0]) + 0.000001));
				G[3][j] = (V[1] / sqrt(abs(VSQ[j] - VSQ[1]) + 0.000001));
				if (J <= 1) {
					G[0][j] = 0;
				}
				if (J <= 2) {
					G[1][j] = 0;
				}
				if (J <= 1) {
					G[2][j] = 0;
				}
				if (J <= 2) {
					G[3][j] = 0;
				}
				for (int L = 1; L <= C4.NL; L++) { // DO 150 L=1, NL
					F[L - 1][j] = 1;
					if (L >= J) {
						F[L - 1][j] = 2;
					}
				}
			}// 150 CONTINUE
			for (int J = 1; J <= C4.NL; J++) { // DO 165 J=1,NL
				for (int M = 1; M <= C4.NL; M++) {
					TID[J - 1][M - 1] = 0;
					DID[J - 1][M - 1] = 0;
				}
			}// 165
			for (int J = 1; J <= C4.NL; J++) { // DO 170 J=1,NL
				final int j = J - 1;
				for (int M = J; M <= C4.NL; M++) {// DO 170 M=J,NL
					if (M == 1) {
						continue;
					}
					final int m = M - 1;
					final int M1 = m;
					for (int L = 1; L <= M1; L++) { // DO 160 L=1,M1
						final int l = L - 1;
						final double SQT = sqrt(VSQ[m] - VSQ[l]);
						final double TIM = THK[l] * SQT / (V[l] * V[m]);
						final double DIM = THK[l] * V[l] / SQT;
						TID[j][m] = (TID[j][m] + F[l][j] * TIM);
						DID[j][m] = (DID[j][m] + F[l][j] * DIM);
					}// 160
				}// 170
			}// 170
			if (ISW.equals("1   ")) {
				final double VC = V[0] * V[1] / sqrt(VSQ[1] - VSQ[0]);
				for (int I = 0; I <= C4.NS; I++) {// DO 180 I=1,NS
					FLT[0][I] = (DLY[0][I] * VC + D[1]);
					FLT[1][I] = (DLY[1][I] * VC + D[1]);
				}// 180
			}
		}

		write("FPRINT_WRITER", data(" KS Z XNEAR XFAR  POS   IQ  KMS  KFM IPUN IMAG   IR IPRN CODE   LATR      " + "LONR"), "(///,A)");
		KSING = 0;

		if (readFromFile) {
			List ar = read(FINPUT_READER, "(I1,F4.0,2F5.0,F5.2,7I5,5I1,2(I4,F6.2))");
			controlCard = new ControlCard((Integer) (ar.get(0) != null ? ar.get(0) : 0), ((Double) (ar.get(1) != null ? ar.get(1) : 0d)), ((Double) (ar.get(2) != null ? ar.get(2) : 0d)), ((Double) (ar.get(3) != null ? ar.get(3) : 0d)), ((Double) (ar.get(4) != null ? ar.get(4)
					: 0d)), (Integer) (ar.get(5) != null ? ar.get(5) : 0), (Integer) (ar.get(6) != null ? ar.get(6) : 0), (Integer) (ar.get(7) != null ? ar.get(7) : 0), (Integer) (ar.get(8) != null ? ar.get(8) : 0), (Integer) (ar.get(9) != null ? ar.get(9) : 0),
					(Integer) (ar.get(10) != null ? ar.get(10) : 0), (Integer) (ar.get(11) != null ? ar.get(11) : 0), (Integer) (ar.get(12) != null ? ar.get(12) : 0), (Integer) (ar.get(13) != null ? ar.get(13) : 0), (Integer) (ar.get(14) != null ? ar.get(14) : 0),
					(Integer) (ar.get(15) != null ? ar.get(15) : 0), (Integer) (ar.get(16) != null ? ar.get(16) : 0), (Integer) (ar.get(17) != null ? ar.get(17) : 0), (Integer) (ar.get(18) != null ? ar.get(18) : 0), ((Double) (ar.get(19) != null ? ar.get(19) : 0d)),
					((Double) (ar.get(20) != null ? ar.get(20) : 0d)));
		}

		// Integration code goes here
		KSING = controlCard.getKSING();
		C2.ZTR = controlCard.getZTR();
		C2.XNEAR = controlCard.getXNEAR();
		C2.XFAR = controlCard.getXFAR();
		C2.POS = controlCard.getPOS();
		C1.IQ = controlCard.getIQ();
		C1.KMS = controlCard.getKMS();
		C1.KFM = controlCard.getKFM();
		C1.IPUN = controlCard.getIPUN();
		C1.IMAG = controlCard.getIMAG();
		C1.IR = controlCard.getIR();
		C1.IPRN = controlCard.getIPRN();
		C1.KPAPER = controlCard.getKPAPER();
		C1.KTEST = controlCard.getKTEST();
		C1.KAZ = controlCard.getKAZ();
		C1.KSORT = controlCard.getKSORT();
		C1.KSEL = controlCard.getKSEL();
		LAT1 = controlCard.getLAT1();
		LAT2 = controlCard.getLAT2();
		LON1 = controlCard.getLON1();
		LON2 = controlCard.getLON2();

		/*
		 * List ar = read(FINPUT_READER,
		 * "(I1,F4.0,2F5.0,F5.2,7I5,5I1,2(I4,F6.2))"); KSING = (Integer)
		 * (ar.get(0) != null ? ar.get(0) : 0); C2.ZTR = ((Double) (ar.get(1) !=
		 * null ? ar.get(1) : 0d)); C2.XNEAR = ((Double) (ar.get(2) != null ?
		 * ar.get(2) : 0d)); C2.XFAR = ((Double) (ar.get(3) != null ? ar.get(3)
		 * : 0d)); C2.POS = ((Double) (ar.get(4) != null ? ar.get(4) : 0d));
		 * C1.IQ = (Integer) (ar.get(5) != null ? ar.get(5) : 0); C1.KMS =
		 * (Integer) (ar.get(6) != null ? ar.get(6) : 0); C1.KFM = (Integer)
		 * (ar.get(7) != null ? ar.get(7) : 0); C1.IPUN = (Integer) (ar.get(8)
		 * != null ? ar.get(8) : 0); C1.IMAG = (Integer) (ar.get(9) != null ?
		 * ar.get(9) : 0); C1.IR = (Integer) (ar.get(10) != null ? ar.get(10) :
		 * 0); C1.IPRN = (Integer) (ar.get(11) != null ? ar.get(11) : 0);
		 * C1.KPAPER = (Integer) (ar.get(12) != null ? ar.get(12) : 0); C1.KTEST
		 * = (Integer) (ar.get(13) != null ? ar.get(13) : 0); C1.KAZ = (Integer)
		 * (ar.get(14) != null ? ar.get(14) : 0); C1.KSORT = (Integer)
		 * (ar.get(15) != null ? ar.get(15) : 0); C1.KSEL = (Integer)
		 * (ar.get(16) != null ? ar.get(16) : 0); LAT1 = (Integer) (ar.get(17)
		 * != null ? ar.get(17) : 0); LAT2 = (Integer) (ar.get(18) != null ?
		 * ar.get(18) : 0); LON1 = ((Double) (ar.get(19) != null ? ar.get(19) :
		 * 0d)); LON2 = ((Double) (ar.get(20) != null ? ar.get(20) : 0d));
		 */

		write("FPRINT_WRITER", data(KSING, C2.ZTR, C2.XNEAR, C2.XFAR, C2.POS, C1.IQ, C1.KMS, C1.KFM, C1.IPUN, C1.IMAG, C1.IR, C1.IPRN, C1.KPAPER, C1.KTEST, C1.KAZ, C1.KSORT, C1.KSEL, (int) LAT1, LAT2, Double.valueOf(LON1).intValue(), LON2), "(1X," + "I1," + "F4.0," + "2F5.0,"
				+ "F5.2," + "" + "7I5,5I1," + "2(I4," + "F6.2))");

		C2.LATR = 60 * LAT1 + LAT2;
		C2.LONR = (60 * LON1 + LON2);

		if (readFromFile) {
			if (C1.IR != 0) {
				for (int I = 1; I <= C1.IR; I++) {
					for (int J = 1; J <= 40; J++) {
						List ar = read(FINPUT_READER, "(20F4.2)");
						QSPA[I - 1] = (double[]) ar.get(0);
					}
					for (int J = 1; J <= 40; J++) {
						write("FPRINT_WRITER", data(" QSPA(", I, QSPA[I - 1], ""), "(/I1,20F5.2,/,10X,20F5.2)");
					}
				}
			}
		}

	}

	private ArrayList<Object> data(final Object... data) {
		return new ArrayList<Object>(Arrays.asList(data));
	}

	private void write(final String filePrefix, final List<Object> data, final String format) throws IOException, ParseException {
		final String writeline = getFormattedString(data, format);
		write(filePrefix, writeline);
	}

	private void write(final String filePrefix, final String data) throws IOException, ParseException {
		if (filePrefix.equals("FPRINT_WRITER")) {
			results.setPrintOutput(results.getPrintOutput() + "\n"/*
																 * + filePrefix
																 * + ":"
																 */+ data);
		} else {
			results.setPunchOutput(results.getPrintOutput() + "\n"/*
																 * + filePrefix
																 * + ":"
																 */+ data);
		}
	}

	private String getFormattedString(final List<Object> data, final String format) throws IOException, ParseException {
		return FortranFormat.write(data, format);
	}

	public void SORT(double[] x, int[] key, int lo) {
		int i = 1;
		for (i = 1; i < lo + 1; i++) {
			key[i - 1] = i;
		}

		int mo = lo;
		outer: while (true) {
			if (mo > 15) {
				mo = 2 * (mo / 4) + 1;
			} else if (mo <= 15 && mo > 1) {
				mo = 2 * (mo / 8) + 1;
			} else {
				return;
			}
			int ko = lo - mo;
			int jo = 1;
			while (true) {
				i = jo;
				do {
					if (x[i - 1] > x[i + mo - 1]) {
						double temp = x[i - 1];
						x[i - 1] = x[i + mo - 1];
						x[i + mo - 1] = temp;
						int kemp = key[i - 1];
						key[i - 1] = key[i + mo - 1];
						key[i + mo - 1] = kemp;
						i = i - mo;
					} else {
						break;
					}
				} while (i >= 1);
				jo += 1;
				if (jo > ko) {
					continue outer;
				}

			}
		}
	}

	/**
	 * Returns string value if it's length is not zero and default value
	 * otherwise
	 * 
	 * @return
	 */
	private String getValueIfNotEmpty(String string, String defaultValue) {
		return string.trim().length() == 0 ? defaultValue : string;
	}

	public Hypo71() {

	}

	public static Hypo71 getInstance() {
		return new Hypo71();
	}

	/**
	 * 
	 * @param BHEAD
	 *            Calculation data name
	 * @param TEST
	 *            Array of values for test array (length = 15). If ATEST[i] =
	 *            1.23456, then leaves TEST[i] default, if null then leaves all
	 *            TEST default
	 * @throws ParseException
	 * @throws IOException
	 */
	public String calculateHypo71(String BHEAD, double[] ATEST, Queue<Station> stationsList, Queue<CrustalModel> crustalModelList, ControlCard controlCard, Queue<PhaseRecord> phaseRecordsList, String fileName) throws IOException, ParseException {
		this.readFromFile = fileName != null;
		if (readFromFile) {
			FINPUT_READER = new BufferedReader(new FileReader(fileName));
		}
		boolean goto40 = false;
		try {
			while (true) {
				if (!goto40) {
					MJUMP = 0;
				}
				goto40 = false;
				INPUT1(stationsList, crustalModelList, controlCard);
				if (C1.IPUN != 0) {
					write("FPUNCH_WRITER", data(" DATE    ORIGIN    LAT ", INS[0], "    LONG ", IEW[0], "    DEPTH    MAG NO GAP DMIN  RMS  ERH  ERZ QM"), "(A,A1,A,A1,A)");
				}
				C5.XFN = C2.XFAR - C2.XNEAR + 0.000001;
				TIME1 = 0;
				g900: while (true) {
					do {
						INPUT2(phaseRecordsList);
						if (MJUMP != 1) {
							if (O1.NR < 1) {
								write("FPRINT_WRITER", "\n\n\n ***** EXTRA BLANK CARD ENCOUNTERED *****");
							} else {
								break;
							}
						} else {
							break g900;
						}
					} while (O1.NR >= 1);
					O1.KKF = 0;
					int KYEAR = C4.KDATE / 10000;
					int KMONTH = (C4.KDATE - 10000 * KYEAR) / 100;
					int KDAY = C4.KDATE - 10000 * KYEAR - 100 * KMONTH;
					if (KSING != 1) {
						SINGLE(false);
					} else {
						SINGLE(true);
					}
					if (O1.KKF != 0) {
						//return " *** INSUFFICIENT DATA FOR LOCATING THIS QUAKE ***";
						break;
					}
					// C------- COMPUTE SUMMARY OF MAGNITUDE RESIDUALS
					// -----------------------
					if (C4.IEXIT == 1 || O1.JAV > C1.IQ) {
						continue;
					}
					int JI;
					for (int i = 0; i < O1.NRP; i++) {
						if (XMAG[i] != 99.9) {
							JI = KDX[i];
							double DXMAG = XMAG[i] - AVXM;
							NXM[JI - 1] = NXM[JI - 1] + 1;
							SXM[JI - 1] = SXM[JI - 1] + DXMAG;
							SXMSQ[JI - 1] = SXMSQ[JI - 1] + DXMAG * DXMAG;
						}
						if (FMAG[i] != 99.9) {
							JI = KDX[i];
							double DFMAG = FMAG[i] - AVFM;
							NFM[JI - 1] = NFM[JI - 1] + 1;
							SFM[JI - 1] = SFM[JI - 1] + DFMAG;
							SFMSQ[JI - 1] = SFMSQ[JI - 1] + DFMAG * DFMAG;
						}
					}
				}
				// C------- END OF ONE DATA SET: PRINT SUMMARY OF RESIDUALS &
				// RETURN ------
				SUMOUT();
				if (MSTA[O1.NR + 1 - 1] == " ***") {
					continue;
				}
				MJUMP = 1;
				if (MSTA[O1.NR + 1 - 1] == " $$$") {
					goto40 = true;
					continue;
				}
				MJUMP = 2;
				if (MSTA[O1.NR + 1 - 1] == " ###") {
					goto40 = true;
				} else {
					break;
				}
			}
			return null;
		} finally {
			if (readFromFile) {
				FINPUT_READER.close();
			}
		}
	}

	private void initStringArray(String[] array) {
		for (int i = 0; i < array.length; i++) {
			array[i] = "";
		}
	}

	public Results getResults() {
		return results;
	}

	public static void main(String[] args) throws FileNotFoundException, IOException {
        if (args.length < 1) {
            System.out.println("Specify input file on command line");
            return;
        }
		runWithFileInput(args[0]);
        if (true) return;

        //System.out.println("DATA INPUT");
		Hypo71 hypoCalculator = new Hypo71();
		Queue<Station> stationsList = new LinkedList<Station>();
		Queue<CrustalModel> crustalModelList = new LinkedList<CrustalModel>();
		Queue<PhaseRecord> phaseRecordsList = new LinkedList<PhaseRecord>();
		// ControlCard controlCard = new ControlCard(0, 5.0, 50.0, 100.0, 1.78,
		// 2, 0, 18, 1, 1, 0, 1, 0, 0, 0, 1, 1, 0, 0, 0, 0);
		Properties props = new Properties();
		props.load(new FileInputStream("hypo71Constants.properties"));
		ControlCard controlCard = new ControlCard(props);

		// Filling stations list
		stationsList.add(new Station(' ', "SR01", 38, 42.55, ' ', 122, 59.17, ' ', 0, -0.15, 0.4, 0.25, 8, 0.0, 0.0, 0, 0, 0));
		stationsList.add(new Station(' ', "SR02", 38, 27.28, ' ', 123, 04.80, ' ', 0, 0.09, 0.4, 0.25, 8, 0.0, 0.0, 0, 0, 0));
		stationsList.add(new Station(' ', "SR03", 38, 14.15, ' ', 122, 51.29, ' ', 0, 0.12, 0.4, 0.25, 8, 0.0, 0.0, 0, 0, 0));
		stationsList.add(new Station(' ', "SR04", 38, 17.20, ' ', 122, 31.92, ' ', 0, 0.14, 0.4, 0.25, 8, 0.0, 0.0, 0, 0, 0));
		stationsList.add(new Station(' ', "SR05", 38, 29.55, ' ', 122, 24.33, ' ', 0, 0.07, 0.4, 0.25, 8, 0.0, 0.0, 0, 0, 0));
		stationsList.add(new Station(' ', "SR06", 38, 42.58, ' ', 122, 32.22, ' ', 0, -0.19, 0.4, 0.25, 8, 0.0, 0.0, 0, 0, 0));
		stationsList.add(new Station(' ', "SR07", 38, 32.20, ' ', 122, 42.78, ' ', 0, 0.03, 0.4, 0.25, 8, 0.0, 0.0, 0, 0, 0));
		stationsList.add(new Station(' ', "SR8A", 38, 35.50, ' ', 122, 49.38, ' ', 0, 0.04, 0.4, 0.25, 8, 0.0, 0.0, 0, 0, 0));
		stationsList.add(new Station(' ', "SR08", 38, 35.92, ' ', 122, 48.25, ' ', 0, 0.07, 0.4, 0.25, 8, 0.0, 0.0, 0, 0, 0));
		stationsList.add(new Station(' ', "SR09", 38, 29.42, ' ', 122, 51.00, ' ', 0, -0.19, 0.4, 0.25, 8, 0.0, 0.0, 0, 0, 0));
		stationsList.add(new Station(' ', "SR10", 38, 25.00, ' ', 122, 38.75, ' ', 0, -0.16, 0.4, 0.25, 8, 0.0, 0.0, 0, 0, 0));
		stationsList.add(new Station(' ', "SR11", 38, 33.58, ' ', 122, 39.48, ' ', 0, 0.02, 0.4, 0.25, 8, 0.0, 0.0, 0, 0, 0));
		stationsList.add(new Station(' ', "SR12", 38, 33.95, ' ', 122, 46.20, ' ', 0, 0.19, 0.4, 0.25, 8, 0.0, 0.0, 0, 0, 0));
		stationsList.add(new Station(' ', "SR13", 38, 28.50, ' ', 122, 41.10, ' ', 0, -0.01, 0.4, 0.25, 8, 0.0, 0.0, 0, 0, 0));
		stationsList.add(new Station(' ', "SR14", 38, 23.08, ' ', 122, 49.38, ' ', 0, 0.01, 0.4, 0.25, 8, 0.0, 0.0, 0, 0, 0));
		stationsList.add(new Station(' ', "SR15", 38, 29.40, ' ', 122, 35.95, ' ', 0, 0.07, 0.4, 0.25, 8, 0.0, 0.0, 0, 0, 0));
		stationsList.add(new Station(' ', "SR16", 38, 32.02, ' ', 122, 58.55, ' ', 0, 0.04, 0.4, 0.25, 8, 0.0, 0.0, 0, 0, 0));
		stationsList.add(new Station(' ', "SR17", 38, 45.95, ' ', 122, 48.35, ' ', 0, 0, 0.4, 0.25, 8, 0.0, 0.0, 0, 0, 0));
		stationsList.add(new Station(' ', "SR18", 38, 17.75, ' ', 122, 44.48, ' ', 0, -0.11, 0.4, 0.25, 8, 0.0, 0.0, 0, 0, 0));
		stationsList.add(new Station(' ', "SR19", 38, 40.25, ' ', 122, 40.08, ' ', 0, -0.05, 0.4, 0.25, 8, 0.0, 0.0, 0, 0, 0));

		// Filling phase records list
		phaseRecordsList.add(new PhaseRecord("SR01", "IPD0", 0.0, 69100512, 6, 51.22, 0.0, "", 0.0, 0.0, 0.0, 0.0, 0.0, "", 0.0, 0.0, "1.22", 'D', "", "SR01IPD0 691005120651.22", ' ', "IPD0"));
		phaseRecordsList.add(new PhaseRecord("SR02", "IPU0", 0.0, 69100512, 6, 51.02, 0.0, "", 0.0, 0.0, 0.0, 0.0, 0.0, "", 0.0, 0.0, "1.02", 'U', "", "SR02IPU0 691005120651.02", ' ', "IPU0"));
		phaseRecordsList.add(new PhaseRecord("SR03", "IPD0", 0.0, 69100512, 6, 50.49, 0.0, "", 0.0, 0.0, 0.0, 0.0, 0.0, "", 0.0, 0.0, "0.49", 'D', "", "SR03IPD0 691005120650.49", ' ', "IPD0"));
		phaseRecordsList.add(new PhaseRecord("SR04", "IPU0", 0.0, 69100512, 6, 49.66, 0.0, "", 0.0, 0.0, 0.0, 0.0, 0.0, "", 0.0, 16.0, "9.66", 'U', "", "SR04IPU0 691005120649.66", ' ', "IPU0"));
		phaseRecordsList.add(new PhaseRecord("SR05", "IPU0", 0.0, 69100512, 6, 49.72, 53.7, "ES 2", 2.0, 0.0, 0.0, 0.0, 0.0, "", 0.0, 0.0, "9.72", 'U', "53.70", "SR05IPU0 691005120649.72", ' ', "IPU0"));
		phaseRecordsList.add(new PhaseRecord("SR06", "IPD0", 0.0, 69100512, 6, 50.10, 54.20, "ESN4", 4.0, 0.0, 0.0, 0.0, 0.0, "", 0.0, 0.0, "0.10", 'D', "54.20", "SR06IPD0 691005120650.10", ' ', "IPD0"));
		phaseRecordsList.add(new PhaseRecord("SR07", "IPD0", 0.0, 69100512, 6, 46.38, 0.0, "", 0.0, 0.0, 0.0, 0.0, 0.0, "", 0.0, 18.0, "6.38", 'D', "", "SR07IPD0 691005120646.38", ' ', "IPD0"));
		phaseRecordsList.add(new PhaseRecord("SR8A", "IPU0", 0.0, 69100512, 6, 48.09, 0.0, "", 0.0, 0.0, 0.0, 0.0, 0.0, "", 0.0, 0.0, "8.09", 'U', "", "SR8AIPU0 691005120648.09", ' ', "IPU0"));
		phaseRecordsList.add(new PhaseRecord("SR09", "IPU0", 0.0, 69100512, 6, 47.23, 0.0, "", 0.0, 0.0, 0.0, 0.0, 0.0, "", 0.0, 0.0, "7.23", 'U', "", "SR09IPU0 691005120647.23", ' ', "IPU0"));
		phaseRecordsList.add(new PhaseRecord("SR10", "IPU0", 0.0, 69100512, 6, 46.40, 0.0, "", 0.0, 0.0, 0.0, 0.0, 0.0, "", 0.0, 0.0, "6.40", 'U', "", "SR10IPU0 691005120646.40", ' ', "IPU0"));
		phaseRecordsList.add(new PhaseRecord("SR11", "IPD0", 0.0, 69100512, 6, 46.89, 0.0, "", 0.0, 0.0, 0.0, 0.0, 0.0, "", 0.0, 0.0, "6.89", 'D', "", "SR11IPD0 691005120646.89", ' ', "IPD0"));
		phaseRecordsList.add(new PhaseRecord("SR12", "IPD0", 0.0, 69100512, 6, 47.32, 0.0, "", 0.0, 0.0, 0.0, 0.0, 0.0, "", 0.0, 0.0, "7.32", 'D', "", "SR12IPD0 691005120647.32", ' ', "IPD0"));
		phaseRecordsList.add(new PhaseRecord("SR20", "IPD0", 0.0, 69100512, 6, 48.88, 0.0, "", 0.0, 0.0, 0.0, 0.0, 0.0, "", 0.0, 0.0, "8.88", 'D', "", "SR20IPD0 691005120648.88", ' ', "IPD0"));
		phaseRecordsList.add(new PhaseRecord("SR13", "IPD0", 0.0, 69100512, 6, 45.46, 0.0, "", 0.0, 0.0, 0.0, 0.0, 0.0, "", 0.0, 0.0, "5.46", 'D', "", "SR13IPD0 691005120645.46", ' ', "IPD0"));
		phaseRecordsList.add(new PhaseRecord("SR14", "IPD0", 0.0, 69100512, 6, 57.78, 0.0, "", 0.0, 0.0, 0.0, 0.0, 0.0, "", 0.0, 0.0, "7.78", 'D', "", "SR14IPD0 691005120657.78", ' ', "IPD0"));
		phaseRecordsList.add(new PhaseRecord("SR15", "IPU0", 0.0, 69100512, 6, 46.80, 0.0, "", 0.0, 0.0, 0.0, 0.0, 0.0, "", 0.0, 0.0, "6.80", 'U', "", "SR15IPU0 691005120646.80", ' ', "IPU0"));
		phaseRecordsList.add(new PhaseRecord("SR16", "IPU0", 0.0, 69100512, 6, 49.47, 0.0, "", 0.0, 0.0, 0.0, 0.0, 0.0, "", 0.0, 0.0, "9.47", 'U', "", "SR16IPU0 691005120649.47", ' ', "IPU0"));
		phaseRecordsList.add(new PhaseRecord("SR18", "IPD0", 0.0, 69100512, 6, 48.55, 0.0, "", 0.0, 0.0, 0.0, 0.0, 0.0, "", 0.0, 0.0, "8.55", 'D', "", "SR18IPD0 691005120648.55", ' ', "IPD0"));
		phaseRecordsList.add(new PhaseRecord("SR19", "IPD0", 0.0, 69100512, 6, 48.88, 0.0, "", 0.0, 0.0, 0.0, 0.0, 0.0, "", 0.0, 0.0, "8.88", 'D', "", "SR19IPD0 691005120648.88", ' ', "IPD0"));

		// Filling crustal model list
		crustalModelList.add(new CrustalModel(3.3, 0.0));
		crustalModelList.add(new CrustalModel(5.0, 1.0));
		crustalModelList.add(new CrustalModel(5.7, 4.0));
		crustalModelList.add(new CrustalModel(6.7, 15.0));
		crustalModelList.add(new CrustalModel(8.0, 25.0));

		try {
			hypoCalculator.calculateHypo71("SOME SANTA ROSA QUAKES FOR TESTING HYPO71", null, stationsList, crustalModelList, controlCard, phaseRecordsList, null);
			System.out.println(hypoCalculator.getResults().getOutput());
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static void runWithFileInput(String finputName) {
		try {
			Queue<Station> stationsList = new LinkedList<Station>();
			Queue<CrustalModel> crustalModelList = new LinkedList<CrustalModel>();
			Queue<PhaseRecord> phaseRecordsList = new LinkedList<PhaseRecord>();
			Hypo71 hypoCalculator = new Hypo71();
			hypoCalculator.calculateHypo71("", null, stationsList, crustalModelList, null, phaseRecordsList, finputName);
			System.out.println(hypoCalculator.getResults().getOutput());
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (FINPUT_READER != null) {
					FINPUT_READER.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
