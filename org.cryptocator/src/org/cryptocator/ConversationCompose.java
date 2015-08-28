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

import java.util.HashMap;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnLongClickListener;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;

/**
 * The ConversationCompose class is responsible for creating new messages. It is
 * basically a stripped down version of the Conversation class.
 * 
 * @author Christian Motika
 * @since 1.2
 * @date 08/23/2015
 * 
 */
public class ConversationCompose extends Activity {

	/** The conversation root view. */
	private LinearLayout conversationRootView;

	/** The sendspinner. */
	Spinner sendspinner = null;

	/** The host uid. */
	private static int hostUid = -1;

	/** The sendbutton. */
	private ImagePressButton sendbutton;

	/** The smiley button. */
	private ImagePressButton smileybutton;

	/** The attachment button. */
	private ImagePressButton attachmentbutton;

	/** The addition button. */
	private ImagePressButton additionbutton;

	/** The phonebutton. */
	private ImagePressButton phonebutton;

	/** The message text. */
	public ImageSmileyEditText messageText;

	/** The phone or uid. */
	public EditText phoneOrUid;

	/** The list of recipients. */
	FastScrollView toList = null;

	/** The additions visible flag tells if, e.g., smilie button is visible. */
	private static boolean additionsVisible = false;

	/** The last orientation landscape. */
	private boolean lastOrientationLandscape = false;

	/** The flat telling that the user has already changed the orientation. */
	private boolean orientationChanged = false;

	// ------------------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
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

		// Apply custom title bar (with holo :-)
		// See Main.java for more explanation
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

		messageText = ((ImageSmileyEditText) findViewById(R.id.messageText));

		messageText
				.setOnCutCopyPasteListener(new ImageSmileyEditText.OnCutCopyPasteListener() {
					public void onPaste() {
						// If an image or smiley is pasted then do a new layout!
						Conversation.reprocessPossibleImagesInText(messageText);
					}

					public void onCut() {
					}

					public void onCopy() {

					}
				});

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

		// Send Button
		sendbutton = ((ImagePressButton) findViewById(R.id.sendbutton));
		LinearLayout sendbuttonparent = (LinearLayout) findViewById(R.id.sendbuttonparent);
		sendbutton.setAdditionalPressWhiteView(sendbuttonparent);

		final LinearLayout additions = (LinearLayout) findViewById(R.id.additions);
		smileybutton = ((ImagePressButton) findViewById(R.id.smileybutton));
		LinearLayout smiliebuttonparent = (LinearLayout) findViewById(R.id.smileybuttonparent);
		smileybutton.setAdditionalPressWhiteView(smiliebuttonparent);
		smileybutton.initializePressImageResource(R.drawable.smileybtn, 3, 300,
				false);

		attachmentbutton = ((ImagePressButton) findViewById(R.id.attachmentbutton));
		LinearLayout attachmentbuttonparent = (LinearLayout) findViewById(R.id.attachmentbuttonparent);
		attachmentbutton.setAdditionalPressWhiteView(attachmentbuttonparent);
		attachmentbutton.initializePressImageResource(R.drawable.attachmentbtn,
				3, 300, false);

