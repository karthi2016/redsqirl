package idiro.workflow.server.action;

import static org.junit.Assert.assertTrue;
import idiro.utils.Tree;
import idiro.utils.TreeNonUnique;
import idiro.workflow.server.DataflowAction;
import idiro.workflow.server.OozieManager;
import idiro.workflow.server.Workflow;
import idiro.workflow.server.action.utils.TestUtils;
import idiro.workflow.server.connect.HiveInterface;
import idiro.workflow.server.datatype.HiveType;
import idiro.workflow.server.enumeration.SavingState;
import idiro.workflow.server.interfaces.DFEOutput;
import idiro.workflow.test.HiveInterfaceTester;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.oozie.client.OozieClient;
import org.junit.Test;

public class HiveAggregTests {
	Logger logger = Logger.getLogger(getClass());

	Map<String, String> getColumns() {
		Map<String, String> ans = new HashMap<String, String>();
		ans.put(HiveInterface.key_columns, "ID STRING, VALUE INT");
		return ans;
	}

	public DataflowAction createSrc(Workflow w, HiveInterface hInt,
			String new_path1) throws RemoteException, Exception {

		String idSource = w.addElement((new Source()).getName());
		Source src = (Source) w.getElement(idSource);
		src.getDFEOutput().put(Source.out_name, new HiveType());
		Map<String, DFEOutput> outs = src.getDFEOutput();
		Iterator<String> keys = outs.keySet().iterator();
		logger.info(outs.keySet().size());
		while(keys.hasNext()){
			logger.info(outs.get(keys.next()).getTypeName());
		}
		// assertTrue("create " + new_path1,
		// hInt.create(new_path1, getColumns()) == null);
		logger.info("created source");
		src.update(src.getInteraction(Source.key_datatype));
		logger.info("got datatype interaction");
		Tree<String> dataTypeTree = src.getInteraction(Source.key_datatype)
				.getTree();
		dataTypeTree.getFirstChild("list").getFirstChild("output").add("Hive");
		logger.info("set tree datatype");

		src.update(src.getInteraction(Source.key_datasubtype));
		Tree<String> datasubtypetree = src.getInteraction(
				Source.key_datasubtype).getTree();
		datasubtypetree.getFirstChild("list").getFirstChild("output")
				.add(new HiveType().getTypeName());

		src.update(src.getInteraction(Source.key_dataset));
		logger.info("update dataset");
		Tree<String> dataSetTree = src.getInteraction(Source.key_dataset)
				.getTree();
		logger.info("got dataset interaction");
		dataSetTree.getFirstChild("browse").getFirstChild("output").add("path")
				.add(new_path1);
		logger.info("set path interaction");

		Tree<String> feat1 = dataSetTree.getFirstChild("browse")
				.getFirstChild("output").add("feature");
		feat1.add("name").add("A");
		feat1.add("type").add("INT");

		Tree<String> feat2 = dataSetTree.getFirstChild("browse")
				.getFirstChild("output").add("feature");
		feat2.add("name").add("B");
		feat2.add("type").add("INT");

		Tree<String> feat3 = dataSetTree.getFirstChild("browse")
				.getFirstChild("output").add("feature");
		feat3.add("name").add("WEIGHT");
		feat3.add("type").add("INT");

		logger.info("updating out");
		String error = src.updateOut();
		logger.info("updated out "
				+ src.getInteraction(Source.key_dataset).getTree()
						.getFirstChild("browse").getFirstChild("output")
						.getFirstChild("path").getFirstChild().getHead());
		assertTrue("source update: " + error, error == null);

		return src;
	}

	public DataflowAction createHiveWithSrc(Workflow w, DataflowAction src,
			HiveInterface hInt) throws RemoteException, Exception {
		String error = null;
		String idHS = w.addElement((new HiveSelect()).getName());
		logger.debug("Hive select: " + idHS);

		HiveAggregator hive = (HiveAggregator) w.getElement(idHS);

		logger.debug(Source.out_name + " " + src.getComponentId());
		logger.debug(HiveSelect.key_input + " " + idHS);

		w.addLink(Source.out_name, src.getComponentId(), HiveSelect.key_input,
				idHS);
		assertTrue("hive select add input: " + error, error == null);
		updateHive(w, hive, hInt);

		logger.debug("HS update out...");
		error = hive.updateOut();
		assertTrue("hive select update: " + error, error == null);
		logger.debug("Features "
				+ hive.getDFEOutput().get(HiveSelect.key_output).getFeatures());

		hive.getDFEOutput()
				.get(HiveSelect.key_output)
				.generatePath(System.getProperty("user.name"),
						hive.getComponentId(), HiveSelect.key_output);

		return hive;
	}

