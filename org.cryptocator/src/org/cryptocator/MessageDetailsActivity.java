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
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TableRow;
import android.widget.TextView;

/**
 * The MessageDetailsActivity class is responsible for displaying a dialog with
 * the details for a conversation item (message).
 * 
 * @author Christian Motika
 * @since 1.2
 * @date 08/23/2015
 * 
 */
public class MessageDetailsActivity extends Activity {

	/** The conversation item to display information for. */
	public static ConversationItem conversationItem = null;

	/** The host uid. */
	public static int hostUid;

	/** The activity. */
	Activity activity = null;

	/** The context. */
	Context context = null;

	/** The alert dialog. */
	AlertDialog alertDialog = null;

	/**
	 * The cancel flag indicates that no OK button was used to close the dialog
	 * activity.
	 */
	boolean cancel = true;

	/**
	 * The handled flag indicates that a button was used to close the dialog
	 * activity.
	 */
	boolean handled = false;

	/** The starttag. */
	private static final String STARTTAG = "[img ";

	/** The endtag. */
	private static final String ENDTAG = "]";

	/**
	 * The details checkbox for the backup prompt. For every prompt a new one is
	 * created because it is made available by the listener.
	 */
	private CheckBox details;
	/**
	 * The removeImages checkbox for the backup prompt. For every prompt a new
	 * one is created because it is made available by the listener.
	 */
	private CheckBox removeImages;

	// ------------------------------------------------------------------------

	@SuppressLint("InflateParams")
	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		cancel = true;
		handled = false;
		activity = this;
		context = this;

		AlertDialog.Builder builder = new AlertDialog.Builder(activity);

		String activityTitle = "";

		LayoutInflater inflaterInfo = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		LinearLayout dialogLayout = (LinearLayout) inflaterInfo.inflate(
				R.layout.activity_messagedetails, null);
		LinearLayout outerLayout = (LinearLayout) dialogLayout
				.findViewById(R.id.messagedetailsmain);

		// -------------

		TextView from = (TextView) dialogLayout.findViewById(R.id.from);
		TextView to = (TextView) dialogLayout.findViewById(R.id.to);

		TableRow.LayoutParams lpFromTo = new TableRow.LayoutParams(200,
				LayoutParams.WRAP_CONTENT);
		lpFromTo.gravity = Gravity.CENTER_VERTICAL;
		from.setLayoutParams(lpFromTo);
		to.setLayoutParams(lpFromTo);

		TextView fromkey = (TextView) dialogLayout.findViewById(R.id.fromkey);
		TextView tokey = (TextView) dialogLayout.findViewById(R.id.tokey);

		TextView created = (TextView) dialogLayout.findViewById(R.id.created);
		TextView sent = (TextView) dialogLayout.findViewById(R.id.sent);
		TextView sendingreceivingtitle = (TextView) dialogLayout
				.findViewById(R.id.sendingreceivingtitle);
		TextView sendingreceiving = (TextView) dialogLayout
				.findViewById(R.id.sendingreceiving);
		ProgressBar sendingreceivingprogress = (ProgressBar) dialogLayout
				.findViewById(R.id.sendingreceivingprogess); // progress for
																// sending/receiving
		TableRow sendingreceivingparent = (TableRow) dialogLayout
				.findViewById(R.id.sendingreceivingparent);
		TextView serverMessageID = (TextView) dialogLayout
				.findViewById(R.id.serverMessageID);
		TextView received = (TextView) dialogLayout.findViewById(R.id.received);
		TextView receivedtext = (TextView) dialogLayout
				.findViewById(R.id.receivedtext);
		TextView read = (TextView) dialogLayout.findViewById(R.id.read);
		TextView revoked = (TextView) dialogLayout.findViewById(R.id.revoked);

		TextView transport = (TextView) dialogLayout
				.findViewById(R.id.transport);
		TextView encrypted = (TextView) dialogLayout
				.findViewById(R.id.encrypted);
		TextView size = (TextView) dialogLayout.findViewById(R.id.size);

