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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.util.Pair;
import android.util.Log;

public class DB {

	public static final String TABLE_MESSAGES = "messages";
	public static final String TABLE_SENDING = "sending";
	public static final String TABLE_SENT = "sent"; // mapping between mid and
													// uid! for fast processing
													// received and read
													// confirmations!

	public static int lastReceivedMid = -1;

	public static int SMS_MID = -1;

	public static int TRANSPORT_INTERNET = 0;
	public static int TRANSPORT_SMS = 1;

	public static int PRIORITY_READCONFIRMATION = 0;
	public static int PRIORITY_MESSAGE = 1;
	public static int PRIORITY_KEY = 2;

	public static long MAXTIMESTAMP = Long.MAX_VALUE;

	public static String WITHDRAWNTEXT = "[ message withdrawn ]";
	public static String SMS_FAILED = "FAILED";

	// -----------------------------------------------------------------

	static int myUid = -1;

	public static int myUid(Context context) {
		if (myUid < 0) {
			String uidString = Utility.loadStringSetting(context, "uid", "");
			try {
				myUid = Integer.parseInt(uidString);
			} catch (Exception e) {
			}
		}
		return myUid;
	}

	// -----------------------------------------------------------------

	public static String getTimestampString() {
		return getTimestamp() + "";
	}

	public static long getTimestamp() {
		long timeStamp = System.currentTimeMillis(); // calendar.getTimeInMillis();
		return timeStamp;
	}

	public static long parseTimestamp(String timestampString) {
		return parseTimestamp(timestampString, getTimestamp());
	}

	public static long parseTimestamp(String timestampString, long defaultValue) {
		try {
			long returnValue = Long.parseLong(timestampString);
			return returnValue;
		} catch (Exception e) {
		}
		return defaultValue;
	}

	// -----------------------------------------------------------------

