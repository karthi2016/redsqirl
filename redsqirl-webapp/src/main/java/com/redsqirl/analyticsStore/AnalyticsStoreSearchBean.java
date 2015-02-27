package com.redsqirl.analyticsStore;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.faces.context.FacesContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.redsqirl.workflow.server.WorkflowPrefManager;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

public class AnalyticsStoreSearchBean implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 5119862157796243985L;
	
	private AnalyticsStoreLoginBean analyticsStoreLoginBean;
	
	private String searchValue;
	
	private String message;
	
	private List<RedSqirlModule> allPackageList;
	
	
	public AnalyticsStoreSearchBean() {
		
	}
	
	@PostConstruct
	public void init(){
		try{
			retrieveAllPackageList();
		}catch (Exception e){
			
		}
	}

	public void retrieveAllPackageList() throws SQLException, ClassNotFoundException{
	
		List<RedSqirlModule> result = new ArrayList<RedSqirlModule>();
		
		try{
			
			Map<String, String> params = FacesContext.getCurrentInstance().
					getExternalContext().getRequestParameterMap();
			String type = params.get("type");
			
			String uri = getRepoServer()+"rest/allpackages";
			
			JSONObject object = new JSONObject();
			object.put("software", "RedSqirl");
			object.put("filter", searchValue);
			if (type != null && !type.isEmpty()){
				object.put("type", type);
			}
			
			Client client = Client.create();
			WebResource webResource = client.resource(uri);
	
			ClientResponse response = webResource.type("application/json")
			   .post(ClientResponse.class, object.toString());
			String ansServer = response.getEntity(String.class);
			
			System.out.println(ansServer);
			
			Set<String> packagesAdded = new HashSet<String>();
			try{
				JSONArray pckArray = new JSONArray(ansServer);
				for(int i = 0; i < pckArray.length();++i){
					JSONObject pckObj = pckArray.getJSONObject(i);
					
					RedSqirlModule pck = new RedSqirlModule();
					String id = pckObj.getString("id");
					pck.setId(Integer.valueOf(id));
					pck.setName(pckObj.getString("name"));
					pck.setTags(pckObj.getString("tags"));
					pck.setImage(getRepoServer() + pckObj.getString("image"));
					
					if (!packagesAdded.contains(id)){
						result.add(pck);
						packagesAdded.add(id);
					}
				}
			} catch (JSONException e){
				e.printStackTrace();
			}

		}catch(Exception e){
			e.printStackTrace();
		}

		setAllPackageList(result);
	}
	
	public String getRepoServer(){
		String pckServer = WorkflowPrefManager.getPckManagerUri();
		if(!pckServer.endsWith("/")){
			pckServer+="/";
		}
		return pckServer;
	}
	
	public String getSearchValue() {
		return searchValue;
	}

	public void setSearchValue(String searchValue) {
		this.searchValue = searchValue;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public AnalyticsStoreLoginBean getAnalyticsStoreLoginBean() {
		return analyticsStoreLoginBean;
	}

	public void setAnalyticsStoreLoginBean(AnalyticsStoreLoginBean analyticsStoreLoginBean) {
		this.analyticsStoreLoginBean = analyticsStoreLoginBean;
	}

	public List<RedSqirlModule> getAllPackageList() {
		return allPackageList;
	}

	public void setAllPackageList(List<RedSqirlModule> allPackageList) {
		this.allPackageList = allPackageList;
	}
	
}