/*
  KeePass Password Safe - The Open-Source Password Manager
  Copyright (C) 2003-2013 Dominik Reichl <dominik.reichl@t-online.de>
  
  Modified to be used with Mono for Android. Changes Copyright (C) 2013 Philipp Crocoll

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 2 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software
  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
*/

using System;
using System.Collections.Generic;
using System.Text;
using System.IO;
using System.Net;
using System.Diagnostics;

#if (!KeePassLibSD && !KeePassRT)
using System.Net.Cache;
using System.Net.Security;
#endif

#if !KeePassRT
using System.Security.Cryptography.X509Certificates;
#endif

using KeePassLib.Native;
using KeePassLib.Utility;
using keepass2android;

namespace KeePassLib.Serialization
{
#if (!KeePassLibSD && !KeePassRT)
	public sealed class IOWebClient : WebClient
	{
		protected override WebRequest GetWebRequest(Uri address)
		{
			WebRequest request = base.GetWebRequest(address);
			IOConnection.ConfigureWebRequest(request);
			return request;
		}
	}
#endif

	public static class IOConnection
	{
#if (!KeePassLibSD && !KeePassRT)
		private static ProxyServerType m_pstProxyType = ProxyServerType.System;
		private static string m_strProxyAddr = string.Empty;
		private static string m_strProxyPort = string.Empty;
		private static string m_strProxyUserName = string.Empty;
		private static string m_strProxyPassword = string.Empty;

		private static bool m_bSslCertsAcceptInvalid = false;
		internal static bool SslCertsAcceptInvalid
		{
			// get { return m_bSslCertsAcceptInvalid; }
			set { m_bSslCertsAcceptInvalid = value; }
		}

		public static RemoteCertificateValidationCallback CertificateValidationCallback { get; set; }
#endif

		// Web request methods
		public const string WrmDeleteFile = "DELETEFILE";
		public const string WrmMoveFile = "MOVEFILE";

		// Web request headers
		public const string WrhMoveFileTo = "MoveFileTo";

		public static event EventHandler<IOAccessEventArgs> IOAccessPre;

#if (!KeePassLibSD && !KeePassRT)
		// Allow self-signed certificates, expired certificates, etc.
		private static bool AcceptCertificate(object sender,
			X509Certificate certificate, X509Chain chain,
			SslPolicyErrors sslPolicyErrors)
		{
			return true;
		}

		internal static void SetProxy(ProxyServerType pst, string strAddr,
			string strPort, string strUserName, string strPassword)
		{
			m_pstProxyType = pst;
			m_strProxyAddr = (strAddr ?? string.Empty);
			m_strProxyPort = (strPort ?? string.Empty);
			m_strProxyUserName = (strUserName ?? string.Empty);
			m_strProxyPassword = (strPassword ?? string.Empty);
		}

		internal static void ConfigureWebRequest(WebRequest request)
		{
			if(request == null) { Debug.Assert(false); return; } // No throw

			// WebDAV support
			if(request is HttpWebRequest)
			{
				request.PreAuthenticate = true; // Also auth GET
				if(request.Method == WebRequestMethods.Http.Post)
					request.Method = WebRequestMethods.Http.Put;
			}
			// else if(request is FtpWebRequest)
			// {
			//	Debug.Assert(((FtpWebRequest)request).UsePassive);
			// }

			// Not implemented and ignored in Mono < 2.10
			try
			{
				//deactivated. No longer supported in Mono 4.8? 
				//request.CachePolicy = new RequestCachePolicy(RequestCacheLevel.NoCacheNoStore);
			}
			catch(NotImplementedException) { }
			catch(Exception) { Debug.Assert(false); }

			try
			{
				IWebProxy prx;
				if(GetWebProxy(out prx)) request.Proxy = prx;
			}
			catch(Exception) { Debug.Assert(false); }
		}

		internal static void ConfigureWebClient(WebClient wc)
		{

			try
			{
				IWebProxy prx;
				if(GetWebProxy(out prx)) wc.Proxy = prx;
			}
			catch(Exception) { Debug.Assert(false); }
		}