		TextView keycreated = (TextView) dialogLayout
				.findViewById(R.id.keycreated);
		TextView keyexpires = (TextView) dialogLayout
				.findViewById(R.id.keyexpires);
		TextView keyhash = (TextView) dialogLayout.findViewById(R.id.keyhash);
		TableRow keycreatedrow = (TableRow) dialogLayout
				.findViewById(R.id.keycreatedrow);
		TableRow keyexpiresrow = (TableRow) dialogLayout
				.findViewById(R.id.keyexpiresrow);
		TableRow keyhashrow = (TableRow) dialogLayout
				.findViewById(R.id.keyhashrow);

		TableRow attachmentrow1 = (TableRow) dialogLayout
				.findViewById(R.id.attachmentsrow1);
		TableRow attachmentrow2 = (TableRow) dialogLayout
				.findViewById(R.id.attachmentsrow2);
		final Spinner attachmentspinner = (Spinner) dialogLayout
				.findViewById(R.id.attachmentspinner);
		Button buttonsave = (Button) dialogLayout.findViewById(R.id.buttonsave);
		Button buttonshare = (Button) dialogLayout
				.findViewById(R.id.buttonshare);

		// Get updated information about this conversation item
		final ConversationItem updatedItem = DB.getMessage(context,
				conversationItem.localid, hostUid, DB.DEFAULT_MESSAGEPART);
		if (updatedItem == null) {
			alertDialog.dismiss();
			activity.finish();
			return;
		}

		// On the device always user the local id to address messages, even for
		// search or backup purpose
		String msgIdString = "  [ " + updatedItem.localid + " ]";
		activityTitle = "Message Details" + msgIdString;
		if (updatedItem.mid == -1) {
			serverMessageID.setText("N/A");
		} else {
			serverMessageID.setText(updatedItem.mid + "");
		}

		int serverId = -1;
		String fromKeyString = "";
		String toKeyString = "";
		if (updatedItem.me()) {
			// from me
			fromKeyString = "Account Key: " + Setup.getPublicKeyHash(context);
			toKeyString = "Account Key: "
					+ Setup.getKeyHash(context, updatedItem.to);
			serverId = Setup.getServerId(context, updatedItem.to);
		} else {
			// to me
			fromKeyString = "Account Key: "
					+ Setup.getKeyHash(context, updatedItem.from);
			toKeyString = "Account Key: " + Setup.getPublicKeyHash(context);
			serverId = Setup.getServerId(context, updatedItem.from);
		}

		String fromName = Main.UID2Name(context, updatedItem.from, true, false,
				serverId);
		String toName = Main.UID2Name(context, updatedItem.to, true, false,
				serverId);

		int fromNameServerStartIndex = fromName.indexOf("@");
		int toNameServerStartIndex = toName.indexOf("@");

		if (fromNameServerStartIndex > 0) {
			String server = fromName.substring(fromNameServerStartIndex + 1)
					.trim();
			fromKeyString = "Server: " + server + "\n" + fromKeyString;
			fromName = fromName.substring(0, fromNameServerStartIndex);
		}
		if (toNameServerStartIndex > 0) {
			String server = toName.substring(toNameServerStartIndex + 1).trim();
			toKeyString = "Server: " + server + "\n" + toKeyString;
			toName = toName.substring(0, toNameServerStartIndex);
		}

		fromkey.setText(fromKeyString);
		tokey.setText(toKeyString);

		from.setText(fromName);
		to.setText(toName);

		created.setText(DB.getDateString(updatedItem.created, true));
		sent.setText(DB.getDateString(updatedItem.sent, true));

