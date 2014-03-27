package gov.usgs.swarm.database.util;

import java.sql.SQLException;

public interface DataAccessTask<T, V>
{
	T doInBackground(DataAccessSwingWorker<T, V> workerThread) throws Exception;

	void done(T taskResult);

	void cancel() throws SQLException;
}