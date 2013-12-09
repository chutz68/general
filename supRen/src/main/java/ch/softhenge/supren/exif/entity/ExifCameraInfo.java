package ch.softhenge.supren.exif.entity;

import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifIFD0Directory;

public class ExifCameraInfo {

	private static final int CAMERA_MODEL_EXIF_TAG = ExifIFD0Directory.TAG_MODEL;

	private final String cameraModel;
	private final String camera4ch;

	/**
	 * 
	 * @param cameraModel
	 * @param camera4ch 4 character Camera name
	 */
	public ExifCameraInfo(String cameraModel, String camera4ch) {
		this.cameraModel= cameraModel;
		this.camera4ch = camera4ch;
	}
	
 	
}
