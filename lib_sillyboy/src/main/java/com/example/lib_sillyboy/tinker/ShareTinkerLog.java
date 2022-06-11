package com.example.lib_sillyboy.tinker;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.lang.reflect.Constructor;

public class ShareTinkerLog {
    private static final String TAG = "Tinker.ShareTinkerLog";

    public static final int FN_LOG_PRINT_STACKTRACE = 0xFA1;
    public static final int FN_LOG_PRINT_PENDING_LOGS = 0xFA2;

    private static final Handler[] tinkerLogInlineFenceRef = {null};

    private static final TinkerLogImp debugLog = new TinkerLogImp() {

        @Override
        public void v(final String tag, final String format, final Object... params) {
            String log = (params == null || params.length == 0) ? format : String.format(format, params);
            Log.v(tag, log);
        }

        @Override
        public void i(final String tag, final String format, final Object... params) {
            String log = (params == null || params.length == 0) ? format : String.format(format, params);
            Log.i(tag, log);

        }

        @Override
        public void d(final String tag, final String format, final Object... params) {
            String log = (params == null || params.length == 0) ? format : String.format(format, params);
            Log.d(tag, log);
        }

        @Override
        public void w(final String tag, final String format, final Object... params) {
            String log = (params == null || params.length == 0) ? format : String.format(format, params);
            Log.w(tag, log);
        }

        @Override
        public void e(final String tag, final String format, final Object... params) {
            String log = (params == null || params.length == 0) ? format : String.format(format, params);
            Log.e(tag, log);
        }

        @Override
        public void printErrStackTrace(String tag, Throwable tr, String format, Object... params) {
            String log = (params == null || params.length == 0) ? format : String.format(format, params);
            if (log == null) {
                log = "";
            }
            log += "  " + Log.getStackTraceString(tr);
            Log.e(tag, log);
        }
    };

    private static final TinkerLogImp[] tinkerLogImpRef = {debugLog};

    static {
        synchronized (tinkerLogInlineFenceRef) {
            try {
                final Class<?> clazz = Class.forName("com.tencent.tinker.lib.util.TinkerLogInlineFence");
                final Constructor<?> ctor = clazz.getDeclaredConstructor();
                ctor.setAccessible(true);
                tinkerLogInlineFenceRef[0] = (Handler) ctor.newInstance();
            } catch (Throwable thr) {
                Log.e(TAG, "[-] Fail to create inline fence instance.", thr);
                tinkerLogInlineFenceRef[0] = null;
            }
        }
    }

    private static Handler getInlineFence() {
        synchronized (tinkerLogInlineFenceRef) {
            return tinkerLogInlineFenceRef[0];
        }
    }

    public static TinkerLogImp getDefaultImpl() {
        return debugLog;
    }

    public static void setTinkerLogImp(TinkerLogImp imp) {
        synchronized (tinkerLogImpRef) {
            tinkerLogImpRef[0] = imp;
            if (imp != null && imp != debugLog) {
                printPendingLogs();
            }
        }
    }

    public static TinkerLogImp getImpl() {
        synchronized (tinkerLogImpRef) {
            return tinkerLogImpRef[0];
        }
    }

    public static void v(final String tag, final String fmt, final Object... values) {
        printLog(Log.VERBOSE, tag, fmt, values);
    }

    public static void d(final String tag, final String fmt, final Object... values) {
        printLog(Log.DEBUG, tag, fmt, values);
    }

    public static void i(final String tag, final String fmt, final Object... values) {
        printLog(Log.INFO, tag, fmt, values);
    }

    public static void w(final String tag, final String fmt, final Object... values) {
        printLog(Log.WARN, tag, fmt, values);
    }

    public static void e(final String tag, final String fmt, final Object... values) {
        printLog(Log.ERROR, tag, fmt, values);
    }

    public static void printErrStackTrace(String tag, Throwable thr, final String format, final Object... values) {
        printLog(tag, thr, format, values);
    }

    public static void printPendingLogs() {
        final Handler inlineFence = getInlineFence();
        if (inlineFence != null) {
            final Message msg = Message.obtain(inlineFence, FN_LOG_PRINT_PENDING_LOGS);
            inlineFence.handleMessage(msg);
            msg.recycle();
        }
    }

    private static void printLog(int priority, String tag, String fmt, Object... values) {
        final long timestamp = System.currentTimeMillis();
        final Object[] args = {priority, timestamp, tag, fmt, values};
        final Handler inlineFence = getInlineFence();
        if (inlineFence != null) {
            final Message msg = Message.obtain(inlineFence, priority, args);
            inlineFence.handleMessage(msg);
            msg.recycle();
        } else {
            debugLog.e(tag, "!! NO_LOG_IMPL !! Original Log: " + fmt, values);
        }
    }

    private static void printLog(String tag, Throwable thr, String fmt, Object... values) {
        final long timestamp = System.currentTimeMillis();
        final Object[] args = {FN_LOG_PRINT_STACKTRACE, timestamp, tag, thr, fmt, values};
        final Handler inlineFence = getInlineFence();
        if (inlineFence != null) {
            final Message msg = Message.obtain(inlineFence, FN_LOG_PRINT_STACKTRACE, args);
            inlineFence.handleMessage(msg);
            msg.recycle();
        } else {
            debugLog.printErrStackTrace(tag, thr, "!! NO_LOG_IMPL !! Original Log: " + fmt, values);
        }
    }

    public interface TinkerLogImp {

        void v(final String tag, final String fmt, final Object... values);

        void d(final String tag, final String fmt, final Object... values);

        void i(final String tag, final String fmt, final Object... values);

        void w(final String tag, final String fmt, final Object... values);

        void e(final String tag, final String fmt, final Object... values);

        void printErrStackTrace(String tag, Throwable tr, final String format, final Object... values);

    }
}