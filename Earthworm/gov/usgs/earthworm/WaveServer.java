package gov.usgs.earthworm;

import gov.usgs.earthworm.message.TraceBuf;
import gov.usgs.net.InternetClient;
import gov.usgs.util.Retriable;
import gov.usgs.util.UtilException;
import gov.usgs.vdx.data.wave.Wave;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

/**
 * A class for managing a connection to an Earthworm Wave Server.
 * 
 * @author Dan Cervelli
 */
public class WaveServer extends InternetClient {
	/**
	 * Method for constructing a WaveServer client class
	 * 
	 * @param sp
	 *            is an "ip-address:port-number" as a string of the wave-server to be connected with
	 */
	public WaveServer(String sp) {
		super(sp);
	}

	/**
	 * Method for constructing a WaveServer client class
	 * 
	 * @param s
	 *            is an ip address of the wave-server
	 * @param p
	 *            is the port number of the wave-server
	 */
	public WaveServer(String s, int p) {
		super(s, p);
	}

	public Menu getMenu() {
		return getMenu("ID");
	}

	public Menu getMenu(final String reqID) {
		Menu ret = null;
		Retriable<Menu> mr = new Retriable<Menu>("WaveServer.getMenu()", maxRetries) {
			public void attemptFix() {
				close();
			}

			public boolean attempt() throws UtilException {
				try {
					if (!connected())
						connect();
					writeString("MENU: " + reqID + "\n");
					String info = readString();
					result = new Menu(info);
					return true;
				} catch (Exception e) {
					logger.warning("getMenu() IOException: " + e.getMessage());
				}
				return false;
			}
		};
		try {
			ret = mr.go();
		} catch (UtilException e) {
			// Do nothing
		}
		return ret;
	}

	public Menu getMenuSCNL() {
		return getMenuSCNL("ID");
	}

	public Menu getMenuSCNL(final String reqID) {
		Menu ret = null;
		Retriable<Menu> mr = new Retriable<Menu>("WaveServer.getMenuSCNL()", maxRetries) {
			public void attemptFix() {
				close();
			}

			public boolean attempt() throws UtilException {
				try {
					if (!connected())
						connect();
					writeString("MENU: " + reqID + " SCNL\n");
					String info = readString();
					result = new Menu(info);
					return true;
				} catch (Exception e) {
					logger.warning("getMenuSCNL() IOException: " + e.getMessage());
				}
				return false;
			}
		};
		try {
			ret = mr.go();
		} catch (UtilException e) {
			// Do nothing
		}
		return ret;
	}

	public MenuItem getMenuItem(final int pin) {
		MenuItem ret = null;
		Retriable<MenuItem> rt = new Retriable<MenuItem>("WaveServer.getMenuItem()", maxRetries) {
			public void attemptFix() {
				close();
				connect();
			}

			public boolean attempt() throws UtilException {
				try {
					if (!connected())
						connect();
					writeString("MENUPIN: ID " + pin + "\n");
					String info = readString();
					result = new MenuItem(info.substring(4));
					return true;
				} catch (Exception e) {
					logger.warning("getMenuItem() IOException: " + e.getMessage());
				}
				return false;
			}
		};

		try {
			ret = rt.go();
		} catch (UtilException e) {
			// Do nothing
		}
		return ret;
	}

	public MenuItem getMenuItem(final String st, final String ch, final String nw) {
		MenuItem ret = null;
		Retriable<MenuItem> rt = new Retriable<MenuItem>("WaveServer.getMenuItem()", maxRetries) {
			public void attemptFix() {
				close();
				connect();
			}

			public boolean attempt() throws UtilException {
				try {
					if (!connected())
						connect();
					writeString("MENUSCN: ID " + st + " " + ch + " " + nw + "\n");
					String info = readString();
					result = new MenuItem(info.substring(4));
					return true;
				} catch (Exception e) {
					logger.warning("getMenuSCN() IOException: " + e.getMessage());
				}
				return false;
			}
		};
		try {
			ret = rt.go();
		} catch (UtilException e) {
			// Do nothing
		}
		return ret;
	}

