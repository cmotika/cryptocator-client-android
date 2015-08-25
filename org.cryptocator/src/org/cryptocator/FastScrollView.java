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
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

/**
 * The FastScrollView class is special view containing a ScrollView and a
 * horizontal FastScrollBar at the right side. The internal ScrollView keeps all
 * the children vertically. The FastScrollView keeps track of the size of each
 * child and automatically scrolls to the correct child item (index) if the
 * FastScroll view is touched by the user. This scroll view is able to defer
 * scrolling if a NoHang listener tells to do so!
 * 
 * @author Christian Motika
 * @since 2.1
 * @date 08/23/2015
 */
public class FastScrollView extends LinearLayout {

	/** The vertically inner children. */
	private LinearLayout innerChilds;

	/** The scroll view at the left side. */
	private ScrollViewEx scrollView;

	/** The scroll bar at the right side. */
	private FastScrollBar scrollBar;

	/** The list of all children. */
	private List<View> childList = new ArrayList<View>();

	/**
	 * The heights invalidate to remember when we should need to recompute the
	 * heights of each child.
	 */
	private boolean heightsInvalidate = true;

	/**
	 * The locked position remembers the index of the child currently visible
	 * (at the top). If the position is locked the position can be restored
	 * afterwards using restoreLockedPosition().
	 */
	int lockedPos = -1;

	/** The scroll bar visibility was set. The default is false on creation. */
	private boolean scrollBarVisibilitySet = false;

	/** The scroll bar is visible. */
	private boolean scrollBarVisible = false;

	/** The cached heights of the children. */
	public List<Integer> heights = new ArrayList<Integer>();

	/** The cached heights sum of all children. */
	public int heightsSum = 0;

	// -------------------------------------------------------------------------

	/** The layout done listener. */
	private OnLayoutDoneListener layoutDoneListener;

	/** The scroll listener. */
	private OnScrollListener scrollListener;

	/** The size change listener. */
	private OnSizeChangeListener sizeChangeListener;

	/** The no hang listener. */
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
		 * On scroll changed.
		 * 
		 * @param fastScrollView
		 *            the fast scroll view
		 * @param x
		 *            the x
		 * @param y
		 *            the y
		 * @param oldx
		 *            the oldx
		 * @param oldy
		 *            the oldy
		 * @param percent
		 *            the percent
		 * @param item
		 *            the item
		 */
		void onScrollChanged(FastScrollView fastScrollView, int x, int y,
				int oldx, int oldy, int percent, int item);

		/**
		 * On scroll rested.
		 * 
		 * @param fastScrollView
		 *            the fast scroll view
		 * @param x
		 *            the x
		 * @param y
		 *            the y
		 * @param oldx
		 *            the oldx
		 * @param oldy
		 *            the oldy
		 * @param percent
		 *            the percent
		 * @param item
		 *            the item
		 */
		void onScrollRested(FastScrollView fastScrollView, int x, int y,
				int oldx, int oldy, int percent, int item);

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

