/*
 * Copyright (c) 2012, salesforce.com, inc.
 * All rights reserved.
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 * - Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * - Neither the name of salesforce.com, inc. nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission of salesforce.com, inc.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.salesforce.androidsdk.gcm;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import android.app.Activity;
import android.os.Bundle;

import com.google.android.gcm.GCMRegistrar;
import com.salesforce.androidsdk.app.ForceApp;
import com.salesforce.androidsdk.rest.ClientManager;
import com.salesforce.androidsdk.rest.ClientManager.AccountInfoNotFoundException;
import com.salesforce.androidsdk.rest.ClientManager.LoginOptions;
import com.salesforce.androidsdk.rest.RestClient;
import com.salesforce.androidsdk.rest.RestClient.AsyncRequestCallback;
import com.salesforce.androidsdk.rest.RestRequest;
import com.salesforce.androidsdk.rest.RestResponse;

public class SFPushNotification {
	
	private final SFPushNotificationOptions options;
	private boolean registered;
	
	public SFPushNotification(SFPushNotificationOptions options) {
		this.options = options;
	}

    public static void unregisterGCM() {
    	GCMRegistrar.unregister(ForceApp.APP);
    }
    
	public static void registerGCM() {
		LoginOptions options = ForceApp.APP.getLoginOptions();
		if (options != null && options.gcmApplicationId != null) {
			GCMRegistrar.checkDevice(ForceApp.APP);
			GCMRegistrar.checkManifest(ForceApp.APP);
			final String regId = GCMRegistrar.getRegistrationId(ForceApp.APP);
			if (regId.equals("")) {
				GCMRegistrar.register(ForceApp.APP, options.gcmApplicationId);
			}
		}
	}
	
	public void registerForSFDCNotifications(Activity activity) throws UnsupportedEncodingException, IOException, AccountInfoNotFoundException {
		if (options != null) {
			RestClient client = getRestClient(activity);
			Map<String, Object> fields = new HashMap<String, Object>();
			fields.put("ApplicationName", options.applicationName);
			fields.put("ConnectionToken", options.pnsToken);
			fields.put("NamespacePrefix", options.namespacePrefix);
			fields.put("Vendor", options.vendor);
			RestRequest request = RestRequest.getRequestForCreate(options.apiVersion, "MobilePushServiceDevice", fields);
			client.sendAsync(request, new AsyncRequestCallback() {
				@Override
				public void onSuccess(RestRequest request, RestResponse result) {
					try {
						JSONObject json = result.asJSONObject();
						options.pushObjectEntity = json.getString("id");
						registered = true;
					} catch (Exception e) {
						registered = false;
					}
				}

				@Override
				public void onError(Exception exception) {
					registered = false;
				}
			});
		}
	}
	
	public void unregisterForSFDCNotifications(Activity activity) throws AccountInfoNotFoundException {
		if (options != null) {
			RestClient client = getRestClient(activity);
			RestRequest request = RestRequest.getRequestForDelete(
					options.apiVersion, "MobilePushServiceDevice",
					options.pushObjectEntity);
			client.sendAsync(request, new AsyncRequestCallback() {
				@Override
				public void onSuccess(RestRequest request, RestResponse result) {
					registered = false;
				}

				@Override
				public void onError(Exception exception) {
					registered = true;
				}
			});
		}
	}

	public boolean isSFDCRegistered() {
		return registered;
	}

	private RestClient getRestClient(Activity activity) throws AccountInfoNotFoundException {
		return new ClientManager(activity, ForceApp.APP.getAccountType(), ForceApp.APP.getLoginOptions()).peekRestClient();
	}

	public static class SFPushNotificationOptions {
		private static final String PNS_TOKEN = "pnsToken";
		private static final String OBJECT_ENTITY = "pushObjectEntity";
		private static final String APP_NAME = "applicationName";
		private static final String NAMESPACE_PREFIX = "namespacePrefix";
		private static final String API_VERSION = "apiVersion";
		private static final String VENDOR = "vendor";
		private final Bundle bundle;
		
		public String pnsToken;
		public String pushObjectEntity;
		public String applicationName;
		public String namespacePrefix;
		public String vendor;
		public String apiVersion;
		
		public SFPushNotificationOptions(String pnsToken, String applicationName, String namespacePrefix) {
            this.pnsToken = pnsToken;
            this.applicationName = applicationName;
            this.namespacePrefix = namespacePrefix;
            this.vendor = "Android";
            this.apiVersion = "v27.0";
            bundle = new Bundle();
            bundle.putString(PNS_TOKEN, pnsToken);
            bundle.putString(APP_NAME, applicationName);
            bundle.putString(NAMESPACE_PREFIX, namespacePrefix);
            bundle.putString(VENDOR, vendor);
            bundle.putString(API_VERSION, apiVersion);
        }
		
		public SFPushNotificationOptions(String pnsToken, String applicationName, String namespacePrefix, String pushObjectEntity) {
			this(pnsToken, applicationName, namespacePrefix);
			this.pushObjectEntity = pushObjectEntity;
			bundle.putString(OBJECT_ENTITY, pushObjectEntity);
		}
		
		public Bundle asBundle() {
            return bundle;
        }

        public static SFPushNotificationOptions fromBundle(Bundle options) {
            return new SFPushNotificationOptions(options.getString(PNS_TOKEN),
                                    options.getString(APP_NAME),
                                    options.getString(NAMESPACE_PREFIX),
                                    options.getString(OBJECT_ENTITY));
        }
	}
}
