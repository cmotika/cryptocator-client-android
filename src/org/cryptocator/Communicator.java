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
 * account and session keys. <BR>
 * <BR>
 * Notes on AES session keys and update strategy. Keys timeout after one hour.
 * They timeout a little later for receiving than for sending. Keys can be send
 * via Internet or SMS. <BR>
 * Scenario: If sending a key via Internet and the other person currently does
 * not have Internet connection we should not send encrypted SMS because the
 * other person would be unable to decrypt.<BR>
 * Solution: Carry two timestamps for the current key. If sending by Internet
 * only set the Internet timestamp. If sending by SMS only set the SMS
 * timestamp. Clear the other transport's timestamp when sending a new key! On
 * receiving a key: Set BOTH timestamps. If receiving delivery confirmation for
 * an Internet key message set the SMS timestamp. If receiving delivery
 * confirmation for an SMS key message then set the Internet timstamp. Now: If
 * sending a message via Internet and the Internet timestamp is not set, re-send
 * the key message via Internet (if not outdated). If sending a message via SMS
 * and the SMS timestamp is not set, re-send the key message via SMS (if not
 * outdated). This way we ensure that the (non-outdated) key is ALWAYS present
 * at the receipient no matter transport method is chosen and no matter
 * connectivity permits SMS or Internet receiving or messages!
 * 
 * @author Christian Motika
 * @since 1.2
 * @date 08/23/2015
 * 
 */
public class Communicator {

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
	 * the scheduler that evaluates and uses messagesToSend as a cache! This flag is also set to
	 * false if itemToSend != null and hence a message was (at least tried to be) sent.
	 */
	public static boolean messagesToSendIsUpToDate = false;


	/** The internetfailcntbarrier after the counter reaches this number the internet is claimed to fail. */
	public static int INTERNETFAILCNTBARRIER = 5;

	/**
	 * The connection flag tells if Internet was ok. It is evaluated in Main for
	 * the info message.
	 */
	public static int internetFailCnt = 0;

	
	/** The loginfailcntbarrier after the counter reaches this number the login is claimed to fail. */
	public static int LOGINFAILCNTBARRIER = 5;
	
	/**
	 * The connection flag tells if Login was ok. It is evaluated in Main for
	 * the info message.
	 */
	public static int loginFailCnt = 0;

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
	 * messages and only if there are messages waiting it triggers the receive
	 * next message.
	 * 
	 * @param context
	 *            the context
	 */
	public static void haveNewMessagesAndReceive(final Context context,
			final int serverId) {
		if (haveNewMessages) {
			// If we already know we have new messages, we not need to run the
			// light-weight have-request!
			receiveNextMessage(context, serverId);
			return;
		}

		// Largest timestamp received
		final int largestMid = DB.getLargestMid(context, serverId);

		String sessionid = Setup.getSessionID(context, serverId);

		String url = null;
		url = Setup.getBaseURL(context, serverId)
				+ "cmd=have&session="
				+ sessionid
				+ "&val="
				+ Utility.urlEncode(largestMid + "#"
						+ DB.getLargestTimestampReceived(context, serverId)
						+ "#" + DB.getLargestTimestampRead(context, serverId));

		Log.d("communicator", "REQUEST HAVE: " + url);
		final String url2 = url;
		@SuppressWarnings("unused")
		HttpStringRequest httpStringRequest = (new HttpStringRequest(context,
				url2, new HttpStringRequest.OnResponseListener() {
					public void response(String response) {
						if (isResponseValid(response)) {
							internetFailCnt = 0;
							final String response2 = response;
							// Log.d("communicator",
							// "RECEIVED NEW MESSAGES WAITING: "
							// + response2);
							// Log.d("communicator", "RESPONSE HAVE: " +
							// response2);
							if (response.equals("-1")) {
								// enforce new session if this happens
								// twice
								Setup.possiblyInvalidateSession(context, false,
										serverId);
							} else if (response2.startsWith("2#")) {
								Setup.possiblyInvalidateSession(context, true,
										serverId); // reset
								// (everythin
								// ok/normal)
								// largestMid too high, reduce! - typically this
								// can only
								// happen if the msg database on the server is
								// reset
								String content = Communicator
										.getResponseContent(response2);
								if (content != null && content.length() > 0) {
									int midFromServer = Utility.parseInt(
											content, largestMid);
									Log.d("communicator",
											"RESPONSE HAVE SET - TOO HIGH MID: "
													+ midFromServer);
									DB.resetLargestMid(context, midFromServer,
											serverId);
								}
							} else {
								// NORMAL PROCESSING - 0##...##... or
								// 1##...##... (the latter means we HAVE
								// messages to receive!)
								String responseTail = response2.substring(3);
								Log.d("communicator",
										"RESPONSE HAVE PROCESS TAIL: "
												+ responseTail);
								handleReadAndReceived(context, responseTail,
										serverId);
								if (response2.startsWith("1##")) {
									Setup.possiblyInvalidateSession(context,
											true, serverId); // reset
																// (everything
									// ok/normal)
									haveNewMessages = true;
									if (!(Conversation.isVisible() && Conversation
											.isTyping())) {
										receiveNextMessage(context, serverId);
									}
								}
							}
						} else {
							internetFailCnt++;
						}
					}
				}));
	}

	// ----------------------------------------------------------------------------------

