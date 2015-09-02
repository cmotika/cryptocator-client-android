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
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * The ImageContextMenu is a polished nice version of a context menu. Do not forget to add this activity in the AndroidManifest.xml<BR>
 *         <activity
 *            android:name="org.cryptocator.ImageContextMenu"
 *          android:theme="@style/Theme.Transparent" >
 *       </activity>
 * 
 * @author Christian Motika
 * @since 1.2
 * @date 08/23/2015
 */
@SuppressLint("UseSparseArrays")
public class ImageContextMenu extends Activity {

	/** The clicked color. */
	public static int CLICKED_COLOR = Color.parseColor("#33FFFFFF");

	/** The notclicked color. */
	public static int NOTCLICKED_COLOR = Color.parseColor("#00FFFFFF");

	/** The clickduration in ms. */
	public static int CLICKDURATION = 300;

	/** The activity. */
	Activity activity = null;

	/** The context. */
	Context context = null;

	/** The alert dialog. */
	AlertDialog alertDialog = null;

	/** The cancel. */
	boolean cancel = true;

	/** The handled. */
	boolean handled = false;

	/** The current instance. */
	ImageContextMenu instance = null;

	/**
	 * The image context menu provider containing the concrete instance of this
	 * context menu.
	 */
	private static ImageContextMenuProvider imageContextMenuProvider = null;

	// ------------------------------------------------------------------------

	/**
	 * The listener interface for receiving imageContextMenuSelection events.
	 * The class that is interested in processing a imageContextMenuSelection
	 * event implements this interface, and the object created with that class
	 * is registered with a component using the component's
	 * <code>addImageContextMenuSelectionListener<code> method. When
	 * the imageContextMenuSelection event occurs, that object's appropriate
	 * method is invoked.
	 * 
	 * @see ImageContextMenuSelectionEvent
	 */
	public interface ImageContextMenuSelectionListener {

		/**
		 * Called back when the user selects an entry of the context menu.
		 * Return true to close the context menu or false to not close it.
		 * 
		 * @return true, if successful
		 */
		public boolean onSelection(ImageContextMenu instance);
	}

	// ------------------------------------------------------------------------

	/**
	 * The Class ImageContextMenuProvider.
	 */
	public static class ImageContextMenuProvider {

		/** The list of captions. */
		private List<String> captions = new ArrayList<String>();

		/** The icons. */
		private List<Drawable> icons = new ArrayList<Drawable>();

		/** The image context menu selection listeners. */
		private List<ImageContextMenuSelectionListener> imageContextMenuSelectionListeners = new ArrayList<ImageContextMenuSelectionListener>();

		/** The visible. */
		private HashMap<Integer, Boolean> entryVisible = new HashMap<Integer, Boolean>();

		/** The enabled. */
		private HashMap<Integer, Boolean> entryEnabled = new HashMap<Integer, Boolean>();

		/** The context menu title. */
		private String title = null;

		/** The icon. */
		private Drawable icon = null;
		
		/** The context. */
		private Context context;

		// ---------------------------------------------------------------------

		/**
		 * Instantiates a new image context menu provider.
		 * 
		 * @param title
		 *            the title of the context menu or null
		 * @param icon
		 *            the icon of the context menu or null
		 */
		public ImageContextMenuProvider(Context context, String title, Drawable icon) {
			this.context = context;
			this.title = title;
			this.icon = icon;
		}

		// ---------------------------------------------------------------------

		/**
		 * Adds an entry to this context menu returning a handle id for
		 * enabling/disabling or changing visibility of this context menu entry.
		 * 
		 * @param caption
		 *            the caption
		 * @param icon
		 *            the icon
		 * @param imageContextMenuSelectionListener
		 *            the image context menu selection listener
		 */
		public int addEntry(
				String caption,
				int iconResource,
				ImageContextMenuSelectionListener imageContextMenuSelectionListener) {
			captions.add(caption);
			Drawable icon = context.getResources().getDrawable(iconResource);
			icons.add(icon);
			imageContextMenuSelectionListeners
					.add(imageContextMenuSelectionListener);
			int handle = captions.size() - 1;
			entryVisible.put(handle, true);
			entryEnabled.put(handle, true);
			return handle;
		}
		
