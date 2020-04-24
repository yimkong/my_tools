package jboss_logmanager_study;

import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.handlers.FileHandler;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.ErrorManager;
import java.util.logging.Formatter;

/**
 * author yg
 * description
 * date 2020/4/24
 */
public class MyHandler extends FileHandler {
    Map<String, Writer> map = new HashMap<>();

    @Override
    protected void doPublish(ExtLogRecord record) {
        this.close();
        String message = record.getMessage();
        if (message.startsWith("A") || message.startsWith("B") || message.startsWith("C")) {
            final String formatted;
            final Formatter formatter = getFormatter();
            try {
                formatted = formatter.format(record);
            } catch (Exception ex) {
                reportError("Formatting error", ex, ErrorManager.FORMAT_FAILURE);
                return;
            }
            if (formatted.length() == 0) {
                // nothing to write; don't bother
                return;
            }
            try {
                synchronized (outputLock) {
                    final Writer writer = getWriter(record);
                    if (writer == null) {
                        return;
                    }
                    writer.write(formatted);
                    // only flush if something was written
                    if (isAutoFlush()) {
                        writer.flush();
                    }
                }
            } catch (Exception ex) {
                reportError("Error writing log message", ex, ErrorManager.WRITE_FAILURE);
            }
        }
    }

    protected Writer getWriter(ExtLogRecord record) throws IOException {
        String message = record.getMessage();
        String key = message.substring(0, 1);
        Writer writer = map.get(key);
        if (writer == null) {
            boolean ok = false;
            File file = new File(key + ".log");
            if (!file.exists()) {
                boolean newFile = file.createNewFile();
                if (!newFile) {
                    //TODO
                    return null;
                }
            }
            final FileOutputStream fos = new FileOutputStream(file, true);
            try {
                final OutputStream bos = new BufferedOutputStream(fos);
                writer = new OutputStreamWriter(bos);
                map.put(key, writer);
                ok = true;
            } finally {
                if (!ok) {
                    safeClose(fos);
                    map.remove(key);
                }
            }
        }
        return writer;
    }

}
