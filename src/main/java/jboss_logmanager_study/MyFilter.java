package jboss_logmanager_study;

import java.util.logging.Filter;
import java.util.logging.LogRecord;

/**
 * author yg
 * description
 * date 2020/4/24
 */
public class MyFilter implements Filter {
    @Override
    public boolean isLoggable(LogRecord record) {
        String message = record.getMessage();
        return message.startsWith("A") || message.startsWith("B") || message.startsWith("C");
    }
}
