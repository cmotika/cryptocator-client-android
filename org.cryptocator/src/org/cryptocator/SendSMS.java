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

import java.util.ArrayList;
import java.util.HashMap;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;

/**
 * The SendSMS class is responsible for dividing SMS into multiparts and sending
 * them. It will internally keep the sent parts in a data structure until they
 * are sent.
 * 
 * @author Christian Motika
 * @since 1.2
 * @date 08/23/2015
 * 
 */
@SuppressLint("UseSparseArrays")
public class SendSMS {

	public static final int SMS_SENDING_TIMEOUT = 60 * 1000; // AFTER 60
																// Seconds, if
																// NO new part
																// was sent, we
																// may try
																// again!

	public static final String SECURESMS_ID = "###CRYPTOCATOR###";

	public static final String SECURESMSSENT = "SECURESMSSENT";
	public static final String SECURESMSDELIVERED = "SECURESMSDELIVERED";

	/** The currently sending sms for counting down sent SMS parts. */
	private static HashMap<Integer, Integer> currentlySending = new HashMap<Integer, Integer>();

	/** The currently sending timestamp for detecting timeouts. */
	private static HashMap<Integer, Long> currentlySendingTimestamp = new HashMap<Integer, Long>();

	/**
	 * The currently sending threads for having a handle to the thread sending a
	 * particular SMS.
	 */
	private static HashMap<Integer, SMSSendThread> currentlySendingThreads = new HashMap<Integer, SMSSendThread>();

	/** The in service means we are registered to network. */
	public static boolean inService = false;

	/** The has signal as set by older devices. */
	public static boolean hasSignal = false;

	/** The has signal new as set by newer devices. */
	public static boolean hasSignalNew = false;

	/** The current signal. */
	public static int signal = 0;

	/** The phone state listener for updating hasSignal and inService flags. */
	private static PhoneStateListener phoneStateListener = null;

	// ------------------------------------------------------------------------

