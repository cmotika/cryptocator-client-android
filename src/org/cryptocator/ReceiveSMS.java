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

import java.util.HashMap;
import java.util.HashSet;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;

/**
 * The ReceiveSMS class is responsible for handling incoming SMS.
 * 
 * @author Christian Motika
 * @since 1.2
 * @date 08/23/2015
 * 
 */
public class ReceiveSMS extends BroadcastReceiver {

	// Get the object of SmsManager
	final SmsManager sms = SmsManager.getDefault();

	// ------------------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.content.BroadcastReceiver#onReceive(android.content.Context,
	 * android.content.Intent)
	 */
	@SuppressLint("UseSparseArrays")
	public void onReceive(Context context, Intent intent) {

		// Retrieves a map of extended data from the intent.
		final Bundle bundle = intent.getExtras();

		boolean receiveAllSMS = Setup.isSMSDefaultApp(context, false);

		if (Setup.isSMSDefaultApp(context, false)) {
			if (intent.getAction().contains("SMS_DELIVER")) {
				// IFF WE ARE THE DEFAULT APPLICATION FOR SMS, THEN IGNORE THE
				// SMS_DELIVER BECAUSE WE
				// ALREADY ALWAYS GET THE SMS_RECEIVED AND DO NOT WANT TO
				// PROCESS AN SMS TWICE!
				return;
			}
		}

		try {

			if (bundle != null) {
				final Object[] pdusObj = (Object[]) bundle.get("pdus");

				HashSet<String> phones = new HashSet<String>();
				HashMap<Integer, byte[]> messages = new HashMap<Integer, byte[]>();

				for (int i = 0; i < pdusObj.length; i++) {
					SmsMessage currentMessage = SmsMessage
							.createFromPdu((byte[]) pdusObj[i]);
					String phone = currentMessage
							.getDisplayOriginatingAddress();
					String message = currentMessage.getDisplayMessageBody();

					phones.add(phone);

					int phoneHash = Setup.getFakeUIDFromPhone(phone);
					Log.d("communicator", "SMS FAKE UID for :" + phone + " is "
							+ phoneHash);
					boolean secureSMS = message
							.startsWith(SendSMS.SECURESMS_ID);
					// abortBroadcast() only works for preKitkat Androids! See
					// below.
					if (secureSMS || receiveAllSMS
							|| messages.containsKey(phone)
							|| messages.containsKey(phoneHash)) {
						// Abort broadcast to other apps
						// FIXME: DISBALED JUST FOR DEBUGGING !! ENABLE IT !!!
						// abortBroadcast();
						abortBroadcast();
					}

					if (!messages.containsKey(phoneHash)) {
						messages.put(phoneHash, new byte[0]);
					}
					byte[] existing = messages.get(phoneHash);
					int elen = existing.length;
					Log.d("communicator", "SMS PART " + i + ": from '" + phone);
					// Log.d("communicator", "SMS PART " + i +
					// " "+phoneHash+" ---> " + list.hashCode());
					byte[] attach = message.getBytes();
					int alen = attach.length;

					byte[] replacement = new byte[elen + alen];
					for (int c = 0; c < elen; c++) {
						// Log.d("communicator", "SMS PART " + i +
						// ": existing '" + c + "'");
						replacement[c] = existing[c];
					}
					for (int c = 0; c < alen; c++) {
						// Log.d("communicator", "SMS PART " + i + ": attach '"
						// + c + "' (" + (elen + c) + ")");
						replacement[elen + c] = attach[c];
					}
					// list.add(message);
					messages.put(phoneHash, replacement);
					// Log.d("communicator", "SMS PART " + i + ": message '" +
					// message + "' now " + replacement.length);

					// for (int c = 0; c < replacement.length; c++) {
					// Log.d("communicator", "SMS A BYTES [" + c + "] = " +
					// replacement[c]);
					// }
				}

				try {
					for (String phone : phones) {
						// StringBuilder sb = new StringBuilder();
						int phoneHash = Setup.getFakeUIDFromPhone(phone);
						// List<String> messageAsList = messages.get(phoneHash);
						// Log.d("communicator", "SMS FROM '"+phone+"' LIST " +
						// messageAsList.hashCode() + " has "+
						// messageAsList.size() + " entries");
						// for (String messagePart : messageAsList) {
						// Log.d("communicator", "SMS APPEND " + messagePart);
						// Log.d("communicator", "SMS APPEND BEFORE " +
						// completedMmessagage);
						// //sb.append(messagePart);
						// completedMmessagage = completedMmessagage +
						// messagePart;
						// Log.d("communicator", "SMS APPEND AFTER " +
						// completedMmessagage);
						// }
						// String completedMmessagage = sb.toString();
						byte[] bytes = messages.get(phoneHash);
						String completedMessage = new String(bytes);

						// Log.d("communicator", "SMS COMPLETE from " + phone
						// + ": ("+completedMessage.length()+") '" +
						// completedMessage + "'");
						if (!handleMessage(context, phone, completedMessage)) {
							Log.d("communicator", "SMS not handled");
						} else {
							Log.d("communicator", "SMS handled");
						}
					}
				} catch (Exception e) {
				}

			} // end for loop
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// -------------------------------------------------------------------------

	/**
	 * Gets the uid by phone or create a new user.
	 * 
	 * @param context
	 *            the context
	 * @param phone
	 *            the phone
	 * @param createUserIfNotExists
	 *            the create user if not exists
	 * @return the uid by phone or create user
	 */
	public static int getUidByPhoneOrCreateUser(Context context, String phone,
			boolean createUserIfNotExists) {
		int uid = Setup.getUIDByPhone(context, phone, false);

		if (uid == -1 && createUserIfNotExists) {
			// New user
			phone = Setup.normalizePhone(phone);
			// Try to resolve the name from the phone book!
			String name = Main.getNameFromAddressBook(context, phone);
			if (name == null) {
				// Name not found, take telephone number as a default name for
				// SMS users
				name = phone;
			}

			// Address book
			Main.addUser(context, phone, name, false);
			uid = Setup.getUIDByPhone(context, phone, false);
			Log.d("communicator", "SMS Receive from new user : " + uid + ", "
					+ name);
		}
		return uid;
	}

	// -------------------------------------------------------------------------

	/**
	 * Handle SMS message.
	 * 
	 * @param context
	 *            the context
	 * @param phone
	 *            the phone
	 * @param message
	 *            the message
	 * @return true, if successful
	 */
	private boolean handleMessage(Context context, String phone, String message) {
		boolean receiveAllSMS = Setup.isSMSDefaultApp(context, false);

		boolean secureSMS = message.startsWith(SendSMS.SECURESMS_ID);
		if (secureSMS) {
			message = message.substring(SendSMS.SECURESMS_ID.length());
		}

		if (secureSMS || receiveAllSMS) {

			int uid = getUidByPhoneOrCreateUser(context, phone, receiveAllSMS);

			Log.d("communicator", "SMS Receive from " + phone
					+ " internally UID=" + uid + " the following text: '"
					+ message + "'");

			// This should be an encrypted sms, sent by Delphino
			// Cryptocator
			if (uid != -1) {
				// Only allow sms messages from known users
				// HANDLE
				final ConversationItem newItem = new ConversationItem();
				newItem.mid = DB.SMS_MID; // this is an SMS, we will not
				// have an mid for it, set it to -1
				newItem.from = uid;
				newItem.to = DB.DEFAULT_MYSELF_UID;
				newItem.created = DB.getTimestamp();
				newItem.sent = DB.getTimestamp();
				newItem.received = DB.getTimestamp();
				newItem.transport = DB.TRANSPORT_SMS;
				if (Conversation.isVisible()
						&& Conversation.getHostUid() == newItem.from) {
					newItem.read = DB.getTimestamp();
				}
				newItem.text = message;
				// WE HANDLE ALL TEXT BECAUSE OF (POSSIBLE) MULTIPART SMS //
				newItem.text = Communicator.handleReceivedText(context,
						message, newItem, -1);

				boolean success2 = Communicator
						.updateDBForReceivedMessage(context, newItem);

				if (newItem.text.contains("[ invalid session key ]")
						|| newItem.text.contains("[ decryption failed ]")) {
					// We should try to update the public rsa key of this user
					if (uid >= 0) {
						// Only for registered users!
						int serverId = Setup.getServerId(context, uid);
						Communicator.updateKeysFromServer(context,
								Main.loadUIDList(context), true, null, serverId);
					}
				}

				if (success2) {
					Communicator.liveUpdateOrNotify(context, newItem);
				}
				// No further processing
				return true;
			}

		}
		// Have not handled message, further processing
		return false;
	}

	// ------------------------------------------------------------------------

	/*
	 * 
	 * A lot has changed in Android 4.4 (API level 19). Firstly, with just
	 * SMS_RECEIVED_ACTION intent filter, apps can only “observe” (or “monitor”)
	 * incoming messages, i.e., read them for purposes like phone verification.
	 * But this will not give you write access to the SMS provider (content
	 * provider) defined by the android.provider.Telephony class and subclasses.
	 * This also means now you cannot delete messages.
	 * 
	 * To get write access to the provider, you’ll need your app to be selected
	 * as the default messaging app by the user. Then incoming SMS’s will be
	 * delivered to your app with the SMS_DELIVER_ACTION intent and also you’ll
	 * need to request the BROADCAST_SMS permission.
	 * 
	 * You can find more information on the process to build a full blown
	 * messaging app that works on Kitkat and above here.
	 * 
	 * Pre-Android 4.4, the SMS_RECEIVED_ACTION intents were ordered broadcasts
	 * that could be aborted by one of the receivers. This would cause a lot of
	 * issues in some cases where a third party app with highest priority would
	 * abort the broadcast preventing it from getting received by another third
	 * party app’s receivers. Since Kitkat, this is not possible anymore (good
	 * news!), i.e., any attempt to abort the broadcast will be ignored and all
	 * the apps who had registered to receive incoming messages will get a
	 * chance.
	 * 
	 * see also
	 * http://android-developers.blogspot.in/2013/10/getting-your-sms-apps
	 * -ready-for-kitkat.html
	 */

	// ------------------------------------------------------------------------

}
