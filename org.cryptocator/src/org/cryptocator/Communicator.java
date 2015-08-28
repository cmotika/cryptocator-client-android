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

import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.Cipher;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.util.Base64;
import android.util.Log;

/**
 * The Communicator class handles most of the communication including
 * sending/receiving messages and receive/read confirmations or the update of
 * account and session keys.
 * 
 * @author Christian Motika
 * @since 1.2
 * @date 08/23/2015
 * 
 */
public class Communicator {

	/**
	 * The message sent indicates that one message was sent and potentially more
	 * need to be send.
	 */
	public static boolean messageSent = false;

	/**
	 * The message received indicates that one message was received and
	 * potentially more need to be received.
	 */
	public static boolean messageReceived = false;

	/**
	 * The have new messages flag is set to true if the have-request revealed
	 * that there are messages to receive.
	 */
	public static boolean haveNewMessages = false;

	/** The Internet messages queued to be sent. */
	public static boolean messagesToSend = false;

	/** The SMS queued to be sent. */
	public static boolean SMSToSend = false;

	/**
	 * If the class is killed, this flag will be false again it will be set by
	 * the scheduler that evaluates and uses messagesToSend as a cache!
	 */
	public static boolean messagesToSendIsUpToDate = false;

	/**
	 * The connection flag tells if Internet was ok. It is evaluated in Main for
	 * the info message.
	 */
	public static boolean internetOk = true;

	/**
	 * The connection flag tells if Login was ok. It is evaluated in Main for
	 * the info message.
	 */
	public static boolean loginOk = true;

	/**
	 * The connection flag tells if the account is not activated. It is
	 * evaluated in Main for the info message
	 */
	public static boolean accountNotActivated = false;

	/**
	 * This flag is a tie-braker and gives alternating priority to Internet and
	 * SMS messages if BOTH have to be sent! This way one of both cannot block
	 * the other.
	 */
	public static boolean lastSendInternet = false;

	/** The key ok separator indicator. */
	public static String KEY_OK_SEPARATOR = "@";

	/**
	 * The key error separator indicator. If the key is separated by this
	 * separator then the key was sent because a messsage could not be
	 * decrypted. We inform the user and ask him or her to re-send the last
	 * message.
	 */
	public static String KEY_ERROR_SEPARATOR = "@@";

	public static String NEWLINEESCAPE = "@@@NEWLINE@@@";
	public static String HASHESCAPE = "@@@HASH@@@";

	// ----------------------------------------------------------------------------------

	/**
	 * This method should help to save communication, it quickly checks for new
	 * messages and only if there are messages waiting it triggeres the receive
	 * next message.
	 * 
	 * @param context
	 *            the context
	 */
	public static void haveNewMessagesAndReceive(final Context context) {
		if (haveNewMessages) {
			// If we already know we have new messages, we not need to run the
			// light-weight have-request!
			receiveNextMessage(context);
			return;
		}

		// Largest timestamp received
		final int largestMid = DB.getLargestMid(context);

		String sessionid = Setup.getSessionID(context);

		String url = null;
		url = Setup.getBaseURL(context)
				+ "cmd=have&session="
				+ sessionid
				+ "&val="
				+ Utility.urlEncode(largestMid + "#"
						+ DB.getLargestTimestampReceived(context) + "#"
						+ DB.getLargestTimestampRead(context));

		Log.d("communicator", "REQUEST HAVE: " + url);
		final String url2 = url;
		@SuppressWarnings("unused")
		HttpStringRequest httpStringRequest = (new HttpStringRequest(context,
				url2, new HttpStringRequest.OnResponseListener() {
					public void response(String response) {
						if (isResponseValid(response)) {
							internetOk = true;
							final String response2 = response;
							// Log.d("communicator",
							// "RECEIVED NEW MESSAGES WAITING: "
							// + response2);
							// Log.d("communicator", "RESPONSE HAVE: " +
							// response2);
							if (response.equals("-1")) {
								// enforce new session if this happens
								// twice
								Setup.possiblyInvalidateSession(context, false);
							} else if (response2.startsWith("2#")) {
								Setup.possiblyInvalidateSession(context, true); // reset
																				// (everythin
																				// ok/normal)
								// largestMid too high, reduce!
								String content = Communicator
										.getResponseContent(response2);
								if (content != null && content.length() > 0) {
									int midFromServer = Utility.parseInt(
											content, largestMid);
									Log.d("communicator",
											"RESPONSE HAVE SET - TOO HIGH MID: "
													+ midFromServer);
									DB.resetLargestMid(context, midFromServer);
								}
							} else {
								// NORMAL PROCESSING - 0##...##... or
								// 1##...##...
								String responseTail = response2.substring(3);
								Log.d("communicator",
										"RESPONSE HAVE PROCESS TAIL: "
												+ responseTail);
								handleReadAndReceived(context, responseTail);
								if (response2.startsWith("1##")) {
									Setup.possiblyInvalidateSession(context,
											true); // reset (everythin
													// ok/normal)
									haveNewMessages = true;
									if (!(Conversation.isVisible() && Conversation
											.isTyping())) {
										receiveNextMessage(context);
									}
								}
							}
						} else {
							internetOk = false;
						}
					}
				}));
	}

	// ----------------------------------------------------------------------------------

	/**
	 * Handle read and received.
	 * 
	 * @param context
	 *            the context
	 * @param response
	 *            the response
	 */
	private static void handleReadAndReceived(final Context context,
			String response) {

		String[] values = response.split("##");
		if (values.length == 2) {
			String partReceived = values[0];
			String partRead = values[1];

			String[] valuesReceived = partReceived.split("#");
			for (String valueReceived : valuesReceived) {
				String midAndTs[] = valueReceived.split("@");
				if (midAndTs.length == 2) {
					int mid = Utility.parseInt(midAndTs[0], -1);
					int senderUid = DB.getHostUidForMid(context, mid);
					String ts = midAndTs[1];
					DB.updateLargestTimestampReceived(context, ts);
					if (mid != -1 && senderUid != -1) {
						DB.updateMessageReceived(context, mid, ts, senderUid);
						updateSentReceivedReadAsync(context, mid, senderUid,
								false, true, false, false);
					}
				}
			}

			String[] valuesRead = partRead.split("#");
			for (String valueRead : valuesRead) {
				String midAndTs[] = valueRead.split("@");
				if (midAndTs.length == 2) {
					int mid = Utility.parseInt(midAndTs[0], -1);
					int senderUid = DB.getHostUidForMid(context, mid);
					DB.removeMappingByMid(context, mid);
					String ts = midAndTs[1];
					DB.updateLargestTimestampRead(context, ts);
					if (mid != -1 && senderUid != -1) {
						DB.updateMessageRead(context, mid, ts, senderUid);
						// FIXME: THIS SEEMS ULTRA-WRONG???? OLD CODE???
						// DB.updateMessageSystem(context, mid, true,
						// senderUid);
						updateSentReceivedReadAsync(context, mid, senderUid,
								false, false, true, false);
					}
				}
			}
		}
	}

	// ----------------------------------------------------------------------------------

	/**
	 * Update sent received read async.
	 * 
	 * @param context
	 *            the context
	 * @param mid
	 *            the mid
	 * @param hostUid
	 *            the host uid
	 * @param sent
	 *            the sent
	 * @param received
	 *            the received
	 * @param read
	 *            the read
	 * @param withdraw
	 *            the withdraw
	 */
	public static void updateSentReceivedReadAsync(final Context context,
			final int mid, final int hostUid, final boolean sent,
			final boolean received, final boolean read, final boolean withdraw) {
		final Handler mUIHandler = new Handler(Looper.getMainLooper());
		mUIHandler.post(new Thread() {
			@Override
			public void run() {
				super.run();
				final Handler handler = new Handler();
				handler.postDelayed(new Runnable() {
					public void run() {
						// Log.d("communicator",
						// "WITHDRAW updateSentReceivedReadAsync() #1 "
						// + withdraw + ", "
						// + Conversation.getHostUid() + " == "
						// + hostUid + ", " + Conversation.active);
						if (Conversation.isAlive()
								&& (Conversation.getHostUid() == hostUid || hostUid == -1)) {
							// Log.d("communicator",
							// "WITHDRAW updateSentReceivedReadAsync() #2");

							if (read) {
								Conversation.setRead(context, mid);
							} else if (received) {
								Conversation.setReceived(context, mid);
							} else if (sent) {
								Conversation.setSent(context, mid);
							}
						}
						if (withdraw) {
							if (Conversation.getInstance() != null
									&& (Conversation.getHostUid() == hostUid || hostUid == -1)) {
								// Log.d("communicator",
								// "WITHDRAW updateSentReceivedReadAsync() #3");
								Conversation.setWithdrawInConversation(context,
										mid);
							}
							// for precaution: clear system notifications
							// for this user
							if (Utility.loadBooleanSetting(context,
									Setup.OPTION_NOTIFICATION,
									Setup.DEFAULT_NOTIFICATION)) {
								Communicator.cancelNotification(context,
										hostUid);
							}
						}
						// If message is sent, we possibly want the update the
						// userlist if it is visible!
						if (withdraw || sent) {
							if (Main.getInstance() != null) {
								// in the withdraw case we also want to update
								// the first line!
								Main.getInstance().rebuildUserlist(context,
										false);
							}
						}
					}
				}, 200);
			}
		});
	}