		// ---------------------------------------------------------------------

		/**
		 * Adds an entry to this context menu returning a handle id for
		 * enabling/disabling or changing visibility of this context menu entry.
		 * 
		 * @param caption
		 *            the caption
		 * @param icon
		 *            the icon
		 * @param imageContextMenuSelectionListener
		 *            the image context menu selection listener
		 */
		public int addEntry(
				String caption,
				Drawable icon,
				ImageContextMenuSelectionListener imageContextMenuSelectionListener) {
			captions.add(caption);
			icons.add(icon);
			imageContextMenuSelectionListeners
					.add(imageContextMenuSelectionListener);
			int handle = captions.size() - 1;
			entryVisible.put(handle, true);
			entryEnabled.put(handle, true);
			return handle;
		}

		// ---------------------------------------------------------------------

		/**
		 * Sets the entry visibility. The handle was returned when the context
		 * menu entry was added.
		 * 
		 * @param handle
		 *            the new visible
		 */
		public void setVisible(int handle, boolean visible) {
			entryVisible.put(handle, visible);
		}

		// ---------------------------------------------------------------------

		/**
		 * Sets the entry visibility. The handle was returned when the context
		 * menu entry was added.
		 * 
		 * @param handle
		 *            the new visible
		 */
		public void setEnabled(int handle, boolean enabled) {
			entryEnabled.put(handle, enabled);
		}

		// ---------------------------------------------------------------------

		/**
		 * Gets the caption of an context menu entry.
		 * 
		 * @param handle
		 *            the handle
		 * @return the caption
		 */
		public String getCaption(int handle) {
			return captions.get(handle);
		}

		// ---------------------------------------------------------------------

		/**
		 * Gets the icon of an context menu entry.
		 * 
		 * @param handle
		 *            the handle
		 * @return the icon
		 */
		public Drawable getIcon(int handle) {
			return icons.get(handle);
		}

		// ---------------------------------------------------------------------

		/**
		 * Checks if context menu entry is visible.
		 * 
		 * @param handle
		 *            the handle
		 * @return true, if is visible
		 */
		public boolean isVisible(int handle) {
			return entryVisible.get(handle);
		}

		// ---------------------------------------------------------------------

		/**
		 * Checks if context menu entry is enabled.
		 * 
		 * @param handle
		 *            the handle
		 * @return true, if is enabled
		 */
		public boolean isEnabled(int handle) {
			return entryEnabled.get(handle);
		}

		// ---------------------------------------------------------------------

	}

	// ------------------------------------------------------------------------

	/**
	 * Sets the image context menu provider. This method MUST be called before
	 * the activity is created/shown.
	 * 
	 * @param imageContextMenuProvider
	 *            the new image context menu provider
	 */
	public static void setImageContextMenuProvider(
			ImageContextMenuProvider imageContextMenuProvider) {
		ImageContextMenu.imageContextMenuProvider = imageContextMenuProvider;
	}

	// ------------------------------------------------------------------------

	/**
	 * Use this method to call the context menu providing an
	 * ImageContextMenuProvider which is mandatory. This provider object
	 * instance can be stored outside it implements the functionality of the
	 * context menu.
	 * 
	 * @param context
	 *            the context
	 * @param imageContextMenuProvider
	 *            the image context menu provider
	 */
	public static void show(Context context,
			ImageContextMenuProvider imageContextMenuProvider) {
		Intent dialogIntent = new Intent(context, ImageContextMenu.class);
		ImageContextMenu.setImageContextMenuProvider(imageContextMenuProvider);
		dialogIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(dialogIntent);
	}