		private static bool GetWebProxy(out IWebProxy prx)
		{
			prx = null;

			if(m_pstProxyType == ProxyServerType.None)
				return true; // Use null proxy
			if(m_pstProxyType == ProxyServerType.Manual)
			{
				try
				{
					if(m_strProxyPort.Length > 0)
						prx = new WebProxy(m_strProxyAddr, int.Parse(m_strProxyPort));
					else prx = new WebProxy(m_strProxyAddr);

					if((m_strProxyUserName.Length > 0) || (m_strProxyPassword.Length > 0))
						prx.Credentials = new NetworkCredential(m_strProxyUserName,
							m_strProxyPassword);

					return true; // Use manual proxy
				}
				catch(Exception exProxy)
				{
					string strInfo = m_strProxyAddr;
					if(m_strProxyPort.Length > 0) strInfo += ":" + m_strProxyPort;
					MessageService.ShowWarning(strInfo, exProxy.Message);
				}

				return false; // Use default
			}

			if((m_strProxyUserName.Length == 0) && (m_strProxyPassword.Length == 0))
				return false; // Use default proxy, no auth

			try
			{
				prx = WebRequest.DefaultWebProxy;
				if(prx == null) prx = WebRequest.GetSystemWebProxy();
				if(prx == null) throw new InvalidOperationException();

				prx.Credentials = new NetworkCredential(m_strProxyUserName,
					m_strProxyPassword);
				return true;
			}
			catch(Exception) { Debug.Assert(false); }

			return false;
		}

		private static void PrepareWebAccess()
		{
			/*
				ServicePointManager.ServerCertificateValidationCallback =
					IOConnection.AcceptCertificate;*/
			ServicePointManager.ServerCertificateValidationCallback = CertificateValidationCallback;
		}

		private static IOWebClient CreateWebClient(IOConnectionInfo ioc, bool digestAuth)
		{
			PrepareWebAccess();

			IOWebClient wc = new IOWebClient();
			ConfigureWebClient(wc);

			if ((ioc.UserName.Length > 0) || (ioc.Password.Length > 0))
			{
				//set the credentials without a cache (in case the cache below fails:

				//check for backslash to determine whether we need to specify the domain:
				int backslashPos = ioc.UserName.IndexOf("\\", StringComparison.Ordinal);
				if (backslashPos > 0)
				{
					string domain = ioc.UserName.Substring(0, backslashPos);
					string user = ioc.UserName.Substring(backslashPos + 1);
					wc.Credentials = new NetworkCredential(user, ioc.Password, domain);
				}
				else
				{
					wc.Credentials = new NetworkCredential(ioc.UserName, ioc.Password);	
				}
				

				if (digestAuth)
				{
					//try to use the credential cache to access with Digest support:
					try
					{
						var credentialCache = new CredentialCache();

						credentialCache.Add(
											new Uri(new Uri(ioc.Path).GetLeftPart(UriPartial.Authority)),
											"Digest",
											new NetworkCredential(ioc.UserName, ioc.Password)
						); 

						credentialCache.Add(
						                    new Uri(new Uri(ioc.Path).GetLeftPart(UriPartial.Authority)),
						                    "NTLM",
						                    new NetworkCredential(ioc.UserName, ioc.Password)
						); 

						
						wc.Credentials = credentialCache;
					} catch (NotImplementedException e)
					{ 
						Kp2aLog.Log(e.ToString());
					} catch (Exception e)
					{ 
						Kp2aLog.LogUnexpectedError(e);
						Debug.Assert(false); 
					}
				}
			}
			else if(NativeLib.IsUnix()) // Mono requires credentials
				wc.Credentials = new NetworkCredential("anonymous", string.Empty);

			return wc;
		}