		/**
		 * On oversroll.
		 * 
		 * @param up
		 *            the up
		 */
		void onOversroll(boolean up);
	}

	// -------------------------------------------------------------------------

	/**
	 * The listener interface for receiving onSizeChange events. The class that
	 * is interested in processing a onSizeChange event implements this
	 * interface, and the object created with that class is registered with a
	 * component using the component's
	 * <code>addOnSizeChangeListener<code> method. When
	 * the onSizeChange event occurs, that object's appropriate
	 * method is invoked.
	 * 
	 * @see OnSizeChangeEvent
	 */
	public interface OnSizeChangeListener {

		/**
		 * On Size Change.
		 * 
		 * @param w
		 *            the w
		 * @param h
		 *            the h
		 * @param oldw
		 *            the oldw
		 * @param oldh
		 *            the oldh
		 */
		void onSizeChange(int w, int h, int oldw, int oldh);
	}

	// -------------------------------------------------------------------------

	/**
	 * The listener interface for receiving onNoHang events. The class that is
	 * interested in processing a onNoHang event implements this interface, and
	 * the object created with that class is registered with a component using
	 * the component's <code>addOnNoHangListener<code> method. When
	 * the onNoHang event occurs, that object's appropriate
	 * method is invoked.
	 * 
	 * @see OnNoHangEvent
	 */
	public interface OnNoHangListener {

		/**
		 * Compute if no hang is needed. ATTENTION: DO COMPUTATION VERY, VERY
		 * LIGHTWEIGHT!
		 * 
		 * @return true, if successful
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

	/**
	 * Sets the on scroll listener.
	 * 
	 * @param onScrollListener
	 *            the new on scroll listener
	 */
	public void setOnScrollListener(OnScrollListener onScrollListener) {
		this.scrollListener = onScrollListener;
	}

	// -------------------------------------------------------------------------

	/**
	 * Sets the on size change listener.
	 * 
	 * @param onSizeChangeListener
	 *            the new on size change listener
	 */
	public void setOnSizeChangeListener(
			OnSizeChangeListener onSizeChangeListener) {
		this.sizeChangeListener = onSizeChangeListener;
	}

	// -------------------------------------------------------------------------

	/**
	 * Sets the on no hang listener.
	 * 
	 * @param onNoHangListener
	 *            the new on no hang listener
	 */
	public void setOnNoHangListener(OnNoHangListener onNoHangListener) {
		this.noHangListener = onNoHangListener;
	}

	// -------------------------------------------------------------------------
	// -------------------------------------------------------------------------

	/**
	 * Instantiates a new fast scroll view.
	 * 
	 * @param context
	 *            the context
	 * @param attrs
	 *            the attrs
	 */
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
	 */
	public FastScrollView(Context context) {
		super(context);
		createInnerViews(context);
	}

	// -------------------------------------------------------------------------

	/**
	 * Creates the inner views.
	 * 
	 * @param context
	 *            the context
	 */
	private void createInnerViews(Context context) {
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

		// DEBUGGING
		// this.setBackgroundColor(Color.rgb(255, 0, 0));
		// scrollView.setBackgroundColor(Color.rgb(0, 255, 0));
		// scrollBar.setBackgroundColor(Color.rgb(0, 0, 255));

		// Do not show standard scrollbar
		scrollView.setVerticalScrollBarEnabled(false);
		scrollView.setHorizontalScrollBarEnabled(false);

		// If the fast scrollbar is used for scrolling, scroll to the correct
		// item (derived by the percent)
		scrollBar.setOnScrollListener(new FastScrollBar.OnScrollListener() {
			public void onScroll(int percent) {
				// Log.d("communicator", "PPPPPP FastScrollView.onScroll()");
				scrollToPercent(percent);
			}

			public void onSnapScroll(int percent, boolean snappedDown,
					boolean snappedUp) {
				if (scrollListener != null) {
					scrollListener
							.onSnapScroll(percent, snappedDown, snappedUp);
				}
			}
		});

		// This scroll view is able to defer scrolling if NoHang listener tells
		// to do so!
		scrollView.setScrollViewListener(new ScrollViewExListener() {
			public void onScrollChanged(final ScrollViewEx scrollView, int x,
					final int y, int oldx, int oldy) {
				if (isNoHangNeeded()) {
					deferredScrolling = y;
					// Log.d("communicator",
					// "PPPPPP NOHANG FastScrollView.onScrollChanged()");
					return;
				}
				// Log.d("communicator",
				// "PPPPPP FastScrollView.onScrollChanged()");

				int percent = getPosToPercent(y);
				updateFastScrollBar(percent, false);

				if (scrollListener != null) {
					int item = getPosToItem(y);
					scrollListener.onScrollChanged(instance, x, y, oldx, oldy,
							percent, item);
				}

			}

			public void onScrollRested(ScrollViewEx scrollView, int x, int y,
					int oldx, int oldy) {
				Log.d("communicator", "PPPPPP FastScrollView.onScrollRested()");
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

		// "reset" heights
		heightsInvalidate = true;
	}

	// -------------------------------------------------------------------------

	/**
	 * Scroll to item index.
	 * 
	 * @param item
	 *            the item
	 */
	public void scrollToItem(int item) {
		int pos = getItemToPos(item);
		int percent = getPosToPercent(pos);
		scrollToPercent(percent);
	}

	// -------------------------------------------------------------------------

	/**
	 * Scroll to percent.
	 * 
	 * @param percent
	 *            the percent
	 */
	public void scrollToPercent(int percent) {
		int pos = getPercentToPos(percent);
		scrollView.scrollTo(0, pos);
		updateFastScrollBar(percent, true);
	}

	// -------------------------------------------------------------------------

	/**
	 * Scroll up completely.
	 */
	public void scrollUp() {
		scrollToPercent(0);
	}

	// -------------------------------------------------------------------------

	/**
	 * Scroll down completely.
	 */
	public void scrollDown() {
		scrollToPercent(100);
	}

	// -------------------------------------------------------------------------

	/**
	 * Reset fast scroll bar. This is only internally used.
	 */
	private void resetFastScrollBar() {
		scrollBar.setProgress(0);
		// 100% percent is the maximum of the fast scroll bar
		int maxPos = 100;
		scrollBar.setMax(maxPos);
	}

	// -------------------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.view.View#onSizeChanged(int, int, int, int)
	 */
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		// CANNOT DO THE CHECK FOR NOHANG HERE BECAUSE OTHERWISE WE CANNOT REACT
		// E.G. WITH SCROLLING! DO NOT SCROLL IN ONSIZECHANGELISTENER DIRECTLY
		// IMPLEMET YOUR OWN STRATEGY TO WAIT UNTIL NO NOHANG ANY MORE!
		if (sizeChangeListener != null) {
			sizeChangeListener.onSizeChange(w, h, oldw, oldh);
		}
		// should update the scrollbar but cannot do this here because no
		// new layout heights are not there yet and would be 0 (zero). wait for
		// the onLayout()!
	}

	// -------------------------------------------------------------------------

	/**
	 * Update scroll bar and possibly hide of show it depending on status
	 * change. If a scroll bar is not necessary because the height of the
	 * children is smaller than the visible height than no scroll bar will be
	 * shown.
	 */
	private void updateScrollBar() {
		Log.d("communicator", "PPPPPP FastScrollView.onSizeChanged()");
		if (getMaxPosition() > 0 && getMaxPosition() <= getVisibleHeight()
				&& (scrollBarVisible || !scrollBarVisibilitySet)) {
			scrollBarVisibilitySet = true;
			scrollBarVisible = false;
			scrollBar.setVisibility(View.GONE);
			heightsInvalidate = true;
		} else if (getMaxPosition() > getVisibleHeight()
				&& (!scrollBarVisible || !scrollBarVisibilitySet)) {
			scrollBar.setVisibility(View.VISIBLE);
			scrollBarVisibilitySet = true;
			scrollBarVisible = true;
			heightsInvalidate = true;
		}
	}

	// -------------------------------------------------------------------------

	/**
	 * Get the percent converted to scroll position (sum of heights of
	 * children).
	 * 
	 * @param percent
	 *            the percent
	 * @return the percent to pos
	 */
	public int getPercentToPos(int percent) {
		int maxPos = getMaxHeight();
		int pos = (percent * maxPos) / 100;

		return pos;
	}

	// -------------------------------------------------------------------------

	/**
	 * Get the scroll position (sum of heights of children) converted to
	 * percent.
	 * 
	 * @param pos
	 *            the pos
	 * @return the pos to percent
	 */
	public int getPosToPercent(int pos) {
		int maxPos = getMaxHeight();
		int percent = (pos * 100) / maxPos;
		return percent;
	}

	// -------------------------------------------------------------------------

	/**
	 * Update the internal fast scroll bar if the scrollBar is not locked or
	 * override is set. This must invalidate the heights.
	 * 
	 * @param percent
	 *            the percent
	 * @param override
	 *            the override
	 */
	private void updateFastScrollBar(int percent, boolean override) {
		if (!scrollBar.isLocked() || override) {
			scrollBar.setMax(100);
			scrollBar.setProgress(100 - percent);
			heightsInvalidate = true;
		}
	}

	// -------------------------------------------------------------------------

	/**
	 * Add a child to the list of children. This must invalidate the heights.
	 * 
	 * @param view
	 *            the view
	 */
	public void addChild(View view) {
		childList.add(view);
		innerChilds.addView(view);
		heightsInvalidate = true;
	}

	// -------------------------------------------------------------------------

	/**
	 * Clear children. This must invalidate the heights.
	 */
	public void clearChilds() {
		childList.clear();
		if (innerChilds != null) {
			innerChilds.removeAllViews();
		}
		heightsInvalidate = true;
	}

	// -------------------------------------------------------------------------

	/** The scrolling was deferred if this is a value != -1. */
	int deferredScrolling = -1;

	/**
	 * Potentially refresh state if during an onLayout() method, this
	 * calculation was skipped because of isNoHangNeeded() was true. Update the
	 * scroll bar now, at a later time (when this method is called). This method
	 * should only be called if user for example is not currently typing fast/
	 */
	public void potentiallyRefreshState() {
		if (deferredScrolling != -1) {
			int percent = getPosToPercent(deferredScrolling);
			updateFastScrollBar(percent, false);
			deferredScrolling = -1;
		}
	}

	// -------------------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.widget.LinearLayout#onLayout(boolean, int, int, int, int)
	 */
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		super.onLayout(changed, l, t, r, b);
		Log.d("communicator", "PPPPPP FastScrollView.onLayout()");
		if (!isNoHangNeeded()) {
			updateScrollBar();
			if (layoutDoneListener != null) {
				layoutDoneListener.doneLayout();
			}
		}
		// IT SEEMS TO BE NECESSARY TO STILL UPDATE THE MEASUREMENTS!!!
		// This should not be deferred like the scrolling
		measureHeights();
		changed = false;
	}

	// -------------------------------------------------------------------------

	/**
	 * Measure the heights if this is necessary. This is necessary if the
	 * heightsInvalidate flag was set to true (e.g., after adding a child).
	 */
	private void measureHeights() {
		if (!heightsInvalidate) {
			return;
		}
		heightsInvalidate = false;
		heights.clear();
		heightsSum = 0;
		for (View layout : childList) {
			int h = layout.getMeasuredHeight();
			heights.add(h);
			heightsSum += h;
		}
	}

	// ------------------------------------------------------------------------

	/**
	 * Get the scroll position (sum of heights of children) for a specific child
	 * element index.
	 * 
	 * @param item
	 *            the item
	 * @return the position by scroll item
	 */
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

	/**
	 * Get the child item index from a scroll position (sum of heights of
	 * children).
	 * 
	 * @param pos
	 *            the pos
	 * @return the pos to item
	 */
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
	 * Get the scroll position (sum of heights of children) for the last
	 * (maximum) child element.
	 * 
	 * @return the max position
	 */
	public int getMaxPosition() {
		return heightsSum;
	}

	// ------------------------------------------------------------------------

	/**
	 * Get the maximum height of the filled inner child area.
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
	 * Get the visible height.
	 * 
	 * @return the visible height
	 */
	private int getVisibleHeight() {
		return scrollView.getHeight();
	}

	// -------------------------------------------------------------------------

	/**
	 * Lock the position of this fast scroll view. With restoreLockedPosition()
	 * this scroll position can be restored at a later time. This should be
	 * called when adding something to the scrollview but if the scroll position
	 * should be kept.
	 */
	public void lockPosition() {
		lockedPos = this.scrollView.getScrollY();
	}

	// -------------------------------------------------------------------------

	/**
	 * Checks if is scroll is currently locked.
	 * 
	 * @return true, if is locked
	 */
	public boolean isLocked() {
		return lockedPos != -1;
	}

	// -------------------------------------------------------------------------

	/**
	 * Restore the previously locked position. The position was set by calling
	 * lockPosition().
	 */
	public void restoreLockedPosition() {
		final int lockedPosCopy = lockedPos;
		lockedPos = -1;
		this.postDelayed(new Runnable() {
			public void run() {
				resetFastScrollBar();
				int percent = getPosToPercent(lockedPosCopy);

				scrollToPercent(percent);
			}
		}, 200);
	}

	// -------------------------------------------------------------------------

	/**
	 * Sets the scroll bar background to a specific color.
	 * 
	 * @param colorResource
	 *            the new scroll background
	 */
	public void setScrollBackground(final int colorResource) {
		final Handler mUIHandler = new Handler(Looper.getMainLooper());
		mUIHandler.post(new Thread() {
			@Override
			public void run() {
				super.run();
				scrollBar.setBackgroundResource(colorResource);
				scrollBar.invalidate();
			}
		});

	}

	// -------------------------------------------------------------------------

	/**
	 * Checks if is no hang needed. If no noHang listener has been established
	 * then always return false.
	 * 
	 * @return true, if is no hang needed
	 */
	private boolean isNoHangNeeded() {
		if (noHangListener == null) {
			return false;
		}
		return noHangListener.noHangNeeded();
	}

	// -------------------------------------------------------------------------

	/**
	 * Sets the snap down value, if > snapDown% then snap to 100%. This value is
	 * forwarded to the internal FastScrollBar.
	 * 
	 * @param snapDownLimit
	 *            the new snap down
	 */
	public void setSnapDown(int snapDownLimit) {
		scrollBar.setSnapDown(snapDownLimit);
	}

	// -------------------------------------------------------------------------

	/**
	 * Sets the snap up value, if < snapUp% then snap to 0%. This value is
	 * forwarded to the internal FastScrollBar.
	 * 
	 * @param snapUpLimit
	 *            the new snap up
	 */
	public void setSnapUp(int snapUpLimit) {
		scrollBar.setSnapUp(snapUpLimit);
	}

	// -------------------------------------------------------------------------

}