	public String getAsciiData(final String station, final String comp, final String network, final double start,
			final double end) {
		String ret = null;
		Retriable<String> rt = new Retriable<String>("WaveServer.getAsciiData()", maxRetries) {
			public void attemptFix() {
				close();
			}

			public boolean attempt() throws UtilException {
				try {
					if (!connected())
						connect();

					String req = "GETSCN: GS " + station + " " + comp + " " + network + " " + start + " " + end
							+ " 0\n";
					writeString(req);
					result = readString();
					return true;
				} catch (Exception e) {
					logger.warning("getAsciiData() IOException: " + e.getMessage());
				}
				return false;
			}
		};
		try {
			ret = rt.go();
		} catch (UtilException e) {
			// Do nothing
		}
		return ret;
	}

	public Wave getRawData(String station, String comp, String network, String location, double start, double end) {

		List<TraceBuf> tbs = getTraceBufs(station, comp, network, location, start, end);
		return TraceBuf.traceBufToWave(tbs);
	}

	public Wave getRawData(String station, String comp, String network, double start, double end) {
		return getRawData(station, comp, network, null, start, end);
	}

	public List<TraceBuf> getTraceBufs(final String station, final String comp, final String network,
			final String location, final double start, final double end) {
		List<TraceBuf> ret = null;
		Retriable<List<TraceBuf>> rt = new Retriable<List<TraceBuf>>("WaveServer.getTraceBufs()", maxRetries) {
			public void attemptFix() {
				close();
			}

			public boolean attempt() throws UtilException {
				try {
					if (!connected())
						connect();
					int spanIndex = 5;
					int lengthIndex = 9;
					String cmd = "GETSCNRAW: GS ";
					String loc = null;
					boolean isTb2 = false;
					if (location != null) {
						cmd = "GETSCNLRAW: GS ";
						loc = location;
						isTb2 = true;
						spanIndex++;
						lengthIndex++;
					}

					String req = cmd + station + " " + comp + " " + network + " " + (loc == null ? "" : loc + " ")
							+ start + " " + end;
					socketOut.print(req + "\n");
					socketOut.flush();
					String info = readString();

					String[] ss = info.split(" ");

					// debugging code attempting to isolate reported problem with WSV
					if (ss.length < spanIndex + 1) {
						logger.warning("Can't parse server response.");
						logger.warning("Sent: " + req);
						logger.warning("Received:" + info);

						return false;
					}

					if (!ss[spanIndex].equals("F"))
						return true;
					int bytes = Integer.parseInt(ss[lengthIndex]);
					byte[] buf = readBinary(bytes);

					DataInputStream dis = new DataInputStream(new ByteArrayInputStream(buf));
					List<TraceBuf> tbs = new ArrayList<TraceBuf>(100);
					while (dis.available() > 0) {
						TraceBuf tb = new TraceBuf();
						tb.processBytes(dis, isTb2);
						tbs.add(tb);
					}
					result = tbs;
					return true;
				} catch (SocketTimeoutException e) {
					logger.warning("getTraceBufs() timeout.");
				} catch (Exception e) {
					logger.warning("getTraceBufs() exception: " + e.getMessage());
					e.printStackTrace();
				}
				return false;
			}
		};
		try {
			ret = rt.go();
		} catch (UtilException e) {
			// Do nothing
		}
		return ret;
	}

	public List<TraceBuf> getTraceBufs(final String station, final String comp, final String network,
			final double start, final double end) {
		return getTraceBufs(station, comp, network, null, start, end);
	}

	/**
	 * Method for returning the host wave-server ip and port as a string with "ip:port"
	 */
	public String toString() {
		return host + ":" + port;
	}

	public static void main(String[] args) {
		if (args.length == 2) {
			String server = args[0];
			int port = Integer.MAX_VALUE;
			try {
				port = Integer.parseInt(args[1]);
			} catch (Exception e) {
			}
			if (port == Integer.MAX_VALUE) {
				System.err.println("Bad port.");
				System.exit(1);
			}
			WaveServer ws = new WaveServer(server, port);
			if (!ws.connect()) {
				System.err.println("Could not connect to " + server + ":" + port);
				System.exit(1);
			}
			System.out.println("Getting menu...");
			Menu menu = ws.getMenu();
			System.out.println(menu.toString());
			ws.close();
		} else {
			System.out.println("java gov.usgs.earthworm.WaveServer [host] [port]");
			System.exit(1);
		}
		System.exit(0);
	}
}
