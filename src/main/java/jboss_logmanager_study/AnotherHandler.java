package jboss_logmanager_study;

import lombok.SneakyThrows;
import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.handlers.FileHandler;

import java.io.File;

/**
 * author yg
 * description 显而易见 这种方法更优雅
 * date 2020/4/24
 */
public class AnotherHandler extends FileHandler {
    @SneakyThrows
    @Override
    protected void preWrite(ExtLogRecord record) {
        String message = record.getMessage();
        String name = message.substring(0, 1) + ".log";
        if (!name.equals(getFile().getName())) {
            close();
            File file = new File(name);
            if (!file.exists()) {
                boolean newFile = file.createNewFile();
                if (!newFile) {
                    //TODO
                    throw new Exception("error");
                }
            }
            setFile(file);
        }
        super.preWrite(record);
    }

    @Override
    protected void doPublish(ExtLogRecord record) {
        String message = record.getMessage();
        if (message.startsWith("A") || message.startsWith("B") || message.startsWith("C")) {
            super.doPublish(record);
        }
    }
}
