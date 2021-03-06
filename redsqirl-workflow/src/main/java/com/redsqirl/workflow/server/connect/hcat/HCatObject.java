package com.redsqirl.workflow.server.connect.hcat;

import java.rmi.RemoteException;
import java.util.Set;

import com.redsqirl.workflow.server.connect.jdbc.JdbcStoreConnection;

public abstract class HCatObject {
	
	// Refresh every 5 seconds
	/** Refresh count */
	protected static final long refreshTimeOut = 10000;		
	Set<String> listObjects = null;
	long databaseLastUpdate;

	
	protected static JdbcStoreConnection getHiveConnection() throws RemoteException{
		return HCatStore.getHiveConnection();
	}
	
	public Set<String> listObjects(){
		if(listObjects == null || refreshTimeOut < System.currentTimeMillis() - databaseLastUpdate){
			listObjects = listObjectsPriv();
			databaseLastUpdate = System.currentTimeMillis();
		}
		return listObjects;
	}
	
	public boolean removeObject(String objName){
		return listObjects == null? false:listObjects.remove(objName);
	}
	
	protected abstract Set<String> listObjectsPriv();

}
