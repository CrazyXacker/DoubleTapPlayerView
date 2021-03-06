package com.github.vkay94.dtpv

import android.content.Context
import android.graphics.drawable.AnimationDrawable
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.google.android.exoplayer2.Player

class YouTubeDoubleTap(context: Context?, attrs: AttributeSet?) : ConstraintLayout(context, attrs),
    PlayerDoubleTapListener {

    companion object {
        const val TAG = ".YouTubeDoubleTap"
        const val DEBUG = BuildConfig.BUILD_TYPE != "release"
    }

    // Layout-Views
    var tvForward: TextView
    var tvRewind: TextView
    var forwardContainer: FrameLayout
    var rewindContainer: FrameLayout

    // Animations
    var forwardAnimation: AnimationDrawable? = null
    var rewindAnimation: AnimationDrawable? = null

    // Player behaviors
    private var playerView: DoubleTapPlayerView? = null
    private var player: Player? = null
    private var seekListener: SeekListener

    private var FAST_FORWARD_REWIND_SKIP = 10000

    // YouTube specific
    private var currentRewindForward = 0

    init {
        LayoutInflater.from(context).inflate(R.layout.yt_overlay, this, true)
        hide()

        // Initialize UI components
        tvForward = findViewById(R.id.tvForward)
        tvRewind = findViewById(R.id.tvRewind)
        forwardContainer = findViewById(R.id.forwardFrameLayout)
        rewindContainer = findViewById(R.id.rewindFrameLayout)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            forwardAnimation =
                ContextCompat.getDrawable(
                    context!!,
                    R.drawable.yt_forward_animation
                ) as AnimationDrawable
            rewindAnimation =
                ContextCompat.getDrawable(
                    context,
                    R.drawable.yt_rewind_animation
                ) as AnimationDrawable

            tvForward.setCompoundDrawablesWithIntrinsicBounds(null, forwardAnimation, null, null)
            tvRewind.setCompoundDrawablesWithIntrinsicBounds(null, rewindAnimation, null, null)
        }

        // Click listeners
        rewindContainer.setOnClickListener {
            currentRewindForward += FAST_FORWARD_REWIND_SKIP / 1000
            tvRewind.text =
                resources.getQuantityString(R.plurals.dtp_rf_seconds, currentRewindForward, currentRewindForward)

            seekToPosition(player?.currentPosition!!.minus(FAST_FORWARD_REWIND_SKIP))
        }

        forwardContainer.setOnClickListener {
            currentRewindForward += FAST_FORWARD_REWIND_SKIP / 1000
            tvForward.text =
                resources.getQuantityString(R.plurals.dtp_rf_seconds, currentRewindForward, currentRewindForward)

            seekToPosition(player?.currentPosition!!.plus(FAST_FORWARD_REWIND_SKIP))
        }

        // Listener for default behavior (stop double tap mode) when video end / start reached
        seekListener = object : SeekListener {
            override fun onVideoStartReached() {
                super.onVideoStartReached()
                player?.seekTo(0)

                // Disable ongoing clicks on rewind container
                rewindContainer.isClickable = false
            }

            override fun onVideoEndReached(totalDuration: Long) {
                super.onVideoEndReached(totalDuration)
                player?.seekTo(totalDuration)

                // Disable ongoing clicks on forward container
                forwardContainer.isClickable = false
            }
        }
    }

    /**
     * Obligatory call!
     * Links the DoubleTapPlayerView to this view for performing seekTo-calls
     * and hiding the controller
     *
     * @param playerView PlayerView which triggers the event
     */
    fun setPlayer(playerView: DoubleTapPlayerView): YouTubeDoubleTap {
        this.playerView = playerView
        this.player = playerView.player
        return this
    }

    /**
     * Optional: Set a listener to observe whether double tap reached
     * the start / end of the video
     */
    fun setSeekListener(seekListener: SeekListener): YouTubeDoubleTap {
        this.seekListener = seekListener
        return this
    }

    /**
     * Sets the forward / rewind steps, default: 10 seconds
     */
    fun setForwardRewindIncrementMs(milliseconds: Int): YouTubeDoubleTap {
        this.FAST_FORWARD_REWIND_SKIP = milliseconds
        return this
    }

    // PlayerDoubleTapListener methods
    override fun onDoubleTapStarted(posX: Float, posY: Float) {
        if (DEBUG) Log.d(TAG, "onDoubleTapStarted: $posX")
    }

    override fun onDoubleTapProgressUp(posX: Float, posY: Float) {
        if (DEBUG) Log.d(TAG, "onDoubleTapProgressUp: $posX")

        // Method called when entering double tapping or already in double tap mode
        // and tapping area is different
        // (for example: started left and then right/middle)

        if (player?.currentPosition == null || playerView?.width == null) return

        // Check first whether forwarding/rewinding is "valid"
        player?.currentPosition?.let {current ->
            // Start of the video and rewind
            if (posX < playerView?.width!! * 0.35 && current <= 500)
                return

            // End of the video and forward
            if (posX > playerView?.width!! * 0.65 && current == player?.duration)
                return
        }

        // Re-enable clicks on the containers
        forwardContainer.isClickable = true
        rewindContainer.isClickable = true

        // YouTube behavior: show overlay on MOTION_UP and hide controls then
        if (this.visibility == View.GONE) {
            playerView?.useController = false
            show()
        }

        currentRewindForward = FAST_FORWARD_REWIND_SKIP / 1000

        val value = when {
            posX < playerView?.width!! * 0.35 -> {
                if (forwardContainer.visibility == View.VISIBLE)
                    forwardContainer.visibility = View.INVISIBLE

                tvRewind.text =
                    resources.getQuantityString(R.plurals.dtp_rf_seconds, currentRewindForward, currentRewindForward)

                rewindContainer.visibility = View.VISIBLE
                rewindAnimation?.start()

                -1
            }
            posX > playerView?.width!! * 0.65 -> {

                if (rewindContainer.visibility == View.VISIBLE)
                    rewindContainer.visibility = View.INVISIBLE

                tvForward.text =
                    resources.getQuantityString(R.plurals.dtp_rf_seconds, currentRewindForward, currentRewindForward)

                forwardContainer.visibility = View.VISIBLE
                forwardAnimation?.start()

                1
            }
            else -> {
                playerView?.cancelInDoubleTapMode()
                return
            }
        }

        val newPosition = player?.currentPosition?.plus(value * FAST_FORWARD_REWIND_SKIP)
        newPosition?.let { seekToPosition(it) }
    }

    override fun onDoubleTapFinished() {
        if (DEBUG) Log.d(TAG, "onDoubleTapFinished")

        // Hide overlay and re-add the controller to the player and show controls
        // if the player was paused previously
        playerView?.useController = true
        hide()

        if (!player?.playWhenReady!!) playerView?.showController()

        // Set overlay to its original state
        rewindContainer.visibility = View.INVISIBLE
        forwardContainer.visibility = View.INVISIBLE

        forwardAnimation?.stop()
        rewindAnimation?.stop()
    }

    /**
     * Seeks the video to desired position.
     * Calls interface functions when start reached ([SeekListener.onVideoStartReached])
     * or when end reached ([SeekListener.onVideoEndReached])
     *
     * @param newPosition desired position
     */
    private fun seekToPosition(newPosition: Long) {

        // Start of the video reached
        if (newPosition <= 0) {
            seekListener.onVideoStartReached()
            return
        }

        // End of the video reached
        player?.duration?.let { total ->
            if (newPosition >= total) {
                seekListener.onVideoEndReached(total)
                return
            }
        }

        // Otherwise
        playerView?.keepInDoubleTapMode()
        player?.seekTo(newPosition)
    }

    fun hide() {
        this.visibility = View.GONE
    }

    fun show() {
        this.visibility = View.VISIBLE
    }

    interface SeekListener {
        /**
         * Called when video start reached during rewinding
         */
        fun onVideoStartReached() {
            if (DEBUG) Log.d(TAG, "SeekListener: onVideoStartReached")
        }

        /**
         * Called when video end reached during forwarding
         */
        fun onVideoEndReached(totalDuration: Long) {
            if (DEBUG) Log.d(TAG, "SeekListener: onVideoEndReached: totalDuration = $totalDuration")
        }
    }
}
