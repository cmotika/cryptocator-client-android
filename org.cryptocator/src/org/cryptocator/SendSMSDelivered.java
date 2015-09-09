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

/**
 * The SendSMSDelivered class is responsible updating the message database and
 * the UI if an SMS is received (delivered) by the recipient. It knows by the
 * local id and the sending id which message to update.
 * 
 * @author Christian Motika
 * @since 1.2
 * @date 08/23/2015
 * 
 */
public class SendSMSDelivered extends BroadcastReceiver {

	// ------------------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.content.BroadcastReceiver#onReceive(android.content.Context,
	 * android.content.Intent)
	 */
	public void onReceive(Context context, Intent intent) {
		int localId = -1;
		int part = DB.DEFAULT_MESSAGEPART;
		int parts = 1;
		int hostUid = -1;
		if (intent.getExtras() != null) {
			Object object = intent.getExtras().get("localid");
			if (object instanceof Integer) {
				localId = (Integer) object;
			}
			object = intent.getExtras().get("part");
			if (object instanceof Integer) {
				part = (Integer) object;
			}
			object = intent.getExtras().get("parts");
			if (object instanceof Integer) {
				parts = (Integer) object;
			}
			object = intent.getExtras().get("hostuid");
			if (object instanceof Integer) {
				hostUid = (Integer) object;
			}
			int resultCode = getResultCode();
			if (resultCode == Activity.RESULT_OK) {
				// For multipart messages, do this for each part!
				ConversationItem itemToSend = DB.getMessage(context, localId,
						hostUid, part);
				if (itemToSend != null) {
					boolean processSMS = true;
					if (part == parts-1) {
						// Only for the last part of a multipart message
						Communicator.processKeyDeliveries(context, hostUid, localId, processSMS);
					}
					// Only do this AFTER previous processing ... otherwise
					// we cannot find the lastKeyMessageMid!
					itemToSend.received = DB.getTimestamp();
					DB.updateMessage(context, itemToSend, hostUid);
					// -1 * localId : convention so that mapping lookup will
					// take localid into account because we will never have a
					// mid
					// for an SMS message!
					Communicator.updateSentReceivedReadAsync(context, -1
							* itemToSend.localid, itemToSend.to, false, true,
							false, false, false);
				}
				Utility.showToastAsync(context, "SMS "+localId+" delivered.");
			}
		}
	}

	// ------------------------------------------------------------------------

}
