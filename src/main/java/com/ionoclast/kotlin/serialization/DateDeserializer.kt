/**
 * @file DateDeserializer.kt
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


package com.ionoclast.kotlin.serialization

import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*


/**
 * *Inflates a [Date] object from the given UNIX style epoch timestamp.*
 *
 * @see GsonBuilder.registerTypeAdapter
 */
class DateDeserializer(private val fallbackDateFormat: String?) : JsonDeserializer<Date> {
    private val fmt by lazy { SimpleDateFormat(fallbackDateFormat) }

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Date {
        val timestamp = json.asString

        try {
            return Date(timestamp.toLong() * 1000L)
        } catch (e: Exception) {
            fallbackDateFormat?.let {
                return fmt.parse(timestamp)
            }
            throw ParseException("Unknown date format $timestamp", 0)
        }
    }
}