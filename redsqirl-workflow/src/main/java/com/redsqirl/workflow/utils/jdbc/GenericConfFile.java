package com.redsqirl.workflow.utils.jdbc;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.redsqirl.workflow.server.action.dictionary.JdbcDictionary;
import com.redsqirl.workflow.server.connect.jdbc.JdbcQueryManager;
import com.redsqirl.workflow.server.enumeration.FieldType;

public class GenericConfFile  extends DbConfFile {

	private static Logger logger = Logger.getLogger(GenericConfFile.class);
	DatabaseMetaData databaseMetaData = null;
	Map<Integer,FieldType> typeRecognized = null;
	
	public GenericConfFile(String name, Connection conn) throws SQLException{
		dictionaryName = name;
		
		databaseMetaData = conn.getMetaData();
		
		typeRecognized = new LinkedHashMap<Integer,FieldType>();
		typeRecognized.put(Types.BOOLEAN, FieldType.BOOLEAN);
		typeRecognized.put(Types.DATE, FieldType.DATETIME);
		typeRecognized.put(Types.TIME, FieldType.DATETIME);
		typeRecognized.put(Types.DOUBLE, FieldType.DOUBLE);
		typeRecognized.put(Types.NUMERIC, FieldType.DOUBLE);
		typeRecognized.put(Types.DECIMAL, FieldType.DOUBLE);
		typeRecognized.put(Types.REAL, FieldType.DOUBLE);
		typeRecognized.put(Types.FLOAT, FieldType.FLOAT);
		typeRecognized.put(Types.INTEGER, FieldType.INT);
		typeRecognized.put(Types.SMALLINT, FieldType.INT);
		typeRecognized.put(Types.TINYINT, FieldType.INT);
		typeRecognized.put(Types.BIGINT, FieldType.LONG);
		typeRecognized.put(Types.VARCHAR, FieldType.STRING);
		typeRecognized.put(Types.CHAR, FieldType.STRING);
		typeRecognized.put(Types.LONGVARCHAR, FieldType.STRING);
		typeRecognized.put(Types.NVARCHAR, FieldType.STRING);
		typeRecognized.put(Types.NCHAR, FieldType.STRING);
		typeRecognized.put(Types.LONGNVARCHAR, FieldType.STRING);
		typeRecognized.put(Types.TIMESTAMP, FieldType.TIMESTAMP);
		
	}
	
	@Override
	protected String getQueryFileContent() {
		String ans = "";
		ans +=JdbcQueryManager.Query.CREATE.toString()+":CREATE TABLE {0} ({1})\n";
		ans +=JdbcQueryManager.Query.DESCRIBE.toString()+":\n";
		ans +=JdbcQueryManager.Query.DROP.toString()+":DROP TABLE {0}\n";
		ans +=JdbcQueryManager.Query.INSERT_SELECT.toString()+":INSERT INTO {0} \n";
		ans +=JdbcQueryManager.Query.INSERT_VALUES.toString()+":INSERT INTO {0} VALUES ({2})\n";
		ans +=JdbcQueryManager.Query.LIST_TABLES.toString()+":\n";
		ans +=JdbcQueryManager.Query.SELECT.toString()+":SELECT * FROM {0}\n";
		ans +=JdbcQueryManager.Query.TRUNCATE.toString()+":DELETE FROM {0}\n";
		return null;
	}

