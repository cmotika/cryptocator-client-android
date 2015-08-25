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
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
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
import android.widget.TextView;

//import android.support.v4.app.DialogFragment;

public class UserdetailsActivity extends Activity {

	public static int uid = -1;
	public static String HIDDENPHONETEXT = "[ automatic ]";

	EditText name;
	EditText phone;
	String phoneHidden; // this is used to save the NOT visible phone number for registered users where the number is not manually edited
	EditText key;
	CheckBox nameCheck;
	CheckBox phoneCheck;

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
		builder.setTitle(Main.UID2Name(context, uid, true));

		LinearLayout.LayoutParams lpTextTitle = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.FILL_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		lpTextTitle.setMargins(20, 20, 20, 20);

		// LinearLayout.LayoutParams lpSection = new LinearLayout.LayoutParams(
		// LinearLayout.LayoutParams.FILL_PARENT,
		// LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
		// lpSection.setMargins(15, 5, 15, 5);

		LinearLayout.LayoutParams lpSectionInnerLeft = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT,
				LinearLayout.LayoutParams.WRAP_CONTENT, .8f);
		lpSectionInnerLeft.setMargins(20, 5, 0, 15);
		LinearLayout.LayoutParams lpSectionInnerRight = new LinearLayout.LayoutParams(
				90, LinearLayout.LayoutParams.WRAP_CONTENT, 0f);
		lpSectionInnerRight.setMargins(0, 5, 15, 15);

		LinearLayout outerLayout = new LinearLayout(context);
		outerLayout.setOrientation(LinearLayout.VERTICAL);
		outerLayout.setGravity(Gravity.CENTER_HORIZONTAL);
		LinearLayout.LayoutParams lpOuterLayout = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.FILL_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		outerLayout.setLayoutParams(lpOuterLayout);

		TextView details = new TextView(context);
		details.setText("THis is some example information....\nAfter new line\nAnothern ew line");
		details.setLayoutParams(lpTextTitle);
		details.setTextSize(16);
		details.setTextColor(Color.WHITE);

		TextView detailsName = new TextView(context);
		detailsName.setText("Display Name: ");
		// detailsName.setLayoutParams(lpTextTitle);
		name = new EditText(context);
		nameCheck = new CheckBox(context);
		nameCheck.setText("Automatically Update");
		LinearLayout nameInnerLayout = new LinearLayout(context);
		nameInnerLayout.setOrientation(LinearLayout.VERTICAL);
		nameInnerLayout.setGravity(Gravity.CENTER_HORIZONTAL);
		nameInnerLayout.addView(detailsName);
		nameInnerLayout.addView(name);
		nameInnerLayout.addView(nameCheck);
		nameInnerLayout.setLayoutParams(lpSectionInnerLeft);

		ImageButton updateNameButton = new ImageButton(context);
		Drawable mapImage3 = context.getResources().getDrawable(
				R.drawable.update);
		if (uid < 0) {
			mapImage3 = context.getResources()
					.getDrawable(R.drawable.updatesms);
		}
		updateNameButton.setImageDrawable(mapImage3);
		updateNameButton.setLayoutParams(lpSectionInnerRight);
		updateNameButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				updateUsername();
			}
		});

		LinearLayout nameLayout = new LinearLayout(context);
		nameLayout.setOrientation(LinearLayout.HORIZONTAL);
		nameLayout.setGravity(Gravity.CENTER_HORIZONTAL);
		nameLayout.addView(nameInnerLayout);
		nameLayout.addView(updateNameButton);
		// nameLayout.setLayoutParams(lpSection);

		TextView detailsPhone = new TextView(context);
		detailsPhone.setText("Phone Number: ");
		// detailsPhone.setLayoutParams(lpTextTitle);
		phone = new EditText(context);
		phone.setInputType(InputType.TYPE_CLASS_PHONE);
		phoneCheck = new CheckBox(context);
		phoneCheck.setText("Automatically Update");
		// android:ems="10"
		// android:inputType="phone">

		LinearLayout phoneInnerLayout = new LinearLayout(context);
		phoneInnerLayout.setOrientation(LinearLayout.VERTICAL);
		phoneInnerLayout.setGravity(Gravity.CENTER_HORIZONTAL);
		phoneInnerLayout.addView(detailsPhone);
		phoneInnerLayout.addView(phone);
		phoneInnerLayout.addView(phoneCheck);
		phoneInnerLayout.setLayoutParams(lpSectionInnerLeft);

		ImageButton updatePhoneButton = new ImageButton(context);
		Drawable mapImage = context.getResources()
				.getDrawable(R.drawable.phone);
		if (uid < 0) {
			mapImage = context.getResources()
					.getDrawable(R.drawable.phonesms);
		}
		updatePhoneButton.setImageDrawable(mapImage);
		updatePhoneButton.setLayoutParams(lpSectionInnerRight);
		updatePhoneButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
				intent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
				startActivityForResult(intent, 1);
			}
		});

		LinearLayout phoneLayout = new LinearLayout(context);
		phoneLayout.setOrientation(LinearLayout.HORIZONTAL);
		phoneLayout.setGravity(Gravity.CENTER_HORIZONTAL);
		phoneLayout.addView(phoneInnerLayout);
		phoneLayout.addView(updatePhoneButton);
		// phoneLayout.setLayoutParams(lpSection);

		TextView detailsKey = new TextView(context);
		detailsKey.setText("Account Key: ");
		// detailsKey.setLayoutParams(lpTextTitle);
		key = new EditText(context);
		// key.setFocusable(false);
		// key.setClickable(true);
		//key.setTextSize(8);
		key.setKeyListener(null);

		LinearLayout keyInnerLayout = new LinearLayout(context);
		keyInnerLayout.setOrientation(LinearLayout.VERTICAL);
		keyInnerLayout.setGravity(Gravity.CENTER_HORIZONTAL);
		keyInnerLayout.addView(detailsKey);
		keyInnerLayout.addView(key);
		keyInnerLayout.setLayoutParams(lpSectionInnerLeft);

		ImageButton updateKeyButton = new ImageButton(context);
		Drawable mapImage2 = context.getResources().getDrawable(
				R.drawable.update);
		updateKeyButton.setImageDrawable(mapImage2);
		updateKeyButton.setLayoutParams(lpSectionInnerRight);
		updateKeyButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				updateKey();
			}
		});

		LinearLayout keyLayout = new LinearLayout(context);
		keyLayout.setOrientation(LinearLayout.HORIZONTAL);
		keyLayout.setGravity(Gravity.CENTER_HORIZONTAL);
		keyLayout.addView(keyInnerLayout);
		keyLayout.addView(updateKeyButton);
		// keyLayout.setBackgroundColor(Color.BLUE);
		// keyLayout.setLayoutParams(lpSection);

		LinearLayout buttonLayout = new LinearLayout(context);
		buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
		buttonLayout.setGravity(Gravity.CENTER_HORIZONTAL
				| Gravity.CENTER_VERTICAL);
		buttonLayout.setBackgroundColor(Color.rgb(150, 150, 150));
		LinearLayout.LayoutParams lpButtonSection = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.FILL_PARENT, 90, 0f);
		// lpButtonSection.setMargins(0, 5, 15, 15);
		buttonLayout.setLayoutParams(lpButtonSection);

		LinearLayout layout = new LinearLayout(context);
		layout.setOrientation(LinearLayout.VERTICAL);
		layout.setGravity(Gravity.LEFT);
		outerLayout.addView(layout);

		layout.addView(details);
		layout.addView(nameLayout);
		layout.addView(phoneLayout);
		layout.addView(keyLayout);

		LinearLayout.LayoutParams lpScrollView = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.FILL_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT, .8f);
		// lpScrollView.setMargins(20, 5, 0, 15);
		ScrollView scrollView = new ScrollView(context);
		scrollView.setLayoutParams(lpScrollView);
		scrollView.addView(outerLayout);

		LinearLayout dialogLayout = new LinearLayout(context);
		dialogLayout.setOrientation(LinearLayout.VERTICAL);
		dialogLayout.addView(scrollView);
		dialogLayout.addView(buttonLayout);

		builder.setView(dialogLayout);

		builder.setOnCancelListener(new OnCancelListener() {
			public void onCancel(DialogInterface dialog) {
				// dialog.dismiss();
				activity.finish();
			}
		});

		if (uid >= 0) {
			builder.setIcon(R.drawable.buttonedit);
		} else {
			builder.setIcon(R.drawable.buttoneditsms);
		}

		alertDialog = builder.show();

		// Grab the window of the dialog, and change the width
		WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
		Window window = alertDialog.getWindow();
		lp.copyFrom(window.getAttributes());
		// This makes the dialog take up the full width
		lp.width = WindowManager.LayoutParams.FILL_PARENT;
		// lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
		window.setAttributes(lp);

		Button deleteButton = new Button(context);
		// deleteButton.setVisibility(View.GONE);
		buttonLayout.addView(deleteButton);
		deleteButton.setVisibility(View.VISIBLE);
		deleteButton.setText("  Delete  ");
		deleteButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (Main.isAlive()) {
					try {
						final String titleMessage = "Delete "
								+ Main.UID2Name(context, uid, false);
						final String textMessage = "Really delete "
								+ Main.UID2Name(context, uid, false)
								+ " and all messages?";
						new MessageAlertDialog(context, titleMessage,
								textMessage, " Delete ", " Cancel ", null,
								new MessageAlertDialog.OnSelectionListener() {
									public void selected(int button,
											boolean cancel) {
										if (!cancel) {
											if (button == 0) {
												// delete
												if (Main.isAlive()) {
													Main.getInstance()
															.deleteUser(
																	context,
																	uid);
												}
												finish();
											}
										}
									}
								}).show();
					} catch (Exception e) {
						// ignore
					}
				}
			}
		});

		Button okButton = new Button(context);
		// okButton.setVisibility(View.GONE);
		buttonLayout.addView(okButton);
		okButton.setVisibility(View.VISIBLE);
		okButton.setText("   Save   ");
		okButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (!phone
						.getText()
						.toString()
						.equals(Setup
								.normalizePhone(phone.getText().toString()))) {
					phone.setText(Setup.normalizePhone(phone.getText()
							.toString()));
				} else {
					save(context);
					cancel = false;
					finish();
				}
			}
		});

		Button cancelButton = new Button(context);
		// cancelButton.setVisibility(View.GONE);
		buttonLayout.addView(cancelButton);
		cancelButton.setVisibility(View.VISIBLE);
		cancelButton.setText("   Cancel   ");
		cancelButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				cancel = true;
				finish();
			}
		});

		Utility.setBackground(context, outerLayout, R.drawable.dolphins3light);
		Utility.setBackground(context, buttonLayout, R.drawable.dolphins4light);

		LayoutParams params = updateNameButton.getLayoutParams();
		params.height = 90;
		params.width = 90;
		params = updatePhoneButton.getLayoutParams();
		params.height = 90;
		params.width = 90;
		params = updateKeyButton.getLayoutParams();
		params.height = 90;
		params.width = 90;

