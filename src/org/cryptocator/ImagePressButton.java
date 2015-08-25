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

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;

/**
 * The ImagePressButton class is a special ImageButton that can highlight its
 * background or an additional (parent) view for a while (300ms) when the button
 * is pressed.
 * 
 * @author Christian Motika
 * @since 1.2
 * @date 08/23/2015
 * 
 */
@SuppressLint("ClickableViewAccessibility")
public class ImagePressButton extends ImageButton {

	/** The original bitmap. */
	Bitmap original;

	/** The pressed bitmap calculated from the original bitmap. */
	Bitmap pressed;

	/** The original drawable. */
	Drawable dOriginal;

	/** The pressed drawable calculated from the original bitmap. */
	Drawable dPressed;

	/** The shift of the pressed bitmap/drawable. */
	int shift = 5;

	/** The delay internal until the original drawable is restored. */
	int delayInternal = 300;

	/**
	 * The flag that tells if the button is active or not reacting to touch
	 * events.
	 */
	boolean active = false;

	/**
	 * The initialized flag tells if the pressed bitmap/drawable has been
	 * created.
	 */
	boolean initialized = false;

	/**
	 * The background internal flag tells if the bitmap should be drawn to the
	 * background or to the foreground. Background images are stretched to the
	 * button size while foreground images are not stretched at all.
	 */
	boolean backgroundInternal = true;

	/**
	 * The additional white view that can be set optionally. If set the
	 * background of this view is set to the WHITEPRESS color for the duration
	 * of 300ms.
	 */
	View additionalWhiteView = null;

	/** The whitepress color. */
	public int WHITEPRESS = Color.parseColor("#44FFFFFF");

	/** The transparent color. */
	public int TRANSPARENT = Color.parseColor("#00FFFFFF");

	// ------------------------------------------------------------------------

	/**
	 * Instantiates a new image press button.
	 * 
	 * @param context
	 *            the context
	 */
	public ImagePressButton(Context context) {
		super(context);
	}

	// ------------------------------------------------------------------------

