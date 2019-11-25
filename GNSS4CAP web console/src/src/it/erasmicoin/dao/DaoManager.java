package it.erasmicoin.dao;

import java.sql.Connection;
import java.sql.SQLException;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.log4j.Logger;

import it.erasmicoin.configurazione.ApplicationConfig;

public abstract class DaoManager{
	
	static Logger log = Logger.getLogger(DaoManager.class.toString());
	//JNDI
	protected final String jndiNameMySQL = ApplicationConfig.getDataSourceName1();
    	
	protected Connection getConnectionMySql() throws  SQLException, ClassNotFoundException, NamingException {
		
		DataSource	ds			= null;
		Context initialContext	= null;		
		Connection connection = null;
		try{
			log.error("jndi:"+jndiNameMySQL);
			initialContext = new InitialContext();
			ds=(DataSource)initialContext.lookup(jndiNameMySQL);
			connection = ds.getConnection();
			
		}catch(NamingException ne){
			throw ne;
		}
		return connection;		
	}
}