		if (updatedItem.me()) {
			// We send this item
			if (updatedItem.sent > 10) {
				// If this item is sent, then hide sending/receiving!
				sendingreceivingparent.setVisibility(View.GONE);
			} else {
				// We don't have sent everything, display progress bar!
				sendingreceivingtitle.setText("Sending:");
				int percentSent = DB.getPercentSentComplete(context,
						updatedItem.multipartid);
				int numTotal = updatedItem.parts;
				int numSent = (int) Math
						.ceil(((double) percentSent * (double) numTotal) / 100);
				sendingreceivingprogress.setMax(100);
				sendingreceivingprogress.setProgress(percentSent);
				String position = "#"
						+ DB.getPositionInSendingQueue(context,
								updatedItem.transport, updatedItem.localid,
								serverId) + " in Sending Queue";
				// Number of tries
				String tries = updatedItem.tries + "x tried to send";
				if (numTotal > 1) {
					sendingreceiving.setText(position + "\n" + tries + "\n"
							+ percentSent + "%, " + numSent + " / " + numTotal);
				} else {
					sendingreceiving.setText(position + "\n" + tries);
					sendingreceivingprogress.setVisibility(View.GONE);
				}
			}
		} else {
			// We receive this item, so now lookup how much percent we have
			// received yet
			int numReceived = DB.getReceivedMultiparts(context,
					updatedItem.multipartid, updatedItem.from).size();
			if (numReceived == 0 || numReceived >= updatedItem.parts) {
				// This is no multipart message or we have received everything,
				// so hide sending/receiving
				sendingreceivingparent.setVisibility(View.GONE);
			} else {
				// We don't have received everything, display progress bar!
				sendingreceivingtitle.setText("Receiving:");
				sendingreceivingprogress.setMax(100);
				int numTotal = updatedItem.parts;
				int percentReceived = (numReceived * 100) / numTotal;
				if (numTotal > 1) {
					sendingreceivingprogress.setProgress(percentReceived);
					sendingreceiving.setText(percentReceived + "%, "
							+ numReceived + " / " + numTotal + " {"
							+ updatedItem.multipartid + "}");
				} else {
					sendingreceivingprogress.setVisibility(View.GONE);
				}
			}
		}

		if (updatedItem.smsfailed) {
			String failureString = " <font color='DD0000'>" + DB.SMS_FAILED
					+ "</font>";
			sent.setText(Html.fromHtml(failureString));
			sendingreceivingparent.setVisibility(View.GONE);
		}

		if (updatedItem.me()) {
			receivedtext.setText("Delivered: ");
		}

		received.setText(DB.getDateString(updatedItem.received, true));
		read.setText(DB.getDateString(updatedItem.read, true));

		if (updatedItem.read < -10) {
			// decryption failed
			String failureString = " <font color='#DD0000'>"
					+ "DECRYPTION FAILED</font><BR>"
					+ DB.getDateString(Math.abs(updatedItem.read), true);
			read.setText(Html.fromHtml(failureString));
		}

		revoked.setText(DB.getDateString(updatedItem.revoked, true));

		if (updatedItem.transport == DB.TRANSPORT_INTERNET) {
			transport.setText("Internet");
			size.setText(Utility.getKB(updatedItem.text.length()) + " KB");
		} else {
			transport.setText("SMS");
			size.setText(((int) (Math.ceil(((double) updatedItem.text.length())
					/ ((double) Setup.SMS_DEFAULT_SIZE_MULTIPART))))
					+ " SMS");
		}
		if (updatedItem.encrypted) {
			encrypted.setText("Yes");
		} else {
			encrypted.setText("No");
		}

		Log.d("communicator", " KEYUPDATE: localid=" + updatedItem.localid);