	@Override
	protected String getDbTypeFileContent() {
		String ans = "";
		Map<FieldType, String> types= new LinkedHashMap<FieldType,String>();
		try{
			ResultSet rs = databaseMetaData.getTypeInfo();
			Map<Integer,String[]> databaseTypes = new LinkedHashMap<Integer,String[]>();
			while (rs.next()) {
				String typeName = rs.getString(1).toUpperCase();
				int dataType = rs.getInt(2);
				int preci = rs.getInt(3);
				String litPref = rs.getString(4);
				String litSuf = rs.getString(5);
				String create_params = rs.getString(6);
				if(typeRecognized.containsKey(dataType)){
					logger.info("Recognize: "+dataType+", "+typeName+", "+create_params+", "+typeRecognized.get(dataType)
					+", "+preci+", "+litPref+", "+litSuf);
					databaseTypes.put(dataType, new String[]{typeName,create_params});
				}
			}
			Iterator<Integer> typeRecIt = typeRecognized.keySet().iterator();
			while(typeRecIt.hasNext()){
				Integer dataType = typeRecIt.next();
				if(databaseTypes.containsKey(dataType) && !types.containsKey(typeRecognized.get(dataType))){
					String typeName = databaseTypes.get(dataType)[0];
					String create_params = databaseTypes.get(dataType)[1];
					String newP = typeName;
					logger.info("Add: "+dataType+", "+typeName+", "+create_params+", "+typeRecognized.get(dataType));
					if(create_params != null && !create_params.isEmpty()){
						newP+=create_params;
						/*
						if(FieldType.STRING.equals(typeRecognized.get(dataType))){
							newP+="(50)";
						}else if(FieldType.INT.equals(typeRecognized.get(dataType))){
							//newP+="(11)";
						}else if(FieldType.DOUBLE.equals(typeRecognized.get(dataType))){
							//newP+="(38)";
						}else if(FieldType.FLOAT.equals(typeRecognized.get(dataType))){
							//newP+="(16)";
						}
						*/
					}else if(FieldType.STRING.equals(typeRecognized.get(dataType)) && newP.startsWith("VAR")){
						newP+="(50)";
					}
					types.put(typeRecognized.get(dataType), newP);
				}
			}
			rs.close();
		}catch(Exception e){
			logger.error(e,e);
		}
		Iterator<FieldType> it = types.keySet().iterator();
		while(it.hasNext()){
			FieldType cur = it.next();
			ans += cur+":"+types.get(cur)+"\n";
		}
		
		return ans;
	}

	@Override
	protected String getRsTypeFileContent() {
		String ans = "";
		
		List<FieldType> priority = new ArrayList<FieldType>(9);
		priority.add(FieldType.BOOLEAN);
		priority.add(FieldType.INT);
		priority.add(FieldType.LONG);
		priority.add(FieldType.FLOAT);
		priority.add(FieldType.DOUBLE);
		priority.add(FieldType.DATE);
		priority.add(FieldType.DATETIME);
		priority.add(FieldType.TIMESTAMP);
		priority.add(FieldType.CHAR);
		priority.add(FieldType.STRING);
		try{
			ResultSet rs = databaseMetaData.getTypeInfo();
			Map<String,FieldType> mapAns = new LinkedHashMap<String,FieldType>();
			while (rs.next()) {
				String typeName = rs.getString(1).toUpperCase();
				int dataType = rs.getInt(2);
				//String precision = rs.getString(3).toUpperCase();
				//String lit_pref = rs.getString(4).toUpperCase();
				//String lit_suf = rs.getString(5).toUpperCase();
				String create_params = rs.getString(6);
				//String nullable = rs.getString(7).toUpperCase();
				//String case_sensitive = rs.getString(8).toUpperCase();
				//String seachable = rs.getString(9).toUpperCase();
				//String unsigned_attr = rs.getString(10).toUpperCase();
				if(typeRecognized.containsKey(dataType)){
					//Type Recognized: Add it in the list
					int numberParam = 0;
					if(create_params != null && !create_params.isEmpty()){
						logger.info("params: "+create_params);
						numberParam = StringUtils.countMatches(create_params,",")+1;
					}else{
						logger.info("No parameters for "+dataType);
					}
					if(numberParam > 0){
						String reg = typeName;
						reg+="\\(";
						for(int i =0; i < numberParam;++i){
							if(i > 0){
								reg+=",";
							}
							reg+="\\d+";
							String regCur = reg+"\\)";
							if(!mapAns.containsKey(regCur) ||
									priority.indexOf(typeRecognized.get(dataType)) > priority.indexOf(mapAns.get(regCur))){
								mapAns.put(regCur, typeRecognized.get(dataType));
							}
						}
					}else{
						mapAns.put(typeName,typeRecognized.get(dataType));
					}
				}
			}
			rs.close();
			Iterator<String> it = mapAns.keySet().iterator();
			while(it.hasNext()){
				String cur = it.next();
				ans+=cur;
				ans+=":"+mapAns.get(cur);
				ans+="\n";
				if(!cur.endsWith(")")&&FieldType.STRING.equals(mapAns.get(cur)) && cur.startsWith("VAR")){
					ans+=cur;
					ans+="\\(\\d+\\)";
					ans+=":"+mapAns.get(cur);
					ans+="\n";
				}
			}
		}catch(Exception e){
			logger.error(e,e);
		}
		
		return ans;
	}

