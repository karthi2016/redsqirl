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

package com.redsqirl.workflow.server.datatype;



import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PathFilter;
import org.apache.log4j.Logger;

import com.idiro.hadoop.NameNodeVar;
import com.idiro.hadoop.checker.HdfsFileChecker;
import com.idiro.utils.RandomString;
import com.redsqirl.utils.FieldList;
import com.redsqirl.utils.OrderedFieldList;
import com.redsqirl.workflow.server.enumeration.FieldType;
import com.redsqirl.workflow.utils.LanguageManagerWF;

/**
 * Map-Reduce Text output type. Output given when an algorithm return a text
 * format map-reduce directory.
 * 
 * @author etienne
 * 
 */
public class MapRedTextFileType extends MapRedHdfs {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8260229620701006942L;
	/** Delimiter Key */
	public final static String key_delimiter = "delimiter";
	
	private static Logger logger = Logger.getLogger(MapRedTextFileType.class);


	/**
	 * Default Constructor
	 * 
	 * @throws RemoteException
	 */
	public MapRedTextFileType() throws RemoteException {
		super();
		setHeaderEditorOnBrowser(true);
	}

	/**
	 * Constructor with FieldList
	 * 
	 * @param fields
	 * @throws RemoteException
	 */
	public MapRedTextFileType(FieldList fields) throws RemoteException {
		super(fields);
		setHeaderEditorOnBrowser(true);
	}

	/**
	 * Get the type name
	 * 
	 * @return name
	 * @throws RemoteException
	 */
	@Override
	public String getTypeName() throws RemoteException {
		return "HDFS TEXT FILE";
	}
	
	@Override
	public String[] getExtensions() throws RemoteException {
		return new String[]{"*","*.txt"};
	}

	/**
	 * Generate a path given values
	 * 
	 * @param userName
	 * @param component
	 * @param outputName
	 * @return generated path
	 * @throws RemoteException
	 */
	@Override
	public String generatePathStr(String component,
			String outputName) throws RemoteException {
		return "/user/" + userName + "/tmp/redsqirl_" + component + "_" + outputName
				+ "_" + RandomString.getRandomName(8)+".txt";
	}
	
	/**
	 * Check if the path is a valid path
	 * 
	 * @return Error Message
	 * @throws RemoteException
	 */
	@Override
	public String isPathValid(String path) throws RemoteException {
		List<String> shouldNotHaveExt = new LinkedList<String>();
		shouldNotHaveExt.add(".bz");
		shouldNotHaveExt.add(".bz2");
		return isPathValid(path, shouldNotHaveExt,null);
	}

	public String isPathValid(String path, List<String> shouldNotHaveExt, List<String> shouldHaveExt) throws RemoteException {
		String error = null;
		HdfsFileChecker hCh = new HdfsFileChecker(path);
		if(path != null){
			if(shouldHaveExt != null && !shouldHaveExt.isEmpty()){
				boolean found = false;
				for(String extCur: shouldHaveExt){
					found |= path.endsWith(extCur);
				}
				if(!found){
					error = LanguageManagerWF.getText(
							"mapredtexttype.shouldhaveext",
							new Object[] { path,shouldHaveExt });

				}
			}else if(shouldNotHaveExt != null && ! shouldNotHaveExt.isEmpty()){
				boolean found = false;
				for(String extCur: shouldNotHaveExt){
					found |= path.endsWith(extCur);
				}
				if(found){
					
					if(shouldNotHaveExt != null && (shouldNotHaveExt.contains(".bz") || shouldNotHaveExt.contains(".bz2"))){
						error = LanguageManagerWF.getText(
								"mapredtexttype.shouldnothaveextcompresssile",
								new Object[] { path,shouldNotHaveExt });
					}else{
						error = LanguageManagerWF.getText(
								"mapredtexttype.shouldnothaveext",
								new Object[] { path,shouldNotHaveExt });
					}
					
				}
			}
			if (!hCh.isInitialized()) {
				error = "internal error";
			} else {
				if (hCh.exists() && !hCh.isFile()) {
					error = LanguageManagerWF.getText("mapredtextfiletype.nofile",new String[]{path});
				}

			}
		}
		return error;
	}
	
	/**
	 * I
	 */
	@Override
	public boolean isPathAutoGeneratedForUser(String component, String outputName) throws RemoteException {
		return getPath().startsWith(
				"/user/" + userName + "/tmp/redsqirl_" + component + "_"
						+ outputName + "_") && getPath().endsWith(".txt");
	}
	
