/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package coatronm1;

import java.io.*;
import java.util.*;
import gnu.io.*;
import java.text.SimpleDateFormat;

public class ReadCOM implements Runnable, SerialPortEventListener {

    private static CommPortIdentifier portId;
    private static Enumeration portList;
    private InputStream inputStream;
    private SerialPort serialPort;
    private final Thread readThread;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyy HH:mm:ss");
    private SimpleDateFormat fileNameDateFormat = new SimpleDateFormat("yyy-MM-dd");
    private static boolean validData = false;
    private Calendar calendar = Calendar.getInstance();
    private static int timeOffset = 0;

    public static void main(String[] args) {
        
        if (args.length > 0) {
            timeOffset = Integer.parseInt(args[0]);
        }
        /* tests
        System.out.println("Time offset in hours: " + timeOffset);
        Calendar calendar1 = Calendar.getInstance();
        calendar1.setTime(new Date());
        calendar1.add(Calendar.HOUR_OF_DAY, timeOffset);
        SimpleDateFormat dateFormat1 = new SimpleDateFormat("dd-MM-yyy HH:mm:ss");
        System.out.println("Current date: "+new StringBuilder(dateFormat1.format(new Date()))+", new date: "+new StringBuilder(dateFormat1.format(calendar1.getTime())));
        */
        portList = CommPortIdentifier.getPortIdentifiers();

        while (portList.hasMoreElements()) {
            portId = (CommPortIdentifier) portList.nextElement();
            if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
                if (portId.getName().equals("COM1")) {
                    ReadCOM reader = new ReadCOM();
                }
            }
        }
    }

    public ReadCOM() {
        try {
            serialPort = (SerialPort) portId.open("CoatronM1", 2000);
        } catch (PortInUseException e) {
            System.out.println(e);
        }
        try {
            inputStream = serialPort.getInputStream();
        } catch (IOException e) {
            System.out.println(e);
        }
        try {
            serialPort.addEventListener(this);
        } catch (TooManyListenersException e) {
            System.out.println(e);
        }
        serialPort.notifyOnDataAvailable(true);
        try {
            serialPort.setSerialPortParams(2400,
                    SerialPort.DATABITS_8,
                    SerialPort.STOPBITS_1,
                    SerialPort.PARITY_NONE);
        } catch (UnsupportedCommOperationException e) {
            System.out.println(e);
        }
        readThread = new Thread(this);
        readThread.start();
        System.out.println("Waiting for data ...");
    }

    @Override
    public void run() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            System.out.println(e);
        }
    }

    @Override
    public void serialEvent(SerialPortEvent event) {
        switch (event.getEventType()) {
            case SerialPortEvent.BI:
            case SerialPortEvent.OE:
            case SerialPortEvent.FE:
            case SerialPortEvent.PE:
            case SerialPortEvent.CD:
            case SerialPortEvent.CTS:
            case SerialPortEvent.DSR:
            case SerialPortEvent.RI:
            case SerialPortEvent.OUTPUT_BUFFER_EMPTY:
                break;
            case SerialPortEvent.DATA_AVAILABLE:
                StringBuilder readBuffer = new StringBuilder(); //new StringBuilder(dateFormat.format(new Date()));
                StringBuilder brutData = new StringBuilder();
                StringBuilder dateNow, today;
                int c;

                try {
                    FileWriter outBrut = new FileWriter("D:\\CoatronM1\\CoatronM1.brut", true);

                    while ((c = inputStream.read()) != -1) {
                        if (c == 10) { /* LF */
                            //System.out.println();
                            if ((readBuffer != null) && (!readBuffer.toString().isEmpty())) {

                                calendar.setTime(new Date());
                                today = new StringBuilder(fileNameDateFormat.format(calendar.getTime()));
                                String dailyFileName = "D:\\CoatronM1\\results\\CoatronM1-" + today.toString() + ".txt";
                                calendar.add(Calendar.HOUR_OF_DAY, timeOffset);
                                dateNow = new StringBuilder(dateFormat.format(calendar.getTime()));
                                String tmpBuffer = dateNow + " " + readBuffer.toString().replaceAll("\\s+", " ");
                                System.out.println(tmpBuffer);

                                FileWriter outFormatted = new FileWriter("D:\\CoatronM1\\CoatronM1.data", true);
                                outFormatted.write(tmpBuffer);
                                outFormatted.write("\n\r");
                                
                                //outFormatted.flush();
                                outFormatted.close();
                    
                                //System.out.println("Attempting to write to <" + dailyFileName + "> ...");
                                File checkExists = new File(dailyFileName);

                                //if the file doesnt exists, then create it
                                if(!checkExists.exists()){
                                    //System.out.println("The file <" +dailyFileName+ "> doesn't exists yet, it will be created now.");
                                    checkExists.createNewFile();
                                }
                                //System.out.println("Open <" + dailyFileName + "> ...");
                                FileWriter dailyFile = new FileWriter(dailyFileName, true);
                                
                                dailyFile.write(tmpBuffer);
                                dailyFile.write("\n");
                                
                                //System.out.println("Close <" + dailyFileName + "> ...");
                                //dailyFile.flush();
                                dailyFile.close();
                    
                                readBuffer.setLength(0);
                            }
                            if ((brutData != null) && (!brutData.toString().isEmpty())) {

                                outBrut.write(brutData.toString());
                                outBrut.write("\n\r");
                                
                                brutData.setLength(0);
                            }

                            validData = false;

                        } else {
                            if (c == 90) /* Z */ {
                                validData = true;
                            }
                            if (validData) {
                                readBuffer.append((char) c);
                            }
                            brutData.append((char) c);
                            //System.out.print((char) c);
                        }
                    }
                    validData = false;
                    outBrut.close();

                    System.out.println("Waiting for data ...");

                } catch (IOException e) {
                    System.out.println(e);
                }
                break;
        }
    }
}