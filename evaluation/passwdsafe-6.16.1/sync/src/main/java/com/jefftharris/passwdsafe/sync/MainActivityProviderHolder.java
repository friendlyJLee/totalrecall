/*
 * Copyright (©) 2018 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import com.jefftharris.passwdsafe.lib.PasswdSafeContract;
import com.jefftharris.passwdsafe.lib.ProviderType;
import com.jefftharris.passwdsafe.lib.Utils;
import com.jefftharris.passwdsafe.lib.view.GuiUtils;
import com.jefftharris.passwdsafe.sync.lib.SyncResults;

/**
 * RecyclerView holder class for a provider in the MainActivity
 */
class MainActivityProviderHolder
        extends RecyclerView.ViewHolder
        implements View.OnClickListener,
                   AdapterView.OnItemSelectedListener
{
    private final MainActivityProviderOps itsProviderOps;
    private final TextView itsTitle;
    private final TextView itsAccount;
    private final View itsLastSuccessLabel;
    private final TextView itsLastSuccess;
    private final View itsLastFailureLabel;
    private final TextView itsLastFailure;
    private final TextView itsWarning;
    private final View itsFreqLabel;
    private final Spinner itsFreqSpin;
    private final ImageButton itsDelete;
    private final ImageButton itsSync;
    private final ImageButton itsEdit;
    private final Button itsChooseFiles;
    private ProviderType itsType;
    private ProviderSyncFreqPref itsFreq;
    private Uri itsProviderUri;

    /**
     * Constructor
     */
    public MainActivityProviderHolder(View view,
                                      MainActivityProviderOps ops)
    {
        super(view);
        itsProviderOps = ops;
        itsTitle = view.findViewById(R.id.title);
        itsAccount = view.findViewById(R.id.account);
        itsLastSuccessLabel = view.findViewById(R.id.last_success_label);
        itsLastSuccess = view.findViewById(R.id.last_success);
        itsLastFailureLabel = view.findViewById(R.id.last_failure_label);
        itsLastFailure = view.findViewById(R.id.last_failure);
        itsWarning = view.findViewById(R.id.warning);
        itsFreqLabel = view.findViewById(R.id.interval_label);
        itsFreqSpin = view.findViewById(R.id.interval);
        itsFreqSpin.setOnItemSelectedListener(this);
        itsDelete = view.findViewById(R.id.delete);
        itsDelete.setOnClickListener(this);
        itsSync = view.findViewById(R.id.sync);
        itsSync.setOnClickListener(this);
        itsEdit = view.findViewById(R.id.edit);
        itsEdit.setOnClickListener(this);
        itsChooseFiles = view.findViewById(R.id.choose_files);
        itsChooseFiles.setOnClickListener(this);
    }

    /**
     * Update the view for a provider item
     */
    public void updateView(Cursor item)
    {
        Context ctx = itemView.getContext();
        
        long id = item.getLong(
                PasswdSafeContract.Providers.PROJECTION_IDX_ID);
        String typeStr = item.getString(
                PasswdSafeContract.Providers.PROJECTION_IDX_TYPE);
        itsType = ProviderType.valueOf(typeStr);
        itsProviderUri = ContentUris.withAppendedId(
                    PasswdSafeContract.Providers.CONTENT_URI, id);

        int freqVal = item.getInt(
                PasswdSafeContract.Providers.PROJECTION_IDX_SYNC_FREQ);
        itsFreq = ProviderSyncFreqPref.freqValueOf(freqVal);

        itsTitle.setCompoundDrawablesWithIntrinsicBounds(
                itsType.getIconId(false), 0, 0, 0);
        itsTitle.setText(itsType.getName(ctx));

        String acct = PasswdSafeContract.Providers.getDisplayName(item);
        itsAccount.setText(acct);

        String lastSuccess = null;
        String lastFailure = null;
        SyncResults syncRes = itsProviderOps.getProviderSyncResults(itsType);
        if (syncRes != null) {
            if (syncRes.hasLastSuccess()) {
                lastSuccess = Utils.formatDate(syncRes.getLastSuccess(), ctx);
            }
            if (syncRes.hasLastFailure()) {
                lastFailure = Utils.formatDate(syncRes.getLastFailure(), ctx);
            }
        }
        updateLast(itsLastSuccessLabel, itsLastSuccess, lastSuccess);
        updateLast(itsLastFailureLabel, itsLastFailure, lastFailure);

        boolean hasChooseFiles = false;
        boolean hasEditDialog = false;
        switch (itsType) {
        case DROPBOX:
        case ONEDRIVE: {
            hasChooseFiles = true;
            break;
        }
        case OWNCLOUD: {
            hasChooseFiles = true;
            hasEditDialog = true;
            break;
        }
        case BOX:
        case GDRIVE: {
            break;
        }
        }
        itsFreqSpin.setSelection(itsFreq.getDisplayIdx());
        GuiUtils.setVisible(itsEdit, hasEditDialog);
        GuiUtils.setVisible(itsFreqLabel, !hasEditDialog);
        GuiUtils.setVisible(itsFreqSpin, !hasEditDialog);

        GuiUtils.setVisible(itsChooseFiles, hasChooseFiles);

        CharSequence warning = itsProviderOps.getProviderWarning(itsType);
        itsWarning.setText(warning);
        GuiUtils.setVisible(itsWarning, !TextUtils.isEmpty(warning));
    }

    @Override
    public void onClick(View v)
    {
        if ((itsType == null) || (itsProviderUri == null)) {
            return;
        }
        if (v == itsDelete) {
            itsProviderOps.handleProviderDelete(itsProviderUri);
        } else if (v == itsSync) {
            itsProviderOps.handleProviderSync(itsType, itsProviderUri);
        } else if (v == itsEdit) {
            itsProviderOps.handleProviderEditDialog(itsType, itsProviderUri,
                                                    itsFreq);
        } else if (v == itsChooseFiles) {
            itsProviderOps.handleProviderChooseFiles(itsType, itsProviderUri);
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent,
                               View view,
                               int pos,
                               long id)
    {
        if ((itsProviderUri == null) || !itsProviderOps.isActivityRunning()) {
            return;
        }
        ProviderSyncFreqPref freq = ProviderSyncFreqPref.displayValueOf(pos);
        itsProviderOps.updateProviderSyncFreq(itsProviderUri, freq);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent)
    {
    }

    /**
     * Update the last times
     */
    private void updateLast(View label, TextView time, String timeStr)
    {
        boolean hasLast = !TextUtils.isEmpty(timeStr);
        GuiUtils.setVisible(label, hasLast);
        GuiUtils.setVisible(time, hasLast);
        time.setText(timeStr);
    }
}
