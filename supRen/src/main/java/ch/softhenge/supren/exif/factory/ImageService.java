package ch.softhenge.supren.exif.factory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;

import ch.softhenge.supren.exif.entity.ImageFile;
import ch.softhenge.supren.exif.file.ImageFileValidator;
import ch.softhenge.supren.exif.property.UserPropertyReader;
import ch.softhenge.supren.exif.property.UserPropertyReader.PropertyName;

/**
 * Image Service is able to handle image filenames and compare them against patterns.
 * 
 * @author Werni
 *
 */
public class ImageService {

	private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME); 
	
	private final File baseDir;
	private final UserPropertyReader userPropertyReader;
	private final ExifService exifService;
	private final ImageFileValidator imageFileValidator;

	private final Collection<ImageFile> imageFileCollection;
	
	/**
	 * Constructor
	 * 
	 * @param resourceFileName
	 * @param baseDirectory
	 */
	public ImageService(String resourceFileName, String baseDirectory) {
		this.baseDir = new File(baseDirectory);
		this.userPropertyReader = new UserPropertyReader(resourceFileName);
		this.exifService = new ExifService();
		this.imageFileValidator = new ImageFileValidator(userPropertyReader);
		this.imageFileCollection = new ArrayList<>();
	}

	
	public void RenameFiles() {
	}

	public void createListOfImageFilesToRename() {
		if (imageFileCollection.isEmpty()) {
			Collection<File> listAllImageFiles = listAllImageFilesInDir();
			for (File file : listAllImageFiles) {
				Date pictureDate = exifService.getPictureDate(file);
				String cameraModel = exifService.getCameraModel(file);
				Entry<Integer, Pattern> patternEntry = imageFileValidator.getIndexOfKnownFilePattern(file.getName());
				ImageFile imageFile;
				if (patternEntry.getKey() != null) {
					String imageNumber = imageFileValidator.getInfilePatternImgNum(file.getName(), patternEntry.getKey());
					String cameraModel4ch = "";
					imageFile = new ImageFile(file, cameraModel, pictureDate, imageNumber, cameraModel4ch, patternEntry.getValue().pattern());
				} else {
					imageFile = new ImageFile(file, cameraModel, pictureDate, null, null, null);
				}
				imageFileCollection.add(imageFile);
			}
		}
	}

	public Collection<ImageFile> getImageFileCollection() {
		return imageFileCollection;
	}
	
	/**
	 * Empties the list of Image Files
	 */
	public void resetImageFileList() {
		imageFileCollection.clear();
	}
	
	
	/**
	 * Return a Collection of all image Files in the base directory
	 * 
	 * @return
	 */
	Collection<File> listAllImageFilesInDir() {
        String fileExtensionList = userPropertyReader.getPropertyMapOfProperty(PropertyName.fileExtensionList).get(UserPropertyReader.INDEX_IF_EXACTLYONE); 
		String[] extensions = fileExtensionList.split(",");
		
		long currTime = System.currentTimeMillis();
		Collection<File> listFiles = FileUtils.listFiles(baseDir, extensions, true);
		LOGGER.fine("Anz Files: " + listFiles.size() + " , took: " + (System.currentTimeMillis() - currTime) + " ms");
		
		return listFiles;
	}
	
}