	@Override
	protected JdbcDictionary getDictionary() {
		JdbcDictionary ans = null;
		try{
			logger.info("Get the functions");		
			//Organise the Functions into menus
			Map<String,List<String[]>> dictionaryFunctionMap = getJdbcDictionay();

			//Merge with the generic dictionary
			ans = new JdbcDictionary(dictionaryName, dictionaryFunctionMap);
		}catch(Exception e){
			logger.error(e,e);
		}
	 	
		// TODO Auto-generated method stub
		return ans;
	}
	
	private Map<String,List<String[]>> getJdbcDictionay(){
		Map<String,List<String[]>> dictionaryFunctionMap = new LinkedHashMap<String,List<String[]>>();
		String stringFct = null;
		try{
			stringFct = databaseMetaData.getStringFunctions();
			logger.info("String functions: "+stringFct);
			String[] fcts = stringFct.split(",");
			dictionaryFunctionMap.put(JdbcDictionary.stringMethods, new LinkedList<String[]>());
			for(int i=0; i< fcts.length;++i){
				String fct = fcts[i];
				String[] details = new String[]{fct,"STRING","ANY...",""};
				dictionaryFunctionMap.get(JdbcDictionary.stringMethods).add(details);
			}
		}catch(Exception e){
			logger.error(e,e);
		}
		String numFct = null;
		try{
			numFct = databaseMetaData.getNumericFunctions();
			logger.info("Numeric functions: "+numFct);
			String[] fcts = stringFct.split(",");
			dictionaryFunctionMap.put(JdbcDictionary.mathMethods, new LinkedList<String[]>());
			for(int i=0; i< fcts.length;++i){
				String fct = fcts[i];
				String[] details = new String[]{fct,"NUMBER","ANY...",null};
				dictionaryFunctionMap.get(JdbcDictionary.mathMethods).add(details);
			}
		}catch(Exception e){
			logger.error(e,e);
		}
		String sysFct = null;
		try{
			sysFct = databaseMetaData.getSystemFunctions();
			logger.info("Sys functions: "+sysFct);
			String[] fcts = stringFct.split(",");
			dictionaryFunctionMap.put(JdbcDictionary.utilsMethods, new LinkedList<String[]>());
			for(int i=0; i< fcts.length;++i){
				String fct = fcts[i];
				String[] details = new String[]{fct,"ANY","ANY...",null};
				dictionaryFunctionMap.get(JdbcDictionary.utilsMethods).add(details);
			}
		}catch(Exception e){
			logger.error(e,e);
		}
		String timeFct = null;
		try{
			timeFct = databaseMetaData.getTimeDateFunctions();
			logger.info("Time functions: "+timeFct);
			String[] fcts = stringFct.split(",");
			dictionaryFunctionMap.put(JdbcDictionary.dateMethods, new LinkedList<String[]>());
			for(int i=0; i< fcts.length;++i){
				String fct = fcts[i];
				String[] details = new String[]{fct,"TIMESTAMP","ANY...",null};
				dictionaryFunctionMap.get(JdbcDictionary.dateMethods).add(details);
			}
		}catch(Exception e){
			logger.error(e,e);
		}
		return dictionaryFunctionMap;
	}

}