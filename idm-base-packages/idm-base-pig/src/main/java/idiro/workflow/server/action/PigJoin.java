package idiro.workflow.server.action;

import idiro.utils.FeatureList;
import idiro.utils.OrderedFeatureList;
import idiro.workflow.server.ListInteraction;
import idiro.workflow.server.Page;
import idiro.workflow.server.connect.HDFSInterface;
import idiro.workflow.server.interfaces.DFEInteraction;
import idiro.workflow.server.interfaces.DFEOutput;

import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Action to join several relations.
 * 
 * @author marcos
 * 
 */
public class PigJoin extends PigElement {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3035179016090477413L;

	public static final String key_featureTable = "Features",
			key_joinType = "Join_Type", key_joinRelation = "Join_Relationship";

	private Page page1, page2, page3, page4;

	private PigTableJoinInteraction tJoinInt;
	private PigJoinRelationInteraction jrInt;
	private PigFilterInteraction filterInt;
	private ListInteraction joinTypeInt;

	public PigJoin() throws RemoteException {
		super(2, Integer.MAX_VALUE,0);

		page1 = addPage("Operations", "Join operations", 1);

		tJoinInt = new PigTableJoinInteraction(
				key_featureTable,
				"Please specify the operations to be executed for each feature",
				0, 0, this);

		page1.addInteraction(tJoinInt);

		page2 = addPage("Relationship", "Join Relationship", 1);

		joinTypeInt = new ListInteraction(key_joinType,
				"Please specify a join type", 0, 0);
		List<String> valueJoinTypeInt = new LinkedList<String>();
		valueJoinTypeInt.add("JOIN");
		valueJoinTypeInt.add("LEFT OUTER JOIN");
		valueJoinTypeInt.add("RIGHT OUTER JOIN");
		valueJoinTypeInt.add("FULL OUTER JOIN");
		joinTypeInt.setPossibleValues(valueJoinTypeInt);
		joinTypeInt.setValue("JOIN");

		jrInt = new PigJoinRelationInteraction(
				key_joinRelation,
				"Please specify the relationship, top to bottom is like left to right",
				0, 0, this);
		
		page2.addInteraction(joinTypeInt);
		page2.addInteraction(jrInt);

		page3 = addPage("Select", "Select Conditions", 1);

		filterInt = new PigFilterInteraction(0, 1, this);

		page3.addInteraction(filterInt);

		page4 = addPage("Output", "Output configurations", 1);

		page4.addInteraction(delimiterOutputInt);
		page4.addInteraction(savetypeOutputInt);

	}

	// @Override
	public String getName() throws RemoteException {
		return "pig_join";
	}

	// @Override
	public void update(DFEInteraction interaction) throws RemoteException {
		if (interaction.getName().equals(filterInt.getName())) {
			filterInt.update();
		} else if (interaction.getName().equals(jrInt.getName())) {
			jrInt.update();
		} else if (interaction.getName().equals(tJoinInt.getName())) {
			tJoinInt.update();
		}
	}

	@Override
	public String getQuery() throws RemoteException {

		HDFSInterface hInt = new HDFSInterface();
		String query = null;
		if (getDFEInput() != null) {
			// Output
			DFEOutput out = output.values().iterator().next();
			Map<String, DFEOutput> x = getAliases();
			Set<Entry<String, DFEOutput>> p = x.entrySet();
			Iterator<Entry<String, DFEOutput>> it = p.iterator();
			String load = "";
			for (DFEOutput in : getDFEInput().get(key_input)) {
				while (it.hasNext()) {
					Entry<String, DFEOutput> next = it.next();
					if (next.getValue().getPath()
							.equalsIgnoreCase(in.getPath())) {
						load += next.getKey() + " = " + getLoadQueryPiece(in)
								+ ";\n";
					}
				}
				it = p.iterator();
			}
			load += "\n";

			String remove = getRemoveQueryPiece(out.getPath()) + "\n\n";

			String from = getCurrentName() + " = " + jrInt.getQueryPiece()
					+ ";\n\n";

			String select = tJoinInt.getQueryPiece(getCurrentName());
			if (!select.isEmpty()) {
				select = getNextName() + " = " + select + ";\n\n";
			}

			String filter = filterInt.getQueryPiece(getCurrentName());
			if (!filter.isEmpty()) {
				filter = getNextName() + " = " + filter + ";\n\n";
			}

			String store = getStoreQueryPiece(out, getCurrentName());

			if (select.isEmpty()) {
				logger.debug("Nothing to select");
			} else {
				query = remove;

				query += load;

				query += from + select;

				query += filter;

				query += store;
			}
		}
		logger.info(query);
		return query;
	}

	public FeatureList getInFeatures() throws RemoteException {
		// FeatureList ans =
		// new OrderedFeatureList();
		// HDFSInterface hInt = new HDFSInterface();
		// List<DFEOutput> lOut = getDFEInput().get(PigJoin.key_input);
		// Iterator<DFEOutput> it = lOut.iterator();
		// while(it.hasNext()){
		// DFEOutput out = it.next();
		// String relationName = hInt.getRelation(out.getPath());
		// FeatureList mapTable = out.getFeatures();
		// Iterator<String> itFeat = mapTable.getFeaturesNames().iterator();
		// while(itFeat.hasNext()){
		// String cur = itFeat.next();
		// ans.addFeature(relationName+"."+cur, mapTable.getFeatureType(cur));
		// }
		// }
		// return ans;

		FeatureList ans = new OrderedFeatureList();
		Map<String, DFEOutput> aliases = getAliases();

		Iterator<String> it = aliases.keySet().iterator();
		while (it.hasNext()) {
			String alias = it.next();
			FeatureList mapTable = aliases.get(alias).getFeatures();
			Iterator<String> itFeat = mapTable.getFeaturesNames().iterator();
			while (itFeat.hasNext()) {
				String cur = itFeat.next();
				ans.addFeature(alias + "." + cur, mapTable.getFeatureType(cur));
			}
		}
		return ans;
	}

	/**
	 * @return the tJoinInt
	 */
	public final PigTableJoinInteraction gettJoinInt() {
		return tJoinInt;
	}

	/**
	 * @return the jrInt
	 */
	public final PigJoinRelationInteraction getJrInt() {
		return jrInt;
	}

	/**
	 * @return the condInt
	 */
	public final PigFilterInteraction getCondInt() {
		return filterInt;
	}

	/**
	 * @return the joinTypeInt
	 */
	public final DFEInteraction getJoinTypeInt() {
		return joinTypeInt;
	}

	@Override
	public FeatureList getNewFeatures() throws RemoteException {
		return tJoinInt.getNewFeatures();
	}
}
