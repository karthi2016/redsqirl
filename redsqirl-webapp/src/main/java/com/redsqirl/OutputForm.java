/** 
 *  Copyright © 2016 Red Sqirl, Ltd. All rights reserved.
 *  Red Sqirl, Clarendon House, 34 Clarendon St., Dublin 2. Ireland
 *
 *  This file is part of Red Sqirl
 *
 *  User agrees that use of this software is governed by: 
 *  (1) the applicable user limitations and specified terms and conditions of 
 *      the license agreement which has been entered into with Red Sqirl; and 
 *  (2) the proprietary and restricted rights notices included in this software.
 *  
 *  WARNING: THE PROPRIETARY INFORMATION OF Red Sqirl IS PROTECTED BY IRISH AND 
 *  INTERNATIONAL LAW.  UNAUTHORISED REPRODUCTION, DISTRIBUTION OR ANY PORTION
 *  OF IT, MAY RESULT IN CIVIL AND/OR CRIMINAL PENALTIES.
 *  
 *  If you have received this software in error please contact Red Sqirl at 
 *  support@redsqirl.com
 */

package com.redsqirl;


import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import com.redsqirl.workflow.server.enumeration.SavingState;
import com.redsqirl.workflow.server.interfaces.DFELinkOutput;
import com.redsqirl.workflow.server.interfaces.DFEOutput;

public class OutputForm implements Serializable {

	private static Logger logger = Logger.getLogger(OutputForm.class);

	private DFEOutput dfeOutput;
	private String componentId;
	private String name;
	private List<SelectItem> savingStateList = new ArrayList<SelectItem>();
	private List<String> savingStateListString = new ArrayList<String>();
	private boolean renderBrowserButton = false;
	private String savingState;
	private String path;
	private String file;
	private String user;
	private String link;
	private Map<String, FileSystemBean> datastores;
	
	public String showGridDataOutput = "Y";
	
	public OutputForm(Map<String, FileSystemBean> datastores, DFEOutput dfeOutput, String componentId, String name) throws RemoteException{
		this.datastores = datastores;
		this.dfeOutput = dfeOutput;
		this.componentId = componentId;
		this.name = name;
		
		HttpSession session = (HttpSession) FacesContext.getCurrentInstance().
				getExternalContext().getSession(false);
		this.user = (String) session.getAttribute("username");
		
		if (dfeOutput.getSavingState() == null){
			dfeOutput.setSavingState(SavingState.TEMPORARY);
		}
		setSavingState(dfeOutput.getSavingState().toString());
		
		try{
			setLink(((DFELinkOutput) dfeOutput).getLink());
			logger.info("link: "+getLink());
		}catch(Exception e){
			//logger.info(e,e);
		}
		
		List<Map<String, String>> outputLines = dfeOutput.select(1);
		if(outputLines != null && !outputLines.isEmpty()){
			setShowGridDataOutput("Y");
		}else{
			setShowGridDataOutput("N");
		}
		
	}

	public OutputForm() {
		super();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<SelectItem> getSavingStateList() {
		return savingStateList;
	}

	public void setSavingStateList(List<SelectItem> outputList) {
		this.savingStateList = outputList;
	}

	public boolean isRenderBrowserButton() {
		return renderBrowserButton;
	}

	public void setRenderBrowserButton(boolean renderButton) {
		this.renderBrowserButton = renderButton;
	}

	public String getPath() throws RemoteException {
		return path;
	}

	public void setPath(String path) throws RemoteException {
		this.path = path;
	}

	public String getFile() {
		return file;
	}

	public void setFile(String file) {
		this.file = file;
	}

	public DFEOutput getDfeOutput() {
		return dfeOutput;
	}

	public void setDfeOutput(DFEOutput dfeOutput) {
		this.dfeOutput = dfeOutput;
	}

	public boolean isHiveBrowser() throws RemoteException{
		return dfeOutput.getBrowserName().equals("HIVE");
	}

	public boolean isHdfsBrowser() throws RemoteException{
		return dfeOutput.getBrowserName().equals("HDFS");
	}

	public String getSavingState() {
		return savingState;
	}

	public void setSavingState(String savingState) throws RemoteException {
		this.savingState = SavingState.valueOf(savingState).name();
		if (savingState.equals(SavingState.RECORDED.toString())){
			logger.info("Recorded");
			setRenderBrowserButton(true);
			//setPath("/");
		}else if (savingState.equals(SavingState.BUFFERED.toString()) ||
				savingState.equals(SavingState.TEMPORARY.toString())){
			setRenderBrowserButton(false);
			
			if(getDfeOutput().getPath() == null || (SavingState.RECORDED.equals(getDfeOutput().getSavingState()) &&
					!getDfeOutput().isPathAutoGeneratedForUser(getComponentId(), getName()))){
				
				getDfeOutput().generatePath(
						getComponentId(), 
						getName());
			}
			setPath(getDfeOutput().getPath());
		}
	}

	public void updateSavingState() throws RemoteException {
		if (savingState.equals(SavingState.RECORDED.toString())){
			setPath("/");
		}
	}

	public String getComponentId() {
		return componentId;
	}

	public void setComponentId(String componentId) {
		this.componentId = componentId;
	}

	public List<String> getNameOutputs(){
		List<String> list = new ArrayList<String>();
		list.add("Output1");
		list.add("Output2");
		return list;
	}

	public String updateDFEOutput() throws RemoteException{
		
		if (getSavingState().equals(SavingState.RECORDED.toString())) {
			if ( getFile() == null || getFile().isEmpty()) {
				return "Path cannot be null";
			}

			String completePath = getPath();
			if (!getPath().endsWith("/")) {
				completePath += "/";
			}
			completePath += getFile();
			logger.info("path: " + completePath);
			
			Map<String,String> props = datastores.get(dfeOutput.getBrowserName()).getDataStore().getProperties(completePath);
			try{
				if(dfeOutput.isPathExist() && dfeOutput.getSavingState() != SavingState.RECORDED){
					if(props == null || props.isEmpty()){
						dfeOutput.moveTo(completePath);
					}else{
						dfeOutput.remove();
						dfeOutput.setPath(completePath);
					}
				}else{
					dfeOutput.setPath(completePath);
				}
			}catch(Exception e){
				logger.warn("Unexpected excepiton: "+e,e);
				return "An unexpected error occured, please try again";
			}
		}
		
		dfeOutput.setSavingState(SavingState.valueOf(getSavingState()));

		return dfeOutput.isPathValid();
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public Map<String, FileSystemBean> getDatastores() {
		return datastores;
	}

	public void setDatastores(Map<String, FileSystemBean> datastores) {
		this.datastores = datastores;
	}

	public List<String> getSavingStateListString() {
		return savingStateListString;
	}

	public void setSavingStateListString(List<String> savingStateListString) {
		this.savingStateListString = savingStateListString;
	}

	public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
	}

	public String getShowGridDataOutput() {
		return showGridDataOutput;
	}

	public void setShowGridDataOutput(String showGridDataOutput) {
		this.showGridDataOutput = showGridDataOutput;
	}
	
}