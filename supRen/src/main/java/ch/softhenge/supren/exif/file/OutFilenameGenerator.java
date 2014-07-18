package ch.softhenge.supren.exif.file;

import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

/**
 * Class is able to create an out filename from the out file pattern and several information such as exif date, image number and exif camera info
 * 
 * @author werni
 *
 */
public class OutFilenameGenerator {

	private static final String PICTURE_DATE = "${PICTURE_DATE}";
	private static final String CAMERA_MODEL4CH = "${CAMERA_MODEL4CH}";
	private static final String IMG_NR = "${IMG_NR}";
	private static final String SEPARATOR = "_";
	
	private final String outFilePattern;
	private final TreeMap<Integer, String> outfilePatternGroupMap;

	/**
	 * Constructor
	 * 
	 * @param outFilePattern
	 */
	public OutFilenameGenerator(String outFilePattern, Map<Integer, String> outfilePatternGroupMap) {
		this.outFilePattern = outFilePattern;
		int patternsFound = 0;
		for (String outfilePattern : outfilePatternGroupMap.values()) {
			if (outfilePattern.contains(PICTURE_DATE)) {
				patternsFound++;
			}
			if (outfilePattern.contains(CAMERA_MODEL4CH)) {
				patternsFound++;
			}
			if (outfilePattern.contains(IMG_NR)) {
				patternsFound++;
			}
			if (outfilePattern.contains(SEPARATOR)) {
				patternsFound++;
			}
		}
		assert patternsFound == 5: "Expected 5 pattern but found " + patternsFound ;
		this.outfilePatternGroupMap = new TreeMap<Integer, String>(outfilePatternGroupMap);

	}
	
	/**
	 * Create a filename out of the pictureDate, cameraModel and imageNumber
	 * 
	 * @param pictureDate
	 * @param cameraModel4ch
	 * @param imageNumber
	 * @return
	 */
	public String createFileName(Date pictureDate, String cameraModel4ch, Integer imageNumber) {
		assert 0 <= imageNumber && imageNumber <= 9999: "imageNumber is not valid, should be between 0 and 9999 but is " + imageNumber;
		
		for (String outfilePattern : outfilePatternGroupMap.values()) {
			if (outfilePattern.contains(PICTURE_DATE)) {
				String fileNamePart1 = createFileNamePart1PictureDate(outfilePattern, pictureDate);
			}
		}

		return "";
	}

	private String createFileNamePart1PictureDate(String pictureDatePattern, Date pictureDate) {
		String dateFormat = pictureDatePattern.substring(pictureDatePattern.indexOf("[") + 1, pictureDatePattern.indexOf("]") - 1);
		return null;
	}
	
}
