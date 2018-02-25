package ch.softhenge.supren.exif.service;

import java.io.File;

import ch.softhenge.supren.exif.entity.ExifFileInfo;

public interface ExifService {

	/**
	 * Return a new ExifFileInfo Object with Exif infos from the image File imageFile.
	 * Return an ExifFileInfo Object containing null values if no Exif info is available from the file.
	 * 
	 * Be careful when using this service with many files. The Exif Tag reading is pretty slow.
	 * 
	 * @param imageFile
	 * @return
	 */
	ExifFileInfo getExifInfoFromImageFile(File imageFile);

}