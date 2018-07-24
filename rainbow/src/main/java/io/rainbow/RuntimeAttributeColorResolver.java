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

package io.rainbow;

import android.content.Context;
import android.content.res.ColorStateList;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.LayoutInflater;

import java.lang.reflect.Field;

import kotlin.text.StringsKt;

public abstract class RuntimeAttributeColorResolver {

    private final SparseArray<String> mAttrValueToNameMap;

    /**
     * @param rAttrClass The attribute class to use to resolve attribute values. i.e. R.attr.class
     */
    public RuntimeAttributeColorResolver(Class rAttrClass) {
        Field[] declaredFields = rAttrClass.getDeclaredFields();
        mAttrValueToNameMap = new SparseArray<>(declaredFields.length);
        for (Field field : declaredFields) {
            try {
                int attrValue = field.getInt(null);
                String attrName = field.getName();
                mAttrValueToNameMap.put(attrValue, attrName);
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * @param context context that can be used when resolving <code>attributeName</code>.
     *                The context is the one supplied by the {@link LayoutInflater#getContext()}
     * @return The color value that was resolved for <code>attributeName</code> or null if no value exists
     */

    public abstract @Nullable
    ColorStateList getColorStateListByAttrName(@NonNull Context context, @Nullable String attributeName);

    /**
     * @param context context that can be used when resolving <code>attributeName</code>.
     *                The context is the one supplied by the {@link LayoutInflater#getContext()}
     * @return The color value that was resolved for <code>attributeName</code> or null if no value exists
     */
    public abstract @Nullable
    Integer getColorByAttrName(@NonNull Context context, @Nullable String attributeName);

    ColorStateList getColorStateListByAttrValue(@NonNull Context context, String colorAttrStringValue) {
        String colorAttrName = getAttrName(colorAttrStringValue);
        if (colorAttrName == null) {
            return null;
        }
        return getColorStateListByAttrName(context, colorAttrName);
    }

    Integer getColorByAttrValue(@NonNull Context context, @Nullable String colorAttrStringValue) {
        String colorAttrName = getAttrName(colorAttrStringValue);
        if (colorAttrName == null) {
            return null;
        }
        return getColorByAttrName(context, colorAttrName);
    }

    @Nullable
    private String getAttrName(@Nullable String colorAttrStringValue) {
        String colorAttrName = null;
        if (!TextUtils.isEmpty(colorAttrStringValue)) {
            Integer colorAttrValue;
            colorAttrValue = StringsKt.toIntOrNull(colorAttrStringValue.substring(1));
            colorAttrName = colorAttrValue != null ? mAttrValueToNameMap.get(colorAttrValue) : null;
        }
        if (colorAttrName == null) {
            return null;
        }
        return colorAttrName;
    }

}
