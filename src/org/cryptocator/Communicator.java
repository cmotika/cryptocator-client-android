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

import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.cryptocator.R;

import android.R.integer;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ClipData.Item;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.text.style.LineHeightSpan.WithDensity;
import android.util.Base64;
import android.util.Log;
import android.view.FocusFinder;
import android.widget.Toast;

public class Communicator {

	public static boolean messageSent = false;
	public static boolean messageReceived = false;

	public static boolean haveNewMessages = false;
	public static boolean messagesToSend = false;
	public static boolean SMSToSend = false;
	public static boolean messagesToSendIsUpToDate = false; // if the class is
	// // killed, this is
	// // false again
	// it will be set by the scheduler that evaluates and uses messagesToSend as
	// a cache!

	public static boolean internetOk = true;
	public static boolean loginOk = true;
	public static boolean accountNotActivated = false;

	public static boolean lastSendInternet = false; // used to toggle

	public static String KEY_OK_SEPARATOR = "@";
	public static String KEY_ERROR_SEPARATOR = "@@";

	// ----------------------------------------------------------------------------------

	// This method should help to save communication, it quickly checks for new
	// messages and only if there are messages waiting
	// it triggeres the receive next message
	public static void haveNewMessagesAndReceive(final Context context) {
		if (haveNewMessages) {
			// If we already know we have new messages, we not need to run the
			// light-weight have-request!
			receiveNextMessage(context);
			return;
		}

		// largest timestamp received
		final int largestMid = DB.getLargestMid(context);

		String sessionid = Setup.getSessionID(context);

		String url = null;
		url = Setup.getBaseURL(context)
				+ "cmd=have&session="
				+ sessionid
				+ "&val="
				+ Utility.encode(largestMid + "#"
						+ DB.getLargestTimestampReceived(context) + "#"
						+ DB.getLargestTimestampRead(context));

		Log.d("communicator", "REQUEST HAVE: " + url);
		final String url2 = url;
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
	// ----------------------------------------------------------------------------------

	// public static void updateReceiveInfo(final Context context) {
	// // largest timestamp received
	// String timestamp = DB.getLargestTimestampReceived(context);
	// if (timestamp.equals("")) {
	// timestamp = DB.MAXTIMESTAMP + "";
	// }
	//
	// String session = Setup.getTmpLogin(context);
	// if (session == null) {
	// // error resume is automatically done by getTmpLogin, not logged in
	// return;
	// }
	//
	// String url = null;
	// url = Setup.getBaseURL(context) + "cmd=inforeceived&session=" + session +
	// "&val="
	// + timestamp;
	// Log.d("communicator", "REQUEST RECEIVE INFO: " + url);
	// final String url2 = url;
	// HttpStringRequest httpStringRequest = (new HttpStringRequest(context,
	// url2, new HttpStringRequest.OnResponseListener() {
	// public void response(String response) {
	// if (isResponseValid(response)) {
	// if (isResponsePositive(response)) {
	// // Log.d("communicator",
	// // "RECEIVED INFO RECEIVEDX OK!!! "
	// // + response2);
	// if (response.startsWith("1##")) {
	// // Log.d("communicator", "RECEIVEDX #1 ");
	// updateReceivedInfoHandleResponse(context,
	// response);
	// }
	// }
	// }
	// }
	// }));
	// }

	// private static void updateReceivedInfoHandleResponse(Context context,
	// String response) {
	// String[] values = response.substring(3).split("##");
	// if (values != null && values.length > 0) {
	// for (String pair : values) {
	// String pairValues[] = pair.split("#");
	// if (pairValues != null && pairValues.length == 2) {
	// int mid = Utility.parseInt(pairValues[0], -1);
	// String timestamp = pairValues[1];
	// // Log.d("communicator", "RECEIVEDX #2 " + mid + ", "
	// // + timestamp);
	// if (mid > -1) {
	// // READ NOTIFICATION
	// // now we have correct values and should update the
	// // database
	// DB.updateMessageReceived(context, mid, timestamp);
	// int hostUid = DB.getHostUidByMid(context, mid);
	// updateSentReceivedReadAsync(context, mid, hostUid,
	// false, true, false, false);
	// }
	// }
	// }
	// }
	// }

	// ----------------------------------------------------------------------------------

	// public static void updateWithdrawInfo(final Context context) {
	// // largest timestamp received
	// String timestamp = DB.getLargestTimestampWithdraw(context);
	// if (timestamp.equals("")) {
	// timestamp = DB.MAXTIMESTAMP + "";
	// }
	//
	// String session = Setup.getTmpLogin(context);
	// if (session == null) {
	// // error resume is automatically done by getTmpLogin, not logged in
	// return;
	// }
	//
	// String url = null;
	// url = Setup.getBaseURL(context) + "cmd=infowithdraw&session=" + session +
	// "&val=" +
	// timestamp;
	// Log.d("communicator", "REQUEST WITHDRAW INFO: " + url);
	// final String url2 = url;
	// HttpStringRequest httpStringRequest = (new HttpStringRequest(context,
	// url2, new HttpStringRequest.OnResponseListener() {
	// public void response(String response) {
	// if (isResponseValid(response)) {
	// if (isResponsePositive(response)) {
	// // Log.d("communicator",
	// // "WITHDRAW INFO RECEIVED OK!!! "
	// // + response2);
	// if (response.startsWith("1##")) {
	// updateWithdrawInfoHandleResponse(context,
	// response);
	// }
	// }
	// }
	// }
	// }));
	// }

	// private static void updateWithdrawInfoHandleResponse(Context context,
	// String response) {
	// String[] values = response.substring(3).split("##");
	// if (values != null && values.length > 0) {
	// for (String pair : values) {
	// String pairValues[] = pair.split("#");
	// if (pairValues != null && pairValues.length == 2) {
	// int mid = Utility.parseInt(pairValues[0], -1);
	// String timestamp = pairValues[1];
	// if (mid > -1) {
	// // WITHDRAWN NOTIFICATION!
	// // now we have correct values and should update the
	// // database
	// DB.updateMessageWithdrawn(context, mid, timestamp);
	// int senderUid = DB.getSenderUidByMid(context, mid);
	// updateSentReceivedReadAsync(context, mid, senderUid,
	// false, false, false, true);
	// }
	// }
	// }
	// }
	//
	// }

	// ----------------------------------------------------------------------------------

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

	// -----

	// public static void updateReadInfo(final Context context) {
	// // if we do not refuse to send these confirmations...
	// if (!Utility.loadBooleanSetting(context, Setup.OPTION_NOREAD,
	// Setup.DEFAULT_NOREAD)) {
	// // ...then go ahead and also receive them
	//
	// // largest timestamp read
	// String timestamp = DB.getLargestTimestampRead(context);
	// String timestamp2 = Utility.loadStringSetting(context,
	// "timestampread", "0");
	// long ts = Utility.parseLong(timestamp, 0);
	// long ts2 = Utility.parseLong(timestamp2, 0);
	// if (ts2 > ts) {
	// // this prevents enabling read confirmation just for a shot
	// // instance of time
	// // you never are able to receive read confirmations for the
	// // duration you
	// // not allowed them!
	// timestamp = timestamp2;
	// }
	// if (timestamp.equals("")) {
	// timestamp = DB.MAXTIMESTAMP + "";
	// }
	//
	// String session = Setup.getTmpLogin(context);
	// if (session == null) {
	// // error resume is automatically done by getTmpLogin, not logged in
	// return;
	// }
	//
	// String uidString = Utility.loadStringSetting(context, "uid", "");
	// String pwdString = Utility.loadStringSetting(context, "pwd", "");
	//
	// String url = null;
	// url = Setup.getBaseURL(context) + "cmd=inforead&session=" + session +
	// "&val=" +
	// timestamp;
	// Log.d("communicator", "REQUEST READ INFO: " + url);
	// final String url2 = url;
	// HttpStringRequest httpStringRequest = (new HttpStringRequest(
	// context, url2, new HttpStringRequest.OnResponseListener() {
	// public void response(String response) {
	// if (isResponseValid(response)) {
	// if (isResponsePositive(response)) {
	// // Log.d("communicator",
	// // "READ INFO RECEIVED OK!!! "
	// // + response2);
	// if (response.startsWith("1##")) {
	// updateReadInfoHandleResponse(context,
	// response);
	// }
	// }
	// }
	// }
	// }));
	// }
	// }

	// private static void updateReadInfoHandleResponse(Context context,
	// String response) {
	// String[] values = response.substring(3).split("##");
	// if (values != null && values.length > 0) {
	// for (String pair : values) {
	// String pairValues[] = pair.split("#");
	// if (pairValues != null && pairValues.length == 2) {
	// int mid = Utility.parseInt(pairValues[0], -1);
	// String timestamp = pairValues[1];
	// if (mid > -1) {
	// // now we have correct values and should update the
	// // database
	// DB.updateMessageRead(context, mid, timestamp);
	// int hostUid = DB.getHostUidByMid(context, mid);
	// updateSentReceivedReadAsync(context, mid, hostUid,
	// false, false, true, false);
	// }
	// }
	// }
	// }
	// }

	// Idee: Mapping item -> View um die view incrementell zu aktualisieren!
	// (ImageView.setVisible(View.Gone)...)

	// ----------------------------------------------------------------------------------
	// ----------------------------------------------------------------------------------

	public static void receiveNextMessage(final Context context) {
		messageReceived = false;

		final int largestMid = DB.getLargestMid(context);
		final boolean discardMessageAndSaveLargestMid = (largestMid == -1); // this
																			// should
																			// be
																			// the
																			// case
																			// for
																			// an
																			// empty
																			// database
																			// only!

		String session = Setup.getTmpLogin(context);
		if (session == null) {
			// error resume is automatically done by getTmpLogin, not logged in
			return;
		}

		String url = null;
		url = Setup.getBaseURL(context) + "cmd=receive&session=" + session
				+ "&val=" + largestMid;
		Log.d("communicator", "RECEIVE NEXT MESSAGE: " + url);
		final String url2 = url;
		HttpStringRequest httpStringRequest = (new HttpStringRequest(context,
				url2, new HttpStringRequest.OnResponseListener() {
					public void response(String response) {
						boolean success1 = false;
						boolean success2 = false;
						boolean wronglyDencoded = false;
						boolean resposeError = true;
						final ConversationItem newItem = new ConversationItem();
						if (isResponseValid(response)) {
							// final String response2 = response;
							if (response.equals("0")) {
								haveNewMessages = false;
								resposeError = false;
								loginOk = true;
								// we currently do not need to look for new
								// messages
							} else if (isResponsePositive(response)) {
								resposeError = false;
								loginOk = true;
								// Log.d("communicator",
								// "RECEIVE NEXT MESSAGE OK!!! "
								// + response);

								// response should have the form
								// senttimestamp#mid

								// update database
								String[] responseArray = response.split("#");
								if (responseArray.length == 6) {
									// a message was received
									String mid = responseArray[1];
									String from = Setup.decUid(context,
											responseArray[2]) + "";

									Log.d("communicator",
											"RECEIVE NEXT MESSAGE DECODED UIDS: "
													+ from + " -> me " + " : "
													+ responseArray[2]);
									if (from.equals("-2")) {
										// enforce new session if this happens
										// twice
										wronglyDencoded = true; // we want fast
																// retry
										Setup.possiblyInvalidateSession(
												context, false);
									}

									if (!from.equals("-2")) {
										// only not update in case or wrong
										// decoding because we want to later try
										// again!
										DB.updateLargestMid(context,
												Utility.parseInt(mid, 0));
									}

									if (from.equals("-1")) {
										// invalid users
										// update cache
										DB.lastReceivedMid = newItem.mid;
										Utility.saveIntSetting(context,
												Setup.SETTINGS_DEFAULTMID,
												newItem.mid);
									}

									// discard unknown or invalid users
									if (!from.equals("-2")
											&& !from.equals("-1")) {
										Setup.possiblyInvalidateSession(
												context, true); // reset
										success1 = true;
										// uids could be recovered/decrypted
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

										// update cache
										DB.lastReceivedMid = newItem.mid;
										Utility.saveIntSetting(context,
												Setup.SETTINGS_DEFAULTMID,
												newItem.mid);

										if (Conversation.isVisible()
												&& Conversation.getHostUid() == newItem.from) {
											newItem.read = DB.getTimestamp();
											;
										}

										// skip if not in our list && ignore is
										// turned on
										if (!discardMessageAndSaveLargestMid) {
											newItem.text = handleReceivedText(
													context, text, newItem);
											success2 = updateUIForReceivedMessage(
													context, newItem);

											if (newItem.text == null
													|| newItem.text.equals("")) {
												// no toast or scrolling on
												// system messages
												// no notification
												success2 = false;
											}

											if (newItem.text
													.contains("[ invalid session key ")
													|| newItem.text
															.contains("[ decryption failed ]")) {
												// we should try to update the
												// public rsa key of this user
												Communicator.updateKeysFromServer(
														context,
														Main.loadUIDList(context),
														true, null);
											}
										} else {
											// discard means no live update
											// please...
											success2 = false;
										}
									} // end uids decrypted sucessfully
								}
							} else {
								// invalidate right away
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

	public static boolean updateUIForReceivedMessage(Context context,
			ConversationItem newItem) {
		List<Integer> uidList = Main.loadUIDList(context);
		boolean skip = false;
		boolean success2 = false;
		if (!Main.alreadyInList(newItem.from, uidList)) {
			// the user who sent us a message is not
			// in our list! What now?
			boolean ignore = Utility.loadBooleanSetting(context,
					Setup.OPTION_IGNORE, Setup.DEFAULT_IGNORE);
			if (!ignore) {
				// The user must be added to our
				// list
				uidList.add(newItem.from);
				DB.ensureDBInitialized(context, uidList);
				Main.saveUIDList(context, uidList);
				Main.possiblyRebuildUserlistAsync(context, false);
			} else {
				skip = true;
			}
		}

		if (skip) {
			messageReceived = true;
			// Log.d("communicator",
			// "RECEIVED MESSAGE SKIPPED!!! "
			// + response2);
		} else if (DB.isAlreadyInDB(context, newItem.mid, newItem.from)) {
			// not insert again!
		} else if (DB.updateMessage(context, newItem, newItem.from)) {
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

	public static String handleReceivedText(final Context context, String text,
			final ConversationItem newItem) {
		// if (text.startsWith("D")) {
		// // D == downloaded (a messages from us was received by the sender of
		// // this message at its creation time)
		// // READ NOTIFICATION
		// // now we have correct values and should update the
		// // database
		// int mid = Utility.parseInt(text.substring(1), -1);
		// Log.d("communicator", "UPDATE RECEIVED MESSAGE mid=" + mid);
		// if (mid > -1) {
		// int senderUid = newItem.from;
		// DB.updateMessageReceived(context, mid, newItem.created + "",
		// senderUid);
		// DB.updateMessageSystem(context, newItem.mid, true, newItem.from);
		// // int hostUid = DB.getHostUidByMid(context, mid);
		// updateSentReceivedReadAsync(context, mid, senderUid, false,
		// true, false, false);
		// }
		// // invalidate text (so that no new message is inserted!)
		// text = "";
		// } else if (text.startsWith("R")) {
		// // R == read
		// // we got a read confirmation
		// // now we have correct values and should update the
		// // database
		// int mid = Utility.parseInt(text.substring(1), -1);
		// if (mid > -1) {
		// int senderUid = newItem.from;
		// DB.updateMessageRead(context, mid, newItem.created + "",
		// senderUid);
		// DB.updateMessageSystem(context, newItem.mid, true, newItem.from);
		// // int hostUid = DB.getHostUidByMid(context, mid);
		// updateSentReceivedReadAsync(context, mid, senderUid, false,
		// false, true, false);
		// }
		// text = "";
		// } else
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
			// this is an RSA encrypted key message!
			text = text.substring(1);
			String possiblyInvalid = "";
			String keyhash = "";

			String separator = KEY_OK_SEPARATOR;
			String notificationMessageAddition = "";
			if (text.contains(KEY_ERROR_SEPARATOR)) {
				separator = KEY_ERROR_SEPARATOR;
				notificationMessageAddition = "\n\nNew key was sent automatically because last message could not be decrypted.\n\nPLEASE RESEND YOUR LAST MESSAGE!";
			}
			// divide key and signature
			String[] values = text.split(separator);
			if (values != null && values.length == 2) {
				String encryptedKey = values[0];
				String signature = values[1];
				// get encryptedhash from signature
				PublicKey senderPubKey = Setup.getKey(context, newItem.from);
				String decryptedKeyHashMustBe = decryptMessage(context,
						signature, senderPubKey);

				// decrypt here
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
					// save as AES key for later
					// decryptying
					// and encrypting usage
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
			// this is an unencrypted message
			newItem.encrypted = false;
			text = text.substring(1);
			text = text.replace("@@@NEWLINE@@@",
					System.getProperty("line.separator"));
			Log.d("communicator", "QQQQQQ handleReceivedText #1  newItem.from="
					+ newItem.from + ", newItem.created=" + newItem.created
					+ ", text=" + text);
			Main.updateLastMessage(context, newItem.from, text, newItem.created);
		} else if (text.startsWith("E")) {
			// this is an AES encrypted message
			newItem.encrypted = true;
			text = text.substring(1);
			// decrypt here
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
				// reset because no error!
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
						// live - update if possible,
						// otherwise
						// create notification!
						if (Conversation.isVisible()
								&& Conversation.getHostUid() == newItem.from) {
							// the conversation is currently
							// open, try to update this
							// right
							// away!
							// ATTENTION: if not scrolled down then send an
							// additional toast!
							if (!Conversation.scrolledDown && !newItem.isKey) {
								Utility.showToastShortAsync(context,
										"New message " + newItem.mid
												+ " received.");
							}
							Conversation.getInstance().updateConversationlist(
									context);
						} else {
							// the conversation is NOT
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
							if (Conversation.isAlive()) {
								// still update because conversation is in
								// memory!
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

	// Returns null to indicate to abort current sending because a new key was
	// generated and
	// issued.
	public static Key getAESKey(Context context, int uid,
			boolean forceSendingNewKey, int transport,
			boolean flagErrorMessageNotification, ConversationItem item,
			boolean forSending) {
		// 1. search for AES key, if no key (or outdated), then
		// a. generate one
		// b. save it for later use
		// c. sent it first in a K ey message
		// d. use the new key

		boolean outDated = Setup.isAESKeyOutdated(context, uid, forSending);
		Log.d("communicator", "#### KEY FOR " + uid + " TIMEOUT? " + outDated);

		if (!outDated && !forceSendingNewKey) {
			// Key is uptodate (hopefully the other part has it as well!)
			Key secretKey = Setup.getAESKey(context, uid);
			Log.d("communicator",
					"#### KEY RETURNING " + uid + " : "
							+ Setup.getAESKeyHash(context, uid));
			return secretKey;
		} else {
			// use the last received message as a random seed
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
			boolean haveKey = Setup.haveKey(context, uid);

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
				// this indicates the error!!!
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

			// sendMessage(context, uid, msgText, DB.getTimestamp() - 1000,
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
			// sending the RSA encrypted KEY-Message before sending the normally
			// created message ensures that
			// the symmetric key is received BEFORE the other side tries to
			// decrypt (possibly with an old key).
			// If a new key is received, any old one is discarded.

			// return null to indicate to cancel sending, we sent the key
			// instead and want to
			// ensure that it arrives earlier
			return null;
			// return newKey;
		}
	}

	// ------------------------------------------------------------------------
	// ------------------------------------------------------------------------

	public static void sendNewNextMessageAsync(final Context context,
			int transport) {
		if (transport == DB.TRANSPORT_INTERNET) {
			Communicator.messagesToSend = true;
		} else {
			Communicator.SMSToSend = true;
		}
		// (new Thread() {
		// public void run() {
		// sendNextMessage(context);
		// }
		// }).start();
	}

	public static void sendNextMessage(final Context context) {
		if ((Conversation.isVisible() && Conversation.isTyping())) {
			Log.d("communicator",
					"#### sendNextMessage() SKIPPED DUE TO TYPING "
							+ messagesToSend);
			// if currently typing, do nothing!
			return;
		} else {
			Log.d("communicator", "#### sendNextMessage() PROCESSING "
					+ messagesToSend);
		}

		messageSent = false;

		// this is a cached value. If a message is entered, then this flag is
		// changed!
		if (!messagesToSend && !SMSToSend) {
			Log.d("communicator",
					"#### sendNextMessage() NO MESSAGES AND NO SMS TO PROCESS... return "
							+ messagesToSend);
			return;
		}

		// randomly choose next transport type to send! (sometimes one
		// connection is stuck, then we dont want to get stuck at all!)
		// TOGGLE FOR THE CASE THAT WE HAVE TO SEND BOTH TYPES
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

		// debugging
		if (transport == DB.TRANSPORT_INTERNET) {
			Log.d("communicator", "#### SEND NEXT : TRY INTERNET MESSAGE ");
		} else {
			Log.d("communicator", "#### SEND NEXT : TRY SMS MESSAGE ");
		}

		// fake hasSignal == true in 1 of 10 unsuccessful tries
		// return true;

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

		// if (itemToSend != null) {
		// Log.d("communicator",
		// "SEND NEXT itemToSend["+itemToSend.localid+"] : to=" + itemToSend.to+
		// " message=" + itemToSend.text);
		// }

		if (itemToSend != null && itemToSend.created > 0 && itemToSend.to != -1) {
			if (!itemToSend.system) {
				Main.updateLastMessage(context, itemToSend.to, itemToSend.text,
						itemToSend.created);
			}

			Log.d("communicator",
					"#### sendNextMessage() NOW ABOUT TO SEND ... #1");

			// if the item wants to be sent unencrypted... well.. do it :(
			boolean forceUnencrypted = !itemToSend.encrypted;

			boolean encryption = !forceUnencrypted
					&& Utility.loadBooleanSetting(context,
							Setup.OPTION_ENCRYPTION, Setup.DEFAULT_ENCRYPTION);
			boolean haveKey = Setup.haveKey(context, itemToSend.to);

			Log.d("communicator",
					"#### sendNextMessage() NOW ABOUT TO SEND ... #2");

			String msgText = itemToSend.text;
			if (itemToSend.system) {
				// for system messages do not modify the text!
			} else if (!encryption || !haveKey) {
				msgText = "U" + msgText;
			} else {
				// this fully automatically gets a not-outdated old AES key or
				// generates a fresh new AES key (also sent)

				// Use the same transport for the key as the message wants to be
				// send!
				// TODO: check if we additionally ALWAYS want to send a key via
				// internet??
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
			// consume message due to error!
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
			// no more messages to send for now, this flag is modified by
			// Conversation.sendButtonClick!
			messagesToSend = false;
		}
	}

	// ------------------
	// ------------------
	// ------------------

	public static void sendMessage(final Context context, final int to,
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

	// itemToSend can be null, in this case nothing will be updated
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

	// itemToSend can be null, in this case nothing will be updated
	private static void sendMessageInternet(final Context context,
			final int to, final String msgText, final long created,
			final ConversationItem itemToSend) {
		messageSent = false;

		String session = Setup.getTmpLogin(context);
		if (session == null) {
			// error resume is automatically done by getTmpLogin, not logged in
			return;
		}

		String url = null;
		String encUid = Setup.encUid(context, to);
		if (encUid == null) {
			// secret may be not set yet, try again later!
			return;
		}
		url = Setup.getBaseURL(context) + "cmd=send&session=" + session
				+ "&host=" + Utility.encode(encUid) + "&val="
				+ Utility.encode(created + "#" + msgText);

		// String uidString = Utility.loadStringSetting(context, "uid", "");
		// String pwdString = Utility.loadStringSetting(context, "pwd", "");
		// String url = null;
		// url = Setup.getBaseURL(context) + "cmd=send&uid=" + uidString +
		// "&pwd=" +
		// pwdString
		// + "&host=" + to + "&val="
		// + Utility.encode(created + "#" + msgText);

		Log.d("communicator", "SEND NEXT MESSAGE: " + url);
		final String url2 = url;
		HttpStringRequest httpStringRequest = (new HttpStringRequest(context,
				url2, new HttpStringRequest.OnResponseListener() {
					public void response(String response) {
						boolean success = false;
						boolean resposeError = true;
						if (isResponseValid(response)) {
							if (isResponsePositive(response)) {
								resposeError = false;
								String ResponseContent = getResponseContent(response);

								Log.d("communicator",
										"SEND NEXT MESSAGE OK!!! " + response);

								// response should have the form
								// senttimestamp#mid

								// update database
								String[] responseArray = ResponseContent
										.split("#");
								if (responseArray.length == 2) {
									String sent = responseArray[0];
									String mid = responseArray[1];
									if (itemToSend != null) {
										// it should not be null even for system
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

	public static void createNotification(Context context, ConversationItem item) {
		Log.d("communicator",
				"@@@@ createNotification() POSSIBLY CREATE NOTIFICATION #4 "
						+ item.from);

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

		Log.d("communicator", "@@@@ createNotification() CREATE NOTIFICATION "
				+ item.from);

		NotificationManager notificationManager = (NotificationManager) context
				.getSystemService(context.NOTIFICATION_SERVICE);
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

		// // Set scrolling text
		// NotificationCompat.BigTextStyle bigxtstyle =
		// new NotificationCompat.BigTextStyle();
		// bigxtstyle.bigText(completeMessage);
		// bigxtstyle.setBigContentTitle(title);

		// Notification n = new Notification(R.drawable.music, "Lyrics for ...",
		// System.currentTimeMillis());
		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
				context)
				// .setStyle(bigxtstyle)
				.setSmallIcon(R.drawable.msgsmall24x24)
				.setPriority(NotificationCompat.PRIORITY_MAX)
				.setCategory(NotificationCompat.CATEGORY_ALARM)
				.setTicker(title + ": " + completeMessage).setWhen(0)
				.setContentTitle(title).setContentText(text)
				// .setOngoing(true)
				.setContentIntent(pendingIntent)
				.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

		// .addAction(R.drawable.music, "Google", pendingIntent)
		// .addAction(R.drawable.music, "AZ", pendingIntent);
		mBuilder.setGroup(Setup.GROUP_CRYPTOCATOR);
		mBuilder.setAutoCancel(true);
		Notification n = mBuilder.build();

		n.contentIntent = pendingIntent;
		int notificationId = 8888888 + item.from;
		notificationManager.notify(notificationId, n);
	}

	// -------------------------------------------------------------------------

	public static void cancelNotification(Context context, int uid) {
		Log.d("communicator", "@@@@ cancelNotification() CANCEL NOTIFICATION "
				+ uid);

		NotificationManager notificationManager = (NotificationManager) context
				.getSystemService(context.NOTIFICATION_SERVICE);

		int notificationId = 8888888 + uid;
		notificationManager.cancel(notificationId);
	}

	// -------------------------------------------------------------------------

	public static int getNotificationCount(Context context, int uid) {
		return Utility.loadIntSetting(context, "notification" + uid, 0);
	}

	// reset = true: delete, reset = false: increment
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

	public static void sendKeyToServer(final Context context, final String key,
			final String keyhash) {
		final String uidString = Utility.loadStringSetting(context, "uid", "");

		String session = Setup.getTmpLogin(context);
		if (session == null) {
			// error resume is automatically done by getTmpLogin, not logged in
			Utility.showToastAsync(context, "Error sending account key "
					+ keyhash + " to server! (1)");
			// disable in settings
			Utility.saveStringSetting(context, Setup.PUBKEY, null);
			Utility.saveStringSetting(context, Setup.PRIVATEKEY, null);
			Utility.saveBooleanSetting(context, Setup.OPTION_ENCRYPTION, false);
			return;
		}

		String url = null;
		url = Setup.getBaseURL(context) + "cmd=sendkey&session=" + session
				+ "&val=" + Utility.encode(key);

		Log.d("communicator", "###### SEND KEY TO SERVER " + url);
		final String url2 = url;
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

	public static void clearKeyFromServer(final Context context,
			final String keyhash) {
		String session = Setup.getTmpLogin(context);
		if (session == null) {
			// error resume is automatically done by getTmpLogin, not logged in
			Utility.showToastAsync(context, "Error clearing account " + keyhash
					+ " key from server! (1)");
			return;
		}

		String url = null;
		url = Setup.getBaseURL(context) + "cmd=clearkey&session=" + session;

		// Log.d("communicator", "###### CLEAR KEY FROM SERVER " + url);
		final String url2 = url;
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

		String session = Setup.getTmpLogin(context);
		if (session == null) {
			// error resume is automatically done by getTmpLogin, not logged in
			return;
		}

		String url = null;
		url = Setup.getBaseURL(context) + "cmd=haskey&session=" + session
				+ "&val=" + Utility.encode(uidliststring);

		Log.d("communicator", "###### REQUEST HAS KEY " + url);
		final String url2 = url;
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

		String session = Setup.getTmpLogin(context);
		if (session == null) {
			// error resume is automatically done by getTmpLogin, not logged in
			return;
		}

		String url = null;
		url = Setup.getBaseURL(context) + "cmd=hasphone&session=" + session
				+ "&val=" + Utility.encode(uidliststring);

		Log.d("communicator", "###### REQUEST HAS PHONE (" + uidliststring
				+ ") " + url);

		// String blaa = "Sara";
		// String blaaEnc = Setup.encText(context, blaa);
		// Log.d("communicator", "###### REQUEST HAS PHONE ENC " + blaaEnc +
		// " ");
		// Log.d("communicator",
		// "###### REQUEST HAS PHONE DEC "
		// + Setup.decText(context, blaaEnc) + " ");
		//
		// Log.d("communicator",
		// "###### REQUEST HAS PHONE DEC UIDLIST("
		// + Setup.decText(context, uidlistStringEnc) + ") ");

		final String url2 = url;
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

	// -------------------------------------------------------------------------

	public static void getKeyFromServer(final Context context, final int uid,
			final Main.UpdateListener updateListener) {

		if (uid < 0) {
			// Do not request keys for SMS/external users!
			return;
		}

		String session = Setup.getTmpLogin(context);
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

	public static String encryptAESMessage(Context context, String text,
			Key secretKey) {
		text = Utility.getRandomString(Setup.RANDOM_STUFF_BYTES) + text; // add
																			// 5
																			// random
																			// characters
																			// just
																			// for
																			// security
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

				// String uidString = Utility
				// .loadStringSetting(context, "uid", "");
				// String pwdString = Utility
				// .loadStringSetting(context, "pwd", "");
				//
				// String url = null;
				// url = Setup.getBaseURL(context) + "cmd=confirm&uid=" +
				// uidString +
				// "&pwd="
				// + pwdString + "&host=" + uid + "&val=" + mid;
				// Log.d("communicator", "SEND READ CONFIRMATION: " + url);
				// final String url2 = url;
				// HttpStringRequest httpStringRequest = (new HttpStringRequest(
				// context, url2,
				// new HttpStringRequest.OnResponseListener() {
				// public void response(String response) {
				// if (isResponseValid(response)) {
				// if (isResponsePositive(response)) {
				// Utility.saveIntSetting(context,
				// "lastreadconfirmationmid", mid);
				// // Log.d("communicator",
				// // "SEND READ CONFIRMATION OK!!! "
				// // + response2);
				// }
				// }
				// }
				// }));
			}
		}
	}

	// -------------------------------------------------------------------------

	// System messages are sent over normal sending interface, this way they
	// will be sent automatically in order and iff temporary login is okay.
	// Both system messages will go to the server.
	// System messages can only go over internet (if available)
	// modes:
	// R == read (these are processed directly by the server)
	// A == withdraw (these are processed and come also back as W-messages)
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
		// sendMessage(context, uid, "R" + mid, DB.getTimestamp(), null,
		// DB.TRANSPORT_INTERNET);
	}

	public static void sendSystemMessageWidthdraw(final Context context,
			final int uid, final int mid) {
		if (uid >= 0 && mid != -1) {
			DB.addSendMessage(context, uid, "A" + mid, false,
					DB.TRANSPORT_INTERNET, true, DB.PRIORITY_KEY);
			Communicator
					.sendNewNextMessageAsync(context, DB.TRANSPORT_INTERNET);
		}
		// sendMessage(context, uid, "A" + mid, DB.getTimestamp(), null,
		// DB.TRANSPORT_INTERNET);
	}

	// -------------------------------------------------------------------------

	public static void sendWithdraw(final Context context, final int uid,
			final int mid) {
		sendSystemMessageWidthdraw(context, uid, mid);

		Utility.showToastAsync(context, "Withdraw request for message " + mid
				+ "...");

		// // String uidString = Utility.loadStringSetting(context, "uid", "");
		// // String pwdString = Utility.loadStringSetting(context, "pwd", "");
		//
		// String session = Setup.getTmpLogin(context);
		// if (session == null) {
		// Utility.showToastAsync(context,
		// "Failed sending withdraw request for message "
		// + mid + " sent.");
		// // error resume is automatically done by getTmpLogin, not logged in
		// return;
		// }
		//
		// String url = null;
		// url = Setup.getBaseURL(context) + "cmd=withdraw&session=" + session +
		// "&val=" +
		// mid;
		// Log.d("communicator", "SEND WITHDRAW REQUEST: " + url);
		// final String url2 = url;
		// HttpStringRequest httpStringRequest = (new HttpStringRequest(context,
		// url2, new HttpStringRequest.OnResponseListener() {
		// public void response(String response) {
		// boolean success = false;
		// if (isResponseValid(response)) {
		// if (isResponsePositive(response)) {
		// success = true;
		// Utility.showToastAsync(context,
		// "Withdraw request for message " + mid
		// + " sent.");
		// // Withdraw locally
		// DB.updateMessageWithdrawn(context, mid,
		// DB.getTimestampString());
		// updateSentReceivedReadAsync(context, mid, -1,
		// false, false, false, true);
		// }
		// }
		// if (!success) {
		// Utility.showToastAsync(context,
		// "Failed sending withdraw request for message "
		// + mid + " sent.");
		// }
		// }
		// }));
	}

	// -------------------------------------------------------------------------
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
	 * Checks if the response does not start with a negative number!
	 * 
	 * @param response
	 *            the response
	 * @return true, if successful
	 */
	public static boolean isResponsePositive(String response) {
		// Log.d("communicator", "@@@@ isResponsePositive: " + response +
		// " ==> " + response.startsWith("-"));
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
	// -------------------------------------------------------------------------

}
