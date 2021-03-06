package palbp.sempreacodar.bouncingball

import android.graphics.Canvas
import android.view.SurfaceHolder
import androidx.annotation.AnyThread
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import palbp.sempreacodar.bouncingball.physics.Location
import palbp.sempreacodar.bouncingball.physics.Velocity

/**
 * The game engine.
 */
class Engine {

    private fun initializeWorld(arena: ArenaView, bounds: Location) {
        if (arena.ball == null) {
            arena.ball = BouncingBall(
                position = Location(bounds.x/2, bounds.y/2),
                velocity = Velocity(5f, 2f),
                radius = 50f,
                bounds = Location(bounds.x, bounds.y)
            )
        }
    }

    private fun doStep(holder: SurfaceHolder, arena: ArenaView) {
        arena.ball?.move()
        draw(holder, arena)
    }

    private fun draw(holder: SurfaceHolder, arena: ArenaView) {
        var canvas: Canvas? = null
        try {
            canvas = holder.lockCanvas()
            arena.draw(canvas)
        } finally {
            if (canvas != null) {
                // The following is a blocking operation, meaning, the current thread only proceeds
                // after the canvas is actually delivered to the image consumer. The consumption
                // rate is determined by the device's FPS
                holder.unlockCanvasAndPost(canvas)
            }
        }
    }

    /**
     * The engine's worker thread. The property is also used to flag whether the engine is running
     * or not.
     */
    @Volatile
    private var worker: Thread? = null

    /**
     * Indicates whether the game engine is running or not
     */
    val isStarted: Boolean
        @AnyThread get() = worker != null

    /**
     * Starts the game engine
     *
     * @param   holder  The surface holder used to draw the game UI
     * @param   arena   The view that displays the game arena
     */
    @MainThread
    fun start(holder: SurfaceHolder, arena: ArenaView) {

        if (isStarted)
            return

        val bounds = Location(arena.width.toFloat(), arena.height.toFloat())
        worker = kotlin.concurrent.thread(name = "Engine thread") @WorkerThread {
            initializeWorld(arena, bounds)
            while (isStarted) {
                doStep(holder, arena)
            }
        }
    }

    /**
     * Stops the engine, blocking the calling thread until the engine thread has stopped
     */
    @MainThread
    fun stop() {
        val terminatingWorker = worker ?: return
        worker = null
        terminatingWorker.interrupt()
        terminatingWorker.join()
    }
}