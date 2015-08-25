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

import java.util.ArrayList;
import java.util.List;

import org.cryptocator.ScrollViewEx.ScrollViewExListener;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;

/**
 * The Class FastScollView.
 */
public class FastScrollView extends LinearLayout {

	/** The context. */
	private Context context;

	/** The inner childs. */
	private LinearLayout innerChilds;

	/** The scroll view. */
	private ScrollViewEx scrollView;

	/** The scroll bar. */
	private FastScrollBar scrollBar;

	/** The selected index. */
	private int selectedIndex = -1;

	/** The child list. */
	private List<View> childList = new ArrayList<View>();

	// /** The instance. */
	// FastScrollView instance = null;

	private boolean heightsIvalidate = true;

	// -------------------------------------------------------------------------

	// if > 90% -> snap to 100%
	public void setSnapDown(int snapDownLimit) {
		scrollBar.setSnapDown(snapDownLimit);
	}

	public void setSnapUp(int snapUpLimit) {
		scrollBar.setSnapUp(snapUpLimit);
	}

	// -------------------------------------------------------------------------
	// -------------------------------------------------------------------------

	private OnLayoutDoneListener layoutDoneListener;

	private OnScrollListener scrollListener;

	private OnSizeChangeListener sizeChangeListener;

	private OnNoHangListener noHangListener;

	// -------------------------------------------------------------------------

	/**
	 * The listener interface for receiving onLayoutDone events. The class that
	 * is interested in processing a onLayoutDone event implements this
	 * interface, and the object created with that class is registered with a
	 * component using the component's
	 * <code>addOnLayoutDoneListener<code> method. When
	 * the onLayoutDone event occurs, that object's appropriate
	 * method is invoked.
	 * 
	 * @see OnLayoutDoneEvent
	 */
	//
	public interface OnLayoutDoneListener {

		/**
		 * Done layout.
		 */
		void doneLayout();
	}

	// -------------------------------------------------------------------------

	public interface OnScrollListener {
		void onScrollChanged(FastScrollView fastScrollView, int x, int y,
				int oldx, int oldy, int percent, int item);

		void onScrollRested(FastScrollView fastScrollView, int x, int y,
				int oldx, int oldy, int percent, int item);

		void onOversroll(boolean up);
	}

	// -------------------------------------------------------------------------

	public interface OnSizeChangeListener {

		/**
		 * On Size Change.
		 */
		void onSizeChange(int w, int h, int oldw, int oldh);
	}

	// -------------------------------------------------------------------------

	public interface OnNoHangListener {

		/**
		 * Compute if no hang is needed. ATTENTION: DO COMPUTATION VERY VERY
		 * LIGHTWEIGHT!
		 */
		boolean noHangNeeded();
	}

	// -------------------------------------------------------------------------

	/**
	 * Sets the on layout done listener.
	 * 
	 * @param layoutDoneListener
	 *            the new on layout done listener
	 */
	public void setOnLayoutDoneListener(OnLayoutDoneListener layoutDoneListener) {
		this.layoutDoneListener = layoutDoneListener;
	}

	// -------------------------------------------------------------------------

	public void setOnScrollListener(OnScrollListener onScrollListener) {
		this.scrollListener = onScrollListener;
	}

	// -------------------------------------------------------------------------

	public void setOnSizeChangeListener(
			OnSizeChangeListener onSizeChangeListener) {
		this.sizeChangeListener = onSizeChangeListener;
	}

	// -------------------------------------------------------------------------

	public void setOnNoHangListener(OnNoHangListener onNoHangListener) {
		this.noHangListener = onNoHangListener;
	}

	// -------------------------------------------------------------------------
	// -------------------------------------------------------------------------

	public FastScrollView(Context context, AttributeSet attrs) {
		super(context, attrs);
		createInnerViews(context);
	}

	// -------------------------------------------------------------------------

	/**
	 * Instantiates a new fast scoll view.
	 * 
	 * @param context
	 *            the context
	 * @param labels
	 *            the labels
	 */
	public FastScrollView(Context context) {
		super(context);
		createInnerViews(context);
	}

	// -------------------------------------------------------------------------

	private void createInnerViews(Context context) {
		this.context = context;
		final FastScrollView instance = this;

		LinearLayout.LayoutParams lpinnerChilds = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		innerChilds = new LinearLayout(context);
		innerChilds.setOrientation(LinearLayout.VERTICAL);
		innerChilds.setLayoutParams(lpinnerChilds);

		scrollView = new ScrollViewEx(context);
		scrollView.addView(innerChilds);

		scrollBar = new FastScrollBar(context);
		scrollBar.setThumb(getResources().getDrawable(
				R.drawable.scrollbarhandle4));

		LinearLayout.LayoutParams lpScrollView = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT, 2);
		lpScrollView.setMargins(0, 0, 0, 0);
		scrollView.setLayoutParams(lpScrollView);