		int mid = DB.getLastSentKeyMessage(context, hostUid, false);
		int localid = DB.getLastSentKeyMessage(context, hostUid, true);
		Log.d("communicator", " KEYUPDATE: getLastSentKeyMessage-mid=" + mid
				+ ", getLastSentKeyMessage-localid=" + localid);

		
		long keyCreatedTSInternet = Setup.getAESKeyDate(context, hostUid,
				DB.TRANSPORT_INTERNET);
		long keyCreatedTS = keyCreatedTSInternet;
		long keyCreatedTSSMS = Setup.getAESKeyDate(context, hostUid,
				DB.TRANSPORT_SMS);
		if ((keyCreatedTSSMS != 0) && (keyCreatedTSInternet == 0)) {
			keyCreatedTS = keyCreatedTSSMS;
		}
		long keyOutdatedTS = keyCreatedTS + Setup.AES_KEY_TIMEOUT_SENDING;
		
		keyhash.setText( Setup.getAESKeyHash(context, hostUid) + " - " + getSessionKeyTransportDisplayInfo(context, hostUid));
		keycreated.setText(DB.getDateString(keyCreatedTS, true));
		keyexpires.setText(DB.getDateString(keyOutdatedTS, true));

		if (hostUid < 0) {
			// SMS user only - hide key info, there cannot be any key!
			keycreatedrow.setVisibility(View.GONE);
			keyexpiresrow.setVisibility(View.GONE);
			keyhashrow.setVisibility(View.GONE);
			// Cheat here with the "\n" ... some extra space because for SMS
			// users we do not show the key section!
			size.setText(size.getText().toString() + "\n");
		}

		if (updatedItem.transport == DB.TRANSPORT_INTERNET) {
			builder.setIcon(R.drawable.msginfo);
		} else {
			builder.setIcon(R.drawable.msginfosms);
		}

