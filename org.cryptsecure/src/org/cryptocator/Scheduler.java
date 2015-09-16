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

import java.util.Date;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * The Scheduler is a central part in processing messages and triggering polling
 * or sending of messages.
 * 
 * @author Christian Motika
 * @since 1.2
 * @date 08/23/2015
 * 
 */
public class Scheduler extends BroadcastReceiver {

	/** The pending intent. */
	public static PendingIntent pendingIntent = null;

	/** A listener triggered when the Scheduler decides to update. */
	private static OnUpdateListener listener = null;

	// -------------------------------------------------------------------------

	/**
	 * The listener interface for receiving onUpdate events. The class that is
	 * interested in processing a onUpdate event implements this interface, and
	 * the object created with that class is registered with a component using
	 * the component's <code>addOnUpdateListener<code> method. When
	 * the onUpdate event occurs, that object's appropriate
	 * method is invoked.
	 * 
	 * @see OnUpdateEvent
	 */
	interface OnUpdateListener {

		/**
		 * Update.
		 */
		public void update();
	}

	// -------------------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.content.BroadcastReceiver#onReceive(android.content.Context,
	 * android.content.Intent)
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();

		if (action != null && action.endsWith("USER_PRESENT")) {
			// IF THE SCREEN TURNS ON AND WE ARE STILL IN CONVERSATION ... THEN
			// UPDATE THESE IMMEDIATELY

			// DEBUG : SHOULD ALSO WORK WITHOUT THIS...
			return;
		}

		// The scheduler must be called again at a later time
		// also the update is triggered here
		Scheduler.reschedule(context, false, true, false);
	}

	// -------------------------------------------------------------------------

	/**
	 * Retrieve the pendingIntent of the Scheduler.
	 * 
	 * @param context
	 *            the context
	 */
	public static void updatePendingIntent(Context context) {
		if (pendingIntent == null) {
			pendingIntent = PendingIntent.getBroadcast(context, 0, new Intent(
					context, Scheduler.class),
					PendingIntent.FLAG_UPDATE_CURRENT);
		}
	}

	// -------------------------------------------------------------------------

	/**
	 * Sets the update listener.
	 * 
	 * @param onUpdateListener
	 *            the new update listener
	 */
	public static void setUpdateListener(OnUpdateListener onUpdateListener) {
		listener = onUpdateListener;
	}

	// -------------------------------------------------------------------------

	/**
	 * Clear update listener.
	 */
	public static void clearUpdateListener() {
		listener = null;
	}

	// -------------------------------------------------------------------------

	/**
	 * Reschedule and trigger update.
	 * 
	 * @param context
	 *            the context
	 * @param error
	 *            the error
	 * @param alsoUpdate
	 *            the also update
	 * @param fast
	 *            the fast
	 */
	// Reschedule the next alarm (that has not been discarded yet)
	public static void reschedule(Context context, boolean error,
			boolean alsoUpdate, boolean fast) {
		if (!Setup.isActive(context) && !Setup.isUserActive()) {
			// NOT BACKGROUND SERVICE ACTIVE AND NO USER USING THE PROGRAM
			// RETURN
			return;
		}

		if (error) {
			// If we have an error claim this to increment the error counter, be
			// sure to unclear if no error!!!
			Setup.setErrorUpdateInterval(context, true);
		}

		// Necessary for using alarm manager!
		updatePendingIntent(context);

		boolean powersave = Utility.loadBooleanSetting(context,
				Setup.OPTION_POWERSAVE, Setup.DEFAULT_POWERSAVE);

		// regular update interval
		int updateSeconds = Setup.REGULAR_UPDATE_TIME;
		if (powersave) {
			updateSeconds = Setup.REGULAR_POWERSAVE_UPDATE_TIME;
		}
		if (Setup.isUserActive()) {
			// A faster update if the user is in the program
			updateSeconds = Setup.REGULAR_UPDATE_TIME_FAST;
			if (powersave) {
				updateSeconds = Setup.REGULAR_POWERSAVE_UPDATE_TIME_FAST;
			}
		}
		if (fast) {
			// A super-faste update for the next message to send or receive
			updateSeconds = Setup.REGULAR_UPDATE_TIME_TRYNEXT;
		}

		// current error update interval
		int errorTimeToWait = Setup.getErrorUpdateInterval(context) * 1000;

		// Default alarm time (may be changed during the following tests...
		long timeToSleep = updateSeconds * 1000;
		long utcTime = new Date().getTime();
		long alarmUtcTime = utcTime + timeToSleep;
		if (error) {
			alarmUtcTime = utcTime + errorTimeToWait;
		}

		try {
			// ======================================
			// == RESCHEDULE ==
			// ======================================
			// Clear all schedules.
			AlarmManager alarmManager = (AlarmManager) context
					.getSystemService(Service.ALARM_SERVICE);

			// Cancel earlier alarms
			if (pendingIntent != null) {
				alarmManager.cancel(Scheduler.pendingIntent);
			}

			String flags = "";
			if (error && !Main.isVisible() && !Conversation.isVisible()) {
				flags += "ERROR  (" + errorTimeToWait + ") ";
			}
			if (fast) {
				flags += "FAST ";
			}
			Log.d("communicator", "#### AUTO-RESCHEDULE " + flags + " @ at "
					+ Utility.getTimeTitle(alarmUtcTime));

			// Finally reschedule using the alarm manager.
			alarmManager.set(AlarmManager.RTC_WAKEUP, alarmUtcTime,
					Scheduler.pendingIntent);

			// if everything goes right then update possibly
			if (alsoUpdate) {
				if (Conversation.isVisible() && Conversation.isTyping()) {
					Log.d("communicator", "#### UPDATE SKIPPED - TYPING");
				} else {
					Log.d("communicator", "#### UPDATE - NOT TYPING");
					update(context, false);
					if (listener != null) {
						listener.update();
					} else {
					}
				}
			}

			// ======================================
		} catch (Exception e) {
			e.printStackTrace();
			// Log.d("Calendar Alarm", "##    #13");
			// IF SOMETHING BAD HAS HAPPEND, RECOVER AFTER DEFAULTSLEEPTIMEERROR
			// if (e.getMessage() != null) {
			// Log.d("communicator", e.getMessage());
			// } else {
			// Log.d("communicator", "#### ERROR in line 159");
			// }

			timeToSleep = Setup.ERROR_UPDATE_INTERVAL * 1000;
			utcTime = new Date().getTime();
			alarmUtcTime = utcTime + timeToSleep;

			// Clear all schedules alarm events.
			AlarmManager alarmManager = (AlarmManager) context
					.getSystemService(Service.ALARM_SERVICE);

			// Cancel earlier alarms
			if (pendingIntent != null) {
				alarmManager.cancel(Scheduler.pendingIntent);
			}
			// Finally reschedule us using the alarm manager. in case of failure
			alarmManager.set(AlarmManager.RTC_WAKEUP, alarmUtcTime,
					Scheduler.pendingIntent);
			// Log.d("communicator", "Scheduler.reschedule() ON ERROR");
		}
	}

	// -------------------------------------------------------------------------

	/**
	 * Update.
	 * 
	 * @param context
	 *            the context
	 */
	// UPDATE
	private static void update(final Context context, final boolean onlyReceive) {
		// Switch to the next server in a round robin style
		final int serverId = Setup.getServerId(Setup
				.getNextReceivingServer(context));

		// If a message previously sent, try to send the next message with
		// priority
		// only if no message is to send, try to receive a message,
		// only if no message is to receive, try to update info.
		// If nothing was done previously, then do all
		new Thread(new Runnable() {
			public void run() {
				try {
					if (!Setup.ensureLoggedIn(context, serverId)) {
						Log.d("communicator",
								"#### Updating ... but not logged in :( ...");
						return;
					}

					if (!Communicator.messagesToSendIsUpToDate) {
						// update! maybe we have something to send!
						Communicator.messagesToSend = (DB
								.getNumberOfMessagesToSend(context,
										DB.TRANSPORT_INTERNET) > 0);
						Communicator.SMSToSend = (DB.getNumberOfMessagesToSend(
								context, DB.TRANSPORT_SMS) > 0);
						Communicator.messagesToSendIsUpToDate = true;
						Log.d("communicator",
								"#### Updating ... messagesToSend and SMSToSend ...");
					} else {
						Log.d("communicator",
								"#### Up-to-date (messagesToSend and SMSToSend)");
					}
					Log.d("communicator", "#### Communicator.messagesToSend="
							+ Communicator.messagesToSend
							+ ", Communicator.SMSToSend="
							+ Communicator.SMSToSend);

					// This means we should be logged in, so normally proceed

					// Log.d("communicator", "#### Communicator.internetOk="+
					// Communicator.internetOk + ", Communicator.loginOk=" +
					// Communicator.loginOk);
					if (Main.isAlive()) {
						Main.getInstance().updateInfoMessageBlockAsync(context);
					}

					// Do this as early as possible
					Setup.updateServerkey(context, serverId, true);


					if (Communicator.messagesToSend || Communicator.SMSToSend) {
						if (!onlyReceive) {
							// The server is chosen automatically depending on
							// the message that needs to be sent
							Communicator.sendNextMessage(context);
						}
						Communicator.haveNewMessagesAndReceive(context,
								serverId);
					} else if (Communicator.messageReceived) {
						Communicator.haveNewMessagesAndReceive(context,
								serverId);
					} else if (Main.isVisible() || Conversation.isVisible()) {
						// Only update receive info & read info if in
						// conversation!
						if (!onlyReceive) {
							Communicator.sendNextMessage(context);
						}
						Communicator.haveNewMessagesAndReceive(context,
								serverId);
					} else {
						// Background service only send & receive messages
						if (!onlyReceive) {
							Communicator.sendNextMessage(context);
						}
						Communicator.haveNewMessagesAndReceive(context,
								serverId);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	// -------------------------------------------------------------------------

}