		private static WebRequest CreateWebRequest(IOConnectionInfo ioc, bool digestAuth)
		{
			PrepareWebAccess();

			WebRequest req = WebRequest.Create(ioc.Path);
			ConfigureWebRequest(req);

			if((ioc.UserName.Length > 0) || (ioc.Password.Length > 0))
			{
				req.Credentials = new NetworkCredential(ioc.UserName, ioc.Password);

				if (digestAuth)
				{
					var credentialCache = new CredentialCache();
					credentialCache.Add(
										new Uri(new Uri(ioc.Path).GetLeftPart(UriPartial.Authority)), // request url's host
										"Digest",  // authentication type 
										new NetworkCredential(ioc.UserName, ioc.Password) // credentials 
										); 
					credentialCache.Add( 
					                    new Uri(new Uri(ioc.Path).GetLeftPart(UriPartial.Authority)), // request url's host
					                    "NTLM",  // authentication type 
					                    new NetworkCredential(ioc.UserName, ioc.Password) // credentials 
					                    ); 
					
					req.Credentials = credentialCache;
				}
			}
			else if(NativeLib.IsUnix()) // Mono requires credentials
				req.Credentials = new NetworkCredential("anonymous", string.Empty);

			return req;
		}

		public static Stream OpenRead(IOConnectionInfo ioc)
		{
			RaiseIOAccessPreEvent(ioc, IOAccessType.Read);

			if(StrUtil.IsDataUri(ioc.Path))
			{
				byte[] pbData = StrUtil.DataUriToData(ioc.Path);
				if (pbData != null)
					return new MemoryStream(pbData, false);
			}

			if (ioc.IsLocalFile())
				return OpenReadLocal(ioc);

			try
			{ 
				return CreateWebClient(ioc, false).OpenRead(new Uri(ioc.Path));
			} catch (WebException ex)
			{
				if ((ex.Response is HttpWebResponse) && (((HttpWebResponse)ex.Response).StatusCode == HttpStatusCode.Unauthorized))
					return CreateWebClient(ioc, true).OpenRead(new Uri(ioc.Path));
				else
					throw;
			}

		}
#else
		public static Stream OpenRead(IOConnectionInfo ioc)
		{
			RaiseIOAccessPreEvent(ioc, IOAccessType.Read);

			return OpenReadLocal(ioc);
		}
#endif

		private static Stream OpenReadLocal(IOConnectionInfo ioc)
		{
			return new FileStream(ioc.Path, FileMode.Open, FileAccess.Read,
				FileShare.Read);
		}

#if (!KeePassLibSD && !KeePassRT)

		class UploadOnCloseMemoryStream: MemoryStream
		{
			IOConnectionInfo ioc;
			string method;
			Uri destinationFilePath;
			
			public UploadOnCloseMemoryStream(IOConnectionInfo _ioc, string _method, Uri _destinationFilePath)
			{
				ioc = _ioc;
				this.method = _method;
				this.destinationFilePath = _destinationFilePath;
			}

			public UploadOnCloseMemoryStream(IOConnectionInfo _ioc, Uri _destinationFilePath)
			{
				this.ioc = _ioc;
				this.method = null;
				this.destinationFilePath = _destinationFilePath;
			}

			public override void Close()
			{
				base.Close();

				WebRequest testReq = WebRequest.Create(ioc.Path);
				if (testReq is HttpWebRequest)
				{
					RepeatWithDigestOnFail(ioc, req =>
					{
						req.Headers.Add("Translate: f");

						if (method != null)
							req.Method = method;
						var data = this.ToArray();

						using (Stream s = req.GetRequestStream())
						{
							s.Write(data, 0, data.Length);
							req.GetResponse();
							s.Close();
						}
					});	
				}
				else
				{
					try
					{
						uploadData(IOConnection.CreateWebClient(ioc, false));
					}
					catch (WebException ex)
					{
						//todo: does this make sense for FTP at all? Remove?
						if ((ex.Response is HttpWebResponse) && (((HttpWebResponse)ex.Response).StatusCode == HttpStatusCode.Unauthorized))
							uploadData(IOConnection.CreateWebClient(ioc, true));
						else
							throw;
					}
				}

				
				
			}

			void uploadData(WebClient webClient)
			{
				if (method != null)
				{
					webClient.UploadData(destinationFilePath, method, this.ToArray());
				}
				else
				{
					webClient.UploadData(destinationFilePath, this.ToArray());
				}
			}

			
		}

