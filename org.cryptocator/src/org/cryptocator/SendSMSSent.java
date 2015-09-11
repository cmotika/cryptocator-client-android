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

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;
import android.util.Log;

/**
 * The SendSMSSent class is responsible updating the message database and the UI
 * if an SMS, i.e., all parts of an SMS, has been sent. It knows by the local id
 * and the sending id which message to update.
 * 
 * @author Christian Motika
 * @since 1.2
 * @date 08/23/2015
 * 
 */
public class SendSMSSent extends BroadcastReceiver {

	// ------------------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.content.BroadcastReceiver#onReceive(android.content.Context,
	 * android.content.Intent)
	 */
	public void onReceive(Context context, Intent intent) {
		int localId = -1;
		int part = -1;
		int hostUid = -1;
		int sendingId = -1;
		if (intent.getExtras() != null) {
			Object object = intent.getExtras().get("localid");
			if (object instanceof Integer) {
				localId = (Integer) object;
			}
			object = intent.getExtras().get("part");
			if (object instanceof Integer) {
				part = (Integer) object;
			}
			object = intent.getExtras().get("hostuid");
			if (object instanceof Integer) {
				hostUid = (Integer) object;
			}
			object = intent.getExtras().get("sendingid");
			if (object instanceof Integer) {
				sendingId = (Integer) object;
			}
			int resultCode = getResultCode();
			if (resultCode == Activity.RESULT_OK) {
				// For multipart messages, do this for the first part only this is the MASTER part
				ConversationItem itemToSend = DB.getMessage(context, localId,
						hostUid, part);
				DB.removeSentMessage(context, sendingId);
				if (itemToSend != null) {
					itemToSend.sent = DB.getTimestamp();
					DB.updateMessage(context, itemToSend, hostUid);
					// -1 * localId : convention so that mapping lookup will
					// take localid into account because we will never have a
					// mid
					// for an SMS message!
					Communicator.updateSentReceivedReadAsync(context, -1
							* itemToSend.localid, itemToSend.to, true, false,
							false, false, false);
				}
				// Utility.showToastAsync(context, "SMS SENT " + localId);
				SendSMS.smsSent(context, localId);
			} else if (resultCode == SmsManager.RESULT_ERROR_GENERIC_FAILURE) {
				SMSFailed(context, localId, sendingId, hostUid,
						" (RESULT_ERROR_GENERIC_FAILURE)");
				Log.d("communicator", "#### SMS " + localId
						+ " NOT SENT (RESULT_ERROR_GENERIC_FAILURE)");

			} else if (resultCode == SmsManager.RESULT_ERROR_NO_SERVICE) {
				Log.d("communicator", "#### SMS " + localId
						+ " NOT SENT (RESULT_ERROR_NO_SERVICE)");
				Utility.showToastAsync(context, "SMS " + localId
						+ " NOT SENT (RESULT_ERROR_NO_SERVICE)");
				SendSMS.smsSent(context, localId);
			} else if (resultCode == SmsManager.RESULT_ERROR_NULL_PDU) {
				// INCREMENT FAIL COUNTER
				SMSFailed(context, localId, sendingId, hostUid,
						"(RESULT_ERROR_NULL_PDU)");
				// if (DB.incrementFailed(context, localId, hostUid)) {
				// if (DB.removeSentMessage(context, sendingId)) {
				// Utility.showToastAsync(
				// context,
				// "SMS "
				// + localId
				// + " to "
				// + Main.UID2Name(context, hostUid, false)
				// + " failed to send after "
				// + Setup.SMS_FAIL_CNT
				// + " tries. (RESULT_ERROR_NULL_PDU)");
				// Conversation.setFailedAsync(context, localId);
				// }
				// }
				// Log.d("communicator", "#### SMS " + localId
				// + " NOT SENT (RESULT_ERROR_NULL_PDU)");
				// Utility.showToastAsync(context, "SMS " + localId
				// + " NOT SENT (RESULT_ERROR_NULL_PDU)");
				// SendSMS.smsSent(localId);
			} else if (resultCode == SmsManager.RESULT_ERROR_RADIO_OFF) {
				Log.d("communicator", "#### SMS " + localId
						+ " NOT SENT (RESULT_ERROR_RADIO_OFF)");
				Utility.showToastAsync(context, "SMS " + localId
						+ " NOT SENT (RESULT_ERROR_RADIO_OFF)");
				SendSMS.smsSent(context, localId);
				// } else if (resultCode == 133404) {
			} else {
				// INCREMENT FAIL COUNTER
				SMSFailed(context, localId, sendingId, hostUid, "(error code "
						+ resultCode + ")");
				// if (DB.incrementFailed(context, localId, hostUid)) {
				// if (DB.removeSentMessage(context, sendingId)) {
				// Utility.showToastAsync(
				// context,
				// "SMS "
				// + localId
				// + " to "
				// + Main.UID2Name(context, hostUid, false)
				// + " failed to send after "
				// + Setup.SMS_FAIL_CNT
				// + " tries. (error code " + resultCode
				// + ")");
				// Conversation.setFailedAsync(context, localId);
				// }
				// }
				// Log.d("communicator", "#### SMS " + localId
				// + " NOT SENT (error code " + resultCode + ")");
				// SendSMS.smsSent(localId);
			}
		}
	}

	// ------------------------------------------------------------------------

	public static void SMSFailed(Context context, int localId, int sendingId,
			int hostUid, String infoAddition) {
		// INCREMENT FAIL COUNTER
		if (DB.incrementFailed(context, localId)) {
			if (DB.removeSentMessage(context, sendingId)) {
				Utility.showToastAsync(context, "SMS " + localId + " to "
						+ Main.UID2Name(context, hostUid, false)
						+ " failed to send after " + Setup.SMS_FAIL_CNT
						+ " tries." + infoAddition);
				Conversation.setFailedAsync(context, localId);
			}
		}
		SendSMS.smsSent(context, localId);
	}

	// ------------------------------------------------------------------------

	public static void SMSFailedSimple(Context context, int localId,
			String infoAddition) {
		// INCREMENT FAIL COUNTER
		if (DB.incrementFailed(context, localId)) {
			if (DB.removeSentMessageByLocalId(context, localId)) {
				Utility.showToastAsync(context, "SMS " + localId
						+ " failed to send after " + Setup.SMS_FAIL_CNT
						+ " tries." + infoAddition);
				Conversation.setFailedAsync(context, localId);
			}
		}
		SendSMS.smsSent(context, localId);
	}

	// ------------------------------------------------------------------------

}
