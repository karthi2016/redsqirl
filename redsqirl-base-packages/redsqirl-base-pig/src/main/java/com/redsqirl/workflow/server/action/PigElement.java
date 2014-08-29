package com.redsqirl.workflow.server.action;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
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

import org.apache.log4j.Logger;

import com.redsqirl.utils.FieldList;
import com.redsqirl.utils.OrderedFieldList;
import com.redsqirl.workflow.server.AppendListInteraction;
import com.redsqirl.workflow.server.DataProperty;
import com.redsqirl.workflow.server.DataflowAction;
import com.redsqirl.workflow.server.InputInteraction;
import com.redsqirl.workflow.server.ListInteraction;
import com.redsqirl.workflow.server.WorkflowPrefManager;
import com.redsqirl.workflow.server.connect.HDFSInterface;
import com.redsqirl.workflow.server.datatype.MapRedCompressedType;
import com.redsqirl.workflow.server.datatype.MapRedCtrlATextType;
import com.redsqirl.workflow.server.datatype.MapRedTextType;
import com.redsqirl.workflow.server.enumeration.FieldType;
import com.redsqirl.workflow.server.interaction.PigGroupInteraction;
import com.redsqirl.workflow.server.interaction.PigOrderInteraction;
import com.redsqirl.workflow.server.interfaces.DFELinkProperty;
import com.redsqirl.workflow.server.interfaces.DFEOutput;
import com.redsqirl.workflow.server.interfaces.DataFlowElement;
import com.redsqirl.workflow.server.oozie.PigAction;
import com.redsqirl.workflow.utils.PigLanguageManager;

/**
 * Common functionalities for a Pig action.
 * A Pig action support as input and output 
 * 
 * @author marcos
 *
 */
