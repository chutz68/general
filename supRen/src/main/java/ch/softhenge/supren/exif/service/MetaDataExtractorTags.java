package ch.softhenge.supren.exif.service;

import java.util.HashSet;
import java.util.Set;

public enum MetaDataExtractorTags {
	CompressionType(-3),
	DataPrecision(0),
	ImageHeight(1),
	ImageWidth(3),
	NumberofComponents(5),
	Component1(6),
	Component2(7),
	Component3(8),
	Make(271),
	Model(272),
	XResolution(282),
	YResolution(283),
	ResolutionUnit(296),
	Software(305),
	DateTime(306),
	ExposureTime(33434),
	FNumber(33437),
	ExposureProgram(34850),
	ISOSpeedRatings(34855),
	SensitivityType(34864),
	RecommendedExposureIndex(34866),
	ExifVersion(36864),
	DateTimeOriginal(36867),
	DateTimeDigitized(36868),
	ShutterSpeedValue(37377),
	ApertureValue(37378),
	ExposureBiasValue(37380),
	MaxApertureValue(37381),
	MeteringMode(37383),
	Flash(37385),
	FocalLength(37386),
	SubSecTimeOriginal(37521),
	SubSecTimeDigitized(37522),
	FocalPlaneXResolution(41486),
	FocalPlaneYResolution(41487),
	FocalPlaneResolutionUnit(41488),
	CustomRendered(41985),
	ExposureMode(41986),
	WhiteBalanceMode(41987),
	SceneCaptureType(41990),
	BodySerialNumber(42033),
	LensSpecification(42034),
	LensModel(42036),
	LensSerialNumber(42037),
	Compression(259),
	ThumbnailOffset(513),
	ThumbnailLength(514),
	ResolutionInfo(1005),
	ThumbnailData(1036),
	CaptionDigest(1061),
	CodedCharacterSet(346),
	ApplicationRecordVersion(512),
	DateCreated(567),
	TimeCreated(572),
	DigitalDateCreated(574),
	DigitalTimeCreated(575)
	;
	
	private int tagType;
	
	private MetaDataExtractorTags(int tagType) {
		this.tagType = tagType;
	}

	public Set<Integer> getAllExtractorTags() {
		Set<Integer> allTags = new HashSet<>();
		for (MetaDataExtractorTags curTag : MetaDataExtractorTags.values()) {
			allTags.add(curTag.tagType);
		}
		return allTags;
	}
	
}