	// ----------------------------------------------------------------------------------

	/**
	 * Receive next message.
	 * 
	 * @param context
	 *            the context
	 */
	public synchronized static void receiveNextMessage(final Context context) {
		messageReceived = false;

		final int largestMid = DB.getLargestMid(context);
		// This should be the case for an empty database only!
		final boolean discardMessageAndSaveLargestMid = (largestMid == -1);

		String session = Setup.getTmpLoginEncoded(context);
		if (session == null) {
			// Error resume is automatically done by getTmpLogin, not logged in
			return;
		}

		String url = null;
		url = Setup.getBaseURL(context) + "cmd=receive&session=" + session
				+ "&val=" + largestMid;
		Log.d("communicator", "RECEIVE NEXT MESSAGE: " + url);
		final String url2 = url;
		@SuppressWarnings("unused")
		HttpStringRequest httpStringRequest = (new HttpStringRequest(context,
				url2, new HttpStringRequest.OnResponseListener() {
					public void response(String response) {
						boolean success1 = false;
						boolean success2 = false;
						boolean wronglyDencoded = false;
						boolean resposeError = true;
						final ConversationItem newItem = new ConversationItem();
						if (isResponseValid(response)) {
							if (response.equals("0")) {
								haveNewMessages = false;
								resposeError = false;
								loginOk = true;
								// We currently do not need to look for new
								// messages
							} else if (isResponsePositive(response)) {
								resposeError = false;
								loginOk = true;
								// Log.d("communicator",
								// "RECEIVE NEXT MESSAGE OK!!! "
								// + response);

								// Response should have the form
								// senttimestamp#mid

								// Update database
								String[] responseArray = response.split("#");
								if (responseArray.length == 6) {
									// A message was received
									String mid = responseArray[1];
									String from = Setup.decUid(context,
											responseArray[2]) + "";

									Log.d("communicator",
											"RECEIVE NEXT MESSAGE DECODED UIDS: "
													+ from + " -> me " + " : "
													+ responseArray[2]);
									if (from.equals("-2")) {
										// Enforce new session if this happens
										// twice
										wronglyDencoded = true; // we want fast
																// retry
										Setup.possiblyInvalidateSession(
												context, false);
									}

									if (!from.equals("-2")) {
										// Only not update in case or wrong
										// decoding because we want to later try
										// again!
										DB.updateLargestMid(context,
												Utility.parseInt(mid, 0));
									}

									if (from.equals("-1")) {
										// Invalid users
										// update cache
										DB.lastReceivedMid = newItem.mid;
										Utility.saveIntSetting(context,
												Setup.SETTINGS_DEFAULTMID,
												newItem.mid);
									}

									// Discard unknown or invalid users
									if (!from.equals("-2")
											&& !from.equals("-1")) {
										Setup.possiblyInvalidateSession(
												context, true); // reset
										success1 = true;
										// Uids could be recovered/decrypted
										String created = responseArray[3];
										String sent = responseArray[4];
										String text = responseArray[5];

										newItem.mid = Utility.parseInt(mid, -1);
										newItem.from = Utility.parseInt(from,
												-1);
										newItem.to = DB.myUid(context);
										newItem.created = DB
												.parseTimestamp(created);
										newItem.sent = DB.parseTimestamp(sent);
										newItem.received = DB.getTimestamp();
										newItem.transport = DB.TRANSPORT_INTERNET;

										// Update cache
										DB.lastReceivedMid = newItem.mid;
										Utility.saveIntSetting(context,
												Setup.SETTINGS_DEFAULTMID,
												newItem.mid);

										if (Conversation.isVisible()
												&& Conversation.getHostUid() == newItem.from) {
											newItem.read = DB.getTimestamp();
											;
										}

										// Skip if not in our list && ignore is
										// turned on OR if this mid is already
										// in our DB!
										boolean alreadyInDB = DB.isAlreadyInDB(
												context, newItem.mid,
												newItem.from);

										// Test if the user not exists and if we
										// want to skip unknown users
										boolean skipBecauseOfUnknownUser = false;
										List<Integer> uidList = Main
												.loadUIDList(context);
										if (!Main.alreadyInList(newItem.from,
												uidList)) {
											// The user who sent us a message is
											// not
											// in our list! What now?
											boolean ignore = Utility
													.loadBooleanSetting(
															context,
															Setup.OPTION_IGNORE,
															Setup.DEFAULT_IGNORE);
											if (ignore) {
												// The user must be added to our
												// list
												uidList.add(newItem.from);
												DB.ensureDBInitialized(context,
														uidList);
												Main.saveUIDList(context,
														uidList);
												Main.possiblyRebuildUserlistAsync(
														context, false);
											} else {
												skipBecauseOfUnknownUser = true;
											}
										}

										if (!skipBecauseOfUnknownUser
												&& !alreadyInDB
												&& !discardMessageAndSaveLargestMid) {
											newItem.text = handleReceivedText(
													context, text, newItem);
											success2 = updateUIForReceivedMessage(
													context, newItem);

											if (newItem.text == null
													|| newItem.text.equals("")) {
												// No toast or scrolling on
												// system messages
												// no notification
												success2 = false;
											}

											if (newItem.text
													.contains("[ invalid session key ")
													|| newItem.text
															.contains("[ decryption failed ]")) {
												// We should try to update the
												// public rsa key of this user
												Communicator.updateKeysFromServer(
														context,
														Main.loadUIDList(context),
														true, null);
											}
										} else {
											// Discard means no live update
											// please...
											success2 = false;
										}
									} // End uids decrypted sucessfully
								}
							} else {
								// Invalidate right away
								Setup.invalidateTmpLogin(context);
								loginOk = false;
								Log.d("communicator",
										"RECEIVE NEXT MESSAGE - LOGIN ERROR!!! "
												+ response);
							}
						}

						// Delayed recursion == try to send a message
						if (success1 || success2 || wronglyDencoded) {
							if (success2) {
								liveUpdateOrNotify(context, newItem);
							}

							// Clear errors and possible super fast reschedule
							Setup.setErrorUpdateInterval(context, false);
							Scheduler.reschedule(context, false, false,
									success2);
						} else {
							// The error case
							Scheduler.reschedule(context, true, false, false);
						}
					}
				}));
	}

	// ---------------------

	/**
	 * Update ui for received message.
	 * 
	 * @param context
	 *            the context
	 * @param newItem
	 *            the new item
	 * @return true, if successful
	 */
	public static boolean updateUIForReceivedMessage(Context context,
			ConversationItem newItem) {
		boolean success2 = false;

		// THE FOLLOWING IS DONE ALREADY IN receiveNextMessage() NOW. 25.08.15
		// AS ALSO ReceiveSMS.handleMessage would create an SMS user.
		//
		// List<Integer> uidList = Main.loadUIDList(context);
		// if (!Main.alreadyInList(newItem.from, uidList)) {
		// // The user who sent us a message is not
		// // in our list! What now?
		// boolean ignore = Utility.loadBooleanSetting(context,
		// Setup.OPTION_IGNORE, Setup.DEFAULT_IGNORE);
		// if (!ignore) {
		// // The user must be added to our
		// // list
		// uidList.add(newItem.from);
		// DB.ensureDBInitialized(context, uidList);
		// Main.saveUIDList(context, uidList);
		// Main.possiblyRebuildUserlistAsync(context, false);
		// } else {
		// skipUnknownUser = true;
		// }
		// }

		if (DB.updateMessage(context, newItem, newItem.from)) {
			messageReceived = true;
			Log.d("communicator", "RECEIVED MESSAGE IN DB NOW!!! from:"
					+ newItem.from);
			success2 = true;
		} else {
			// Log.d("communicator",
			// "RECEIVED MESSAGE NOT IN DB :( "
			// + response2);
		}
		return success2;
	}

	// ---------------------

