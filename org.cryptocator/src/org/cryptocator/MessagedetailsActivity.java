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

import org.cryptocator.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TableRow;
import android.widget.TextView;

//import android.support.v4.app.DialogFragment;

public class MessagedetailsActivity extends Activity {

	public static ConversationItem conversationItem = null;
	public static int hostUid;

	Activity activity = null;
	Context context = null;
	AlertDialog alertDialog = null;
	boolean cancel = true;
	boolean handled = false;

	// -------------------------------------------------------------------------

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		cancel = true;
		handled = false;
		activity = this;
		context = this;

		// super.setTheme(R.style.Theme_Transparent);
		// this.setTheme(android.R.style.Theme_Dialog);
		// getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);

		// ATTENTION: Necessary to see the calling activity in the background!
		// android:theme="@style/Theme.Transparent"

		// this.setStyle(DialogFragment.STYLE_NORMAL,
		// R.style.AlertDialogCustom);
		String activityTitle = "";

		LayoutInflater inflaterInfo = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		LinearLayout dialogLayout = (LinearLayout) inflaterInfo.inflate(
				R.layout.activity_messagedetails, null);

		LinearLayout outerLayout = (LinearLayout) dialogLayout
				.findViewById(R.id.messagedetailsmain);
		LinearLayout buttonLayout = (LinearLayout) dialogLayout
				.findViewById(R.id.messagedetailsbuttons);

		// / -------------

		TextView from = (TextView) dialogLayout.findViewById(R.id.from);
		TextView to = (TextView) dialogLayout.findViewById(R.id.to);
		TextView fromkey = (TextView) dialogLayout.findViewById(R.id.fromkey);
		TextView tokey = (TextView) dialogLayout.findViewById(R.id.tokey);

		TextView created = (TextView) dialogLayout.findViewById(R.id.created);
		TextView sent = (TextView) dialogLayout.findViewById(R.id.sent);
		TextView received = (TextView) dialogLayout.findViewById(R.id.received);
		TextView read = (TextView) dialogLayout.findViewById(R.id.read);
		TextView withdrawn = (TextView) dialogLayout
				.findViewById(R.id.withdrawn);

		TextView transport = (TextView) dialogLayout
				.findViewById(R.id.transport);
		TextView encrypted = (TextView) dialogLayout
				.findViewById(R.id.encrypted);

		TextView keycreated = (TextView) dialogLayout
				.findViewById(R.id.keycreated);
		TextView keyexpires = (TextView) dialogLayout
				.findViewById(R.id.keyexpires);
		TextView keyhash = (TextView) dialogLayout.findViewById(R.id.keyhash);

		// get updated information
		final ConversationItem updatedItem = DB.getMessage(context,
				conversationItem.localid, hostUid);

		String msgIdString = " [ " + updatedItem.mid + " ]";
		if (updatedItem.mid == -1) {
			msgIdString = " [ *" + updatedItem.localid + " ]";
		}
		activityTitle = "Message Details" + msgIdString;

		if (updatedItem.me(context)) {
			// from me
			fromkey.setText("Account Key: " + Setup.getPublicKeyHash(context));
			tokey.setText("Account Key: "
					+ Setup.getKeyHash(context, updatedItem.to));
		} else {
			// to me
			fromkey.setText("Account Key: "
					+ Setup.getKeyHash(context, updatedItem.from));
			tokey.setText("Account Key: " + Setup.getPublicKeyHash(context));
		}

		from.setText(Main.UID2Name(context, updatedItem.from, true));
		to.setText(Main.UID2Name(context, updatedItem.to, true));

		created.setText(DB.getDateString(updatedItem.created, true));

		sent.setText(DB.getDateString(updatedItem.sent, true));
		if (updatedItem.smsfailed) {
			sent.setText(DB.SMS_FAILED);
		}

		received.setText(DB.getDateString(updatedItem.received, true));
		read.setText(DB.getDateString(updatedItem.read, true));
		withdrawn.setText(DB.getDateString(updatedItem.withdraw, true));

		if (updatedItem.transport == DB.TRANSPORT_INTERNET) {
			transport.setText("Internet");
		} else {
			transport.setText("SMS");
		}
		if (updatedItem.encrypted) {
			encrypted.setText("Yes");
		} else {
			encrypted.setText("No");
		}

		int hostUid = updatedItem.from;
		if (updatedItem.me(context)) {
			hostUid = updatedItem.to;
		}

		long keyCreatedTS = Setup.getAESKeyDate(context, hostUid);
		long keyOutdatedTS = keyCreatedTS + Setup.AES_KEY_TIMEOUT_SENDING;
		keycreated.setText(DB.getDateString(keyCreatedTS, true));
		keyexpires.setText(DB.getDateString(keyOutdatedTS, true));
		String keyhashstring = Setup.getAESKeyHash(context, hostUid);
		keyhash.setText(keyhashstring + " (current)");

