@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

/**
 * @file AbstractRestClient.kt
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


package com.ionoclast.kotlin.net

import android.util.Log
import com.google.gson.GsonBuilder
import com.ionoclast.kotlin.coroutine.AndroidUI
import com.ionoclast.kotlin.coroutine.task
import com.ionoclast.kotlin.serialization.DateDeserializer
import com.sdge.emergencymanagement.BuildConfig
import kotlinx.coroutines.experimental.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.lang.Exception
import java.util.*
import java.util.concurrent.TimeUnit


fun networkTask(block: suspend CoroutineScope.()->Unit) = launch(AndroidUI, block = block)

/**
 * *Abstract base class for Android REST API clients in Kotlin 1.1+.*
 *
 * To implement an API client using this class, create your endpoint interfaces as usual,
 * and wrap them in suspending functions that call `makeRequest(Job, Call<T>)`.
 *
 * To use these APIs, create a coroutine block that will continue on the UI thread, such
 * as by using the supplied `networkTask()` function. If you need to make sequential
 * calls, such as pulling a list of IDs and querying one or more with a second call, or
 * prefetching a token to use with another call, you can do that within the coroutine
 * block; the will be executed in order, with results returned on the UI thread as if
 * it were all synchronously executed. If you want multiple concurrent calls, they
 * can be spawned from within multiple coroutine blocks.
 *
 * You should pre-allocate a Job instance for your Fragment or Activity, and pass that
 * to your API calls, so they can all be cancelled appropriately with the Android app
 * lifecycle, on user request, after a timeout, etc.
 *
 * @see Retrofit
 * @see makeRequest
 * @see networkTask
 * @see Job
 *
 * @author btoskin &lt;brigham@ionoclast.com&gt;
 */
abstract class AbstractRestClient {
    companion object {
        private val TAG = AbstractRestClient::class.java.simpleName
    }


    data class Response<out T>(val result: T? = null, val err: Throwable? = null)

    class ApiError(msg: String, cause: Throwable? = null) : IOException(msg, cause)


    abstract val BASE_URI: String
    abstract val TIMEOUT_SECS: Long?

    protected val restClient by lazy { buildClientAdapter() }

    /**
     * *Executes the given `Call` in a coroutine on a worker thread, cancellable via the given `Job`.*
     *
     * This method is the secret sauce to implementing a clean and small API client. Once you have
     * a retrofit API service interface instance, just pass it in here, along with a `Job`. If
     * `cancel()` is called on the job, the background coroutine will be canceled, which will
     * terminate the network request.
     *
     * @see Retrofit.create
     * @see Job
     */
    protected suspend fun <T> makeRequest(job: Job, call: Call<T>) = try {
        val response = task(CommonPool + job) { call.execute()  }
        response.takeIf { it.isSuccessful } ?.let { Response(it.body()) }
                ?: Response(err = ApiError("API Error (${response.code()}): ${response.errorBody()}"))
    } catch (cancelled: CancellationException) {
        Log.d(TAG, "API call cancelled: ${call.request().url()}")
        call.cancel()
        Response<T>(err = cancelled)
    } catch (ex: Exception) {
        Response<T>(err = ApiError("API call failed: ${call.request().url()}", ex))
    }

    /**
     * *Builds a reasonable default API adapter.*
     *
     * Initializes the following networking components and features:
     * * Uses OkHttp for the underlying HTTP client implementation
     * * Registers a verbose logging interceptor in debug builds
     * * Sets connection and read timeouts as specified by the class implementation
     * * Registers a GSON decoder to inflate your models, with a `Date` helper class
     * * Binds the adapter to the base URI as specified by the class implementation
     *
     * You may override this method, if you need to add more hooks, or more fine-grained
     * control over how the components are configured.
     *
     * @see TIMEOUT_SECS
     * @see BASE_URI
     * @see DateDeserializer
     */
    protected fun buildClientAdapter(): Retrofit {
        val logger = HttpLoggingInterceptor()
        logger.level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        val client = OkHttpClient.Builder()
                .addInterceptor(logger)
                .connectTimeout(TIMEOUT_SECS ?: 0L, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECS ?: 0L, TimeUnit.SECONDS)
                .build()
        val gson = GsonBuilder().registerTypeAdapter(Date::class.java, DateDeserializer()).create()
        val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URI)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
        return retrofit
    }
}