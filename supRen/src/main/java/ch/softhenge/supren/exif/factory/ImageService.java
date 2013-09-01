package ch.softhenge.supren.exif.factory;

import java.io.File;
import java.util.Collection;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;

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
	
	private File baseDir;
	private UserPropertyReader userPropertyReader;

	
	/**
	 * Constructor
	 * 
	 * @param resourceFileName
	 * @param baseDirectory
	 */
	public ImageService(String resourceFileName, String baseDirectory) {
		this.baseDir = new File(baseDirectory);
		this.userPropertyReader = new UserPropertyReader(resourceFileName);
	}

	
	public void RenameFiles() {
		
	}

	public Collection<File> listImageFilesToRename() {
		Collection<File> listAllImageFiles = listAllImageFilesInDir();
		for (File file : listAllImageFiles) {
			
		}
		return null;
	}

	
	
	/**
	 * Return a Collection of all image Files in the base directory
	 * 
	 * @return
	 */
	public Collection<File> listAllImageFilesInDir() {
        String fileExtensionList = userPropertyReader.getPropertyMapOfProperty(PropertyName.fileExtensionList).get(UserPropertyReader.INDEX_IF_EXACTLYONE); 
		String[] extensions = fileExtensionList.split(",");
		
		long currTime = System.currentTimeMillis();
		Collection<File> listFiles = FileUtils.listFiles(baseDir, extensions, true);
		LOGGER.fine("Anz Files: " + listFiles.size() + " , took: " + (System.currentTimeMillis() - currTime) + " ms");
		
		return listFiles;
	}
	
}
