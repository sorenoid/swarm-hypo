package gov.usgs.swarm.database.util;

import gov.usgs.swarm.Swarm;

import javax.sql.DataSource;
import javax.swing.JOptionPane;

import org.eclipse.persistence.config.SessionCustomizer;
import org.eclipse.persistence.descriptors.ClassDescriptor;
import org.eclipse.persistence.sessions.JNDIConnector;
import org.eclipse.persistence.sessions.Session;
import org.h2.jdbcx.JdbcDataSource;

public class SessionCustomize implements SessionCustomizer {
	  public void customize(Session session) {
	    session.getLogin().setConnector(new JNDIConnector(getDataSource()));
	 
	    for (ClassDescriptor descriptor : session.getDescriptors().values()) {
	      descriptor.getCachePolicy().setIdentityMapSize(1000);
	    }
	  }
	  public static DataSource getDataSource() {
		  	String dbName = null;
		  	do{
		  		dbName = JOptionPane.showInputDialog("Please enter the database name:");
		  	}while(null == dbName || "".equalsIgnoreCase(dbName.trim()));
		  	
	  		JdbcDataSource dataSource = new JdbcDataSource();	
			dataSource.setURL("jdbc:h2:"+dbName);
			dataSource.setUser(Swarm.USER);
			dataSource.setPassword(Swarm.PASSWORD);
			return dataSource;
		}
}
