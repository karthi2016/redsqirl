package idiro.workflow.server.datatype;

import idiro.hadoop.NameNodeVar;
import idiro.hadoop.checker.HdfsFileChecker;
import idiro.hadoop.pig.PigUtils;
import idiro.utils.FeatureList;
import idiro.utils.OrderedFeatureList;
import idiro.utils.RandomString;
import idiro.workflow.server.DataOutput;
import idiro.workflow.server.OozieManager;
import idiro.workflow.server.connect.HDFSInterface;
import idiro.workflow.server.enumeration.DataBrowser;
import idiro.workflow.server.enumeration.FeatureType;
import idiro.workflow.utils.LanguageManager;

import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PathFilter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Map-Reduce Text output type. Output given when an algorithm return a text
 * format map-reduce directory.
 * 
 * @author etienne
 * 
 */
public class MapRedTextType extends DataOutput {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8260229620701006942L;

	public final static String key_delimiter = "delimiter";
	public final static String key_header = "header";

	protected static HDFSInterface hdfsInt;

	public MapRedTextType() throws RemoteException {
		super();
		if (hdfsInt == null) {
			hdfsInt = new HDFSInterface();
		}
		addProperty(key_header, "");
	}

	public MapRedTextType(FeatureList features) throws RemoteException {
		super(features);
		if (hdfsInt == null) {
			hdfsInt = new HDFSInterface();
		}
		addProperty(key_header, "");
	}

	@Override
	public String getTypeName() throws RemoteException {
		return "TEXT MAP-REDUCE DIRECTORY";
	}

	@Override
	public DataBrowser getBrowser() throws RemoteException {
		return DataBrowser.HDFS;
	}

	@Override
	public void generatePath(String userName, String component,
			String outputName) throws RemoteException {
		setPath("/user/" + userName + "/tmp/idm_" + component + "_"
				+ outputName + "_" + RandomString.getRandomName(8));
	}

	@Override
	public String isPathValid() throws RemoteException {
		String error = null;
		HdfsFileChecker hCh = new HdfsFileChecker(getPath());
		if (!hCh.isInitialized() || hCh.isFile()) {
			error = LanguageManager.getText("mapredtexttype.dirisfile");
		} else {
			final FileSystem fs;
			try {
				fs = NameNodeVar.getFS();
				hCh.setPath(new Path(getPath()).getParent());
				if (!hCh.isDirectory()) {
					error = LanguageManager.getText("mapredtexttype.nodir");
				}
				FileStatus[] stat = fs.listStatus(new Path(getPath()),
						new PathFilter() {

					@Override
					public boolean accept(Path arg0) {
						return !arg0.getName().startsWith("_");
					}
				});
				for (int i = 0; i < stat.length && error == null; ++i) {
					if (stat[i].isDir()) {
						error = LanguageManager.getText(
								"mapredtexttype.notmrdir",
								new Object[] { getPath() });
					}
				}
				fs.close();
			} catch (IOException e) {

				error = LanguageManager.getText("unexpectedexception",
						new Object[] { e.getMessage() });

				logger.error(error);
			}

		}
		hCh.close();
		return error;
	}

	@Override
	public boolean isPathAutoGeneratedForUser(String userName,
			String component, String outputName) throws RemoteException {
		return getPath().startsWith(
				"/user/" + userName + "/tmp/idm_" + component + "_"
						+ outputName + "_");
	}

	@Override
	public boolean isPathExists() throws RemoteException {
		boolean ok = false;
		if (getPath() != null) {
			logger.info("checking if path exitst :" + getPath().toString());
			HdfsFileChecker hCh = new HdfsFileChecker(getPath());
			if (hCh.exists()) {
				ok = true;
			}
			hCh.close();
		}
		return ok;
	}

	@Override
	public String remove() throws RemoteException {
		return hdfsInt.delete(getPath());
	}

	@Override
	public boolean oozieRemove(Document oozieDoc, Element action,
			File localDirectory, String pathFromOozieDir,
			String fileNameWithoutExtension) throws RemoteException {
		Element fs = oozieDoc.createElement("fs");
		action.appendChild(fs);

		Element rm = oozieDoc.createElement("delete");
		rm.setAttribute("path", "${" + OozieManager.prop_namenode + "}"
				+ getPath());
		fs.appendChild(rm);

		return true;
	}

