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
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * The ImageContextMenu is a polished nice version of a context menu. Do not
 * forget to add this activity in the AndroidManifest.xml<BR>
 * <activity android:name="org.cryptocator.ImageContextMenu"
 * android:theme="@style/Theme.Transparent" > </activity>
 * 
 * @author Christian Motika
 * @since 1.2
 * @date 08/23/2015
 */
@SuppressLint("UseSparseArrays")
public class ImageContextMenu extends Activity {

	/** The background color. */
	public static int BACKGROUND = Color.parseColor("#44444444");

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
	static ImageContextMenu instance = null;

	/**
	 * The image context menu provider containing the concrete instance of this
	 * context menu.
	 */
	private static ImageContextMenuProvider imageContextMenuProvider = null;

	/**
	 * The show time when the dialog is shown to be reactive to menu button a
	 * little bit later.
	 */
	private long showTime = 0;

	// ------------------------------------------------------------------------

	/**
	 * The Interface ExtendedEntryViewProvider.
	 */
	interface ExtendedEntryViewProvider {

		/**
		 * Provide view.
		 * 
		 * @param context
		 *            the context
		 * @return the view
		 */
		View provideView(Context context);
	}

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

		/** The views for extended entries. */
		private List<ExtendedEntryViewProvider> viewProviders = new ArrayList<ExtendedEntryViewProvider>();

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

		/** The text size. */
		private int textSize = 20;

		// ---------------------------------------------------------------------

		/**
		 * Instantiates a new image context menu provider.
		 * 
		 * @param title
		 *            the title of the context menu or null
		 * @param icon
		 *            the icon of the context menu or null
		 */
		public ImageContextMenuProvider(Context context, String title,
				Drawable icon) {
			this.context = context;
			this.title = title;
			this.icon = icon;
		}

		// ---------------------------------------------------------------------

		/**
		 * Sets or renewes the title of the context menu.
		 * 
		 * @param title
		 *            the new title
		 */
		public void setTitle(String title) {
			this.title = title;
		}

		// ---------------------------------------------------------------------

		/**
		 * Sets a specific text size.
		 * 
		 * @param textSize
		 *            the new text size
		 */
		public void setTextSize(int textSize) {
			this.textSize = textSize;
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
			viewProviders.add(null);
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
			viewProviders.add(null);
			imageContextMenuSelectionListeners
					.add(imageContextMenuSelectionListener);
			int handle = captions.size() - 1;
			entryVisible.put(handle, true);
			entryEnabled.put(handle, true);
			return handle;
		}

		// ---------------------------------------------------------------------

		/**
		 * Adds an enhanced entry to this context menu returning a handle id for
		 * enabling/disabling or changing visibility of this context menu entry.
		 * An extended entry only contains of an arbitrary view that can be
		 * used, e.g., to display additional information. Caption and icon of
		 * this entry will be null.
		 * 
		 * @param viewProvider
		 *            the view provider
		 * @param imageContextMenuSelectionListener
		 *            the image context menu selection listener
		 * @return the int
		 */
		public int addEntry(
				ExtendedEntryViewProvider viewProvider,
				ImageContextMenuSelectionListener imageContextMenuSelectionListener) {
			captions.add(null);
			icons.add(null);
			viewProviders.add(viewProvider);
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
		 * Gets the caption of an context menu entry or null if this is an
		 * extended entry.
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
		 * Gets the icon of an context menu entry or null if this is an extended
		 * entry.
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
		 * Gets the view of an extended context menu entry or null if this is
		 * not an extended entry.
		 * 
		 * @param handle
		 *            the handle
		 * @return the icon
		 */
		public ExtendedEntryViewProvider getViewProvider(int handle) {
			return viewProviders.get(handle);
		}

		// ---------------------------------------------------------------------

		/**
		 * Gets the view of an extended context menu entry or null if this is
		 * not an extended entry.
		 * 
		 * @param handle
		 *            the handle
		 * @return the icon
		 */
		public boolean setViewProvider(int handle, ExtendedEntryViewProvider viewProvider) {
			try {
				viewProviders.remove(handle);
				viewProviders.add(handle, viewProvider);
			} catch (Exception e) {
				return false;
			}
			return true;
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

	/**
	 * Closes the context menu if it is visible.
	 */
	public static void close() {
		if (visible()) {
			instance.finish();
		}
	}

	// ------------------------------------------------------------------------

	/**
	 * Tells if the context menu is visible.
	 * 
	 * @return true, if successful
	 */
	public static boolean visible() {
		return (instance != null);
	}

	// --------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	public void onDestroy() {
		instance = null;
		super.onDestroy();
	}

	// --------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onStop()
	 */
	@Override
	public void onStop() {
		if (instance != null) {
			try {
				alertDialog.dismiss();
			} catch (Exception e) {
				// ignore errors
			}
			instance.finish();
			instance = null;
		}
		super.onStop();
	}

	// --------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onStop()
	 */
	@Override
	public void onPause() {
		super.onPause();
		if (instance != null) {
			try {
				alertDialog.dismiss();
			} catch (Exception e) {
				// ignore errors
			}
			instance.finish();
			instance = null;
		}
	}

	// --------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		finish();
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

		// LinearLayout.LayoutParams lpEntry = new LinearLayout.LayoutParams(
		// LinearLayout.LayoutParams.MATCH_PARENT,
		// LinearLayout.LayoutParams.WRAP_CONTENT);
		// lpEntry.setMargins(20, 20, 20, 20);

		LinearLayout.LayoutParams lpCaption = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT, 2);
		lpCaption.setMargins(10, 12, 15, 12);
		lpCaption.gravity = Gravity.CENTER_VERTICAL;

