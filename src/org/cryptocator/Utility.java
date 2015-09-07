/*
 * Copyright (c) 2015, Christian Motika. Dedicated to Sara.
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
 * notice, an acknowledgment to all contributors, this list of conditions
 * and the following disclaimer in the documentation and/or other materials
 * provided with the distribution.
 * 
 * 3. Neither the name Delphino Cryptocator nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * 4. Free or commercial forks of Cryptocator are permitted as long as
 *    both (a) and (b) are and stay fulfilled. 
 *    (a) this license is enclosed
 *    (b) the protocol to communicate between Cryptocator servers
 *        and Cryptocator clients *MUST* must be fully conform with 
 *        the documentation and (possibly updated) reference 
 *        implementation from cryptocator.org. This is to ensure 
 *        interconnectivity between all clients and servers. 
 * 
 * THIS SOFTWARE IS PROVIDED BY THE CONTRIBUTORS “AS IS” AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE CONTRIBUTORS
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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader.TileMode;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaScannerConnection;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

/**
 * The Utility class has convenience methods for enabling the access of often
 * used or system functionality across projects.
 * 
 * @author Christian Motika
 * @since 1.2
 * @date 08/23/2015
 * 
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class Utility {

	/** The shared settings for one program instance. */
	private static SharedPreferences settings;

	/**
	 * The constant PREFERENCESURI should be updated when used in another
	 * project. It should contain a unique URI for the project, e.g., the
	 * package name.
	 */
	public static final String PREFERENCESURI = "org.cryptocator";

	// -------------------------------------------------------------------------

	/**
	 * Hide soft keys in full screen. This only applies to HONECOMB+ Android
	 * versions. Older versions rely on hardware keys and do not have soft keys
	 * that might get annoying in a full screen application.
	 * 
	 * @param activity
	 *            the activity
	 */
	@SuppressLint("InlinedApi")
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

	/**
	 * Hide the soft keyboard. Consider also using hideKeyboardExplicit() if
	 * there is a text input field.
	 * 
	 * @param activity
	 *            the activity
	 */
	public static void hideKeyboard(Activity activity) {
		// This can be used to suppress the keyboard until the user actually
		// touches the edittext view.
		activity.getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
	}

	// -------------------------------------------------------------------------

	/**
	 * Hide the keyboard explicitly for a text input field known to be able to
	 * get the focus.
	 * 
	 * @param textInput
	 *            the text input
	 */
	public static void hideKeyboardExplicit(EditText textInput) {
		// hide keyboard explicitly
		InputMethodManager imm = (InputMethodManager) textInput.getContext()
				.getSystemService(Context.INPUT_METHOD_SERVICE);
		// imm.restartInput(addUserName);
		imm.hideSoftInputFromWindow(textInput.getWindowToken(), 0);
	}

	// -------------------------------------------------------------------------

	/**
	 * Show keyboard explicitly for a text input field known to be able to get
	 * the focus.
	 * 
	 * @param textInput
	 *            the text input
	 */
	public static void showKeyboardExplicit(EditText textInput) {
		InputMethodManager keyboard = (InputMethodManager) textInput
				.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
		keyboard.showSoftInput(textInput, 0);
	}

	// -------------------------------------------------------------------------
	// -------------------------------------------------------------------------

	/**
	 * Get or update the settings for this application.
	 * 
	 * @param context
	 *            the context
	 * @return the settings
	 */
	public static SharedPreferences getSettings(Context context) {
		if (Utility.settings != null) {
			return Utility.settings;
		}
		Utility.settings = context.getSharedPreferences(Utility.PREFERENCESURI,
				Activity.MODE_PRIVATE);
		return Utility.settings;
	}

	// -------------------------------------------------------------------------

	/**
	 * Build the settings map. Delimited with @@@, key:value.
	 * 
	 * @param settingsString
	 *            the settings string
	 * @return the hash map
	 */
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

	/**
	 * Load a String setting.
	 * 
	 * @param context
	 *            the context
	 * @param id
	 *            the id
	 * @param defaultValue
	 *            the default value
	 * @return the string
	 */
	public static String loadStringSetting(Context context, String id,
			String defaultValue) {
		return loadStringSetting(context, id, defaultValue, null);
	}

	// -------------------------------------------------------------------------

	/**
	 * Load a string setting.
	 * 
	 * @param context
	 *            the context
	 * @param id
	 *            the id
	 * @param defaultValue
	 *            the default value
	 * @param settingsMap
	 *            the settings map
	 * @return the string
	 */
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

	/**
	 * Load an int setting.
	 * 
	 * @param context
	 *            the context
	 * @param id
	 *            the id
	 * @param defaultValue
	 *            the default value
	 * @return the int
	 */
	public static int loadIntSetting(Context context, String id,
			int defaultValue) {
		return loadIntSetting(context, id, defaultValue, null);
	}

	// -------------------------------------------------------------------------

	/**
	 * Load an int setting.
	 * 
	 * @param context
	 *            the context
	 * @param id
	 *            the id
	 * @param defaultValue
	 *            the default value
	 * @param settingsMap
	 *            the settings map
	 * @return the int
	 */
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

	/**
	 * Load a long setting.
	 * 
	 * @param context
	 *            the context
	 * @param id
	 *            the id
	 * @param defaultValue
	 *            the default value
	 * @return the long
	 */
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

	/**
	 * Load a double setting.
	 * 
	 * @param context
	 *            the context
	 * @param id
	 *            the id
	 * @param defaultValue
	 *            the default value
	 * @return the double
	 */
	public static double loadDoubleSetting(Context context, String id,
			double defaultValue) {
		String doubleString = loadStringSetting(context, id, defaultValue + "");
		double returnValue = parseDouble(doubleString, defaultValue);
		return returnValue;
	}

	// -------------------------------------------------------------------------

	/**
	 * Load boolean setting.
	 * 
	 * @param context
	 *            the context
	 * @param id
	 *            the id
	 * @param defaultValue
	 *            the default value
	 * @return true, if successful
	 */
	public static boolean loadBooleanSetting(Context context, String id,
			boolean defaultValue) {
		return loadBooleanSetting(context, id, defaultValue, null);
	}

	// -------------------------------------------------------------------------

	/**
	 * Load a boolean setting.
	 * 
	 * @param context
	 *            the context
	 * @param id
	 *            the id
	 * @param defaultValue
	 *            the default value
	 * @param settingsMap
	 *            the settings map
	 * @return true, if successful
	 */
	@SuppressLint("DefaultLocale")
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

	/**
	 * Save a boolean setting.
	 * 
	 * @param context
	 *            the context
	 * @param id
	 *            the id
	 * @param value
	 *            the value
	 */
	public static void saveBooleanSetting(Context context, String id,
			boolean value) {
		saveBooleanSetting(context, id, value, null);
	}

	// -------------------------------------------------------------------------

	/**
	 * Save a boolean setting.
	 * 
	 * @param context
	 *            the context
	 * @param id
	 *            the id
	 * @param value
	 *            the value
	 * @param settingsString
	 *            the settings string
	 */
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

	/**
	 * Save an int setting.
	 * 
	 * @param context
	 *            the context
	 * @param id
	 *            the id
	 * @param value
	 *            the value
	 */
	public static void saveIntSetting(Context context, String id, int value) {
		saveIntSetting(context, id, value, null);
	}

	// -------------------------------------------------------------------------

	/**
	 * Save an int setting.
	 * 
	 * @param context
	 *            the context
	 * @param id
	 *            the id
	 * @param value
	 *            the value
	 * @param settingsString
	 *            the settings string
	 */
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

	/**
	 * Save a long setting.
	 * 
	 * @param context
	 *            the context
	 * @param id
	 *            the id
	 * @param value
	 *            the value
	 */
	public static void saveLongSetting(Context context, String id, long value) {
		// Update the settings variable
		getSettings(context);
		SharedPreferences.Editor settingsEditor = settings.edit();
		settingsEditor.putLong(id, value);
		settingsEditor.commit();
	}

	// -------------------------------------------------------------------------

	/**
	 * Save a double setting.
	 * 
	 * @param context
	 *            the context
	 * @param id
	 *            the id
	 * @param value
	 *            the value
	 */
	public static void saveDoubleSetting(Context context, String id,
			double value) {
		saveStringSetting(context, id, value + "");
	}

	// -------------------------------------------------------------------------

	/**
	 * Save a string setting.
	 * 
	 * @param context
	 *            the context
	 * @param id
	 *            the id
	 * @param value
	 *            the value
	 */
	public static void saveStringSetting(Context context, String id,
			String value) {
		saveStringSetting(context, id, value, null);
	}

	// -------------------------------------------------------------------------

	/**
	 * Save a string setting.
	 * 
	 * @param context
	 *            the context
	 * @param id
	 *            the id
	 * @param value
	 *            the value
	 * @param settingsString
	 *            the settings string
	 */
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

	/**
	 * Sets the background tiled for a view element according to a given
	 * resourceID.
	 * 
	 * @param activity
	 *            the activity
	 * @param view
	 *            the view
	 * @param resourceID
	 *            the resource id
	 */
	@SuppressWarnings("deprecation")
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

	/**
	 * Parses the int.
	 * 
	 * @param integerString
	 *            the integer string
	 * @param defaultValue
	 *            the default value
	 * @return the int
	 */
	public static int parseInt(String integerString, int defaultValue) {
		int returnInteger = -1;
		try {
			returnInteger = Integer.parseInt(integerString);
			return returnInteger;
		} catch (Exception e) {
			return defaultValue;
		}
	}

	// ---------------------------------------------------------------------------

	/**
	 * Parses the long.
	 * 
	 * @param longString
	 *            the long string
	 * @param defaultValue
	 *            the default value
	 * @return the long
	 */
	public static long parseLong(String longString, long defaultValue) {
		long returnInteger = -1;
		try {
			returnInteger = Long.parseLong(longString);
			return returnInteger;
		} catch (Exception e) {
			return defaultValue;
		}
	}

	// ---------------------------------------------------------------------------

	/**
	 * Parses the double.
	 * 
	 * @param doubleString
	 *            the double string
	 * @param defaultValue
	 *            the default value
	 * @return the double
	 */
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

	/**
	 * Encode for URL.
	 * 
	 * @param text
	 *            the text
	 * @return the string
	 */
	public static String urlEncode(String text) {
		try {
			return URLEncoder.encode(text, "utf-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}

	// ---------------------------------------------------------------------------

	/**
	 * decode from URL.
	 * 
	 * @param text
	 *            the text
	 * @return the string
	 */
	public static String urlDecode(String urltext) {
		try {
			return URLDecoder.decode(urltext, "utf-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}

	// ---------------------------------------------------------------------------

	/**
	 * Gets the data.
	 * 
	 * @param context
	 *            the context
	 * @param url
	 *            the url
	 * @return the data
	 * @throws ClientProtocolException
	 *             the client protocol exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
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

	/**
	 * Get the content as string.
	 * 
	 * @param response
	 *            the response
	 * @return the content as string
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
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

	/**
	 * Re-encode iso. Convert from internal Java String format to internal Java
	 * String format.
	 * 
	 * @param s
	 *            the s
	 * @return the string
	 */
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

	/**
	 * Convert to UTF8. Convert from internal Java String format to UTF-8.
	 * 
	 * @param s
	 *            the s
	 * @return the string
	 */
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

	/**
	 * Convert from UTF8. Convert from UTF-8 to internal Java String format.
	 * 
	 * @param s
	 *            the s
	 * @return the string
	 */
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

	/**
	 * Gets the tile number for OpenStreetMap.
	 * 
	 * @param lat
	 *            the lat
	 * @param lon
	 *            the lon
	 * @param zoom
	 *            the zoom
	 * @return the tile number
	 */
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

	/**
	 * Update tile for OpenStreetMap.
	 * 
	 * @param context
	 *            the context
	 * @param image
	 *            the image
	 * @param zoom
	 *            the zoom
	 * @param latitude
	 *            the latitude
	 * @param longitude
	 *            the longitude
	 */
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

	/**
	 * Get the date.
	 * 
	 * @param millis
	 *            the millis
	 * @return the date
	 */
	@SuppressLint("SimpleDateFormat")
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

	// -------------------------------------------------------------------------

	/**
	 * Get the time title in form DATE @ HOURS:MINUTES, e.g., Mon, 3. Oct 10 @
	 * 10:20.
	 * 
	 * @param millis
	 *            the millis
	 * @return the time title
	 */
	public static String getTimeTitle(long millis) {
		return getDate(millis) + " @ " + getHours(millis) + ":"
				+ getMinutes(millis);
	}

	// -------------------------------------------------------------------------

	/**
	 * Get the hours.
	 * 
	 * @param millis
	 *            the millis
	 * @return the hours
	 */
	@SuppressLint("SimpleDateFormat")
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

	// -------------------------------------------------------------------------

	/**
	 * Get the year.
	 * 
	 * @param millis
	 *            the millis
	 * @return the year
	 */
	@SuppressLint("SimpleDateFormat")
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

	// -------------------------------------------------------------------------

	/**
	 * Get the minutes.
	 * 
	 * @param millis
	 *            the millis
	 * @return the minutes
	 */
	@SuppressLint("SimpleDateFormat")
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

	// -------------------------------------------------------------------------

	/**
	 * Get the seconds.
	 * 
	 * @param millis
	 *            the millis
	 * @return the seconds
	 */
	@SuppressLint("SimpleDateFormat")
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

	// -------------------------------------------------------------------------

	/**
	 * Get the milli seconds.
	 * 
	 * @param millis
	 *            the millis
	 * @return the milli seconds
	 */
	@SuppressLint("SimpleDateFormat")
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

	/**
	 * Gets the device id. This method needs: <uses-permission
	 * android:name="android.permission.READ_PHONE_STATE" />
	 * 
	 * @param context
	 *            the context
	 * @return the device id
	 */
	@SuppressLint("DefaultLocale")
	public static String getDeviceId(Context context) {
		return Secure
				.getString(context.getContentResolver(), Secure.ANDROID_ID)
				.toUpperCase();
	}

	// -------------------------------------------------------------------------

	/**
	 * Show toast short async from non-UI thread.
	 * 
	 * @param context
	 *            the context
	 * @param toastText
	 *            the toast text
	 */
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

	/**
	 * Show toast in ui thread.
	 * 
	 * @param context
	 *            the context
	 * @param toastText
	 *            the toast text
	 */
	public static void showToastInUIThread(final Context context,
			final String toastText) {
		int duration = Toast.LENGTH_SHORT;
		Toast toast = Toast.makeText(context, toastText, duration);
		toast.show();
	}

	// -------------------------------------------------------------------------

	/**
	 * Show toast async from non-UI thread.
	 * 
	 * @param context
	 *            the context
	 * @param toastText
	 *            the toast text
	 */
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
	 * Copy text to clipboard. This method checks for honeycomp changes to the
	 * clipboard.
	 * 
	 * @param context
	 *            the context
	 * @param text
	 *            the text
	 */
	@SuppressWarnings("deprecation")
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
	 * Paste text from clipboard. This method checks for honeycomp changes to
	 * the clipboard.
	 * 
	 * @param context
	 *            the context
	 * @return the string
	 */
	@SuppressWarnings("deprecation")
	public static String pasteFromClipboard(final Context context) {
		int sdk = android.os.Build.VERSION.SDK_INT;
		if (sdk < android.os.Build.VERSION_CODES.HONEYCOMB) {
			android.text.ClipboardManager clipboard = (android.text.ClipboardManager) context
					.getSystemService(Context.CLIPBOARD_SERVICE);
			return clipboard.getText().toString();
		} else {
			android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context
					.getSystemService(Context.CLIPBOARD_SERVICE);
			return clipboard.getText().toString();
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
	 * Create an MD5 from a string.
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
	 * Check if is phone completely muted (even no vibration).
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
	 * Check if is phone muted but vibration may still be on.
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
	 * Get the list as string where the values are separated by separation.
	 * 
	 * @param list
	 *            the list
	 * @param separation
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
	 * Create string list from string. Dual to getListAsString().
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
	 * Create an integer list from string. Dual to getListAsString().
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

	/**
	 * Cut text into one line.
	 * 
	 * @param text
	 *            the text
	 * @param maxWidth
	 *            the max width
	 * @param textSize
	 *            the text size
	 * @return the string
	 */
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

	/**
	 * Get the screen width.
	 * 
	 * @param context
	 *            the context
	 * @return the screen width
	 */
	@SuppressWarnings("deprecation")
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

	// -------------------------------------------------------------------------

	/**
	 * Get the screen height.
	 * 
	 * @param context
	 *            the context
	 * @return the screen height
	 */
	@SuppressWarnings("deprecation")
	public static int getScreenHeight(Context context) {
		WindowManager wm = (WindowManager) context
				.getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		return display.getHeight();
		// API LEVEL 11
		// API LEVEL 13
		// Point size = new Point();
		// display.getSize(size);
		// int width = size.x;
		// int height = size.y;
		// return height;
	}

	// -------------------------------------------------------------------------
	// -------------------------------------------------------------------------

	/**
	 * Gets the phone number.
	 * 
	 * @param context
	 *            the context
	 * @return the phone number
	 */
	public static String getPhoneNumber(Context context) {
		TelephonyManager tMgr = (TelephonyManager) context
				.getSystemService(Context.TELEPHONY_SERVICE);
		String mPhoneNumber = tMgr.getLine1Number();
		return mPhoneNumber;
	}

	// -------------------------------------------------------------------------

	/**
	 * Checks if is valid phone number.
	 * 
	 * @param number
	 *            the number
	 * @return true, if is valid phone number
	 */
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

	/**
	 * Error vibrate.
	 * 
	 * @param context
	 *            the context
	 */
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

	/**
	 * Convert BASE64 to string.
	 * 
	 * @param encodedString
	 *            the encoded string
	 * @return the string
	 */
	public static String convertBASE64ToString(String encodedString) {
		return new String(Base64.decode(encodedString.getBytes(),
				Base64.DEFAULT));
	}

	// -------------------------------------------------------------------------

	/**
	 * Convert string to BASE64
	 * 
	 * @param originalString
	 *            the original string
	 * @return the string
	 */
	public static String convertStringToBASE64(String originalString) {
		return Base64.encodeToString(originalString.getBytes(), Base64.DEFAULT);
	}

	// -------------------------------------------------------------------------

	/**
	 * Gets random string of specified length.
	 * 
	 * @param len
	 *            the len
	 * @return the random string
	 */
	@SuppressLint("DefaultLocale")
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

	/**
	 * Checks if is screen locked.
	 * 
	 * @param context
	 *            the context
	 * @return true, if is screen locked
	 */
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

	/**
	 * Sets the content view with custom title. This is necessary for a holo
	 * layout where a custom title bar is normally not permitted.
	 * 
	 * @param activity
	 *            the activity
	 * @param resIdMainLayout
	 *            the res id main layout
	 * @param resIdTitle
	 *            the res id title
	 * @return the linear layout
	 */
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

	/**
	 * Kill own process.<BR>
	 * <BR>
	 * ATTENTION: Use with care! Typically we need to let the OS handle process
	 * elimination. Only in very rare situations we need to kill ourselves!
	 */
	public static void killOwnProcess() {
		android.os.Process.killProcess(android.os.Process.myPid());
	}

	// -------------------------------------------------------------------------

	/**
	 * Kill own process delayed by some milliseconds.<BR>
	 * <BR>
	 * ATTENTION: Use with care! Typically we need to let the OS handle process
	 * elimination. Only in very rare situations we need to kill ourselves!
	 * 
	 * @param milliseconds
	 *            the milliseconds
	 */
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

	/**
	 * Checks if is orientation landscape.
	 * 
	 * @param context
	 *            the context
	 * @return true, if is orientation landscape
	 */
	@SuppressWarnings("deprecation")
	public static boolean isOrientationLandscape(Context context) {
		Display display = ((WindowManager) context
				.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
		// int rotation = display.getRotation();
		int rotation = display.getOrientation();
		return (rotation != 0 && rotation != 180);
	}

	// -------------------------------------------------------------------------

	/**
	 * Notfiy alarm. alarmType == RingtoneManager.TYPE_ALARM |
	 * RingtoneManager.TYPE_NOTIFICATION | RingtoneManager.TYPE_RINGTONE
	 * 
	 * @param context
	 *            the context
	 * @param alarmType
	 *            the alarm type
	 */
	public static void notfiyAlarm(Context context, final int alarmType) {
		final Ringtone ringtone = RingtoneManager.getRingtone(context,
				RingtoneManager.getDefaultUri(alarmType));
		new Thread(new Runnable() {
			public void run() {
				ringtone.play();
			}
		}).start();
	}

	// -------------------------------------------------------------------------

	/**
	 * Reads a file and encodes the bytes with BASE64 for transmission.
	 * 
	 * @param attachmentPath
	 *            the attachment path
	 * @return the encoded image
	 */
	public static String getEncodedFile(String attachmentPath) {
		try {
			byte[] bytes = getFile(attachmentPath);
			String encodedFile = Base64.encodeToString(bytes, Base64.DEFAULT);
			return encodedFile;
		} catch (Exception e) {
			// Ignore, return null
			e.printStackTrace();
		}
		return null;
	}

	// -------------------------------------------------------------------------

	/**
	 * Reads a file and returns the bytes
	 * 
	 * @param attachmentPath
	 *            the attachment path
	 * @return the encoded image
	 */
	public static byte[] getFile(String attachmentPath) {
		File attachmentFile = new File(attachmentPath);
		int fileSize = (int) attachmentFile.length();
		byte[] bytes = new byte[fileSize];
		try {
			BufferedInputStream buf = new BufferedInputStream(
					new FileInputStream(attachmentFile));
			buf.read(bytes, 0, bytes.length);
			buf.close();
			return bytes;
		} catch (Exception e) {
			// Ignore, return null
			e.printStackTrace();
		}
		return null;
	}

	// -------------------------------------------------------------------------

	/**
	 * Gets the bitmap from bytes.
	 * 
	 * @param bytes
	 *            the bytes
	 * @return the bitmap from bytes
	 */
	public static Bitmap getBitmapFromBytes(byte[] bytes) {
		Bitmap bitmap = null;
		try {
			if (Build.VERSION.SDK_INT < 19) {
				bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
			} else {
				try {
					bitmap = BitmapFactory.decodeByteArray(bytes, 0,
							bytes.length);
				} catch (Exception e) {
					// Fallback
					e.printStackTrace();
					try {
						bitmap = BitmapFactory.decodeByteArray(bytes, 0,
								bytes.length);
					} catch (Exception e2) {
						// fail slient
						e2.printStackTrace();
						return null;
					}
				}
			}
		} catch (Exception e) {
		}
		return bitmap;
	}

	// -------------------------------------------------------------------------

	/**
	 * Gets the drawable from bitmap.
	 * 
	 * @param bitmap
	 *            the bitmap
	 * @return the drawable from bitmap
	 */
	public static Drawable getDrawableFromBitmap(Context context, Bitmap bitmap) {
		if (bitmap != null) {
			Drawable drawable = new BitmapDrawable(context.getResources(),
					bitmap);
			return drawable;
		}
		return null;
	}

	// -------------------------------------------------------------------------

	/**
	 * Load drawable image from a BASE64 encoded String.
	 * 
	 * @param encodedImage
	 *            the encoded image
	 * @return the drawable
	 */
	public static Drawable loadDrawableFromBASE64String(String encodedImage) {
		byte[] imageBytes = Base64.decode(encodedImage.getBytes(),
				Base64.DEFAULT);
		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
				imageBytes);
		Drawable drawable = Drawable.createFromStream(byteArrayInputStream,
				"attachment");
		return drawable;
	}

	// -------------------------------------------------------------------------

	/**
	 * Load a bitmap image from a BASE64 encoded String.
	 * 
	 * @param context
	 *            the context
	 * @param encodedImage
	 *            the encoded image
	 * @return the bitmap
	 */
	public static Bitmap loadImageFromBASE64String(Context context,
			String encodedImage) {
		byte[] imageBytes = Base64.decode(encodedImage.getBytes(),
				Base64.DEFAULT);
		return getBitmapFromBytes(imageBytes);
	}

	// -------------------------------------------------------------------------

	/**
	 * Get a resized version of a bitmap image.
	 * 
	 * @param bitmap
	 *            the bitmap
	 * @param maxWidth
	 *            the max width
	 * @param maxHeight
	 *            the max height
	 * @return the resized image
	 */
	public static Bitmap getResizedImage(Bitmap bitmap, int maxWidth,
			int maxHeight, boolean deleteSource) {
		int bitmapWidth = bitmap.getWidth();
		int bitmapHeight = bitmap.getHeight();
		int newWidth = bitmapWidth;
		int newHeight = bitmapHeight;

		Log.d("communicator", "RESIZE: maxWidth=" + maxWidth + ", maxHeight="
				+ maxHeight);
		Log.d("communicator", "RESIZE: bmpW=" + bitmapWidth + ", bmpH="
				+ bitmapHeight);

		if (bitmapWidth > bitmapHeight) {
			// Log.d("communicator", "RESIZE Landscape: bitmapWidth="
			// + bitmapWidth + " >? " + maxWidth + "=maxWidth");
			// Landscape
			// if (bitmapWidth > maxWidth) {
			float scale = ((float) bitmapWidth) / ((float) maxWidth);
			// Log.d("communicator", "RESIZE: (1) scale=" + scale);
			newWidth = maxWidth;
			newHeight = (int) ((float) bitmapHeight / scale);
			// } else if (bitmapHeight > maxHeight) {
			// float scale = ((float) bitmapHeight) / ((float) maxHeight);
			// // Log.d("communicator", "RESIZE: (2) scale=" + scale);
			// newHeight = maxHeight;
			// newWidth = (int) ((float) bitmapWidth / scale);
			// }

		} else {
			// Log.d("communicator", "RESIZE Portrait: bitmapHeight="
			// + bitmapHeight + " >? " + maxHeight + "=maxHeight");
			// Portrait
			// if (bitmapHeight > maxHeight) {
			float scale = ((float) bitmapHeight) / ((float) maxHeight);
			// Log.d("communicator", "RESIZE: (3) scale=" + scale);
			newHeight = maxHeight;
			newWidth = (int) ((float) bitmapWidth / scale);
			// } else if (bitmapWidth > maxWidth) {
			// float scale = ((float) bitmapWidth) / ((float) maxWidth);
			// // Log.d("communicator", "RESIZE: (4) scale=" + scale);
			// newWidth = maxWidth;
			// newHeight = (int) ((float) bitmapHeight / scale);
			// }
		}
		Log.d("communicator", "RESIZE RESULT: newWidth=" + newWidth + ", "
				+ newHeight + "=newHeight");
		Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth,
				newHeight, true);
		if (deleteSource) {
			bitmap.recycle();
			System.gc();
		}
		return scaledBitmap;
	}

	// -------------------------------------------------------------------------

	/**
	 * Smart paste some pasteText into an editText. Remember the position (i)
	 * and jump behind the pastedText afterwards if not jumpToEnd is selected.
	 * If selectAfterPaste is set then select the pasted text.
	 * 
	 * 
	 * @param editText
	 *            the edit text
	 * @param pasteText
	 *            the paste text
	 * @param ensureBefore
	 *            the ensure before
	 * @param ensureAfter
	 *            the ensure after
	 * @param jumpToEnd
	 *            the jump to end
	 */
	public static void smartPaste(EditText editText, String pasteText,
			String ensureBefore, String ensureAfter, boolean jumpToEnd,
			boolean selectAfterPaste, boolean ensureBeforeOnlyIfNotBeginning) {
		// messageText.getText().append(textualSmiley);
		// if text was selected replace the text
		int i = editText.getSelectionStart();
		int e = editText.getSelectionEnd();
		String prevText = editText.getText().toString();
		if (i < 0) {
			// default fallback is concatenation
			if (!prevText.endsWith(ensureBefore)) {
				if (!ensureBeforeOnlyIfNotBeginning || prevText.length() > 0) {
					prevText = prevText.concat(ensureBefore);
				}
			}
			editText.setText(prevText + pasteText + ensureAfter);
		} else {
			// otherwise try to fill in the text
			String text1 = prevText.substring(0, i);
			if (!text1.endsWith(ensureBefore)) {
				if (!ensureBeforeOnlyIfNotBeginning || prevText.length() > 0) {
					text1 = text1.concat(ensureBefore);
				}
			}
			if (e < 0) {
				e = i;
			}
			String text2 = prevText.substring(e);
			if (!text2.startsWith(ensureAfter)) {
				text2 = ensureAfter + text2;
			}
			editText.setText(text1.concat(pasteText).concat(text2));
		}
		if (selectAfterPaste) {
			editText.setSelection(i, i + pasteText.length());
		} else if (jumpToEnd) {
			editText.setSelection(editText.getText().length());
		} else {
			editText.setSelection(i);
		}
	}

	// -------------------------------------------------------------------------

	/**
	 * Stores an image in the gallery. This is a fixed version that stores
	 * correct DATE_TAKEN so that the ordering of remains correct.
	 * 
	 * @see android.provider.MediaStore.Images.Media#insertImage(ContentResolver,
	 *      Bitmap, String, String)
	 */
	public static final String insertImage(ContentResolver contentResolver,
			Bitmap bitmap, String title, String description) {

		final int SAVEQUALITY = 100;

		ContentValues values = new ContentValues();
		values.put(Images.Media.TITLE, title);
		values.put(Images.Media.DISPLAY_NAME, title);
		values.put(Images.Media.DESCRIPTION, description);
		values.put(Images.Media.MIME_TYPE, "image/jpeg");
		// Fix
		values.put(Images.Media.DATE_ADDED, System.currentTimeMillis());
		values.put(Images.Media.DATE_TAKEN, System.currentTimeMillis());

		Uri url = null;
		String returnValue = null;
		boolean ok = false;

		try {
			url = contentResolver.insert(
					MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

			if (bitmap != null) {
				OutputStream outputStream = contentResolver
						.openOutputStream(url);
				try {
					bitmap.compress(Bitmap.CompressFormat.JPEG, SAVEQUALITY,
							outputStream);
					ok = true;
				} catch (Exception e) {
					// ignore
				}
				outputStream.close();
			}
		} catch (Exception e) {
			// ignore
		}

		if (!ok) {
			// If something went wrong, delete the entry
			if (url != null) {
				contentResolver.delete(url, null, null);
				url = null;
			}
		}

		if (url != null) {
			returnValue = url.toString();
		}

		return returnValue;
	}

	// -------------------------------------------------------------------------

	/**
	 * Update media scanner so that the image is shown in the gallery without a
	 * reboot of the device.
	 * 
	 * @param context
	 *            the context
	 * @param imagePath
	 *            the image path
	 */
	public static void updateMediaScanner(Context context, String imagePath) {

		MediaScannerConnection.scanFile(context, new String[] { imagePath },
				null, new MediaScannerConnection.OnScanCompletedListener() {
					public void onScanCompleted(String path, Uri uri) {
					}
				});
	}

	// -------------------------------------------------------------------------

	/**
	 * Gets the bitmap from content uri.
	 * 
	 * @param context
	 *            the context
	 * @param imageUri
	 *            the image uri
	 * @return the bitmap from content uri
	 */
	public static Bitmap getBitmapFromContentUri(Context context, Uri imageUri) {
		Bitmap bitmap = null;
		try {
			InputStream input = context.getContentResolver().openInputStream(
					imageUri);
			bitmap = BitmapFactory.decodeStream(input);
		} catch (Exception e) {
			// handle exception
		}
		return bitmap;
	}

	// -------------------------------------------------------------------------

	/**
	 * Convert the image URI to the direct file system path of the image so that
	 * it can be loaded.
	 * 
	 * @param contentUri
	 *            the content uri
	 * @return the real path from uri
	 */
	@SuppressLint("NewApi")
	// file
	@SuppressWarnings("deprecation")
	public static String getRealPathFromURI(Activity activity, Uri contentUri) {
		String returnPath = null;

		Log.d("communicator", "IMPORT getRealPathFromURI() contentUri="
				+ contentUri.toString());

		try {
			if (Build.VERSION.SDK_INT < 19) {
				// can post image
				String[] proj = { MediaStore.Images.Media.DATA };
				Cursor cursor = activity.managedQuery(contentUri, proj, // Which
																		// columns
						// to
						// return
						null, // WHERE clause; which rows to return (all rows)
						null, // WHERE clause selection arguments (none)
						null); // Order-by clause (ascending by name)
				int column_index = cursor
						.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
				cursor.moveToFirst();
				returnPath = cursor.getString(column_index);
			} else if (Build.VERSION.SDK_INT > 19) {
				String[] projection = { MediaStore.Images.Media.DATA };
				String wholeID = DocumentsContract.getDocumentId(contentUri);
				String id = wholeID.split(":")[1];
				String sel = MediaStore.Images.Media._ID + "=?";
				Cursor cursor = activity.getContentResolver().query(
						MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
						projection, sel, new String[] { id }, null);
				int column_index = cursor
						.getColumnIndex(MediaStore.Images.Media.DATA);
				cursor.moveToFirst();
				returnPath = cursor.getString(column_index).toString();
				cursor.close();
			} else {
				returnPath = contentUri.toString();
			}
		} catch (Exception e) {
		}

		Log.d("communicator", "IMPORT getRealPathFromURI() returnPath="
				+ returnPath);

		return returnPath;
	}

	// -------------------------------------------------------------------------

	/**
	 * Checks if a is camera available on the current device.
	 * 
	 * @param context
	 *            the context
	 * @return true, if is camera available
	 */
	public static boolean isCameraAvailable(Context context) {
		PackageManager packageManager = context.getPackageManager();
		return (packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA));
	}

	// -------------------------------------------------------------------------

	/**
	 * Gets the length in KB with one decimal after comma.
	 * 
	 * @param lengthInBytes
	 *            the length in bytes
	 * @return the kb
	 */
	public static String getKB(int lengthInBytes) {
		int lenKB = (int) Math.ceil(((double) lengthInBytes) / 100);
		float lenKB2 = ((float) lenKB) / 10;
		return lenKB2 + "";
	}

	// -------------------------------------------------------------------------

}
