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

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.SeekBar;

@SuppressLint("WrongCall")
public class FastScrollBar extends SeekBar {
			 
	// ms for wich to lock the setProgress() input in case of moving manually!
	static final int LOCKTIMEOUT = 200;
	
	private Drawable thumb;
	long lockedTimestamp = 0;
	
	private int snapUp = -1;
	private int snapDown = -1;
	private int currentProgress;
	
	public static int NOSNAP = -1;
	
	private OnScrollListener scrollListener;
	
	public interface OnScrollListener {
		void onScroll(int percent);

		void onSnapScroll(int percent, boolean snappedDown, boolean snappedUp);
	}
	
	public void setOnScrollListener(OnScrollListener onScrollListener) {
		scrollListener = onScrollListener;
	}

	public FastScrollBar(Context context) {
		super(context);
	}

	public FastScrollBar(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public FastScrollBar(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(h, w, oldh, oldw);
	}

	@Override
	public void setThumb(Drawable thumb) {
		super.setThumb(thumb);
		this.thumb = thumb;
//		this.scrollTo(0, (int) currentProgress);
	}

	@Override
	protected synchronized void onMeasure(int widthMeasureSpec,
			int heightMeasureSpec) {
		super.onMeasure(heightMeasureSpec, widthMeasureSpec);
		setMeasuredDimension(getMeasuredHeight(), getMeasuredWidth());
	}

	// Canvas cachedc = null;
	// public void refresgDraw() {
	// if (cachedc != null) {
	// cachedc.rotate(-90);
	// //c.translate(-getHeight(), 0);
	// super.onDraw(cachedc);
	// }
	// }
	
	// if > 90% -> snap to 100%
	public void setSnapDown(int snapDownLimit) {
		snapDown = snapDownLimit;
	}
	public void setSnapUp(int snapUpLimit) {
		snapUp = snapUpLimit;
	}

	public boolean isLocked() {
		return ((getTimestamp() - LOCKTIMEOUT) < lockedTimestamp);
	}

	@Override
	public synchronized void setProgress(int progress) {
		super.setProgress(progress);
		onSizeChanged(getWidth(), getHeight(), 0, 0);
	}

	protected void onDraw(Canvas c) {
		c.rotate(-90);
		c.translate(-getHeight(), 0);
		super.onDraw(c);
	}

	public static long getTimestamp() {
		long timeStamp = System.currentTimeMillis(); // calendar.getTimeInMillis();
		return timeStamp;
	}


	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (!isEnabled()) {
			return false;
		}
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			lockedTimestamp = getTimestamp();
		case MotionEvent.ACTION_MOVE:
			lockedTimestamp = getTimestamp();
		case MotionEvent.ACTION_UP:
			lockedTimestamp = getTimestamp();
			int i = 0;
			
			int max = getMax();
			int height = getHeight();
			int cur = (int) event.getY();
			int percent = (max * cur / height);

			boolean snappedDown = false;
			boolean snappedUp = false;
			if (snapDown != -1) {
				if (percent >= snapDown && percent < max) {
					percent = max;
					snappedDown = true;
				}
			}
			if (snapUp != -1) {
				if (percent <= snapUp && percent > 0) {
					percent = 0;
					snappedUp = true;
				}
			}
			i = max - percent;
			
			currentProgress = i;
			setProgress(i);
			
			if (scrollListener != null) {
				scrollListener.onScroll(percent);
				if (snappedUp || snappedDown) {
					scrollListener.onSnapScroll(percent, snappedDown, snappedUp);
				}
			}
			
//			Log.d("communicator", "@@@@@@ Progress=" + getProgress() + "");
			onSizeChanged(getWidth(), getHeight(), 0, 0);
			break;
		case MotionEvent.ACTION_CANCEL:
			break;
		}
		return true;
	}

}
