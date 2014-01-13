package ch.softhenge.supren.exif.property;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UserPropertyReader {

	public static final int INDEX_IF_EXACTLYONE = 1;
	private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME); 

	
	public enum Occurence {
		ExactlyOnce,
		OnceOrMultiple;
	}
	
	public enum PropertyName {
		FileExtensionPattern("fileExtensionPattern", null, Occurence.ExactlyOnce),
		fileExtensionList("fileExtensionList", null, Occurence.ExactlyOnce),
		InfilePattern("(infilePattern)([0-9]{3})", 2, Occurence.OnceOrMultiple),
		InfilePatternImgNumGroup("(infilePatternImgNumGroup)([0-9]{3})", 2, Occurence.OnceOrMultiple),
		OutfilePattern("outfilePattern", null, Occurence.ExactlyOnce),
		OutfilePatternImgNumGroup("outfilePatternImgNumGroup", null, Occurence.ExactlyOnce),
		OutfilePatternGroup("outfilePatternGroup", 5, Occurence.OnceOrMultiple),
		OutfileCase("outfileCase", null, Occurence.ExactlyOnce),
		CameraModel("(cameraModel)([0-9]{3})", 2, Occurence.OnceOrMultiple),
		CameraModel4ch("(cameraModel4ch)([0-9]{3})", 2, Occurence.OnceOrMultiple);
		
		private String patternName;
		private Integer patternNumGroupPos;
		private Occurence occurence;
		
		private PropertyName(String patternName, Integer patternNumGroupPos, Occurence occurence) {
			this.patternName = patternName;
			this.patternNumGroupPos = patternNumGroupPos;
			this.occurence = occurence;
		}
	}	
	
	private String fileName;
	private Map<PropertyName, Map<Integer, String>> mapOfPropertyMap;

	/**
	 * Constructor
	 * @param fileName
	 */
	public UserPropertyReader(String fileName) {
		this.fileName = fileName;
		this.mapOfPropertyMap = new HashMap<PropertyName, Map<Integer, String>>();
		Properties properties = readPropertyFile();
		fillPropertyMap(properties);
		verifyProperties();
	}

	public Map<PropertyName, Map<Integer, String>> getMapOfPropertyMap() {
		return mapOfPropertyMap;
	}
	
	public Map<Integer, String> getPropertyMapOfProperty(PropertyName propertyName)  {
		return mapOfPropertyMap.get(propertyName);
	}
	
	private Properties readPropertyFile() {
    	Properties properties = new Properties();
        //load a properties file
		try {
			String fileNamePath = this.getClass().getClassLoader().getResource(fileName).getFile();
			properties.load(new FileInputStream(fileNamePath));
			return properties;
		} catch (IOException e) {
			LOGGER.severe("Proprties File " + fileName + " nicht auffindbar");
			throw new RuntimeException(e);
		}
	}
	
	private void fillPropertyMap(Properties properties)  {
		Map<Integer,String> propertyElement;
		int index;
		for (Entry<Object, Object> propEntry : properties.entrySet()) {
			for (PropertyName propEnum : PropertyName.values()) {
				Pattern pat = Pattern.compile(propEnum.patternName);
				Matcher mat = pat.matcher(propEntry.getKey().toString());
				if (mat.matches()) {
					if (propEnum.occurence == Occurence.OnceOrMultiple) {
						index = Integer.valueOf(mat.group(propEnum.patternNumGroupPos));
					} else {
						index = INDEX_IF_EXACTLYONE;
					}
					if (this.mapOfPropertyMap.containsKey(propEnum)) {
						this.mapOfPropertyMap.get(propEnum).put(index, propEntry.getValue().toString());
					} else {
						propertyElement = new HashMap<Integer, String>();
						propertyElement.put(index, propEntry.getValue().toString());	
						this.mapOfPropertyMap.put(propEnum, propertyElement);
					}
				}
			}
		}
	}
	
	private void verifyProperties() {
		Map<Integer, String> infilePatterns = getPropertyMapOfProperty(PropertyName.InfilePattern);
		for (Entry<Integer, String> infilePatternEntry : infilePatterns.entrySet()) {
			int infFilePatternImgNum = Integer.valueOf(getPropertyMapOfProperty(PropertyName.InfilePatternImgNumGroup).get(infilePatternEntry.getKey()));
			if (infFilePatternImgNum == 0) continue;
			String numberStr = infilePatternEntry.getValue().split("\\(")[infFilePatternImgNum];
			numberStr = numberStr.split("\\)")[0];
			if (!"[0-9]{4}".equals(numberStr)) {
				throw new IllegalArgumentException("Pattern: " + infilePatternEntry.getValue() + " is invalid or the image number index: "
			            + infFilePatternImgNum + " doesn't match for patern with index " +  infilePatternEntry.getKey());
			}
		}
	}
}