	@Override
	public List<String> select(int maxToRead) throws RemoteException {
		List<String> ans = null;

		if (isPathValid() == null && isPathExists()) {
			try {
				final FileSystem fs = NameNodeVar.getFS();
				FileStatus[] stat = fs.listStatus(new Path(getPath()),
						new PathFilter() {

					@Override
					public boolean accept(Path arg0) {
						return !arg0.getName().startsWith("_");
					}
				});
				ans = new ArrayList<String>(maxToRead);
				for (int i = 0; i < stat.length; ++i) {
					ans.addAll(hdfsInt.select(stat[i].getPath().toString(),
							getChar(getProperty(key_delimiter)),
							(maxToRead / stat.length) + 1));
				}
			} catch (IOException e) {
				String error = "Unexpected error: " + e.getMessage();
				logger.error(error);
				ans = null;
			}
		}
		return ans;
	}

	public boolean isVariableName(String name) {
		String regex = "[a-zA-Z]+[a-zA-Z0-9_]*";
		return name.matches(regex);
	}

	private String setFeaturesFromHeader() throws RemoteException {

		logger.info("setFeaturesFromHeader()");

		String header = getProperty(key_header);
		String error = null;

		if (header != null && !header.isEmpty()) {

			String newLabels[] = header.split(",");

			logger.info("setFeaturesFromHeader features "+ features);
			if (features != null && features.getSize() != newLabels.length) {
				error = LanguageManager.getText("mapredtexttype.setheaders.wronglabels");
			}

			if(header.trim().endsWith(",")){
				error = LanguageManager.getText("mapredtexttype.setheaders.wronglabels");
			}

			FeatureList newFL = new OrderedFeatureList();

			try {

				if (newLabels[0].trim().split("\\s+").length > 1) {

					logger.info("setFeaturesFromHeader if ");

					for (int j = 0; j < newLabels.length && error == null; j++) {
						String label = newLabels[j].trim();
						String[] nameType = label.split("\\s+");
						if (nameType.length != 2) {
							error = LanguageManager.getText("mapredtexttype.setheaders.wrongpair");
						} else {
							logger.info("nameType[1] " + nameType[0] + " " + nameType[1]);

							if(isVariableName(nameType[0])){
								
								try {
									newFL.addFeature(nameType[0], FeatureType.valueOf(nameType[1].toUpperCase()));
								}
								catch (Exception e) {
									logger.error(e);
									error = LanguageManager.getText("mapredtexttype.msg_error_type_new_header", new Object[] { nameType[1] });
								}

							}else{
								error = LanguageManager.getText("mapredtexttype.msg_error_name_header", new Object[] { nameType[0] });
							}

						}
					}

				} else {

					logger.info("setFeaturesFromHeader else ");

					/*if (features != null && features.getSize() != newLabels.length) {
						error = LanguageManager.getText("mapredtexttype.setheaders.wronglabels");
					}*/

					logger.info("setFeaturesFromHeader else error  "+ error);
					//logger.info("setFeaturesFromHeader else features "+ features);

					if(error == null && features != null && features.getFeaturesNames() != null){
						Iterator<String> it = features.getFeaturesNames().iterator();
						int j = 0;
						while (it.hasNext() && error == null) {
							String featName = it.next();
							logger.info("getFeatureType featName " + featName);

							if(isVariableName(newLabels[j].trim())){
								newFL.addFeature(newLabels[j].trim(),features.getFeatureType(featName));
							}else{
								error = LanguageManager.getText("mapredtexttype.msg_error_name_header", new Object[] { newLabels[j].trim() });
								break;
							}

							++j;
						}
					}

				}
			} catch (Exception e) {
				logger.error(e);
				error = LanguageManager.getText("mapredtexttype.setheaders.typeunknown");
			}

			if (error == null && !newFL.getFeaturesNames().isEmpty()) {
				error = checkFeatures(newFL);
			}

			if (error == null && !newFL.getFeaturesNames().isEmpty()) {
				setFeatures(newFL);
			} else if (error != null) {
				removeProperty(key_header);
			}

		}

		logger.info("setFeaturesFromHeader-error " + error);

		return error;
	}

