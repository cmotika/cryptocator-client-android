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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore.Images;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.Spinner;
import android.widget.TextView;

/**
 * The Conversation activity is the second most important class. It holds the
 * currently displayed conversation. Also there should always only be one
 * instance of this activity. Hence some data is static for faster access.
 * 
 * @author Christian Motika
 * @date 08/23/2015
 * @since 1.2
 * 
 */
@SuppressLint("InflateParams")
public class Conversation extends Activity {

	/** The fast scroll view. */
	FastScrollView fastScrollView;

	/** The conversation root view. */
	private View conversationRootView;

	/** The titleconversation. */
	private LinearLayout titleconversation;

	/** The conversation list. */
	private List<ConversationItem> conversationList = new ArrayList<ConversationItem>();

	/** The conversation list diff. */
	private List<ConversationItem> conversationListDiff = new ArrayList<ConversationItem>(); // only
																								// new
	/** The sendspinner. */
	Spinner sendspinner = null;

	/** The failed color as background for failed SMS. */
	private static int FAILEDCOLOR = Color.parseColor("#6DB0FD");

	/** The mapping. */
	@SuppressLint("UseSparseArrays")
	private HashMap<Integer, Mapping> mapping = new HashMap<Integer, Mapping>();

	/** The host uid. */
	private static int hostUid = -1;

	/** The has scrolled. */
	private static boolean hasScrolled = false;

	/** The scroll item. */
	private static int scrollItem = -1;

	/** The max scroll message items. */
	private static int maxScrollMessageItems = -1;

	/**
	 * The scrolled down. If scrolled down, maintain scrolled down lock even
	 * when orientation changes, keyboard comes up or new message arrives.
	 */
	public static boolean scrolledDown = false;

	/** The scrolled up. Keep being scrolled up if this was set to true earlier. */
	private static boolean scrolledUp = false;

	/** The conversation size. */
	private int conversationSize = -1;

	/** The central send button. */
	private ImagePressButton sendbutton;

	/** The smiley button. */
	private ImagePressButton smileybutton;

	/** The attachment button. */
	private ImagePressButton attachmentbutton;

	/** The addition button. */
	private ImagePressButton additionbutton;

	/** The message text. */
	public ImageSmileyEditText messageText;

	/** The inflater. */
	private LayoutInflater inflater;

	/** The additions visible flag tells if, e.g., smilie button is visible. */
	private static boolean additionsVisible = false;

	/**
	 * The last height is necessary to detect if the height changes. If it NOT
	 * changes we can skip a lot of processing which makes the UI feel much
	 * faster.
	 */
	int lastHeight = 0;

	/**
	 * The color of the fast scroll background when scrolling and not scroll
	 * locked down.
	 */
	final int FASTSCROLLBACKSCROLLINGBACKGROUND = Color.parseColor("#44000000");

	/**
	 * The color of the fast scroll background when scroll locked down.
	 */
	final int FASTSCROLLBACKLOCKEDBACKGROUND = Color.parseColor("#00555555");

	// ------------------------------------------------------------------------

	/**
	 * The internal class Mapping for mapping message ids to elements in the
	 * conversation in order to update, e.g., e state icons.
	 */
	private class Mapping {

		/** The speech. */
		ImageView speech;

		/** The oneline. */
		LinearLayout oneline;

		/** The sent. */
		ImageView sent;

		/** The received. */
		ImageView received;

		/** The read. */
		ImageView read;

		/** The text. */
		EditText text;
	}

	// ------------------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Setup.possiblyDisableScreenshot(this);
		super.onCreate(savedInstanceState);
		Conversation.visible = true;
		instance = this;
		final Activity context = this;

		inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		// Apply custom title bar (with holo :-)
		// See comment in Main.java
		LinearLayout main = Utility.setContentViewWithCustomTitle(this,
				R.layout.activity_conversation, R.layout.title_conversation);
		main.setGravity(Gravity.BOTTOM);
		titleconversation = (LinearLayout) findViewById(R.id.titleconversation);

