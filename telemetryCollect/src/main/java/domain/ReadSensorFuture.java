package domain;

import com.pi4j.wiringpi.Gpio;
import java.io.Closeable;
import java.util.concurrent.Callable;

public class ReadSensorFuture implements Callable<byte[]>, Closeable {

    private final int pinNumber;
    private static final int LONGEST_ZERO = 50000;
    private boolean keepRunning = true;

    public ReadSensorFuture(int pinNumber) {
        this.pinNumber = pinNumber;
        Gpio.pinMode(pinNumber, Gpio.OUTPUT);
        Gpio.digitalWrite(pinNumber, Gpio.HIGH);
    }

    @Override
    public byte[] call() {

        byte[] data = new byte[5];
        long startTime = System.nanoTime();

        sendStartSignal();
        waitForResponseSignal();
        for (int i = 0; i < 40; i++) {
            while (keepRunning && Gpio.digitalRead(pinNumber) == Gpio.LOW) {
            }
            startTime = System.nanoTime();
            while (keepRunning && Gpio.digitalRead(pinNumber) == Gpio.HIGH) {
            }
            long timeHight = System.nanoTime() - startTime;
            data[i / 8] <<= 1;
            if ( timeHight > LONGEST_ZERO) {
                data[i / 8] |= 1;
            }
        }
        return data;
    }

    private void sendStartSignal() {
        Gpio.pinMode(pinNumber, Gpio.OUTPUT);
        Gpio.digitalWrite(pinNumber, Gpio.LOW);
        Gpio.delay(1);
        Gpio.digitalWrite(pinNumber, Gpio.HIGH);
    }

    private void waitForResponseSignal() {
        Gpio.pinMode(pinNumber, Gpio.INPUT);
        while (keepRunning && Gpio.digitalRead(pinNumber) == Gpio.HIGH) {
        }
        while (keepRunning && Gpio.digitalRead(pinNumber) == Gpio.LOW) {
        }
        while (keepRunning && Gpio.digitalRead(pinNumber) == Gpio.HIGH) {
        }
    }

    @Override
    public void close() {
        keepRunning = false;

        Gpio.pinMode(pinNumber, Gpio.OUTPUT);
        Gpio.digitalWrite(pinNumber, Gpio.HIGH);
    }
}