		int numImages = getImageAttachmentNumber(context, updatedItem.text);
		if (numImages > 0) {
			if (numImages == 1) {
				// We do not need the spinner, there is just one image
				attachmentspinner.setVisibility(View.GONE);
			}
			String images[] = new String[numImages];
			for (int i = 0; i < numImages; i++) {
				images[i] = "Image " + (i + 1);
			}
			ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(
					this, android.R.layout.simple_spinner_item, images);
			spinnerArrayAdapter
					.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			attachmentspinner.setAdapter(spinnerArrayAdapter);
			buttonsave.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					int number = attachmentspinner.getSelectedItemPosition();
					String encodedImg = extractImageAttachment(context,
							updatedItem.text, number);
					Conversation.saveImageInGallery(context, encodedImg, false,
							Conversation.buildImageTitleAddition(context,
									updatedItem),
							Conversation.buildImageDescription(context,
									updatedItem));
				}
			});
			buttonshare.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					int number = attachmentspinner.getSelectedItemPosition();
					String encodedImg = extractImageAttachment(context,
							updatedItem.text, number);
					Conversation.shareImage(context, encodedImg);
				}
			});
		} else {
			attachmentrow1.setVisibility(View.GONE);
			attachmentrow2.setVisibility(View.GONE);
		}

		int hostUidTmp = updatedItem.from;
		if (updatedItem.me()) {
			hostUidTmp = updatedItem.to;
		}
		final int hostUid = hostUidTmp;

		ImageLabelButton buttonbackup = (ImageLabelButton) dialogLayout
				.findViewById(R.id.buttonbackup);
		buttonbackup.setMarginUp(3);
		buttonbackup.setMarginDown(-7);
		buttonbackup.setTextAndImageResource("Backup", R.drawable.btnbackupsm);
		ImageLabelButton buttonclear = (ImageLabelButton) dialogLayout
				.findViewById(R.id.buttonclear);
		buttonclear.setMarginUp(3);
		buttonclear.setMarginDown(-7);
		buttonclear.setTextAndImageResource("Clear", R.drawable.btnclear);

		buttonbackup.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				final List<ConversationItem> conversationList = new ArrayList<ConversationItem>();
				DB.loadConversation(context, hostUid, conversationList, -1);

				DB.loadConversation(context, hostUid, conversationList, -1);
				int messagesToBackup = 0;
				for (messagesToBackup = 0; messagesToBackup < conversationList
						.size(); messagesToBackup++) {
					if (conversationList.get(messagesToBackup).localid == updatedItem.localid) {
						messagesToBackup++;
						break;
					}
				}

				new MessageAlertDialog(
						context,
						"Backup Messages",
						"Backup all "
								+ messagesToBackup
								+ " older messages up to this one to the Clipboard?",
						"Backup", null, "Abort",
						new MessageAlertDialog.OnSelectionListener() {
							public void selected(int button, boolean cancel) {
								if (!cancel && button == 0) {
									// Backup all message up & including to this
									// one
									BackupActivity.doBackup(context, 0,
											updatedItem.localid,
											conversationList,
											details.isChecked(),
											removeImages.isChecked());
								}
							}
						}, new MessageAlertDialog.OnInnerViewProvider() {
							public View provide(MessageAlertDialog dialog) {
								LinearLayout.LayoutParams lpDetails = new LinearLayout.LayoutParams(
										LayoutParams.WRAP_CONTENT,
										LayoutParams.WRAP_CONTENT);
								lpDetails.setMargins(20, 0, 20, 0);
								LinearLayout.LayoutParams lpRemoveImages = new LinearLayout.LayoutParams(
										LayoutParams.WRAP_CONTENT,
										LayoutParams.WRAP_CONTENT);
								lpRemoveImages.setMargins(20, 0, 20, 20);
								LinearLayout checkBoxLayout = new LinearLayout(
										activity);
								checkBoxLayout
										.setOrientation(LinearLayout.VERTICAL);

								details = new CheckBox(context);
								details.setText("Include Message Details");
								details.setLayoutParams(lpDetails);

								removeImages = new CheckBox(context);
								removeImages.setText("Remove Images");
								removeImages.setLayoutParams(lpRemoveImages);
								removeImages.setChecked(true);

								checkBoxLayout.addView(details);
								checkBoxLayout.addView(removeImages);
								return checkBoxLayout;
							}
						}).show();
			}
		});
		buttonclear.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {

				final List<ConversationItem> conversationList = new ArrayList<ConversationItem>();
				DB.loadConversation(context, hostUid, conversationList, -1);
				int messagesToDelete = 0;
				for (messagesToDelete = 0; messagesToDelete < conversationList
						.size(); messagesToDelete++) {
					if (conversationList.get(messagesToDelete).localid == updatedItem.localid) {
						messagesToDelete++;
						break;
					}
				}

				new MessageAlertDialog(
						context,
						"Clear Messages",
						"Really clear all "
								+ messagesToDelete
								+ " older messages up to and including this one?",
						"Clear", null, "Abort",
						new MessageAlertDialog.OnSelectionListener() {
							public void selected(int button, boolean cancel) {
								if (!cancel && button == 0) {
									// Clear message up to & including this one
									int numCleared = DB.clearSelective(context,
											hostUid, (-1 * updatedItem.localid)
													+ "");

									if (numCleared > 0) {
										Utility.showToastAsync(context,
												+numCleared
														+ " messages cleared.");
										// We close and have to refresh, because
										// this message has been gone now!
										if (Conversation.isAlive()) {
											Conversation.getInstance()
													.rebuildConversation(
															context, 200);
										}
										alertDialog.dismiss();
										activity.finish();
									} else {
										Utility.showToastAsync(context,
												"Nothing cleared.");
									}
								}
							}
						}).show();
			}
		});

		// -------------

		builder.setTitle(activityTitle);

		outerLayout.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// dialog.dismiss();
				alertDialog.dismiss();
				activity.finish();
			}
		});

		builder.setView(dialogLayout);

		builder.setOnCancelListener(new OnCancelListener() {
			public void onCancel(DialogInterface dialog) {
				// dialog.dismiss();
				alertDialog.dismiss();
				activity.finish();
			}
		});

		alertDialog = builder.show();

		// Grab the window of the dialog, and change the width
		// WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
		// Window window = alertDialog.getWindow();
		// lp.copyFrom(window.getAttributes());
		// This makes the dialog take up the full width
		// lp.width = WindowManager.LayoutParams.MATCH_PARENT;
		// lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
		// window.setAttributes(lp);

		Utility.setBackground(context, outerLayout, R.drawable.dolphins3light);
	}

	// ------------------------------------------------------------------------

	/**
	 * Gets the session key display info for the user details dialog and the
	 * user prompt dialog.
	 * 
	 * @param hostUid
	 *            the host uid
	 * @return the session key display info
	 */
	public static String getSessionKeyTransportDisplayInfo(Context context, int hostUid) {
		String forTransport = "";
		long keyCreatedTSInternet = Setup.getAESKeyDate(context, hostUid,
				DB.TRANSPORT_INTERNET);
		long keyCreatedTSSMS = Setup.getAESKeyDate(context, hostUid,
				DB.TRANSPORT_SMS);
		if (keyCreatedTSInternet != 0 && keyCreatedTSSMS != 0) {
			forTransport = "Internet + SMS";
		} else if (keyCreatedTSInternet != 0) {
			forTransport = "Internet";
		} else if (keyCreatedTSSMS != 0) {
			forTransport = "SMS";
		} else {
			forTransport = "";
		}
		return forTransport;
	}

	// ------------------------------------------------------------------------

	/**
	 * Gets the number or image attachments in the message text.
	 * 
	 * @param context
	 *            the context
	 * @param text
	 *            the text
	 * @return the image attachment number
	 */
	public static int getImageAttachmentNumber(Context context, String text) {
		String[] imageParts = text.split("\\[img");
		if (imageParts == null) {
			return 0;
		}
		return Math.max(imageParts.length - 1, 0);
	}

	// ------------------------------------------------------------------------

	/**
	 * Auto save all images of this message and return the number of images
	 * saved.
	 * 
	 * @param context
	 *            the context
	 * @param messagText
	 *            the messag text
	 * @return the int
	 */
	public static int autoSaveAllImages(Context context, String messageText,
			ConversationItem conversationItem) {
		int num = getImageAttachmentNumber(context, messageText);
		int saved = 0;
		if (num > 0) {
			for (int curNum = 0; curNum < num; curNum++) {
				String encodedImg = extractImageAttachment(context,
						messageText, curNum);
				if (Conversation.saveImageInGallery(context, encodedImg, true,
						Conversation.buildImageTitleAddition(context,
								conversationItem), Conversation
								.buildImageDescription(context,
										conversationItem))) {
					saved++;
				}
			}
		}
		if (saved > 0) {
			Utility.showToastAsync(context, "Auto-saved " + saved
					+ " image attachments to gallery.");
		}
		return saved;
	}

	// ------------------------------------------------------------------------

	/**
	 * Extract image attachment as BASE64 encoded image.
	 * 
	 * @param context
	 *            the context
	 * @param text
	 *            the text
	 * @param number
	 *            the number
	 * @return the string
	 */
	public static String extractImageAttachment(Context context, String text,
			int number) {
		int start = text.indexOf(STARTTAG);

		if (start < 0) {
			// No images to remove
			return null;
		}

		// The total text is larger than the attachment limit and there are
		// images included. So we now need to erase these...
		boolean done = false;
		start = 0;
		int end = 0;
		while (!done) {
			// Search for start tag
			start = text.indexOf(STARTTAG, end);
			if (start == -1) {
				// Not further start Found
				done = true;
			} else {
				// Found, process this image
				end = text.indexOf(ENDTAG, start) + 1;

				String textImage = text.substring(start, end);
				if (number == 0) {
					return textImage.substring(STARTTAG.length(),
							textImage.length() - ENDTAG.length());
				}
				number--;
			}
		}
		return null;
	}

	// ------------------------------------------------------------------------

}