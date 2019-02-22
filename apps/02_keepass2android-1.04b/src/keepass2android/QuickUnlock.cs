/*
This file is part of Keepass2Android, Copyright 2013 Philipp Crocoll. 

  Keepass2Android is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  Keepass2Android is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with Keepass2Android.  If not, see <http://www.gnu.org/licenses/>.
  */

using System;
using Android;
using Android.App;
using Android.Content;
using Android.OS;
using Android.Views;
using Android.Widget;
using Android.Content.PM;
using KeePassLib.Keys;
using Android.Preferences;
using Android.Runtime;
using Android.Support.Design.Widget;
using Android.Views.InputMethods;
using KeePassLib.Serialization;

namespace keepass2android
{
	[Activity(Label = "@string/app_name", 
		ConfigurationChanges = ConfigChanges.Orientation,
		WindowSoftInputMode = SoftInput.AdjustResize,
		MainLauncher = false,
        Theme = "@style/MyTheme_Blue")]
	public class QuickUnlock : LifecycleDebugActivity, IFingerprintAuthCallback
	{
		private IOConnectionInfo _ioc;
		private QuickUnlockBroadcastReceiver _intentReceiver;
		private ActivityDesign _design;
		private bool _fingerprintPermissionGranted;
		private IFingerprintIdentifier _fingerprintIdentifier;
		private int _quickUnlockLength;
		private const int FingerprintPermissionRequestCode = 0;

		public QuickUnlock()
		{
			_design = new ActivityDesign(this);
		}

		protected override void OnCreate(Bundle bundle)
		{
			_design.ApplyTheme();
			base.OnCreate(bundle);
			
			//use FlagSecure to make sure the last (revealed) character of the password is not visible in recent apps
			if (PreferenceManager.GetDefaultSharedPreferences(this).GetBoolean(
				GetString(Resource.String.ViewDatabaseSecure_key), true))
			{
				Window.SetFlags(WindowManagerFlags.Secure, WindowManagerFlags.Secure);
			}

			_ioc = App.Kp2a.GetDb().Ioc;

			if (_ioc == null)
			{
				Finish();
				return;
			}

			SetContentView(Resource.Layout.QuickUnlock);

			var toolbar = FindViewById<Android.Support.V7.Widget.Toolbar>(Resource.Id.mytoolbar);

			SetSupportActionBar(toolbar);

			var collapsingToolbar = FindViewById<CollapsingToolbarLayout>(Resource.Id.collapsing_toolbar);
			collapsingToolbar.SetTitle(GetString(Resource.String.QuickUnlock_prefs));

			if (App.Kp2a.GetDb().KpDatabase.Name != "")
			{
				FindViewById(Resource.Id.filename_label).Visibility = ViewStates.Visible;
				((TextView) FindViewById(Resource.Id.filename_label)).Text = App.Kp2a.GetDb().KpDatabase.Name;
			}
			else
			{
				if (
					PreferenceManager.GetDefaultSharedPreferences(this)
					                 .GetBoolean(GetString(Resource.String.RememberRecentFiles_key),
					                             Resources.GetBoolean(Resource.Boolean.RememberRecentFiles_default)))
				{
					((TextView) FindViewById(Resource.Id.filename_label)).Text = App.Kp2a.GetFileStorage(_ioc).GetDisplayName(_ioc);
				}
				else
				{
					((TextView) FindViewById(Resource.Id.filename_label)).Text = "*****";
				}

			}


			TextView txtLabel = (TextView) FindViewById(Resource.Id.QuickUnlock_label);

			_quickUnlockLength = App.Kp2a.QuickUnlockKeyLength;

		    if (PreferenceManager.GetDefaultSharedPreferences(this)
		        .GetBoolean(GetString(Resource.String.QuickUnlockHideLength_key), false))
		    {
		        txtLabel.Text = GetString(Resource.String.QuickUnlock_label_secure);
            }
		    else
		    {
		        txtLabel.Text = GetString(Resource.String.QuickUnlock_label, new Java.Lang.Object[] { _quickUnlockLength });
            }
			

			EditText pwd = (EditText) FindViewById(Resource.Id.QuickUnlock_password);
			pwd.SetEms(_quickUnlockLength);
			Util.MoveBottomBarButtons(Resource.Id.QuickUnlock_buttonLock, Resource.Id.QuickUnlock_button, Resource.Id.bottom_bar, this);

			Button btnUnlock = (Button) FindViewById(Resource.Id.QuickUnlock_button);
			btnUnlock.Click += (object sender, EventArgs e) =>
				{
					OnUnlock(_quickUnlockLength, pwd);
				};

		    

			Button btnLock = (Button) FindViewById(Resource.Id.QuickUnlock_buttonLock);
			btnLock.Text = btnLock.Text.Replace("�", "ss");
			btnLock.Click += (object sender, EventArgs e) =>
				{
					App.Kp2a.LockDatabase(false);
					Finish();
				};
			pwd.EditorAction += (sender, args) =>
				{
					if ((args.ActionId == ImeAction.Done) || ((args.ActionId == ImeAction.ImeNull) && (args.Event.Action == KeyEventActions.Down)))
						OnUnlock(_quickUnlockLength, pwd);
				};

			_intentReceiver = new QuickUnlockBroadcastReceiver(this);
			IntentFilter filter = new IntentFilter();
			filter.AddAction(Intents.DatabaseLocked);
			RegisterReceiver(_intentReceiver, filter);

			if ((int) Build.VERSION.SdkInt >= 23)
			{
				Kp2aLog.Log("requesting fingerprint permission");
				RequestPermissions(new[] { Manifest.Permission.UseFingerprint }, FingerprintPermissionRequestCode);
			}
			else
			{
				
			}

		}

