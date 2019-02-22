/*
 * Copyright (©) 2017 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.lib;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import java.lang.ref.WeakReference;

/**
 * Managed reference to fragment or activity for callbacks
 */
public final class ManagedRef<T>
{
    private final WeakReference<T> itsRef;

    /**
     * Constructor
     */
    public ManagedRef(@NonNull T obj)
    {
        itsRef = new WeakReference<>(obj);
    }

    /**
     * Get the reference
     * @return The managed reference; null if an activity or fragment that's not
     * running
     */
    public @Nullable T get()
    {
        T obj = itsRef.get();
        if (((obj instanceof Fragment) && !((Fragment)obj).isAdded()) ||
            ((obj instanceof Activity) && ((Activity)obj).isFinishing())) {
                obj = null;
        }
        return obj;
    }

    /**
     * Clear the reference
     */
    public void clear()
    {
        itsRef.clear();
    }
}
