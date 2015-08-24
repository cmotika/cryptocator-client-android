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
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.cryptocator.ScrollViewEx.ScrollViewExListener;

import org.cryptocator.R;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.NumberKeyListener;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnLongClickListener;
import android.view.WindowManager.LayoutParams;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Spinner;
//import android.support.v7.widget.PopupMenu;
import android.widget.TextView;
import android.view.ViewGroup;
import android.view.View.OnKeyListener;

public class ConversationCompose extends Activity {

	// private LinearLayout conversationinnerview;
	private LinearLayout conversationRootView;

	Spinner sendspinner = null;

	private static boolean alive = false;
	private static int hostUid = -1;

	private ImagePressButton sendbutton;
	private ImagePressButton phonebutton;
	public EditText messageText;
	public EditText phoneOrUid;

	private ScrollViewEx scrollView;

	private LayoutInflater inflater;

	// tolist
	FastScrollView toList = null;

	public static int getHostUid() {
		return hostUid;
	}

	@Override
	protected void onStart() {
		super.onStart();
		final Context context = this;

		setTitle("New Message");

		// SET SENDSPINNER
		sendspinner = (Spinner) findViewById(R.id.sendspinner);

		final String OPTION0 = "SELECT AN OPTIONS"; // / NOT DISPLAYED BY DATA
													// ADAPTER!!!
		final String OPTIONCHATON = "  Enable Chat Mode";
		final String OPTIONCHATOFF = "  Disable Chat Mode";

		final String OPTIONUSECSMS = "  Send Unsecure SMS";

		String[] spinnerTitlesONLYSMS = { OPTION0, OPTIONCHATON };
		String[] spinnerTitlesONLYSMSChat = { OPTION0, OPTIONCHATOFF, };

		// Populate the spinner using a customized ArrayAdapter that hides the
		// first (dummy) entry
		final ArrayAdapter<String> dataAdapterONLYSMS = getMyDataAdatpter(spinnerTitlesONLYSMS);
		final ArrayAdapter<String> dataAdapterONLYSMSChat = getMyDataAdatpter(spinnerTitlesONLYSMSChat);

		updateSendspinner(context, dataAdapterONLYSMS, dataAdapterONLYSMSChat);

		sendspinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				Object object = sendspinner.getSelectedItem();
				if (object instanceof String) {
					String option = (String) object;
					if (option.equals(OPTIONCHATON)
							|| (option.equals(OPTIONCHATOFF))) {
						boolean chatmodeOn = Utility.loadBooleanSetting(
								context, Setup.OPTION_CHATMODE,
								Setup.DEFAULT_CHATMODE);
						chatmodeOn = !chatmodeOn;
						Utility.saveBooleanSetting(context,
								Setup.OPTION_CHATMODE, chatmodeOn);
						updateSendspinner(context, dataAdapterONLYSMS,
								dataAdapterONLYSMSChat);
						if (chatmodeOn) {
							Utility.showToastAsync(context,
									"Chat mode enabled.");
						} else {
							Utility.showToastAsync(context,
									"Chat mode disabled.");
						}
					}
				}
				sendspinner.setSelection(0);
			}

