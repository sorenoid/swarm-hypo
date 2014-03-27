package gov.usgs.swarm.database.managers;

import gov.usgs.swarm.database.ScriptRunner;
import gov.usgs.swarm.database.model.ApplicationConnection;
import gov.usgs.swarm.database.util.DataAccessResult;
import gov.usgs.swarm.database.util.DataAccessSwingWorker;
import gov.usgs.swarm.database.util.DataAccessTask;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.SQLException;

public class ApplicationConnectionManager extends AbstractDataAccessManager
{

	private ApplicationConnectionManager()
	{}

	public void establishConnection(final String dbUrl, final String userName, final String userPassword,
			final DataAccessResult<ApplicationConnection> dataAccessResult) throws SQLException
	{
		super.executeLongOperationWithoutProgressBar(new DataAccessTask<ApplicationConnection, Void>()
		{
			@Override
			public ApplicationConnection doInBackground(DataAccessSwingWorker<ApplicationConnection, Void> workerThread)
					throws Exception
			{
				ApplicationConnection appConnection = new ApplicationConnection(dbUrl, userName, userPassword);
				ScriptRunner scriptRunner = new ScriptRunner(appConnection.getConnection(), false, false);
				scriptRunner.runScript(new BufferedReader(new FileReader("gov/usgs/swarm/database/h2DB-create.txt")));
				return appConnection;
			}

			@Override
			public void done(ApplicationConnection applicationConnection)
			{
				dataAccessResult.done(applicationConnection);
			}

			@Override
			public void cancel() throws SQLException
			{}
		});
	}

	/**
	 * This method is only to test what tables database contains
	 * 
	 * @param connection
	 * @throws SQLException
	 */
	// private void printDatabase(final Connection connection) throws
	// SQLException
	// {
	// Statement stat = connection.createStatement();
	// ResultSet rs = stat.executeQuery("Show tables");
	//
	// System.out.println("Creation DB Done , tables created :");
	// while (rs.next()) {
	// System.out.println(rs.getString("table_name"));
	// }
	// stat.close();
	// }

	/**
	 * @return new instance of ApplicationConnectionManager at every call
	 */
	public static ApplicationConnectionManager instance()
	{
		return new ApplicationConnectionManager();
	}
}