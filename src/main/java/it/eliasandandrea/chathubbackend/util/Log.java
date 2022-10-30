package it.eliasandandrea.chathubbackend.util;

public class Log {

    private final static System.Logger logger;

    static {
        logger = System.getLogger("ChatHub-Backend");
    }

    public static void info(String what) {
        logger.log(System.Logger.Level.INFO, what);
    }

    public static void warning(String what, Exception e) {
        logger.log(System.Logger.Level.WARNING, what, e);
    }

    public static void warning(String what) {
        logger.log(System.Logger.Level.WARNING, what);
    }

    public static void error(String what, Exception e) {
        logger.log(System.Logger.Level.ERROR, what, e);
    }

    public static void error(String what) {
        logger.log(System.Logger.Level.ERROR, what);
    }

}
