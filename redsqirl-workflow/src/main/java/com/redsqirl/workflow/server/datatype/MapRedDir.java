package com.redsqirl.workflow.server.datatype;



import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
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
import org.apache.pig.data.DataType;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.idiro.hadoop.NameNodeVar;
import com.idiro.hadoop.checker.HdfsFileChecker;
import com.redsqirl.utils.FieldList;
import com.redsqirl.utils.OrderedFieldList;
import com.redsqirl.workflow.server.DataOutput;
import com.redsqirl.workflow.server.OozieManager;
import com.redsqirl.workflow.server.connect.HDFSInterface;
import com.redsqirl.workflow.server.enumeration.FieldType;
import com.redsqirl.workflow.utils.LanguageManagerWF;

public abstract class MapRedDir extends DataOutput{


	/**
	 * 
	 */
	private static final long serialVersionUID = 3497308078096391496L;
	
	private static Logger logger = Logger.getLogger(MapRedDir.class);
	
	/** HDFS Interface */
	protected static HDFSInterface hdfsInt;
	

	protected static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

	protected static SimpleDateFormat datetimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

	protected static SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");


	protected List<FieldType> fieldsNumberHierarchicalOrder = new LinkedList<FieldType>();

	protected List<FieldType> fieldsStrHierarchicalOrder = new LinkedList<FieldType>();

	protected List<FieldType> fieldsDateHierarchicalOrder = new LinkedList<FieldType>();
	
	public MapRedDir() throws RemoteException{
		super();
		init();
	}
	
	public MapRedDir(FieldList fields) throws RemoteException {
		super(fields);
		init();
	}
	
	private void init() throws RemoteException{
		if (hdfsInt == null) {
			hdfsInt = new HDFSInterface();
		}
		
		fieldsNumberHierarchicalOrder.add(FieldType.INT);
		fieldsNumberHierarchicalOrder.add(FieldType.LONG);
		fieldsNumberHierarchicalOrder.add(FieldType.FLOAT);
		fieldsNumberHierarchicalOrder.add(FieldType.DOUBLE);
		fieldsNumberHierarchicalOrder.add(FieldType.CATEGORY);
		fieldsNumberHierarchicalOrder.add(FieldType.STRING);

		fieldsStrHierarchicalOrder.add(FieldType.CHAR);
		fieldsStrHierarchicalOrder.add(FieldType.CATEGORY);
		fieldsStrHierarchicalOrder.add(FieldType.STRING);
		fieldsStrHierarchicalOrder.add(FieldType.CATEGORY);

		fieldsDateHierarchicalOrder.add(FieldType.DATE);
		fieldsDateHierarchicalOrder.add(FieldType.DATETIME);
		fieldsDateHierarchicalOrder.add(FieldType.TIMESTAMP);
		fieldsDateHierarchicalOrder.add(FieldType.CATEGORY);
		fieldsDateHierarchicalOrder.add(FieldType.STRING);
	}
	
	protected String checkCompatibility(FieldList from, FieldList to) throws RemoteException{
		Iterator<String> flIt = from.getFieldNames().iterator();
		Iterator<String> fieldIt = to.getFieldNames().iterator();
		String error = null;
		boolean ok = true;
		int i = 1;
		while (flIt.hasNext() && ok) {
			String nf = flIt.next();
			if (!fieldIt.hasNext()){
				ok = false;
				error = LanguageManagerWF.getText(
						"mapredtexttype.msg_error_number_fields");
			} else{
				String of = fieldIt.next();
				logger.info("types field " + i + ": "
						+ from.getFieldType(nf) + " , "
						+ to.getFieldType(of));
				ok &= canCast(from.getFieldType(nf),
						to.getFieldType(of));
				if (!ok) {
					error = LanguageManagerWF.getText(
							"mapredtexttype.msg_error_cannot_cast",
							new Object[] { from.getFieldType(nf),
									to.getFieldType(of) });
				}
				++i;
			}
		}
		return error;
	}
	

	/**
	 * Get the DataBrowser
	 * 
	 * @return {@link idiro.workflow.server.enumeration.DataBrowser}
	 * @throws RemoteException
	 */
	@Override
	public String getBrowser() throws RemoteException {
		return hdfsInt.getBrowserName();
	}


	@Override
	public boolean isPathExists() throws RemoteException {
		boolean ok = false;
		if (getPath() != null) {
			logger.info("checking if path exists: " + getPath().toString());
			int again = 10;
			FileSystem fs = null;
			while (again > 0) {
				try {
					fs = NameNodeVar.getFS();
					logger.debug("Attempt " + (11 - again) + ": existence "
							+ getPath());
					ok = fs.exists(new Path(getPath()));
					again = 0;
				} catch (Exception e) {
					logger.error(e);
					--again;
				}
				try {
					// fs.close();
				} catch (Exception e) {
					logger.error(e);
				}
				if (again > 0) {
					try {
						Thread.sleep(10);
					} catch (InterruptedException e1) {
						logger.error(e1);
					}
				}
			}
		}
		return ok;
	}
	
