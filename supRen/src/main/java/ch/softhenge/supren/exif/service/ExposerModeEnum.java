package ch.softhenge.supren.exif.service;

/**
 * Exposure program that the camera used when image was taken. '1' means
 * manual control, '2' program normal, '3' aperture priority, '4' shutter
 * priority, '5' program creative (slow program), '6' program action
 * (high-speed program), '7' portrait mode, '8' landscape mode.
 */
public enum ExposerModeEnum {
	manualMode(1),
	programmMode(2),
	aperturePriority(3),
	shutterPriority(4),
	programmCreative(5),
	programmAction(6),
	portraitMode(7),
	landscapeMode(8);
	
	private int exposerVal;
	
	private ExposerModeEnum(int exposerVal) {
		this.exposerVal = exposerVal;
	}
	
	public ExposerModeEnum getExposerMode(int exposerVal) {
		for (ExposerModeEnum curMode : ExposerModeEnum.values()) {
			if (curMode.exposerVal == exposerVal) {
				return curMode;
			}
		}
		return null;
	}
}
