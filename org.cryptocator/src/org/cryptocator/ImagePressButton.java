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

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;

public class ImagePressButton extends ImageButton {

	Bitmap original;
	Bitmap pressed;
	Drawable dOriginal;
	Drawable dPressed;
	int shift = 5;
	int delayInternal = 300;
	boolean active = false;
	boolean initialized = false;
	boolean backgroundInternal = true;
	View additionalWhiteView = null;
	
	public int WHITEPRESS = Color.parseColor("#44FFFFFF");
	public int TRANSPARENT = Color.parseColor("#00FFFFFF");

	public ImagePressButton(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}

	public ImagePressButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}

	public ImagePressButton(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void setImageResource(int resId) {
		super.setImageResource(resId);
	}

	public void setImagePressResource(int resId) {
		super.setImageResource(resId);
	}

	public void initializePressImageResource(int resId) {
		initializePressImageResource(resId, shift, delayInternal,
				Color.parseColor("#00000000"), true);
	}

	public void initializePressImageResourceWhite(int resId) {
		initializePressImageResource(resId, shift, delayInternal,
				Color.parseColor("#44FFFFFF"), true);
	}


	public void setAdditionalPressWhiteView(View view) {
		additionalWhiteView = view;
	}

	public void initializePressImageResourceWhite(int resId, boolean background) {
		initializePressImageResource(resId, shift, delayInternal,
				WHITEPRESS, background);
	}

	public void initializePressImageResource(int resId, boolean background) {
		initializePressImageResource(resId, shift, delayInternal,
				Color.parseColor("#00000000"), background);
	}

	public void initializePressImageResource(int resId, int shift, int delay) {
		initializePressImageResource(resId, shift, delay,
				Color.parseColor("#00000000"), true);
	}

	public void initializePressImageResource(int resId, int shift, int delay, boolean background) {
		initializePressImageResource(resId, shift, delay,
				Color.parseColor("#00000000"), background);
	}
	
	public void deactivatePressImageResourceWhite() {
		active = false; 
	}

	public void activatePressImageResourceWhite() {
		active = true;
	} 

	public void initializePressImageResource(int resId, int shift, int delay,
			int color, boolean background) {
		active = true;
		this.shift = shift;
		this.delayInternal = delay;
		this.backgroundInternal = background;

		Paint paint = new Paint();

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
									additionalWhiteView.setBackgroundColor(TRANSPARENT);
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

		// super.setImageBitmap(icon2);
	}

	// @Override
	// protected void onDraw(Canvas canvas) {
	// super.onDraw(canvas);
	//
	// // Calc text size
	// Paint paint = new Paint();
	// paint.setColor(this.textColor);
	// paint.setTextSize(this.textSize);
	// Rect bounds = new Rect();
	// paint.getTextBounds(this.text, 0, this.text.length(), bounds);
	//
	// // Draw the Text
	// float buttonWitdh = this.getWidth();
	// float buttonHeight = this.getHeight();
	// canvas.drawText(this.text, (buttonWitdh - bounds.width()) / 2,
	// buttonHeight - height - height / 2, paint);
	// }

}
