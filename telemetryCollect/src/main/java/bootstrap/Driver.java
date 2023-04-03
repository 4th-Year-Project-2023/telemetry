package bootstrap;

import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.wiringpi.Gpio;
import domain.DHT22;
import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.EnhancedPatternLayout;
import org.apache.log4j.Level;
import org.apache.log4j.Priority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Driver {

    public static Properties projectProperties;
    public static Logger logger = LoggerFactory.getLogger(Driver.class);

    public static void main(String[] args) throws IOException {
        configureLogging("logFile", "INFO");
        projectProperties = readPropertiesFile("project.properties");

        System.out.println("Starting process");
        logger.info("Starting Process");
        if (Gpio.wiringPiSetup() == -1) {
            logger.error("GPIO wiringPiSetup Failed!");
            return;
        }

        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
        DHT22 dht22 = new DHT22(RaspiPin.GPIO_07);
        scheduledExecutorService.scheduleWithFixedDelay(dht22,1,10000, TimeUnit.MILLISECONDS);
    }

    public static String configureLogging(String logFile, String logLevel) {
        DailyRollingFileAppender dailyRollingFileAppender = new DailyRollingFileAppender();

        String logFilename = logFile + "/telemetry.log";
        switch (logLevel) {
            case "DEBUG": {
                dailyRollingFileAppender.setThreshold(Level.toLevel(Priority.DEBUG_INT));
            }
            case "WARN": {
                dailyRollingFileAppender.setThreshold(Level.toLevel(Priority.WARN_INT));
            }
            case "ERROR": {
                dailyRollingFileAppender.setThreshold(Level.toLevel(Priority.ERROR_INT));
            }
            default: {
                dailyRollingFileAppender.setThreshold(Level.toLevel(Priority.INFO_INT));
            }
            break;
        }

        System.out.println("Log files written out at " + logFilename);
        dailyRollingFileAppender.setFile(logFilename);
        dailyRollingFileAppender.setLayout(new EnhancedPatternLayout("%d [%t] %-5p %c - %m%n"));

        dailyRollingFileAppender.activateOptions();
        org.apache.log4j.Logger.getRootLogger().addAppender(dailyRollingFileAppender);
        return dailyRollingFileAppender.getFile();
    }

    public static Properties readPropertiesFile(String fileName) throws IOException {
        FileInputStream fis = null;
        Properties prop = null;
        try {
            fis = new FileInputStream(fileName);
            prop = new Properties();
            prop.load(fis);
        } catch(FileNotFoundException e) {
            e.printStackTrace();
        } catch(IOException e) {
            e.printStackTrace();
        } finally {
            fis.close();
        }
        return prop;
    }
}
