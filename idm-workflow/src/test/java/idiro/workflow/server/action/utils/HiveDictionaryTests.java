package idiro.workflow.server.action.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import idiro.utils.OrderedFeatureList;
import idiro.utils.FeatureList;
import idiro.workflow.server.enumeration.FeatureType;
import idiro.workflow.test.TestUtils;

import java.rmi.RemoteException;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.junit.Test;

public class HiveDictionaryTests {

	Logger logger = Logger.getLogger(getClass());

	public FeatureList getFeatures() throws RemoteException{
		FeatureList features = new OrderedFeatureList();
		features.addFeature("col1", FeatureType.STRING);
		features.addFeature("col2", FeatureType.DOUBLE);
		features.addFeature("col3", FeatureType.INT);
		features.addFeature("col4", FeatureType.BOOLEAN);
		return features;
	}
	
	public Set<String> getAgg(){
		Set<String> agg = new HashSet<String>();
		agg.add("col1");
		return agg;
	}

	public void isBoolean(String expr,FeatureList features)throws Exception{
		assertTrue(expr,
				HiveDictionary.getInstance().getReturnType(expr, features).equalsIgnoreCase("boolean")
				);
	}

	public void isNotBoolean(String expr,FeatureList features)throws Exception{
		assertFalse(expr,
				HiveDictionary.getInstance().getReturnType(expr, features).equalsIgnoreCase("boolean")
				);
	}

	public void isNull(String expr,FeatureList features)throws Exception{
		try{
			assertTrue(expr,
					HiveDictionary.getInstance().getReturnType(expr, features) == null
					);
		}catch(Exception e){}
	}
	public void isNull(String expr,
			FeatureList features,
			Set<String> agg)throws Exception{
		try{
			assertTrue(expr,
					HiveDictionary.getInstance().getReturnType(expr, features,agg) == null
					);
		}catch(Exception e){}
	}

	public void isNumber(String expr,FeatureList features)throws Exception{
		assertTrue(expr,
				HiveDictionary.getInstance().getReturnType(expr, features).equalsIgnoreCase("NUMBER")
				);
	}

	public void isNotNumber(String expr,FeatureList features)throws Exception{
		assertFalse(expr,
				HiveDictionary.getInstance().getReturnType(expr, features).equalsIgnoreCase("NUMBER")
				);
	}

	public void is(String expr,
			FeatureList features,
			String type)throws Exception{
		assertTrue(expr,
				HiveDictionary.getInstance().getReturnType(expr, features).equalsIgnoreCase(type)
				);
	}
	
	public void is(String expr,
			FeatureList features,
			Set<String> agg,
			String type)throws Exception{
		assertTrue(expr,
				HiveDictionary.getInstance().getReturnType(expr, features,agg).equalsIgnoreCase(type)
				);
	}

	public void isNot(String expr,
			FeatureList features,
			String type)throws Exception{
		assertFalse(expr,
				HiveDictionary.getInstance().getReturnType(expr, features).equalsIgnoreCase(type)
				);
	}

	@Test
	public void testBooleanOperations() throws RemoteException{
		TestUtils.logTestTitle("HiveDictionaryTests#testBooleanOperations");
		FeatureList features = getFeatures();
		try{
			isBoolean("TRUE",features);
			isBoolean("col4",features);
			isNotBoolean("col1",features);
			isBoolean("col4 AND col4",features);
			isBoolean("col4",features);
			isBoolean("col4 OR col4",features);
			isBoolean("col4 OR col4 AND col4",features);
			isBoolean("(col4 OR col4 ) AND col4",features);
			isBoolean("col4 OR ( col4 AND col4 )",features);
			isBoolean("NOT col4",features);
			isNull("or col4",features);
			isNull("col4 NOT",features);
			isNull("IS NULL col4",features);
			isBoolean("col2 <= col2 ", features);
			isBoolean("col2 <= 40 ", features);
			isBoolean("col2 < 40 ", features);
			isBoolean("col2 > 40 ", features);
			isBoolean("col2 >= 40 ", features);
			isBoolean("col2 <= col3 ", features);
			isBoolean("col2 IS NULL", features);
			isBoolean("col2 IS NOT NULL", features);
			isBoolean("col2 IS NOT NULL", features);
			isBoolean("col1 LIKE 'blabla'", features);
			isBoolean("col1 <= col3 ", features);
			isBoolean(
					"col1 <= col3 AND col3 > 40 OR ( col4 and col1 LIKE 'bla')", 
					features);

		}catch(Exception e){
			logger.error("Exception when testing boolean operations: "+e.getMessage());
			assertTrue("Fail on exception",false);
		}
		TestUtils.logTestTitle("success");
	}

	@Test
	public void testArithmeticOperations() throws RemoteException{
		TestUtils.logTestTitle("HiveDictionaryTests#testArithmeticOperations");
		FeatureList features = getFeatures();
		try{
			isNumber("col2 + col3",features);
			isNumber("col2 - col3",features);
			isNumber("col2 * col3",features);
			isNumber("col2 / col3",features);
			isNumber("col2 - col3 / col2 + col3",features);
			isNumber("( col2 - col3 ) / col2 + col3",features);
			isNumber("( col2 - col3) / ( col2 + col3 )",features);
			isNumber( "(col2 - col3)/(col2+col3)",features);
		}catch(Exception e){
			logger.error("Exception when testing boolean operations: "+e.getMessage());
			assertTrue("Fail on exception",false);
		}
	}


	@Test
	public void testMethods() throws RemoteException{
		TestUtils.logTestTitle("HiveDictionaryTests#testMethods");
		FeatureList features = getFeatures();
		try{
			is("substr('bla',1)",features,"STRING");
			is( "substr('bla',1,2)",features,"STRING");
			is( "substr(substr('bla',1,2),1,2)",features,"STRING");
			isNull("substr('bla',1,2,3)",features);
			is("cast('bla' AS STRING)",features,"STRING");
			is("cast(substr('bla',1,2) AS STRING)",features,"STRING");
		}catch(Exception e){
			logger.error("Exception when testing boolean operations: "+e.getMessage());
			assertTrue("Fail on exception",false);
		}

	}
	
	@Test
	public void testAggreg() throws RemoteException{
		TestUtils.logTestTitle("HiveDictionaryTests#testAggreg");
		FeatureList features = getFeatures();
		Set<String> agg = getAgg();
		try{
			is("count(*)",features,agg,"BIGINT");
			is("sum(col2)",features,agg,"DOUBLE");
			is("avg(col2)",features,agg,"DOUBLE");
			is("max(col2)",features,agg,"DOUBLE");
			is("min(col2)",features,agg,"DOUBLE");
			is("col1",features,agg,"STRING");
			isNull("col2",features,agg);
			is("min(round(col2)+rand())",features,agg,"DOUBLE");
		}catch(Exception e){
			logger.error("Exception when testing boolean operations: "+e.getMessage());
			assertTrue("Fail on exception",false);
		}
	}
}
