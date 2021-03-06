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

import static org.junit.Assert.assertTrue;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.redsqirl.utils.FieldList;
import com.redsqirl.utils.OrderedFieldList;
import com.redsqirl.workflow.server.connect.HiveInterface;
import com.redsqirl.workflow.server.datatype.HiveTypePartition;
import com.redsqirl.workflow.server.enumeration.FieldType;
import com.redsqirl.workflow.test.TestUtils;

public class HiveTypePartitionTests {
	Logger logger = Logger.getLogger(getClass());

	Map<String, String> getColumns() {
		Map<String, String> ans = new HashMap<String, String>();
		ans.put(HiveInterface.key_columns, "ID STRING, VALUE INT");
		return ans;
	}

	Map<String, String> getPartitions() {
		Map<String, String> ans = new HashMap<String, String>();
		ans.put(HiveInterface.key_columns, "ID STRING, VALUE INT");
		ans.put(HiveInterface.key_partitions, "COUNTRY STRING, DT STRING");
		return ans;
	}

	FieldList getFeatures() throws RemoteException {
		FieldList ans = new OrderedFieldList();
		ans.addField("ID", FieldType.STRING);
		ans.addField("VALUE", FieldType.INT);
		return ans;
	}

	FieldList getFeaturesWPart() throws RemoteException {
		FieldList ans = new OrderedFieldList();
		ans.addField("ID", FieldType.STRING);
		ans.addField("VALUE", FieldType.INT);
		ans.addField("COUNTRY", FieldType.STRING);
		ans.addField("DT", FieldType.STRING);
		return ans;
	}

	String getParts() {
		return "COUNTRY='Ireland',DT='20120201'";
	}

	@Test
	public void getPartitionsFilterTest() {
		TestUtils.logTestTitle("HiveTypeTests#getPartitionsFilterTest");
		try {
			HiveInterface hInt = new HiveInterface();
			HiveTypePartition part = new HiveTypePartition();
			
			String new_path1 = TestUtils.getTablePath(1);
			hInt.delete(new_path1);
			String newPart1 = new_path1+"/"+getParts();
			hInt.create(newPart1, getPartitions());
			hInt.goTo(new_path1);
			Map<String, Map<String, String>> results = hInt.getChildrenProperties(true);
			logger.info("results "+results.toString());
			int size = results.size();
			assertTrue("test 1 size : "+size , size == 2);
			logger.info("partitions "+hInt.getPartitions(new_path1, new ArrayList<String>()));
			boolean error = hInt.exists(new_path1+"/country=Ireland");
			assertTrue("Error "+error + " "+new_path1+"/country=Ireland",error);
			
			hInt.goTo(new_path1+"/country=Ireland");
			logger.info("current path "+hInt.getPath());
			results = hInt.getChildrenProperties(true);
			logger.info("results "+results.toString());
			size = results.size();
			assertTrue("test 2 size : "+size , size == 1);
			
			hInt.goTo(new_path1+"/dt=20120201");
			results = hInt.getChildrenProperties(true);
			logger.info("results "+results.toString());
			size = results.size();
			assertTrue("test 3 size : "+size , size == 1);
					
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			assertTrue(e.getMessage(), false);
		}
	}

}
