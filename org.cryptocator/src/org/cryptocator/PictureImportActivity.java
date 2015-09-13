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

import java.io.ByteArrayOutputStream;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

/**
 * The PictureImportActivity class is responsible for displaying an import
 * dialog to let the user choose a picture and its size/quality or to let him
 * take a photo in order to insert it into a message.
 * 
 * @author Christian Motika
 * @since 1.2
 * @date 08/23/2015
 * 
 */
public class PictureImportActivity extends Activity {

	/**
	 * The host uid should be set before starting the activity in order to make
	 * estimations on server permitted size.
	 */
	public static int hostUid = -1;

	/** The activity. */
	Activity activity = null;

	/** The context. */
	Context context = null;

	/** The picture import dialog. */
	AlertDialog pictureImportDialog = null;

	/** The sizetextback background. */
	private LinearLayout sizetextback = null;

	/** The cancel flag. Per default it is true unless an OK-button was pressed. */
	boolean cancel = true;

	/**
	 * The handled flag. It tells if a button was pressed or if the dialog was
	 * closed, e.g., using the BACK key.
	 */
	boolean handled = false;

	/** The attachment bitmap needs to be set before showing this dialog. */
	public static Bitmap attachmentBitmap = null;

	// /** The source bitmap. */
	// public static Bitmap sourceBitmap = null;

	/** The quality selection. */
	private RatingBar quality;

	/** The quality text. */
	private TextView qualitytext;

	/** The size text. */
	private TextView sizetext;

	/** The size0. */
	private ImageView size0;

	/** The size0 highlight. */
	private LinearLayout size0highlight;

	/** The size1. */
	private ImageView size1;

	/** The size1 highlight. */
	private LinearLayout size1highlight;

	/** The size2. */
	private ImageView size2;

	/** The size3. */
	private ImageView size3;

	/** The size2 highlight. */
	private LinearLayout size2highlight;

	/** The size3 highlight. */
	private LinearLayout size3highlight;

	/** The selected size. */
	private int selectedSize = 0;

	/** The selected quality. */
	private int selectedQuality = 0;

	/** The highlightback for highlighting the selection. */
	private static int HIGHLIGHTBACK = Color.parseColor("#7700AAFF");

	/** The transparent color for not highlighting the selection. */
	private static int BACKTRANSPARENTCOLOR = Color.parseColor("#00FFFFFF");

	/** The qualitydefault in %. */
	private static int QUALITYDEFAULT = 2;

	/** The sizedefault. */
	private static int SIZEDEFAULT = 0;

	/** The SIZE 0 in pixel. */
	private static int SIZE0 = 150;

	/** The SIZE 1 in pixel. */
	private static int SIZE1 = 250;

	/** The SIZE 2 in pixel. */
	private static int SIZE2 = 400;

	/** The SIZE 3 in pixel. */
	private static int SIZE3 = 800;

	/** The result. */
	private static String result = "";

	/** The picture import selected listener. */
	private static OnPictureImportListener onPictureImportListener = null;

	/** The alert color if attachment size is too large for Internet message. */
	private static int BACKALERTOKCOLOR = Color.parseColor("#8800FF00");

	/** The alert color if attachment size is too large for Internet message. */
	private static int BACKALERTWARN1COLOR = Color.parseColor("#88FFFF00");

	/** The alert color if attachment size is too large for Internet message. */
	private static int BACKALERTWARN2COLOR = Color.parseColor("#88FF0000");

	// ------------------------------------------------------------------------

	/**
	 * The listener interface for receiving onPictureImport events. The class
	 * that is interested in processing a onPictureImport event implements this
	 * interface, and the object created with that class is registered with a
	 * component using the component's
	 * <code>addOnPictureImportListener<code> method. When
	 * the onPictureImport event occurs, that object's appropriate
	 * method is invoked.
	 * 
	 * @see OnPictureImportEvent
	 */
	public interface OnPictureImportListener {

