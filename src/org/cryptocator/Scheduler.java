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

import java.util.Date;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/*
 * 
 <action android:name="android.intent.action.PROVIDER_CHANGED" />
 <action android:name="android.intent.action.BOOT_COMPLETED" />
 <action android:name="android.intent.action.USER_PRESENT" />
 <action android:name="android.intent.action.ANY_DATA_STATE" />
 <action android:name="android.intent.action.DATE_CHANGED" />
 <!-- <action android:name="android.intent.action.TIME_TICK" /> -->
 <action android:name="android.intent.action.USER_PRESENT" />
 <action android:name="android.intent.action.SERVICE_STATE" />
 <action android:name="android.net.conn.CONNECTIVITY_CHANGE" />
 <action android:name="com.google.gservices.intent.action.GSERVICES_CHANGED" />
 <action android:name="android.accounts.LOGIN_ACCOUNTS_CHANGED" />
 <action android:name="android.intent.action.SIM_STATE_CHANGED" />
 <action android:name="android.intent.action.SCREEN_ON" />
 <action android:name="android.intent.action.SCREEN_OFF" />
 <action android:name="android.intent.action.CLOSE_SYSTEM_DIALOGS" />
 <action android:name="com.google.gservices.intent.action.GSERVICES_OVERRIDE" />
 <action android:name="android.intent.action.NOTIFICATION_UPDATE" />
 <action android:name="android.intent.action.NOTIFICATION_ADD" />
 <action android:name="android.intent.action.UMS_DISCONNECTED" />
 <action android:name="android.intent.action.PHONE_STATE" />
 <action android:name="android.intent.action.UID_REMOVED" />
 <action android:name="android.intent.action.UMS_CONNECTED" />
 <action android:name="android.net.conn.BACKGROUND_DATA_SETTING_CHANGED" />
 <action android:name="android.intent.action.NOTIFICATION_ADD" />
 <action android:name="android.net.conn.BACKGROUND_DATA_SETTING_CHANGED" />
 <action android:name="android.intent.action.CONFIGURATION_CHANGED" />
 <action android:name="android.intent.action.LOCALE_CHANGED" />

 if event is ongoing() ---> put it in ongoingEventList

 * 
 * 
 */

public class Scheduler extends BroadcastReceiver {

	interface OnUpdateListener {
		public void update();
	}

	public static PendingIntent pendingIntent = null;
	private static OnUpdateListener listener = null;

	// -------------------------------------------------------------------------

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();

		// Do the rescheduling silently!
		// Log.d("communicator",
		// "#### SCHEDULE INTENT ON CalendarAlarmScheduler, action = "
		// + action);

		if (action != null && action.endsWith("USER_PRESENT")) {
			// IF THE SCREEN TURNS ON AND WE ARE STILL IN CONVERSATION ... THEN
			// UPDATE THESE IMMEDIATELY
			update(context);
		}

		Scheduler.reschedule(context, false, true, false);
	}

	// Retrieve the pendingIntent of the Scheduler
	public static void updatePendingIntent(Context context) {
		if (pendingIntent == null) {
			pendingIntent = PendingIntent.getBroadcast(context, 0, new Intent(
					context, Scheduler.class),
					PendingIntent.FLAG_UPDATE_CURRENT);
		}
	}

	// -------------------------------------------------------------------------
	// -------------------------------------------------------------------------

	public static void setUpdateListener(OnUpdateListener onUpdateListener) {
		listener = onUpdateListener;
	}

	public static void clearUpdateListener() {
		listener = null;
	}

	// -------------------------------------------------------------------------
	// -------------------------------------------------------------------------

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

			// if everythin goes right then update possibly
			if (alsoUpdate) {
				if (Conversation.isVisible() && Conversation.isTyping()) {
					Log.d("communicator", "#### UPDATE SKIPPED - TYPING");
				} else {
					Log.d("communicator", "#### UPDATE - NOT TYPING");
					update(context);
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

	// UPDATE
	private static void update(final Context context) {
		// If a message previously sent, try to send the next message with
		// priority
		// only if no message is to send, try to receive a message,
		// only if no message is to receive, try to update info.
		// If nothing was done previously, then do all
		new Thread(new Runnable() {
			public void run() {
				try {
					if (!Setup.ensureLoggedIn(context)) {
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
					}
					Log.d("communicator", "#### Communicator.messagesToSend="
							+ Communicator.messagesToSend
							+ ", Communicator.SMSToSend="
							+ Communicator.SMSToSend);

					// This means we should be logged in, so normally proceed

					// Log.d("communicator", "#### Communicator.internetOk="+
					// Communicator.internetOk + ", Communicator.loginOk=" +
					// Communicator.loginOk);
					if (Main.isVisible()) {
						Main.getInstance().updateInfoMessageBlockAsync(context);
					}

					// Do this as early as possible
					Setup.updateServerkey(context);

					Log.d("communicator", "#### Communicator.messageSent="
							+ Communicator.messageSent
							+ ", Communicator.messagesToSend="
							+ Communicator.messagesToSend);

					if (Communicator.messageSent || Communicator.messagesToSend) {
						Communicator.sendNextMessage(context);
						Communicator.haveNewMessagesAndReceive(context);
					} else if (Communicator.messageReceived) {
						Communicator.haveNewMessagesAndReceive(context);
					} else if (Main.isVisible() || Conversation.isVisible()) {
						// Only update receive info & read info if in
						// conversation!
						Communicator.sendNextMessage(context);
						Communicator.haveNewMessagesAndReceive(context);
					} else {
						// Background service only send & receive messages
						Communicator.sendNextMessage(context);
						Communicator.haveNewMessagesAndReceive(context);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	// -------------------------------------------------------------------------
	// -------------------------------------------------------------------------

	// public static WatchDogThread watchDogThread = null;
	//
	// public static void startWatchDogThread(Context context, int seconds) {
	// if (watchDogThread == null) {
	// watchDogThread = new WatchDogThread(context, seconds);
	// watchDogThread.start();
	// } else {
	// watchDogThread.cancel();
	// }
	// }
	//
	// public static void stopWatchDogThread() {
	// if (watchDogThread != null) {
	// watchDogThread.cancel();
	// }
	// }
	//
	// static class WatchDogThread extends Thread {
	// Context context;
	// boolean cancel = false;
	// int seconds = 0;
	//
	// public WatchDogThread(Context context,
	// int seconds) {
	// super();
	// this.context = context;
	// this.seconds = seconds;
	// }
	//
	// public void cancel() {
	// cancel = true;
	// }
	//
	// public void run() {
	// Setup.debugLog(context, "Scheduler.WatchDog #1");
	// while (!cancel) {
	// seconds--;
	// Log.d("Notdienst", "XXX WatchDog " + seconds);
	// if (seconds < 0) {
	// break;
	// }
	// try {
	// Thread.sleep(1000);
	// } catch (InterruptedException e) {
	// e.printStackTrace();
	// }
	// }
	// Setup.debugLog(context, "Scheduler.WatchDog #2 cancel="+cancel);
	// if (!cancel) {
	// Setup.debugLog(context, "Scheduler.WatchDog #3 ERROR - RESCHEDULE");
	// // FAST RESCHEDULE ON ERROR AFTER SOME (ERROR INTERVAL) TIME
	// Scheduler.reschedule(context, true, false, true);
	// }
	// watchDogThread = null;
	// }
	//
	// }

	// -------------------------------------------------------------------------
	// -------------------------------------------------------------------------

}
