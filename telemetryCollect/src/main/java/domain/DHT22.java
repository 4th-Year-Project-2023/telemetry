package domain;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;
import java.util.concurrent.*;

import com.pi4j.io.gpio.Pin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DHT22 implements Runnable{

    private int pinNumber;

    private byte[] data = null;

    private Double humidity = null;

    private Double temperature = null;

    private java.util.Date currDate;

    static Logger logger = LoggerFactory.getLogger(DHT22.class);

    public DHT22(Pin pin) {

        pinNumber = pin.getAddress();
    }

    private void getData() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
//        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        
        ReadSensorFuture readSensor = new ReadSensorFuture(pinNumber);
        Future<byte[]> future = executor.submit(readSensor);
//        Future<byte[]> future = scheduler.schedule(readSensor,15,TimeUnit.SECONDS);
        // Reset data
        data = new byte[5];
        try {
            data = future.get(3, TimeUnit.SECONDS);
            readSensor.close();
        } catch (TimeoutException e) {
            logger.error("TimeoutException : " + e );
            readSensor.close();
            future.cancel(true);
            executor.shutdown();
//            scheduler.shutdown();
            throw e;

        }
        readSensor.close();
        executor.shutdown();
    }


     public void read() throws Exception {

        getData();
        logger.info("byte data" + data);
        humidity = getReadingValueFromBytes(data[0], data[1]);
        temperature = getReadingValueFromBytes(data[2], data[3]);
     }

    public void start(){
        boolean gotValue = false;
        while(!gotValue){
            try {
                System.out.println();
                read();
                currDate = new java.util.Date();
                System.out.println(getCurrDate() + " Humidity=" + getHumidity() +
                        "%, Temperature=" + getTemperature() + "*C");
                logger.info(getCurrDate() + " Humidity=" + getHumidity() +
                        "%, Temperature=" + getTemperature() + "*C");
                gotValue = true;
            } catch (TimeoutException e) {
                logger.error("ERROR: " + e);
            } catch (Exception e) {
                logger.error("ERROR: " + e);
            }
        }
        System.out.println("Ending collection process");
        logger.info("Ending collection process");

        MqttPublisher mqttPublisher = new MqttPublisher();
        try {
            mqttPublisher.sendMessage(getCurrDate(),getHumidity(),getTemperature());
        } catch (IOException e) {
            e.printStackTrace();
            logger.error("Exception while publishing " + e);
        }

    }

    private double getReadingValueFromBytes(final byte hi, final byte low) {
        ByteBuffer bb = ByteBuffer.allocate(2);
        bb.order(ByteOrder.BIG_ENDIAN);
        bb.put(hi);
        bb.put(low);
        short shortVal = bb.getShort(0);
        return new Double(shortVal) / 10;
    }

    public Double getHumidity() {
        return humidity;
    }

    public Double getTemperature() {
        return temperature;
    }

    public Date getCurrDate() {
        return currDate;
    }

    @Override
    public void run() {
//        humidity = null;
//        temperature = null;
//        data = null;
        start();
    }
}