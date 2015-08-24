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

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Date;
import java.util.List;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.cryptocator.R;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Telephony;
import android.support.v4.util.Pair;
import android.text.InputType;
import android.text.method.KeyListener;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.os.Build;

public class Setup extends Activity {

	public static String BASEURLDEFAULT = "http://www.cryptocator.org";
	public static String DATABASEPREFIX = "dcomm";
	public static String DATABASEPOSTFIX = ".db";
	public static String DATABASESENDING = "sending.db";
	public static String DATABASESENT = "sent.db";
	public static String INTENTEXTRA = "org.cryptocator.hostuid";
	public static String APPLICATION_PACKAGE_NAME = "org.cryptocator";
	public static String GROUP_CRYPTOCATOR = "org.cryptocator.notificationgroup";

	private int accountLocked = 3;

	public static final int ADVANCEDSETUPCOUNTDOWNSTART = 11;
	private int advanedSetupCountdown = ADVANCEDSETUPCOUNTDOWNSTART;
	public static final String SETTINGS_BASEURL = "baseurl";
	private static String BASEURLCACHED = null;

	public static String getBaseURL(Context context) {
		if (BASEURLCACHED == null) {
			BASEURLCACHED = Utility.loadStringSetting(context,
					SETTINGS_BASEURL, BASEURLDEFAULT);
			if (!BASEURLCACHED.endsWith("?")) {
				if (!BASEURLCACHED.endsWith("/")) {
					BASEURLCACHED = BASEURLCACHED + "/";
				}
				BASEURLCACHED = BASEURLCACHED + "?";
			}
		}
		return BASEURLCACHED;
	}

	// After this time these confirmations will be discarded, maybe the person
	// has not enabled read confirmation or has left and does not use
	// Cryptocator any more.
	public static final int TIMEOUT_FOR_RECEIVEANDREAD_CONFIRMATIONS = 90 * 24
			* 60 * 60 * 1000; // ~ 3 month

	// FIXME: REMOVE BEFORE RELEASE
	// TIMEOUT FOR AES KEY ... SEND KEY SHOULD TIMEOUT BEFORE (so that we can
	// auto-send a new key)
	public static final int AES_KEY_TIMEOUT_SENDING = 60 * 60 * 1000; // 60
																		// Minutes
	public static final int AES_KEY_TIMEOUT_RECEIVING = 70 * 60 * 1000; // 60
																		// Minutes
	// public static final int AES_KEY_TIMEOUT_SENDING = 5 * 60 * 1000; // 5
	// Minutes
	// public static final int AES_KEY_TIMEOUT_RECEIVING = 6 * 60 * 1000; // 6
	// Minutes

	public static final int SECRETLEN = 20; // this is the predefined session
											// length used by the server!
	public static final int SALTLEN = 10; // this is the predefined session

	// Number of additionally added stuffing bytes to enhance encryption
	// for short and for same messages
	public static final int RANDOM_STUFF_BYTES = 5;

	// If not manually refreshed
	public static final int UPDATE_KEYS_MIN_INTERVAL = 60 * 1000; // 1 Minute
	public static final String SETTING_LASTUPDATEKEYS = "lastupdatekeys";
	public static final int UPDATE_PHONES_MIN_INTERVAL = 10 * 60 * 1000; // 10
																			// Minutes
	public static final String SETTING_LASTUPDATEPHONES = "lastupdatephones";
	public static final int UPDATE_NAMES_MIN_INTERVAL = 60 * 60 * 1000; // 60
																		// Minutes
	public static final String SETTING_LASTUPDATENAMES = "lastupdatenames";

	// background
	public static final int REGULAR_UPDATE_TIME = 20; // 60 sec
	// if user is present
	public static final int REGULAR_UPDATE_TIME_FAST = 5; // 60 sec if user
	// background
	public static final int REGULAR_POWERSAVE_UPDATE_TIME = 60; // 60 sec
	// if user is present
	public static final int REGULAR_POWERSAVE_UPDATE_TIME_FAST = 10; // 60 sec
																		// if
																		// user
	// present

	// public static final int REGULAR_UPDATE_TIME = 60; // 60 sec
	// public static final int REGULAR_UPDATE_TIME_FAST = 10; // 60 sec if user
	// // present

	// "recursion" interval for sending/receiveing multiple messages
	public static final int REGULAR_UPDATE_TIME_TRYNEXT = 5;

	// do not interrupt the user while he types, only if he stops typing
	// for at least these milliseconds, allow background activity (sending &&
	// receiving)
	public static final int TYPING_TIMEOUT_BEFORE_BACKGROUND_ACTIVITY = 5000; // 5
																				// seconds
	// do not interrupt if the user is typing fast, do not even SCROLL (ui
	// activity)
	// in this time but scroll if the user holds on for at least 1 seconds
	public static final int TYPING_TIMEOUT_BEFORE_UI_ACTIVITY = 1000; // 1
	// seconds

	public static final int ERROR_UPDATE_INTERVAL = 10; // 10 seconds
	public static final int ERROR_UPDATE_INCREMENT = 50; // + 50%
	public static final int ERROR_UPDATE_MAXIMUM = 5 * 60; // 5 Minutes maximal
															// interval

	public static final int MAX_SHOW_CONVERSATION_MESSAGES = 50; // for
																	// performance
	public static final int SHOW_ALL = -1;

	public static final String NA = "N/A";

	public static final String ERROR_TIME_TO_WAIT = "errortimetowait";

	public static final String OPTION_ACTIVE = "active";
	public static final boolean DEFAULT_ACTIVE = true;
	public static final String HELP_ACTIVE = "Enables the background service for receiving messages. Turning this off may save battery but you will not receive messages if the app is closed.";

	public static final String OPTION_TONE = "System Alert Tone";
	public static final boolean DEFAULT_TONE = true;
	public static final String HELP_TONE = "Play system alert tone when a new message is received (and the phone is not muted).";

	public static final String OPTION_VIBRATE = "vibrate";
	public static final boolean DEFAULT_VIBRATE = true;
	public static final String HELP_VIBTRATE = "Vibrate when a new message is received (and the phone is not muted).";

	public static final String OPTION_NOTIFICATION = "notification";
	public static final boolean DEFAULT_NOTIFICATION = true;
	public static final String HELP_NOTIFICATION = "Prompt a system notification when a new message is received.";

	public static final String OPTION_IGNORE = "ignore";
	public static final boolean DEFAULT_IGNORE = false;
	public static final String HELP_IGNORE = "Only message from users in your userlist will be received. Messages from other users will be silently discarded.";

	public static final String OPTION_ENCRYPTION = "encryption";
	public static final boolean DEFAULT_ENCRYPTION = false;
	public static final String HELP_ENCRYPTION = "Use encryption for sending messages. Will only work if your communication partner has also turned on encryption.\n\nIt is strongly advised that you always leave encryption on!";

	public static final String OPTION_NOREAD = "noread";
	public static final boolean DEFAULT_NOREAD = false;
	public static final String HELP_NOREAD = "Refuse read confirmations for received messages (second blue checkmark).\n\nWARNING: If you refuse read confirmation you cannot see read confirmations of anybody else!";

	public static final String OPTION_NOSCREENSHOTS = "noscreenshots";
	public static final boolean DEFAULT_NOSCREENSHOTS = true;
	public static final String HELP_NOSCREENSHOTS = "Disallows making automatic or manual screenshots of your messages for privacy protection.";

	public static final String OPTION_CHATMODE = "chatmode";
	public static final boolean DEFAULT_CHATMODE = false;
	public static final String HELP_CHATMODE = "Send a message by hitting <RETURN>. If chat mode is turned on, you cannot make explicit linebreaks.";

	public static final String OPTION_QUICKTYPE = "Quick Type";
	public static final boolean DEFAULT_QUICKTYPE = true;
	public static final String HELP_QUICKTYPE = "If you switch your phone orientation to landscape in order to type, the keyboard is shown automatically and you can just start typing without extra clicking into the message input text field.";

	public static final String OPTION_RECEIVEALLSMS = "receiveallsms";
	public static final boolean DEFAULT_RECEIVEALLSMS = false;
	public static final String HELP_RECEIVEALLSMSE = "You can use Delphino Cryptocator even as your default app for all SMS. Users that are not registered are listed by their names from your address book and you can only send them plain text SMS.";

	public static final String OPTION_POWERSAVE = "powersave";
	public static final boolean DEFAULT_POWERSAVE = true;
	public static final String HELP_POWERSAVE = "Delphino Cryptocator can operate in a power save mode were sending/receiving is reduced to every 10 seconds when active or 60 seconds when passive instead of 5 seconds and 20 seconds respectively in the non-power save mode.\n\nThis mode saves your battery.";

	public static final String OPTION_SMSMODE = "smsmode";
	public static final boolean DEFAULT_SMSMODE = false;

	public static final String PUBKEY = "pk";
	public static final String PRIVATEKEY = "k";
	public static final String AESKEY = "aes";
	public static final String PHONE = "hostphone";

	public static final String SETTINGS_USERLIST = "userlist";
	public static final String SETTINGS_USERLISTLASTMESSAGE = "userlistlastmessage";
	public static final String SETTINGS_USERLISTLASTMESSAGETIMESTAMP = "userlistlastmessagetimestamp";
	public static final String SETTINGS_HAVESENTRSAKEYYET = "sentrsakeyforuser";
	public static final String SETTINGS_PHONE = "phone";
	public static final String SETTINGS_UPDATENAME = "updatename";
	public static final boolean SETTINGS_DEFAULT_UPDATENAME = true;
	public static final String SETTINGS_UPDATEPHONE = "updatephone";
	public static final boolean SETTINGS_DEFAULT_UPDATEPHONE = true;
	public static final String SETTINGS_PHONEISMODIFIED = "phoneismodified";
	public static final String SETTINGS_DEFAULTMID = "defaultmid"; // + uid,
																	// this is
																	// the base
																	// mid, we
																	// do not
																	// request
																	// messages
																	// before
																	// this mid.
																	// per user!
	public static final int SETTINGS_DEFAULTMID_DEFAULT = -1; // should only be
																// -1 if there
																// is no msg in
																// the DB. Then
																// we retrieve
																// the highest
																// mid from
																// server!

	public static final String SETTINGS_SERVERKEY = "serverkey";
	public static final String SETTINGS_SESSIONSECRET = "tmpsession";
	public static final String SETTINGS_SESSIONID = "tmpsessionseed";
	public static final String SETTINGS_LOGINERRORCNT = "loginerrorcnt";
	public static final String SETTINGS_INVALIDATIONCOUNTER = "invalidationcounter"; // if
																						// uids
																						// are
																						// corrupted
																						// more
																						// than
																						// MAX
																						// (see
																						// next)
																						// then
																						// invalidate
																						// the
																						// session
	public static final int SETTINGS_INVALIDATIONCOUNTER_MAX = 2; // if uids are
																	// corrupted
																	// more than
																	// MAX (see
																	// next)
																	// then
																	// invalidate
																	// the
																	// session

	public static final String SETTINGS_LARGEST_MID_RECEIVED = "largestmidreceived"; // for
																						// receiving
																						// msgs
	public static final String SETTINGS_LARGEST_TS_RECEIVED = "largesttsreceived"; // receive
																					// confirmation
																					// of
																					// sent
	public static final String SETTINGS_LARGEST_TS_READ = "largesttsread"; // read
																			// confirmation
																			// of
																			// sent

	public static final String SETTINGS_HAVEASKED_NOENCRYPTION = "haveaskednoenryption";

	// try to send an SMS 5 times before claiming failed (ONLY counting unknown
	// errors, no network errors!)
	public static final int SMS_FAIL_CNT = 3;

	private CheckBox active;
	private CheckBox encryption;
	private CheckBox notification;
	private CheckBox tone;
	private CheckBox vibrate;
	private CheckBox ignore;
	private CheckBox noread;
	private CheckBox chatmode;
	private CheckBox quicktype;
	private CheckBox noscreenshots;
	private CheckBox powersave;
	private CheckBox receiveallsms;
	private ImageView helpactive;
	private ImageView helpencryption;
	private ImageView helptone;
	private ImageView helpvibrate;
	private ImageView helpnotification;
	private ImageView helpignore;
	private ImageView helpnoread;
	private ImageView helpquicktype;
	private ImageView helpchatmode;
	private ImageView helpnoscreenshots;
	private ImageView helppowersave;
	private ImageView helpreceiveallsms;

	private static EditText uid;
	private EditText email;
	private EditText pwd;

	private EditText usernew;
	private EditText emailnew;
	private EditText pwdnew;

	private EditText user;

	private EditText pwdchange;

	private static TextView error;
	private static TextView info;
	private static TextView deviceid;

	private static LinearLayout advancedsettings;
	// private static LinearLayout baseurl;
	private static EditText baseurltext;
	private static Button baseurlbutton;
	private static Button buttonclearsending;
	private static Button buttondebugprint;
	private static Button buttondeletedatabase;
	// private TextView textexisting;
	// private TextView textexisting2;

	private Button create;
	private Button login;
	private Button updatepwd;
	private Button updateuser;

	private static EditText phone;
	private static Button enablesmsoption;
	private static Button disablesmsoption;

	private static Button backup;
	private Button restore;

	private LinearLayout accountnew;
	private LinearLayout accountexisting;
	private static LinearLayout accountonline;
	private LinearLayout settingspart;
	private static ScrollView mainscrollview;

	private static boolean online = false;

	private CheckBox newaccount;

	private boolean accountType = false;

