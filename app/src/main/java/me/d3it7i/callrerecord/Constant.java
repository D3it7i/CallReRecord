package me.d3it7i.callrerecord;

public final class Constant {
    public static final String DIALER_ANDROID_PACKAGE_NAME = "com.google.android.dialer";
    public static final String DIALER_CALLRECORDPROMPT = "/data/data/"+ DIALER_ANDROID_PACKAGE_NAME + "/files/callrecordingprompt";
    public static final String START_DIALER_CALLRECORDPROMPT_FILE = DIALER_CALLRECORDPROMPT+"/starting_custom.wav";
    public static final String STOPP_DIALER_CALLRECORDPROMPT_FILE = DIALER_CALLRECORDPROMPT+"/ending_custom.wav";
}
