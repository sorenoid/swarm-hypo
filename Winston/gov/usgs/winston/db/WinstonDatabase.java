package gov.usgs.winston.db;

import gov.usgs.util.ConfigFile;
import gov.usgs.util.Log;
import gov.usgs.util.Retriable;
import gov.usgs.util.Util;
import gov.usgs.util.UtilException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A class that manages a connection to a Winston database.
 * 
 * TODO: set default st and et in channels table to 1E300, -1E300.
 * 
 * @author Dan Cervelli
 */
public class WinstonDatabase {
	public static final String WINSTON_TABLE_DATE_FORMAT = "yyyy_MM_dd";
	public static final String CURRENT_SCHEMA_VERSION = "1.1.1";
	private static final String DEFAULT_DATABASE_PREFIX = "W";
	private static final String DEFAULT_CONFIG_FILENAME = "Winston.config";
	private static final int DEFAULT_CACHE_CAPACITY = 100;

	/** Connection to the Winston */
	private Connection winstonConnection;

	/** Statement for interacting with the Winston. */
	private Statement winstonStatement;

	/** The connection state with the Winston. */
	private boolean winstonConnected;

	public final String dbDriver;
	public final String dbURL;
	public final int cacheCap;
	public final String databasePrefix;
	public final String tableEngine;

	private Logger logger;

	private PreparedStatementCache preparedStatements;

	public WinstonDatabase(String dbDriver, String dbURL, String databasePrefix) {
		this(dbDriver, dbURL, databasePrefix, DEFAULT_CACHE_CAPACITY);
	}

	public WinstonDatabase(String dbDriver, String dbURL, String databasePrefix, int cacheCap) {
		this(dbDriver, dbURL, databasePrefix, null, cacheCap);
	}

	public WinstonDatabase(String dbDriver, String dbURL, String databasePrefix, String tableEngine, int cacheCap) {
		logger = Log.getLogger("gov.usgs.winston");

		// Set default Locale to US. This ensures that decimals play well with the SQL standard. ie. no decimal comma
		Locale.setDefault(Locale.US);

		this.dbDriver = dbDriver;
		this.dbURL = dbURL;
		this.cacheCap = cacheCap;
		this.databasePrefix = Util.stringToString(databasePrefix, DEFAULT_DATABASE_PREFIX);
		this.tableEngine = (tableEngine == null) ? "" : ("ENGINE = " + tableEngine);

		preparedStatements = new PreparedStatementCache(this.cacheCap, true);
		connect();
	}

	public Logger getLogger() {
		return logger;
	}

	private void connect() {
		winstonConnected = false;
		try {
			Class.forName(dbDriver).newInstance();
			DriverManager.setLoginTimeout(3);
			winstonConnection = DriverManager.getConnection(dbURL);
			winstonStatement = winstonConnection.createStatement();
			winstonConnected = true;
			preparedStatements.clear();
			logger.log(Level.INFO, "Connected to database.");
		} catch (ClassNotFoundException e) {
			logger.log(Level.SEVERE, "Could not load the database driver, check your CLASSPATH.",
					Util.getLineNumber(this, e));
			System.exit(-1);
		} catch (Exception e) {
			winstonConnection = null;
			winstonStatement = null;
			logger.log(Level.SEVERE, "Could not connect to Winston.", e);
			winstonConnected = false;
		}
	}

	public void close() {
		if (!checkConnect())
			return;

		try {
			winstonStatement.close();
			winstonConnection.close();
			winstonConnected = false;
		} catch (Exception e) {
			logger.warning("Error closing database.  This is unusual, but not critical.");
		}
	}

	public boolean checkConnect() {
		if (winstonConnected)
			return true;
		else {
			try {
				new Retriable<Object>() {
					public boolean attempt() throws UtilException {
						connect();
						return winstonConnected;
					}
				}.go();
			} catch (UtilException e) {
				// Do nothing
			}
			return winstonConnected;
		}
	}

	public boolean connected() {
		return winstonConnected;
	}

