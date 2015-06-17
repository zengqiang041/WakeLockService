/*
 * Copyright (c) 2015 Hai Bison
 *
 * See the file LICENSE at the root directory of this project for copying permission.
 */

package haibison.android.wake_lock_service;

import android.os.Bundle;

/**
 * Event listener for {link WakeLockService}.
 * <p/>
 * Note that this is a one-way interface so the server does not block waiting for the client.
 */
oneway interface IWakeLockServiceEventListener {

    /**
     * Will be called when the service state has changed.
     *
     * @param newState
     *            new state.
     */
    void onStateChanged(int newState);

    /**
     * Will be called when the service sends you a message.
     *
     * @param msgId
     *            the message ID.
     * @param msg
     *            the message.
     */
    void onMessage(int msgId, in Bundle msg);

}
