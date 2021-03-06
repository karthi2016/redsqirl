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

package com.redsqirl.workflow.server.action;


import java.io.File;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.redsqirl.utils.FieldList;
import com.redsqirl.utils.OrderedFieldList;
import com.redsqirl.utils.Tree;
import com.redsqirl.utils.TreeNonUnique;
import com.redsqirl.workflow.server.DataflowAction;
import com.redsqirl.workflow.server.OozieUniqueActionAbs;
import com.redsqirl.workflow.server.enumeration.FieldType;
import com.redsqirl.workflow.server.interfaces.DFEOptimiser;
import com.redsqirl.workflow.server.interfaces.DFEOutput;

/**
 * Common functionalities for a Sql action. A Sql action support as input and
 * output
 * 
 * 
 * @author marcos
 * 
 */
public abstract class SqlElement extends DataflowAction {

	/**
	 * RMI id
	 */
	private static final long serialVersionUID = -1651299366774317959L;

	private static Logger logger = Logger.getLogger(SqlElement.class);
	
	/**
	 * Names of different elements
	 */
	public static final 
	String key_output = "", 
			key_input = "in",
			key_condition = "Condition", 
			key_outputType = "Output_Type",
			key_order = "Order";

	/**
	 * Common interactions
	 */
	protected SqlGroupInteraction groupingInt;
	
	
	public SqlElement(OozieUniqueActionAbs action)
			throws RemoteException {
		super(action);

	}
	
	public SqlElement(OozieUniqueActionAbs action,DFEOptimiser optimiser)
			throws RemoteException {
		super(action,optimiser);
	}

	/**
	 * Get the query to write into a script
	 * 
	 * @return The query to write into a script
	 * @throws RemoteException
	 */
	public abstract String getQuery() throws RemoteException;

	
	/**
	 * Write the Oozie Action Files 
	 * @param files
	 * @throws RemoteException
	 */
	public abstract boolean writeOozieActionFiles(File[] files) throws RemoteException;
	
	
	/**
	 * Update the output of the action
	 * @throws RemoteException
	 */
	public abstract String updateOut() throws RemoteException;
	
	/**
	 * Get the field list that are generated from this action
	 * @return new FieldList
	 * @throws RemoteException
	 */
	public abstract FieldList getNewFields() throws RemoteException;
	
	/**
	 * Get the Group By Interaction
	 * @return groupingInt
	 */
	public SqlGroupInteraction getGroupingInt() {
		return groupingInt;
	}
	/**
	 * Get the Group By Fields
	 * @return Set of group by fields
	 * @throws RemoteException
	 */
	public Set<String> getGroupByFields() throws RemoteException {
		Set<String> fields = null;
		SqlGroupInteraction group = getGroupingInt();
		if (group != null) {
			fields = new HashSet<String>();
			Tree<String> tree = group.getTree();
			logger.info("group tree : "
					+ ((TreeNonUnique<String>) tree).toString());
			if (tree != null
					&& tree.getFirstChild("applist").getFirstChild("output")
							.getSubTreeList().size() > 0) {
				Iterator<Tree<String>> values = tree.getFirstChild("applist")
						.getFirstChild("output").getChildren("value")
						.iterator();
				while (values.hasNext()) {
					fields.add(values.next().getFirstChild().getHead());
				}
			}
		} else {
			logger.info("group interaction is null");
		}

		return fields;
	}
	
	/**
	 * Get the fields that are in the input
	 * @return input FieldList
	 * @throws RemoteException
	 */
	public FieldList getInFields() throws RemoteException {
		FieldList ans = new OrderedFieldList(false);
		Map<String, DFEOutput> aliases = getAliases();

		Iterator<String> it = aliases.keySet().iterator();
		while (it.hasNext()) {
			String alias = it.next();
			FieldList mapTable = aliases.get(alias).getFields();
			Iterator<String> itFeat = mapTable.getFieldNames().iterator();
			while (itFeat.hasNext()) {
				String cur = itFeat.next();
				ans.addField(alias + "." + cur, mapTable.getFieldType(cur));
			}
		}
		return ans;
	}
	
	/* Get the input fields with the alias
	 * @param alias
	 * @return FieldList
	 * @throws RemoteException
	 */
	public FieldList getInFields(String alias) throws RemoteException {
		FieldList ans = null;
		Map<String, DFEOutput> aliases = getAliases();
		if(aliases.get(alias) != null){
			ans = new OrderedFieldList(false);
			FieldList mapTable = aliases.get(alias).getFields();
			Iterator<String> itFeat = mapTable.getFieldNames().iterator();
			while (itFeat.hasNext()) {
				String cur = itFeat.next();
				ans.addField(alias + "." + cur, mapTable.getFieldType(cur));
			}
		}
		return ans;
	}

	public abstract Map<String,DFEOutput> getJoinAliases() throws RemoteException;
	
	
	

	public List<String> getTypes(){
		List<String> types = new ArrayList<String>(FieldType.values().length);
		for(FieldType ft:FieldType.values()){
			types.add(ft.name());
		}
		logger.info(types);
		return types;
	}
}