	/**
	 * Handle received text.
	 * 
	 * @param context
	 *            the context
	 * @param text
	 *            the text
	 * @param newItem
	 *            the new item
	 * @return the string
	 */
	public static String handleReceivedText(final Context context, String text,
			final ConversationItem newItem) {
		if (text.startsWith("W")) {
			// W == withdraw
			// this is a withdraw message followed my the mid to withdraw
			String midString = text.substring(1);
			// Flag this W-Message as system message
			DB.updateMessageSystem(context, newItem.mid, true, newItem.from);
			if (midString != null & midString.length() > 0) {
				int mid = Utility.parseInt(midString, -1);
				if (mid > -1) {
					// WITHDRAWN NOTIFICATION!
					// now we have correct values and should update the
					// database
					int hostUid = newItem.from;
					newItem.system = true;
					DB.updateMessageWithdrawn(context, mid, newItem.created
							+ "", hostUid);
					Main.updateLastMessage(context, hostUid,
							"[ message withdrawn ]", DB.getTimestamp());
					// If the senderUid will always be the table/hostUid of the
					// conversation
					// where the message to withdraw resides. If we sent the
					// message then
					// we can look thi up under the mid in this table senderUid
					// in order to
					// know if we are the sender
					int senderUid = DB.getSenderUidByMid(context, mid, hostUid);
					if (senderUid == DB.myUid(context)) {
						senderUid = -1;
					}
					updateSentReceivedReadAsync(context, mid, senderUid, false,
							false, false, true);
				}
			}
			text = "";
		} else if (text.startsWith("K")) {
			// This is an RSA encrypted key message!
			text = text.substring(1);
			String possiblyInvalid = "";
			String keyhash = "";

			String separator = KEY_OK_SEPARATOR;
			String notificationMessageAddition = "";
			if (text.contains(KEY_ERROR_SEPARATOR)) {
				separator = KEY_ERROR_SEPARATOR;
				notificationMessageAddition = "\n\nNew key was sent automatically because last message could not be decrypted.\n\nPLEASE RESEND YOUR LAST MESSAGE!";
			}
			// Divide key and signature
			String[] values = text.split(separator);
			if (values != null && values.length == 2) {
				String encryptedKey = values[0];
				String signature = values[1];
				// Get encryptedhash from signature
				PublicKey senderPubKey = Setup.getKey(context, newItem.from);
				String decryptedKeyHashMustBe = decryptMessage(context,
						signature, senderPubKey);

				// Decrypt here
				PrivateKey myPrivateKey = Setup.getPrivateKey(context);
				text = decryptMessage(context, encryptedKey, myPrivateKey);

				// test if signature is valid
				String decryptedKeyHashIs = Utility.md5(text);
				// Log.d("communicator",
				// "@@@@@ SIGNATURE "
				// + decryptedKeyHashIs
				// + " SHOUD BE "
				// + decryptedKeyHashMustBe);

				if (decryptedKeyHashIs.equals(decryptedKeyHashMustBe)) {
					// Save as AES key for later decryptying and encrypting
					// usage
					Setup.saveAESKey(context, newItem.from, text);
					Setup.setAESKeyDate(context, newItem.from,
							DB.getTimestampString());
					keyhash = Setup.getAESKeyHash(context, newItem.from);
					// Discard the message
					Utility.showToastAsync(
							context,
							"Received new session key "
									+ keyhash
									+ " from "
									+ Main.UID2Name(context, newItem.from,
											false) + ".");
				} else {
					possiblyInvalid = "invalid ";
					Utility.showToastAsync(
							context,
							"Received invalid session key from "
									+ Main.UID2Name(context, newItem.from,
											false) + ".");
				}
			} else {
				possiblyInvalid = "invalid ";
			}
			newItem.isKey = true;
			text = "[ " + possiblyInvalid + "session key " + keyhash
					+ " received ]" + notificationMessageAddition;
			Main.updateLastMessage(context, newItem.from, text, newItem.created);
		} else if (text.startsWith("U")) {
			// This is an unencrypted message
			newItem.encrypted = false;
			text = text.substring(1);
			text = text.replace(NEWLINEESCAPE,
					System.getProperty("line.separator"));
			text = text.replace(HASHESCAPE, "#");
			Log.d("communicator", "QQQQQQ handleReceivedText #1  newItem.from="
					+ newItem.from + ", newItem.created=" + newItem.created
					+ ", text=" + text);
			Main.updateLastMessage(context, newItem.from, text, newItem.created);
		} else if (text.startsWith("E")) {
			// This is an AES encrypted message
			newItem.encrypted = true;
			text = text.substring(1);
			// Decrypt here
			Key secretKey = Setup.getAESKey(context, newItem.from);
			text = decryptAESMessage(context, text, secretKey);

			if (text == null) {
				text = "[ decryption failed ]";
				int numInvalid = Utility.loadIntSetting(context,
						"invalidkeycounter" + newItem.from, 0);
				if (numInvalid == 0) {
					text = "[ decryption failed ]\n\nAutomatically sending new session key. Other user is asked to resend his last message.";
					// ONE TIME REQUEST NEW KEY AUTOMATICALLY ... do this only
					// once because if we have the wrong RSA key this
					// might trigger a cycle forever!!!
					// DO THIS AFTER 10 Sek to allow receiving a new RSA key
					// before .. this is NOT bullet safe!
					Utility.saveIntSetting(context, "invalidkeycounter"
							+ newItem.from, 1);
					(new Thread(new Runnable() {
						public void run() {
							try {
								Thread.sleep(7000);
							} catch (InterruptedException e) {
							}
							Communicator.getAESKey(context, newItem.from, true,
									newItem.transport, true, null, false);
						}
					})).start();
				}
				Main.updateLastMessage(context, newItem.from, text,
						newItem.created);
			} else {
				Log.d("communicator",
						"QQQQQQ handleReceivedText #2  newItem.from="
								+ newItem.from + ", newItem.created="
								+ newItem.created + ", text=" + text);
				Main.updateLastMessage(context, newItem.from, text,
						newItem.created);
				// Reset because no error!
				Utility.saveIntSetting(context, "invalidkeycounter"
						+ newItem.from, 0);
			}

			// PrivateKey myPrivateKey = Setup
			// .getPrivateKey(context);
			// text = decryptMessage(context, text,
			// myPrivateKey);
		}
		return text;
	}

	// ---------------------

	/**
	 * Live update or notify.
	 * 
	 * @param context
	 *            the context
	 * @param newItem
	 *            the new item
	 */
	public static void liveUpdateOrNotify(final Context context,
			final ConversationItem newItem) {
		Log.d("communicator",
				"@@@@ liveUpdateOrNotify() POSSIBLY CREATE NOTIFICATION #1 "
						+ newItem.from);

		final Handler mUIHandler = new Handler(Looper.getMainLooper());
		mUIHandler.post(new Thread() {
			@Override
			public void run() {
				super.run();
				final Handler handler = new Handler();
				handler.postDelayed(new Runnable() {
					public void run() {
						// Live - update if possible,
						// otherwise create notification!
						if (Conversation.isVisible()
								&& Conversation.getHostUid() == newItem.from) {
							// The conversation is currently
							// open, try to update this
							// right away!
							//
							// ATTENTION: If not scrolled down then send an
							// additional toast!
							if (!Conversation.scrolledDown && !newItem.isKey) {
								if (newItem.transport == DB.TRANSPORT_INTERNET) {
									Utility.showToastShortAsync(context,
											"New message " + newItem.mid
													+ " received.");
								} else {
									Utility.showToastShortAsync(context,
											"New SMS received.");
								}
							}
							Conversation.getInstance().updateConversationlist(
									context);
						} else {
							// The conversation is NOT
							// currently
							// open, we need a new
							// notification
							Log.d("communicator",
									"@@@@ liveUpdateOrNotify() POSSIBLY CREATE NOTIFICATION #2 "
											+ newItem.from);
							if (!newItem.isKey) {
								Log.d("communicator",
										"@@@@ liveUpdateOrNotify() POSSIBLY CREATE NOTIFICATION #3 "
												+ newItem.from);
								createNotification(context, newItem);
							}
							if (Conversation.isAlive()
									&& Conversation.getHostUid() == newItem.from) {
								// Still update because conversation is in
								// memory!
								// Also possibly scroll down: If the user
								// unlocks the phone,
								// and scrolledDown was true, then he expects to
								// see the
								// last/newest message!
								Conversation.getInstance()
										.updateConversationlist(context);
							}
						}

						if (Main.isVisible()) {
							Main.getInstance().rebuildUserlist(context, false);
						}
					}
				}, 200);
			}
		});
	}

	// ------------------------------------------------------------------------
	// ------------------------------------------------------------------------

