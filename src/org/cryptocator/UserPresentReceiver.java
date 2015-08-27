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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * The UserPresentReceiver is responsible for canceling notifications but ONLY
 * if the screen is UNLOCKED.
 * 
 * @author Christian Motika
 * @since 1.2
 * @date 08/23/2015
 */
public class UserPresentReceiver extends BroadcastReceiver {

	// ------------------------------------------------------------------------

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();

		Log.d("communicator",
				"@@@@ UserPresentReceiver.onReceive() USER_PRESENT #0" + action);

		if (action != null && action.endsWith("USER_PRESENT")) {
			Log.d("communicator",
					"@@@@ UserPresentReceiver.onReceive() USER_PRESENT #1");

			// onResume of the activity will be called when ACTION_SCREEN_ON
			// is fired. Create a handler and wait for ACTION_USER_PRESENT.
			// When it is fired, implement what you want for your activity.
			//
			// WE NEED TO WAIT UNTIL THE SCREEN IS UNLOCKED BEFORE WE
			// CANCEL THE NOTIFICATION
			if (Utility.loadBooleanSetting(context, Setup.OPTION_NOTIFICATION,
					Setup.DEFAULT_NOTIFICATION)
					&& !Utility.isScreenLocked(context)) {
				Log.d("communicator",
						"@@@@ UserPresentReceiver.onReceive() USER_PRESENT #2");
				if (Conversation.isAlive() && Conversation.isVisible()) {
					Log.d("communicator",
							"@@@@ UserPresentReceiver.onReceive() USER_PRESENT #3");
					Communicator.cancelNotification(context,
							Conversation.getHostUid());
				}
			}
		}
	}

	// ------------------------------------------------------------------------

}
