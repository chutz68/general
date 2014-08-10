package ch.softhenge.supren.exif.file;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ch.softhenge.supren.exif.entity.FilePattern;
import ch.softhenge.supren.exif.property.UserPropertyReader;
import ch.softhenge.supren.exif.property.UserPropertyReader.PropertyName;

/**
 * Validates an image file against a set of Patterns
 * 
 * @author werni
 * 
 */
public class ImageFileValidator {

	private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME); 
	
	/**
	 * This map contains all known filename patterns. The map key is the index
	 * number
	 **/
	private final Map<Integer, String> infilePatternMap;
	
	/**
	 * This map contains from all known filename patterns the position of the
	 * image number. The map key is the index number
	 **/
	private final Map<Integer, String> infilePatternImgNumMap;
	
	/**A map with key PatternIdx, containing all known File Patterns**/
	private final Map<Integer, FilePattern> filePatternMap;
	
	/**A Map with the cameraModel as key and cameraModel4ch as value**/
	private final Map<String, String> cameraModelMap;
	
	/**
	 * Constructor
	 * 
	 * Read in and out patterns and camera models and read it into map.
	 * If reading infile pattern, make sure that outfile pattern is also an infile pattern
	 * 
	 * @param userPropertyReader
	 */
	public ImageFileValidator(UserPropertyReader userPropertyReader) {
		Map<PropertyName, Map<Integer, String>> propertyMap = userPropertyReader.getMapOfPropertyMap();
		this.infilePatternMap = propertyMap.get(PropertyName.InfilePattern);
		String outFilePattern = propertyMap.get(PropertyName.OutfilePattern).get(UserPropertyReader.INDEX_IF_EXACTLYONE);
		this.infilePatternImgNumMap = propertyMap.get(PropertyName.InfilePatternImgNumGroup);
		String fileExtensionPattern = propertyMap.get(PropertyName.FileExtensionPattern).get(UserPropertyReader.INDEX_IF_EXACTLYONE);
		this.filePatternMap = new HashMap<Integer, FilePattern>();
		boolean foundOutFilePattern = false;
		int largestPatternIdx = -1;
		for (Entry<Integer, String> stringPatternEntry : infilePatternMap.entrySet()) {
			Pattern pattern = Pattern.compile(stringPatternEntry.getValue() + fileExtensionPattern, Pattern.CASE_INSENSITIVE);
			boolean isOutFilePattern = outFilePattern.equals(stringPatternEntry.getValue());
			foundOutFilePattern = isOutFilePattern | foundOutFilePattern;
			FilePattern filePattern = new FilePattern(stringPatternEntry.getValue(), stringPatternEntry.getKey(), pattern, isOutFilePattern);
			largestPatternIdx = Math.max(largestPatternIdx, stringPatternEntry.getKey());
			this.filePatternMap.put(stringPatternEntry.getKey(), filePattern);
		}
		if (!foundOutFilePattern) {
			Pattern pattern = Pattern.compile(outFilePattern + fileExtensionPattern, Pattern.CASE_INSENSITIVE);		
			FilePattern filePattern = new FilePattern(outFilePattern, largestPatternIdx + 1, pattern, true);
			this.filePatternMap.put(largestPatternIdx + 1, filePattern);
		}
		this.cameraModelMap = new HashMap<>();
		Map<Integer, String> cameraModelMap = propertyMap.get(PropertyName.CameraModel);
		Map<Integer, String> cameraModel4chMap = propertyMap.get(PropertyName.CameraModel4ch);
		for (Entry<Integer, String> cameraModelEntry : cameraModelMap.entrySet()) {
			String cameraModel4ch = cameraModel4chMap.get(cameraModelEntry.getKey());
			if (cameraModel4ch == null) {
				throw new IllegalArgumentException("No containing cameramodel4ch found for camermodel " + cameraModelEntry.getValue() + " with index " + cameraModelEntry.getKey());
			}
			this.cameraModelMap.put(cameraModelEntry.getValue(), cameraModel4ch);
		}
	}

	/**
	 * Validate image file with regular expression against all known image file patterns. 
	 * Return the according FilePattern object.
	 * 
	 * @param imageFileName
	 * @return the according FilePattern object or null if no pattern could be found.
	 */
	public FilePattern getFilePattern(String imageFileName) {
		for (FilePattern filePattern : filePatternMap.values()) {
			Matcher matcher = filePattern.getFilePattern().matcher(imageFileName);
			if (matcher.matches()) {
				return filePattern;
			}
		}
		return null;
	}
	
	/**
	 * Get all File Patterns
	 * 
	 * @return
	 */
	public Collection<FilePattern> getFilePatterns() {
		return filePatternMap.values();
	}

	/**
	 * Get the 4 digit number from the filenamepattern with the index indexOfFilePattern as String with leading zeros. 
	 * For example from the filename IMG_4711, pattern:
	 * (^img_)([0-9]{4}) whereas img num is in second matcher group, return 4711. 
	 * If the pattern is unknown or the patternImageNum is 0, this indicates that there is no
	 * known number: return null also if no Image File number can be
	 * detected. Get the indexOfFilePattern using Method
	 * getIndexOfKnownFilePattern before.
	 * 
	 * @param imageFileName
	 * @param indexOfFilePattern
	 * @return
	 */
	public String getInfilePatternImgNum(String imageFileName, Integer indexOfFilePattern) {
		if (indexOfFilePattern == null || imageFileName == null) {
			return null;
		}
		String filePattern = this.infilePatternMap.get(indexOfFilePattern);
		String filePatternImgStrg = this.infilePatternImgNumMap.get(indexOfFilePattern);
		int filePatternImgNumGroup;
		try {
			filePatternImgNumGroup = Integer.valueOf(filePatternImgStrg);
		} catch (NumberFormatException e) {
			throw new NumberFormatException(
					"Can't get a Number from imageFileName: " + imageFileName
							+ " filePattern: " + filePattern
							+ ". This should be a number, but is isn't: "
							+ filePatternImgStrg);
		}
		if (filePatternImgNumGroup == 0) {
			return null;
		}
		Pattern pattern = filePatternMap.get(indexOfFilePattern).getFilePattern();
		Matcher matcher = pattern.matcher(imageFileName);
		if (matcher.matches()) {
			String imageNumber = matcher.group(filePatternImgNumGroup);
			return imageNumber;
		}
		LOGGER.warning("Image File: " + imageFileName + " doesn't match to pattern: " + filePattern);
		return null;
	}
	
	/**
	 * Return cameraModel4ch for CameraModel
	 * 
	 * @param cameraModel
	 * @return
	 */
	public String getCameraModel4chForCameraModel(String cameraModel) {
		return this.cameraModelMap.get(cameraModel);
	}
	
}