			public void onNothingSelected(AdapterView<?> arg0) {
				//
			}
		});
		sendspinner.setSelection(0);
		// END SET SENDSPINNER

	}

	// -------------------------------

	private void updateSendspinner(Context context,
			ArrayAdapter<String> dataAdapterONLYSMS,
			ArrayAdapter<String> dataAdapterONLYSMSChat) {
		boolean chatmodeOn = Utility.loadBooleanSetting(context,
				Setup.OPTION_CHATMODE, Setup.DEFAULT_CHATMODE);

		// only SMS mode available
		if (chatmodeOn) {
			sendspinner.setAdapter(dataAdapterONLYSMSChat);
		} else {
			sendspinner.setAdapter(dataAdapterONLYSMS);
		}
	}

	// -------------------------------

	ArrayAdapter<String> getMyDataAdatpter(String[] titles) {
		final ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_dropdown_item, titles) {
			@Override
			public View getDropDownView(int position, View convertView,
					ViewGroup parent) {
				View v = null;

				// If this is the initial dummy entry, make it hidden
				if (position == 0) {
					TextView tv = new TextView(getContext());
					tv.setHeight(0);
					tv.setVisibility(View.GONE);
					v = tv;
				} else {
					// Pass convertView as null to prevent reuse of special case
					// views
					v = super.getDropDownView(position, null, parent);
				}

				// Hide scroll bar because it appears sometimes unnecessarily,
				// this does not prevent scrolling
				parent.setVerticalScrollBarEnabled(false);
				return v;
			}
		};
		return dataAdapter;
	}

	// ------------------------------------------------------------------------
	// ------------------------------------------------------------------------

	private int getCurrentUid(Context context) {
		return parsePhoneOrUid(phoneOrUid.getText().toString());
	}

	private void sendMessage(final Context context) {
		String messageTextString = messageText.getText().toString();
		int uid = getCurrentUid(context);
		if (messageTextString == null || messageTextString.trim().length() == 0
				|| uid == 0) {
			return;
		}
		if (uid < 0) {
			// for not registered SMS users we do not prompt but just send an
			// sms
			sendMessage(context, DB.TRANSPORT_SMS, false);
			return;
		}
		// now check if SMS and encryption is available
		boolean mySMSAvailable = Setup.isSMSOptionEnabled(context);
		boolean otherSMSAvailable = Setup.havePhone(context, uid);
		boolean sms = mySMSAvailable && otherSMSAvailable;
		boolean encryption = Setup.isEncryptionAvailable(context, uid);

		sendMessagePrompt(context, sms, encryption);
	}

	private void sendMessagePrompt(final Context context, final boolean sms,
			final boolean encryption) {
		String name = "";
		int uid = parsePhoneOrUid(phoneOrUid.getText().toString());
		if (uid != -1) {
			name = Main.UID2Name(context, uid, false);
		} else {
			int backupUid = ReceiveSMS.getUidByPhoneOrCreateUser(context,
					phoneOrUid.getText().toString(), false);
			if (backupUid != -1) {
				name = Main.UID2Name(context, backupUid, false);
			}
		}

		String title = "Send Message to " + name;
		String text = "Send the new message encrypted or not encrypted and via Internet or SMS?";
		if (sms && !encryption) {
			text = "Send the new message (unencrypted) via Internet or SMS?";
		} else if (!sms && encryption) {
			text = "Send the new message encrypted or not encrypted?";
		}

		new MessageAlertDialog(context, title, text, null, null, " Cancel ",
				new MessageAlertDialog.OnSelectionListener() {
					public void selected(int button, boolean cancel) {
						// nothing
					}
				}, new MessageAlertDialog.OnInnerViewProvider() {

					public View provide(final MessageAlertDialog dialog) {
						LinearLayout buttonLayout = new LinearLayout(context);
						buttonLayout.setOrientation(LinearLayout.VERTICAL);
						buttonLayout.setGravity(Gravity.CENTER_HORIZONTAL);
						LinearLayout buttonLayout2 = new LinearLayout(context);
						buttonLayout2.setOrientation(LinearLayout.HORIZONTAL);
						buttonLayout2.setGravity(Gravity.CENTER_HORIZONTAL);
						LinearLayout buttonLayout1 = new LinearLayout(context);
						buttonLayout1.setOrientation(LinearLayout.HORIZONTAL);
						buttonLayout1.setGravity(Gravity.CENTER_HORIZONTAL);

						LinearLayout.LayoutParams lpButtons = new LinearLayout.LayoutParams(
								180, 140);
						lpButtons.setMargins(10, 10, 10, 10);
						LinearLayout.LayoutParams lpButtonsLayout = new LinearLayout.LayoutParams(
								LayoutParams.WRAP_CONTENT,
								LayoutParams.WRAP_CONTENT);
						lpButtonsLayout.setMargins(0, 0, 0, 20);

						ImageLabelButton internetButtonE = new ImageLabelButton(
								context);
						internetButtonE.setTextAndImageResource("Internet",
								R.drawable.sendlock);
						internetButtonE.setLayoutParams(lpButtons);
						internetButtonE
								.setOnClickListener(new View.OnClickListener() {
									public void onClick(View v) {
										sendMessage(context,
												DB.TRANSPORT_INTERNET, true);
										dialog.dismiss();
									}
								});

						ImageLabelButton internetButton = new ImageLabelButton(
								context);
						internetButton.setTextAndImageResource(
								"Internet Unsec.", R.drawable.send);
						internetButton.setLayoutParams(lpButtons);
						internetButton
								.setOnClickListener(new View.OnClickListener() {
									public void onClick(View v) {
										sendMessage(context,
												DB.TRANSPORT_INTERNET, false);
										dialog.dismiss();
									}
								});

						ImageLabelButton smsButtonE = new ImageLabelButton(
								context);
						smsButtonE.setTextAndImageResource("SMS",
								R.drawable.sendsmslock);
						smsButtonE.setLayoutParams(lpButtons);
						smsButtonE
								.setOnClickListener(new View.OnClickListener() {
									public void onClick(View v) {
										sendMessage(context, DB.TRANSPORT_SMS,
												true);
										dialog.dismiss();
									}
								});

						ImageLabelButton smsButton = new ImageLabelButton(
								context);
						smsButton.setTextAndImageResource("SMS Unsecure",
								R.drawable.sendsms);
						smsButton.setLayoutParams(lpButtons);
						smsButton
								.setOnClickListener(new View.OnClickListener() {
									public void onClick(View v) {
										sendMessage(context, DB.TRANSPORT_SMS,
												false);
										dialog.dismiss();
									}
								});

						if (sms && encryption) {
							buttonLayout1.addView(internetButtonE);
							buttonLayout1.addView(internetButton);
							buttonLayout2.addView(smsButtonE);
							buttonLayout2.addView(smsButton);
							buttonLayout.addView(buttonLayout1);
							buttonLayout.addView(buttonLayout2);
							buttonLayout2.setLayoutParams(lpButtonsLayout);
						} else if (sms && !encryption) {
							buttonLayout1.addView(internetButton);
							buttonLayout1.addView(smsButton);
							buttonLayout.addView(buttonLayout1);
							buttonLayout1.setLayoutParams(lpButtonsLayout);
						} else if (!sms && encryption) {
							buttonLayout1.addView(internetButtonE);
							buttonLayout1.addView(internetButton);
							buttonLayout.addView(buttonLayout1);
							buttonLayout1.setLayoutParams(lpButtonsLayout);
						} else if (!sms && !encryption) {
							buttonLayout.addView(internetButton);
							buttonLayout.setLayoutParams(lpButtonsLayout);
						}

						return buttonLayout;
					}
				}).show();
	}

	// ------------------------------------------------------------------------

	private void sendMessage(Context context, int transport, boolean encrypted) {
		String messageTextString = messageText.getText().toString();
		if (messageTextString.trim().length() > 0) {

			// this is the main uid we first try
			int uid = parsePhoneOrUid(phoneOrUid.getText().toString());

			// Log.d("communicator", "######## sendMessage uid=" + uid);

			// if a phone number is entered then take the backup uid
			if (uid == -1) {
				String phoneString = phoneOrUid.getText().toString();
				// Log.d("communicator", "######## sendMessage phoneString="
				// + phoneString);
				if (phoneString == null || phoneString.trim().length() == 0) {
					return;
				}
				// try to find user... or create a new one
				int backupUid = ReceiveSMS.getUidByPhoneOrCreateUser(context,
						phoneString, true);
				// Log.d("communicator", "######## sendMessage backupUid="
				// + backupUid);
				uid = backupUid;
			}

			String name = Main.UID2Name(context, uid, false);
			// Log.d("communicator", "######## sendMessage name=" + name);

			if (uid != -1) {
				// Log.d("communicator",
				// "######## sendMessage SENDING NOW... transport="
				// + transport + ", encrypted=" + encrypted);
				if (DB.addSendMessage(context, uid, messageTextString,
						encrypted, transport, false, DB.PRIORITY_MESSAGE)) {
					Communicator.sendNewNextMessageAsync(context, transport);
					String encryptedText = "";
					if (!encrypted) {
						encryptedText = "unsecure ";
					}
					if (transport == DB.TRANSPORT_INTERNET) {
						Utility.showToastInUIThread(context, "Sending "
								+ encryptedText + "message to " + name + ".");
					} else {
						Utility.showToastInUIThread(context, "Sending "
								+ encryptedText + "SMS to " + name + ".");
					}
					messageText.setText("");
					phoneOrUid.setText("");
					Utility.saveStringSetting(context, "cachedraftcompose", "");
					Utility.saveStringSetting(context,
							"cachedraftcomposephone", "");
					finish();
				}
			}
		}
	}

	// ------------------------------------------------------------------------
	// ------------------------------------------------------------------------

	public void setTitle(String title) {
		TextView titletext = (TextView) findViewById(R.id.titletext);
		titletext.setText(title);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ConversationCompose.visible = true;
		instance = this;
		alive = true;
		final Activity context = this;

		// POSSIBLY RECEIVE SHARED-DATA!
		if (getIntent() != null) {
			Bundle extras = getIntent().getExtras();
			if (extras != null) {
				String sharedText = getIntent().getStringExtra(
						Intent.EXTRA_TEXT);
				if (sharedText != null) {
					// save the text as draft (override any old draft!) ... will
					// be loaded further down
					Utility.saveStringSetting(context, "cachedraftcompose",
							sharedText);
				}
			}
		}

		if (Utility.loadBooleanSetting(context, Setup.OPTION_NOSCREENSHOTS,
				Setup.DEFAULT_NOSCREENSHOTS)) {
			getWindow().setFlags(LayoutParams.FLAG_SECURE,
					LayoutParams.FLAG_SECURE);
		}

		inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		// Apply custom title bar (with holo :-)
		LinearLayout main = Utility.setContentViewWithCustomTitle(this,
				R.layout.activity_conversation_compose, R.layout.title_general);
		// main.setGravity(Gravity.BOTTOM);
		Utility.setBackground(this, main, R.drawable.dolphins1);

		LinearLayout titlegeneral = (LinearLayout) findViewById(R.id.titlegeneral);
		Utility.setBackground(this, titlegeneral, R.drawable.dolphins3blue);
		ImagePressButton btnback = (ImagePressButton) findViewById(R.id.btnback);
		btnback.initializePressImageResource(R.drawable.btnback);
		LinearLayout btnbackparent = (LinearLayout) findViewById(R.id.btnbackparent);
		btnback.setAdditionalPressWhiteView(btnbackparent);
		btnback.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				goBack(context);
			}
		});

		toList = (FastScrollView) findViewById(R.id.tolist);
		toList.setSnapDown(80);
		toList.setSnapUp(20);

		messageText = ((EditText) findViewById(R.id.messageText));

		TextWatcher textWatcher = new TextWatcher() {
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				Conversation.lastKeyStroke = DB.getTimestamp();
			}

			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			public void afterTextChanged(Editable s) {
			}
		};
		messageText.addTextChangedListener(textWatcher);

		// ActionBar actionBar = getActionBar();
		// actionBar.setDisplayHomeAsUpEnabled(true);

		// Send Button

		sendbutton = ((ImagePressButton) findViewById(R.id.sendbutton));
		LinearLayout sendbuttonparent = (LinearLayout) findViewById(R.id.sendbuttonparent);
		sendbutton.setAdditionalPressWhiteView(sendbuttonparent);

		sendbutton.setLongClickable(true);
		sendbutton.setOnLongClickListener(new OnLongClickListener() {
			public boolean onLongClick(View v) {
				sendspinner.performClick();
				return false;
			}
		});

		sendbutton.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				sendMessage(context);
			}
		});

		messageText.setOnKeyListener(new OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				// Log.d("communicator",
				// "@@@@ setOnKeyListener() : keyCode = " + keyCode);
				boolean chatmodeOn = Utility.loadBooleanSetting(context,
						Setup.OPTION_CHATMODE, Setup.DEFAULT_CHATMODE);
				if (keyCode == 66 && chatmodeOn) {
					if (chatmodeOn) {
						sendbutton.performClick();
					}
					return true;
				}
				return false;
			}

		});

		LinearLayout inputLayout = ((LinearLayout) findViewById(R.id.inputlayout));
		LinearLayout inputLayout2 = ((LinearLayout) findViewById(R.id.inputlayout2));
		conversationRootView = (LinearLayout) findViewById(R.id.conversationRootView);
		Utility.setBackground(this, conversationRootView, R.drawable.dolphins2);
		Utility.setBackground(this, inputLayout, R.drawable.dolphins1);
		Utility.setBackground(this, inputLayout2, R.drawable.dolphins1);

		// DO NOT SCROLL HERE BECAUSE onResume() WILL DO THIS.
		// onResume() is ALWAYS called if the user starts OR returns to the APP!
		// scrollOnCreateOrResume(context);
		// scrollView = (ScrollViewEx)
		// findViewById(R.id.conversationscrollview);

		phoneOrUid = (EditText) findViewById(R.id.phone);
		phonebutton = (ImagePressButton) findViewById(R.id.phonebutton);
		phonebutton.initializePressImageResourceWhite(R.drawable.phoneneutral,
				false);
		phonebutton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
				intent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
				startActivityForResult(intent, 1);
			}
		});

		TextWatcher textWatcher2 = new TextWatcher() {
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				updateSendButtonImage(context);
			}

			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			public void afterTextChanged(Editable s) {
			}
		};
		phoneOrUid.addTextChangedListener(textWatcher2);

		// build userlist before parsing draft!
		buildToList(context);

		// possibly load draft
		String draft = Utility.loadStringSetting(this, "cachedraftcompose", "");
		if (draft != null && draft.length() > 0) {
			messageText.setText(draft);
			messageText.setSelection(0, draft.length());
			Utility.saveStringSetting(this, "cachedraftcompose", "");
		}
		String draftPhone = Utility.loadStringSetting(this,
				"cachedraftcomposephone", "");
		if (draftPhone != null && draftPhone.length() > 0) {
			phoneOrUid.setText(draftPhone);
			parseAndMarkUserRadio(draftPhone, 200);
			Utility.saveStringSetting(this, "cachedraftcomposephone", "");
		}

		// The following code is necessary to FORCE further scrolling down if
		// the virtual keyboard
		// is brought up. Otherwise the top scrolled position is remaining but
		// this is uncomfortable!

		conversationRootView.getViewTreeObserver().addOnGlobalLayoutListener(
				new ViewTreeObserver.OnGlobalLayoutListener() {
					public void onGlobalLayout() {

						if (Utility
								.loadBooleanSetting(context,
										Setup.OPTION_QUICKTYPE,
										Setup.DEFAULT_QUICKTYPE)
								&& Utility.isOrientationLandscape(context)) {
							// if keyboard is visible, then set cursor to text
							// field!
							// fake a keyboard entry for convenience
							Conversation.lastKeyStroke = DB.getTimestamp();
							messageText.postDelayed(new Runnable() {
								public void run() {
									// fake a keyboard entry for convenience
									Conversation.lastKeyStroke = DB
											.getTimestamp();
									messageText.requestFocus();
									Utility.showKeyboardExplicit(messageText);
									messageText.requestFocus();
								}
							}, 200);
						}
					}
				});

		// Force overflow buttons
		Utility.forceOverflowMenuButtons(this);
		// updateMenu(this);
		parseAndMarkUserRadio(phoneOrUid.getText().toString(), 1000);

		updateSendButtonImage(context);

	}

	// -------------------------------------------------------------------------

	// -------------------------------------------------------------------------
	// -------------------------------------------------------------------------

	// "If the process is killed then all static variables will be reinitialized to their default values."

	// private static boolean visible;
	private static ConversationCompose instance = null;

	private static boolean visible = false;

	public static ConversationCompose getInstance() {
		return instance;
	}

	// Returns true if the activity is visible
	public static boolean isVisible() {
		return (visible && instance != null);
	}

	// Returns true if an instance is available
	public static boolean isAlive() {
		return (instance != null && ConversationCompose.alive);
	}

	@Override
	public void onDestroy() {
		ConversationCompose.visible = false;
		alive = false;
		super.onDestroy();
		/*
		 * Note: do not count on this method being called as a place for saving
		 * data! For example, if an activity is editing data in a content
		 * provider, those edits should be committed in either onPause() or
		 * onSaveInstanceState(Bundle), not here. This method is usually
		 * implemented to free resources like threads that are associated with
		 * an activity, so that a destroyed activity does not leave such things
		 * around while the rest of its application is still running. There are
		 * situations where the system will simply kill the activity's hosting
		 * process without calling this method (or any others) in it, so it
		 * should not be used to do things that are intended to remain around
		 * after the process goes away.
		 * 
		 * You can move your code to onPause() or onStop()
		 */
	}

	// --------------------------------------

	/*
	 * 
	 * // possibly load draft String draft = Utility.loadStringSetting(this,
	 * "cachedraftcompose", ""); if (draft != null && draft.length() > 0) {
	 * messageText.setText(draft); messageText.setSelection(0, draft.length());
	 * Utility.saveStringSetting(this, "cachedraftcompose", ""); } String
	 * draftPhone = Utility.loadStringSetting(this, "cachedraftcomposephone" ,
	 * ""); if (draftPhone != null && draftPhone.length() > 0) {
	 * phone.setText(draftPhone); Utility.saveStringSetting(this,
	 * "cachedraftcomposephone", ""); } (non-Javadoc)
	 * 
	 * @see android.app.Activity#onStop()
	 */

	@Override
	public void onStop() {
		// if chat is not empty, save it as draft
		String msg = messageText.getText().toString();
		boolean saved = false;
		if (msg != null && msg.trim().length() > 0) {
			String draft = Utility.loadStringSetting(this, "cachedraftcompose",
					"");
			if (!draft.equals(msg)) {
				// only if message changed
				Utility.saveStringSetting(this, "cachedraftcompose", msg);
				saved = true;
			}
		}
		String phoneString = phoneOrUid.getText().toString();
		if (phoneString != null && phoneString.trim().length() > 0) {
			String draftphone = Utility.loadStringSetting(this,
					"cachedraftcomposephone", "");
			if (!draftphone.equals(phoneString)) {
				// only if message changed
				Utility.saveStringSetting(this, "cachedraftcomposephone",
						phoneString);
				saved = true;
			}
		}
		if (saved) {
			Utility.showToastShortAsync(this, "Message saved as draft.");
		}
		ConversationCompose.visible = false;
		super.onStop();
	}

	// ------------------------------------------------------------------------

	@Override
	protected void onPause() {
		super.onPause();
		// Necessary for the following situation:
		// if scrolled to somewhere and changing focused activity, the
		// scrollview would automatically try to
		// scroll up! this prevents!
		// Log.d("communicator", "@@@@ onPause() LOCK POSITION ");
		// scrollView.lockPosition();
	}

	// @Override
	// protected void onSaveInstanceState(Bundle outState) {
	// super.onSaveInstanceState(outState);
	// scrollView.lockPosition();
	// Log.d("communicator", "#### onSaveInstanceState() LOCK POSITION ");
	// }

	// --------------------------------------

	// @Override
	// protected void onRestoreInstanceState (Bundle outState) {
	// super.onRestoreInstanceState (outState);
	// scrollView.restoreLockedPosition();
	// Log.d("communicator",
	// "#### onRestoreInstanceState() RESTORE LOCKED POSITION ");
	// }

	// --------------------------------------

	@Override
	public void onResume() {
		super.onResume();
		// updateMenu(this);
		if (!ConversationCompose.isAlive()) {
			// Log.d("communicator",
			// "#### ConversationCompose onResume() NOT ALIVE ANY MORE ");
			// this class instance was lost, close it
			ConversationCompose.visible = false;
			this.finish();
		} else {
			// Log.d("communicator",
			// "#### ConversationCompose onResume() ALIVE !!! :-)");
			ConversationCompose.visible = true;
			// Reset error claims
			Setup.setErrorUpdateInterval(this, false);
			Scheduler.reschedule(this, false, false, true);

			// ALWAYS SHOW KEYBOARD
			messageText.requestFocus();
			Utility.showKeyboardExplicit(messageText);
			parseAndMarkUserRadio(phoneOrUid.getText().toString(), 100);
		}
	}

	// ------------------------------------------------------------------------

	private HashMap<Integer, RadioButton> userRadioMapping = new HashMap<Integer, RadioButton>();
	private HashMap<Integer, Integer> userOrderMapping = new HashMap<Integer, Integer>();

	private void parseAndMarkUserRadio(String phoneOrUid, int delay) {
		int tmp = parsePhoneOrUid(phoneOrUid);
		// Log.d("communicator", "@@@@@@ " + phoneOrUid + " >>> " + tmp);
		markUserRadio(tmp, delay);
	}

	private int parsePhoneOrUid(String phoneOrUid) {
		if (phoneOrUid.startsWith("[") && phoneOrUid.endsWith("]")) {
			try {
				String tmp = phoneOrUid.substring(1, phoneOrUid.length() - 1);
				return Utility.parseInt(tmp, -1);
			} catch (Exception e) {
			}
		}
		return -1;
	}

	private void markUserRadio(int uid, final int delay) {
		int i = 0;
		for (int useruid : userRadioMapping.keySet()) {
			i++;
			userRadioMapping.get(useruid).setChecked(useruid == uid);
			if (useruid == uid) {
				final int scrollItem = userOrderMapping.get(useruid);
				toList.postDelayed(new Runnable() {
					public void run() {
						toList.scrollToItem(scrollItem);
					}
				}, delay);
			}
		}
	}

	private void buildToList(final Context context) {
		// Log.d("communicator", "#### ConversationCompose buildToList :-)");

		toList.clearChilds();
		userRadioMapping.clear();
		userOrderMapping.clear();
		int i = 0;

		List<Integer> uidList = Main.loadUIDList(context);
		// for (int uid : uidList) {
		// Log.d("communicator", "#### ConversationCompose uid " + uid);
		// }

		List<UidListItem> fullUidList = Main.buildSortedFullUidList(context,
				uidList, true);

		// for (UidListItem item : fullUidList) {
		// Log.d("communicator", "#### ConversationCompose uid " + item.name);
		// }

		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		for (UidListItem item : fullUidList) {

			final int uid = item.uid;
			String name = new String(item.name);

			LinearLayout tolistitem = (LinearLayout) inflater.inflate(
					R.layout.tolistitem, null);

			final RadioButton userradio = (RadioButton) tolistitem
					.findViewById(R.id.userradio);
			userRadioMapping.put(uid, userradio);
			userOrderMapping.put(uid, i++);
			userradio.setOnClickListener(new View.OnClickListener() {

				public void onClick(View v) {
					markUserRadio(uid, 100);
					phoneOrUid.setText("[" + uid + "]");
					updateSendButtonImage(context);
				}
			});
			userradio.setId(uid + name.hashCode());
			userradio.setText(name);
			ImageView usericon = (ImageView) tolistitem
					.findViewById(R.id.usericon);
			usericon.setImageResource(R.drawable.personsmall);
			if (uid < 0) {
				usericon.setImageResource(R.drawable.personsmssmall);
			}

			toList.addChild(tolistitem);
		}

	}

	// ------------------------------------------------------------------------
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

				this.phoneOrUid.setText(Setup.normalizePhone(phone));
				// Unmark all potentially marked users
				markUserRadio(-1, 0);
				// normalize phone number
			}

			// ALWAYS SHOW KEYBOARD
			messageText.requestFocus();
			Utility.showKeyboardExplicit(messageText);
			updateSendButtonImage(this);
		}
	}

	// -------------------------------------------------------------------------

	public void goBack(Context context) {
		// GET TO THE MAIN SCREEN IF THIS ICON IS CLICKED !
		Intent intent = new Intent(this, Main.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
	}

	// -------------------------------------------------------------------------

	// public boolean onOptionsItemSelected(MenuItem item) {
	// switch (item.getItemId()) {
	// case android.R.id.home:
	// // GET TO THE MAIN SCREEN IF THIS ICON IS CLICKED !
	// Intent intent = new Intent(this, Main.class);
	// intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	// startActivity(intent);
	// return true;
	// default:
	// return super.onOptionsItemSelected(item);
	// }
	// }

	// -------------------------------------------------------------------------

	public void updateSendButtonImage(Context context) {
		int uid = getCurrentUid(context);
		// now check if SMS and encryption is available
		// boolean mySMSAvailable = Setup.isSMSOptionEnabled(context);
		// boolean otherSMSAvailable = Setup.havePhone(context, uid);
		// boolean sms = mySMSAvailable && otherSMSAvailable;
		// boolean encryption = Setup.isEncryptionAvailable(context, uid);

		if (uid == 0 || phoneOrUid == null || phoneOrUid.length() == 0) {
			// system, disable
			sendbutton.setEnabled(false);
			sendbutton.setImageResource(R.drawable.senddisabled);
			sendbutton.deactivatePressImageResourceWhite();
		} else {
			if ((uid == -1 || !Main.alreadyInList(uid,
					Main.loadUIDList(context)))
					&& !Utility.isValidPhoneNumber(phoneOrUid.getText()
							.toString())) {
				sendbutton.setEnabled(false);
				sendbutton.setImageResource(R.drawable.senddisabled);
				sendbutton.deactivatePressImageResourceWhite();
			} else {
				sendbutton.setEnabled(true);
				if (uid < 0) {
					sendbutton.setImageResource(R.drawable.sendsms);
					sendbutton.initializePressImageResource(R.drawable.sendsms,
							false);
				} else {
					sendbutton.setImageResource(R.drawable.send);
					sendbutton.initializePressImageResource(R.drawable.send,
							false);
				}
			}
		}
		parseAndMarkUserRadio(phoneOrUid.getText().toString(), 100);

	}

	// -------------------------------------------------------------------------

}
