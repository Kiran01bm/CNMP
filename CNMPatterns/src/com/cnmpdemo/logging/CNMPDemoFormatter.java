package com.cnmpdemo.logging;

import java.text.MessageFormat;
import java.util.Date;
import java.util.logging.LogRecord;
import java.util.logging.Formatter;

/**
 * Created by kiranya on 7/4/18.
 */
public class CNMPDemoFormatter extends Formatter {
    private final MessageFormat messageFormat = new MessageFormat("[{3,date,hh:mm:ss} {2} {0} {5}]{4} \n");

    private final String emptyString = "";

    @Override
    public String format (LogRecord record)
    {
        Object[] arguments = new Object[6];

        arguments[0] = emptyString;//record.getLoggerName();
        arguments[1] = emptyString;//record.getLevel();
        arguments[2] = Thread.currentThread().getName();
        arguments[3] = new Date(record.getMillis());
        arguments[4] = record.getMessage();
        arguments[5] = emptyString;//record.getSourceMethodName();
        return messageFormat.format(arguments);
    }
}