public abstract class PigElement extends DataflowAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1651299366774317959L;
	
	private static Logger logger = Logger.getLogger(PigElement.class);
	
								/**
								 * Output Key
								 */
	public static final String key_output = "",
			key_output_audit = "audit",
			/**Input Key*/
			key_input = "in",
			/**Delimiter Key*/
			key_delimiter="delimiter",
			/**Condition Key*/
			key_condition = "condition",
			/**Output Type Key*/
			key_outputType = "output_type",
			/**Default Delimiter*/
			default_delimiter = new String(new char[]{'\001'}),
			/**Field Key for table*/
			key_fieldTable = "field",
			/**Parallel clause Key*/
			key_parallel = "parallel",
			/**Order Key*/
			key_order = "order",
			/**Order Type Key*/
			key_order_type = "order_type",
			/**Audit Key */
			key_audit="audit";
			
	
	/**Input Interaction for delimiter*/
	protected InputInteraction delimiterOutputInt;
	/**List Interaction for save output type*/
	protected ListInteraction savetypeOutputInt;
	/**Group Interaction*/
	public PigGroupInteraction groupingInt;
	
	/**
	 * Order Interaction
	 */
	protected PigOrderInteraction orderInt;
	
	/**
	 * Order Type Interaction
	 */
	protected ListInteraction orderTypeInt;
	
	/**
	 * Audit Interaction
	 */
	protected AppendListInteraction auditInt;
	
	/**
	 * Parallel clause Interaction
	 */
	protected InputInteraction parallelInt;
	
	/**Map of inputs*/
	protected Map<String, DFELinkProperty> input;
	/**
	 * Count of named bags
	 */
	private int nameCont;
	/**
	 * Constructor
	 * @param nbInMin
	 * @param nbInMax
	 * @param placeDelimiterInPage
	 * @throws RemoteException
	 */
	public PigElement( int nbInMin, int nbInMax,int placeDelimiterInPage) throws RemoteException {
		super(new PigAction());
		init(nbInMin,nbInMax);
		
		orderInt = new PigOrderInteraction(
				key_order, 
				PigLanguageManager.getText("pig.order_interaction.title"), 
				PigLanguageManager.getText("pig.order_interaction.legend"), 
				0, 0, this);
		
		orderTypeInt = new ListInteraction(
				key_order_type, 
				PigLanguageManager.getText("pig.order_type_interaction.title"), 
				PigLanguageManager.getText("pig.order_type_interaction.title"), 
				1, 0);
		
		orderTypeInt.setDisplayRadioButton(true);
		List<String> values = new ArrayList<String>();
		values.add("ASCENDING");
		values.add("DESCENDING");
		orderTypeInt.setPossibleValues(values);
		orderTypeInt.setReplaceDisable(true);
		
		String pigParallel = WorkflowPrefManager.getUserProperty(
				WorkflowPrefManager.user_pig_parallel,
				WorkflowPrefManager.getSysProperty(WorkflowPrefManager.sys_pig_parallel,
						""));
		
		parallelInt = new InputInteraction(
				key_parallel,
				PigLanguageManager.getText("pig.parallel_interaction.title"),
				PigLanguageManager.getText("pig.parallel_interaction.legend"), 
				placeDelimiterInPage, 0);
		parallelInt.setRegex("^$|^[1-9]\\d*$");
		parallelInt.setValue(pigParallel);

		delimiterOutputInt = new InputInteraction(
				key_delimiter,
				PigLanguageManager.getText("pig.delimiter_interaction.title"),
				PigLanguageManager.getText("pig.delimiter_interaction.legend"), 
				placeDelimiterInPage+1, 0);
		delimiterOutputInt.setRegex("^(#\\d{1,3}|.)?$");
		delimiterOutputInt.setValue("#1");


		savetypeOutputInt = new ListInteraction(
				key_outputType,
				PigLanguageManager.getText("pig.outputtype_interaction.title"),
				PigLanguageManager.getText("pig.outputtype_interaction.legend"), placeDelimiterInPage+2, 0);
		savetypeOutputInt.setDisplayRadioButton(true);
		List<String> saveTypePos = new LinkedList<String>();
		saveTypePos.add( new MapRedTextType().getTypeName());
		saveTypePos.add( new MapRedCompressedType().getTypeName());
		savetypeOutputInt.setPossibleValues(saveTypePos);
		savetypeOutputInt.setValue(new MapRedCompressedType().getTypeName());
		savetypeOutputInt.setReplaceDisable(true);
		
		auditInt= new AppendListInteraction(key_audit,
				  	PigLanguageManager.getText("pig.audit_interaction.title"),
				  	PigLanguageManager.getText("pig.audit_interaction.legend"), placeDelimiterInPage+3, 0);
		List<String> auditIntVal = new LinkedList<String>();
		auditIntVal.add(PigLanguageManager.getText("pig.audit_interaction_doaudit"));
		auditInt.setPossibleValues(auditIntVal);
		auditInt.setDisplayCheckBox(true);
		auditInt.setReplaceDisable(true);
		
	}
	/**
	 * Initialise the element
	 * @param nbInMin
	 * @param nbInMax
	 * @throws RemoteException
	 */
	protected void init(int nbInMin, int nbInMax) throws RemoteException{
		if(input == null){
			Map<String, DFELinkProperty> in = new LinkedHashMap<String, DFELinkProperty>();
			in.put(key_input, new DataProperty(MapRedCompressedType.class, nbInMin, nbInMax));
			input = in;
		}
	}
	
	public Map<String,List<String>> getDistinctValues() throws RemoteException{
		Map<String, List<String>> ans = null;
		List<DataFlowElement> lin = getInputComponent().get(key_input);
		if(lin != null && lin.size() > 0){
			if(groupingInt != null){
				ans = (new AuditGenerator()).readDistinctValuesAudit(getAliases().keySet().iterator().next(),lin.get(0).getDFEOutput().get(key_output_audit));
			}else{
				ans = (new AuditGenerator()).readDistinctValuesAudit(null,lin.get(0).getDFEOutput().get(key_output_audit));
			}
		}
		
		return ans;
	}
	

	/**
	 * Get the Query for the action
	 * @return query
	 * @throws RemoteException
	 */
	public abstract String getQuery() throws RemoteException;
	/**
	 * Get the Input Field
	 * @return input FieldList
	 * @throws RemoteException
	 */
	public abstract FieldList getInFields() throws RemoteException;
	/**
	 * Get the new Field
	 * @return new FieldList
	 * @throws RemoteException
	 */
	public abstract FieldList getNewField() throws RemoteException;
	/**
	 * Get the Input Relations
	 * @return Set of Input relations
	 * @throws RemoteException
	 */
	public Set<String> getInRelations() throws RemoteException{
		Set<String> ans = new LinkedHashSet<String>();
		HDFSInterface hInt = new HDFSInterface();
		List<DFEOutput> lOut = getDFEInput().get(key_input);
		Iterator<DFEOutput> it = lOut.iterator();
		while(it.hasNext()){
			ans.add(hInt.getRelation(it.next().getPath()));
		}
		return ans; 
	}
	/**
	 * Write the Oozie Action Files
	 * @param files
	 * @return <code>true</code> if write the oozie files was ok else <code>false</code>
	 * @throws RemoteException
	 */
	@Override
	public boolean writeOozieActionFiles(File[] files) throws RemoteException {
		logger.info("Write queries in file: "+files[0].getAbsolutePath());
		String toWrite = getQuery();
		
		int doAudit = auditInt.getValues().size();
		if(doAudit > 0){
			toWrite += "\n";
			toWrite += (new AuditGenerator()).getQuery(
					getDFEOutput().get(key_output), 
					getDFEOutput().get(key_output_audit), 
					parallelInt.getValue(),
					getCurrentName());
		}
		boolean ok = toWrite != null;
		if(ok){
			logger.info("Content of "+files[0].getName()+": "+toWrite);
			try {
				FileWriter fw = new FileWriter(files[0]);
				BufferedWriter bw = new BufferedWriter(fw);
				bw.write(toWrite);	
				bw.close();

			} catch (IOException e) {
				ok = false;
				logger.error("Fail to write into the file "+files[0].getAbsolutePath());
			}
		}

		logger.info("Write properties in file: "+files[1].getName());
		toWrite = getProperties(output.values().iterator().next());
		ok = toWrite != null;
		if(ok){
			try {
				logger.debug("Content of "+files[1]+": "+toWrite);
				FileWriter fw = new FileWriter(files[1]);
				BufferedWriter bw = new BufferedWriter(fw);
				bw.write(toWrite);	
				bw.close();
			} catch (IOException e) {
				ok = false;
				logger.error("Fail to write into the file "+files[1].getAbsolutePath(),e);
			}
		}
		return ok;
	}
	/**
	 * Get the properties of the input
	 * @param out
	 * @return properties String
	 * @throws RemoteException
	 */
	public String getProperties(DFEOutput out) throws RemoteException{
		String properties = "";

		properties += "number_fields="+out.getFields().getSize()+"\n";

		int cont = 0;
		for (String name : out.getFields().getFieldNames()){
			properties += "field"+cont+"_name="+name+"\n";
			properties += "field"+cont+"_value="+out.getFields().getFieldType(name)+"\n";
		}

		return properties;
	}
	/**
	 * Update the output of the action
	 */
	public String updateOut() throws RemoteException {
		String error = checkIntegrationUserVariables();
		logger.info("Error in updae out : "+error);
		if(error == null){
			FieldList new_field = getNewField();
			String type = savetypeOutputInt.getValue();
			DFEOutput out = output.get(key_output);
			logger.info("output type : "+type);
			
			if(out != null && !type.equalsIgnoreCase(out.getTypeName())){
				output.remove(key_output).clean();
			}
			
			if(output.get(key_output) == null){
				if(type.equalsIgnoreCase(new MapRedTextType().getTypeName())){
					output.put(key_output, new MapRedTextType());
				}else if(type.equalsIgnoreCase(new MapRedCompressedType().getTypeName())){
					output.put(key_output, new MapRedCompressedType());
				}
			}
			
			output.get(key_output).setFields(new_field);
			output.get(key_output).addProperty(MapRedTextType.key_delimiter, delimiterOutputInt.getValue());
			
			int doAudit = auditInt.getValues().size();
			if(doAudit > 0){
				if (output.get(key_output_audit) == null) {
					output.put(key_output_audit, new MapRedCtrlATextType());
				}
				try {
					FieldList fl = new OrderedFieldList();
					fl.addField("Legend", FieldType.STRING);
					Iterator<String> it = output.get(key_output)
							.getFields().getFieldNames().iterator();
					while (it.hasNext()) {
						fl.addField("AUDIT_" + it.next(), FieldType.STRING);
					}
					output.get(key_output_audit).setFields(fl);
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
				}
			}else{
				output.remove(key_output_audit);
			}
		}
		return error;
	}

	/**
	 * Get the input map
	 * @return Map of input
	 * @throws RemoteException
	 */
	public Map<String, DFELinkProperty> getInput() throws RemoteException {
		return input;
	}
	
	/**
	 * Get the remove query piece of the query
	 * @param out
	 * @return query
	 * @throws RemoteException
	 */
	public String getRemoveQueryPiece(String out) throws RemoteException{
		logger.debug("create remove...");
		return "rmf "+out;
	}
	/**
	 * Get the load query piece for the query
	 * @param out
	 * @return query
	 * @throws RemoteException
	 */
	public String getLoadQueryPiece(DFEOutput out) throws RemoteException{
		logger.debug("create load...");

		String delimiter = out.getProperty(MapRedTextType.key_delimiter);
		delimiter = ((MapRedTextType)out).getPigDelimiter();
		if (delimiter == null){
			delimiter = default_delimiter;
		}

		String function = getLoadStoreFuncion(out, delimiter);
		String createSelect = "LOAD '" + out.getPath() + "'";
		
		if (function != null){
			createSelect += " USING "+function;
		}
		createSelect += " as (";

		Iterator<String> it = out.getFields().getFieldNames().iterator();
		logger.info("attribute list size : "+out.getFields().getSize());
		while (it.hasNext()){
			String e = it.next();
			createSelect += e+":"+PigTypeConvert.getPigType(out.getFields().getFieldType(e));
			if (it.hasNext()){
				createSelect += ", ";
			}
		}
		createSelect +=")";

		return createSelect;
	}

	public String getStoreQueryPiece(DFEOutput out, String relationName) throws RemoteException{
		MapRedTextType output = (MapRedTextType) getDFEOutput().get(key_output); 
		String delimiter = output.getPigDelimiter();

		String function = getStoreFunction(delimiter);
		logger.info(function);
		
		String query = "STORE "+relationName+" INTO '" + out.getPath() + "'";
				
		query += " USING "+function;
		query += ";";
				
		return query;
	}

	/**
	 * Get the store part of the query
	 * @param delimiter
	 * @return query
	 * @throws RemoteException
	 */
	public String getStoreFunction(String delimiter) throws RemoteException{
		String type = "";
		String function = null;
		if(delimiter==null || delimiter.equalsIgnoreCase("")){
			delimiter ="|";
		}
		try{
			
			type = savetypeOutputInt.getTree().getFirstChild("list").getFirstChild("output").getFirstChild().getHead();
			logger.info("type: "+type);
			if(type.equalsIgnoreCase("TEXT MAP-REDUCE DIRECTORY")||type.equalsIgnoreCase("COMPRESSED MAP-REDUCE DIRECTORY")){
				function = "PigStorage('"+delimiter+"')";//TODO Schema IS a problem, '-schema');
			}
			if (type.equalsIgnoreCase("BINARY MAP-REDUCE DIRECTORY")){
				function = "BinStorage()";
			}
			logger.info("Storing via "+function);
			return function;
		}catch (Exception e){
			logger.error("There was an error getting the output type");
		}
		return null;

	}
	/**
	 * Get the function to load or store the data
	 * @param out
	 * @param delimiter
	 * @return function
	 * @throws RemoteException
	 */
	private String getLoadStoreFuncion(DFEOutput out, String delimiter) throws RemoteException{
		String function = null;
		if (out.getTypeName().equals("TEXT MAP-REDUCE DIRECTORY") ||
				out.getTypeName().equals("COMPRESSED MAP-REDUCE DIRECTORY")){
			function = "PigStorage('"+delimiter+"')";
		}
		else if (out.getTypeName().equals("BINARY MAP-REDUCE DIRECTORY")){
			function = "BinStorage()";
		}
		return function;
	}
	/**
	 * Get the Current Name of the bag
	 * @return current Name
	 */
	public String getCurrentName(){
		return "A"+nameCont;
	}
	/**
	 * Get the Next Name in the bag
	 * @return next Name
	 */
	public String getNextName(){
		nameCont++;
		return "A"+nameCont;
	}
	
	/**
	 * Get the Previous Name in the bag
	 * @return next Name
	 */
	public String getPreviousName(){
		return "A"+(nameCont-1);
	}
	/**
	 * Get the grouping interaction
	 * @return groupingInt
	 */
	public PigGroupInteraction getGroupingInt() {
		return groupingInt;
	}
	
	/**
	 * Get the ordering interaction
	 * @return groupingInt
	 */
	public PigOrderInteraction getOrderInt() {
		return orderInt;
	}
	
	/**
	 * Get the ordering interaction
	 * @return groupingInt
	 */
	public ListInteraction getSaveTypeInt() {
		return savetypeOutputInt;
	}
}
