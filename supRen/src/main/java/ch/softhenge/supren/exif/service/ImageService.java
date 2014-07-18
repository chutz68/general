package ch.softhenge.supren.exif.service;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;

import ch.softhenge.supren.exif.entity.ExifFileInfo;
import ch.softhenge.supren.exif.entity.FilePattern;
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

	private static final String UNKNOWN_PATTERN = "Unknown";

	private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME); 
	
	private final File baseDir;
	private final UserPropertyReader userPropertyReader;
	private final ExifService exifService;
	private final ImageFileValidator imageFileValidator;

	/**The Map with the filename pattern as key **/
	private Map<String, Collection<ImageFile>> mapOfImageFiles;
	/**This is the mv command that could be used to rename files**/
	private String mvCommand;
	/**This is the mv undo command that could be used to undo renamed files**/
	private String mvUndoCommand;
	/**No mv command for those files possible.*/
	private String mvError;
	
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
		resetImageFileList();
	}


	/**
	 * Create necessary mv and undo commands of the files to be renamed
	 */
	public void createMvAndUndoCommands() {
		createImageFilesMap();
		StringBuilder sbmv = new StringBuilder();
		StringBuilder sbundomv = new StringBuilder();
		StringBuilder sberror = new StringBuilder();
		for (Entry<String, Collection<ImageFile>> imageFilesEntry : this.mapOfImageFiles.entrySet()) {
			for (ImageFile imageFile : imageFilesEntry.getValue()) {
				if (imageFilesEntry.getKey().equals(UNKNOWN_PATTERN)) {
					sberror.append("# ImageFile ").append(imageFile.getOriginalFileName()).append(" can't be renamed. Filepattern is unknown\n");
				} else if (imageFile.getFilePattern().getPatternIdx() == 0) {
					sberror.append("# ImageFile ").append(imageFile.getOriginalFileName()).append(" can't be renamed. No image number available\n");
				} else {
					enrichImageFileWithExifInfo(imageFile);
					sbmv.append("mv ").append(imageFile.getFilePath()).append(File.separator).append(imageFile.getOriginalFileName()).append(" ");
					sbmv.append(imageFile.getFilePath()).append(File.separator).append(imageFile.getNewFileName()).append("\n");
					sbundomv.append("mv ").append(imageFile.getFilePath()).append(File.separator).append(imageFile.getNewFileName()).append(" ");
					sbundomv.append(imageFile.getFilePath()).append(File.separator).append(imageFile.getOriginalFileName()).append("\n");						
				}
			}
		}
		this.mvCommand = sbmv.toString();
		this.mvUndoCommand = sbundomv.toString();
		this.mvError = sberror.toString();
	}
	
	public void RenameFiles() {
	}

	/**
	 * Create a list of Image Files that are candidates to rename and save it
	 * as a map and get it using getMapOfImageFiles.
	 */
	public void createImageFilesMap() {
		if (getListOfImageFiles().isEmpty()) {
			Collection<File> listAllImageFiles = listAllImageFilesInDir();
			for (File file : listAllImageFiles) {
				FilePattern filePattern = imageFileValidator.getFilePattern(file.getName());
				ImageFile imageFile;
				String mapKey;
				if (filePattern != null) {
					mapKey = filePattern.getFilePatternString();
					String imageNumber = imageFileValidator.getInfilePatternImgNum(file.getName(), filePattern.getPatternIdx());
					if (imageNumber == null) {
						imageNumber = String.format("%04d", this.mapOfImageFiles.get(mapKey).size() + ImageFile.FIRST_IMAGE_NUMBER);
						imageFile = new ImageFile(file, imageNumber, false, filePattern);
					} else {
						imageFile = new ImageFile(file, imageNumber, true, filePattern);
					}
				} else {
					mapKey = UNKNOWN_PATTERN;
					imageFile = new ImageFile(file, null, false, FilePattern.UNKNOWN_FILE_PATTERN);
				}
				this.mapOfImageFiles.get(mapKey).add(imageFile);
			}
		}
	}

	
	public Map<String, Collection<ImageFile>> getMapOfImageFiles() {
		return mapOfImageFiles;
	}
	
	/**
	 * 
	 * @return a List of all Image Files no mater whether they are known or not
	 */
	public Collection<ImageFile> getListOfImageFiles() {
		Collection<ImageFile> resultFiles = new ArrayList<>();
		for (Collection<ImageFile> imageFiles : this.mapOfImageFiles.values()) {
			resultFiles.addAll(imageFiles);
		}
		return resultFiles;
	}

	/**
	 * 
	 * @return a List of all unknown Image Files
	 */
	public Collection<ImageFile> getListOfUnknownImageFiles() {
		return this.mapOfImageFiles.get(UNKNOWN_PATTERN);
	}
	

	public String getMvCommand() {
		return mvCommand;
	}


	public String getMvUndoCommand() {
		return mvUndoCommand;
	}


	public String getMvError() {
		return mvError;
	}


	/**
	 * Empties the list of Image Files
	 */
	public void resetImageFileList() {
		this.mapOfImageFiles = new HashMap<String, Collection<ImageFile>>();
		Map<Integer, String> infilePatternMap = this.userPropertyReader.getPropertyMapOfProperty(PropertyName.InfilePattern);
		for (String infilePattern : infilePatternMap.values()) {
			this.mapOfImageFiles.put(infilePattern, new ArrayList<ImageFile>());
		}
		this.mapOfImageFiles.put(UNKNOWN_PATTERN, new ArrayList<ImageFile>());
		this.mvCommand = null;
		this.mvUndoCommand = null;
	}
	
	
	/**
	 * Return a Collection of all image Files in the base directory
	 * 
	 * @return
	 */
	private Collection<File> listAllImageFilesInDir() {
        String fileExtensionList = userPropertyReader.getPropertyMapOfProperty(PropertyName.fileExtensionList).get(UserPropertyReader.INDEX_IF_EXACTLYONE); 
		String[] extensions = fileExtensionList.split(",");
		
		long currTime = System.currentTimeMillis();
		Collection<File> listFiles = FileUtils.listFiles(baseDir, extensions, true);
		LOGGER.fine("Anz Files: " + listFiles.size() + " , took: " + (System.currentTimeMillis() - currTime) + " ms");
		
		return listFiles;
	}
	
	private void enrichImageFileWithExifInfo(ImageFile imageFile) {
		if (imageFile.getExifFileInfo() == null) {
			ExifFileInfo exifFileInfo = exifService.getExifInfoFromImageFile(imageFile.getImageFile());
			String cameraModel4ch = imageFileValidator.getCameraModel4chForCameraModel(exifFileInfo.getCameraModel());
			imageFile.setExifFileInfo(exifFileInfo);
			imageFile.setCameraModel4ch(cameraModel4ch);
		}
	}
	
}