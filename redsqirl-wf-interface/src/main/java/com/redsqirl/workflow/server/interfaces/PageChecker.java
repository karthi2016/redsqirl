package com.redsqirl.workflow.server.interfaces;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface PageChecker extends Remote,Serializable{
	
	/**
	 * Check the page 
	 * @return null if OK, or a short description of the error
	 * @throws RemoteException
	 */
	public String check(DFEPage page) throws RemoteException;

}
