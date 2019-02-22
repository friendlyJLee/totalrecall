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

using Android.App;
using Android.Content;
using Android.OS;
using Java.Lang;

namespace keepass2android
{
	/// <summary>
	/// Class to run a task while a progress dialog is shown
	/// </summary>
	public class ProgressTask {
		private readonly Handler _handler;
		private readonly RunnableOnFinish _task;
		private readonly IProgressDialog _progressDialog;
        private readonly IKp2aApp _app;
		private Thread _thread;

		public ProgressTask(IKp2aApp app, Context ctx, RunnableOnFinish task) {
			_task = task;
			_handler = app.UiThreadHandler;
            _app = app;
			
			// Show process dialog
			_progressDialog = app.CreateProgressDialog(ctx);
			_progressDialog.SetTitle(_app.GetResourceString(UiStringKey.progress_title));
			_progressDialog.SetMessage("Initializing...");
			
			// Set code to run when this is finished
			_task.OnFinishToRun = new AfterTask(task.OnFinishToRun, _handler, _progressDialog);
			_task.SetStatusLogger(new ProgressDialogStatusLogger(_app, _handler, _progressDialog));
			
			
		}
		
		public void Run() {
			// Show process dialog
			_progressDialog.Show();
			
			
			// Start Thread to Run task
			_thread = new Thread(_task.Run);
			_thread.Start();
			
		}

		public void JoinWorkerThread()
		{
			_thread.Join();
		}
		
		private class AfterTask : OnFinish {
			readonly IProgressDialog _progressDialog;

			public AfterTask (OnFinish finish, Handler handler, IProgressDialog pd): base(finish, handler)
			{
				_progressDialog = pd;
			}

			public override void Run() {
				base.Run();

				if (Handler != null) //can be null in tests
				{
					// Remove the progress dialog
					Handler.Post(delegate { _progressDialog.Dismiss(); });
				}
				else
				{
					_progressDialog.Dismiss();
				}

			}
			
		}
		
	}
}