	public DataflowAction createHiveWithHive(Workflow w, DataflowAction src,
			HiveInterface hInt) throws RemoteException, Exception {
		String error = null;
		String idHS = w.addElement((new HiveSelect()).getName());
		logger.debug("Hive select: " + idHS);

		HiveSelect hive = (HiveSelect) w.getElement(idHS);

		w.addLink(HiveSelect.key_output, src.getComponentId(),
				HiveSelect.key_input, idHS);
		assertTrue("hive select add input: " + error, error == null);

		updateHiveGb(w, hive, hInt);

		return hive;
	}

	public void updateHive(Workflow w, HiveAggregator hive, HiveInterface hInt)
			throws RemoteException, Exception {

		logger.debug("update hive...");

		// hive.update(hive.getPartInt());
		hive.update(hive.getGroupingInt());
		ConditionInteraction ci = hive.getCondInt();
		hive.update(ci);

		// Tree<String> cond = ci.getTree().getFirstChild("editor");
		// cond.add("output").add("VALUE < 10");
		TableSelectInteraction tsi = hive.gettSelInt();
		hive.update(tsi);
		{
			Tree<String> out = tsi.getTree().getFirstChild("table");
			Tree<String> rowId = out.add("row");
			rowId.add(TableSelectInteraction.table_feat_title).add("A");
			rowId.add(TableSelectInteraction.table_op_title).add("A");
			rowId.add(TableSelectInteraction.table_type_title).add("INT");
			rowId = out.add("row");
			rowId.add(TableSelectInteraction.table_feat_title).add("B");
			rowId.add(TableSelectInteraction.table_op_title).add("B");
			rowId.add(TableSelectInteraction.table_type_title).add("INT");
			rowId = out.add("row");
			rowId.add(TableSelectInteraction.table_feat_title).add("WEIGHT");
			rowId.add(TableSelectInteraction.table_op_title).add("WEIGHT");
			rowId.add(TableSelectInteraction.table_type_title).add("INT");
		}

		logger.debug("HS update out...");
		String error = hive.updateOut();
		assertTrue("hive select update: " + error, error == null);
	}

	public void updateHiveGb(Workflow w, HiveSelect hive, HiveInterface hInt)
			throws RemoteException, Exception {

		logger.debug("update hive...");

		hive.update(hive.getPartInt());
		hive.update(hive.getGroupingInt());

		Tree<String> gb = hive.getGroupingInt().getTree()
				.getFirstChild("applist").getFirstChild("output");
		gb.add("value").add("ID");

		ConditionInteraction ci = hive.getCondInt();
		hive.update(ci);

		TableSelectInteraction tsi = hive.gettSelInt();
		hive.update(tsi);
		{
			Tree<String> out = tsi.getTree().getFirstChild("table");
			Tree<String> rowId = out.add("row");
			rowId.add(TableSelectInteraction.table_feat_title).add("ID");
			rowId.add(TableSelectInteraction.table_op_title).add("ID");
			rowId.add(TableSelectInteraction.table_type_title).add("STRING");
			rowId = out.add("row");
			rowId.add(TableSelectInteraction.table_feat_title).add("SUM_VALUE");
			rowId.add(TableSelectInteraction.table_op_title).add("SUM(VALUE)");
			rowId.add(TableSelectInteraction.table_type_title).add("DOUBLE");
		}

		logger.debug("HS update out...");
		String error = hive.updateOut();
		assertTrue("hive select update: " + error, error == null);
	}