		LinearLayout.LayoutParams lpIcon = new LinearLayout.LayoutParams(70,
				LinearLayout.LayoutParams.WRAP_CONTENT, 1);
		lpIcon.setMargins(25, 12, 10, 12);

		LinearLayout outerLayout = new LinearLayout(context);
		outerLayout.setOrientation(LinearLayout.VERTICAL);
		outerLayout.setGravity(Gravity.CENTER_HORIZONTAL);

		int max = imageContextMenuProvider.captions.size();
		for (int i = 0; i < max; i++) {
			if (!imageContextMenuProvider.isVisible(i)) {
				continue;
			}

			final LinearLayout entryLayout = new LinearLayout(context);
			entryLayout.setOrientation(LinearLayout.HORIZONTAL);
			entryLayout.setGravity(Gravity.LEFT);

			ExtendedEntryViewProvider viewProvider = imageContextMenuProvider.getViewProvider(i);
			if (viewProvider != null) {
				// Handle extended entry
				entryLayout.addView(viewProvider.provideView(context));
			} else {
				// Handle normal entry
				String caption = imageContextMenuProvider.getCaption(i);
				Drawable icon = imageContextMenuProvider.getIcon(i);
				// Caption
				TextView text = new TextView(context);
				text.setText(caption);
				text.setLayoutParams(lpCaption);
				text.setTextSize(imageContextMenuProvider.textSize);
				text.setTextColor(Color.WHITE);
				text.setGravity(Gravity.CENTER_VERTICAL);
				if (!imageContextMenuProvider.isEnabled(i)) {
					text.setTextColor(Color.GRAY);
				}
				// Icon
				ImageView img = new ImageView(context);
				img.setImageDrawable(icon);
				img.setLayoutParams(lpIcon);
				entryLayout.addView(img);
				entryLayout.addView(text);
			}

			final ImageContextMenuSelectionListener listener = imageContextMenuProvider.imageContextMenuSelectionListeners
					.get(i);

			if (imageContextMenuProvider.isEnabled(i)) {
				// Click selection listener
				entryLayout.setClickable(true);
				entryLayout.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {
						entryLayout.setBackgroundColor(CLICKED_COLOR);
						final boolean shouldClose = listener
								.onSelection(instance);
						entryLayout.postDelayed(new Runnable() {
							public void run() {
								entryLayout
										.setBackgroundColor(NOTCLICKED_COLOR);
								if (shouldClose) {
									// alertDialog.dismiss();
									finish();
								}
							}
						}, CLICKDURATION);
					}
				});
			}

			outerLayout.setBackgroundColor(BACKGROUND);

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

		LinearLayout.LayoutParams lpScrollView = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		ScrollView scrollView = new ScrollView(context);
		scrollView.setLayoutParams(lpScrollView);
		scrollView.addView(outerLayout);

		builder.setView(scrollView);

		// Show the dialog and populate the alertDialog variable that will be
		// passed to the click listener
		alertDialog = builder.show();
		showTime = System.currentTimeMillis();

		alertDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
			public boolean onKey(DialogInterface dialog, int keyCode,
					KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_MENU) {
					if (System.currentTimeMillis() - showTime > 200) {
						finish();
					}
				}
				return false;
			}
		});

		alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
			public void onCancel(DialogInterface dialog) {
				finish();
			}
		});

	}

	// ------------------------------------------------------------------------

}