//		if (uid >= 0) {
//			// Do not display phon number iff registered user!
//			phoneLayout.setVisibility(View.GONE);
//		}

		// DATA
		String text = "UID: " + uid + "\n\n";

		if (uid >= 0) {
			text += "Registered user\n";
		} else {
			text += "External SMS contact\n";
		}

		if (Setup.haveKey(context, uid)) {
			text += "Encryption available\n";
		} else {
			text += "No encryption available\n";
		}

		if (Setup.havePhone(context, uid)) {
			text += "SMS sending available";
		} else {
			text += "No SMS sending available";
		}
		details.setText(text);

		key.setText(Setup.getKeyHash(context, uid));

		name.setText(Main.UID2Name(context, uid, false));

		if (Setup.havePhone(context, uid)) {
			phoneHidden = Setup.getPhone(context, uid);
			if (Setup.isPhoneModified(context, uid)) {
				// the phone number has been modified so it is okay to display it
				phone.setText(phoneHidden);
			} else {
				// the phone number was set by the server for a registered user, so it is NOT okay to display it
				// for privacy!
				phone.setText(HIDDENPHONETEXT);
			}
		} else {
			phone.setText("");
		}

		if (uid >= 0) {
			nameCheck.setChecked(Main.isUpdateName(context, uid));
			phoneCheck.setChecked(Main.isUpdatePhone(context, uid));
		} else {
			nameCheck.setChecked(false);
			nameCheck.setEnabled(false);
			nameCheck.setVisibility(View.GONE);
			phoneCheck.setChecked(false);
			phoneCheck.setEnabled(false);
			phoneCheck.setVisibility(View.GONE);
			// updateNameButton.setVisibility(View.GONE);
			updateKeyButton.setVisibility(View.GONE);
		}
	}

	// ------------------------------------------------------------------------

	private void updateUsername() {

		if (uid >= 0) {
			Main.updateUID2Name(this, uid, new Main.UpdateListener() {
				public void onUpdate(final String data) {
					final Handler mUIHandler = new Handler(Looper
							.getMainLooper());
					mUIHandler.post(new Thread() {
						@Override
						public void run() {
							super.run();
							if (!data.equals("-1")) {
								name.setText(data);
							} else {
								name.setText(Main.UID2Name(context, uid, false,
										true));
							}
						}
					});
				}
			});
		} else {
			// update from phonebook if possible
			String phoneString = Setup.normalizePhone(phone.getText()
					.toString());
			String nameString = Main.getNameFromAddressBook(context,
					phoneString);
			if (nameString == null) {
				// name not found, take telephone number as a default name for
				// SMS users
				nameString = phoneString;
			}
			name.setText(nameString);

		}

	}

	// ------------------------------------------------------------------------

	private void updateKey() {
		key.setText("Updating...");
		Communicator.getKeyFromServer(this, uid, new Main.UpdateListener() {
			public void onUpdate(final String data) {
				final Handler mUIHandler = new Handler(Looper.getMainLooper());
				mUIHandler.post(new Thread() {
					@Override
					public void run() {
						super.run();
						key.setText(Setup.getKeyHash(context, uid));
//						key.setText(data);
					}
				});
			}
		});
	}

	// ------------------------------------------------------------------------

	private void save(Context context) {
		
		String newPhone = Setup.normalizePhone(phone.getText().toString());
		if (!newPhone.equals(HIDDENPHONETEXT)) {
			// the phone number has been manually edited, so we can now flag it as manually edited and show it next time
			Setup.savePhoneIsModified(context, uid, true);
			Setup.savePhone(context, uid,
				newPhone, true);
		}
		Main.saveUID2Name(context, uid, name.getText().toString());
		Main.setUpdateName(context, uid, nameCheck.isChecked());
		Main.setUpdatePhone(context, uid, phoneCheck.isChecked());
	}

	// ------------------------------------------------------------------------

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == 1) {
			if (resultCode == RESULT_OK) {
				Uri contactData = data.getData();
				Cursor cursor = managedQuery(contactData, null, null, null,
						null);
				cursor.moveToFirst();

				String phone = cursor
						.getString(cursor
								.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Nickname.NAME));
				String name = cursor
						.getString(cursor
								.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME));

				if (uid < 0) {
					this.name.setText(name);
				}
				this.phone.setText(Setup.normalizePhone(phone));

				// normalize phone number
			}
		}
	}

}