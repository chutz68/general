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
