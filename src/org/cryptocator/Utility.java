/*
 * Copyright (c) 2015, Christian Motika.
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 * all contributors, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, an acknowledgement to all contributors, this list of conditions
 * and the following disclaimer in the documentation and/or other materials
 * provided with the distribution.
 * 
 * 3. Neither the name Delphino Cryptocator nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS “AS IS” AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, 
 * OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
 * OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 */
package org.cryptocator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.ClipData;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Shader.TileMode;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class Utility {

	private static SharedPreferences settings;
	public static final String PREFERENCESURI = "org.cryptocator";

	// -------------------------------------------------------------------------

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static void hideSoftkeys(Activity activity) {
		if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			activity.getWindow().getDecorView()
					.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
			activity.getWindow().getDecorView()
					.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
		}
	}

	// -------------------------------------------------------------------------

	public static void hideKeyboard(Activity activity) {
		// This can be used to suppress the keyboard until the user actually
		// touches the edittext view.
		activity.getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
	}

	public static void hideKeyboardExplicit(EditText textInput) {
		// hide keyboard explicitly
		InputMethodManager imm = (InputMethodManager) textInput.getContext()
				.getSystemService(Context.INPUT_METHOD_SERVICE);
		// imm.restartInput(addUserName);
		imm.hideSoftInputFromWindow(textInput.getWindowToken(), 0);
	}

	public static void showKeyboardExplicit(EditText textInput) {
		InputMethodManager keyboard = (InputMethodManager) textInput
				.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
		keyboard.showSoftInput(textInput, 0);
	}

	// -------------------------------------------------------------------------
	// -------------------------------------------------------------------------

	// Get or update the settings for this application.
	public static SharedPreferences getSettings(Context context) {
		if (Utility.settings != null) {
			return Utility.settings;
		}
		Utility.settings = context.getSharedPreferences(Utility.PREFERENCESURI,
				Activity.MODE_WORLD_WRITEABLE);
		return Utility.settings;
	}

	// -------------------------------------------------------------------------

	// Delimited with @@@, key:value
	public static HashMap<String, String> buildSettingsMap(String settingsString) {
		HashMap<String, String> returnMap = new HashMap<String, String>();
		String[] settings = settingsString.split("@@@");
		for (String setting : settings) {
			int i = setting.indexOf(":");
			if (i > -1) {
				String key = setting.substring(0, i);
				String value = setting.substring(i + 1);
				returnMap.put(key, value);
			}
		}
		return returnMap;
	}

	// -------------------------------------------------------------------------

	// Get an String setting.
	public static String loadStringSetting(Context context, String id,
			String defaultValue) {
		return loadStringSetting(context, id, defaultValue, null);
	}

	// Get an String setting.
	public static String loadStringSetting(Context context, String id,
			String defaultValue, HashMap<String, String> settingsMap) {
		if (settingsMap == null) {
			// Update the settings variable
			String returnValue = defaultValue;
			try {
				getSettings(context);
				returnValue = settings.getString(id, defaultValue);
			} catch (Exception e) {
			}
			return returnValue;

		} else {
			if (!settingsMap.containsKey(id)) {
				return defaultValue;
			}
			return settingsMap.get(id);
		}
	}

	// -------------------------------------------------------------------------

	// Get an integer setting.
	public static int loadIntSetting(Context context, String id,
			int defaultValue) {
		return loadIntSetting(context, id, defaultValue, null);
	}

	// Get an integer setting.
	public static int loadIntSetting(Context context, String id,
			int defaultValue, HashMap<String, String> settingsMap) {
		if (settingsMap == null) {
			// Update the settings variable
			int returnValue = defaultValue;
			try {
				getSettings(context);
				returnValue = settings.getInt(id, defaultValue);
			} catch (Exception e) {
			}
			return returnValue;
		} else {
			if (!settingsMap.containsKey(id)) {
				return defaultValue;
			}
			int returnValue = defaultValue;
			try {
				returnValue = parseInt(settingsMap.get(id), defaultValue);
			} catch (Exception e) {
			}
			return returnValue;
		}
	}

	// -------------------------------------------------------------------------

	// Get a long setting.
	public static long loadLongSetting(Context context, String id,
			long defaultValue) {
		// Update the settings variable
		long returnValue = defaultValue;
		try {
			getSettings(context);
			returnValue = settings.getLong(id, defaultValue);
		} catch (Exception e) {
		}
		return returnValue;
	}

	// -------------------------------------------------------------------------

	// Get a double setting.
	public static double loadDoubleSetting(Context context, String id,
			double defaultValue) {
		String doubleString = loadStringSetting(context, id, defaultValue + "");
		double returnValue = parseDouble(doubleString, defaultValue);
		return returnValue;
	}

	// -------------------------------------------------------------------------

	// Get a boolean setting.
	public static boolean loadBooleanSetting(Context context, String id,
			boolean defaultValue) {
		return loadBooleanSetting(context, id, defaultValue, null);
	}

	// Get a boolean setting.
	public static boolean loadBooleanSetting(Context context, String id,
			boolean defaultValue, HashMap<String, String> settingsMap) {
		if (settingsMap == null) {
			// Update the settings variable
			boolean returnValue = defaultValue;
			try {
				getSettings(context);
				returnValue = settings.getBoolean(id, defaultValue);
			} catch (Exception e) {
			}
			return returnValue;
		} else {
			if (!settingsMap.containsKey(id)) {
				return defaultValue;
			}
			if (settingsMap.get(id).toLowerCase().equals("true")) {
				return true;
			}
			return false;
		}

	}

	// -------------------------------------------------------------------------

	// Saves a boolean setting.
	public static void saveBooleanSetting(Context context, String id,
			boolean value) {
		saveBooleanSetting(context, id, value, null);
	}

	// Saves a boolean setting.
	public static void saveBooleanSetting(Context context, String id,
			boolean value, List<String> settingsString) {
		if (settingsString == null) {
			// Update the settings variable
			getSettings(context);
			SharedPreferences.Editor settingsEditor = settings.edit();
			settingsEditor.putBoolean(id, value);
			settingsEditor.commit();
		} else {
			settingsString.add(id + ":" + value);
		}

	}

	// -------------------------------------------------------------------------

	// Saves an integer setting
	public static void saveIntSetting(Context context, String id, int value) {
		saveIntSetting(context, id, value, null);
	}

	// Saves an integer setting
	public static void saveIntSetting(Context context, String id, int value,
			List<String> settingsString) {
		if (settingsString == null) {
			// Update the settings variable
			getSettings(context);
			SharedPreferences.Editor settingsEditor = settings.edit();
			settingsEditor.putInt(id, value);
			settingsEditor.commit();
		} else {
			settingsString.add(id + ":" + value);
		}
	}

	// -------------------------------------------------------------------------

	// Saves a long setting
	public static void saveLongSetting(Context context, String id, long value) {
		// Update the settings variable
		getSettings(context);
		SharedPreferences.Editor settingsEditor = settings.edit();
		settingsEditor.putLong(id, value);
		settingsEditor.commit();
	}

	// -------------------------------------------------------------------------

	// Saves a double setting
	public static void saveDoubleSetting(Context context, String id,
			double value) {
		saveStringSetting(context, id, value + "");
	}

	// -------------------------------------------------------------------------

	// Saves an integer setting
	public static void saveStringSetting(Context context, String id,
			String value) {
		saveStringSetting(context, id, value, null);
	}

	// Saves an integer setting
	public static void saveStringSetting(Context context, String id,
			String value, List<String> settingsString) {
		if (settingsString == null) {
			// Update the settings variable
			getSettings(context);
			SharedPreferences.Editor settingsEditor = settings.edit();
			if (value != null) {
				settingsEditor.putString(id, value);
			} else {
				settingsEditor.remove(id);
			}
			settingsEditor.commit();
		} else {
			settingsString.add(id + ":" + value);
		}
	}

	// -------------------------------------------------------------------------
	// -------------------------------------------------------------------------

	public static void setBackground(Context activity, View view, int resourceID) {
		// set dolphin background
		Bitmap bm2 = BitmapFactory.decodeResource(activity.getResources(),
				resourceID);
		BitmapDrawable background2 = new BitmapDrawable(bm2);
		background2.setTileModeXY(TileMode.REPEAT, TileMode.REPEAT);
		view.setBackgroundDrawable(background2);
	}

	// ---------------------------------------------------------------------------
	// ---------------------------------------------------------------------------

	public static int parseInt(String integerString, int defaultValue) {
		int returnInteger = -1;
		try {
			returnInteger = Integer.parseInt(integerString);
			return returnInteger;
		} catch (Exception e) {
			return defaultValue;
		}
	}

	public static long parseLong(String longString, long defaultValue) {
		long returnInteger = -1;
		try {
			returnInteger = Long.parseLong(longString);
			return returnInteger;
		} catch (Exception e) {
			return defaultValue;
		}
	}

	public static double parseDouble(String doubleString, double defaultValue) {
		double returnDouble = -1;
		try {
			returnDouble = Long.parseLong(doubleString);
			return returnDouble;
		} catch (Exception e) {
			return defaultValue;
		}
	}

	// ---------------------------------------------------------------------------
	// ---------------------------------------------------------------------------

	public static String encode(String text) {
		try {
			return URLEncoder.encode(text, "utf-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}

	// ---------------------------------------------------------------------------

	public static String getData(Context context, String url)
			throws ClientProtocolException, IOException {
		// Create a new HttpClient
		HttpClient httpclient = new DefaultHttpClient();
		HttpGet request = new HttpGet();
		String returnValue = "";
		try {
			URI website = new URI(url);
			request.setURI(website);
			HttpResponse response = httpclient.execute(request);
			returnValue = getContentAsString(response);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return returnValue;
	}

	// -------------------------------------------------------------------------

	public static String getContentAsString(HttpResponse response)
			throws IOException {
		String returnString = "";
		HttpEntity httpEntity = response.getEntity();

		InputStream inputStream = httpEntity.getContent();
		InputStreamReader is = new InputStreamReader(inputStream);

		if (is != null) {
			Writer writer = new StringWriter();

			char[] buffer = new char[1024];
			try {
				Reader reader = new BufferedReader(is);
				int n;
				while ((n = reader.read(buffer)) != -1) {
					writer.write(buffer, 0, n);
				}
			} finally {
				is.close();
			}

			returnString = writer.toString();

		}

		return returnString;
	}

	// -------------------------------------------------------------------------
	// -------------------------------------------------------------------------

	// convert from internal Java String format to internal Java String format
	public static String reencodeISO(String s) {
		String out = null;
		try {
			out = new String(s.getBytes("ISO-8859-1"), "ISO-8859-1");
		} catch (java.io.UnsupportedEncodingException e) {
			return null;
		}
		return out;
	}

	// -------------------------------------------------------------------------

	// convert from internal Java String format -> UTF-8
	public static String convertToUTF8(String s) {
		String out = null;
		try {
			out = new String(s.getBytes("UTF-8"), "ISO-8859-1");
		} catch (java.io.UnsupportedEncodingException e) {
			return null;
		}
		return out;
	}

	// -------------------------------------------------------------------------

	// convert from UTF-8 -> internal Java String format
	public static String convertFromUTF8(String s) {
		String out = null;
		try {
			out = new String(s.getBytes("ISO-8859-1"), "UTF-8");
		} catch (java.io.UnsupportedEncodingException e) {
			return null;
		}
		return out;
	}

	// -------------------------------------------------------------------------
	// -------------------------------------------------------------------------

	public static String getTileNumber(double lat, double lon, final int zoom) {

		// if (zoom == 17) { //250
		// lat += 0.0002000;
		// lon -= 0.0015000;
		// // lon -= 0.001000;
		// }
		// else if (zoom == 16) { //500
		// lat += 0.0008000;
		// lon -= 0.0008000;
		// // lon -= 2*0.001000;
		// }

		int xtile = (int) Math.floor(((lon + 180) / 360) * (1 << zoom));
		int ytile = (int) Math
				.floor((1 - Math.log(Math.tan(Math.toRadians(lat)) + 1
						/ Math.cos(Math.toRadians(lat)))
						/ Math.PI)
						/ 2 * (1 << zoom));
		if (xtile < 0)
			xtile = 0;
		if (xtile >= (1 << zoom))
			xtile = ((1 << zoom) - 1);
		if (ytile < 0)
			ytile = 0;
		if (ytile >= (1 << zoom))
			ytile = ((1 << zoom) - 1);

		return ("" + zoom + "/" + xtile + "/" + ytile);
	}

	// -------------------------------------------------------------------------

	public static void updateTile(Context context, final ImageView image,
			int zoom, double latitude, double longitude) {
		final String url = "http://tile.openstreetmap.org/"
				+ getTileNumber(latitude, longitude, zoom) + ".png";

		(new Thread() {
			public void run() {
				try {
					final InputStream is = (InputStream) new URL(url)
							.getContent();
					final Bitmap bm = BitmapFactory.decodeStream(is);
					// Drawable d = new BitmapDrawable(bm);
					if (image != null) {

						final Handler mUIHandler = new Handler(
								Looper.getMainLooper());
						mUIHandler.post(new Thread() {
							@Override
							public void run() {
								super.run();
								image.setImageBitmap(bm);
							}
						});

					}
				} catch (MalformedURLException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
		}).start();
	}

	// -------------------------------------------------------------------------
	// -------------------------------------------------------------------------

	public static String getDate(long millis) {
		// Create a DateFormatter object for displaying date in specified
		// format.
		SimpleDateFormat formatter = new SimpleDateFormat("E, d. MMM yy");
		// Create a calendar object that will convert the date and time value in
		// milliseconds to date.
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(millis);
		return formatter.format(calendar.getTime());
	}

	public static String getTimeTitle(long millis) {
		return getDate(millis) + " @ " + getHours(millis) + ":"
				+ getMinutes(millis);
	}

	public static String getHours(long millis) {
		// Create a DateFormatter object for displaying date in specified
		// format.
		SimpleDateFormat formatter = new SimpleDateFormat("HH");
		// Create a calendar object that will convert the date and time value in
		// milliseconds to date.
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(millis);
		return formatter.format(calendar.getTime());
	}

	public static String getYear(long millis) {
		// Create a DateFormatter object for displaying date in specified
		// format.
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy");
		// Create a calendar object that will convert the date and time value in
		// milliseconds to date.
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(millis);
		return formatter.format(calendar.getTime());
	}

	public static String getMinutes(long millis) {
		// Create a DateFormatter object for displaying date in specified
		// format.
		SimpleDateFormat formatter = new SimpleDateFormat("mm");
		// Create a calendar object that will convert the date and time value in
		// milliseconds to date.
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(millis);
		return formatter.format(calendar.getTime());
	}

	public static String getSeconds(long millis) {
		// Create a DateFormatter object for displaying date in specified
		// format.
		SimpleDateFormat formatter = new SimpleDateFormat("s");
		// Create a calendar object that will convert the date and time value in
		// milliseconds to date.
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(millis);
		return formatter.format(calendar.getTime());
	}

	public static String getMilliSeconds(long millis) {
		// Create a DateFormatter object for displaying date in specified
		// format.
		SimpleDateFormat formatter = new SimpleDateFormat("S");
		// Create a calendar object that will convert the date and time value in
		// milliseconds to date.
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(millis);
		return formatter.format(calendar.getTime());
	}

	// -------------------------------------------------------------------------

	// needs: <uses-permission
	// android:name="android.permission.READ_PHONE_STATE" />
	public static String getDeviceId(Context context) {
		// final TelephonyManager tm = (TelephonyManager)
		// context.getSystemService(Context.TELEPHONY_SERVICE);

		// if (tm.getDeviceId() != null){
		// return tm.getDeviceId(); //*** use for mobiles
		// }
		// else{
		return Secure
				.getString(context.getContentResolver(), Secure.ANDROID_ID)
				.toUpperCase(); // *** use for tablets
		// }

		// final String tmDevice, tmSerial, androidId;
		// tmDevice = "" + tm.getDeviceId();
		// //tmSerial = "" + tm.getSimSerialNumber();
		// //androidId = "" +
		// android.provider.Settings.Secure.getString(context.getContentResolver(),
		// android.provider.Settings.Secure.ANDROID_ID);
		//
		// // UUID deviceUuid = new UUID(androidId.hashCode(),
		// ((long)tmDevice.hashCode() << 32) | tmSerial.hashCode());
		// // String deviceId = deviceUuid.toString();
		// return tmDevice;
	}

	// -------------------------------------------------------------------------

	public static void showToastShortAsync(final Context context,
			final String toastText) {
		final Handler mUIHandler = new Handler(Looper.getMainLooper());
		mUIHandler.post(new Thread() {
			@Override
			public void run() {
				super.run();
				int duration = Toast.LENGTH_SHORT;
				Toast toast = Toast.makeText(context, toastText, duration);
				toast.show();
			}
		});
	}

	// -------------------------------------------------------------------------

	public static void showToastInUIThread(final Context context,
			final String toastText) {
		int duration = Toast.LENGTH_SHORT;
		Toast toast = Toast.makeText(context, toastText, duration);
		toast.show();
	}

	public static void showToastAsync(final Context context,
			final String toastText) {
		final Handler mUIHandler = new Handler(Looper.getMainLooper());
		mUIHandler.post(new Thread() {
			@Override
			public void run() {
				super.run();
				int duration = Toast.LENGTH_LONG;
				Toast toast = Toast.makeText(context, toastText, duration);
				toast.show();
			}
		});
	}

	// -------------------------------------------------------------------------

	/**
	 * Copy to clipboard. This method checks for honeycomp changes to the
	 * clipboard.
	 * 
	 * @param context
	 *            the context
	 * @param text
	 *            the text
	 */
	public static void copyToClipboard(Context context, String text) {
		if (text != null) {
			int sdk = android.os.Build.VERSION.SDK_INT;
			if (sdk < android.os.Build.VERSION_CODES.HONEYCOMB) {
				android.text.ClipboardManager clipboard = (android.text.ClipboardManager) context
						.getSystemService(Context.CLIPBOARD_SERVICE);
				clipboard.setText(text);
			} else {
				android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context
						.getSystemService(Context.CLIPBOARD_SERVICE);
				android.content.ClipData clip = android.content.ClipData
						.newPlainText("WordKeeper", text);
				clipboard.setPrimaryClip(clip);
			}
		}
	}

	// -------------------------------------------------------------------------

	/**
	 * Paste from clipboard. This method checks for honeycomp changes to the
	 * clipboard.
	 * 
	 * @param context
	 *            the context
	 * @return the string
	 */
	public static String pasteFromClipboard(final Context context) {
		int sdk = android.os.Build.VERSION.SDK_INT;
		if (sdk < android.os.Build.VERSION_CODES.HONEYCOMB) {
			android.text.ClipboardManager clipboard = (android.text.ClipboardManager) context
					.getSystemService(Context.CLIPBOARD_SERVICE);
			return clipboard.getText().toString();
		} else {
			android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context
					.getSystemService(Context.CLIPBOARD_SERVICE);
			ClipData clip = clipboard.getPrimaryClip();
			return clipboard.getText().toString();
			// if (clip != null && clip.getItemCount() > 0) {
			// return clip.getItemAt(0).coerceToText(context).toString();
			// }
		}
	}

	// -------------------------------------------------------------------------

	/**
	 * Force overflow menu buttons. Typically these are only available for
	 * phones without a hardware context-menu key.
	 * 
	 * @param context
	 *            the context
	 */
	public static void forceOverflowMenuButtons(Context context) {
		// =========================
		// how-to-force-use-of-overflow-menu-on-devices-with-menu-button
		try {
			ViewConfiguration config = ViewConfiguration.get(context);
			Field menuKeyField = ViewConfiguration.class
					.getDeclaredField("sHasPermanentMenuKey");
			if (menuKeyField != null) {
				menuKeyField.setAccessible(true);
				menuKeyField.setBoolean(config, false);
			}
		} catch (Exception ex) {
			// Ignore
		}
		// =========================
	}

	// -------------------------------------------------------------------------

	/**
	 * Creates an MD5 from a string.
	 * 
	 * @param s
	 *            the s
	 * @return the string
	 */
	public static final String md5(final String s) {
		try {
			// Create MD5 Hash
			MessageDigest digest = java.security.MessageDigest
					.getInstance("MD5");
			digest.update(s.getBytes());
			byte messageDigest[] = digest.digest();

			// Create Hex String
			StringBuffer hexString = new StringBuffer();
			for (int i = 0; i < messageDigest.length; i++) {
				String h = Integer.toHexString(0xFF & messageDigest[i]);
				while (h.length() < 2)
					h = "0" + h;
				hexString.append(h);
			}
			return hexString.toString();

		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	// -------------------------------------------------------------------------
	// -------------------------------------------------------------------------

	/**
	 * Checks if is phone completely muted (even no vibration).
	 * 
	 * @param context
	 *            the context
	 * @return true, if is phone muted
	 */
	public static boolean isPhoneMuted(Context context) {
		AudioManager audio = (AudioManager) context
				.getSystemService(Context.AUDIO_SERVICE);
		switch (audio.getRingerMode()) {
		case AudioManager.RINGER_MODE_NORMAL:
			return false;
		case AudioManager.RINGER_MODE_SILENT:
			return true;
		case AudioManager.RINGER_MODE_VIBRATE:
			return false;
		}
		return false;
	}

	// -------------------------------------------------------------------------

	/**
	 * Checks if is phone muted but vibration may still be on.
	 * 
	 * @param context
	 *            the context
	 * @return true, if is phone muted with vibration
	 */
	public static boolean isPhoneMutedOrVibration(Context context) {
		AudioManager audio = (AudioManager) context
				.getSystemService(Context.AUDIO_SERVICE);
		switch (audio.getRingerMode()) {
		case AudioManager.RINGER_MODE_NORMAL:
			return false;
		case AudioManager.RINGER_MODE_SILENT:
			return false;
		case AudioManager.RINGER_MODE_VIBRATE:
			return true;
		}
		return false;
	}

	// -------------------------------------------------------------------------

	/**
	 * Gets the list as string where the values are separated by separation.
	 * 
	 * @param list
	 *            the list
	 * @param sepraration
	 *            the separation
	 * @return the list as string
	 */
	public static String getListAsString(List<?> list, String separation) {
		if (list == null || list.isEmpty()) {
			return "";
		}
		StringBuilder s = new StringBuilder();
		for (Object item : list) {
			if (s.length() != 0) {
				s.append(separation);
			}
			s.append(item);
		}
		return s.toString();
	}

	// -------------------------------------------------------------------------

	/**
	 * Creates string list from string. Dual to getListAsString().
	 * 
	 * @param separatedString
	 *            the separated string
	 * @param separation
	 *            the separation
	 * @return the list from string
	 */
	public static List<String> getListFromString(String separatedString,
			String separation) {
		if (separatedString == null) {
			return new ArrayList<String>();
		}
		return Arrays.asList(separatedString.split(separation));
	}

	// -------------------------------------------------------------------------

	/**
	 * Creates an integer list from string. Dual to getListAsString().
	 * 
	 * @param separatedString
	 *            the separated string
	 * @param separation
	 *            the separation
	 * @param defaultValue
	 *            the default value
	 * @return the list from string
	 */
	public static List<Integer> getListFromString(String separatedString,
			String separation, int defaultValue) {
		if (separatedString == null) {
			return new ArrayList<Integer>();
		}
		ArrayList<Integer> list = new ArrayList<Integer>();
		String[] array = separatedString.split(separation);
		for (String elem : array) {
			list.add(Utility.parseInt(elem, defaultValue));
		}
		return list;
	}

	// -------------------------------------------------------------------------

	public static String cutTextIntoOneLine(String text, int maxWidth,
			int textSize) {
		try {
			Paint paint = new Paint();
			Rect bounds = new Rect();
			int textWidth = 0;
			paint.setTypeface(Typeface.DEFAULT);// your preference here
			paint.setTextSize(textSize);// have this the same as your text size

			String outText = text;
			String outText2 = text;

			boolean modified = false;
			boolean cutDown = false;
			while (true) {
				if (modified) {
					paint.getTextBounds(outText2, 0, outText2.length(), bounds);
				} else {
					paint.getTextBounds(outText, 0, outText.length(), bounds);
				}
				textWidth = bounds.width();
				if (textWidth <= maxWidth) {
					break;
				} else {
					modified = true;
					if (!cutDown) {
						cutDown = true;
						int estimatedLen = (outText.length() * maxWidth)
								/ textWidth;
						estimatedLen += 20; // be carefull!
						if (estimatedLen > outText.length()) {
							estimatedLen = outText.length();
						}
						outText = outText.substring(0, estimatedLen);
						outText2 = outText + "...";
					} else {
						// reduce by one character
						outText = outText.substring(0, outText.length() - 1);
						outText2 = outText + "...";
					}
				}
			}
			if (modified) {
				return outText2;
			} else {
				return outText;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	// -------------------------------------------------------------------------

	public static int getScreenWidth(Context context) {
		WindowManager wm = (WindowManager) context
				.getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		return display.getWidth(); // API LEVEL 11
		// API LEVEL 13
		// Point size = new Point();
		// display.getSize(size);
		// int width = size.x;
		// int height = size.y;
		// return width;
	}

	public static int getScreenHeight(Context context) {
		WindowManager wm = (WindowManager) context
				.getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		return display.getHeight(); // API LEVEL 11
		// API LEVEL 13
		// Point size = new Point();
		// display.getSize(size);
		// int width = size.x;
		// int height = size.y;
		// return height;
	}

	// -------------------------------------------------------------------------
	// -------------------------------------------------------------------------

	public static String getPhoneNumber(Context context) {
		TelephonyManager tMgr = (TelephonyManager) context
				.getSystemService(Context.TELEPHONY_SERVICE);
		String mPhoneNumber = tMgr.getLine1Number();
		return mPhoneNumber;
	}

	public static boolean isValidPhoneNumber(String number) {
		if (number == null) {
			return false;
		}
		if (number.length() < 10) {
			return false;
		}
		if (!number.startsWith("+")) {
			return false;
		}
		if (Utility.parseLong(number.substring(1), -1) == -1) {
			return false;
		}
		return true;
	}

	// -------------------------------------------------------------------------

	public static void errorVibrate(Context context) {
		Vibrator vibrator = (Vibrator) context
				.getSystemService(Context.VIBRATOR_SERVICE);
		try {
			vibrator.vibrate(50);
			Thread.sleep(120);
			vibrator.vibrate(50);
			Thread.sleep(120);
			vibrator.vibrate(50);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	// -------------------------------------------------------------------------

	public static String convertBASE64ToString(String encodedString) {
		return new String(Base64.decode(encodedString.getBytes(),
				Base64.DEFAULT));
	}

	public static String convertStringToBASE64(String originalString) {
		return Base64.encodeToString(originalString.getBytes(), Base64.DEFAULT);
	}

	// -------------------------------------------------------------------------

	public static String getRandomString(int len) {
		Random r = new Random();
		String returnString = "";
		for (int c = 0; c < len; c++) {
			int upper = (int) (Math.random() * 1);
			String alphabet = "0123456789abcdefghijklmnopqrstuvwxyz";
			String rndChar = String.valueOf(alphabet.charAt(r.nextInt(alphabet
					.length())));
			if (upper > 0) {
				rndChar = rndChar.toUpperCase();
			}
			returnString += rndChar;
		}
		return returnString;
	}

	// -------------------------------------------------------------------------

	public static boolean isScreenLocked(Context context) {
		KeyguardManager myKM = (KeyguardManager) context
				.getSystemService(Context.KEYGUARD_SERVICE);
		if (myKM.inKeyguardRestrictedInputMode()) {
			// it is locked
			return true;
		} else {
			// it is not locked
			return false;
		}
	}

	// -------------------------------------------------------------------------

	public static LinearLayout setContentViewWithCustomTitle(Activity activity,
			int resIdMainLayout, int resIdTitle) {
		Context context = activity.getApplicationContext();

		// Inflate the given layouts
		LayoutInflater inflaterInfo = (LayoutInflater) activity
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View titleView = (View) inflaterInfo.inflate(resIdTitle, null);
		View mainView = (View) inflaterInfo.inflate(resIdMainLayout, null);

		// Own custom title bar
		//
		// ATTENTION:
		// ADD THIS TO THEME <item name="android:windowNoTitle">true</item>
		activity.requestWindowFeature(Window.FEATURE_NO_TITLE);
		// We can ONLY disable the original title bar because you cannot combine
		// HOLO theme with a CUSTOM title bar :(
		// So we make our own title bar instead!

		// ALSO REMOVE TITLEBAR FROM APPLICATION AT STARTUP:
		// ADD TO MANIFEST
		// android:theme="@android:style/Theme.NoTitleBar"

		// THE FOLLOWING IS NOT WORKING WITH HOLO
		// requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		// setContentView(R.layout.activity_main);
		// getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
		// R.layout.title_main);

		// Create title layout
		LinearLayout.LayoutParams lpTitle = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		LinearLayout titleLayout = new LinearLayout(context);
		titleLayout.setOrientation(LinearLayout.VERTICAL);
		titleLayout.addView(titleView);
		titleLayout.setLayoutParams(lpTitle);

		// Create main layout
		LinearLayout.LayoutParams lpMain = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.MATCH_PARENT);
		LinearLayout mainLayout = new LinearLayout(context);
		mainLayout.setOrientation(LinearLayout.VERTICAL);
		mainLayout.addView(mainView);
		mainLayout.setLayoutParams(lpMain);

		// Create root outer layout
		LinearLayout outerLayout = new LinearLayout(context);
		outerLayout.setOrientation(LinearLayout.VERTICAL);
		outerLayout.addView(titleLayout);
		outerLayout.addView(mainLayout);

		// outerLayout.setBackgroundColor(Color.rgb(255, 0, 0));
		// mainLayout.setBackgroundColor(Color.rgb(0, 255, 0));
		// titleLayout.setBackgroundColor(Color.rgb(0, 0, 255));

		// lpSectionInnerLeft.setMargins(20, 5, 0, 15);
		// LinearLayout.LayoutParams lpSectionInnerRight = new
		// LinearLayout.LayoutParams(
		// 90, LinearLayout.LayoutParams.WRAP_CONTENT, 0f);
		// lpSectionInnerRight.setMargins(0, 5, 15, 15);

		// After setting NO TITLE .. apply the layout
		activity.setContentView(outerLayout);

		return mainLayout;
	}

	// -------------------------------------------------------------------------

	public static void killOwnProcess() {
		android.os.Process.killProcess(android.os.Process.myPid());
	}

	public static void killOwnProcessDelayed(final int milliseconds) {
		new Thread(new Runnable() {
			public void run() {
				try {
					Thread.sleep(milliseconds);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				android.os.Process.killProcess(android.os.Process.myPid());
			}
		}).start();

	}

	// -------------------------------------------------------------------------

	/**
	 * No screenshots. Call this in OnCreate BEFORE setting the layout!
	 * 
	 * @param activity
	 *            the activity
	 */
	public static void noScreenshots(Activity activity) {
		// ON GINGERBREAD DEVICES (esp from SAMSUNG) there may be a bug
		// scrambling the view
		// if this is set.
		// See:
		// http://stackoverflow.com/questions/9822076/how-do-i-prevent-android-taking-a-screenshot-when-my-app-goes-to-the-background
		if (android.os.Build.VERSION.SDK_INT != android.os.Build.VERSION_CODES.GINGERBREAD
				&& android.os.Build.VERSION.SDK_INT != android.os.Build.VERSION_CODES.GINGERBREAD_MR1) {
			activity.getWindow().setFlags(
					WindowManager.LayoutParams.FLAG_SECURE,
					WindowManager.LayoutParams.FLAG_SECURE);
		}
	}

	// -------------------------------------------------------------------------

	public static boolean isOrientationLandscape(Context context) {
		Display display = ((WindowManager) context
				.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
		// int rotation = display.getRotation();
		int rotation = display.getOrientation();
		return (rotation != 0 && rotation != 180);
	}

	// -------------------------------------------------------------------------

	// alarmType ==
	// RingtoneManager.TYPE_ALARM
	// RingtoneManager.TYPE_NOTIFICATION
	// RingtoneManager.TYPE_RINGTONE
	//
	public static void notfiyAlarm(Context context, final int alarmType) {
		// final MediaPlayer player = MediaPlayer.create(context,
		// RingtoneManager.getDefaultUri(alarmType));
		final Ringtone ringtone = RingtoneManager.getRingtone(context,
				RingtoneManager.getDefaultUri(alarmType));
		new Thread(new Runnable() {
			public void run() {
				ringtone.play();
				// player.start();
				// while (player.isPlaying()) {
				// try {
				// Thread.sleep(1000);
				// } catch (InterruptedException e) {
				// }
				// }

			}
		}).start();
	}

	// -------------------------------------------------------------------------
	// -------------------------------------------------------------------------
}
