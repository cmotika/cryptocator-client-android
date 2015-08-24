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

import java.util.ArrayList;
import java.util.List;

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
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TableRow;
import android.widget.TextView;

//import android.support.v4.app.DialogFragment;

public class BackupActivity extends Activity {

	public static ConversationItem conversationItem = null;
	public static int hostUid;

	private int firstMid = 0;
	private int lastMid = 0;
	private List<ConversationItem> conversationList = new ArrayList<ConversationItem>();

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
				R.layout.activity_backup, null);

		LinearLayout outerLayout = (LinearLayout) dialogLayout
				.findViewById(R.id.backupmain);
		LinearLayout buttonLayout = (LinearLayout) dialogLayout
				.findViewById(R.id.backupbuttons);

		// / -------------

		builder.setIcon(R.drawable.backup);
		activityTitle = "Backup Messages";

		final EditText fromText = (EditText) dialogLayout
				.findViewById(R.id.fromid);
		final EditText toText = (EditText) dialogLayout.findViewById(R.id.toid);

		final CheckBox first = (CheckBox) dialogLayout
				.findViewById(R.id.fromfirst);
		final CheckBox last = (CheckBox) dialogLayout.findViewById(R.id.tolast);
		final CheckBox details = (CheckBox) dialogLayout
				.findViewById(R.id.details);

		DB.loadConversation(context, hostUid, conversationList, -1);

		if (conversationList.size() > 0) {
			lastMid = conversationList.get(conversationList.size() - 1).mid;
			firstMid = conversationList.get(0).mid;
		}
		fromText.setText(firstMid + "");
		toText.setText(lastMid + "");

		first.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				fromText.setText(firstMid + "");
				if (isChecked) {
					fromText.setVisibility(View.GONE);
				} else {
					fromText.setVisibility(View.VISIBLE);
				}
			}
		});
		last.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				toText.setText(lastMid + "");
				if (isChecked) {
					toText.setVisibility(View.GONE);
				} else {
					toText.setVisibility(View.VISIBLE);
				}
			}
		});
		first.setChecked(true);
		last.setChecked(true);

		Button buttonBackup = (Button) dialogLayout
				.findViewById(R.id.buttonbackup);
		Button buttonCancel = (Button) dialogLayout
				.findViewById(R.id.buttoncancel);

		buttonBackup.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// DO BACKUP
				StringBuilder backupText = new StringBuilder();
				boolean active = false;

				int fromMid = Utility.parseInt(fromText.getText().toString(),
						-1);
				int toMid = Utility.parseInt(toText.getText().toString(), -1);

				for (ConversationItem item : conversationList) {

					if (!active && item.mid >= fromMid) {
						active = true;
					}

					if (active) {
						if (backupText.length() != 0) {
							backupText.append(System
									.getProperty("line.separator"));
							backupText.append(System
									.getProperty("line.separator"));
						}

						backupText.append(Main.UID2Name(context, item.from,
								true));
						if (details.isChecked()) {
							backupText.append(System.getProperty("line.separator"));
							backupText.append("Created: "
									+ DB.getDateString(item.created, false));
							backupText.append(System
									.getProperty("line.separator"));
							backupText.append("Sent: "
									+ DB.getDateString(item.sent, false));
							backupText.append(System
									.getProperty("line.separator"));
							backupText.append("Received: "
									+ DB.getDateString(item.received, false));
							backupText.append(System
									.getProperty("line.separator"));
							backupText.append("Read: "
									+ DB.getDateString(item.read, false));
							backupText.append(System
									.getProperty("line.separator"));
							backupText.append("Withdrawn: "
									+ DB.getDateString(item.withdraw, false));
							backupText.append(System
									.getProperty("line.separator"));
							if (item.transport == DB.TRANSPORT_INTERNET) {
								backupText.append("Transport: Internet");
							} else {
								backupText.append("Transport: SMS");
							}
							backupText.append(System
									.getProperty("line.separator"));
							if (item.encrypted) {
								backupText.append("Encrypted: Yes");
							} else {
								backupText.append("Encrypted: No");
							}
							backupText.append(System
									.getProperty("line.separator"));
						} else {
							backupText.append(" @ ");
							backupText.append(DB
									.getDateString(item.sent, false));
						}
						backupText.append(System.getProperty("line.separator"));
						backupText.append(item.text);
					}
					
					if (active && item.mid >= toMid) {
						active = false;
					}
					

				}

				Log.d("communicator", "@@@@ BACKUP : "
						+ backupText.toString());

				Utility.copyToClipboard(context, backupText.toString());
				if (backupText.toString().length() > 0) {
					Utility.showToastAsync(context,
							"Backup of Conversation in Clipboard now.");
				} else {
					Utility.showToastAsync(context,
							"Nothing to backup.");
				}

				activity.finish();
			}
		});
		buttonCancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				activity.finish();
			}
		});

		// / -------------

		builder.setTitle(activityTitle);

		// outerLayout.setOnClickListener(new View.OnClickListener() {
		// public void onClick(View v) {
		// // dialog.dismiss();
		// activity.finish();
		// }
		// });

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

}