	public void setTitle(String title) {
		TextView titletext = (TextView) findViewById(R.id.titletext);
		titletext.setText(title);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Setup.possiblyDisableScreenshot(this);

		final Activity context = this;

		// Apply custom title bar (with holo :-)
		LinearLayout main = Utility.setContentViewWithCustomTitle(this,
				R.layout.activity_setup, R.layout.title_general);
		// main.setGravity(Gravity.BOTTOM);
		Utility.setBackground(this, main, R.drawable.dolphins1);

		LinearLayout titlegeneral = (LinearLayout) findViewById(R.id.titlegeneral);
		Utility.setBackground(this, titlegeneral, R.drawable.dolphins3blue);
		ImagePressButton btnback = (ImagePressButton) findViewById(R.id.btnback);
		btnback.initializePressImageResource(R.drawable.btnback);
		LinearLayout btnbackparent = (LinearLayout) findViewById(R.id.btnbackparent);
		btnback.setAdditionalPressWhiteView(btnbackparent);
		btnback.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				goBack(context);
			}
		});

		accountType = this.getIntent().hasExtra("account");

		uid = (EditText) findViewById(R.id.uid);
		uid.setTag(uid.getKeyListener()); // remember the listener for later use
		uid.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				updateAccountLocked(context, false);
			}
		});
		TextView textexisting2 = (TextView) findViewById(R.id.textexisting2);
		textexisting2.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				updateAccountLocked(context, false);
			}
		});

		email = (EditText) findViewById(R.id.email);
		email.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				updateAccountLocked(context, false);
			}
		});
		pwd = (EditText) findViewById(R.id.pwd);
		pwd.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				updateAccountLocked(context, false);
			}
		});

		usernew = (EditText) findViewById(R.id.usernew);
		emailnew = (EditText) findViewById(R.id.emailnew);
		pwdnew = (EditText) findViewById(R.id.pwdnew);
		phone = (EditText) findViewById(R.id.phone);

		error = (TextView) findViewById(R.id.error);
		info = (TextView) findViewById(R.id.info);

		advancedsettings = (LinearLayout) findViewById(R.id.advancedsettings);
		advancedsettings.setVisibility(View.GONE);
		baseurltext = (EditText) findViewById(R.id.baseurltext);
		baseurlbutton = (Button) findViewById(R.id.baseurlbutton);
		baseurlbutton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				String oldBaseUrl = Utility.loadStringSetting(context,
						Setup.SETTINGS_BASEURL, BASEURLDEFAULT);
				if (!baseurltext.getText().toString().equals(oldBaseUrl)) {
					promptChangeBaseURL(context);
				} else {
					advancedsettings.setVisibility(View.GONE);
				}

			}
		});
		buttonclearsending = (Button) findViewById(R.id.buttonclearsending);
		buttonclearsending.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				DB.rebuildDBSending(context);
				Utility.showToastInUIThread(context, "Sending Queue Cleared.");
			}
		});
		buttondebugprint = (Button) findViewById(R.id.buttondebugprint);
		buttondebugprint.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				String debug = DB.printDBSending(context);
				for (int uid : Main.loadUIDList(context)) {
					DB.printDB(context, uid);
				}
				Utility.showToastInUIThread(context, "Debug Output Printed");
				setErrorInfo(debug, false);
			}
		});
		buttondeletedatabase = (Button) findViewById(R.id.buttondeletedatabase);
		buttondeletedatabase.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				String title = "Delete Local Database";
				String text = "Enter UID to completely (!) delete the local database:";
				new MessageInputDialog(context, title, text, " Delete ", null,
						"Cancel", "",
						new MessageInputDialog.OnSelectionListener() {
							public void selected(MessageInputDialog dialog,
									int button, boolean cancel,
									String uidToDelete) {
								if (button == MessageInputDialog.BUTTONOK0) {
									// look if UID is a valid user
									int uid = Utility.parseInt(uidToDelete, -1);
									if (uid != -1) {
										DB.dropDB(context, uid);
										Utility.showToastAsync(context,
												"Database of UID " + uid
														+ " deleted.");
									} else {
										Utility.showToastAsync(context,
												"UID is invalid. Cannot proceed.");
									}
								}
								dialog.dismiss();
							};
						}).show();
			}
		});

		deviceid = (TextView) findViewById(R.id.deviceid);
		deviceid.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				advanedSetupCountdown--;
				if (advanedSetupCountdown <= 5 && advanedSetupCountdown > 1) {
					String toastText = "" + advanedSetupCountdown
							+ " more clicks to see the advanced options.";
					Utility.showToastInUIThread(context, toastText);
				}
				if (advanedSetupCountdown == 1) {
					String toastText = "You will see the ADVANCED options at the next click!";
					Utility.showToastInUIThread(context, toastText);
				}
				if (advanedSetupCountdown <= 0) {
					advanedSetupCountdown = ADVANCEDSETUPCOUNTDOWNSTART;
					baseurltext.setText(Utility.loadStringSetting(context,
							SETTINGS_BASEURL, BASEURLDEFAULT));
					// Make it visible
					baseurlbutton.setEnabled(false);
					baseurltext.setEnabled(false);
					baseurlbutton.postDelayed(new Runnable() {
						public void run() {
							baseurlbutton.setEnabled(true);
							baseurltext.setEnabled(true);
						}
					}, 4000);
					advancedsettings.setVisibility(View.VISIBLE);
				}
			}
		});

		updateTitleIDInfo(context);

		pwdchange = (EditText) findViewById(R.id.pwdchange);

		accountnew = (LinearLayout) findViewById(R.id.accountnew);
		accountexisting = (LinearLayout) findViewById(R.id.accountexisting);
		accountonline = (LinearLayout) findViewById(R.id.accountonline);
		settingspart = (LinearLayout) findViewById(R.id.settingssection);
		newaccount = (CheckBox) findViewById(R.id.newaccount);
		mainscrollview = (ScrollView) findViewById(R.id.mainscrollview);

		user = (EditText) findViewById(R.id.user);

		create = (Button) findViewById(R.id.create);
		login = (Button) findViewById(R.id.login);

		updatepwd = (Button) findViewById(R.id.updatepwd);
		updateuser = (Button) findViewById(R.id.updateuser);
		backup = (Button) findViewById(R.id.backup);
		restore = (Button) findViewById(R.id.restore);

		enablesmsoption = (Button) findViewById(R.id.enablesmsoption);
		disablesmsoption = (Button) findViewById(R.id.disablesmsoption);

		active = (CheckBox) findViewById(R.id.active);
		encryption = (CheckBox) findViewById(R.id.encryption);
		tone = (CheckBox) findViewById(R.id.tone);
		vibrate = (CheckBox) findViewById(R.id.vibrate);

		ignore = (CheckBox) findViewById(R.id.ignore);
		notification = (CheckBox) findViewById(R.id.notification);
		noread = (CheckBox) findViewById(R.id.noread);
		chatmode = (CheckBox) findViewById(R.id.chatmode);
		quicktype = (CheckBox) findViewById(R.id.quicktype);
		receiveallsms = (CheckBox) findViewById(R.id.receiveallsms);
		noscreenshots = (CheckBox) findViewById(R.id.noscreenshots);
		powersave = (CheckBox) findViewById(R.id.powersave);

		// requestWindowFeature(Window.FEATURE_LEFT_ICON);
		String uidString2 = Utility.loadStringSetting(context, "uid", "");
		if (uidString2 != null & uidString2.trim().length() > 0) {
			uidString2 = " - UID " + uidString2;
		}
		if (!accountType) {
			setTitle("Settings" + uidString2);
			newaccount.setVisibility(View.GONE);
			accountnew.setVisibility(View.GONE);
			accountexisting.setVisibility(View.GONE);
			accountonline.setVisibility(View.GONE);
			// getWindow(). setIcon(R.drawable.msgsetup);
			// getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON,R.drawable.msgsetup);
			// getActionBar().setIcon(R.drawable.msgsetup);
		} else {
			setTitle("Account" + uidString2);
			newaccount.setVisibility(View.VISIBLE);
			settingspart.setVisibility(View.GONE);
			// getActionBar().setIcon(R.drawable.account);
			// getWindow().setIcon(R.drawable.account);
			// getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON,R.drawable.account);
		}

		setErrorInfo(null);

		try {
			if (accountType) {
				newaccount
						.setOnCheckedChangeListener(new OnCheckedChangeListener() {
							public void onCheckedChanged(
									CompoundButton buttonView, boolean isChecked) {
								setErrorInfo(null);
								if (newaccount.isChecked()) {
									accountexisting.setVisibility(View.GONE);
									accountnew.setVisibility(View.VISIBLE);

									online = false;
									updateonline();
								} else {
									accountexisting.setVisibility(View.VISIBLE);
									accountnew.setVisibility(View.GONE);
								}

								updateonline();
								// Utility.saveBooleanSetting(context, "active",
								// active.isChecked());
							}
						});
			}

			// other settings
			active.setChecked(Utility.loadBooleanSetting(context,
					OPTION_ACTIVE, DEFAULT_ACTIVE));
			active.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					setActive(context, active.isChecked());
				}
			});
			tone.setChecked(Utility.loadBooleanSetting(context,
					Setup.OPTION_TONE, Setup.DEFAULT_TONE));
			tone.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					Utility.saveBooleanSetting(context, Setup.OPTION_TONE,
							tone.isChecked());
				}
			});
			vibrate.setChecked(Utility.loadBooleanSetting(context,
					Setup.OPTION_VIBRATE, true));
			vibrate.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					Utility.saveBooleanSetting(context, Setup.OPTION_VIBRATE,
							vibrate.isChecked());
				}
			});
			ignore.setChecked(Utility.loadBooleanSetting(context,
					Setup.OPTION_IGNORE, Setup.DEFAULT_IGNORE));
			ignore.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					Utility.saveBooleanSetting(context, Setup.OPTION_IGNORE,
							ignore.isChecked());
				}
			});
			notification.setChecked(Utility.loadBooleanSetting(context,
					Setup.OPTION_NOTIFICATION, Setup.DEFAULT_NOTIFICATION));
			notification.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					Utility.saveBooleanSetting(context,
							Setup.OPTION_NOTIFICATION, notification.isChecked());
				}
			});
			noread.setChecked(Utility.loadBooleanSetting(context,
					Setup.OPTION_NOREAD, Setup.DEFAULT_NOREAD));
			noread.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					if (!noread.isChecked()) {
						// we allowed it now, save the timestamp to just receive
						// read confirmations FROM NOW ON
						// this will hide ALL confirmations sent before this
						// time!
						Utility.saveStringSetting(context, "timestampread",
								DB.getTimestampString());
						Utility.saveBooleanSetting(context,
								Setup.OPTION_NOREAD, false);
					} else {
						noread.setChecked(false);
						askOnRefuseReadConfirmation(context);
					}
				}
			});
			chatmode.setChecked(Utility.loadBooleanSetting(context,
					Setup.OPTION_CHATMODE, Setup.DEFAULT_CHATMODE));
			chatmode.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					Utility.saveBooleanSetting(context, Setup.OPTION_CHATMODE,
							chatmode.isChecked());
				}
			});
			quicktype.setChecked(Utility.loadBooleanSetting(context,
					Setup.OPTION_QUICKTYPE, Setup.DEFAULT_QUICKTYPE));
			quicktype.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					Utility.saveBooleanSetting(context, Setup.OPTION_QUICKTYPE,
							quicktype.isChecked());
				}
			});
			noscreenshots.setChecked(Utility.loadBooleanSetting(context,
					Setup.OPTION_NOSCREENSHOTS, Setup.DEFAULT_NOSCREENSHOTS));
			noscreenshots.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					Utility.saveBooleanSetting(context,
							Setup.OPTION_NOSCREENSHOTS,
							noscreenshots.isChecked());
					if (!noscreenshots.isChecked()) {
						// allow screenshots, tell user to restart app!
						Conversation
								.promptInfo(
										context,
										"Allow Screenshots",
										"You may need to fully restart the App in order to make screenshots.\n\nIf this does not work, you may even need to restart your phone.\n\nConsider re-enabling this feature soon to protect your privacy!");
					}
				}
			});
			powersave.setChecked(Utility.loadBooleanSetting(context,
					Setup.OPTION_POWERSAVE, Setup.DEFAULT_POWERSAVE));
			powersave.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					Utility.saveBooleanSetting(context, Setup.OPTION_POWERSAVE,
							powersave.isChecked());
				}
			});
			encryption.setChecked(Utility.loadBooleanSetting(context,
					Setup.OPTION_ENCRYPTION, Setup.DEFAULT_ENCRYPTION));
			encryption.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					if (encryption.isChecked()) {
						enableEncryption(context);
						updateTitleIDInfo(context);
						Utility.saveBooleanSetting(context,
								Setup.OPTION_ENCRYPTION, true);
					} else {
						encryption.setChecked(true);
						askOnDisableEncryption(context);
					}
				}
			});
			receiveallsms.setChecked(isSMSDefaultApp(context, false));
			receiveallsms.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					if (receiveallsms.isChecked()) {
						// Enable Delphino Cryptocator as default messaging app
						setSMSDefaultApp(context, true);
					} else {
						receiveallsms.setChecked(true);
						promptDisableReceiveAllSms(context);
					}
				}
			});

			// / help
			helpactive = (ImageView) findViewById(R.id.helpactive);
			helpencryption = (ImageView) findViewById(R.id.helpencryption);
			helptone = (ImageView) findViewById(R.id.helptone);
			helpvibrate = (ImageView) findViewById(R.id.helpvibrate);
			helpnotification = (ImageView) findViewById(R.id.helpnotification);
			helpignore = (ImageView) findViewById(R.id.helpignore);
			helpnoread = (ImageView) findViewById(R.id.helpnoread);
			helpchatmode = (ImageView) findViewById(R.id.helpchatmode);
			helpquicktype = (ImageView) findViewById(R.id.helpquicktype);
			helpnoscreenshots = (ImageView) findViewById(R.id.helpnoscreenshots);
			helppowersave = (ImageView) findViewById(R.id.helppowersave);
			helpreceiveallsms = (ImageView) findViewById(R.id.helpreceiveallsms);
			helpactive.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					setErrorInfo(HELP_ACTIVE, false);
				}
			});
			helpencryption.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					setErrorInfo(HELP_ENCRYPTION, false);
				}
			});
			helptone.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					setErrorInfo(HELP_TONE, false);
				}
			});
			helpvibrate.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					setErrorInfo(HELP_VIBTRATE, false);
				}
			});
			helpnotification.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					setErrorInfo(HELP_NOTIFICATION, false);
				}
			});
			helpignore.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					setErrorInfo(HELP_IGNORE, false);
				}
			});
			helpnoread.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					setErrorInfo(HELP_NOREAD, false);
				}
			});
			helpchatmode.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					setErrorInfo(HELP_CHATMODE, false);
				}
			});
			helpquicktype.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					setErrorInfo(HELP_QUICKTYPE, false);
				}
			});
			helpnoscreenshots.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					setErrorInfo(HELP_NOSCREENSHOTS, false);
				}
			});
			helppowersave.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					setErrorInfo(HELP_POWERSAVE, false);
				}
			});
			helpreceiveallsms.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					setErrorInfo(HELP_RECEIVEALLSMSE, false);
				}
			});

			LinearLayout mainsettingsinnerlayout = (LinearLayout) findViewById(R.id.mainsettingsinnerlayout);
			mainsettingsinnerlayout.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					setErrorInfo(null);
				}
			});

			String uidString = Utility.loadStringSetting(context, "uid", "");
			boolean noAccountYet = uidString.equals("");
			uid.setText(uidString);
			newaccount.setChecked(true);
			newaccount.setChecked(false);
			newaccount.setChecked(noAccountYet);

			if (!noAccountYet) {
				this.accountLocked = 3;
			} else {
				this.accountLocked = 0;
			}

			updateAccountLocked(context, true);

			user.setText(Utility.loadStringSetting(context, "username", ""));
			email.setText(Utility.loadStringSetting(context, "email", ""));
			pwd.setText(Utility.loadStringSetting(context, "pwd", ""));
			pwd.setInputType(InputType.TYPE_CLASS_TEXT
					| InputType.TYPE_TEXT_VARIATION_PASSWORD);

			pwdnew.setInputType(InputType.TYPE_CLASS_TEXT
					| InputType.TYPE_TEXT_VARIATION_PASSWORD);
			pwdchange.setInputType(InputType.TYPE_CLASS_TEXT
					| InputType.TYPE_TEXT_VARIATION_PASSWORD);

			create.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					setErrorInfo(null);
					createNewAccount(context);
				}
			});

			login.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					setErrorInfo(null);
					validate(context);
				}
			});

			updateuser.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					setErrorInfo(null);
					updateUsername(context);
				}
			});

			updatepwd.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					setErrorInfo(null);
					updatePwd(context);
				}
			});

			backup.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					setErrorInfo(null);
					backup(context, false, true);
				}
			});
			restore.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					setErrorInfo(null);
					restore(context, true);
				}
			});

			updatePhoneNumberAndButtonStates(context);
			enablesmsoption.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					// enable or update?
					updateSMSOption(context, true);
					backup(context, true, false);
				}
			});
			disablesmsoption.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					// enable or update?
					updateSMSOption(context, false);
				}
			});

			LinearLayout settingsBackground = ((LinearLayout) findViewById(R.id.settingsbackground));
			Utility.setBackground(this, settingsBackground,
					R.drawable.dolphins1);
			settingsBackground.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					setErrorInfo(null);
				}
			});

		} catch (Exception e) {
			e.printStackTrace();
		}

		Utility.hideKeyboard(context);
	}

	// -------------------------------------------------------------------------

	private void updateAccountLocked(Context context, boolean silent) {
		if (accountLocked > 0) {
			login.setText("   Validate Login   ");
			newaccount.setEnabled(false);
			uid.setFocusable(false);
			email.setFocusable(false);
			pwd.setFocusable(false);
			uid.setClickable(true);
			email.setClickable(true);
			pwd.setClickable(true);
			// uid.setKeyListener((KeyListener) uid.getTag());
			// uid.setBackgroundColor(Color.parseColor("#22000000"));
			uid.setTextColor(Color.parseColor("#FF666666"));
			email.setTextColor(Color.parseColor("#FF666666"));
			pwd.setTextColor(Color.parseColor("#FF666666"));
			if (!silent) {
				Utility.showToastShortAsync(context, accountLocked
						+ " more clicks to edit the login information.");
				accountLocked = accountLocked - 1;
			}
		} else if (accountLocked == 0) {
			online = false;
			updateonline();

			login.setText("   Validate Login / Save   ");
			accountLocked = accountLocked - 1;
			Conversation
					.promptInfo(
							context,
							"Changing Login Information",
							"You are about to change the login information!\n\nIt is very curcial that you only change your login information if this is really required! All local data on you phone is linked to your account. If you switch to another account (user) you should clear all your contacts beforehand. Also note that your old account key becomes invalid and others might get into trouble sending you encrypted messages!\n\nDo not change your login information unless you really know what you are doing!");

			newaccount.setEnabled(true);
			uid.setTextColor(Color.parseColor("#FFDDDDDD"));
			email.setTextColor(Color.parseColor("#FFDDDDDD"));
			pwd.setTextColor(Color.parseColor("#FFDDDDDD"));
			uid.setFocusable(true);
			uid.setFocusableInTouchMode(true);
			uid.setEnabled(true);
			email.setFocusable(true);
			email.setFocusableInTouchMode(true);
			email.setEnabled(true);
			pwd.setFocusable(true);
			pwd.setFocusableInTouchMode(true);
			pwd.setEnabled(true);
			// uid.setBackgroundColor(Color.parseColor("#00000000"));
			// uid.setKeyListener(null); // make it non editable
		}
	}

	// -------------------------------------------------------------------------

	public static void updateTitleIDInfo(Context context) {
		deviceid.setText("DeviceID: " + getDeviceId(context)
				+ "   --   Account Key: " + Setup.getPublicKeyHash(context));
	}

	// -------------------------------------------------------------------------

	public void promptChangeBaseURL(final Context context) {
		final Activity activity = this;
		String titleMessage = "Changing Message Server";
		String textMessage = "ATTENTION: You are about to change the message server from http://www.cryptocator.org to something else. "
				+ "This is only desirable if you operate an own private message server at this location. Be warned that your userlist will "
				+ "completely be deleted! You should make a backup of it before (Account Settings). Additionally, you may need to create"
				+ " a new account at this different server - you cannot use the current account!\n\nUsing an own private server you will ONLY be "
				+ "able to communicate with other people registered on THIS particular server. This is typically ONLY relevant for companies or other"
				+ " closed user groups.\n\nDo you really want to change the message server and clear your userlist?";
		new MessageAlertDialog(context, titleMessage, textMessage,
				"Still Do it!", "       Cancel      ", null,
				new MessageAlertDialog.OnSelectionListener() {
					public void selected(int button, boolean cancel) {
						if (!cancel) {
							if (button == 0) {
								// Clear the userlist
								List<Integer> uidlist = Main
										.loadUIDList(context);
								for (int uid : uidlist) {
									if (uid >= 0) {
										Main.deleteUser(context, uid);
									}
								}
								Utility.saveStringSetting(context,
										Setup.SETTINGS_BASEURL, baseurltext
												.getText().toString().trim());
								Utility.saveStringSetting(context, "uid", null);
								Utility.saveStringSetting(context, "username",
										null);
								Utility.saveStringSetting(context, "email",
										null);
								Utility.saveStringSetting(context, "pwd", null);
								// now really try to withdraw
								Main.exitApplication(context);
							} else {
								advancedsettings.setVisibility(View.GONE);
							}

						}
					}
				}).show();
	}

	// ------------------------------------------------------------------------

	private static void updatePhoneNumberAndButtonStates(Context context) {
		if (phone == null || enablesmsoption == null
				|| disablesmsoption == null) {
			// not visible
			return;
		}
		if (!isSMSOptionEnabled(context)) {
			// Not saved phone number
			phone.setText(Utility.getPhoneNumber(context));
			// disabled
			enablesmsoption.setEnabled(true);
			enablesmsoption.setText(" Enable SMS ");
			disablesmsoption.setEnabled(false);
		} else {
			phone.setText(Utility
					.loadStringSetting(context, SETTINGS_PHONE, ""));
			// enabled
			enablesmsoption.setEnabled(true);
			enablesmsoption.setText(" Update ");
			disablesmsoption.setEnabled(true);
		}
	}

	// -------------------------------------------------------------------------

	public static boolean isSMSModeOn(Context context, int hostUid) {
		return (hostUid < 0 || Utility.loadBooleanSetting(context,
				Setup.OPTION_SMSMODE + hostUid, Setup.DEFAULT_SMSMODE));
	}

	// -------------------------------------------------------------------------

	void setErrorInfo(String errorMessage) {
		setErrorInfo(this, errorMessage, true);
	}

	void setErrorInfo(String message, boolean isError) {
		setErrorInfo(this, message, isError);
	}

	static void setErrorInfo(Context context, String errorMessage) {
		setErrorInfo(context, errorMessage, true);
	}

	static void setErrorInfo(Context context, String message, boolean isError) {
		if (error == null || info == null || uid == null
				|| mainscrollview == null) {
			// not visible only send this as toast
			if (message != null && message.length() > 0) {
				Utility.showToastAsync(context, message);
			}
			return;
		}
		if (message == null || message.length() == 0) {
			error.setVisibility(View.GONE);
			error.setText("");
			info.setVisibility(View.GONE);
			info.setText("");
		} else {
			if (isError) {
				error.setVisibility(View.VISIBLE);
				error.setText(message);
				error.requestFocus();
			} else {
				info.setVisibility(View.VISIBLE);
				info.setText(message);
				info.requestFocus();
			}
			uid.requestFocus();
			Utility.hideKeyboardExplicit(uid);
			mainscrollview.scrollTo(0, 0);
			// Utility.hideKeyboard(this);
			// final Activity context = this;
			// error.postDelayed(new Runnable() {
			// public void run() {
			// mainscrollview.scrollTo(0, 0);
			// Utility.hideKeyboard(context);
			// }
			// }, 200);
		}
	}

	static void updateonline() {
		if (accountonline == null) {
			// not visible, nothing to do
			return;
		}
		if (!online) {
			accountonline.setVisibility(View.GONE);
		} else {
			accountonline.setVisibility(View.VISIBLE);
		}
	}

	// ------------------------------------------------------------------------

	void validate(final Context context) {
		Setup.updateServerkey(context);

		final String uidBefore = Utility.loadStringSetting(context, "uid", "");

		login.setEnabled(false);
		setErrorInfo(null);
		String uidString = "";
		final int tmpUid = Utility
				.parseInt(uid.getText().toString().trim(), -1);
		if (tmpUid > 0) {
			uidString = tmpUid + "";
		} else {
			uid.setText("");
		}
		String emailString = email.getText().toString().trim();
		String pwdString = pwd.getText().toString().trim();

		if ((emailString.length() == 0 && uidString.length() == 0)
				|| pwdString.length() == 0) {
			setErrorInfo("You must provide a password in combination of a UID or email!");
			login.setEnabled(true);
			return;
		}

		// DETECT UID=CHANGE ===> EXIT
		// APPLICATION!
		if (!uidBefore.equals(uidString)) {
			Setup.disableEncryption(context);
			updateCurrentMid(context);
		}

		Utility.saveStringSetting(context, "uid", uidString);
		Utility.saveStringSetting(context, "email", emailString);
		Utility.saveStringSetting(context, "pwd", pwdString);

		LoginData loginData = calculateLoginVal(context, uidString,
				emailString, pwdString);
		if (loginData == null) {
			setErrorInfo("Encryption error, try again after restarting the App!");
			login.setEnabled(true);
			return;
		}

		// RSA encode
		PublicKey serverKey = getServerkey(context);
		String uidStringEnc = Communicator.encryptServerMessage(context,
				uidString, serverKey);
		if (uidStringEnc == null) {
			setErrorInfo("Encryption error. Try again after restarting the App.");
			login.setEnabled(true);
			return;
		}
		uidStringEnc = Utility.encode(uidStringEnc);
		String emailStringEnd = Communicator.encryptServerMessage(context,
				emailString, serverKey);
		if (emailStringEnd == null) {
			setErrorInfo("Encryption error. Try again after restarting the App.");
			login.setEnabled(true);
			return;
		}
		emailStringEnd = Utility.encode(emailStringEnd);

		String reseturl = null;
		String reactivateurl = null;
		String url = Setup.getBaseURL(context) + "cmd=validate&val1="
				+ loginData.user + "&val2=" + loginData.password + "&val3="
				+ loginData.val;
		if (!uidString.equals("")) {
			reseturl = Setup.getBaseURL(context) + "cmd=resetpwd&uid="
					+ uidStringEnc;
			reactivateurl = Setup.getBaseURL(context)
					+ "cmd=resendactivation&uid=" + uidStringEnc;
		} else {
			reseturl = Setup.getBaseURL(context) + "cmd=resetpwd&email="
					+ emailStringEnd;
			reactivateurl = Setup.getBaseURL(context)
					+ "cmd=resendactivation&email=" + emailStringEnd;
		}
		Log.d("communicator", "LOGIN VALIDATE: " + url);

		final String url2 = url;
		final String reseturl2 = reseturl;
		final String reactivateurl2 = reactivateurl;
		HttpStringRequest httpStringRequest = (new HttpStringRequest(context,
				url2, new HttpStringRequest.OnResponseListener() {
					public void response(String response) {
						Log.d("communicator", "###### VALIDATE: response="
								+ response);
						if (Communicator.isResponseValid(response)) {
							Log.d("communicator",
									"###### VALIDATE: response positive="
											+ response);
							String emailR = "";
							String uidR = "";
							String usernameR = "";
							final String response2 = response;
							final boolean positiveResponse = Communicator
									.isResponsePositive(response);
							if (positiveResponse) {
								String responseContent = Communicator
										.getResponseContent(response);
								Log.d("communicator",
										"###### VALIDATE: responseContent="
												+ responseContent);
								// the return value should be the new created
								// UID
								String[] array = responseContent.split("#");
								if (array != null && array.length == 5) {
									String sessionID = array[0];
									String loginErrCnt = array[1];
									updateSuccessfullLogin(context, sessionID,
											loginErrCnt);
									uidR = decUid(context, array[2]) + "";
									emailR = decText(context, array[3]);
									usernameR = decText(context, array[4]);
									// no invalid but also no unknown users
									if ((!uidR.equals("-1"))
											&& (!uidR.equals("-2"))
											&& emailR != null
											&& usernameR != null) {
										Utility.saveStringSetting(context,
												"uid", uidR);
										Utility.saveStringSetting(context,
												"email", emailR);
										Utility.saveStringSetting(context,
												"username", usernameR);
										// DETECT UID=CHANGE ===> EXIT
										// APPLICATION!
										if (!uidBefore.equals(uidR)) {
											Utility.showToastAsync(
													context,
													"Account/UID changed. Cryptocator must be re-started in order to operate properly...");
											Main.exitApplication(context);
										}
									} else {
										uidR = tmpUid + "";
										emailR = email.getText().toString()
												.trim();
										usernameR = Utility.loadStringSetting(
												context, "username", "");
									}
								}
							}
							final String email2 = emailR;
							final String uid2 = uidR;
							final String username2 = usernameR;
							final Handler mUIHandler = new Handler(Looper
									.getMainLooper());
							mUIHandler.post(new Thread() {
								@Override
								public void run() {
									super.run();
									login.setEnabled(true);
									// UPDATE DISPLAY HERE
									if (positiveResponse) {
										// EVERYTHING OK
										email.setText(email2);
										uid.setText(uid2);
										user.setText(username2);
										Utility.saveStringSetting(context,
												"username", username2);
										if (tmpUid > 0) {
											Main.saveUID2Name(context, tmpUid,
													username2);
										}
										pwdchange.setText("");
										online = true;
										updateonline();
										setErrorInfo(
												"Login successfull.\n\nYou can now edit your username, enable sms support, change your password, or backup or restore your user list below.",
												false);
										// // if encryption is on, generate a
										// new
										// // key and send it to the server!
										// if
										// (Utility.loadBooleanSetting(context,
										// Setup.OPTION_ENCRYPTION,
										// Setup.DEFAULT_ENCRYPTION)) {
										// if (!Utility
										// .loadBooleanSetting(
										// context,
										// Setup.SETTINGS_HAVESENTRSAKEYYET
										// + uid2,
										// false)) {
										// enableEncryption(context);
										// }
										// }
									} else {
										if (response2.equals("-4")) {
											// email already registered
											setErrorInfo("You account has not been activated yet. Go to your email inbox and follow the activation link! Be sure to also look in the spam email folder.\n\nIf you cannot find the activation email, click the following link to resend it to you:\n"
													+ reactivateurl2);
										} else if (response2.equals("-11")) {
											// email already registered
											setErrorInfo("You new password has not been activated yet. Go to your email inbox and follow the activation link! Be sure to also look in the spam email folder.\n\nIf you cannot find the activation email, you can reset your password once more here:\n"
													+ reseturl2);
										} else {
											// / Clear server key to enforce a
											// soon reload!
											Utility.saveStringSetting(context,
													Setup.SETTINGS_SERVERKEY,
													null);

											Utility.saveStringSetting(context,
													"pwd", "");
											setErrorInfo("Login failed. \n\nIf you don't know your password, click the following link to reset it:\n"
													+ reseturl2);
										}
									}
								}
							});
						} else {
							// / Clear server key to enforce a soon reload!
							Utility.saveStringSetting(context,
									Setup.SETTINGS_SERVERKEY, null);

							final Handler mUIHandler = new Handler(Looper
									.getMainLooper());
							mUIHandler.post(new Thread() {
								@Override
								public void run() {
									super.run();
									login.setEnabled(true);
									setErrorInfo(
											"Server error or no internet connection. Try again later.",
											true);
								}
							});
						}
					}
				}));

	}

	// ------------------------------------------------------------------------

	void updateUsername(final Context context) {
		updateuser.setEnabled(false);
		setErrorInfo(null);
		final String usernameString = user.getText().toString();

		// RSA encode
		PublicKey serverKey = getServerkey(context);
		String usernameStringEnc = Communicator.encryptServerMessage(context,
				usernameString, serverKey);
		if (usernameStringEnc == null) {
			setErrorInfo("Encryption error. Try again after restarting the App.");
			updateuser.setEnabled(true);
			return;
		}
		usernameStringEnc = Utility.encode(usernameStringEnc);

		String session = Setup.getTmpLogin(context);
		if (session == null) {
			setErrorInfo("Session error. Try again after restarting the App.");
			updateuser.setEnabled(true);
			// error resume is automatically done by getTmpLogin, not logged in
			return;
		}

		String url = null;
		url = Setup.getBaseURL(context) + "cmd=updateuser&session=" + session
				+ "&user=" + usernameStringEnc;
		Log.d("communicator", "UPDATE USERNAME: " + url);
		final String url2 = url;
		HttpStringRequest httpStringRequest = (new HttpStringRequest(context,
				url2, new HttpStringRequest.OnResponseListener() {
					public void response(String response) {
						if (Communicator.isResponseValid(response)) {
							final String response2 = response;
							if (Communicator.isResponsePositive(response2)) {
								Utility.saveStringSetting(context, "username",
										usernameString);
							}
							final Handler mUIHandler = new Handler(
									Looper.getMainLooper());
							mUIHandler.post(new Thread() {
								@Override
								public void run() {
									super.run();
									updateuser.setEnabled(true);
									// UPDATE DISPLAY HERE
									if (Communicator
											.isResponsePositive(response2)) {
										// EVERYTHING OK
										setErrorInfo("Username changed.", false);
									} else {
										if (response2.equals("-5")) {
											// email already registered
											setErrorInfo("Update username failed!");
										} else if (response2.equals("-13")) {
											setErrorInfo("Username too or long short. It should consist of at least 2 (valid) up to 16 characters!");
										} else if (response2.equals("-16")) {
											setErrorInfo("Username contains invalid characters!");
										} else {
											setErrorInfo("Login failed.");
											online = false;
											updateonline();
										}
									}
								}
							});
						} else {
							final Handler mUIHandler = new Handler(Looper
									.getMainLooper());
							mUIHandler.post(new Thread() {
								@Override
								public void run() {
									super.run();
									updateuser.setEnabled(true);
									setErrorInfo(
											"Server error or no internet connection. Try again later.",
											true);
								}
							});
						}
					}
				}));

	}

	// ------------------------------------------------------------------------

	void updatePwd(final Context context) {
		updatepwd.setEnabled(false);
		setErrorInfo(null);
		final String usernameString = user.getText().toString();
		final String pwdChangeString = pwdchange.getText().toString();

		// RSA encode
		PublicKey serverKey = getServerkey(context);
		String pwdChangeStringEnc = Communicator.encryptServerMessage(context,
				pwdChangeString, serverKey);
		if (pwdChangeStringEnc == null) {
			setErrorInfo("Encryption error. Try again after restarting the App.");
			updatepwd.setEnabled(true);
			return;
		}
		pwdChangeStringEnc = Utility.encode(pwdChangeStringEnc);

		String session = Setup.getTmpLogin(context);
		if (session == null) {
			setErrorInfo("Session error. Try again after restarting the App.");
			updatepwd.setEnabled(true);
			// error resume is automatically done by getTmpLogin, not logged in
			return;
		}

		String url = null;
		url = Setup.getBaseURL(context) + "cmd=updatepwd&session=" + session
				+ "&val=" + pwdChangeStringEnc;
		Log.d("communicator", "UPDATE PWD: " + url);

		final String url2 = url;
		HttpStringRequest httpStringRequest = (new HttpStringRequest(context,
				url2, new HttpStringRequest.OnResponseListener() {
					public void response(String response) {
						if (Communicator.isResponseValid(response)) {
							final String response2 = response;
							if (Communicator.isResponsePositive(response2)) {
								Utility.saveStringSetting(context, "username",
										usernameString);
							}
							final Handler mUIHandler = new Handler(
									Looper.getMainLooper());
							mUIHandler.post(new Thread() {
								@Override
								public void run() {
									super.run();
									updatepwd.setEnabled(true);
									// UPDATE DISPLAY HERE
									if (Communicator
											.isResponsePositive(response2)) {
										// EVERYTHING OK
										// save the new password
										Utility.saveStringSetting(context,
												"pwd", pwdChangeString);
										pwd.setText(pwdChangeString);
										pwdchange.setText("");
										setErrorInfo(
												"Password changed. Check your email to activate the new password!",
												false);
									} else {
										if (response2.equals("-6")) {
											// email already registered
											setErrorInfo("Update password failed!");
										} else if (response2.equals("-14")) {
											pwdchange.setText("");
											setErrorInfo("Password too or long short. It should consist of at least 6 (valid) up to 16 characters!");
										} else if (response2.equals("-15")) {
											pwdchange.setText("");
											setErrorInfo("Password contains invalid characters!");
										} else {
											pwdchange.setText("");
											setErrorInfo("Login failed.");
											online = false;
											updateonline();
										}
									}
								}
							});
						} else {
							final Handler mUIHandler = new Handler(Looper
									.getMainLooper());
							mUIHandler.post(new Thread() {
								@Override
								public void run() {
									super.run();
									updatepwd.setEnabled(true);
									setErrorInfo(
											"Server error or no internet connection. Try again later.",
											true);
								}
							});
						}
					}
				}));

	}

	// ------------------------------------------------------------------------

	void createNewAccount(final Context context) {
		Setup.updateServerkey(context);

		create.setEnabled(false);
		setErrorInfo(null);
		// String uidString = uid.getText().toString();
		String emailString = emailnew.getText().toString().trim();
		String usernameString = usernew.getText().toString().trim();
		String pwdString = pwdnew.getText().toString();


		// RSA encode
		PublicKey serverKey = getServerkey(context);

		emailString = Communicator.encryptServerMessage(context, emailString,
				serverKey);
		if (emailString == null) {
			setErrorInfo("Encryption error. Habe you specified a valid email address? Try again after restarting the App.");
			create.setEnabled(true);
			return;
		}
		emailString = Utility.encode(emailString);

		pwdString = Communicator.encryptServerMessage(context, pwdString,
				serverKey);
		if (pwdString == null) {
			setErrorInfo("Encryption error. Habe you specified a valid password? Try again after restarting the App.");
			create.setEnabled(true);
			return;
		}
		pwdString = Utility.encode(pwdString);

		usernameString = Communicator.encryptServerMessage(context,
				usernameString, serverKey);
		if (usernameString == null) {
			setErrorInfo("Encryption error. Habe you specified a valid username? Try again after restarting the App.");
			create.setEnabled(true);
			return;
		}
		usernameString = Utility.encode(usernameString);

		String url = null;
		String reseturl = null;
		url = Setup.getBaseURL(context) + "cmd=create&email=" + emailString
				+ "&pwd=" + pwdString + "&user=" + usernameString;
		reseturl = Setup.getBaseURL(context) + "cmd=resetpwd&email="
				+ emailString;
		Log.d("communicator", "CREATE ACCOUNt: " + url);

		final String url2 = url;
		final String reseturl2 = reseturl;
		HttpStringRequest httpStringRequest = (new HttpStringRequest(context,
				url2, new HttpStringRequest.OnResponseListener() {
					public void response(String response) {
						final String response2 = response;
						if (Communicator.isResponseValid(response)) {
							final String newUID = Communicator
									.getResponseContent(response);
							if (Communicator.isResponsePositive(response)) {
								// the return value should be the new created
								// UID
								Utility.saveStringSetting(context, "uid",
										newUID);
							}
							final Handler mUIHandler = new Handler(Looper
									.getMainLooper());
							mUIHandler.post(new Thread() {
								@Override
								public void run() {
									super.run();
									create.setEnabled(true);
									// UPDATE DISPLAY HERE
									if (Communicator
											.isResponsePositive(response2)) {
										// EVERYTHING OK
										uid.setText(newUID);
										email.setText(emailnew.getText()
												.toString());
										pwd.setText(pwdnew.getText().toString());
										
										// Utility.saveStringSetting(context, "uid", uidString);
										String emailString = emailnew.getText().toString().trim();
										String usernameString = usernew.getText().toString().trim();
										String pwdString = pwdnew.getText().toString();
										Utility.saveStringSetting(context, "username", usernameString);
										Utility.saveStringSetting(context, "email", emailString);
										Utility.saveStringSetting(context, "pwd", pwdString);
										
										Utility.saveBooleanSetting(context, Setup.OPTION_ENCRYPTION, false);
										saveHaveAskedForEncryption(context, false);
										
										newaccount.setChecked(false);
										updateCurrentMid(context);
										setErrorInfo(
												"Registration successfull!\n\nYOUR NEW UNIQUE UID IS: "
														+ newUID
														+ "\n\nGo to your email account and follow the activation link we sent you. Be sure to also check your spam email folder.",
												false);
										Utility.showToastAsync(
												context,
												"Your new UID is "+newUID+". Cryptocator must be re-started in order to operate properly...");
										Main.exitApplication(context);
										
									} else {
										if (response2.equals("-2")) {
											setErrorInfo("Email address already taken.\n\nHave you activated your account already? If not go to your email account and follow the activation link we sent you.\n\nIf this is your address and you cannot find the activation email then reset the password, otherwise use a different address.\n\nTo reset the password click here:\n"
													+ reseturl2);
										} else if (response2.equals("-12")) {
											setErrorInfo("Email address is not valid. You cannot use this address for registration.");
										} else if (response2.equals("-13")) {
											setErrorInfo("Username too or long short. It should consist of at least 2 (valid) up to 16 characters!");
										} else if (response2.equals("-14")) {
											setErrorInfo("Password too or long short. It should consist of at least 6 (valid) up to 16 characters!");
										} else if (response2.equals("-15")) {
											setErrorInfo("Password contains invalid characters!");
										}
									}
								}
							});
						} else {
							final Handler mUIHandler = new Handler(Looper
									.getMainLooper());
							mUIHandler.post(new Thread() {
								@Override
								public void run() {
									super.run();
									create.setEnabled(true);
									setErrorInfo(
											"Server error or no internet connection. Try again later.",
											true);
								}
							});
						}
					}
				}));

	}

	// ------------------------------------------------------------------------

	public static void updateCurrentMid(final Context context) {
		String url = null;
		url = Setup.getBaseURL(context) + "cmd=mid";
		final String url2 = url;
		HttpStringRequest httpStringRequest = (new HttpStringRequest(context,
				url2, new HttpStringRequest.OnResponseListener() {
					public void response(String response) {
						if (Communicator.isResponseValid(response)) {
							String content = Communicator
									.getResponseContent(response);
							if (content != null) {
								DB.resetLargestMid(context,
										Utility.parseInt(content, -1));
								Log.d("communicator",
										"XXXX SAVED LARGEST MID '" + content
												+ "'");
							}
						} else {
							// Log.d("communicator",
							// "XXXX FAILED TO UPDATE SERVER KEY '"
							// + response + "'");
						}
					}
				}));
	}

	// ------------------------------------------------------------------------
	// ------------------------------------------------------------------------

	public static void backup(final Context context, final boolean silent,
			boolean manual) {
		if (backup != null) {
			backup.setEnabled(false);
		}
		if (!silent) {
			setErrorInfo(context, null);
		}
		String userlistString = Utility.loadStringSetting(context,
				SETTINGS_USERLIST, "").replace(",", "#");

		if (userlistString.length() == 0) {
			if (!silent) {
				setErrorInfo(context, "No users in your list yet!");
			}
			if (backup != null) {
				backup.setEnabled(true);
			}
			return;
		}

		// RSA encode
		PublicKey serverKey = getServerkey(context);
		String userlistStringEnc = Communicator.encryptServerMessage(context,
				userlistString, serverKey);
		if (userlistStringEnc == null) {
			setErrorInfo(context,
					"Encryption error. Try again after restarting the App.");
			backup.setEnabled(true);
			return;
		}
		userlistStringEnc = Utility.encode(userlistStringEnc);

		String session = Setup.getTmpLogin(context);
		if (session == null) {
			setErrorInfo(context,
					"Session error. Try again after restarting the App.");
			backup.setEnabled(true);
			// error resume is automatically done by getTmpLogin, not logged in
			return;
		}

		String url = null;
		url = Setup.getBaseURL(context) + "cmd=backup&session=" + session
				+ "&val=" + userlistStringEnc;
		if (manual) {
			url = Setup.getBaseURL(context) + "cmd=backupmanual&session="
					+ session + "&val=" + userlistStringEnc;
		}

		Log.d("communicator", "BACKUP: " + url);

		final String url2 = url;
		HttpStringRequest httpStringRequest = (new HttpStringRequest(context,
				url2, new HttpStringRequest.OnResponseListener() {
					public void response(String response) {
						if (Communicator.isResponseValid(response)) {
							final String response2 = response;
							final Handler mUIHandler = new Handler(
									Looper.getMainLooper());
							mUIHandler.post(new Thread() {
								@Override
								public void run() {
									super.run();
									if (backup != null) {
										backup.setEnabled(true);
									}
									// UPDATE DISPLAY HERE
									if (Communicator
											.isResponsePositive(response2)) {
										// EVERYTHING OK
										if (!silent) {
											setErrorInfo(
													context,
													"Backup of user list to server successful. You can now later restore it, e.g., on a different device or in case of data loss. Note that no messages are backed up.",
													false);
										}
									} else {
										if (!silent) {
											setErrorInfo(context,
													"Backup of user list failed. Try again later.");
										}
										online = false;
										updateonline();
									}
								}
							});
						} else {
							final Handler mUIHandler = new Handler(Looper
									.getMainLooper());
							mUIHandler.post(new Thread() {
								@Override
								public void run() {
									super.run();
									if (backup != null) {
										backup.setEnabled(true);
									}
									if (!silent) {
										setErrorInfo(context,
												"Backup of user list failed. Try again later.");
										online = false;
										updateonline();
									}
								}
							});
						}
					}
				}));

	}

	// ------------------------------------------------------------------------

	void restore(final Context context, boolean manual) {
		restore.setEnabled(false);
		setErrorInfo(null);

		String session = Setup.getTmpLogin(context);
		if (session == null) {
			setErrorInfo(context,
					"Session error. Try again after restarting the App.");
			restore.setEnabled(true);
			// error resume is automatically done by getTmpLogin, not logged in
			return;
		}

		String url = null;
		url = Setup.getBaseURL(context) + "cmd=restore&session=" + session;
		if (manual) {
			url = Setup.getBaseURL(context) + "cmd=restoremanual&session="
					+ session;
		}
		final String url2 = url;
		HttpStringRequest httpStringRequest = (new HttpStringRequest(context,
				url2, new HttpStringRequest.OnResponseListener() {
					public void response(String response) {
						if (Communicator.isResponseValid(response)) {
							final String response2 = response;
							final Handler mUIHandler = new Handler(
									Looper.getMainLooper());
							mUIHandler.post(new Thread() {
								@Override
								public void run() {
									super.run();
									restore.setEnabled(true);
									// UPDATE DISPLAY HERE
									if (Communicator
											.isResponsePositive(response2)) {
										String responseContent = Communicator
												.getResponseContent(response2);
										if (responseContent == null) {
											responseContent = "";
										}
										responseContent = decText(context,
												responseContent);
										if (responseContent == null) {
											responseContent = "";
										}
										responseContent = responseContent
												.replace("#", ",");
										// EVERYTHING OK
										List<Integer> updateList = Main.appendUIDList(
												context, responseContent,
												Main.loadUIDList(context));
										Main.saveUIDList(context, updateList);
										if (Setup.isSMSOptionEnabled(context)) {
											Setup.backup(context, true, false);
										}
										setErrorInfo(
												"Restore of user list from server successful. Users from backup where added to your existing local userlist.",
												false);
									} else {
										setErrorInfo("Restore of user list failed. Try again later.");
										online = false;
										updateonline();
									}
								}
							});
						} else {
							final Handler mUIHandler = new Handler(Looper
									.getMainLooper());
							mUIHandler.post(new Thread() {
								@Override
								public void run() {
									super.run();
									setErrorInfo("Restore of user list failed. Try again later.");
									online = false;
									updateonline();
									restore.setEnabled(true);
								}
							});
						}
					}
				}));

	}

	// -------------------------------------------------------------------------
	// -------------------------------------------------------------------------

	public static void updateSMSOption(final Context context,
			final boolean enable) {
		if (enablesmsoption != null) {
			enablesmsoption.setEnabled(false);
		}
		setErrorInfo(context, null);
		String phoneString = "";
		if (phone != null) {
			phoneString = phone.getText().toString(); // Utility.loadStringSetting(context,
			// Setup.SETTINGS_PHONE,
			// "");
		} else {
			// the case if this is called not from the dialog but from
			// Conversation
			phoneString = Utility.getPhoneNumber(context);
		}
		if (phoneString == null) {
			phoneString = "";
		}
		// only check valid number for enabling/update
		phoneString = normalizePhone(phoneString);
		final String phoneString2 = phoneString;
		if (!Utility.isValidPhoneNumber(phoneString) && enable) {
			setErrorInfo(
					context,
					"No valid phone number provided! Phone number must be in the following format:\n+491711234567");
			if (enablesmsoption != null) {
				enablesmsoption.setEnabled(true);
			}
			return;
		}
		if (!enable) {
			phoneString = "delete";
		}

		// RSA encode
		PublicKey serverKey = getServerkey(context);
		String phoneStringEnc = Communicator.encryptServerMessage(context,
				phoneString, serverKey);
		if (phoneStringEnc == null) {
			setErrorInfo(context,
					"Encryption error. Try again after restarting the App.");
			enablesmsoption.setEnabled(true);
			return;
		}
		phoneStringEnc = Utility.encode(phoneStringEnc);

		String session = Setup.getTmpLogin(context);
		if (session == null) {
			setErrorInfo(context,
					"Session error. Try again after restarting the App.");
			enablesmsoption.setEnabled(true);
			// error resume is automatically done by getTmpLogin, not logged in
			return;
		}

		String url = null;
		url = Setup.getBaseURL(context) + "cmd=updatephone&session=" + session
				+ "&val=" + phoneStringEnc;
		final String url2 = url;
		HttpStringRequest httpStringRequest = (new HttpStringRequest(context,
				url2, new HttpStringRequest.OnResponseListener() {
					public void response(String response) {
						if (Communicator.isResponseValid(response)) {
							final String response2 = response;
							final Handler mUIHandler = new Handler(
									Looper.getMainLooper());
							mUIHandler.post(new Thread() {
								@Override
								public void run() {
									super.run();
									if (enablesmsoption != null) {
										enablesmsoption.setEnabled(true);
									}
									// UPDATE DISPLAY HERE
									if (Communicator
											.isResponsePositive(response2)) {
										// EVERYTHING OK
										if (enable) {
											Utility.saveStringSetting(context,
													SETTINGS_PHONE,
													phoneString2);
										} else {
											Utility.saveStringSetting(context,
													SETTINGS_PHONE, "");
										}
										updatePhoneNumberAndButtonStates(context);
										if (enable) {
											setErrorInfo(
													context,
													"SMS option is enabled. Your phone number was updated on the server. Users from your userlist can now send you secure SMS!\n\nAttention: You are advised to check [x] Ignore Unknown Users in the settings to ensure your phone number can just be retrieved by persons you know.",
													false);
										} else {
											setErrorInfo(
													context,
													"SMS option is now disabled. Your phone number was removed from the server.",
													false);
										}

									} else {
										setErrorInfo(context,
												"Failed to update your phone number on server. Try again later.");
										online = false;
										updateonline();
									}
								}
							});
						} else {
							final Handler mUIHandler = new Handler(Looper
									.getMainLooper());
							mUIHandler.post(new Thread() {
								@Override
								public void run() {
									super.run();
									if (enablesmsoption != null) {
										enablesmsoption.setEnabled(true);
									}
									online = false;
									updateonline();
								}
							});
						}
					}
				}));
	}

	// -------------------------------------------------------------------------
	// -------------------------------------------------------------------------

	public static void setActive(Context context, boolean active) {
		Utility.saveBooleanSetting(context, Setup.OPTION_ACTIVE, active);
		if (active) {
			Scheduler.reschedule(context, false, true, false);
		}
	}

	// -------------------------------------------------------------------------

	public static boolean isActive(Context context) {
		return Utility.loadBooleanSetting(context, Setup.OPTION_ACTIVE,
				Setup.DEFAULT_ACTIVE);
	}

	// -------------------------------------------------------------------------

	public static int getErrorUpdateInterval(Context context) {
		return Utility.loadIntSetting(context, ERROR_TIME_TO_WAIT,
				Setup.ERROR_UPDATE_INTERVAL);
	}

	// -------------------------------------------------------------------------

	public static void setErrorUpdateInterval(Context context, boolean error) {
		if (!error) {
			// in the non-error case, reset the counter
			Utility.saveIntSetting(context, ERROR_TIME_TO_WAIT,
					Setup.ERROR_UPDATE_INTERVAL);
		} else {
			// flag a new error and possibly increment the counter
			int currentCounter = getErrorUpdateInterval(context);
			if (Main.isVisible() || Conversation.isVisible()) {
				// if the conversation is visible then do not increment!!! This
				// is just for not wasting energy on standby or if the user
				// is not in this app!
				Utility.saveIntSetting(context, ERROR_TIME_TO_WAIT,
						Setup.ERROR_UPDATE_INTERVAL);
			} else if (currentCounter < ERROR_UPDATE_MAXIMUM) {
				// add 20%
				currentCounter += ((currentCounter * ERROR_UPDATE_INCREMENT) / 100);
				Utility.saveIntSetting(context, ERROR_TIME_TO_WAIT,
						currentCounter);
			}
		}
	}

	// -------------------------------------------------------------------------

	// Returns true if the user is supposed to be actively using the program
	public static boolean isUserActive() {
		return (Conversation.isVisible() || Main.isVisible());
	}

	// -------------------------------------------------------------------------

	public static void enableEncryption(Context context) {
		Utility.saveBooleanSetting(context, Setup.OPTION_ENCRYPTION, true);

		// Generate key pair for 1024-bit RSA encryption and decryption
		Key publicKey = null;
		Key privateKey = null;
		try {
			KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
			kpg.initialize(1024); // 2048
			KeyPair kp = kpg.genKeyPair();
			publicKey = kp.getPublic();
			String encodedpublicKey = Base64.encodeToString(
					publicKey.getEncoded(), Base64.DEFAULT);
			encodedpublicKey = encodedpublicKey.replace("\n", "").replace("\r",
					"");
			privateKey = kp.getPrivate();
			String encodedprivateKey = Base64.encodeToString(
					privateKey.getEncoded(), Base64.DEFAULT);

			Utility.saveStringSetting(context, Setup.PUBKEY, encodedpublicKey);
			Utility.saveStringSetting(context, Setup.PRIVATEKEY,
					encodedprivateKey);

			Log.d("communicator", "###### encodedpublicKey=" + encodedpublicKey);

			// send public key to server
			String keyHash = getPublicKeyHash(context);
			Communicator.sendKeyToServer(context, encodedpublicKey, keyHash);

		} catch (Exception e) {
			Log.e("communicator", "RSA key pair error");
		}

	}

	// -------------------------------------------------------------------------

	public static void disableEncryption(Context context) {
		Utility.saveBooleanSetting(context, Setup.OPTION_ENCRYPTION, false);
		saveHaveAskedForEncryption(context, false);
		String keyHash = getPublicKeyHash(context);
		Utility.saveStringSetting(context, Setup.PUBKEY, null);
		Utility.saveStringSetting(context, Setup.PRIVATEKEY, null);
		Communicator.clearKeyFromServer(context, keyHash);
	}

	// -------------------------------------------------------------------------
	// -------------------------------------------------------------------------
	// THE AES PART

	public static boolean isAESKeyOutdated(Context context, int uid,
			boolean forSending) {
		long nowTime = DB.getTimestamp();
		long otherTime = getAESKeyDate(context, uid);
		if (otherTime < 1) {
			return true;
		}
		// Log.d("communicator", "###### AES : nowTime = " + nowTime);
		// Log.d("communicator",
		// "###### AES : nowTime - (Setup.AES_KEY_TIMEOUT*1000) = " + (nowTime -
		// (Setup.AES_KEY_TIMEOUT*1000)));
		// Log.d("communicator", "###### AES : otherTime = "+ otherTime);
		if (forSending) {
			if (nowTime - (Setup.AES_KEY_TIMEOUT_SENDING) > otherTime) {
				// Log.d("communicator", "###### AES : OUTDATED!!! :((( ");
				return true;
			}
		} else {
			if (nowTime - (Setup.AES_KEY_TIMEOUT_RECEIVING) > otherTime) {
				// Log.d("communicator", "###### AES : OUTDATED!!! :((( ");
				return true;
			}
		}
		// Log.d("communicator", "###### AES : NOT OUTDATED");
		return false;
	}

	public static long getAESKeyDate(Context context, int uid) {
		String keycreated = Utility.loadStringSetting(context, Setup.AESKEY
				+ "created" + uid, "");
		if ((keycreated == null || keycreated.length() == 0)) {
			return 0;
		}
		long returnKey = Utility.parseLong(keycreated, 0);
		return returnKey;
	}

	public static void setAESKeyDate(Context context, int uid, String keycreated) {
		Utility.saveStringSetting(context, Setup.AESKEY + "created" + uid,
				keycreated);
	}

	// -------------------------------------------------------------------------

	public static void saveAESKey(Context context, int uid, String key) {
		Utility.saveStringSetting(context, Setup.AESKEY + uid, key);
	}

	// -------------------------------------------------------------------------

	public static boolean haveAESKey(Context context, int uid) {
		String key = Utility.loadStringSetting(context, Setup.AESKEY + uid, "");
		boolean returnValue = false;
		if ((key != null && key.length() != 0)) {
			returnValue = true;
		}
		return returnValue;
	}

	// -------------------------------------------------------------------------

	public static String serializeAESKey(Key key) {
		return Base64.encodeToString(key.getEncoded(), Base64.DEFAULT);
	}

	// -------------------------------------------------------------------------

	public static Key generateAESKey(String randomSeed) {
		// Set up secret key spec for 128-bit AES encryption and decryption
		SecretKeySpec sks = null;
		try {
			SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
			sr.setSeed(randomSeed.getBytes());
			KeyGenerator kg = KeyGenerator.getInstance("AES");
			kg.init(128, sr);
			sks = new SecretKeySpec((kg.generateKey()).getEncoded(), "AES");
		} catch (Exception e) {
			Log.e("communicator", "AES secret key spec error");
		}
		return sks;
	}

	// -------------------------------------------------------------------------

	public static String getAESKeyAsString(Context context, int uid) {
		String encodedKey = Utility.loadStringSetting(context, Setup.AESKEY
				+ uid, "");
		return encodedKey;
	}

	public static String getAESKeyHash(Context context, int uid) {
		String key = getAESKeyAsString(context, uid);
		if (key == null || key.length() < 1) {
			return NA;
		}
		String hash = NA;
		try {
			hash = Utility.md5(key).substring(0, 4).toUpperCase();
		} catch (Exception e) {
		}
		return hash;
	}

	public static Key getAESKey(Context context, int uid) {
		String encodedKey = getAESKeyAsString(context, uid);
		try {
			if ((encodedKey != null && encodedKey.length() != 0)) {
				byte[] decodedKey = Base64.decode(encodedKey, Base64.DEFAULT);
				// rebuild key using SecretKeySpec
				Key originalKey = new SecretKeySpec(decodedKey, 0,
						decodedKey.length, "AES");
				// PublicKey originalKey = KeyFactory.getInstance("RSA")
				// .generatePublic(new X509EncodedKeySpec(decodedKey));
				return originalKey;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	// -------------------------------------------------------------------------
	// -------------------------------------------------------------------------
	// THE RSA PART

	public static String getKeyDate(Context context, int uid) {
		String keycreated = Utility.loadStringSetting(context, Setup.PUBKEY
				+ "created" + uid, "");
		if ((keycreated == null || keycreated.length() == 0)) {
			return null;
		}
		return keycreated;
	}

	public static void setKeyDate(Context context, int uid, String keycreated) {
		Utility.saveStringSetting(context, Setup.PUBKEY + "created" + uid,
				keycreated);
	}

	// -------------------------------------------------------------------------

	public static void saveKey(Context context, int uid, String key) {
		Utility.saveStringSetting(context, Setup.PUBKEY + uid, key);
	}

	// -------------------------------------------------------------------------

	public static boolean haveKey(Context context, int uid) {
		String key = Utility.loadStringSetting(context, Setup.PUBKEY + uid, "");
		boolean returnValue = false;
		if ((key != null && key.length() != 0)) {
			returnValue = true;
		}
		return returnValue;
	}

	// -------------------------------------------------------------------------

	public static String getKeyAsString(Context context, int uid) {
		String encodedKey = Utility.loadStringSetting(context, Setup.PUBKEY
				+ uid, "");
		return encodedKey;
	}

	public static String getKeyHash(Context context, int uid) {
		String key = getKeyAsString(context, uid);
		if (key == null || key.length() < 1) {
			return NA;
		}
		String hash = NA;
		try {
			hash = ((Utility.md5(key.trim().toUpperCase())).substring(0, 4))
					.toUpperCase();
		} catch (Exception e) {
		}
		return hash;
	}

	public static PublicKey getKey(Context context, String encodedKeyAsString) {
		if ((encodedKeyAsString != null && encodedKeyAsString.length() != 0)) {
			Log.d("communicator", "XXXX LOAD SOME RSA KEY1 '"
					+ (new String(encodedKeyAsString)) + "'");

			byte[] decodedKey = Base64.decode(encodedKeyAsString,
					Base64.DEFAULT);

			Log.d("communicator", "XXXX LOAD SOME RSA KEY2 '"
					+ (new String(decodedKey)) + "'");

			// rebuild key using SecretKeySpec
			// Key originalKey = new SecretKeySpec(decodedKey, 0,
			// decodedKey.length, "AES");
			try {
				PublicKey originalKey = KeyFactory.getInstance("RSA")
						.generatePublic(new X509EncodedKeySpec(decodedKey));
				return originalKey;
			} catch (InvalidKeySpecException e) {
				e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	public static PublicKey getKey(Context context, int uid) {
		String encodedKeyAsString = getKeyAsString(context, uid);
		return (getKey(context, encodedKeyAsString));
	}

	// -------------------------------------------------------------------------

	public static String getPublicKeyAsString(Context context) {
		String encodedKey = Utility
				.loadStringSetting(context, Setup.PUBKEY, "");
		return encodedKey;
	}

	// private static String keyHash(String keyAsString) {
	// return keyAsString.length() + "";
	// return keyAsString;
	// return Utility.md5(keyAsString);
	// if (keyAsString == null) {
	// return null;
	// }
	// int width = 10;
	// int maxlen = keyAsString.length() - 1;
	// int i = maxlen;
	// StringBuilder sb = new StringBuilder();
	// Log.d("communicator",
	// "XXXX keyHash("+keyAsString+") " + maxlen);
	// while (i >= 0) {
	// Log.d("communicator",
	// "XXXX keyHash("+i+") " + sb.toString());
	// sb.append(keyAsString.substring(i,i+1));
	// i -= width;
	// }
	// return sb.toString();
	// }

	public static String getPublicKeyHash(Context context) {
		String key = getPublicKeyAsString(context);
		if (key == null || key.length() < 1) {
			return NA;
		}
		String hash = NA;
		try {
			hash = ((Utility.md5(key.trim().toUpperCase())).substring(0, 4))
					.toUpperCase();
			// hash = key; // Utility.md5(key).substring(0, 4).toUpperCase();
		} catch (Exception e) {
		}
		return hash;
	}

	public static PublicKey getPublicKey(Context context) {
		String encodedKey = getPublicKeyAsString(context);
		if ((encodedKey != null && encodedKey.length() != 0)) {
			byte[] decodedKey = Base64.decode(encodedKey, Base64.DEFAULT);
			// rebuild key using SecretKeySpec
			// SecretKey originalKey = new SecretKeySpec(decodedKey, 0,
			// decodedKey.length, "RSA");
			try {
				PublicKey originalKey = KeyFactory.getInstance("RSA")
						.generatePublic(new X509EncodedKeySpec(decodedKey));
				return originalKey;
			} catch (InvalidKeySpecException e) {
				e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	// -------------------------------------------------------------------------

	public static PrivateKey getPrivateKey(Context context) {
		String encodedKey = Utility.loadStringSetting(context,
				Setup.PRIVATEKEY, "");
		if ((encodedKey != null && encodedKey.length() != 0)) {
			byte[] decodedKey = Base64.decode(encodedKey, Base64.DEFAULT);
			// rebuild key using SecretKeySpec
			// SecretKey originalKey = new SecretKeySpec(decodedKey, 0,
			// decodedKey.length, "AES");
			try {
				// PrivateKey originalKey = KeyFactory.getInstance("RSA")
				// .generatePrivate(new X509EncodedKeySpec(decodedKey));
				PrivateKey originalKey = KeyFactory.getInstance("RSA")
						.generatePrivate(new PKCS8EncodedKeySpec(decodedKey));
				return originalKey;
			} catch (InvalidKeySpecException e) {
				e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	// -------------------------------------------------------------------------

	public static boolean encryptedSentPossible(Context context, int uid) {
		boolean encryption = Utility.loadBooleanSetting(context,
				Setup.OPTION_ENCRYPTION, Setup.DEFAULT_ENCRYPTION);
		boolean haveKey = Setup.haveKey(context, uid);

		if (!encryption || !haveKey) {
			return false;
		}
		return true;
	}

	// -------------------------------------------------------------------------

	public static boolean isEncryptionEnabled(Context context) {
		boolean encryption = Utility.loadBooleanSetting(context,
				Setup.OPTION_ENCRYPTION, Setup.DEFAULT_ENCRYPTION);
		return encryption;
	}

	public static boolean isEncryptionAvailable(Context context, int hostUid) {
		boolean encryption = isEncryptionEnabled(context);
		boolean haveKey = Setup.haveKey(context, hostUid);
		return (encryption && haveKey);
	}

	// -------------------------------------------------------------------------
	// -------------------------------------------------------------------------

	public static void savePhoneIsModified(Context context, int uid,
			boolean isModified) {
		Utility.saveBooleanSetting(context, Setup.SETTINGS_PHONEISMODIFIED
				+ uid, isModified);
	}

	public static boolean isPhoneModified(Context context, int uid) {
		// for NOT registered users this is always true!
		if (uid < 0) {
			return true;
		}
		return Utility.loadBooleanSetting(context,
				Setup.SETTINGS_PHONEISMODIFIED + uid, false);
	}

	public static void savePhone(Context context, int uid, String phone,
			boolean manual) {
		if (!manual) {
			// this phone number comes from the server...
			savePhoneIsModified(context, uid, false);
		} else {
			// this phone number was entered manually in the user details dialog
			savePhoneIsModified(context, uid, true);
		}
		Utility.saveStringSetting(context, Setup.PHONE + uid, phone);
	}

	public static String getPhone(Context context, int uid) {
		return Utility.loadStringSetting(context, Setup.PHONE + uid, null);
	}

	public static boolean havePhone(Context context, int uid) {
		String phone = getPhone(context, uid);
		return (phone != null && phone.length() > 0);
	}

	public static int getUIDByPhone(Context context, String phone,
			boolean allInternalAndExternalUsers) {
		// Prefer registered users if we have the sme phone number for a
		// registered users
		int smsuserAsFallback = -1;
		int registeredDefault = -1;
		for (int uid : Main.loadUIDList(context)) {
			String currentPhone = getPhone(context, uid);
			// Log.d("communicator",
			// "XXXX getUIDByPhone currentPhone["+uid+"] '"+currentPhone+"' =?= '"+phone+"' (searched) ");
			if (currentPhone != null && currentPhone.equals(phone)) {
				if (uid != -1 || allInternalAndExternalUsers) {
					if (uid >= 0) {
						registeredDefault = uid;
					} else {
						smsuserAsFallback = uid;
					}
				}
			}
		}
		if ((registeredDefault != -1) && (smsuserAsFallback != -1)) {
			askMergeUser(context, smsuserAsFallback, registeredDefault);
		}
		if (registeredDefault != -1) {
			return registeredDefault;
		}
		return smsuserAsFallback;
	}

	public static boolean isSMSOptionEnabled(Context context) {
		String personalphone = Utility.loadStringSetting(context,
				SETTINGS_PHONE, "");
		return (personalphone.trim().length() != 0);
	}

	private static void askMergeUser(final Context context,
			final int smsuserAsFallback, final int registeredDefault) {
		try {
			final String titleMessage = "User Merge";
			final String textMessage = "SMS contact "
					+ Main.UID2Name(context, smsuserAsFallback, true)
					+ " has the same phone number as registered user "
					+ Main.UID2Name(context, registeredDefault, true)
					+ ". It is recommended that you merge the SMS to the registered user.\n\nDo you want to merge them now?";
			new MessageAlertDialog(context, titleMessage, textMessage, " Yes ",
					" Cancel ", null,
					new MessageAlertDialog.OnSelectionListener() {
						public void selected(int button, boolean cancel) {
							if (!cancel) {
								if (button == 0) {
									if (DB.mergeUser(context,
											smsuserAsFallback,
											registeredDefault)) {
										// remove merged user
										if (Main.isAlive()) {
											Main.getInstance().deleteUser(
													context, smsuserAsFallback);
										}
									}
								}
							}
						}
					}).show();
		} catch (Exception e) {
			// ignore
		}
	}

	// -------------------------------------------------------------------------
	// -------------------------------------------------------------------------

	private void askOnDisableEncryption(final Context context) {
		try {
			final String titleMessage = "Disable Encryption";
			final String textMessage = "Encrypted messages are a main feature of this messaging service. Unencrypted plaintext messages can be possibly read by anyone observing your internet connection.\n\n"
					+ "Do you really want to disable encryption and only send and receive plaintext messages? ";
			new MessageAlertDialog(context, titleMessage, textMessage,
					" Disable Encryption ", " Cancel ", null,
					new MessageAlertDialog.OnSelectionListener() {
						public void selected(int button, boolean cancel) {
							encryption.setChecked(true);
							if (!cancel) {
								if (button == 0) {
									// disable encryption
									disableEncryption(context);
									encryption.setChecked(false);
									updateTitleIDInfo(context);
								}
							}
						}
					}).show();
		} catch (Exception e) {
			// ignore
		}
	}

	// ------------------------------------------------------------------------

	private void askOnRefuseReadConfirmation(final Context context) {
		try {
			final String titleMessage = "Refuse Read Confirmation";
			final String textMessage = "Read confirmations make sense in most cases because devices nowadays are always-on an often receive messages without our attendance. It makes sense to use read confirmations. This is a mutual value meaning that if you benefit from other people sending you read confirmations you should also send read confirmations out.\n\nIn order to enforce this mutual value, you can see read confirmations ONLY iff you have enabled them too!\n\nYou are now about to refuse sending and receiving any read confirmations. Do you really want to proceed?";
			new MessageAlertDialog(context, titleMessage, textMessage,
					" Disable Read Confirmations ", " Cancel ", null,
					new MessageAlertDialog.OnSelectionListener() {
						public void selected(int button, boolean cancel) {
							noread.setChecked(false);
							if (!cancel) {
								if (button == 0) {
									Utility.saveBooleanSetting(context,
											Setup.OPTION_NOREAD, true);
									noread.setChecked(true);
								}
							}
						}
					}).show();
		} catch (Exception e) {
			// ignore
		}
	}

	// ------------------------------------------------------------------------

	public static boolean noAccountYet(Context context) {
		String uidString = Utility.loadStringSetting(context, "uid", "");
		return (uidString == null || uidString.length() == 0);
	}

	// ------------------------------------------------------------------------

	public static String normalizePhone(String phone) {
		return phone.replace("(", "").replace(")", "").replace(" ", "")
				.replace("-", "").replace("/", "");
	}

	// ------------------------------------------------------------------------

	/**
	 * Calculate a hopefully unique UID from a phone numer
	 * 
	 * @param phoneOrUid
	 *            the phone
	 * @return the fake uid from phone
	 */
	private static int FAKEUIDLEN = 4;

	public static int getFakeUIDFromPhone(String phone) {
		Log.d("communicator", "XXX FAKEUID input phone=" + phone);
		phone = Setup.normalizePhone(phone);
		phone = phone.replace("+49", "");
		phone = phone.replace("+1", "");
		phone = phone.replace("+", "");
		phone = phone.replaceAll("[^0-9]", "");
		Log.d("communicator", "XXX FAKEUID normalized phone=" + phone);
		int parts = phone.length() / FAKEUIDLEN;
		if (phone.length() % FAKEUIDLEN != 0) {
			parts++;
		}
		int returnUID = 0;
		int phoneLen = phone.length();
		for (int part = 0; part < parts; part++) {
			int start = part * FAKEUIDLEN;
			int end = start + FAKEUIDLEN;
			if (end >= phoneLen) {
				end = phoneLen - 1;
			}
			Log.d("communicator", "XXX FAKEUID start=" + start + ", end=" + end);
			String phonePart = phone.substring(start, end);
			int phonePartInt = Utility.parseInt(phonePart, 0);
			Log.d("communicator", "XXX FAKEUID part[" + part + "] returnUID="
					+ returnUID + " + phonePartInt=" + phonePartInt);
			returnUID = returnUID + phonePartInt;
		}
		Log.d("communicator", "XXX FAKEUID " + phone + " --> "
				+ (-1 * returnUID));

		if (returnUID == 0) {
			int i = 0;
			boolean toggle = false;
			String tmp = Utility.md5(phone);
			for (byte c : tmp.getBytes()) {
				if (toggle) {
					i += c;
				} else {
					i += 10 * c;
				}
				toggle = !toggle;
			}
			String tmp2 = i + "";
			if (tmp2.length() > FAKEUIDLEN) {
				tmp2 = tmp2.substring(0, FAKEUIDLEN);
			}
			returnUID = Utility.parseInt(tmp2, 0);
		}
		Log.d("communicator", "XXX FAKEUID RETURNED " + phone + " --> "
				+ (-1 * returnUID));
		return (-1 * returnUID);
	}

	// ------------------------------------------------------------------------

	private void promptDisableReceiveAllSms(final Context context) {
		if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			try {
				final String titleMessage = "Default SMS App";
				final String textMessage = "If you disable Delphino Cryptocator to receive all SMS, you should define a different SMS default app. Otherwise you wont't receive unsecure plain text SMS anymore!\n\nDo you want to change the default SMS app?";
				new MessageAlertDialog(context, titleMessage, textMessage,
						" Yes ", " Cancel ", null,
						new MessageAlertDialog.OnSelectionListener() {
							public void selected(int button, boolean cancel) {
								noread.setChecked(false);
								if (!cancel) {
									if (button == 0) {
										setSMSDefaultApp(context, false);
										receiveallsms.setChecked(false);
										startActivityForResult(
												new Intent(
														android.provider.Settings.ACTION_WIRELESS_SETTINGS),
												0);
									}
								}
							}
						}).show();
			} catch (Exception e) {
				// ignore
			}
		} else {
			// Just turn this feature off
			setSMSDefaultApp(context, false);
			receiveallsms.setChecked(false);
		}
	}

	// ------------------------------------------------------------------------

	@TargetApi(Build.VERSION_CODES.KITKAT)
	public static boolean isSMSDefaultApp(Context context,
			boolean strictOnlyLocalSettings) {
		boolean receiveAllSMS = Utility.loadBooleanSetting(context,
				Setup.OPTION_RECEIVEALLSMS, Setup.DEFAULT_RECEIVEALLSMS);
		boolean androidDefaultSMSApp = receiveAllSMS;
		if (strictOnlyLocalSettings) {
			return receiveAllSMS;
		}
		if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			String androidSettings = Telephony.Sms
					.getDefaultSmsPackage(context);
			androidDefaultSMSApp = context.getPackageName().equals(
					androidSettings);
			return androidDefaultSMSApp;
			// Log.d("communicator", "XXXX DEFAULT ASK FOR SMS APP = "
			// + androidDefaultSMSApp + " =?= " + androidSettings);
		} else {
			return receiveAllSMS;
			// Log.d("communicator", "XXXX DEFAULT NOT ASK FOR SMS APP");
		}
		// return androidDefaultSMSApp || receiveAllSMS;
	}

	@TargetApi(Build.VERSION_CODES.KITKAT)
	public void setSMSDefaultApp(Context context, boolean enable) {
		if (enable) {
			if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
				String packageName = context.getPackageName();
				// Log.d("communicator", "XXXX DEFAULT SMS APP REQUEST FOR "
				// + packageName);
				Intent intent = new Intent(
						Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
				intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME,
						packageName);
				context.startActivity(intent);
			}
		} else {
			// Intent intent = new
			// Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
			// intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME,
			// context.getPackageName());
		}
		Utility.saveBooleanSetting(context, Setup.OPTION_RECEIVEALLSMS, enable);
	}

	// -------------------------------------------------------------------------
	// -------------------------------------------------------------------------

	public static String getSessionSecret(final Context context) {
		return Utility.loadStringSetting(context, Setup.SETTINGS_SESSIONSECRET,
				"");
	}

	public static String getSessionID(final Context context) {
		return Utility.loadStringSetting(context, Setup.SETTINGS_SESSIONID, "");
	}

	// if a tmplogin failed, do a new login!
	public static void invalidateTmpLogin(Context context) {
		Utility.saveStringSetting(context, Setup.SETTINGS_SESSIONSECRET, "");
		Utility.saveStringSetting(context, Setup.SETTINGS_SESSIONID, "");
	}

	/**
	 * Ensure logged in.
	 * 
	 * @param context
	 *            the context
	 * @return true, if successful
	 */
	public static boolean ensureLoggedIn(Context context) {
		// Check if we are logged in... if not re-login
		String secret = Setup.getSessionSecret(context);
		String sessionid = Setup.getSessionID(context);
		if (secret == null || secret.length() == 0 || sessionid == null
				|| sessionid.length() == 0) {
			long lastLoginRequest = Utility.loadLongSetting(context,
					"lastloginrequest", 0);
			long now = DB.getTimestamp();
			final int MINIMUM_WAIT_TO_LOGIN = 10 * 1000; // do not relogin twice
															// within 10
															// seconds!
			if (now - lastLoginRequest < MINIMUM_WAIT_TO_LOGIN) {
				// NO RELOGIN FOR NOW, WAIT AT LEAST MINIMUM_WAIT_TO_LOGIN
				return false;
			}
			// Remember that we relogin now
			Utility.saveLongSetting(context, "lastloginrequest", now);
			Setup.possiblyInvalidateSession(context, true); // reset
															// invalidation
															// counter
			// Auto-Re-Login
			Setup.login(context);
			// Automatic error resume
			Scheduler.reschedule(context, true, false, false);
			// Do nothing
			return false;
		}
		// we are logged in
		return true;
	}

	// returns the session value that is used
	// expect session=
	// md5(sessionid#timestampinseconds#secret#salt)#sessionid#salt
	public static String getTmpLogin(Context context) {
		String secret = getSessionSecret(context);
		String sessionid = getSessionID(context);
		if (!Setup.ensureLoggedIn(context)) {
			return null;
		}
		String timeStampInSeconds = "" + ((DB.getTimestamp() / 1000) / 100);
		String salt = Utility.getRandomString(Setup.SALTLEN);
		String session = Utility.md5(sessionid + "#" + timeStampInSeconds + "#"
				+ secret + "#" + salt)
				+ "#" + sessionid + "#" + salt;
		return Utility.encode(session);
	}

	/**
	 * Calculate login val based on a uid or email in combination with a
	 * password. Either uid or email may be null. If both are set then uid is
	 * taken.
	 * 
	 * @param context
	 *            the context
	 * @param uid
	 *            the uid
	 * @param email
	 *            the email
	 * @param pwd
	 *            the pwd
	 * @return the string
	 */
	public static LoginData calculateLoginVal(Context context, String uid,
			String email, String pwd) {
		String sessionSecret = Utility.getRandomString(Setup.SECRETLEN);
		Utility.saveStringSetting(context, Setup.SETTINGS_SESSIONSECRET,
				sessionSecret);
		String timeStampInSeconds = "" + (DB.getTimestamp() / 1000);
		String deviceID = getDeviceId(context);

		// RSA encode
		PublicKey serverKey = getServerkey(context);

		String loginUser = email;
		if (uid != null && uid.trim().length() > 0) {
			loginUser = uid;
		}
		if (serverKey == null) {
			Setup.updateServerkey(context);
			return null; // no server key, seems to be an internet error?
		}
		if (loginUser == null) {
			return null; // email or uid must be given
		}
		Log.d("communicator", "XXXX LOGGING IN: loginUser ='" + loginUser
				+ "' email=" + email + ", uid=" + uid + ", serverKey="
				+ serverKey);

		String eLoginUser = Communicator.encryptServerMessage(context,
				loginUser, serverKey);
		if (eLoginUser == null) {
			return null; // encryption error
		}
		String eLoginUserHash = Utility.md5(eLoginUser).substring(0, 5);

		String loginPassword = pwd;
		String eLoginPassword = Communicator.encryptServerMessage(context,
				loginPassword, serverKey);
		if (eLoginPassword == null) {
			return null; // encryption error
		}
		String eLoginPasswordHash = Utility.md5(eLoginPassword).substring(0, 5);

		// expect val= shortuserhash#password#session#timestamp (timestamp in
		// seconds!!!!)
		String valString = eLoginUserHash + "#" + eLoginPasswordHash + "#"
				+ sessionSecret + "#" + timeStampInSeconds + "#" + deviceID;

		Log.d("communicator", "XXXX LOGGING IN: DECODED val ='" + valString
				+ "'");

		String encryptedBase64ValString = Communicator.encryptServerMessage(
				context, valString, serverKey);

		if (encryptedBase64ValString == null) {
			// encoding error, we cannot login!!!
			return null;
		}

		Log.d("communicator", "XXXX LOGGING IN: ENCODED BASE64 val ="
				+ encryptedBase64ValString);

		LoginData loginData = new LoginData();
		loginData.user = Utility.encode(eLoginUser);
		loginData.password = Utility.encode(eLoginPassword);
		loginData.val = Utility.encode(encryptedBase64ValString);

		return loginData;
	}

	public static void updateSuccessfullLogin(Context context,
			String sessionID, String loginErrCnt) {
		Utility.saveStringSetting(context, Setup.SETTINGS_SESSIONID, sessionID);
		int loginerrorcnt = Utility.parseInt(loginErrCnt, 0);
		// save the login error counter
		Utility.saveIntSetting(context, Setup.SETTINGS_LOGINERRORCNT,
				loginerrorcnt);
		Log.d("communicator", "XXXX LOGGED IN: SESSIONID = '" + sessionID
				+ "', errcnt=" + loginerrorcnt);
	}

	// update if not present, will be deleted on login failure
	public static void login(final Context context) {
		Communicator.accountNotActivated = false;
		Setup.updateServerkey(context);
		if (getSessionSecret(context).equals("")
				|| getSessionID(context).equals("") || true) {
			// no sessionsecret or no session seed

			String uidString = Utility.loadStringSetting(context, "uid", "");
			String pwdString = Utility.loadStringSetting(context, "pwd", "");

			LoginData loginData = calculateLoginVal(context, uidString, null,
					pwdString);
			if (loginData == null) {
				return;
			}

			String url = null;
			// www.delphino.net/cryptocator/index.php?cmd=login2&uid=5&val=passw%23session%23timestamp%23HTC1
			url = Setup.getBaseURL(context) + "cmd=login2&val1="
					+ loginData.user + "&val2=" + loginData.password + "&val3="
					+ loginData.val;
			Log.d("communicator", "XXXX LOGGING IN: URL = '" + url);
			final String url2 = url;
			HttpStringRequest httpStringRequest = (new HttpStringRequest(
					context, url2, new HttpStringRequest.OnResponseListener() {
						public void response(String response) {
							if (response.equals("-4")) {
								// not activated
								Communicator.accountNotActivated = true;
								if (Main.isAlive()) {
									Main.getInstance()
											.updateInfoMessageBlockAsync(
													context);
								}
							}
							if (Communicator.isResponseValid(response)) {
								String responseContent = Communicator
										.getResponseContent(response);
								Log.d("communicator",
										"XXXX LOGGING IN: RESPONS = '"
												+ response);
								if (responseContent != null) {
									String[] values = responseContent
											.split("#");
									if (values != null && values.length == 2) {
										String sessionID = values[0];
										String loginErrCnt = values[1];
										updateSuccessfullLogin(context,
												sessionID, loginErrCnt);
									}
								}
							} else {
								Log.d("communicator", "XXXX LOGIN FAILED'");
								// / Clear server key to enforce a soon reload!
								Utility.saveStringSetting(context,
										Setup.SETTINGS_SERVERKEY, null);
							}
						}
					}));
		}
	}

	public static String getServerkeyAsString(final Context context) {
		return Utility.loadStringSetting(context, Setup.SETTINGS_SERVERKEY, "");
	}

	public static PublicKey getServerkey(final Context context) {

		try {
			String modAndExpString = new String(getServerkeyAsString(context));
			String[] values = modAndExpString.split("#");
			if (values.length == 2) {
				String exp = values[0];
				String mod = values[1];

				Log.d("communicator", "XXXX LOAD KEY EXP '" + (new String(exp))
						+ "'");
				Log.d("communicator", "XXXX LOAD KEY MOD '" + (new String(mod))
						+ "'");

				// If convert from string
				// BigInteger biExp = new BigInteger(exp.getBytes());
				// BigInteger biMod = new BigInteger(mod.getBytes());

				// If convert from hexstring
				BigInteger biExp = new BigInteger(exp, 16);
				BigInteger biMod = new BigInteger(mod, 16);

				RSAPublicKeySpec pubKeySpec = new RSAPublicKeySpec(biMod, biExp);

				// RSAPublicKeySpec pubKeySpec = new RSAPublicKeySpec(
				// new BigInteger(1, mod.getBytes()), new BigInteger(1,
				// exp.getBytes()));
				KeyFactory keyFactory = null;
				keyFactory = KeyFactory.getInstance("RSA");
				PublicKey publicKey = keyFactory.generatePublic(pubKeySpec);
				return publicKey;
			}

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		/*
		 * 
		 * PublicKey publicKey = null; try { RSAPublicKeySpec pubKeySpec = new
		 * RSAPublicKeySpec(new BigInteger(1, modulus), new BigInteger(1,
		 * exponent)); KeyFactory keyFactory = null; if(provider != null &&
		 * !provider.isEmpty()) { keyFactory = KeyFactory.getInstance("RSA",
		 * provider); } else { keyFactory = KeyFactory.getInstance("RSA"); }
		 * 
		 * publicKey = keyFactory.generatePublic(pubKeySpec); } catch(Exception
		 * ex) { logger.error(ex.getMessage()); return null; }
		 */

		/*
		 * http://phpanswerz.com/content-28797482-convert-crypt-rsa-public-format
		 * -pkcs1-from-php-to-rsa-public-key-in-java.html
		 * 
		 * The documentation is a little confusing, but by looking at the source
		 * code for method _convertPublicKey($n, $e) starting at line 950 it
		 * appears that if $publicKeyFormat == PUBLIC_FORMAT_PKCS8 then the
		 * output format should be one that is compatible with Java's
		 * X509EncodedKeySpec class.
		 */

		// String encodedKeyAsString = null;
		// try {
		// encodedKeyAsString = new String(getServerkeyAsString(context));
		// encodedKeyAsString = new
		// String(encodedKeyAsString.getBytes("UTF-8"));
		// encodedKeyAsString = new String(Base64.decode(encodedKeyAsString,
		// Base64.DEFAULT));
		// encodedKeyAsString = encodedKeyAsString.substring(0,
		// encodedKeyAsString.length()-1);
		// } catch (Exception e1) {
		// e1.printStackTrace();
		// }
		// if ((encodedKeyAsString != null && encodedKeyAsString.length() != 0))
		// {
		// Log.d("communicator", "XXXX LOAD KEY 1 '"
		// + (new String(encodedKeyAsString)) + "'");
		//
		// byte[] decodedKey = Base64.decode(encodedKeyAsString,
		// Base64.DEFAULT);
		// Log.d("communicator", "XXXX LOAD KEY 2 '"
		// + (new String(decodedKey)) + "'");
		// // rebuild key using SecretKeySpec
		// // Key originalKey = new SecretKeySpec(decodedKey, 0,
		// // decodedKey.length, "AES");
		// try {
		// //X509EncodedKeySpec keySpec = new X509EncodedKeySpec(
		// // decodedKey);
		// KeyFactory kf = KeyFactory.getInstance("RSA");
		// PublicKey originalKey = kf.generatePublic(new
		// X509EncodedKeySpec(decodedKey));
		// //PublicKey originalKey = kf.generatePublic(keySpec);
		// return originalKey;
		// } catch (InvalidKeySpecException e) {
		// e.printStackTrace();
		// } catch (NoSuchAlgorithmException e) {
		// e.printStackTrace();
		// }
		// }
		return null;
	}

	// update if not present, will be deleted on login failure
	public static void updateServerkey(final Context context) {
		if (getServerkeyAsString(context).equals("")) {
			// no serverkey there, needs to update

			String url = null;
			url = Setup.getBaseURL(context) + "cmd=serverkey";
			final String url2 = url;
			HttpStringRequest httpStringRequest = (new HttpStringRequest(
					context, url2, new HttpStringRequest.OnResponseListener() {
						public void response(String response) {
							if (Communicator.isResponseValid(response)) {
								if (response.length() > 10) {
									Utility.saveStringSetting(
											context,
											Setup.SETTINGS_SERVERKEY,
											Communicator
													.getResponseContent(response));
									Log.d("communicator",
											"XXXX SAVED SERVER KEY '"
													+ getServerkeyAsString(context)
													+ "'");
									Communicator.internetOk = true;
								} else {
									Communicator.internetOk = false;
								}
							} else {
								Communicator.internetOk = false;
								// Log.d("communicator",
								// "XXXX FAILED TO UPDATE SERVER KEY '"
								// + response + "'");
							}
						}
					}));
		}
	}

	// -------------------------------------------------------------------------
	// -------------------------------------------------------------------------

	private static byte[] prepareKey(Context context) {
		try {
			String secret = Utility.md5(getSessionSecret(context).substring(5)); // the
			// first
			// 5
			// characters
			// remain
			// a
			// secret!
			String timeStampInSeconds = "" + ((DB.getTimestamp() / 1000) / 100);

			// Log.d("communicator",
			// "XXXXX prepare key secret='"
			// + secret + "'");
			//
			// Log.d("communicator",
			// "XXXXX prepare key timeStampInSeconds='"
			// + timeStampInSeconds + "'");

			timeStampInSeconds = Utility.md5(timeStampInSeconds);

			byte[] secretArray = secret.getBytes();
			byte[] timeStampInSecondsArray = timeStampInSeconds.getBytes();
			byte[] entrcypted = new byte[32];

			for (int i = 0; i < 32; i++) {
				entrcypted[i] = (byte) (secretArray[i] ^ timeStampInSecondsArray[i]);
			}
			return entrcypted;
		} catch (Exception e) {
			return null;
		}
	}

	// -------------------------------------------------------------------------
	public static String decText(Context context, String textEncrypted) {
		try {
			byte[] encrypted = Base64.decode(textEncrypted, Base64.DEFAULT);

			byte[] keyArray = prepareKey(context);
			if (keyArray == null) {
				return null;
			}
			byte[] decrypted = new byte[keyArray.length];

			int i = 0;
			for (byte b : encrypted) {
				decrypted[i] = (byte) (b ^ keyArray[i++]);
			}

			String decryptedString = new String(decrypted);

			int i1 = decryptedString.indexOf("#");
			int i2 = decryptedString.lastIndexOf("#");
			if (i1 >= 0 && i2 > -0) {
				String result = decryptedString.substring(i1 + 1, i2);
				// int result = Utility.parseInt(uid, -1);

				// Log.d("communicator",
				// "XXXXX dec uid result '"
				// + result + "'");

				return result;
			}
			return null;
		} catch (Exception e) {
			return null;
		}
	}

	// -------------------------------------------------------------------------

	public static String encText(Context context, String text) {
		byte[] keyArray = prepareKey(context);
		if (keyArray == null) {
			return null;
		}
		int pad = keyArray.length - text.length() - 2;
		int index = (int) (Math.random() * pad);
		String rnd = Utility.getRandomString(pad);
		text = rnd.substring(0, index) + "#" + text + "#"
				+ rnd.substring(index);

		byte[] textArray = text.getBytes();
		byte[] entrcypted = new byte[keyArray.length];
		int i = 0;
		for (byte b : textArray) {
			entrcypted[i] = (byte) (b ^ keyArray[i]);
			i++;
		}

		String encryptedString = Base64.encodeToString(entrcypted,
				Base64.DEFAULT);

		// Log.d("communicator",
		// "XXXXX enc encrypted '"
		// + encryptedString + "'");

		return encryptedString;
	}

	// -------------------------------------------------------------------------
	// -------------------------------------------------------------------------

	private static byte[] prepareSimpleKey(Context context) {
		String secret = getSessionSecret(context);
		if (secret == null || secret.length() < 20) {
			return null;
		}
		secret = Utility.md5(secret.substring(5)); // the first 5 characters
													// remain a secret!
		String timeStampInSeconds = "" + ((DB.getTimestamp() / 1000) / 100);

		// Log.d("communicator",
		// "XXXXX prepare key timeStampInSeconds='"
		// + timeStampInSeconds + "'");
		// Log.d("communicator",
		// "XXXXX prepare key secret='"
		// + secret + "'");

		timeStampInSeconds = Utility.md5(timeStampInSeconds);

		byte[] secretArray = secret.getBytes();
		byte[] timeStampInSecondsArray = timeStampInSeconds.getBytes();

		byte[] encrypted = new byte[8];

		encrypted[0] = timeStampInSecondsArray[0];
		encrypted[1] = secretArray[0];
		encrypted[2] = timeStampInSecondsArray[1];
		encrypted[3] = secretArray[1];
		encrypted[4] = timeStampInSecondsArray[2];
		encrypted[5] = secretArray[2];
		encrypted[6] = timeStampInSecondsArray[3];
		encrypted[7] = secretArray[3];

		// for (int c = 0; c <= 7; c++) {
		// Log.d("communicator",
		// "XXXXX KEY["+c+"]='"
		// + encrypted[c] + "'");
		// }

		return encrypted;
	}

	public static String encUid(Context context, int uid) {
		String text = uid + "";
		byte[] simpleKey = prepareSimpleKey(context);
		if (simpleKey == null) {
			return null;
		}
		String checkSum = Utility.md5(text).substring(0, 1);
		int uidEncrypted = uid + 1 * simpleKey[0] + 1 * simpleKey[1] + 10
				* simpleKey[2] + 10 * simpleKey[3] + 100 * simpleKey[4] + 100
				* simpleKey[5] + 1000 * simpleKey[6] + 1000 * simpleKey[7];
		return checkSum + uidEncrypted;
	}

	public static int decUid(Context context, String uidEncrypted) {
		byte[] simpleKey = prepareSimpleKey(context);
		if (simpleKey == null) {
			return -1;
		}
		String checkSum = uidEncrypted.substring(0, 1);
		String uidEncrypted2 = uidEncrypted.substring(1);

		int decUid = Utility.parseInt(uidEncrypted2, -1);
		if (decUid >= 0) {
			int diff = 1 * simpleKey[0] + 1 * simpleKey[1] + 10 * simpleKey[2]
					+ 10 * simpleKey[3] + 100 * simpleKey[4] + 100
					* simpleKey[5] + 1000 * simpleKey[6] + 1000 * simpleKey[7];
			decUid = decUid - diff;
			String checkSum2 = Utility.md5(decUid + "").substring(0, 1);
			if (checkSum.equals(checkSum2)) {
				return decUid;
			}
		}
		return -2; // make a difference to -1 which is just an invalid user but
					// -1 is invalid decoding == unknown user
	}

	// -------------------------------------------------------------------------

	public static void possiblyInvalidateSession(Context context, boolean reset) {
		Log.d("communicator", "possiblyInvalidateSession() reset? " + reset);
		if (!reset) {
			int counter = Utility.loadIntSetting(context,
					Setup.SETTINGS_INVALIDATIONCOUNTER,
					Setup.SETTINGS_INVALIDATIONCOUNTER_MAX);
			Log.d("communicator", "possiblyInvalidateSession() counter: "
					+ counter);
			if (counter > Setup.SETTINGS_INVALIDATIONCOUNTER_MAX) {
				// guard against two high counter
				// reset
				possiblyInvalidateSession(context, true);
				return;
			}
			if (counter > 0) {
				counter--;
				Utility.saveIntSetting(context,
						Setup.SETTINGS_INVALIDATIONCOUNTER, counter);
			} else {
				// reset
				possiblyInvalidateSession(context, true);
				// trigger a new login by invalidating the session
				invalidateTmpLogin(context);
			}
		} else {
			// reset to the maximum
			Utility.saveIntSetting(context, Setup.SETTINGS_INVALIDATIONCOUNTER,
					Setup.SETTINGS_INVALIDATIONCOUNTER_MAX);
		}
	}

	public static String getDeviceId(Context context) {
		String deviceID = Utility.getDeviceId(context);
		if (deviceID == null) {
			deviceID = "";
		}
		deviceID = Utility.md5(deviceID);
		if (deviceID.length() > 4) {
			// 0 inclusive, 4 exclusiv == 4 characters
			deviceID = deviceID.substring(0, 4);
		}
		return deviceID;
	}

	// ------------------------------------------------------------------------

	public void goBack(Context context) {
		// GET TO THE MAIN SCREEN IF THIS ICON IS CLICKED !
		Intent intent = new Intent(this, Main.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
	}

	// @Override
	// public boolean onOptionsItemSelected(MenuItem item) {
	// switch (item.getItemId()) {
	// case android.R.id.home:
	// // GET TO THE MAIN SCREEN IF THIS ICON IS CLICKED !
	// Intent intent = new Intent(this, Main.class);
	// intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	// startActivity(intent);
	// return true;
	// }
	// return false;
	// }

	// ------------------------------------------------------------------------

	static void possiblyDisableScreenshot(Activity activity) {
		if (Utility.loadBooleanSetting(activity, Setup.OPTION_NOSCREENSHOTS,
				Setup.DEFAULT_NOSCREENSHOTS)) {
			Utility.noScreenshots(activity);
		}
	}

	// ------------------------------------------------------------------------

	static void possiblyPromptNoEncryption(final Context context) {
		if (Setup.isEncryptionEnabled(context)) {
			return; // encryption is already enabled, everything is ok
		}
		if (Utility.loadBooleanSetting(context,
				Setup.SETTINGS_HAVEASKED_NOENCRYPTION, false)) {
			return; // we already asked!
		}
		if (!Setup.isUIDDefined(context)) {
			return; // no account defined!
		}
		// At this point we should ask the user to enable encryption!
		final String titleMessage = "Enable Encryption?";
		final String textMessage = "You currently have disabled encryption.\n\nGenerally this is a bad idea because nobody is able to send you encrypted private messages. You can still receive unencrypted plain text messages.\n\nDo you want to turn on encryption and enable you to send/receive secure messages?";
		new MessageAlertDialog(context, titleMessage, textMessage, " Yes ",
				" Stay Insecure ", null,
				new MessageAlertDialog.OnSelectionListener() {
					public void selected(int button, boolean cancel) {
						if (cancel) {
							// prompt again on cancel!
							possiblyPromptNoEncryption(context);
						}
						if (button == 0) {
							// enable
							enableEncryption(context);
						} else if (button == 1) {
							// remember we have asked
							saveHaveAskedForEncryption(context, true);
						}
					}
				}).show();
	}

	// ------------------------------------------------------------------------

	public static void saveHaveAskedForEncryption(Context context,
			boolean haveAsked) {
		Utility.saveBooleanSetting(context,
				Setup.SETTINGS_HAVEASKED_NOENCRYPTION, haveAsked);
	}

	// ------------------------------------------------------------------------

	public static boolean isUIDDefined(Context context) {
		return (Utility.loadStringSetting(context, "uid", "").trim().length() != 0);
	}

	// ------------------------------------------------------------------------
	// ------------------------------------------------------------------------
}
