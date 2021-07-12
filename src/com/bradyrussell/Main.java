package com.bradyrussell;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

//todo open in specified resolution aspect ratio
//todo RLE pixels

public class Main {
    public static void main(String[] args) throws InterruptedException {
        final String pageURL;
        final String outputResolution;
        final String chromeDriver;

        if(System.getenv().containsKey("webpage_oc_streamer_page_url") && System.getenv().containsKey("webpage_oc_streamer_output_resolution") && System.getenv().containsKey("webpage_oc_streamer_chrome_driver")) {
            pageURL = System.getenv("webpage_oc_streamer_page_url");
            outputResolution = System.getenv("webpage_oc_streamer_output_resolution");
            chromeDriver = System.getenv("webpage_oc_streamer_chrome_driver");
        } else {
            Path ConfigPath = Path.of("webpage_oc_streamer_config.cfg");
            try {
                List<String> config = Files.readAllLines(ConfigPath);
                pageURL = config.get(0);
                outputResolution = config.get(1);
                chromeDriver = config.get(2);
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Creating default config file. Please fill it out. Alternatively, set the environment variables webpage_oc_streamer_page_url, webpage_oc_streamer_output_resolution, and webpage_oc_streamer_chrome_driver.");
                try {
                    Files.writeString(ConfigPath, "https://google.com\n160x50\nchrome_driver_path");
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
                System.exit(1);
                return;
            }
        }

        int outputX, outputY;

        try{
            String[] split = outputResolution.split("x");
            outputX = Integer.parseInt(split[0]);
            outputY = Integer.parseInt(split[1]);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            System.out.println("The specified resolution is not valid! ");
            e.printStackTrace();
            return;
        }

        System.setProperty("webdriver.chrome.driver", chromeDriver);
        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.setHeadless(true);
        chromeOptions.addArguments("--verbose", "--no-sandbox","--disable-extensions"," --disable-gpu", "--disable-infobars", "--start-maximized","--single-process", "--disable-dev-shm-usage");
        WebDriver driver = new ChromeDriver(chromeOptions);
        driver.get(pageURL);

        WebDriverWait wait = new WebDriverWait(driver, 10);

        try {
            while(true) {
                ServerSocket serverSocket = null;
                try{
                    Thread.sleep(100);
                    serverSocket = new ServerSocket(54321, 1);

                    System.out.println("Waiting for connection!");
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Connection received! Begin streaming...");
                    if(clientSocket.isConnected()) {
                        DataOutputStream dataOutputStream = new DataOutputStream(clientSocket.getOutputStream());
                        InputStream inputStream = clientSocket.getInputStream();

                        while(clientSocket.isConnected()) {
                            byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
                            BufferedImage image = ImageIO.read(new ByteArrayInputStream(screenshot));

                            //scaling
                            BufferedImage resized = new BufferedImage(outputX, outputY, BufferedImage.TYPE_3BYTE_BGR);
                            Graphics2D graphics = resized.createGraphics();
                            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                            graphics.drawImage(image, 0, 0, outputX, outputY, 0, 0, image.getWidth(), image.getHeight(), null);
                            graphics.dispose();
                            //end scaling

                            byte[] data = ((DataBufferByte) resized.getRaster().getDataBuffer()).getData();
                            dataOutputStream.writeInt(data.length);
                            dataOutputStream.write(data);

                            while (inputStream.available() <= 0) {
                                Thread.sleep(100);
                                if(inputStream.read() == 0xF1) break; // ready for new frame
                            }
                        }
                    }
                    System.out.println("Connection lost! Ending stream.");
                } catch (SocketException e){
                    if(serverSocket != null) serverSocket.close();
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            driver.quit();
            System.exit(0); // process wants to hang for some reason, selenium probably
        }
    }
}