		LinearLayout.LayoutParams lpScrollBar = new LinearLayout.LayoutParams(
				30, LinearLayout.LayoutParams.MATCH_PARENT, 1);
		lpScrollBar.setMargins(0, 0, 0, 0);
		scrollBar.setLayoutParams(lpScrollBar);
		scrollBar.setProgressDrawable(new ColorDrawable(
				android.R.color.transparent));

		// android.view.ViewGroup.LayoutParams params = scrollBar
		// .getLayoutParams();
		// params.width = 25;

		// this.setBackgroundColor(Color.rgb(255, 0, 0));
		// scrollView.setBackgroundColor(Color.rgb(0, 255, 0));
		// scrollBar.setBackgroundColor(Color.rgb(0, 0, 255));

		// Do not show standard scrollbar
		scrollView.setVerticalScrollBarEnabled(false);
		scrollView.setHorizontalScrollBarEnabled(false);

		scrollBar.setOnScrollListener(new FastScrollBar.OnScrollListener() {

			public void onScroll(int percent) {
				// int mymax = getVisibleHeight();
				// int percent = (y * 100) / mymax;
				// Log.d("communicator", "######## onTouch y=" + y + ", max =" +
				// mymax + ", percent="+ percent);

				Log.d("communicator", "PPPPPP FastScrollView.onScroll()");

				scrollToPercent(percent);
			}

			public void onSnapScroll(int percent, boolean snappedDown,
					boolean snappedUp) {
				// TODO Auto-generated method stub

			}
		});

		scrollView.setScrollViewListener(new ScrollViewExListener() {
			public void onScrollChanged(final ScrollViewEx scrollView, int x,
					final int y, int oldx, int oldy) {
				if (isNoHangNeeded()) {
					deferredScrolling = y;
					Log.d("communicator",
							"PPPPPP NOHANG FastScrollView.onScrollChanged()");
					return;
				}
				Log.d("communicator", "PPPPPP FastScrollView.onScrollChanged()");

				int percent = getPosToPercent(y);
				updateFastScrollBar(percent, false);

				if (scrollListener != null) {
					int item = getPosToItem(y);
					// Log.d("communicator",
					// "@@@@@ ON SCROLL scrollView.scrollView w/ instance = "+instance.hashCode());
					// Log.d("communicator",
					// "@@@@ ON SCROLL ("+instance.hashCode()+") CHANGED =======> "
					// + instance.heights.size() + ", " + instance.heightsSum);
					scrollListener.onScrollChanged(instance, x, y, oldx, oldy,
							percent, item);
				}

			}

			public void onScrollRested(ScrollViewEx scrollView, int x, int y,
					int oldx, int oldy) {
				Log.d("communicator", "PPPPPP FastScrollView.onScrollRested()");

				// Log.d("communicator",
				// "@@@@@ scrollView.scrollView w/ instance = "+instance.hashCode());
				// Log.d("communicator",
				// "@@@@ SCROLL RESTED ("+instance.hashCode()+") CHANGED =======> "
				// + instance.heights.size() + ", " + instance.heightsSum);
				// Log.d("communicator", "######## SCROLL RESTED");
				if (scrollListener != null) {
					int percent = getPosToPercent(y);
					int item = getPosToItem(y);
					scrollListener.onScrollRested(instance, x, y, oldx, oldy,
							percent, item);
				}

			}

			public void onOversroll(boolean up) {
				Log.d("communicator", "PPPPPP FastScrollView.onOversroll()");

				if (scrollListener != null) {
					scrollListener.onOversroll(up);
				}
			}
		});

