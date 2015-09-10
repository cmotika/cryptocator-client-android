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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import org.cryptocator.ImageContextMenu.ImageContextMenuProvider;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.provider.ContactsContract.PhoneLookup;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

/**
 * This is the main activity for Delphino Cryptocator. It is responsible showing
 * the main screen with the user list. Some of the data is hold static because
 * there should always only be one instance of this activity.<BR>
 * <BR>
 * With version 1.3 Delphino Cryptocator will support multiple message servers
 * which allows for a user to be part of multiple closed user groups, e.g., (1)
 * family, (2) work, (3) hobby. The message servers can be activated an
 * deactivated, e.g., on holiday one may want to disable the (2) work-message
 * server. As old style UIDs are only unique throughout ONE server these will
 * now be called SUIDs (server=uids). The combination of an SUID and a serverId
 * will bring us to the new UIDs that will now be used.
 * 
 * @author Christian Motika
 * @date 08/23/2015
 * @since 1.2
 * 
 */
@SuppressLint("InflateParams")
public class Main extends Activity {

	/**
	 * The Constant DEFAULTTEXT. This will be displayed in the text input field
	 * when the user wants to add someone to the userlist. This is for other
	 * registered users only.
	 */
	private static final String DEFAULTTEXT = "Enter the UID here!";

	// ------------------------------------------------------------------------

	/** The current list of uids. */
	private static List<Integer> uidList = null;

	/** The adduseritem. */
	private LinearLayout adduseritem;

	/** The serverspinner. */
	private Spinner serverspinner;

	/** The main background. */
	private LinearLayout mainBackground;

	/** The maininnerview. */
	private LinearLayout mainInnerView;

	/** The add user text. */
	private TextView addUserText;

	/** The add user name. */
	private KeyEventEditText addUserName;

	/** The add user button. */
	private Button addUserButton;

	/** The context. */
	private Activity context = this;

	/** The image context menu provider for the main menu. */
	private ImageContextMenuProvider imageContextMenuProvider = null;

	/** The image context menu provider for the account menu. */
	private ImageContextMenuProvider imageAccountMenuProvider = null;

	/**
	 * The skips ONE resume. Necessary for the add user context menu call
	 * because we do not want to rebuild the user list if we show the add user
	 * section.
	 */
	public static boolean skipResume = false;

	/**
	 * The high contrast will be set by a possibly existing light sensor. If the
	 * light, e.g., outside is very bright we want a better readablity and
	 * change the text color to WHITE instead of the default VERY LIGHT GRAY.
	 */
	public static boolean highContrast = false;

	/**
	 * If the light sensor value is higher than 98% of the so far maximum value,
	 * then high contrast will be set to true.
	 */
	public static int highContrastBarrierInPercent = 98;

	/** The Constant TEXTCOLOEWHITE. */
	public static final int TEXTCOLOEWHITE = Color.parseColor("#FFFFFFFF");

	/** The Constant TEXTCOLOEWHITEDIMMED. */
	public static final int TEXTCOLOEWHITEDIMMED = Color
			.parseColor("#FFE8E8E8");

	/** The Constant TEXTCOLOEWHITEDIMMED. */
	public static final int TEXTCOLOEWHITEDIMMED2 = Color
			.parseColor("#CCDDDDDD");

	// ------------------------------------------------------------------------

	/**
	 * The listener interface for receiving update events. The class that is
	 * interested in processing a update event implements this interface, and
	 * the object created with that class is registered with a component using
	 * the component's <code>addUpdateListener<code> method. When
	 * the update event occurs, that object's appropriate
	 * method is invoked.
	 * 
	 * @see UpdateEvent
	 */
	public interface UpdateListener {

		/**
		 * On update.
		 * 
		 * @param data
		 *            the data
		 */
		public void onUpdate(String data);
	}

	// ------------------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Setup.possiblyDisableScreenshot(this);

		DB.possiblyUpdate(this);

		Main.visible = true;
		instance = this;
		context = this;

		// Possibly create the context menu
		createContextMenu(context);

		// Do this as early as possible
		Setup.updateAllServerkeys(context);

		if (Utility.loadBooleanSetting(context, Setup.OPTION_NOSCREENSHOTS,
				Setup.DEFAULT_NOSCREENSHOTS)) {
			getWindow().setFlags(LayoutParams.FLAG_SECURE,
					LayoutParams.FLAG_SECURE);
		}

		// Apply custom title bar (with holo :-)
		LinearLayout main = Utility.setContentViewWithCustomTitle(this,
				R.layout.activity_main, R.layout.title_main);
		Utility.setBackground(this, main, R.drawable.dolphins1);

		// Comments on own custom title bar
		//
		// ATTENTION:
		// ADD THIS TO THEME <item name="android:windowNoTitle">true</item>
		// requestWindowFeature(Window.FEATURE_NO_TITLE);
		// We can ONLY disable the original title bar because you cannot
		// combine HOLO theme with a CUSTOM title bar :(
		// So we make our own title bar instead!
		//
		// THE FOLLOWING IS NOT WORKING WITH HOLO
		// requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		// setContentView(R.layout.activity_main);
		// getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
		// R.layout.title_main);
		//
		//
		// After setting NO TITLE .. apply the layout
		// setContentView(R.layout.activity_main);

		LinearLayout titlemain = (LinearLayout) findViewById(R.id.titlemain);

