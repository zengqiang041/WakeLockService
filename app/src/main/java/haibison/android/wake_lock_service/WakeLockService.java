/*
 * Copyright (c) 2015 Hai Bison
 *
 * See the file LICENSE at the root directory of this project for copying permission.
 */

package haibison.android.wake_lock_service;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static android.text.format.DateUtils.SECOND_IN_MILLIS;

/**
 * Base service which uses {@link android.os.PowerManager.WakeLock} for its entire life time.
 * <p/>
 * <strong>Notes</strong> <ul><li>Use of {@link android.os.PowerManager.WakeLock} is optional. If you don't declare
 * permission {@link android.Manifest.permission#WAKE_LOCK} in your manifest, the service still works.</li><li>We
 * recommend to put this service on a separate process.</li> <li>{@link #onStartCommand(Intent, int, int)} returns
 * {@link #START_NOT_STICKY} by default. If you don't handle incoming intents from clients, you should always pass them
 * to super method. If you want the super method return other result, override {@link
 * #getResultForOnStartCommand()}.</li> </ul>
 */
public class WakeLockService extends Service {

    /**
     * The library name.
     */
    public static final String LIB_NAME = "WakeLockService";

    /**
     * The library version name.
     */
    public static final String LIB_VERSION_NAME = "0.0.4";

    private static final String CLASSNAME = WakeLockService.class.getName();

    /**
     * Use this action to stop this service.
     */
    public static final String ACTION_STOP_SELF = CLASSNAME + ".STOP_SELF";

    /**
     * This extra holds a {@link PendingIntent} which will be called when your main action is done.
     * <p/>
     * Type: {@link PendingIntent}.
     */
    public static final String EXTRA_POST_PENDING_INTENT = CLASSNAME + ".POST_PENDING_INTENT";

    /**
     * Maximum idle time, in milliseconds.
     */
    public static final long MAX_IDLE_TIME = SECOND_IN_MILLIS * 10;

    /**
     * The service is idle.
     */
    public static final int SERVICE_STATE_IDLE = -1;

    /**
     * The service is working.
     */
    public static final int SERVICE_STATE_WORKING = -2;

    /**
     * Extended-classes' service states can be defined starting from this value (and go upward).
     */
    protected static final int SERVICE_STATE_FIRST_EXTENDER = 0;

    /**
     * Extended-classes' messages can be defined starting from this value (and go upward).
     */
    protected static final int MSG_FIRST_EXTENDER = 0;

    /**
     * Message finished.
     */
    public static final int MSG_FINISHED = -1;

    /**
     * Message cancelled.
     */
    public static final int MSG_CANCELLED = -2;

    /**
     * Message error.
     */
    public static final int MSG_ERROR = -3;

    private final RemoteCallbackList<IWakeLockServiceEventListener> mEventListeners = new
            RemoteCallbackList<IWakeLockServiceEventListener>();

    private PowerManager mPowerManager;
    private PowerManager.WakeLock mWakeLock;
    private int mServiceState = SERVICE_STATE_IDLE;

    private final BlockingQueue<Runnable> mWorkerQueue = new LinkedBlockingQueue<Runnable>();
    private ThreadPoolExecutor mThreadPoolExecutor;

    /**
     * Will be called by this service to confirm using wake lock or not. Default implementation returns {@code true}.
     *
     * @return {@code true} or {@code false}.
     */
    protected boolean shouldUseWakeLock() {
        return true;
    }//shouldUseWakeLock()

    @Override
    public void onCreate() {
        super.onCreate();

        // Get PowerManager
        mPowerManager = (PowerManager) getSystemService(POWER_SERVICE);

        // Acquire new wake lock.
        if (shouldUseWakeLock() &&
                checkPermission(Manifest.permission.WAKE_LOCK, android.os.Process.myPid(), android.os.Process.myUid())
                        == PackageManager.PERMISSION_GRANTED) {
            mWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, CLASSNAME);
            mWakeLock.acquire();
        }//if

