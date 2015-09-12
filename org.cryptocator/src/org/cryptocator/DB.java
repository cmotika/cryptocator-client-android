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
 *    both (a) and (b) are and stay fulfilled: 
 *    (a) This license is enclosed.
 *    (b) The protocol to communicate between Cryptocator servers
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * The DB class handles the local data base accesses and is responsible for
 * loading conversations or adding new messages to a conversation.
 * 
 * @author Christian Motika
 * @since 1.2
 * @date 08/23/2015
 * 
 */
@SuppressLint({ "SimpleDateFormat", "UseSparseArrays" })
public class DB {

	/**
	 * The database/table TABLE_MESSAGES contains all messages for a specific
	 * user (UID).
	 */
	public static final String TABLE_MESSAGES = "messages";

	/**
	 * The database/table TABLE_SENDING contains all messages enqueued to be
	 * sent. There is only one such database/table.
	 */
	public static final String TABLE_SENDING = "sending";

	/**
	 * database/table TABLE_SENT contains a mapping between all sent messages
	 * (mid) and the hostuid were we did not receive a read confirmation yet but
	 * are awaiting such a confirmation.
	 */
	public static final String TABLE_SENT = "sent";

	/**
	 * The globally last received mid (cached version). There is also a settings
	 * entry to be updated: Setup.SETTINGS_DEFAULTMID.
	 */
	@SuppressLint("UseSparseArrays")
	public static HashMap<Integer, Integer> lastReceivedMid = new HashMap<Integer, Integer>();

	/** The SMS dummy mid. */
	public static int SMS_MID = -1;

	/** The transport Internet. */
	public static int TRANSPORT_INTERNET = 0;

	/** The transport SMS. */
	public static int TRANSPORT_SMS = 1;

	/** The priority for readconfirmation. */
	public static int PRIORITY_READCONFIRMATION = 0;

	/** The priority for failed to decrypt message. */
	public static int PRIORITY_FAILEDTODECRYPT = 0;

	/** The priority for message. */
	public static int PRIORITY_MESSAGE = 1;

	/** The priority for key. */
	public static int PRIORITY_KEY = 2;

	/** The maximal timestamp. */
	public static long MAXTIMESTAMP = Long.MAX_VALUE;

	/** The revoked text. */
	public static String REVOKEDTEXT = "[ message revoked ]";

	/** The SMS failed. */
	public static String SMS_FAILED = "FAILED";

	/** The default empty string represents no multipart id. */
	public static String NO_MULTIPART_ID = "";

	/** The default message part is 0 also for non-multipart messages */
	public static int DEFAULT_MESSAGEPART = 0;

	/**
	 * The multipart timeout is used for clearing duplicate incoming parts of
	 * multipart SMS. Look back 10 min to clear these duplications.
	 */
	public static int MULTIPART_TIMEOUT = 10 * 60 * 1000;

	/** The default receipient uid == WE. */
	public static int DEFAULT_MYSELF_UID = -3;

	// -----------------------------------------------------------------

	/** The active user uid. */
	private static HashMap<Integer, Integer> myUid = new HashMap<Integer, Integer>();

	// -----------------------------------------------------------------

	/**
	 * Returns the active user's real uid w.r.t. a server.
	 * 
	 * @param context
	 *            the context
	 * @return the int
	 */
	public static int myUid(Context context, int serverId) {
		try {
			if (!myUid.containsKey(serverId)) {
				String uidString = Utility.loadStringSetting(context,
						Setup.SERVER_UID + serverId, "");
				try {
					myUid.put(serverId, Integer.parseInt(uidString));
				} catch (Exception e) {
				}
			}
			return myUid.get(serverId);
		} catch(Exception e) {
			return -1;
		}
	}

	/**
	 * My uid dummy place holder locally.
	 * 
	 * @return the int
	 */
	public static int myUid() {
		return DEFAULT_MYSELF_UID;
	}

	// -----------------------------------------------------------------

	/**
	 * Gets the timestamp as string.
	 * 
	 * @return the timestamp string
	 */
	public static String getTimestampString() {
		return getTimestamp() + "";
	}

	// -----------------------------------------------------------------

	/**
	 * Gets the timestamp.
	 * 
	 * @return the timestamp
	 */
	public static long getTimestamp() {
		long timeStamp = System.currentTimeMillis(); // calendar.getTimeInMillis();
		return timeStamp;
	}

	// -----------------------------------------------------------------

	/**
	 * Parses the timestamp. If this fails the current time is returned.
	 * 
	 * @param timestampString
	 *            the timestamp string
	 * @return the long
	 */
	public static long parseTimestamp(String timestampString) {
		return parseTimestamp(timestampString, getTimestamp());
	}

	/**
	 * Parses the timestamp with a default value.
	 * 
	 * @param timestampString
	 *            the timestamp string
	 * @param defaultValue
	 *            the default value
	 * @return the long
	 */
	public static long parseTimestamp(String timestampString, long defaultValue) {
		try {
			long returnValue = Long.parseLong(timestampString);
			return returnValue;
		} catch (Exception e) {
		}
		return defaultValue;
	}

	// -----------------------------------------------------------------

	/**
	 * Gets the date string.
	 * 
	 * @param timestamp
	 *            the timestamp
	 * @param details
	 *            the details
	 * @return the date string
	 */
	public static String getDateString(long timestamp, boolean details) {

		if (timestamp < 5000) {
			return Setup.NA;
		}

		String format = "HH:mm,  dd. MMM yyyy";
		String thisYear = Utility.getYear(getTimestamp());
		String otherYear = Utility.getYear(getTimestamp());
		if (thisYear.equals(otherYear)) {
			// If the same year, then do not print it
			format = "HH:mm, MMM dd";
		}
		if (details) {
			// format = "EEE, d MMM yyyy  HH:mm:ss";
			// format = "EEE, MM/dd/yy  HH:mm:ss";
			// format = "d MMM yyyy  HH:mm:ss";
			format = "HH:mm:ss, d. MMM ''yy";
		}

		// Create a DateFormatter object for displaying date in specified
		// format.
		SimpleDateFormat formatter = new SimpleDateFormat(format);
		// Create a calendar object that will convert the date and time value in
		// milliseconds to date.
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(timestamp);
		String returnValue = formatter.format(calendar.getTime());
		return returnValue;
	}

	// -----------------------------------------------------------------

	/**
	 * Open a user specific database for the messages.
	 * 
	 * @param context
	 *            the context
	 * @param uid
	 *            the uid
	 * @return the SQ lite database
	 */
	public static SQLiteDatabase openDB(Context context, int uid) {
		SQLiteDatabase db;
		db = context.openOrCreateDatabase(Setup.DATABASEPREFIX + uid
				+ Setup.DATABASEPOSTFIX, SQLiteDatabase.CREATE_IF_NECESSARY,
				null);
		if (!isTableExists(db, TABLE_MESSAGES)) {
			initializeDB(context, db);
		}
		return db;
	}

	// -----------------------------------------------------------------

	/**
	 * Open the unique database for enqueued messages about to be sent.
	 * 
	 * @param context
	 *            the context
	 * @return the SQ lite database
	 */
	public static SQLiteDatabase openDBSending(Context context) {
		SQLiteDatabase db;
		db = context.openOrCreateDatabase(Setup.DATABASESENDING,
				SQLiteDatabase.CREATE_IF_NECESSARY, null);
		if (!isTableExists(db, TABLE_SENDING)) {
			initializeDBSending(context, db);
		}
		return db;
	}

	// -----------------------------------------------------------------

	/**
	 * Open unique database for already sent messages where waiting for read
	 * confirmations.
	 * 
	 * @param context
	 *            the context
	 * @return the SQ lite database
	 */
	public static SQLiteDatabase openDBSent(Context context) {
		SQLiteDatabase db;
		db = context.openOrCreateDatabase(Setup.DATABASESENT,
				SQLiteDatabase.CREATE_IF_NECESSARY, null);
		if (!isTableExists(db, TABLE_SENT)) {
			initializeDBSending(context, db);
		}
		return db;
	}

	// -----------------------------------------------------------------

	/**
	 * Drop a user message db.
	 * 
	 * @param context
	 *            the context
	 * @param uid
	 *            the uid
	 */
	public static void dropDB(Context context, int uid) {
		SQLiteDatabase db = openDB(context, uid);
		if (isTableExists(db, TABLE_MESSAGES)) {
			final String CREATE_TABLE_MSGS = "DROP TABLE `" + TABLE_MESSAGES
					+ "`;";
			db.execSQL(CREATE_TABLE_MSGS);
		}
		db.close();
	}

	// -----------------------------------------------------------------

	/**
	 * Drop the unique db sending.
	 * 
	 * @param context
	 *            the context
	 */
	public static void dropDBSending(Context context) {
		SQLiteDatabase db = openDBSending(context);
		if (isTableExists(db, TABLE_SENDING)) {
			final String CREATE_TABLE_MSGS = "DROP TABLE `" + TABLE_SENDING
					+ "`;";
			db.execSQL(CREATE_TABLE_MSGS);
		}
		db.close();
	}

	// -----------------------------------------------------------------

	/**
	 * Drop the unique db sent.
	 * 
	 * @param context
	 *            the context
	 */
	public static void dropDBSent(Context context) {
		SQLiteDatabase db = openDBSending(context);
		if (isTableExists(db, TABLE_SENT)) {
			final String CREATE_TABLE_MSGS = "DROP TABLE `" + TABLE_SENT + "`;";
			db.execSQL(CREATE_TABLE_MSGS);
		}
		db.close();
	}

	// -----------------------------------------------------------------

	/**
	 * Initialize the message table in the user specific uid db.
	 * 
	 * @param context
	 *            the context
	 * @param db
	 *            the db
	 */
	public static void initializeDB(Context context, SQLiteDatabase db) {
		db.setVersion(1);
		db.setLocale(Locale.getDefault());

		// final String CREATE_TABLE_MSGS = "CREATE TABLE IF NOT EXISTS  `"
		// + TABLE_MESSAGES
		// + "` ("
		// +
		// "`localid` INTEGER PRIMARY KEY, `mid` INTEGER , `fromuid` INTEGER , "
		// + "`touid` INTEGER , `text` VARCHAR( 2000 ) , "
		// + "`created` VARCHAR( 50 ), "
		// +
		// "`sent` VARCHAR( 50 ) , `received` VARCHAR( 50 ), `read` VARCHAR( 50 ), `revoked` VARCHAR( 50 ), `encrypted` VARCHAR( 1 ), `transport` VARCHAR( 1 ), `system` VARCHAR( 1 ) , `part` INTEGER DEFAULT "
		// + DB.DEFAULT_MESSAGEPART
		// +
		// " , `parts` INTEGER DEFAULT 1, `multipartid` VARCHAR( 5 ) DEFAULT ``);";

		final String CREATE_TABLE_MSGS = "CREATE TABLE IF NOT EXISTS  `"
				+ TABLE_MESSAGES
				+ "` ("
				+ "`localid` INTEGER PRIMARY KEY, `mid` INTEGER , `fromuid` INTEGER , "
				+ "`touid` INTEGER , `text` VARCHAR( 2000 ) , "
				+ "`created` VARCHAR( 50 ), "
				+ "`sent` VARCHAR( 50 ) , `received` VARCHAR( 50 ), `read` VARCHAR( 50 ), `revoked` VARCHAR( 50 ), `encrypted` VARCHAR( 1 ), `transport` VARCHAR( 1 ), `system` VARCHAR( 1 ) , `part` INTEGER DEFAULT "
				+ DB.DEFAULT_MESSAGEPART
				+ " , `parts` INTEGER DEFAULT 1, `multipartid` VARCHAR( 5 ) DEFAULT ``);";

		db.execSQL(CREATE_TABLE_MSGS);
	}

	// -----------------------------------------------------------------

	/**
	 * Initialize the sending table in the unique db sending.
	 * 
	 * @param context
	 *            the context
	 * @param db
	 *            the db
	 */
	public static void initializeDBSending(Context context, SQLiteDatabase db) {
		db.setVersion(1);
		db.setLocale(Locale.getDefault());

		final String CREATE_TABLE_MSGS = "CREATE TABLE IF NOT EXISTS  `"
				+ TABLE_SENDING
				+ "` ("
				+ "`sendingid` INTEGER PRIMARY KEY, `localid` INTEGER, `fromuid` INTEGER , "
				+ "`touid` INTEGER , `text` VARCHAR( 2000 ) , "
				+ "`created` VARCHAR( 50 ), "
				+ "`sent` VARCHAR( 50 ), `encrypted` VARCHAR( 1 ), `transport` VARCHAR( 1 ), `system` VARCHAR( 1 ), `prio` INTEGER, `smsfailcnt` INTEGER, `tries` INTEGER , `lasttry` VARCHAR( 50 ), `part` INTEGER  DEFAULT 0, `parts` INTEGER DEFAULT 1 , `multipartid` VARCHAR( 5 ) DEFAULT ``,  `serverId` INTEGER DEFAULT -1  );";
		db.execSQL(CREATE_TABLE_MSGS);
	}

	// -----------------------------------------------------------------