	// ------------------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {

		// Set the instance
		instance = this;

		if (imageContextMenuProvider == null) {
			throw new RuntimeException(
					"An ImageContextMenuProvider needs to be specified before calling"
							+ " ImageContextMenu. You should only open the context menu thru the"
							+ " show() Method of ImageContextMenu.");
		}

		super.onCreate(savedInstanceState);
		cancel = true;
		handled = false;
		activity = this;
		context = this;

		AlertDialog.Builder builder = new AlertDialog.Builder(activity);

		builder.setTitle(imageContextMenuProvider.title);
		builder.setIcon(imageContextMenuProvider.icon);

		LinearLayout.LayoutParams lpEntry = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		lpEntry.setMargins(20, 20, 20, 20);

		LinearLayout.LayoutParams lpCaption = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.MATCH_PARENT, 2);
		lpCaption.setMargins(10, 12, 15, 12);

		LinearLayout.LayoutParams lpIcon = new LinearLayout.LayoutParams(
				70,
				LinearLayout.LayoutParams.WRAP_CONTENT, 1);
		lpIcon.setMargins(25, 12, 10 , 12);

		LinearLayout outerLayout = new LinearLayout(context);
		outerLayout.setOrientation(LinearLayout.VERTICAL);
		outerLayout.setGravity(Gravity.CENTER_HORIZONTAL);

		int max = imageContextMenuProvider.captions.size();
		for (int i = 0; i < max; i++) {
			String caption = imageContextMenuProvider.getCaption(i);
			Drawable icon = imageContextMenuProvider.getIcon(i);
			final ImageContextMenuSelectionListener listener = imageContextMenuProvider.imageContextMenuSelectionListeners
					.get(i);

			final LinearLayout entryLayout = new LinearLayout(context);
			entryLayout.setOrientation(LinearLayout.HORIZONTAL);
			entryLayout.setGravity(Gravity.LEFT);

			// Click selection listener
			entryLayout.setClickable(true);
			entryLayout.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					entryLayout.setBackgroundColor(CLICKED_COLOR);
					final boolean shouldClose = listener.onSelection(instance);
					entryLayout.postDelayed(new Runnable() {
						public void run() {
							entryLayout.setBackgroundColor(NOTCLICKED_COLOR);
							if (shouldClose) {
								//alertDialog.dismiss();
								finish();
							}
						}
					}, CLICKDURATION);
				}
			});

			// Caption
			TextView text = new TextView(context);
			text.setText(caption);
			text.setLayoutParams(lpCaption);
			text.setTextSize(20);
			text.setTextColor(Color.WHITE);
			text.setGravity(Gravity.CENTER_VERTICAL);

			// Icon
			ImageView img = new ImageView(context);
			img.setImageDrawable(icon);
			img.setLayoutParams(lpIcon);

			entryLayout.addView(img);
			entryLayout.addView(text);

			outerLayout.addView(entryLayout);
			
			if (i < max - 1) {
				// Divider between useritems
				LinearLayout.LayoutParams lpDiv1 = new LinearLayout.LayoutParams(
						LinearLayout.LayoutParams.MATCH_PARENT,
						LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
				lpDiv1.setMargins(0, 0, 0, 0);
				LinearLayout div1 = new LinearLayout(context);
				div1.setBackgroundColor(Color.GRAY);
				div1.setLayoutParams(lpDiv1);
				outerLayout.addView(div1);
				android.view.ViewGroup.LayoutParams params = div1
						.getLayoutParams();
				params.height = 1;
			}
			
		}

		builder.setView(outerLayout);
		
		// Show the dialog and populate the alertDialog variable that will be
		// passed to the click listener
		alertDialog = builder.show();

		alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
			public void onCancel(DialogInterface dialog) {
				finish();
			}
		});
		
	}

	// ------------------------------------------------------------------------

}