		public static Stream OpenWrite(IOConnectionInfo ioc)
		{
			if(ioc == null) { Debug.Assert(false); return null; }

			RaiseIOAccessPreEvent(ioc, IOAccessType.Write);

			if(ioc.IsLocalFile()) return OpenWriteLocal(ioc);

			Uri uri = new Uri(ioc.Path);

			// Mono does not set HttpWebRequest.Method to POST for writes,
			// so one needs to set the method to PUT explicitly
			if(NativeLib.IsUnix() && (uri.Scheme.Equals(Uri.UriSchemeHttp,
				StrUtil.CaseIgnoreCmp) || uri.Scheme.Equals(Uri.UriSchemeHttps,
				StrUtil.CaseIgnoreCmp)))
				return new UploadOnCloseMemoryStream(ioc, WebRequestMethods.Http.Put, uri);

			return new UploadOnCloseMemoryStream(ioc, uri);
		}
#else
		public static Stream OpenWrite(IOConnectionInfo ioc)
		{
			RaiseIOAccessPreEvent(ioc, IOAccessType.Write);

			return OpenWriteLocal(ioc);
		}
#endif

		private static Stream OpenWriteLocal(IOConnectionInfo ioc)
		{
			return new FileStream(ioc.Path, FileMode.Create, FileAccess.Write,
				FileShare.None);
		}

		public static bool FileExists(IOConnectionInfo ioc)
		{
			return FileExists(ioc, false);
		}

		public static bool FileExists(IOConnectionInfo ioc, bool bThrowErrors)
		{
			if(ioc == null) { Debug.Assert(false); return false; }

			RaiseIOAccessPreEvent(ioc, IOAccessType.Exists);

			if(ioc.IsLocalFile()) return File.Exists(ioc.Path);

#if (!KeePassLibSD && !KeePassRT)
			if(ioc.Path.StartsWith("ftp://", StrUtil.CaseIgnoreCmp))
			{
				bool b = SendCommand(ioc, WebRequestMethods.Ftp.GetDateTimestamp);
				if(!b && bThrowErrors) throw new InvalidOperationException();
				return b;
			}
#endif

			try
			{
				Stream s = OpenRead(ioc);
				if(s == null) throw new Java.IO.FileNotFoundException();

				try { s.ReadByte(); }
				catch(Exception) { }

				// We didn't download the file completely; close may throw
				// an exception -- that's okay
				try { s.Close(); }
				catch(Exception) { }
			}
			catch(Exception)
			{
				if(bThrowErrors) throw;
				return false;
			}

			return true;
		}

		delegate void DoWithRequest(WebRequest req);


		static void RepeatWithDigestOnFail(IOConnectionInfo ioc, DoWithRequest f)
		{
			WebRequest req = CreateWebRequest(ioc, false);
			try{
				f(req);
			}
			catch (WebException ex)
			{
				if ((ex.Response is HttpWebResponse) && (((HttpWebResponse) ex.Response).StatusCode == HttpStatusCode.Unauthorized))
				{
					req = CreateWebRequest(ioc, true);
					f(req);
				}
				else throw;
			}
		}

		public static void DeleteFile(IOConnectionInfo ioc)
		{
			RaiseIOAccessPreEvent(ioc, IOAccessType.Delete);

			//in case a user entered a directory instead of a filename, make sure we're never 
			//deleting their whole WebDAV/FTP content
			if (ioc.Path.EndsWith("/"))
				throw new IOException("Delete file does not expect directory URIs.");

			if(ioc.IsLocalFile()) { File.Delete(ioc.Path); return; }

#if (!KeePassLibSD && !KeePassRT)
			RepeatWithDigestOnFail(ioc, (WebRequest req) => {
				if(req != null)
				{
					if(req is HttpWebRequest) req.Method = "DELETE";
					else if(req is FtpWebRequest) req.Method = WebRequestMethods.Ftp.DeleteFile;
					else if(req is FileWebRequest)
					{
						File.Delete(UrlUtil.FileUrlToPath(ioc.Path));
						return;
					}
					else req.Method = WrmDeleteFile;

					DisposeResponse(req.GetResponse(), true);
				}
			});
#endif
		}

