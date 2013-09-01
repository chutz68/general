package ch.softhenge.supren.exif.tag;

import java.text.Format;
import java.text.SimpleDateFormat;

import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;


public enum AvailableTags {

	FileDateOrig_yyyyddmm(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL, new SimpleDateFormat("YYYYMMDD")),
	CameraModel(ExifIFD0Directory.TAG_MODEL, null);
	
	private final int tagId;
	private final Format format;
	
	public int getTagId() {
		return tagId;
	}

	public Format getFormat() {
		return format;
	}
	
	private AvailableTags(int tagId, Format format) {
		this.tagId = tagId;
		this.format = format;
	}
	
}
