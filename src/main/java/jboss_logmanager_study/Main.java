package jboss_logmanager_study;

import jboss_logmanager_study.test.Test;
import org.jboss.logmanager.Level;
import org.jboss.logmanager.Logger;

/**
 * author yg
 * description 根据日志内容生成不同的文件名
 * date 2020/4/24
 */
public class Main {
    public static void main(String[] args) throws InterruptedException {
        System.setProperty("java.util.logging.manager","org.jboss.logmanager.LogManager");
        String string = Main.class.getClassLoader().getResource("jboss_logging.properties").toString();
        System.setProperty("logging.configuration", string);
        Logger logger = Logger.getLogger(Main.class.getName());
        logger.log(Level.TRACE, "ATrace MEssage");
        logger.log(Level.DEBUG, "ATrace MEssage");
        logger.log(Level.INFO, "BTrace MEssage");
        logger.log(Level.WARN, "CTrace MEssage");
        logger.log(Level.ERROR, "Trace MEssage");
        logger.log(Level.FATAL, "Trace MEssage");
        new Test().log();
    }
}