	@Override
	public void setFeatures(FeatureList fl) {
		// if(getProperty(key_header) == null ||
		// getProperty(key_header).trim().isEmpty()){
		logger.info("setFeatures :");
		super.setFeatures(fl);
		// }
	}

	private void generateFeaturesMap() throws RemoteException {

		logger.info("generateFeaturesMap --");

		features = new OrderedFeatureList();
		try {
			List<String> lines = this.select(10);
			if (lines != null) {
				for (String line : lines) {
					if (!line.trim().isEmpty()) {
						int cont = 0;

						for (String s : line
								.split(Pattern
										.quote(getChar(getProperty(key_delimiter))))) {
							String nameColumn = generateColumnName(cont++);

							logger.info("line: " + line);
							logger.info("s: " + s);
							logger.info("key_delimiter: "
									+ Pattern
									.quote(getChar(getProperty(key_delimiter))));
							logger.info("new nameColumn: " + nameColumn);

							FeatureType type = getType(s.trim());
							if (features.containsFeature(nameColumn)) {
								if (!canCast(type,
										features.getFeatureType(nameColumn))) {
									features.addFeature(nameColumn, type);
								}
							} else {
								features.addFeature(nameColumn, type);
							}
						}

					}
				}
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}

	}

	private String getDefaultDelimiter(String text) {
		if (text.contains("\001")) {
			return "#1";
		} else if (text.contains("\002")) {
			return "#2";
		} else if (text.contains("|")) {
			return "#124";
		}
		return "#1";
	}

	private FeatureType getType(String expr) {

		FeatureType type = null;
		if (expr.equalsIgnoreCase("TRUE") || expr.equalsIgnoreCase("FALSE")) {
			type = FeatureType.BOOLEAN;
		} else {
			try {
				Integer.valueOf(expr);
				type = FeatureType.INT;
			} catch (Exception e) {
			}
			if (type == null) {
				try {
					Long.valueOf(expr);
					type = FeatureType.LONG;
				} catch (Exception e) {
				}
			}
			if (type == null) {
				try {
					Float.valueOf(expr);
					type = FeatureType.FLOAT;
				} catch (Exception e) {
				}
			}
			if (type == null) {
				try {
					Double.valueOf(expr);
					type = FeatureType.DOUBLE;
				} catch (Exception e) {
				}
			}
			if (type == null) {
				type = FeatureType.STRING;
			}
		}
		logger.info("getType: " + expr + " - " + type);
		return type;
	}

	private boolean canCast(FeatureType from, FeatureType to) {
		if (from.equals(to)) {
			return true;
		}

		List<FeatureType> features = new ArrayList<FeatureType>();
		features.add(FeatureType.INT);
		features.add(FeatureType.LONG);
		features.add(FeatureType.FLOAT);
		features.add(FeatureType.DOUBLE);
		features.add(FeatureType.STRING);

		if (from.equals(FeatureType.BOOLEAN)) {
			if (to.equals(FeatureType.STRING)) {
				return true;
			}
			return false;
		} else if (features.indexOf(from) <= features.indexOf(to)) {
			return true;
		}
		return false;
	}

	@Override
	public void addProperty(String key, String value) {

		if (key.equals(key_delimiter) && value.length() == 1) {
			value = "#" + String.valueOf((int) value.charAt(0));
		}
		super.addProperty(key, value);
	}

	@Override
	public void setPath(String path) throws RemoteException {
		String oldPath = getPath();

		if (path == null) {
			super.setPath(path);
			setFeatures(null);
			return;
		}

		if (!path.equalsIgnoreCase(oldPath)) {

			super.setPath(path);

			logger.info("setPath() " + path);
			if (isPathExists()) {
				List<String> list = select(1);

				if (list != null && !list.isEmpty()) {
					String text = list.get(0);
					if (getProperty(key_delimiter) == null) {
						String delimiter = getDefaultDelimiter(text);

						logger.info("delimiter -> " + delimiter);

						super.addProperty(key_delimiter, delimiter);
					} else {
						if (!text.contains(getChar(getProperty(key_delimiter)))) {
							String delimiter = getDefaultDelimiter(text);

							logger.info("delimiter -> " + delimiter);

							super.addProperty(key_delimiter, delimiter);

						}
					}
				}

				generateFeaturesMap();

				String error = null;
				String header = getProperty(key_header);
				if (header != null && !"".equalsIgnoreCase(header)) {
					logger.info("setFeaturesFromHeader --");
					error = setFeaturesFromHeader();
					if (error != null) {
						throw new RemoteException(error);
					}
				}
				/*else {
					generateFeaturesMap();
				}*/

			}
		}

	}

	@Override
	public String checkFeatures(FeatureList fl) throws RemoteException {
		String error = null;

		logger.info("checkFeatures- ");

		if (isPathExists() && features != null) {

			/*logger.info("checkFeatures-features " + features.getSize());
			logger.info("checkFeatures-fl " + fl.getSize());

			if (features.getSize() != fl.getSize()) {
				error = LanguageManager.getText("mapredtexttype.checkfeatures.incorrectsize");
			}*/

			//if (error == null) {

			logger.info("features.getFeaturesNames- "+features.getFeaturesNames());
			logger.info("fl.getFeaturesNames- "+fl.getFeaturesNames());

			for (int i = 0; i < features.getFeaturesNames().size(); i++) {

				String flName = features.getFeaturesNames().get(i);
				String featName = fl.getFeaturesNames().get(i);

				logger.info("checkFeatures-flName " + flName);
				logger.info("checkFeatures-featName " + featName);
				logger.info("checkFeatures-featNameType " + features.getFeatureType(flName));
				logger.info("checkFeatures-flNameType " + fl.getFeatureType(featName));

				if (!fl.getFeatureType(featName).equals(features.getFeatureType(flName))) {
					error = LanguageManager.getText("mapredtexttype.checkfeatures.incorrectnames", new Object[] { flName, featName });
				}

			}



			/*Iterator<String> flIt = fl.getFeaturesNames().iterator();
			Iterator<String> featuresIt = features.getFeaturesNames().iterator();

			while (flIt.hasNext() && error != null) {
				String flName = flIt.next();
				String featName = featuresIt.next();

				logger.info("checkFeatures-flName " + flName);
				logger.info("checkFeatures-featName " + featName);
				logger.info("checkFeatures-flNameType " + fl.getFeatureType(flName));
				logger.info("checkFeatures-featNameType " + features.getFeatureType(featName));

				if (!fl.getFeatureType(flName).equals(features.getFeatureType(featName))) {
					error = LanguageManager.getText("mapredtexttype.checkfeatures.incorrectnames", new Object[] { flName, featName });
				}
			}
			 */

			//}

		}

		logger.info("checkFeatures-error " + error);

		return error;
	}

	private String generateColumnName(int columnIndex) {
		if (columnIndex > 25) {
			return generateColumnName(((columnIndex) / 26) - 1)
					+ generateColumnName(((columnIndex) % 26));
		} else
			return String.valueOf((char) (columnIndex + 65));
	}

	protected String getChar(String asciiCode) {
		String result = null;
		if (asciiCode != null && asciiCode.startsWith("#")
				&& asciiCode.length() > 1) {
			result = String.valueOf(Character.toChars(Integer.valueOf(asciiCode
					.substring(1))));
		} else {
			result = asciiCode;
		}
		return result;
	}

	public String getOctalDelimiter() {
		String asciiCode = getProperty(key_delimiter);
		String result = null;
		if (asciiCode != null && asciiCode.startsWith("#")
				&& asciiCode.length() > 1) {
			result = Integer.toOctalString(Integer.valueOf(asciiCode
					.substring(1)));
			if (result.length() == 2) {
				result = "\\0" + result;
			} else {
				result = "\\" + result;
			}
		}
		return result;
	}

	public String getPigDelimiter() {
		String asciiCode = getProperty(key_delimiter);
		Character c = null;
		if (asciiCode != null && asciiCode.startsWith("#")
				&& asciiCode.length() > 1) {
			int i = Integer.valueOf(asciiCode.substring(1));
			c = new Character((char) i);
		} else if (asciiCode.length() == 1) {
			c = asciiCode.charAt(0);
		}
		return c != null ? PigUtils.getDelimiter(c) : asciiCode;
	}

	public String getDelimiterOrOctal() {
		String octal = getOctalDelimiter();
		return octal != null ? octal
				: getProperty(MapRedTextType.key_delimiter);
	}

	@Override
	protected String getDefaultColor() {
		return "Chocolate";
	}

}