	public String isPathValid(List<String> shouldNotHaveExt, List<String> shouldHaveExt) throws RemoteException {
		String error = null;
		HdfsFileChecker hCh = new HdfsFileChecker(getPath());
		if(shouldHaveExt != null && !shouldHaveExt.isEmpty()){
			boolean found = false;
			for(String extCur: shouldHaveExt){
				found |= getPath().endsWith(extCur);
			}
			if(!found){
				error = LanguageManagerWF.getText(
						"mapredtexttype.shouldhaveext",
						new Object[] { getPath(),shouldHaveExt });
				
			}
		}else if(shouldNotHaveExt != null && ! shouldNotHaveExt.isEmpty()){
			boolean found = false;
			for(String extCur: shouldNotHaveExt){
				found |= getPath().endsWith(extCur);
			}
			if(found){
				error = LanguageManagerWF.getText(
						"mapredtexttype.shouldnothaveext",
						new Object[] { getPath(),shouldNotHaveExt });
				
			}
		}
		
		if (!hCh.isInitialized() || hCh.isFile()) {
			error = LanguageManagerWF.getText("mapredtexttype.dirisfile");
		} else {
			FileSystem fs;
			try {
				fs = NameNodeVar.getFS();
				hCh.setPath(new Path(getPath()).getParent());
				if (!hCh.isDirectory()) {
					error = LanguageManagerWF.getText("mapredtexttype.nodir",new String[]{getPath()});
				}
				FileStatus[] stat = fs.listStatus(new Path(getPath()),
						new PathFilter() {

					@Override
					public boolean accept(Path arg0) {
						return !arg0.getName().startsWith("_") && !arg0.getName().startsWith(".");
					}
				});
				for (int i = 0; i < stat.length && error == null; ++i) {
					if (stat[i].isDir()) {
						error = LanguageManagerWF.getText(
								"mapredtexttype.notmrdir",
								new Object[] { getPath() });
					}else{
						
						if(shouldHaveExt != null && !shouldHaveExt.isEmpty()){
							boolean found = false;
							for(String extCur: shouldHaveExt){
								found |= stat[i].getPath().getName().endsWith(extCur);
							}
							if(!found){
								error = LanguageManagerWF.getText(
										"mapredtexttype.shouldhaveext",
										new Object[] { getPath(),shouldHaveExt });
								
							}
						}else if(shouldNotHaveExt != null && ! shouldNotHaveExt.isEmpty()){
							boolean found = false;
							for(String extCur: shouldNotHaveExt){
								found |= stat[i].getPath().getName().endsWith(extCur);
							}
							if(found){
								error = LanguageManagerWF.getText(
										"mapredtexttype.shouldnothaveext",
										new Object[] { getPath(),shouldNotHaveExt });
								
							}
						}
						
						
						try {
							hdfsInt.select(stat[i].getPath().toString(),"", 1);
						} catch (Exception e) {
							error = LanguageManagerWF
									.getText("mapredtexttype.notmrdir");
						}
					}
				}
				try {
					// fs.close();
				} catch (Exception e) {
					logger.error("Fail to close FileSystem: " + e);
				}
			} catch (IOException e) {

				error = LanguageManagerWF.getText("unexpectedexception",
						new Object[] { e.getMessage() });

				logger.error(error);
			}

		}
		// hCh.close();
		return error;
	}
	
	/**
	 * Move the current path to a new one
	 * 
	 * @param newPath
	 * @throws RemoteException
	 */
	@Override
	public void moveTo(String newPath) throws RemoteException {
		if (isPathExists()) {
			hdfsInt.move(getPath(), newPath);
		}
		setPath(newPath);
	}

	/**
	 * Copy the current path to a new one
	 * 
	 * @param newPath
	 * @throws RemoteException
	 * 
	 */
	@Override
	public void copyTo(String newPath) throws RemoteException {
		if (isPathExists()) {
			hdfsInt.copy(getPath(), newPath);
		}
		setPath(newPath);
	}

	/**
	 * Remove the current path from hdfs
	 * 
	 * @return Error Message
	 * @throws RemoteException
	 */
	@Override
	public String remove() throws RemoteException {
		return hdfsInt.delete(getPath());
	}