		additionbutton = ((ImagePressButton) findViewById(R.id.additionbutton));
		// LinearLayout additionbuttonparent = (LinearLayout)
		// findViewById(R.id.additionbuttonparent);
		// additionbutton.setAdditionalPressWhiteView(additionbuttonparent);
		// additionbutton.initializePressImageResource(R.drawable.additionbtn,
		// 0,
		// 300, true);
		additionbutton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				// toggle
				additionsVisible = !additionsVisible;
				if (additionsVisible) {
					additions.setVisibility(View.VISIBLE);
				} else {
					additions.setVisibility(View.GONE);
				}
			}
		});
		if (additionsVisible) {
			additions.setVisibility(View.VISIBLE);
		} else {
			additions.setVisibility(View.GONE);
		}
		// If smileys are turned of then do not display the additions button
		if (!(Utility.loadBooleanSetting(context, Setup.OPTION_SMILEYS,
				Setup.DEFAULT_SMILEYS))) {
			smileybutton.setVisibility(View.GONE);
			LinearLayout.LayoutParams lpattachmentbuttonparent = new LinearLayout.LayoutParams(
					LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
			lpattachmentbuttonparent.setMargins(2, 0, 5, 0);
			attachmentbuttonparent.setLayoutParams(lpattachmentbuttonparent);
		}
		smileybutton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				final boolean wasKeyboardVisible = isKeyboardVisible(conversationRootView);
				SmileyPrompt smileyPrompt = new SmileyPrompt();

				smileyPrompt
						.setOnSmileySelectedListener(new SmileyPrompt.OnSmileySelectedListener() {
							public void onSelect(String textualSmiley) {
								if (textualSmiley != null) {
									Utility.smartPaste(messageText,
											textualSmiley, " ", " ", true,
											false);
								}
								if (wasKeyboardVisible) {
									potentiallyShowKeyboard(context, true);
								}
							}
						});
				smileyPrompt.promptSmileys(context);
			}
		});

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
						// fake a keyboard entry for convenience
						Conversation.lastKeyStroke = DB.getTimestamp();

						boolean nowLandscape = Utility
								.isOrientationLandscape(context);
						boolean nowOrientationChanged = (lastOrientationLandscape != nowLandscape);
						lastOrientationLandscape = nowLandscape;
						if (nowOrientationChanged) {
							orientationChanged = true;
						}

						Log.d("communicator", "SMILEY: nowLandscape="
								+ nowLandscape + ", lastOrientationLandscape="
								+ lastOrientationLandscape + ", nowChanged="
								+ nowOrientationChanged
								+ ", orientationChanged=" + orientationChanged);

						if (Utility
								.loadBooleanSetting(context,
										Setup.OPTION_QUICKTYPE,
										Setup.DEFAULT_QUICKTYPE)
								&& nowLandscape) {

							// if we NEVER changed before (=start in landscape)
							// or if we have just changed the orientation
							if (!orientationChanged || nowOrientationChanged) {
								// if keyboard is visible, then set cursor to
								// text
								// field!
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
					}
				});

		// Force overflow buttons
		Utility.forceOverflowMenuButtons(this);
		// updateMenu(this);
		parseAndMarkUserRadio(phoneOrUid.getText().toString(), 1000);

		updateSendButtonImage(context);

	}

	// -------------------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onStart()
	 */
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

	/**
	 * Update sendspinner.
	 * 
	 * @param context
	 *            the context
	 * @param dataAdapterONLYSMS
	 *            the data adapter onlysms
	 * @param dataAdapterONLYSMSChat
	 *            the data adapter onlysms chat
	 */
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

	/**
	 * Gets the my data adatpter.
	 * 
	 * @param titles
	 *            the titles
	 * @return the my data adatpter
	 */
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

	/**
	 * Gets the current uid.
	 * 
	 * @param context
	 *            the context
	 * @return the current uid
	 */
	private int getCurrentUid(Context context) {
		return parsePhoneOrUid(phoneOrUid.getText().toString());
	}

	// ------------------------------------------------------------------------

	/**
	 * Send message.
	 * 
	 * @param context
	 *            the context
	 */
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
			sendMessageOrPrompt(context, DB.TRANSPORT_SMS, false);
			return;
		}
		// now check if SMS and encryption is available
		boolean mySMSAvailable = Setup.isSMSOptionEnabled(context);
		boolean otherSMSAvailable = Setup.havePhone(context, uid);
		boolean sms = mySMSAvailable && otherSMSAvailable;
		boolean encryption = Setup.isEncryptionAvailable(context, uid);

		sendMessagePrompt(context, sms, encryption);
	}

	// ------------------------------------------------------------------------

	/**
	 * Send message prompt.
	 * 
	 * @param context
	 *            the context
	 * @param sms
	 *            the sms
	 * @param encryption
	 *            the encryption
	 */
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
										sendMessageOrPrompt(context,
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
										sendMessageOrPrompt(context,
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
										sendMessageOrPrompt(context, DB.TRANSPORT_SMS,
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
										sendMessageOrPrompt(context, DB.TRANSPORT_SMS,
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

	/**
	 * Gets the host uid.
	 * 
	 * @return the host uid
	 */
	public static int getHostUid() {
		return hostUid;
	}

	// ------------------------------------------------------------------------

	/**
	 * Send message or prompt the user if the message is too large for SMS or
	 * contains too large images for Internet/server limits.
	 * 
	 * @param context
	 *            the context
	 * @param transport
	 *            the transport
	 * @param encrypted
	 *            the encrypted
	 * @param promptLargeSMSOrLargeImages
	 *            the prompt large sms or large images
	 */
	private void sendMessageOrPrompt(final Context context,
			final int transport, final boolean encrypted) {
		final String messageTextString = messageText.getText().toString()
				.trim();
		if (messageTextString.length() > 0) {
			if (transport == DB.TRANSPORT_INTERNET) {
				final String messageTextString2 = Conversation
						.possiblyRemoveImageAttachments(context,
								messageTextString);
				if ((messageTextString2.length() != messageTextString.length())) {
					String title = "WARNING";
					String text = "This message contains at least one image that exceeded server limits. "
							+ "It will be removed automatically.\n\nDo you still want to send the message?";
					new MessageAlertDialog(context, title, text, " Yes ",
							" No ", " Cancel ",
							new MessageAlertDialog.OnSelectionListener() {
								public void selected(int button, boolean cancel) {
									if (button == MessageAlertDialog.BUTTONOK0) {
										sendMessage(context, transport,
												encrypted, messageTextString2);
									}
								}
							}).show();
					return;
				}
			} else {
				if (messageTextString.length() > Setup.SMS_SIZE_WARNING) {
					int numSMS = (int) (messageTextString.length() / Setup.SMS_DEFAULT_SIZE);
					String title = "WARNING";
					String text = "This is a large message which will need "
							+ numSMS + " SMS to be sent!\n\nReally send "
							+ numSMS + " SMS?";
					new MessageAlertDialog(context, title, text, " Yes ",
							" No ", " Cancel ",
							new MessageAlertDialog.OnSelectionListener() {
								public void selected(int button, boolean cancel) {
									if (button == MessageAlertDialog.BUTTONOK0) {
										sendMessage(context, transport,
												encrypted, messageTextString);
									}
								}
							}).show();
					return;
				}
			}
			// Message is not too long and not contains too large images
			sendMessage(context, transport, encrypted, messageTextString);
		}
	}

	// ------------------------------------------------------------------------

	/**
	 * Send message.
	 * 
	 * @param context
	 *            the context
	 * @param transport
	 *            the transport
	 * @param encrypted
	 *            the encrypted
	 */
	private void sendMessage(final Context context, final int transport,
			final boolean encrypted, final String messageTextString) {

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
			if (DB.addSendMessage(context, uid, messageTextString, encrypted,
					transport, false, DB.PRIORITY_MESSAGE)) {
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
				Utility.saveStringSetting(context, "cachedraftcomposephone", "");
				finish();
			}
		}
	}

	// ------------------------------------------------------------------------

	/**
	 * Sets the title.
	 * 
	 * @param title
	 *            the new title
	 */
	public void setTitle(String title) {
		TextView titletext = (TextView) findViewById(R.id.titletext);
		titletext.setText(title);
	}

	// -------------------------------------------------------------------------
	// ------------------------------------------------------------------------

	// "If the process is killed then all static variables will be reinitialized to their default values."

	/** The instance. */
	// private static boolean visible;
	private static ConversationCompose instance = null;

	/** The visible flag. */
	private static boolean visible = false;

	/** The alive flag. */
	private static boolean alive = false;

	/**
	 * Gets the single instance of ConversationCompose.
	 * 
	 * @return single instance of ConversationCompose
	 */
	public static ConversationCompose getInstance() {
		return instance;
	}

	/**
	 * Checks if is visible.
	 * 
	 * @return true, if is visible
	 */
	// Returns true if the activity is visible
	public static boolean isVisible() {
		return (visible && instance != null);
	}

	/**
	 * Checks if is alive.
	 * 
	 * @return true, if is alive
	 */
	// Returns true if an instance is available
	public static boolean isAlive() {
		return (instance != null && ConversationCompose.alive);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onDestroy()
	 */
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
			Utility.showToastShortAsync(this, "Draft saved.");
		}
		ConversationCompose.visible = false;
		super.onStop();
	}

	// ------------------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onPause()
	 */
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

	// ------------------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onResume()
	 */
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
	// ------------------------------------------------------------------------

	/** The user radio mapping for auto-selecting the current user. */
	@SuppressLint("UseSparseArrays")
	private HashMap<Integer, RadioButton> userRadioMapping = new HashMap<Integer, RadioButton>();

	/** The user order mapping for ordering the userlist. */
	@SuppressLint("UseSparseArrays")
	private HashMap<Integer, Integer> userOrderMapping = new HashMap<Integer, Integer>();

	// ------------------------------------------------------------------------

	/**
	 * Parses the and mark user radio.
	 * 
	 * @param phoneOrUid
	 *            the phone or uid
	 * @param delay
	 *            the delay
	 */
	@SuppressLint("UseSparseArrays")
	private void parseAndMarkUserRadio(String phoneOrUid, int delay) {
		int tmp = parsePhoneOrUid(phoneOrUid);
		// Log.d("communicator", "@@@@@@ " + phoneOrUid + " >>> " + tmp);
		markUserRadio(tmp, delay);
	}

	// ------------------------------------------------------------------------

	/**
	 * Parses the phone or uid text field and returns the uid if it was given in
	 * form of "[uid]" where uid is an integer. It returns -1 if this is not a
	 * valid uid.
	 * 
	 * @param phoneOrUid
	 *            the phone or uid
	 * @return the int
	 */
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

	// ------------------------------------------------------------------------

	/**
	 * Mark user radio for the particular uid of the selected recipient.
	 * 
	 * @param uid
	 *            the uid
	 * @param delay
	 *            the delay
	 */
	private void markUserRadio(int uid, final int delay) {
		for (int useruid : userRadioMapping.keySet()) {
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

	// ------------------------------------------------------------------------

	/**
	 * Builds the recipient list.
	 * 
	 * @param context
	 *            the context
	 */
	@SuppressLint("InflateParams")
	private void buildToList(final Context context) {
		toList.clearChilds();
		userRadioMapping.clear();
		userOrderMapping.clear();
		int i = 0;

		List<Integer> uidList = Main.loadUIDList(context);
		List<UidListItem> fullUidList = Main.buildSortedFullUidList(context,
				uidList, true);

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

	@SuppressWarnings("deprecation")
	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onActivityResult(int, int,
	 * android.content.Intent)
	 */
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

	/**
	 * Go back.
	 * 
	 * @param context
	 *            the context
	 */
	public void goBack(Context context) {
		// GET TO THE MAIN SCREEN IF THIS ICON IS CLICKED !
		Intent intent = new Intent(this, Main.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
	}

	// -------------------------------------------------------------------------

	/**
	 * Update send button image.
	 * 
	 * @param context
	 *            the context
	 */
	public void updateSendButtonImage(Context context) {
		int uid = getCurrentUid(context);
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

	/** The min width diff. */
	private static int minWidthDiff = 100;

	/** The m last height differece. */
	private static int lastHeightDifferece;

	/** The keyboard visible. */
	private static boolean keyboardVisible = false;

	/**
	 * Checks if is keyboard visible. This is a central message that is used in
	 * {@link OnGlobalLayoutListener} in order to detect keyboard
	 * opening/closing.
	 * 
	 * @param rootView
	 *            the root view
	 * @return true, if is keyboard visible
	 */
	private boolean isKeyboardVisible(View rootView) {
		// Screen height
		Rect rect = new Rect();
		rootView.getWindowVisibleDisplayFrame(rect);
		int screenHeight = rootView.getRootView().getHeight();
		// Height difference of root view and screen heights
		int heightDifference = screenHeight - (rect.bottom - rect.top);

		// If height difference is different then the last time and if
		// is bigger than 1/4 of the screen => assume visible keyboard
		if (heightDifference != lastHeightDifferece) {
			if (heightDifference > screenHeight / 4
					&& ((heightDifference > lastHeightDifferece + minWidthDiff) || (heightDifference < lastHeightDifferece
							- minWidthDiff))) {
				// Keyboard is now visible
				// Log.d("communicator", "@@@@@@ CHANGE TO VISIBLE=TRUE");
				lastHeightDifferece = heightDifference;
				keyboardVisible = true;
			} else if (heightDifference < screenHeight / 4) {
				// Log.d("communicator", "@@@@@@ CHANGE TO VISIBLE=FALSE");
				// Keyboard is now hidden
				lastHeightDifferece = heightDifference;
				keyboardVisible = false;
			}
		}
		return keyboardVisible;
	}

	// -------------------------------------------------------------------------

	/**
	 * Show the keyboard (if parameter is true) but always fake lastKeyStrok
	 * 
	 * @param context
	 *            the context
	 * @param showKeyboard
	 *            the show keyboard
	 */
	public void potentiallyShowKeyboard(final Context context,
			final boolean showKeyboard) {
		// Fake a keyboard entry for convenience: If the user immediately
		// wants to start typing - he just can and will not be interrupted!
		// :-)
		Conversation.lastKeyStroke = DB.getTimestamp();
		messageText.postDelayed(new Runnable() {
			public void run() {
				// Fake a keyboard entry for convenience
				Conversation.lastKeyStroke = DB.getTimestamp();
				messageText.requestFocus();
				Utility.showKeyboardExplicit(messageText);
				messageText.requestFocus();
			}
		}, 200);

	}

	// -------------------------------------------------------------------------

}
