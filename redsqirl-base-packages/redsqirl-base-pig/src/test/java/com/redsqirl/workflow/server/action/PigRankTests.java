package com.redsqirl.workflow.server.action;

import static org.junit.Assert.assertTrue;

import java.rmi.RemoteException;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.oozie.client.OozieClient;
import org.junit.Test;

import com.redsqirl.workflow.server.OozieManager;
import com.redsqirl.workflow.server.Workflow;
import com.redsqirl.workflow.server.connect.HDFSInterface;
import com.redsqirl.workflow.server.enumeration.SavingState;
import com.redsqirl.workflow.server.interfaces.DataFlowElement;
import com.redsqirl.workflow.test.TestUtils;

public class PigRankTests {

	static Logger logger = Logger.getLogger(PigRankTests.class);

	public static DataFlowElement createPigWithSrc(Workflow w,
			DataFlowElement src, HDFSInterface hInt) throws RemoteException,
			Exception {
		String error = null;
		String idHS = w.addElement((new PigRank()).getName());
		logger.debug("Pig group rank: " + idHS);

		PigRank pig = (PigRank) w.getElement(idHS);

		logger.info(PigBinarySource.out_name + " " + src.getComponentId());
		logger.debug(PigSelect.key_input + " " + idHS);

		error = w.addLink(PigBinarySource.out_name, src.getComponentId(),
				PigSelect.key_input, idHS);
		assertTrue("pig select add link: " + error, error == null);

		updatePig(w, pig, hInt);

		logger.debug("HS update out...");
		error = pig.updateOut();
		assertTrue("pig select update: " + error, error == null);
		logger.debug("Features "
				+ pig.getDFEOutput().get(PigSelect.key_output).getFields());

		pig.getDFEOutput()
				.get(PigSelect.key_output)
				.generatePath(System.getProperty("user.name"),
						pig.getComponentId(), PigSelect.key_output);

		return pig;
	}

	public static DataFlowElement createPigWithPig(Workflow w,
			DataFlowElement src, HDFSInterface hInt) throws RemoteException,
			Exception {
		String error = null;
		String idHS = w.addElement(new PigSelect().getName());
		PigRank pig = (PigRank) w.getElement(idHS);
		logger.info("Pig select: " + idHS);

		w.addLink(PigSelect.key_output, src.getComponentId(),
				PigSelect.key_input, idHS);
		assertTrue("pig select add input: " + error, error == null);

		updatePig(w, pig, hInt);
		logger.info("Updating Pig");

		logger.debug("HS update out...");
		error = pig.updateOut();
		assertTrue("pig select update: " + error, error == null);

		return pig;
	}

	public static void updatePig(Workflow w, PigRank pig,
			HDFSInterface hInt) throws RemoteException, Exception {

		logger.info("update pig...");
		pig.rankUpdate();

		List<String> vals = pig.rank.getPossibleValues();
		String rnk = vals.get(1);

		pig.orderTypeInt.setValue(pig.orderTypeInt.getPossibleValues().get(0));
		
		pig.rank.setValue(rnk);
		
		pig.getFilterInt().update();
		
		pig.getFilterInt().setValue(rnk + " < 3");

		String error = pig.updateOut();
		assertTrue("pig select update: " + error, error == null);
	}

	@Test
	public void basic() {

		TestUtils.logTestTitle(getClass().getName() + "#basic");
		String error = null;
		try {
			Workflow w = new Workflow("workflow1_" + getClass().getName());
			HDFSInterface hInt = new HDFSInterface();
			String new_path1 = TestUtils.getPath(1);
			String new_path2 = TestUtils.getPath(2);

			hInt.delete(new_path1);
			hInt.delete(new_path2);

			DataFlowElement src = PigTestUtils.createSrc_ID_VALUE(w, hInt,
					new_path1);
			PigRank pig = (PigRank) createPigWithSrc(w, src, hInt);

			logger.info(pig.getQuery());

			pig.getDFEOutput().get(PigSelect.key_output)
					.setSavingState(SavingState.RECORDED);
			pig.getDFEOutput().get(PigSelect.key_output).setPath(new_path2);

			/*logger.info("run...");
			OozieClient wc = OozieManager.getInstance().getOc();
			logger.info("Got Oozie Client");
			error = w.run();
			assertTrue("Job submition failed: " + error, error == null);
			String jobId = w.getOozieJobId();
			if (jobId == null) {
				assertTrue("jobId cannot be null", false);
			}
			logger.info(jobId);

			// wait until the workflow job finishes printing the status every
			// 10 seconds
			while (wc.getJobInfo(jobId).getStatus() == org.apache.oozie.client.WorkflowJob.Status.RUNNING) {
				System.out.println("Workflow job running ...");
				logger.info("Workflow job running ...");
				Thread.sleep(10 * 1000);
			}
			logger.info("Workflow job completed ...");
			logger.info(wc.getJobInfo(jobId));
			error = wc.getJobInfo(jobId).toString();
			assertTrue(error, error.contains("SUCCEEDED"));*/
			 
		} catch (Exception e) {
			logger.error(e.toString(), e);

			assertTrue("error : " + e.toString(), false);
		}
	}

	// @Test
	public void oneBridge() {
		TestUtils.logTestTitle(getClass().getName() + "#oneBridge");

		try {
			Workflow w = new Workflow("workflow_test2");
			String error = null;

			HDFSInterface hInt = new HDFSInterface();
			String new_path1 = TestUtils.getPath(1);
			String new_path2 = TestUtils.getPath(2);

			hInt.delete(new_path1);
			hInt.delete(new_path2);
			logger.info("deleted paths if existed");

			DataFlowElement src = PigTestUtils.createSrc_ID_VALUE(w, hInt,
					new_path1);
			DataFlowElement pig = createPigWithPig(w,
					createPigWithSrc(w, src, hInt), hInt);

			pig.getDFEOutput().get(PigSelect.key_output)
					.setSavingState(SavingState.RECORDED);
			pig.getDFEOutput().get(PigSelect.key_output).setPath(new_path2);

			logger.info("run...");
			error = w.run();
			assertTrue("Job submition failed: " + error, error == null);
			String jobId = w.getOozieJobId();
			OozieClient wc = OozieManager.getInstance().getOc();

			// wait until the workflow job finishes printing the status every 10
			// seconds
			while (wc.getJobInfo(jobId).getStatus() == org.apache.oozie.client.WorkflowJob.Status.RUNNING) {
				System.out.println("Workflow job running ...");
				Thread.sleep(10 * 1000);
			}
			logger.info("Workflow job completed ...");
			error = wc.getJobInfo(jobId).toString();
			logger.debug(error);
			assertTrue(error, error.contains("SUCCEEDED"));
		} catch (Exception e) {
			logger.error("Unexpected exception: " + e.toString());
			assertTrue(e.getMessage(), false);
		}
	}

}
