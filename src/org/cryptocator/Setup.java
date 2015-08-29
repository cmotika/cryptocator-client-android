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
 * notice, an acknowledgment to all contributors, this list of conditions
 * and the following disclaimer in the documentation and/or other materials
 * provided with the distribution.
 * 
 * 3. Neither the name Delphino Cryptocator nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
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
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.List;

import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;

import android.annotation.SuppressLint;
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
import android.text.InputType;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * The Setup class is the third most important activity. It allows the user to
 * create an account or enter/login with an existing account. It allows to
 * enable the SMS option and has a second appearance for the settings dialog.
 * 
 * @author Christian Motika
 * @date 08/23/2015
 * @since 1.2
 * 
 */
@SuppressLint({ "TrulyRandom", "DefaultLocale" })
public class Setup extends Activity {

	// //// BASIC APP CONSTANTS //// //

	/** The server URL to be used per default. */
	public static String BASEURLDEFAULT = "http://www.cryptocator.org";

	/**
	 * The prefix to be used for UID databases names. All conversations are
	 * saved in separate databases.
	 */
	public static String DATABASEPREFIX = "dcomm";

	/** The postfix to be used for UID database names. */
	public static String DATABASEPOSTFIX = ".db";

	/** The name of the sending database. */
	public static String DATABASESENDING = "sending.db";

	/** The name of the database for keeping a mapping for read confirmations. */
	public static String DATABASESENT = "sent.db";

	/** The internal id for intentextra. */
	public static String INTENTEXTRA = "org.cryptocator.hostuid";

	/**
	 * The application package name used for making this app the default SMS
	 * app.
	 */
	public static String APPLICATION_PACKAGE_NAME = "org.cryptocator";

	/** The group for system notifications created by Cryptocator. */
	public static String GROUP_CRYPTOCATOR = "org.cryptocator.notificationgroup";

	/**
	 * The locked count-down initial value for enabling editing the account
	 * information.
	 */
	private int accountLocked = 3;

	/**
	 * The advanced count-down initial value for enabling advanced options that
	 * should typically not be edited.
	 */
	public static final int ADVANCEDSETUPCOUNTDOWNSTART = 11;

	/** The current advanced count-down. */
	private int advanedSetupCountdown = ADVANCEDSETUPCOUNTDOWNSTART;

	/** The the server URL as saved (if not the default one). */
	public static final String SETTINGS_BASEURL = "baseurl";

	/**
	 * The cached version of the base URL for faster access because of frequent
	 * use.
	 */
	private static String BASEURLCACHED = null;

	/**
	 * After this time these confirmations will be discarded (from the sent.db),
	 * maybe the person has not enabled read confirmation or has left and does
	 * not use Cryptocator any more. It is ~ 3 month.
	 */
	public static final int TIMEOUT_FOR_RECEIVEANDREAD_CONFIRMATIONS = 90 * 24
			* 60 * 60 * 1000;

	/**
	 * The timeout for renewing the session key when about to send a message.
	 * Typically 60 minutes.
	 */
	public static final int AES_KEY_TIMEOUT_SENDING = 60 * 60 * 1000;

	/**
	 * The timeout for renewing the session key when about to receive a message.
	 * Typically 70 minutes. It should be a little longer to have an asymmetry
	 * and reduce the risk of both clients sending new session keys at the same
	 * time. This would considered to be a session key clash and needs to be
	 * resolved manually by one of the client users initiating a new sesison.
	 */
	public static final int AES_KEY_TIMEOUT_RECEIVING = 70 * 60 * 1000;

	/**
	 * The length of the session secret. The secret should have this exact
	 * length because the server implementation also depends on this length. The
	 * secret lives only for one session/login. The secret should be 20 bytes
	 * long.
	 */
	public static final int SECRETLEN = 20;

	/**
	 * The length of the request salt. The salt should have this exact length
	 * because the server implementation also depends on this length. The salt
	 * is renewed for every request. The salt should be 10 bytes long.
	 */
	public static final int SALTLEN = 10;

	/**
	 * Number of additionally added stuffing bytes to enhance encryption for
	 * short and for messages of equal content. It ensures that an attacker
	 * cannot tell that two messages are equal. The length must be the same for
	 * both clients and should be 5.
	 */
	public static final int RANDOM_STUFF_BYTES = 5;

	/**
	 * Keys cannot be updated more frequent than this interval. This helps
	 * making the app more responsive when switching back to the main activity.
	 * This will always try to update keys but will skip if the previous update
	 * was not at least ago this interval of time. Only the user may manually
	 * trigger the refresh an override this interval. Its 1 min.
	 */
	public static final int UPDATE_KEYS_MIN_INTERVAL = 60 * 1000;

	/** Save the timestamp when the last key update took place. */
	public static final String SETTING_LASTUPDATEKEYS = "lastupdatekeys";

	/**
	 * Similar to the update keys (s.a.) this is for updating phone numbers. The
	 * interval is a little longer: 10 min.
	 */
	public static final int UPDATE_PHONES_MIN_INTERVAL = 10 * 60 * 1000; // 10

	/** Save the time stamp when the last phone number update took place. */
	public static final String SETTING_LASTUPDATEPHONES = "lastupdatephones";

	/**
	 * Similar to the keys this is the minimal interval when automatic refresh
	 * of user names takes place. Typically 60 min.
	 */
	public static final int UPDATE_NAMES_MIN_INTERVAL = 60 * 60 * 1000;

	/** Save the time stamp when the last user name update took place. */
	public static final String SETTING_LASTUPDATENAMES = "lastupdatenames";

	/**
	 * This is the regular update time for requesting new messages when the app
	 * is in the background and power-save is OFF. It is 20 sec.
	 */
	public static final int REGULAR_UPDATE_TIME = 20;

	/**
	 * This is the regular update time for requesting new messages when the app
	 * is in the foreground and power-save is OFF. It is 5 sec.
	 */
	public static final int REGULAR_UPDATE_TIME_FAST = 5;

	/**
	 * This is the regular update time for requesting new messages when the app
	 * is in the background and power-save is ON. It is 60 sec.
	 */
	public static final int REGULAR_POWERSAVE_UPDATE_TIME = 60;

	/**
	 * This is the regular update time for requesting new messages when the app
	 * is in the foreground and power-save is ON. It is 10 sec.
	 */
	public static final int REGULAR_POWERSAVE_UPDATE_TIME_FAST = 10;

	/**
	 * "recursion" interval for sending/receiveing multiple messages one after
	 * another in seconds. It is 5 sec.
	 */
	public static final int REGULAR_UPDATE_TIME_TRYNEXT = 5;

	/**
	 * do not interrupt the user while he types, only if he stops typing for at
	 * least these milliseconds, allow background activity (sending &&
	 * receiving). It is 5 sec.
	 */
	public static final int TYPING_TIMEOUT_BEFORE_BACKGROUND_ACTIVITY = 5000;

	/**
	 * do not interrupt if the user is typing fast, do not even SCROLL (UI
	 * activity) in this time but scroll if the user holds on for at least 1
	 * seconds.
	 */
	public static final int TYPING_TIMEOUT_BEFORE_UI_ACTIVITY = 2000;

	/** After a connection error first try again after 10 sec. */
	public static final int ERROR_UPDATE_INTERVAL = 10; // 10 seconds

	/**
	 * After multiple consecutive connection errors for each error add 50% of
	 * interval time to save energy.
	 */
	public static final int ERROR_UPDATE_INCREMENT = 50;

	/**
	 * After multiple consecutive connection errors do not enlarge the retry
	 * interval to more than this maximum, typically 5 min.
	 */
	public static final int ERROR_UPDATE_MAXIMUM = 5 * 60;

	/**
	 * When showing a conversation first show only this maximum number of
	 * messages. Typically 50.
	 */
	public static final int MAX_SHOW_CONVERSATION_MESSAGES = 50;

	/** The Constant for showing ALL messages. */
	public static final int SHOW_ALL = -1;

	/** The Constant for not applicable. */
	public static final String NA = "N/A";

	/** The Constant ERROR_TIME_TO_WAIT. */
	public static final String ERROR_TIME_TO_WAIT = "errortimetowait";

	/** The Constant OPTION_ACTIVE. */
	public static final String OPTION_ACTIVE = "active";

	/** The Constant DEFAULT_ACTIVE. */
	public static final boolean DEFAULT_ACTIVE = true;

	/** The Constant HELP_ACTIVE. */
	public static final String HELP_ACTIVE = "Enables the background service for receiving messages. Turning this off may save battery but you will not receive messages if the app is closed.";