	/**
	 * Gets the AES key. Returns null to indicate to abort current sending
	 * because a new key was generated and issued.
	 * 
	 * @param context
	 *            the context
	 * @param uid
	 *            the uid
	 * @param forceSendingNewKey
	 *            the force sending new key
	 * @param transport
	 *            the transport
	 * @param flagErrorMessageNotification
	 *            the flag error message notification
	 * @param item
	 *            the item
	 * @param forSending
	 *            the for sending
	 * @return the AES key
	 */
	public static Key getAESKey(Context context, int uid,
			boolean forceSendingNewKey, int transport,
			boolean flagErrorMessageNotification, ConversationItem item,
			boolean forSending) {
		// 1. Search for AES key, if no key (or outdated), then
		// a. generate one
		// b. save it for later use
		// c. sent it first in a K ey message
		// d. use the new key

		boolean outDated = Setup.isAESKeyOutdated(context, uid, forSending);
		Log.d("communicator", "#### KEY FOR " + uid + " TIMEOUT? " + outDated);

		if (!outDated && !forceSendingNewKey) {
			// Key is up-to-date (hopefully the other part has it as well!)
			Key secretKey = Setup.getAESKey(context, uid);
			Log.d("communicator",
					"#### KEY RETURNING " + uid + " : "
							+ Setup.getAESKeyHash(context, uid));
			return secretKey;
		} else {
			// Use the last received message as a random seed
			String lastMsg = Main.getLastMessage(context, uid);
			// Log.d("communicator", "###### RANDOM SEED lastMsg " + lastMsg);
			String randomSeed = lastMsg + "-" + DB.getTimestamp() + "";
			// Log.d("communicator", "###### RANDOM SEED " + randomSeed);
			Key newKey = Setup.generateAESKey(randomSeed);
			String newKeyAsString = Setup.serializeAESKey(newKey);
			// Save
			Setup.saveAESKey(context, uid, newKeyAsString);
			Setup.setAESKeyDate(context, uid, DB.getTimestampString());
			String keyhash = Setup.getAESKeyHash(context, uid);

			Log.d("communicator", "#### KEY CREATING NEW " + uid + " : "
					+ Setup.getAESKeyHash(context, uid));

			// Encrypt the key
			// boolean haveKey = Setup.haveKey(context, uid);

			Log.d("communicator", "#### KEY NEW KEY #1");

			// RSA encrypt here
			PublicKey pubKeyOther = Setup.getKey(context, uid);
			String encryptedKey = encryptMessage(context, newKeyAsString,
					pubKeyOther);

			Log.d("communicator", "#### KEY NEW KEY #2");

			// Sign the KEY
			PrivateKey myPrivateKey = Setup.getPrivateKey(context);

			Log.d("communicator", "#### KEY NEW KEY #3");

			String keyHash = Utility.md5(newKeyAsString);
			String signature = encryptMessage(context, keyHash, myPrivateKey);

			Log.d("communicator", "#### KEY NEW KEY #4");

			String separator = KEY_OK_SEPARATOR;
			if (flagErrorMessageNotification) {
				// This indicates the error!!!
				separator = KEY_ERROR_SEPARATOR;
			}

			Log.d("communicator", "#### KEY NEW KEY #5");

			String msgText = "K" + encryptedKey + separator + signature;

			Log.d("communicator", "#### KEY NEW KEY #6");

			// Sent a KEY-Message via the same transport as the original message
			// should go!
			DB.addSendMessage(context, uid, msgText, false, transport, true,
					DB.PRIORITY_KEY, item);

			Conversation.updateConversationlistAsync(context);

			Log.d("communicator", "#### KEY NEW KEY #7");

			Communicator.sendNewNextMessageAsync(context, transport);

			Log.d("communicator", "#### KEY NEW KEY #8");

			Log.d("communicator", "#### KEY NEW KEY NOW ADDED! RETURNING NULL");

			// SendMessage(context, uid, msgText, DB.getTimestamp() - 1000,
			// null,
			// transport);
			if (transport == DB.TRANSPORT_INTERNET) {
				Utility.showToastAsync(context, "Sending new session "
						+ keyhash + " key...");
			} else {
				Utility.showToastAsync(context, "Sending new session key "
						+ keyhash + " via SMS...");
			}

			// ATTENTION:
			// Sending the RSA encrypted KEY-Message before sending the normally
			// created message ensures that
			// the symmetric key is received BEFORE the other side tries to
			// decrypt (possibly with an old key).
			// If a new key is received, any old one is discarded.

			// return null to indicate to cancel sending, we sent the key
			// instead and want to ensure that it arrives earlier than the
			// message.
			// The message is left in the send queue.
			return null;
		}
	}

	// ------------------------------------------------------------------------
	// ------------------------------------------------------------------------

	/**
	 * Send new next message. This JUST sets the flag that something is in the
	 * sending queue now. This is a hint for the Scheduler that will process the
	 * messages in the sending queue. The Scheduler is responsible for calling
	 * sendNext(). We only need to enqeue it and set the according flag. The
	 * latter is done using this method.
	 * 
	 * @param context
	 *            the context
	 * @param transport
	 *            the transport
	 */
	public static void sendNewNextMessageAsync(final Context context,
			int transport) {
		if (transport == DB.TRANSPORT_INTERNET) {
			Communicator.messagesToSend = true;
		} else {
			Communicator.SMSToSend = true;
		}
	}

	// ------------------------------------------------------------------------

	/**
	 * Send next message. This is NOT called directly but only from the
	 * Scheduler (or if the user selects REFRESH manually). This method will use
	 * the tie-braker if both types of messages are about to send.
	 * 
	 * @param context
	 *            the context
	 */
	public static void sendNextMessage(final Context context) {
		if ((Conversation.isVisible() && Conversation.isTyping())) {
			Log.d("communicator",
					"#### sendNextMessage() SKIPPED DUE TO TYPING "
							+ messagesToSend);
			// If currently typing, do nothing!
			return;
		} else {
			Log.d("communicator", "#### sendNextMessage() PROCESSING "
					+ messagesToSend);
		}

		messageSent = false;

		// This is a cached value. If a message is entered, then this flag is
		// changed!
		if (!messagesToSend && !SMSToSend) {
			Log.d("communicator",
					"#### sendNextMessage() NO MESSAGES AND NO SMS TO PROCESS... return "
							+ messagesToSend);
			return;
		}

		// Toggle next transport type to send if both types need to be send!
		// Sometimes one
		// connection is stuck, then we dont want to get stuck at all!
		int transport = DB.TRANSPORT_INTERNET;
		if (messagesToSend && SMSToSend) {
			if (!lastSendInternet) {
			} else {
				transport = DB.TRANSPORT_SMS;
			}
			lastSendInternet = !lastSendInternet;
		} else if (messagesToSend) {
			transport = DB.TRANSPORT_INTERNET;
		} else if (SMSToSend) {
			transport = DB.TRANSPORT_SMS;
		}

		// Debugging
		if (transport == DB.TRANSPORT_INTERNET) {
			Log.d("communicator", "#### SEND NEXT : TRY INTERNET MESSAGE ");
		} else {
			Log.d("communicator", "#### SEND NEXT : TRY SMS MESSAGE ");
		}

		// Lookup only if we expect something to send
		final ConversationItem itemToSend = DB.getNextMessage(context,
				transport);
		// Log.d("communicator",
		// "SEND NEXT QUERY sendNextMessage(), itemToSend = " + itemToSend);

		if (itemToSend != null) {
			Log.d("communicator",
					"#### sendNextMessage() SEND NEXT QUERY: localid="
							+ itemToSend.localid + ", to=" + itemToSend.to
							+ ", created=" + itemToSend.created + ", text="
							+ itemToSend.text + ", smsfailcnt="
							+ itemToSend.smsfailcnt);
		} else {
			Log.d("communicator",
					"#### sendNextMessage() SEND NEXT QUERY: IS NULL :(");
			if (transport == DB.TRANSPORT_INTERNET) {
				Communicator.messagesToSend = false;
			} else {
				Communicator.SMSToSend = false;
			}
		}

		if (itemToSend != null && itemToSend.created > 0 && itemToSend.to != -1) {
			if (!itemToSend.system) {
				Main.updateLastMessage(context, itemToSend.to, itemToSend.text,
						itemToSend.created);
			}

			Log.d("communicator",
					"#### sendNextMessage() NOW ABOUT TO SEND ... #1");

			// If the item wants to be sent unencrypted... well.. do it :(
			boolean forceUnencrypted = !itemToSend.encrypted;

			boolean encryption = !forceUnencrypted
					&& Utility.loadBooleanSetting(context,
							Setup.OPTION_ENCRYPTION, Setup.DEFAULT_ENCRYPTION);
			boolean haveKey = Setup.haveKey(context, itemToSend.to);

			Log.d("communicator",
					"#### sendNextMessage() NOW ABOUT TO SEND ... #2");

			String msgText = itemToSend.text;
			if (itemToSend.system) {
				// For system messages do not modify the text!
			} else if (!encryption || !haveKey) {
				msgText = "U" + msgText;
			} else {
				// This fully automatically gets a not-outdated old AES key or
				// generates a fresh new AES key (also sent)

				// Use the same transport for the key as the message wants to be
				// send!
				// TODO: check if we additionally ALWAYS want to send a key via
				// Internet? This is still an open question. 8/23/15
				Key secretKey = Communicator.getAESKey(context, itemToSend.to,
						false, itemToSend.transport, false, itemToSend, true);
				if (secretKey == null) {
					// this indicates that we want to abort sending at this time
					// because we generated a new key
					// Fake an error case to allow the key to be retrieved
					// before this message
					Setup.setErrorUpdateInterval(context, true);
					Setup.setErrorUpdateInterval(context, true);
					Setup.setErrorUpdateInterval(context, true);
					Setup.setErrorUpdateInterval(context, true);
					Scheduler.reschedule(context, true, false, false);
					Log.d("communicator",
							"#### sendNextMessage() NOW ABOUT TO SEND ... ABORT DUE TO NEW KEY #3");
					return;
				}
				// encrypt here
				msgText = "E" + encryptAESMessage(context, msgText, secretKey);
				// PublicKey pubKeyOther = Setup.getKey(context, itemToSend.to);
				// msgText = encryptMessage(context, msgText, pubKeyOther);
				// msgText = "E" + msgText;
			}

			Log.d("communicator",
					"#### sendNextMessage() NOW ABOUT TO SEND ... #4");

			sendMessage(context, itemToSend.to, msgText, itemToSend.created,
					itemToSend, itemToSend.transport);
		} else if (itemToSend != null) {
			// Consume message due to error! We MUST consume it - otherwise
			// following enqueued messages might not get processed. And the
			// clear protocol is to process them in the order they were
			// created because order IS important since session keys are also
			// part of the messages.
			DB.printDBSending(context);
			String toastText = "Error sending Message " + itemToSend.sendingid
					+ ". Invalid receipient or creation date.";
			String text = itemToSend.text;
			if (text != null) {
				Utility.copyToClipboard(context, text);
				toastText += " Message text copied to clipboard.";
			}
			DB.removeSentMessage(context, itemToSend.sendingid);

			Utility.showToastAsync(context, toastText);

			// No more further messages to send for now, this flag is modified
			// by
			// Conversation.sendButtonClick!
			messagesToSend = false;
		}
	}

