/*
 * Copyright (c) 2015 Hai Bison
 *
 * See the file LICENSE at the root directory of this project for copying permission.
 */

package haibison.android.wake_lock_service;

import android.os.Bundle;

import haibison.android.wake_lock_service.IWakeLockServiceEventListener;

/**
 * Interface for {@link WakeLockService}.
 */
interface IWakeLockService {

    /**
     * Registers an event listener. Given listener will be notified immediately by
     * {@link IWakeLockServiceEventListener#onStateChanged(int)}.
     *
     * @param listener
     *            the listener.
     */
    void registerEventListener(IWakeLockServiceEventListener listener);

    /**
     * Unregisters an event listener.
     *
     * @param listener
     *            the listener.
     */
    void unregisterEventListener(IWakeLockServiceEventListener listener);

    /**
     * Gets service state.
     *
     * @return the service state.
     */
    int getServiceState();

    /**
     * Gets boolean value from an ID.
     *
     * @param id
     *            the ID.
     * @return the value. Default implementation returns {@code false}.
     */
    boolean getBooleanValue(int id);

    /**
     * Gets a float value from an ID.
     *
     * @param id
     *            the ID.
     * @return the value. Default implementation returns {@code 0}.
     */
    float getFloatValue(int id);

    /**
     * Gets double value from an ID.
     *
     * @param id
     *            the ID.
     * @return the value. Default implementation returns {@code 0}.
     */
    double getDoubleValue(int id);

    /**
     * Gets integer value from an ID.
     *
     * @param id
     *            the ID.
     * @return the value. Default implementation returns {@code 0}.
     */
    int getIntValue(int id);

    /**
     * Gets long value from an ID.
     *
     * @param id
     *            the ID.
     * @return the value. Default implementation returns {@code 0}.
     */
    long getLongValue(int id);

    /**
     * Gets a {@link CharSequence} value from an ID.
     *
     * @param id
     *            the ID.
     * @return the value. Default implementation returns {@code null}.
     */
    CharSequence getCharSequenceValue(int id);

    /**
     * Gets string value from an ID.
     *
     * @param id
     *            the ID.
     * @return the value. Default implementation returns {@code null}.
     */
    String getStringValue(int id);

    /**
     * Gets a {@link Bundle} value from an ID.
     *
     * @param id
     *            the ID.
     * @return the value. Default implementation returns {@code null}.
     */
    Bundle getBundleValue(int id);

}