	/** The Constant OPTION_TONE. */
	public static final String OPTION_TONE = "System Alert Tone";

	/** The Constant DEFAULT_TONE. */
	public static final boolean DEFAULT_TONE = true;

	/** The Constant HELP_TONE. */
	public static final String HELP_TONE = "Play system alert tone when a new message is received (and the phone is not muted).";

	/** The Constant OPTION_VIBRATE. */
	public static final String OPTION_VIBRATE = "vibrate";

	/** The Constant DEFAULT_VIBRATE. */
	public static final boolean DEFAULT_VIBRATE = true;

	/** The Constant HELP_VIBTRATE. */
	public static final String HELP_VIBTRATE = "Vibrate when a new message is received (and the phone is not muted).";

	/** The Constant OPTION_NOTIFICATION. */
	public static final String OPTION_NOTIFICATION = "notification";

	/** The Constant DEFAULT_NOTIFICATION. */
	public static final boolean DEFAULT_NOTIFICATION = true;

	/** The Constant HELP_NOTIFICATION. */
	public static final String HELP_NOTIFICATION = "Prompt a system notification when a new message is received.";

	/** The Constant OPTION_IGNORE. */
	public static final String OPTION_IGNORE = "ignore";

	/** The Constant DEFAULT_IGNORE. */
	public static final boolean DEFAULT_IGNORE = false;

	/** The Constant HELP_IGNORE. */
	public static final String HELP_IGNORE = "Only message from users in your userlist will be received. Messages from other users will be silently discarded.";

	/** The Constant OPTION_ENCRYPTION. */
	public static final String OPTION_ENCRYPTION = "encryption";

	/** The Constant DEFAULT_ENCRYPTION. */
	public static final boolean DEFAULT_ENCRYPTION = false;

	/** The Constant HELP_ENCRYPTION. */
	public static final String HELP_ENCRYPTION = "Use encryption for sending messages. Will only work if your communication partner has also turned on encryption.\n\nIt is strongly advised that you always leave encryption on!";

	/** The Constant OPTION_AUTOSAVE. */
	public static final String OPTION_AUTOSAVE = "autosaveattachments";

	/** The Constant DEFAULT_AUTOSAVE. */
	public static final boolean DEFAULT_AUTOSAVE = true;

	/** The Constant HELP_AUTOSAVE. */
	public static final String HELP_AUTOSAVE = "Image attachments can be automatically saved to your gallery.";

	/** The Constant OPTION_NOREAD. */
	public static final String OPTION_NOREAD = "noread";

	/** The Constant DEFAULT_NOREAD. */
	public static final boolean DEFAULT_NOREAD = false;

	/** The Constant HELP_NOREAD. */
	public static final String HELP_NOREAD = "Refuse read confirmations for received messages (second blue checkmark).\n\nWARNING: If you refuse read confirmation you cannot see read confirmations of anybody else!";

	/** The Constant OPTION_NOSCREENSHOTS. */
	public static final String OPTION_NOSCREENSHOTS = "noscreenshots";

	/** The Constant DEFAULT_NOSCREENSHOTS. */
	public static final boolean DEFAULT_NOSCREENSHOTS = true;

	/** The Constant HELP_NOSCREENSHOTS. */
	public static final String HELP_NOSCREENSHOTS = "Disallows making automatic or manual screenshots of your messages for privacy protection.";

	/** The Constant OPTION_CHATMODE. */
	public static final String OPTION_CHATMODE = "chatmode";

	/** The Constant DEFAULT_CHATMODE. */
	public static final boolean DEFAULT_CHATMODE = false;

	/** The Constant HELP_CHATMODE. */
	public static final String HELP_CHATMODE = "Send a message by hitting <RETURN>. If chat mode is turned on, you cannot make explicit linebreaks.";

	/** The Constant OPTION_QUICKTYPE. */
	public static final String OPTION_QUICKTYPE = "Quick Type";

	/** The Constant DEFAULT_QUICKTYPE. */
	public static final boolean DEFAULT_QUICKTYPE = true;

	/** The Constant HELP_QUICKTYPE. */
	public static final String HELP_QUICKTYPE = "If you switch your phone orientation to landscape in order to type, the keyboard is shown automatically and you can just start typing without extra clicking into the message input text field.";

	/** The Constant OPTION_QUICKTYPE. */
	public static final String OPTION_SMILEYS = "Graphical Smileys";

	/** The Constant DEFAULT_QUICKTYPE. */
	public static final boolean DEFAULT_SMILEYS = true;

	/** The Constant HELP_QUICKTYPE. */
	public static final String HELP_SMILEY = "If you enable this option then textual smileys are shown as graphical ones.";

	/** The Constant OPTION_RECEIVEALLSMS. */
	public static final String OPTION_RECEIVEALLSMS = "receiveallsms";

	/** The Constant DEFAULT_RECEIVEALLSMS. */
	public static final boolean DEFAULT_RECEIVEALLSMS = false;

	/** The Constant HELP_RECEIVEALLSMSE. */
	public static final String HELP_RECEIVEALLSMSE = "You can use Delphino Cryptocator even as your default app for all SMS. Users that are not registered are listed by their names from your address book and you can only send them plain text SMS.";

	/** The Constant OPTION_POWERSAVE. */
	public static final String OPTION_POWERSAVE = "powersave";

	/** The Constant DEFAULT_POWERSAVE. */
	public static final boolean DEFAULT_POWERSAVE = true;

	/** The Constant HELP_POWERSAVE. */
	public static final String HELP_POWERSAVE = "Delphino Cryptocator can operate in a power save mode were sending/receiving is reduced to every 10 seconds when active or 60 seconds when passive instead of 5 seconds and 20 seconds respectively in the non-power save mode.\n\nThis mode saves your battery.";

	/** The Constant OPTION_SMSMODE. */
	public static final String OPTION_SMSMODE = "smsmode";

	/** The Constant DEFAULT_SMSMODE. */
	public static final boolean DEFAULT_SMSMODE = false;

	/** The Constant PUBKEY for saving/loading the public RSA key. */
	public static final String PUBKEY = "pk";

	/** The Constant PRIVATEKEY for saving/loading the private RSA key. */
	public static final String PRIVATEKEY = "k";

	/** The Constant AESKEY for session keys. */
	public static final String AESKEY = "aes";

	/**
	 * The Constant KEYEXTRAWAITCOUNTDOWN is a countdown that is set by the
	 * automated session key sending component for SMS to 10 and for Internet to
	 * 3. Basically it enforces to delay the sending of the next SMS or Internet
	 * message a certain amount of time to make it more likely that the new auto
	 * generated session key has been received before.
	 */
	public static final String KEYEXTRAWAITCOUNTDOWN = "keyextrawaitcountdown";

	/**
	 * The Constant LASTKEYMID for the last mid of a key message sent to a user.
	 * This should be reset when a new key is send, it should be set by the
	 * first use of DB.getLastSendKeyMessage().
	 */
	public static final String LASTKEYMID = "lastaeskeymid";

	/** The Constant PHONE for other users phone numbers. */
	public static final String PHONE = "hostphone";

	/** The Constant SETTINGS_USERLIST for saving/loading the userlist. */
	public static final String SETTINGS_USERLIST = "userlist";

	/**
	 * The Constant SETTINGS_USERLISTLASTMESSAGE for saving/loading the last
	 * message per user.
	 */
	public static final String SETTINGS_USERLISTLASTMESSAGE = "userlistlastmessage";

	/**
	 * The Constant SETTINGS_USERLISTLASTMESSAGETIMESTAMP for saving/loading the
	 * timestamp of the last message per user.
	 */
	public static final String SETTINGS_USERLISTLASTMESSAGETIMESTAMP = "userlistlastmessagetimestamp";

	/**
	 * The Constant SETTINGS_HAVESENTRSAKEYYET for remember if we have sent the
	 * rsa key to the server yet.
	 */
	public static final String SETTINGS_HAVESENTRSAKEYYET = "sentrsakeyforuser";

	/** The Constant SETTINGS_PHONE for the phone number. */
	public static final String SETTINGS_PHONE = "phone";

	/** The Constant SETTINGS_UPDATENAME. */
	public static final String SETTINGS_UPDATENAME = "updatename";

	/** The Constant SETTINGS_DEFAULT_UPDATENAME. */
	public static final boolean SETTINGS_DEFAULT_UPDATENAME = true;

	/** The Constant SETTINGS_UPDATEPHONE. */
	public static final String SETTINGS_UPDATEPHONE = "updatephone";

	/** The Constant SETTINGS_DEFAULT_UPDATEPHONE. */
	public static final boolean SETTINGS_DEFAULT_UPDATEPHONE = true;

