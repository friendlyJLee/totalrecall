/*
 * Copyright (©) 2017 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.onedrive;

import android.content.Context;
import android.net.Uri;

import com.jefftharris.passwdsafe.sync.lib.AbstractLocalToRemoteSyncOper;
import com.jefftharris.passwdsafe.sync.lib.AbstractRemoteToLocalSyncOper;
import com.jefftharris.passwdsafe.sync.lib.AbstractRmSyncOper;
import com.jefftharris.passwdsafe.sync.lib.DbFile;
import com.jefftharris.passwdsafe.sync.lib.DbProvider;
import com.jefftharris.passwdsafe.sync.lib.ProviderRemoteFile;
import com.jefftharris.passwdsafe.sync.lib.ProviderSyncer;
import com.jefftharris.passwdsafe.sync.lib.SyncConnectivityResult;
import com.jefftharris.passwdsafe.sync.lib.SyncLogRecord;
import com.jefftharris.passwdsafe.sync.lib.SyncRemoteFiles;
import com.microsoft.onedriveaccess.IOneDriveService;
import com.microsoft.onedriveaccess.model.Drive;
import com.microsoft.onedriveaccess.model.Item;

import java.util.List;

import retrofit.RetrofitError;

/**
 * The OnedriveSyncer class encapsulates an OneDrive sync operation
 */
public class OnedriveSyncer extends ProviderSyncer<IOneDriveService>
{
    private static final String TAG = "OnedriveSyncer";

    /**
     * Constructor
     */
    public OnedriveSyncer(IOneDriveService service,
                          DbProvider provider,
                          SyncConnectivityResult connResult,
                          SyncLogRecord logrec, Context ctx)
    {
        super(service, provider, connResult, logrec, ctx, TAG);
    }


    /**
     * Get the account display name
     */
    public static String getDisplayName(IOneDriveService client)
            throws RetrofitError
    {
        Drive drive = client.getDrive();
        if ((drive != null) && (drive.Owner != null) &&
            (drive.Owner.User != null)) {
            return drive.Owner.User.DisplayName;
        } else {
            return null;
        }
    }

    /** Is the error a 404 not-found error */
    public static boolean isNot404Error(RetrofitError e)
    {
        return (e.isNetworkError() ||
                (e.getResponse() == null) ||
                (e.getResponse().getStatus() != 404));
    }


    /** Create a remote identifier from the local name of a file */
    public static String createRemoteIdFromLocal(DbFile dbfile)
    {
        return ProviderRemoteFile.PATH_SEPARATOR +
               Uri.encode(dbfile.itsLocalTitle);
    }


    @Override
    protected SyncRemoteFiles getSyncRemoteFiles(List<DbFile> dbfiles)
    {
        SyncRemoteFiles files = new SyncRemoteFiles();
        for (DbFile dbfile: dbfiles) {
            if (dbfile.itsRemoteId == null) {
                Item item = getRemoteFile(createRemoteIdFromLocal(dbfile));
                if (item != null) {
                    files.addRemoteFileForNew(dbfile.itsId,
                                              new OnedriveProviderFile(item));
                }
            } else {
                switch (dbfile.itsRemoteChange) {
                case NO_CHANGE:
                case ADDED:
                case MODIFIED: {
                    Item item = getRemoteFile(dbfile.itsRemoteId);
                    if (item != null) {
                        files.addRemoteFile(new OnedriveProviderFile(item));
                    }
                    break;
                }
                case REMOVED: {
                    break;
                }
                }
            }
        }
        return files;
    }


    /**
     * Create an operation to sync local to remote
     */
    @Override
    protected AbstractLocalToRemoteSyncOper<IOneDriveService>
    createLocalToRemoteOper(DbFile dbfile)
    {
        return new OnedriveLocalToRemoteOper(dbfile);
    }


    /**
     * Create an operation to sync remote to local
     */
    @Override
    protected AbstractRemoteToLocalSyncOper<IOneDriveService>
    createRemoteToLocalOper(DbFile dbfile)
    {
        return new OnedriveRemoteToLocalOper(dbfile);
    }


    /**
     * Create an operation to remove a file
     */
    @Override
    protected AbstractRmSyncOper<IOneDriveService>
    createRmFileOper(DbFile dbfile)
    {
        return new OnedriveRmFileOper(dbfile);
    }


    /**
     * Get a remote file's entry from OneDrive
     * @return The file's entry if found; null if not found or deleted
     */
    private Item getRemoteFile(String remoteId) throws RetrofitError
    {
        try {
            Item item = itsProviderClient.getItemByPath(remoteId, null);
            if (item.Deleted != null) {
                return null;
            }
            return item;
        } catch (RetrofitError e) {
            if (isNot404Error(e)) {
                throw e;
            }
            return null;
        }
    }
}
