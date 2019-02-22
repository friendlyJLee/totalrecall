﻿using System;
using Android.App;
using Android.App.Assist;
using Android.Content;
using Android.OS;
using Android.Service.Autofill;
using Android.Support.V7.App;
using Android.Util;
using Android.Views.Autofill;
using Android.Widget;
using Java.Util;
using keepass2android.services.AutofillBase.model;
using System.Collections.Generic;
using System.Linq;

namespace keepass2android.services.AutofillBase
{

    public abstract class ChooseForAutofillActivityBase : AppCompatActivity
    {
        protected Intent ReplyIntent;


        public static string ExtraQueryString => "EXTRA_QUERY_STRING";
        public static string ExtraIsManualRequest => "EXTRA_IS_MANUAL_REQUEST";
        public static string ExtraAutoReturnFromQuery => "EXTRA_AUTO_RETURN_FROM_QUERY";

        public int RequestCodeQuery => 6245;

        protected override void OnCreate(Bundle savedInstanceState)
        {
            base.OnCreate(savedInstanceState);
            
            //if launched from history, don't re-use the task. Proceed to FileSelect instead.
            if (Intent.Flags.HasFlag(ActivityFlags.LaunchedFromHistory))
            {
                Kp2aLog.Log("Forwarding to FileSelect. QueryCredentialsActivity started from history.");
                RestartApp();
                return;
            }

            string requestedUrl = Intent.GetStringExtra(ExtraQueryString);
            if (requestedUrl == null)
            {
                Toast.MakeText(this, "Cannot execute query for null.", ToastLength.Long).Show();
                RestartApp();
                return;
            }

            var i = GetQueryIntent(requestedUrl, Intent.GetBooleanExtra(ExtraAutoReturnFromQuery, true));
            StartActivityForResult(i, RequestCodeQuery);
        }

        protected abstract Intent GetQueryIntent(string requestedUrl, bool autoReturnFromQuery);

        protected void RestartApp()
        {
            Intent intent = IntentBuilder.GetRestartAppIntent(this);
            StartActivity(intent);
            Finish();
        }

        
        public override void Finish()
        {
            if (ReplyIntent != null)
            {
                SetResult(Result.Ok, ReplyIntent);
            }
            else
            {
                SetResult(Result.Canceled);
            }
            base.Finish();
        }

        void OnFailure()
        {
            Log.Warn(CommonUtil.Tag, "Failed auth.");
            ReplyIntent = null;
        }

        protected void OnSuccess(FilledAutofillFieldCollection clientFormDataMap, bool isManual)
        {
            var intent = Intent;
            AssistStructure structure = (AssistStructure)intent.GetParcelableExtra(AutofillManager.ExtraAssistStructure);
            StructureParser parser = new StructureParser(this, structure);
            parser.ParseForFill(isManual);
            AutofillFieldMetadataCollection autofillFields = parser.AutofillFields;
            int partitionIndex = AutofillHintsHelper.GetPartitionIndex(autofillFields.FocusedAutofillCanonicalHints.FirstOrDefault());
            FilledAutofillFieldCollection partitionData = AutofillHintsHelper.FilterForPartition(clientFormDataMap, partitionIndex);
            ReplyIntent = new Intent();
            SetDatasetIntent(AutofillHelper.NewDataset(this, autofillFields, partitionData, IntentBuilder));
        }

        protected override void OnActivityResult(int requestCode, Result resultCode, Intent data)
        {
            base.OnActivityResult(requestCode, resultCode, data);

            if (requestCode == RequestCodeQuery)
            {
                if (resultCode == ExpectedActivityResult)
                    OnSuccess(GetDataset(data), Intent.GetBooleanExtra(ExtraIsManualRequest, false));
                else
                OnFailure(); 
                Finish();

            }

        }

        protected virtual Result ExpectedActivityResult
        {
            get { return Result.Ok; }
        }

        /// <summary>
        /// Creates the FilledAutofillFieldCollection from the intent returned from the query activity
        /// </summary>
        protected abstract FilledAutofillFieldCollection GetDataset(Intent data);

        public abstract IAutofillIntentBuilder IntentBuilder { get; }

        
        protected void SetResponseIntent(FillResponse fillResponse)
        {
            ReplyIntent.PutExtra(AutofillManager.ExtraAuthenticationResult, fillResponse);
        }

        protected void SetDatasetIntent(Dataset dataset)
        {
            ReplyIntent.PutExtra(AutofillManager.ExtraAuthenticationResult, dataset);
        }
    }
}