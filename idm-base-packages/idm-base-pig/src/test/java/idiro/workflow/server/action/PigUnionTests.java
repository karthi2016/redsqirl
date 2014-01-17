package idiro.workflow.server.action;

import static org.junit.Assert.assertTrue;
import idiro.hadoop.NameNodeVar;
import idiro.utils.Tree;
import idiro.workflow.server.OozieManager;
import idiro.workflow.server.Workflow;
import idiro.workflow.server.connect.HDFSInterface;
import idiro.workflow.server.datatype.MapRedTextType;
import idiro.workflow.server.enumeration.SavingState;
import idiro.workflow.server.interfaces.DataFlowElement;
import idiro.workflow.test.TestUtils;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.log4j.Logger;
import org.apache.oozie.client.OozieClient;
import org.junit.Test;

public class PigUnionTests {

	Logger logger = Logger.getLogger(getClass());

	Map<String,String> getProperties(){
		Map<String,String> ans = new HashMap<String,String>();
		return ans;
	}
	

	public void createHDFSFile(Path p, String containt) throws IOException {
		FileSystem fileSystem = NameNodeVar.getFS();

		// Check if the file already exists
		if (fileSystem.exists(p)) {
			logger.warn("File " + p.toString() + " already exists");
			return;
		}

		// Create a new file and write data to it.
		fileSystem.mkdirs(p);
		FSDataOutputStream out = fileSystem.create(new Path(p, "part-0000"));
		out.write(containt.getBytes());
		out.close();
		fileSystem.close();
	}
	
	public void createInput(Path p) throws IOException {
		String content = "A,1\n";
		content += "B,2\n";
		content += "C,3\n";
		content += "D,4\n";
		content += "E,5\n";
		content += "F,6\n";
		content += "G,7\n";

		createHDFSFile(p, content);
	}
	
	public DataFlowElement createSrc(
			Workflow w,
			HDFSInterface hInt, 
			String new_path1 ) throws RemoteException, Exception{
		
		String idSource = w.addElement((new Source()).getName());
		Source src = (Source)w.getElement(idSource);
		
		createInput(new Path(new_path1));

		
		src.update(src.getInteraction(Source.key_datatype));
		Tree<String> dataTypeTree = src.getInteraction(Source.key_datatype).getTree();
		dataTypeTree.getFirstChild("list").getFirstChild("output").add("HDFS");
		
		src.update(src.getInteraction(Source.key_datasubtype));
		Tree<String> dataSubTypeTree = src.getInteraction(Source.key_datasubtype).getTree();
		dataSubTypeTree.getFirstChild("list").getFirstChild("output").add(MapRedTextType.class.getSimpleName());

		src.update(src.getInteraction(Source.key_dataset));
		Tree<String> dataSetTree = src.getInteraction(Source.key_dataset).getTree();
		dataSetTree.getFirstChild("browse").getFirstChild("output").add("path").add(new_path1);
		dataSetTree.getFirstChild("browse").getFirstChild("output").add("property").add(MapRedTextType.key_delimiter).add(",");

		Tree<String> feat1 = dataSetTree.getFirstChild("browse")
				.getFirstChild("output").add("feature");
		feat1.add("name").add("ID");
		feat1.add("type").add("STRING");

		Tree<String> feat2 = dataSetTree.getFirstChild("browse")
				.getFirstChild("output").add("feature");
		feat2.add("name").add("VALUE");
		feat2.add("type").add("INT");
		
		String error = src.updateOut();
		assertTrue("source update: "+error,error == null);
		
		return src;
	}
	
	public DataFlowElement createPigWithSrc(
			Workflow w,
			DataFlowElement src1,
			DataFlowElement src2,
			HDFSInterface hInt) throws RemoteException, Exception{
		String error = null;
		String idHS = w.addElement((new PigUnion()).getName());
		logger.debug("Pig select: "+idHS);
		
		PigUnion pig = (PigUnion) w.getElement(idHS);
		
		logger.debug(Source.out_name+" "+src1.getComponentId());
		logger.debug(PigUnion.key_input+" "+idHS);
		
		w.addLink(
				Source.out_name, src1.getComponentId(), 
				PigUnion.key_input, idHS);
		assertTrue("pig select add input: "+error,error == null);
		
		logger.debug(Source.out_name+" "+src2.getComponentId());
		logger.debug(PigUnion.key_input+" "+idHS);
		
		w.addLink(
				Source.out_name, src2.getComponentId(), 
				PigUnion.key_input, idHS);
		assertTrue("pig select add input: "+error,error == null);
		
		String alias1 ="";
		String alias2 = "";
		Iterator<String> itAlias = pig.getAliases().keySet().iterator();
		while(itAlias.hasNext()){
			String swp = itAlias.next();
			if(pig.getAliases().get(swp).getPath().equals(TestUtils.getPath(1))){
				alias1 = swp;
			}else{
				alias2 = swp;
			}
		}
		
		updatePig(w,pig,alias1,alias2,hInt);
		logger.debug("Features "+pig.getDFEOutput().get(PigUnion.key_output).getFeatures());
		
		pig.getDFEOutput().get(PigUnion.key_output).generatePath(
				System.getProperty("user.name"), 
				pig.getComponentId(), 
				PigUnion.key_output);
		
		return pig;
	}
	
