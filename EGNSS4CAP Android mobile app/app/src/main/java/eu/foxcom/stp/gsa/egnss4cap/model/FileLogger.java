package eu.foxcom.stp.gsa.egnss4cap.model;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.NonNull;

import org.joda.time.DateTime;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class FileLogger {

    public static final int EXIT_STATUS = 11;

    private Context context;
    private String TAG;

    public FileLogger(Context context, Class c) {
        this.context = context;
        this.TAG = c.getSimpleName();
        initExceptionHandler();
    }

    public void logException(Throwable throwable) {
        writeExceptionToFile(throwable, Thread.currentThread(), false);
    }

    private void writeUnhandledExceptionToFile(Throwable unhandledException, Thread thread) {
        writeExceptionToFile(unhandledException, thread, true);
    }

    private void writeExceptionToFile(Throwable exception, Thread thread, boolean unhandled) {
        String dirName = "logs";
        File expDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS + "/" + dirName);
        if (!expDir.exists()) {
            expDir.mkdir();
        }
        String fileName;
        if (unhandled) {
            fileName = "unhandledExceptions.log";
        } else {
            fileName = "exceptions.log";
        }
        File expFile = new File(expDir.getAbsolutePath() + "/" + fileName);
        try {
            expFile.createNewFile();
        } catch (IOException e) {
            Log.e(TAG, "An unexpected error occurred while creating the " + fileName + " file.", e);
            return;
        }
        FileWriter fileWriter;
        try {
            fileWriter = new FileWriter(expFile, true);
        } catch (IOException e) {
            Log.e(TAG, "An unexpected error occurred while opening the " + fileName + " file for writing.", e);
            return;
        }
        try {
            fileWriter.write(DateTime.now().toString() + "\n");
            fileWriter.write("Message: " + exception.getMessage() + "\n");
            fileWriter.write("Thread id: " + thread.getId() + "\nThread name: " + thread.getName() + "\n");
            fileWriter.write("StackTrace: \n");
            fileWriter.flush();
            PrintWriter printWriter = new PrintWriter(fileWriter);
            exception.printStackTrace(printWriter);
            printWriter.flush();
            fileWriter.write("\n\n");
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            Log.e(TAG, "An unexpected error occurred while writing to the " + fileName + " file.", e);
            return;
        }
    }

    private void initExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
                writeUnhandledExceptionToFile(e, t);
                // snaha o vynucený pád aplikace (prevence zamrznutí)
                System.exit(EXIT_STATUS);
            }
        });
    }
}

/**
 * Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
 */
