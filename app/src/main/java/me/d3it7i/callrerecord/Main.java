package me.d3it7i.callrerecord;

import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.Arrays;

public class Main implements IXposedHookLoadPackage {
    public static final String TAG = "CallReRecord";
    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpParam) throws Throwable {
        if (!lpParam.packageName.equals(Constant.DIALER_ANDROID_PACKAGE_NAME)) {
            return;
        }
        Log.d(TAG, "Loaded app: " + lpParam.packageName);

        Method synthesizeToFile = TextToSpeech.class.getDeclaredMethod("synthesizeToFile",
                CharSequence.class, Bundle.class, File.class, String.class);
        XposedBridge.hookMethod(synthesizeToFile, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                Log.d(TAG, "synthesizeToFile: " + Arrays.toString(param.args));

                param.args[0]="";
                Object ret = XposedBridge.invokeOriginalMethod(param.method,
                        param.thisObject, param.args);

                // ready replace data
                File file = (File) param.args[2];
                byte[] overWriteDataBuf = null;
                try {
                    if (file.getName().contains("call_recording_starting")) {
                        Log.i(TAG, "Prepare to override with "+Constant.START_DIALER_CALLRECORDPROMPT_FILE);
                        overWriteDataBuf = readData(Constant.START_DIALER_CALLRECORDPROMPT_FILE);
                    } else if (file.getName().contains("call_recording_ending")) {
                        Log.i(TAG, "Prepare to override to "+Constant.STOPP_DIALER_CALLRECORDPROMPT_FILE);
                        overWriteDataBuf = readData(Constant.STOPP_DIALER_CALLRECORDPROMPT_FILE);
                    }

                    if (overWriteDataBuf != null) {
                        try (FileOutputStream out = new FileOutputStream(file)) {
                            out.write(overWriteDataBuf);
                            out.flush();
                        }
                    }
                } catch (IOException e) {
                    Log.w(TAG, "not found custom wav file or miss permission, fallback to silent", e);
                }
                return ret;
            }
        });
    }
    private byte[] readData(String filename) throws IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return Files.readAllBytes(new File(filename).toPath());
        }
        throw new UnsupportedOperationException("not support Android Oreo below");
    }
}