	/** The Constant SETTINGS_PHONEISMODIFIED. */
	public static final String SETTINGS_PHONEISMODIFIED = "phoneismodified";

	/**
	 * The Constant SETTINGS_DEFAULTMID. Globally for all users: This is the
	 * base mid, we do not request messages before this mid.
	 */
	public static final String SETTINGS_DEFAULTMID = "defaultmid";

	/**
	 * The Constant SETTINGS_DEFAULTMID_DEFAULT. Globally for all users: should
	 * only be -1 if there is no msg in the DB. Then we retrieve the highest mid
	 * from server!
	 */
	public static final int SETTINGS_DEFAULTMID_DEFAULT = -1;

	/**
	 * The Constant SETTINGS_SERVERKEY for sending, e.g., encrypted login data
	 * to the server.
	 */
	public static final String SETTINGS_SERVERKEY = "serverkey";

	/** The Constant SETTINGS_SESSIONSECRET. To remember the session. */
	public static final String SETTINGS_SESSIONSECRET = "tmpsessionsecret";

	/** The Constant SETTINGS_SESSIONID. To remember the session id */
	public static final String SETTINGS_SESSIONID = "tmpsessionid";

	/**
	 * The Constant SETTINGS_LOGINERRORCNT. Saves the login errors that we
	 * receive from the server on successful login. It is currently not used.
	 */
	public static final String SETTINGS_LOGINERRORCNT = "loginerrorcnt";

	/**
	 * The Constant SETTINGS_INVALIDATIONCOUNTER. If uids are corrupted more
	 * than MAX (see next) then invalidate the session.
	 */
	public static final String SETTINGS_INVALIDATIONCOUNTER = "invalidationcounter";

	/**
	 * The Constant SETTINGS_INVALIDATIONCOUNTER_MAX. If uids are corrupted more
	 * than MAX (see next) then invalidate the session
	 */
	public static final int SETTINGS_INVALIDATIONCOUNTER_MAX = 2;

	/**
	 * The Constant SETTINGS_LARGEST_MID_RECEIVED. This is used as a basis for
	 * receiving (newer) messages than this mid.
	 */
	public static final String SETTINGS_LARGEST_MID_RECEIVED = "largestmidreceived";

	/**
	 * The Constant SETTINGS_LARGEST_TS_RECEIVED. This is used as a basis for
	 * receiving (newer) receive confirmation than this timestamp.
	 */
	public static final String SETTINGS_LARGEST_TS_RECEIVED = "largesttsreceived";

	/**
	 * The Constant SETTINGS_LARGEST_TS_READ. This is used as a basis for
	 * receiving (newer) read confirmation than this timestamp.
	 */
	public static final String SETTINGS_LARGEST_TS_READ = "largesttsread";

	/**
	 * The Constant SETTINGS_HAVEASKED_NOENCRYPTION. Remember if we have asked
	 * the user to enable the encryption option. This is reset on UID change or
	 * when turning the encryption feature off.
	 */
	public static final String SETTINGS_HAVEASKED_NOENCRYPTION = "haveaskednoenryption";

	/**
	 * The Constant SMS_FAIL_CNT. Try to send an SMS 5 times before claiming
	 * failed (ONLY counting unknown errors, NO network errors!). If the network
	 * is down the SMS should wait until it is up again and NOT fail.
	 */
	public static final int SMS_FAIL_CNT = 3;

	/**
	 * The Constant SERVER_ATTACHMENT_LIMIT. Must be acquired by the server on
	 * refresh of userlist or if not set (=-1). A value of 0 means that
	 * attachments are not allowed for Internet messages and any other values
	 * limits the size in KB for messages.
	 */
	public static final String SERVER_ATTACHMENT_LIMIT = "serverattachmentlimit";

	/**
	 * The Constant SERVER_ATTACHMENT_LIMIT_DEFAULT tells that we did not yet
	 * load the server limit.
	 */
	public static final int SERVER_ATTACHMENT_LIMIT_DEFAULT = -1;

	/**
	 * The Constant UPDATE_SERVER_ATTACHMENT_LIMIT_INTERVAL. Update at most
	 * every 60 minutes.
	 */
	public static final int UPDATE_SERVER_ATTACHMENT_LIMIT_MINIMAL_INTERVAL = 60; // Minutes

	/**
	 * The Constant LASTUPDATE_SERVER_ATTACHMENT_LIMIT. The timestamp when the
	 * server limit last time
	 */
	public static final String LASTUPDATE_SERVER_ATTACHMENT_LIMIT = "lastupdateserverattachmentlimit";

	/**
	 * The Constant SIZEWARNING_SMS. If this numebr of bytes is passed more than
	 * 10 multipart SMS need to be send. Before sending such large SMS alert the
	 * user!
	 */
	public static final int SMS_SIZE_WARNING = 1600;

	/** The standardsmssize. */
	public static int SMS_DEFAULT_SIZE = 160;

	// ------------------------------------------------------------------------

	/** The active. */
	private CheckBox active;

	/** The encryption. */
	private CheckBox encryption;

	/** The notification. */
	private CheckBox notification;

	/** The tone. */
	private CheckBox tone;

	/** The vibrate. */
	private CheckBox vibrate;

	/** The ignore. */
	private CheckBox ignore;

	/** The autosave. */
	private CheckBox autosave;

	/** The noread. */
	private CheckBox noread;

	/** The chatmode. */
	private CheckBox chatmode;

	/** The quicktype. */
	private CheckBox quicktype;

	/** The smileys. */
	private CheckBox smileys;

	/** The noscreenshots. */
	private CheckBox noscreenshots;

	/** The powersave. */
	private CheckBox powersave;

	/** The receiveallsms. */
	private CheckBox receiveallsms;

	/** The helpactive. */
	private ImageView helpactive;

	/** The helpencryption. */
	private ImageView helpencryption;

	/** The helptone. */
	private ImageView helptone;

	/** The helpvibrate. */
	private ImageView helpvibrate;

	/** The helpnotification. */
	private ImageView helpnotification;

	/** The helpignore. */
	private ImageView helpignore;

	/** The helpautosave. */
	private ImageView helpautosave;

	/** The helpnoread. */
	private ImageView helpnoread;

	/** The helpquicktype. */
	private ImageView helpquicktype;

	/** The helpsmileys. */
	private ImageView helpsmileys;

	/** The helpchatmode. */
	private ImageView helpchatmode;

	/** The helpnoscreenshots. */
	private ImageView helpnoscreenshots;

	/** The helppowersave. */
	private ImageView helppowersave;

	/** The helpreceiveallsms. */
	private ImageView helpreceiveallsms;

	/** The uid. */
	private static EditText uid;

	/** The email. */
	private EditText email;

	/** The pwd. */
	private EditText pwd;

	/** The usernew. */
	private EditText usernew;

	/** The emailnew. */
	private EditText emailnew;

	/** The pwdnew. */
	private EditText pwdnew;

	/** The user. */
	private EditText user;

	/** The pwdchange. */
	private EditText pwdchange;

	/** The error. */
	private static TextView error;

	/** The info. */
	private static TextView info;

	/** The deviceid. */
	private static TextView deviceid;

	/** The advancedsettings. */
	private static LinearLayout advancedsettings;

	/** The baseurltext. */
	private static EditText baseurltext;

	/** The baseurlbutton. */
	private static Button baseurlbutton;

	/** The buttonclearsending. */
	private static Button buttonclearsending;

	/** The buttondebugprint. */
	private static Button buttondebugprint;

	/** The buttondeletedatabase. */
	private static Button buttondeletedatabase;

	/** The create. */
	private Button create;

	/** The login. */
	private Button login;

	/** The updatepwd. */
	private Button updatepwd;

	/** The updateuser. */
	private Button updateuser;

	/** The phone. */
	private static EditText phone;

	/** The enablesmsoption. */
	private static Button enablesmsoption;

	/** The disablesmsoption. */
	private static Button disablesmsoption;

	/** The backup. */
	private static Button backup;

	/** The restore. */
	private Button restore;

	/** The accountnew. */
	private LinearLayout accountnew;

	/** The accountexisting. */
	private LinearLayout accountexisting;

	/** The accountonline. */
	private static LinearLayout accountonline;

	/** The settingspart. */
	private LinearLayout settingspart;

	/** The mainscrollview. */
	private static ScrollView mainscrollview;

	/** The online. */
	private static boolean online = false;

	/** The newaccount. */
	private CheckBox newaccount;

	/** The account type. */
	private boolean accountType = false;

	// ------------------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Setup.possiblyDisableScreenshot(this);