	public static String getDateString(long timestamp, boolean details) {

		if (timestamp < 10) {
			return Setup.NA;
		}

		String format = "HH:mm,  dd. MMM yyyy";
		String thisYear = Utility.getYear(getTimestamp());
		String otherYear = Utility.getYear(getTimestamp());
		if (thisYear.equals(otherYear)) {
			// if the same year, then do not print it
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

	public static void dropDBSent(Context context) {
		SQLiteDatabase db = openDBSending(context);
		if (isTableExists(db, TABLE_SENT)) {
			final String CREATE_TABLE_MSGS = "DROP TABLE `" + TABLE_SENT + "`;";
			db.execSQL(CREATE_TABLE_MSGS);
		}
		db.close();
	}

	// -----------------------------------------------------------------

	public static void initializeDB(Context context, SQLiteDatabase db) {
		db.setVersion(1);
		db.setLocale(Locale.getDefault());
		db.setLockingEnabled(true);

		final String CREATE_TABLE_MSGS = "CREATE TABLE IF NOT EXISTS  `"
				+ TABLE_MESSAGES
				+ "` ("
				+ "`localid` INTEGER PRIMARY KEY, `mid` INTEGER , `fromuid` INTEGER , "
				+ "`touid` INTEGER , `text` VARCHAR( 2000 ) , "
				+ "`created` VARCHAR( 50 ), "
				+ "`sent` VARCHAR( 50 ) , `received` VARCHAR( 50 ), `read` VARCHAR( 50 ), `withdraw` VARCHAR( 50 ), `encrypted` VARCHAR( 1 ), `transport` VARCHAR( 1 ), `system` VARCHAR( 1 )  );";
		db.execSQL(CREATE_TABLE_MSGS);
	}

	public static void initializeDBSending(Context context, SQLiteDatabase db) {
		db.setVersion(1);
		db.setLocale(Locale.getDefault());
		db.setLockingEnabled(true);

		final String CREATE_TABLE_MSGS = "CREATE TABLE IF NOT EXISTS  `"
				+ TABLE_SENDING
				+ "` ("
				+ "`sendingid` INTEGER PRIMARY KEY, `localid` INTEGER, `fromuid` INTEGER , "
				+ "`touid` INTEGER , `text` VARCHAR( 2000 ) , "
				+ "`created` VARCHAR( 50 ), "
				+ "`sent` VARCHAR( 50 ), `encrypted` VARCHAR( 1 ), `transport` VARCHAR( 1 ), `system` VARCHAR( 1 ), `prio` INTEGER, `smsfailcnt` INTEGER   );";
		db.execSQL(CREATE_TABLE_MSGS);
	}

	public static void initializeDBSent(Context context, SQLiteDatabase db) {
		db.setVersion(1);
		db.setLocale(Locale.getDefault());
		db.setLockingEnabled(true);

		final String CREATE_TABLE_MSGS = "CREATE TABLE IF NOT EXISTS  `"
				+ TABLE_SENT
				+ "` ("
				+ "`mid` INTEGER PRIMARY KEY, `hostuid` INTEGER, `ts` VARCHAR( 50 ));";
		db.execSQL(CREATE_TABLE_MSGS);
	}

	// -----------------------------------------------------------------

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
							final String textMessage = "You database seems to be corrupted.\n\nYou may want to try closing the application, restart you device and try it again. If this does not help your messages may be lost and the only fix then is to recreate the database.\n\n"
									+ " Do you want to erase all messages and recreate the database?";
							new MessageAlertDialog(
									context,
									titleMessage,
									textMessage,
									" Erase All ",
									" Cancel ",
									null,
									new MessageAlertDialog.OnSelectionListener() {
										public void selected(int button,
												boolean cancel) {
											if (!cancel) {
												if (button == 0) {
													// delete
													dropDB(context, uid);
													SQLiteDatabase db = openDB(
															context, uid);
													initializeDB(context, db);
													db.close();
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

	public static void rebuildDBSending(final Context context) {
		// delete
		dropDBSending(context);
		SQLiteDatabase db = openDBSending(context);
		initializeDBSending(context, db);
		db.close();
	}

	// ------------------------------------------------------------------------
	private static void rebuildDBSent(final Context context) {
		// delete
		dropDBSent(context);
		SQLiteDatabase db = openDBSent(context);
		initializeDBSent(context, db);
		db.close();
	}

	// ------------------------------------------------------------------------

	// This is called once on startup and after adding new users (before
	// inserting new messages)
	public static void ensureDBInitialized(Context context,
			List<Integer> uidList) {
		try {
			// the following will not harm existing tables!
			for (int uid : uidList) {
				SQLiteDatabase db = openDB(context, uid);
				initializeDB(context, db);
				db.close();
			}
			SQLiteDatabase db = openDBSending(context);
			initializeDBSending(context, db);
			db.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
		
	}

	// ------------------------------------------------------------------------
	// ------------------------------------------------------------------------

	// If a message is possibly not yet sent, we can try to withdraw it BEFORE
	// it got sent!
	public static boolean withdrawFromSending(Context context, int localId) {
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

	public static void removeMappingByMid(Context context, int mid) {
		SQLiteDatabase db = openDBSent(context);
		db.delete(TABLE_SENT, "`mid` = " + mid, null);
		db.close();
	}

	public static void removeMappingByHostUid(Context context, int hostUid) {
		SQLiteDatabase db = openDBSent(context);
		db.delete(TABLE_SENT, "`hostuid` = " + hostUid, null);
		db.close();
	}

	// --------------------------------------------

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
		return returnUid;
	}

	// priority 0 == read confirmation, 1 == normal messages, 2 == keys
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
	// ------------------------------------------------------------------------

	public static boolean removeSentMessage(Context context, int sendingId) {
		SQLiteDatabase db = openDBSending(context);
		int i = db.delete(TABLE_SENDING, "`sendingid` = " + sendingId, null);
		db.close();
		return i > 0;
	}

	// ------------------------------------------------------------------------

	public static boolean addSendMessage(final Context context, int hostUid,
			String text, boolean encrypted, int transport, boolean system,
			int priority) {
		return addSendMessage(context, hostUid, text, encrypted, transport,
				system, priority, null);
	}

	// priority 0 == read confirmation, 1 == normal messages, 2 == keys
	public static boolean addSendMessage(final Context context, int hostUid,
			String text, boolean encrypted, int transport, boolean system,
			int priority, ConversationItem item) {

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

		String messageTextToShow = text;
		boolean systemToShow = system;
		if (priority == DB.PRIORITY_KEY) {
			Log.d("communicator", "#### KEY NEW KEY #6.5");
			// Exchange key message text (make it a little bit more readable...
			// we do not need the
			// original key message text locally, we allready stored to AES key
			// in our device!
			String keyhash = Setup.getAESKeyHash(context, hostUid);
			String keyText = "[ session key " + keyhash + " sent ]";
			messageTextToShow = keyText;
			systemToShow = false;
		}

		Log.d("communicator", "#### KEY NEW KEY #6.6");

		// add the message or real key here
		int localId = addMessage(context, DB.myUid(context), hostUid,
				messageTextToShow, created, null, null, null, null, encrypted,
				transport, systemToShow);

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
			values.put("fromuid", DB.myUid(context));
			values.put("touid", hostUid);
			values.put("transport", transport);
			values.put("text", text);
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
			SQLiteDatabase db = null;
			try {
				db = openDBSending(context);
				long rowId = db.insertOrThrow(DB.TABLE_SENDING, null, values);
				if (rowId > -1) {
					// valid insert
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

	// Returns the localId of the inserted message on success or -1 if failed.
	public static int addMessage(Context context, int from, int to,
			String text, String created, String sent, String received,
			String read, String withdraw, boolean encrypted, int transport,
			boolean system) {
		int uid = from;
		if (to != DB.myUid(context)) {
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
		if (withdraw != null) {
			values.put("withdraw", withdraw);
		} else {
			values.put("withdraw", "");
		}
		if (system) {
			values.put("system", "1");
		} else {
			values.put("system", "0");
		}
		try {
			SQLiteDatabase db = openDB(context, uid);
			long rowId = db.insert(DB.TABLE_MESSAGES, null, values);
			if (rowId > -1) {
				// valid insert
				String QUERY = "SELECT `localid`  FROM `" + TABLE_MESSAGES
						+ "` WHERE ROWID = " + rowId;
				// Log.d("communicator", "QUERY = " + QUERY);
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

	public static void cleanupDBSending(Context context) {
		SQLiteDatabase db = openDBSending(context);
		db.delete(
				TABLE_SENDING,
				"`fromuid` == -1 OR `touid` = -1 OR  `text` = '' OR `created` = '-1'",
				null);
		db.close();
	}

	public static void cleanupDB(Context context, int uid) {
		SQLiteDatabase db = openDB(context, uid);
		db.delete(
				TABLE_MESSAGES,
				"`fromuid` == -1 OR `touid` = -1 OR  `text` = '' OR `created` = '-1'",
				null);
		db.close();
	}

	// -----------------------------------------------------------------

	public static void printDB(Context context, int uid) {
		// cleanupDB(context);
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
					entry += name + "=" + val + ", ";
				}
				Log.d("communicator", "DBTABLE [" + uid + "] " + entry);
				cursor.moveToNext();
			}
			cursor.close();
		}
		db.close();
	}

	public static String printDBSending(Context context) {
		// public static void printDB(Context context, int uid) {
		// cleanupDB(context);
		// SQLiteDatabase db = openDB(context, uid);
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
				String returnStringLine ="DBTABLE [SENDING]" + entry; 
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

	private static int cachedConversationSize = -1;
	
	// gets the COMPLETE size of all messages in DB
	public static int getConversationSize(Context context, int hostUid, boolean forceRefresh) {
		if ((cachedConversationSize == -1) || forceRefresh) {
			SQLiteDatabase db = openDB(context, hostUid);


			try {
				String QUERY = "SELECT `localid`, `mid`, `fromuid`, `touid`, `text`, `created`, `sent`, `received` , `read`, `withdraw`, `encrypted`, `transport`, `system` FROM `"
						+ TABLE_MESSAGES
						+ "` WHERE ((`fromuid` = "
						+ myUid(context)
						+ " AND `touid` = "
						+ hostUid
						+ ") OR (`fromuid` = "
						+ hostUid
						+ " AND `touid` = "
						+ myUid(context)
						+ ")) AND `system` != '1' ORDER BY `created` DESC, `sent` DESC "; // AND
																							// `system`
																							// !=
																							// '1'			;
				Cursor cursor = db.rawQuery(QUERY, null);
				if (cursor != null && cursor.moveToFirst()) {
					cachedConversationSize = cursor.getCount();
					cursor.close();
				}
			} catch (Exception e) {
				// if anything goes wrong loading the conversation, clear the table
				askToRebuildDB(context, hostUid);
			}
			db.close();
		}
		return cachedConversationSize;
	}
	
	//-------------------------------------------------------------------------

	// Attention: system messages will NOT be loaded
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
			String QUERY = "SELECT `localid`, `mid`, `fromuid`, `touid`, `text`, `created`, `sent`, `received` , `read`, `withdraw`, `encrypted`, `transport`, `system` FROM `"
					+ TABLE_MESSAGES
					+ "` WHERE ((`fromuid` = "
					+ myUid(context)
					+ " AND `touid` = "
					+ hostUid
					+ ") OR (`fromuid` = "
					+ hostUid
					+ " AND `touid` = "
					+ myUid(context)
					+ ")) AND `system` != '1' ORDER BY `created` DESC, `sent` DESC " // AND
																						// `system`
																						// !=
																						// '1'
					+ LIMIT;
			Log.d("communicator", "loadConversation() QUERY = " + QUERY);

			Cursor cursor = db.rawQuery(QUERY, null);
			if (cursor != null && cursor.moveToFirst()) {
				Log.d("communicator",
						"loadConversation() getCount = " + cursor.getCount());

				for (int c = 0; c < cursor.getCount(); c++) {

					int localid = Utility.parseInt(cursor.getString(0), -1);
					int mid = Utility.parseInt(cursor.getString(1), -1);
					int from = Utility.parseInt(cursor.getString(2), -1);
					int to = Utility.parseInt(cursor.getString(3), -1);
					String text = cursor.getString(4);

					long created = parseTimestamp(cursor.getString(5), -1);
					long sent = parseTimestamp(cursor.getString(6), -1);

					boolean smsfailed = cursor.getString(6).equals(
							DB.SMS_FAILED);

					long received = parseTimestamp(cursor.getString(7), -1);
					long read = parseTimestamp(cursor.getString(8), -1);
					long withdraw = parseTimestamp(cursor.getString(9), -1);
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
						item.withdraw = withdraw;
						item.encrypted = encrypted;
						item.transport = transport;
						item.system = system;
						item.smsfailed = smsfailed;
						// We must add to position 0 (zero) because we have DESC
						// ordering (for the limit)
						conversationList.add(0, item);
					}
					cursor.moveToNext();
				}
				cursor.close();
			}
		} catch (Exception e) {
			// if anything goes wrong loading the conversation, clear the table
			askToRebuildDB(context, hostUid);
		}
		db.close();
		return;
	}

	// -----------------------------------------------------------------

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

	// static String getLargestTimestampWithdraw(Context context) {
	// String returnTimestamp = "";
	// SQLiteDatabase db = openDB(context);
	//
	// String QUERY = "SELECT `withdraw` FROM `" + TABLE_MESSAGES
	// + "` WHERE `touid` = " + DB.myUid(context)
	// + " ORDER BY `withdraw` DESC";
	//
	// Cursor cursor = db.rawQuery(QUERY, null);
	// if (cursor != null && cursor.moveToFirst()) {
	// if (cursor.getCount() > 0) {
	// returnTimestamp = cursor.getString(0);
	// }
	// cursor.close();
	// }
	// db.close();
	// return returnTimestamp;
	// }

	// -----------------------------------------------------------------

	static long largestTimestampReceived = -1;

	static long getLargestTimestampReceived(Context context) {
		if (largestTimestampReceived == -1) {
			largestTimestampReceived = Utility.loadLongSetting(context,
					Setup.SETTINGS_LARGEST_TS_RECEIVED, DB.getTimestamp());
		}
		return largestTimestampReceived;
	}

	static void updateLargestTimestampReceived(Context context, String newTS) {
		Log.d("communicator", " UPDATE LARGEST TIMESTAMP RECEIVED: " + newTS);
		long newTSLong = Utility.parseLong(newTS, 0);
		if (newTSLong > getLargestTimestampReceived(context)) {
			Utility.saveLongSetting(context,
					Setup.SETTINGS_LARGEST_TS_RECEIVED, newTSLong);
			largestTimestampReceived = newTSLong;
		}
	}

	// -----------------------------------------------------------------

	static long largestTimestampRead = -1;

	static long getLargestTimestampRead(Context context) {
		if (largestTimestampRead == -1) {
			largestTimestampRead = Utility.loadLongSetting(context,
					Setup.SETTINGS_LARGEST_TS_READ, DB.getTimestamp());
		}
		return largestTimestampRead;
	}

	static void updateLargestTimestampRead(Context context, String newTS) {
		Log.d("communicator", " UPDATE LARGEST TIMESTAMP READ: " + newTS);
		long newTSLong = Utility.parseLong(newTS, 0);
		if (newTSLong > getLargestTimestampRead(context)) {
			Utility.saveLongSetting(context, Setup.SETTINGS_LARGEST_TS_READ,
					newTSLong);
			largestTimestampRead = newTSLong;
		}
	}

	// -----------------------------------------------------------------

	// static String getLargestTimestampReceived(Context context) {
	// String returnTimestamp = "";
	// SQLiteDatabase db = openDB(context);
	//
	// String QUERY = "SELECT `received` FROM `" + TABLE_MESSAGES
	// + "` WHERE `fromuid` = " + DB.myUid(context)
	// + " AND `transport` = 0" + " ORDER BY `received` DESC";
	// // Log.d("communicator", "LARGEST TIMESTAMP RECEIVED QUERY = " + QUERY);
	//
	// Cursor cursor = db.rawQuery(QUERY, null);
	// if (cursor != null && cursor.moveToFirst()) {
	// if (cursor.getCount() > 0) {
	// returnTimestamp = cursor.getString(0);
	// // Log.d("communicator",
	// // "LARGEST TIMESTAMP RECEIVED returnTimestamp = "
	// // + returnTimestamp);
	// }
	// cursor.close();
	// }
	// db.close();
	// return returnTimestamp;
	// }

	// -----------------------------------------------------------------

	// static String getLargestTimestampRead(Context context) {
	// String returnTimestamp = "";
	// SQLiteDatabase db = openDB(context);
	//
	// String QUERY = "SELECT `read` FROM `" + TABLE_MESSAGES
	// + "` WHERE `fromuid` = " + DB.myUid(context)
	// + " AND `transport` = 0" + " ORDER BY `read` DESC";
	// // Log.d("communicator", "LARGEST TIMESTAMP READ QUERY = " + QUERY);
	//
	// Cursor cursor = db.rawQuery(QUERY, null);
	// if (cursor != null && cursor.moveToFirst()) {
	// if (cursor.getCount() > 0) {
	// returnTimestamp = cursor.getString(0);
	// }
	// cursor.close();
	// }
	// db.close();
	// return returnTimestamp;
	// }

	// -----------------------------------------------------------------

	static int getLargestMidForUIDExceptSystemMessages(Context context, int uid) {
		// To filter system messages, just skip blank messages, as a convention
		// system messages are cleared
		int mid = -1;
		SQLiteDatabase db = openDB(context, uid);

		String QUERY = "SELECT `mid` FROM `" + TABLE_MESSAGES
				+ "` WHERE `touid` = " + DB.myUid(context)
				+ " AND `fromuid` = " + uid
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

	static void resetLargestMid(Context context, int mid) {
		// Log.d("communicator",
		// "@@@@@@@ RESET LARGEST MID: " + mid);
		lastReceivedMid = mid;
		Utility.saveIntSetting(context, Setup.SETTINGS_LARGEST_MID_RECEIVED,
				mid);
	}

	static boolean updateLargestMid(Context context, int mid) {
		// Log.d("communicator",
		// "@@@@@@@ UPDATE LARGEST MID: " + mid);
		int currentMid = getLargestMid(context);
		if (mid > currentMid) {
			resetLargestMid(context, mid);
			lastReceivedMid = mid;
			return true;
		}
		return false;
	}

	static int getLargestMid(Context context) {
		// Cache to make it more efficient, be sure to invalidate if receiving
		// new messages -- this is also used for clearing conversation!!!
		if (lastReceivedMid == -1) {
			lastReceivedMid = Utility.loadIntSetting(context,
					Setup.SETTINGS_LARGEST_MID_RECEIVED, -1);
		}
		// Log.d("communicator",
		// "@@@@@@@ GET LARGEST MID: " + lastReceivedMid);
		return lastReceivedMid;
	}

	// -----------------------------------------------------------------

	// static boolean nextMessageSent(Context context, ConversationItem)

	// Gets the next message to send from DB
	static ConversationItem getNextMessage(Context context, int transport) {
		ConversationItem returnItem = null;

		String transportQueryPart = " AND `transport` = " + transport;
		if (transport == -1) {
			transportQueryPart = "";
		}

		SQLiteDatabase db = openDBSending(context);

		// + "`localid` INTEGER PRIMARY KEY, `fromuid` INTEGER , "
		// + "`touid` INTEGER , `text` VARCHAR( 2000 ) , "
		// + "`created` VARCHAR( 50 ), "
		// +
		// "`sent` VARCHAR( 50 ), `encrypted` VARCHAR( 1 ), `transport` VARCHAR( 1 ), `system` VARCHAR( 1 )  );";

		// for sending
		String QUERY = "SELECT `sendingid`, `localid`, `fromuid`, `touid`, `text`, `created`, `encrypted`, `transport`, `system`, `smsfailcnt`  FROM `"
				+ TABLE_SENDING
				+ "` WHERE `touid` != -1 AND `localid` != -1 AND `smsfailcnt` <= "
				+ Setup.SMS_FAIL_CNT
				+ " "
				+ transportQueryPart
				+ " ORDER BY `prio` DESC, `created` ASC";
		// The priority ensures that first KEYs are sent, THEN messages and only
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
				}
			}
			cursor.close();
		}
		db.close();
		return returnItem;
	}

	// -----------------------------------------------------------------

	// Gets the message with local id. If localid != -1 then transport does not
	// matter! If
	// transport == -1 then transport does not matter!
	static ConversationItem getMessage(Context context, int localid, int uid) {
		ConversationItem returnItem = null;

		SQLiteDatabase db = openDB(context, uid);

		// for display specific message
		String QUERY = "SELECT `localid`, `mid`, `fromuid`, `touid`, `text`, `created`, `sent`, `received` , `read`, `withdraw`, `encrypted`, `transport`, `system` FROM `"
				+ TABLE_MESSAGES + "` WHERE `localid` = " + localid;

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
				long withdraw = parseTimestamp(cursor.getString(9), -1);
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
					returnItem.withdraw = withdraw;
					returnItem.encrypted = encrypted;
					returnItem.transport = transport2;
					returnItem.system = system;
					returnItem.smsfailed = smsfailed;
					returnItem.isKey = text.startsWith("K");
				}
			}
			cursor.close();
		}
		db.close();
		return returnItem;
	}

	// -----------------------------------------------------------------

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
	//
	// static int getHostUidByMid(Context context, int mid) {
	// int toUid = -1;
	// SQLiteDatabase db = openDB(context);
	//
	// String QUERY = "SELECT `touid` FROM `" + TABLE_MESSAGES
	// + "` WHERE `mid` = " + mid;
	// // Log.d("communicator", "LARGEST MID QUERY = " + QUERY);
	// Cursor cursor = db.rawQuery(QUERY, null);
	// if (cursor != null && cursor.moveToFirst()) {
	// if (cursor.getCount() > 0) {
	// toUid = Utility.parseInt(cursor.getString(0), -1);
	// }
	// cursor.close();
	// }
	// db.close();
	// return toUid;
	// }

	// -----------------------------------------------------------------

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

	public static boolean updateOwnMessageRead(Context context, int hostUid) {
		boolean success = false;
		ContentValues values = new ContentValues();
		values.put("read", DB.getTimestampString());
		// update
		try {
			SQLiteDatabase db = openDB(context, hostUid);
			// all messages that I have received, I flag in my own DB as read
			// now that I am reading..
			db.update("messages", values, "`touid` = " + DB.myUid(context)
					+ " AND `fromuid` != " + DB.myUid(context), null);
			db.close();
			success = true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return success;
	}

	// -----------------------------------------------------------------
	// -----------------------------------------------------------------

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

	// This message will try to withdraw unsent messages, if this succeeds, it
	// will
	// withdraw right away. Otherwise we send a withdraw request to the server
	// and
	// wait for the server response message!
	public static void tryToWithdrawMessage(Context context, int mid,
			int localid, String timestamp, int hostUid) {
		// if mid < 0 it is still a localid, but this does not mean it is not
		// sent yet!
		boolean withdrawBeforeSending = withdrawFromSending(context, localid);
		if (withdrawBeforeSending) {
			Log.d("communicator", " WITHDRAW BEFOR SENDING POSSIBLE :-)");
			ContentValues values = new ContentValues();
			values.put("text", DB.WITHDRAWNTEXT);
			values.put("withdraw", timestamp);
			// update
			try {
				SQLiteDatabase db = openDB(context, hostUid);
				int rows1 = db.update("messages", values, "mid = " + mid
						+ " AND sent", null);
				int rows2 = db.update("messages", values, "localid = "
						+ localid, null);
				// try both
				Conversation.setWithdrawInConversation(context, mid);
				Conversation.setWithdrawInConversation(context, -1 * localid);
				db.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			Log.d("communicator", " WITHDRAW BEFORE SENDING NOT POSSIBLE :-(");
		}
		if (mid > -1) {
			// Send a withdraw request just to make sure, even if we could
			// possibly remove it
			// from the sending queue it may just have been sent!
			Communicator.sendSystemMessageWidthdraw(context, hostUid, mid);
		}
	}

	// -----------------------------------------------------------------

	public static boolean updateMessageWithdrawn(Context context, int mid,
			String timestamp, int hostUid) {
		boolean success = false;
		ContentValues values = new ContentValues();
		values.put("text", DB.WITHDRAWNTEXT);
		values.put("withdraw", timestamp);
		// update
		try {
			SQLiteDatabase db = openDB(context, hostUid);
			int rows = db.update("messages", values, "mid = " + mid, null);
			// Log.d("communicator", "UPDATE WITHDRAW OF MID " + mid + "= " +
			// timestamp +": " + rows);
			db.close();
			success = true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return success;
	}

	// -----------------------------------------------------------------

	public static boolean updateMessageSystem(Context context, int mid,
			boolean isSystemMessage, int hostUid) {
		boolean success = false;
		ContentValues values = new ContentValues();
		if (isSystemMessage) {
			values.put("system", "1");
		} else {
			values.put("system", "0");
		}
		// update
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

	public static boolean isMessageFailed(String sentTimestamp) {
		return (sentTimestamp.equals(SMS_FAILED));
	}

	public static boolean updateMessageFailed(Context context, int localid,
			int hostUid) {
		boolean success = false;
		ContentValues values = new ContentValues();
		values.put("sent", SMS_FAILED);
		// update
		try {
			SQLiteDatabase db = openDB(context, hostUid);
			int rows = db.update(TABLE_MESSAGES, values,
					"localid = " + localid, null);
			Log.d("communicator", "SET PERMENANT FAILED OF localid " + localid
					+ " of user " + hostUid);
			db.close();
			success = true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return success;
	}

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

	public static boolean incrementFailed(Context context, int localid,
			int hostUid) {
		int failcnt = getfailed(context, localid);
		failcnt++;
		boolean permamentFailed = false;
		ContentValues values = new ContentValues();
		values.put("smsfailcnt", failcnt);
		// update
		try {
			SQLiteDatabase db = openDBSending(context);
			int rows = db.update(TABLE_SENDING, values, "localid = " + localid,
					null);
			Log.d("communicator", "INCREMENTD FAILED OF localid " + localid
					+ " to " + failcnt);
			db.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		// if failed too often, then flag as failed in DB_MESSAGES
		if (failcnt > Setup.SMS_FAIL_CNT) {
			permamentFailed = updateMessageFailed(context, localid, hostUid);
		}
		return permamentFailed;
	}

	// -----------------------------------------------------------------

	public static boolean updateMessageReceived(Context context, int mid,
			String timestamp, int hostUid) {
		boolean success = false;
		ContentValues values = new ContentValues();
		values.put("received", timestamp);
		// update
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

	public static boolean isAlreadyInDB(Context context, int mid, int hostUid) {
		if (mid == SMS_MID) {
			// incoming SMS have this mid -1
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

	public static boolean updateMessage(Context context,
			ConversationItem itemToUpdate, int hostUid) {
		return updateMessage(context, itemToUpdate, hostUid, false);
	}

	public static boolean updateMessage(Context context,
			ConversationItem itemToUpdate, int hostUid, boolean isSentKeyMessage) {
		boolean success = false;
		ContentValues values = new ContentValues();
		values.put("mid", itemToUpdate.mid);
		values.put("fromuid", itemToUpdate.from);
		values.put("touid", itemToUpdate.to);
		if (!isSentKeyMessage) {
			values.put("text", itemToUpdate.text);
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
		values.put("withdraw", itemToUpdate.withdraw + "");
		values.put("transport", itemToUpdate.transport + "");
		if (itemToUpdate.encrypted) {
			values.put("encrypted", "E");
		} else {
			values.put("encrypted", "U");
		}
		int localid = itemToUpdate.localid;
		if (localid >= 0) {
			// update
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
			// create new item
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

	// public static Pair<String, String> getLastMessage(Context context, int
	// uid) {
	// SQLiteDatabase db = openDB(context, uid);
	//
	// String returnText = "";
	// String returnDate = "";
	// Cursor cursor = null;
	//
	// try {
	// // String QUERY = "SELECT `mid`, `text`, `sent` FROM `"
	// // + TABLE_MESSAGES + "` WHERE ((`fromuid` = " + uid
	// // + " AND `touid` = " + DB.myUid(context)
	// // + ")  OR  (`fromuid` = " + DB.myUid(context)
	// // + " AND `touid` = " + uid + ")) ORDER BY `sent` DESC";
	//
	// String QUERY = "SELECT `mid`, `text`, `sent` FROM `"
	// + TABLE_MESSAGES + "` ORDER BY `sent` DESC";
	//
	// // Log.d("communicator", "GET LAST MESSAGE QUERY = " + QUERY);
	//
	// cursor = db.rawQuery(QUERY, null);
	// if (cursor != null && cursor.moveToFirst()) {
	// // Log.d("communicator", "getCount = " + cursor.getCount());
	// if (cursor.getCount() > 0) {
	// String text = null;
	// boolean haveNextEntry = true;
	// do {
	// text = cursor.getString(1);
	// Log.d("communicator", "GET LAST MESSAGE RESULT TEXT: '" + text + "'");
	// long sent = parseTimestamp(cursor.getString(2), -1);
	// returnDate = DB.getDateString(sent, false);
	// returnText = text;
	// if ((text == null || text.length() < 1)) {
	// haveNextEntry = cursor.moveToNext();
	// }
	// } while ((text == null || text.length() < 1) && haveNextEntry);
	// Log.d("communicator", "GET LAST MESSAGE RESULT TEXT: ==> '" + text +
	// "'");
	// }
	// cursor.close();
	// }
	// } catch (Exception e) {
	// if (cursor != null) {
	// cursor.close();
	// }
	// }
	// db.close();
	// return new Pair(returnText, returnDate);
	// }

	// ------------------------------------------------------------------------

	public static void deleteUser(Context context, int uid) {
		context.deleteDatabase(Setup.DATABASEPREFIX + uid
				+ Setup.DATABASEPOSTFIX);
		// SQLiteDatabase db = openDB(context, uid);
		// db.delete(TABLE_MESSAGES,
		// "(`fromuid` = " + uid + " AND `touid` = " + DB.myUid(context)
		// + ")  OR  (`fromuid` = " + DB.myUid(context)
		// + " AND `touid` = " + uid + ")", null);
		// db.close();
		// printDB(context);
	}

	// ------------------------------------------------------------------------

	// -----------------------------------------------------------------

	// public static List<Integer> sortUIDsByDB(Context context, List<Integer>
	// inputList) {
	//
	//
	// SQLiteDatabase db = openDB(context);
	// List<Integer> returnList = new ArrayList<Integer>();
	//
	// int myUid = DB.myUid(context);
	//
	// try {
	// String QUERY = "SELECT `fromuid`, `touid`, `sent` FROM `"
	// + TABLE_MESSAGES + "` ORDER BY `sent` DESC";
	// Cursor cursor = db.rawQuery(QUERY, null);
	// if (cursor != null && cursor.moveToFirst()) {
	// // Log.d("communicator", "getCount = " + cursor.getCount());
	//
	// for (int c = 0; c < cursor.getCount(); c++) {
	// int from = Utility.parseInt(cursor.getString(0), -1);
	// int to = Utility.parseInt(cursor.getString(1), -1);
	// if (from != -1) {
	// if (!returnList.contains(from) && from != myUid) {
	// returnList.add(from);
	// }
	// if (!returnList.contains(to) && to != myUid) {
	// returnList.add(to);
	// }
	// if (to == from && to == myUid) {
	// // exception if we sent us messages
	// returnList.add(to);
	// }
	// }
	// cursor.moveToNext();
	// }
	// cursor.close();
	// }
	// } catch (Exception e) {
	// e.printStackTrace();
	// // if anything goes wrong loading the conversation, clear the table
	// askToRebuildDB(context);
	// }
	// db.close();
	// return returnList;
	// }

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
	 */
	public static boolean mergeUser(Context context, int mergeFromUid,
			int mergeToUid) {
		boolean success = false;
		// update
		try {
			List<ConversationItem> conversationToMergeList = new ArrayList<ConversationItem>();
			// now move all (except system messages, these will not be loaded
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
						+ "", item.withdraw + "", item.encrypted,
						item.transport, item.system);
			}
			success = true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return success;
	}

	// -----------------------------------------------------------------

}