		protected override void OnStart()
		{
			base.OnStart();
			DonateReminder.ShowDonateReminderIfAppropriate(this);
			
		}

		public override void	OnRequestPermissionsResult(int requestCode, string[] permissions, Permission[] grantResults)
		{
			Kp2aLog.Log("OnRequestPermissionsResult " + (requestCode == FingerprintPermissionRequestCode) +
			            ((grantResults.Length > 0) && (grantResults[0] == Permission.Granted)));
			
			if ((requestCode == FingerprintPermissionRequestCode) &&  (grantResults.Length > 0) && (grantResults[0] == Permission.Granted))
			{
				var btn = FindViewById<ImageButton>(Resource.Id.fingerprintbtn);
				btn.Click += (sender, args) =>
				{
					AlertDialog.Builder b = new AlertDialog.Builder(this);
					b.SetTitle(Resource.String.fingerprint_prefs);
					b.SetMessage(btn.Tag.ToString());
					b.SetPositiveButton(Android.Resource.String.Ok, (o, eventArgs) => ((Dialog)o).Dismiss());
					b.Show();
				};
				_fingerprintPermissionGranted = true;
				Kp2aLog.Log("_fingerprintPermissionGranted");
				if (_onResumeDone)
				{
					//it seems the permission result is called after onResume sometimes. Repeat fingerprint unlock then.
					InitFingerprintUnlock();	
				}
			}
		}

		bool _onResumeDone = false;

		public void OnFingerprintError(string message)
		{
			Kp2aLog.Log("fingerprint error: " + message);
			var btn = FindViewById<ImageButton>(Resource.Id.fingerprintbtn);

			btn.SetImageResource(Resource.Drawable.ic_fingerprint_error);
			btn.PostDelayed(() =>
			{
				btn.SetImageResource(Resource.Drawable.ic_fp_40px);
				btn.Tag = GetString(Resource.String.fingerprint_unlock_hint);
			}, 1300);
			Toast.MakeText(this, message, ToastLength.Long).Show();
		}

