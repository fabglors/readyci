package com.squarepolka.readyci.tasks.app.android;

import com.squarepolka.readyci.taskrunner.BuildEnvironment;
import com.squarepolka.readyci.tasks.Task;
import com.squarepolka.readyci.tasks.TaskExecuteException;
import com.squarepolka.readyci.util.PropertyMissingException;
import org.springframework.stereotype.Component;

@Component
public class AndroidCreateApkFile extends Task {

    public static final String TASK_CREATE_APK_FILE = "android_create_apk_file";
    public static final String BUILD_PROP_CONFIGURATION = "configuration";


    @Override
    public String taskIdentifier() {
        return TASK_CREATE_APK_FILE;
    }

    @Override
    public void performTask(BuildEnvironment buildEnvironment) throws Exception {
        String configuration = buildEnvironment.getProperty(BUILD_PROP_CONFIGURATION);
        String arg = String.format("assemble%s", configuration);
        executeCommand(new String[] {
                "./gradlew",
                arg
        }, buildEnvironment.projectPath);
    }
}