	// @Test
	public void hIntTest() throws RemoteException {
		HiveInterface hInt = new HiveInterface();
		Map<String, Map<String, String>> map = hInt.getChildrenProperties();
		Iterator<String> keys = map.keySet().iterator();
		logger.info(keys.hasNext());
		while (keys.hasNext()) {
			Map<String, String> vals = map.get(keys.next());
			Iterator<String> values = vals.values().iterator();
			Iterator<String> keyvals = vals.keySet().iterator();
			while (values.hasNext() && keyvals.hasNext()) {
				logger.info("pair : " + keyvals.next() + " " + values.next());
			}
		}
		hInt.open();
	}

	@Test
	public void basic() {

		TestUtils.logTestTitle(getClass().getName() + "#basic");
		String error = null;
		try {
			Workflow w = new Workflow("workflow1_" + getClass().getName());
			HiveInterfaceTester hInt = null;// = new HiveInterfaceTester();
			// String new_path1 = "/" + TestUtils.getTableName(1);
			String new_path1 = "/keith_test2";
			String new_path2 = "/" + TestUtils.getTableName(2);

			// hInt.delete(new_path1);
			// hInt.delete(new_path2);
			logger.info("creating source");
			DataflowAction src = createSrc(w, hInt, new_path1);
			logger.info("creating hive");
			DataflowAction hive = createHiveWithSrc(w, src, hInt);
			logger.info("setting dfe params");

			hive.getDFEOutput().get(HiveSelect.key_output)
					.setSavingState(SavingState.RECORDED);
			hive.getDFEOutput().get(HiveSelect.key_output).setPath(new_path2);
			// assertTrue("create " + new_path2,
			// hInt.create(new_path2, getColumns()) == null);
			logger.info("run...");
			String jobId = w.run();
			OozieClient wc = OozieManager.getInstance().getOc();

			// wait until the workflow job finishes printing the status every 10
			// seconds
			while (wc.getJobInfo(jobId).getStatus() == org.apache.oozie.client.WorkflowJob.Status.RUNNING) {
				System.out.println("Workflow job running ...");
				Thread.sleep(10 * 1000);
			}
			logger.info("Workflow job completed ...");
			logger.info(wc.getJobInfo(jobId));
			error = wc.getJobInfo(jobId).toString();
			assertTrue(error, error.contains("SUCCEEDED"));
		} catch (Exception e) {
			logger.error(e.getMessage());
			assertTrue(e.getMessage(), false);
		}
	}

	// @Test
	// public void oneBridge() {
	// TestUtils.logTestTitle(getClass().getName() + "#oneBridge");
	//
	// try {
	// Workflow w = new Workflow("test_one_bridge");
	// String error = null;
	//
	// HiveInterface hInt = new HiveInterface();
	// logger.info(hInt.open());
	// String new_path1 = TestUtils.getTablePath(1);
	// String new_path2 = TestUtils.getTablePath(2);
	//
	// // hInt.delete(new_path1);
	// // hInt.delete(new_path2);
	//
	// DataflowAction src = createSrc(w, hInt, new_path1);
	// DataflowAction hive = createHiveWithHive(w,
	// createHiveWithSrc(w, src, hInt), hInt);
	//
	// hive.getDFEOutput().get(HiveSelect.key_output)
	// .setSavingState(SavingState.RECORDED);
	// hive.getDFEOutput().get(HiveSelect.key_output).setPath(new_path2);
	//
	// logger.debug("run...");
	// error = w.run();
	// assertTrue("Launch join: " + error, error == null);
	// OozieClient wc = OozieManager.getInstance().getOc();
	// String jobId = w.getOozieJobId();
	//
	// // wait until the workflow job finishes printing the status every 10
	// // seconds
	// while (wc.getJobInfo(jobId).getStatus() ==
	// org.apache.oozie.client.WorkflowJob.Status.RUNNING) {
	// System.out.println("Workflow job running ...");
	// Thread.sleep(10 * 1000);
	// }
	// logger.info("Workflow job completed ...");
	// error = wc.getJobInfo(jobId).toString();
	// logger.debug(error);
	// assertTrue(error, error.contains("SUCCEEDED"));
	// } catch (Exception e) {
	// logger.error("Unexpected exception: " + e.getMessage());
	// assertTrue(e.getMessage(), false);
	// }
	// }

	// @Test
	public void HiveSelectinteractionstest() throws RemoteException {
		HiveSelect select = new HiveSelect();
		TableSelectInteraction tsel = select.gettSelInt();
	}
}