		public void OnFingerprintAuthSucceeded()
		{
			Kp2aLog.Log("OnFingerprintAuthSucceeded");
			_fingerprintIdentifier.StopListening();
			var btn = FindViewById<ImageButton>(Resource.Id.fingerprintbtn);

			btn.SetImageResource(Resource.Drawable.ic_fingerprint_success);

			EditText pwd = (EditText)FindViewById(Resource.Id.QuickUnlock_password);
			pwd.Text = ExpectedPasswordPart;
			
			btn.PostDelayed(() =>
			{
			
				App.Kp2a.UnlockDatabase();
				Finish();
			}, 500);


		}
		private bool InitFingerprintUnlock()
		{
			Kp2aLog.Log("InitFingerprintUnlock");

			if (_fingerprintIdentifier != null)
			{
				Kp2aLog.Log("Already listening for fingerprint!");
				return true;
			}


			var btn = FindViewById<ImageButton>(Resource.Id.fingerprintbtn);
			try
			{
				FingerprintUnlockMode um;
				Enum.TryParse(PreferenceManager.GetDefaultSharedPreferences(this).GetString(App.Kp2a.GetDb().CurrentFingerprintModePrefKey, ""), out um);
				btn.Visibility = (um != FingerprintUnlockMode.Disabled) ? ViewStates.Visible : ViewStates.Gone;

				if (um == FingerprintUnlockMode.Disabled)
				{
					_fingerprintIdentifier = null;
					return false;
				}

				if (_fingerprintPermissionGranted)
				{
					FingerprintModule fpModule = new FingerprintModule(this);
					Kp2aLog.Log("fpModule.FingerprintManager.IsHardwareDetected=" + fpModule.FingerprintManager.IsHardwareDetected);
					if (fpModule.FingerprintManager.IsHardwareDetected) //see FingerprintSetupActivity
						_fingerprintIdentifier = new FingerprintDecryption(fpModule, App.Kp2a.GetDb().CurrentFingerprintPrefKey, this,
							App.Kp2a.GetDb().CurrentFingerprintPrefKey);
				}
				if (_fingerprintIdentifier == null)
				{
					try
					{
						Kp2aLog.Log("trying Samsung Fingerprint API...");
						_fingerprintIdentifier = new FingerprintSamsungIdentifier(this);
						btn.Click += (sender, args) =>
						{
							if (_fingerprintIdentifier.Init())
								_fingerprintIdentifier.StartListening(this, this);
						};
						Kp2aLog.Log("trying Samsung Fingerprint API...Seems to work!");
					}
					catch (Exception)
					{
						Kp2aLog.Log("trying Samsung Fingerprint API...failed.");
						FindViewById<ImageButton>(Resource.Id.fingerprintbtn).Visibility = ViewStates.Gone;
						_fingerprintIdentifier = null;
						return false;	
					}
				}
				btn.Tag = GetString(Resource.String.fingerprint_unlock_hint);

				if (_fingerprintIdentifier.Init())
				{
					Kp2aLog.Log("successfully initialized fingerprint.");
					btn.SetImageResource(Resource.Drawable.ic_fp_40px);
					_fingerprintIdentifier.StartListening(this, this);
					return true;
				}
				else
				{
					Kp2aLog.Log("failed to initialize fingerprint.");
					HandleFingerprintKeyInvalidated();
				}
			}
			catch (Exception e)
			{
				Kp2aLog.Log("Error initializing Fingerprint Unlock: " + e);
				btn.SetImageResource(Resource.Drawable.ic_fingerprint_error);
				btn.Tag = "Error initializing Fingerprint Unlock: " + e;

				_fingerprintIdentifier = null;
			}
			return false;

		}

		private void HandleFingerprintKeyInvalidated()
		{
			var btn = FindViewById<ImageButton>(Resource.Id.fingerprintbtn);
//key invalidated permanently
			btn.SetImageResource(Resource.Drawable.ic_fingerprint_error);
		    btn.Tag = GetString(Resource.String.fingerprint_unlock_failed) + " " + GetString(Resource.String.fingerprint_reenable2);
            _fingerprintIdentifier = null;
		}

