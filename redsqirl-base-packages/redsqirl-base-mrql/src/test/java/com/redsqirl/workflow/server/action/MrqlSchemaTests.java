package com.redsqirl.workflow.server.action;

import static org.junit.Assert.assertTrue;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.oozie.client.OozieClient;
import org.junit.Test;

import com.redsqirl.utils.Tree;
import com.redsqirl.workflow.server.InputInteraction;
import com.redsqirl.workflow.server.ListInteraction;
import com.redsqirl.workflow.server.OozieManager;
import com.redsqirl.workflow.server.Workflow;
import com.redsqirl.workflow.server.connect.HDFSInterface;
import com.redsqirl.workflow.server.datatype.MapRedTextType;
import com.redsqirl.workflow.server.enumeration.SavingState;
import com.redsqirl.workflow.server.interaction.MrqlFilterInteraction;
import com.redsqirl.workflow.server.interaction.MrqlOrderInteraction;
import com.redsqirl.workflow.server.interaction.MrqlTableSelectInteraction;
import com.redsqirl.workflow.server.interfaces.DataFlowElement;
import com.redsqirl.workflow.test.TestUtils;

public class MrqlSchemaTests {

	static Logger logger = Logger.getLogger(MrqlSchemaTests.class);

	
	public static DataFlowElement createMrqlWithSrc(
			Workflow w,
			DataFlowElement src,
			HDFSInterface hInt) throws RemoteException, Exception{
		String error = null;
		String idHS = w.addElement((new MrqlSelect()).getName());
		logger.debug("Mrql select: "+idHS);
		
		MrqlSelect mrql = (MrqlSelect) w.getElement(idHS);
		
		logger.info(MrqlCompressSource.out_name+" "+src.getComponentId());
		logger.debug(MrqlSelect.key_input+" "+idHS);
		
		error = w.addLink(
				MrqlCompressSource.out_name, src.getComponentId(), 
				MrqlSelect.key_input, idHS);
		assertTrue("mrql select add link: "+error,error == null);
		
		updateMrql(w,mrql,hInt);
		
		
		logger.debug("HS update out...");
		error = mrql.updateOut();
		assertTrue("mrql select update: "+error,error == null);
		logger.debug("Features "+mrql.getDFEOutput().get(MrqlSelect.key_output).getFields());
		
		mrql.getDFEOutput().get(MrqlSelect.key_output).generatePath(
				System.getProperty("user.name"), 
				mrql.getComponentId(), 
				MrqlSelect.key_output);
		
		
		return mrql;
	}
	
	public static void updateMrql(
			Workflow w,
			MrqlSelect mrql,
			HDFSInterface hInt) throws RemoteException, Exception{
		
		logger.info("update mrql...");
		
		logger.info("got dfe");
		MrqlFilterInteraction ci = mrql.getCondInt();
		logger.info("update mrql... get condition");
		mrql.update(ci);
		logger.info("update mrql...update");
		Tree<String> cond = ci.getTree()
				.getFirstChild("editor").getFirstChild("output");
		logger.info("update mrql...get condition tree");
		cond.add("VALUE < 10");
		logger.info("update mrql...add to condition tree");
		
		logger.info("update mrql... update");
		MrqlTableSelectInteraction tsi = mrql.gettSelInt();
		logger.info("update mrql... get table select interaction");
		
		mrql.update(tsi);
		{
			Tree<String> out = tsi.getTree().getFirstChild("table");
			Tree<String> rowId = out.add("row");
			rowId.add(MrqlTableSelectInteraction.table_feat_title).add("ID");
			rowId.add(MrqlTableSelectInteraction.table_op_title).add("ID");
			rowId.add(MrqlTableSelectInteraction.table_type_title).add("STRING");
			rowId = out.add("row");
			rowId.add(MrqlTableSelectInteraction.table_feat_title).add("VALUE");
			rowId.add(MrqlTableSelectInteraction.table_op_title).add("VALUE");
			rowId.add(MrqlTableSelectInteraction.table_type_title).add("INT");
		}

		MrqlOrderInteraction oi = mrql.getOrderInt();
		mrql.update(oi);
		List<String> values = new ArrayList<String>();
		values.add("ID");
		oi.setValues(values);
		
		ListInteraction ot = (ListInteraction) mrql.getInteraction(MrqlElement.key_order_type);
		mrql.update(oi);
		ot.setValue("ASCENDING");
		
		InputInteraction dl = (InputInteraction) mrql.getInteraction(MrqlElement.key_delimiter);
		mrql.update(dl);
		dl.setValue(",");
		
		ListInteraction st = (ListInteraction) mrql.getInteraction(MrqlElement.key_outputType);
		mrql.update(st);
		st.setValue(new MapRedTextType().getTypeName());
		
		logger.info("HS update out...");
		String error = mrql.updateOut();
		assertTrue("mrql select update: "+error,error == null);
	}
	
	
	
