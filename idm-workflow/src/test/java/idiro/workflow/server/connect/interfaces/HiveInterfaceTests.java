package idiro.workflow.server.connect.interfaces;

import static org.junit.Assert.assertTrue;
import idiro.workflow.server.WorkflowPrefManager;
import idiro.workflow.server.connect.HiveInterface;
import idiro.workflow.test.TestUtils;

import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.junit.Test;

public class HiveInterfaceTests {

	static Logger logger = Logger.getLogger(HiveInterfaceTests.class);
	public static int resultExists = 0;
	public static int resultExecute = 0;
	public static int resultExecuteQuery = 0;

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

	Map<String, String> getPartition() {
		Map<String, String> ans = new HashMap<String, String>();
		ans.put(HiveInterface.key_columns, "ID STRING, VALUE INT");
		ans.put(HiveInterface.key_partitions, "DT STRING");
		return ans;
	}

	// @Test
	public void basic() {
		TestUtils.logTestTitle("HiveInterfaceTests#basic");
		try {

			HiveInterface hInt = new HiveInterface();
			Map<String, String> columns = getColumns();

			String new_path1 = TestUtils.getTablePath(1);
			hInt.delete(new_path1);

			assertTrue("create " + new_path1,
					hInt.create(new_path1, columns) == null);

			String new_path2 = TestUtils.getTablePath(2);
			hInt.delete(new_path2);
			assertTrue("copy to " + new_path2,
					hInt.copy(new_path1, new_path2) == null);

			assertTrue("copy to " + new_path2,
					hInt.copy(new_path1, new_path2) != null);

			String new_path3 = TestUtils.getTablePath(3);
			hInt.delete(new_path3);
			assertTrue("move to " + new_path3,
					hInt.move(new_path1, new_path3) == null);

			hInt.goTo(new_path2);
			assertTrue("getPath", hInt.getPath().equals(new_path2));

			hInt.goTo(new_path3);
			assertTrue("getPath", hInt.getPath().equals(new_path3));

			hInt.goPrevious();
			assertTrue("getPath", hInt.getPath().equals(new_path2));

			assertTrue("delete " + new_path2, hInt.delete(new_path2) == null);
			assertTrue("delete " + new_path3, hInt.delete(new_path3) == null);
		} catch (Exception e) {
			logger.error(e.getMessage());
			assertTrue(e.getMessage(), false);
		}
	}

	// @Test
	public void partitionMgmt() {
		TestUtils.logTestTitle("HiveInterfaceTests#partitionMgmt");
		try {
			String error = "";
			HiveInterface hInt = new HiveInterface();
			Map<String, String> partition = getPartition();
			Map<String, String> partitions = getPartitions();

			String new_path1 = TestUtils.getTablePath(1);
			hInt.delete(new_path1);

			error = hInt.create(new_path1, partition);
			assertTrue("create " + new_path1 + " , " + error, error == null);
			logger.debug("create 1 : " + new_path1);

			String new_partition = new_path1 + "/DT='20120102'";
			error = hInt.create(new_partition, new HashMap<String, String>());
			assertTrue("create " + new_partition + " , " + error, error == null);
			logger.debug("create 2 : " + new_partition);

			error = hInt.create(new_partition, new HashMap<String, String>());
			assertTrue("create " + new_partition + " , " + error, error != null);
			logger.debug("create 3 : " + new_partition);

			String new_path2 = TestUtils.getTablePath(2);
			hInt.delete(new_path2);

			String new_partitions = new_path2
					+ "/COUNTRY='Ireland'/DT='20120102'";
			error = hInt.create(new_partitions, partitions);
			assertTrue("create " + new_partitions + " , " + error,
					error == null);
			logger.debug("create 4 : " + new_partitions);

			List<String> part = new ArrayList<String>();
			List<String> list = hInt.getPartitions(new_partitions, part);
			assertTrue("partitions empty : " + list.toString(), !list.isEmpty());
			assertTrue("partitions " + list.toString(), list.size() == 2);
			boolean berror = hInt.exists(new_partitions);
			assertTrue("1) exists : " + new_partitions, berror);

			String new_path3 = TestUtils.getTablePath(3);
			new_path3 += "/SIZE=9";
			partition.put(HiveInterface.key_partitions,
					hInt.getTypesPartitons(new_path3));

			error = hInt.create(new_path3, partition);
			assertTrue("create : " + new_path3 + " , " + error, error == null);
			logger.info("path : " + new_path3);
			logger.info("partitions : "
					+ hInt.getPartitions(new_path3, new ArrayList<String>()));
			boolean exists = hInt.exists(new_path3);
			assertTrue("2) exists " + new_path3, exists);

			error = hInt.delete(new_path1);
			assertTrue("delete " + new_path1 + " , " + error, error == null);
			error = hInt.delete(new_path2);
			assertTrue("delete " + new_path2 + " , " + error, error == null);
			error = hInt.delete(new_path3);
			assertTrue("delete " + new_path3 + " , " + error, error == null);

		} catch (Exception e) {
			logger.error(e.getMessage());
			assertTrue(e.getMessage(), false);
		}
	}

