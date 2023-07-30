package io.github.d3it7i.callrecording.xposed;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Objects;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class Init implements IXposedHookLoadPackage {
    private static final String TAG = "CallRecording";
    private static final String DIALER_ANDROID_PACKAGE_NAME = "com.google.android.dialer";
    private static final String DIALER_CALLRECORDINGPROMPT = "/data/data/"+ DIALER_ANDROID_PACKAGE_NAME + "/files/callrecordingprompt";
    private static final String START_DIALER_CALLRECORDINGPROMPT_FILE = DIALER_CALLRECORDINGPROMPT+"/starting_custom.wav";
    private static final String STOPP_DIALER_CALLRECORDINGPROMPT_FILE = DIALER_CALLRECORDINGPROMPT+"/ending_custom.wav";
    private static byte[] startwav = null;
    private static byte[] endingwav = null;

    @SuppressWarnings({"SoonBlockedPrivateApi", "JavaReflectionMemberAccess"})
    private static void hookDispatchOnInit() {
        try {
            Method dispatchOnInit = TextToSpeech.class.getDeclaredMethod("dispatchOnInit", int.class);
            XposedBridge.hookMethod(dispatchOnInit, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    Log.d(TAG, "dispatchOnInit: " + Arrays.toString(param.args));
                    if (!Objects.equals(param.args[0], TextToSpeech.SUCCESS)) {
                        param.args[0] = TextToSpeech.SUCCESS;
                        Log.w(TAG, "TTS failed, ignore");
                    }
                }
            });
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "dispatchOnInit method not found", e);
        }
    }

    private static void hookSynthesizeToFile() {
        try {
            Method synthesizeToFile = TextToSpeech.class.getDeclaredMethod("synthesizeToFile",
                    CharSequence.class, Bundle.class, File.class, String.class);
            XposedBridge.hookMethod(synthesizeToFile, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    Log.d(TAG, "synthesizeToFile: " + Arrays.toString(param.args));
                    // Slient TTS word
                    param.args[0] = "";
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (startwav == null || endingwav == null ) {
                        Log.d(TAG,  START_DIALER_CALLRECORDINGPROMPT_FILE+" and "+STOPP_DIALER_CALLRECORDINGPROMPT_FILE+" not exist");
                        return;
                    }

                    var file = (File) param.args[2];
                    try (var out = new FileOutputStream(file)) {
                        // start voice record announcement
                        Log.d(TAG, "Hook ing TTS: " + param.args[0]);
                        if (((String) param.args[3]).contains("starting")) {
                            out.write(startwav);
                        } else if (((String) param.args[3]).contains("ending")) { // or is stopped
                            out.write(endingwav);
                        } else {
                            return;
                        }
                        param.setResult(TextToSpeech.SUCCESS);
                    } catch (IOException e) {
                        Log.e(TAG, "synthesizeToFile: cannot write " + file, e);
                    }
                    try {
                        var field = param.thisObject.getClass().getDeclaredField("mUtteranceProgressListener");
                        field.setAccessible(true);
                        var listener = (UtteranceProgressListener) field.get(param.thisObject);
                        if (listener == null) return;
                        var onDone = UtteranceProgressListener.class.getDeclaredMethod("onDone", String.class);
                        onDone.invoke(listener, (String) param.args[3]);
                    } catch (ReflectiveOperationException e) {
                        Log.e(TAG, "synthesizeToFile: cannot invoke onDone", e);
                    }
                }
            });
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "synthesizeToFile method not found", e);
        }
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!DIALER_ANDROID_PACKAGE_NAME.equals(lpparam.packageName)) return;
        try {
            File file = new File(START_DIALER_CALLRECORDINGPROMPT_FILE);
            startwav = Files.readAllBytes(file.toPath());
            file = new File(STOPP_DIALER_CALLRECORDINGPROMPT_FILE);
            endingwav = Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            Log.e(TAG, "read wav: IOException", e);
        }
        new Thread(() -> {
            hookSynthesizeToFile();
            hookDispatchOnInit();
        }).start();
        Log.d(TAG, "handleLoadPackage done");
    }
}
