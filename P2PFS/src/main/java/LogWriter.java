import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LogWriter {

    private Timestamp timeStamp;
    private FileWriter logWriter;

    private static final SimpleDateFormat simpleDF = new SimpleDateFormat("yyy-MM-dd HH:mm:ss");
    private static final long logFileLimit = 50000000;

    public LogWriter(String logFileName) throws IOException {
        File logFile = new File("src/main/logs/" + logFileName);

        this.timeStamp = new Timestamp(System.currentTimeMillis());
        this.logWriter = new FileWriter(logFile, true);
        // delete current log if greater than 50 MB
        delete(logFile);
    }

    public void log(String message) throws IOException {
        timeStamp = new Timestamp(System.currentTimeMillis());
        logWriter.append(simpleDF.format(timeStamp)  + " : " + message);
        logWriter.flush();
    }

    public void lnlog(String message) throws IOException {
        timeStamp = new Timestamp(System.currentTimeMillis());
        logWriter.append("\n" + simpleDF.format(timeStamp)  + " : " + message);
        logWriter.flush();
    }

    private void delete(File logFile) throws IOException {
        // if file size is larger than 50 MB than log will be deleted
        if (logFile.length() > logFileLimit) {
            System.out.println("Log file limit of " + logFileLimit/1000000.0f + " reached, deleting all logged data");
            logWriter = new FileWriter(logFile);
            logWriter.write("");
        }
    }
}
