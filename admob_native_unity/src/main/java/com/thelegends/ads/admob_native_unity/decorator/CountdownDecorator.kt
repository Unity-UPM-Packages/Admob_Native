package com.thelegends.ads.admob_native_unity.decorator

import android.app.Activity
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.google.android.gms.ads.nativead.NativeAd
import com.orbitalsonic.sonictimer.SonicCountDownTimer
import com.thelegends.admob_native_unity.NativeAdCallbacks
import com.thelegends.ads.admob_native_unity.R
import com.thelegends.ads.admob_native_unity.showbehavior.*

class CountdownDecorator(
    private val wrappedBehavior: BaseShowBehavior,
    private val initialDelaySeconds: Float,
    private val countdownDurationSeconds: Float,
    private val closeButtonDelaySeconds: Float
) : BaseShowBehavior() {

    private var initialDelayTimer: SonicCountDownTimer? = null
    private var countdownTimer: SonicCountDownTimer? = null
    private var closeButtonDelayTimer: SonicCountDownTimer? = null

    private val countdownTimerDurationMillis = (countdownDurationSeconds * 1000).toLong()
    private val initialDelayBeforeCountdownMillis = (initialDelaySeconds * 1000).toLong()
    private val closeButtonClickableDelayMillis = (closeButtonDelaySeconds * 1000).toLong()

    override fun show(
        activity: Activity,
        nativeAd: NativeAd,
        layoutName: String,
        callbacks: NativeAdCallbacks
    ) {
        activity.runOnUiThread {
            wrappedBehavior.show(activity, nativeAd, layoutName, callbacks)
            val view = wrappedBehavior.rootView
            view?.let {
                startCloseLogic(it, callbacks)
            }
        }
    }

    override fun destroy() {
        initialDelayTimer?.cancelCountDownTimer()
        initialDelayTimer = null

        countdownTimer?.cancelCountDownTimer()
        countdownTimer = null

        closeButtonDelayTimer?.cancelCountDownTimer()
        closeButtonDelayTimer = null

        wrappedBehavior.destroy()
    }

    private fun startCloseLogic(rootView: View, callbacks: NativeAdCallbacks) {
        val closeButton = rootView.findViewById<View>(R.id.ad_close_button)
        val progressBar = rootView.findViewById<ProgressBar>(R.id.ad_progress_bar)
        val countdownText = rootView.findViewById<TextView>(R.id.ad_countdown_text)

        // Cancel any existing timers
        initialDelayTimer?.cancelCountDownTimer()
        countdownTimer?.cancelCountDownTimer()
        closeButtonDelayTimer?.cancelCountDownTimer()

        // PHASE 1: Initial state - Hide everything
        closeButton?.visibility = View.GONE
        closeButton?.alpha = 1.0f
        progressBar?.visibility = View.GONE
        countdownText?.visibility = View.GONE
        closeButton?.isClickable = false

        // TIMER 1: Initial delay before showing progress/countdown
        initialDelayTimer = object : SonicCountDownTimer(initialDelayBeforeCountdownMillis, 100) {
            override fun onTimerTick(timeRemaining: Long) {
                // Silent countdown, no UI updates
            }

            override fun onTimerFinish() {
                startMainCountdown(closeButton, progressBar, countdownText)
            }
        }
        initialDelayTimer?.startCountDownTimer()

        // Setup close button click listener (will only work when enabled)
        closeButton?.setOnClickListener {
            if (closeButton.isClickable) {
                callbacks.onAdClosed()
                destroy()
            }
        }
    }

    private fun startMainCountdown(
        closeButton: View?,
        progressBar: ProgressBar?,
        countdownText: TextView?
    ) {
        val tag = progressBar?.tag as? String ?: ""
        val isLineFill = tag.contains("line_fill") || tag.contains("reverse")

        if (isLineFill) {
            progressBar?.progress = 0
        } else {
            progressBar?.progress = 100
        }

        progressBar?.visibility = View.VISIBLE
        countdownText?.visibility = View.VISIBLE
        closeButton?.visibility = View.GONE
        closeButton?.alpha = 0.5f

        countdownTimer = object : SonicCountDownTimer(countdownTimerDurationMillis, 16) {
            override fun onTimerTick(timeRemaining: Long) {
                val secondsRemaining = (timeRemaining / 1000).toInt()

                // Stop showing countdown when it reaches 0, move to next phase immediately
                if (timeRemaining <= 0) {
                    onTimerFinish()
                    return
                }

                if (secondsRemaining <= 2) {
                    closeButton?.visibility = View.VISIBLE
                    closeButton?.alpha = 0.5f
                    closeButton?.isClickable = false
                } else {
                    closeButton?.visibility = View.GONE
                }

                countdownText?.text = (secondsRemaining + 1).toString()

                if (isLineFill) {
                    val elapsedTime = countdownTimerDurationMillis - timeRemaining
                    val progressPercent =
                        ((elapsedTime * 100) / countdownTimerDurationMillis).toInt().coerceIn(0, 100)
                    progressBar?.progress = progressPercent
                } else {
                    val progressPercent =
                        (timeRemaining * 100 / countdownTimerDurationMillis).toInt().coerceAtLeast(0)
                    progressBar?.progress = progressPercent
                }
            }

            override fun onTimerFinish() {
                if (isLineFill) {
                    progressBar?.progress = 100
                }
                startCloseButtonDelay(closeButton, progressBar, countdownText)
            }
        }
        countdownTimer?.startCountDownTimer()
    }

    private fun startCloseButtonDelay(
        closeButton: View?,
        progressBar: ProgressBar?,
        countdownText: TextView?
    ) {
        val tag = progressBar?.tag as? String ?: ""
        val keepVisible = tag.contains("line_fill") || tag.contains("keep")

        if (!keepVisible) {
            progressBar?.visibility = View.GONE
        } else {
            progressBar?.visibility = View.VISIBLE
            progressBar?.progress = 100
        }

        countdownText?.visibility = View.GONE
        closeButton?.visibility = View.VISIBLE
        closeButton?.alpha = 1.0f
        closeButton?.isClickable = false


        // TIMER 3: Close button clickable delay
        closeButtonDelayTimer = object : SonicCountDownTimer(closeButtonClickableDelayMillis, 100) {
            override fun onTimerTick(timeRemaining: Long) {
                // Silent countdown, no UI updates
            }

            override fun onTimerFinish() {
                closeButton?.isClickable = true
            }
        }
        closeButtonDelayTimer?.startCountDownTimer()
    }

}