	// @Test
	public void getTypesFromPathTest() throws RemoteException {
		TestUtils.logTestTitle("HiveInterfaceTests#getTypesFromPathTest");
		HiveInterface hInt = new HiveInterface();
		String new_path1 = TestUtils.getTablePath(1);
		new_path1 += "/COUNTRY='Ireland'/DT='20120102'/PRICE=5.0/SIZE=7";
		// new_path1+="/DT='20120102'";
		logger.info("result : " + hInt.getTypesPartitons(new_path1));
	}

	@Test
	public void selectPartitionTest() throws SQLException {
		try {
			HiveInterface hInt = new HiveInterface();
			String path_1 = TestUtils.getTablePath(1);
			String part_path = path_1 + "/COUNTRY='Ireland'/DT='20120204'";
			logger.info("execute : " + hInt.getExecute());
			hInt.delete(path_1);
			String error = hInt.create(part_path, getPartitions());
			assertTrue("create error " + error, error == null);
			hInt.goTo(part_path);
			 List<String> result = hInt.select("\001", 5);
			 logger.info("result : "+result.toString());
			 hInt.delete(path_1);
			 WorkflowPrefManager.resetSys();
			 WorkflowPrefManager.resetUser();

		} catch (RemoteException e) {
			e.printStackTrace();
			logger.info("error in this test " + e.getMessage());
		}
	}

	// @Test
	public void interfaceConcurrency() throws RemoteException {
		TestUtils.logTestTitle("interfaceConcurrency");

		HiveInterface hInt = new HiveInterface();
		String path1 = TestUtils.getTablePath(25);

		int size = 15;
		Thread[] exists = new Thread[size];
		Thread[] execute = new Thread[size];
		Thread[] executeQuery = new Thread[size];

		// logger.info("Init count down latch..");
		logger.info("Created Latch no creating arrays");
		for (int i = 0; i < size; ++i) {

			exists[i] = new Thread(new HiveThreadexist(path1));
			execute[i] = new Thread(new HiveThreadExecute("SHOW TABLES"));
			executeQuery[i] = new Thread(new HiveThreadExecuteQuery(
					"SHOW TABLES"));
			exists[i].start();
			execute[i].start();
			executeQuery[i].start();

		}

		logger.info("Latch countdown");

		boolean end = false;
		int count = 0;
		int countMax = 1000;
		while (!end && count < countMax) {
			if (count % 100 == 0) {
				logger.info("await thread : " + count);
			}
			end = true;
			for (int i = 0; i < size; ++i) {
				end &= !(exists[i].isAlive() || executeQuery[i].isAlive() || execute[i]
						.isAlive());
			}
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			++count;
		}
		if (!end) {
			logger.info("wait too long");
			for (int i = 0; i < size; ++i) {
				exists[i].destroy();
				execute[i].destroy();
				executeQuery[i].destroy();
			}
			assertTrue("thread destroyed", false);
		}

		boolean ok = false;
		int executeVal = HiveInterface.getExecute();
		int doARefreshcount = HiveInterface.getDoARefreshcount();
		if (executeVal == 0 && doARefreshcount == 0) {
			ok = true;
		}
		assertTrue("result was not equal to size for exist " + resultExists
				+ ", " + size, resultExists == size);
		assertTrue("result was not equal to size for execute " + resultExecute
				+ ", " + size, resultExecute == size);
		assertTrue("result was not equal to size for executeQuery "
				+ resultExecuteQuery + ", " + size, resultExecuteQuery == size);
		assertTrue("HiveInterface Execute and doARefresh not empty : "
				+ executeVal + " , " + doARefreshcount, ok);
	}

	public class HiveThreadexist implements Runnable {
		String path;
		HiveInterface hInt;

		public HiveThreadexist(String p) {
			path = p;
		}

		@Override
		public void run() {
			try {
				hInt = new HiveInterface();
				if (!hInt.exists(path)) {
					++resultExists;
				}
			} catch (RemoteException e) {
				logger.info("exception");
				e.printStackTrace();
			}
		}

	}

	public class HiveThreadExecute implements Runnable {
		String query;

		public HiveThreadExecute(String p) {
			query = p;
		}

		@Override
		public void run() {
			try {
				if (HiveInterface.execute(query)) {
					++resultExecute;
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}

		}
	}

	public class HiveThreadExecuteQuery implements Runnable {
		String query;

		public HiveThreadExecuteQuery(String p) {
			query = p;
		}

		@Override
		public void run() {
			try {
				if (HiveInterface.executeQuery(query) != null) {
					++resultExecuteQuery;
				}

			} catch (SQLException e) {
				e.printStackTrace();
			}

		}
	}
}