	// ------------------
	// ------------------
	// ------------------

	/**
	 * Send message. Internal dispatch to Internet or SMS sending.
	 * 
	 * @param context
	 *            the context
	 * @param to
	 *            the to
	 * @param msgText
	 *            the msg text
	 * @param created
	 *            the created
	 * @param itemToSend
	 *            the item to send
	 * @param transport
	 *            the transport
	 */
	private static void sendMessage(final Context context, final int to,
			final String msgText, final long created,
			final ConversationItem itemToSend, final int transport) {
		// Dispatching for Internet and SMS Message sending
		if (transport != DB.TRANSPORT_SMS) {
			Log.d("communicator", "#### sendMessage() INTERNET");
			sendMessageInternet(context, to, msgText, created, itemToSend);
		} else {
			Log.d("communicator", "#### sendMessage() SMS");
			// Utility.showToastAsync(context, "Sending secure SMS ...");
			sendMessageSMS(context, to, msgText, created, itemToSend);
		}
	}

	// ------------------------------------------------------------------------

	/**
	 * Send message SMS. itemToSend can be null, in this case nothing will be
	 * updated.
	 * 
	 * @param context
	 *            the context
	 * @param to
	 *            the to
	 * @param msgText
	 *            the msg text
	 * @param created
	 *            the created
	 * @param itemToSend
	 *            the item to send
	 */
	private static void sendMessageSMS(final Context context, final int to,
			final String msgText, final long created,
			final ConversationItem itemToSend) {
		Log.d("communicator", "#### sendMessageSMS() #1");

		String phone = Setup.getPhone(context, to);
		Log.d("communicator", "#### sendMessageSMS() #2 " + phone);
		if (phone != null && phone.length() > 0) {
			String messageTextString = msgText;
			int localId = -1;
			int hostUid = -1;
			int sendingId = -1;
			boolean encrypted = false;
			if (itemToSend != null) {
				localId = itemToSend.localid;
				hostUid = itemToSend.to;
				encrypted = itemToSend.encrypted;
				sendingId = itemToSend.sendingid;
			}
			// Utility.showToastAsync(context, "Sending SMS Message "
			// +localId+" to " + phone);
			Log.d("communicator", "#### sendMessageSMS() #3 ");
			SendSMS.sendSMS(context, phone, messageTextString, localId,
					hostUid, sendingId, encrypted);
		} else {
			// MUST eliminate message because otherwise next messages will get
			// stuck...
			if (itemToSend != null && itemToSend.text != null
					&& itemToSend.sendingid != -1) {
				Utility.copyToClipboard(context, itemToSend.text);
				Utility.showToastAsync(context,
						"Error sending SMS Message, no phone number for user "
								+ Main.UID2Name(context, to, false)
								+ ". Message text copied to clipboard.");
			}
			DB.removeSentMessage(context, itemToSend.sendingid);
		}

	}

	// ------------------------------------------------------------------------

	/**
	 * Send message Internet. itemToSend can be null, in this case nothing will
	 * be updated.
	 * 
	 * @param context
	 *            the context
	 * @param to
	 *            the to
	 * @param msgText
	 *            the msg text
	 * @param created
	 *            the created
	 * @param itemToSend
	 *            the item to send
	 */
	private static void sendMessageInternet(final Context context,
			final int to, final String msgText, final long created,
			final ConversationItem itemToSend) {
		messageSent = false;

		String session = Setup.getTmpLogin(context);
		if (session == null) {
			// Error resume is automatically done by getTmpLogin, not logged in
			return;
		}

		String url = null;
		String encUid = Setup.encUid(context, to);
		if (encUid == null) {
			// Secret may be not set yet, try again later!
			return;
		}

		Log.d("communicator", "SEND NEXT MESSAGE msgText=" + msgText);

		url = Setup.getBaseURL(context) + "cmd=send&session=" + Utility.urlEncode(session)
				+ "&host=" +encUid + "&val=" +  Utility.urlEncode(created + "#"
				+ msgText);
		

//		
//		// TEST GET REQUEST JUST TO COMPARE
//		final String url2222 = Setup.getBaseURL(context) + "cmd=send&session=" + Utility.encode(session)
//				+ "&host=" + Utility.encode(encUid) + "&val=" + Utility.encode(created + "#" + msgText);
//		Log.d("communicator", "SEND NEXT MESSAGE2222: " + url2222);
//		HttpStringRequest httpStringRequest2222 = (new HttpStringRequest(context,
//				url2222, false, new HttpStringRequest.OnResponseListener() {
//					public void response(String response) {
//						Log.d("communicator",
//								"SEND NEXT MESSAGE2222 OK!!! " + response);
//					}
//		}));
		
		
		Log.d("communicator", "SEND NEXT MESSAGE: " + url);
		final String url2 = url;
		@SuppressWarnings("unused")
		HttpStringRequest httpStringRequest = (new HttpStringRequest(context,
				url2, true, new HttpStringRequest.OnResponseListener() {
					public void response(String response) {
						boolean success = false;
						boolean resposeError = true;
						if (isResponseValid(response)) {
							if (isResponsePositive(response)) {
								resposeError = false;
								String ResponseContent = getResponseContent(response);

								Log.d("communicator",
										"SEND NEXT MESSAGE OK!!! " + response);

								// Response should have the form
								// senttimestamp#mid

								// Update database
								String[] responseArray = ResponseContent
										.split("#");
								if (responseArray.length == 2) {
									String sent = responseArray[0];
									String mid = responseArray[1];
									if (itemToSend != null) {
										// It should not be null even for system
										// messages!
										itemToSend.sent = DB
												.parseTimestamp(sent);
										itemToSend.mid = Utility.parseInt(mid,
												-1);
										// Create a mapping for later matching
										// uid and mid
										DB.addMapping(context, itemToSend.mid,
												itemToSend.to);

										boolean isSentKeyMessage = itemToSend
												.me(context)
												&& itemToSend.isKey;

										Log.d("communicator", "KEYMESSAGE "
												+ itemToSend.mid + ", "
												+ isSentKeyMessage);

										DB.updateMessage(context, itemToSend,
												itemToSend.to, isSentKeyMessage);
										updateSentReceivedReadAsync(context,
												itemToSend.mid, to, true,
												false, false, false);
										if (!itemToSend.system) {
											if (Conversation.isVisible()
													&& Conversation
															.getHostUid() == itemToSend.to
													&& !Conversation.scrolledDown) {
												Utility.showToastShortAsync(
														context,
														"Message sent.");
											}
										}
										DB.removeSentMessage(context,
												itemToSend.sendingid);
									}
									messageSent = true;
									success = true;
								}
							} else {
								Log.d("communicator",
										"SEND NEXT MESSAGE NOT OK!!! "
												+ response);
								if (!response.equals("-111")) { // -111 is the
																// code that the
																// uid
																// decryption
																// failed, just
																// try again
									Log.d("communicator",
											"SEND NEXT MESSAGE NOT OK INVALIDATING!!! "
													+ response);
									// Something may be wrong with our
									// session...
									Setup.possiblyInvalidateSession(context,
											false);
									// Setup.invalidateTmpLogin(context);
								} else {
									Setup.possiblyInvalidateSession(context,
											false);
								}
							}
						}

						if (success) {
							// Clear errors and super fast reschedule
							Setup.possiblyInvalidateSession(context, true); // reset
							Setup.setErrorUpdateInterval(context, false);
							Scheduler.reschedule(context, false, false, true);
						} else {
							// The error case
							Scheduler.reschedule(context, true, false, false);
						}
					}
				}));

	}

