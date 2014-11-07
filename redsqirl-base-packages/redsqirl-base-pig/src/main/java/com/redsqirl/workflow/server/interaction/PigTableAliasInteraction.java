package com.redsqirl.workflow.server.interaction;


import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.redsqirl.workflow.server.TableInteraction;
import com.redsqirl.workflow.server.action.PigElement;
import com.redsqirl.workflow.server.interfaces.DFEOutput;
import com.redsqirl.workflow.utils.PigLanguageManager;

/**
 * Interaction for selecting inputs and aliases for an interaction
 * 
 * @author marcos
 * 
 */
public class PigTableAliasInteraction extends TableInteraction {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4973968329944889374L;
	/** Action that the interaction is contained in */
	private PigElement hu;
	/** Relation Column Title */
	public static final String table_input_title = PigLanguageManager
			.getTextWithoutSpace("pig.table_alias_interaction.input_column"),
	/** Operation Column title */
	table_alias_title = PigLanguageManager
			.getTextWithoutSpace("pig.table_alias_interaction.alias_column");
	/** Field Column title */

	/* Minimum number of inputs */
	private int minNumInputs;

	/**
	 * Constructor
	 * 
	 * @param id
	 * @param name
	 * @param legend
	 * @param column
	 * @param placeInColumn
	 * @param hu
	 * @throws RemoteException
	 */
	public PigTableAliasInteraction(String id, String name, String legend,
			int column, int placeInColumn, PigElement hu, int minNumInputs)
			throws RemoteException {
		super(id, name, legend, column, placeInColumn);
		this.hu = hu;
		this.minNumInputs = minNumInputs;
		getRootTable();
	}

	/**
	 * Update the Interaction with input values
	 * 
	 * @param in
	 * @throws RemoteException
	 */
	public void update() throws RemoteException {

		
		Map<String,DFEOutput> aliases = getAliases();

		boolean rowsEmpty = getValues().isEmpty();

		Iterator<String> it = aliases.keySet().iterator();
		while (it.hasNext()) {
			String alias = it.next();
			if (rowsEmpty) {
				Map<String, String> row = new HashMap<String, String>();
				row.put(table_alias_title, alias);
				row.put(table_input_title, alias);

				addRow(row);
			}
		}

		updateColumnConstraint(table_input_title, null, null,
				aliases.keySet());

	}

	/**
	 * Generate the root table of interaction
	 * 
	 * @throws RemoteException
	 */
	protected void getRootTable() throws RemoteException {

		addColumn(table_input_title, null, null, null);

		addColumn(table_alias_title, 1, "[a-zA-Z]([A-Za-z0-9_]{0,29})", null,
				null);

	}

	/**
	 * Get the new field list from the interaction
	 * 
	 * @return FieldList
	 * @throws RemoteException
	 */
	public Map<String, DFEOutput> getAliases() throws RemoteException {
		return hu.getAliases();
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

		if (msg == null) {
			List<Map<String, String>> lRow = getValues();

			if (lRow.size() < minNumInputs) {
				msg = PigLanguageManager
						.getText("pig.table_alias_interaction.checknumberinput");
			} else {
				 Map<String, DFEOutput> aliases = getAliases();
				for (Map<String, String> row : lRow) {
					if (!aliases.containsKey(row.get(table_input_title))) {
						msg = PigLanguageManager
								.getText("pig.table_alias_interaction.checkvalidinput");
					}
				}
			}
		}

		return msg;
	}

}
