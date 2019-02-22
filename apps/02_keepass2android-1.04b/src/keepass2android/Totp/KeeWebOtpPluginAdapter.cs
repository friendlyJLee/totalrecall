﻿using System;
using System.Collections.Generic;
using System.Web;
using Android.Content;
using PluginTOTP;

namespace keepass2android
{
    internal class KeeWebOtpPluginAdapter : ITotpPluginAdapter
    {
        public TotpData GetTotpData(IDictionary<string, string> entryFields, Context ctx, bool muteWarnings)
        {
            TotpData res = new TotpData();
            string data;
            if (!entryFields.TryGetValue("otp", out data))
            {
                return res;
            }

            string otpUriStart = "otpauth://totp/";

            if (!data.StartsWith(otpUriStart))
                return res;


            try
            {
                Uri myUri = new Uri(data);
                var parsedQuery = HttpUtility.ParseQueryString(myUri.Query);
                res.TotpSeed = parsedQuery.Get("secret");
                res.Length = parsedQuery.Get("digits");
                res.Duration = parsedQuery.Get("period");
            }
            catch (Exception)
            {
                return res;
            }
            
            res.IsTotpEnry = true;
            return res;
        }
    }
}