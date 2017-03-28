/**
 * @file AndroidCouroutineUtil.kt
 * @author btoskin &lt;brigham@ionoclast.com&gt;
 *
 * Copyright © 2017 Ionoclast Laboratories, LLC.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */


package com.ionoclast.kotlin.coroutine

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.run
import kotlin.coroutines.experimental.AbstractCoroutineContextElement
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.ContinuationInterceptor
import kotlin.coroutines.experimental.CoroutineContext


/**
 * *Schedules continuations on the UI thread.*
 */
object AndroidUI : AbstractCoroutineContextElement(ContinuationInterceptor), ContinuationInterceptor {
    override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> = AndroidUiContinuation(continuation)
}

/** @private */
private class AndroidUiContinuation<in T>(val cont: Continuation<T>) : Continuation<T> {
    companion object {
        private val sMainHandler = Handler(Looper.getMainLooper())
    }

    override val context get() = cont.context

    override fun resume(value: T) {
        if(Looper.getMainLooper() === Looper.myLooper()) cont.resume(value)
        else sMainHandler.post { cont.resume(value) }
    }

    override fun resumeWithException(exception: Throwable) {
        if(Looper.getMainLooper() === Looper.myLooper()) cont.resumeWithException(exception)
        else sMainHandler.post { cont.resumeWithException(exception) }
    }
}

/**
 * *Runs the block on with the specified context.*
 * @param context defaults to `CommonPool`
 * @param co suspending closure to execute on `context`
 * @see CommonPool
 */
@Suppress("EXPERIMENTAL_FEATURE_WARNING")
suspend fun <T> task(context: CoroutineContext = CommonPool, co: suspend CoroutineScope.()->T) = run(context, co)