	public Connection getConnection() {
		return winstonConnection;
	}

	public Statement getStatement() {
		return winstonStatement;
	}

	public Statement getNewStatement() throws SQLException {
		return winstonConnection.createStatement();
	}

	public String getSchemaVersion() {
		useRootDatabase();
		String sv = null;
		try {
			ResultSet rs = winstonStatement
					.executeQuery("SELECT schemaversion FROM version ORDER BY installtime DESC LIMIT 1");
			rs.next();
			sv = rs.getString(1);
		} catch (SQLException e) {
			sv = "1.0.0";
		}
		return sv;
	}

	public boolean execute(final String sql) {
		Boolean b = null;
		try {
			b = new Retriable<Boolean>() {
				public void attemptFix() {
					close();
					connect();
				}

				public boolean attempt() throws UtilException {
					try {
						winstonStatement.execute(sql);
						result = new Boolean(true);
						return true;
					} catch (SQLException e) {
						logger.log(Level.SEVERE, "execute() failed, SQL: " + sql, e);
					}
					result = new Boolean(false);
					return false;
				}
			}.go();
		} catch (UtilException e) {
			// Do nothing
		}
		return b != null && b.booleanValue();
	}

	public ResultSet executeQuery(final String sql) {
		ResultSet rs = null;
		try {
			rs = new Retriable<ResultSet>() {
				public void attemptFix() {
					close();
					connect();
				}

				public boolean attempt() throws UtilException {
					try {
						result = winstonStatement.executeQuery(sql);
						return true;
					} catch (SQLException e) {
						logger.log(Level.SEVERE, "executeQuery() failed, SQL: " + sql, e);
					}
					return false;
				}
			}.go();
		} catch (UtilException e) {
			// Do nothing
		}
		return rs;
	}

	private void createTables() {
		try {
			getStatement().execute(
					"CREATE TABLE instruments (" + "iid INT PRIMARY KEY AUTO_INCREMENT," + "name VARCHAR(255) UNIQUE, "
							+ "description VARCHAR(255), " + "lon DOUBLE DEFAULT -999, " + "lat DOUBLE DEFAULT -999, "
							+ "height DOUBLE DEFAULT -999, " + "timezone VARCHAR(128)) " + tableEngine);

			getStatement().execute(
					"CREATE TABLE channels (" + "sid INT PRIMARY KEY AUTO_INCREMENT, " + "iid INT, "
							+ "code VARCHAR(50), " + "st DOUBLE, " + "et DOUBLE, " + "alias VARCHAR(255), "
							+ "unit VARCHAR(255), " + "linearA DOUBLE DEFAULT 1E300, "
							+ "linearB DOUBLE DEFAULT 1E300) " + tableEngine);

			getStatement().execute(
					"CREATE TABLE version (" + "schemaversion VARCHAR(10), " + "installtime DATETIME) " + tableEngine);

			getStatement().execute("INSERT INTO version VALUES ('" + CURRENT_SCHEMA_VERSION + "', NOW())");

			getStatement().execute(
					"CREATE TABLE grouplinks (glid INT PRIMARY KEY AUTO_INCREMENT, " + "sid INT, nid INT) "
							+ tableEngine);

			getStatement().execute(
					"CREATE TABLE groupnodes (nid INT PRIMARY KEY AUTO_INCREMENT, " + "parent INT DEFAULT 0, "
							+ "name CHAR(255), " + "open BOOL DEFAULT 0) " + tableEngine);

			getStatement().execute(
					"CREATE TABLE channelmetadata (" + "sid INT, " + "name VARCHAR(255), " + "value TEXT, "
							+ "PRIMARY KEY (sid, name)) " + tableEngine);

			getStatement().execute(
					"CREATE TABLE instrumentmeetadata (imid INT PRIMARY KEY AUTO_INCREMENT, " + "iid INT, "
							+ "name VARCHAR(255), " + "value TEXT) " + tableEngine);

			getStatement().execute(
					"CREATE TABLE supp_data (sdid INT NOT NULL AUTO_INCREMENT, "
							+ "st DOUBLE NOT NULL, et DOUBLE, sdtypeid INT NOT NULL, "
							+ "sd_short VARCHAR(90) NOT NULL, sd TEXT NOT NULL, PRIMARY KEY (sdid)) " + tableEngine);

			getStatement()
					.execute(
							"CREATE TABLE supp_data_type (sdtypeid INT NOT NULL AUTO_INCREMENT, "
									+ "supp_data_type VARCHAR(20), supp_color VARCHAR(6) NOT NULL, draw_line TINYINT, PRIMARY KEY (sdtypeid), UNIQUE KEY (supp_data_type) ) "
									+ tableEngine);

			getStatement().execute(
					"CREATE TABLE supp_data_xref ( sdid INT NOT NULL, cid INT NOT NULL, " + "UNIQUE KEY (sdid,cid) ) "
							+ tableEngine);
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Could not create tables in WWS database.  Are permissions set properly?", e);
		}
	}

