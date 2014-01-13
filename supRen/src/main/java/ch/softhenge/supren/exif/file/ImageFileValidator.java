package ch.softhenge.supren.exif.file;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
	
	/** Map of Pattern that contains all Filename Patterns including file extensions**/
	private Map<Integer, Pattern> patternMap;

	/**
	 * 
	 * @param userPropertyReader
	 */
	public ImageFileValidator(UserPropertyReader userPropertyReader) {
		Map<PropertyName, Map<Integer, String>> propertyMap = userPropertyReader
				.getMapOfPropertyMap();
		this.infilePatternMap = propertyMap.get(PropertyName.InfilePattern);
		this.infilePatternImgNumMap = propertyMap.get(PropertyName.InfilePatternImgNumGroup);
		String fileExtensionPattern = propertyMap.get(PropertyName.FileExtensionPattern).get(UserPropertyReader.INDEX_IF_EXACTLYONE);
		this.patternMap = new HashMap<>();
		for (Entry<Integer, String> stringPatternEntry : infilePatternMap.entrySet()) {
			Pattern pattern = Pattern.compile(stringPatternEntry.getValue() + fileExtensionPattern, Pattern.CASE_INSENSITIVE);
			this.patternMap.put(stringPatternEntry.getKey(), pattern);
		}
	}

	/**
	 * Validate image with regular expression against all known image file
	 * patterns. Return the index of the pattern to which it matches.
	 * 
	 * @param imageFileName
	 * @return pattern index if the file matches, null if the filename is of a
	 *         unknown pattern.
	 */
	public Integer getIndexOfKnownFilePattern(String imageFileName) {
		for (Entry<Integer, Pattern> patternEntry : this.patternMap.entrySet()) {
			Matcher matcher = patternEntry.getValue().matcher(imageFileName);
			if (matcher.matches()) {
				return patternEntry.getKey();
			}
		}
		return null;
	}

	/**
	 * Get the 4 digit number from the known filenamepattern with the index
	 * indexOfFilePattern For example from the filename IMG_4711, pattern:
	 * (^img_)([0-9]{4}) whereas img num is in second matcher group, return 4711. If the pattern is
	 * unknown or the patternImageNum is 0, this indicates that there is no
	 * known number: return null also if no Image File number can be
	 * detected. Get the indexOfFilePattern using Method
	 * getIndexOfKnownFilePattern before.
	 * 
	 * @param imageFileName
	 * @param indexOfFilePattern
	 * @return
	 */
	public Integer getInfilePatternImgNum(String imageFileName,
			Integer indexOfFilePattern) {
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
		Pattern pattern = patternMap.get(indexOfFilePattern);
		Matcher matcher = pattern.matcher(imageFileName);
		if (matcher.matches()) {
			Integer imageNumber = Integer.valueOf(matcher.group(filePatternImgNumGroup));
			return imageNumber;
		}
		LOGGER.warning("Image File: " + imageFileName + " doesn't match to pattern: " + filePattern);
		return null;
	}
}
