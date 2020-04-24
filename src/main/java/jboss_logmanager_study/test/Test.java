package jboss_logmanager_study.test;

import org.jboss.logmanager.Level;
import org.jboss.logmanager.Logger;

/**
 * author yg
 * description
 * date 2020/4/24
 */
public class Test {
    Logger logger = Logger.getLogger(Test.class.getName());

    public void log() {
        logger.log(Level.ERROR, "ATrace MEssage");
        logger.log(Level.FATAL, "ETrace MEssage");
        logger.log(Level.ERROR, "ATrace MEssage");
        logger.log(Level.ERROR, "BTrace MEssage");
    }
}
