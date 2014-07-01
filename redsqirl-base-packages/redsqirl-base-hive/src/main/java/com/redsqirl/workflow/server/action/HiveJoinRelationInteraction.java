package com.redsqirl.workflow.server.action;


import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.redsqirl.utils.FeatureList;
import com.redsqirl.utils.Tree;
import com.redsqirl.utils.TreeNonUnique;
import com.redsqirl.workflow.server.TableInteraction;
import com.redsqirl.workflow.server.action.utils.HiveDictionary;
import com.redsqirl.workflow.server.connect.HiveInterface;
import com.redsqirl.workflow.server.interfaces.DFEOutput;
import com.redsqirl.workflow.utils.HiveLanguageManager;

/**
 * Specify the relationship between joined tables. The order is important as it
 * will be the same in the SQL query.
 * 
 * @author etienne
 * 
 */
public class HiveJoinRelationInteraction extends TableInteraction {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7384667815452362352L;
	/**
	 * Join action that the interaction belongs to
	 */
	private HiveJoin hj;
	/** Table title */
	public static final String table_table_title = HiveLanguageManager
			.getTextWithoutSpace("hive.join_relationship_interaction.relation_column"),
			/** Feature title */
			table_feat_title = HiveLanguageManager
					.getTextWithoutSpace("hive.join_relationship_interaction.op_column");

	/**
	 * Constructor
	 * 
	 * @param id
	 * @param name
	 * @param legend
	 * @param column
	 * @param placeInColumn
	 * @param hj
	 * @throws RemoteException
	 */
	public HiveJoinRelationInteraction(String id, String name, String legend,
			int column, int placeInColumn, HiveJoin hj) throws RemoteException {
		super(id, name, legend, column, placeInColumn);
		this.hj = hj;
		tree.removeAllChildren();
		tree.add(getRootTable());
	}

	/**
	 * Check the interaction for errors
	 * 
	 * @return Error Message
	 * @throws RemoteException
	 */
	@Override
	public String check() throws RemoteException {
		String msg = super.check();
		if (msg != null) {
			return msg;
		}

		List<Map<String, String>> lRow = getValues();
		Set<String> relations = hj.getAliases().keySet();
		if (relations.size() != lRow.size()) {
			msg = HiveLanguageManager
					.getText("hive.join_relationship_interaction.checkrownb");
		} else {
			Set<String> featType = new LinkedHashSet<String>();
			FeatureList inFeats = hj.getInFeatures();
			logger.debug(inFeats.getFeaturesNames());
			Iterator<Map<String, String>> rows = lRow.iterator();
			int rowNb = 0;
			while (rows.hasNext() && msg == null) {
				++rowNb;
				Map<String, String> row = rows.next();
				try {
					String relation = row.get(table_table_title);
					String rel = row.get(table_feat_title);
					String type = HiveDictionary.getInstance().getReturnType(
							rel, inFeats);

					if (type == null) {
						msg = HiveLanguageManager
								.getText(
										"hive.join_relationship_interaction.checkexpressionnull",
										new Object[] { rowNb });
					} else {
						featType.add(type);
					}

					Iterator<String> itRelation = relations.iterator();
					while (itRelation.hasNext() && msg == null) {
						String curTab = itRelation.next();
						if (rel.contains(curTab + ".")
								&& !curTab.equalsIgnoreCase(relation)) {
							msg = HiveLanguageManager
									.getText(
											"hive.join_relationship_interaction.checktable2times",
											new Object[] { rowNb, curTab,
													relation });
						}
					}

				} catch (Exception e) {
					msg = e.getMessage();
				}
			}

			if (msg == null && featType.size() != 1) {
				msg = HiveLanguageManager
						.getText("hive.join_relationship_interaction.checksametype");
			}
		}

		return msg;
	}

	/**
	 * Update the interaction
	 * 
	 * @throws RemoteException
	 */
	public void update() throws RemoteException {

		Set<String> tablesIn = hj.getAliases().keySet();

		// Remove constraint on first column
		updateColumnConstraint(table_table_title, null, 1, tablesIn);

		updateColumnConstraint(table_feat_title, null, null, null);
		updateEditor(table_feat_title, HiveDictionary.generateEditor(
				HiveDictionary.getInstance().createDefaultSelectHelpMenu(),
				hj.getInFeatures()));

		if (getValues().isEmpty()) {
			List<Map<String, String>> lrows = new LinkedList<Map<String, String>>();
			Iterator<String> tableIn = tablesIn.iterator();
			while (tableIn.hasNext()) {
				Map<String, String> curMap = new LinkedHashMap<String, String>();
				curMap.put(table_table_title, tableIn.next());
				curMap.put(table_feat_title, "");
				logger.info("row : " + curMap);
				lrows.add(curMap);
			}
			setValues(lrows);
		}
	}

	/**
	 * Get the root table of the interaction
	 * 
	 * @return Tree of root table
	 * @throws RemoteException
	 */
	protected Tree<String> getRootTable() throws RemoteException {
		// Table
		Tree<String> input = new TreeNonUnique<String>("table");
		Tree<String> columns = new TreeNonUnique<String>("columns");
		input.add(columns);

		// Feature name
		Tree<String> table = new TreeNonUnique<String>("column");
		columns.add(table);
		table.add("title").add(table_table_title);

		columns.add("column").add("title").add(table_feat_title);
		// logger.info("input : "+input.toString());
		return input;
	}
	/**
	 * Get the Query Piece for the join relationship
	 * @return query piece
	 * @throws RemoteException
	 */
	public String getQueryPiece() throws RemoteException {
		logger.debug("join...");

		String joinType = hj.getJoinTypeInt().getTree().getFirstChild("list")
				.getFirstChild("output").getFirstChild().getHead();

		String join = "";
		String prev = "";
		Map<String, DFEOutput> aliases = hj.getAliases();
		HiveInterface hi = new HiveInterface();
		Iterator<Tree<String>> it = getTree().getFirstChild("table")
				.getChildren("row").iterator();
		if (it.hasNext()) {
			Tree<String> cur = it.next();
			String curAlias = cur.getFirstChild(table_table_title)
					.getFirstChild().getHead();
			prev = cur.getFirstChild(table_feat_title).getFirstChild()
					.getHead();
			join = hi.getTableAndPartitions(aliases.get(curAlias).getPath())[0]
					+ " " + curAlias;
		}
		while (it.hasNext()) {
			Tree<String> cur = it.next();
			String curFeat = cur.getFirstChild(table_feat_title)
					.getFirstChild().getHead();
			String curAlias = cur.getFirstChild(table_table_title)
					.getFirstChild().getHead();
			join += " "
					+ joinType
					+ " "
					+ hi.getTableAndPartitions(aliases.get(curAlias).getPath())[0]
					+ " " + curAlias + " ON (" + prev + " = " + curFeat + ")";
			prev = curFeat;
		}

		return join;
	}

	/**
	 * Check an expression for errors using
	 * {@link com.redsqirl.workflow.server.action.utils.HiveDictionary}
	 * @return Error Message
	 * @throws RemoteException
	 */
	public String checkExpression(String expression, String modifier)
			throws RemoteException {
		String error = null;
		try {
			if (HiveDictionary.getInstance().getReturnType(expression,
					hj.getInFeatures()) == null) {
				error = HiveLanguageManager.getText("hive.expressionnull");
			}
		} catch (Exception e) {
			error = HiveLanguageManager.getText("hive.expressionexception");
			logger.error(error, e);
		}
		return error;
	}

}