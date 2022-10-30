package it.eliasandandrea.chathubbackend.configUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.Properties;

public class Configuration {

    public static Properties properties;

    public static void init(){
        try {
            File configFileDir = getConfigurationFile();
            File configFile = new File(configFileDir, "config.properties");
            System.out.println("Configuration file: " + configFile.getAbsolutePath());
            if (!configFile.exists()){
                OutputStream outputStream = new FileOutputStream(configFile);
                properties = getDefaultProperties();
                properties.store(outputStream, "ChatHub Server Configuration File");
                outputStream.close();
            }else{
                properties = new Properties();
                properties.load(new java.io.FileInputStream(configFile));
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Properties getDefaultProperties(){
        Properties properties = new Properties();
        properties.setProperty("name", "ChatHub Server");
        properties.setProperty("port", "5476");
        properties.setProperty("keystorePassword", "password");
        return properties;
    }

    private static File getConfigurationFile() throws URISyntaxException {
        return new File(Configuration.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    }
}