        // Create thread pool executor
        mThreadPoolExecutor = new ThreadPoolExecutor(1, Runtime.getRuntime()
                .availableProcessors(), MAX_IDLE_TIME, TimeUnit.MILLISECONDS,
                mWorkerQueue) {

            @Override
            protected void beforeExecute(Thread t, Runnable r) {
                cancelScheduleToStopSelf();

                super.beforeExecute(t, r);
            }// beforeExecute()

            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                super.afterExecute(r, t);

                scheduleToStopSelf();
            }// afterExecute()

        };
    }//onCreate()

    @Override
    public IBinder onBind(Intent intent) {
        return mServiceBinder;
    }//onBind()

    /**
     * The service binder,
     */
    private final IWakeLockService.Stub mServiceBinder = new IWakeLockService.Stub() {

        @Override
        public void registerEventListener(IWakeLockServiceEventListener listener) throws RemoteException {
            if (listener != null) {
                mEventListeners.register(listener);
                notifyServiceStateChanged(listener);
            }// if
        }//registerEventListener()

        @Override
        public void unregisterEventListener(IWakeLockServiceEventListener listener) throws RemoteException {
            if (listener != null) mEventListeners.unregister(listener);
        }//unregisterEventListener()

        @Override
        public int getServiceState() throws RemoteException {
            return mServiceState;
        }//getServiceState()

        @Override
        public boolean getBooleanValue(int id) throws RemoteException {
            return WakeLockService.this.getBooleanValue(id);
        }//getBooleanValue()

        @Override
        public float getFloatValue(int id) throws RemoteException {
            return WakeLockService.this.getFloatValue(id);
        }//getFloatValue()

        @Override
        public double getDoubleValue(int id) throws RemoteException {
            return WakeLockService.this.getDoubleValue(id);
        }//getDoubleValue()

        @Override
        public int getIntValue(int id) throws RemoteException {
            return WakeLockService.this.getIntValue(id);
        }//getIntValue()

        @Override
        public long getLongValue(int id) throws RemoteException {
            return WakeLockService.this.getLongValue(id);
        }//getLongValue()

        @Override
        public CharSequence getCharSequenceValue(int id) throws RemoteException {
            return WakeLockService.this.getCharSequenceValue(id);
        }//getCharSequenceValue()

        @Override
        public String getStringValue(int id) throws RemoteException {
            return WakeLockService.this.getStringValue(id);
        }//getStringValue()

        @Override
        public Bundle getBundleValue(int id) throws RemoteException {
            return WakeLockService.this.getBundleValue(id);
        }//getBundleValue

    };//mServiceBinder

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            if (getServiceState() != SERVICE_STATE_WORKING) scheduleToStopSelf();
            return getResultForOnStartCommand();
        }//if

        if (ACTION_STOP_SELF.equals(intent.getAction())) {
            stopSelf();
        }//ACTION_STOP_SELF
        else if (getServiceState() != SERVICE_STATE_WORKING)
            scheduleToStopSelf();

        return getResultForOnStartCommand();
    }//onStartCommand()

    /**
     * Gets result for {@link #onStartCommand(Intent, int, int)}.
     *
     * @return the result. Default is {@link #START_NOT_STICKY}.
     */
    protected int getResultForOnStartCommand() {
        return START_NOT_STICKY;
    }//getResultForOnStartCommand()

    @Override
    public void onDestroy() {
        // Release wake lock.
        try {
            if (mWakeLock != null && mWakeLock.isHeld()) mWakeLock.release();
        } catch (Throwable t) {
            Log.e(CLASSNAME, t.getMessage(), t);
        }

        // Cancel schedule to stop self, if any.
        cancelScheduleToStopSelf();

        // Shutdown all tasks
        mThreadPoolExecutor.shutdownNow();

        super.onDestroy();
    }// onDestroy()

    /**
     * Gets worker queue.
     *
     * @return the worker queue.
     */
    protected BlockingQueue<Runnable> getWorkerQueue() {
        return mWorkerQueue;
    }//getWorkerQueue()

    /**
     * Gets the instance of {@link PowerManager}.
     *
     * @return the instance of {@link PowerManager}.
     */
    protected PowerManager getPowerManager() {
        return mPowerManager;
    }//getPowerManager()

    /**
     * Sets service state.
     *
     * @param newState new state.
     */
    protected void setServiceState(int newState) {
        if (mServiceState == newState) return;

        mServiceState = newState;
        notifyServiceStateChanged();
    }//setServiceState()

    /**
     * Gets service state.
     *
     * @return service state.
     */
    protected int getServiceState() {
        return mServiceState;
    }//getServiceState()

    /**
     * Notifies all listeners that service state has changed.
     */
    protected void notifyServiceStateChanged() {
        notifyServiceStateChanged(null);
    }//notifyServiceStateChanged()

    /**
     * Notifies listener(s) that service state has changed.
     *
     * @param listener the listener. If {@code null}, all listeners will be notified. If not {@code null}, it must be
     *                 one of existing listeners.
     */
    protected void notifyServiceStateChanged(IWakeLockServiceEventListener listener) {
        final int state = mServiceState;

        final int count = mEventListeners.beginBroadcast();
        for (int i = 0; i < count; i++) {
            try {
                IWakeLockServiceEventListener l = mEventListeners.getBroadcastItem(i);
                if (listener == null || listener == l) l.onStateChanged(state);
                if (listener == l) break;
            } catch (RemoteException e) {
                // Ignore it. RemoteCallbackList will handle this exception for us.
            }
        }// for
        mEventListeners.finishBroadcast();
    }//notifyServiceStateChanged()

    /**
     * Will be called by {@link IWakeLockService#getBooleanValue(int)}.
     *
     * @param id the ID.
     * @return the value. Default implementation returns {@code false}.
     */
    protected boolean getBooleanValue(int id) {
        return false;
    }//getBooleanValue()

    /**
     * Will be called by {@link IWakeLockService#getFloatValue(int)}.
     *
     * @param id the ID.
     * @return the value. Default implementation returns {@code 0}.
     */
    protected float getFloatValue(int id) {
        return 0;
    }//getFloatValue()

    /**
     * Will be called by {@link IWakeLockService#getDoubleValue(int)}.
     *
     * @param id the ID.
     * @return the value. Default implementation returns {@code 0}.
     */
    protected double getDoubleValue(int id) {
        return 0;
    }//getDoubleValue()

    /**
     * Will be called by {@link IWakeLockService#getIntValue(int)}.
     *
     * @param id the ID.
     * @return the value. Default implementation returns {@code 0}.
     */
    protected int getIntValue(int id) {
        return 0;
    }//getIntValue()

    /**
     * Will be called by {@link IWakeLockService#getLongValue(int)}.
     *
     * @param id the ID.
     * @return the value. Default implementation returns {@code 0}.
     */
    protected long getLongValue(int id) {
        return 0;
    }//getLongValue()

    /**
     * Will be called by {@link IWakeLockService#getCharSequenceValue(int)}.
     *
     * @param id the ID.
     * @return the value. Default implementation returns {@code null}.
     */
    protected CharSequence getCharSequenceValue(int id) {
        return null;
    }//getCharSequenceValue()

    /**
     * Will be called by {@link IWakeLockService#getStringValue(int)}.
     *
     * @param id the ID.
     * @return the value. Default implementation returns {@code null}.
     */
    protected String getStringValue(int id) {
        return null;
    }//getStringValue()

    /**
     * Will be called by {@link IWakeLockService#getBundleValue(int)}.
     *
     * @param id the ID.
     * @return the value. Default implementation returns {@code null}.
     */
    protected Bundle getBundleValue(int id) {
        return null;
    }//getBundleValue()

    /**
     * Sends simple message to all listeners.
     *
     * @param msgId message ID.
     */
    protected void sendSimpleMessage(int msgId) {
        sendSimpleMessage(msgId, null);
    }//sendSimpleMessage()

    /**
     * Sends simple message to all listeners.
     *
     * @param msgId message ID.
     * @param msg   message (optional).
     */
    protected void sendSimpleMessage(int msgId, Bundle msg) {
        sendSimpleMessage(msgId, msg, null);
    }//sendSimpleMessage()

    /**
     * Sends simple message to listener(s).
     *
     * @param msgId    message ID.
     * @param msg      message (optional).
     * @param listener the listener. If {@code null}, the message will be sent to all listeners. If not {@code null}, it
     *                 must be one of existing listeners.
     */
    protected void sendSimpleMessage(int msgId, Bundle msg, IWakeLockServiceEventListener listener) {
        final int count = mEventListeners.beginBroadcast();
        for (int i = 0; i < count; i++) {
            try {
                IWakeLockServiceEventListener l = mEventListeners.getBroadcastItem(i);
                if (listener == null || listener == l) l.onMessage(msgId, msg);
                if (listener == l) break;
            } catch (RemoteException e) {
                // Ignore it. RemoteCallbackList will handle this exception for us.
            }
        }// for
        mEventListeners.finishBroadcast();
    }//sendSimpleMessage()

    /**
     * Executes given command.
     *
     * @param command the command.
     */
    protected void executeCommand(Runnable command) {
        mThreadPoolExecutor.execute(command);
    }// executeCommand()

    /**
     * Submits given command.
     *
     * @param command the command.
     * @return the future task.
     */
    protected Future<?> submitCommand(Runnable command) {
        return mThreadPoolExecutor.submit(command);
    }//submitCommand()

    /**
     * Schedules to stop this service.
     */
    protected void scheduleToStopSelf() {
        Intent command = new Intent(ACTION_STOP_SELF, null, this, WakeLockService.this.getClass());
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, command, PendingIntent.FLAG_CANCEL_CURRENT);

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + MAX_IDLE_TIME, pendingIntent);
    }// scheduleToStopSelf()

    /**
     * Cancels schedule to stop this service.
     */
    protected void cancelScheduleToStopSelf() {
        Intent command = new Intent(ACTION_STOP_SELF, null, this, WakeLockService.this.getClass());
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, command, PendingIntent.FLAG_CANCEL_CURRENT);

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
    }// cancelScheduleToStopSelf()

    /**
     * Executes post pending intent via {@link #EXTRA_POST_PENDING_INTENT}. This method just logs {@link
     * android.app.PendingIntent.CanceledException} if it raises, and ignores it.
     *
     * @param intent incoming intent from client.
     * @return {@code true} if there was post pending intent and it was executed successfully. {@code false} otherwise.
     */
    protected boolean executePostPendingIntent(Intent intent) {
        PendingIntent pendingIntent = intent.getParcelableExtra(EXTRA_POST_PENDING_INTENT);
        if (pendingIntent == null) return false;

        try {
            pendingIntent.send();
            return true;
        } catch (PendingIntent.CanceledException e) {
            Log.e(CLASSNAME, e.getMessage(), e);
            return false;
        }
    }//executePostPendingIntent()


}
