package ch.softhenge.supren.exif.entity;

import java.util.regex.Pattern;

/**
 * An object Containing all information about a File Pattern
 * 
 * @author werni
 *
 */
public class FilePattern {
	private final String filePatternString;
	private final Integer patternIdx;
	private final Pattern filePattern;
	
	private static final String UNKNOWN_PATTERN = "Unknown";

	public static final FilePattern UNKNOWN_FILE_PATTERN = new FilePattern(UNKNOWN_PATTERN, 0, null);
	
	/**
	 * Constructor
	 * 
	 * @param filePatternString
	 * @param patternIdx
	 * @param filePattern
	 */
	public FilePattern(String filePatternString, Integer patternIdx, Pattern filePattern) {
		this.filePatternString = filePatternString;
		this.filePattern = filePattern;
		this.patternIdx = patternIdx;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((filePatternString == null) ? 0 : filePatternString
						.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FilePattern other = (FilePattern) obj;
		if (filePatternString == null) {
			if (other.filePatternString != null)
				return false;
		} else if (!filePatternString.equals(other.filePatternString))
			return false;
		return true;
	}

	public String getFilePatternString() {
		return filePatternString;
	}

	public Integer getPatternIdx() {
		return patternIdx;
	}

	public Pattern getFilePattern() {
		return filePattern;
	}
}