		/**
		 * On import is called when an image is selected for import.
		 * 
		 * @param encodedImage
		 *            the encoded image
		 */
		void onImport(String encodedImage);

		/**
		 * On cancel is called when the dialog is dismissed and no image should
		 * be imported.
		 */
		void onCancel();
	}

	// ------------------------------------------------------------------------

	/**
	 * Sets the on picture import listener.
	 * 
	 * @param onPictureImportListener
	 *            the new on picture import listener
	 */
	public static void setOnPictureImportListener(
			OnPictureImportListener onPictureImportListener) {
		PictureImportActivity.onPictureImportListener = onPictureImportListener;
	}

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

		// ATTENTION: Necessary to see the calling activity in the background!
		// android:theme="@style/Theme.Transparent"

		// The title is set later
		String activityTitle = "";

		LayoutInflater inflaterInfo = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		LinearLayout dialogLayout = (LinearLayout) inflaterInfo.inflate(
				R.layout.activity_pictureimport, null);

		LinearLayout outerLayout = (LinearLayout) dialogLayout
				.findViewById(R.id.pictureimportmain);
		LinearLayout buttonLayout = (LinearLayout) dialogLayout
				.findViewById(R.id.pictureimportbuttons);

		// -------------

		sizetextback = (LinearLayout) dialogLayout
				.findViewById(R.id.sizetextback);

		// Set icon and title of the dialog
		builder.setIcon(R.drawable.pictureimport);
		activityTitle = "Insert Image";

		qualitytext = (TextView) dialogLayout.findViewById(R.id.qualitytext);
		sizetext = (TextView) dialogLayout.findViewById(R.id.sizetext);

