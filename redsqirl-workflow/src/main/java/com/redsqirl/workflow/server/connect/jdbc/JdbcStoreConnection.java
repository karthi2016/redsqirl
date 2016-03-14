package com.redsqirl.workflow.server.connect.jdbc;

import java.net.URL;
import java.rmi.RemoteException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.idiro.utils.db.BasicStatement;
import com.idiro.utils.db.JdbcConnection;
import com.idiro.utils.db.JdbcDetails;
import com.redsqirl.workflow.server.connect.jdbc.JdbcQueryManager.Query;

public class JdbcStoreConnection extends JdbcConnection{

	private static Logger logger = Logger.getLogger(JdbcStoreConnection.class);
	protected List<String> tables;

	// Refresh every 3 seconds
	/** Refresh count */
	protected static final long refreshTimeOut = 20000;
	protected static long updateTables = 0;
	protected static boolean listing = false;
	
	public JdbcStoreConnection(JdbcDetails arg0, RedSqirlBasicStatement arg1)
			throws Exception {
		super(arg0, (BasicStatement) arg1);
		setMaxTimeInMinuteBeforeCleaningStatement(2);
	}
	
	public JdbcStoreConnection(URL jarPath, String driverClassname,
			JdbcDetails connectionDetails, RedSqirlBasicStatement bs) throws Exception {
		super(jarPath, driverClassname, connectionDetails, (BasicStatement) bs);
		setMaxTimeInMinuteBeforeCleaningStatement(2);
	}
	
	public JdbcStoreConnection(String driverClassname,
			JdbcDetails connectionDetails, RedSqirlBasicStatement bs) throws Exception {
		super(driverClassname, connectionDetails, (BasicStatement) bs);
		setMaxTimeInMinuteBeforeCleaningStatement(2);
	}

	public final List<String> listTables() throws SQLException, RemoteException {
		if(!listing){
			if (tables == null || refreshTimeOut < System.currentTimeMillis() - updateTables) {
				listing = true;
				tables = execListTables();
				updateTables = System.currentTimeMillis();
				listing = false;
			}
		}else{
			while(listing){
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
				}
			}
		}
		if(tables != null){
			logger.info("tables on "+connectionDetails.getDburl()+": "+tables.toString());
		}
		return tables;
	}
	
	protected final List<String> execListTables() throws SQLException, RemoteException {
		
		List<String> results = new ArrayList<String>();
		String query = getBs().showAllTables();
		ResultSet rs = null;
		if(query ==  null || query.isEmpty()){
			rs = connection.getMetaData().getTables(null, null, null, new String[] {"TABLE"});
		}else{
			rs = executeQuery(query);
		}
		while (rs.next()) {
			results.add(rs.getString(1).trim().toUpperCase());
		}
		rs.close();
		
		return results;
	}
	
	protected String execDesc(String table){
		String fieldsStr = null;
		try {
			String query = getBs().showFeaturesFrom(table);
			ResultSet rs = null;
			int nameIdx = 1;
			int typeIdx = 2;
			if(query == null || query.isEmpty()){
				rs = connection.getMetaData().getColumns(null, null, table, null);
				nameIdx = 4;
				typeIdx = 6;
			}else{
				rs = executeQuery(query);
			}
			int i = 0;
			Integer parts = 0;
			boolean fieldPart = true;
			while (rs.next()) {
				boolean ok = true;
				String name = rs.getString(nameIdx).toUpperCase();
				String type = rs.getString(typeIdx).toUpperCase();
				if (name == null || name.isEmpty() || name.contains("#")
						|| type == null) {
					logger.debug("name is null " + name == null + ", " + name);
					logger.debug("name is empty " + name.isEmpty());
					logger.debug("type is null " + type == null + " , " + type);
					ok = false;
					fieldPart = false;
				}
				if (ok) {
					if (type.equalsIgnoreCase("null")) {
						ok = false;
					}
				}
				if (ok) {
					if (fieldPart) {
						if (i == 0) {
							fieldsStr = "";
							fieldsStr += name.trim() + "," + type.trim();
						} else {
							fieldsStr += ";" + name.trim() + "," + type.trim();
						}
					} else {
						if (name != null && !name.isEmpty()
								&& !name.contains("#") && type != null) {
							++parts;
						}
					}
					++i;
				}
			}
			rs.close();

		} catch (Exception e) {
			logger.error("Fail to check the existence " + table,e);
		}
		return fieldsStr;
	}
	
	public void resetUpdateTables(){
		updateTables = 0;
	}
	
	public String getConnType() throws RemoteException{
		return getConnType(connectionDetails.getDburl());
	}
	
	public static String getConnType(String url) throws RemoteException{
		if(url.startsWith("jdbc:")){
			url = url.substring(5);
		}
		String ans = url.substring(0, url.indexOf(":"));
		if("hive2".equals(ans)){
			ans = "hive";
		}
		logger.info(ans);
		return ans;
	}
	
	public RedSqirlBasicStatement getRsBs(){
		return (RedSqirlBasicStatement) getBs();
	}

}