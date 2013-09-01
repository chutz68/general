package ch.softhenge.supren.exif.factory;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class SupRenMain {

	private final static Logger LOGGER = LogManager.getLogManager().getLogger(Logger.GLOBAL_LOGGER_NAME);

	
	public SupRenMain() {
		LOGGER.setLevel(Level.WARNING);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.WARNING);
		LOGGER.addHandler(handler);
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {

	}

}