	/**
	 * Initialize the unique sent table in the unique db sent.
	 * 
	 * @param context
	 *            the context
	 * @param db
	 *            the db
	 */
	public static void initializeDBSent(Context context, SQLiteDatabase db) {
		db.setVersion(1);
		db.setLocale(Locale.getDefault());

		final String CREATE_TABLE_MSGS = "CREATE TABLE IF NOT EXISTS  `"
				+ TABLE_SENT
				+ "` ("
				+ "`mid` INTEGER PRIMARY KEY, `hostuid` INTEGER, `ts` VARCHAR( 50 ));";
		db.execSQL(CREATE_TABLE_MSGS);
	}

	// -----------------------------------------------------------------

	/**
	 * Ask to rebuild db for a specific user.
	 * 
	 * @param context
	 *            the context
	 * @param uid
	 *            the uid
	 */
	private static void askToRebuildDB(final Context context, final int uid) {
		try {
			final Handler mUIHandler = new Handler(Looper.getMainLooper());
			mUIHandler.post(new Thread() {
				@Override
				public void run() {
					super.run();
					final Handler handler = new Handler();
					handler.postDelayed(new Runnable() {
						public void run() {
							final String titleMessage = "Database Corrupt";
							final String textMessage = "You database seems to be corrupted.\n\nYou "
									+ "may want to try closing the application, restart you device and "
									+ "try it again.\n\nIf this does not help you may try to repair the DB"
									+ " if you just updated from an earlier app version.\n\nIf this still "
									+ "does not help your messages may be lost and the only fix then is to"
									+ " erase and recreate the database.\n\n"
									+ "Do you want to erase all messages and recreate the database?";
							new MessageAlertDialog(
									context,
									titleMessage,
									textMessage,
									"Erase All",
									"Repair",
									"Cancel",
									new MessageAlertDialog.OnSelectionListener() {
										public void selected(int button,
												boolean cancel) {
											if (!cancel) {
												if (button == 0) {
													// Delete
													dropDB(context, uid);
													SQLiteDatabase db = openDB(
															context, uid);
													initializeDB(context, db);
													db.close();
												}
												if (button == 1) {
													tryRepairDB(context, uid);
												}
											}
										}
									}).show();
						}
					}, 200);
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	// ------------------------------------------------------------------------

	/**
	 * This method will try to repair a DB from the last version to this current
	 * version. The difference was that we added 3 columns for supporting
	 * multipart messages.
	 * 
	 * @param context
	 *            the context
	 * @param uid
	 *            the uid
	 */
	public static void tryRepairDB(Context context, int uid) {
		// try repair ---> end exit application afterwards
		try {
			// SQLiteDatabase db = openDBSending(context);
			// if (isTableExists(db, TABLE_SENDING)) {
			// String REPAIR_QUERY = "ALTER TABLE `" + TABLE_SENDING +
			// "` ADD COLUMN `part` INTEGER;";
			// db.execSQL(REPAIR_QUERY);
			// REPAIR_QUERY = "ALTER TABLE `" + TABLE_SENDING +
			// "` ADD COLUMN `parts` INTEGER;";
			// db.execSQL(REPAIR_QUERY);
			// REPAIR_QUERY = "ALTER TABLE `" + TABLE_SENDING +
			// "` ADD COLUMN `multipartid` VARCHAR( 5 );";
			// db.execSQL(REPAIR_QUERY);
			// }
			// db.close();
			SQLiteDatabase db = openDB(context, uid);
			if (isTableExists(db, TABLE_MESSAGES)) {
				String REPAIR_QUERY = "ALTER TABLE `" + TABLE_MESSAGES
						+ "` ADD COLUMN `part` INTEGER DEFAULT 0;";
				db.execSQL(REPAIR_QUERY);
				REPAIR_QUERY = "ALTER TABLE `" + TABLE_MESSAGES
						+ "` ADD COLUMN `parts` INTEGER DEFAULT 1;";
				db.execSQL(REPAIR_QUERY);
				REPAIR_QUERY = "ALTER TABLE `" + TABLE_MESSAGES
						+ "` ADD COLUMN `multipartid` VARCHAR( 5 ) DEFAULT ``;";
				db.execSQL(REPAIR_QUERY);
			}
			db.close();
			Main.exitApplication(context);
		} catch (Exception e) {
			// silent exception if repair fails...
		}
	}

	// ------------------------------------------------------------------------

	/**
	 * Rebuild db sending.
	 * 
	 * @param context
	 *            the context
	 */
	public static void rebuildDBSending(final Context context) {
		// Delete
		dropDBSending(context);
		SQLiteDatabase db = openDBSending(context);
		initializeDBSending(context, db);
		db.close();
	}

	// -----------------------------------------------------------------

	/**
	 * Rebuild db sent.
	 * 
	 * @param context
	 *            the context
	 */
	// ------------------------------------------------------------------------
	private static void rebuildDBSent(final Context context) {
		// Delete
		dropDBSent(context);
		SQLiteDatabase db = openDBSent(context);
		initializeDBSent(context, db);
		db.close();
	}

	// ------------------------------------------------------------------------

	/**
	 * Ensure db initialized. This is called once on startup and after adding
	 * new users (before inserting new messages)
	 * 
	 * @param context
	 *            the context
	 * @param uidList
	 *            the uid list
	 */
	public static void ensureDBInitialized(Context context,
			List<Integer> uidList) {
		try {
			// The following will not harm existing tables!
			for (int uid : uidList) {
				SQLiteDatabase db = openDB(context, uid);
				initializeDB(context, db);
				db.close();
			}
			SQLiteDatabase db = openDBSending(context);
			initializeDBSending(context, db);
			db.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	// ------------------------------------------------------------------------

	/**
	 * Revoked from sending. If a message is possibly not yet sent, we can try
	 * to revoke it BEFORE it got sent!
	 * 
	 * @param context
	 *            the context
	 * @param localId
	 *            the local id
	 * @return true, if successful
	 */
	public static boolean revokeFromSending(Context context, int localId) {
		SQLiteDatabase db = null;
		boolean success = false;
		try {
			db = openDBSending(context);
			if (db.delete(TABLE_SENDING, "`localid` = " + localId, null) > 0) {
				success = true;
			}
			db.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return success;
	}

	// ------------------------------------------------------------------------

	/**
	 * Removes the old mappings.
	 * 
	 * @param context
	 *            the context
	 */
	public static void removeOldMappings(Context context) {
		String barrierTS = (DB.getTimestamp() - Setup.TIMEOUT_FOR_RECEIVEANDREAD_CONFIRMATIONS)
				+ "";
		SQLiteDatabase db = null;
		try {
			db = openDBSent(context);
			db.delete(TABLE_SENT, "`ts` < " + barrierTS, null);
			db.close();
		} catch (Exception e) {
			if (db != null) {
				db.close();
			}
			rebuildDBSent(context);
			e.printStackTrace();
		}

	}

	// ------------------------------------------------------------------------

	/**
	 * Removes the all previous / older messages based for a user based on an
	 * mid. If negative or with * than interpret this as local id.
	 * 
	 * @param context
	 *            the context
	 * @param uid
	 *            the uid
	 * @param midString
	 *            the mid string
	 * @return the int
	 */
	public static int clearSelective(Context context, int uid, String midString) {
		midString = midString.trim();
		boolean isLocal = false;
		if (midString.startsWith("*") || midString.startsWith("-")) {
			isLocal = true;
			midString = midString.substring(1);
		}
		int messageId = Utility.parseInt(midString, -1);
		if (messageId == -1) {
			return -1;
		}

		String query = "`mid` <= " + messageId;
		if (isLocal) {
			query = "`localid` <= " + messageId;
		}

		int localId = messageId;
		if (!isLocal) {
			localId = DB.getHostLocalIdByMid(context, messageId, uid);
		}

		Log.d("communicator", "@@@@ SELECTIVE CLEAR : localId=" + localId
				+ ", uid=" + uid + ", query=" + query);

		// First test if the local id belongs to the hostuid...
		ConversationItem item = getMessage(context, localId, uid,
				DB.DEFAULT_MESSAGEPART);
		if (item == null) {
			return -2;
		}

		int numCleard = 0;
		SQLiteDatabase db = null;
		try {
			db = openDB(context, uid);
			numCleard = db.delete(TABLE_MESSAGES, query, null);
			db.close();
		} catch (Exception e) {
			if (db != null) {
				db.close();
			}
			rebuildDBSent(context);
			e.printStackTrace();
		}
		return numCleard;
	}

	// ------------------------------------------------------------------------

	/**
	 * Removes the mapping by mid.
	 * 
	 * @param context
	 *            the context
	 * @param mid
	 *            the mid
	 */
	public static void removeMappingByMid(Context context, int mid) {
		Log.d("communicator", " UPDATE MESSAGE REMOVE MAPPING: mid=" + mid);
		SQLiteDatabase db = openDBSent(context);
		db.delete(TABLE_SENT, "`mid` = " + mid, null);
		db.close();
	}

	// ------------------------------------------------------------------------

	/**
	 * Removes the mapping by host uid.
	 * 
	 * @param context
	 *            the context
	 * @param hostUid
	 *            the host uid
	 */
	public static void removeMappingByHostUid(Context context, int hostUid) {
		SQLiteDatabase db = openDBSent(context);
		db.delete(TABLE_SENT, "`hostuid` = " + hostUid, null);
		db.close();
	}

	// --------------------------------------------

	/**
	 * Gets the number of messages to send.
	 * 
	 * @param context
	 *            the context
	 * @param transport
	 *            the transport
	 * @return the number of messages to send
	 */
	public static int getNumberOfMessagesToSend(Context context, int transport) {
		cleanupDBSending(context);
		SQLiteDatabase db = null;
		Cursor cursor = null;
		int numberOfMessages = 0;
		try {
			db = openDBSending(context);

			String QUERY = "SELECT `localid` FROM `" + TABLE_SENDING
					+ "` WHERE `transport` = " + transport;

			cursor = db.rawQuery(QUERY, null);
			if (cursor != null && cursor.moveToFirst()) {
				numberOfMessages = cursor.getCount();
				Log.d("communicator", "#### AUTO-getNumberOfMessagesToSend "
						+ numberOfMessages);
				// DB.printDB(context);
				cursor.close();
			}
			db.close();
		} catch (Exception e) {
			if (cursor != null) {
				cursor.close();
			}
			if (db != null) {
				db.close();
			}
			e.printStackTrace();
		}
		return numberOfMessages;
	}

	// --------------------------------------------

	/**
	 * Gets the position of an unsent message in the sending queue.
	 * 
	 * @param context
	 *            the context
	 * @param transport
	 *            the transport
	 * @param localId
	 *            the local id
	 * @return the number of messages to send
	 */
	public static int getPositionInSendingQueue(Context context, int transport,
			int localId, int serverId) {
		int positionOfItem = -1;
		cleanupDBSending(context);
		SQLiteDatabase db = null;
		Cursor cursor = null;
		try {
			db = openDBSending(context);

			String QUERY = "SELECT `localid` FROM `" + TABLE_SENDING
					+ "` WHERE `transport` = " + transport
					+ " AND `serverId` = " + serverId
					+ " ORDER BY `prio` DESC, `created` ASC";

			cursor = db.rawQuery(QUERY, null);
			if (cursor != null && cursor.moveToFirst()) {
				int position = 1;
				Log.d("communicator", "getCount = " + cursor.getCount());
				for (int c = 0; c < cursor.getCount(); c++) {
					int vglLocalId = Utility.parseInt(cursor.getString(0), -1);
					if (vglLocalId == localId) {
						positionOfItem = position;
						break;
					} else {
						position++;
						cursor.moveToNext();
					}
				}
				cursor.close();
			}
			db.close();
		} catch (Exception e) {
			if (cursor != null) {
				cursor.close();
			}
			if (db != null) {
				db.close();
			}
			e.printStackTrace();
		}
		return positionOfItem;
	}

	// --------------------------------------------

	/**
	 * Gets the host uid for mid.
	 * 
	 * @param context
	 *            the context
	 * @param mid
	 *            the mid
	 * @return the host uid for mid
	 */
	public static int getHostUidForMid(Context context, int mid) {
		if (mid == -1) {
			return -1;
		}
		SQLiteDatabase db = openDBSent(context);
		int returnUid = -1;
		String QUERY = "SELECT `hostuid` FROM `" + TABLE_SENT
				+ "` WHERE `mid` = " + mid;

		Cursor cursor = db.rawQuery(QUERY, null);
		if (cursor != null && cursor.moveToFirst()) {
			if (cursor.getCount() > 0) {
				returnUid = Utility.parseInt(cursor.getString(0), -1);
			}
			cursor.close();
		}
		db.close();
		if (returnUid == -1) {
			Log.d("communicator",
					" UPDATE MESSAGE getHostUidForMid() returnUid still -1, now searching in all user's DBs for mid="
							+ mid);

			for (int uid : Main.loadUIDList(context)) {
				db = openDB(context, uid);
				QUERY = "SELECT `mid` FROM `" + TABLE_MESSAGES
						+ "` WHERE `mid` = " + mid;
				cursor = db.rawQuery(QUERY, null);
				if (cursor != null && cursor.moveToFirst()) {
					if (cursor.getCount() > 0) {
						returnUid = uid;
					}
					cursor.close();
				}
				db.close();
				if (returnUid != -1) {
					// FOUND
					break;
				}
			}

		}

		if (returnUid == -1) {
			Log.d("communicator",
					" UPDATE MESSAGE getHostUidForMid() NO WHERE found mid="
							+ mid);
		} else {
			Log.d("communicator",
					" UPDATE MESSAGE getHostUidForMid() found mid=" + mid
							+ " at user uid=" + returnUid);
		}
		return returnUid;
	}

	// ------------------------------------------------------------------------

	/**
	 * Adds the mapping. priority 0 == read confirmation, 1 == normal messages,
	 * 2 == keys.
	 * 
	 * @param context
	 *            the context
	 * @param mid
	 *            the mid
	 * @param hostUid
	 *            the host uid
	 * @return true, if successful
	 */
	public static boolean addMapping(Context context, int mid, int hostUid) {
		boolean success = false;
		ContentValues values = new ContentValues();
		values.put("mid", mid);
		values.put("hostuid", hostUid);
		values.put("ts", DB.getTimestampString());
		SQLiteDatabase db = null;
		try {
			db = openDBSent(context);
			long rowId = db.insertOrThrow(DB.TABLE_SENT, null, values);
			if (rowId > -1) {
				success = true;
			}
			db.close();
		} catch (Exception e) {
			if (db != null) {
				db.close();
			}
			rebuildDBSent(context);
			e.printStackTrace();
		}
		return success;
	}

	// ------------------------------------------------------------------------

	/**
	 * Removes the sent message.
	 * 
	 * @param context
	 *            the context
	 * @param localId
	 *            the local id
	 * @return true, if successful
	 */
	public static boolean removeSentMessageByLocalId(Context context,
			int localId) {
		SQLiteDatabase db = openDBSending(context);
		int i = db.delete(TABLE_SENDING, "`localid` = " + localId, null);
		db.close();
		return i > 0;
	}

	// ------------------------------------------------------------------------

	/**
	 * Removes the sent message.
	 * 
	 * @param context
	 *            the context
	 * @param sendingId
	 *            the sending id
	 * @return true, if successful
	 */
	public static boolean removeSentMessage(Context context, int sendingId) {
		SQLiteDatabase db = openDBSending(context);
		int i = db.delete(TABLE_SENDING, "`sendingid` = " + sendingId, null);
		db.close();
		return i > 0;
	}

	// ------------------------------------------------------------------------

	/**
	 * Adds the send message.
	 * 
	 * @param context
	 *            the context
	 * @param hostUid
	 *            the host uid
	 * @param text
	 *            the text
	 * @param encrypted
	 *            the encrypted
	 * @param transport
	 *            the transport
	 * @param system
	 *            the system
	 * @param priority
	 *            the priority
	 * @return true, if successful
	 */
	public static boolean addSendMessage(final Context context, int hostUid,
			String text, boolean encrypted, int transport, boolean system,
			int priority) {
		return addSendMessage(context, hostUid, text, encrypted, transport,
				system, priority, null);
	}

	// ------------------------------------------------------------------------

	/**
	 * The Class MultiPartInfo.
	 */
	public static class MultiPartInfo {

		/** The mulitpartid. */
		public String mulitpartid;

		/** The part. */
		public int part;

		/** The parts. */
		public int parts;

		/** The text. */
		public String text = "";
	}

	// ------------------------------------------------------------------------

	/**
	 * Gets the list of recent multipartids of completed combined multipart
	 * messages.
	 * 
	 * @param context
	 *            the context
	 * @param uid
	 *            the uid
	 * @return the recent multipart ids
	 */
	public static Set<String> getRecentMultipartIds(Context context, int uid) {
		Set<String> recentMultipartIds = new HashSet<String>();
		SQLiteDatabase db = null;
		Cursor cursor = null;
		try {
			db = openDB(context, uid);
			long ts = DB.getTimestamp() - MULTIPART_TIMEOUT;

			String QUERY = "SELECT `multipartid` FROM `" + TABLE_MESSAGES
					+ "` WHERE `part` = 0 AND `parts` = 1 AND `received` > "
					+ ts;

			cursor = db.rawQuery(QUERY, null);
			if (cursor != null && cursor.moveToFirst()) {

				// This will increase on receiving from 0 -> totalParts
				for (int c = 0; c < cursor.getCount(); c++) {
					String multipartid = cursor.getString(0);
					if (!multipartid.equals(DB.NO_MULTIPART_ID)) {
						recentMultipartIds.add(multipartid);
					}
					cursor.moveToNext();
				}
			}
			if (cursor != null) {
				cursor.close();
			}
			db.close();
		} catch (Exception e) {
			try {
				if (cursor != null) {
					cursor.close();
				}
			} catch (Exception e2) {
				e.printStackTrace();
			}
			try {
				if (db != null) {
					db.close();
				}
			} catch (Exception e2) {
				e.printStackTrace();
			}
			e.printStackTrace();
		}
		return recentMultipartIds;
	}

	// ------------------------------------------------------------------------

	/**
	 * Checks if a multipartid was recently already used and already combined.
	 * In this case we can claim the current SMS a duplicate and skip it.
	 * 
	 * @param context
	 *            the context
	 * @param uid
	 *            the uid
	 * @param multipartid
	 *            the multipartid
	 * @return true, if is multipart duplicate
	 */
	public static boolean isMultipartDuplicate(Context context, int uid,
			String multipartid) {
		Set<String> recentMultipartIds = getRecentMultipartIds(context, uid);
		return (recentMultipartIds.contains(multipartid));
	}

	// ------------------------------------------------------------------------

	/**
	 * Clean multipart duplicates that may arrive when the sending device or the
	 * network may have caused duplication of SMS which we do not want. We use
	 * this method after combining.
	 * 
	 * @param multipartid
	 *            the multipartid
	 */
	public static void cleanMultipartDuplicates(Context context, int uid) {
		// Remove all parts
		SQLiteDatabase db = null;
		try {
			db = openDB(context, uid);
			Set<String> recentMultipartIds = getRecentMultipartIds(context, uid);
			for (String multipartid : recentMultipartIds) {
				Log.d("communicator", "MULTIPART cleaning '" + multipartid
						+ "'");
			}
			db.close();
		} catch (Exception e) {
			try {
				if (db != null) {
					db.close();
				}
			} catch (Exception e2) {
				e.printStackTrace();
			}
			e.printStackTrace();
		}
	}

	// ------------------------------------------------------------------------

	/**
	 * Combine a multi part message iff all parts of the message have been
	 * received. If this is not yet the case then
	 * 
	 * @param multipartId
	 *            the multipart id
	 * @param uid
	 *            the uid
	 * @return true, if successful
	 */
	public static boolean combineMultiPartMessage(Context context,
			ConversationItem item) {
		String multipartId = item.multipartid;
		int uid = item.from;

		Set<Integer> receivedParts = getReceivedMultiparts(context,
				multipartId, uid);

		// Log.d("communicator",
		// "MULTIPART combineMultiPartMessage() receivedParts="
		// + receivedParts + ", item.parts - receivedParts - 1 = "
		// + (item.parts - receivedParts - 1) + ", from=" + uid);

		// Add the just received part to see if we have ALL parts (each just
		// ONCE!)
		receivedParts.add(item.part);

		if (item.parts - receivedParts.size() == 0) {
			// This is the last part, we can no go ahead and combine!

			String messageParts[] = new String[item.parts];
			// The currently received (LAST) part
			messageParts[item.part] = item.text;
			// AND all OTHER previously received parts...
			SQLiteDatabase db = null;
			Cursor cursor = null;
			try {
				db = openDB(context, uid);
				// 'AND `parts` = " + item.parts;' prevents combining again if
				// under any circumstance other duplicate parts
				// may arrive later... we will not touch the alrady combined
				// message because it has set parts to 1!
				String QUERY = "SELECT `text`, `part` FROM `" + TABLE_MESSAGES
						+ "` WHERE `multipartid` = '" + multipartId
						+ "' AND `parts` = " + item.parts;
				cursor = db.rawQuery(QUERY, null);
				if (cursor != null && cursor.moveToFirst()) {
					// This will increase on receiving from 0 -> totalParts
					for (int c = 0; c < cursor.getCount(); c++) {
						int part = Utility.parseInt(cursor.getString(1), -1);
						if (part != -1) {
							messageParts[part] = cursor.getString(0);
						}
						cursor.moveToNext();
					}
					cursor.close();
				}
				db.close();

				// Now update the item: ATTENTION, the parts are orderd the
				// other way round! KEEP THIS IN MIND!
				item.text = "";
				for (String messagePart : messageParts) {
					item.text = messagePart + item.text;
				}

				Log.d("communicator", "MULTIPART COMBINED TEXT item.text="
						+ item.text);

				// It is necessary to reset the part to 0, because only
				// part == 0 items will be displayed in conversations (and
				// already filtered when conversations are loaded)
				item.part = DB.DEFAULT_MESSAGEPART;
				item.parts = 1;
				// KEEP the item.multipartid because we need to update the UI
				// conversation list and this is the reference to it!

				// Clean other parts, this is the only call that MUST be made to
				// this method
				// even if there are NO duplictations sent. Further calls will
				// just eliminate
				// erroneously sent duplications.
				cleanMultipartDuplicates(context, uid);
			} catch (Exception e) {
				if (cursor != null) {
					cursor.close();
				}
				if (db != null) {
					db.close();
				}
				e.printStackTrace();
			}

			// Tell that we could combine!
			return true;
		}
		// We could not yet combine because parts are missin!
		return false;
	}

	// ------------------------------------------------------------------------

	/**
	 * Gets the multi part info or null if no multipart message.
	 * 
	 * @param text
	 *            the text
	 * @return the multi part info
	 */
	public static MultiPartInfo getMultiPartInfo(String text) {
		// Log.d("communicator",
		// "MULTIPART getMultiPartInfo() #1");
		if (!text.startsWith("M")) {
			return null;
		}
		// Log.d("communicator",
		// "MULTIPART getMultiPartInfo() #2 " + text.length());
		if (text.length() < 10) {
			return null;
		}
		String parts[] = text.split(":");
		// Log.d("communicator",
		// "MULTIPART getMultiPartInfo() #3 " + parts.length);
		if (parts.length < 3) {
			return null;
		}
		// Log.d("communicator",
		// "MULTIPART getMultiPartInfo() #4");
		try {
			// At this point we a pretty confident that this is a multipart
			// message. Try to parse it!
			MultiPartInfo multiPartInfo = new MultiPartInfo();
			multiPartInfo.mulitpartid = parts[0].substring(1, 6);
			multiPartInfo.part = Utility.parseInt(
					parts[0].substring(6, parts[0].length()), -1);
			if (multiPartInfo.part == -1) {
				// Log.d("communicator",
				// "MULTIPART getMultiPartInfo() multiPartInfo.part == -1");
				return null;
			}
			multiPartInfo.parts = Utility.parseInt(parts[1], -1);
			if (multiPartInfo.parts == -1) {
				// Log.d("communicator",
				// "MULTIPART getMultiPartInfo() multiPartInfo.parts == -1");
				return null;
			}
			for (int p = 2; p < parts.length; p++) {
				multiPartInfo.text += parts[p];
			}
			return multiPartInfo;
		} catch (Exception e) {
			// Log.d("communicator",
			// "MULTIPART getMultiPartInfo() #ERROR");
			// If parsing goes wrong, return the text
			return null;
		}
	}

	// ------------------------------------------------------------------------

	/**
	 * Adds the send message. priority 0 == read confirmation, 1 == normal
	 * messages, 2 == keys.
	 * 
	 * @param context
	 *            the context
	 * @param hostUid
	 *            the host uid
	 * @param text
	 *            the text
	 * @param encrypted
	 *            the encrypted
	 * @param transport
	 *            the transport
	 * @param system
	 *            the system
	 * @param priority
	 *            the priority
	 * @param item
	 *            the item
	 * @return true, if successful
	 */
	public static boolean addSendMessage(final Context context, int hostUid,
			String text, boolean encrypted, int transport, boolean system,
			int priority, ConversationItem item) {

		if (!system) {
			// Do this only for part = 0 (the last part of a multi part message)
			Main.updateLastMessage(context, hostUid, text, DB.getTimestamp());
		}

		// AUTOMATED MULTIPART SPLITTING - NOT FOR SYSTEM SMS MESSAGES NOT FOR
		// EXTERNAL USER! //
		if (transport == DB.TRANSPORT_SMS && hostUid >= 0
				&& text.length() > Setup.MULTIPART_MESSAGELIMIT && !system) {
			// SPLIT UP
			double textLen = (double) text.length();
			int parts = (int) Math.ceil(textLen
					/ (double) Setup.MULTIPART_MESSAGELIMIT);
			String multipartid = Utility.md5(DB.getTimestampString() + textLen)
					.substring(0, 5);
			boolean returnOk = true;

			for (int part = 0; part < parts; part++) {
				int start = part * Setup.MULTIPART_MESSAGELIMIT;
				int end = (part + 1) * Setup.MULTIPART_MESSAGELIMIT;
				if (end > text.length()) {
					end = text.length();
				}
				// The part number is chosen such that the first part gets the
				// highest number and the last one 0
				// so that the last one (DEFAULT PART) is updated only if ALL
				// parts have been sent/received/read!
				int partNumber = parts - part - 1;
				String messagePart = text.substring(start, end);
				String messagePartText = "M" + multipartid + partNumber + ":"
						+ parts + ":" + messagePart;
				returnOk = returnOk
						&& addSendMultipartMessage(context, hostUid,
								messagePartText, text, encrypted, transport,
								system, priority, item, partNumber, parts,
								multipartid);
			}
			return returnOk;
		} else {
			return addSendMultipartMessage(context, hostUid, text, text,
					encrypted, transport, system, priority, item,
					DB.DEFAULT_MESSAGEPART, 1, NO_MULTIPART_ID);
		}

	}

	// ------------------------------------------------------------------------

	/**
	 * Adds the send multipart message.
	 * 
	 * @param context
	 *            the context
	 * @param hostUid
	 *            the host uid
	 * @param text
	 *            the text
	 * @param encrypted
	 *            the encrypted
	 * @param transport
	 *            the transport
	 * @param system
	 *            the system
	 * @param priority
	 *            the priority
	 * @param item
	 *            the item
	 * @param part
	 *            the part
	 * @param parts
	 *            the parts
	 * @param multipartid
	 *            the multipartid
	 * @return true, if successful
	 */
	private static boolean addSendMultipartMessage(final Context context,
			int hostUid, String partText, String totalText, boolean encrypted,
			int transport, boolean system, int priority, ConversationItem item,
			int part, int parts, String multipartid) {

		Log.d("communicator", "MULTIPART addSendMultipartMessage() part="
				+ part + ", parts=" + parts + ", multipartid=" + multipartid);

		Log.d("communicator", "#### KEY NEW KEY #6.1");

		String created = DB.getTimestampString();

		Log.d("communicator", "#### KEY NEW KEY #6.2");

		if (priority == DB.PRIORITY_KEY && item != null && item.created > 0) {
			Log.d("communicator", "#### KEY NEW KEY #6.3");
			// We want to have KEYs ordered BEFORE the message they are related
			// because
			created = (item.created - (2 * 2000)) + ""; // deduct 2 seconds
		}

		Log.d("communicator", "#### KEY NEW KEY #6.4");

		String messageTextToShow = totalText;
		boolean systemToShow = system;
		if (priority == DB.PRIORITY_KEY) {
			Log.d("communicator", "#### KEY NEW KEY #6.5");
			// Exchange key message text (make it a little bit more readable...
			// we do not need the
			// original key message text locally, we already stored to AES key
			// in our device!
			String keyhash = Setup.getAESKeyHash(context, hostUid);
			String keyText = "[ session key " + keyhash + " sent ]";
			messageTextToShow = keyText;
			// Attention: We further set system message to FALSE to be be able
			// to
			// read it!
			systemToShow = false;
		}

		Log.d("communicator", "#### KEY NEW KEY #6.6");

		// Add the message or real key here
		if (part != DB.DEFAULT_MESSAGEPART) {
			// The default message part get the full text for the local DB, the
			// others get "", the partText is only relevant for SENDING table
			messageTextToShow = "";
		}

		final int myUid = DB.myUid();
		int localId = addMessage(context, myUid, hostUid, messageTextToShow,
				created, null, null, null, null, encrypted, transport,
				systemToShow, part, parts, multipartid);

		Log.d("communicator", "#### KEY NEW KEY #6.7");

		if (priority == DB.PRIORITY_KEY) {
			if (Conversation.isAlive()) {
				try {
					final Handler mUIHandler = new Handler(
							Looper.getMainLooper());
					mUIHandler.post(new Thread() {
						@Override
						public void run() {
							super.run();
							Conversation.getInstance().updateConversationlist(
									context);
						}
					});
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		Log.d("communicator", "#### KEY NEW KEY #6.8");

		boolean insertInSending = false;
		if (localId > -1) {

			Log.d("communicator", "#### KEY NEW KEY #6.9");

			ContentValues values = new ContentValues();
			values.put("localid", localId);
			values.put("fromuid", myUid);
			values.put("touid", hostUid);
			values.put("transport", transport);
			values.put("text", partText);
			if (encrypted) {
				values.put("encrypted", "E");
			} else {
				values.put("encrypted", "U");
			}
			values.put("created", created);
			if (system) {
				values.put("system", "1");
			} else {
				values.put("system", "0");
			}
			values.put("prio", priority);
			values.put("smsfailcnt", 0); // Used for SMS only
			values.put("tries", 0); // Incremented at each try
			values.put("part", part);
			values.put("parts", parts);
			values.put("multipartid", multipartid);
			// Put it as outgoing for THIS server of the particular receipient
			// we will ROUND ROBIN thru all possible servers on sending
			// in case single ones fail
			int serverId = Setup.getServerId(context, hostUid);
			values.put("serverId", serverId);
			SQLiteDatabase db = null;
			try {
				db = openDBSending(context);
				long rowId = db.insertOrThrow(DB.TABLE_SENDING, null, values);
				if (rowId > -1) {
					// Valid insert
					insertInSending = true;
				}
				db.close();
			} catch (Exception e) {
				if (db != null) {
					db.close();
				}
				rebuildDBSending(context);
				e.printStackTrace();
			}
		}
		Log.d("communicator", "#### KEY NEW KEY #6.10");

		return insertInSending;
	}

	// -----------------------------------------------------------------

	/**
	 * Gets the multipart id from sending queue table by localId of the first
	 * part.
	 * 
	 * @param context
	 *            the context
	 * @param localId
	 *            the local id
	 * @return the multipart id
	 */
	public static String getMultipartIdSending(Context context, int localId) {
		SQLiteDatabase db = null;
		Cursor cursor = null;
		int numberOfMessages = 0;
		try {
			db = openDBSending(context);

			String QUERY = "SELECT `multipartid` FROM `" + TABLE_SENDING
					+ "` WHERE `localid` = " + localId;

			cursor = db.rawQuery(QUERY, null);
			if (cursor != null && cursor.moveToFirst()) {
				numberOfMessages = cursor.getCount();
				if (numberOfMessages > 0) {
					return cursor.getString(0);
				}
				cursor.close();
			}
			db.close();
		} catch (Exception e) {
			if (cursor != null) {
				cursor.close();
			}
			if (db != null) {
				db.close();
			}
			e.printStackTrace();
		}
		// No multipart id
		return NO_MULTIPART_ID;
	}

	// -----------------------------------------------------------------

	/**
	 * Gets the multipart id from messages table by localId of the first part.
	 * 
	 * @param context
	 *            the context
	 * @param localId
	 *            the local id
	 * @param hostUid
	 *            the host uid
	 * @return the multipart id
	 */
	public static String getMultipartIdReceived(Context context, int localId,
			int hostUid) {
		SQLiteDatabase db = null;
		Cursor cursor = null;
		int numberOfMessages = 0;
		try {
			db = openDB(context, hostUid);

			String QUERY = "SELECT `multipartid` FROM `" + TABLE_MESSAGES
					+ "` WHERE `localid` = " + localId;

			cursor = db.rawQuery(QUERY, null);
			if (cursor != null && cursor.moveToFirst()) {
				numberOfMessages = cursor.getCount();
				if (numberOfMessages > 0) {
					return cursor.getString(0);
				}
				cursor.close();
			}
			db.close();
		} catch (Exception e) {
			if (cursor != null) {
				cursor.close();
			}
			if (db != null) {
				db.close();
			}
			e.printStackTrace();
		}
		// No multipart id
		return NO_MULTIPART_ID;
	}

	// -----------------------------------------------------------------

	// public static int getPercentReceivedComplete(Context context, int
	// localId,
	// int senderUid) {
	// String multipartId = getMultipartIdReceived(context, localId, senderUid);
	// return getPercentReceivedComplete(context, multipartId, senderUid);
	// }

	// -----------------------------------------------------------------

	/**
	 * Get the number of received multi part messages. If no multi part message
	 * it returns the empty list.
	 * 
	 * @param context
	 *            the context
	 * @param multipartId
	 *            the multipart id
	 * @param senderUid
	 *            the sender uid
	 * @return the int
	 */
	public static Set<Integer> getReceivedMultiparts(Context context,
			String multipartId, int senderUid) {
		if (multipartId != null && !multipartId.equals(NO_MULTIPART_ID)) {
			// Okay we have a multi part id, so now count the other parts that
			// we have with the same ID
			SQLiteDatabase db = null;
			Cursor cursor = null;
			Set<Integer> haveConsidered = new HashSet<Integer>();
			try {
				db = openDB(context, senderUid);

				String QUERY = "SELECT `part` FROM `" + TABLE_MESSAGES
						+ "` WHERE `multipartid` = '" + multipartId + "'";

				cursor = db.rawQuery(QUERY, null);

				if (cursor != null && cursor.moveToFirst()) {
					// This will eliminate duplicates (which we of course still
					// do not want...)

					for (int c = 0; c < cursor.getCount(); c++) {
						int part = Utility.parseInt(cursor.getString(0), -1);
						if (part > -1) {
							haveConsidered.add(part);
						}
						cursor.moveToNext();
					}

					// This will increase on receiving from 0 -> totalParts
					// numReceived = haveConsidered.size();
					cursor.close();
				}
				db.close();
			} catch (Exception e) {
				if (cursor != null) {
					cursor.close();
				}
				if (db != null) {
					db.close();
				}
				e.printStackTrace();
			}
			return haveConsidered;
		} else {
			return new HashSet<Integer>();
		}
	}

	// -----------------------------------------------------------------

	/**
	 * Percent complete return 100 for non-multipart messages, return percent
	 * for multi part messages.
	 */
	public static int getPercentReceivedComplete(Context context,
			String multipartId, int senderUid, int totalParts) {
		if (!multipartId.equals(NO_MULTIPART_ID)) {
			// Okay we have a multi part id, so now count the other parts that
			// we have with the same ID
			int numReceived = getReceivedMultiparts(context, multipartId,
					senderUid).size();
			// int totalParts = Utility.parseInt(cursor.getString(0), 1);
			int percent = Math.max((numReceived * 100) / totalParts, 0);
			return percent;
		} else {
			// We do not have a multi part id, so retrun 100%
			return 100;
		}
	}

	// -----------------------------------------------------------------

	// public static int getPercentSentComplete(Context context, int localId) {
	// String multipartId = getMultipartIdSending(context, localId);
	// return getPercentSentComplete(context, multipartId);
	// }

	// -----------------------------------------------------------------

	/**
	 * Percent complete return 100 for non-multipart messages, return percent
	 * for multi part messages.
	 * 
	 * @param context
	 *            the context
	 * @param multipartId
	 *            the multipart id
	 * @return the int
	 */
	public static int getPercentSentComplete(Context context, String multipartId) {
		if (!multipartId.equals(NO_MULTIPART_ID)) {
			// Ok we have a multipart id, now count in the sending table how
			// much we have sent yet
			SQLiteDatabase db = null;
			Cursor cursor = null;
			int percent = 0;
			try {
				db = openDBSending(context);

				String QUERY = "SELECT `parts` FROM `" + TABLE_SENDING
						+ "` WHERE `multipartid` = '" + multipartId + "'";

				cursor = db.rawQuery(QUERY, null);
				if (cursor != null && cursor.moveToFirst()) {
					// This will decrease on sending from totalParts -> 0
					int numnotyetsent = cursor.getCount();
					int totalParts = Utility.parseInt(cursor.getString(0), 1);
					int numsent = totalParts - numnotyetsent;
					percent = Math.max((numsent * 100) / totalParts, 0);
					Log.d("communicator", "MULTIPART numnotyetsent="
							+ numnotyetsent + ", totalParts=" + totalParts
							+ ", numsent=" + numsent + ", percent=" + percent);

					cursor.close();
				}
				db.close();
			} catch (Exception e) {
				if (cursor != null) {
					cursor.close();
				}
				if (db != null) {
					db.close();
				}
				e.printStackTrace();
			}
			return percent;
		} else {
			// Ok we do not have a multipart id, return 100%
			return 100;
		}
	}

	// -----------------------------------------------------------------

	/**
	 * Adds the message. Returns the localId of the inserted message on success
	 * or -1 if failed.
	 * 
	 * @param context
	 *            the context
	 * @param from
	 *            the from
	 * @param to
	 *            the to
	 * @param text
	 *            the text
	 * @param created
	 *            the created
	 * @param sent
	 *            the sent
	 * @param received
	 *            the received
	 * @param read
	 *            the read
	 * @param revoke
	 *            the revoke
	 * @param encrypted
	 *            the encrypted
	 * @param transport
	 *            the transport
	 * @param system
	 *            the system
	 * @param part
	 *            the part
	 * @param parts
	 *            the parts
	 * @param multipartid
	 *            the multipartid
	 * @return the int
	 */
	public static int addMessage(Context context, int from, int to,
			String text, String created, String sent, String received,
			String read, String revoke, boolean encrypted, int transport,
			boolean system, int part, int parts, String multipartid) {
		int uid = from;
		if (to != DB.myUid()) {
			uid = to;
		}
		int localId = -1;
		ContentValues values = new ContentValues();
		values.put("fromuid", from);
		values.put("touid", to);
		values.put("transport", transport);
		values.put("text", text);
		if (encrypted) {
			values.put("encrypted", "E");
		} else {
			values.put("encrypted", "U");
		}
		if (created != null) {
			values.put("created", created);
		} else {
			values.put("created", "");
		}
		if (sent != null) {
			values.put("sent", sent);
		} else {
			values.put("sent", "");
		}
		if (received != null) {
			values.put("received", received);
		} else {
			values.put("received", "");
		}
		if (read != null) {
			values.put("read", read);
		} else {
			values.put("read", "");
		}
		if (revoke != null) {
			values.put("revoked", revoke);
		} else {
			values.put("revoked", "");
		}
		if (system) {
			values.put("system", "1");
		} else {
			values.put("system", "0");
		}
		values.put("part", part);
		values.put("parts", parts);
		values.put("multipartid", multipartid);
		try {
			SQLiteDatabase db = openDB(context, uid);
			long rowId = db.insert(DB.TABLE_MESSAGES, null, values);
			if (rowId > -1) {
				// Valid insert
				String QUERY = "SELECT `localid`  FROM `" + TABLE_MESSAGES
						+ "` WHERE ROWID = " + rowId;
				// Log.d("communicator", "addMessage() QUERY = " + QUERY);
				Cursor cursor = db.rawQuery(QUERY, null);
				if (cursor != null && cursor.moveToFirst()) {
					localId = Utility.parseInt(cursor.getString(0), -1);
				}
				cursor.close();
			}
			db.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return localId;
	}

	// -----------------------------------------------------------------

	/**
	 * Cleanup db sending.
	 * 
	 * @param context
	 *            the context
	 */
	public static void cleanupDBSending(Context context) {
		SQLiteDatabase db = openDBSending(context);
		db.delete(
				TABLE_SENDING,
				"`fromuid` == -1 OR `touid` = -1 OR  `text` = '' OR `created` = '-1'",
				null);
		db.close();
	}

	// -----------------------------------------------------------------

	/**
	 * Cleanup db.
	 * 
	 * @param context
	 *            the context
	 * @param uid
	 *            the uid
	 */
	public static void cleanupDB(Context context, int uid) {
		SQLiteDatabase db = openDB(context, uid);
		db.delete(
				TABLE_MESSAGES,
				"`fromuid` == -1 OR `touid` = -1 OR  `text` = '' OR `created` = '-1'",
				null);
		db.close();
	}

	// -----------------------------------------------------------------

	/**
	 * Prints the db.
	 * 
	 * @param context
	 *            the context
	 * @param uid
	 *            the uid
	 */
	public static void printDB(Context context, int uid) {
		SQLiteDatabase db = openDB(context, uid);

		String QUERY = "SELECT *  FROM `" + TABLE_MESSAGES
				+ "` ORDER BY `localid`";
		Log.d("communicator", "DBTABLE QUERY = " + QUERY);
		Cursor cursor = db.rawQuery(QUERY, null);
		if (cursor != null && cursor.moveToFirst()) {
			Log.d("communicator", "getCount = " + cursor.getCount());
			for (int c = 0; c < cursor.getCount(); c++) {

				String entry = "";
				int columns = cursor.getColumnCount();
				for (int cc = 0; cc < columns; cc++) {
					String name = cursor.getColumnName(cc);
					String val = cursor.getString(cc);
					if (val != null) {
						val = val.replace("\n", "").replace("\r", "");
					}

					entry += name + "=" + val + ", ";
				}
				Log.d("communicator", "DBTABLE [" + uid + "] " + entry);
				cursor.moveToNext();
			}
			cursor.close();
		}
		db.close();
	}

	// -----------------------------------------------------------------

	/**
	 * Prints the db sending.
	 * 
	 * @param context
	 *            the context
	 * @return the string
	 */
	public static String printDBSending(Context context) {
		SQLiteDatabase db = openDBSending(context);
		String returnString = "DBSENDING:\n";

		String QUERY = "SELECT *  FROM `" + TABLE_SENDING
				+ "` ORDER BY `localid`";
		Log.d("communicator", "DBTABLE QUERY = " + QUERY);
		Cursor cursor = db.rawQuery(QUERY, null);
		if (cursor != null && cursor.moveToFirst()) {
			Log.d("communicator", "getCount = " + cursor.getCount());
			for (int c = 0; c < cursor.getCount(); c++) {
				String entry = "";
				int columns = cursor.getColumnCount();
				for (int cc = 0; cc < columns; cc++) {
					String name = cursor.getColumnName(cc);
					String val = cursor.getString(cc);
					entry += name + "=" + val + ", ";
				}
				String returnStringLine = "DBTABLE [SENDING]" + entry;
				returnString = returnString + returnStringLine + "\n\n\n";
				Log.d("communicator", returnStringLine);
				cursor.moveToNext();
			}
			cursor.close();
		}
		db.close();
		return returnString;
	}

	// -----------------------------------------------------------------

	/** The cached conversation size. */
	private static int cachedConversationSize = -1;

	/**
	 * Gets the conversation size. Gets the COMPLETE size of all messages in DB.
	 * This value is cached. For multipart messages only the first part counts.
	 * 
	 * @param context
	 *            the context
	 * @param hostUid
	 *            the host uid
	 * @param forceRefresh
	 *            the force refresh
	 * @return the conversation size
	 */
	public static int getConversationSize(Context context, int hostUid,
			boolean forceRefresh) {
		if ((cachedConversationSize == -1) || forceRefresh) {
			SQLiteDatabase db = openDB(context, hostUid);

			try {
				String QUERY = "SELECT `localid`, `mid`, `fromuid`, `touid`, `text`, `created`, `sent`, `received` , `read`, `revoked`, `encrypted`, `transport`, `system`, `part` FROM `"
						+ TABLE_MESSAGES
						+ "` WHERE ((`fromuid` = "
						+ myUid()
						+ " AND `touid` = "
						+ hostUid
						+ ") OR (`fromuid` = "
						+ hostUid
						+ " AND `touid` = "
						+ myUid()
						+ ")) AND ( `system` != '1' OR `mid` = -1) AND `part` = "
						+ DB.DEFAULT_MESSAGEPART
						+ " ORDER BY `created` DESC, `sent` DESC "; // AND

				Cursor cursor = db.rawQuery(QUERY, null);
				if (cursor != null && cursor.moveToFirst()) {
					cachedConversationSize = cursor.getCount();
					cursor.close();
				}
			} catch (Exception e) {
				// If anything goes wrong loading the conversation, clear the
				// table
				askToRebuildDB(context, hostUid);
			}
			db.close();
		}
		return cachedConversationSize;
	}

	// -------------------------------------------------------------------------

	/**
	 * Load conversation. Attention: system messages will NOT be loaded. For
	 * multipart messages only the first part is loaded (if that exists).
	 * 
	 * @param context
	 *            the context
	 * @param hostUid
	 *            the host uid
	 * @param conversationList
	 *            the conversation list
	 * @param maxScrollMessageItems
	 *            the max scroll message items
	 */
	public static void loadConversation(Context context, int hostUid,
			List<ConversationItem> conversationList, int maxScrollMessageItems) {
		conversationList.clear();

		SQLiteDatabase db = openDB(context, hostUid);

		String LIMIT = " LIMIT " + maxScrollMessageItems;
		// Show only the last 50 messages
		if (maxScrollMessageItems == -1) {
			LIMIT = "";
		}

		try {
			String QUERY = "SELECT `localid`, `mid`, `fromuid`, `touid`, `text`, `created`, `sent`, `received` , `read`, `revoked`, `encrypted`, `transport`, `system`, `part`, `parts`, `multipartid` FROM `"
					+ TABLE_MESSAGES
					+ "` WHERE ((`fromuid` = "
					+ myUid()
					+ " AND `touid` = "
					+ hostUid
					+ ") OR (`fromuid` = "
					+ hostUid
					+ " AND `touid` = "
					+ myUid()
					+ ")) "
					+ " AND `fromuid` != -1 AND `touid` != -1 AND `text` != '' AND (`system` != '1' OR `mid` = '-1')"
					// "AND `system` != 1 "
					// AND `part` = "+ DB.DEFAULT_MESSAGEPART
					// + " GROUP BY `multipartid` HAVING `part` = MIN(`part`)"

					+ " ORDER BY case when `sent` < 10 then 1 else 0 end DESC, `sent` DESC, `created` DESC" // AND

					
					//+ " ORDER BY  `sent` IS NULL, `sent` DESC, `created` DESC" // AND
					//BY date IS NULL, date DESC
					
					//+ " ORDER BY `sent` DESC, `created` DESC" // AND

					// + " ORDER BY `sent` DESC NULLS LAST, `created` DESC" // AND
					
					//+ "ORDER BY CASE WHEN `sent` IS NULL THEN '1' ELSE '0' DESC, `sent` DESC" //, `sent` DESC, `created` DESC"
					//order by case when MyDate is null then 1 else 0 end
					
					// `system`
					// !=
					// '1'
					+ LIMIT;
			;
			Log.d("communicator", "loadConversation() -2255 QUERY = " + QUERY);

			Cursor cursor = db.rawQuery(QUERY, null);
			if (cursor != null && cursor.moveToFirst()) {
				Log.d("communicator", "loadConversation() -2255 getCount = "
						+ cursor.getCount());

				for (int c = 0; c < cursor.getCount(); c++) {

					int localid = Utility.parseInt(cursor.getString(0), -1);
					int mid = Utility.parseInt(cursor.getString(1), -1);
					int from = Utility.parseInt(cursor.getString(2), -1);
					int to = Utility.parseInt(cursor.getString(3), -1);
					String text = cursor.getString(4);

					Log.d("communicator", "loadConversation() -2255 text["
							+ cursor.getString(1) + "] = " + text);

					long created = parseTimestamp(cursor.getString(5), -1);
					long sent = parseTimestamp(cursor.getString(6), -1);

					boolean smsfailed = cursor.getString(6).equals(
							DB.SMS_FAILED);

					long received = parseTimestamp(cursor.getString(7), -1);
					long read = parseTimestamp(cursor.getString(8), -1);
					long revoked = parseTimestamp(cursor.getString(9), -1);
					String encyptedString = cursor.getString(10);
					int transport = Utility.parseInt(cursor.getString(11),
							TRANSPORT_INTERNET);
					String systemString = cursor.getString(12);
					boolean encrypted = false;
					if (encyptedString.equals("E")) {
						encrypted = true;
					}
					boolean system = false;
					if (systemString.equals("1")) {
						system = true;
					}
					int part = Utility.parseInt(cursor.getString(13), 1);
					int parts = Utility.parseInt(cursor.getString(14), 1);
					String multipartid = cursor.getString(15);

					if ((from != -1) && (to != -1)) {
						ConversationItem item = new ConversationItem();
						item.localid = localid;
						item.mid = mid;
						item.to = to;
						item.from = from;
						item.text = text;
						item.created = created;
						item.sent = sent;
						item.received = received;
						item.read = read;
						item.revoked = revoked;
						item.encrypted = encrypted;
						item.transport = transport;
						item.system = system;
						item.smsfailed = smsfailed;
						item.part = part;
						item.parts = parts;
						item.multipartid = multipartid;
						// We must add to position 0 (zero) because we have DESC
						// ordering (for the limit)
						conversationList.add(0, item);
					}
					cursor.moveToNext();
				}
				cursor.close();
			}
		} catch (Exception e) {
			// If anything goes wrong loading the conversation, clear the table
			askToRebuildDB(context, hostUid);
		}
		db.close();
		return;
	}

	// -----------------------------------------------------------------

	/**
	 * Checks if is table exists.
	 * 
	 * @param db
	 *            the db
	 * @param tableName
	 *            the table name
	 * @return true, if is table exists
	 */
	static boolean isTableExists(SQLiteDatabase db, String tableName) {
		if (tableName == null || db == null || !db.isOpen()) {
			return false;
		}
		Cursor cursor = db
				.rawQuery(
						"SELECT COUNT(*) FROM sqlite_master WHERE type = ? AND name = ?",
						new String[] { "table", tableName });
		if (!cursor.moveToFirst()) {
			return false;
		}
		int count = cursor.getInt(0);
		cursor.close();
		return count > 0;
	}

	// -----------------------------------------------------------------

	/** The largest timestamp received. */
	@SuppressLint("UseSparseArrays")
	static HashMap<Integer, Long> largestTimestampReceived = new HashMap<Integer, Long>();

	/**
	 * Gets the largest timestamp received. This value is cached.
	 * 
	 * @param context
	 *            the context
	 * @return the largest timestamp received
	 */
	static long getLargestTimestampReceived(Context context, int serverId) {
		if (!largestTimestampReceived.containsKey(serverId)) {
			largestTimestampReceived.put(serverId, Utility.loadLongSetting(
					context, Setup.SETTINGS_LARGEST_TS_RECEIVED + serverId,
					DB.getTimestamp()));
		}
		return largestTimestampReceived.get(serverId);
	}

	// -------------------------------------------------------------------------

	/**
	 * Gets the mid or localid of the last sent key message. This is needed for
	 * proper setting key timestamps of the OTHER transport method that was NOT
	 * used to send this messages when receiving delivery confirmations. If
	 * LASTKEYMID + hostUid == -1 this means: Invalidated because of NEW key
	 * sent If LASTKEYMID + hostUid == -2 this means: We do not await any key.
	 * The mid is needed for mapping deliveries of internet messages, the
	 * localid is needed for mapping deliveries of SMS.
	 * 
	 * @param context
	 *            the context
	 * @param hostUid
	 *            the host uid
	 * @param needLocalIdForSMS
	 *            the need local id for sms
	 * @return the last sent key message
	 */
	static int getLastSentKeyMessage(Context context, int hostUid,
			boolean needLocalIdForSMS) {
		int returnMidOrLocalId = Utility.loadIntSetting(context,
				Setup.LASTKEYMID + hostUid, -1);
		returnMidOrLocalId = -1; // DEBUG ONLY
		if (returnMidOrLocalId == -1) {
			returnMidOrLocalId = -2; // set to non-awaiting...
			SQLiteDatabase db = openDB(context, hostUid);
			String QUERY = "SELECT `mid` FROM `"
					+ TABLE_MESSAGES
					+ "` WHERE `received` < 10 AND `text` LIKE '%session key%' ORDER BY `mid` DESC";
			if (needLocalIdForSMS) {
				QUERY = "SELECT `localid` FROM `"
						+ TABLE_MESSAGES
						+ "` WHERE `received` < 10 AND `text` LIKE '%session key%' ORDER BY `localid` DESC";
			}

			Log.d("communicator",
					" KEYUPDATE: getLastSentKeyMessage() #1 QUERY=" + QUERY);

			Cursor cursor = db.rawQuery(QUERY, null);
			if (cursor != null && cursor.moveToFirst()) {
				Log.d("communicator",
						" KEYUPDATE: getLastSentKeyMessage() #2 QUERY=" + QUERY);

				if (cursor.getCount() > 0) {
					Log.d("communicator",
							" KEYUPDATE: getLastSentKeyMessage() #3 QUERY="
									+ QUERY);

					// Try again later (-1) if failing to parse Integer
					returnMidOrLocalId = Utility.parseInt(cursor.getString(0),
							-1);
				}
				cursor.close();
			}
			db.close();
			Log.d("communicator",
					" KEYUPDATE: getLastSentKeyMessage() SAVED NEW mid="
							+ returnMidOrLocalId);
			Utility.saveIntSetting(context, Setup.LASTKEYMID + hostUid,
					returnMidOrLocalId);
		} else {
			Log.d("communicator",
					" KEYUPDATE: getLastSentKeyMessage() CACHED mid="
							+ returnMidOrLocalId);
		}
		return returnMidOrLocalId;
	}

	// -------------------------------------------------------------------------

	/**
	 * Update largest timestamp received.
	 * 
	 * @param context
	 *            the context
	 * @param newTS
	 *            the new ts
	 */
	static void updateLargestTimestampReceived(Context context, String newTS,
			int serverId) {
		Log.d("communicator", " UPDATE LARGEST TIMESTAMP RECEIVED: " + newTS);
		long newTSLong = Utility.parseLong(newTS, 0);
		if (newTSLong > getLargestTimestampReceived(context, serverId)) {
			Utility.saveLongSetting(context, Setup.SETTINGS_LARGEST_TS_RECEIVED
					+ serverId, newTSLong);
			largestTimestampReceived.put(serverId, newTSLong);
		}
	}

	// -----------------------------------------------------------------

	/** The largest timestamp read. */
	@SuppressLint("UseSparseArrays")
	static HashMap<Integer, Long> largestTimestampRead = new HashMap<Integer, Long>();

	/**
	 * Gets the largest timestamp read. This value is cached.
	 * 
	 * @param context
	 *            the context
	 * @return the largest timestamp read
	 */
	static long getLargestTimestampRead(Context context, int serverId) {
		if (!largestTimestampRead.containsKey(serverId)) {
			largestTimestampRead.put(serverId, Utility.loadLongSetting(context,
					Setup.SETTINGS_LARGEST_TS_READ + serverId,
					DB.getTimestamp()));
		}
		return largestTimestampRead.get(serverId);
	}

	// -------------------------------------------------------------------------

	/**
	 * Update largest timestamp read.
	 * 
	 * @param context
	 *            the context
	 * @param newTS
	 *            the new ts
	 */
	static void updateLargestTimestampRead(Context context, String newTS,
			int serverId) {
		Log.d("communicator", " UPDATE LARGEST TIMESTAMP READ: " + newTS);
		long newTSLong = Utility.parseLong(newTS, 0);
		if (newTSLong > getLargestTimestampRead(context, serverId)) {
			Utility.saveLongSetting(context, Setup.SETTINGS_LARGEST_TS_READ
					+ serverId, newTSLong);
			largestTimestampRead.put(serverId, newTSLong);
		}
	}

	// -----------------------------------------------------------------

	/**
	 * Gets the largest mid for uid except system messages.
	 * 
	 * @param context
	 *            the context
	 * @param uid
	 *            the uid
	 * @return the largest mid for uid except system messages
	 */
	static int getLargestMidForUIDExceptSystemMessages(Context context, int uid) {
		// To filter system messages, just skip blank messages, as a convention
		// system messages are cleared
		int mid = -1;
		SQLiteDatabase db = openDB(context, uid);

		String QUERY = "SELECT `mid` FROM `" + TABLE_MESSAGES
				+ "` WHERE `touid` = " + DB.myUid() + " AND `fromuid` = " + uid
				+ " AND `transport` = 0 AND `system` != '1'"
				+ " ORDER BY `mid` DESC";

		Cursor cursor = null;
		try {
			cursor = db.rawQuery(QUERY, null);
			if (cursor != null && cursor.moveToFirst()) {
				// Log.d("communicator", "getCount = " + cursor.getCount());
				if (cursor.getCount() > 0) {
					mid = Utility.parseInt(cursor.getString(0), -1);
					// Log.d("communicator", "LARGEST MID FOR USER " + uid +
					// " QUERY = "
					// + QUERY + " ===> " + mid) ;
				}
				cursor.close();
			}
		} catch (Exception e) {
			if (cursor != null) {
				cursor.close();
			}
			e.printStackTrace();
			askToRebuildDB(context, uid);
		}
		db.close();
		// if -1 is returned, then we expect only to get ONE, the newest message
		// to start from!
		// -1 should only be returned for a fresh, new database or if there are
		// no users in our
		// list.
		return mid;
	}

	// -----------------------------------------------------------------

	/**
	 * Reset largest mid.
	 * 
	 * @param context
	 *            the context
	 * @param mid
	 *            the mid
	 */
	static void resetLargestMid(Context context, int mid, int serverId) {
		// Log.d("communicator",
		// "@@@@@@@ RESET LARGEST MID: " + mid);
		lastReceivedMid.put(serverId, mid);
		Utility.saveIntSetting(context, Setup.SETTINGS_LARGEST_MID_RECEIVED
				+ serverId, mid);
	}

	// -------------------------------------------------------------------------

	/**
	 * Update largest mid.
	 * 
	 * @param context
	 *            the context
	 * @param mid
	 *            the mid
	 * @return true, if successful
	 */
	static boolean updateLargestMid(Context context, int mid, int serverId) {
		// Log.d("communicator",
		// "@@@@@@@ UPDATE LARGEST MID: " + mid);
		int currentMid = getLargestMid(context, serverId);
		if (mid > currentMid) {
			resetLargestMid(context, mid, serverId);
			lastReceivedMid.put(serverId, mid);
			return true;
		}
		return false;
	}

	// -------------------------------------------------------------------------

	/**
	 * Gets the largest mid.
	 * 
	 * @param context
	 *            the context
	 * @return the largest mid
	 */
	static int getLargestMid(Context context, int serverId) {
		// Cache to make it more efficient, be sure to invalidate if receiving
		// new messages -- this is also used for clearing conversation!!!
		if (!lastReceivedMid.containsKey(serverId)) {
			lastReceivedMid.put(serverId, Utility.loadIntSetting(context,
					Setup.SETTINGS_LARGEST_MID_RECEIVED + serverId, -1));
		}
		// Log.d("communicator",
		// "@@@@@@@ GET LARGEST MID: " + lastReceivedMid);
		return lastReceivedMid.get(serverId);
	}

	// -----------------------------------------------------------------

	/**
	 * Gets the next message. Gets the next message to send from DB for a
	 * particular transport medium and a particular server (iff INTERNET is the
	 * transport medium). If SMS is the transport medium then user -1 for the
	 * serverID. If transport is -1 then do not consider specific transport or
	 * servers!
	 * 
	 * @param context
	 *            the context
	 * @param transport
	 *            the transport
	 * @param serverId
	 *            the server id
	 * @return the next message
	 */
	static ConversationItem getNextMessage(Context context, int transport,
			int serverId) {
		ConversationItem returnItem = null;

		String transportQueryPart = " AND `transport` = " + transport
				+ " AND `serverId` = " + serverId;
		if (transport == DB.TRANSPORT_SMS) {
			// For SMS messages the serverId is irrelevant!
			transportQueryPart = " AND `transport` = " + transport;
		}
		if (transport == -1) {
			transportQueryPart = "";
		}

		SQLiteDatabase db = openDBSending(context);

		try {
			// For sending
			String QUERY = "SELECT `sendingid`, `localid`, `fromuid`, `touid`, `text`, `created`, `encrypted`, `transport`, `system`, `smsfailcnt`, `tries`, `lasttry`, `part`, `parts`, `multipartid`  FROM `"
					+ TABLE_SENDING
					+ "` WHERE `touid` != -1 AND `localid` != -1 AND `smsfailcnt` <= "
					+ Setup.SMS_FAIL_CNT
					+ " "
					+ transportQueryPart
					+ " ORDER BY `prio` DESC, `created` ASC";

			// The priority ensures that first KEYs are sent, THEN messages and
			// only
			// THEN read confirmations

			// Log.d("communicator", "SEND NEXT QUERY = " + QUERY);

			Cursor cursor = db.rawQuery(QUERY, null);
			if (cursor != null && cursor.moveToFirst()) {
				// Log.d("communicator", "SEND NEXT QUERY getCount = " +
				// cursor.getCount());
				if (cursor.getCount() > 0) {
					returnItem = new ConversationItem();
					int sendingid = Utility.parseInt(cursor.getString(0), -1);
					int localid = Utility.parseInt(cursor.getString(1), -1);
					// Log.d("communicator", "SEND NEXT QUERY localID = " +
					// localid2);
					// int mid = Utility.parseInt(cursor.getString(), -1);
					int from = Utility.parseInt(cursor.getString(2), -1);
					int to = Utility.parseInt(cursor.getString(3), -1);
					String text = cursor.getString(4);

					long created = parseTimestamp(cursor.getString(5), -1);
					String encyptedString = cursor.getString(6);
					int transport2 = Utility.parseInt(cursor.getString(7),
							TRANSPORT_INTERNET);
					int smsfailcnt = Utility.parseInt(cursor.getString(9), 0);
					String systemString = cursor.getString(8);
					boolean encrypted = false;
					if (encyptedString.equals("E")) {
						encrypted = true;
					}
					boolean system = false;
					if (systemString.equals("1")) {
						system = true;
					}

					int tries = Utility.parseInt(cursor.getString(10), 0);
					long lasttry = parseTimestamp(cursor.getString(11), -1);
					int part = Utility.parseInt(cursor.getString(12), 0);
					int parts = Utility.parseInt(cursor.getString(13), 0);
					String multipartid = cursor.getString(14);

					if ((from != -1) && (to != -1)) {
						returnItem.sendingid = sendingid;
						returnItem.localid = localid;
						returnItem.from = from;
						returnItem.to = to;
						returnItem.text = text;
						returnItem.created = created;
						returnItem.encrypted = encrypted;
						returnItem.transport = transport2;
						returnItem.system = system;
						returnItem.smsfailcnt = smsfailcnt;
						returnItem.isKey = text.startsWith("K");
						returnItem.tries = tries;
						returnItem.lasttry = lasttry;
						returnItem.part = part;
						returnItem.parts = parts;
						returnItem.multipartid = multipartid;
					}
				}
				cursor.close();
			}
		} catch (Exception e) {
			rebuildDBSending(context);
		}
		db.close();

		// Possibly fix the transport if receipient is only external SMS user
		// We should not come here but still want to guard against it
		if (returnItem != null && returnItem.transport == DB.TRANSPORT_INTERNET
				&& returnItem.to < 0) {
			returnItem.transport = DB.TRANSPORT_SMS;
		}

		return returnItem;
	}

	// -----------------------------------------------------------------

	/**
	 * Gets the message. Gets the message with local id. If localid != -1 then
	 * transport does not matter! If transport == -1 then transport does not
	 * matter!
	 * 
	 * @param context
	 *            the context
	 * @param localid
	 *            the localid
	 * @param uid
	 *            the uid
	 * @param part
	 *            the part
	 * @return the message
	 */
	static ConversationItem getMessage(Context context, int localid, int uid,
			int part) {
		ConversationItem returnItem = null;

		SQLiteDatabase db = openDB(context, uid);

		// for display specific message
		String QUERY = "SELECT `localid`, `mid`, `fromuid`, `touid`, `text`, `created`, `sent`, `received` , `read`, `revoked`, `encrypted`, `transport`, `system`, `parts`, `multipartid`  FROM `"
				+ TABLE_MESSAGES
				+ "` WHERE `localid` = "
				+ localid
				+ " AND `part` = " + part;

		// Log.d("communicator", "GET MESSAGE QUERY = " + QUERY);

		Cursor cursor = db.rawQuery(QUERY, null);
		if (cursor != null && cursor.moveToFirst()) {
			// Log.d("communicator", "GET MESSAGE QUERY getCount = " +
			// cursor.getCount());
			if (cursor.getCount() > 0) {
				returnItem = new ConversationItem();
				int localid2 = Utility.parseInt(cursor.getString(0), -1);
				// Log.d("communicator", "SEND NEXT QUERY localID = " +
				// localid2);
				int mid = Utility.parseInt(cursor.getString(1), -1);
				int from = Utility.parseInt(cursor.getString(2), -1);
				int to = Utility.parseInt(cursor.getString(3), -1);
				String text = cursor.getString(4);

				long created = parseTimestamp(cursor.getString(5), -1);
				long sent = parseTimestamp(cursor.getString(6), -1);
				boolean smsfailed = cursor.getString(6).equals(DB.SMS_FAILED);

				long received = parseTimestamp(cursor.getString(7), -1);
				long read = parseTimestamp(cursor.getString(8), -1);
				long revoked = parseTimestamp(cursor.getString(9), -1);
				String encyptedString = cursor.getString(10);
				int transport2 = Utility.parseInt(cursor.getString(11),
						TRANSPORT_INTERNET);
				String systemString = cursor.getString(12);
				boolean encrypted = false;
				if (encyptedString.equals("E")) {
					encrypted = true;
				}
				boolean system = false;
				if (systemString.equals("1")) {
					system = true;
				}
				int parts = Utility.parseInt(cursor.getString(13), 1);

				Log.d("communicator",
						"MULTIPART LOAD MESSAGE cursor.getString(14)="
								+ cursor.getString(14));

				String multipartid = cursor.getString(14);

				if ((from != -1) && (to != -1)) {
					returnItem.localid = localid2;
					returnItem.mid = mid;
					returnItem.from = from;
					returnItem.to = to;
					returnItem.text = text;
					returnItem.created = created;
					returnItem.sent = sent;
					returnItem.received = received;
					returnItem.read = read;
					returnItem.revoked = revoked;
					returnItem.encrypted = encrypted;
					returnItem.transport = transport2;
					returnItem.system = system;
					returnItem.smsfailed = smsfailed;
					returnItem.isKey = text.startsWith("K");
					returnItem.part = part;
					returnItem.parts = parts;
					returnItem.multipartid = multipartid;
				}
			}
			cursor.close();
		}
		db.close();
		return returnItem;
	}

	// -----------------------------------------------------------------

	/**
	 * Gets the sender uid by mid.
	 * 
	 * @param context
	 *            the context
	 * @param mid
	 *            the mid
	 * @param hostUid
	 *            the host uid
	 * @return the sender uid by mid
	 */
	static int getSenderUidByMid(Context context, int mid, int hostUid) {
		int toUid = -1;
		SQLiteDatabase db = openDB(context, hostUid);

		String QUERY = "SELECT `fromuid` FROM `" + TABLE_MESSAGES
				+ "` WHERE `mid` = " + mid;
		// Log.d("communicator", "LARGEST MID QUERY = " + QUERY);
		Cursor cursor = db.rawQuery(QUERY, null);
		if (cursor != null && cursor.moveToFirst()) {
			if (cursor.getCount() > 0) {
				toUid = Utility.parseInt(cursor.getString(0), -1);
			}
			cursor.close();
		}
		db.close();
		return toUid;
	}

	// -----------------------------------------------------------------

	/**
	 * Gets the host local id by mid.
	 * 
	 * @param context
	 *            the context
	 * @param mid
	 *            the mid
	 * @param hostUid
	 *            the host uid
	 * @return the host local id by mid
	 */
	public static int getHostLocalIdByMid(Context context, int mid, int hostUid) {
		int localId = -1;
		SQLiteDatabase db = openDB(context, hostUid);

		String QUERY = "SELECT `localid` FROM `" + TABLE_MESSAGES
				+ "` WHERE `mid` = " + mid;
		// Log.d("communicator", "LARGEST MID QUERY = " + QUERY);
		Cursor cursor = db.rawQuery(QUERY, null);
		if (cursor != null && cursor.moveToFirst()) {
			if (cursor.getCount() > 0) {
				localId = Utility.parseInt(cursor.getString(0), -1);
			}
			cursor.close();
		}
		db.close();
		return localId;
	}

	// -----------------------------------------------------------------

	/**
	 * Gets the uid (sender/receipient) by localid from the sending queue.
	 * 
	 * @param context
	 *            the context
	 * @param localId
	 *            the local id
	 * @param receipient
	 *            the receipient
	 * @return the host local id by mid
	 */
	public static int getUidByMid(Context context, int localId,
			boolean receipient) {
		int uid = -1;
		SQLiteDatabase db = openDBSending(context);
		String QUERY = "SELECT `fromuid` FROM `" + TABLE_SENDING
				+ "` WHERE `localid` = " + localId;
		if (receipient) {
			QUERY = "SELECT `touid` FROM `" + TABLE_SENDING
					+ "` WHERE `localid` = " + localId;
		}
		Cursor cursor = db.rawQuery(QUERY, null);
		if (cursor != null && cursor.moveToFirst()) {
			if (cursor.getCount() > 0) {
				uid = Utility.parseInt(cursor.getString(0), -1);
			}
			cursor.close();
		}
		db.close();
		return uid;
	}

	// -----------------------------------------------------------------

	/**
	 * Update own message read.
	 * 
	 * @param context
	 *            the context
	 * @param hostUid
	 *            the host uid
	 * @return true, if successful
	 */
	public static boolean updateOwnMessageRead(Context context, int hostUid) {
		boolean success = false;
		ContentValues values = new ContentValues();
		values.put("read", DB.getTimestampString());
		// Update
		try {
			SQLiteDatabase db = openDB(context, hostUid);
			// all messages that I have received, I flag in my own DB as read
			// now that I am reading..
			db.update("messages", values, "`touid` = " + DB.myUid()
					+ " AND `fromuid` != " + DB.myUid(), null);
			db.close();
			success = true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return success;
	}

	// -----------------------------------------------------------------

	/**
	 * Update message read.
	 * 
	 * @param context
	 *            the context
	 * @param mid
	 *            the mid
	 * @param timestamp
	 *            the timestamp
	 * @param hostUid
	 *            the host uid
	 * @return true, if successful
	 */
	public static boolean updateMessageRead(Context context, int mid,
			String timestamp, int hostUid) {
		boolean success = false;
		ContentValues values = new ContentValues();
		values.put("read", timestamp);
		// update
		try {
			SQLiteDatabase db = openDB(context, hostUid);
			int rows = db.update("messages", values, "`mid` <= " + mid
					+ " AND `read` < 10", null);
			Log.d("communicator", "updateMessageRead rows = " + rows);
			db.close();
			success = true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return success;
	}

	// -----------------------------------------------------------------

	/**
	 * Try to revoke message. This message will try to revoke unsent messages,
	 * if this succeeds, it will be revoked right away. Otherwise we send a
	 * revoke request to the server and wait for the server response message!
	 * 
	 * @param context
	 *            the context
	 * @param mid
	 *            the mid
	 * @param localid
	 *            the localid
	 * @param timestamp
	 *            the timestamp
	 * @param hostUid
	 *            the host uid
	 */
	public static void tryToRevokeMessage(Context context, int mid,
			int localid, String timestamp, int hostUid) {
		// If mid < 0 it is still a localid, but this does not mean it is not
		// sent yet!
		boolean revokeBeforeSending = revokeFromSending(context, localid);
		if (revokeBeforeSending) {
			Log.d("communicator", " REVOKE BEFORE SENDING POSSIBLE :-)");
			ContentValues values = new ContentValues();
			values.put("text", DB.REVOKEDTEXT);
			values.put("revoked", timestamp);
			// update
			try {
				SQLiteDatabase db = openDB(context, hostUid);
				db.update("messages", values, "mid = " + mid + " AND sent",
						null);
				db.update("messages", values, "localid = " + localid, null);
				// Try both
				Conversation.setRevokedInConversation(context, mid);
				Conversation.setRevokedInConversation(context, -1 * localid);
				db.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			Log.d("communicator", " REVOKE BEFORE SENDING NOT POSSIBLE :-(");
		}
		if (mid > -1) {
			// Send a revoke request just to make sure, even if we could
			// possibly remove it
			// from the sending queue it may just have been sent!
			Communicator.sendSystemMessageRevoke(context, hostUid, mid);
		}
	}

	// -----------------------------------------------------------------

	/**
	 * Update message revoked.
	 * 
	 * @param context
	 *            the context
	 * @param mid
	 *            the mid
	 * @param timestamp
	 *            the timestamp
	 * @param hostUid
	 *            the host uid
	 * @return true, if successful
	 */
	public static boolean updateMessageRevoked(Context context, int mid,
			String timestamp, int hostUid) {
		boolean success = false;
		ContentValues values = new ContentValues();
		values.put("text", DB.REVOKEDTEXT);
		values.put("revoked", timestamp);
		// Update
		try {
			SQLiteDatabase db = openDB(context, hostUid);
			db.update("messages", values, "mid = " + mid, null);
			// Log.d("communicator", "UPDATE REVOKED OF MID " + mid + "= " +
			// timestamp +": " + rows);
			db.close();
			success = true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return success;
	}

	// -----------------------------------------------------------------

	/**
	 * Update message system.
	 * 
	 * @param context
	 *            the context
	 * @param mid
	 *            the mid
	 * @param isSystemMessage
	 *            the is system message
	 * @param hostUid
	 *            the host uid
	 * @return true, if successful
	 */
	public static boolean updateMessageSystem(Context context, int mid,
			boolean isSystemMessage, int hostUid) {
		boolean success = false;
		ContentValues values = new ContentValues();
		if (isSystemMessage) {
			values.put("system", "1");
		} else {
			values.put("system", "0");
		}
		// Update
		try {
			SQLiteDatabase db = openDB(context, hostUid);
			int rows = db.update("messages", values, "mid = " + mid, null);
			Log.d("communicator", "UPDATE SYSTEM OF MID " + mid + ": " + rows);
			db.close();
			success = true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return success;
	}

	// -----------------------------------------------------------------

	/**
	 * Checks if is message failed.
	 * 
	 * @param sentTimestamp
	 *            the sent timestamp
	 * @return true, if is message failed
	 */
	public static boolean isMessageFailed(String sentTimestamp) {
		return (sentTimestamp.equals(SMS_FAILED));
	}

	// -----------------------------------------------------------------

	/**
	 * Update message failed for localid. This message is used if an SMS failed
	 * to sent.
	 * 
	 * @param context
	 *            the context
	 * @param localid
	 *            the localid
	 * @param hostUid
	 *            the host uid
	 * @return true, if successful
	 */
	public static boolean updateMessageFailed(Context context, int localid,
			int hostUid) {
		boolean success = false;
		ContentValues values = new ContentValues();
		values.put("sent", SMS_FAILED);
		// Update
		try {
			SQLiteDatabase db = openDB(context, hostUid);
			db.update(TABLE_MESSAGES, values, "localid = " + localid, null);
			Log.d("communicator", "SET PERMENANT FAILED OF localid " + localid
					+ " of user " + hostUid);
			db.close();
			success = true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return success;
	}

	// -----------------------------------------------------------------

	/**
	 * Gets the failed.
	 * 
	 * @param context
	 *            the context
	 * @param localid
	 *            the localid
	 * @return the failed
	 */
	public static int getfailed(Context context, int localid) {
		if (localid == -1) {
			return -1;
		}
		SQLiteDatabase db = openDBSending(context);
		int returnFailCnt = Setup.SMS_FAIL_CNT;
		String QUERY = "SELECT `smsfailcnt` FROM `" + TABLE_SENDING
				+ "` WHERE `localid` = " + localid;

		Cursor cursor = db.rawQuery(QUERY, null);
		if (cursor != null && cursor.moveToFirst()) {
			if (cursor.getCount() > 0) {
				returnFailCnt = Utility.parseInt(cursor.getString(0), -1);
			}
			cursor.close();
		}
		db.close();
		return returnFailCnt;
	}

	// -----------------------------------------------------------------

	/**
	 * Increment failed.
	 * 
	 * @param context
	 *            the context
	 * @param localid
	 *            the localid
	 * @return true, if successful
	 */
	public static boolean incrementFailed(Context context, int localid) {
		int hostUid = getUidByMid(context, localid, true);
		int failcnt = getfailed(context, localid);
		failcnt++;
		boolean permamentFailed = false;
		ContentValues values = new ContentValues();
		values.put("smsfailcnt", failcnt);
		// Update
		try {
			SQLiteDatabase db = openDBSending(context);
			db.update(TABLE_SENDING, values, "localid = " + localid, null);
			Log.d("communicator", "INCREMENTD FAILED OF localid " + localid
					+ " to " + failcnt);
			db.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		// If failed too often, then flag as failed in DB_MESSAGES
		if (failcnt > Setup.SMS_FAIL_CNT) {
			permamentFailed = updateMessageFailed(context, localid, hostUid);
		}
		return permamentFailed;
	}

	// -----------------------------------------------------------------

	/**
	 * Gets the tries counter for a localid.
	 * 
	 * @param context
	 *            the context
	 * @param localid
	 *            the localid
	 * @return the failed
	 */
	public static int getTries(Context context, int localid) {
		if (localid == -1) {
			return -1;
		}
		SQLiteDatabase db = openDBSending(context);
		int returnTries = 0;
		String QUERY = "SELECT `tries` FROM `" + TABLE_SENDING
				+ "` WHERE `localid` = " + localid;

		Cursor cursor = db.rawQuery(QUERY, null);
		if (cursor != null && cursor.moveToFirst()) {
			if (cursor.getCount() > 0) {
				returnTries = Utility.parseInt(cursor.getString(0), -1);
			}
			cursor.close();
		}
		db.close();
		return returnTries;
	}

	// -----------------------------------------------------------------

	/**
	 * Increment failed.
	 * 
	 * @param context
	 *            the context
	 * @param localid
	 *            the localid
	 * @return true, if successful
	 */
	public static boolean incrementTries(Context context, int localid) {
		int triescnt = getTries(context, localid);
		triescnt++;
		boolean permamentFailed = false;
		ContentValues values = new ContentValues();
		values.put("tries", triescnt);
		// Update
		try {
			SQLiteDatabase db = openDBSending(context);
			db.update(TABLE_SENDING, values, "localid = " + localid, null);
			Log.d("communicator", "INCREMENTD FAILED OF localid " + localid
					+ " to " + triescnt);
			db.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return permamentFailed;
	}

	// -----------------------------------------------------------------

	/**
	 * Update message received.
	 * 
	 * @param context
	 *            the context
	 * @param mid
	 *            the mid
	 * @param timestamp
	 *            the timestamp
	 * @param hostUid
	 *            the host uid
	 * @return true, if successful
	 */
	public static boolean updateMessageReceived(Context context, int mid,
			String timestamp, int hostUid) {
		boolean success = false;
		ContentValues values = new ContentValues();
		values.put("received", timestamp);
		// Update
		try {
			SQLiteDatabase db = openDB(context, hostUid);
			int rows = db.update(TABLE_MESSAGES, values, "mid = " + mid, null);
			Log.d("communicator", "UPDATE RECEIVED OF MID " + mid + ": " + rows);
			db.close();
			success = true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return success;
	}

	// -----------------------------------------------------------------

	/**
	 * Checks if is already in db.
	 * 
	 * @param context
	 *            the context
	 * @param mid
	 *            the mid
	 * @param hostUid
	 *            the host uid
	 * @return true, if is already in db
	 */
	public static boolean isAlreadyInDB(Context context, int mid, int hostUid) {
		if (mid == SMS_MID) {
			// Incoming SMS have this mid -1
			return false;
		}
		boolean foundInDB = false;
		try {
			SQLiteDatabase db = openDB(context, hostUid);
			String QUERY = "SELECT `mid` FROM `" + TABLE_MESSAGES
					+ "` WHERE `mid` = " + mid;
			Cursor cursor = db.rawQuery(QUERY, null);
			if (cursor != null && cursor.moveToFirst()) {
				foundInDB = cursor.getCount() > 0;
				cursor.close();
			}
			db.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return foundInDB;
	}

	// -----------------------------------------------------------------

	/**
	 * Update message.
	 * 
	 * @param context
	 *            the context
	 * @param itemToUpdate
	 *            the item to update
	 * @param hostUid
	 *            the host uid
	 * @return true, if successful
	 */
	public static boolean updateMessage(Context context,
			ConversationItem itemToUpdate, int hostUid) {
		return updateMessage(context, itemToUpdate, hostUid, false);
	}

	// -----------------------------------------------------------------

	/**
	 * Update message.
	 * 
	 * @param context
	 *            the context
	 * @param itemToUpdate
	 *            the item to update
	 * @param hostUid
	 *            the host uid
	 * @param isSentKeyMessage
	 *            the is sent key message
	 * @return true, if successful
	 */
	public static boolean updateMessage(Context context,
			ConversationItem itemToUpdate, int hostUid, boolean isSentKeyMessage) {
		boolean success = false;
		ContentValues values = new ContentValues();
		values.put("mid", itemToUpdate.mid);
		values.put("fromuid", itemToUpdate.from);
		values.put("touid", itemToUpdate.to);
		if (!isSentKeyMessage) {
			if (itemToUpdate.text != null) {
				values.put("text", itemToUpdate.text);
			}
			if (itemToUpdate.system) {
				values.put("system", "1");
			} else {
				values.put("system", "0");
			}
		}
		values.put("created", itemToUpdate.created + "");
		values.put("sent", itemToUpdate.sent + "");
		values.put("received", itemToUpdate.received + "");
		values.put("read", itemToUpdate.read + "");
		values.put("revoked", itemToUpdate.revoked + "");
		values.put("transport", itemToUpdate.transport + "");
		values.put("part", itemToUpdate.part);
		values.put("parts", itemToUpdate.parts);
		values.put("multipartid", itemToUpdate.multipartid + "");
		if (itemToUpdate.encrypted) {
			values.put("encrypted", "E");
		} else {
			values.put("encrypted", "U");
		}
		int localid = itemToUpdate.localid;
		if (localid >= 0) {
			// Update
			try {
				SQLiteDatabase db = openDB(context, hostUid);
				db.update(DB.TABLE_MESSAGES, values, "localid = " + localid,
						null);
				db.close();
				success = true;
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			// Create new item
			try {
				SQLiteDatabase db = openDB(context, hostUid);
				db.insert("messages", null, values);
				db.close();
				success = true;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return success;
	}

	// -----------------------------------------------------------------

	/**
	 * Delete user.
	 * 
	 * @param context
	 *            the context
	 * @param uid
	 *            the uid
	 */
	public static void deleteUser(Context context, int uid) {
		context.deleteDatabase(Setup.DATABASEPREFIX + uid
				+ Setup.DATABASEPOSTFIX);
	}

	// -----------------------------------------------------------------

	/**
	 * Merge all SMS from one user account to another.
	 * 
	 * @param context
	 *            the context
	 * @param mergeFromUid
	 *            the merge from uid
	 * @param mergeToUid
	 *            the merge to uid
	 * @return true, if successful
	 */
	public static boolean mergeUser(Context context, int mergeFromUid,
			int mergeToUid) {
		boolean success = false;
		// Update
		try {
			List<ConversationItem> conversationToMergeList = new ArrayList<ConversationItem>();
			// Now move all (except system messages, these will not be loaded
			// and hence not be copied!)
			loadConversation(context, mergeFromUid, conversationToMergeList, -1);
			for (ConversationItem item : conversationToMergeList) {
				if (item.from == mergeFromUid) {
					item.from = mergeToUid;
				}
				if (item.to == mergeFromUid) {
					item.to = mergeToUid;
				}
				addMessage(context, item.from, item.to, item.text, item.created
						+ "", item.sent + "", item.received + "", item.read
						+ "", item.revoked + "", item.encrypted,
						item.transport, false, 0, 1, NO_MULTIPART_ID);
			}
			success = true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return success;
	}

	// -----------------------------------------------------------------

	public static void possiblyUpdate(Context context) {
		String oldMyUidString = Utility.loadStringSetting(context,
				Setup.SERVER_UID, "");

		if (oldMyUidString != null && oldMyUidString.length() > 0) {

			if (Setup.getVersion(context) < Setup.VERSION_MULTISERVER) {
				// Recovery action needed - GO FROM SINGLE SERVER VERSION TO
				// MULTISERVER VERSION

				// We must go thru the uid list, create a new uid from this then
				// suid
				// considering cryptocator.org as the server
				// we then must update the database and replace
				// 1. our uid by -1
				// 2. the other suid by the new uid

				String newMyUidString = "-3";

				List<Integer> newUidList = new ArrayList<Integer>();
				List<Integer> uidList = new ArrayList<Integer>();
				String listString = Utility.loadStringSetting(context,
						Setup.SETTINGS_USERLIST, "");
				Main.appendUIDList(context, listString, uidList);

				for (int oldUid : uidList) {

					int uid = oldUid;
					if (oldUid > 0) {
						int newUid = Setup.getUid(context, oldUid,
								Setup.getServerId(Setup.DEFAULT_SERVER));
						newUidList.add(newUid);

						// Copy all data from the oldUid to the newUid
						mergeUser(context, oldUid, newUid);
						uid = newUid;
					} else {
						// SMS user keep their
						newUidList.add(oldUid);
					}

					try {
						SQLiteDatabase db = openDB(context, uid);

						ContentValues values = new ContentValues();
						values.put("fromuid", newMyUidString);
						db.update(DB.TABLE_MESSAGES, values, "fromuid = "
								+ oldMyUidString, null);
						values = new ContentValues();
						values.put("touid", newMyUidString);
						db.update(DB.TABLE_MESSAGES, values, "touid = "
								+ oldMyUidString, null);

						db.close();
					} catch (Exception e) {
						e.printStackTrace();
					}

					if (oldUid > 0) {
						dropDB(context, oldUid);
					}
				}

				// Save the new Uid list instead
				Log.d("communicator",
						"MIGRATE OLD: " + Utility.getListAsString(uidList, ","));
				Log.d("communicator",
						"MIGRATE NEW: "
								+ Utility.getListAsString(newUidList, ","));

				Utility.saveStringSetting(context, Setup.SETTINGS_USERLIST,
						Utility.getListAsString(newUidList, ","));

				Setup.setVersionUpdated(context, Setup.VERSION_MULTISERVER,
						true,
						"Cryptocator updated DB to new version and needs to be restarted.");
			}

			if (Setup.getVersion(context) < Setup.VERSION_MULTISERVERREVOKE) {
				// Rename message table column revoked to revoke

				List<Integer> uidList = new ArrayList<Integer>();
				String listString = Utility.loadStringSetting(context,
						Setup.SETTINGS_USERLIST, "");
				Main.appendUIDList(context, listString, uidList);
				int i = 0;
				for (int uid : uidList) {
					String name = Main.UID2Name(context, uid, false);
					Utility.showToastAsync(context, "Testing DB[" + i
							+ "] for " + name);
					try {
						SQLiteDatabase db = openDB(context, uid);
						// check if column name is old
						boolean error = true;
						try {
							db.rawQuery("SELECT `withdraw` from "
									+ TABLE_MESSAGES, null);
							error = false;
						} catch (Exception e) {
							error = true;
						}
						if (!error) {
							// We have an old column update now
							Utility.showToastAsync(context, "Now Updating DB["
									+ i + "] for " + name);
							db.execSQL("BEGIN TRANSACTION;");
							db.execSQL("ALTER TABLE " + TABLE_MESSAGES
									+ " RENAME TO BACKUP" + TABLE_MESSAGES
									+ ";");
							final String CREATE_TABLE_MSGS = "CREATE TABLE IF NOT EXISTS  `"
									+ TABLE_MESSAGES
									+ "` ("
									+ "`localid` INTEGER PRIMARY KEY, `mid` INTEGER , `fromuid` INTEGER , "
									+ "`touid` INTEGER , `text` VARCHAR( 2000 ) , "
									+ "`created` VARCHAR( 50 ), "
									+ "`sent` VARCHAR( 50 ) , `received` VARCHAR( 50 ), `read` VARCHAR( 50 ), `revoked` VARCHAR( 50 ), `encrypted` VARCHAR( 1 ), `transport` VARCHAR( 1 ), `system` VARCHAR( 1 ) , `part` INTEGER DEFAULT "
									+ DB.DEFAULT_MESSAGEPART
									+ " , `parts` INTEGER DEFAULT 1, `multipartid` VARCHAR( 5 ) DEFAULT ``);";
							db.execSQL(CREATE_TABLE_MSGS);
							db.execSQL("INSERT INTO "
									+ TABLE_MESSAGES
									+ "(`mid`,`fromuid`,`touid`,`text`,`created`,`sent`,`received`,`read`,`revoked`,`encrypted`,`transport`,`system`,`part`) SELECT `mid`,`fromuid`,`touid`,`text`,`created`,`sent`,`received`,`read`,`withdraw`,`encrypted`,`transport`,`system`,`part` FROM BACKUP"
									+ TABLE_MESSAGES + ";");
							db.execSQL("DROP TABLE BACKUP" + TABLE_MESSAGES
									+ ";");
							db.execSQL("COMMIT;");
						}
						db.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				Setup.setVersionUpdated(context,
						Setup.VERSION_MULTISERVERREVOKE, true,
						"Cryptocator updated DB to new version and needs to be restarted.");

			}
		}
	}

	// -----------------------------------------------------------------

}