		if (updatedItem.transport == DB.TRANSPORT_INTERNET) {
			builder.setIcon(R.drawable.msginfo);
		} else {
			builder.setIcon(R.drawable.msginfosms);
		}

		Button buttonresend = (Button) dialogLayout
				.findViewById(R.id.buttonresend);
		Button buttonWithdraw = (Button) dialogLayout
				.findViewById(R.id.buttonwithdraw);
		Button buttonCopy = (Button) dialogLayout.findViewById(R.id.buttoncopy);
		Button buttonRespond = (Button) dialogLayout
				.findViewById(R.id.buttonrespond);

		buttonresend.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (resendMessage(conversationItem)) {
					activity.finish();
				}
			}
		});

		buttonWithdraw.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// ask withdraw
				askWithdraw(context, updatedItem.mid, updatedItem.localid,
						updatedItem.to);
			}
		});
		buttonCopy.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Utility.copyToClipboard(context, conversationItem.text);
				activity.finish();
			}
		});
		buttonRespond.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				String respondText = "''" + conversationItem.text + "'' -  ";
				Conversation.getInstance().messageText.setText(respondText);
				Conversation.getInstance().messageText.setSelection(
						respondText.length() - 1, respondText.length() - 1);
				Conversation.getInstance().messageText.postDelayed(
						new Runnable() {
							public void run() {
								Conversation.getInstance().messageText
										.requestFocus();
								Utility.showKeyboardExplicit(Conversation
										.getInstance().messageText);
							}
						}, 200); // 400ms important because after 200ms the
									// resume() will hid the keyboard
				activity.finish();
			}
		});

		// / -------------

		builder.setTitle(activityTitle);

		outerLayout.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// dialog.dismiss();
				activity.finish();
			}
		});

		builder.setView(dialogLayout);

		builder.setOnCancelListener(new OnCancelListener() {
			public void onCancel(DialogInterface dialog) {
				// dialog.dismiss();
				activity.finish();
			}
		});

		alertDialog = builder.show();

		// Grab the window of the dialog, and change the width
		WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
		Window window = alertDialog.getWindow();
		lp.copyFrom(window.getAttributes());
		// This makes the dialog take up the full width
		lp.width = WindowManager.LayoutParams.FILL_PARENT;
		// lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
		window.setAttributes(lp);

		Utility.setBackground(context, outerLayout, R.drawable.dolphins3light);
		Utility.setBackground(context, buttonLayout, R.drawable.dolphins4light);

		if ((!conversationItem.me(context))
				|| conversationItem.transport == DB.TRANSPORT_SMS
				|| conversationItem.withdraw > 10) {
			buttonWithdraw.setEnabled(false);
			buttonWithdraw.setVisibility(View.GONE);
		}

		if (!conversationItem.smsfailed) {
			buttonresend.setVisibility(View.GONE);
		}

		// LayoutParams params = updateNameButton.getLayoutParams();
		// params.height = 90;
		// params.width = 90;
		// params = updatePhoneButton.getLayoutParams();
		// params.height = 90;
		// params.width = 90;
		// params = updateKeyButton.getLayoutParams();
		// params.height = 90;
		// params.width = 90;

	}

	// ------------------------------------------------------------------------
	// ------------------------------------------------------------------------

	public boolean resendMessage(ConversationItem conversationItem) {
		// resend previously failed SMS
		String messageTextString = conversationItem.text;
		boolean encrypted = conversationItem.encrypted;
		int transport = conversationItem.transport;

		if (DB.addSendMessage(context, hostUid, messageTextString, encrypted,
				transport, false, DB.PRIORITY_MESSAGE)) {
			Conversation.updateConversationlistAsync(context);
			Communicator.sendNewNextMessageAsync(context, transport);
			return true;
		}
		return false;
	}

	public void askWithdraw(final Context context, final int mid,
			final int localid, final int toHostUid) {
		final Activity activity = this;
		String titleMessage = "Withdraw Message " + mid;
		if (mid == -1) {
			titleMessage = "Withdraw Message *" + localid;
		}
		String textMessage = "Attention! Withdrawing a message should be used with precaution.\n\nA withdrawn message is deleted from server. There"
				+ " is no guarantee that it is deleted from other devices that may already have received the message. All devices that connect to the server are advised to"
				+ " delete the message. Anyhow, this message may already have been read by the recipient. Furthermore, withdrawing will cancel new message notifications of the recipient. You should proceed only if there is no alternative!\n\nDo you really want to withdraw the message?";
		new MessageAlertDialog(context, titleMessage, textMessage,
				" Withdraw ", " Cancel ", null,
				new MessageAlertDialog.OnSelectionListener() {
					public void selected(int button, boolean cancel) {
						if (!cancel) {
							if (button == 0) {
								// now really try to withdraw
								DB.tryToWithdrawMessage(context, mid, localid,
										DB.getTimestampString(), toHostUid);
								activity.finish();
							}
						}
					}
				}).show();
	}

	// ------------------------------------------------------------------------

}