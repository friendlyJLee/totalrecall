/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.util.Pair;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.jefftharris.passwdsafe.file.PasswdExpiryFilter;
import com.jefftharris.passwdsafe.file.PasswdFileData;
import com.jefftharris.passwdsafe.file.PasswdFileDataUser;
import com.jefftharris.passwdsafe.file.PasswdFileUri;
import com.jefftharris.passwdsafe.file.PasswdRecord;
import com.jefftharris.passwdsafe.file.PasswdRecordFilter;
import com.jefftharris.passwdsafe.lib.ActContext;
import com.jefftharris.passwdsafe.lib.ApiCompat;
import com.jefftharris.passwdsafe.lib.FileSharer;
import com.jefftharris.passwdsafe.lib.ManagedTask;
import com.jefftharris.passwdsafe.lib.ManagedTasks;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.lib.view.GuiUtils;
import com.jefftharris.passwdsafe.lib.view.ProgressFragment;
import com.jefftharris.passwdsafe.view.ConfirmPromptDialog;
import com.jefftharris.passwdsafe.view.CopyField;
import com.jefftharris.passwdsafe.view.EditRecordResult;
import com.jefftharris.passwdsafe.view.PasswdFileDataView;
import com.jefftharris.passwdsafe.view.PasswdLocation;
import com.jefftharris.passwdsafe.view.PasswdRecordListData;

import org.pwsafe.lib.file.PwsRecord;

import java.io.IOException;
import java.util.BitSet;
import java.util.Date;
import java.util.List;

/**
 * The main PasswdSafe activity for showing a password file
 */