	/**
	 * Process key deliveries. We must set the key timestamp of the other
	 * transport medium to the value of the one that this key was initially sent
	 * with. Rationale: After a key is delivered to the other partner it is safe
	 * to be used for ANY transport medium.
	 * 
	 * @param context
	 *            the context
	 * @param hostUid
	 *            the host uid
	 */
	public static void processKeyDeliveries(final Context context,
			int senderUid, int midOrLocalId, boolean processSMS) {
		// If we wait for a key message delivery confirmation in
		// order to set the other transport's timestamp...
		// check here:
		int lastKeyMessageMid = DB.getLastSentKeyMessage(context, senderUid,
				processSMS);
		Log.d("communicator",
				" KEYUPDATE: handleReadAndReceived() lastKeyMessageMid="
						+ lastKeyMessageMid + " != -1 ???");
		if (lastKeyMessageMid > -1) {
			// -1 means we await in principle but not have a
			// mid, maybe the key messages is not yet sent
			// -2 means we do not await such a key message!
			Log.d("communicator",
					" KEYUPDATE: handleReadAndReceived() (lastKeyMessageMid == mid) ??? midOrLocalId="
							+ midOrLocalId);
			if (lastKeyMessageMid == midOrLocalId) {
				// Yes... we just received the delivery
				// confirmation and can NOW use our key for BOTH
				// transport methods!
				long timestampInternet = Setup.getAESKeyDate(context,
						senderUid, DB.TRANSPORT_INTERNET);
				long timestampSMS = Setup.getAESKeyDate(context, senderUid,
						DB.TRANSPORT_SMS);
				Log.d("communicator",
						" KEYUPDATE: handleReadAndReceived() timestampInternet="
								+ timestampInternet + ", timestampSMS="
								+ timestampSMS);
				// Do not await any more!
				Utility.saveIntSetting(context, Setup.LASTKEYMID + senderUid,
						-2);
				if (timestampInternet > 0) {
					Log.d("communicator",
							" KEYUPDATE: handleReadAndReceived() timestampInternet now also saved for SMS");
					Setup.setAESKeyDate(context, senderUid, timestampInternet
							+ "", DB.TRANSPORT_SMS);
				} else {
					Log.d("communicator",
							" KEYUPDATE: handleReadAndReceived() timestampSMS now also saved for Internet");
					Setup.setAESKeyDate(context, senderUid, timestampSMS + "",
							DB.TRANSPORT_INTERNET);
				}

			}
		}
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
			String response, int serverId) {

		String[] values = response.split("##");
		// Log.d("communicator", " UPDATE MESSAGE RECEIVED/READ: valuesSize=" +
		// values.length);
		if (values.length == 2) {
			String partReceived = values[0];
			String partRead = values[1];

			String[] valuesReceived = partReceived.split("#");
			for (String valueReceived : valuesReceived) {
				String midAndTs[] = valueReceived.split("@");
				// Log.d("communicator",
				// " UPDATE MESSAGE RECEIVED: midAndTs["+valueReceived+"]=" +
				// midAndTs.length);
				if (midAndTs.length == 2) {
					int mid = Utility.parseInt(midAndTs[0], -1);
					int senderUid = DB.getHostUidForMid(context, mid);
					String ts = midAndTs[1];
					Log.d("communicator", " UPDATE MESSAGE RECEIVED: mid="
							+ mid + ", senderUid=" + senderUid);
					if (mid != -1 && serverId != -1) {
						DB.updateLargestTimestampReceived(context, ts, serverId);
					}
					if (mid != -1 && senderUid != -1) {
						boolean processSMS = false;
						processKeyDeliveries(context, senderUid, mid,
								processSMS);
						// Only do this AFTER previous processing ... otherwise
						// we cannot find the lastKeyMessageMid!
						DB.updateMessageReceived(context, mid, ts, senderUid);
						updateSentReceivedReadAsync(context, mid, senderUid,
								false, true, false, false, false);

					}
				}
			}

			String[] valuesRead = partRead.split("#");
			for (String valueRead : valuesRead) {
				String midAndTs[] = valueRead.split("@");
				// Log.d("communicator",
				// " UPDATE MESSAGE READ: midAndTs["+valueRead+"]=" +
				// midAndTs.length);
				if (midAndTs.length == 2) {
					int mid = Utility.parseInt(midAndTs[0], -1);
					int senderUid = DB.getHostUidForMid(context, mid);
					String ts = midAndTs[1];
					boolean failed = false;
					if (ts != null && ts.startsWith("-")) {
						// This indicates a FAILED TO DECRYPT message => flag this here
						// make ts positive, it is the read timestamp still!
						ts = ts.substring(1);
						failed = true;
					}
					Log.d("communicator", " UPDATE MESSAGE READ: mid=" + mid
							+ ", senderUid=" + senderUid);
					if (mid != -1 && serverId != -1) {
						DB.updateLargestTimestampRead(context, ts, serverId);
					}
					if (mid != -1 && senderUid != -1) {
						// ONLY remove the mapping if we know we processed the
						// read confirmation!!!
						DB.removeMappingByMid(context, mid);

						if (failed) {
							// Flag decryption failed again by using negative read TS
							DB.updateMessageRead(context, mid, "-"+ts, senderUid);
							updateSentReceivedReadAsync(context, mid, senderUid,
									false, false, false, false, true);
						} else {
							// Everything ok
							DB.updateMessageRead(context, mid, ts, senderUid);
							updateSentReceivedReadAsync(context, mid, senderUid,
									false, false, true, false, false);
						}
						
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
			final boolean received, final boolean read, final boolean withdraw, final boolean decryptionfailed) {
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

							if (decryptionfailed) {
								Conversation.setDecryptionFailed(context, mid);
							}
							else if (read) {
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
	public synchronized static void receiveNextMessage(final Context context,
			final int serverId) {
		messageReceived = false;

		final int largestMid = DB.getLargestMid(context, serverId);
		// This should be the case for an empty database only!
		final boolean discardMessageAndSaveLargestMid = (largestMid == -1);

		String session = Setup.getTmpLoginEncoded(context, serverId);
		if (session == null) {
			// Error resume is automatically done by getTmpLogin, not logged in
			return;
		}

		String url = null;
		url = Setup.getBaseURL(context, serverId) + "cmd=receive&session="
				+ session + "&val=" + largestMid;
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
								loginFailCnt = 0;
								// We currently do not need to look for new
								// messages
							} else if (isResponsePositive(response)) {
								resposeError = false;
								loginFailCnt = 0;
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
											responseArray[2], serverId) + "";

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
												context, false, serverId);
									}

									if (!from.equals("-2")) {
										// Only not update in case or wrong
										// decoding because we want to later try
										// again!
										DB.updateLargestMid(context,
												Utility.parseInt(mid, 0),
												serverId);
									}

									if (from.equals("-1")) {
										// Invalid users
										// update cache
										DB.lastReceivedMid.put(serverId,
												newItem.mid);
										Utility.saveIntSetting(context,
												Setup.SETTINGS_DEFAULTMID,
												newItem.mid);
									}

									// Discard unknown or invalid users
									if (!from.equals("-2")
											&& !from.equals("-1")) {
										Setup.possiblyInvalidateSession(
												context, true, serverId); // reset
										success1 = true;
										// Uids could be recovered/decrypted
										String created = responseArray[3];
										String sent = responseArray[4];
										String text = responseArray[5];

										newItem.mid = Utility.parseInt(mid, -1);
										newItem.from = Utility.parseInt(from,
												-1);
										// NEW: recalculate uid from suid got
										// from server
										newItem.from = Setup.getUid(context,
												newItem.from, serverId);

										Log.d("communicator",
												"RECEIVE NEXT MESSAGE FROM LOCAL UID: newItem.from="
														+ newItem.from);

										newItem.to = DB.myUid();
										newItem.created = DB
												.parseTimestamp(created);
										newItem.sent = DB.parseTimestamp(sent);
										newItem.received = DB.getTimestamp();
										newItem.transport = DB.TRANSPORT_INTERNET;

										// Update cache
										DB.lastReceivedMid.put(serverId,
												newItem.mid);
										Utility.saveIntSetting(context,
												Setup.SETTINGS_DEFAULTMID
														+ serverId, newItem.mid);

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
											if (!ignore) {
												// The user must be added to our
												// list
												// ATTENTION. from is already
												// converted from suid to uid!
												int uid = newItem.from;
												uidList.add(uid);
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

										Log.d("communicator",
												"RECEIVE NEXT MESSAGE skipBecauseOfUnknownUser="
														+ skipBecauseOfUnknownUser);

										if (!skipBecauseOfUnknownUser
												&& !alreadyInDB
												&& !discardMessageAndSaveLargestMid) {
											newItem.text = handleReceivedText(
													context, text, newItem,
													serverId);
											success2 = updateDBForReceivedMessage(
													context, newItem);

											// Auto-save images possibly (only
											// if single message or combined!)
											if (Utility.loadBooleanSetting(
													context,
													Setup.OPTION_AUTOSAVE,
													Setup.DEFAULT_AUTOSAVE)
													&& newItem.readyToProcess()) {
												MessageDetailsActivity
														.autoSaveAllImages(
																context,
																newItem.text,
																newItem);
											}

											if (newItem.text == null
													|| newItem.text.equals("")) {
												// No toast or scrolling on
												// system messages
												// no notification
												success2 = false;
											}

											// THE FOLLOWING IS ALEADY DONE SELECTIVELY FOR THIS
											// USER IN HANDLETEXT!
											// if (newItem.text
											// .contains("[ invalid session key ")
											// || newItem.text
											// .contains("[ decryption failed ]"))
											// {
											// // We should try to update the
											// // public rsa key of this user
											// Communicator.updateKeysFromServer(
											// context,
											// Main.loadUIDList(context),
											// true, null, serverId);
											// }
										} else {
											// Discard means no live update
											// please...
											success2 = false;
										}
									} // End uids decrypted sucessfully
								}
							} else {
								// Invalidate right away
								Setup.invalidateTmpLogin(context, serverId);
								loginFailCnt++;
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
	public static boolean updateDBForReceivedMessage(Context context,
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

		if (DB.isMultipartDuplicate(context, newItem.from, newItem.multipartid)) {
			Log.d("communicator",
					"RECEIVED MESSAGE SKIPPED BECAUSE OF DUPLICATE MULTIPARTID!!!");
			return false;
		}

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

		// Clean up any erroneously received duplicates
		DB.cleanMultipartDuplicates(context, newItem.from);

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
			final ConversationItem newItem, int serverId) {
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
					if (senderUid == DB.myUid()) {
						senderUid = -1;
					}
					updateSentReceivedReadAsync(context, mid, senderUid, false,
							false, false, true, false);
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
				notificationMessageAddition = "\n\nThis new key was sent automatically because last message could not be decrypted.\n\nPLEASE RESEND YOUR LAST MESSAGE!";
			}
			// Divide key and signature
			String[] values = text.split(separator);
			// Be backwoards compatible here! The timestamp values[2] is NEW and
			// optional for a while!
			if (values != null && values.length == 2 || values.length == 3) {
				String encryptedKey = values[0];
				String signature = values[1];
				String timestamp = DB.getTimestampString();
				if (values.length == 3) {
					timestamp = values[2];
				}

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
					// WAIT... before we save the Key we now test the timestamp
					// if we already have a newer key received by the other
					// party
					long tsInternet = Setup.getAESKeyDate(context,
							newItem.from, DB.TRANSPORT_INTERNET);
					long tsSMS = Setup.getAESKeyDate(context, newItem.from,
							DB.TRANSPORT_SMS);
					long tsThisKey = Utility.parseLong(timestamp,
							DB.getTimestamp());
					if (tsInternet > tsThisKey || tsSMS > tsThisKey) {
						// Ouuups... it looks like we received an outdated/old
						// key maybe over a transport medium
						// that was offline for a while. Note that this could be
						// SMS when there is no network or
						// Internet iff the WLAN is down.
						// We do *NOT* want to override the current key and
						// discard the outdated one. We state that:
						possiblyInvalid = "outdated ";
						Utility.showToastAsync(
								context,
								"Received outdated session key"
										+ " from "
										+ Main.UID2Name(context, newItem.from,
												false) + ".");
					} else {
						// Okay the timestamp indicates that this is a newer key
						// than we ever had: So take it!
						// Save as AES key for later decryptying and encrypting
						// usage
						Setup.saveAESKey(context, newItem.from, text);
						// When receiving a key then set update timestamps for
						// both
						// because we have the current key and could possibly
						// use it
						// for both transport ways immediately!
						Setup.setAESKeyDate(context, newItem.from,
								DB.getTimestampString(), DB.TRANSPORT_INTERNET);
						Setup.setAESKeyDate(context, newItem.from,
								DB.getTimestampString(), DB.TRANSPORT_SMS);
						keyhash = Setup.getAESKeyHash(context, newItem.from)
								+ " ";
						// Discard the message
						Utility.showToastAsync(
								context,
								"Received new session key "
										+ keyhash
										+ " from "
										+ Main.UID2Name(context, newItem.from,
												false) + ".");
					}
				} else {
					possiblyInvalid = "invalid ";
					Utility.showToastAsync(
							context,
							"Received invalid session key from "
									+ Main.UID2Name(context, newItem.from,
											false) + ".");
					
					// Try to receive RSA Key (if we have internet connection...)
					Communicator.getKeyFromServer(context, newItem.from, null, serverId);
				}
			} else {
				possiblyInvalid = "invalid ";
			}
			newItem.isKey = true;
			text = "[ " + possiblyInvalid + "session key " + keyhash
					+ "received ]" + notificationMessageAddition;
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
		} else if (text.startsWith("E")) {
			// This is an AES encrypted message
			newItem.encrypted = true;
			text = text.substring(1);
			// Decrypt here
			Key secretKey = Setup.getAESKey(context, newItem.from);
			text = decryptAESMessage(context, text, secretKey);
			
			if (text == null) {
				// DECRYPTION with current key failed, now try the backup key we might have
				// before claiming this message to be failed to decrypted!
				Key secretKeyBackup = Setup.getAESKeyBackup(context, newItem.from);
				if (secretKeyBackup != null) {
					text = decryptAESMessage(context, text, secretKey);
					
					if (text != null) {
						// OK we could use the backup key... anyways do request a new key now
						Communicator.getAESKey(context, newItem.from, true,
								newItem.transport, true, null, false, true);
					}
				}
			}
			
			// FAKE DECRYPTION ERROR HERE
			//text = null;

			if (text == null) {
				// So ... if this still failed, then we might not have the corrent backup key
				// or the wrong RSA key?! ... at least we will no tell the sender that we could
				// not decrypt his message. He may then decide to resend it!
				sendSystemMessageFailed(context, newItem.from, newItem.mid);
				
				text = "[ decryption failed ]";
				int numInvalid = Utility.loadIntSetting(context,
						"invalidkeycounter" + newItem.from, 0);
				if (numInvalid == 0) {
					// TODO: SEND SYSTEM MESSAGE: COULD NOT DECRYPT MID!
					text = "[ decryption failed ]\n\nAutomatically sending new session key. Other user is asked to resend his last message.";
					// ONE TIME REQUEST NEW KEY AUTOMATICALLY ... do this only
					// once because if we have the wrong RSA key this
					// might trigger a cycle forever!!!

					// Invalidate the counter
					Utility.saveIntSetting(context, "invalidkeycounter"
							+ newItem.from, 1);
					
					// Try to receive RSA Key (if we have internet connection...)
					Communicator.getKeyFromServer(context, newItem.from, null, serverId);
					
					// DO THIS AFTER 10 Sek to allow receiving a new RSA key
					// before .. this is NOT bullet safe!
					(new Thread(new Runnable() {
						public void run() {
							try {
								Thread.sleep(7000);
							} catch (InterruptedException e) {
							}
							Communicator.getAESKey(context, newItem.from, true,
									newItem.transport, true, null, false, true);
						}
					})).start();
				}
			} else {
				Log.d("communicator",
						"QQQQQQ handleReceivedText #2  newItem.from="
								+ newItem.from + ", newItem.created="
								+ newItem.created + ", text=" + text);
				// Reset because no error!
				Utility.saveIntSetting(context, "invalidkeycounter"
						+ newItem.from, 0);
			}

			// PrivateKey myPrivateKey = Setup
			// .getPrivateKey(context);
			// text = decryptMessage(context, text,
			// myPrivateKey);
		}

		Log.d("communicator", "MULTIPART START PROCESSING");

		// MULTIPART PROCESSING //
		if (text != null) {
			DB.MultiPartInfo multipartinfo = DB.getMultiPartInfo(text);
			if (multipartinfo != null) {
				// This is a part of a multipart message
				newItem.part = multipartinfo.part;
				newItem.parts = multipartinfo.parts;
				newItem.multipartid = multipartinfo.mulitpartid;
				newItem.text = multipartinfo.text; // text w/o multipart
													// info

				Log.d("communicator", "MULTIPART INFO part=" + newItem.part
						+ ", parts" + newItem.parts + ", multipartid"
						+ newItem.multipartid);

				if (DB.combineMultiPartMessage(context, newItem)) {
					// On the LAST part of all, combine all parts!
					// COMBINING MEANS:
					// - delete all previously received parts
					// - combine the text of all into this newItem!
					// => this newItem MUST be added to the database but
					// will be afterwards as if received completely here!
					// it has no multipart information any more, except the
					// multipartid that is necessary to update the right
					// conversation item!
					Main.updateLastMessage(context, newItem.from, newItem.text,
							newItem.created);

					Log.d("communicator", "MULTIPART COMBINED !!! :)");
				} else {
					Log.d("communicator", "MULTIPART NOT YET COMBINED :(");

				}

				// The text may have been modified by the combine method...
				// yes this
				// is a little tricky :-)
				text = newItem.text;

			} else {
				// This a NON-multipart message, normal proceed
				Main.updateLastMessage(context, newItem.from, text,
						newItem.created);
			}
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
				"@@@@ liveUpdateOrNotify() POSSIBLY CREATE NOTIFICATION #1 from="
						+ newItem.from + ", part=" + newItem.part
						+ ", multipartid=" + newItem.multipartid);

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
							if (!Conversation.scrolledDown && !newItem.isKey
									&& newItem.readyToProcess()) {
								if (newItem.transport == DB.TRANSPORT_INTERNET) {
									Utility.showToastShortAsync(context,
											"New message " + newItem.mid
													+ " received.");
								} else {
									Utility.showToastShortAsync(context,
											"New SMS received.");
								}
							}

							Log.d("communicator",
									"@@@@ liveUpdateOrNotify() POSSIBLY CREATE NOTIFICATION #A , part="
											+ newItem.part
											+ ", newItem.multipartid="
											+ newItem.multipartid);

							if (!newItem.multipartid.equals(DB.NO_MULTIPART_ID)) {
								// This IS a multipart message
								Conversation.hideMultiparts(context,
										newItem.multipartid);
							}
							Conversation.getInstance().updateConversationlist(
									context);
							if (!newItem.multipartid.equals(DB.NO_MULTIPART_ID)) {
								Conversation.setMultipartProgress(context,
										newItem.multipartid, DB
												.getPercentReceivedComplete(
														context,
														newItem.multipartid,
														newItem.from,
														newItem.parts),
										newItem.text);
							}
						} else {
							// The conversation is NOT
							// currently
							// open, we need a new
							// notification
							Log.d("communicator",
									"@@@@ liveUpdateOrNotify() POSSIBLY CREATE NOTIFICATION #2 "
											+ newItem.from);
							if (!newItem.isKey && newItem.readyToProcess()) {
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
								if (!newItem.multipartid
										.equals(DB.NO_MULTIPART_ID)) {
									// This IS a multipart message
									Conversation.hideMultiparts(context,
											newItem.multipartid);
								}
								Conversation.getInstance()
										.updateConversationlist(context);
								if (!newItem.multipartid
										.equals(DB.NO_MULTIPART_ID)) {
									Conversation
											.setMultipartProgress(
													context,
													newItem.multipartid,
													DB.getPercentReceivedComplete(
															context,
															newItem.multipartid,
															newItem.from,
															newItem.parts),
													newItem.text);
								}

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
	 *            the use of the session key: sending timeout is earlier than
	 *            the receivig timeout
	 * @param automatic
	 *            the automatic if automatic, then extra countdown is
	 *            established
	 * @return the AES key
	 */
	public static Key getAESKey(Context context, int uid,
			boolean forceSendingNewKey, int transport,
			boolean flagErrorMessageNotification, ConversationItem item,
			boolean forSending, boolean automatic) {
		// 1. Search for AES key, if no key (or outdated), then
		// a. generate one
		// b. save it for later use
		// c. sent it first in a K ey message
		// d. use the new key

		boolean outDatedInternet = Setup.isAESKeyOutdated(context, uid,
				forSending, DB.TRANSPORT_INTERNET);
		boolean outDatedSMS = Setup.isAESKeyOutdated(context, uid, forSending,
				DB.TRANSPORT_SMS);
		Log.d("communicator", "#### KEY FOR " + uid + " TIMEOUT? "
				+ (outDatedInternet || outDatedSMS));

		boolean outdatedOrNeedsResending = (outDatedInternet && transport == DB.TRANSPORT_INTERNET)
				|| (outDatedSMS && transport == DB.TRANSPORT_SMS);
		boolean outdatedBoth = outDatedInternet && outDatedSMS;

		if (!outdatedOrNeedsResending && !forceSendingNewKey) {
			// Key is up-to-date - AT LEAST for the current transport (hopefully
			// the other part has it as well!)
			Key secretKey = Setup.getAESKey(context, uid);
			Log.d("communicator",
					"#### KEY RETURNING " + uid + " : "
							+ Setup.getAESKeyHash(context, uid));
			return secretKey;
		} else {
			Key savedOrNewKey = Setup.getAESKey(context, uid);
			String savedOrNewKeyAsString = null;
			long keyTimestamp = DB.getTimestamp();

			// Only generate a new key if outdated or forceupdate.
			// Otherwise re-send the current key by the current media and
			// don't forget to set the timestamp for this media.
			if (outdatedBoth || forceSendingNewKey) {
				// Okay... really a new key is needed...
				// Use the last received message as a random seed
				String lastMsg = Main.getLastMessage(context, uid);
				// Log.d("communicator", "###### RANDOM SEED lastMsg " +
				// lastMsg);
				String randomSeed = lastMsg + "-" + DB.getTimestamp() + "";
				// Log.d("communicator", "###### RANDOM SEED " + randomSeed);
				savedOrNewKey = Setup.generateAESKey(randomSeed);
				savedOrNewKeyAsString = Setup.serializeAESKey(savedOrNewKey);
				// Save new key
				Setup.saveAESKey(context, uid, savedOrNewKeyAsString);
				// Because this is a NEW key now ... we have to
				// Invalidate key timestamp of OTHER transport method, also
				// Invalidate DB.getLastKeyMessage() cache set to -1 ==
				// awaiting!
				Utility.saveIntSetting(context, Setup.LASTKEYMID + uid, -1);
				if (transport == DB.TRANSPORT_INTERNET) {
					Log.d("communicator",
							" KEYUPDATE: getAESKey() invalidate SMS timestamp");
					Setup.setAESKeyDate(context, uid, null, DB.TRANSPORT_SMS);
				} else {
					Log.d("communicator",
							" KEYUPDATE: getAESKey() invalidate Internet timestamp");
					Setup.setAESKeyDate(context, uid, null,
							DB.TRANSPORT_INTERNET);
				}
			} else {
				// Okay ... we have a key that is not outdated but we sent it
				// over to the other person using the OTHER transport medium not
				// the one we would like to use now. So we should resend the
				// current key over THIS transport medium.
				savedOrNewKey = Setup.getAESKey(context, uid);
				savedOrNewKeyAsString = Setup.serializeAESKey(savedOrNewKey);
				// Do not await any more!
				Utility.saveIntSetting(context, Setup.LASTKEYMID + uid, -2);
				// We need to save the older timestamp of the other transport
				// here!
				if (transport == DB.TRANSPORT_INTERNET) {
					keyTimestamp = Setup.getAESKeyDate(context, uid,
							DB.TRANSPORT_SMS);
				} else {
					keyTimestamp = Setup.getAESKeyDate(context, uid,
							DB.TRANSPORT_INTERNET);
				}
			}

			if (automatic || true) {
				// Set extra count down that prevents immediate sending of
				// the next message in order to allow the key message to be
				// received before any next message!
				Setup.extraCrountDownSet(context, transport);
			}

			// Save the timestamp of the CURRENT transport
			Setup.setAESKeyDate(context, uid, keyTimestamp + "", transport);
			Log.d("communicator",
					" KEYUPDATE: getAESKey() SAVE NEW KEY FOR TRANPSORT="
							+ transport);

			String keyhash = Setup.getAESKeyHash(context, uid);
			// RSA encrypt here
			PublicKey pubKeyOther = Setup.getKey(context, uid);
			String encryptedKey = encryptMessage(context,
					savedOrNewKeyAsString, pubKeyOther);
			// Sign the KEY
			PrivateKey myPrivateKey = Setup.getPrivateKey(context);
			String keyHash = Utility.md5(savedOrNewKeyAsString);
			String signature = encryptMessage(context, keyHash, myPrivateKey);
			String separator = KEY_OK_SEPARATOR;
			if (flagErrorMessageNotification) {
				// This indicates the error!!!
				separator = KEY_ERROR_SEPARATOR;
			}
			// NEW: Send a timestamp so that we can rule out outdated keys that
			// we might receive LATER over
			// the other transport medium that currently might not work!
			String msgText = "K" + encryptedKey + separator + signature
					+ separator + DB.getTimestampString();
			// Sent a KEY-Message via the same transport as the original message
			// should go!
			DB.addSendMessage(context, uid, msgText, false, transport, true,
					DB.PRIORITY_KEY, item);
			Conversation.updateConversationlistAsync(context);
			Communicator.sendNewNextMessageAsync(context, transport);
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

		// If transport is INTERNET then use the NEXT server that we have messages
		// to send for in a Round Robin fashion. If transport is SMS stick with
		// serverId -1.
		ConversationItem itemToSend = null; 
		if (transport == DB.TRANSPORT_INTERNET) {
			// Get the next message considering all servers we have an account
			// for in a RR style.
			itemToSend = Setup.getNextSendingServerMessage(context);
		} else {
			// Lookup only if we expect something to send
			itemToSend = DB.getNextMessage(context,
					DB.TRANSPORT_SMS, -1);
		}
		
		if (itemToSend == null) {
			if (transport == DB.TRANSPORT_INTERNET) {
				Communicator.messagesToSend = false;
				 Log.d("communicator",
				 "SEND NEXT QUERY sendNextMessage() DETECTED  itemToSend = " + itemToSend + " => messagesToSend:=false");
			} else {
				 Log.d("communicator",
				 "SEND NEXT QUERY sendNextMessage() DETECTED  itemToSend = " + itemToSend + " => SMStoSend:=false");
				Communicator.SMSToSend = false;
			}
		}
		
		

		if (itemToSend != null) {
			if (!itemToSend.isKey && !Setup.extraCountDownToZero(context)) {
				// If extra countdown is established, we only allow key messages
				// to
				// be sent!
				return;
			}
			// Increment the number of tries for this item
			DB.incrementTries(context, itemToSend.localid);

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
						false, itemToSend.transport, false, itemToSend, true,
						true);
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
			String toastText = "Error sending message " + itemToSend.sendingid
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
			int part = DB.DEFAULT_MESSAGEPART;
			int parts = 1;
			if (itemToSend != null) {
				localId = itemToSend.localid;
				hostUid = itemToSend.to;
				sendingId = itemToSend.sendingid;
				part = itemToSend.part;
				parts = itemToSend.parts;
			}
			// Utility.showToastAsync(context, "Sending SMS Message "
			// +localId+" to " + phone);
			boolean registeredReceipient = to >= 0;
			Log.d("communicator", "#### sendMessageSMS() #3 ");
			SendSMS.sendSMS(context, phone, messageTextString, localId,
					hostUid, sendingId, part, parts, registeredReceipient);
		} else {
			// MUST eliminate message because otherwise next messages will get
			// stuck...
			if (itemToSend != null && itemToSend.text != null
					&& itemToSend.sendingid != -1) {
				Utility.copyToClipboard(context, itemToSend.text);
				Utility.showToastAsync(context,
						"Error sending SMS message, no phone number for user "
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
		// Guard against invalid (SMS) users...
		if (to < 0) {
			return;
		}

		final int serverId = Setup.getServerId(context, to);

		String session = Setup.getTmpLogin(context, serverId);
		if (session == null) {
			// Error resume is automatically done by getTmpLogin, not logged in
			return;
		}

		String url = null;
		int toSUid = Setup.getSUid(context, to);
		String encUid = Setup.encUid(context, toSUid, serverId);
		if (encUid == null) {
			// Secret may be not set yet, try again later!
			return;
		}

		Log.d("communicator", "SEND NEXT MESSAGE msgText=" + msgText);

		url = Setup.getBaseURL(context, serverId) + "cmd=send&session="
				+ Utility.urlEncode(session) + "&host=" + encUid + "&val="
				+ Utility.urlEncode(created + "#" + msgText);

		//
		// // TEST GET REQUEST JUST TO COMPARE
		// final String url2222 = Setup.getBaseURL(context) +
		// "cmd=send&session=" + Utility.encode(session)
		// + "&host=" + Utility.encode(encUid) + "&val=" +
		// Utility.encode(created + "#" + msgText);
		// Log.d("communicator", "SEND NEXT MESSAGE2222: " + url2222);
		// HttpStringRequest httpStringRequest2222 = (new
		// HttpStringRequest(context,
		// url2222, false, new HttpStringRequest.OnResponseListener() {
		// public void response(String response) {
		// Log.d("communicator",
		// "SEND NEXT MESSAGE2222 OK!!! " + response);
		// }
		// }));

		Log.d("communicator", "SEND NEXT MESSAGE: " + url);
		final String url2 = url;
		@SuppressWarnings("unused")
		HttpStringRequest httpStringRequest = (new HttpStringRequest(context,
				url2, true, new HttpStringRequest.OnResponseListener() {
					public void response(String response) {
						// Log.d("communicator",
						// "SEND NEXT MESSAGE RESPONSE: " + response);
						
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
												.me() && itemToSend.isKey;

										Log.d("communicator", "KEYMESSAGE "
												+ itemToSend.mid + ", "
												+ isSentKeyMessage);

										// Be careful here! The sending table
										// may have a different
										// msg than the msg DB because we might
										// have a multipart
										// message. => DO NOT UPDATE THE TEXT!
										itemToSend.text = null;
										DB.updateMessage(context, itemToSend,
												itemToSend.to, isSentKeyMessage);
										updateSentReceivedReadAsync(context,
												itemToSend.mid, to, true,
												false, false, false, false);
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
											false, serverId);
									// Setup.invalidateTmpLogin(context);
								} else {
									Setup.possiblyInvalidateSession(context,
											false, serverId);
								}
							}
						}

						if (success) {
							// Clear errors and super fast reschedule
							Setup.possiblyInvalidateSession(context, true,
									serverId); // reset
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
	public static void createNotification(final Context context,
			ConversationItem item) {
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

		String completeMessage = item.text;
		if (completeMessage == null) {
			completeMessage = "";
		}
		String completeTextWithoutImages = Conversation
				.possiblyRemoveImageAttachments(context, completeMessage, true,
						"[ image ]", -1);

		int cnt = getNotificationCount(context, item.from);
		String title = Main.UID2Name(context, item.from, false);
		String text = completeTextWithoutImages;
		if (cnt > 1) {
			text = cnt + " new messages";
		}

		int maxWidth = Utility.getScreenWidth(context) - 80;
		text = Utility.cutTextIntoOneLine(text, maxWidth, 25);

		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
				context).setSmallIcon(R.drawable.msgsmall24x24)
				.setPriority(NotificationCompat.PRIORITY_MAX)
				.setCategory(NotificationCompat.CATEGORY_ALARM)
				.setTicker(title + ": " + completeTextWithoutImages).setWhen(0)
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
			final String keyhash, final int serverId) {
		String session = Setup.getTmpLoginEncoded(context, serverId);
		if (session == null) {
			// Error resume is automatically done by getTmpLogin, not logged in
			Utility.showToastAsync(
					context,
					"Error sending account key " + keyhash + " to "
							+ Setup.getServerLabel(context, serverId, true)
							+ "! (1) - Disabling Encryption!");
			// Disable in settings
			Utility.saveStringSetting(context, Setup.PUBRSAKEY, null);
			Utility.saveStringSetting(context, Setup.PRIVATERSAKEY, null);
			Utility.saveBooleanSetting(context, Setup.OPTION_ENCRYPTION, false);
			// Try to delete keys on all servers
			Setup.disableEncryption(context);
			return;
		}

		String url = null;
		url = Setup.getBaseURL(context, serverId) + "cmd=sendkey&session="
				+ session + "&val=" + Utility.urlEncode(key);

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
							Utility.showToastAsync(
									context,
									"New account key "
											+ keyhash
											+ " sent to "
											+ Setup.getServerLabel(context,
													serverId, true) + ".");
						} else {
							Utility.showToastAsync(
									context,
									"Error sending account "
											+ keyhash
											+ " key to "
											+ Setup.getServerLabel(context,
													serverId, true)
											+ "! (2) - Disabling Encryption!");
							// disable in settings
							Utility.saveStringSetting(context, Setup.PUBRSAKEY,
									null);
							Utility.saveStringSetting(context,
									Setup.PRIVATERSAKEY, null);
							Utility.saveBooleanSetting(context,
									Setup.OPTION_ENCRYPTION, false);
							// Try to delete keys on all servers
							Setup.disableEncryption(context);
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
			final String keyhash, final int serverId) {
		String session = Setup.getTmpLoginEncoded(context, serverId);
		if (session == null) {
			// Error resume is automatically done by getTmpLogin, not logged in
			Utility.showToastAsync(
					context,
					"Error clearing account " + keyhash + " key from "
							+ Setup.getServerLabel(context, serverId, true)
							+ "! (1)");
			return;
		}

		String url = null;
		url = Setup.getBaseURL(context, serverId) + "cmd=clearkey&session="
				+ session;

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
							Utility.showToastAsync(
									context,
									"Account key "
											+ keyhash
											+ " cleared from "
											+ Setup.getServerLabel(context,
													serverId, true) + ".");
						} else {
							Utility.showToastAsync(
									context,
									"Error clearing account key "
											+ keyhash
											+ " from "
											+ Setup.getServerLabel(context,
													serverId, true) + "! (2)");
						}

					}
				}));

	}

	// -------------------------------------------------------------------------

	/**
	 * Update keys from ALL servers. This should be done on manual refresh or if
	 * encryption failed for a registered users!
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
	public static void updateKeysFromAllServers(final Context context,
			final List<Integer> uidList, final boolean forceUpdate,
			final Main.UpdateListener updateListener) {
		for (int serverId : Setup.getServerIds(context)) {
			if (Setup.isServerAccount(context, serverId)) {
				updateKeysFromServer(context, uidList, forceUpdate,
						updateListener, serverId);
			}
		}
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
			final Main.UpdateListener updateListener, final int serverId) {

		long lastTime = Utility.loadLongSetting(context,
				Setup.SETTING_LASTUPDATEKEYS + serverId, 0);
		long currentTime = DB.getTimestamp();
		if (!forceUpdate
				&& (lastTime + Setup.UPDATE_KEYS_MIN_INTERVAL + serverId > currentTime)) {
			// Do not do this more frequently
			return;
		}
		Utility.saveLongSetting(context, Setup.SETTING_LASTUPDATEKEYS
				+ serverId, currentTime);

		String uidliststring = "";
		final ArrayList<Integer> sentList = new ArrayList<Integer>();
		for (int uid : uidList) {
			// not do this of fake UIDs (sms-only users!) or if the user is not
			// from THIS server!
			if (uid > 0) {
				if (Setup.getServerId(context, uid) == serverId) {
					int suid = Setup.getSUid(context, uid);
					sentList.add(uid);
					if (uidliststring.length() != 0) {
						uidliststring += "#";
					}
					uidliststring += Setup.encUid(context, suid, serverId);
				}
			}
		}

		String session = Setup.getTmpLoginEncoded(context, serverId);
		if (session == null) {
			// error resume is automatically done by getTmpLogin, not logged in
			return;
		}

		String url = null;
		url = Setup.getBaseURL(context, serverId) + "cmd=haskey&session="
				+ session + "&val=" + Utility.urlEncode(uidliststring);

		Log.d("communicator", "###### REQUEST HAS KEY " + url);
		final String url2 = url;
		@SuppressWarnings("unused")
		HttpStringRequest httpStringRequest = (new HttpStringRequest(context,
				url2, new HttpStringRequest.OnResponseListener() {
					public void response(String response) {
						if (isResponseValid(response)) {
							// Log.d("communicator",
							// "###### HAS KEY VALUES RECEIVED!!! "
							// + response);
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
															uid,
															updateListener,
															serverId);
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
	 * Update phones from ALL servers. This should only be used on start up or
	 * manual refresh.
	 * 
	 * @param context
	 *            the context
	 * @param uidList
	 *            the uid list
	 * @param forceUpdate
	 *            the force update
	 */
	public static void updatePhonesFromAllServers(final Context context,
			final List<Integer> uidList, final boolean forceUpdate) {
		for (int serverId : Setup.getServerIds(context)) {
			if (Setup.isServerAccount(context, serverId)) {
				updatePhonesFromServer(context, uidList, forceUpdate, serverId);
			}
		}
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
			final List<Integer> uidList, final boolean forceUpdate,
			final int serverId) {
		Log.d("communicator", "###### REQUEST HAS PHONE #1");

		long lastTime = Utility.loadLongSetting(context,
				Setup.SETTING_LASTUPDATEPHONES + serverId, 0);
		long currentTime = DB.getTimestamp();
		if (!Setup.isSMSOptionEnabled(context, serverId)) {
			// if no sms option is enabled, then do not retrieve keys!
			return;
		}
		if (!forceUpdate
				&& (lastTime + Setup.UPDATE_PHONES_MIN_INTERVAL + serverId > currentTime)) {
			// Do not do this more frequently
			return;
		}
		Utility.saveLongSetting(context, Setup.SETTING_LASTUPDATEPHONES
				+ serverId, currentTime);

		String uidliststring = "";
		final ArrayList<Integer> uidListUsed = new ArrayList<Integer>();
		for (int uid : uidList) {
			// not do this of fake UIDs (sms-only users!) or if this user is not
			// from THIS server
			if (uid > 0) {
				if (Setup.getServerId(context, uid) == serverId) {
					int suid = Setup.getSUid(context, uid);
					uidListUsed.add(uid);
					if (uidliststring.length() != 0) {
						uidliststring += "#";
					}
					uidliststring += Setup.encUid(context, suid, serverId);
				}
			}
		}
		if (uidliststring.equals("")) {
			return;
		}

		String session = Setup.getTmpLoginEncoded(context, serverId);
		if (session == null) {
			// error resume is automatically done by getTmpLogin, not logged in
			return;
		}

		String url = null;
		url = Setup.getBaseURL(context, serverId) + "cmd=hasphone&session="
				+ session + "&val=" + Utility.urlEncode(uidliststring);

		// Log.d("communicator", "###### REQUEST HAS PHONE (" + uidliststring
		// + ") " + url);

		final String url2 = url;
		@SuppressWarnings("unused")
		HttpStringRequest httpStringRequest = (new HttpStringRequest(context,
				url2, new HttpStringRequest.OnResponseListener() {
					public void response(String response) {
						if (isResponseValid(response)) {
							// Log.d("communicator",
							// "###### HAS PHONE VALUES RECEIVED!!! response="
							// + response);
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
														value, serverId);
												if (value != null) {
													boolean isUpdate = Main
															.isUpdatePhone(
																	context,
																	uid);
													if (isUpdate) {
														Setup.savePhone(
																context, uid,
																value, false);
													}
												}
												// Log.d("communicator",
												// "###### RESPONSE HAS PHONE SAVE FOR "+Main.UID2Name(context,
												// uid, false)+": "
												// + value);
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
			final Main.UpdateListener updateListener, final int serverId) {

		if (uid < 0) {
			// Do not request keys for SMS/external users!
			return;
		}

		String session = Setup.getTmpLoginEncoded(context, serverId);
		if (session == null) {
			// error resume is automatically done by getTmpLogin, not logged in
			return;
		}

		String url = null;
		int sUid = Setup.getSUid(context, uid);
		url = Setup.getBaseURL(context, serverId) + "cmd=getkey&session="
				+ session + "&val=" + Setup.encUid(context, sUid, serverId);

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
	 * Send system message failed. This message will tell the sender that his
	 * message could not be decrypted.
	 * System messages are sent over normal sending
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
	public static void sendSystemMessageFailed(final Context context,
			final int uid, final int mid) {
		// uid = user from which we have read messages
		// mid = largest mid that we have read
		if (uid >= 0 && mid != -1) {
			DB.addSendMessage(context, uid, "F" + mid, false,
					DB.TRANSPORT_INTERNET, true, DB.PRIORITY_FAILEDTODECRYPT);
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
