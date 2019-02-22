/*
 * Copyright (©) 2012 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.lib.view;

import java.lang.reflect.Method;

import android.annotation.TargetApi;
import android.app.Activity;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;


/**
 * The GuiUtilsHoneycomb class contains helper GUI methods that are usable on
 * Honeycomb and higher
 */
@SuppressWarnings("CanBeFinal")
@TargetApi(11)
public final class GuiUtilsHoneycomb
{
    private static Method itsInvalidateOptionsMenuMeth;
    private static Method itsSetTextIsSelectableMeth;

    static {
        try {
            itsInvalidateOptionsMenuMeth =
                Activity.class.getMethod("invalidateOptionsMenu");
            itsSetTextIsSelectableMeth =
                TextView.class.getMethod("setTextIsSelectable", boolean.class);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    /**
     * Set a button checked without animation
     */
    public static void setCheckedNoAnim(CompoundButton view, boolean checked)
    {
        view.setChecked(checked);
        view.jumpDrawablesToCurrentState();
    }

    /**
     * Invalidate the options menu on an activity
     */
    public static void invalidateOptionsMenu(Activity act)
    {
        try {
            itsInvalidateOptionsMenuMeth.invoke(act);
        }
        catch (Exception e) {
            PasswdSafeUtil.showFatalMsg(e, act);
        }
    }

    /** Set the text in a TextView as selectable */
    public static void setTextSelectable(TextView tv)
    {
        try {
            itsSetTextIsSelectableMeth.invoke(tv, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