public class PasswdSafe extends AppCompatActivity
        implements AbstractPasswdSafeRecordFragment.Listener,
                   AboutFragment.Listener,
                   View.OnClickListener,
                   MenuItem.OnActionExpandListener,
                   ConfirmPromptDialog.Listener,
                   PasswdSafeChangePasswordFragment.Listener,
                   PasswdSafeEditRecordFragment.Listener,
                   PasswdSafeExpirationsFragment.Listener,
                   PasswdSafeListFragment.Listener,
                   PasswdSafeOpenFileFragment.Listener,
                   PasswdSafePolicyListFragment.Listener,
                   PasswdSafeNavDrawerFragment.Listener,
                   PasswdSafeNewFileFragment.Listener,
                   PasswdSafeRecordFragment.Listener,
                   PreferencesFragment.Listener,
                   PreferenceFragmentCompat.OnPreferenceStartScreenCallback
{
    public static final int CONTEXT_GROUP_RECORD_BASIC = 1;
    public static final int CONTEXT_GROUP_LIST = 2;
    public static final int CONTEXT_GROUP_LIST_CONTENTS = 3;

    private enum ChangeMode
    {
        /** Initial mode with no file open */
        INIT,
        /** Opening a file */
        FILE_OPEN,
        /** Creating a new file */
        FILE_NEW,
        /** Initial mode for an open file */
        OPEN_INIT,
        /** An open file */
        OPEN,
        /** A record */
        RECORD,
        /** Edit a record */
        EDIT_RECORD,
        /** Change a password */
        CHANGE_PASSWORD,
        /** View about info */
        VIEW_ABOUT,
        /** View expiration info */
        VIEW_EXPIRATION,
        /** View policy list */
        VIEW_POLICY_LIST,
        /** View preferences */
        VIEW_PREFERENCES
    }

    private enum ViewMode
    {
        /** Initial mode */
        INIT,
        /** Opening a file */
        FILE_OPEN,
        /** Creating a new file */
        FILE_NEW,
        /** Viewing a list of records */
        VIEW_LIST,
        /** Viewing a record */
        VIEW_RECORD,
        /** Editing a record */
        EDIT_RECORD,
        /** Changing a password */
        CHANGING_PASSWORD,
        /** Viewing about info */
        VIEW_ABOUT,
        /** Viewing expiration info */
        VIEW_EXPIRATION,
        /** Viewing a list of policies */
        VIEW_POLICY_LIST,
        /** Viewing preferences */
        VIEW_PREFERENCES
    }

    /** Action conformed via ConfirmPromptDialog */
    private enum ConfirmAction
    {
        /** Copy a password */
        COPY_PASSWORD,
        /** Delete a file */
        DELETE_FILE,
        /** Delete a record */
        DELETE_RECORD,
        /** Share a file */
        SHARE_FILE,
        /** Show settings to enable the keyboard */
        SHOW_ENABLE_KEYBOARD
    }

    /** Method for finishing the edit of the file */
    private enum EditFinish
    {
        ADD_RECORD,
        CHANGE_PASSWORD,
        DELETE_RECORD,
        EDIT_NOSAVE_RECORD,
        EDIT_SAVE_RECORD,
        POLICY_EDIT,
        PROTECT_RECORD
    }

    /** Fragment holding the open file data */
    private PasswdSafeFileDataFragment itsFileDataFrag;

    /** The location in the password file */
    private PasswdLocation itsLocation = new PasswdLocation();

    /** Panel for displaying the query */
    private View itsQueryPanel;

    /** The query label */
    private TextView itsQuery;

    /** Panel for displaying expired entries */
    private View itsExpiryPanel;

    /** The expired entries label */
    private TextView itsExpiry;

    /** The search menu item */
    private MenuItem itsSearchItem = null;

    /** Fragment managing the behaviors, interactions and presentation of the
     * navigation drawer. */
    private PasswdSafeNavDrawerFragment itsNavDrawerFrag;

    /** Currently running tasks */
    private final ManagedTasks itsTasks = new ManagedTasks();

    /** Used to store the last screen title */
    private CharSequence itsTitle;

    /** Does the UI show two panes */
    private boolean itsIsTwoPane = false;

    /** Receiver for file timeout notifications */
    private FileTimeoutReceiver itsTimeoutReceiver;

    /** Current view mode */
    private ViewMode itsCurrViewMode = ViewMode.INIT;

    /** Whether to confirm a back operation if it will close the file */
    private boolean itsIsConfirmBackClosed = true;

    /** Has the activity been resumed */
    private boolean itsIsResumed = false;

    private static final String FRAG_DATA = "data";
    private static final String STATE_TITLE = "title";
    private static final String STATE_QUERY = "query";
    private static final String STATE_EXPIRY_VISIBLE = "expiryVisible";

    private static final String CONFIRM_ARG_ACTION = "action";
    private static final String CONFIRM_ARG_LOCATION = "location";
    private static final String CONFIRM_ARG_RECORD = "record";

    private static final int MENU_BIT_CAN_ADD = 0;
    private static final int MENU_BIT_HAS_FILE_OPS = 1;
    private static final int MENU_BIT_HAS_FILE_CHANGE_PASSWORD = 2;
    private static final int MENU_BIT_HAS_FILE_PROTECT = 3;
    private static final int MENU_BIT_HAS_FILE_SHARE = 4;
    private static final int MENU_BIT_HAS_FILE_DELETE = 5;
    private static final int MENU_BIT_PROTECT_ALL = 6;
    private static final int MENU_BIT_HAS_SEARCH = 7;
    private static final int MENU_BIT_HAS_CLOSE = 8;

    private static final String TAG = "PasswdSafe";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        PasswdSafeApp.setupTheme(this);
        super.onCreate(savedInstanceState);
        ApiCompat.setRecentAppsVisible(getWindow(), false);
        setContentView(R.layout.activity_passwdsafe);
        itsIsTwoPane = (findViewById(R.id.two_pane) != null);

        itsNavDrawerFrag = (PasswdSafeNavDrawerFragment)
                getSupportFragmentManager().findFragmentById(
                        R.id.navigation_drawer);
        itsNavDrawerFrag.setUp(findViewById(R.id.drawer_layout));

        itsQueryPanel = findViewById(R.id.query_panel);
        View queryClearBtn = findViewById(R.id.query_clear_btn);
        assert queryClearBtn != null;
        queryClearBtn.setOnClickListener(this);
        itsQuery = findViewById(R.id.query);

        itsExpiryPanel = findViewById(R.id.expiry_panel);
        assert itsExpiryPanel != null;
        itsExpiryPanel.setOnClickListener(this);
        GuiUtils.setVisible(itsExpiryPanel, false);
        View expiryClearBtn = findViewById(R.id.expiry_clear_btn);
        assert expiryClearBtn != null;
        expiryClearBtn.setOnClickListener(this);
        itsExpiry = findViewById(R.id.expiry);

        FragmentManager fragMgr = getSupportFragmentManager();
        itsFileDataFrag = (PasswdSafeFileDataFragment)
                fragMgr.findFragmentByTag(FRAG_DATA);
        if (itsFileDataFrag == null) {
            itsFileDataFrag = new PasswdSafeFileDataFragment();
            fragMgr.beginTransaction().add(itsFileDataFrag, FRAG_DATA).commit();
        }
        boolean newFileDataFrag = itsFileDataFrag.checkNew();

        itsTimeoutReceiver = new FileTimeoutReceiver(this);

        if (newFileDataFrag || (savedInstanceState == null)) {
            itsTitle = getTitle();
            doUpdateView(ViewMode.INIT, new PasswdLocation());
            changeInitialView();

            Intent intent = getIntent();
            PasswdSafeUtil.dbginfo(TAG, "onCreate: %s", intent);
            switch (String.valueOf(intent.getAction())) {
            case PasswdSafeUtil.VIEW_INTENT:
            case Intent.ACTION_VIEW: {
                changeFileOpenView(intent);
                break;
            }
            case PasswdSafeUtil.NEW_INTENT: {
                changeFileNewView(intent);
                break;
            }
            default: {
                Log.e(TAG, "Unknown action for intent: " + intent);
                finish();
                break;
            }
            }
        } else {
            itsTitle = savedInstanceState.getCharSequence(STATE_TITLE);
            itsQuery.setText(savedInstanceState.getCharSequence(STATE_QUERY));
            if (savedInstanceState.getBoolean(STATE_EXPIRY_VISIBLE, false)) {
                itsFileDataFrag.getFileDataView().resetExpiryChanged();
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);

        PasswdSafeUtil.dbginfo(TAG, "onNewIntent: %s", intent);
        switch (String.valueOf(intent.getAction())) {
        case PasswdSafeUtil.VIEW_INTENT:
        case Intent.ACTION_VIEW: {
            final Uri openUri = PasswdSafeApp.getOpenUriFromIntent(intent);
            Boolean reopen = itsFileDataFrag.useFileData(
                    fileData -> !fileData.getUri().getUri().equals(openUri));
            if ((reopen == null) || reopen) {
                // Close and reopen the new file
                itsFileDataFrag.setFileData(null);
                doUpdateView(ViewMode.INIT, new PasswdLocation());
                changeInitialView();
                changeFileOpenView(intent);
            }
            break;
        }
        case Intent.ACTION_SEARCH: {
            setRecordQueryFilter(intent.getStringExtra(SearchManager.QUERY));
            break;
        }
        case PasswdSafeUtil.SEARCH_VIEW_INTENT: {
            collapseSearch();
            String data = intent.getStringExtra(SearchManager.EXTRA_DATA_KEY);
            if (data == null) {
                break;
            }
            PasswdLocation loc = null;
            if (data.startsWith(PasswdRecordFilter.SEARCH_VIEW_RECORD)) {
                int pfxlen = PasswdRecordFilter.SEARCH_VIEW_RECORD.length();
                final String uuid = data.substring(pfxlen);
                loc = useFileData(fileData -> {
                    PwsRecord rec = fileData.getRecord(uuid);
                    if (rec == null) {
                        return null;
                    }
                    return new PasswdLocation(rec, fileData);
                });
            } else if (data.startsWith(PasswdRecordFilter.SEARCH_VIEW_GROUP)) {
                int pfxlen = PasswdRecordFilter.SEARCH_VIEW_GROUP.length();
                loc = new PasswdLocation(data.substring(pfxlen));
            }
            if (loc != null) {
                changeLocation(loc);
            }
            break;
        }
        default: {
            FragmentManager fragMgr = getSupportFragmentManager();
            Fragment frag = fragMgr.findFragmentById(R.id.content);
            if (frag instanceof PasswdSafeOpenFileFragment) {
                ((PasswdSafeOpenFileFragment)frag).onNewIntent(intent);
            }
            break;
        }
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState)
    {
        itsNavDrawerFrag.onPostCreate();
        super.onPostCreate(savedInstanceState);
    }

    @Override
    protected void onPostResume()
    {
        super.onPostResume();
        itsIsResumed = true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putCharSequence(STATE_TITLE, itsTitle);
        outState.putCharSequence(STATE_QUERY, itsQuery.getText());
        outState.putBoolean(STATE_EXPIRY_VISIBLE,
                            itsExpiryPanel.getVisibility() == View.VISIBLE);
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        itsIsResumed = false;
        itsTasks.cancelTasks();
    }

    @Override
    protected void onDestroy()
    {
        itsTimeoutReceiver.onDestroy();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        if (itsNavDrawerFrag.isDrawerOpen()) {
            return super.onCreateOptionsMenu(menu);
        }

        // Only show items in the action bar relevant to this screen
        // if the drawer is not showing. Otherwise, let the drawer
        // decide what to show in the action bar.
        getMenuInflater().inflate(R.menu.activity_passwdsafe, menu);
        restoreActionBar();

        // Get the SearchView and set the searchable configuration
        SearchManager searchManager =
                (SearchManager)getSystemService(Context.SEARCH_SERVICE);
        itsSearchItem = menu.findItem(R.id.menu_search);
        itsSearchItem.setOnActionExpandListener(this);
        collapseSearch();
        if (searchManager != null) {
            SearchView searchView = (SearchView)itsSearchItem.getActionView();
            searchView.setSearchableInfo(
                    searchManager.getSearchableInfo(getComponentName()));
            searchView.setIconifiedByDefault(true);
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        final BitSet options = new BitSet();
        options.set(MENU_BIT_HAS_CLOSE);

        itsFileDataFrag.useFileData((PasswdFileDataUser<Void>)fileData -> {
            boolean fileEditable = fileData.canEdit();
            switch (itsCurrViewMode) {
            case VIEW_LIST: {
                options.set(MENU_BIT_CAN_ADD, fileEditable);
                options.set(MENU_BIT_HAS_SEARCH, true);
                if (fileEditable) {
                    options.set(MENU_BIT_HAS_FILE_OPS, true);
                    options.set(MENU_BIT_HAS_FILE_CHANGE_PASSWORD,
                                fileData.isNotYubikey());
                    options.set(MENU_BIT_HAS_FILE_PROTECT, true);
                    options.set(MENU_BIT_HAS_FILE_SHARE, true);
                    options.set(MENU_BIT_PROTECT_ALL,
                                itsLocation.getGroups().isEmpty());
                }
                if (fileData.canDelete()) {
                    options.set(MENU_BIT_HAS_FILE_OPS, true);
                    options.set(MENU_BIT_HAS_FILE_DELETE, true);
                }
                break;
            }
            case VIEW_RECORD: {
                options.set(MENU_BIT_CAN_ADD, fileEditable);
                break;
            }
            case INIT:
            case FILE_OPEN:
            case FILE_NEW:
            case VIEW_ABOUT:
            case VIEW_EXPIRATION:
            case VIEW_POLICY_LIST:
            case VIEW_PREFERENCES: {
                break;
            }
            case EDIT_RECORD:
            case CHANGING_PASSWORD: {
                options.set(MENU_BIT_HAS_CLOSE, false);
                break;
            }
            }
            return null;
        });
        if ((itsSearchItem != null) && itsSearchItem.isActionViewExpanded()) {
            options.clear(MENU_BIT_CAN_ADD);
        }

        MenuItem item = menu.findItem(R.id.menu_add);
        if (item != null) {
            item.setVisible(options.get(MENU_BIT_CAN_ADD));
        }

        item = menu.findItem(R.id.menu_close);
        if (item != null) {
            item.setVisible(options.get(MENU_BIT_HAS_CLOSE));
        }

        item = menu.findItem(R.id.menu_file_ops);
        if (item != null) {
            item.setVisible(options.get(MENU_BIT_HAS_FILE_OPS));
        }

        item = menu.findItem(R.id.menu_file_change_password);
        if (item != null) {
            item.setEnabled(options.get(MENU_BIT_HAS_FILE_CHANGE_PASSWORD));
        }

        if (options.get(MENU_BIT_HAS_FILE_OPS)) {
            boolean hasProtect = options.get(MENU_BIT_HAS_FILE_PROTECT);
            boolean viewProtectAll = options.get(MENU_BIT_PROTECT_ALL);
            item = menu.findItem(R.id.menu_file_protect_records);
            if (item != null) {
                item.setEnabled(hasProtect);
                item.setTitle(viewProtectAll ? R.string.protect_all :
                                      R.string.protect_group);
            }
            item = menu.findItem(R.id.menu_file_unprotect_records);
            if (item != null) {
                item.setEnabled(hasProtect);
                item.setTitle(viewProtectAll ? R.string.unprotect_all :
                                      R.string.unprotect_group);
            }

            item = menu.findItem(R.id.menu_file_share);
            if (item != null) {
                item.setEnabled(options.get(MENU_BIT_HAS_FILE_SHARE));
            }

            item = menu.findItem(R.id.menu_file_delete);
            if (item != null) {
                item.setEnabled(options.get(MENU_BIT_HAS_FILE_DELETE));
            }
        }

        item = menu.findItem(R.id.menu_search);
        if (item != null) {
            item.setVisible(options.get(MENU_BIT_HAS_SEARCH));
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (!itsIsResumed) {
            return super.onOptionsItemSelected(item);
        }
        switch (item.getItemId()) {
        case android.R.id.home: {
            if (itsNavDrawerFrag.isDrawerEnabled()) {
                return super.onOptionsItemSelected(item);
            }
            onBackPressed();
            return true;
        }
        case R.id.menu_add: {
            editRecord(itsLocation.selectRecord(null));
            return true;
        }
        case R.id.menu_close: {
            checkNavigation(false, this::finish);
            return true;
        }
        case R.id.menu_file_change_password: {
            PasswdSafeUtil.dbginfo(TAG, "change password");
            doChangeView(ChangeMode.CHANGE_PASSWORD,
                         PasswdSafeChangePasswordFragment.newInstance());
            return true;
        }
        case R.id.menu_file_delete: {
            String uriName = itsFileDataFrag.useFileData(
                    fileData -> fileData.getUri().getIdentifier(PasswdSafe.this,
                                                                true));
            if (uriName == null) {
                return true;
            }
            Bundle confirmArgs = new Bundle();
            confirmArgs.putString(CONFIRM_ARG_ACTION,
                                  ConfirmAction.DELETE_FILE.name());
            ConfirmPromptDialog dialog = ConfirmPromptDialog.newInstance(
                    getString(R.string.delete_file_msg, uriName),
                    null, getString(R.string.delete), confirmArgs);
            dialog.show(getSupportFragmentManager(), "Delete file");
            return true;
        }
        case R.id.menu_file_protect_records: {
            protectRecords(true);
            return true;
        }
        case R.id.menu_file_share: {
            Bundle confirmArgs = new Bundle();
            confirmArgs.putString(CONFIRM_ARG_ACTION,
                                  ConfirmAction.SHARE_FILE.name());
            ConfirmPromptDialog dialog = ConfirmPromptDialog.newInstance(
                    getString(R.string.share_file_p),
                    getString(R.string.share_file_msg),
                    getString(R.string.share), confirmArgs);
            dialog.show(getSupportFragmentManager(), "Share file");

            return true;
        }
        case R.id.menu_file_unprotect_records: {
            protectRecords(false);
            return true;
        }
        default: {
            return super.onOptionsItemSelected(item);
        }
        }
    }

    @Override
    public void onBackPressed()
    {
        FragmentManager fragMgr = getSupportFragmentManager();
        if (fragMgr.getBackStackEntryCount() == 0) {
            switch (itsCurrViewMode) {
            case VIEW_LIST: {
                if (itsIsConfirmBackClosed) {
                    Toast.makeText(this, R.string.press_again_close_warning,
                                   Toast.LENGTH_SHORT).show();
                    itsIsConfirmBackClosed = false;
                    return;
                }
                break;
            }
            case INIT:
            case CHANGING_PASSWORD:
            case EDIT_RECORD:
            case FILE_OPEN:
            case FILE_NEW:
            case VIEW_RECORD:
            case VIEW_ABOUT:
            case VIEW_EXPIRATION:
            case VIEW_POLICY_LIST:
            case VIEW_PREFERENCES: {
                break;
            }
            }
        }

        checkNavigation(false, PasswdSafe.super::onBackPressed);
    }

    /**
     * Called when a view has been clicked.
     *
     * @param v The view that was clicked.
     */
    @Override
    public void onClick(View v)
    {
        switch (v.getId()) {
        case R.id.query_clear_btn: {
            setRecordQueryFilter(null);
            break;
        }
        case R.id.expiry_panel: {
            PasswdExpiryFilter filter =
                    itsFileDataFrag.getFileDataView().getExpiredRecordsFilter();
            if (filter != null) {
                setRecordExpiryFilter(filter, null);
            }
            GuiUtils.setVisible(itsExpiryPanel, false);
            break;
        }
        case R.id.expiry_clear_btn: {
            GuiUtils.setVisible(itsExpiryPanel, false);
            break;
        }
        }
    }

    @Override
    public boolean onMenuItemActionExpand(MenuItem item)
    {
        invalidateOptionsMenu();
        return true;
    }

    @Override
    public boolean onMenuItemActionCollapse(MenuItem item)
    {
        invalidateOptionsMenu();
        return true;
    }

    @Override
    public boolean onPreferenceStartScreen(PreferenceFragmentCompat caller,
                                           PreferenceScreen pref)
    {
        doChangeView(ChangeMode.VIEW_PREFERENCES,
                     PreferencesFragment.newInstance(pref.getKey()));
        return true;
    }

    /**
     * Show the file records
     */
    @Override
    public void showFileRecords()
    {
        changeOpenView(itsLocation, false);
    }

    /**
     * Show the file password policies
     */
    @Override
    public void showFilePasswordPolicies()
    {
        doChangeView(ChangeMode.VIEW_POLICY_LIST,
                     PasswdSafePolicyListFragment.newInstance());
    }

    /**
     * Show the file expired passwords
     */
    @Override
    public void showFileExpiredPasswords()
    {
        doChangeView(ChangeMode.VIEW_EXPIRATION,
                     PasswdSafeExpirationsFragment.newInstance());
    }

    @Override
    public void showRecordPreferences()
    {
        doChangeView(ChangeMode.VIEW_PREFERENCES,
                     PreferencesFragment.newInstance(
                             PreferencesFragment.SCREEN_RECORD));
    }

    /**
     * Show the preferences
     */
    @Override
    public void showPreferences()
    {
        doChangeView(ChangeMode.VIEW_PREFERENCES,
                     PreferencesFragment.newInstance(null));
    }

    /**
     * Show the about dialog
     */
    @Override
    public void showAbout()
    {
        doChangeView(ChangeMode.VIEW_ABOUT, AboutFragment.newInstance());
    }

    /**
     * Handle when the file open is canceled
     */
    @Override
    public void handleFileOpenCanceled()
    {
        PasswdSafeUtil.dbginfo(TAG, "handleFileOpenCanceled");
        finish();
    }

    /**
     * Handle when the file was successfully opened
     */
    @Override
    public void handleFileOpen(PasswdFileData fileData, String recToOpen)
    {
        PasswdSafeUtil.dbginfo(TAG, "handleFileOpen: %s, rec: %s",
                               fileData.getUri(), recToOpen);

        itsFileDataFrag.setFileData(fileData);
        changeOpenView(itsLocation, true);

        PasswdSafeApp app = (PasswdSafeApp)getApplication();
        app.getNotifyMgr().cancelNotification(fileData.getUri());

        // Jump to record to open if given
        if (!TextUtils.isEmpty(recToOpen)) {
            PwsRecord rec = fileData.getRecord(recToOpen);
            if (rec != null) {
                changeLocation(new PasswdLocation(rec, fileData));
            } else {
                Toast.makeText(this, R.string.record_not_found,
                               Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Handle when the file new is canceled
     */
    @Override
    public void handleFileNewCanceled()
    {
        PasswdSafeUtil.dbginfo(TAG, "handleFileNewCanceled");
        finish();
    }

    /**
     * Handle when the file was successfully created
     */
    @Override
    public void handleFileNew(PasswdFileData fileData)
    {
        PasswdSafeUtil.dbginfo(TAG, "handleFileNew: %s", fileData.getUri());
        itsFileDataFrag.setFileData(fileData);
        changeOpenView(itsLocation, true);
    }

    /**
     * Get the current record items in a background thread
     */
    @Override
    public List<PasswdRecordListData> getBackgroundRecordItems(
            boolean incRecords,
            boolean incGroups)
    {
        PasswdFileDataView dataView = itsFileDataFrag.getFileDataView();
        return dataView.getRecords(incRecords, incGroups);
    }

    @Override
    public boolean isCopySupported()
    {
        return true;
    }

    @Override
    public void copyField(final CopyField field, final String recUuid)
    {
        switch (field) {
        case PASSWORD: {
            SharedPreferences prefs = Preferences.getSharedPrefs(this);
            if (Preferences.isCopyPasswordConfirm(prefs)) {
                break;
            }

            // Need to prompt
            Bundle confirmArgs = new Bundle();
            confirmArgs.putString(CONFIRM_ARG_ACTION,
                                  ConfirmAction.COPY_PASSWORD.name());
            confirmArgs.putString(CONFIRM_ARG_RECORD, recUuid);
            Bundle enableArgs = new Bundle();
            enableArgs.putString(CONFIRM_ARG_ACTION,
                                 ConfirmAction.SHOW_ENABLE_KEYBOARD.name());
            ConfirmPromptDialog dialog = ConfirmPromptDialog.newInstance(
                    getString(R.string.copy_password),
                    getString(R.string.copy_password_warning),
                    getString(android.R.string.copy), confirmArgs,
                    getString(R.string.enable), enableArgs);
            dialog.show(getSupportFragmentManager(), "Copy password");
            return;
        }
        case USER_NAME:
        case URL:
        case EMAIL: {
            break;
        }
        }

        String copyStr = itsFileDataFrag.useFileData(fileData -> {
            PwsRecord rec = fileData.getRecord(recUuid);
            if (rec == null) {
                return null;
            }

            switch (field) {
            case PASSWORD: {
                PasswdRecord passwdRec = fileData.getPasswdRecord(rec);
                if (passwdRec != null) {
                    return passwdRec.getPassword(fileData);
                }
                break;
            }
            case USER_NAME: {
                return fileData.getUsername(rec);
            }
            case URL: {
                String url = fileData.getURL(rec);
                if (!TextUtils.isEmpty(url)) {
                    return url;
                }
                break;
            }
            case EMAIL: {
                String email = fileData.getEmail(rec);
                if (!TextUtils.isEmpty(email)) {
                    return email;
                }
                break;
            }
            }
            return null;
        });
        if (copyStr != null) {
            PasswdSafeUtil.copyToClipboard(copyStr, PasswdSafe.this);
        }
    }

    /**
     * Change the location in the password file
     */
    @Override
    public void changeLocation(PasswdLocation location)
    {
        if (isFileOpen()) {
            PasswdSafeUtil.dbginfo(TAG, "changeLocation loc: %s", location);
            if (!itsLocation.equals(location)) {
                changeOpenView(location, false);
            }
        }
    }

    /**
     * Use the file data
     */
    @Override
    public <RetT> RetT useFileData(PasswdFileDataUser<RetT> user)
    {
        return itsFileDataFrag.useFileData(user);
    }

    @Override
    public void editRecord(PasswdLocation location)
    {
        if (isFileOpen()) {
            PasswdSafeUtil.dbginfo(TAG, "editRecord loc: %s", location);
            doChangeView(ChangeMode.EDIT_RECORD,
                         PasswdSafeEditRecordFragment.newInstance(location),
                         (location != null) ? location.getRecord() : null);
        }
    }

    @Override
    public void deleteRecord(PasswdLocation location, String title)
    {
        PasswdSafeUtil.dbginfo(TAG, "deleteRecord loc: %s", location);
        Bundle confirmArgs = new Bundle();
        confirmArgs.putString(CONFIRM_ARG_ACTION,
                              ConfirmAction.DELETE_RECORD.name());
        confirmArgs.putParcelable(CONFIRM_ARG_LOCATION, location);
        ConfirmPromptDialog dialog = ConfirmPromptDialog.newInstance(
                getString(R.string.delete_record_msg, title), null,
                getString(R.string.delete), confirmArgs);
        dialog.show(getSupportFragmentManager(), "Delete record");
    }

    @Override
    public void filterSimilarRecords(final PasswdLocation location)
    {
        if (!isFileOpen() || !location.isRecord()) {
            return;
        }
        PasswdRecordFilter filter = itsFileDataFrag.useFileData(fileData -> {
            PasswdFileDataView dataView = itsFileDataFrag.getFileDataView();
            String recordUuid = location.getRecord();
            return dataView.createSimilarRecordFilter(recordUuid, fileData);
        });

        setRecordFilter(filter);
    }

    /**
     * Update the view for opening a file
     */
    @Override
    public void updateViewFileOpen()
    {
        doUpdateView(ViewMode.FILE_OPEN, new PasswdLocation());
    }

    /**
     * Update the view for creating a new file
     */
    @Override
    public void updateViewFileNew()
    {
        doUpdateView(ViewMode.FILE_NEW, new PasswdLocation());
    }

    /**
     * Update the view for a list of records
     */
    @Override
    public void updateViewList(PasswdLocation location)
    {
        doUpdateView(ViewMode.VIEW_LIST, location);
    }

    /**
     * Update the view for a record
     */
    @Override
    public void updateViewRecord(PasswdLocation location)
    {
        doUpdateView(ViewMode.VIEW_RECORD, location);
    }

    /** Update the view for editing a record */
    @Override
    public void updateViewEditRecord(PasswdLocation location)
    {
        doUpdateView(ViewMode.EDIT_RECORD, location);
    }

    @Override
    public boolean activityHasMenu()
    {
        return true;
    }

    @Override
    public boolean isNavDrawerClosed()
    {
        return !itsNavDrawerFrag.isDrawerOpen();
    }

    @Override
    public void finishEditRecord(EditRecordResult result)
    {
        finishEdit(result.itsIsNewRecord ?
                   EditFinish.ADD_RECORD :
                   (result.itsIsSave ?
                    EditFinish.EDIT_SAVE_RECORD :
                    EditFinish.EDIT_NOSAVE_RECORD),
                   null, result.itsNewLocation, null);
    }

    @Override
    public void updateViewAbout()
    {
        doUpdateView(ViewMode.VIEW_ABOUT, itsLocation);
    }

    @Override
    public void finishChangePassword()
    {
        itsFileDataFrag.useFileData((PasswdFileDataUser<Void>)fileData -> {
            SavedPasswordsMgr savedMgr = new SavedPasswordsMgr(PasswdSafe.this);
            savedMgr.removeSavedPassword(fileData.getUri());
            return null;
        });

        finishEdit(EditFinish.CHANGE_PASSWORD, null, null, null);
    }

    @Override
    public void updateViewChangingPassword()
    {
        doUpdateView(ViewMode.CHANGING_PASSWORD, itsLocation);
    }

    @Override
    public void updateViewExpirations()
    {
        doUpdateView(ViewMode.VIEW_EXPIRATION, itsLocation);
    }

    @Override
    public void setRecordExpiryFilter(PasswdExpiryFilter filter,
                                      Date customDate)
    {
        PasswdRecordFilter recFilter =
                new PasswdRecordFilter(filter, customDate);
        setRecordFilter(recFilter);
    }

    @Override
    public void updateViewPolicyList()
    {
        doUpdateView(ViewMode.VIEW_POLICY_LIST, itsLocation);
    }

    @Override
    public void finishPolicyEdit(Runnable postSaveRun)
    {
        finishEdit(EditFinish.POLICY_EDIT, null, null, postSaveRun);
    }

    @Override
    public void updateViewPreferences()
    {
        doUpdateView(ViewMode.VIEW_PREFERENCES, itsLocation);
    }

    @Override
    public void promptConfirmed(Bundle confirmArgs)
    {
        PasswdSafeUtil.dbginfo(TAG, "promptConfirmed: %s", confirmArgs);
        ConfirmAction action;
        try {
            action = ConfirmAction.valueOf(
                    confirmArgs.getString(CONFIRM_ARG_ACTION));
        } catch (Exception e) {
            return;
        }

        switch (action) {
        case COPY_PASSWORD: {
            SharedPreferences prefs = Preferences.getSharedPrefs(this);
            Preferences.setCopyPasswordConfirm(true, prefs);
            copyField(CopyField.PASSWORD,
                      confirmArgs.getString(CONFIRM_ARG_RECORD));
            break;
        }
        case DELETE_FILE: {
            PasswdFileUri uri = itsFileDataFrag.useFileData(
                    PasswdFileData::getUri);
            if (uri != null) {
                itsTasks.startTask(new DeleteTask(uri, this));
            }
            break;
        }
        case DELETE_RECORD: {
            final PasswdLocation location =
                    confirmArgs.getParcelable(CONFIRM_ARG_LOCATION);
            if (location == null) {
                break;
            }

            Boolean removed = itsFileDataFrag.useFileData(fileData -> {
                PwsRecord rec = fileData.getRecord(location.getRecord());
                if (rec != null) {
                    return fileData.removeRecord(
                            rec, new ActContext(PasswdSafe.this));
                }
                return null;
            });
            if ((removed != null) && removed) {
                finishEdit(EditFinish.DELETE_RECORD, location.getRecord(),
                           location.selectRecord(null), null);
            }
            break;
        }
        case SHARE_FILE: {
            shareFile();
            break;
        }
        case SHOW_ENABLE_KEYBOARD: {
            try {
                startActivity(
                        new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS));
            } catch (ActivityNotFoundException e) {
                PasswdSafeUtil.showError("Keyboard settings not found", TAG, e,
                                         new ActContext(this));
            }
            break;
        }
        }
    }

    @Override
    public void promptCanceled()
    {
    }

    /**
     * Set the record filter from a query string
     */
    private void setRecordQueryFilter(String query)
    {
        PasswdFileDataView fileView = itsFileDataFrag.getFileDataView();
        PasswdRecordFilter filter;
        try {
            filter = fileView.createRecordFilter(query);
        } catch (Exception e) {
            PasswdSafeUtil.showError(e.getMessage(), TAG, e,
                                     new ActContext(this));
            return;
        }
        setRecordFilter(filter);
    }

    /**
     * Set the record filter
     */
    private void setRecordFilter(PasswdRecordFilter filter)
    {
        PasswdFileDataView fileView = itsFileDataFrag.getFileDataView();
        fileView.setRecordFilter(filter);
        itsFileDataFrag.refreshFileData();
        if (filter != null) {
            itsQuery.setText(getString(R.string.query_label,
                                       filter.toString(this)));
            GuiUtils.setVisible(itsQuery, true);
            collapseSearch();
        } else {
            GuiUtils.setVisible(itsQuery, false);
        }
        changeOpenView(new PasswdLocation(), true);
    }

    /**
     * Collapse the search view
     */
    private void collapseSearch()
    {
        if ((itsSearchItem != null) && itsSearchItem.isActionViewExpanded()) {
            itsSearchItem.collapseActionView();
        }
        invalidateOptionsMenu();
    }

    /**
     * Is there an open file
     */
    private boolean isFileOpen()
    {
        Boolean isOpen = itsFileDataFrag.useFileData(fileData -> true);
        return (isOpen != null) && isOpen;
    }

    /**
     * Protect or unprotect all records under the current group
     */
    private void protectRecords(final boolean doProtect)
    {
        Boolean doSave = itsFileDataFrag.useFileData(fileData -> {
            itsFileDataFrag.getFileDataView().walkGroupRecords(recordUuid -> {
                PwsRecord rec = fileData.getRecord(recordUuid);
                if (rec != null) {
                    fileData.setProtected(doProtect, rec);
                }
            });
            return true;
        });
        if ((doSave != null) && doSave) {
            finishEdit(EditFinish.PROTECT_RECORD, null, null, null);
        }
    }

    /**
     * Share the file
     */
    private void shareFile()
    {
        Pair<String, String> rc = itsFileDataFrag.useFileData((fileData) -> {
            PasswdFileUri uri = fileData.getUri();
            return new Pair<>(uri.getIdentifier(PasswdSafe.this, true),
                              uri.getFileName());
        });
        String fileId = TextUtils.isEmpty(rc.first) ?
                        "PasswdSafe file" : rc.first;
        String fileName = TextUtils.isEmpty(rc.second) ?
                          "share.psafe3" : rc.second;

        PasswdSafeUtil.dbginfo(TAG, "share: " + fileId);
        try {
            itsTasks.startTask(new ShareTask(fileId, fileName, this));
        } catch (Exception e) {
            PasswdSafeUtil.showError("Error sharing", TAG, e,
                                     new ActContext(this));
        }
    }

    /**
     * Finish editing the file
     */
    private void finishEdit(EditFinish task,
                            String popTag,
                            PasswdLocation newLocation,
                            Runnable postSaveRun)
    {
        FinishSaveInfo saveInfo = new FinishSaveInfo(task, popTag,
                                                     newLocation, postSaveRun);
        if (saveInfo.itsIsSave) {
            String fileId = itsFileDataFrag.useFileData(
                    fileData -> fileData.getUri().getIdentifier(PasswdSafe.this,
                                                                false));
            if (fileId == null) {
                fileId = "";
            }
            itsTasks.startTask(new SaveTask(fileId, saveInfo, this));
        } else {
            editFinished(saveInfo);
        }
    }

    /**
     * Handle when an edit is finished
     */
    private void editFinished(FinishSaveInfo saveState)
    {
        if (saveState.itsIsSave) {
            itsFileDataFrag.refreshFileData();
        }

        FragmentManager fragMgr = getSupportFragmentManager();
        if (saveState.itsIsPopBack) {
            fragMgr.popBackStackImmediate();

            if (saveState.itsPopTag != null) {
                //noinspection StatementWithEmptyBody
                while(fragMgr.popBackStackImmediate(
                        saveState.itsPopTag,
                        FragmentManager.POP_BACK_STACK_INCLUSIVE)) {
                    // Pop all fragments up to the first use of the given tag
                }
            }
        }

        if (saveState.itsIsAddRecord) {
            if (itsIsTwoPane) {
                changeOpenView(saveState.itsNewLocation, false);
            } else {
                Fragment contentsFrag = fragMgr.findFragmentById(R.id.content);
                if (contentsFrag instanceof PasswdSafeListFragment) {
                    ((PasswdSafeListFragment)contentsFrag).updateSelection(
                            saveState.itsNewLocation);
                }
            }
        } else if (saveState.shouldResetLoc(itsFileDataFrag.getFileDataView(),
                                            itsLocation)) {
            changeOpenView(new PasswdLocation(), true);
        }

        if (saveState.itsPostSaveRun != null) {
            saveState.itsPostSaveRun.run();
        }
    }

    /**
     * Change the initial view
     */
    private void changeInitialView()
    {
        doChangeView(ChangeMode.INIT, null);
    }

    /**
     * Change the view for opening a file
     */
    private void changeFileOpenView(Intent intent)
    {
        Uri openUri = PasswdSafeApp.getOpenUriFromIntent(intent);
        Uri intentUri = intent.getData();
        String recToOpen = (intentUri != null) ?
                           intentUri.getQueryParameter("recToOpen") : null;
        Fragment openFrag = PasswdSafeOpenFileFragment.newInstance(openUri,
                                                                   recToOpen);
        doChangeView(ChangeMode.FILE_OPEN, openFrag);
    }

    /**
     * Change the view for creating a new file
     */
    private void changeFileNewView(Intent intent)
    {
        Uri newUri = PasswdSafeApp.getOpenUriFromIntent(intent);
        Fragment newFrag = PasswdSafeNewFileFragment.newInstance(newUri);
        doChangeView(ChangeMode.FILE_NEW, newFrag);
    }

    /**
     * Change the view for an open file
     */
    private void changeOpenView(PasswdLocation location, boolean initial)
    {
        Fragment viewFrag;
        ChangeMode viewMode;
        if (location.isRecord()) {
            viewFrag = PasswdSafeRecordFragment.newInstance(location);
            viewMode = ChangeMode.RECORD;
        } else {
            viewFrag = PasswdSafeListFragment.newInstance(location, true);
            viewMode = initial ? ChangeMode.OPEN_INIT : ChangeMode.OPEN;
        }
        doChangeView(viewMode, viewFrag, location.getRecord());
    }

    /**
     * Change the view of the activity
     */
    private void doChangeView(ChangeMode mode, Fragment contentFrag)
    {
        doChangeView(mode, contentFrag,  null);
    }

    /**
     * Change the view of the activity with an optional identifying tag for the
     * transition
     */
    private void doChangeView(final ChangeMode mode,
                              final Fragment contentFrag,
                              final String transTag)
    {
        checkNavigation(true, () -> {
            FragmentManager fragMgr = getSupportFragmentManager();
            FragmentTransaction txn = fragMgr.beginTransaction();

            boolean clearBackStack = false;
            boolean supportsBack = false;
            switch (mode) {
            case INIT:
            case FILE_OPEN:
            case FILE_NEW:
            case OPEN_INIT: {
                clearBackStack = true;
                break;
            }
            case OPEN:
            case RECORD:
            case EDIT_RECORD:
            case CHANGE_PASSWORD:
            case VIEW_ABOUT:
            case VIEW_EXPIRATION:
            case VIEW_POLICY_LIST:
            case VIEW_PREFERENCES: {
                supportsBack = true;
                break;
            }
            }
            if (clearBackStack) {
                //noinspection StatementWithEmptyBody
                while (fragMgr.popBackStackImmediate()) {
                    // Clear back stack
                }
            }

            txn.setTransitionStyle(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            if (contentFrag != null) {
                txn.replace(R.id.content, contentFrag);
            } else {
                Fragment currFrag = fragMgr.findFragmentById(R.id.content);
                if ((currFrag != null) && currFrag.isAdded()) {
                    txn.remove(currFrag);
                }
            }

            if (supportsBack) {
                txn.addToBackStack(transTag);
            }

            txn.commit();
            itsIsConfirmBackClosed = true;
        });
    }

    /**
     * Check whether to confirm before performing a navigation change
     */
    private void checkNavigation(final boolean popOnConfirm,
                                 final Runnable navRun)
    {
        boolean doPrompt = false;
        switch (itsCurrViewMode) {
        case EDIT_RECORD: {
            doPrompt = true;
            break;
        }
        case INIT:
        case FILE_OPEN:
        case FILE_NEW:
        case VIEW_LIST:
        case VIEW_RECORD:
        case CHANGING_PASSWORD:
        case VIEW_ABOUT:
        case VIEW_EXPIRATION:
        case VIEW_POLICY_LIST:
        case VIEW_PREFERENCES: {
            break;
        }
        }

        if (doPrompt) {
            DialogInterface.OnClickListener listener = (dialog, which) -> {
                dialog.dismiss();
                if (popOnConfirm) {
                    FragmentManager fragMgr = getSupportFragmentManager();
                    fragMgr.popBackStackImmediate();
                }
                navRun.run();
            };
            new AlertDialog.Builder(this)
                    .setTitle(R.string.continue_p)
                    .setMessage(R.string.any_changes_will_be_lost)
                    .setPositiveButton(R.string.continue_str, listener)
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        } else {
            navRun.run();
        }
    }

    /**
     * Update the view mode
     */
    private void doUpdateView(ViewMode mode, @NonNull PasswdLocation location)
    {
        PasswdSafeUtil.dbginfo(TAG, "doUpdateView: mode: %s, loc: %s",
                               mode, location);

        itsLocation = location;
        itsFileDataFrag.setLocation(itsLocation);
        itsCurrViewMode = mode;

        FragmentManager fragMgr = getSupportFragmentManager();
        boolean showLeftList = false;
        boolean queryVisibleForMode = false;
        PasswdSafeNavDrawerFragment.Mode drawerMode =
                PasswdSafeNavDrawerFragment.Mode.INIT;
        boolean fileTimeoutPaused = true;
        String fileNameUpdate = null;
        switch (mode) {
        case INIT:
        case FILE_OPEN:
        case FILE_NEW: {
            itsTitle = PasswdSafeApp.getAppTitle(null, this);
            fileNameUpdate = "";
            break;
        }
        case VIEW_LIST: {
            showLeftList = true;
            queryVisibleForMode = true;
            drawerMode = PasswdSafeNavDrawerFragment.Mode.RECORDS_LIST;
            fileTimeoutPaused = false;
            itsTitle = null;
            String groups = itsLocation.getGroupPath();
            if (TextUtils.isEmpty(groups)) {
                fileNameUpdate = itsFileDataFrag.useFileData(fileData -> {
                    PasswdFileUri uri = fileData.getUri();
                    itsTitle = PasswdSafeApp.getAppFileTitle(uri,
                                                             PasswdSafe.this);
                    return uri.getIdentifier(PasswdSafe.this, true);
                });
            }
            if (itsTitle == null) {
                itsTitle = PasswdSafeApp.getAppTitle(groups, this);
            }

            Fragment contentsFrag = fragMgr.findFragmentById(R.id.content);
            if (contentsFrag instanceof PasswdSafeListFragment) {
                PasswdSafeListFragment.Mode contentsMode = itsIsTwoPane ?
                        PasswdSafeListFragment.Mode.RECORDS :
                        PasswdSafeListFragment.Mode.ALL;
                ((PasswdSafeListFragment)contentsFrag).updateLocationView(
                        itsLocation, contentsMode);
            }
            break;
        }
        case VIEW_RECORD: {
            showLeftList = true;
            drawerMode = itsIsTwoPane ?
                    PasswdSafeNavDrawerFragment.Mode.RECORDS_LIST :
                    PasswdSafeNavDrawerFragment.Mode.RECORDS_SINGLE;
            fileTimeoutPaused = false;
            itsTitle = itsFileDataFrag.useFileData(fileData -> {
                if (itsLocation.isRecord()) {
                    PwsRecord rec = fileData.getRecord(itsLocation.getRecord());
                    if (rec != null) {
                        return fileData.getTitle(rec);
                    }
                }
                return null;
            });
            if (itsTitle == null) {
                itsTitle = getString(R.string.new_entry);
            }
            break;
        }
        case EDIT_RECORD: {
            drawerMode = PasswdSafeNavDrawerFragment.Mode.RECORDS_ACTION;
            itsTitle = itsFileDataFrag.useFileData(fileData -> {
                if (itsLocation.isRecord()) {
                    PwsRecord rec = fileData.getRecord(itsLocation.getRecord());
                    if (rec != null) {
                        return getString(R.string.edit_item,
                                         fileData.getTitle(rec));
                    }
                }
                return null;
            });
            if (itsTitle == null) {
                itsTitle = getString(R.string.new_entry);
            }
            break;
        }
        case CHANGING_PASSWORD: {
            itsTitle = getString(R.string.change_password);
            drawerMode = PasswdSafeNavDrawerFragment.Mode.RECORDS_ACTION;
            break;
        }
        case VIEW_ABOUT: {
            drawerMode = PasswdSafeNavDrawerFragment.Mode.ABOUT;
            fileTimeoutPaused = false;
            itsTitle = PasswdSafeApp.getAppTitle(getString(R.string.about),
                                                 this);
            break;
        }
        case VIEW_EXPIRATION: {
            drawerMode = PasswdSafeNavDrawerFragment.Mode.EXPIRATIONS;
            fileTimeoutPaused = false;
            itsTitle = PasswdSafeApp.getAppTitle(
                    getString(R.string.password_expiration), this);
            break;
        }
        case VIEW_POLICY_LIST: {
            drawerMode = PasswdSafeNavDrawerFragment.Mode.POLICIES;
            fileTimeoutPaused = false;
            itsTitle = PasswdSafeApp.getAppTitle(
                    getString(R.string.password_policies), this);
            break;
        }
        case VIEW_PREFERENCES: {
            drawerMode = PasswdSafeNavDrawerFragment.Mode.PREFERENCES;
            fileTimeoutPaused = false;
            itsTitle = PasswdSafeApp.getAppTitle(
                    getString(R.string.preferences), this);
            break;
        }
        }

        GuiUtils.invalidateOptionsMenu(this);
        boolean fileOpen = isFileOpen();
        itsNavDrawerFrag.updateView(drawerMode, fileNameUpdate, fileOpen);
        restoreActionBar();
        itsTimeoutReceiver.updateTimeout(fileTimeoutPaused);

        PasswdFileDataView fileDataView = itsFileDataFrag.getFileDataView();
        GuiUtils.setVisible(itsQueryPanel,
                            queryVisibleForMode &&
                            (fileDataView.getRecordFilter() != null));

        if (fileOpen) {
            if (fileDataView.checkExpiryChanged()) {
                GuiUtils.setVisible(itsExpiryPanel,
                                    fileDataView.hasExpiredRecords());
                itsExpiry.setText(fileDataView.getExpiredRecordsStr(this));
            }
        } else {
            GuiUtils.setVisible(itsExpiryPanel, false);
        }

        if (itsIsTwoPane) {
            PasswdSafeListFragment.Mode listMode = itsLocation.isRecord() ?
                    PasswdSafeListFragment.Mode.ALL :
                    PasswdSafeListFragment.Mode.GROUPS;
            Fragment listFrag = fragMgr.findFragmentById(R.id.content_list);
            if (listFrag instanceof PasswdSafeListFragment) {
                ((PasswdSafeListFragment)listFrag).updateLocationView(
                        itsLocation, listMode);
            }

            if (listFrag != null) {
                FragmentTransaction txn = fragMgr.beginTransaction();
                if (showLeftList) {
                    txn.setTransitionStyle(
                            FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                    txn.show(listFrag);
                } else {
                    txn.setTransitionStyle(
                            FragmentTransaction.TRANSIT_FRAGMENT_CLOSE);
                    txn.hide(listFrag);
                }
                txn.commit();
            }
        }
    }

    /**
     * Restore the action bar from the nav drawer
     */
    private void restoreActionBar()
    {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setTitle(itsTitle);
        }
    }

    /**
     * Information for finishing the save of the file
     */
    private static final class FinishSaveInfo
    {
        private boolean itsIsAddRecord;
        private boolean itsIsSave;
        private boolean itsIsPopBack;
        private final String itsPopTag;
        private final PasswdLocation itsNewLocation;
        private final Runnable itsPostSaveRun;

        /**
         * Constructor
         */
        public FinishSaveInfo(EditFinish task,
                              String popTag,
                              PasswdLocation newLocation,
                              Runnable postSaveRun)
        {
            switch (task) {
            case ADD_RECORD: {
                itsIsAddRecord = true;
                itsIsSave = true;
                itsIsPopBack = true;
                break;
            }
            case CHANGE_PASSWORD:
            case DELETE_RECORD:
            case EDIT_SAVE_RECORD: {
                itsIsAddRecord = false;
                itsIsSave = true;
                itsIsPopBack = true;
                break;
            }
            case EDIT_NOSAVE_RECORD: {
                itsIsAddRecord = false;
                itsIsSave = false;
                itsIsPopBack = true;
                break;
            }
            case POLICY_EDIT:
            case PROTECT_RECORD: {
                itsIsAddRecord = false;
                itsIsSave = true;
                itsIsPopBack = false;
                break;
            }
            }
            itsPopTag = popTag;
            itsNewLocation = newLocation;
            itsPostSaveRun = postSaveRun;
        }

        /**
         * Should the location be reset
         */
        public boolean shouldResetLoc(PasswdFileDataView dataView,
                                      PasswdLocation currLocation)
        {
            //noinspection SimplifiableIfStatement
            if (!itsIsSave || (itsNewLocation == null)) {
                return false;
            }

            return dataView.isGroupingRecords() &&
                   (!itsNewLocation.equalGroups(currLocation) ||
                    !dataView.hasGroup(itsNewLocation.getRecordGroup()));
        }
    }

    /**
     * Task to save a file in the background
     */
    private static final class SaveTask extends AbstractTask
    {
        private final @NonNull FinishSaveInfo itsSaveInfo;
        private final PasswdSafeFileDataFragment itsFileDataFrag;

        /**
         * Constructor
         */
        public SaveTask(String fileId,
                        @NonNull FinishSaveInfo saveInfo,
                        PasswdSafe act)
        {
            super(act.getString(R.string.saving_file, fileId), act);
            itsSaveInfo = saveInfo;
            itsFileDataFrag = act.itsFileDataFrag;
        }

        @Override
        protected Boolean doInBackground() throws Throwable
        {
            Exception e = itsFileDataFrag.useFileData(fileData -> {
                try {
                    fileData.save(getContext());
                    PasswdSafeUtil.dbginfo(TAG, "SaveTask finished");
                    return null;
                } catch (Exception e1) {
                    return e1;
                }
            });
            if (e != null) {
                throw e;
            }
            return true;
        }

        @Override
        protected void onTaskFinished(Boolean result, Throwable error,
                                      @NonNull PasswdSafe act)
        {
            super.onTaskFinished(result, error, act);
            if (result != null) {
                act.editFinished(itsSaveInfo);
            } else if (error != null) {
                String msg = error.toString();
                if ((error instanceof IOException) &&
                    (ApiCompat.SDK_VERSION >= ApiCompat.SDK_KITKAT)) {
                    msg = act.getString(R.string.kitkat_sdcard_warning, msg);
                }
                PasswdSafeUtil.showFatalMsg(error, msg, act);
            }
        }
    }

    /**
     * Task to share a file
     */
    private static final class ShareTask extends AbstractTask
    {
        private final String itsFileId;
        private final FileSharer itsSharer;
        private final PasswdSafeFileDataFragment itsFileDataFrag;

        /**
         * Constructor
         */
        public ShareTask(String fileId, String fileName, PasswdSafe act)
                throws IOException
        {
            super(act.getString(R.string.sharing_file, fileId), act);
            itsFileId = fileId;
            itsSharer = new FileSharer(fileName, getContext(),
                                       PasswdSafeUtil.PACKAGE);
            itsFileDataFrag = act.itsFileDataFrag;
        }

        @Override
        protected Boolean doInBackground() throws Throwable
        {
            Exception e = itsFileDataFrag.useFileData((fileData) -> {
                try {
                    fileData.saveAs(itsSharer.getFile(), getContext());
                    PasswdSafeUtil.dbginfo(TAG, "ShareTask finished");
                    return null;
                } catch (Exception ex) {
                    return ex;
                }
            });
            if (e != null) {
                throw e;
            }
            return true;
        }

        @Override
        protected void onTaskFinished(Boolean result, Throwable error,
                                      @NonNull PasswdSafe act)
        {
            super.onTaskFinished(result, error, act);
            if (result != null) {
                try {
                    itsSharer.share(
                            act.getString(R.string.sharing_file, itsFileId),
                            PasswdSafeUtil.MIME_TYPE_PSAFE3, null,
                            itsFileId, act);
                } catch (Exception e) {
                    error = e;
                }
            }
            if (error != null) {
                PasswdSafeUtil.showError("Error sharing: " + error.getMessage(),
                                         TAG, error, new ActContext(act));
            }
        }
    }

    /**
     * Task to delete a file in the background
     */
    private static final class DeleteTask extends AbstractTask
    {
        private final PasswdFileUri itsFileUri;

        /**
         * Constructor
         */
        public DeleteTask(PasswdFileUri uri, PasswdSafe act)
        {
            super(act.getString(R.string.delete_file), act);
            itsFileUri = uri;
        }

        @Override
        protected Boolean doInBackground() throws Throwable
        {
            Context ctx = getContext();
            RecentFilesDb recentFilesDb = new RecentFilesDb(ctx);
            recentFilesDb.removeUri(itsFileUri.getUri());

            itsFileUri.delete(ctx);
            return true;
        }

        @Override
        protected void onTaskFinished(Boolean result, Throwable error,
                                      @NonNull PasswdSafe act)
        {
            super.onTaskFinished(result, error, act);
            if (result != null) {
                act.finish();
            } else if (error != null) {
                PasswdSafeUtil.showFatalMsg(error, "Error deleting file", act);
            }
        }
    }

    /**
     * Abstract task for background operations
     */
    private static abstract class AbstractTask
            extends ManagedTask<Boolean, PasswdSafe>
    {
        private final ProgressFragment itsProgressFrag;

        /**
         * Constructor
         */
        protected AbstractTask(String msg, PasswdSafe act)
        {
            super(act, act);
            itsProgressFrag = ProgressFragment.newInstance(msg);
        }

        @Override @CallSuper
        protected void onTaskStarted(@NonNull PasswdSafe act)
        {
            itsProgressFrag.show(act.getSupportFragmentManager(), null);
        }

        @Override @CallSuper
        protected void onTaskFinished(Boolean result,
                                      Throwable error,
                                      @NonNull PasswdSafe act)
        {
            act.itsTasks.taskFinished(this);
            itsProgressFrag.dismiss();
        }
    }
}