	// -------------------------------------------------------------------------

	/**
	 * Creates the notification.
	 * 
	 * @param context
	 *            the context
	 * @param item
	 *            the item
	 */
	public static void createNotification(Context context, ConversationItem item) {
		boolean vibrate = Utility.loadBooleanSetting(context,
				Setup.OPTION_VIBRATE, Setup.DEFAULT_VIBRATE);
		if (vibrate) {
			if (!Utility.isPhoneMuted(context)) {
				Vibrator vibrator = (Vibrator) context
						.getSystemService(Context.VIBRATOR_SERVICE);
				vibrator.vibrate(200);
			}
		}

		boolean tone = Utility.loadBooleanSetting(context, Setup.OPTION_TONE,
				Setup.DEFAULT_TONE);
		if (tone) {
			if (!Utility.isPhoneMutedOrVibration(context)) {
				Utility.notfiyAlarm(context, RingtoneManager.TYPE_NOTIFICATION);
			}
		}

		// ALWAYS SET THE NOTIFICATION COUNTER!!! THEN ONLY RETURN FROM THIS
		// METHOD POSSIBLY IF THE
		// USER DOES NOT WANT TO SEE A REAL NOTIFICATION
		setNotificationCount(context, item.from, false);

		boolean notification = Utility.loadBooleanSetting(context,
				Setup.OPTION_NOTIFICATION, Setup.DEFAULT_NOTIFICATION);
		if (!notification) {
			return;
		}

		NotificationManager notificationManager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		Intent notificationIntent = new Intent(context, TransitActivity.class);
		notificationIntent = notificationIntent.putExtra(Setup.INTENTEXTRA,
				item.from);
		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
				notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

		int cnt = getNotificationCount(context, item.from);
		String title = Main.UID2Name(context, item.from, false);
		String text = item.text;
		String completeMessage = item.text;
		if (text == null) {
			text = "";
		}
		if (cnt > 1) {
			text = cnt + " new messages";
		}

		int maxWidth = Utility.getScreenWidth(context) - 80;
		text = Utility.cutTextIntoOneLine(text, maxWidth, 25);

		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
				context).setSmallIcon(R.drawable.msgsmall24x24)
				.setPriority(NotificationCompat.PRIORITY_MAX)
				.setCategory(NotificationCompat.CATEGORY_ALARM)
				.setTicker(title + ": " + completeMessage).setWhen(0)
				.setContentTitle(title).setContentText(text)
				.setContentIntent(pendingIntent)
				.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

		mBuilder.setGroup(Setup.GROUP_CRYPTOCATOR);
		mBuilder.setAutoCancel(true);
		Notification n = mBuilder.build();

