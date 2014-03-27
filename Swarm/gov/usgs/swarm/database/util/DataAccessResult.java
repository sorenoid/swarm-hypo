package gov.usgs.swarm.database.util;

public interface DataAccessResult<T>
{
	public void done(T taskResult);
}