/*
 * MIT License
 *
 * Copyright (c) 2018 Alex Fourman
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
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.rainbow

import android.content.Context
import android.support.annotation.AttrRes
import android.util.Log

/**
 * Resolve the color value specified by `attributeId` in regard to the `context`
 */
fun getAttributeColor(context: Context, @AttrRes attributeId: Int): Int? {
    val typedArray = context.obtainStyledAttributes(intArrayOf(attributeId))
    val color = if (typedArray.hasValue(0)) typedArray.getColor(0, 0) else null
    if (color == null) {
        Log.w("Rainbow", "attributeId value could not be resolved. $attributeId")
    }
    typedArray.recycle()
    return color
}
