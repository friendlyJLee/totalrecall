/*
This file is part of Keepass2Android, Copyright 2013 Philipp Crocoll. This file is based on Keepassdroid, Copyright Brian Pellin.

  Keepass2Android is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 2 of the License, or
  (at your option) any later version.

  Keepass2Android is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with Keepass2Android.  If not, see <http://www.gnu.org/licenses/>.
  */

using System;
using Android.Content;
using Android.OS;
using Android.Widget;

namespace keepass2android
{
	public abstract class OnFinish
	{
		protected bool Success;
		protected String Message;
		protected Exception Exception;
		
		protected OnFinish BaseOnFinish;
		protected Handler Handler;
		private ProgressDialogStatusLogger _statusLogger = new ProgressDialogStatusLogger(); //default: no logging but not null -> can be used whenever desired
		

		public ProgressDialogStatusLogger StatusLogger
		{
			get { return _statusLogger; }
			set { _statusLogger = value; }
		}


		protected OnFinish(Handler handler) {
			BaseOnFinish = null;
			Handler = handler;
			
		}

		protected OnFinish(OnFinish finish, Handler handler) {
			BaseOnFinish = finish;
			Handler = handler;
		}

		protected OnFinish(OnFinish finish) {
			BaseOnFinish = finish;
			Handler = null;
		}

		public void SetResult(bool success, string message, Exception exception) {
			Success = success;
			Message = message;
			Exception = exception;
		}
		
		public void SetResult(bool success) {
			Success = success;
		}
		
		public virtual void Run() {
			if (BaseOnFinish == null) return;
			// Pass on result on call finish
			BaseOnFinish.SetResult(Success, Message, Exception);
				
			if ( Handler != null ) {
				Handler.Post(BaseOnFinish.Run); 
			} else {
				BaseOnFinish.Run();
			}
		}
		
		protected void DisplayMessage(Context ctx) {
			DisplayMessage(ctx, Message);
		}

		public static void DisplayMessage(Context ctx, string message)
		{
			if ( !String.IsNullOrEmpty(message) ) {
				Kp2aLog.Log("OnFinish message: "+message);
				Toast.MakeText(ctx, message, ToastLength.Long).Show();
			}
		}
	}
}

