package ch.softhenge.supren.exif.file;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validates an image file against a set of Patterns
 * 
 * @author werni
 *
 */
public class ImageFileValidator {

	private Map<Integer, Pattern> patternMap;
	private Map<Integer, List<String>> matchesFilenameList; //A List of all images that were matches using the pattern with the index as key
	private List<String> unknownFilenameList;

	/**
	 * Constructor
	 * 
	 * @param stringPatternMap
	 * @param fileExtensionPattern
	 */
	public ImageFileValidator(Map<Integer, String> stringPatternMap, String fileExtensionPattern) {
		patternMap = new HashMap<>();
		matchesFilenameList = new HashMap<>();
		unknownFilenameList = new ArrayList<>();
		for (Entry<Integer, String> stringPatternEntry : stringPatternMap.entrySet()) {
			Pattern pattern = Pattern.compile(stringPatternEntry.getValue() + fileExtensionPattern, Pattern.CASE_INSENSITIVE);
			patternMap.put(stringPatternEntry.getKey(), pattern);
			matchesFilenameList.put(stringPatternEntry.getKey(), new ArrayList<String>());
		}
	}

	/**
	 * Validate image with regular expression against all known image file patterns
	 * 
	 * @param imageFileName
	 * @return true: filename is a known filename, false is an unknown filename
	 */
	public boolean validate(final String imageFileName) {
		for (Entry<Integer, Pattern> patternEntry : this.patternMap.entrySet()) {
			Matcher matcher = patternEntry.getValue().matcher(imageFileName);
			if (matcher.matches()) {
				matchesFilenameList.get(patternEntry.getKey()).add(imageFileName);
				return true;
			}
		}
		unknownFilenameList.add(imageFileName);
		return false;
	}

	/**
	 * Get the number of filenames that matched the pattern
	 * 
	 * @param idx
	 * @return number of matches, 0 if none or unknown index
	 */
	public Integer getMatchesFilenameCountForPatternIdx(int idx) {
		if (matchesFilenameList.containsKey(idx)) {
			return matchesFilenameList.get(idx).size();
		}
		return 0;
	}

	/**
	 * Return a list of all filenames that matched the pattern
	 * 
	 * @param idx
	 * @return
	 */
	public List<String> getFilenameListForPatternIdx(int idx) {
		if (matchesFilenameList.containsKey(idx)) {
			return matchesFilenameList.get(idx);
		}
		return Collections.emptyList();
	}

	/**
	 * Get a list of Files that are not known and don't match any known pattern
	 * 
	 * @return
	 */
	public List<String> getUnknownFilenameList() {
		return unknownFilenameList;
	}
}