	/**
	 * Ensure phone state listener is registered. This listener is responsible
	 * for setting inService and hasSignal/hasSignalNew flags.
	 * 
	 * @param context
	 *            the context
	 */
	@SuppressWarnings("deprecation")
	private static void ensurePhoneStateListener(Context context) {
		if (phoneStateListener == null) {
			final Handler mUIHandler = new Handler(Looper.getMainLooper());
			mUIHandler.post(new Thread() {
				@Override
				public void run() {
					super.run();

					phoneStateListener = new PhoneStateListener() {
						public void onSignalStrengthChanged(int signal) {
							if (signal > 0) {
								SendSMS.signal = signal;
								Log.d("communicator",
										"#### onSignalStrengthChanged  --- SIGNAL STRENGTH "
												+ signal);
								hasSignal = true;
							} else {
								Log.d("communicator",
										"#### onSignalStrengthChanged  --- NO SIGNAL");
								hasSignal = false;
							}
						}

						public void onSignalStrengthsChanged(
								SignalStrength signalStrength) {
							int signal = signalStrength.getGsmSignalStrength();
							if (signal > 0) {
								SendSMS.signal = signal;
								Log.d("communicator",
										"#### onSignalStrengthsChanged (NEW)  --- SIGNAL STRENGTH "
												+ signal);
								hasSignalNew = true;
							} else {
								Log.d("communicator",
										"#### onSignalStrengthsChanged (NEW) --- NO SIGNAL");
								hasSignalNew = false;
							}
						}

						@Override
						public void onServiceStateChanged(
								ServiceState serviceState) {
							super.onServiceStateChanged(serviceState);
							//String phonestate;

							switch (serviceState.getState()) {
							case ServiceState.STATE_EMERGENCY_ONLY:
								Log.d("communicator",
										"#### phoneStateListener --- EMERGENCY ONLU ");
								//phonestate = "STATE_EMERGENCY_ONLY";
								inService = false;
								break;
							case ServiceState.STATE_IN_SERVICE:
								Log.d("communicator",
										"#### phoneStateListener --- IN SERVICE ");
								//phonestate = "STATE_IN_SERVICE";
								inService = true;
								break;
							case ServiceState.STATE_OUT_OF_SERVICE:
								Log.d("communicator",
										"#### phoneStateListener --- OUT OF SERVICE ");
								//phonestate = "STATE_OUT_OF_SERVICE";
								inService = false;
								break;
							case ServiceState.STATE_POWER_OFF:
								Log.d("communicator",
										"#### phoneStateListener --- POWER OFF ");
								//phonestate = "STATE_POWER_OFF";
								inService = false;
								break;
							default:
								Log.d("communicator",
										"#### phoneStateListener --- UNKNOWN ");
								//phonestate = "Unknown";
								inService = false;
								break;
							}
						}
					};

				}
			});
		}
		TelephonyManager telephonyManager = (TelephonyManager) context
				.getSystemService(Context.TELEPHONY_SERVICE);
		telephonyManager.listen(phoneStateListener,
				PhoneStateListener.LISTEN_SIGNAL_STRENGTH);
		telephonyManager.listen(phoneStateListener,
				PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
		telephonyManager.listen(phoneStateListener,
				PhoneStateListener.LISTEN_SERVICE_STATE);
	}

	// ------------------------------------------------------------------------

	/**
	 * When starting to send an SMS set the sending count down and update the
	 * timestamp. The timestamp is necessary to detect a timeout if an SMS could
	 * not be sent here in order to be able to try it again. Before the timeout
	 * is reached any further tries to send this SMS will be rejected. After the
	 * timeout retries are permitted.
	 * 
	 * @param localId
	 *            the local id
	 * @param numParts
	 *            the num parts
	 * @param sendingThread
	 *            the sending thread
	 */
	public static synchronized void startSendingTimestamp(int localId,
			int numParts, SMSSendThread sendingThread) {
		currentlySending.put(localId, numParts);
		currentlySendingTimestamp.put(localId, DB.getTimestamp());
		currentlySendingThreads.put(localId, sendingThread);

	}

	// ------------------------------------------------------------------------

	/**
	 * Update sending timestamp if an SMS is sent.
	 * 
	 * @param localId
	 *            the local id
	 */
	public static synchronized void updateSendingTimestamp(int localId) {
		currentlySendingTimestamp.put(localId, DB.getTimestamp());
	}

	// ------------------------------------------------------------------------

	/**
	 * Set an SMS part with the local id to be successfully sent. If all parts
	 * have been sent (countDown == 0) then remove it from the status of
	 * currently being in sending.
	 * 
	 * @param localId
	 *            the local id
	 */
	public static synchronized void smsSent(int localId) {
		if (currentlySending != null && localId != -1) {
			if (currentlySending.containsKey(localId)) {
				updateSendingTimestamp(localId);
				int countDown = currentlySending.get(localId);
				countDown--;
				if (countDown == 0) {
					currentlySending.remove(localId);
					currentlySendingTimestamp.remove(localId);
					SMSSendThread thread = currentlySendingThreads.get(localId);
					if (thread != null) {
						Log.d("communicator",
								"#### smsSending aborting thread ("
										+ thread.hashCode()
										+ ") on successfull sent for localid="
										+ localId);
						thread.cancel();
					}
				} else {
					currentlySending.put(localId, countDown);
				}
			}
		}
	}

	// ------------------------------------------------------------------------

	/**
	 * Tell if an SMS with the local id is still sending. Sending may take a
	 * while for all parts to be sent.
	 * 
	 * @param localId
	 *            the local id
	 * @return true, if successful
	 */
	public static synchronized boolean smsSending(int localId) {
		if (localId == -1) {
			return false;
		}
		if (currentlySendingTimestamp.containsKey(localId)) {
			long ts = currentlySendingTimestamp.get(localId);
			if ((DB.getTimestamp() - ts) > SMS_SENDING_TIMEOUT) {
				Log.d("communicator", "#### sendSMS() SMS sending of localid="
						+ localId + " timed out!!!!");
				// sending timed out, remove, so that we can try again later!!!
				currentlySending.remove(localId);
				currentlySendingTimestamp.remove(localId);
				SMSSendThread thread = currentlySendingThreads.get(localId);
				if (thread != null) {
					Log.d("communicator",
							"#### smsSending()? aborting thread ("
									+ thread.hashCode()
									+ ") on timeout for localid=" + localId);
					thread.cancel();
				}
				return false;
			} else {
				Log.d("communicator",
						"#### smsSending()? SMS still sending localid="
								+ localId + " ... wait!!!");
			}
		} else {
			Log.d("communicator",
					"#### smsSending()? SMS not yet sending localid=" + localId
							+ " ... sending NOW!");
		}
		return currentlySendingTimestamp.containsKey(localId);
	}

	// ------------------------------------------------------------------------

	/**
	 * Send an SMS if it is not still sending. If it is still sending and this
	 * fails, we will automatically try again at a later point in time.
	 * 
	 * @param context
	 *            the context
	 * @param number
	 *            the number
	 * @param text
	 *            the text
	 * @param localId
	 *            the local id
	 * @param hostUid
	 *            the host uid
	 * @param sendingId
	 *            the sending id
	 * @param encrypted
	 *            the encrypted
	 */
	public static void sendSMS(final Context context, final String number,
			final String text, final int localId, final int hostUid,
			final int sendingId, final boolean encrypted) {
		// Ensure signal strength listener is active
		ensurePhoneStateListener(context);

		if (smsSending(localId)) {
			Log.d("communicator",
					"#### sendSMS() STILL SENDING THIS ALREADY ... ABORT");
			// Utility.showToastAsync(context, "SMS " + localId
			// + " still sending... not sending again!");
			return;
		}
		Log.d("communicator",
				"#### sendSMS() STILL NOT SENDING THIS BUT SENDING ...  NOW ");

		SMSSendThread thread = new SMSSendThread(context, number, localId,
				hostUid, sendingId, text, encrypted);
		(new Thread(thread)).start();

	}

	// ------------------------------------------------------------------------

	/**
	 * Checks if we currently have a signal and are possibly able to send SMS.
	 * This is when older devices set hasSignal or newer devices set
	 * hasSignalNew and at the same time inService is set to true.
	 * 
	 * @return true, if successful
	 */
	private static boolean hasSignal() {
		return (SendSMS.hasSignal || SendSMS.hasSignalNew) && SendSMS.inService;
	}

	// ------------------------------------------------------------------------
	// ------------------------------------------------------------------------

	/**
	 * The internal class SMSSendThread is able to send an SMS. If there is no
	 * signal or we are not registered to a network then this thread waits until
	 * it is possible to try sending. Then it sends parts if this SMS is too
	 * long (which most encrypted SMS will be).
	 */
	static class SMSSendThread implements Runnable {

		boolean cancel = false;

		Context context;
		String number;
		int localId;
		int hostUid;
		int sendingId;
		String text;
		boolean encrypted;

		// --------------------------------------------------------------------

		public SMSSendThread(Context context, String number, int localId,
				int hostUid, int sendingId, String text, boolean encrypted) {
			this.context = context;
			this.number = number;
			this.localId = localId;
			this.hostUid = hostUid;
			this.sendingId = sendingId;
			this.text = text;
			this.encrypted = encrypted;
		}

		// --------------------------------------------------------------------

		public void cancel() {
			Log.d("communicator", "#### sendSMSThread(" + this.hashCode()
					+ ") CANCEL localid=" + localId);
			cancel = true;
		}

		// --------------------------------------------------------------------

		public void run() {
			Log.d("communicator", "#### sendSMSThread(" + this.hashCode()
					+ ") STILL NOT SENDING THIS BUT SENDING " + localId
					+ " ...  NOW RUN #1");

			SmsManager smsManager = SmsManager.getDefault();
			boolean smsDone = false;
			boolean extraWaitForSignal = false;

			Intent intentSent = new Intent(SECURESMSSENT);
			Intent intentDelivered = new Intent(SECURESMSDELIVERED);
			intentSent.putExtra("localid", localId);
			intentSent.putExtra("hostuid", hostUid);
			intentSent.putExtra("sendingid", sendingId); // for deleting the
															// msg from
															// sending DB
			intentDelivered.putExtra("localid", localId);
			intentDelivered.putExtra("hostuid", hostUid);
			// FLAG_ACTIVITY_NEW_TASK VERY VERY important!!! Otherwise the
			// putExtra will get lost...

			String sendingText = text;
			// if encrypted message or KEY message
			if (encrypted || sendingText.startsWith("K")) {
				sendingText = SECURESMS_ID + sendingText; // Utility.convertStringToBASE64(sendingText);
			} else {
				// remove leading "U"
				if (sendingText.startsWith("U")) {
					sendingText = sendingText.substring(1);
				}
			}

			Log.d("communicator", "#### sendSMSThread(" + this.hashCode()
					+ ") STILL NOT SENDING THIS BUT SENDING " + localId
					+ " ...  NOW RUN #2");

			ArrayList<String> parts = smsManager.divideMessage(sendingText);
			int numParts = parts.size();
			ArrayList<PendingIntent> sentIntents = new ArrayList<PendingIntent>();
			ArrayList<PendingIntent> deliveryIntents = new ArrayList<PendingIntent>();

			// Try to send SMS
			startSendingTimestamp(localId, numParts, this);

			Log.d("communicator", "#### sendSMSThread(" + this.hashCode()
					+ ") STILL NOT SENDING THIS BUT SENDING " + localId
					+ " ...  NOW RUN #3");

			for (int i = 0; i < numParts; i++) {
				PendingIntent pendingIntentSent = PendingIntent.getBroadcast(
						context, 0, intentSent, Intent.FLAG_ACTIVITY_NEW_TASK);
				PendingIntent pendingIntentDelivered = PendingIntent
						.getBroadcast(context, 0, intentDelivered,
								Intent.FLAG_ACTIVITY_NEW_TASK);
				sentIntents.add(pendingIntentSent);
				deliveryIntents.add(pendingIntentDelivered);
			}

			Log.d("communicator", "#### sendSMSThread(" + this.hashCode()
					+ ") STILL NOT SENDING THIS BUT SENDING " + localId
					+ " ...  NOW RUN #4");

			while (!smsDone) {
				try {
					if (cancel) {
						Log.d("communicator",
								"#### sendSMSThread(" + this.hashCode()
										+ ") CANCEL SENDING " + localId
										+ " ...  AND RETURN");
						return;
					}
					Log.d("communicator",
							"#### sendSMSThread(" + this.hashCode()
									+ ") STILL NOT SENDING THIS BUT SENDING "
									+ localId + " ...  NOW ");
					if (hasSignal()) {
						Log.d("communicator",
								"#### sendSMSThread(" + this.hashCode()
										+ ") HAVE SIGNAL ");
						if (extraWaitForSignal) {
							Thread.sleep(5000);
						}

						Log.d("communicator", "sendSMS() SMS Send " + parts
								+ " part msg to " + number
								+ " the following text: '" + sendingText + "'");

						try {
							// Try to send (multipart) SMS
							smsManager.sendMultipartTextMessage(number, null,
									parts, sentIntents, deliveryIntents);
						} catch (Exception ee) {
							ee.printStackTrace();

							// INCREMENT FAIL COUNTER
							if (DB.incrementFailed(context, localId, hostUid)) {
								if (DB.removeSentMessage(context, sendingId)) {
									Utility.showToastAsync(
											context,
											"SMS "
													+ localId
													+ " to "
													+ Main.UID2Name(context,
															hostUid, false)
													+ " failed to send after "
													+ Setup.SMS_FAIL_CNT
													+ " tries. (RESULT_ERROR_GENERIC_FAILURE)");
									Conversation.setFailedAsync(context,
											localId);
								}
							}
							Log.d("communicator",
									"#### SMS "
											+ localId
											+ " NOT SENT (RESULT_ERROR_GENERIC_FAILURE)");
							SendSMS.smsSent(localId);
						}

						Log.d("communicator",
								"sendSMSThread(" + this.hashCode()
										+ ") SMS Send " + localId + " DONE");

						// smsManager.sendTextMessage(number, null,
						// sendingText,
						// pendingIntentSent, pendingIntentDelivered);
						smsDone = true;
					} else {
						Log.d("communicator",
								"#### sendSMSThread(" + this.hashCode()
										+ ") WAIT FOR SIGNAL ");
						extraWaitForSignal = true;
						Thread.sleep(2000);
						// possibly timeout myself...
						smsSending(localId);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

	}

	// ------------------------------------------------------------------------
	// ------------------------------------------------------------------------

}