		// Add title bar buttons
		ImagePressButton btnback = (ImagePressButton) findViewById(R.id.btnback);
		btnback.initializePressImageResource(R.drawable.btnback);
		LinearLayout btnbackparent = (LinearLayout) findViewById(R.id.btnbackparent);
		btnback.setAdditionalPressWhiteView(btnbackparent);
		btnback.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				goBack(context);
			}
		});
		ImagePressButton btnkey = (ImagePressButton) findViewById(R.id.btnkey);
		btnkey.initializePressImageResource(R.drawable.btnkey);
		LinearLayout btnkeyparent = (LinearLayout) findViewById(R.id.btnkeyparent);
		btnkey.setAdditionalPressWhiteView(btnkeyparent);
		btnkey.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				possiblePromptNewSession(context);
			}
		});
		ImagePressButton btnsearch = (ImagePressButton) findViewById(R.id.btnsearch);
		btnsearch.initializePressImageResource(R.drawable.btnsearch);
		LinearLayout btnsearchparent = (LinearLayout) findViewById(R.id.btnsearchparent);
		btnsearch.setAdditionalPressWhiteView(btnsearchparent);
		btnsearch.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				promptSearch(context);
			}
		});
		ImagePressButton btnmenu = (ImagePressButton) findViewById(R.id.btnmenu);
		btnmenu.initializePressImageResource(R.drawable.btnmenu);
		LinearLayout btnmenuparent = (LinearLayout) findViewById(R.id.btnmenuparent);
		btnmenu.setAdditionalPressWhiteView(btnmenuparent);
		btnmenu.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				openOptionsMenu();
			}
		});

		// conversationinnerview = ((LinearLayout)
		// findViewById(R.id.conversationinnerview));
		fastScrollView = (FastScrollView) findViewById(R.id.fastscrollview);

		fastScrollView
				.setOnNoHangListener(new FastScrollView.OnNoHangListener() {
					public boolean noHangNeeded() {
						return isTypingFast();
					}
				});

		messageText = ((ImageSmileyEditText) findViewById(R.id.messageText));
		// fastscrollbar = (FastScrollBar) findViewById(R.id.fastscrollbar);

		messageText
				.setOnCutCopyPasteListener(new ImageSmileyEditText.OnCutCopyPasteListener() {
					public void onPaste() {
						// If an image or smiley is pasted then do a new layout!
						reprocessPossibleImagesInText(messageText);
					}

					public void onCut() {
					}

					public void onCopy() {
						promptImageSaveAs(context);
					}
				});

		TextWatcher textWatcher = new TextWatcher() {
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				// DO FAST COMPUTATION HERE
				// It does appear that System.currentTimeMillis() is twice as
				// fast as System.nanoTime(). However 29ns is going to be much
				// shorter than anything else you'd be measuring anyhow.
				lastKeyStroke = System.currentTimeMillis();
			}

			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			public void afterTextChanged(Editable s) {
			}
		};
		messageText.addTextChangedListener(textWatcher);

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
											false, true);
								}
								if (wasKeyboardVisible) {
									Log.d("communicator",
											"@@@@ smileybutton->scrollDownNow()");
									scrollDownNow(context, true);
								}
							}
						});
				smileyPrompt.promptSmileys(context);
			}
		});
		attachmentbutton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				promptImageInsert(context, hostUid);
			}
		});

		if (hostUid == 0) {
			// system, disable
			sendbutton.setEnabled(false);
		}

		sendbutton.setLongClickable(true);
		sendbutton.setOnLongClickListener(new OnLongClickListener() {
			public boolean onLongClick(View v) {
				sendspinner.performClick();
				return false;
			}
		});

		updateSendButtonImage(context);

		sendbutton.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				if (isSMSModeAvailableAndOn(context)) {
					sendMessageOrPrompt(context, DB.TRANSPORT_SMS, true);
				} else {
					sendMessageOrPrompt(context, DB.TRANSPORT_INTERNET, true);
				}
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

		// Possibly load draft
		String draft = Utility.loadStringSetting(this, "cachedraft" + hostUid,
				"");
		if (draft != null && draft.length() > 0) {
			messageText.setText(draft);
			messageText.setSelection(0, draft.length());
			Utility.saveStringSetting(this, "cachedraft" + hostUid, "");
		}

		// Here we initialize / rebuild the complete (visible) conversation
		// list.
		rebuildConversationlist(context);

		// Setting backgrounds
		Utility.setBackground(this, main, R.drawable.dolphins2);
		Utility.setBackground(this, titleconversation, R.drawable.dolphins3blue);
		View inputLayout = ((View) findViewById(R.id.inputlayout));
		conversationRootView = (View) findViewById(R.id.conversationRootView);
		Utility.setBackground(this, conversationRootView, R.drawable.dolphins2);
		Utility.setBackground(this, inputLayout, R.drawable.dolphins1);

		// DO NOT SCROLL HERE BECAUSE onResume() WILL DO THIS.
		// onResume() is ALWAYS called if the user starts OR returns to the APP!
		// DO NOT -> scrollOnCreateOrResume(context);

		// Snap to 100% if >90%
		fastScrollView.setSnapDown(85);
		fastScrollView.setSnapUp(10);

		fastScrollView
				.setOnScrollListener(new FastScrollView.OnScrollListener() {

					public void onScrollRested(FastScrollView fastScrollView,
							int x, int y, int oldx, int oldy, int percent,
							int item) {

						// Log.d("communicator",
						// "@@@@ onScrollRested SCROLL ("+fastScrollView.hashCode()+") CHANGED =======> "
						// + fastScrollView.heights.size() + ", " +
						// fastScrollView.heightsSum);

						if (percent >= 99) {
							scrolledDown = true;
							scrolledUp = false;
							fastScrollView
									.setScrollBackground(FASTSCROLLBACKLOCKEDBACKGROUND);
						} else if (percent <= 1) {
							showTitlebarAsync(context);
							scrolledUp = true;
							scrolledDown = false;
							fastScrollView
									.setScrollBackground(FASTSCROLLBACKSCROLLINGBACKGROUND);
						} else {
							scrolledUp = false;
							scrolledDown = false;
							fastScrollView
									.setScrollBackground(FASTSCROLLBACKSCROLLINGBACKGROUND);
						}
						scrollItem = item;

						// Log.d("communicator",
						// "@@@@ onScrollRested() :  y = " + y
						// + ", percent=" + percent + ", item="
						// + item + ", getMaxPosition="
						// + fastScrollView.getMaxPosition());

						// Log.d("communicator",
						// "@@@@ onScrollRested() :  scrollItem = "
						// + scrollItem + ", percent=" + percent
						// + ", scrolledUp=" + scrolledUp
						// + ", scrolledDown=" + scrolledDown);

						// Switch back to normal title
						updateConversationTitleAsync(context);
					}

					public void onScrollChanged(FastScrollView fastScrollView,
							int x, int y, int oldx, int oldy, int percent,
							int item) {
						// Log.d("communicator", "@@@@ onScrollChanged:" +
						// percent);
						int realItem = conversationSize
								- conversationList.size() + item;
						updateConversationTitleAsync(context, realItem + " / "
								+ conversationSize);
					}

					public void onOversroll(boolean up) {
						// Switch back to normal title
						updateConversationTitleAsync(context);
					}

					public void onSnapScroll(int percent, boolean snappedDown,
							boolean snappedUp) {
						// DO NOTHING HERE
					}

				});

		fastScrollView
				.setOnSizeChangeListener(new FastScrollView.OnSizeChangeListener() {
					public void onSizeChange(int w, int h, int oldw, int oldh) {
						// Log.d("communicator", "######## SCROLL CHANGED X");
						// if the keyboard pops up and scrolledDown == true,
						// then scroll down manually!
						scrollDownAfterTypingFast(false);
					}
				});

		// The following code is necessary to FORCE further scrolling down if
		// the virtual keyboard
		// is brought up. Otherwise the top scrolled position is remaining but
		// this is uncomfortable!
		conversationRootView.getViewTreeObserver().addOnGlobalLayoutListener(
				new ViewTreeObserver.OnGlobalLayoutListener() {
					public void onGlobalLayout() {

						Rect r = new Rect();
						conversationRootView.getWindowVisibleDisplayFrame(r);
						// get screen height
						int screenHeight = conversationRootView.getRootView()
								.getHeight();
						// calculate the height difference
						int heightDifference = screenHeight - r.bottom - r.top;
						if (lastHeight == heightDifference) {
							return;
						}
						lastHeight = heightDifference;

						Log.d("communicator",
								"@@@@ onGlobalLayout() scoll down request "
										+ heightDifference + ", scrolledDown="
										+ scrolledDown);

						// THE FOLLOWING IS A FEATURE TO HIDE THE TITLE IF THE
						// MESSAGE TEXT GETS LONGER
						//
						// IT IS DEACTIVATED RIGHT NOW BECAUSE IT FEELS
						// UNCOMFORTABLE IF THE BACK BUTTON
						// AND TITLE BAR IS MISSING ESP. FOR SHORT MESSAGES. MAY
						// BE REACTIVATE THIS FEATURE
						// AT A LATER TIME BUT MAKE SURE TO BE PERFORMANT!
						//
						// if (messageText.length() == 0) {
						// messageTextEmptyHeight = messageText.getHeight();
						// }
						// if (isKeyboardVisible(conversationRootView)) {
						// if (!scrolledUp
						// && messageText.getHeight() > messageTextEmptyHeight)
						// {
						// // if scrolled up completely, we want to show it
						// // even with keyboard visible!
						// // also, if this is an empty or one-line message
						// // we want to see the titlebar! (e.g., directly
						// // after sending)
						// titleconversation.setVisibility(View.GONE);
						// }
						// } else {
						// titleconversation.setVisibility(View.VISIBLE);
						// }

						boolean showKeyboard = false;
						if (Utility
								.loadBooleanSetting(context,
										Setup.OPTION_QUICKTYPE,
										Setup.DEFAULT_QUICKTYPE)
								&& Utility.isOrientationLandscape(context)) {
							showKeyboard = true;
						}

						// if the keyboard pops up and scrolledDown == true,
						// then scroll down manually!
						// do not do this delayed because orientation changed!
						if (scrolledDown) {
							scrollDownNow(context, showKeyboard);
						}
					}
				});
	}

	// ------------------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onStart()
	 */
	@Override
	protected void onStart() {
		super.onStart();
		final Context context = this;

		// SET SENDSPINNER
		sendspinner = (Spinner) findViewById(R.id.sendspinner);

		// NOT DISPLAYED BY DATA ADAPTER!!!
		final String OPTION0 = "SELECT AN OPTIONS";

		final String OPTIONCHATON = "  Enable Chat Mode";
		final String OPTIONCHATOFF = "  Disable Chat Mode";
		final String OPTIONSMSON = "  Enable SMS Mode";
		final String OPTIONSMSOFF = "  Disable SMS Mode";
		final String OPTIONUSECMSG = "  Send Unsecure Message";
		final String OPTIONSECMSG = "  Send Secure Message";
		final String OPTIONUSECSMS = "  Send Unsecure SMS";
		final String OPTIONSECSMS = "  Send Secure SMS";

		String[] spinnerTitles = { OPTION0, OPTIONCHATON, OPTIONSMSON,
				OPTIONUSECSMS, OPTIONSECSMS, OPTIONUSECMSG };
		String[] spinnerTitlesChat = { OPTION0, OPTIONCHATOFF, OPTIONSMSON,
				OPTIONUSECSMS, OPTIONSECSMS, OPTIONUSECMSG };

		String[] spinnerTitlesSMS = { OPTION0, OPTIONCHATON, OPTIONSMSOFF,
				OPTIONUSECMSG, OPTIONSECMSG, OPTIONUSECSMS };
		String[] spinnerTitlesSMSChat = { OPTION0, OPTIONCHATOFF, OPTIONSMSOFF,
				OPTIONUSECMSG, OPTIONSECMSG, OPTIONUSECSMS };

		String[] spinnerTitlesNOSMS = { OPTION0, OPTIONCHATON, OPTIONSMSON,
				OPTIONUSECMSG };
		String[] spinnerTitlesNOSMSChat = { OPTION0, OPTIONCHATOFF,
				OPTIONSMSON, OPTIONUSECMSG };

		String[] spinnerTitlesONLYSMS = { OPTION0, OPTIONCHATON, OPTIONSECSMS };
		String[] spinnerTitlesONLYSMSChat = { OPTION0, OPTIONCHATOFF,
				OPTIONSECSMS };

		// Populate the spinner using a customized ArrayAdapter that hides the
		// first (dummy) entry
		final ArrayAdapter<String> dataAdapter = getMyDataAdapter(spinnerTitles);
		final ArrayAdapter<String> dataAdapterChat = getMyDataAdapter(spinnerTitlesChat);
		final ArrayAdapter<String> dataAdapterSMS = getMyDataAdapter(spinnerTitlesSMS);
		final ArrayAdapter<String> dataAdapterChatSMS = getMyDataAdapter(spinnerTitlesSMSChat);
		final ArrayAdapter<String> dataAdapterNOSMS = getMyDataAdapter(spinnerTitlesNOSMS);
		final ArrayAdapter<String> dataAdapterNOSMSChat = getMyDataAdapter(spinnerTitlesNOSMSChat);
		final ArrayAdapter<String> dataAdapterONLYSMS = getMyDataAdapter(spinnerTitlesONLYSMS);
		final ArrayAdapter<String> dataAdapterONLYSMSChat = getMyDataAdapter(spinnerTitlesONLYSMSChat);

		updateSendspinner(context, dataAdapter, dataAdapterChat,
				dataAdapterSMS, dataAdapterChatSMS, dataAdapterNOSMS,
				dataAdapterNOSMSChat, dataAdapterONLYSMS,
				dataAdapterONLYSMSChat);

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
						updateSendspinner(context, dataAdapter,
								dataAdapterChat, dataAdapterSMS,
								dataAdapterChatSMS, dataAdapterNOSMS,
								dataAdapterNOSMSChat, dataAdapterONLYSMS,
								dataAdapterONLYSMSChat);
						if (chatmodeOn) {
							Utility.showToastAsync(context,
									"Chat mode enabled.");
						} else {
							Utility.showToastAsync(context,
									"Chat mode disabled.");
						}
					} else if (option.equals(OPTIONSMSON)
							|| (option.equals(OPTIONSMSOFF))) {
						boolean smsmodeOn = Setup.isSMSModeOn(context, hostUid);
						boolean haveTelephoneNumber = Setup.havePhone(context,
								hostUid);
						if (!smsmodeOn && !haveTelephoneNumber) {
							if (Setup.isSMSOptionEnabled(context)) {
								inviteOtherUserToSMSMode(context);
							} else {
								inviteUserToSMSMode(context);
							}
						} else {
							// normal toggle mode
							smsmodeOn = !smsmodeOn;
							Utility.saveBooleanSetting(context,
									Setup.OPTION_SMSMODE + hostUid, smsmodeOn);
							updateSendspinner(context, dataAdapter,
									dataAdapterChat, dataAdapterSMS,
									dataAdapterChatSMS, dataAdapterNOSMS,
									dataAdapterNOSMSChat, dataAdapterONLYSMS,
									dataAdapterONLYSMSChat);
							if (smsmodeOn) {
								Utility.showToastAsync(
										context,
										"SMS mode for "
												+ Main.UID2Name(context,
														hostUid, false)
												+ " enabled.");
							} else {
								Utility.showToastAsync(
										context,
										"SMS mode for "
												+ Main.UID2Name(context,
														hostUid, false)
												+ " disabled.");
							}
						}
						// COPY AND PASTE FEATURES ARE CURRENTLY DISABLED FOR A
						// CLEANER UI
						//
						// } else if (option.equals(OPTIONCOPY)) {
						// String text = messageText.getText().toString();
						// Utility.copyToClipboard(context, text);
						// messageText.selectAll();
						// } else if (option.equals(OPTIONPASTE)) {
						// String text = Utility.pasteFromClipboard(context);
						// int i = messageText.getSelectionStart();
						// if (text != null) {
						// String prevText = messageText.getText().toString();
						// if (i < 0) {
						// // default fallback is concatenation
						// messageText.setText(prevText + text);
						// } else {
						// // otherwise try to fill in the text
						// messageText.setText(prevText.substring(0, i)
						// + text + prevText.substring(i));
						// }
						// messageText.setSelection(text.length()
						// + prevText.length());
						// }
					} else if (option.equals(OPTIONSECSMS)) {
						if (hostUid >= 0) {
							sendMessageOrPrompt(context, DB.TRANSPORT_SMS, true);
						} else {
							promptInfo(
									context,
									"No Registered User",
									"In order to send secure encrypted SMS or messages, your communication partner needs to be registered.");
						}
					} else if (option.equals(OPTIONSECMSG)) {
						if (Setup.haveKey(context, hostUid)) {
							sendMessageOrPrompt(context, DB.TRANSPORT_INTERNET,
									true);
						} else {
							promptInfo(
									context,
									"No Encryption Possible",
									"In order to send secure encrypted messages or SMS, your communication partner needs to enable encryption.");

						}
					} else if (option.equals(OPTIONUSECMSG)) {
						sendMessageOrPrompt(context, DB.TRANSPORT_INTERNET,
								false);
					} else if (option.equals(OPTIONUSECSMS)) {
						sendMessageOrPrompt(context, DB.TRANSPORT_SMS, false);
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

	// ------------------------------------------------------------------------

	/**
	 * Gets the host uid of the current conversation.
	 * 
	 * @return the host uid
	 */
	public static int getHostUid() {
		return hostUid;
	}

	// ------------------------------------------------------------------------

	/**
	 * Update send spinner (context menu for the send button).
	 * 
	 * @param context
	 *            the context
	 * @param dataAdapter
	 *            the data adapter
	 * @param dataAdapterChat
	 *            the data adapter chat
	 * @param dataAdapterSMS
	 *            the data adapter sms
	 * @param dataAdapterChatSMS
	 *            the data adapter chat sms
	 * @param dataAdapterNOSMS
	 *            the data adapter nosms
	 * @param dataAdapterNOSMSChat
	 *            the data adapter nosms chat
	 * @param dataAdapterONLYSMS
	 *            the data adapter onlysms
	 * @param dataAdapterONLYSMSChat
	 *            the data adapter onlysms chat
	 */
	private void updateSendspinner(Context context,
			ArrayAdapter<String> dataAdapter,
			ArrayAdapter<String> dataAdapterChat,
			ArrayAdapter<String> dataAdapterSMS,
			ArrayAdapter<String> dataAdapterChatSMS,
			ArrayAdapter<String> dataAdapterNOSMS,
			ArrayAdapter<String> dataAdapterNOSMSChat,
			ArrayAdapter<String> dataAdapterONLYSMS,
			ArrayAdapter<String> dataAdapterONLYSMSChat) {
		boolean chatmodeOn = Utility.loadBooleanSetting(context,
				Setup.OPTION_CHATMODE, Setup.DEFAULT_CHATMODE);
		boolean smsmodeOn = Setup.isSMSModeOn(context, hostUid);
		boolean havephonenumber = Setup.havePhone(context, hostUid);
		boolean onlySMS = hostUid < 0;

		if (onlySMS) {
			// only SMS mode available
			if (chatmodeOn) {
				sendspinner.setAdapter(dataAdapterONLYSMSChat);
			} else {
				sendspinner.setAdapter(dataAdapterONLYSMS);
			}
		} else if (!havephonenumber) {
			// no SMS mode available
			if (chatmodeOn) {
				sendspinner.setAdapter(dataAdapterNOSMSChat);
			} else {
				sendspinner.setAdapter(dataAdapterNOSMS);
			}
		} else {
			// sms mode available
			if (smsmodeOn) {
				// sms mode on
				if (chatmodeOn) {
					sendspinner.setAdapter(dataAdapterChatSMS);
				} else {
					sendspinner.setAdapter(dataAdapterSMS);
				}
			} else {
				// normal : sms mode off
				if (chatmodeOn) {
					sendspinner.setAdapter(dataAdapterChat);
				} else {
					sendspinner.setAdapter(dataAdapter);
				}
			}
		}
		updateSendButtonImage(context);
	}

	// -------------------------------

	/**
	 * Gets the my data adapter for the send spinner.
	 * 
	 * @param titles
	 *            the titles
	 * @return the my data adapter
	 */
	ArrayAdapter<String> getMyDataAdapter(String[] titles) {
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
	 * Invite other user to enable his SMS mode.
	 * 
	 * @param context
	 *            the context
	 */
	private void inviteOtherUserToSMSMode(final Context context) {
		// Possibly read other ones telephone number
		// at this point
		Communicator.updatePhonesFromServer(context, Main.loadUIDList(context),
				true);

		final String titleMessage = "Enable SMS";
		String partner = Main.UID2Name(context, hostUid, false);
		final String textMessage = "Delphino Cryptocator also allows to send/receive secure encrypted SMS.\n\nTo be able to use this option both communication partners have to turn this feature on.\n\nCurrently '"
				+ partner
				+ "' seems not to have turned on this feature. If you know '"
				+ partner
				+ "' has turned it on, try to refresh your userlist manually.\n\nAlternative: You can long press on the name '"
				+ partner
				+ "' in the userlist and add her/his phone number manually. This will just enable you to send him SMS.";
		new MessageAlertDialog(context, titleMessage, textMessage, " Ok ",
				null, null, new MessageAlertDialog.OnSelectionListener() {
					public void selected(int button, boolean cancel) {
					}
				}).show();
	}

	// ------------------------------------------------------------------------

	/**
	 * Invite current user to sms mode.
	 * 
	 * @param context
	 *            the context
	 */
	private void inviteUserToSMSMode(final Context context) {
		try {
			final String titleMessage = "Enable SMS";
			final String textMessage = "Delphino Cryptocator also allows to send/receive secure encrypted SMS.\n\nFor this,"
					+ " your phone number and your userlist must be stored at the server. Furthermore, to be able to use this"
					+ " option both communication partners have to turn this feature on.\n\nDo you want to enable the secure"
					+ " SMS possibility?";
			new MessageAlertDialog(context, titleMessage, textMessage, " Yes ",
					" Account ", " Cancel ",
					new MessageAlertDialog.OnSelectionListener() {
						public void selected(int button, boolean cancel) {
							if (!cancel) {
								if (button == 0) {
									// Turn on if possible
									String phone = Utility
											.getPhoneNumber(context);
									if (phone != null && phone.length() > 0) {
										Setup.updateSMSOption(context, true);
										Setup.backup(context, true, false);
										// Possibly read other ones telephone
										// number at this poiint
										Communicator.updatePhonesFromServer(
												context,
												Main.loadUIDList(context), true);
									} else {
										Utility.showToastAsync(
												context,
												"Cannot automatically read required phone number. Please enable SMS option"
														+ " manually in account settings after login/validate!");
										// Go to account settings
										Main.startAccount(context);
									}
								} else if (button == 1) {
									// Go to account settings
									Main.startAccount(context);
								}
							}
						}
					}).show();
		} catch (Exception e) {
			// ignore
		}
	}

	// ------------------------------------------------------------------------
	// ------------------------------------------------------------------------

	/**
	 * Send message dispatch method is only called from sendMessageOrPrompt().
	 * It should not be called directly.
	 * 
	 * @param context
	 *            the context
	 * @param text
	 *            the text
	 * @param encrypted
	 *            the encrypted
	 * @param transport
	 *            the transport
	 */
	private void sendMessage(Context context, int transport, boolean encrypted,
			String text) {
		if (transport == DB.TRANSPORT_INTERNET) {
			if (encrypted) {
				sendSecureMsg(context, text);
			} else {
				sendUnsecureMsg(context, text);
			}
		} else {
			if (encrypted) {
				sendSecureSms(context, text);
			} else {
				sendUnsecureSms(context, text);
			}
		}
	}

	// ------------------------------------------------------------------------

	/**
	 * Send unsecure sms.
	 * 
	 * @param context
	 *            the context
	 */
	private void sendUnsecureSms(Context context, String text) {
		if (text.length() > 0) {
			String phone = Setup.getPhone(context, hostUid);
			if (phone != null && phone.length() > 0) {
				if (DB.addSendMessage(context, hostUid, text, false,
						DB.TRANSPORT_SMS, false, DB.PRIORITY_MESSAGE)) {
					updateConversationlist(context);
					Communicator.sendNewNextMessageAsync(context,
							DB.TRANSPORT_SMS);
					messageText.setText("");
				}
			}
		}
	}

	// ------------------------------------------------------------------------

	/**
	 * Send secure sms.
	 * 
	 * @param context
	 *            the context
	 */
	private void sendSecureSms(Context context, String text) {
		if (text.length() > 0) {
			boolean encrypted = Setup.encryptedSentPossible(context, hostUid);
			String phone = Setup.getPhone(context, hostUid);
			if (phone != null && phone.length() > 0) {
				if (DB.addSendMessage(context, hostUid, text, encrypted,
						DB.TRANSPORT_SMS, false, DB.PRIORITY_MESSAGE)) {
					updateConversationlist(context);
					Communicator.sendNewNextMessageAsync(context,
							DB.TRANSPORT_SMS);
					messageText.setText("");
				}
			}
		}
	}

	// ------------------------------------------------------------------------

	/**
	 * Send secure msg.
	 * 
	 * @param context
	 *            the context
	 */
	private void sendSecureMsg(Context context, String text) {
		if (text.length() > 0) {
			text = possiblyRemoveImageAttachments(context, text);
			boolean encrypted = Setup.encryptedSentPossible(context, hostUid);
			if (DB.addSendMessage(context, hostUid, text, encrypted,
					DB.TRANSPORT_INTERNET, false, DB.PRIORITY_MESSAGE)) {
				updateConversationlist(context);
				Communicator.sendNewNextMessageAsync(context,
						DB.TRANSPORT_INTERNET);
				messageText.setText("");
			}
		}
	}

	// ------------------------------------------------------------------------

	/**
	 * Send unsecure msg.
	 * 
	 * @param context
	 *            the context
	 */
	private void sendUnsecureMsg(Context context, String text) {
		if (text.length() > 0) {
			text = possiblyRemoveImageAttachments(context, text);
			boolean encrypted = false;
			if (DB.addSendMessage(context, hostUid, text, encrypted,
					DB.TRANSPORT_INTERNET, false, DB.PRIORITY_MESSAGE)) {
				updateConversationlist(context);
				Communicator.sendNewNextMessageAsync(context,
						DB.TRANSPORT_INTERNET);
			}
			messageText.setText("");
		}
	}

	// ------------------------------------------------------------------------
	// ------------------------------------------------------------------------

	/**
	 * Sets the title of the conversation custom title bar.
	 * 
	 * @param title
	 *            the new title
	 */
	public void setTitle(String title) {
		TextView titletext = (TextView) findViewById(R.id.titletext);
		titletext.setText(title);
	}

	// -------------------------------------------------------------------------
	// -------------------------------------------------------------------------

	/**
	 * Checks if is SMS mode available so we and our partner have enabled SMS
	 * mode by providing phone numbers.
	 * 
	 * @param context
	 *            the context
	 * @return true, if is SMS mode available
	 */
	private boolean isSMSModeAvailable(Context context) {
		boolean smsOptionOn = Setup.isSMSOptionEnabled(context);
		boolean haveTelephoneNumber = Setup.havePhone(context, hostUid);
		return (haveTelephoneNumber && smsOptionOn);
	}

	// -------------------------------------------------------------------------

	/**
	 * Checks if is SMS mode available (because we have a telephone number) and
	 * if the mode is and on.
	 * 
	 * @param context
	 *            the context
	 * @return true, if is SMS mode available and on
	 */
	private boolean isSMSModeAvailableAndOn(Context context) {
		boolean smsmodeOn = Setup.isSMSModeOn(context, hostUid);
		return (isSMSModeAvailable(context) && smsmodeOn);
	}

	// -------------------------------------------------------------------------

	/**
	 * Update send button image according to the possible enabled SMS mode and
	 * according to the communication partner. For an unregistered user there
	 * will only be SMS sending available.
	 * 
	 * @param context
	 *            the context
	 */
	private void updateSendButtonImage(Context context) {
		if (hostUid < 0) {
			sendbutton.setImageResource(R.drawable.sendsms);
			sendbutton.initializePressImageResource(R.drawable.sendsms, false);
			// sendbutton.setImageResource(R.drawable.sendsms);
		} else if (isSMSModeAvailableAndOn(context)) {
			if (Setup.isEncryptionAvailable(context, hostUid)) {
				sendbutton.setImageResource(R.drawable.sendsmslock);
				sendbutton.initializePressImageResource(R.drawable.sendsmslock,
						false);
			} else {
				sendbutton.setImageResource(R.drawable.sendsms);
				sendbutton.initializePressImageResource(R.drawable.sendsms,
						false);
			}
		} else if (Setup.isEncryptionAvailable(context, hostUid)) {
			sendbutton.setImageResource(R.drawable.sendlock);
			sendbutton.initializePressImageResource(R.drawable.sendlock, false);
		} else {
			sendbutton.setImageResource(R.drawable.send);
			sendbutton.initializePressImageResource(R.drawable.send, false);
		}
	}

	// -------------------------------------------------------------------------
	// -------------------------------------------------------------------------

	// "If the process is killed then all static variables will be reinitialized to their default values."

	/** The instance. */
	private static Conversation instance = null;

	/** The visible. */
	private static boolean visible = false;

	/**
	 * The last key stroke. Only written from Conversation and
	 * ConversationCompose. This allows basically to detect the type or fasttype
	 * mode where NoHang² skips any heavier computation to make the UI feel
	 * fast.
	 */
	public static long lastKeyStroke = 0;

	/**
	 * Checks if is typing. We want to skip background activity if the user is
	 * typing
	 * 
	 * @return true, if is typing
	 */
	public static boolean isTyping() {
		// If the conversation is NOT visible the user cannot type fast!
		return isVisible()
				&& !(lastKeyStroke == 0)
				&& ((System.currentTimeMillis() - lastKeyStroke) < Setup.TYPING_TIMEOUT_BEFORE_BACKGROUND_ACTIVITY);
	}

	/**
	 * Checks if is typing fast. We want to defer any heavier (also UI)
	 * computation like scrolling.
	 * 
	 * @return true, if is typing fast
	 */
	public static boolean isTypingFast() {
		// If the conversation is NOT visible the user cannot type fast!
		return isVisible()
				&& !(lastKeyStroke == 0)
				&& ((System.currentTimeMillis() - lastKeyStroke) < Setup.TYPING_TIMEOUT_BEFORE_UI_ACTIVITY);
	}

	/**
	 * Gets the single instance of Conversation.
	 * 
	 * @return single instance of Conversation
	 */
	public static Conversation getInstance() {
		return instance;
	}

	/**
	 * Returns true if the activity is visible.
	 * 
	 * @return true, if is visible
	 */
	public static boolean isVisible() {
		return (visible && instance != null);
	}

	/**
	 * Checks if this activity is alive and UI elements like status icons should
	 * be updated, e.g., when incoming messages arrive.
	 * 
	 * @return true, if is alive
	 */
	public static boolean isAlive() {
		return (instance != null && Conversation.hostUid != -1);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	public void onDestroy() {
		Conversation.visible = false;
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
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onStop()
	 */
	@Override
	public void onStop() {
		Conversation.visible = false;
		// If chat is not empty, save it as draft
		String msg = messageText.getText().toString();
		if (msg != null && msg.trim().length() > 0) {
			String draft = Utility.loadStringSetting(this, "cachedraft"
					+ hostUid, "");
			if (!draft.equals(msg)) {
				// only if message changed
				Utility.saveStringSetting(this, "cachedraft" + hostUid, msg);
				Utility.showToastShortAsync(this, "Draft saved.");
			}
		}
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
		Conversation.visible = false;
		super.onPause();
		// Necessary for the following situation:
		// if scrolled to somewhere and changing focused activity, the
		// scrollview would automatically try to
		// scroll up! this prevents!
		// Log.d("communicator", "@@@@ onPause() LOCK POSITION ");
		fastScrollView.lockPosition();
	}

	// --------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onResume()
	 */
	@Override
	public void onResume() {
		super.onResume();
		if (!Conversation.isAlive()) {
			// this class instance was lost, close it
			Conversation.visible = false;
			this.finish();
		} else {
			Conversation.visible = true;
			// Reset error claims
			Setup.setErrorUpdateInterval(this, false);
			Scheduler.reschedule(this, false, false, true);

			conversationSize = DB.getConversationSize(this, hostUid, true);

			// Reset the new message counter
			Communicator.setNotificationCount(this, hostUid, true);
			// set all messages (internally as read)

			// Flag all messages (internally as read)
			DB.updateOwnMessageRead(this, hostUid);
			if (Conversation.isVisible()) {
				Communicator.sendReadConfirmation(this, hostUid);
			}

			// Cancel possible system notifications for this hostuid. Do this
			// only if the screen is also UNLOCKED otherwise the user might be
			// interested in further seeing the notification of an arrived
			// message!
			if (Utility.loadBooleanSetting(this, Setup.OPTION_NOTIFICATION,
					Setup.DEFAULT_NOTIFICATION)
					&& !Utility.isScreenLocked(this)) {
				Communicator.cancelNotification(this, hostUid);
			}

			// If (Utility.loadBooleanSetting(this, Setup.OPTION_NOTIFICATION,
			// Setup.DEFAULT_NOTIFICATION) && Utility.isScreenLocked(this)) {
			// onResume of the activity will be called when ACTION_SCREEN_ON
			// is fired. Create a handler and wait for ACTION_USER_PRESENT.
			// When it is fired, implement what you want for your activity.
			//
			// WE NEED TO WAIT UNTIL THE SCREEN IS UNLOCKED BEFORE WE
			// CANCEL THE NOTIFICATION
			// =>>> The UserPresentReceiver listens to USER_PRESENT, it will
			// clear the notification in this case!
			// }

			// ALWAYS (RE)SET KEYBOARD TO INVISIBLE (also when orientation
			// changes)!!!
			Utility.hideKeyboard(this);
			Utility.hideKeyboardExplicit(messageText);

			scrollOnCreateOrResume(this);
		}
	}

	// --------------------------------------

	/**
	 * Scroll on create or resume.
	 * 
	 * @param context
	 *            the context
	 */
	private void scrollOnCreateOrResume(Context context) {
		// Only scroll for the first time, it is annoying to scroll when
		// changing orientation, we want to prevent this!

		if (fastScrollView.isLocked()) {
			// Log.d("communicator", "@@@@ SCROLL RESTORE #0");
			fastScrollView.restoreLockedPosition();
		} else if (!hasScrolled) {
			hasScrolled = true;
			// Log.d("communicator",
			// "@@@@ onCreate() 1: not scrolled  down before");
			fastScrollView.postDelayed(new Runnable() {
				public void run() {
					Log.d("communicator", "@@@@ SCROLL DOWN #1");
					isKeyboardVisible(conversationRootView);
					fastScrollView.scrollDown();
					// foceScrollDown();
					scrolledDown = true;
					fastScrollView.setScrollBackground(R.color.gray);
				}
			}, 200);
		} else if (scrolledDown) {
			// Log.d("communicator", "@@@@ onCreate() 2: scrolled down lock");
			// scroll to restore position
			fastScrollView.postDelayed(new Runnable() {
				public void run() {
					// Log.d("communicator", "@@@@ SCROLL DOWN #2");
					fastScrollView.scrollDown();
				}
			}, 200);
		} else if (scrolledUp) {
			// Log.d("communicator", "@@@@ onCreate() 3: scrolled up lock");
			// Scroll to restore position
			fastScrollView.postDelayed(new Runnable() {
				public void run() {
					Log.d("communicator", "@@@@ SCROLL UP #3");
					fastScrollView.scrollUp();
				}
			}, 200);
		} else if (scrollItem > -1) {
			// Log.d("communicator",
			// "@@@@ onCreate() 4: scroll to specific position");
			// Scroll to restore position
			fastScrollView.postDelayed(new Runnable() {
				public void run() {
					Log.d("communicator", "@@@@ SCROLL ITEM #4:" + scrollItem);
					fastScrollView.scrollToItem(scrollItem + 1);
				}
			}, 200);
		}
	}

	// -------------------------------------------------------------------------
	// -------------------------------------------------------------------------

	/**
	 * Force scroll down.
	 * 
	 * @param keyboardVisible
	 *            the keyboard visible
	 */
	private void foceScrollDown(boolean keyboardVisible, int delay) {
		Log.d("communicator", "@@@@ foceScrollDown(alsoSetTextFocus? "
				+ keyboardVisible + ")");
		fastScrollView.postDelayed(new Runnable() {
			public void run() {
				fastScrollView.scrollDown();
			}
		}, 100 + delay);

		Log.d("communicator", "@@@@ foceScrollDown() -> keyboardVisible="
				+ keyboardVisible);

		// If keyboard is visible, then set cursor to text field!
		if (keyboardVisible) {
			// Fake a keyboard entry for convenience: If the user immediately
			// wants to start typing - he just can and will not be interrupted!
			// :-)
			lastKeyStroke = DB.getTimestamp();
			messageText.postDelayed(new Runnable() {
				public void run() {
					// Fake a keyboard entry for convenience
					lastKeyStroke = DB.getTimestamp();
					messageText.requestFocus();
					Utility.showKeyboardExplicit(messageText);
					messageText.requestFocus();
				}
			}, 200 + delay);
		}
	}

	// -------------------------------------------------------------------------
	// -------------------------------------------------------------------------

	/**
	 * The active scroller. Each scroller increments this counter and will not
	 * resume if he has not the highest number any more => this means another
	 * scroller has arrived which will do the job of us.
	 */
	private int activeScoller = 0;

	/**
	 * Scroll down after typing fast.
	 * 
	 * @param showKeyboard
	 *            the show keyboard
	 */
	private void scrollDownAfterTypingFast(boolean showKeyboard) {
		if (!scrolledDown && hasScrolled) {
			Log.d("communicator",
					"@@@@ scollDownAfterTypingFast() => return (scrolledDown="
							+ scrolledDown + ")");
			return;
		}
		// -1 means we do not have a number yet!
		// note that this is not thread safe so potentially two threads may get
		// the same
		// number. this is OK for us as this only tries to reduce the number of
		// backward threads
		// when typing fast & long texts. The worst case scenario is only that
		// we try to scroll
		// more than once... but thats OK because we actually do not scroll if
		// we already are there!
		scrollDownAfterTypingFast(showKeyboard, -1);
	}

	/**
	 * Scroll down after typing fast.
	 * 
	 * @param showKeyboard
	 *            the show keyboard
	 * @param scrollNumber
	 *            the scroll number
	 */
	private void scrollDownAfterTypingFast(final boolean showKeyboard,
			final int scrollNumber) {
		// Log.d("communicator", "@@@@ scollDownAfterTypingFast([" +
		// scrollNumber
		// + "], showKeyboard=" + showKeyboard + ") ENTRY ");
		final Context context = this;
		if (isTypingFast()) {
			// Log.d("communicator", "@@@@ scollDownAfterTypingFast(["
			// + scrollNumber + "], showKeyboard=" + showKeyboard
			// + ") 1 => isTypingFast, retry ");
			fastScrollView.postDelayed(new Runnable() {
				public void run() {
					isKeyboardVisible(conversationRootView);
					if (!isTypingFast()) {
						// here we scroll if not already scrolled down!
						foceScrollDown(keyboardVisible || showKeyboard, 0);
					} else {
						int newOrOldScrollNumber = scrollNumber;
						if (scrollNumber < 0) {
							// take a new number
							activeScoller++;
							newOrOldScrollNumber = activeScoller;
							// Log.d("communicator",
							// "@@@@ scollDownAfterTypingFast(["
							// + scrollNumber
							// + "]) 1A => take new number "
							// + newOrOldScrollNumber
							// + " and wait...");
						} else {
							// test if there is someone "behind" us how will do
							// the work, so we can
							// stop our recursion...
							if (activeScoller > scrollNumber) {
								// Log.d("communicator",
								// "@@@@ scollDownAfterTypingFast(["
								// + scrollNumber
								// + "]) 1A_1 => we have an old number = "
								// + newOrOldScrollNumber + " < "
								// + activeScoller + " => QUIT");
								return; // SOMEONE ELSE WILL SCROLL FOR US...
							}
							// Log.d("communicator",
							// "@@@@ scollDownAfterTypingFast(["
							// + scrollNumber
							// + "]) 1A_2 => we have the highest number "
							// + newOrOldScrollNumber
							// + " ... further waiting");
						}
						// WAIT RECUSIVELY
						scrollDownAfterTypingFast(showKeyboard,
								newOrOldScrollNumber);
					}
				}
			}, 100 + Setup.TYPING_TIMEOUT_BEFORE_UI_ACTIVITY);
			// Ensure that we scroll after this time at least if
			// the user rests
			// for some time..
		} else if (!isTypingFast()) {
			scrollDownNow(context, showKeyboard);
		}
	}

	// -------------------------------------------------------------------------

	/**
	 * Scroll down now.
	 * 
	 * @param context
	 *            the context
	 * @param showKeyboard
	 *            the show keyboard
	 * @param delay
	 *            the delay
	 */
	public void scrollDownNow(final Context context, final boolean showKeyboard) {
		scrollDownSoon(context, showKeyboard, 0);
	}

	// -------------------------------------------------------------------------

	/**
	 * Scroll down soon or now - do not wait for fast typing to end, we should
	 * be sure that the user does not fast type!
	 * 
	 * @param context
	 *            the context
	 * @param showKeyboard
	 *            the show keyboard
	 */
	public void scrollDownSoon(final Context context,
			final boolean showKeyboard, final int delay) {
		// we should update the scroll view height here because it was
		// blocked/skipped during fast typing!
		fastScrollView.potentiallyRefreshState();
		// Otherwise we can scroll immediately if the user already rests!
		// and is not typing
		fastScrollView.postDelayed(new Runnable() {
			public void run() {
				updateConversationTitleAsync(context);
				isKeyboardVisible(conversationRootView);
				foceScrollDown(keyboardVisible || showKeyboard, delay);
			}
		}, 100);
	}

	// -------------------------------------------------------------------------

	/**
	 * Rebuild conversationlist.
	 * 
	 * @param context
	 *            the context
	 */
	private void rebuildConversationlist(final Context context) {
		fastScrollView.clearChilds();
		resetMapping();

		loadConversationList(context, hostUid, maxScrollMessageItems);
		try {
			// For every item in the conversation list we build a balloon
			// which is also filling the mapping (that we just cleaned)
			for (ConversationItem conversationItem : conversationList) {
				View newView = addConversationLine(context, conversationItem);
				if (newView != null) {
					fastScrollView.addChild(newView);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		if (Conversation.isVisible()) {
			Communicator.sendReadConfirmation(context, hostUid);
		}

		updateConversationTitle(context);
	}

	// -------------------------------------------------------------------------

	/**
	 * Show titlebar async from non UI thread.
	 * 
	 * @param context
	 *            the context
	 */
	public void showTitlebarAsync(final Context context) {
		final Handler mUIHandler = new Handler(Looper.getMainLooper());
		mUIHandler.post(new Thread() {
			@Override
			public void run() {
				super.run();
				titleconversation.setVisibility(View.VISIBLE);
			}
		});
	}

	// -------------------------------------------------------------------------

	/**
	 * Update conversationlist async from non UI thread.
	 * 
	 * @param context
	 *            the context
	 */
	public static void updateConversationlistAsync(final Context context) {
		final Handler mUIHandler = new Handler(Looper.getMainLooper());
		mUIHandler.post(new Thread() {
			@Override
			public void run() {
				super.run();
				if (Conversation.isAlive()) {
					Conversation.getInstance().updateConversationlist(context);
				}
			}
		});
	}

	// -------------------------------------------------------------------------

	/**
	 * Update conversationlist.
	 * 
	 * @param context
	 *            the context
	 */
	public void updateConversationlist(final Context context) {
		loadConversationList(context, hostUid, maxScrollMessageItems);
		try {
			if (!Conversation.scrolledDown) {
				fastScrollView.lockPosition();
			}
			if (conversationListDiff.size() > 0) {
				for (ConversationItem conversationItem : conversationListDiff) {
					View newView = addConversationLine(context,
							conversationItem);
					if (newView != null) {
						fastScrollView.addChild(newView);
					}
				}
			}
			conversationSize += conversationListDiff.size();

			if (!Conversation.scrolledDown) {
				fastScrollView.restoreLockedPosition();
			} else {
				scrollDownAfterTypingFast(false);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (Conversation.isVisible()) {
			Communicator.sendReadConfirmation(this, hostUid);
		}
		updateConversationTitle(context);
	}

	// ------------------------------------------------------------------------

	/**
	 * Adds the mapping. Note the convention: Use local id instead as a
	 * placeholder if a mid not yet exists (before the message is sent or if it
	 * is an SMS). Local ids by convertion are negative numbers.
	 * 
	 * @param mid
	 *            the mid
	 * @param localid
	 *            the localid
	 * @param sent
	 *            the sent
	 * @param received
	 *            the received
	 * @param read
	 *            the read
	 * @param text
	 *            the text
	 * @param oneline
	 *            the oneline
	 * @param speech
	 *            the speech
	 */
	private void addMapping(int mid, int localid, ImageView sent,
			ImageView received, ImageView read, EditText text,
			LinearLayout oneline, ImageView speech) {
		Mapping mappingItem = new Mapping();
		mappingItem.read = read;
		mappingItem.received = received;
		mappingItem.sent = sent;
		mappingItem.text = text;
		mappingItem.oneline = oneline;
		mappingItem.speech = speech;
		int insertId = mid;
		if (insertId == -1) {
			// Convention: use local id instead as a placeholder
			insertId = -1 * localid;
		}
		// Log.d("communicator",
		// "MAPPING INSERT "
		// + insertId);
		mapping.put(insertId, mappingItem);
	}

	/**
	 * Reset mapping.
	 */
	private void resetMapping() {
		mapping.clear();
	}

	/**
	 * Gets the mapping. If mid is negative by convention we look for a local id
	 * mapped element because the mid will still be -1 (eg for not-yet-sent
	 * messages or for SMS).
	 * 
	 * @param context
	 *            the context
	 * @param mid
	 *            the mid
	 * @return the mapping
	 */
	public Mapping getMapping(Context context, int mid) {
		// Log.d("communicator",
		// "MAPPING REQUEST "
		// + mid);
		Mapping mappingItem = mapping.get(mid);
		if (mappingItem != null && mid > 0) {
			// if we already found a valid mid, then return the mappingItem
			return mappingItem;
		} else if (mid < 0) {
			// FOR SMS MESSAGES: mid == local id!!!
			int negativeLocalId = mid;
			mappingItem = mapping.get(negativeLocalId);
			if (mappingItem != null) {
				// if we already found a valid mid, then return the mappingItem
				return mappingItem;
			}
		}
		// otherwise lookup the localid in the DB and update it!
		// in the not-sent-case there is no mid yet, so the localid is used in
		// the mapping
		// it will be available under the NEGATIVE localid to prevent conflicts
		// with other (always positive) mids
		int localId = DB.getHostLocalIdByMid(context, mid, hostUid);
		if (localId >= 0) {
			// found a localId to the (now) available mid ... so update the
			// mapping to this mid!
			int negativeLocalId = localId * -1;
			mappingItem = mapping.get(negativeLocalId);
			// ATTENTION: for SMS-messages, there will never be a mid!!! DO NOT
			// UPDATE THE MAPPING IN THIS CASE!
			if (mid > 0) {
				// update in the real mapping for later usage
				mapping.put(mid, mappingItem);
			}
		}
		return mappingItem;
	}

	// -------------------------------------------------------------------------

	/**
	 * Sets the sent status for a displayed message.
	 * 
	 * @param context
	 *            the context
	 * @param mid
	 *            the mid
	 */
	public static void setSent(Context context, int mid) {
		Conversation conversation = Conversation.getInstance();
		if (Conversation.isAlive()) {
			Mapping mappingItem = conversation.getMapping(context, mid);
			if (mappingItem != null) {
				if (mappingItem.received.getVisibility() != View.VISIBLE) {
					// if not already received...
					mappingItem.sent.setVisibility(View.VISIBLE);
					mappingItem.received.setVisibility(View.GONE);
					mappingItem.read.setVisibility(View.GONE);
				}
			}
		}
	}

	// -------------------------------------------------------------------------

	/**
	 * Sets the withdraw status for a displayed message.
	 * 
	 * @param context
	 *            the context
	 * @param mid
	 *            the mid
	 */
	public static void setWithdrawInConversation(Context context, int mid) {
		Conversation conversation = Conversation.getInstance();
		if (Conversation.isAlive()) {
			Mapping mappingItem = conversation.getMapping(context, mid);
			if (mappingItem != null) {
				mappingItem.text.setText(DB.WITHDRAWNTEXT);
			}
		}
	}

	// -------------------------------------------------------------------------

	/**
	 * Sets the received status for a displayed message.
	 * 
	 * @param context
	 *            the context
	 * @param mid
	 *            the mid
	 */
	public static void setReceived(Context context, int mid) {
		Conversation conversation = Conversation.getInstance();
		if (Conversation.isAlive()) {
			Mapping mappingItem = conversation.getMapping(context, mid);
			if (mappingItem != null) {
				if (mappingItem.read.getVisibility() != View.VISIBLE) {
					// if not already read...
					mappingItem.sent.setVisibility(View.GONE);
					mappingItem.received.setVisibility(View.VISIBLE);
					mappingItem.read.setVisibility(View.GONE);
				}
			}
		}
	}

	// -------------------------------------------------------------------------

	/**
	 * Sets the failed status async from non UI thread.
	 * 
	 * @param context
	 *            the context
	 * @param localid
	 *            the localid
	 */
	public static void setFailedAsync(final Context context, final int localid) {
		final Handler mUIHandler = new Handler(Looper.getMainLooper());
		mUIHandler.post(new Thread() {
			@Override
			public void run() {
				super.run();
				setFailed(context, localid);
			}
		});
	}

	/**
	 * Sets the failed status for a displayed message.
	 * 
	 * @param context
	 *            the context
	 * @param localid
	 *            the localid
	 */
	public static void setFailed(Context context, int localid) {
		Conversation conversation = Conversation.getInstance();
		if (Conversation.isAlive()) {
			Mapping mappingItem = conversation
					.getMapping(context, -1 * localid);
			if (mappingItem != null) {
				mappingItem.oneline.setBackgroundColor(FAILEDCOLOR);
				mappingItem.speech.setImageResource(R.drawable.speechmefailed);
			}
		}
	}

	// -------------------------------------------------------------------------

	/**
	 * Sets the read status for a displayed message.
	 * 
	 * @param context
	 *            the context
	 * @param mid
	 *            the mid
	 */
	public static void setRead(Context context, int mid) {
		if (!Utility.loadBooleanSetting(context, Setup.OPTION_NOREAD,
				Setup.DEFAULT_NOREAD)) {
			Conversation conversation = Conversation.getInstance();
			if (Conversation.isAlive()) {
				Mapping mappingItem = conversation.getMapping(context, mid);
				if (mappingItem != null) {
					mappingItem.sent.setVisibility(View.GONE);
					mappingItem.received.setVisibility(View.GONE);
					mappingItem.read.setVisibility(View.VISIBLE);
				}
			}
		}
	}

	// ------------------------------------------------------------------------

	/**
	 * Reset values of this activity. This is usually called from outside if the
	 * activity is initialized to be used with a specific hostUid (= the
	 * conversation partner).
	 * 
	 * @param hostUid
	 *            the host uid
	 */
	public static void resetValues(int hostUid) {
		Conversation.hostUid = hostUid;
		Conversation.maxScrollMessageItems = Setup.MAX_SHOW_CONVERSATION_MESSAGES;
		hasScrolled = false;
		scrollItem = -1;
	}

	// ------------------------------------------------------------------------

	/**
	 * Adds the conversation line. Here is where the mapping is created and
	 * where all elements (e.g. status icons) get created for a specific
	 * conversation item.
	 * 
	 * @param context
	 *            the context
	 * @param conversationItem
	 *            the conversation item
	 * @return the view
	 */
	private View addConversationLine(final Context context,
			final ConversationItem conversationItem) {

		// Do not show system messages or empty messages
		if (conversationItem.text == null
				|| conversationItem.text.length() == 0) {
			return null;
		}

		View conversationlistitem = null;
		ImageSmileyEditText conversationText = null;

		// Inflate other XMLs for me or for my conversation partner
		if (!conversationItem.me(context)) {
			conversationlistitem = inflater.inflate(R.layout.conversationitem,
					null);
			conversationText = (ImageSmileyEditText) conversationlistitem
					.findViewById(R.id.conversationtext);
			// Rounded corners
			LinearLayout oneline = (LinearLayout) conversationlistitem
					.findViewById(R.id.oneline);
			oneline.setBackgroundResource(R.drawable.rounded_corners);

			ImageView speech = (ImageView) conversationlistitem
					.findViewById(R.id.msgspeech);

			// Create a mapping for later async update
			addMapping(conversationItem.mid, conversationItem.localid, null,
					null, null, conversationText, oneline, speech);
		} else {
			conversationlistitem = inflater.inflate(
					R.layout.conversationitemme, null);

			// // Rounded corners
			LinearLayout oneline = (LinearLayout) conversationlistitem
					.findViewById(R.id.oneline);
			oneline.setBackgroundResource(R.drawable.rounded_cornersme);

			ImageView sent = (ImageView) conversationlistitem
					.findViewById(R.id.msgsent);
			ImageView received = (ImageView) conversationlistitem
					.findViewById(R.id.msgreceived);
			ImageView read = (ImageView) conversationlistitem
					.findViewById(R.id.msgread);
			conversationText = (ImageSmileyEditText) conversationlistitem
					.findViewById(R.id.conversationtext);
			ImageView speech = (ImageView) conversationlistitem
					.findViewById(R.id.msgspeech);

			// Create a mapping for later async update
			addMapping(conversationItem.mid, conversationItem.localid, sent,
					received, read, conversationText, oneline, speech);

			if (conversationItem.read > 0
					&& !Utility.loadBooleanSetting(context,
							Setup.OPTION_NOREAD, Setup.DEFAULT_NOREAD)) {
				// only display read for the people that allow this
				sent.setVisibility(View.GONE);
				received.setVisibility(View.GONE);
			} else if (conversationItem.received > 0) {
				sent.setVisibility(View.GONE);
				read.setVisibility(View.GONE);
			} else if (conversationItem.sent > 0) {
				received.setVisibility(View.GONE);
				read.setVisibility(View.GONE);
			} else {
				sent.setVisibility(View.GONE);
				received.setVisibility(View.GONE);
				read.setVisibility(View.GONE);
			}

			if (conversationItem.smsfailed) {
				oneline.setBackgroundColor(FAILEDCOLOR);
				speech.setImageResource(R.drawable.speechmefailed);
			}
		}

		conversationText
				.setOnCutCopyPasteListener(new ImageSmileyEditText.OnCutCopyPasteListener() {
					public void onPaste() {
					}

					public void onCut() {
					}

					public void onCopy() {
						promptImageSaveAs(context);
					}
				});

		TextView conversationTime = (TextView) conversationlistitem
				.findViewById(R.id.conversationtime);

		// // set software layering
		// conversationText.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

		ImageView locked = (ImageView) conversationlistitem
				.findViewById(R.id.msglocked);
		if (!conversationItem.encrypted) {
			locked.setVisibility(View.GONE);
		}

		ImageView sms = (ImageView) conversationlistitem
				.findViewById(R.id.msgsms);
		if (conversationItem.transport == DB.TRANSPORT_INTERNET) {
			sms.setVisibility(View.GONE);
		}

		OnLongClickListener longClickListener = new OnLongClickListener() {
			public boolean onLongClick(View arg0) {
				promptMessageDetails(context, conversationItem);
				return true;
			}
		};
		conversationlistitem.setOnLongClickListener(longClickListener);
		conversationTime.setOnLongClickListener(longClickListener);

		long time = conversationItem.sent;
		if (time < 0) {
			time = conversationItem.created;
		}
		conversationTime.setText(DB.getDateString(time, false));
		conversationTime.setId(conversationTime.hashCode());

		conversationText.setText(conversationItem.text);
		conversationText.setId(conversationItem.hashCode());

		return conversationlistitem;
	}

	// ------------------------------------------------------------------------

	/**
	 * Possible prompt for sending a new session key if this is not possible
	 * because one of the involved users does not have encryption enabled.
	 * 
	 * @param context
	 *            the context
	 */
	private void possiblePromptNewSession(Context context) {
		if (hostUid < 0) {
			// Tell the user to register
			promptInfo(
					this,
					"No Registered User",
					"In order to use encryption, your communication partner needs to be registered.");
			return;
		} else if (!Setup.isEncryptionAvailable(this, hostUid)) {
			// Tell the user to activate
			promptInfo(
					this,
					"Encryption Disabled",
					"You cannot use encryption because either you or your communication partner has not turned on this feature.\n\nIn order to use encryption both communication partners need to turn this feature on in their settings.");
			return;
		}
		// if (!isSMSModeAvailable(context)) {
		// // No choice, because SMS is not available
		// sendNewSession(context, DB.TRANSPORT_INTERNET);
		// return;
		// }
		// ALWAYS OFFER A CHOICE TO MAKE SURE WE REALLY WANT TO SEND A NEW
		// SESSION
		// Choice between Internet and SMS
		promptNewSession(context);
	}

	// -------------------------------------------------------------------------

	/**
	 * Send a new session key using the given transport.
	 * 
	 * @param context
	 *            the context
	 * @param transport
	 *            the transport
	 */
	public void sendNewSession(Context context, int transport) {
		// Send new key
		Communicator.getAESKey(this, Conversation.hostUid, true, transport,
				false, null, true);
	}

	// ------------------------------------------------------------------------

	/**
	 * Prompt to send a new session key. Prompt the user for a transport
	 * (Internet or SMS).
	 * 
	 * @param context
	 *            the context
	 */
	private void promptNewSession(final Context context) {
		String title = "Send New Session Key";
		String text = "Send the new session key via Internet or SMS?";

		// No choice, because SMS is not available
		final boolean noSMSOption = !isSMSModeAvailable(context);
		if (noSMSOption) {
			text = "Send the new session key via Internet?";
		}

		new MessageAlertDialog(context, title, text, null, null, " Cancel ",
				new MessageAlertDialog.OnSelectionListener() {
					public void selected(int button, boolean cancel) {
						// nothing
					}
				}, new MessageAlertDialog.OnInnerViewProvider() {

					public View provide(final MessageAlertDialog dialog) {
						LinearLayout buttonLayout = new LinearLayout(context);
						buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
						buttonLayout.setGravity(Gravity.CENTER_HORIZONTAL);

						LinearLayout.LayoutParams lpButtons = new LinearLayout.LayoutParams(
								180, 120);
						lpButtons.setMargins(20, 20, 20, 20);

						ImageLabelButton internetButton = new ImageLabelButton(
								context);
						internetButton.setTextAndImageResource("Internet",
								R.drawable.send);
						internetButton.setLayoutParams(lpButtons);
						internetButton
								.setOnClickListener(new View.OnClickListener() {
									public void onClick(View v) {
										sendNewSession(context,
												DB.TRANSPORT_INTERNET);
										dialog.dismiss();
									}
								});
						ImageLabelButton smsButton = new ImageLabelButton(
								context);
						smsButton.setTextAndImageResource("SMS",
								R.drawable.sendsms);
						smsButton.setLayoutParams(lpButtons);
						smsButton
								.setOnClickListener(new View.OnClickListener() {
									public void onClick(View v) {
										sendNewSession(context,
												DB.TRANSPORT_SMS);
										dialog.dismiss();
									}
								});
						buttonLayout.addView(internetButton);
						if (!noSMSOption) {
							buttonLayout.addView(smsButton);
						}
						return buttonLayout;
					}
				}).show();
	}

	// ------------------------------------------------------------------------

	/**
	 * Prompt message details. Use the Messagedetails activity for that. Set the
	 * hostUid and the conversationItem here.
	 * 
	 * @param context
	 *            the context
	 * @param conversationItem
	 *            the conversation item
	 */
	private void promptMessageDetails(final Context context,
			final ConversationItem conversationItem) {
		try {
			Intent dialogIntent = new Intent(context,
					MessageDetailsActivity.class);
			dialogIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			MessageDetailsActivity.conversationItem = conversationItem;
			MessageDetailsActivity.hostUid = hostUid;
			context.startActivity(dialogIntent);
		} catch (Exception e) {
			e.printStackTrace();
			// ignore
		}
	}

	// ------------------------------------------------------------------------

	/**
	 * Load conversation list. This is a helper method that uses the database DB
	 * class. It constructs a conversationListDiff list that can be used to only
	 * update the currently displayed conversation. This is necessary to FAST
	 * add new sent or received messages! Note that it is not possible to change
	 * the order later on. The only case where this matters is when suspending a
	 * message for sending and auto-sending a fresh session key before sending
	 * the suspended message afterwards. In this case the suspended message was
	 * added before the key message but both messages were sent in the other
	 * order. It may be harder to fix this because we want to see the message
	 * already even at a point when it is not clear if a new key needs to be
	 * generated and send before. A hot-fix might be to add the message another
	 * time and make the previous message invisible to fake a correct order. But
	 * as this seems not to be clean currently we live with a wrong order in
	 * this scenario. Still it is the correct created-order but only the wrong
	 * sent-order.
	 * 
	 * @param context
	 *            the context
	 * @param hostUid
	 *            the host uid
	 * @param maxScrollMessageItems
	 *            the max scroll message items
	 */
	private void loadConversationList(Context context, int hostUid,
			int maxScrollMessageItems) {
		List<ConversationItem> conversationListNew = new ArrayList<ConversationItem>();
		DB.loadConversation(context, hostUid, conversationListNew,
				maxScrollMessageItems);
		conversationListDiff.clear();
		for (ConversationItem itemNew : conversationListNew) {
			boolean found = false;
			for (ConversationItem item : conversationList) {
				if (item.localid == itemNew.localid) {
					found = true;
					break;
				}
			}
			if (!found) {
				conversationListDiff.add(itemNew);
			}
		}
		conversationList = conversationListNew;
	}

	// ------------------------------------------------------------------------
	// ------------------------------------------------------------------------

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

	// ------------------------------------------------------------------------
	// ------------------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// This is necessary to enable a context menu
		getMenuInflater().inflate(R.menu.activity_conversation, menu);
		return true;
	}

	// ------------------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onKeyDown(int, android.view.KeyEvent)
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// SEARCH KEY OVERRIDE
		if (keyCode == KeyEvent.KEYCODE_SEARCH) {
			promptSearch(this);
			return true;
		}
		return super.onKeyDown(keyCode, event);

	}

	// ------------------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	public boolean onOptionsItemSelected(MenuItem item) {
		// The context menu implementation
		switch (item.getItemId()) {
		case android.R.id.home:
			goBack(this);
			return true;
		case R.id.item2:
			maxScrollMessageItems = Setup.SHOW_ALL;
			updateConversationlist(this);
			rebuildConversationlist(this);
			onRestart();
			onStart();
			onResume();
			return true;
		case R.id.item3:
			clearConversation(this);
			return true;
		case R.id.item4:
			backup(this);
			return true;
		case R.id.itemsearch:
			promptSearch(this);
			return true;
		case R.id.item6:
			possiblePromptNewSession(this);
			return true;
		case R.id.menu_settings:
			doRefresh(this);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	// ------------------------------------------------------------------------

	/**
	 * Go back to the main activity.
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
	 * Do refresh and try to send or receive message.
	 * 
	 * @param context
	 *            the context
	 */
	public void doRefresh(final Context context) {
		Communicator.sendNextMessage(this);
		Communicator.haveNewMessagesAndReceive(this);
		Utility.showToastShortAsync(this, "Refreshing...");
	}

	// ------------------------------------------------------------------------

	/**
	 * Prompt to search the conversation. Note that this might not be standard
	 * behavior but still it is a very simple behavior. A search starts at the
	 * current position (conversation item visible at the top of the screen).
	 * The user can select to search UP or DOWN starting from this item. The
	 * text box allows to enter the search text the user searches for. If there
	 * is no text entered yet the keyboard pops up automatically. Of course this
	 * is annoying if the user wants to search for several occurrences.
	 * Therefore the search text is saved and filled out the next time the user
	 * clicks on search. In this case, when there was a previous search and the
	 * search text contains something, the keyboard is hidden by default and the
	 * user must click into the text field in order to change the text field.
	 * The user may also click on clean which will erase the search text and pop
	 * up the keyboard immediately. We believe this is a very clean an
	 * responsive search dialog behavior. If something is found then the
	 * conversation scrolls to this conversation item and tries to highlight the
	 * whole text. The next search result must be another conversation item (UP
	 * or DOWN). If there is no other (UP or DOWN) then the user will get a
	 * toast informing about the end of search.
	 * 
	 * @param context
	 *            the context
	 */
	public void promptSearch(final Context context) {

		String lastSearch = Utility.loadStringSetting(context,
				"lastconversationsearch", "");
		final int lastFound = Utility.loadIntSetting(context,
				"lastconversationsearchfound", -1);

		// String title = "Search Conversation";
		String title = "Enter some text to search for:";
		new MessageInputDialog(context, title, null, " Up ", "Down", "Clear",
				lastSearch, new MessageInputDialog.OnSelectionListener() {
					public void selected(MessageInputDialog dialog, int button,
							boolean cancel, String searchString) {
						if (button == MessageInputDialog.BUTTONCANCEL) {
							// Cancel search (remove search text)
							Utility.saveStringSetting(context,
									"lastconversationsearch", "");
							dialog.setText("", true);
						} else if ((button == MessageInputDialog.BUTTONOK0)
								|| (button == MessageInputDialog.BUTTONOK1)) {
							dialog.dismiss();
							Utility.saveStringSetting(context,
									"lastconversationsearch", searchString);

							// try to unmark (remove previous highlighting)
							{
								if (lastFound != -1) {
									Mapping mappingItem = mapping
											.get(lastFound);
									if (mappingItem != null) {
										EditText editText = mappingItem.text;
										if (editText != null) {
											editText.setSelection(0, 0);
										}
									}
								}
							}

							boolean reverse = (button == MessageInputDialog.BUTTONOK0);
							int start = scrollItem;
							int end = conversationList.size() - 1;
							if (start > end) {
								start = end;
							}
							int incr = 1;
							if (reverse) {
								start = scrollItem - 2;
								end = 0;
								if (start < end) {
									start = end;
								}
								incr = -1;
							}
							if (scrollItem > -1) {
								int foundItem = -1;
								int mid = -1;
								for (int c = start; c != end; c = c + incr) {
									if (conversationList.get(c).text
											.contains(searchString)) {
										foundItem = c;
										// Try to mark / highlight
										ConversationItem item = conversationList
												.get(c);
										mid = item.mid;
										if (mid == -1) {
											mid = -1 * item.localid;
										}
										if (lastFound != mid) {
											// Otherwise search on...
											Mapping mappingItem = mapping
													.get(mid);
											if (mappingItem != null) {
												EditText editText = mappingItem.text;
												if (editText != null) {
													editText.selectAll();
												}
											}
											Utility.saveIntSetting(
													context,
													"lastconversationsearchfound",
													mid);
											break;
										} else {
											foundItem = -1; // We did not find
															// anything NEW
															// here...!
										}
									}
								}
								if (foundItem != -1) {
									fastScrollView.scrollToItem(foundItem);
									Utility.showToastInUIThread(context, "'"
											+ searchString
											+ "' found in message "
											+ (mid + "").replace("-", "*")
											+ ".");
								} else {
									String updown = "(down)";
									if (reverse) {
										updown = "(up)";
									}
									Utility.showToastInUIThread(context, "'"
											+ searchString + "' not found "
											+ updown + ".");
								}
							}
						}
					}
				}).show();

	}

	// ------------------------------------------------------------------------

	/**
	 * Prompt info. This is a shortcut for bringing some information to the
	 * user's attention. It is mostly used in this activity but also in others.
	 * Therefore it is a static method.
	 * 
	 * @param context
	 *            the context
	 * @param title
	 *            the title
	 * @param text
	 *            the text
	 */
	public static void promptInfo(Context context, String title, String text) {
		new MessageAlertDialog(context, title, text, " Ok ", null, null,
				new MessageAlertDialog.OnSelectionListener() {
					public void selected(int button, boolean cancel) {
					}
				}).show();
	}

	// ------------------------------------------------------------------------

	/**
	 * Prompt the backup activity to allow the user to backup its conversation.
	 * 
	 * @param context
	 *            the context
	 */
	public void backup(Context context) {
		Intent dialogIntent = new Intent(context, BackupActivity.class);
		dialogIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		BackupActivity.hostUid = hostUid;
		context.startActivity(dialogIntent);
	}

	// ------------------------------------------------------------------------

	/**
	 * Clear conversation.
	 * 
	 * @param context
	 *            the context
	 */
	public void clearConversation(final Context context) {
		new MessageAlertDialog(
				context,
				"Clear Conversation",
				"Clearing the conversation means deleting all messages between you and this user from your local device. Keep in mind that you might not see"
						+ " all but only the last "
						+ Setup.MAX_SHOW_CONVERSATION_MESSAGES
						+ " messages of the current conversation.\n\n"
						+ "Do you really want to clear the whole conversation with "
						+ Main.UID2Name(context, hostUid, false) + " ?",
				" Clear ", " Abort ", null,
				new MessageAlertDialog.OnSelectionListener() {
					public void selected(int button, boolean cancel) {
						if (!cancel && button == 0) {
							// Clear conversation
							DB.deleteUser(context, hostUid);
							rebuildConversationlist(context);
							// Update the first line
							Utility.saveStringSetting(context,
									Setup.SETTINGS_USERLISTLASTMESSAGE
											+ hostUid, null);
							Utility.saveLongSetting(context,
									Setup.SETTINGS_USERLISTLASTMESSAGETIMESTAMP
											+ hostUid, -1);
						}
					}
				}).show();
	}

	// ------------------------------------------------------------------------

	/**
	 * Update conversation title async from non UI thread.
	 * 
	 * @param context
	 *            the context
	 */
	public void updateConversationTitleAsync(final Context context) {
		updateConversationTitleAsync(context, null);
	}

	// -------------------------------------------------------------------------

	/**
	 * Update conversation title async from non UI thread.
	 * 
	 * @param context
	 *            the context
	 * @param titleText
	 *            the title text
	 */
	public void updateConversationTitleAsync(final Context context,
			final String titleText) {
		final Handler mUIHandler = new Handler(Looper.getMainLooper());
		mUIHandler.post(new Thread() {
			@Override
			public void run() {
				super.run();
				updateConversationTitle(context, titleText);
			}
		});
	}

	// -------------------------------------------------------------------------

	/**
	 * Update conversation title.
	 * 
	 * @param context
	 *            the context
	 */
	public void updateConversationTitle(Context context) {
		updateConversationTitle(context, null);
	}

	// -------------------------------------------------------------------------

	/**
	 * Update conversation title.
	 * 
	 * @param context
	 *            the context
	 * @param titleText
	 *            the title text
	 */
	public void updateConversationTitle(Context context, String titleText) {
		if (titleText == null || titleText.length() == 0) {
			Conversation.getInstance().setTitle(
					Main.UID2Name(context, hostUid, false) + " - "
							+ Setup.getAESKeyHash(context, hostUid));
		} else {
			Conversation.getInstance().setTitle(titleText);
		}
	}

	// -------------------------------------------------------------------------

	private static final int SELECT_PICTURE = 1;
	private static final int TAKE_PHOTO = 2;

	/**
	 * Select attachment.
	 * 
	 * @param activity
	 *            the activity
	 */
	public static void selectAttachment(Activity activity) {
		Intent intent = new Intent();

		if (Build.VERSION.SDK_INT >= 19) {
			intent = new Intent(
					Intent.ACTION_PICK,
					android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
			activity.startActivityForResult(intent, SELECT_PICTURE);
		} else {
			intent.setType("image/*");
			intent.setAction(Intent.ACTION_PICK);
			activity.startActivityForResult(
					Intent.createChooser(intent, "Select Attachment"),
					SELECT_PICTURE);
		}
	}

	// -------------------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onActivityResult(int, int,
	 * android.content.Intent)
	 */
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		// To handle when an image is selected from the browser, add the
		// following
		// to your Activity
		if (resultCode == RESULT_OK) {
			if (requestCode == SELECT_PICTURE) {
				boolean ok = false;
				String attachmentPath = Utility.getRealPathFromURI(this,
						data.getData());
				if (attachmentPath != null) {
					try {
						insertImage(this, attachmentPath);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				if (!ok) {
					Utility.showToastInUIThread(this,
							"Selected file is not a valid image.");
				}
			}
			if (requestCode == TAKE_PHOTO) {
				final boolean keyboardWasVisible = keyboardVisible;
				final boolean wasScrolledDown = scrolledDown;
				final Context context = this;
				PictureImportActivity
						.setOnPictureImportListener(new PictureImportActivity.OnPictureImportListener() {
							public void onImport(String encodedImage) {
								lastKeyStroke = DB.getTimestamp()
										- Setup.TYPING_TIMEOUT_BEFORE_UI_ACTIVITY
										- 1;
								Utility.smartPaste(messageText, encodedImage,
										" ", " ", false, false, true);
								if (wasScrolledDown) {
									scrollDownNow(context, keyboardWasVisible);
									scrollDownSoon(context, keyboardWasVisible,
											2000);
								}
							}
						});

				Bitmap bitmap = (Bitmap) data.getExtras().get("data");
				Intent dialogIntent = new Intent(this,
						PictureImportActivity.class);
				dialogIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				PictureImportActivity.attachmentBitmap = bitmap;
				this.startActivity(dialogIntent);
			}
		}
	}

	// -------------------------------------------------------------------------

	/**
	 * Save image in gallery.
	 * 
	 * @param context
	 *            the context
	 * @param encodedImg
	 *            the encoded img
	 */
	public static void saveImageInGallery(final Context context,
			String encodedImg) {
		try {
			// Save image in gallery
			Bitmap bitmap = Utility.loadImageFromBASE64String(context,
					encodedImg);
			String bitmapPath = Utility.insertImage(
					context.getContentResolver(), bitmap, "Cryptocator Images",
					null);
			Utility.updateMediaScanner(context, bitmapPath);
			Utility.showToastAsync(context, "Image saved to " + bitmapPath);
		} catch (Exception e) {
			Utility.showToastAsync(context, "Error saving image to gallery.");
		}
	}

	// -------------------------------------------------------------------------

	/**
	 * Share image.
	 * 
	 * @param context
	 *            the context
	 * @param encodedImg
	 *            the encoded img
	 */
	public static void shareImage(final Context context, String encodedImg) {
		// Share image to other apps
		try {
			Bitmap bitmap = Utility.loadImageFromBASE64String(context,
					encodedImg);

			String bitmapPath = Images.Media.insertImage(
					context.getContentResolver(), bitmap, "Cryptocator Images",
					null);
			Uri bitmapUri = Uri.parse(bitmapPath);

			Intent sendIntent = new Intent(Intent.ACTION_SEND);
			sendIntent.setType("image/*");
			sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Cryptocator Image");
			sendIntent.putExtra(Intent.EXTRA_STREAM, bitmapUri);
			sendIntent.putExtra(Intent.EXTRA_TEXT, "Cryptocator Image");
			context.startActivity(Intent.createChooser(sendIntent, "Share"));
		} catch (Exception e) {
			Utility.showToastAsync(context, "Error sharing image.");
		}
	}

	// -------------------------------------------------------------------------

	/**
	 * Prompt the user to save the image (currently in the clipboard) to the
	 * gallery or share it to other apps.
	 * 
	 * @param context
	 *            the context
	 */
	public static void promptImageSaveAs(final Context context) {
		final String copiedText = Utility.pasteFromClipboard(context);
		int imgStart = copiedText.indexOf("[img ");
		if (imgStart == -1) {
			// no image copied or more than an image... do not ask
			return;
		}
		int imgEnd = copiedText.indexOf("]", imgStart);
		if (imgEnd == -1) {
			// no image copied or more than an image... do not ask
			return;
		}
		final String encodedImg = copiedText.substring(imgStart, imgEnd);

		String title = "Save Image?";
		String text = "You copied an image into the Clipboard. Do you want to save or share it?";

		new MessageAlertDialog(context, title, text, " Save ", " Share ",
				" Cancel ", new MessageAlertDialog.OnSelectionListener() {
					public void selected(int button, boolean cancel) {
						if (button == MessageAlertDialog.BUTTONOK0) {
							saveImageInGallery(context, encodedImg);
						}
						if (button == MessageAlertDialog.BUTTONOK1) {
							shareImage(context, encodedImg);
						}
					}
				}, null).show();
	}

	// -------------------------------------------------------------------------

	/**
	 * Reprocess possible images in text and display them. This is necessary
	 * after adding such images to the text, e.g. by past or a select action.
	 * This is achieved by setting the text of the editText which re-triggers
	 * the image processing. The cursor position is saved before.
	 * 
	 * @param editText
	 *            the edit text
	 */
	public static void reprocessPossibleImagesInText(EditText editText) {
		int selection = editText.getSelectionStart();
		String messageTextBackup = editText.getText().toString();
		editText.setText(messageTextBackup);
		if (selection > -1) {
			editText.setSelection(selection);
		}
	}

	// -------------------------------------------------------------------------

	/**
	 * Prompt the image insert dialog
	 * 
	 * @param context
	 *            the context
	 */
	public void insertImage(final Context context, String attachmentPath) {
		final boolean keyboardWasVisible = keyboardVisible;
		final boolean wasScrolledDown = scrolledDown;
		PictureImportActivity
				.setOnPictureImportListener(new PictureImportActivity.OnPictureImportListener() {
					public void onImport(String encodedImage) {
						Utility.smartPaste(messageText, encodedImage, " ", " ",
								false, false, true);
						if (wasScrolledDown) {
							scrollDownNow(context, keyboardWasVisible);
							scrollDownSoon(context, keyboardWasVisible, 2000);
						}
					}
				});
		Intent dialogIntent = new Intent(context, PictureImportActivity.class);
		dialogIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		byte[] bytes = Utility.getFile(attachmentPath);
		Bitmap bitmap = Utility.getBitmapFromBytes(bytes);

		PictureImportActivity.attachmentBitmap = bitmap;
		context.startActivity(dialogIntent);
	}

	// ------------------------------------------------------------------------

	/**
	 * Prompt the user to import a picture from gallery or take a fresh photo.
	 * 
	 * @param context
	 *            the context
	 */
	public static void promptImageInsert(final Activity activity, int hostUid) {
		if (hostUid < 0) {
			// SMS users cannot receive images!
			promptInfo(
					activity,
					"No Registered User",
					"In order to send attachment images, your communication partner needs to be registered.");
			return;
		}
		if (!Setup.isAttachmentsAllowedByServer(activity)) {
			String title = "Attachments Not Allowed";
			String text = "Attachments are not allowed by the server and will be removed for Internet messages.\n"
					+ "You may still send them via SMS but be advised not to send too "
					+ "large images via SMS.\n\nDo you still want to add an attachment?";

			new MessageAlertDialog(activity, title, text, " Yes ", " No ",
					" Cancel ", new MessageAlertDialog.OnSelectionListener() {
						public void selected(int button, boolean cancel) {
							if (button == MessageAlertDialog.BUTTONOK0) {
								promptImageInsert2(activity);
							}
						}
					}, null).show();

		} else {
			promptImageInsert2(activity);
		}

	}

	// ------------------------------------------------------------------------

	/**
	 * Prompt the user to import a picture from gallery or take a fresh photo.
	 * 
	 * @param context
	 *            the context
	 */
	public static void promptImageInsert2(final Activity activity) {
		String title = "Insert Image";
		String text = "Do you want to import an image from the gallery or take a new photo?";

		new MessageAlertDialog(activity, title, text, null, null, " Cancel ",
				new MessageAlertDialog.OnSelectionListener() {
					public void selected(int button, boolean cancel) {
						// Nothing
					}
				}, new MessageAlertDialog.OnInnerViewProvider() {

					public View provide(final MessageAlertDialog dialog) {
						LinearLayout buttonLayout = new LinearLayout(activity);
						buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
						buttonLayout.setGravity(Gravity.CENTER_HORIZONTAL);

						LinearLayout.LayoutParams lpButtons = new LinearLayout.LayoutParams(
								180, 140);
						lpButtons.setMargins(20, 20, 20, 20);

						ImageLabelButton galleryButton = new ImageLabelButton(
								activity);
						galleryButton.setTextAndImageResource("Gallery",
								R.drawable.pictureimport);
						galleryButton.setLayoutParams(lpButtons);
						galleryButton
								.setOnClickListener(new View.OnClickListener() {
									public void onClick(View v) {
										selectAttachment(activity);
										dialog.dismiss();
									}
								});
						ImageLabelButton photoButton = new ImageLabelButton(
								activity);
						photoButton.setTextAndImageResource("Take Photo",
								R.drawable.photobtn);
						photoButton.setLayoutParams(lpButtons);
						photoButton
								.setOnClickListener(new View.OnClickListener() {
									public void onClick(View v) {
										takePhoto(activity);
										dialog.dismiss();
									}
								});
						buttonLayout.addView(galleryButton);
						buttonLayout.addView(photoButton);
						return buttonLayout;
					}
				}).show();
	}

	// -------------------------------------------------------------------------

	/**
	 * Take photo.
	 * 
	 * @param activity
	 *            the activity
	 */
	private static void takePhoto(Activity activity) {
		if (!Utility.isCameraAvailable(activity)) {
			Utility.showToastAsync(activity, "No Camera available.");
			return;
		}
		Intent cameraIntent = new Intent(
				android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
		activity.startActivityForResult(cameraIntent, TAKE_PHOTO);
	}

	// -------------------------------------------------------------------------

	/**
	 * Possibly remove image attachments if too large. This is a convenience
	 * method: It will not force to remove all images and it will substitue
	 * removed images by an empty string.
	 * 
	 * @param context
	 *            the context
	 * @param text
	 *            the text
	 * @return the string
	 */
	public static String possiblyRemoveImageAttachments(Context context,
			String text) {
		return possiblyRemoveImageAttachments(context, text, false, "");
	}

	// -------------------------------------------------------------------------

	/**
	 * Possibly remove image attachments if too large. The forceRemoveAll flag
	 * may be used to get a version without any images, e.g. for the first line
	 * or the ticker.
	 * 
	 * @param context
	 *            the context
	 * @param text
	 *            the text
	 * @param forceRemoveAll
	 *            the force remove all
	 * @return the string
	 */
	public static String possiblyRemoveImageAttachments(Context context,
			String text, boolean forceRemoveAll, String substitute) {

		int limit = Setup.getAttachmentServerLimit(context) * 1000;
		if (text.length() < limit && !forceRemoveAll) {
			Log.d("communicator",
					"total text is smaller than the attachment limit : textlen="
							+ text.length() + " < " + limit + " (limit)");
			// The total text is smaller than the attachment limit, so we do not
			// need to erase images manually
			return text;
		}

		final String STARTTAG = "[img ";
		final String ENDTAG = "]";

		int start = text.indexOf(STARTTAG);

		if (start < 0) {
			Log.d("communicator", "possiblyRemoveImageAttachments NO images");
			// No images to remove
			return text;
		}

		// The total text is larger than the attachment limit and there are
		// images included. So we now need to erase these...
		String strippedText = "";
		boolean done = false;
		start = 0;
		int end = 0;
		while (!done) {
			// Search for start tag
			start = text.indexOf(STARTTAG, end);
			if (start == -1) {
				// Not further start Found
				done = true;
				// Add last remaining text
				String textBetween = text.substring(end, text.length());
				strippedText += textBetween;
			} else {
				// Process any text since last end to this start
				String textBetween = text.substring(end, start);
				strippedText += textBetween;

				// Found, process this image
				end = text.indexOf(ENDTAG, start) + 1;

				String textImage = text.substring(start, end);

				int diff = end - start;
				if (diff <= limit && !forceRemoveAll) {
					// Image small enough, append
					strippedText += textImage;
				} else {
					strippedText += substitute;
				}
			}
		}
		return strippedText;
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
				Log.d("communicator",
						"msgTextLEN=" + messageTextString.length());
				Log.d("communicator",
						"msgText2LEN=" + messageTextString2.length());
				Log.d("communicator", "msgText=" + messageTextString);
				Log.d("communicator", "msgText2=" + messageTextString2);

				if ((messageTextString2.length() != messageTextString.length())) {
					String title = "WARNING";
					String text = "This message contains at least one image that exceeds server limits. "
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
							+ numSMS
							+ " multi part SMS to be sent!\n\nReally send "
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

}