	@Override
	public boolean isPathAutoGeneratedForUser(String path) throws RemoteException {
		return path.startsWith(
				"/user/" + userName + "/tmp/redsqirl_") && path.endsWith(".txt");
	}
	
	@Override
	public void removeAllDataUnderGeneratePath() throws RemoteException {
		try{
			String root = "/user/" + userName + "/tmp";
			Iterator<String> it = hdfsInt.getChildrenProperties(root).keySet().iterator();
			while(it.hasNext()){
				String curChildren = it.next();
				if(isPathAutoGeneratedForUser(curChildren)){
					hdfsInt.delete(curChildren);
				}
			}
		}catch(Exception e){}
	}

	/**
	 * Select data from the current path
	 * 
	 * @param maxToRead
	 *            limit
	 * @return List of rows returned
	 * @throws RemoteException
	 */
	protected List<Map<String,String>> readRecord(int maxToRead) throws RemoteException {
		List<Map<String,String>> ans = new LinkedList<Map<String,String>>();
		
		List<String> list = selectLine(maxToRead);
		
		if(list != null){
			
			List<String> fieldNames = getFields().getFieldNames();
			Iterator<String> it = list.iterator();
			
			while(it.hasNext()){
				String l = it.next();
				if(l != null && ! l.isEmpty()){
					String[] line = l.split(
							Pattern.quote(getChar(getProperty(key_delimiter))), -1);
					if (fieldNames.size() == line.length) {
						Map<String, String> cur = new LinkedHashMap<String, String>();
						for (int i = 0; i < line.length; ++i) {
							cur.put(fieldNames.get(i), line[i]);
						}
						ans.add(cur);
					} else {
						logger.error("The line size (" + line.length
								+ ") is not compatible to the number of fields ("
								+ fieldNames.size() + "). " + "The splitter is '"
								+ getChar(getProperty(key_delimiter)) + "'.");
						logger.error("Error line: " + l);
						ans = null;
						break;
					}
				}
			}
			
		}
		
		return ans;
	}


	/**
	 * Set the FieldList for the data set
	 * 
	 * @param fl
	 * 
	 */
	@Override
	public void setFields(FieldList fl) {
		logger.debug("setFields :");
		super.setFields(fl);
	}

	/**
	 * Get a Default delimiter from text
	 * 
	 * @param text
	 * @return delimiter
	 */
	protected String getDefaultDelimiter(String text) {
		if (text.contains("\001")) {
			return "#1";
		} else if (text.contains("\002")) {
			return "#2";
		} else if (text.contains("|")) {
			return "#124";
		} else if (text.contains(",")) {
			return "#44";
		}
		return "#1";
	}

	/**
	 * Add a property to the dataset
	 * 
	 * @param key
	 * @param value
	 */
	@Override
	public void addProperty(String key, String value) {

		if (key.equals(key_delimiter) && value.length() == 1) {
			value = "#" + String.valueOf((int) value.charAt(0));
		}
		super.addProperty(key, value);
	}
	
	public void setPathNoHeader(String path) throws RemoteException{
		super.setPath(path);
	}

	/**
	 * Set the path
	 * 
	 * @param path
	 * @throws RemoteException
	 */
	@Override
	public void setPath(String path) throws RemoteException {
		String oldPath = getPath();

		if (path == null) {
			super.setPath(path);
			setFields(null);
			return;
		}

		if (!path.equalsIgnoreCase(oldPath)) {

			super.setPath(path);

			logger.debug("setPath() " + path);
			List<String> list = selectLine(2000);
			if (list != null) {

				if (!list.isEmpty()) {
					String text = list.get(0);

					if (getProperty(key_delimiter) == null) {
						String delimiter = getDefaultDelimiter(text);

						logger.debug("delimiter -> " + delimiter);

						super.addProperty(key_delimiter, delimiter);
					}
				}

				FieldList fl = generateFieldsMap(getChar(getProperty(key_delimiter)),list);
				if(fields == null || fields.getSize() == 0){
					fields = fl;
				}else{
					logger.debug(fields.getFieldNames());
					logger.debug(fl.getFieldNames());
					String error = checkCompatibility(fl,fields);
					if(error != null){
						logger.debug(error);
						fields = fl;
						throw new RemoteException(error);
					}
				}
			}
		}
	}

	/**
	 * Get the character from an ascii value
	 * 
	 * @param asciiCode
	 * @return character
	 */
	protected String getChar(String asciiCode) {
		String result = null;
		if(asciiCode == null){
			//default
			result = "|";
		}else if (asciiCode.startsWith("#")
				&& asciiCode.length() > 1) {
			result = String.valueOf(Character.toChars(Integer.valueOf(asciiCode
					.substring(1))));
		} else {
			result = asciiCode;
		}
		return result;
	}

