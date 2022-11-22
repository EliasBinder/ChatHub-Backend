package it.eliasandandrea.chathub.backend.configUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Properties;

public class Configuration {

    public static Properties properties;

    private static HashMap<String, String> defaultValues = new HashMap<>(){{
        put("name", "ChatHub Server");
        put("port", "5476");
        put("keystorePassword", "password");
        put("mode", "rmi");
    }};

    public static void init(){
        try {
            File configFileDir = getJarPath();
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
                for (String key : defaultValues.keySet()){
                    if (!properties.containsKey(key)){
                        properties.setProperty(key, defaultValues.get(key));
                    }
                }
                OutputStream outputStream = new FileOutputStream(configFile);
                properties.store(outputStream, "ChatHub Server Configuration File");
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Properties getDefaultProperties(){
        Properties properties = new Properties();
        defaultValues.entrySet().forEach((entry) -> {
            properties.setProperty(entry.getKey(), entry.getValue());
        });
        return properties;
    }

    public static String getProp(String key){
        return (String) properties.getOrDefault(key, defaultValues.get(key));
    }

    private static File getJarPath() throws URISyntaxException {
        return new File(Configuration.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile();
    }
}