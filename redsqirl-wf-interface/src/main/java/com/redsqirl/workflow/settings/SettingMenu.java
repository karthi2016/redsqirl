package com.redsqirl.workflow.settings;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.redsqirl.workflow.server.WorkflowPrefManager;

public class SettingMenu {

	protected Map<String,Setting> properties = new LinkedHashMap<String,Setting>();
	protected Map<String,SettingMenu> menu = new LinkedHashMap<String,SettingMenu>();
	private static Logger logger = Logger.getLogger(SettingMenu.class);
	protected Setting.Scope scopeMenu;
	
	public SettingMenu(){
	}
	
	public SettingMenu(String path,JSONObject json){
		read(path,json);
	}
	
	protected SettingMenu(JSONObject json, String path, 
			Properties sysProperties, Properties userProperties, Properties langProperties){
		read(json, path,sysProperties, userProperties, langProperties);
	}
	
	public boolean isTemplate(){
		return false;
	}
	
	public void read(String path,JSONObject json){
		Properties sysProperties = WorkflowPrefManager.getSysProperties(); 
		Properties userProperties = WorkflowPrefManager.getUserProperties(); 
		Properties langProperties  = WorkflowPrefManager.getLangProperties();
		read(json, path,sysProperties, userProperties, langProperties);
	}
	
	protected void read(JSONObject json, String path, 
			Properties sysProperties, Properties userProperties, Properties langProperties){
		try{
			readSettings((JSONArray) json.get("settings"),path,sysProperties, userProperties, langProperties);
		}catch(Exception e){
			logger.warn(e,e);
		}
		readMenu(json, path,sysProperties, userProperties, langProperties);
	}
	
	protected void readMenu(JSONObject json, String path, 
			Properties sysProperties, Properties userProperties, Properties langProperties){
		try{
			JSONArray tabsArray = (JSONArray) json.get("tabs");
			for(int i = 0; i < tabsArray.length();++i){
				JSONObject tabObj = tabsArray.getJSONObject(i);
				String tabName = null;
				try{
					tabName = tabObj.getString("name");
				}catch(Exception e){}
				if(tabName != null){
					String newPath = path+"."+tabName;
					menu.put(tabName, new SettingMenu(tabObj,newPath,sysProperties,userProperties,langProperties));
				}else{
					String templateName = null;
					try{
						templateName = tabObj.getString("template_name");
					}catch(Exception e){}
					if(templateName != null){
						String newPath = path+"."+templateName;
						menu.put(templateName, new TemplateSettingMenu(tabObj,newPath,sysProperties,userProperties,langProperties));
					}
				}
			}
		}catch(Exception e){
		}
	}
	public void deleteAllProperties(){
		deleteProperties(true,true);
	}
	
	public void deleteAllUserProperties(){
		deleteProperties(true,false);
	}
	
	public void deleteAllSysProperties(){
		deleteProperties(false,true);
	}
	
	public void deleteProperties(boolean user, boolean sys){
		Iterator<Setting> it = properties.values().iterator();
		Properties sysProp = null;
		Properties userProp = null;
		if(user){
			userProp = WorkflowPrefManager.getUserProperties();
		}
		if(sys){
			sysProp = WorkflowPrefManager.getSysProperties();
		}
		
		while(it.hasNext()){
			String propName = it.next().getPropertyName();
			if(user && userProp != null){
				userProp.remove(propName);
			}
			if(sys && sysProp != null){
				sysProp.remove(propName);
			}
		}
		if(user && userProp != null){
			try {
				WorkflowPrefManager.storeUserProperties(userProp);
			} catch (IOException e) {
				logger.warn(e,e);
			}
		}
		if(sys && sysProp != null){
			try{
				WorkflowPrefManager.storeSysProperties(sysProp);
			} catch (IOException e) {
				logger.warn(e,e);
			}
		}
	}
	
	protected Setting.Scope getClearScope(){
		return null;
	}
	
	public String getPropertyValue(String name){
		try{
			if(name.contains(".")){
				String[] splitArr = name.split("\\.", 2);
				logger.info(menu.keySet());
				return !menu.containsKey(splitArr[0]) ? null : menu.get(splitArr[0]).getPropertyValue(splitArr[1]);
			}else{
				logger.info(properties.keySet());
				return !properties.containsKey(name) ? null : properties.get(name).getValue();
			}
		}catch(Exception e){
			logger.warn("Property "+name+" not found. "+e,e);
		}
		return null;
	}
	
	protected void readSettings(JSONArray settingArray, String path, 
			Properties sysProperties, Properties userProperties, Properties langProperties){
		scopeMenu = Setting.Scope.USER;
		for(int i = 0; i < settingArray.length();++i){
			
			try{
				JSONObject settingObj = settingArray.getJSONObject(i);
				String property = settingObj.getString("property");
				String defaultValue =  settingObj.getString("default");
				Setting.Scope scope =  readScope(settingObj);
				Setting.Type type =  readType(settingObj);
				Setting.Checker validator = readChecker(settingObj);

				String propertyName = path+"."+property;
				properties.put(property, new Setting(scope,defaultValue,type,validator));
				properties.get(property).setDescription(langProperties.getProperty(propertyName+"_desc",propertyName));
				properties.get(property).setLabel(langProperties.getProperty(propertyName+"_label",propertyName));
				properties.get(property).setPropertyName(propertyName);
			}catch(Exception e){
				logger.warn(e,e);
			}
		}
	}
	
	protected Setting.Scope readScope(JSONObject setting){
		
		String scope = null;
		try{
			scope = setting.getString("scope");
		}catch(Exception e){
		}
		
		if(scope == null){
			return Setting.Scope.ANY;
		}
		try{
			return Setting.Scope.valueOf(scope.toUpperCase());
		}catch(Exception e){
			logger.warn("Scope "+scope+" unrecognized.");
		}
		return Setting.Scope.ANY;
	}
	
	protected Setting.Type readType(JSONObject setting){
		
		String type = null;
		try{
			type = setting.getString("type");
		}catch(Exception e){
		}
		
		if(type == null){
			return Setting.Type.STRING;
		}
		try{
			return Setting.Type.valueOf(type.toUpperCase());
		}catch(Exception e){
			logger.warn("Scope "+type+" unrecognized.");
		}
		return Setting.Type.STRING;
	}
	
	protected Setting.Checker readChecker(JSONObject setting){
		Setting.Checker ans = null;
		String checker = null;
		try{
			checker = setting.getString("validator");
		}catch(Exception e){
		}
		if(checker == null){
			return ans;
		}
		try{
			ans = (Setting.Checker) Class.forName(checker).newInstance();
		}catch(Exception e){
			logger.warn("Class "+checker+" cannot be instanced: "+e,e);
		}
		return ans;
	}

	public Map<String, Setting> getProperties() {
		return properties;
	}

	public Map<String, SettingMenu> getMenu() {
		return menu;
	}

	public Setting.Scope getScopeMenu() {
		return scopeMenu;
	}
}