	public boolean useRootDatabase() {
		return useDatabase("ROOT");
	}

	public boolean useDatabase(String db) {
		if (!checkConnect())
			return false;

		try {
			try {
				winstonStatement.execute("USE `" + databasePrefix + "_" + db + "`");
			} catch (SQLException e) {
				close();
				connect();
			}
			winstonStatement.execute("USE `" + databasePrefix + "_" + db + "`");
			return true;
		} catch (SQLException e) {
			if (e.getMessage().indexOf("Unknown database") != -1)
				logger.log(Level.INFO, "Attempt to use nonexistent database: " + db);
			else
				logger.log(Level.SEVERE, "Could not use database: " + db, e);
		}
		return false;
	}

	public boolean checkDatabase() {
		if (!checkConnect())
			return false;

		try {
			boolean failed = false;
			try {
				getStatement().execute("USE " + databasePrefix + "_ROOT");
			} catch (Exception e) {
				failed = true;
			}
			if (failed) {
				getStatement().execute("CREATE DATABASE `" + databasePrefix + "_ROOT`");
				getStatement().execute("USE `" + databasePrefix + "_ROOT`");
				logger.info("Created new Winston database: " + databasePrefix);
				createTables();
			}
			return true;
		} catch (Exception e) {
			logger.severe("Could not locate or create WWS database.  Are permissions set properly?");
		}
		return false;
	}

	public boolean tableExists(String db, String table) {
		try {
			ResultSet rs = getStatement().executeQuery(
					String.format("SELECT COUNT(*) FROM `%s_%s`.%s", databasePrefix, db, table));
			boolean result = rs.next();
			rs.close();
			return result;
		} catch (Exception e) {
		}
		return false;
	}

	public PreparedStatement getPreparedStatement(String sql) {
		try {
			PreparedStatement ps = (PreparedStatement) preparedStatements.get(sql);
			if (ps == null) {
				ps = winstonConnection.prepareStatement(sql);
				preparedStatements.put(sql, ps);
				logger.finest(String.format("Adding statement to cache(%d/%d): %s", preparedStatements.size(),
						preparedStatements.maxSize(), sql));
			}
			return ps;
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Could not prepare statement.", e);
		}
		return null;
	}

	public static WinstonDatabase processWinstonConfigFile() {
		return processWinstonConfigFile(new ConfigFile(DEFAULT_CONFIG_FILENAME));
	}

	public static WinstonDatabase processWinstonConfigFile(ConfigFile cf) {
		String dbDriver = cf.getString("winston.driver");
		String dbURL = cf.getString("winston.url");
		String databasePrefix = cf.getString("winston.prefix");
		String tableEngine = cf.getString("winston.tableEngine");
		int cacheCap = Util.stringToInt(cf.getString("winston.statementCacheCap"), DEFAULT_CACHE_CAPACITY);

		return new WinstonDatabase(dbDriver, dbURL, databasePrefix, tableEngine, cacheCap);
	}
}
