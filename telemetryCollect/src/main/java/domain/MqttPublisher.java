package domain;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static bootstrap.Driver.projectProperties;


public class MqttPublisher {

    private IMqttClient mqttClient;
    private String message;
    File unsentDataFile = new File(projectProperties.getProperty("unsentDataLocation"));
    static Logger logger = LoggerFactory.getLogger(MqttPublisher.class);

    public MqttPublisher() {
        setUpConnection();
    }

    private void setUpConnection() {
        try {
            mqttClient = new MqttClient(projectProperties.getProperty("destination.address"), "pegPi" + ThreadLocalRandom.current().nextLong(), new MemoryPersistence());
            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(false);
            options.setMaxInflight(3);
            options.setKeepAliveInterval(300);
            options.setUserName(projectProperties.getProperty("senderUserName"));
            options.setPassword(projectProperties.getProperty("senderPassword").toCharArray());
            mqttClient.connect(options);
        } catch (MqttException e) {
            e.printStackTrace();
//            addUnsentData(humidity,temperature,unsentDataFile);
            logger.error("Connection setup Failed" + e);
        }
    }

    public void sendMessage(Date currDate, Double humidity, Double temperature) throws IOException {
        if (unsentDataFile.exists()) {
//            System.out.println("file found");
            formMessageFromFile(currDate, humidity, temperature);
        } else {
//            System.out.println("file not found");
            message = currDate + " Humidity : " + humidity + " Temperature : " + temperature;
        }
//        System.out.println("Message DATA : " + message);
        MqttMessage mqttMessage = new MqttMessage(message.getBytes());
        try {
            mqttClient.publish(projectProperties.getProperty("mqtt.topic"), mqttMessage);
            mqttClient.disconnect();
            mqttClient.close();

            System.out.println("Sent message");
            logger.info("Sent readings");

//            clear datafile
            FileWriter fw = new FileWriter(unsentDataFile, false);
            PrintWriter pw = new PrintWriter(fw, false);
            pw.flush();
            pw.close();
            fw.close();
        } catch (MqttException e) {
            e.printStackTrace();
            logger.error("Error while sending message " + e);
            addUnsentData(currDate, humidity, temperature, unsentDataFile);
        }
    }


    public void formMessageFromFile(Date currDate, Double humidity, Double temperature) throws IOException {

        FileWriter fw = new FileWriter(unsentDataFile, true);
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write("\n" + currDate + " Humidity : " + humidity + " Temperature : " + temperature);
        bw.close();

        message = new String(Files.readAllBytes(Paths.get(String.valueOf(unsentDataFile))));
//        System.out.println("making message");

    }

    public void addUnsentData(Date currDate, Double humidity, Double temperature, File unsentDataFile) throws IOException {
        if (!unsentDataFile.exists()) {
            unsentDataFile.createNewFile();
        }

        FileWriter fw = new FileWriter(unsentDataFile, true);
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write("\n" + currDate + " Humidity : " + humidity + " Temperature : " + temperature);
        bw.close();
    }


}