	@Test
	public void basic(){
		
		TestUtils.logTestTitle(getClass().getName()+"#basic");
		String error = null;
		try{
			Workflow w = new Workflow("workflow1_"+getClass().getName());
			HDFSInterface hInt = new HDFSInterface();
			String new_path1 = TestUtils.getPath(1);
			String new_path2 = TestUtils.getPath(2); 
			
			hInt.delete(new_path1);
			hInt.delete(new_path2);
			
			DataFlowElement src = MrqlTestUtils.createSrc_ID_VALUE(w,hInt,new_path1);
			MrqlSelect mrql = (MrqlSelect)createMrqlWithSrc(w,src,hInt);

			mrql.getDFEOutput().get(MrqlSelect.key_output).setSavingState(SavingState.RECORDED);
			mrql.getDFEOutput().get(MrqlSelect.key_output).setPath(new_path2);
			
			logger.info("run...");
			OozieClient wc = OozieManager.getInstance().getOc();
			logger.info("Got Oozie Client");
			error = w.run();
			assertTrue("Job submition failed: "+error, error == null);
			String jobId = w.getOozieJobId();
			if(jobId == null){
				assertTrue("jobId cannot be null", false);
			}
			logger.info(jobId);
			
			// wait until the workflow job finishes printing the status every 10 seconds
		    while(
		    		wc.getJobInfo(jobId).getStatus() == 
		    		org.apache.oozie.client.WorkflowJob.Status.RUNNING) {
		    	System.out.println("Workflow job running ...");
		    	logger.info("Workflow job running ...");
		        Thread.sleep(10 * 1000);
		    }
		    logger.info("Workflow job completed ...");
		    
		    String idSource2 = w.addElement((new MrqlTextSource()).getName());
			MrqlTextSource src2 = (MrqlTextSource)w.getElement(idSource2);
			
			src2.update(src2.getInteraction(MrqlTextSource.key_dataset));
			Tree<String> dataSetTree = src2.getInteraction(MrqlTextSource.key_dataset).getTree();
			dataSetTree.getFirstChild("browse").getFirstChild("output").add("path").add(new_path2);
			dataSetTree.getFirstChild("browse").getFirstChild("output").add("property").add(MapRedTextType.key_delimiter).add(",");
			
			error = src2.updateOut();

			assertTrue("source update: "+error,error == null);
			
			assertTrue("number of fields in source should be 2 instead of " + 
					src2.getDFEOutput().get(MrqlTextSource.out_name).getFields().getSize(),
					src2.getDFEOutput().get(MrqlTextSource.out_name).getFields().getSize() == 2);
			
			assertTrue("field list " + 
					src2.getDFEOutput().get(MrqlTextSource.out_name).getFields().getFieldNames(),
					src2.getDFEOutput().get(MrqlTextSource.out_name).getFields().getFieldNames().contains("ID"));
			assertTrue("field list " + 
					src2.getDFEOutput().get(MrqlTextSource.out_name).getFields().getFieldNames(),
					src2.getDFEOutput().get(MrqlTextSource.out_name).getFields().getFieldNames().contains("VALUE"));
		    
		    logger.info(wc.getJobInfo(jobId));
		    error = wc.getJobInfo(jobId).toString();
		    assertTrue(error, error.contains("SUCCEEDED"));
		}catch(Exception e){
			logger.error("Exception : ",e);
			assertTrue(e.getMessage(),false);
		}
	}
}