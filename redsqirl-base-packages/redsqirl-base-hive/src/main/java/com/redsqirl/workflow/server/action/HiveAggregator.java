package com.redsqirl.workflow.server.action;


import java.rmi.RemoteException;
import java.util.Iterator;

import org.apache.log4j.Logger;

import com.redsqirl.utils.FieldList;
import com.redsqirl.workflow.server.Page;
import com.redsqirl.workflow.server.connect.HiveInterface;
import com.redsqirl.workflow.server.interfaces.DFEInteraction;
import com.redsqirl.workflow.server.interfaces.DFEOutput;
import com.redsqirl.workflow.utils.HiveLanguageManager;
/**
 * Action to allow for aggregation methods/operations on data in hive
 * @author keith
 *
 */
public class HiveAggregator extends HiveElement {
	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 5757307122737374106L;
	
	private static Logger logger = Logger.getLogger(HiveAggregator.class);
	
	/**
	 * Pages
	 */
	private Page page1, page2, page3, page4;
	/**
	 * Group by Key
	 */
	private static final String key_group = "Group By";
	/**
	 * fields key
	 */
	private static final String key_fields = "Fields";
	// private static final String key_group ="";
	/**
	 * Table Select interaction
	 */
	private HiveTableSelectInteraction tSelInt;
	/**
	 * Constructor
	 * @throws RemoteException
	 */
	public HiveAggregator() throws RemoteException {
		super(3, 1, 1);

		page1 = addPage(
				HiveLanguageManager.getText("hive.aggregator_page1.title"),
				HiveLanguageManager.getText("hive.aggregator_page1.legend"), 1);

		groupingInt = new HiveGroupByInteraction(key_group,
				HiveLanguageManager
						.getText("hive.aggregator_group_interaction.title"),
				HiveLanguageManager
						.getText("hive.aggregator_group_interaction.legend"),
				0, 0);

		page1.addInteraction(groupingInt);

		page2 = addPage(
				HiveLanguageManager.getText("hive.aggregator_page2.title"),
				HiveLanguageManager.getText("hive.aggregator_page2.legend"), 1);

		tSelInt = new HiveTableSelectInteraction(
				key_fields,
				HiveLanguageManager
						.getText("hive.aggregator_fields_interaction.title"),
				HiveLanguageManager
						.getText("hive.aggregator_fields_interaction.legend"),
				0, 0, this);

		page2.addInteraction(tSelInt);
		
		page3 = addPage(HiveLanguageManager.getText("hive.aggregator_page3.title"),
				HiveLanguageManager.getText("hive.aggregator_page3.title"), 1);
		page3.addInteraction(orderInt);

		page4 = addPage(key_condition, "Create a condition for the attributes",
				1);
		condInt = new HiveFilterInteraction( 0, 0, this);
		page4.addInteraction(condInt);
	}
	/**
	 * Get the name of the action 
	 * @return name
	 * @throws RemoteException
	 */
	@Override
	public String getName() throws RemoteException {

		return "hive_aggregator";
	}
	/**
	 * Get the query for the action
	 * @return query
	 * @throws RemoteException
	 */
	@Override
	public String getQuery() throws RemoteException {
		HiveInterface hInt = new HiveInterface();
		String query = null;
		if (getDFEInput() != null) {
			DFEOutput in = getDFEInput().get(key_input).get(0);
			logger.debug("In and out...");
			// Input
			String[] tableAndPartsIn = hInt.getTableAndPartitions(in.getPath());
			String tableIn = tableAndPartsIn[0];
			// Output
			DFEOutput out = output.values().iterator().next();
			String tableOut = hInt.getTableAndPartitions(out.getPath())[0];

			String insert = "INSERT OVERWRITE TABLE " + tableOut;
			String from = " FROM " + tableIn + " ";
			String create = "CREATE TABLE IF NOT EXISTS " + tableOut;
			String where = condInt.getQueryPiece();
			
			String order = orderInt.getQueryPiece();

			logger.debug("group by...");
			String groupby = "";
			Iterator<String> gIt = getGroupByFields().iterator();
			if (gIt.hasNext()) {
				groupby = gIt.next();
			}
			while (gIt.hasNext()) {
				groupby += "," + gIt.next();
			}
			if (!groupby.isEmpty()) {
				groupby = " GROUP BY " + groupby;
			}
			String select = tSelInt.getQueryPiece(out);
			String createSelect = tSelInt.getCreateQueryPiece(out);

			if (select.isEmpty()) {
				logger.debug("Nothing to select");
			} else {
				query = create + "\n" + createSelect + ";\n\n";

				query += insert + "\n" + select + "\n" + from + "\n" + where
						+ groupby + "\n" + order + ";";
			}
		}

		return query;
	}
	/**
	 * Get the input FieldList
	 * @return input FieldList
	 * @throws RemoteException
	 */
	@Override
	public FieldList getInFields() throws RemoteException {
		return getDFEInput().get(key_input).get(0).getFields();
	}
	/**
	 * Get the new fields from the action
	 * @return new FieldList
	 * @throws RemoteException
	 */
	@Override
	public FieldList getNewFields() throws RemoteException {
		return tSelInt.getNewFields();
	}
	
	/**
	 * Update the interaction in the action
	 * @param interaction
	 * @throws RemoteException
	 */
	@Override
	public void update(DFEInteraction interaction) throws RemoteException {
		DFEOutput in = getDFEInput().get(key_input).get(0);
		if (in != null) {
			if (interaction.getName().equals(condInt.getName())) {
				logger.info("Hive condition interaction updating");
				condInt.update();
			} else if (interaction.getName().equals(groupingInt.getName())) {

				UpdateGroupInt(groupingInt, in);
			}
			else if (interaction.getName().equals(tSelInt.getName())) {
				logger.info("Hive tableSelect interaction updating");
				tSelInt.update(in);
			} 
			else if (interaction.getName().equals(orderInt.getName())) {
				orderInt.update();
			}
		}
	}
	/**
	 * Get the table select interaction
	 * @return tSelInt
	 */
	public HiveTableSelectInteraction gettSelInt() {
		return tSelInt;
	}

	/*public void updateGrouping(Tree<String> treeGrouping, DFEOutput in)
			throws RemoteException {

		Tree<String> list = null;
		if (treeGrouping.getSubTreeList().isEmpty()) {
			list = treeGrouping.add("applist");
			list.add("output");
		} else {
			list = treeGrouping.getFirstChild("applist");
			list.remove("values");
		}
		Tree<String> values = list.add("values");
		Iterator<String> it = in.getFields().getFieldNames().iterator();
		while (it.hasNext()) {
			values.add("value").add(it.next());
		}
	}*/

}
