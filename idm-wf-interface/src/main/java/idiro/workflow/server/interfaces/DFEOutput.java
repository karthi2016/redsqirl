package idiro.workflow.server.interfaces;

import idiro.utils.FeatureList;
import idiro.workflow.server.enumeration.DataBrowser;
import idiro.workflow.server.enumeration.SavingState;

import java.io.File;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public interface DFEOutput extends Remote{

	/**
	 * The type name of the DFEOutput
	 * @return
	 * @throws RemoteException
	 */
	public String getTypeName() throws RemoteException;
	
	/**
	 * Get the browser to use
	 * @return
	 * @throws RemoteException
	 */
	public DataBrowser getBrowser() throws RemoteException;
	
	/**
	 * Get the colour of the type
	 * @return
	 * @throws RemoteException
	 */
	public String getColour() throws RemoteException;
	
	/**
	 * Set the colour of the type
	 * @param colour
	 * @throws RemoteException
	 */
	public void setColour(String colour) throws RemoteException;
	
	/**
	 * @return the features
	 */
	public FeatureList getFeatures() throws RemoteException;

	/**
	 * @return the properties
	 */
	public Map<String, String> getProperties() throws RemoteException;
	
	/**
	 * @return the path
	 */
	public String getPath() throws RemoteException;
	
	/**
	 * path the path to set
	 */
	public void setPath(String path) throws RemoteException;
	
	/**
	 * Generate automatically a valid path for the given user.
	 * @param component
	 * @param outputName
	 * @throws RemoteException 
	 */
	public void generatePath(String userName,
				String component, 
				String outputName) throws RemoteException;
	
	/**
	 * True if the current path is valid.
	 * A path is valid if the path already exists, or
	 * the path can be created automatically during the execution.
	 */
	public String isPathValid() throws RemoteException;
	
	/**
	 * True if the path has been auto generated.
	 * True if the path has been auto generated using 
	 * {@link #generatePath(String,String,String) generatePath} method
	 * for the given user
	 * @return
	 * @throws RemoteException
	 */
	public boolean isPathAutoGeneratedForUser(
			String userName,
			String component, 
			String outputName) throws RemoteException;
	
	/**
	 * True if the path exists.
	 * @return
	 * @throws RemoteException
	 */
	public boolean isPathExists() throws RemoteException;
	
	/**
	 * Write the dataOutput attributes in an xml element 
	 * @param parent
	 * @param doc
	 * @throws RemoteException
	 */
	public void write(Document doc,Element parent) throws RemoteException;
	
	/**
	 * Read the dataOutput attributes from an xml element 
	 * @param parent
	 * @param doc
	 * @throws RemoteException
	 */
	public void read(Element parent) throws RemoteException;
	
	/**
	 * Delete immediately the pointed output
	 * @throws RemoteException
	 */
	public String remove() throws RemoteException;
	
	/**
	 * Xml code to Delete the pointed output from an oozie action
	 * @throws RemoteException
	 */
	public boolean oozieRemove(Document oozieDoc,Element action,
			File localDirectory, String pathFromOozieDir,
			String fileNameWithoutExtension) throws RemoteException;
	
	/**
	 * Select the first lines of the output if exists
	 * @param path
	 * @param maxToRead
	 * @return
	 * @throws RemoteException
	 */
	List<String> select(int maxToRead) throws RemoteException;
	
	/**
	 * @param features the features to set
	 */
	public void setFeatures(FeatureList features) throws RemoteException;

	/**
	 * @return the savingState
	 */
	public SavingState getSavingState() throws RemoteException;

	/**
	 * @param savingState the savingState to set
	 */
	public void setSavingState(SavingState savingState) throws RemoteException;
	
	/**
	 * Add a key/value property
	 * @param key
	 * @param value
	 * @throws RemoteException
	 */
	public void addProperty(String key, String value) throws RemoteException;
	
	/**
	 * Get the property value associated with the key 
	 * @param key
	 * @return
	 * @throws RemoteException
	 */
	public String getProperty(String key) throws RemoteException;
	
	/**
	 * Remove a property
	 * @param key
	 * @throws RemoteException
	 */
	public void removeProperty(String key) throws RemoteException;
}