		n.contentIntent = pendingIntent;
		int notificationId = 8888888 + item.from;
		notificationManager.notify(notificationId, n);
	}

	// -------------------------------------------------------------------------

	/**
	 * Cancel notification.
	 * 
	 * @param context
	 *            the context
	 * @param uid
	 *            the uid
	 */
	public static void cancelNotification(Context context, int uid) {
		NotificationManager notificationManager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);

		int notificationId = 8888888 + uid;
		notificationManager.cancel(notificationId);
	}

	// -------------------------------------------------------------------------

	/**
	 * Gets the notification count.
	 * 
	 * @param context
	 *            the context
	 * @param uid
	 *            the uid
	 * @return the notification count
	 */
	public static int getNotificationCount(Context context, int uid) {
		return Utility.loadIntSetting(context, "notification" + uid, 0);
	}

	// ------------------------------------------------------------------------

	/**
	 * Sets the notification count. reset = true: delete, reset = false:
	 * increment.
	 * 
	 * @param context
	 *            the context
	 * @param uid
	 *            the uid
	 * @param reset
	 *            the reset
	 */
	public static void setNotificationCount(Context context, int uid,
			boolean reset) {
		if (!reset) {
			Utility.saveIntSetting(context, "notification" + uid,
					getNotificationCount(context, uid) + 1);
		} else {
			Utility.saveIntSetting(context, "notification" + uid, 0);
		}
	}

	// -------------------------------------------------------------------------

	/**
	 * Send key to server.
	 * 
	 * @param context
	 *            the context
	 * @param key
	 *            the key
	 * @param keyhash
	 *            the keyhash
	 */
	public static void sendKeyToServer(final Context context, final String key,
			final String keyhash) {
		final String uidString = Utility.loadStringSetting(context, "uid", "");

		String session = Setup.getTmpLoginEncoded(context);
		if (session == null) {
			// Error resume is automatically done by getTmpLogin, not logged in
			Utility.showToastAsync(context, "Error sending account key "
					+ keyhash + " to server! (1)");
			// Disable in settings
			Utility.saveStringSetting(context, Setup.PUBKEY, null);
			Utility.saveStringSetting(context, Setup.PRIVATEKEY, null);
			Utility.saveBooleanSetting(context, Setup.OPTION_ENCRYPTION, false);
			return;
		}

		String url = null;
		url = Setup.getBaseURL(context) + "cmd=sendkey&session=" + session
				+ "&val=" + Utility.urlEncode(key);

		Log.d("communicator", "###### SEND KEY TO SERVER " + url);
		final String url2 = url;
		@SuppressWarnings("unused")
		HttpStringRequest httpStringRequest = (new HttpStringRequest(context,
				url2, new HttpStringRequest.OnResponseListener() {
					public void response(String response) {
						boolean success = false;
						if (isResponseValid(response)) {
							if (response.equals("1")) {
								success = true;
							}
						}
						if (success) {
							Utility.showToastAsync(context, "New account key "
									+ keyhash + " sent to server.");
							Utility.saveBooleanSetting(context,
									Setup.SETTINGS_HAVESENTRSAKEYYET
											+ uidString, true);
						} else {
							Utility.showToastAsync(context,
									"Error sending account " + keyhash
											+ " key to server! (2)");
							// disable in settings
							Utility.saveStringSetting(context, Setup.PUBKEY,
									null);
							Utility.saveStringSetting(context,
									Setup.PRIVATEKEY, null);
							Utility.saveBooleanSetting(context,
									Setup.OPTION_ENCRYPTION, false);
						}
					}
				}));

	}

	// -------------------------------------------------------------------------

	/**
	 * Clear key from server.
	 * 
	 * @param context
	 *            the context
	 * @param keyhash
	 *            the keyhash
	 */
	public static void clearKeyFromServer(final Context context,
			final String keyhash) {
		String session = Setup.getTmpLoginEncoded(context);
		if (session == null) {
			// Error resume is automatically done by getTmpLogin, not logged in
			Utility.showToastAsync(context, "Error clearing account " + keyhash
					+ " key from server! (1)");
			return;
		}

		String url = null;
		url = Setup.getBaseURL(context) + "cmd=clearkey&session=" + session;

		// Log.d("communicator", "###### CLEAR KEY FROM SERVER " + url);
		final String url2 = url;
		@SuppressWarnings("unused")
		HttpStringRequest httpStringRequest = (new HttpStringRequest(context,
				url2, new HttpStringRequest.OnResponseListener() {
					public void response(String response) {
						boolean success = false;
						if (isResponseValid(response)) {
							if (response.equals("1")) {
								success = true;
							}
						}

						if (success) {
							Utility.showToastAsync(context, "Account key "
									+ keyhash + " cleared from server.");
						} else {
							Utility.showToastAsync(context,
									"Error clearing account key " + keyhash
											+ " from server! (2)");
						}

					}
				}));

	}

	// -------------------------------------------------------------------------

	/**
	 * Update keys from server.
	 * 
	 * @param context
	 *            the context
	 * @param uidList
	 *            the uid list
	 * @param forceUpdate
	 *            the force update
	 * @param updateListener
	 *            the update listener
	 */
	public static void updateKeysFromServer(final Context context,
			final List<Integer> uidList, final boolean forceUpdate,
			final Main.UpdateListener updateListener) {

		long lastTime = Utility.loadLongSetting(context,
				Setup.SETTING_LASTUPDATEKEYS, 0);
		long currentTime = DB.getTimestamp();
		if (!forceUpdate
				&& (lastTime + Setup.UPDATE_KEYS_MIN_INTERVAL > currentTime)) {
			// Do not do this more frequently
			return;
		}
		Utility.saveLongSetting(context, Setup.SETTING_LASTUPDATEKEYS,
				currentTime);

		String uidliststring = "";
		final ArrayList<Integer> sentList = new ArrayList<Integer>();
		for (int uid : uidList) {
			// not do this of fake UIDs (sms-only users!)
			if (uid > 0) {
				sentList.add(uid);
				if (uidliststring.length() != 0) {
					uidliststring += "#";
				}
				uidliststring += Setup.encUid(context, uid);
			}
		}

		String session = Setup.getTmpLoginEncoded(context);
		if (session == null) {
			// error resume is automatically done by getTmpLogin, not logged in
			return;
		}

		String url = null;
		url = Setup.getBaseURL(context) + "cmd=haskey&session=" + session
				+ "&val=" + Utility.urlEncode(uidliststring);

		Log.d("communicator", "###### REQUEST HAS KEY " + url);
		final String url2 = url;
		@SuppressWarnings("unused")
		HttpStringRequest httpStringRequest = (new HttpStringRequest(context,
				url2, new HttpStringRequest.OnResponseListener() {
					public void response(String response) {
						if (isResponseValid(response)) {
							// Log.d("communicator",
							// "###### HAS KEY VALUES RECEIVED!!! "
							// + response2);
							if (isResponsePositive(response)) {
								String responseContent = getResponseContent(response);
								List<String> values = Utility
										.getListFromString(responseContent, "#");
								if (values.size() > 0) {
									boolean updateNeeded = false;

									int index = 0;
									for (String value : values) {
										if (uidList.size() > index) {
											int uid = sentList.get(index);
											if (value.equals("0")) {
												// no key ... delete possibly
												// old
												// key
												if (Setup.haveKey(context, uid)) {
													updateNeeded = true;
													Setup.saveKey(context, uid,
															null);
													Setup.setKeyDate(context,
															uid, null);
												}
											} else {
												// check if the timestamp
												// matches or
												// requires to reload the key!
												String keydate = Setup
														.getKeyDate(context,
																uid);
												if (keydate == null
														|| !keydate
																.equals(value)) {
													Log.d("communicator",
															"###### KEY TIMESTAMP online = "
																	+ value
																	+ " != "
																	+ keydate
																	+ " = cache  NOT MATCHING, REQUIRE CURRENT KEY FROM SERVER "
																	+ uid);
													// not matching, update
													// required, delete old
													// values
													// first
													Setup.saveKey(context, uid,
															null);
													Setup.setKeyDate(context,
															uid, null);
													getKeyFromServer(context,
															uid, updateListener);
												} else {
													// Log.d("communicator",
													// "###### KEY UP TO DATE "
													// + uid);
												}
											}
										}
										index++;
									}
									if (updateNeeded) {
										Main.possiblyRebuildUserlistAsync(
												context, false);
									}
								}
							}
						}
					}
				}));
	}

	// -------------------------------------------------------------------------

	/**
	 * Update phones from server.
	 * 
	 * @param context
	 *            the context
	 * @param uidList
	 *            the uid list
	 * @param forceUpdate
	 *            the force update
	 */
	public static void updatePhonesFromServer(final Context context,
			final List<Integer> uidList, final boolean forceUpdate) {

		long lastTime = Utility.loadLongSetting(context,
				Setup.SETTING_LASTUPDATEPHONES, 0);
		long currentTime = DB.getTimestamp();
		if (!Setup.isSMSOptionEnabled(context)) {
			// if no sms option is enabled, then do not retrieve keys!
			return;
		}
		if (!forceUpdate
				&& (lastTime + Setup.UPDATE_PHONES_MIN_INTERVAL > currentTime)) {
			// Do not do this more frequently
			return;
		}
		Utility.saveLongSetting(context, Setup.SETTING_LASTUPDATEPHONES,
				currentTime);

		String uidliststring = "";
		final ArrayList<Integer> uidListUsed = new ArrayList<Integer>();
		for (int uid : uidList) {
			// not do this of fake UIDs (sms-only users!)
			if (uid > 0) {
				uidListUsed.add(uid);
				if (uidliststring.length() != 0) {
					uidliststring += "#";
				}
				uidliststring += Setup.encUid(context, uid);
			}
		}
		if (uidliststring.equals("")) {
			return;
		}

		String session = Setup.getTmpLoginEncoded(context);
		if (session == null) {
			// error resume is automatically done by getTmpLogin, not logged in
			return;
		}

		String url = null;
		url = Setup.getBaseURL(context) + "cmd=hasphone&session=" + session
				+ "&val=" + Utility.urlEncode(uidliststring);

		Log.d("communicator", "###### REQUEST HAS PHONE (" + uidliststring
				+ ") " + url);

		final String url2 = url;
		@SuppressWarnings("unused")
		HttpStringRequest httpStringRequest = (new HttpStringRequest(context,
				url2, new HttpStringRequest.OnResponseListener() {
					public void response(String response) {
						if (isResponseValid(response)) {
							// Log.d("communicator",
							// "###### HAS KEY VALUES RECEIVED!!! "
							// + response2);
							if (isResponsePositive(response)) {
								String responseContent = getResponseContent(response);
								List<String> values = Utility
										.getListFromString(responseContent, "#");
								if (values.size() > 0) {
									int index = 0;
									for (String value : values) {
										if (uidListUsed.size() > index) {
											int uid = uidListUsed.get(index);
											if (value.equals("-1")) {
												// no phone number or not
												// elibale
												// the other person needs to
												// have you in his userlist in
												// order
												// to legitimate you to download
												// his phone number! this is
												// a strong privacy requirement
												if (Main.isUpdatePhone(context,
														uid)) {
													Setup.savePhone(context,
															uid, "", false);
												}
											} else {
												// we are allowed to add this
												// telephone number locally
												value = Setup.decText(context,
														value);
												if (value != null) {
													if (Main.isUpdatePhone(
															context, uid)) {
														Setup.savePhone(
																context, uid,
																value, false);
													}
												}
												// Log.d("communicator",
												// "###### RESPONSE HAS PHONE SAVE ("+uidliststring2+") #4 "
												// + uid);
											}
										}
										index++;
									}
								}
							}
						}
					}
				}));
	}

	// -------------------------------------------------------------------------

	/**
	 * Gets the key from server.
	 * 
	 * @param context
	 *            the context
	 * @param uid
	 *            the uid
	 * @param updateListener
	 *            the update listener
	 * @return the key from server
	 */
	public static void getKeyFromServer(final Context context, final int uid,
			final Main.UpdateListener updateListener) {

		if (uid < 0) {
			// Do not request keys for SMS/external users!
			return;
		}

		String session = Setup.getTmpLoginEncoded(context);
		if (session == null) {
			// error resume is automatically done by getTmpLogin, not logged in
			return;
		}

		String url = null;
		url = Setup.getBaseURL(context) + "cmd=getkey&session=" + session
				+ "&val=" + Setup.encUid(context, uid);

		Log.d("communicator", "###### REQUEST KEY FOR UID " + uid
				+ " FROM SERVER " + url);
		final String url2 = url;
		@SuppressWarnings("unused")
		HttpStringRequest httpStringRequest = (new HttpStringRequest(context,
				url2, new HttpStringRequest.OnResponseListener() {
					public void response(String response) {
						if (isResponseValid(response)) {
							// Log.d("communicator", "###### KEY RECEIVED!!! "
							// + response2);
							if (isResponsePositive(response)) {
								String responseContent = getResponseContent(response);
								List<String> values = Utility
										.getListFromString(responseContent, "#");
								if (values.size() == 2) {
									Setup.setKeyDate(context, uid,
											values.get(0));
									Setup.saveKey(context, uid, values.get(1));
									Log.d("communicator",
											"##### KEY SAVED TO CACHE FOR USER "
													+ uid + " timestamp ="
													+ values.get(0) + ", key="
													+ values.get(1));
									Main.possiblyRebuildUserlistAsync(context,
											false);
									if (updateListener != null) {
										updateListener.onUpdate(values.get(1));
									}
								}
							}
						}
					}
				}));
	}

	// -------------------------------------------------------------------------

	/**
	 * Decrypt message.
	 * 
	 * @param context
	 *            the context
	 * @param text
	 *            the text
	 * @param key
	 *            the key
	 * @return the string
	 */
	public static String decryptMessage(Context context, String text, Key key) {
		// Decode the encoded data with RSA public key
		byte[] decodedBytes = null;
		try {
			Cipher c = Cipher.getInstance("RSA");
			c.init(Cipher.DECRYPT_MODE, key);
			byte[] textAsBytes = Base64.decode(text, Base64.DEFAULT);
			decodedBytes = c.doFinal(textAsBytes);
			String returnText = new String(decodedBytes);
			return returnText;
		} catch (Exception e) {
			Log.e("communicator", "RSA decryption error");
			e.printStackTrace();
		}
		return null;
	}

	// -------------------------------------------------------------------------

	/**
	 * Encrypt server message.
	 * 
	 * @param context
	 *            the context
	 * @param text
	 *            the text
	 * @param key
	 *            the key
	 * @return the string
	 */
	@SuppressLint("TrulyRandom")
	public static String encryptServerMessage(Context context, String text,
			Key key) {
		// Encode the original data with RSA private key
		byte[] encodedBytes = null;
		String returnText = null;
		try {
			// None
			// Cipher c = Cipher.getInstance("RSA/None/PKCS1Padding", "BC");
			// Cipher c = Cipher.getInstance("RSA/ECB/PKCS1Padding", "BC");
			Cipher c = Cipher.getInstance("RSA/ECB/PKCS1Padding");
			c.init(Cipher.ENCRYPT_MODE, key);
			encodedBytes = c.doFinal(text.getBytes());
			returnText = Base64.encodeToString(encodedBytes, Base64.DEFAULT);
		} catch (Exception e) {
			Log.e("communicator", "RSA encryption error");
			e.printStackTrace();
		}
		return returnText;
	}

	// -------------------------------------------------------------------------

	/**
	 * Encrypt message.
	 * 
	 * @param context
	 *            the context
	 * @param text
	 *            the text
	 * @param key
	 *            the key
	 * @return the string
	 */
	public static String encryptMessage(Context context, String text, Key key) {
		// Encode the original data with RSA private key
		byte[] encodedBytes = null;
		String returnText = null;
		try {
			Cipher c = Cipher.getInstance("RSA");
			c.init(Cipher.ENCRYPT_MODE, key);
			encodedBytes = c.doFinal(text.getBytes());
			returnText = Base64.encodeToString(encodedBytes, Base64.DEFAULT);
		} catch (Exception e) {
			Log.e("communicator", "RSA encryption error");
			e.printStackTrace();
		}
		return returnText;
	}

	// -------------------------------------------------------------------------

	/**
	 * Decrypt aes message.
	 * 
	 * @param context
	 *            the context
	 * @param text
	 *            the text
	 * @param secretKey
	 *            the secret key
	 * @return the string
	 */
	public static String decryptAESMessage(Context context, String text,
			Key secretKey) {
		byte[] decodedBytes = null;
		try {
			Cipher c = Cipher.getInstance("AES");
			c.init(Cipher.DECRYPT_MODE, secretKey);
			byte[] textAsBytes = Base64.decode(text, Base64.DEFAULT);
			decodedBytes = c.doFinal(textAsBytes);
			String returnText = new String(decodedBytes);
			return returnText.substring(Setup.RANDOM_STUFF_BYTES); // remove the
																	// 5 random
																	// characters
		} catch (Exception e) {
			Log.e("communicator", "AES decryption error");
			e.printStackTrace();
		}
		return null;
	}

	// -------------------------------------------------------------------------

	/**
	 * Encrypt aes message.
	 * 
	 * @param context
	 *            the context
	 * @param text
	 *            the text
	 * @param secretKey
	 *            the secret key
	 * @return the string
	 */
	public static String encryptAESMessage(Context context, String text,
			Key secretKey) {
		// Add 5 random characters just for security
		text = Utility.getRandomString(Setup.RANDOM_STUFF_BYTES) + text;
		byte[] encodedBytes = null;
		String returnText = null;
		try {
			Cipher c = Cipher.getInstance("AES");
			c.init(Cipher.ENCRYPT_MODE, secretKey);
			encodedBytes = c.doFinal(text.getBytes());
			returnText = Base64.encodeToString(encodedBytes, Base64.DEFAULT);
		} catch (Exception e) {
			Log.e("communicator", "AES encryption error");
			e.printStackTrace();
		}
		return returnText;
	}

	// -------------------------------------------------------------------------

	/**
	 * Send read confirmation.
	 * 
	 * @param context
	 *            the context
	 * @param uid
	 *            the uid
	 */
	public static void sendReadConfirmation(final Context context, final int uid) {
		// only send readConfirmation for registered users!
		if (uid <= 0) {
			return;
		}

		// if we do not refuse to send these confirmations...
		if (!Utility.loadBooleanSetting(context, Setup.OPTION_NOREAD,
				Setup.DEFAULT_NOREAD)) {
			// ...then go ahead and send them
			// send the largest mid for the user watching messages, so that the
			// server can figure out!
			// ATTENTION: this largest mid MUST NOT be a system message (R, W)
			// because we do not send read confirmations for these! (this would
			// result in a ping pong of read confirmations!)
			final int mid = DB.getLargestMidForUIDExceptSystemMessages(context,
					uid);

			// wait. first let's see if we have sent this already!
			int lastMidSent = Utility.loadIntSetting(context,
					"lastreadconfirmationmid", -1);

			Log.d("communicator",
					"SEND READ CONFIRMATION ?? lastreadconfirmationmid="
							+ lastMidSent + " =?= " + +mid + "=mid");

			if (lastMidSent != mid) {
				sendSystemMessageRead(context, uid, mid);
				Utility.saveIntSetting(context, "lastreadconfirmationmid", mid);
			}
		}
	}

	// -------------------------------------------------------------------------

	/**
	 * Send system message read. System messages are sent over normal sending
	 * interface, this way they will be sent automatically in order and iff
	 * temporary login is okay. Both system messages will go to the server.
	 * System messages can only go over internet (if available) modes: R == read
	 * (these are processed directly by the server) A == withdraw (these are
	 * processed and come also back as W-messages)
	 * 
	 * @param context
	 *            the context
	 * @param uid
	 *            the uid
	 * @param mid
	 *            the mid
	 */
	public static void sendSystemMessageRead(final Context context,
			final int uid, final int mid) {
		// uid = user from which we have read messages
		// mid = largest mid that we have read
		if (uid >= 0 && mid != -1) {
			DB.addSendMessage(context, uid, "R" + mid, false,
					DB.TRANSPORT_INTERNET, true, DB.PRIORITY_READCONFIRMATION);
			Communicator
					.sendNewNextMessageAsync(context, DB.TRANSPORT_INTERNET);
		}
	}

	// -------------------------------------------------------------------------

	/**
	 * Send system message widthdraw. System messages are sent over normal
	 * sending interface, this way they will be sent automatically in order and
	 * iff temporary login is okay. Both system messages will go to the server.
	 * System messages can only go over internet (if available) modes: R == read
	 * (these are processed directly by the server) A == withdraw (these are
	 * processed and come also back as W-messages)
	 * 
	 * @param context
	 *            the context
	 * @param uid
	 *            the uid
	 * @param mid
	 *            the mid
	 */
	public static void sendSystemMessageWidthdraw(final Context context,
			final int uid, final int mid) {
		Utility.showToastAsync(context, "Withdraw request for message " + mid
				+ "...");

		if (uid >= 0 && mid != -1) {
			DB.addSendMessage(context, uid, "A" + mid, false,
					DB.TRANSPORT_INTERNET, true, DB.PRIORITY_KEY);
			Communicator
					.sendNewNextMessageAsync(context, DB.TRANSPORT_INTERNET);
		}
	}

	// -------------------------------------------------------------------------

	/**
	 * Checks if is response is a number or starts with a number followed by a
	 * #.
	 * 
	 * @param response
	 *            the response
	 * @return true, if is response ok/valid
	 */
	public static boolean isResponseValid(String response) {
		if (response == null || response.length() == 0) {
			// no info received
			return false;
		}
		if (Utility.parseInt(response, Integer.MIN_VALUE) != Integer.MIN_VALUE) {
			// is a number
			return true;
		}
		int i = response.indexOf("#");
		if (i < 1) {
			// no # found
			return false;
		}
		String partResponse = response.substring(0, i);
		if (Utility.parseInt(partResponse, Integer.MIN_VALUE) != Integer.MIN_VALUE) {
			// first part is a number
			return true;
		}
		return false;
	}

	// -------------------------------------------------------------------------

	/**
	 * Checks if the response does not start with a negative number!.
	 * 
	 * @param response
	 *            the response
	 * @return true, if successful
	 */
	public static boolean isResponsePositive(String response) {
		return !response.startsWith("-");
	}

	// -------------------------------------------------------------------------

	/**
	 * Gets the response content, i.e., everything after the first number, or
	 * null iff there is no such content.
	 * 
	 * @param response
	 *            the response
	 * @return the response content
	 */
	public static String getResponseContent(String response) {
		int i = response.indexOf("#");
		if (i < 1) {
			// no # found, no content
			return null;
		}
		return response.substring(i + 1);
	}

	// -------------------------------------------------------------------------

}