	@Override
	public boolean oozieRemove(Document oozieDoc, Element action,
			File localDirectory, String pathFromOozieDir,
			String fileNameWithoutExtension) throws RemoteException {
		Element fs = oozieDoc.createElement("fs");
		action.appendChild(fs);

		Element rm = oozieDoc.createElement("delete");
		rm.setAttribute("path", "${" + OozieManager.prop_namenode + "}"
				+ getPath());
		fs.appendChild(rm);

		return true;
	}
	

	/**
	 * Is name a variable
	 * 
	 * @param name
	 * @return <code>true</code> if name matches structure of a variable name
	 *         (contains characters with numbers) and has a maximum else
	 *         <code>false</code>
	 */
	public boolean isVariableName(String name) {
		String regex = "[a-zA-Z]([a-zA-Z0-9_]{0,29})";
		return name.matches(regex);
	}
	

	public List<String> selectLine(int maxToRead) throws RemoteException {
		List<String> ans = null;
		if (isPathValid() == null && isPathExists()) {
			try {
				FileSystem fs = NameNodeVar.getFS();
				FileStatus[] stat = fs.listStatus(new Path(getPath()),
						new PathFilter() {

					@Override
					public boolean accept(Path arg0) {
						return !arg0.getName().startsWith("_") && !arg0.getName().startsWith(".");
					}
				});
				ans = new ArrayList<String>(maxToRead);
				for (int i = 0; i < stat.length; ++i) {
					ans.addAll(hdfsInt.select(stat[i].getPath().toString(),
							",",
							(maxToRead / stat.length) + 1));
				}
				try {
					// fs.close();
				} catch (Exception e) {
					logger.error("Fail to close FileSystem: " + e);
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
	 * Generate a column name
	 * 
	 * @param columnIndex
	 * @return name
	 */
	private String generateColumnName(int columnIndex) {
		return "FIELD"+(columnIndex+1);
	}
	
	/**
	 * Generate a fields list from the data in the current path
	 * 
	 * @return FieldList
	 * @throws RemoteException
	 */
	protected FieldList generateFieldsMap(String delimiter) throws RemoteException {

		logger.info("generateFieldsMap --");
		
		FieldList fl = new OrderedFieldList();
		try {
			List<String> lines = this.selectLine(2000);
			Map<String,Set<String>> valueMap = new LinkedHashMap<String,Set<String>>();
			Map<String,Integer> nbValueMap = new LinkedHashMap<String,Integer>();
			
			List<String[]> schemaList = getSchemaList();
			Map<String, FieldType> schemaTypeMap = new LinkedHashMap<String, FieldType>();
			
			if (lines != null) {							
				logger.trace("key_delimiter: "
					+ Pattern
					.quote(delimiter));
				for (String line : lines) {
					boolean full = true;
					if (!line.trim().isEmpty()) {
						int cont = 0;
						for (String s : line.split(Pattern
								.quote(delimiter))) {

							String nameColumn;
							if (schemaList != null && !schemaList.isEmpty() 
									&& schemaList.size() > cont){
								nameColumn = schemaList.get(cont)[0];
								String typeColumn = schemaList.get(cont++)[1].toUpperCase();
								FieldType fieldType;
								
								if (typeColumn.equalsIgnoreCase("CHARARRAY")) {
									fieldType = FieldType.STRING;
								} else if (typeColumn.equalsIgnoreCase("NUMBER")) {
									fieldType = FieldType.DOUBLE;
								} else {
									fieldType = FieldType.valueOf(typeColumn);
								}
								
								schemaTypeMap.put(nameColumn, fieldType);
							}
							else{
								nameColumn = generateColumnName(cont++);
							}
							
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
	
	private List<String[]> getSchemaList(){
		
		JSONParser parser = new JSONParser();
		List<String[]> schemaMap = new ArrayList<String[]>();
		
		List<String> schemaList;
		try {
			schemaList = hdfsInt.select(getPath()+"/.pig_schema", "", 10);
			
			
			if (schemaList != null && !schemaList.isEmpty()){
				JSONObject a = (JSONObject) parser.parse(schemaList.get(0));
				
				JSONArray fields = (JSONArray) a.get("fields");
				for (int i = 0; i < fields.size(); ++i){
					JSONObject obj = (JSONObject) fields.get(i);
					schemaMap.add(new String[]{String.valueOf(obj.get("name")), 
							DataType.findTypeName( ((Long)obj.get("type")).byteValue() )});
				}
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		return schemaMap;
	}
	

	public FieldType getType(Set<String> exValue, int numberOfValues, FieldType schemaType){
		FieldType typeAns = schemaType; 
		boolean restart = false;
		do{
			restart = false;
			Iterator<String> fieldValueIt = exValue.iterator();
			while(fieldValueIt.hasNext() && !restart){
				String fieldValue = fieldValueIt.next();
				FieldType typeCur = getType(fieldValue);
				logger.info("Value: "+fieldValue);
				logger.info("Type ans: "+typeAns);
				logger.info("Type cur: "+typeCur);
				if(typeAns == null){
					typeAns = typeCur;
				}else if(canCast(typeCur,typeAns)){
					//Nothing to do
				}else if(canCast(typeAns,typeCur)){
					//Get the higher type
					typeAns = typeCur;
				}else{
					logger.info("Have to reset the type");
					//Not the good type
					if(typeCur.equals(FieldType.CHAR) && typeAns.equals(FieldType.INT)){
						logger.info("Test integer");
						try {
							Integer.valueOf(fieldValue);
						} catch (Exception e) {
							typeAns = FieldType.STRING;
						}
					}else if(typeAns.equals(FieldType.CHAR) && typeCur.equals(FieldType.INT)){
						logger.info("Set to int and start again");
						typeAns = FieldType.INT;
						restart = true;
					}else{
						logger.info("Set to string");
						typeAns = FieldType.STRING;
					}
				}
			}
			logger.info(restart);
		}while(restart);

		if(typeAns.equals(FieldType.STRING)){
			int nbValues = exValue.size();
			logger.info(nbValues+" / "+numberOfValues);
			if(nbValues < 101 && nbValues * 100 /numberOfValues < 5){
				typeAns = FieldType.CATEGORY;
			}
		}
		
		return typeAns;
	}

	/**
	 * Get the FieldType of
	 * 
	 * @param expr
	 *            to get FieldType of
	 * @return {@link com.redsqirl.workflow.server.enumeration.FieldType}
	 */
	public static FieldType getType(String expr) {

		FieldType type = null;
		if (expr.equalsIgnoreCase("TRUE") || expr.equalsIgnoreCase("FALSE")) {
			type = FieldType.BOOLEAN;
		} else {
			if(expr.length() == 1){
				type = FieldType.CHAR;
			}

			try {
				Integer.valueOf(expr);
				type = FieldType.INT;
			} catch (Exception e) {
			}
			if (type == null) {
				try {
					Long.valueOf(expr);
					type = FieldType.LONG;
				} catch (Exception e) {
				}
			}
			if (type == null) {
				try {
					Float.valueOf(expr);
					type = FieldType.FLOAT;
				} catch (Exception e) {
				}
			}
			if (type == null) {
				try {
					Double.valueOf(expr);
					type = FieldType.DOUBLE;
				} catch (Exception e) {
				}
			}
			if (type == null && expr.length() < 11) {
				try {
					dateFormat.parse(expr);
					type = FieldType.DATE;
				} catch (Exception e) {
				}
			}

			if (type == null && expr.length() < 20) {
				try {
					datetimeFormat.parse(expr);
					type = FieldType.DATETIME;
				} catch (Exception e) {
				}
			}

			if (type == null) {
				try {
					timestampFormat.parse(expr);
					type = FieldType.TIMESTAMP;
				} catch (Exception e) {
				}
			}

			if (type == null) {
				type = FieldType.STRING;
			}
		}
		return type;
	}
	

	/**
	 * Check if a field can be converted from one type to another
	 * 
	 * @param from
	 * @param to
	 * @return <code>true</code> the cast is possible else <code>false</code>
	 */
	public boolean canCast(FieldType from, FieldType to) {
		if (from.equals(to)) {
			return true;
		}

		if (from.equals(FieldType.BOOLEAN)) {
			if (to.equals(FieldType.STRING) || to.equals(FieldType.CATEGORY)) {
				return true;
			}
			return false;
		} else{
			int fromNumberIdx = fieldsNumberHierarchicalOrder.indexOf(from);
			int toNumberIdx = fieldsNumberHierarchicalOrder.lastIndexOf(to);
			if( fromNumberIdx != -1 && toNumberIdx != -1 && fromNumberIdx <= toNumberIdx){
				return true;
			}
			int fromStrIdx = fieldsStrHierarchicalOrder.indexOf(from);
			int toStrIdx = fieldsStrHierarchicalOrder.lastIndexOf(to);
			if( fromStrIdx != -1 && toStrIdx != -1 && fromStrIdx <= toStrIdx){
				return true;
			}
			int fromDateIdx = fieldsDateHierarchicalOrder.indexOf(from);
			int toDateIdx = fieldsDateHierarchicalOrder.lastIndexOf(to);
			if( fromDateIdx != -1 && toDateIdx != -1 && fromDateIdx <= toDateIdx){
				return true;
			}
		}
		return false;
	}

}