		// Set the menu buttons
		ImagePressButton btncompose = (ImagePressButton) findViewById(R.id.btncompose);
		btncompose.initializePressImageResource(R.drawable.btncompose);
		LinearLayout btncomposeparent = (LinearLayout) findViewById(R.id.btncomposeparent);
		btncompose.setAdditionalPressWhiteView(btncomposeparent);
		btncompose.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				showHideAddUser(context, false);
				composeMessage(context, null, null);
			}
		});

		// TextView titletext = (TextView) findViewById(R.id.titletext);
		final LinearLayout titletextparent = (LinearLayout) findViewById(R.id.titletextparent);
		titletextparent.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				ImageContextMenu.show(context, createAccountMenu(context));
			}
		});
		titletextparent.setOnTouchListener(new View.OnTouchListener() {
			@SuppressLint("ClickableViewAccessibility")
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					titletextparent
							.setBackgroundColor(ImagePressButton.WHITEPRESS);
					titletextparent.postDelayed(new Runnable() {
						public void run() {
							titletextparent
									.setBackgroundColor(ImagePressButton.TRANSPARENT);
						}
					}, 300);
				}
				return false;
			}
		});

		ImagePressButton btnadduser = (ImagePressButton) findViewById(R.id.btnadduser);
		btnadduser.initializePressImageResource(R.drawable.btnadduser);
		LinearLayout btnadduserparent = (LinearLayout) findViewById(R.id.btnadduserparent);
		btnadduser.setAdditionalPressWhiteView(btnadduserparent);
		btnadduser.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				showHideAddUser(context, false);
				showHideAddUser(context, true);
			}
		});
		ImagePressButton btnrefresh = (ImagePressButton) findViewById(R.id.btnrefresh);
		btnrefresh.initializePressImageResource(R.drawable.btnrefresh);
		LinearLayout btnrefreshparent = (LinearLayout) findViewById(R.id.btnrefreshparent);
		btnrefresh.setAdditionalPressWhiteView(btnrefreshparent);
		btnrefresh.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				showHideAddUser(context, false);
				doRefresh(context);
			}
		});
		ImagePressButton btnmenu = (ImagePressButton) findViewById(R.id.btnmenu);
		btnmenu.initializePressImageResource(R.drawable.btnmenu);
		LinearLayout btnmenuparent = (LinearLayout) findViewById(R.id.btnmenuparent);
		btnmenu.setAdditionalPressWhiteView(btnmenuparent);
		btnmenu.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				showHideAddUser(context, false);
				openOptionsMenu();
			}
		});

		// Yes, at startup resolve names!
		// but only after rebuild the uidList is filled
		rebuildUserlist(context, true);

		// Ensure that all databases are up and running...
		DB.ensureDBInitialized(context, uidList);

		// Refresh current RSA keys from server (in the background if necessary)
		Communicator.updateKeysFromAllServers(context, uidList, false, null);
		Communicator.updatePhonesFromAllServers(this, uidList, false);
		// Refresh server attachment limit (if needed)
		Setup.updateAttachmentAllServerLimits(context, false);

		// Set the backgrounds
		Utility.setBackground(this, titlemain, R.drawable.dolphins3blue);
		mainBackground = (LinearLayout) findViewById(R.id.mainbackground);
		Utility.setBackground(this, mainBackground, R.drawable.dolphins1);
		Utility.setBackground(this, mainInnerView, R.drawable.dolphins1);

		// If we click on the background, hide the adduser panel and open the
		// context menu
		main.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				showHideAddUser(context, false);
				// show context menu
				openOptionsMenu();
			}
		});

		// Only prompt this if no one in the userlist and no UID (account)
		// information is stored in the settings
		if (uidList.size() == 0 && !Setup.isUIDDefined(context)) {
			possiblyPromptUserIfNoAccount(this, mainBackground);
		} else {
			Setup.possiblyPromptNoEncryption(context);
		}

		// Cleanup old mappings between mid and uid (recipient of our messages)
		DB.removeOldMappings(context);
	}

	// ------------------------------------------------------------------------

	/**
	 * Sets the title of the custom title bar.
	 * 
	 * @param title
	 *            the new title
	 */
	public void setTitle(String title) {
		TextView titletext = (TextView) findViewById(R.id.titletext);
		titletext.setText(title);
	}

	// ------------------------------------------------------------------------

	/**
	 * Possibly prompt user if no account was found.
	 * 
	 * @param context
	 *            the context
	 * @param anyView
	 *            the any view
	 * @return true, if successful
	 */
	public static boolean possiblyPromptUserIfNoAccount(final Context context,
			View anyView) {
		if (Setup.noAccountYet(context)) {
			anyView.postDelayed(new Runnable() {
				public void run() {
					// Check if no account
					String title = "Welcome!";
					String text = "You don't have configured an account yet. In order to use Delphino Cryptocator you need an account."
							+ "\n\nDo you want to create one now or enter an existing one?";
					new MessageAlertDialog(context, title, text, " Yes ",
							" No ", null,
							new MessageAlertDialog.OnSelectionListener() {
								public void selected(int button, boolean cancel) {
									if (!cancel && button == 0) {
										startAccount(context, -1);
									}
								}
							}).show();
				}
			}, 400);
			return false;
		}
		return true;
	}

	// ------------------------------------------------------------------------

	/**
	 * Creates the account menu for the main activity.
	 * 
	 * @param context
	 *            the context
	 */
	private ImageContextMenuProvider createAccountMenu(final Context context) {
		if (imageAccountMenuProvider == null) {
			imageAccountMenuProvider = new ImageContextMenuProvider(context,
					"Your Accounts", context.getResources().getDrawable(
							R.drawable.buttonedit));

			for (final int serverId : Setup.getServerIds(context)) {
				int myUid = DB.myUid(context, serverId);
				String myName = Main.UID2Name(context, myUid, true, false,
						serverId);
				imageAccountMenuProvider
						.addEntry(
								myName,
								R.drawable.menuaccount,
								new ImageContextMenu.ImageContextMenuSelectionListener() {
									public boolean onSelection(
											ImageContextMenu instance) {
										startAccount(context, serverId);
										return true;
									}
								});
			}
			imageAccountMenuProvider.addEntry("Settings",
					R.drawable.menusettings,
					new ImageContextMenu.ImageContextMenuSelectionListener() {
						public boolean onSelection(ImageContextMenu instance) {
							startSettings(context);
							return true;
						}
					});
		}
		return imageAccountMenuProvider;
	}

	// ------------------------------------------------------------------------

	/**
	 * Creates the context menu for the main activity.
	 * 
	 * @param context
	 *            the context
	 */
	private ImageContextMenuProvider createContextMenu(final Context context) {
		if (imageContextMenuProvider == null) {
			imageContextMenuProvider = new ImageContextMenuProvider(context,
					null, null);
			imageContextMenuProvider.addEntry("Compose",
					R.drawable.menucompose,
					new ImageContextMenu.ImageContextMenuSelectionListener() {
						public boolean onSelection(ImageContextMenu instance) {
							composeMessage(context, null, null);
							return true;
						}
					});
			imageContextMenuProvider.addEntry("Add User",
					R.drawable.menuadduser,
					// We must skip resume on finish, otherwise the userlist
					// will be rebuild
					// skipResume is reset by Main.onResume() itself for the
					// next time
					new ImageContextMenu.ImageContextMenuSelectionListener() {
						public boolean onSelection(ImageContextMenu instance) {
							Main.skipResume = true;
							if (possiblyPromptUserIfNoAccount(context,
									mainBackground)) {
								showHideAddUser(context, true);
							}
							return true;
						}
					});
			imageContextMenuProvider.addEntry("Add SMS User",
					R.drawable.menuadduserext,
					new ImageContextMenu.ImageContextMenuSelectionListener() {
						public boolean onSelection(ImageContextMenu instance) {
							addExternalSMSUser(context);
							return true;
						}
					});
			imageContextMenuProvider.addEntry("Accounts",
					R.drawable.menuaccount,
					new ImageContextMenu.ImageContextMenuSelectionListener() {
						public boolean onSelection(ImageContextMenu instance) {
							startAccount(context, -1);
							return true;
						}
					});
			imageContextMenuProvider.addEntry("Settings",
					R.drawable.menusettings,
					new ImageContextMenu.ImageContextMenuSelectionListener() {
						public boolean onSelection(ImageContextMenu instance) {
							startSettings(context);
							return true;
						}
					});
			imageContextMenuProvider.addEntry("Refresh",
					R.drawable.menurefresh,
					new ImageContextMenu.ImageContextMenuSelectionListener() {
						public boolean onSelection(ImageContextMenu instance) {
							doRefresh(context);
							return true;
						}
					});
		}
		return imageContextMenuProvider;
	}

	// ------------------------------------------------------------------------

	// /*
	// * (non-Javadoc)
	// *
	// * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	// */
	// public boolean onOptionsItemSelected(MenuItem item) {
	// // The context menu implementation
	// switch (item.getItemId()) {
	// case android.R.id.home:
	// composeMessage(context, null, null);
	// return true;
	// case R.id.item1:
	// startAccount(this);
	// return true;
	// case R.id.item2:
	// startSettings(this);
	// return true;
	// case R.id.item3b:
	// addExternalSMSUser(this);
	// return true;
	// case R.id.item3:
	// if (possiblyPromptUserIfNoAccount(this, mainBackground)) {
	// showHideAddUser(this, true);
	// }
	// return true;
	// default:
	// return super.onOptionsItemSelected(item);
	// }
	// }

	// ------------------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// This is necessary to enable a context menu
		// getMenuInflater().inflate(R.menu.activity_main, menu);
		ImageContextMenu.show(context, createContextMenu(context));
		return false;
	}

	// ------------------------------------------------------------------------

	/**
	 * Start settings activity.
	 * 
	 * @param context
	 *            the context
	 */
	public static void startSettings(Context context) {
		Intent dialogIntent = new Intent(context, Setup.class);
		dialogIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
				| Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
		context.startActivity(dialogIntent);
	}

	// ------------------------------------------------------------------------

	/**
	 * Start account settings for an optional preferred serverId. ServerId can
	 * be -1 for no preference.
	 * 
	 * @param context
	 *            the context
	 * @param serverId
	 *            the server id
	 */
	public static void startAccount(Context context, int serverId) {
		Intent dialogIntent = new Intent(context, Setup.class);
		dialogIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
				| Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
		dialogIntent.putExtra("account", "account");
		dialogIntent.putExtra("serverId", serverId);
		context.startActivity(dialogIntent);
	}

	// ------------------------------------------------------------------------
	// ------------------------------------------------------------------------

	/**
	 * Do refresh.
	 * 
	 * @param context
	 *            the context
	 */
	public static void doRefresh(final Context context) {
		updateLightSensor(context);
		Main.getInstance().updateInfoMessageBlockAsync(context);
		Communicator.updateKeysFromAllServers(context, uidList, true, null);
		Communicator.updatePhonesFromAllServers(context, uidList, true);
		// Force check Internet and account login information
		int serverId = Setup.getServerId(Setup.getNextReceivingServer(context));
		Communicator.haveNewMessagesAndReceive(context, serverId);
		Communicator.receiveNextMessage(context, serverId);
		Setup.updateAttachmentAllServerLimits(context, true);

		Utility.showToastAsync(context, "Refreshing...");
		if (Main.isAlive()) {
			Main.getInstance().mainBackground.postDelayed(new Runnable() {
				public void run() {
					for (int serverId : Setup.getServerIds(context)) {
						if (Setup.isServerAccount(context, serverId)
								&& Setup.isServerActive(context, serverId)) {
							updateUID2Name(context, uidList, serverId);
						}
					}
					if (Main.isAlive()) {
						// We have just resolved the UIDs one line before
						Main.getInstance().rebuildUserlist(context, false);
					}
				}
			}, 4000);
		}
	}

	// ------------------------------------------------------------------------

	/**
	 * Prompts for adding an external SMS user.
	 * 
	 * @param context
	 *            the context
	 */
	public void addExternalSMSUser(final Context context) {
		try {
			final String titleMessage = "Add External SMS Contact";
			final String textMessage = "Typically you add the UIDs of other registered users to your userlist.\n\nAdditionally you can use Cryptocator to communicate with other users that do not have an account but only using unsecure SMS. Therefore Delphino Cryptocator needs to be your default SMS application (see Settings!).\n\nDo you want to add an external SMS contact from your phonebook?";
			new MessageAlertDialog(context, titleMessage, textMessage, " Yes ",
					" No ", null, new MessageAlertDialog.OnSelectionListener() {
						public void selected(int button, boolean cancel) {
							if (!cancel) {
								if (button == 0) {
									// Show the address book for enabling the
									// user to pick
									// a contact, the next method
									// (onActivityResult) will pick up
									// the result.
									Intent intent = new Intent(
											Intent.ACTION_GET_CONTENT);
									intent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
									startActivityForResult(intent, 1);
								}
							}
						}
					}).show();
		} catch (Exception e) {
			// ignore
		}
	}

	// ------------------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onActivityResult(int, int,
	 * android.content.Intent)
	 */
	@SuppressWarnings("deprecation")
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

				// Normalize phone number
				phone = Setup.normalizePhone(phone);
				addUser(this, phone, name, true);
			}
		}
	}

	// ------------------------------------------------------------------------

	/**
	 * Possibly rebuild userlist async from non UI thread.
	 * 
	 * @param context
	 *            the context
	 * @param resolveNames
	 *            the resolve names
	 */
	public static void possiblyRebuildUserlistAsync(final Context context,
			final boolean resolveNames) {
		final Handler mUIHandler = new Handler(Looper.getMainLooper());
		mUIHandler.post(new Thread() {
			@Override
			public void run() {
				super.run();
				final Handler handler = new Handler();
				handler.postDelayed(new Runnable() {
					public void run() {
						if (Main.isVisible()) {
							// Rebuild the list if it was visible
							Main.getInstance().rebuildUserlist(context,
									resolveNames);
						}
					}
				}, 200);
			}
		});
	}

	// ------------------------------------------------------------------------

	/**
	 * Rebuild userlist. resolveUIDs MUST be false if this method is called from
	 * the response of UID2Name! Otherwise this can generate a loop if names
	 * cannot be resolved! As Cryptocator now supports multiple servers but we
	 * can only display one account name, we take the first server!
	 * 
	 * @param context
	 *            the context
	 * @param resolveNames
	 *            the resolve names
	 */
	public void rebuildUserlist(final Context context,
			final boolean resolveNames) {
		try {
			final int serverDefaultId = Setup.getServerDefaultId(context);
			final int myUid = DB.myUid(context, serverDefaultId);
			String myName = Main.UID2Name(context, myUid, true, resolveNames,
					serverDefaultId);

			// setTitle(myName + ", " + myUid + ", " + serverDefaultId);

			if (!(myName.equals("no active account")) || myUid == -1) {
				setTitle(myName);
			} else {
				setTitle("User " + myUid + "");
				// In case we have no name yet, try a login/validate!
				if (serverDefaultId != -1) {
					Setup.login(context, serverDefaultId);
				}
				final Handler mUIHandler = new Handler(Looper.getMainLooper());
				mUIHandler.postDelayed(new Runnable() {
					public void run() {
						setTitle("User " + myUid + "");
						// Also try to download the own username and update the
						// title!
						Main.updateUID2Name(context, myUid,
								new Main.UpdateListener() {
									public void onUpdate(final String data) {
										final Handler mUIHandler = new Handler(
												Looper.getMainLooper());
										mUIHandler.post(new Thread() {
											@Override
											public void run() {
												super.run();
												setTitle("[" + myUid + "] - "
														+ data);
												if (!data.equals("-1")) {
													Utility.loadStringSetting(
															context,
															Setup.SERVER_USERNAME
																	+ serverDefaultId,
															data);
													String myName = Main
															.UID2Name(context,
																	myUid,
																	true,
																	resolveNames);
													if (!(myName
															.equals("no active account"))) {
														setTitle(myName);
													}
												}
											}
										});
									}
								});
					}
				}, 5000);
			}

			mainInnerView = ((LinearLayout) findViewById(R.id.maininnerview));
			mainInnerView.removeAllViews();

			LayoutInflater inflaterInfo = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			LinearLayout infolistitem = (LinearLayout) inflaterInfo.inflate(
					R.layout.infolistitem, null);

			mainInnerView.addView(infolistitem);
			updateInfo(this);

			LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View adduserlistitem = inflater.inflate(R.layout.adduserlistitem,
					null);
			mainInnerView.addView(adduserlistitem);

			// Add user panel
			serverspinner = ((Spinner) findViewById(R.id.serverspinner));
			adduseritem = ((LinearLayout) findViewById(R.id.adduseritem));
			addUserText = ((TextView) adduserlistitem
					.findViewById(R.id.adduser_text));
			addUserName = ((KeyEventEditText) adduserlistitem
					.findViewById(R.id.adduser_name));
			addUserButton = ((Button) adduserlistitem
					.findViewById(R.id.adduser_button));
			addUserText.setVisibility(View.VISIBLE);
			addUserName.setVisibility(View.GONE);
			addUserButton.setVisibility(View.GONE);
			Utility.setBackground(this, adduseritem, R.drawable.dolphins3);
			// This is not visible until we select it from the context menu
			showHideAddUser(context, false);
			addUserName
					.setKeyListener(new KeyEventEditText.KeyEventEditTextEditTextKeyListener() {
						public boolean keyEvent(int keyCode, KeyEvent event) {
							if (keyCode == KeyEvent.KEYCODE_BACK
									&& event.getAction() == KeyEvent.ACTION_UP) {
								showHideAddUser(context, false);
								return true;
							}
							return true;
						}
					});

			addUserButton.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					showHideAddUser(context, false);
					int adduid = Utility.parseInt(addUserName.getText()
							.toString(), -1);
					if (adduid == -1) {
						Utility.showToastAsync(context, "Nobody added.");
						return;
					}
					// Get selected server
					int serverId = Setup.getServerDefaultId(context);
					if (Setup.getServers(context).size() >= 2) {
						serverId = Setup.getServerId((String) serverspinner
								.getSelectedItem());
					}

					// Test is server is active and we have an account here!
					if (!(Setup.isServerActive(context, serverId) && Setup
							.isServerAccount(context, serverId))) {
						Conversation
								.promptInfo(
										context,
										"Cannot Add User",
										"You can only add a user from a server where"
												+ " you have an account. Make also sure that the"
												+ " server is currently not disabled.");
						return;
					}

					int realUid = Setup.getUid(context, adduid, serverId);
					boolean alreadyInList = alreadyInList(realUid, uidList);
					if (realUid >= 0 && !alreadyInList) {
						addUser(context, realUid);
					} else {
						if (alreadyInList) {
							Utility.showToastAsync(context, "user " + adduid
									+ " already in list.");
						} else {
							Utility.showToastAsync(context, "user " + adduid
									+ " not found.");
						}
					}
				}
			});

			// Reload the userlist
			uidList = loadUIDList(context);

			// Log.d("communicator",
			// "MIGRATE UIDLIST NOW: " + Utility.getListAsString(uidList, ","));
			// uidList.addAll(Utility.getListFromString("545529245,545529244,545529246,545529251,545529250",
			// ",", -1));
			// saveUIDList(context, uidList);

			// Resolve names to cache if not already in the cache
			if (resolveNames) {
				for (int uid : uidList) {
					UID2Name(context, uid, false, resolveNames);
				}
			}
			List<UidListItem> fullUidList = buildSortedFullUidList(context,
					uidList, false);

			boolean showMessageServerLabel = ((Utility
					.isOrientationLandscape(context)) && (Setup.getServerIds(
					context).size() > 1));

			boolean lightBack = true;
			for (UidListItem item : fullUidList) {
				String name = item.name;

				// If landscape and more than one message server AND registered
				// user
				// then display @host behind the name!
				if (showMessageServerLabel && item.uid >= 0) {
					int serverId = Setup.getServerId(context, item.uid);
					name += " <font color='#777777'>@ "
							+ Setup.getServerLabel(context, serverId, true)
							+ "</font>"; // not <small>
				}

				String lastMessage = Conversation
						.possiblyRemoveImageAttachments(context,
								item.lastMessage, true, "[ image ]", -1);
				String lastDate = DB.getDateString(item.lastMessageTimestamp,
						false);
				if (lastMessage == null) {
					lastMessage = "[ empty message ]";
				}

				lastMessage = lastMessage.replace("\n", " ").replace("\r", " ");

				int maxWidth = Utility.getScreenWidth(context) - 120;
				lastMessage = Utility.cutTextIntoOneLine(lastMessage, maxWidth,
						21);

				// Toggle light and dark entries
				lightBack = !lightBack;

				addUserLine(context, mainInnerView, name, lastDate,
						lastMessage, item.uid, lightBack);

			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// ------------------------------------------------------------------------

	/**
	 * Show hide add user.
	 * 
	 * @param context
	 *            the context
	 * @param show
	 *            the show
	 */
	private void showHideAddUser(Context context, boolean show) {
		if (show) {
			Setup.updateServerSpinner(context, serverspinner);
			if (Setup.getServers(context).size() < 2) {
				serverspinner.setVisibility(View.GONE);
			}
			adduseritem.setVisibility(View.VISIBLE);
			addUserText.setVisibility(View.GONE);
			addUserName.setVisibility(View.VISIBLE);
			addUserButton.setVisibility(View.VISIBLE);
			addUserName.setText(DEFAULTTEXT);
			addUserName.selectAll();
			// This can be used to show the keyboard explicitly
			addUserName.requestFocus();
			addUserName.postDelayed(new Runnable() {
				public void run() {
					Utility.showKeyboardExplicit(addUserName);
				}
			}, 100);
			addUserName.requestFocus();
			addUserName.postDelayed(new Runnable() {
				public void run() {
					Utility.showKeyboardExplicit(addUserName);
				}
			}, 500);
		} else {
			adduseritem.setVisibility(View.GONE);
			addUserName.postDelayed(new Runnable() {
				public void run() {
					Utility.hideKeyboardExplicit(addUserName);
				}
			}, 100);
		}
	}

	// ------------------------------------------------------------------------

	/**
	 * Adds the user line.
	 * 
	 * @param context
	 *            the context
	 * @param parent
	 *            the parent
	 * @param name
	 *            the name
	 * @param date
	 *            the date
	 * @param lastMessage
	 *            the last message
	 * @param uid
	 *            the uid
	 * @param lightBack
	 *            the light back
	 */
	private void addUserLine(final Context context, LinearLayout parent,
			String name, String date, String lastMessage, final int uid,
			boolean lightBack) {

		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View userlistitemtmp = inflater.inflate(R.layout.userlistitem, null);
		ImageView personicon = (ImageView) userlistitemtmp
				.findViewById(R.id.personicon);

		boolean haveKey = Setup.haveKey(context, uid);
		if (Communicator.getNotificationCount(context, uid) > 0) {
			userlistitemtmp = inflater.inflate(R.layout.userlistitemmsg, null);
			if (haveKey) {
				personicon.setImageResource(R.drawable.personlockmsg);
			} else {
				personicon.setImageResource(R.drawable.personmsg);
			}
		} else {
			if (haveKey) {
				personicon.setImageResource(R.drawable.personlock);
			} else {
				personicon.setImageResource(R.drawable.person);
			}
		}
		if (uid < 0) {
			if (Communicator.getNotificationCount(context, uid) > 0) {
				personicon.setImageResource(R.drawable.personsmsmsg);
			} else {
				personicon.setImageResource(R.drawable.personsms);
			}
		}
		// Make it final
		final View userlistitem = userlistitemtmp;

		TextView userlistName = (TextView) userlistitem
				.findViewById(R.id.userlistname);
		TextView userlistDate = (TextView) userlistitem
				.findViewById(R.id.userlistdate);
		TextView userlistText = (TextView) userlistitem
				.findViewById(R.id.userlisttext);

		if (highContrast) {
			userlistName.setTypeface(null, Typeface.BOLD);
			userlistDate.setTypeface(null, Typeface.BOLD);
			userlistName.setTextColor(TEXTCOLOEWHITE);
			userlistDate.setTextColor(TEXTCOLOEWHITE);
			userlistText.setTextColor(TEXTCOLOEWHITE);
		} else {
			userlistName.setTextColor(TEXTCOLOEWHITEDIMMED);
			userlistDate.setTextColor(TEXTCOLOEWHITEDIMMED);
			userlistText.setTextColor(TEXTCOLOEWHITEDIMMED2);
		}

		userlistName.setText(Html.fromHtml(name));
		userlistDate.setText(date);
		userlistText.setText(lastMessage);

		int backidtmp = R.drawable.darkerback;
		if (lightBack) {
			backidtmp = R.drawable.darkback;
		}
		final int backid = backidtmp;
		Utility.setBackground(this, userlistitem, backid);

		// If a useritem is touched, highlight it for 500ms
		userlistitem.setOnTouchListener(new View.OnTouchListener() {
			@SuppressLint("ClickableViewAccessibility")
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					Utility.setBackground(userlistitem.getContext(),
							userlistitem, R.drawable.lighterback);
					userlistitem.postDelayed(new Runnable() {
						public void run() {
							Utility.setBackground(userlistitem.getContext(),
									userlistitem, backid);
						}
					}, 500);
				}
				return false;
			}
		});

		// If useritem is clicked go to conversation
		userlistitem.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				// Start conversation activity for the chosen user
				Intent dialogIntent = new Intent(context, Conversation.class);
				dialogIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				Conversation.resetValues(uid);
				context.startActivity(dialogIntent);
			}
		});

		// If useritem is LONG clicked open compose/call/edit decision dialog
		userlistitem.setOnLongClickListener(new OnLongClickListener() {
			public boolean onLongClick(View arg0) {
				showHideAddUser(context, false);
				promptUserClick(context, uid);
				return true;
			}
		});

		// Divider between useritems
		LinearLayout.LayoutParams lpDiv1 = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
		LinearLayout.LayoutParams lpDiv2 = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
		LinearLayout div1 = new LinearLayout(context);
		div1.setBackgroundColor(Color.GRAY);
		div1.setLayoutParams(lpDiv1);
		parent.addView(div1);
		parent.addView(userlistitem);
		LinearLayout div2 = new LinearLayout(context);
		div2.setBackgroundColor(Color.BLACK);
		div2.setLayoutParams(lpDiv2);
		final android.view.ViewGroup.LayoutParams params = div1
				.getLayoutParams();
		params.height = 1;
		final android.view.ViewGroup.LayoutParams params2 = div2
				.getLayoutParams();
		params2.height = 2;
		parent.addView(div2);

	}

	// ------------------------------------------------------------------------
	// ------------------------------------------------------------------------

	/**
	 * Save uid list.
	 * 
	 * @param context
	 *            the context
	 * @param uidList
	 *            the uid list
	 */
	public static void saveUIDList(Context context, List<Integer> uidList) {
		String saveString = "";
		for (int uid : uidList) {
			if (saveString.length() > 0) {
				saveString += ",";
			}
			saveString += uid;
		}
		Utility.saveStringSetting(context, "userlist", saveString);
	}

	// ------------------------------------------------------------------------

	/**
	 * Already in list.
	 * 
	 * @param uid
	 *            the uid
	 * @param uidList
	 *            the uid list
	 * @return true, if successful
	 */
	public static boolean alreadyInList(int uid, List<Integer> uidList) {
		for (int uiditem : uidList) {
			Log.d("communicator", "RECEIVE ALREADYINLIST? uiditem=" + uiditem
					+ " =?= " + uid + "= uid");

			if (uiditem == uid) {
				return true;
			}
		}
		return false;
	}

	// ------------------------------------------------------------------------

	/**
	 * Gets the last message.
	 * 
	 * @param context
	 *            the context
	 * @param hostUid
	 *            the host uid
	 * @return the last message
	 */
	public static String getLastMessage(Context context, int hostUid) {
		return Utility.loadStringSetting(context,
				Setup.SETTINGS_USERLISTLASTMESSAGE + hostUid, "");
	}

	// ------------------------------------------------------------------------

	/**
	 * Gets the last message timestamp.
	 * 
	 * @param context
	 *            the context
	 * @param hostUid
	 *            the host uid
	 * @return the last message timestamp
	 */
	public static long getLastMessageTimestamp(Context context, int hostUid) {
		return Utility.loadLongSetting(context,
				Setup.SETTINGS_USERLISTLASTMESSAGETIMESTAMP + hostUid, 0);
	}

	// ------------------------------------------------------------------------

	/**
	 * Update last message. Should be called when sending or receiving or
	 * revoked messages.
	 * 
	 * @param context
	 *            the context
	 * @param hostUid
	 *            the host uid
	 * @param message
	 *            the message
	 * @param timestamp
	 *            the timestamp
	 */
	public static void updateLastMessage(Context context, int hostUid,
			String message, long timestamp) {

		Utility.saveStringSetting(context, Setup.SETTINGS_USERLISTLASTMESSAGE
				+ hostUid, message);
		Utility.saveLongSetting(context,
				Setup.SETTINGS_USERLISTLASTMESSAGETIMESTAMP + hostUid,
				timestamp);
	}

	// ------------------------------------------------------------------------

	/**
	 * Builds the sorted full uid list. The fulluidlist differs from the uid
	 * list in that the uid list just holds the integer (uids) and the full list
	 * also has more information like the last message timestamp or name in
	 * order to sort and display the user items. For fast access typically the
	 * lean, plain uidlist should be used.
	 * 
	 * @param context
	 *            the context
	 * @param uidList
	 *            the uid list
	 * @param sortByName
	 *            the sort by name
	 * @return the list
	 */
	public static List<UidListItem> buildSortedFullUidList(Context context,
			List<Integer> uidList, boolean sortByName) {
		List<UidListItem> returnList = new ArrayList<UidListItem>();
		for (int uid : uidList) {
			UidListItem item = new UidListItem();
			item.uid = uid;
			item.name = UID2Name(context, uid, false);
			item.lastMessage = getLastMessage(context, uid);
			item.lastMessageTimestamp = getLastMessageTimestamp(context, uid);
			returnList.add(item);
		}
		UidListItem.sort(returnList, sortByName);
		return returnList;
	}

	// ------------------------------------------------------------------------

	/**
	 * Load uid list.
	 * 
	 * @param context
	 *            the context
	 * @return the list
	 */
	public static List<Integer> loadUIDList(Context context) {
		List<Integer> uidList = new ArrayList<Integer>();
		String listString = Utility.loadStringSetting(context,
				Setup.SETTINGS_USERLIST, "");
		appendUIDList(context, listString, uidList);
		return uidList;
	}

	// ------------------------------------------------------------------------

	/**
	 * Append uid list.
	 * 
	 * @param context
	 *            the context
	 * @param commaSeparatedListString
	 *            the comma separated list string
	 * @param uidList
	 *            the uid list
	 * @return the list
	 */
	public static List<Integer> appendUIDList(Context context,
			String commaSeparatedListString, List<Integer> uidList) {
		String[] array = commaSeparatedListString.split(",");
		// ArrayList<Integer> returnList = new ArrayList<Integer>();
		LinkedHashSet<Integer> returnList = new LinkedHashSet<Integer>();
		returnList.addAll(uidList);
		for (String uidString : array) {
			int uid = Utility.parseInt(uidString, -1);
			if (uid != -1) {
				returnList.add(uid);
			}
		}
		uidList.clear();
		uidList.addAll(returnList);
		return uidList;
	}

	// ------------------------------------------------------------------------
	// ------------------------------------------------------------------------
	//
	// /**
	// * Ask to delete a user. This method is, e.g., called from the
	// * UserdetailsActivity.
	// *
	// * @param context
	// * the context
	// * @param uid
	// * the uid
	// */
	// public void askToDelete(final Context context, final int uid) {
	// try {
	// final String titleMessage = "Delete "
	// + UID2Name(context, uid, false, true);
	// final String textMessage = "Really delete "
	// + UID2Name(context, uid, false, true)
	// + " and all messages?";
	// new MessageAlertDialog(context, titleMessage, textMessage,
	// " Delete ", " Cancel ", null,
	// new MessageAlertDialog.OnSelectionListener() {
	// public void selected(int button, boolean cancel) {
	// if (!cancel) {
	// if (button == 0) {
	// // delete
	// deleteUser(context, uid);
	// }
	// }
	// }
	// }).show();
	// } catch (Exception e) {
	// // ignore
	// }
	// }

	// ------------------------------------------------------------------------

	/**
	 * Delete a user locally.
	 * 
	 * @param context
	 *            the context
	 * @param uid
	 *            the uid
	 */
	public static void deleteUser(Context context, int uid) {
		int serverId = Setup.getServerId(context, uid);
		DB.removeMappingByHostUid(context, uid);
		DB.deleteUser(context, uid);
		Setup.saveKey(context, uid, null);
		Setup.savePhone(context, uid, null, false);
		Setup.setKeyDate(context, uid, null);
		// Call this twice to also delete the backup AES key!
		Setup.saveAESKey(context, uid, null);
		Setup.saveAESKey(context, uid, null);
		Setup.setAESKeyDate(context, uid, null, DB.TRANSPORT_INTERNET);
		Setup.setAESKeyDate(context, uid, null, DB.TRANSPORT_SMS);
		Utility.saveStringSetting(context, Setup.SETTINGS_USERLISTLASTMESSAGE
				+ uid, null);
		Utility.saveLongSetting(context,
				Setup.SETTINGS_USERLISTLASTMESSAGETIMESTAMP + uid, -1);
		Utility.saveIntSetting(context, "invalidkeycounter" + uid, 0);
		Utility.saveBooleanSetting(context, Setup.OPTION_SMSMODE + uid, false);
		Communicator.setNotificationCount(context, uid, true);
		List<Integer> uidListTmp = Main.loadUIDList(context);
		int index = 0;
		for (int uiditem : uidListTmp) {
			if (uiditem == uid) {
				uidListTmp.remove(index);
				break;
			}
			index++;
		}
		saveUIDList(context, uidListTmp);

		// Do backup to server iff SMS option is on!
		if (Setup.isSMSOptionEnabled(context, serverId)) {
			Setup.backup(context, true, false, serverId);
		}
		if (Main.isAlive()) {
			Main.getInstance().deleteUserFromCurrentList(context, uid);
		}
	}

	// ------------------------------------------------------------------------

	/**
	 * Delete user from current list.
	 * 
	 * @param context
	 *            the context
	 * @param uid
	 *            the uid
	 */
	public void deleteUserFromCurrentList(Context context, int uid) {
		int index = 0;
		for (int uiditem : uidList) {
			if (uiditem == uid) {
				uidList.remove(index);
				break;
			}
			index++;
		}
		rebuildUserlist(context, true);
	}

	// ------------------------------------------------------------------------
	// ------------------------------------------------------------------------

	/**
	 * Adds the user.
	 * 
	 * @param context
	 *            the context
	 * @param phone
	 *            the phone
	 * @param name
	 *            the name
	 * @param manual
	 *            the manual
	 */
	public static void addUser(final Context context, final String phone,
			final String name, final boolean manual) {
		int fakeUID = Setup.getFakeUIDFromPhone(phone);
		Setup.savePhone(context, fakeUID, phone, manual);
		internalAddUserAndRefreshUserlist(context, fakeUID, name);
	}

	// ------------------------------------------------------------------------

	/*
	 * Adds the user.
	 * 
	 * @param context the context
	 * 
	 * @param uid the uid
	 */
	public void addUser(final Context context, final int uid) {
		final int serverId = Setup.getServerId(context, uid);
		final int suid = Setup.getSUid(context, uid);

		String session = Setup.getTmpLoginEncoded(context, serverId);
		if (session == null) {
			// Error resume is automatically done by getTmpLogin, not logged in
			return;
		}

		String url = null;
		url = Setup.getBaseURL(context, serverId) + "cmd=getuser&session="
				+ session + "&val=" + Setup.encUid(context, suid, serverId);
		final String url2 = url;
		// Log.d("communicator", "XXXX REQUEST addUser :" + url2);
		@SuppressWarnings("unused")
		HttpStringRequest httpStringRequest = (new HttpStringRequest(context,
				url2, new HttpStringRequest.OnResponseListener() {
					public void response(String response) {
						// Log.d("communicator",
						// "XXXX RESPONSE1 addUser :"+response);
						if (Communicator.isResponseValid(response)) {
							// Log.d("communicator",
							// "XXXX RESPONSE2 addUser :"+response);
							if (Communicator.isResponsePositive(response)) {
								String responseContent = Communicator
										.getResponseContent(response);
								String responseName = Setup.decText(context,
										responseContent, serverId);
								if (responseContent.equals("-1")
										|| responseName == null) {
									Utility.showToastAsync(context, "User "
											+ suid + " not found.");
								} else {
									internalAddUserAndRefreshUserlist(context,
											uid, responseName);
								}
							} else {
								Utility.showToastAsync(context,
										"Cannot add user " + suid
												+ ". Login failed.");
							}
						} else {
							Utility.showToastAsync(context,
									"Server error. Check internet connection.");
						}
					}
				}));
	}

	// ------------------------------------------------------------------------

	/**
	 * Internal add user and refresh userlist.
	 * 
	 * @param context
	 *            the context
	 * @param uid
	 *            the uid
	 * @param name
	 *            the name
	 */
	private static void internalAddUserAndRefreshUserlist(
			final Context context, int uid, String name) {
		final int serverId = Setup.getServerId(context, uid);
		saveUID2Name(context, uid, name);
		List<Integer> tmpUidList;
		if (Main.isAlive()) {
			tmpUidList = Main.uidList;
		} else {
			tmpUidList = Main.loadUIDList(context);
		}
		tmpUidList.add(uid);
		DB.ensureDBInitialized(context, uidList);
		saveUIDList(context, tmpUidList);

		Communicator.updateKeysFromServer(context, uidList, true, null,
				serverId);
		Communicator.updatePhonesFromServer(context, uidList, true, serverId);

		// Do backup to server iff SMS option is on!
		// this is for privacy/security: the server otherwise would not
		// allow this new added person to download YOUR phone number!
		// but if you have switched on sms option you want to allow exactly
		// this.
		if (Setup.isSMSOptionEnabled(context, serverId)) {
			Setup.backup(context, true, false, serverId);
		}
		possiblyRebuildUserlistAsync(context, true);
	}

	// ------------------------------------------------------------------------
	// ------------------------------------------------------------------------

	/**
	 * Checks if is update name.
	 * 
	 * @param context
	 *            the context
	 * @param uid
	 *            the uid
	 * @return true, if is update name
	 */
	public static boolean isUpdateName(Context context, int uid) {
		// Only update registered users where the option [x] autoupdate is set!
		return (uid >= 0 && Utility.loadBooleanSetting(context,
				Setup.SETTINGS_UPDATENAME + uid,
				Setup.SETTINGS_DEFAULT_UPDATENAME));
	}

	// ------------------------------------------------------------------------

	/**
	 * Sets the update name.
	 * 
	 * @param context
	 *            the context
	 * @param uid
	 *            the uid
	 * @param automaticUpdate
	 *            the automatic update
	 */
	public static void setUpdateName(Context context, int uid,
			boolean automaticUpdate) {
		Utility.saveBooleanSetting(context, Setup.SETTINGS_UPDATENAME + uid,
				automaticUpdate);
	}

	// ------------------------------------------------------------------------

	/**
	 * Checks if is update phone.
	 * 
	 * @param context
	 *            the context
	 * @param uid
	 *            the uid
	 * @return true, if is update phone
	 */
	public static boolean isUpdatePhone(Context context, int uid) {
		// Only update registered users where the option [x] autoupdate is set!
		// OR if this phone number was NOT manually edited (meaning it came from
		// the server --> then we want for example to delete/update it
		return (uid >= 0 && (Utility.loadBooleanSetting(context,
				Setup.SETTINGS_UPDATEPHONE + uid,
				Setup.SETTINGS_DEFAULT_UPDATEPHONE) || !Setup.isPhoneModified(
				context, uid)));
	}

	// ------------------------------------------------------------------------

	/**
	 * Sets the update phone.
	 * 
	 * @param context
	 *            the context
	 * @param uid
	 *            the uid
	 * @param automaticUpdate
	 *            the automatic update
	 */
	public static void setUpdatePhone(Context context, int uid,
			boolean automaticUpdate) {
		Utility.saveBooleanSetting(context, Setup.SETTINGS_UPDATEPHONE + uid,
				automaticUpdate);
	}

	// ------------------------------------------------------------------------
	// ------------------------------------------------------------------------

	/**
	 * Save ui d2 name.
	 * 
	 * @param context
	 *            the context
	 * @param uid
	 *            the uid
	 * @param name
	 *            the name
	 */
	public static void saveUID2Name(Context context, int uid, String name) {
		Utility.saveStringSetting(context, "uid2name" + uid, name);
	}

	// ------------------------------------------------------------------------

	/**
	 * UI d2 name.
	 * 
	 * @param context
	 *            the context
	 * @param uid
	 *            the uid
	 * @param fullNameWithUID
	 *            the full name with uid
	 * @return the string
	 */
	public static String UID2Name(Context context, int uid,
			boolean fullNameWithUID) {
		return UID2Name(context, uid, fullNameWithUID, false);
	}

	/**
	 * UI d2 name.
	 * 
	 * @param context
	 *            the context
	 * @param uid
	 *            the uid
	 * @param fullNameWithUID
	 *            the full name with uid
	 * @param resolve
	 *            the resolve
	 * @return the string
	 */
	public static String UID2Name(Context context, int uid,
			boolean fullNameWithUID, boolean resolve) {
		int serverDefaultId = Setup.getServerDefaultId(context);
		return UID2Name(context, uid, fullNameWithUID, resolve, serverDefaultId);
	}

	/**
	 * UI d2 name for a specific server
	 * 
	 * @param context
	 *            the context
	 * @param uid
	 *            the uid
	 * @param fullNameWithUID
	 *            the full name with uid
	 * @param resolve
	 *            the resolve
	 * @param serverId
	 *            the server id
	 * @return the string
	 */
	public static String UID2Name(Context context, int uid,
			boolean fullNameWithUID, boolean resolve, int serverId) {
		String myName = Utility.loadStringSetting(context,
				Setup.SERVER_USERNAME + serverId, "");
		int myUid = Utility.parseInt(Utility.loadStringSetting(context,
				Setup.SERVER_UID + serverId, ""), -1);

		String name = Utility.loadStringSetting(context, "uid2name" + uid, "");
		int suid = Setup.getSUid(context, uid);

		if (uid == myUid || uid == DB.myUid()) {
			String suidString;
			if (uid == DB.myUid() && myName.length() > 0) {
				suidString = Utility.loadStringSetting(context,
						Setup.SERVER_UID + serverId, "");
				name = myName;
			} else if (uid == myUid && uid != -1) {
				suidString = uid + "";
				name = myName;
			} else {
				return "no active account";
			}
			if (fullNameWithUID) {
				String serverLabel = Setup.getServerLabel(context, serverId,
						false);
				if (serverLabel.length() > 0) {
					serverLabel = " @ " + serverLabel;
				}
				return name + "  [ " + suidString + " ]" + serverLabel;
			}
			return name;

		}
		if (uid == 0 || suid == 0) {
			return "System";
		}
		if (Utility.parseInt(name, 0) < 0) {
			// if we do not have the name yet, return the uid instead and try to
			// find the name async for the next time!
			if (resolve) {
				updateUID2Name(context, uid, null);
			}
			return "User " + uid;
		} else if (name.equals("")) {
			// if we do not have the name yet, return the uid instead and try to
			// find the name async for the next time!
			if (resolve) {
				updateUID2Name(context, uid, null);
			}
			return "User " + uid + "";
		} else {
			if (fullNameWithUID) {
				int serverIdOther = Setup.getServerId(context, uid);
				;
				String serverLabel = Setup.getServerLabel(context,
						serverIdOther, false);
				if (serverLabel.length() > 0) {
					serverLabel = " @ " + serverLabel;
				}
				return name + "  [ " + suid + " ]" + serverLabel;
			} else {
				return name;
			}
		}
	}

	// ------------------------------------------------------------------------

	/**
	 * Update a single cached name for a UID.
	 * 
	 * @param context
	 *            the context
	 * @param uid
	 *            the uid
	 * @param updateListener
	 *            the update listener
	 */
	public static void updateUID2Name(final Context context, final int uid,
			final UpdateListener updateListener) {
		if (uid >= 0) {
			int serverId = Setup.getServerId(context, uid);
			updateUID2Name(context, uid, null, null, updateListener, serverId);
		}
	}

	/**
	 * Update all cached names of the UIDs in the list.
	 * 
	 * @param context
	 *            the context
	 * @param uids
	 *            the uids
	 */
	public static void updateUID2Name(final Context context,
			final List<Integer> uids, int serverId) {
		ArrayList<String> encUids = new ArrayList<String>();
		ArrayList<Integer> requestedUids = new ArrayList<Integer>();
		for (int uid : uids) {
			if (uid >= 0 && Setup.getServerId(context, uid) == serverId) {
				requestedUids.add(uid);
				final int suid = Setup.getSUid(context, uid);
				encUids.add(Setup.encUid(context, suid, serverId));
			}
		}
		updateUID2Name(context, -1, Utility.getListAsString(encUids, "#"),
				requestedUids, null, serverId);
	}

	/**
	 * Update ui d2 name.
	 * 
	 * @param context
	 *            the context
	 * @param uidSingleLookup
	 *            the uid single lookup
	 * @param uidListAsString
	 *            the uid list as string
	 * @param uidList
	 *            the uid list
	 * @param updateListener
	 *            the update listener
	 */
	private static void updateUID2Name(final Context context,
			final int uidSingleLookup, final String uidListAsString,
			final List<Integer> uidList, final UpdateListener updateListener,
			final int serverId) {
		if ((uidListAsString == null || uidListAsString.length() == 0)
				&& (uidSingleLookup == -1)) {
			// nobody in the list, nobody to look up
			return;
		}

		String uidListAsStringEncoded = "";
		if (uidListAsString != null) {
			uidListAsStringEncoded = Utility.urlEncode(uidListAsString);
		} else {
			final int suid = Setup.getSUid(context, uidSingleLookup);
			uidListAsStringEncoded = Setup.encUid(context, suid, serverId) + "";
		}

		String session = Setup.getTmpLoginEncoded(context, serverId);
		if (session == null) {
			// error resume is automatically done by getTmpLogin, not logged in
			return;
		}

		String url = null;
		url = Setup.getBaseURL(context, serverId) + "cmd=getuser&session="
				+ session + "&val=" + uidListAsStringEncoded;
		final String url2 = url;
		Log.d("communicator", "REQUEST USERNAMES: " + url);
		@SuppressWarnings("unused")
		HttpStringRequest httpStringRequest = (new HttpStringRequest(context,
				url2, new HttpStringRequest.OnResponseListener() {
					public void response(String response) {
						if (Communicator.isResponseValid(response)) {
							if (Communicator.isResponsePositive(response)) {
								String responseContent = Communicator
										.getResponseContent(response);
								// response is
								// name1#name2#name3#-1#name5...
								if (uidList == null && uidSingleLookup != -1) {
									// SINGLE LOOKUP
									String newName = Setup.decText(context,
											responseContent, serverId);
									if (Main.isUpdateName(context,
											uidSingleLookup) && newName != null) {
										saveUID2Name(context, uidSingleLookup,
												newName);
										// it is important to NOT resolve names
										// again,
										// if some could not be resolved!
										// otherwise this will
										// get a live-lock-loop!!!
										possiblyRebuildUserlistAsync(context,
												false);
									}
									if (updateListener != null
											&& newName != null) {
										updateListener.onUpdate(newName);
									}
								} else {
									// MULTIPLE LOOKUP
									List<String> names = Utility
											.getListFromString(responseContent,
													"#");
									for (int i = 0; i < names.size(); i++) {
										String name = names.get(i);
										String newName = Setup.decText(context,
												name, serverId);
										int uid = uidList.get(i);
										if (Main.isUpdateName(context, uid)
												&& newName != null) {
											saveUID2Name(context, uid, newName);
										}
									}
									// it is important to NOT resolve names
									// again,
									// if some could not be resolved!
									// otherwise this will
									// get a live-lock-loop!!!
									possiblyRebuildUserlistAsync(context, false);
								}
							}
						}
					}
				}));
	}

	// ------------------------------------------------------------------------
	// ------------------------------------------------------------------------

	/**
	 * Update info message block async from non UI thread.
	 * 
	 * @param context
	 *            the context
	 */
	public void updateInfoMessageBlockAsync(final Context context) {
		final Handler mUIHandler = new Handler(Looper.getMainLooper());
		mUIHandler.post(new Thread() {
			@Override
			public void run() {
				super.run();
				updateInfo(context);
			}
		});

	}

	/**
	 * Update info.
	 * 
	 * @param context
	 *            the context
	 */
	public void updateInfo(final Context context) {
		String message = null;
		if (Communicator.internetFailCnt > Communicator.INTERNETFAILCNTBARRIER) {
			message = "No Internet Connection or Server Error";
		} else if (Setup.noAccountYet(context)) {
			message = "No Account Defined Yet";
		} else if (Communicator.accountNotActivated) {
			message = "Account Not Activated - Check Your Email";
		} else if (Communicator.loginFailCnt > Communicator.LOGINFAILCNTBARRIER) {
			message = "Login Error - Check Account Settings";
		} else if (!Setup.isSMSDefaultApp(context, false)
				&& (Setup.isSMSDefaultApp(context, true))) {
			message = "Cryptocator not Default SMS App anymore - Check Settings";
		}

		// Log.d("communicator", "#### setInfok=" + message);
		LinearLayout infolistitem = (LinearLayout) mainInnerView
				.findViewById(R.id.infolistitem);
		TextView infolistitemtext = ((TextView) infolistitem
				.findViewById(R.id.infolistitemtext));
		infolistitemtext.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				showHideAddUser(context, false);
				// show context menu
				openOptionsMenu();
			}
		});

		if (infolistitem != null) {
			if (message == null || message.length() == 0) {
				infolistitem.setVisibility(View.GONE);
				infolistitemtext.setText("");
			} else {
				infolistitem.setVisibility(View.VISIBLE);
				infolistitemtext.setText(message);
			}
		}
	}

	// ------------------------------------------------------------------------
	// ------------------------------------------------------------------------

	/**
	 * Gets the name from address book if available, otherwise it will return
	 * the phone number itself.
	 * 
	 * @param context
	 *            the context
	 * @param phone
	 *            the phone
	 * @return the name from address book
	 */
	public static String getNameFromAddressBook(Context context, String phone) {
		try {
			ContentResolver contentResolver = context.getContentResolver();
			Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI,
					Uri.encode(phone));
			Cursor cursor = contentResolver
					.query(uri, new String[] { PhoneLookup.DISPLAY_NAME },
							null, null, null);
			if (cursor == null) {
				return phone;
			}
			String contactName = null;
			if (cursor.moveToFirst()) {
				contactName = cursor.getString(cursor
						.getColumnIndex(PhoneLookup.DISPLAY_NAME));
			}
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}
			return contactName;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return phone;
	}

	// ------------------------------------------------------------------------
	// ------------------------------------------------------------------------

	/**
	 * Prompt user click.
	 * 
	 * @param context
	 *            the context
	 * @param uid
	 *            the uid
	 */
	public static void promptUserClick(final Context context, final int uid) {
		final boolean havePhone = Setup.havePhone(context, uid);
		String title = UID2Name(context, uid, true);
		String text = null;

		new MessageAlertDialog(context, title, text, null, null, " Cancel ",
				new MessageAlertDialog.OnSelectionListener() {
					public void selected(int button, boolean cancel) {
						// nothing
					}
				}, new MessageAlertDialog.OnInnerViewProvider() {

					public View provide(final MessageAlertDialog dialog) {

						LinearLayout outerLayout = new LinearLayout(context);
						outerLayout.setOrientation(LinearLayout.VERTICAL);
						
						LinearLayout infoTextBoxInner = new LinearLayout(context);
						LinearLayout.LayoutParams lpInfoTextBoxInner = new LinearLayout.LayoutParams(
								LinearLayout.LayoutParams.MATCH_PARENT,
								LinearLayout.LayoutParams.WRAP_CONTENT);
						infoTextBoxInner.setLayoutParams(lpInfoTextBoxInner);
						infoTextBoxInner.setGravity(Gravity.CENTER_HORIZONTAL);

						TextView infoText = new TextView(context);
						LinearLayout.LayoutParams lpInfoText = new LinearLayout.LayoutParams(
								LinearLayout.LayoutParams.WRAP_CONTENT,
								LinearLayout.LayoutParams.WRAP_CONTENT);
						lpInfoText.setMargins(25, 30, 25, 8);
						infoText.setLayoutParams(lpInfoText);
						infoText.setTextColor(Color.GRAY);
						infoText.setTextSize(13);
						infoTextBoxInner.addView(infoText);
						
						// Session information
						String sessionInfo = "";
						long keyCreatedTSInternet = Setup.getAESKeyDate(context, uid,
								DB.TRANSPORT_INTERNET);
						long keyCreatedTS = keyCreatedTSInternet;
						long keyCreatedTSSMS = Setup.getAESKeyDate(context, uid,
								DB.TRANSPORT_SMS);
						if ((keyCreatedTSSMS != 0) && (keyCreatedTSInternet == 0)) {
							keyCreatedTS = keyCreatedTSSMS;
						}
						long keyOutdatedTS = keyCreatedTS + Setup.AES_KEY_TIMEOUT_SENDING;
						sessionInfo += "Current Session Key: " + MessageDetailsActivity.getSessionKeyDisplayInfo(context, uid) + "\n";
						sessionInfo += "Session Key Created: " + DB.getDateString(keyCreatedTS, true) + "\n";
						sessionInfo += "Session Key Expires: " + DB.getDateString(keyOutdatedTS, true);
						if (uid >= 0) {
							// Only display info for registered users
							infoText.setText(sessionInfo);
						} else {
							infoText.setText("No registered user.\nNo encryption available.");
						}
						
						
						LinearLayout buttonLayout = new LinearLayout(context);
						buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
						buttonLayout.setGravity(Gravity.CENTER_HORIZONTAL);

						LinearLayout.LayoutParams lpButtons = new LinearLayout.LayoutParams(
								130, 140);
						lpButtons.setMargins(2, 20, 2, 30);

						ImageLabelButton composeButton = new ImageLabelButton(
								context);
						if (uid >= 0) {
							composeButton.setTextAndImageResource("Compose",
									R.drawable.compose);
						} else {
							composeButton.setTextAndImageResource("Compose",
									R.drawable.composesms);
						}
						composeButton.setLayoutParams(lpButtons);
						composeButton
								.setOnClickListener(new View.OnClickListener() {
									public void onClick(View v) {
										// Compose a message to user
										composeMessage(context, "", "[" + uid
												+ "]");
										dialog.dismiss();
									}
								});

						ImageLabelButton callButton = new ImageLabelButton(
								context);
						if (uid >= 0) {
							callButton.setTextAndImageResource("Call",
									R.drawable.call);
						} else {
							callButton.setTextAndImageResource("Call",
									R.drawable.callsms);
						}
						callButton.setLayoutParams(lpButtons);
						callButton
								.setOnClickListener(new View.OnClickListener() {
									public void onClick(View v) {
										// Call user
										callUser(context, uid);
										dialog.dismiss();
									}
								});

						ImageLabelButton editButton = new ImageLabelButton(
								context);
						if (uid >= 0) {
							editButton.setTextAndImageResource("Edit",
									R.drawable.buttonedit);
						} else {
							editButton.setTextAndImageResource("Edit",
									R.drawable.buttoneditsms);
						}
						editButton.setLayoutParams(lpButtons);
						editButton
								.setOnClickListener(new View.OnClickListener() {
									public void onClick(View v) {
										// Edit user
										editUser(context, uid);
										dialog.dismiss();
									}
								});

						buttonLayout.addView(composeButton);
						if (havePhone) {
							buttonLayout.addView(callButton);
						}
						buttonLayout.addView(editButton);

						
						outerLayout.addView(infoTextBoxInner);
						outerLayout.addView(buttonLayout);
						return outerLayout;
					}
				}).show();
	}

	// ------------------------------------------------------------------------

	/**
	 * Compose a message, leave text or phone blank to load the previously saved
	 * draft values. Set them to override these.
	 * 
	 * @param context
	 *            the context
	 * @param text
	 *            the text
	 * @param phone
	 *            the phone
	 */
	public static void composeMessage(Context context, String text, String phone) {
		if (text != null) {
			Utility.saveStringSetting(context, "cachedraftcompose", text);
		}
		if (phone != null) {
			Utility.saveStringSetting(context, "cachedraftcomposephone", phone);
		}
		Intent dialogIntent = new Intent(context, ConversationCompose.class);
		dialogIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(dialogIntent);
	}

	// ------------------------------------------------------------------------

	/**
	 * Edits the user using the {@link UserDetailsActivity}.
	 * 
	 * @param context
	 *            the context
	 * @param uid
	 *            the uid
	 */
	public static void editUser(Context context, int uid) {
		Intent dialogIntent = new Intent(context, UserDetailsActivity.class);
		dialogIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		UserDetailsActivity.uid = uid;
		context.startActivity(dialogIntent);
	}

	// ------------------------------------------------------------------------

	/**
	 * Call the user.
	 * 
	 * @param context
	 *            the context
	 * @param uid
	 *            the uid
	 */
	public static void callUser(Context context, int uid) {
		String phone = Setup.getPhone(context, uid);
		Intent callIntent = new Intent(Intent.ACTION_CALL);
		callIntent.setData(Uri.parse("tel:" + phone.trim().toString()));
		callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(callIntent);
	}

	// ------------------------------------------------------------------------
	// ------------------------------------------------------------------------

	/** The instance. */
	private static Main instance = null;

	/** The visible. */
	private static boolean visible = false;

	// ------------------------------------------------------------------------

	/**
	 * Gets the single instance of Main.
	 * 
	 * @return single instance of Main
	 */
	public static Main getInstance() {
		return instance;
	}

	// ------------------------------------------------------------------------

	/**
	 * Checks if this activity is currently visible.
	 * 
	 * @return true, if is visible
	 */
	public static boolean isVisible() {
		return (visible && instance != null);
	}

	// ------------------------------------------------------------------------

	/**
	 * Checks if this activity is alive (and may be visible later). UI updates
	 * should trigger updates in Main if Main is alive, e.g. first line of
	 * conversation list when a new message arrives.
	 * 
	 * @return true, if is alive
	 */
	public static boolean isAlive() {
		return (instance != null && uidList != null);
	}

	// ------------------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	public void onDestroy() {
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
		Main.visible = false;
	}

	// ------------------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onStop()
	 */
	@Override
	public void onStop() {
		final SensorManager manager = (SensorManager) context
				.getSystemService(Context.SENSOR_SERVICE);
		if (lightSensorListener != null) {
			manager.unregisterListener(lightSensorListener);
		}
		super.onStop();
		Main.visible = false;
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
		// This will possibly update the high contrast value if the device has
		// such
		// a sensor and is outside. In this case we display the names and text
		// with a BRIGHT WHITE instead of the default DIMMED WHITE.
		updateLightSensor(this);

		if (getIntent().getBooleanExtra("EXITAPPLICATION", false)) {
			Log.d("communicator", "EXITAPPLICATION ON REQUEST");
			Utility.killOwnProcessDelayed(5000);
			finish();
		}
		Main.visible = true;

		// Only ONCE skip the resume after context menu closure
		if (skipResume) {
			skipResume = false;
			return;

		}

		// Do the following NOT in the UI thread
		final Context context = this;
		new Thread(new Runnable() {
			public void run() {
				try {
					Communicator.updateKeysFromAllServers(context, uidList,
							false, null);
					Communicator.updatePhonesFromAllServers(context, uidList,
							false);
				} catch (Exception e) {
				}
			}
		}).start();

		// WE NEED TO REBUILD THE USERLIST BECAUSE NEW MESSAGES COULD HAVE
		// ARRIVED //
		// WE SHOW THE FIRST LINE - BUT WE DO NOT NEED TO RESOLVE UIDS //
		rebuildUserlist(this, false);

		// Reset error claims
		Setup.setErrorUpdateInterval(context, false);
		Scheduler.reschedule(context, false, false, true);
	}

	// ------------------------------------------------------------------------

	/**
	 * Exits application completely. This triggers onCreate to kill the process.
	 * Only call this if this is really necessary, e.g., when the user changes
	 * its identity.
	 * 
	 * @param context
	 *            the context
	 */
	public static void exitApplication(Context context) {
		Intent intent = new Intent(context, Main.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.putExtra("EXITAPPLICATION", true);
		context.startActivity(intent);
		System.gc();
	}

	// ------------------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onKeyDown(int, android.view.KeyEvent)
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// Prevent closure when addUser text field is visible
		if ((keyCode == KeyEvent.KEYCODE_BACK)
				&& (adduseritem.getVisibility() == View.VISIBLE)) {
			return true;
		} else {
			return super.onKeyDown(keyCode, event);
		}
	}

	// ------------------------------------------------------------------------

	private static SensorEventListener lightSensorListener = null;

	/**
	 * Update light sensor.
	 * 
	 * @param context
	 *            the context
	 */
	public static void updateLightSensor(final Context context) {
		try {

			final SensorManager manager = (SensorManager) context
					.getSystemService(Context.SENSOR_SERVICE);
			Sensor sensor = manager.getDefaultSensor(Sensor.TYPE_LIGHT);

			if (lightSensorListener == null) {
				lightSensorListener = new SensorEventListener() {
					public void onSensorChanged(SensorEvent event) {
						if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
							String value = String.valueOf(event.values[0]);
							final int intValue = (int) (Float.parseFloat(value));
							int maxValue = Utility.loadIntSetting(context,
									"LIGHTMAXVAL", 0);
							if (intValue > 100) {
								if (intValue > maxValue) {
									Utility.saveIntSetting(context, "LIGHTMAXVAL",
											intValue);
									highContrast = true;
								} else {
									// Check if we are above the barrier
									int barrierValue = (highContrastBarrierInPercent * maxValue) / 100;
									if (intValue > barrierValue) {
										if (!highContrast) {
											highContrast = true;
											Main.getInstance().rebuildUserlist(
													context, false);
										}
									} else {
										if (highContrast) {
											highContrast = false;
											Main.getInstance().rebuildUserlist(
													context, false);
										}
									}
								}
							}

							// Only aquire the light sensor value once
							// not constantly
							//manager.unregisterListener(this);
						}

					}

					public void onAccuracyChanged(Sensor sensor, int accuracy) {
					}
				};
			}
			manager.registerListener(lightSensorListener, sensor, SensorManager.SENSOR_DELAY_NORMAL);
		} catch (Exception e) {
			// ignore sensor errors
		}
	}
	// ------------------------------------------------------------------------
}
