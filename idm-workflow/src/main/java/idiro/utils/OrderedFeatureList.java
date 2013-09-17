package idiro.utils;

import idiro.workflow.server.enumeration.FeatureType;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrderedFeatureList extends UnicastRemoteObject implements FeatureList {
	
	private Map<String, FeatureType> features;
	private List<String> positions;
	
	public OrderedFeatureList() throws RemoteException {
		super();
		features = new HashMap<String, FeatureType>();
		positions = new ArrayList<String>();
	}
	
	public boolean containsFeature(String name){
		return features.containsKey(name);
	}
	
	public FeatureType getFeatureType(String name){
		return features.get(name);
	}
	
	public void addFeature(String name, FeatureType type){
		if (!features.containsKey(name)){
			positions.add(name);
		}
		features.put(name, type);
	}
	
	public List<String> getFeaturesNames(){
		return positions;
	}
	
	public int getSize(){
		return positions.size();
	}
	
	
}