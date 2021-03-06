/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.lib;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;

import com.jefftharris.passwdsafe.sync.R;

/**
 * The SyncLogRecord class contains a record in the sync log
 */
public class SyncLogRecord
{
    private final String itsAccount;
    private final long itsStartTime;
    private final List<Exception> itsFailures = new ArrayList<>();
    private long itsEndTime = -1;
    private final boolean itsIsManualSync;
    private boolean itsIsNotConnected = false;
    private boolean itsIsInterrupted = false;
    private final List<String> itsEntries = new ArrayList<>();
    private final List<String> itsConflictFiles = new ArrayList<>();

    /** Constructor */
    public SyncLogRecord(String account, String typeName, boolean manual)
    {
        itsAccount = account + " (" + typeName + ")";
        itsStartTime = System.currentTimeMillis();
        itsIsManualSync = manual;
    }

    /** Get the account name */
    public String getAccount()
    {
        return itsAccount;
    }

    /** Get the start time */
    public long getStartTime()
    {
        return itsStartTime;
    }

    /** Does the record represent a successful sync */
    public boolean isSuccess()
    {
        return itsFailures.isEmpty();
    }

    /** Add an exception failure */
    public void addFailure(Exception e)
    {
        itsFailures.add(e);
    }

    /** Get the end time for a sync */
    public long getEndTime()
    {
        return itsEndTime;
    }

    /** Set the end time for a sync */
    public void setEndTime()
    {
        itsEndTime = System.currentTimeMillis();
    }

    /** Get whether the sync was started manually */
    public boolean isManualSync()
    {
        return itsIsManualSync;
    }

    /** Get whether the network was not connected */
    public boolean isNotConnected()
    {
        return itsIsNotConnected;
    }

    /** Set whether the network is not connected */
    public void setNotConnected(boolean notConnected)
    {
        itsIsNotConnected = notConnected;
    }

    /**
     * Check whether the sync was interrupted
     */
    public void checkSyncInterrupted() throws InterruptedException
    {
        if (Thread.interrupted() || itsIsInterrupted) {
            itsIsInterrupted = true;
            throw new InterruptedException();
        }
    }

    /** Add a sync operation entry */
    public void addEntry(String entry)
    {
        itsEntries.add(entry);
    }

    /** Get the sync operation entries */
    public List<String> getEntries()
    {
        return itsEntries;
    }

    /** Add a conflict file */
    public void addConflictFile(String filename)
    {
        itsConflictFiles.add(filename);
    }

    /** Get the conflict files */
    public List<String> getConflictFiles()
    {
        return itsConflictFiles;
    }

    /** Get a string representation of the actions in the record */
    public String getActions(Context ctx)
    {
        StringBuilder actions = new StringBuilder();
        for (String entry: itsEntries) {
            if (actions.length() != 0) {
                actions.append("\n");
            }
            actions.append(entry);
        }
        for (Exception e: itsFailures) {
            if (actions.length() != 0) {
                actions.append("\n");
            }
            actions.append(ctx.getString(R.string.error_fmt, e.toString()));
        }
        return actions.toString();
    }

    /**
     * Get the stack trace in the record
     */
    public String getStacktrace()
    {
        StringWriter strwriter = new StringWriter();
        PrintWriter writer = new PrintWriter(strwriter);
        boolean first = true;
        for (Exception e: itsFailures) {
            if (first) {
                first = false;
            } else {
                writer.println();
                writer.println();
            }
            e.printStackTrace(writer);
        }
        return strwriter.toString();
    }

    /** Get a string representation of the record */
    public String toString(Context ctx)
    {
        return ctx.getString(
                R.string.sync_log_record, itsAccount,
                ctx.getString(itsIsManualSync ?
                                      R.string.manual :
                                      R.string.automatic),
                ctx.getString(itsIsNotConnected ?
                                      R.string.network_not_connected :
                                      R.string.network_connected),
                itsStartTime, itsEndTime) +
               "\n" +
               getActions(ctx);
    }
}
