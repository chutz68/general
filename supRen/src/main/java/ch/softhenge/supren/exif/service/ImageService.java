package ch.softhenge.supren.exif.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.comparator.LastModifiedFileComparator;

import ch.softhenge.supren.exif.entity.ExifFileInfo;
import ch.softhenge.supren.exif.entity.FilePattern;
import ch.softhenge.supren.exif.entity.ImageFile;
import ch.softhenge.supren.exif.file.ImageFileValidator;
import ch.softhenge.supren.exif.file.OutFilenameGenerator;
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
	private final static String UNIX_SEPERATOR = "/";
	
	private final File baseDir;
	private final UserPropertyReader userPropertyReader;
	private final ExifService exifService;
	private final ImageFileValidator imageFileValidator;
	private final OutFilenameGenerator outFilenameGenerator;

	/**The Map with the filename pattern as key **/
	private Map<FilePattern, Collection<ImageFile>> mapOfImageFiles;
	/**This is the mv command that could be used to rename files**/
	private String mvCommand;
	/**This is the mv undo command that could be used to undo renamed files**/
	private String mvUndoCommand;
	/**No mv command for those files possible.*/
	private String mvError;
	/**Already new Filename*/
	private String mvAlreadyDone;
	/**also rename files of unknown cameras with the default camera shortname*/
	private boolean forceRenameUnknownCameras;
	/**The total number of files*/
	private int totalFilesCnt;
	private int totalFilesFilteredOut;
	
	/**
	 * Constructor
	 * 
	 * @param resourceFileName
	 * @param baseDirectory with full path
	 * @param forceRenameUnknownCameras if set true, also rename files of unknown cameras with the default camera shortname 
	 * @param ExifServic. The Exif Service to be used
	 */
	public ImageService(String resourceFileName, String baseDirectory, boolean forceRenameUnknownCameras, ExifService exifService) {
		this.baseDir = new File(baseDirectory);
		this.userPropertyReader = new UserPropertyReader(resourceFileName);
		this.forceRenameUnknownCameras = forceRenameUnknownCameras;
		this.exifService = exifService;
		this.imageFileValidator = new ImageFileValidator(userPropertyReader);
		Map<Integer, String> outfilePatternGroupMap = userPropertyReader.getPropertyMapOfProperty(PropertyName.OutfilePatternGroup);
		this.outFilenameGenerator = new OutFilenameGenerator(outfilePatternGroupMap);
		resetImageFileList();
	}


	/**
	 * Create necessary mv and undo commands of the files to be renamed
	 */
	public void createMvAndUndoCommands(Long daysback) {
		createImageFilesMap(daysback, null);
		StringBuilder sbmv = new StringBuilder();
		StringBuilder sbdone = new StringBuilder();
		StringBuilder sbundomv = new StringBuilder();
		StringBuilder sbErrFilepattern = new StringBuilder();
		StringBuilder sbErrCameraType = new StringBuilder();
		Set<String> unknownCameraModels = new HashSet<String>();
		for (Entry<FilePattern, Collection<ImageFile>> imageFilesEntry : this.mapOfImageFiles.entrySet()) {
			for (ImageFile imageFile : imageFilesEntry.getValue()) {
				if (imageFilesEntry.getKey().equals(FilePattern.UNKNOWN_FILE_PATTERN)) {
					sbErrFilepattern.append("# ImageFile ").append(imageFile.getFileNameAndPath()).append(" can't be renamed. Filepattern is unknown\n");
				} else if (imageFile.getFilePattern().getPatternIdx() == 0) {
					sbErrFilepattern.append("# ImageFile ").append(imageFile.getFileNameAndPath()).append(" can't be renamed. No image number available\n");
				} else if (imageFile.getFilePattern() != null && imageFile.getFilePattern().isOutPattern()) {
					sbdone.append("# ImageFile ").append(imageFile.getFileNameAndPath()).append(" already has new filename\n");
				} else {
					enrichImageFileWithExifInfo(imageFile, unknownCameraModels);
					if (imageFile.isKnownCameraModel()) {
						sbmv.append("mv ").append('"').append(imageFile.getUnixFilePath()).append(UNIX_SEPERATOR).append(imageFile.getOriginalFileName()).append('"').append(" ");
						sbmv.append('"').append(imageFile.getUnixFilePath()).append(UNIX_SEPERATOR).append(imageFile.getNewFileName()).append('"').append("\n");
						sbundomv.append("mv ").append('"').append(imageFile.getUnixFilePath()).append(UNIX_SEPERATOR).append(imageFile.getNewFileName()).append(" ");
						sbundomv.append(imageFile.getUnixFilePath()).append(UNIX_SEPERATOR).append(imageFile.getOriginalFileName()).append('"').append("\n");
					} else {
						if (imageFile.getExifFileInfo() != null) {
							sbErrCameraType.append("# ImageFile ").append(imageFile.getFileNameAndPath()).append(" can't be renamed. Unknown Camera type " + imageFile.getExifFileInfo().getCameraModel() + "\n");
						} else {
							sbErrCameraType.append("# ImageFile ").append(imageFile.getFileNameAndPath()).append(" can't be renamed. Unknown Camera type\n");
						}
					}
				}
			}
		}
		this.mvCommand = sbmv.toString();
		this.mvUndoCommand = sbundomv.toString();
		this.mvAlreadyDone = sbdone.toString();
		this.mvError = new String();
		for (String unknownCamera : unknownCameraModels) {
			mvError = mvError + unknownCamera + "\n";
		}
		this.mvError += sbErrFilepattern.toString();
		this.mvError += sbErrCameraType.toString();
	}
	
	/**
	 * Create csv Files of image Files
	 */
	public String createCsvSeperatedStringOfImageFiles(Long daysback) {
		createImageFilesMap(daysback, null);
		StringBuilder sbCsv = new StringBuilder();
		enrichImageFilesWithExifInfo(sbCsv);
		return (sbCsv.toString());
	}
	
	/**
	 * Copy image files that have a rating > 1 from a folder and it's subfolder (recursive) to a bestOfFolder
	 * 
	 * @param bestOfFolderName: full file name
	 * @param subfolderFilter: The subfolder to be filtered
	 */
	public String copyBestOfToNewFolder(String bestOfFolderName, String subfolderFilter) {
		String bestofFolderUnix = bestOfFolderName.replace("\\", UNIX_SEPERATOR);
		StringBuilder sbcp = new StringBuilder();
		String lastSubFolderName = "";
		resetImageFileList();
		createImageFilesMap(0L, subfolderFilter);
		StringBuilder sbCsv = new StringBuilder();
		enrichImageFilesWithExifInfo(sbCsv);
		int cntAllFilesToCheck = 0;
		int cntAllRatedFiles = 0;
		for (Entry<FilePattern, Collection<ImageFile>> imageFilesEntry : this.mapOfImageFiles.entrySet()) {
			if (imageFilesEntry != null) {
				for (ImageFile imageFile : imageFilesEntry.getValue()) {
					cntAllFilesToCheck++;
					if (imageFile.getExifFileInfo() != null && imageFile.getExifFileInfo().getRating() > 1) {
						String currentSubfolder = bestofFolderUnix + UNIX_SEPERATOR + imageFile.getFileNameAndPath().substring(baseDir.getAbsolutePath().length()).split("\\\\")[1];
						if (!lastSubFolderName.equals(currentSubfolder)) {
							sbcp.append("mkdir -p ").append('"').append(currentSubfolder).append('"').append("\n");
							lastSubFolderName = currentSubfolder;
						}
						sbcp.append("cp -p ").append('"').append(imageFile.getUnixFilePath()).append(UNIX_SEPERATOR).append(imageFile.getOriginalFileName()).append('"')
						.append(" ").append('"').append(currentSubfolder).append(UNIX_SEPERATOR).append('"').append("\n");
						//LOGGER.info(imageFile.getFileNameAndPath() + " was copied");
						cntAllRatedFiles++;
					}
				}	
			}
		}
		LOGGER.info("Copy " + cntAllRatedFiles + " files to bestof folder from " + cntAllFilesToCheck + " checked");
		return sbcp.toString();
	}

	/**
	 * Find Files in BestOfFolder that have no rating
	 * 
	 * @return
	 */
	public String findBestOfPhotosWithoutRating() {
		StringBuilder sbcp = new StringBuilder();
		createImageFilesMap(0L, "\\BestOf\\");
		StringBuilder sbCsv = new StringBuilder();
		enrichImageFilesWithExifInfo(sbCsv);
		for (Entry<FilePattern, Collection<ImageFile>> imageFilesEntry : this.mapOfImageFiles.entrySet()) {
			for (ImageFile imageFile : imageFilesEntry.getValue()) {
				if (imageFile.getExifFileInfo().getRating() == 0) {
					sbcp.append(imageFile.getUnixFilePath()).append(UNIX_SEPERATOR).append(imageFile.getOriginalFileName()).append("\n");
				}
			}
		}
		return sbcp.toString();
	}


	/**
	 * Enrich all imageFiles with Exif Infos
	 * This might take a while, since every file is scanned. 
	 * 
	 * @param append
	 */
	public void enrichImageFilesWithExifInfo(Appendable append) {
		Set<String> unknownCameraModels = new HashSet<String>();
		int cnt = -1;
		int totalImages = getListOfImageFiles().size();
		for (Entry<FilePattern, Collection<ImageFile>> imageFilesEntry : this.mapOfImageFiles.entrySet()) {
			for (ImageFile imageFile : imageFilesEntry.getValue()) {
				enrichImageFileWithExifInfo(imageFile, unknownCameraModels);
				try {
					append.append(imageFile.toString()).append("\n");
					//LOGGER.info("Got exif info for image File " + imageFile);
				} catch (IOException e) {
					e.printStackTrace();
				}
				if (++cnt % 100 == 0) {
					LOGGER.info("Got exif info of " + cnt + " fotos of a total of " + totalImages);
				}
				cnt++;				
			}
		}
	}
	
	/**
	 * Create a list of Image Files that are candidates to rename and save it
	 * as a map and get it using getMapOfImageFiles.
	 * @param daysback: Number of days to check for picture files. In case of null or 0, check all files
	 * @param subFolderNameOnly: This subfoldername must exist. If null, check all subfolders
	 */
	public void createImageFilesMap(Long daysback, String subFolderNameOnly) {
		if (getListOfImageFiles().isEmpty()) {
			this.totalFilesCnt = 0;
			this.totalFilesFilteredOut = 0;
			long currentDateTime = (new Date()).getTime();
			Collection<File> listAllImageFiles = listAllImageFilesInDir();
			for (File file : listAllImageFiles) {
				if (daysback != null && daysback > 0 && file.lastModified() <= currentDateTime - (daysback * 1000 * 24 * 60 * 60)) {
					totalFilesFilteredOut++;
					continue;
				}
				String filePathOnly = file.getPath().substring(0, file.getPath().lastIndexOf(File.separator)+1);
				if (subFolderNameOnly != null && !filePathOnly.toUpperCase().contains(subFolderNameOnly.toUpperCase())) {
					totalFilesFilteredOut++;
					continue;
				}
				FilePattern filePattern = imageFileValidator.getFilePattern(file.getName());
				ImageFile imageFile;
				if (filePattern != null) {
					String imageNumber = imageFileValidator.getInfilePatternImgNum(file.getName(), filePattern.getPatternIdx());
					if (imageNumber == null) {
						imageNumber = String.format("%04d", this.mapOfImageFiles.get(filePattern).size() + ImageFile.FIRST_IMAGE_NUMBER);
						imageFile = new ImageFile(file, imageNumber, false, filePattern);
					} else {
						imageFile = new ImageFile(file, imageNumber, true, filePattern);
					}
				} else {
					filePattern = FilePattern.UNKNOWN_FILE_PATTERN;
					imageFile = new ImageFile(file, null, false, filePattern);
				}
				this.mapOfImageFiles.get(filePattern).add(imageFile);
				totalFilesCnt++;
			}
			LOGGER.fine("Anz Files used: " + totalFilesCnt);
			LOGGER.fine("Anz Files filtered out: " + totalFilesFilteredOut);
		}
	}

	
	public Map<FilePattern, Collection<ImageFile>> getMapOfImageFiles() {
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
		return this.mapOfImageFiles.get(FilePattern.UNKNOWN_FILE_PATTERN);
	}
	

	public String getMvCommand() {
		return mvCommand;
	}


	public String getMvUndoCommand() {
		return mvUndoCommand;
	}


	public String getMvAlreadyDone() {
		return mvAlreadyDone;
	}


	public String getMvError() {
		return mvError;
	}


	/**
	 * Empties the list of Image Files
	 */
	public void resetImageFileList() {
		this.mapOfImageFiles = new HashMap<FilePattern, Collection<ImageFile>>();
		Collection<FilePattern> filePatterns = this.imageFileValidator.getFilePatterns();
		for (FilePattern filePattern : filePatterns) {
			this.mapOfImageFiles.put(filePattern, new ArrayList<ImageFile>());
		}
		this.mapOfImageFiles.put(FilePattern.UNKNOWN_FILE_PATTERN, new ArrayList<ImageFile>());
		this.mvCommand = null;
		this.mvUndoCommand = null;
	}
	
	
	/**
	 * Return a Collection of all image Files in the base directory recursively
	 * 
	 * @return
	 */
	private Collection<File> listAllImageFilesInDir() {
		long currTime = System.currentTimeMillis();
		String [] extensions = getAllExtensions();
		Collection<File> listFiles = FileUtils.listFiles(baseDir, extensions, true);
		Collections.sort((List<File>) listFiles, LastModifiedFileComparator.LASTMODIFIED_REVERSE);
		LOGGER.fine("Anz Files: " + listFiles.size() + " , took: " + (System.currentTimeMillis() - currTime) + " ms");
		
		return listFiles;
	}
	
	/**
	 * 
	 * @return all extensions, also non case sensitive as an array
	 */
	private String[] getAllExtensions() {
        String fileExtensionList = userPropertyReader.getPropertyMapOfProperty(PropertyName.fileExtensionList).get(UserPropertyReader.INDEX_IF_EXACTLYONE); 
        String[] fileExtensionsLowC = fileExtensionList.toLowerCase().split(",");
        String[] fileExtensionsUpC = fileExtensionList.toUpperCase().split(",");
		Set<String> fileExtSet = new HashSet<String>(Arrays.asList(fileExtensionsLowC));
		fileExtSet.addAll(Arrays.asList(fileExtensionsUpC));
		return fileExtSet.toArray(new String[fileExtSet.size()]);
	}
	
	private void enrichImageFileWithExifInfo(ImageFile imageFile, Set<String> unknownCameraModels) {
		if (imageFile.getExifFileInfo() == null) {
			ExifFileInfo exifFileInfo = exifService.getExifInfoFromImageFile(imageFile.getImageFile());
			if (exifFileInfo == null) return;
			String cameraModel4ch = imageFileValidator.getCameraModel4chForCameraModel(exifFileInfo.getCameraModel());
			imageFile.setExifFileInfo(exifFileInfo);
			imageFile.setCameraModel4ch(cameraModel4ch);
			if (!cameraModel4ch.equals(imageFileValidator.getUnknownCamera4ch())) {
				imageFile.setKnownCameraModel(true);
			} else if (!imageFile.getFilePattern().isUnknownPattern() && imageFile.getExifFileInfo().getPictureDate() != null) {
				unknownCameraModels.add(imageFile.getExifFileInfo().getCameraModel());
				if (forceRenameUnknownCameras) {
					imageFile.setKnownCameraModel(true);
				}
			}
			if (!imageFile.getFilePattern().isUnknownPattern()) {
				String outFileName = generateOutFileName(imageFile.getExifFileInfo().getPictureDate(), cameraModel4ch, imageFile.getImageNumber());
				imageFile.setNewFileName(outFileName);
			}
		}
	}
	
	private String generateOutFileName(Date pictureDate, String cameraModel4ch, String imageNumber) {
		return outFilenameGenerator.createOutFileName(pictureDate, cameraModel4ch, imageNumber);
	}
	
}
