package gov.usgs.swarm.database.managers;

import gov.usgs.swarm.database.util.DataAccessSwingWorker;
import gov.usgs.swarm.database.util.DataAccessTask;

public class AbstractDataAccessManager
{
	protected <T, V> void executeLongOperation(DataAccessTask<T, V> dataAccessTask)
	{
		new DataAccessSwingWorker<T, V>(dataAccessTask, true).execute();
	}

	protected <T, V> void executeLongOperationWithoutProgressBar(DataAccessTask<T, Void> dataAccessTask)
	{
		new DataAccessSwingWorker<T, Void>(dataAccessTask, false).execute();
	}
}