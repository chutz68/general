package ch.softhenge.supren.exif.service;

import java.util.HashMap;
import java.util.Map;

public enum MetaDataExtractorTags {
	//CompressionType(-3),
	//DataPrecision(0),
	ImageHeight(1), 				// 3240 pixels
	ImageWidth(3),  				// 2160 pixels
	//NumberofComponents(5),
	//Component1(6),
	//Component2(7),
	//Component3(8),
	Make(271), 						// Canon
	Model(272), 					// Canon EOS 6D
	//XResolution(282),
	//YResolution(283),
	//ResolutionUnit(296),
	//Software(305),
	DateTime(306), 					// 2013:04:03 00:52:48
	ExposureTime(33434), 			// 1/250 sec
	FNumber(33437), 				// f/5.6
	ExposureProgram(34850), 		// Program normal
	ISOSpeedRatings(34855), 		// 100
	SensitivityType(34864), 		// Recommended Exposure Index
	//RecommendedExposureIndex(34866),
	ExifVersion(36864), 			// 2.30
	DateTimeOriginal(36867), 		// 2013:04:01 11:26:07
	//DateTimeDigitized(36868),
	ShutterSpeedValue(37377), 		// 1/249 sec
	ApertureValue(37378), 			// f/5.6
	ExposureBiasValue(37380), 		// 0 EV
	MaxApertureValue(37381), 		// f/5.7
	MeteringMode(37383), 			// Multi-segment
	Flash(37385), 					// Flash did not fire
	FocalLength(37386), 			// 250 mm
	//SubSecTimeOriginal(37521),
	//SubSecTimeDigitized(37522),
	//FocalPlaneXResolution(41486),
	//FocalPlaneYResolution(41487),
	//FocalPlaneResolutionUnit(41488),
	CustomRendered(41985), 			// Normal process
	ExposureMode(41986), 			// Auto exposure
	WhiteBalanceMode(41987), 		// Auto white balance
	SceneCaptureType(41990), 		// Standard
	//BodySerialNumber(42033),
	LensSpecification(42034), 		// 70-300mm
	LensModel(42036), 				// EF70-300mm f/4-5.6 IS USM
	//LensSerialNumber(42037),
	Compression(259), 				// JPEG (old-style)
	//ThumbnailOffset(513),
	//ThumbnailLength(514),
	//ResolutionInfo(1005),
	//ThumbnailData(1036),
	//CaptionDigest(1061),
	//CodedCharacterSet(346),
	//ApplicationRecordVersion(512),
	//DateCreated(567),
	//TimeCreated(572),
	//DigitalDateCreated(574),
	//DigitalTimeCreated(575)
	;
	
	private int tagType;
	
	private MetaDataExtractorTags(int tagType) {
		this.tagType = tagType;
	}

	public static Map<Integer, String> getAllExtractorTags() {
		Map<Integer, String> allTags = new HashMap<>();
		for (MetaDataExtractorTags curTag : MetaDataExtractorTags.values()) {
			allTags.put(curTag.tagType, null);
		}
		return allTags;
	}
	
}
