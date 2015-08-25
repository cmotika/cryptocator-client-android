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
import android.view.MotionEvent;
import android.widget.SeekBar;

/**
 * The FastScrollBar class is a vertical special scrollbar which actually is a
 * utilized SeekBar. It has a specific thumb image that can be defined. A scroll
 * listener can be defined that listens to onScroll or onSnapScroll events.
 * Snapping can be defined for up and down.
 * 
 * @author Christian Motika
 * @since 1.2
 * @date 08/23/2015
 * 
 */
@SuppressLint("WrongCall")
public class FastScrollBar extends SeekBar {

	/**
	 * The Constant LOCKTIMEOUT specifies the ms for wich to lock the
	 * setProgress() input in case of moving manually!.
	 */
	static final int LOCKTIMEOUT = 200;

	/** The locked timestamp. */
	long lockedTimestamp = 0;

	/** The snap up. */
	private int snapUp = -1;

	/** The snap down. */
	private int snapDown = -1;

	/** The current progress. */
	private int currentProgress;

	/** The nosnap. */
	public static int NOSNAP = -1;

	/** The scroll listener. */
	private OnScrollListener scrollListener;

	// ------------------------------------------------------------------------

	/**
	 * The listener interface for receiving onScroll events. The class that is
	 * interested in processing a onScroll event implements this interface, and
	 * the object created with that class is registered with a component using
	 * the component's <code>addOnScrollListener<code> method. When
	 * the onScroll event occurs, that object's appropriate
	 * method is invoked.
	 * 
	 * @see OnScrollEvent
	 */
	public interface OnScrollListener {

		/**
		 * On scroll.
		 * 
		 * @param percent
		 *            the percent
		 */
		void onScroll(int percent);

		/**
		 * On snap scroll.
		 * 
		 * @param percent
		 *            the percent
		 * @param snappedDown
		 *            the snapped down
		 * @param snappedUp
		 *            the snapped up
		 */
		void onSnapScroll(int percent, boolean snappedDown, boolean snappedUp);
	}

	// ------------------------------------------------------------------------

	/**
	 * Sets the on scroll listener.
	 * 
	 * @param onScrollListener
	 *            the new on scroll listener
	 */
	public void setOnScrollListener(OnScrollListener onScrollListener) {
		scrollListener = onScrollListener;
	}

	// ------------------------------------------------------------------------

	/**
	 * Instantiates a new fast scroll bar.
	 * 
	 * @param context
	 *            the context
	 */
	public FastScrollBar(Context context) {
		super(context);
	}

	// ------------------------------------------------------------------------

	/**
	 * Instantiates a new fast scroll bar.
	 * 
	 * @param context
	 *            the context
	 * @param attrs
	 *            the attrs
	 * @param defStyle
	 *            the def style
	 */
	public FastScrollBar(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	// ------------------------------------------------------------------------

	/**
	 * Instantiates a new fast scroll bar.
	 * 
	 * @param context
	 *            the context
	 * @param attrs
	 *            the attrs
	 */
	public FastScrollBar(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	// ------------------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.widget.AbsSeekBar#onSizeChanged(int, int, int, int)
	 */
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(h, w, oldh, oldw);
	}

	// ------------------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.widget.AbsSeekBar#setThumb(android.graphics.drawable.Drawable)
	 */
	@Override
	public void setThumb(Drawable thumb) {
		super.setThumb(thumb);
	}

	// ------------------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.widget.AbsSeekBar#onMeasure(int, int)
	 */
	@Override
	protected synchronized void onMeasure(int widthMeasureSpec,
			int heightMeasureSpec) {
		super.onMeasure(heightMeasureSpec, widthMeasureSpec);
		setMeasuredDimension(getMeasuredHeight(), getMeasuredWidth());
	}

	// ------------------------------------------------------------------------

	/**
	 * Sets the snap down value, if > snapDown% then snap to 100%.
	 * 
	 * @param snapDownLimit
	 *            the new snap down
	 */
	public void setSnapDown(int snapDownLimit) {
		snapDown = snapDownLimit;
	}

	// ------------------------------------------------------------------------

	/**
	 * Sets the snap up value, if < snapUp% then snap to 0%.
	 * 
	 * @param snapUpLimit
	 *            the new snap up
	 */
	public void setSnapUp(int snapUpLimit) {
		snapUp = snapUpLimit;
	}

	// ------------------------------------------------------------------------

	/**
	 * Checks if is locked.
	 * 
	 * @return true, if is locked
	 */
	public boolean isLocked() {
		return ((getTimestamp() - LOCKTIMEOUT) < lockedTimestamp);
	}

	// ------------------------------------------------------------------------

	@SuppressLint("ClickableViewAccessibility")
	/*
	 * (non-Javadoc)
	 * 
	 * @see android.widget.ProgressBar#setProgress(int)
	 */
	@Override
	public synchronized void setProgress(int progress) {
		super.setProgress(progress);
		onSizeChanged(getWidth(), getHeight(), 0, 0);
	}

	// ------------------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.widget.AbsSeekBar#onDraw(android.graphics.Canvas)
	 */
	protected void onDraw(Canvas c) {
		c.rotate(-90);
		c.translate(-getHeight(), 0);
		super.onDraw(c);
	}

	// ------------------------------------------------------------------------

	/**
	 * Gets the timestamp.
	 * 
	 * @return the timestamp
	 */
	public static long getTimestamp() {
		long timeStamp = System.currentTimeMillis(); // calendar.getTimeInMillis();
		return timeStamp;
	}

	// ------------------------------------------------------------------------

	/**
	 * Gets the current progress in percent.
	 * 
	 * @return the current progress
	 */
	public int getCurrentProgress() {
		return currentProgress;
	}

	// ------------------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.widget.AbsSeekBar#onTouchEvent(android.view.MotionEvent)
	 */
	// This ScrollBar should only be used for touch displays.
	@SuppressLint("ClickableViewAccessibility")
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
					scrollListener
							.onSnapScroll(percent, snappedDown, snappedUp);
				}
			}

			onSizeChanged(getWidth(), getHeight(), 0, 0);
			break;
		case MotionEvent.ACTION_CANCEL:
			break;
		}
		return true;
	}

	// ------------------------------------------------------------------------

}