		/// <summary>
		/// Rename/move a file. For local file system and WebDAV, the
		/// specified file is moved, i.e. the file destination can be
		/// in a different directory/path. In contrast, for FTP the
		/// file is renamed, i.e. its destination must be in the same
		/// directory/path.
		/// </summary>
		/// <param name="iocFrom">Source file path.</param>
		/// <param name="iocTo">Target file path.</param>
		public static void RenameFile(IOConnectionInfo iocFrom, IOConnectionInfo iocTo)
		{
			RaiseIOAccessPreEvent(iocFrom, iocTo, IOAccessType.Move);

			if(iocFrom.IsLocalFile()) { File.Move(iocFrom.Path, iocTo.Path); return; }

#if (!KeePassLibSD && !KeePassRT)
			RepeatWithDigestOnFail(iocFrom, (WebRequest req)=> { if(req != null)
			{
				if(req is HttpWebRequest)
				{
					req.Method = "MOVE";
					req.Headers.Set("Destination", iocTo.Path); // Full URL supported
				}
				else if(req is FtpWebRequest)
				{
					req.Method = WebRequestMethods.Ftp.Rename;
					string strTo = UrlUtil.GetFileName(iocTo.Path);

					// We're affected by .NET bug 621450:
					// https://connect.microsoft.com/VisualStudio/feedback/details/621450/problem-renaming-file-on-ftp-server-using-ftpwebrequest-in-net-framework-4-0-vs2010-only
					// Prepending "./", "%2E/" or "Dummy/../" doesn't work.

					((FtpWebRequest)req).RenameTo = strTo;
				}
				else if(req is FileWebRequest)
				{
					File.Move(UrlUtil.FileUrlToPath(iocFrom.Path),
						UrlUtil.FileUrlToPath(iocTo.Path));
					return;
				}
				else
				{
					req.Method = WrmMoveFile;
					req.Headers.Set(WrhMoveFileTo, iocTo.Path);
				}

				DisposeResponse(req.GetResponse(), true);
			}
			});
			
#endif

			// using(Stream sIn = IOConnection.OpenRead(iocFrom))
			// {
			//	using(Stream sOut = IOConnection.OpenWrite(iocTo))
			//	{
			//		MemUtil.CopyStream(sIn, sOut);
			//		sOut.Close();
			//	}
			//
			//	sIn.Close();
			// }
			// DeleteFile(iocFrom);
		}

#if (!KeePassLibSD && !KeePassRT)
		private static bool SendCommand(IOConnectionInfo ioc, string strMethod)
		{
			try
			{
				RepeatWithDigestOnFail(ioc, (WebRequest req)=> {
					req.Method = strMethod;
					DisposeResponse(req.GetResponse(), true);
			
				});
			}
			catch(Exception) { return false; }

			return true;
		}
#endif

		private static void DisposeResponse(WebResponse wr, bool bGetStream)
		{
			if(wr == null) return;

			try
			{
				if(bGetStream)
				{
					Stream s = wr.GetResponseStream();
					if(s != null) s.Close();
				}
			}
			catch(Exception) { Debug.Assert(false); }

			try { wr.Close(); }
			catch(Exception) { Debug.Assert(false); }
		}

		public static byte[] ReadFile(IOConnectionInfo ioc)
		{
			Stream sIn = null;
			MemoryStream ms = null;
			try
			{
				sIn = IOConnection.OpenRead(ioc);
				if (sIn == null) return null;

				ms = new MemoryStream();
				MemUtil.CopyStream(sIn, ms);

				return ms.ToArray();
			}
			catch (Exception e)
			{
				Kp2aLog.Log("error opening file: " + e);
			}
			finally
			{
				if(sIn != null) sIn.Close();
				if(ms != null) ms.Close();
			}

			return null;
		}

		private static void RaiseIOAccessPreEvent(IOConnectionInfo ioc, IOAccessType t)
		{
			RaiseIOAccessPreEvent(ioc, null, t);
		}

		private static void RaiseIOAccessPreEvent(IOConnectionInfo ioc,
			IOConnectionInfo ioc2, IOAccessType t)
		{
			if(ioc == null) { Debug.Assert(false); return; }
			// ioc2 may be null

			if(IOConnection.IOAccessPre != null)
			{
				IOConnectionInfo ioc2Lcl = ((ioc2 != null) ? ioc2.CloneDeep() : null);
				IOAccessEventArgs e = new IOAccessEventArgs(ioc.CloneDeep(), ioc2Lcl, t);
				IOConnection.IOAccessPre(null, e);
			}
		}
	}
}