	/**
	 * Instantiates a new image press button.
	 * 
	 * @param context
	 *            the context
	 * @param attrs
	 *            the attrs
	 */
	public ImagePressButton(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	// ------------------------------------------------------------------------

	/**
	 * Instantiates a new image press button.
	 * 
	 * @param context
	 *            the context
	 * @param attrs
	 *            the attrs
	 * @param defStyle
	 *            the def style
	 */
	public ImagePressButton(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	// ------------------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.widget.ImageView#setImageResource(int)
	 */
	@Override
	public void setImageResource(int resId) {
		super.setImageResource(resId);
	}

	// ------------------------------------------------------------------------

	/**
	 * Sets the image press resource.
	 * 
	 * @param resId
	 *            the new image press resource
	 */
	public void setImagePressResource(int resId) {
		super.setImageResource(resId);
	}

	// ------------------------------------------------------------------------

	/**
	 * Initialize press image resource.
	 * 
	 * @param resId
	 *            the res id
	 */
	@SuppressLint("ClickableViewAccessibility")
	public void initializePressImageResource(int resId) {
		initializePressImageResource(resId, shift, delayInternal,
				TRANSPARENT, true);
	}

	// ------------------------------------------------------------------------

	/**
	 * Initialize press image resource white.
	 * 
	 * @param resId
	 *            the res id
	 */
	public void initializePressImageResourceWhite(int resId) {
		initializePressImageResource(resId, shift, delayInternal,
				WHITEPRESS, true);
	}

	// ------------------------------------------------------------------------

	/**
	 * Sets the additional press white view.
	 * 
	 * @param view
	 *            the new additional press white view
	 */
	public void setAdditionalPressWhiteView(View view) {
		additionalWhiteView = view;
	}

	// ------------------------------------------------------------------------

	/**
	 * Initialize press image resource white.
	 * 
	 * @param resId
	 *            the res id
	 * @param background
	 *            the background
	 */
	public void initializePressImageResourceWhite(int resId, boolean background) {
		initializePressImageResource(resId, shift, delayInternal, WHITEPRESS,
				background);
	}

	// ------------------------------------------------------------------------

	/**
	 * Initialize press image resource.
	 * 
	 * @param resId
	 *            the res id
	 * @param background
	 *            the background
	 */
	public void initializePressImageResource(int resId, boolean background) {
		initializePressImageResource(resId, shift, delayInternal, TRANSPARENT,
				background);
	}

	// ------------------------------------------------------------------------

	/**
	 * Initialize press image resource.
	 * 
	 * @param resId
	 *            the res id
	 * @param shift
	 *            the shift
	 * @param delay
	 *            the delay
	 */
	public void initializePressImageResource(int resId, int shift, int delay) {
		initializePressImageResource(resId, shift, delay, TRANSPARENT, true);
	}

	// ------------------------------------------------------------------------

	/**
	 * Initialize press image resource.
	 * 
	 * @param resId
	 *            the res id
	 * @param shift
	 *            the shift
	 * @param delay
	 *            the delay
	 * @param background
	 *            the background
	 */
	public void initializePressImageResource(int resId, int shift, int delay,
			boolean background) {
		initializePressImageResource(resId, shift, delay, TRANSPARENT,
				background);
	}

	// ------------------------------------------------------------------------

	/**
	 * Deactivate press image resource white.
	 */
	public void deactivatePressImageResourceWhite() {
		active = false;
	}

	// ------------------------------------------------------------------------

	/**
	 * Activate press image resource white.
	 */
	public void activatePressImageResourceWhite() {
		active = true;
	}

	// ------------------------------------------------------------------------

	/**
	 * Initialize press image resource and installs the listener for the
	 * duration after the touch event of the button (press).
	 * 
	 * @param resId
	 *            the res id
	 * @param shift
	 *            the shift
	 * @param delay
	 *            the delay
	 * @param color
	 *            the color
	 * @param background
	 *            the background
	 */
	public void initializePressImageResource(int resId, int shift, int delay,
			int color, boolean background) {
		active = true;
		this.shift = shift;
		this.delayInternal = delay;
		this.backgroundInternal = background;

		Bitmap icon = BitmapFactory.decodeResource(this.getResources(), resId);

		Bitmap icon2 = Bitmap.createBitmap(icon.getWidth() + shift,
				icon.getHeight() + shift, Bitmap.Config.ARGB_8888);

		Canvas canvas = new Canvas(icon2);
		// draw the source image centered
		canvas.drawBitmap(icon, shift, shift, new Paint());

		original = icon;
		pressed = icon2;
		dOriginal = new BitmapDrawable(getResources(), original);
		dPressed = new BitmapDrawable(getResources(), pressed);
		dPressed.setColorFilter(color, PorterDuff.Mode.LIGHTEN);

		if (initialized) {
			return;
		}
		initialized = true;

		final ImageButton instance = this;
		this.setOnTouchListener(new View.OnTouchListener() {
			@SuppressWarnings("deprecation")
			public boolean onTouch(View v, MotionEvent event) {
				if (active) {
					if (event.getAction() == MotionEvent.ACTION_DOWN) {
						if (additionalWhiteView != null) {
							additionalWhiteView.setBackgroundColor(WHITEPRESS);
						}
						if (backgroundInternal) {
							instance.setBackgroundDrawable(dPressed);
						} else {
							instance.setImageDrawable(dPressed);
						}
						instance.postDelayed(new Runnable() {
							public void run() {
								if (additionalWhiteView != null) {
									additionalWhiteView
											.setBackgroundColor(TRANSPARENT);
								}
								if (backgroundInternal) {
									instance.setBackgroundDrawable(dOriginal);
								} else {
									instance.setImageDrawable(dOriginal);
								}
							}
						}, delayInternal);
					}
				}
				return false;
			}
		});
	}

	// ------------------------------------------------------------------------

}