		quality = (RatingBar) dialogLayout.findViewById(R.id.quality);
		quality.setMax(6);
		quality.setStepSize(1.0f);
		quality.setProgress(QUALITYDEFAULT);
		quality.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
			public void onRatingChanged(RatingBar ratingBar, float rating,
					boolean fromUser) {
				updateQuality(rating);
			}
		});

		size0 = (ImageView) dialogLayout.findViewById(R.id.size0);
		size0highlight = (LinearLayout) dialogLayout
				.findViewById(R.id.size0highlight);
		size1 = (ImageView) dialogLayout.findViewById(R.id.size1);
		size1highlight = (LinearLayout) dialogLayout
				.findViewById(R.id.size1highlight);
		size2 = (ImageView) dialogLayout.findViewById(R.id.size2);
		size2highlight = (LinearLayout) dialogLayout
				.findViewById(R.id.size2highlight);
		size3 = (ImageView) dialogLayout.findViewById(R.id.size3);
		size3highlight = (LinearLayout) dialogLayout
				.findViewById(R.id.size3highlight);

		size0highlight.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				size0highlight.setBackgroundColor(HIGHLIGHTBACK);
				size1highlight.setBackgroundColor(BACKTRANSPARENTCOLOR);
				size2highlight.setBackgroundColor(BACKTRANSPARENTCOLOR);
				size3highlight.setBackgroundColor(BACKTRANSPARENTCOLOR);
				selectedSize = SIZE0;
				updateTemporaryResult();
			}
		});
		size1highlight.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				size0highlight.setBackgroundColor(BACKTRANSPARENTCOLOR);
				size1highlight.setBackgroundColor(HIGHLIGHTBACK);
				size2highlight.setBackgroundColor(BACKTRANSPARENTCOLOR);
				size3highlight.setBackgroundColor(BACKTRANSPARENTCOLOR);
				selectedSize = SIZE1;
				updateTemporaryResult();
			}
		});
		size2highlight.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				size0highlight.setBackgroundColor(BACKTRANSPARENTCOLOR);
				size1highlight.setBackgroundColor(BACKTRANSPARENTCOLOR);
				size2highlight.setBackgroundColor(HIGHLIGHTBACK);
				size3highlight.setBackgroundColor(BACKTRANSPARENTCOLOR);
				selectedSize = SIZE2;
				updateTemporaryResult();
			}
		});
		size3highlight.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				size0highlight.setBackgroundColor(BACKTRANSPARENTCOLOR);
				size1highlight.setBackgroundColor(BACKTRANSPARENTCOLOR);
				size2highlight.setBackgroundColor(BACKTRANSPARENTCOLOR);
				size3highlight.setBackgroundColor(HIGHLIGHTBACK);
				selectedSize = SIZE3;
				updateTemporaryResult();
			}
		});
		if (SIZEDEFAULT == 0) {
			size0highlight.setBackgroundColor(HIGHLIGHTBACK);
			size1highlight.setBackgroundColor(BACKTRANSPARENTCOLOR);
			size2highlight.setBackgroundColor(BACKTRANSPARENTCOLOR);
			selectedSize = SIZE0;
		} else if (SIZEDEFAULT == 1) {
			size0highlight.setBackgroundColor(BACKTRANSPARENTCOLOR);
			size1highlight.setBackgroundColor(HIGHLIGHTBACK);
			size2highlight.setBackgroundColor(BACKTRANSPARENTCOLOR);
			selectedSize = SIZE1;
		} else {
			size0highlight.setBackgroundColor(BACKTRANSPARENTCOLOR);
			size1highlight.setBackgroundColor(BACKTRANSPARENTCOLOR);
			size2highlight.setBackgroundColor(HIGHLIGHTBACK);
			selectedSize = SIZE2;
		}

		updateQuality(QUALITYDEFAULT);

		Button buttonInsert = (Button) dialogLayout
				.findViewById(R.id.buttonpictureimport);
		Button buttonCancel = (Button) dialogLayout
				.findViewById(R.id.buttoncancel);

		buttonInsert.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (PictureImportActivity.onPictureImportListener != null) {
					PictureImportActivity.onPictureImportListener
							.onImport(result);
				}
				activity.finish();
			}
		});
		buttonCancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				activity.finish();
			}
		});

		// -------------

		builder.setTitle(activityTitle);
		builder.setView(dialogLayout);

		builder.setOnCancelListener(new OnCancelListener() {
			public void onCancel(DialogInterface dialog) {
				// dialog.dismiss();
				activity.finish();
			}
		});

		pictureImportDialog = builder.show();

		// Grab the window of the dialog, and change the width
		WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
		Window window = pictureImportDialog.getWindow();
		lp.copyFrom(window.getAttributes());
		// This makes the dialog take up the full width
		lp.width = WindowManager.LayoutParams.MATCH_PARENT;
		// lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
		window.setAttributes(lp);

		Utility.setBackground(context, outerLayout, R.drawable.dolphins3light);
		Utility.setBackground(context, buttonLayout, R.drawable.dolphins4light);

		updateTemporaryResult();
	}

	// ------------------------------------------------------------------------

	private void updateImages(Context context) {
		// GET request is limited to 2048 bytes => We NEED PIST
		// request
		Drawable drawable0 = getResizedImageAsDrawable(context,
				attachmentBitmap, SIZE0, SIZE0, selectedQuality);
		Drawable drawable1 = getResizedImageAsDrawable(context,
				attachmentBitmap, SIZE1, SIZE1, selectedQuality);
		Drawable drawable2 = getResizedImageAsDrawable(context,
				attachmentBitmap, SIZE2, SIZE2, selectedQuality);
		Drawable drawable3 = getResizedImageAsDrawable(context,
				attachmentBitmap, SIZE3, SIZE3, selectedQuality);

		size0.setImageDrawable(drawable0);
		size1.setImageDrawable(drawable1);
		size2.setImageDrawable(drawable2);
		size3.setImageDrawable(drawable3);
		updateTemporaryResult();
	}

	// ------------------------------------------------------------------------

	/**
	 * Gets the resized image as drawable.
	 * 
	 * @param context
	 *            the context
	 * @param attachmentBitmap
	 *            the attachment path
	 * @param maxWidth
	 *            the max width
	 * @param maxHeight
	 *            the max height
	 * @param quality
	 *            the quality
	 * @return the resized image as drawable
	 */
	public static Drawable getResizedImageAsDrawable(Context context,
			Bitmap bitmap, int maxWidth, int maxHeight, int quality) {
		Log.d("communicator", "OPEN IMAGE:" + attachmentBitmap);
		// byte[] bytes = Utility.getFile(attachmentPath);
		// Bitmap bitmap = Utility.getBitmapFromBytes(bytes);
		Bitmap resizedBitmap = Utility.getResizedImage(bitmap, maxWidth,
				maxHeight, false, false);
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		resizedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream);
		byte[] byteArray = stream.toByteArray();
		bitmap = Utility.getBitmapFromBytes(byteArray);
		Drawable drawable = Utility.getDrawableFromBitmap(context, bitmap);
		return drawable;
	}


	// ------------------------------------------------------------------------

	/**
	 * Update temporary result.
	 */
	private void updateTemporaryResult() {
		result = Utility.getResizedImageAsBASE64String(this, attachmentBitmap,
				selectedSize, selectedSize, selectedQuality, false).replace("\n", "")
				.replace("\r", "");
		int len = result.length();
		int sms = len / Setup.SMS_DEFAULT_SIZE;

		int lenKB = len / 1000;
		String lenKB2 = Utility.getKB(len);

		if (result != null) {
			result = "[img " + result + "]";
		}
		String displayText = "Image will need " + lenKB2 + " KB / " + sms
				+ " SMS.";

		if (pictureImportDialog != null) {
			pictureImportDialog.setTitle("Insert - " + lenKB2 + " KB / " + sms
					+ " SMS");
		}

		sizetextback.setBackgroundColor(BACKALERTOKCOLOR);

		if (sms > (Setup.SMS_SIZE_WARNING / Setup.SMS_DEFAULT_SIZE)) {
			sizetextback.setBackgroundColor(BACKALERTWARN1COLOR);
			// This attachment image will produce more SMS than the current
			// warning limit, so also warn here!
			displayText += "\n\nLarge image: Should not be sent via SMS!";
		}

		int serverId = Setup.getServerId(context, hostUid);

		int limit = Setup.getAttachmentServerLimit(context, serverId);
		String serverLabel = Setup.getServerLabel(context, serverId, true);
		// If limit == -1 => we do not know!
		if (limit != -1) {
			if (limit == 0) {
				sizetextback.setBackgroundColor(BACKALERTWARN2COLOR);
				displayText += "\n\nServer "
						+ serverLabel
						+ " does not allow any attachments. Image will get removed when sent as Internet message.";
			} else if (lenKB > limit) {
				sizetextback.setBackgroundColor(BACKALERTWARN2COLOR);
				displayText += "\n\nServer "
						+ serverLabel
						+ " permits only attachments up to "
						+ limit
						+ " KB. Image will get removed when sent as Internet message.";
			}
		}
		sizetext.setText(displayText);
	}

	// ------------------------------------------------------------------------

	/**
	 * Update quality, update text (%) and images according to this new quality.
	 * 
	 * @param rating
	 *            the rating
	 */
	private void updateQuality(float rating) {
		final int QUALITY0 = 5;
		final int QUALITY1 = 10;
		final int QUALITY2 = 20;
		final int QUALITY3 = 40;
		final int QUALITY4 = 60;
		final int QUALITY5 = 80;

		if (rating <= 1) {
			selectedQuality = QUALITY0;
		} else if (rating <= 2) {
			selectedQuality = QUALITY1;
		} else if (rating <= 3) {
			selectedQuality = QUALITY2;
		} else if (rating <= 4) {
			selectedQuality = QUALITY3;
		} else if (rating <= 5) {
			selectedQuality = QUALITY4;
		} else if (rating <= 6) {
			selectedQuality = QUALITY5;
		}
		qualitytext.setText("Quality: " + selectedQuality + "%");
		updateImages(activity);
	}

	// ------------------------------------------------------------------------

}