		final Activity context = this;

		// Apply custom title bar (with holo :-)
		// See Main for more details.
		LinearLayout main = Utility.setContentViewWithCustomTitle(this,
				R.layout.activity_setup, R.layout.title_general);

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
		autosave = (CheckBox) findViewById(R.id.autosave);
		noread = (CheckBox) findViewById(R.id.noread);
		chatmode = (CheckBox) findViewById(R.id.chatmode);
		quicktype = (CheckBox) findViewById(R.id.quicktype);
		smileys = (CheckBox) findViewById(R.id.smileys);
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
							}
						});
			}

			// other settings
			active.setChecked(Utility.loadBooleanSetting(context,
					OPTION_ACTIVE, DEFAULT_ACTIVE));
			active.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					setActive(context, active.isChecked());
					if (active.isChecked()) {
						// Start the scheduler
						Scheduler.reschedule(context, false, false, false);
					} else {
						// We do not have to end the scheduler, it will end
						// automatically...
					}
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
			autosave.setChecked(Utility.loadBooleanSetting(context,
					Setup.OPTION_AUTOSAVE, Setup.DEFAULT_AUTOSAVE));
			autosave.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					Utility.saveBooleanSetting(context, Setup.OPTION_AUTOSAVE,
							autosave.isChecked());
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
			smileys.setChecked(Utility.loadBooleanSetting(context,
					Setup.OPTION_SMILEYS, Setup.DEFAULT_SMILEYS));
			smileys.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					Utility.saveBooleanSetting(context, Setup.OPTION_SMILEYS,
							smileys.isChecked());
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

			// help icon clicks
			helpactive = (ImageView) findViewById(R.id.helpactive);
			helpencryption = (ImageView) findViewById(R.id.helpencryption);
			helptone = (ImageView) findViewById(R.id.helptone);
			helpvibrate = (ImageView) findViewById(R.id.helpvibrate);
			helpnotification = (ImageView) findViewById(R.id.helpnotification);
			helpignore = (ImageView) findViewById(R.id.helpignore);
			helpautosave = (ImageView) findViewById(R.id.helpautosave);
			helpnoread = (ImageView) findViewById(R.id.helpnoread);
			helpchatmode = (ImageView) findViewById(R.id.helpchatmode);
			helpquicktype = (ImageView) findViewById(R.id.helpquicktype);
			helpsmileys = (ImageView) findViewById(R.id.helpsmileys);
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
			helpautosave.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					setErrorInfo(HELP_AUTOSAVE, false);
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
			helpsmileys.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					setErrorInfo(HELP_SMILEY, false);
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

			// Setting backgrounds
			Utility.setBackground(this, main, R.drawable.dolphins1);
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

	// ------------------------------------------------------------------------

	/**
	 * Gets the base url of the server. Typically this is cryptocator.org if not
	 * another server is configured.
	 * 
	 * @param context
	 *            the context
	 * @return the base url
	 */
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

	// ------------------------------------------------------------------------

	/**
	 * Sets the title of the custom title bar of the setup activity.
	 * 
	 * @param title
	 *            the new title
	 */
	public void setTitle(String title) {
		TextView titletext = (TextView) findViewById(R.id.titletext);
		titletext.setText(title);
	}

	// -------------------------------------------------------------------------

	/**
	 * Update account locked. Count down the number of times the user cliks on
	 * the account login information. If it was more than the initial
	 * accountLocked valued (we count down to 0), then unlock the user
	 * information text input fields but prompt a warning.
	 * 
	 * @param context
	 *            the context
	 * @param silent
	 *            the silent
	 */
	private void updateAccountLocked(Context context, boolean silent) {
		if (accountLocked > 0) {
			login.setText("   Validate / Login   ");
			newaccount.setEnabled(false);
			uid.setFocusable(false);
			email.setFocusable(false);
			pwd.setFocusable(false);
			uid.setClickable(true);
			email.setClickable(true);
			pwd.setClickable(true);
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

			login.setText("   Validate / Save   ");
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
		}
	}

	// -------------------------------------------------------------------------

	/**
	 * Update title id info. The title info field always displayes the DeviceID
	 * and the current RSA account key to the user.
	 * 
	 * @param context
	 *            the context
	 */
	public static void updateTitleIDInfo(Context context) {
		deviceid.setText("DeviceID: " + getDeviceId(context)
				+ "   --   Account Key: " + Setup.getPublicKeyHash(context));
	}

	// -------------------------------------------------------------------------

	/**
	 * Prompt to change server base url. This is basically a WARNING.
	 * 
	 * @param context
	 *            the context
	 */
	public void promptChangeBaseURL(final Context context) {
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

	/**
	 * Update phone number and button states.
	 * 
	 * @param context
	 *            the context
	 */
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

	/**
	 * Checks if is SMS mode on.
	 * 
	 * @param context
	 *            the context
	 * @param hostUid
	 *            the host uid
	 * @return true, if is SMS mode on
	 */
	public static boolean isSMSModeOn(Context context, int hostUid) {
		return (hostUid < 0 || Utility.loadBooleanSetting(context,
				Setup.OPTION_SMSMODE + hostUid, Setup.DEFAULT_SMSMODE));
	}

	// -------------------------------------------------------------------------

	/**
	 * Sets the error info.
	 * 
	 * @param errorMessage
	 *            the new error info
	 */
	void setErrorInfo(String errorMessage) {
		setErrorInfo(this, errorMessage, true);
	}

	/**
	 * Sets the error info.
	 * 
	 * @param message
	 *            the message
	 * @param isError
	 *            the is error
	 */
	void setErrorInfo(String message, boolean isError) {
		setErrorInfo(this, message, isError);
	}

	/**
	 * Sets the error info.
	 * 
	 * @param context
	 *            the context
	 * @param errorMessage
	 *            the error message
	 */
	static void setErrorInfo(Context context, String errorMessage) {
		setErrorInfo(context, errorMessage, true);
	}

	/**
	 * Sets the error info.
	 * 
	 * @param context
	 *            the context
	 * @param message
	 *            the message
	 * @param isError
	 *            the is error
	 */
	static void setErrorInfo(Context context, String message, boolean isError) {
		if (error == null || info == null || uid == null
				|| mainscrollview == null) {
			// Not visible only send this as toast
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
		}
	}

	// -------------------------------------------------------------------------

	/**
	 * Updateonline if the login/validation was successful or hide account
	 * modifications (change pw, change username, enable/disable sms option) in
	 * case online is false.
	 */
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

	/**
	 * Validate the login. This will also set online:=true in the success case.
	 * If the account login information was change we here detect an UID change
	 * and remove the key from server and shutdown the app.
	 * 
	 * @param context
	 *            the context
	 */
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
		uidStringEnc = Utility.urlEncode(uidStringEnc);
		String emailStringEnd = Communicator.encryptServerMessage(context,
				emailString, serverKey);
		if (emailStringEnd == null) {
			setErrorInfo("Encryption error. Try again after restarting the App.");
			login.setEnabled(true);
			return;
		}
		emailStringEnd = Utility.urlEncode(emailStringEnd);

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
		@SuppressWarnings("unused")
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
									} else {
										if (response2.equals("-4")) {
											// Email already registered
											setErrorInfo("You account has not been activated yet. Go to your email inbox and follow the activation link! Be sure to also look in the spam email folder.\n\nIf you cannot find the activation email, click the following link to resend it to you:\n"
													+ reactivateurl2);
										} else if (response2.equals("-11")) {
											// Email already registered
											setErrorInfo("You new password has not been activated yet. Go to your email inbox and follow the activation link! Be sure to also look in the spam email folder.\n\nIf you cannot find the activation email, you can reset your password once more here:\n"
													+ reseturl2);
										} else {
											// Clear server key to enforce a
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
							// Clear server key to enforce a soon reload!
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

	/**
	 * Update the username if the user has logged in / validate before.
	 * 
	 * @param context
	 *            the context
	 */
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
		usernameStringEnc = Utility.urlEncode(usernameStringEnc);

		String session = Setup.getTmpLoginEncoded(context);
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
		@SuppressWarnings("unused")
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

	/**
	 * Update pwd if the user has logged in/validate before. On success this
	 * will update the saved password. The new password must be activated
	 * afterwards by clicking on the link in the email. Otherwise the old
	 * password will not change. But due to the fact that the new password is
	 * already saved the user is forced to 1. click on the link in the email to
	 * activate the new password or alternatively 2. reenter the old password.
	 * 
	 * @param context
	 *            the context
	 */
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
		pwdChangeStringEnc = Utility.urlEncode(pwdChangeStringEnc);

		String session = Setup.getTmpLoginEncoded(context);
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
		@SuppressWarnings("unused")
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

	/**
	 * Creates a new account. It disables encryption (so the user is prompted on
	 * next restart of the app to enable encryption). After creating the account
	 * it needs to be activated which can only be done by clicking on the link
	 * in the email. If the account is not activated it is useless and the
	 * server is allowed to clear un-activated accounts after a while.
	 * 
	 * @param context
	 *            the context
	 */
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
		emailString = Utility.urlEncode(emailString);

		pwdString = Communicator.encryptServerMessage(context, pwdString,
				serverKey);
		if (pwdString == null) {
			setErrorInfo("Encryption error. Habe you specified a valid password? Try again after restarting the App.");
			create.setEnabled(true);
			return;
		}
		pwdString = Utility.urlEncode(pwdString);

		usernameString = Communicator.encryptServerMessage(context,
				usernameString, serverKey);
		if (usernameString == null) {
			setErrorInfo("Encryption error. Habe you specified a valid username? Try again after restarting the App.");
			create.setEnabled(true);
			return;
		}
		usernameString = Utility.urlEncode(usernameString);

		String url = null;
		String reseturl = null;
		url = Setup.getBaseURL(context) + "cmd=create&email=" + emailString
				+ "&pwd=" + pwdString + "&user=" + usernameString;
		reseturl = Setup.getBaseURL(context) + "cmd=resetpwd&email="
				+ emailString;
		Log.d("communicator", "CREATE ACCOUNt: " + url);

		final String url2 = url;
		final String reseturl2 = reseturl;
		@SuppressWarnings("unused")
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

										// Utility.saveStringSetting(context,
										// "uid", uidString);
										String emailString = emailnew.getText()
												.toString().trim();
										String usernameString = usernew
												.getText().toString().trim();
										String pwdString = pwdnew.getText()
												.toString();
										Utility.saveStringSetting(context,
												"username", usernameString);
										Utility.saveStringSetting(context,
												"email", emailString);
										Utility.saveStringSetting(context,
												"pwd", pwdString);

										Utility.saveBooleanSetting(context,
												Setup.OPTION_ENCRYPTION, false);
										saveHaveAskedForEncryption(context,
												false);

										newaccount.setChecked(false);
										updateCurrentMid(context);
										setErrorInfo(
												"Registration successfull!\n\nYOUR NEW UNIQUE UID IS: "
														+ newUID
														+ "\n\nGo to your email account and follow the activation link we sent you. Be sure to also check your spam email folder.",
												false);
										Utility.showToastAsync(
												context,
												"Your new UID is "
														+ newUID
														+ ". Cryptocator must be re-started in order to operate properly...");
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

	/**
	 * Update current highest mid. This is necessary to receive (request) the
	 * correct next newest messages from the server. After an UID change or
	 * after creating a new account the current highest mid is fetched from the
	 * server.
	 * 
	 * @param context
	 *            the context
	 */
	public static void updateCurrentMid(final Context context) {
		String url = null;
		url = Setup.getBaseURL(context) + "cmd=mid";
		final String url2 = url;
		@SuppressWarnings("unused")
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
						}
					}
				}));
	}

	// ------------------------------------------------------------------------
	// ------------------------------------------------------------------------

	/**
	 * Backup the userlist to the server. There is a manual and an automatic
	 * backup. The automatic is used if SMS option is turned on in order do
	 * validate if someone else is allowed to download/see our phone number.
	 * Only registered users will be backed up. Only the UIDs will be backed up
	 * no display name or phone number will be send to the server!
	 * 
	 * @param context
	 *            the context
	 * @param silent
	 *            the silent
	 * @param manual
	 *            the manual
	 */
	public static void backup(final Context context, final boolean silent,
			boolean manual) {
		if (backup != null) {
			backup.setEnabled(false);
		}
		if (!silent) {
			setErrorInfo(context, null);
		}

		// String userlistString = Utility.loadStringSetting(context,
		// SETTINGS_USERLIST, "");
		List<Integer> uidList = Main.loadUIDList(context);

		if (uidList.size() == 0) {
			if (!silent) {
				setErrorInfo(context, "No users in your list yet!");
			}
			if (backup != null) {
				backup.setEnabled(true);
			}
			return;
		}

		// We MUST encrypt each UID individually because the message gets too
		// long otherwise for RSA encryption
		String userlistString = "";
		for (int uid : uidList) {
			// Only backup registered users for now!
			if (uid >= 0) {
				String uidEnc = Setup.encUid(context, uid);
				if (userlistString.length() > 0) {
					userlistString += "#";
				}
				userlistString += uidEnc;
			}
		}

		// // RSA encode
		// PublicKey serverKey = getServerkey(context);
		// String userlistStringEnc = Communicator.encryptServerMessage(context,
		// userlistString, serverKey);
		// if (userlistStringEnc == null) {
		// setErrorInfo(context,
		// "Encryption error. Try again after restarting the App.");
		// backup.setEnabled(true);
		// return;
		// }
		String userlistStringEnc = Utility.urlEncode(userlistString);

		String session = Setup.getTmpLoginEncoded(context);
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
		@SuppressWarnings("unused")
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
													"Backup of user list to server successful. You can now later restore it, e.g., on a different device or in case of data loss.\n\nNote that no messages are backed up.\n\nAlso note that ONLY registered users are backed up and only their UID not their display name or phone number was saved at the server!",
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

	/**
	 * Restore the userlist. Again, there is a manual and an automatic version.
	 * 
	 * @param context
	 *            the context
	 * @param manual
	 *            the manual
	 */
	void restore(final Context context, boolean manual) {
		restore.setEnabled(false);
		setErrorInfo(null);

		String session = Setup.getTmpLoginEncoded(context);
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
		@SuppressWarnings("unused")
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

	/**
	 * Update SMS option. If turning SMS option on then also backup the current
	 * userlist to the server (automatic). Send the phone number to the server.
	 * If disabling turn off automatic backup and clear the phone number from
	 * the server. <BR>
	 * <BR>
	 * If SMS option is disabled, also clear all automatically downloaded phone
	 * numbers!
	 * 
	 * @param context
	 *            the context
	 * @param enable
	 *            the enable
	 */
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
		if (!enable) {
			// Remove all automatically downloaed phone numbers! Only users that
			// enable the SMS option themselves are eligable to do an
			// autoupdate.
			for (Integer uid : Main.loadUIDList(context)) {
				if (!Setup.isPhoneModified(context, uid)) {
					// Only for registered users we will get here
					// We fake "manual" here because after disabling the SMS
					// option the user can only manually edit a phone number
					// anyways.
					Setup.savePhone(context, uid, null, true);
				}
			}
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
		phoneStringEnc = Utility.urlEncode(phoneStringEnc);

		String session = Setup.getTmpLoginEncoded(context);
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
		@SuppressWarnings("unused")
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

	/**
	 * Sets the active flag for the background send/receive service.
	 * 
	 * @param context
	 *            the context
	 * @param active
	 *            the active
	 */
	public static void setActive(Context context, boolean active) {
		Utility.saveBooleanSetting(context, Setup.OPTION_ACTIVE, active);
		if (active) {
			Scheduler.reschedule(context, false, true, false);
		}
	}

	// -------------------------------------------------------------------------

	/**
	 * Checks if the background send/receive service is active.
	 * 
	 * @param context
	 *            the context
	 * @return true, if is active
	 */
	public static boolean isActive(Context context) {
		return Utility.loadBooleanSetting(context, Setup.OPTION_ACTIVE,
				Setup.DEFAULT_ACTIVE);
	}

	// -------------------------------------------------------------------------

	/**
	 * Gets the error update interval that is calculated based on the
	 * consecutive errors that might have occurred.
	 * 
	 * @param context
	 *            the context
	 * @return the error update interval
	 */
	public static int getErrorUpdateInterval(Context context) {
		return Utility.loadIntSetting(context, ERROR_TIME_TO_WAIT,
				Setup.ERROR_UPDATE_INTERVAL);
	}

	// -------------------------------------------------------------------------

	/**
	 * Sets or recalculates the error update interval which might increase after
	 * many consecutive errors up to a maximum. If error is false, then the
	 * interval is reset to its default minimal interval.
	 * 
	 * @param context
	 *            the context
	 * @param error
	 *            the error
	 */
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

	/**
	 * Checks if is user active.
	 * 
	 * @return true, if is user active
	 */
	// Returns true if the user is supposed to be actively using the program
	public static boolean isUserActive() {
		return (Conversation.isVisible() || Main.isVisible());
	}

	// -------------------------------------------------------------------------

	/**
	 * Enable encryption.
	 * 
	 * @param context
	 *            the context
	 */
	@SuppressLint({ "DefaultLocale", "TrulyRandom" })
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

	/**
	 * Disable encryption.
	 * 
	 * @param context
	 *            the context
	 */
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

	/**
	 * Checks if is AES key outdated.
	 * 
	 * @param context
	 *            the context
	 * @param uid
	 *            the uid
	 * @param forSending
	 *            the for sending
	 * @return true, if is AES key outdated
	 */
	public static boolean isAESKeyOutdated(Context context, int uid,
			boolean forSending, int transport) {
		long nowTime = DB.getTimestamp();
		long otherTime = getAESKeyDate(context, uid, transport);
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

	// -------------------------------------------------------------------------

	/**
	 * Gets the AES key date.
	 * 
	 * @param context
	 *            the context
	 * @param uid
	 *            the uid
	 * @return the AES key date
	 */
	public static long getAESKeyDate(Context context, int uid, int transport) {
		String keycreated = Utility.loadStringSetting(context, Setup.AESKEY
				+ "created" + transport + "_" + uid, "");
		if ((keycreated == null || keycreated.length() == 0)) {
			return 0;
		}
		long returnKey = Utility.parseLong(keycreated, 0);
		return returnKey;
	}

	// -------------------------------------------------------------------------

	/**
	 * Sets the AES key date.
	 * 
	 * @param context
	 *            the context
	 * @param uid
	 *            the uid
	 * @param keycreated
	 *            the keycreated
	 */
	public static void setAESKeyDate(Context context, int uid,
			String keycreated, int transport) {
		Utility.saveStringSetting(context, Setup.AESKEY + "created" + transport
				+ "_" + uid, keycreated);
	}

	// -------------------------------------------------------------------------

	/**
	 * Save aes key.
	 * 
	 * @param context
	 *            the context
	 * @param uid
	 *            the uid
	 * @param key
	 *            the key
	 */
	public static void saveAESKey(Context context, int uid, String key) {
		Utility.saveStringSetting(context, Setup.AESKEY + uid, key);
	}

	// -------------------------------------------------------------------------

	/**
	 * Have aes key.
	 * 
	 * @param context
	 *            the context
	 * @param uid
	 *            the uid
	 * @return true, if successful
	 */
	public static boolean haveAESKey(Context context, int uid) {
		String key = Utility.loadStringSetting(context, Setup.AESKEY + uid, "");
		boolean returnValue = false;
		if ((key != null && key.length() != 0)) {
			returnValue = true;
		}
		return returnValue;
	}

	// -------------------------------------------------------------------------

	/**
	 * Serialize aes key.
	 * 
	 * @param key
	 *            the key
	 * @return the string
	 */
	public static String serializeAESKey(Key key) {
		return Base64.encodeToString(key.getEncoded(), Base64.DEFAULT);
	}

	// -------------------------------------------------------------------------

	/**
	 * Generate aes key.
	 * 
	 * @param randomSeed
	 *            the random seed
	 * @return the key
	 */
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

	/**
	 * Gets the AES key as string.
	 * 
	 * @param context
	 *            the context
	 * @param uid
	 *            the uid
	 * @return the AES key as string
	 */
	public static String getAESKeyAsString(Context context, int uid) {
		String encodedKey = Utility.loadStringSetting(context, Setup.AESKEY
				+ uid, "");
		return encodedKey;
	}

	/**
	 * Gets the AES key hash.
	 * 
	 * @param context
	 *            the context
	 * @param uid
	 *            the uid
	 * @return the AES key hash
	 */
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

	/**
	 * Gets the AES key.
	 * 
	 * @param context
	 *            the context
	 * @param uid
	 *            the uid
	 * @return the AES key
	 */
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

	/**
	 * Gets the key date.
	 * 
	 * @param context
	 *            the context
	 * @param uid
	 *            the uid
	 * @return the key date
	 */
	public static String getKeyDate(Context context, int uid) {
		String keycreated = Utility.loadStringSetting(context, Setup.PUBKEY
				+ "created" + uid, "");
		if ((keycreated == null || keycreated.length() == 0)) {
			return null;
		}
		return keycreated;
	}

	// -------------------------------------------------------------------------

	/**
	 * Sets the key date.
	 * 
	 * @param context
	 *            the context
	 * @param uid
	 *            the uid
	 * @param keycreated
	 *            the keycreated
	 */
	public static void setKeyDate(Context context, int uid, String keycreated) {
		Utility.saveStringSetting(context, Setup.PUBKEY + "created" + uid,
				keycreated);
	}

	// -------------------------------------------------------------------------

	/**
	 * Save key.
	 * 
	 * @param context
	 *            the context
	 * @param uid
	 *            the uid
	 * @param key
	 *            the key
	 */
	public static void saveKey(Context context, int uid, String key) {
		Utility.saveStringSetting(context, Setup.PUBKEY + uid, key);
	}

	// -------------------------------------------------------------------------

	/**
	 * Have key.
	 * 
	 * @param context
	 *            the context
	 * @param uid
	 *            the uid
	 * @return true, if successful
	 */
	public static boolean haveKey(Context context, int uid) {
		String key = Utility.loadStringSetting(context, Setup.PUBKEY + uid, "");
		boolean returnValue = false;
		if ((key != null && key.length() != 0)) {
			returnValue = true;
		}
		return returnValue;
	}

	// -------------------------------------------------------------------------

	/**
	 * Gets the key as string.
	 * 
	 * @param context
	 *            the context
	 * @param uid
	 *            the uid
	 * @return the key as string
	 */
	public static String getKeyAsString(Context context, int uid) {
		String encodedKey = Utility.loadStringSetting(context, Setup.PUBKEY
				+ uid, "");
		return encodedKey;
	}

	/**
	 * Gets the key hash.
	 * 
	 * @param context
	 *            the context
	 * @param uid
	 *            the uid
	 * @return the key hash
	 */
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

	/**
	 * Gets the key.
	 * 
	 * @param context
	 *            the context
	 * @param encodedKeyAsString
	 *            the encoded key as string
	 * @return the key
	 */
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

	/**
	 * Gets the key.
	 * 
	 * @param context
	 *            the context
	 * @param uid
	 *            the uid
	 * @return the key
	 */
	public static PublicKey getKey(Context context, int uid) {
		String encodedKeyAsString = getKeyAsString(context, uid);
		return (getKey(context, encodedKeyAsString));
	}

	// -------------------------------------------------------------------------

	/**
	 * Gets the public key as string.
	 * 
	 * @param context
	 *            the context
	 * @return the public key as string
	 */
	public static String getPublicKeyAsString(Context context) {
		String encodedKey = Utility
				.loadStringSetting(context, Setup.PUBKEY, "");
		return encodedKey;
	}

	/**
	 * Gets the public key hash.
	 * 
	 * @param context
	 *            the context
	 * @return the public key hash
	 */
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

	/**
	 * Gets the public key.
	 * 
	 * @param context
	 *            the context
	 * @return the public key
	 */
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

	/**
	 * Gets the private key.
	 * 
	 * @param context
	 *            the context
	 * @return the private key
	 */
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

	/**
	 * Encrypted sent possible.
	 * 
	 * @param context
	 *            the context
	 * @param uid
	 *            the uid
	 * @return true, if successful
	 */
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

	/**
	 * Checks if is encryption enabled.
	 * 
	 * @param context
	 *            the context
	 * @return true, if is encryption enabled
	 */
	public static boolean isEncryptionEnabled(Context context) {
		boolean encryption = Utility.loadBooleanSetting(context,
				Setup.OPTION_ENCRYPTION, Setup.DEFAULT_ENCRYPTION);
		return encryption;
	}

	// -------------------------------------------------------------------------

	/**
	 * Checks if is encryption available.
	 * 
	 * @param context
	 *            the context
	 * @param hostUid
	 *            the host uid
	 * @return true, if is encryption available
	 */
	public static boolean isEncryptionAvailable(Context context, int hostUid) {
		boolean encryption = isEncryptionEnabled(context);
		boolean haveKey = Setup.haveKey(context, hostUid);
		return (encryption && haveKey);
	}

	// -------------------------------------------------------------------------
	// -------------------------------------------------------------------------

	/**
	 * Save phone is modified.
	 * 
	 * @param context
	 *            the context
	 * @param uid
	 *            the uid
	 * @param isModified
	 *            the is modified
	 */
	public static void savePhoneIsModified(Context context, int uid,
			boolean isModified) {
		Utility.saveBooleanSetting(context, Setup.SETTINGS_PHONEISMODIFIED
				+ uid, isModified);
	}

	/**
	 * Checks if is phone number was modified by the user manually.
	 * 
	 * @param context
	 *            the context
	 * @param uid
	 *            the uid
	 * @return true, if is phone modified
	 */
	public static boolean isPhoneModified(Context context, int uid) {
		// for NOT registered users this is always true!
		if (uid < 0) {
			return true;
		}
		return Utility.loadBooleanSetting(context,
				Setup.SETTINGS_PHONEISMODIFIED + uid, false);
	}

	// -------------------------------------------------------------------------

	/**
	 * Save phone for a user.
	 * 
	 * @param context
	 *            the context
	 * @param uid
	 *            the uid
	 * @param phone
	 *            the phone
	 * @param manual
	 *            the manual
	 */
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

	// -------------------------------------------------------------------------

	/**
	 * Gets the phone for a user.
	 * 
	 * @param context
	 *            the context
	 * @param uid
	 *            the uid
	 * @return the phone
	 */
	public static String getPhone(Context context, int uid) {
		return Utility.loadStringSetting(context, Setup.PHONE + uid, null);
	}

	// -------------------------------------------------------------------------

	/**
	 * Have phone for a user.
	 * 
	 * @param context
	 *            the context
	 * @param uid
	 *            the uid
	 * @return true, if successful
	 */
	public static boolean havePhone(Context context, int uid) {
		String phone = getPhone(context, uid);
		return (phone != null && phone.length() > 0);
	}

	// -------------------------------------------------------------------------

	/**
	 * Gets the UID by phone. This is necessary to sort incoming SMS.
	 * 
	 * @param context
	 *            the context
	 * @param phone
	 *            the phone
	 * @param allInternalAndExternalUsers
	 *            the all internal and external users
	 * @return the UID by phone
	 */
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

	// -------------------------------------------------------------------------

	/**
	 * Checks if is SMS option enabled.
	 * 
	 * @param context
	 *            the context
	 * @return true, if is SMS option enabled
	 */
	public static boolean isSMSOptionEnabled(Context context) {
		String personalphone = Utility.loadStringSetting(context,
				SETTINGS_PHONE, "");
		return (personalphone.trim().length() != 0);
	}

	// -------------------------------------------------------------------------

	/**
	 * Ask merge user. If a user is already in the userlist and a separate user
	 * existed with this telephone number we ask to merge both accounts
	 * together.
	 * 
	 * @param context
	 *            the context
	 * @param smsuserAsFallback
	 *            the smsuser as fallback
	 * @param registeredDefault
	 *            the registered default
	 */
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
											Main.deleteUser(context,
													smsuserAsFallback);
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

	/**
	 * Ask on disable encryption.
	 * 
	 * @param context
	 *            the context
	 */
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

	/**
	 * Ask on refuse read confirmation.
	 * 
	 * @param context
	 *            the context
	 */
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

	/**
	 * No account yet.
	 * 
	 * @param context
	 *            the context
	 * @return true, if successful
	 */
	public static boolean noAccountYet(Context context) {
		String uidString = Utility.loadStringSetting(context, "uid", "");
		return (uidString == null || uidString.length() == 0);
	}

	// ------------------------------------------------------------------------

	/**
	 * Normalize phone.
	 * 
	 * @param phone
	 *            the phone
	 * @return the string
	 */
	public static String normalizePhone(String phone) {
		return phone.replace("(", "").replace(")", "").replace(" ", "")
				.replace("-", "").replace("/", "");
	}

	// ------------------------------------------------------------------------
	// -------------------------------------------------------------------------

	/**
	 * Calculate a hopefully unique UID from a phone numer.
	 * 
	 * @return the fake uid from phone
	 */
	private static int FAKEUIDLEN = 4;

	/**
	 * Gets the fake uid from phone.
	 * 
	 * @param phone
	 *            the phone
	 * @return the fake uid from phone
	 */
	public static int getFakeUIDFromPhone(String phone) {
		// Log.d("communicator", "XXX FAKEUID input phone=" + phone);
		phone = Setup.normalizePhone(phone);
		phone = phone.replace("+49", "");
		phone = phone.replace("+1", "");
		phone = phone.replace("+", "");
		// phone = phone.replaceAll("[^0-9]", "");
		// // Log.d("communicator", "XXX FAKEUID normalized phone=" + phone);
		// int parts = phone.length() / FAKEUIDLEN;
		// if (phone.length() % FAKEUIDLEN != 0) {
		// parts++;
		// }
		// int returnUID = 0;
		// int phoneLen = phone.length();
		// for (int part = 0; part < parts; part++) {
		// int start = part * FAKEUIDLEN;
		// int end = start + FAKEUIDLEN;
		// if (end >= phoneLen) {
		// end = phoneLen - 1;
		// }
		// // Log.d("communicator", "XXX FAKEUID start=" + start + ", end=" +
		// // end);
		// String phonePart = phone.substring(start, end);
		// int phonePartInt = Utility.parseInt(phonePart, 0);
		// // Log.d("communicator", "XXX FAKEUID part[" + part + "] returnUID="
		// // + returnUID + " + phonePartInt=" + phonePartInt);
		// returnUID = returnUID + phonePartInt;
		// }
		// // Log.d("communicator", "XXX FAKEUID " + phone + " --> "
		// // + (-1 * returnUID));
		// if (returnUID == 0) {

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
		int returnUID = Utility.parseInt(tmp2, 0);
		// }
		// Log.d("communicator", "XXX FAKEUID RETURNED " + phone + " --> "
		// + (-1 * returnUID));
		return (-1 * returnUID);
	}

	// -------------------------------------------------------------------------
	// ------------------------------------------------------------------------

	/**
	 * Prompt disable receive all sms.
	 * 
	 * @param context
	 *            the context
	 */
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

	/**
	 * Checks if is SMS default app.
	 * 
	 * @param context
	 *            the context
	 * @param strictOnlyLocalSettings
	 *            the strict only local settings
	 * @return true, if is SMS default app
	 */
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

	// -------------------------------------------------------------------------

	/**
	 * Sets the SMS default app.
	 * 
	 * @param context
	 *            the context
	 * @param enable
	 *            the enable
	 */
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

	/**
	 * Gets the session secret.
	 * 
	 * @param context
	 *            the context
	 * @return the session secret
	 */
	public static String getSessionSecret(final Context context) {
		return Utility.loadStringSetting(context, Setup.SETTINGS_SESSIONSECRET,
				"");
	}

	// -------------------------------------------------------------------------

	/**
	 * Gets the session id.
	 * 
	 * @param context
	 *            the context
	 * @return the session id
	 */
	public static String getSessionID(final Context context) {
		return Utility.loadStringSetting(context, Setup.SETTINGS_SESSIONID, "");
	}

	// -------------------------------------------------------------------------

	/**
	 * Invalidate tmp login.
	 * 
	 * @param context
	 *            the context
	 */
	// if a tmplogin failed, do a new login!
	public static void invalidateTmpLogin(Context context) {
		Utility.saveStringSetting(context, Setup.SETTINGS_SESSIONSECRET, "");
		Utility.saveStringSetting(context, Setup.SETTINGS_SESSIONID, "");
	}

	// -------------------------------------------------------------------------

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

	// -------------------------------------------------------------------------

	/**
	 * Gets the tmp login url encoded or null.
	 * 
	 * @param context
	 *            the context
	 * @return the tmp login encoded
	 */
	public static String getTmpLoginEncoded(Context context) {
		String session = getTmpLogin(context);
		if (session != null) {
			return Utility.urlEncode(session);
		}
		return null;
	}

	// -------------------------------------------------------------------------

	/**
	 * Gets the tmp login. returns the session value that is used expect
	 * session=md5(sessionid#timestampinseconds#secret#salt)#sessionid#salt
	 * 
	 * @param context
	 *            the context
	 * @return the tmp login
	 */
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
		return session;
	}

	// -------------------------------------------------------------------------

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
		loginData.user = Utility.urlEncode(eLoginUser);
		loginData.password = Utility.urlEncode(eLoginPassword);
		loginData.val = Utility.urlEncode(encryptedBase64ValString);

		return loginData;
	}

	// -------------------------------------------------------------------------

	/**
	 * Update successfull login.
	 * 
	 * @param context
	 *            the context
	 * @param sessionID
	 *            the session id
	 * @param loginErrCnt
	 *            the login err cnt
	 */
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

	// -------------------------------------------------------------------------

	/**
	 * Login.
	 * 
	 * @param context
	 *            the context
	 */
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
			@SuppressWarnings("unused")
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

	// -------------------------------------------------------------------------

	/**
	 * Gets the serverkey as string.
	 * 
	 * @param context
	 *            the context
	 * @return the serverkey as string
	 */
	public static String getServerkeyAsString(final Context context) {
		return Utility.loadStringSetting(context, Setup.SETTINGS_SERVERKEY, "");
	}

	// -------------------------------------------------------------------------

	/**
	 * Gets the serverkey.
	 * 
	 * @param context
	 *            the context
	 * @return the serverkey
	 */
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
		return null;
	}

	// -------------------------------------------------------------------------

	/**
	 * Update serverkey.
	 * 
	 * @param context
	 *            the context
	 */
	// Update if not present, will be deleted on login failure
	public static void updateServerkey(final Context context) {
		if (getServerkeyAsString(context).equals("")) {
			// No serverkey there, needs to update

			String url = null;
			url = Setup.getBaseURL(context) + "cmd=serverkey";
			final String url2 = url;
			@SuppressWarnings("unused")
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

	/**
	 * Prepare key.
	 * 
	 * @param context
	 *            the context
	 * @return the byte[]
	 */
	private static byte[] prepareKey(Context context) {
		try {
			String secret = Utility.md5(getSessionSecret(context).substring(5)); // the
			// first 5 characters remain a secret!
			String timeStampInSeconds = "" + ((DB.getTimestamp() / 1000) / 100);

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

	/**
	 * Dec text.
	 * 
	 * @param context
	 *            the context
	 * @param textEncrypted
	 *            the text encrypted
	 * @return the string
	 */
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
				return result;
			}
			return null;
		} catch (Exception e) {
			return null;
		}
	}

	// -------------------------------------------------------------------------

	/**
	 * Enc text.
	 * 
	 * @param context
	 *            the context
	 * @param text
	 *            the text
	 * @return the string
	 */
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
		return encryptedString;
	}

	// -------------------------------------------------------------------------
	// -------------------------------------------------------------------------

	/**
	 * Prepare simple key.
	 * 
	 * @param context
	 *            the context
	 * @return the byte[]
	 */
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

	// -------------------------------------------------------------------------

	/**
	 * Enc uid.
	 * 
	 * @param context
	 *            the context
	 * @param uid
	 *            the uid
	 * @return the string
	 */
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

	// -------------------------------------------------------------------------

	/**
	 * Dec uid.
	 * 
	 * @param context
	 *            the context
	 * @param uidEncrypted
	 *            the uid encrypted
	 * @return the int
	 */
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

	/**
	 * Possibly invalidate session.
	 * 
	 * @param context
	 *            the context
	 * @param reset
	 *            the reset
	 */
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

	// -------------------------------------------------------------------------

	/**
	 * Gets the device id.
	 * 
	 * @param context
	 *            the context
	 * @return the device id
	 */
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

	/**
	 * Go back.
	 * 
	 * @param context
	 *            the context
	 */
	public void goBack(Context context) {
		// GET TO THE MAIN SCREEN IF THIS ICON IS CLICKED !
		Intent intent = new Intent(this, Main.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
	}

	// ------------------------------------------------------------------------

	/**
	 * Possibly disable screenshot.
	 * 
	 * @param activity
	 *            the activity
	 */
	static void possiblyDisableScreenshot(Activity activity) {
		if (Utility.loadBooleanSetting(activity, Setup.OPTION_NOSCREENSHOTS,
				Setup.DEFAULT_NOSCREENSHOTS)) {
			Utility.noScreenshots(activity);
		}
	}

	// ------------------------------------------------------------------------

	/**
	 * Possibly prompt no encryption.
	 * 
	 * @param context
	 *            the context
	 */
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

	/**
	 * Save have asked for encryption.
	 * 
	 * @param context
	 *            the context
	 * @param haveAsked
	 *            the have asked
	 */
	public static void saveHaveAskedForEncryption(Context context,
			boolean haveAsked) {
		Utility.saveBooleanSetting(context,
				Setup.SETTINGS_HAVEASKED_NOENCRYPTION, haveAsked);
	}

	// ------------------------------------------------------------------------

	/**
	 * Checks if is UID defined.
	 * 
	 * @param context
	 *            the context
	 * @return true, if is UID defined
	 */
	public static boolean isUIDDefined(Context context) {
		return (Utility.loadStringSetting(context, "uid", "").trim().length() != 0);
	}

	// ------------------------------------------------------------------------

	/**
	 * Update attachment server limit if more than
	 * UPDATE_SERVER_ATTACHMENT_LIMIT_MINIMAL_INTERVAL minutes have passed or
	 * forceUpdate is set.
	 * 
	 * @param context
	 *            the context
	 * @param forceUpdate
	 *            the force update
	 */
	public static void updateAttachmentServerLimit(final Context context,
			boolean forceUpdate) {
		long lastTime = Utility.loadLongSetting(context,
				Setup.LASTUPDATE_SERVER_ATTACHMENT_LIMIT, 0);
		final long currentTime = DB.getTimestamp();
		if (!forceUpdate
				&& (lastTime
						+ (Setup.UPDATE_SERVER_ATTACHMENT_LIMIT_MINIMAL_INTERVAL * 1000 * 60) > currentTime)) {
			// Do not do this more frequently
			return;
		}

		String url = null;
		url = Setup.getBaseURL(context) + "cmd=attachments";
		final String url2 = url;
		@SuppressWarnings("unused")
		HttpStringRequest httpStringRequest = (new HttpStringRequest(context,
				url2, new HttpStringRequest.OnResponseListener() {
					public void response(String response) {
						if (Communicator.isResponseValid(response)) {
							if (Communicator.isResponsePositive(response)) {
								// Save the attachment limit in KB
								String content = Communicator
										.getResponseContent(response);
								if (content != null) {
									int limit = Utility.parseInt(content, -1);
									Utility.saveIntSetting(context,
											Setup.SERVER_ATTACHMENT_LIMIT,
											limit);
									Log.d("communicator",
											"XXXX SAVED SERVER ATTACHMENT LIMIT KB='"
													+ content + "'");
									Utility.saveLongSetting(
											context,
											Setup.LASTUPDATE_SERVER_ATTACHMENT_LIMIT,
											currentTime);
								}
							} else {
								// No attachments allowed
								Utility.saveIntSetting(context,
										Setup.SERVER_ATTACHMENT_LIMIT, 0);
							}
						}
					}
				}));
	}

	// ------------------------------------------------------------------------

	/**
	 * Gets the attachment server limit. -1 means that no limit has been set
	 * (this is also means we do not allow attachments), 0 means that the server
	 * does not allow attachments at all and any other values is the limit in
	 * KB.
	 * 
	 * @param context
	 *            the context
	 * @return the attachment server limit
	 */
	public static int getAttachmentServerLimit(Context context) {
		return Utility.loadIntSetting(context, Setup.SERVER_ATTACHMENT_LIMIT,
				Setup.SERVER_ATTACHMENT_LIMIT_DEFAULT);
	}

	// ------------------------------------------------------------------------

	/**
	 * Checks if is attachments allowed by server that is if the server has ben
	 * queried already if he allows and if the server has responded to allow
	 * more than 0 KB.
	 * 
	 * @param context
	 *            the context
	 * @return true, if is attachments allowed by server
	 */
	public static boolean isAttachmentsAllowedByServer(Context context) {
		return (getAttachmentServerLimit(context) > 0);
	}

	// ------------------------------------------------------------------------

	/**
	 * Extra count down to zero. Returns true if sending is permitted, it
	 * returns false if we need to wait for the next cycle and the count down is
	 * not yet at zero.
	 * 
	 * @param context
	 *            the context
	 * @return true, if successful
	 */
	public static boolean extraCountDownToZero(Context context) {
		int cnt = Utility.loadIntSetting(context, Setup.KEYEXTRAWAITCOUNTDOWN,
				0);
		if (cnt <= 0) {
			return true;
		}
		cnt = cnt - 1;
		Utility.saveIntSetting(context, Setup.KEYEXTRAWAITCOUNTDOWN, cnt);
		return false;
	}

	// ------------------------------------------------------------------------

	/**
	 * Establish the extra crount down depending on the transport type.
	 * 
	 * @param context
	 *            the context
	 * @param transport
	 *            the transport
	 */
	public static void extraCrountDownSet(Context context, int transport) {
		int cnt = 15; // For SMS
		if (transport == DB.TRANSPORT_INTERNET) {
			cnt = 5;
		}
		Utility.saveIntSetting(context, Setup.KEYEXTRAWAITCOUNTDOWN, cnt);
	}

	// ------------------------------------------------------------------------

}