	/**
	 * Get the delimiter in octal format
	 * 
	 * @return delimiter
	 */
	public String getOctalDelimiter() {
		String asciiCode = getProperty(key_delimiter);
		String result = null;
		if (asciiCode != null && asciiCode.startsWith("#")
				&& asciiCode.length() > 1) {
			result = Integer.toOctalString(Integer.valueOf(asciiCode
					.substring(1)));
			if (result.length() == 2) {
				result = "\\0" + result;
			} else {
				result = "\\" + result;
			}
		}
		return result;
	}

	/**
	 * Get the delimiter to be used in Pig format
	 * 
	 * @return delimiter
	 */
	public String getChar() {
		String asciiCode = getProperty(key_delimiter);
		Character c = null;
		if (asciiCode == null) {
			c = '|';
		} else if (asciiCode != null && asciiCode.startsWith("#")
				&& asciiCode.length() > 1) {
			int i = Integer.valueOf(asciiCode.substring(1));
			c = new Character((char) i);
		} else if (asciiCode.length() == 1) {
			c = asciiCode.charAt(0);
		}
		
		String result = null;
		
		if (c != null){
			result = String.valueOf(c);
		}
		
		return result;
	}

	/**
	 * Get the delimiter in either octal or decimal notation
	 * 
	 * @return The delimiter in either octal or decimal notation.
	 */
	public String getDelimiterOrOctal() {
		String octal = getOctalDelimiter();
		return octal != null ? octal
				: getProperty(MapRedTextFileType.key_delimiter);
	}

	@Override
	protected String getDefaultColor() {
		return "MediumSlateBlue";
	}
	
	@Override
	public List<String> selectLine(int maxToRead) throws RemoteException {
		List<String> ans = null;
		if (isPathValid() == null && isPathExist()) {
			try {
				FileSystem fs = NameNodeVar.getFS();
				FileStatus[] stat = fs.listStatus(new Path(getPath()),
						new PathFilter() {

					@Override
					public boolean accept(Path arg0) {
						return !arg0.getName().startsWith("_") && !arg0.getName().startsWith(".");
					}
				});
				if(stat.length > 0){
					ans = new ArrayList<String>(maxToRead);
					for (int i = 0; i < stat.length; ++i) {
						ans.addAll(hdfsInt.select(stat[i].getPath().toString(),
								",",
								(maxToRead / stat.length) + 1));
					}
				}
			} catch (IOException e) {
				String error = "Unexpected error: " + e.getMessage();
				logger.error(error);
				ans = null;
			}
		}
		return ans;
	}
	
	/**
	 * Generate a fields list from the data in the current path
	 * 
	 * @return FieldList
	 * @throws RemoteException
	 */
	protected FieldList generateFieldsMap(String delimiter,List<String> lines) throws RemoteException {

		logger.debug("generateFieldsMap --");
		
		FieldList fl = new OrderedFieldList();
		try {
			
			Map<String,Set<String>> valueMap = new LinkedHashMap<String,Set<String>>();
			Map<String,Integer> nbValueMap = new LinkedHashMap<String,Integer>();
			
			Map<String, FieldType> schemaTypeMap = new LinkedHashMap<String, FieldType>();
			
			if (lines != null) {
				logger.trace("key_delimiter: " + Pattern.quote(delimiter));
				for (String line : lines) {
					boolean full = true;
					if (!line.trim().isEmpty()) {
						int cont = 0;
						for (String s : line.split(Pattern
								.quote(delimiter))) {

							String nameColumn = generateColumnName(cont++);
							
							if(!valueMap.containsKey(nameColumn)){
								valueMap.put(nameColumn, new LinkedHashSet<String>());
								nbValueMap.put(nameColumn, 0);
							}

							if(valueMap.get(nameColumn).size() < 101){
								full = false;
								valueMap.get(nameColumn).add(s.trim());
								nbValueMap.put(nameColumn,nbValueMap.get(nameColumn)+1);
							}

						}
					}
					if(full){
						break;
					}
				}
				
				Iterator<String> valueIt = valueMap.keySet().iterator();
				while(valueIt.hasNext()){
					String cat = valueIt.next();
					fl.addField(cat,getType(valueMap.get(cat),nbValueMap.get(cat), schemaTypeMap.get(cat)));
				}

			}
		} catch (RemoteException e) {
			e.printStackTrace();
		} 
		return fl;

	}
	
	@Override
	public boolean allowDirectories(){
		return false;
	}

}