		private void ClearFingerprintUnlockData()
		{
			ISharedPreferencesEditor edit = PreferenceManager.GetDefaultSharedPreferences(this).Edit();
			edit.PutString(App.Kp2a.GetDb().CurrentFingerprintPrefKey, "");
			edit.PutString(App.Kp2a.GetDb().CurrentFingerprintModePrefKey, FingerprintUnlockMode.Disabled.ToString());
			edit.Commit();
		}

		private void OnUnlock(int quickUnlockLength, EditText pwd)
		{
			var expectedPasswordPart = ExpectedPasswordPart;
			if (pwd.Text == expectedPasswordPart)
			{
				Kp2aLog.Log("QuickUnlock successful!");
				App.Kp2a.UnlockDatabase();
			}
			else
			{
				Kp2aLog.Log("QuickUnlock not successful!");
				App.Kp2a.LockDatabase(false);
				Toast.MakeText(this, GetString(Resource.String.QuickUnlock_fail), ToastLength.Long).Show();
			}
			Finish();
		}

		private string ExpectedPasswordPart
		{
			get
			{
				KcpPassword kcpPassword = (KcpPassword) App.Kp2a.GetDb().KpDatabase.MasterKey.GetUserKey(typeof (KcpPassword));
				String password = kcpPassword.Password.ReadString();

			    var passwordStringInfo = new System.Globalization.StringInfo(password);

			    int passwordLength = passwordStringInfo.LengthInTextElements;
                
                String expectedPasswordPart = passwordStringInfo.SubstringByTextElements(Math.Max(0, passwordLength - _quickUnlockLength),
                    Math.Min(passwordLength, _quickUnlockLength));
				return expectedPasswordPart;
			}
		}

		private void OnLockDatabase()
		{
			CheckIfUnloaded();
		}

		protected override void OnResume()
		{
			base.OnResume();
			_design.ReapplyTheme();
			
			CheckIfUnloaded();


			bool showKeyboard = ((!InitFingerprintUnlock()) || (Util.GetShowKeyboardDuringFingerprintUnlock(this)));			

			EditText pwd = (EditText)FindViewById(Resource.Id.QuickUnlock_password);
			pwd.PostDelayed(() =>
			{
				InputMethodManager keyboard = (InputMethodManager)GetSystemService(Context.InputMethodService);
				if (showKeyboard)
					keyboard.ShowSoftInput(pwd, 0);
				else
					keyboard.HideSoftInputFromWindow(pwd.WindowToken, HideSoftInputFlags.ImplicitOnly);
			}, 50);

			_onResumeDone = true;
			


			
			
			
		}

		

		protected override void OnPause()
		{
			if (_fingerprintIdentifier != null)
			{
				Kp2aLog.Log("FP: Stop listening");
				_fingerprintIdentifier.StopListening();
				_fingerprintIdentifier = null;
			}

			base.OnPause();
		}

		protected override void OnDestroy()
		{
			base.OnDestroy();
			try
			{
				UnregisterReceiver(_intentReceiver);
			}
			catch (Exception e)
			{
				Kp2aLog.LogUnexpectedError(e);
			}
			
		}

		private void CheckIfUnloaded()
		{
			if ((App.Kp2a.GetDb() == null) || (App.Kp2a.GetDb().Loaded == false))
			{
				Finish();
			}
		}

		public override void OnBackPressed()
		{
			SetResult(KeePass.ExitClose);
			base.OnBackPressed();
		}

		private class QuickUnlockBroadcastReceiver : BroadcastReceiver
		{
			readonly QuickUnlock _activity;
			public QuickUnlockBroadcastReceiver(QuickUnlock activity)
			{
				_activity = activity;
			}

			public override void OnReceive(Context context, Intent intent)
			{
				switch (intent.Action)
				{
					case Intents.DatabaseLocked:
						_activity.OnLockDatabase();
						break;
				}
			}
		}


	}
}

