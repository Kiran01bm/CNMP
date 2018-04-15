package com.cnmpdemo.logging;

import java.io.IOException;
import java.util.logging.*;

/**
 * Created by kiranya on 7/4/18.
 */
public class CNMPDemoLogger {

    // Get the global logger to configure it
    public Logger logger;
    public FileHandler fileHandler;
    public Formatter formatterTxt;

    public void setup() throws IOException {

        try {
            // Create logger
            logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
            logger.setLevel(Level.ALL);

            // Configure the file handler
            fileHandler = new FileHandler("cnmpdemo.log");
            fileHandler.setLevel(Level.ALL);

            // Add filehandler to the logger
            logger.addHandler(fileHandler);

            // Create formatter
            formatterTxt = new CNMPDemoFormatter();

            // Add formatter to the filehandler
            fileHandler.setFormatter(formatterTxt);

        } catch(IOException exception) {
            logger.log(Level.SEVERE, "Error occur in FileHandler.", exception);
        }
    }
}