		LinearLayout.LayoutParams lpouterLayout = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);

		LinearLayout outerLayout = new LinearLayout(context);
		outerLayout.setOrientation(LinearLayout.HORIZONTAL);
		outerLayout.addView(scrollView);
		outerLayout.addView(scrollBar);
		outerLayout.setLayoutParams(lpouterLayout);
		this.addView(outerLayout);

		resetFastScrollBar();
		// reset heights
		heightsIvalidate = true;
	}

	// -------------------------------------------------------------------------
	// -------------------------------------------------------------------------

	public void scrollToItem(int item) {
		int pos = getItemToPos(item);
		int percent = getPosToPercent(pos);
		// Log.d("communicator", "######## scrollToItem item=" + item +
		// " => pos="
		// + pos + " => percent=" + percent);
		scrollToPercent(percent);
	}

	// -------------------------------------------------------------------------

	public void scrollToPercent(int percent) {
		int pos = getPercentToPos(percent);
		// Log.d("communicator", "######## scrollToPercent percent=" + percent
		// + " => pos=" + pos);
		scrollView.scrollTo(0, pos);
		updateFastScrollBar(percent, true);
	}

	// -------------------------------------------------------------------------

	public void scrollUp() {
		scrollToPercent(0);
	}

	// -------------------------------------------------------------------------

	public void scrollDown() {
		scrollToPercent(100);
	}

	// -------------------------------------------------------------------------

	private void resetFastScrollBar() {
		scrollBar.setProgress(0);
		int maxPos = 100;// getMaxPosition() - scrollView.getHeight();
		scrollBar.setMax(maxPos);
	}

	// -------------------------------------------------------------------------

	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		// CANNOT DO THE CHECK FOR NOHANG HERE BECAUSE OTHERWISE WE CANNOT REACT
		// E.G. WITH SCROLLING! DO NOT SCROLL IN ONSIZECHANGELISTENER DIRECTLY
		// IMPLEMET YOUR OWN STRATEGY TO WAIT UNTIL NO NOHANG ANY MORE!
		if (sizeChangeListener != null) {
			sizeChangeListener.onSizeChange(w, h, oldw, oldh);
		}
		// should update the scrollbar but cannot do this here because no
		// new layout heights are there yet. wait for the layout!
	}

	// -------------------------------------------------------------------------

	private boolean scrollBarSet = false;
	private boolean scrollBarVisible = false;

	private void updateScrollBar(int i) {
		Log.d("communicator", "PPPPPP FastScrollView.onSizeChanged()");
		// Log.d("communicator", "@@@@ " + i +
		// " updateScrollBar getMaxHeight()="
		// + getMaxPosition() + ", getVisibleHeight()="
		// + getVisibleHeight());
		if (getMaxPosition() > 0 && getMaxPosition() <= getVisibleHeight()
				&& (scrollBarVisible || !scrollBarSet)) {
			scrollBarSet = true;
			scrollBarVisible = false;
			// android.view.ViewGroup.LayoutParams params = scrollBar
			// .getLayoutParams();
			// params.width = 60;
			scrollBar.setVisibility(View.GONE);
			// this.invalidate();
			// this.requestLayout();
			heightsIvalidate = true;
		} else if (getMaxPosition() > getVisibleHeight()
				&& (!scrollBarVisible || !scrollBarSet)) {
			scrollBar.setVisibility(View.VISIBLE);
			// android.view.ViewGroup.LayoutParams params = scrollBar
			// .getLayoutParams();
			// params.width = 30;
			// this.invalidate();
			// this.requestLayout();
			scrollBarSet = true;
			scrollBarVisible = true;
			heightsIvalidate = true;
		}
	}

	// -------------------------------------------------------------------------

	public int getPercentToPos(int percent) {
		int maxPos = getMaxHeight();
		int pos = (percent * maxPos) / 100;

		return pos;
	}

	// -------------------------------------------------------------------------

	public int getPosToPercent(int pos) {
		int maxPos = getMaxHeight();
		int percent = (pos * 100) / maxPos;
		return percent;
	}

	// -------------------------------------------------------------------------

	private void updateFastScrollBar(int percent, boolean override) {
		if (!scrollBar.isLocked() || override) {
			// Log.d("communicator", "######## updateFastScrollBar percent="
			// + percent);
			scrollBar.setMax(100);
			scrollBar.setProgress(100 - percent);
			heightsIvalidate = true;
		}
	}

	// -------------------------------------------------------------------------

	// forceNoUpdateUi == true makes it more efficient to add several childs at
	// once
	public void addChild(View view) {
		childList.add(view);
		innerChilds.addView(view);
		heightsIvalidate = true;
	}

	// -------------------------------------------------------------------------

	public void clearChilds() {
		childList.clear();
		if (innerChilds != null) {
			innerChilds.removeAllViews();
		}
		heightsIvalidate = true;
	}

	// -------------------------------------------------------------------------

	boolean deferredMeasureHeights = false;
	int deferredScrolling = -1;

	/**
	 * Potentially refresh state if during an onLayout() method, this
	 * calculation was skipped because of isNoHangNeeded() was true
	 */
	public void potentiallyRefreshState() {
		if (deferredMeasureHeights) {
			measureHeights();
			deferredMeasureHeights = false;
		}
		if (deferredScrolling != -1) {
			int percent = getPosToPercent(deferredScrolling);
			updateFastScrollBar(percent, false);
			deferredScrolling = -1;
		}
	}

	// -------------------------------------------------------------------------

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		super.onLayout(changed, l, t, r, b);
		// if (isNoHangNeeded()) {
		// // remember that we need an update so someone should call
		// // potentiallyRefreshState()!
		// deferredMeasureHeights = true;
		// Log.d("communicator", "PPPPPP NOHANG FastScrollView.onLayout()");
		// return;
		// }
		Log.d("communicator", "PPPPPP FastScrollView.onLayout()");
		if (!isNoHangNeeded()) {
			updateScrollBar(4);
			if (layoutDoneListener != null) {
				layoutDoneListener.doneLayout();
			}
		}
		// IT SEEMS TO BE NECESSARY TO STILL UPDATE THE MEASUREMENTS!!!
		// WHY???
		measureHeights();
		changed = false;
	}

	// -------------------------------------------------------------------------

	/** The heights. */
	public List<Integer> heights = new ArrayList<Integer>();
	public int heightsSum = 0;

	/**
	 * Measure the heights
	 */
	private void measureHeights() {
		if (!heightsIvalidate) {
			return;
		}
		heightsIvalidate = false;
		heights.clear();
		//String heightsString = "";
		heightsSum = 0;
		for (View layout : childList) {
			int h = layout.getMeasuredHeight();
			heights.add(h);
			heightsSum += h;
			//heightsString += heightsSum + ",";
		}
		// invalidate
		// Log.d("communicator", "@@@@ internal heights ("+this.hashCode()+"): "
		// + heightsString);
		// Log.d("communicator", "@@@@ internal heights size: " +
		// heights.size());
		// Log.d("communicator", "@@@@ internal heightsSum: " + heightsSum);
	}

	// ------------------------------------------------------------------------

	/**
	 * Gets the scroll position for a specific child element
	 * 
	 * @param item
	 *            the item
	 * @return the position by scroll item
	 */
	// Gets the scroll position (sum of heights) by scrolled item
	public int getItemToPos(int item) {
		int itemcnt = 0;
		int hsum = 0;
		for (int h : heights) {
			itemcnt++;
			if (itemcnt >= item) {
				return hsum;
			}
			hsum += h;
		}
		return hsum;
	}

	// ------------------------------------------------------------------------

	// Gets the scrolled item by scroll position (sum of heights)
	public int getPosToItem(int pos) {
		if (heights.size() == 0) {
			return -1;
		}
		int item = 0;
		int hsum = 0;
		for (int h : heights) {
			hsum += h;
			item++;
			if (hsum > pos) {
				return item;
			}
		}
		return item;
	}

	// ------------------------------------------------------------------------

	/**
	 * Gets the scroll position for the last (maximum) child element.
	 * 
	 * @return the max position
	 */
	public int getMaxPosition() {
		return heightsSum;
	}

	// ------------------------------------------------------------------------

	/**
	 * Gets the maximum height of the filled inner child area.
	 * 
	 * @return the max height
	 */
	public int getMaxHeight() {
		int maxHeightCached = getMaxPosition() - getVisibleHeight();
		if (maxHeightCached < 1) {
			maxHeightCached = 1;
		}
		return maxHeightCached;
	}

	// -------------------------------------------------------------------------

	/**
	 * Gets the visible height.
	 * 
	 * @return the visible height
	 */
	private int getVisibleHeight() {
		return scrollView.getHeight();
	}

	// -------------------------------------------------------------------------

	int lockedPos = -1;
	int lockedPos2 = -1;

	public void lockPosition() {
		lockedPos = this.scrollView.getScrollY();
	}

	public boolean isLocked() {
		return lockedPos != -1;
	}

	public void restoreLockedPosition() {
		final int lockedPosCopy = lockedPos;
		lockedPos = -1;
		final FastScrollView me = this;
		this.postDelayed(new Runnable() {
			public void run() {
				resetFastScrollBar();
				int percent = getPosToPercent(lockedPosCopy);

				scrollToPercent(percent);
			}
		}, 200);
	}

	// -------------------------------------------------------------------------

	public void setScrollBackground(final int colorResource) {
		// Log.d("communicator", "@@@@ set bg color " + colorResource);

		final Handler mUIHandler = new Handler(Looper.getMainLooper());
		mUIHandler.post(new Thread() {
			@Override
			public void run() {
				super.run();
				// Log.d("communicator", "@@@@ set bg color2 " + colorResource);
				scrollBar.setBackgroundResource(colorResource);
				// scrollBar.invalidate();
				// scrollBar.requestLayout(); }
				scrollBar.invalidate();
				// scrollBar.requestLayout();
			}
		});

	}

	// -------------------------------------------------------------------------
	// -------------------------------------------------------------------------

	private boolean isNoHangNeeded() {
		if (noHangListener == null) {
			return false;
		}
		return noHangListener.noHangNeeded();
	}
	// -------------------------------------------------------------------------

}
