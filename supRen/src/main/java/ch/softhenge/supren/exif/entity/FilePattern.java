package ch.softhenge.supren.exif.entity;

import java.util.regex.Pattern;

/**
 * An object Containing all information about a Pattern of a File that can be understood by the program.
 * 
 * @author werni
 *
 */
public class FilePattern {
	private final String filePatternString;
	private final Integer patternIdx;
	private final Pattern filePattern;
	private final Integer groupOfImageNumber;
	private final boolean isOutPattern;
	
	private static final String UNKNOWN_PATTERN = "Unknown";

	public static final FilePattern UNKNOWN_FILE_PATTERN = new FilePattern(UNKNOWN_PATTERN, 0, null, 0, false);
	
	/**
	 * Constructor
	 * 
	 * @param filePatternString
	 * @param patternIdx
	 * @param filePattern
	 * @param groupOfImageNumber The Group where the image number of the filename can be used, from the pattern
	 * @param isOutPattern
	 */
	public FilePattern(String filePatternString, Integer patternIdx, Pattern filePattern, Integer groupOfImageNumber, boolean isOutPattern) {
		this.filePatternString = filePatternString;
		this.filePattern = filePattern;
		this.patternIdx = patternIdx;
		this.groupOfImageNumber = groupOfImageNumber;
		this.isOutPattern = isOutPattern;
	}

	@Override
	public int hashCode() {
		return filePatternString == null ? 0 : filePatternString.hashCode();
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
	
	public Integer getGroupOfImageNumber() {
		return groupOfImageNumber;
	}
	
	/**
	 * @return true if the pattern contains the image number in the pattern.
	 * use getGroupOfImageNumber() to get the concrete group.
	 */
	public boolean hasImageNumberInFilePattern() {
		return groupOfImageNumber > 0;
	}
	
	public boolean isOutPattern() {
		return isOutPattern;
	}

	public String toString() {
		String stringVar = patternIdx + ": " + filePatternString;
		if (isOutPattern) {
			stringVar = stringVar + " is Outfile Pattern";
		}
		return stringVar;
	}
}
