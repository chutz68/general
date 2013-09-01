package ch.softhenge.supren.exif.entity;

import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifIFD0Directory;

public class ExifCameraInfo {

	private static final int CAMERA_MODEL_EXIF_TAG = ExifIFD0Directory.TAG_MODEL;

	private String cameraModel;
	private String cameraOut;

	/**
	 * 
	 * @param cameraModel
	 * @param cameraOut Part of the filename
	 */
	public ExifCameraInfo(String cameraModel, String cameraOut) {
		this.cameraModel= cameraModel;
		this.cameraOut = cameraOut;
	}
	
 	
}