	public void updatePig(
			Workflow w,
			PigUnion pig,
			String relation_from_1,
			String relation_from_2,
			HDFSInterface hInt) throws RemoteException, Exception{
		
		logger.debug("update pig...");
		
		PigTableUnionInteraction tsi = pig.gettUnionSelInt();
		pig.update(tsi);
		{
			Tree<String> out = tsi.getTree().getFirstChild("table");
			Tree<String> rowId = out.add("row");
			rowId.add(PigTableUnionInteraction.table_relation_title).add(relation_from_1);
			rowId.add(PigTableUnionInteraction.table_feat_title).add("ID");
			rowId.add(PigTableUnionInteraction.table_op_title).add(relation_from_1+".ID");
			rowId.add(PigTableUnionInteraction.table_type_title).add("STRING");
			rowId = out.add("row");
			rowId.add(PigTableUnionInteraction.table_relation_title).add(relation_from_1);
			rowId.add(PigTableUnionInteraction.table_feat_title).add("VALUE");
			rowId.add(PigTableUnionInteraction.table_op_title).add(relation_from_1+".VALUE");
			rowId.add(PigTableUnionInteraction.table_type_title).add("INT");
			rowId = out.add("row");
			rowId.add(PigTableUnionInteraction.table_relation_title).add(relation_from_2);
			rowId.add(PigTableUnionInteraction.table_feat_title).add("ID");
			rowId.add(PigTableUnionInteraction.table_op_title).add(relation_from_2+".ID");
			rowId.add(PigTableUnionInteraction.table_type_title).add("STRING");
			rowId = out.add("row");
			rowId.add(PigTableUnionInteraction.table_relation_title).add(relation_from_2);
			rowId.add(PigTableUnionInteraction.table_feat_title).add("VALUE");
			rowId.add(PigTableUnionInteraction.table_op_title).add(relation_from_2+".VALUE");
			rowId.add(PigTableUnionInteraction.table_type_title).add("INT");
		}

		logger.debug("HS update out...");
		String error = pig.updateOut();
		assertTrue("pig union update: "+error,error == null);
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
			String new_path3 = TestUtils.getPath(3); 
			
			hInt.delete(new_path1);
			hInt.delete(new_path2);
			hInt.delete(new_path3);
			
			DataFlowElement src1 = createSrc(w,hInt,new_path1);
			DataFlowElement src2 = createSrc(w,hInt,new_path2);
			DataFlowElement pig = createPigWithSrc(w,src1,src2,hInt);

			pig.getDFEOutput().get(PigUnion.key_output).setSavingState(SavingState.RECORDED);
			pig.getDFEOutput().get(PigUnion.key_output).setPath(new_path3);
			
			//run
			error = w.run();
			assertTrue("Job submition failed: "+error, error == null);
			String jobId = w.getOozieJobId();
			if(jobId == null){
				assertTrue("jobId cannot be null", false);
			}
			logger.info(jobId);
			
			
			OozieClient wc = OozieManager.getInstance().getOc();
			
			// wait until the workflow job finishes printing the status every 10 seconds
		    while(
		    		wc.getJobInfo(jobId).getStatus() == 
		    		org.apache.oozie.client.WorkflowJob.Status.RUNNING) {
		        System.out.println("Workflow job running ...");
		        Thread.sleep(10 * 1000);
		    }
		    logger.info("Workflow job completed ...");
		    logger.info(wc.getJobInfo(jobId));
		    error = wc.getJobInfo(jobId).toString();
		    assertTrue(error, error.contains("SUCCEEDED"));
		}catch(Exception e){
			logger.error(e.getMessage());
			assertTrue(e.getMessage(),false);
		}
	}
}
