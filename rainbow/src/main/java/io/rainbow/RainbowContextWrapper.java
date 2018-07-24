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
import android.content.ContextWrapper;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.CompoundButtonCompat;
import android.support.v4.widget.ImageViewCompat;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import kotlin.text.StringsKt;


/**
 * RainbowContextWrapper brings runtime color attribute override in a simple way through
 * calling {@link #wrap(Context, RuntimeAttributeColorResolver)}.
 * <p>
 * Attributes currently supported:
 * {@link android.R.attr#buttonTint}
 * {@link android.R.attr#drawableTint}
 * {@link android.R.attr#textColor}
 * {@link android.R.attr#background}
 * {@link android.R.attr#indeterminateTint}
 * {@link android.R.attr#backgroundTint}
 * {@link android.support.v7.appcompat.R.attr#backgroundTint}
 * {@link android.support.v7.appcompat.R.attr#titleTextColor}
 * {@link android.R.attr#textColorHighlight}
 * {@link android.R.attr#tint}
 * </p>
 */
public final class RainbowContextWrapper
        extends ContextWrapper {

    private final RuntimeAttributeColorResolver mRuntimeAttributeColorResolver;

    private RainbowLayoutInflater mInflater;

    private RainbowContextWrapper(@NonNull Context base, @NonNull RuntimeAttributeColorResolver runtimeAttributeColorResolver) {
        super(base);
        mRuntimeAttributeColorResolver = runtimeAttributeColorResolver;
    }

    /**
     * Call this method from {@link android.app.Activity#attachBaseContext(Context)}.
     * Always use the wrapped context as the param to <code>super.attachBaseContext(wrappedContext)</code>
     * Wrap a context to intercept {@link #getSystemService(String)} calls for {@link Context#LAYOUT_INFLATER_SERVICE}
     * so that we can provide our own {@link LayoutInflater} that will override view attributes with values from {@link RuntimeAttributeColorResolver}
     */
    @NonNull
    public static Context wrap(@NonNull Context context, @NonNull RuntimeAttributeColorResolver runtimeAttributeColorResolver) {
        return new RainbowContextWrapper(context, runtimeAttributeColorResolver);
    }

    @Override
    public Object getSystemService(String name) {
        if (LAYOUT_INFLATER_SERVICE.equals(name)) {
            if (mInflater == null) {
                mInflater = new RainbowLayoutInflater(LayoutInflater.from(getBaseContext()), this, mRuntimeAttributeColorResolver);
            }
            return mInflater;
        }
        return super.getSystemService(name);
    }

    private static final class RainbowLayoutInflater
            extends LayoutInflater {

        /**
         * see com.android.internal.policy.PhoneLayoutInflater#sClassPrefixList
         */
        private static final String[] sClassPrefixList = {"android.widget.", "android.webkit.", "android.app."};

        private RuntimeAttributeColorResolver mRuntimeAttributeColorResolver;

        RainbowLayoutInflater(LayoutInflater layoutInflater, Context newContext, @NonNull RuntimeAttributeColorResolver runtimeAttributeColorResolver) {
            super(layoutInflater, newContext);
            mRuntimeAttributeColorResolver = runtimeAttributeColorResolver;
        }

        @Override
        public LayoutInflater cloneInContext(Context newContext) {
            return new RainbowLayoutInflater(this, newContext, mRuntimeAttributeColorResolver);
        }

        @Override
        public void setFactory(Factory factory) {
            if (factory instanceof FactoryWrapper) {
                super.setFactory(factory);
            } else {
                super.setFactory(new FactoryWrapper(factory));
            }
        }

        @Override
        public void setFactory2(Factory2 factory) {
            if (factory instanceof Factory2Wrapper) {
                super.setFactory2(factory);
            } else {
                super.setFactory2(new Factory2Wrapper(factory));
            }
        }

        @Override
        protected View onCreateView(View parent, String name, AttributeSet attrs) throws
                ClassNotFoundException {
            View view = super.onCreateView(parent, name, attrs);
            if (view != null) {
                onViewCreated(attrs, view);
            }
            return view;
        }

        private void onViewCreated(AttributeSet attrs, View view) {
            if (view == null || attrs == null) {
                return;
            }
            int count = attrs.getAttributeCount();
            for (int i = 0; i < count; i++) {
                int attributeNameResource = attrs.getAttributeNameResource(i);

                //region buttonTint
                if (view instanceof CompoundButton && (attributeNameResource == android.R.attr.buttonTint || attributeNameResource == android.support.v7.appcompat.R.attr.buttonTint)) {
                    CompoundButton compoundButton = (CompoundButton) view;
                    String buttonTintValue = attrs.getAttributeValue(i);
                    ColorStateList checkboxColor = mRuntimeAttributeColorResolver.getColorStateListByAttrValue(getContext(), buttonTintValue);
                    if (checkboxColor != null) {
                        CompoundButtonCompat.setButtonTintList(compoundButton, checkboxColor);
                    }

                }
                //endregion
                //region drawableTint
                if (attributeNameResource == android.R.attr.drawableTint) {
                    String drawableTintAttrValue = attrs.getAttributeValue(i);
                    if (view instanceof TextView) {
                        TextView textView = (TextView) view;
                        Integer color = mRuntimeAttributeColorResolver.getColorByAttrValue(getContext(), drawableTintAttrValue);
                        if (color == null) {
                            //This is because there is no AppCompat version for drawableTint
                            //in which case of API < 23 the system will ignore the attribute but we WANT the effect before API 23 as well
                            Integer attrValue = attrValueToInt(drawableTintAttrValue);
                            if (attrValue != null) {
                                TypedValue outValue = new TypedValue();
                                boolean isResolved = getContext().getTheme()
                                        .resolveAttribute(attrValue, outValue, true);
                                if (isResolved) {
                                    color = outValue.data;
                                }
                            }

                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            if (color != null) {
                                textView.setCompoundDrawableTintList(ColorStateList.valueOf(color));
                            }
                        } else {
                            Drawable[] drawables = textView.getCompoundDrawablesRelative();
                            for (Drawable drawable : drawables) {
                                if (drawable != null && color != null) {
                                    DrawableCompat.setTint(drawable, color);
                                }
                            }
                        }
                    }
                }
                //endregion
                //region textColor
                if (attributeNameResource == android.R.attr.textColor && view instanceof TextView) {
                    String textColorAttrValue = attrs.getAttributeValue(i);
                    TextView textView = (TextView) view;
                    Integer color = mRuntimeAttributeColorResolver.getColorByAttrValue(getContext(), textColorAttrValue);
                    if (color != null) {
                        textView.setTextColor(color);
                    }
                }
                //endregion
                //region background
                if (attributeNameResource == android.R.attr.background) {
                    String backgroundColorAttrValue = attrs.getAttributeValue(i);
                    Integer color = mRuntimeAttributeColorResolver.getColorByAttrValue(getContext(), backgroundColorAttrValue);
                    if (color != null) {
                        view.setBackgroundColor(color);
                    }
                }
                //endregion
                //region indeterminateTint
                if (attributeNameResource == android.R.attr.indeterminateTint) {
                    String indeterminateTintColorAttrValue = attrs.getAttributeValue(i);
                    if (view instanceof ProgressBar) {
                        Integer color = mRuntimeAttributeColorResolver.getColorByAttrValue(getContext(), indeterminateTintColorAttrValue);
                        if (color != null) {
                            ProgressBar progressBar = (ProgressBar) view;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                progressBar.setIndeterminateTintList(ColorStateList.valueOf(color));
                            } else {
                                Drawable indeterminateDrawable = progressBar.getIndeterminateDrawable();
                                DrawableCompat.setTint(indeterminateDrawable, color);
                            }
                        }
                    }
                }
                //endregion
                //region backgroundTint
                if (attributeNameResource == android.R.attr.backgroundTint) {
                    String backgroundTintColorAttrValue = attrs.getAttributeValue(i);
                    Integer color = mRuntimeAttributeColorResolver.getColorByAttrValue(getContext(), backgroundTintColorAttrValue);
                    if (color != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            view.setBackgroundTintList(ColorStateList.valueOf(color));
                        } else {
                            view.getBackground()
                                    .setColorFilter(color, PorterDuff.Mode.SRC_IN);
                        }
                    }
                }
                //endregion
                //region backgroundTint
                if (attributeNameResource == android.support.v7.appcompat.R.attr.backgroundTint) {
                    String backgroundTintColorAttrValueApp = attrs.getAttributeValue(i);
                    Integer color = mRuntimeAttributeColorResolver.getColorByAttrValue(getContext(), backgroundTintColorAttrValueApp);
                    if (color != null) {
                        ViewCompat.setBackgroundTintList(view, ColorStateList.valueOf(color));
                    }
                }
                //endregion
                //region textColorHighlight
                if (attributeNameResource == android.R.attr.textColorHighlight) {
                    String textColorHighlightAttrValueApp = attrs.getAttributeValue(i);
                    Integer color = mRuntimeAttributeColorResolver.getColorByAttrValue(getContext(), textColorHighlightAttrValueApp);
                    if (view instanceof TextView && color != null) {
                        TextView textView = (TextView) view;
                        textView.setHighlightColor(color);
                    }
                }
                //endregion
                //region tint
                if (attributeNameResource == android.R.attr.tint) {
                    String tintAttrValueApp = attrs.getAttributeValue(i);
                    Integer color = mRuntimeAttributeColorResolver.getColorByAttrValue(getContext(), tintAttrValueApp);
                    if (view instanceof ImageView && color != null) {
                        ImageView imageView = (ImageView) view;
                        ImageViewCompat.setImageTintList(imageView, ColorStateList.valueOf(color));
                    }
                }
                //endregion
                //region titleTextColor
                if (attributeNameResource == android.support.v7.appcompat.R.attr.titleTextColor) {
                    String toolbarTitleTextColorValue = attrs.getAttributeValue(i);
                    Integer color = mRuntimeAttributeColorResolver.getColorByAttrValue(getContext(), toolbarTitleTextColorValue);
                    if (view instanceof Toolbar && color != null) {
                        Toolbar toolbar = (Toolbar) view;
                        toolbar.setTitleTextColor(color);
                    }
                }
                //endregion
            }

        }

        /**
         * Extract int value from its string representation
         */
        @Nullable
        private Integer attrValueToInt(String attrValue) {
            if (attrValue == null || attrValue.length() <= 1) {
                return null;
            }
            return StringsKt.toIntOrNull(attrValue.substring(1));
        }

        @Override
        protected View onCreateView(String name, AttributeSet attrs) throws
                ClassNotFoundException {
            View view = null;
            // This mimics the {@code PhoneLayoutInflater} in the way it tries to inflate the base
            // classes.
            for (String prefix : sClassPrefixList) {
                try {
                    view = createView(name, prefix, attrs);
                    if (view != null) {
                        break;
                    }
                } catch (ClassNotFoundException ignored) {
                }
            }
            if (view == null) {
                view = super.onCreateView(name, attrs);
            }
            return view;
        }

        /**
         * Wraps {@link android.view.LayoutInflater.Factory2} so that views created by the original factory will still reach
         * {@link RainbowLayoutInflater#onViewCreated(AttributeSet, View)}
         */
        private class Factory2Wrapper
                implements Factory2 {

            private final Factory2 mFactory2;

            Factory2Wrapper(Factory2 factory2) {
                mFactory2 = factory2;
            }

            @Override
            public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
                View view = mFactory2.onCreateView(parent, name, context, attrs);
                onViewCreated(attrs, view);
                return view;
            }

            @Override
            public View onCreateView(String name, Context context, AttributeSet attrs) {
                View view = mFactory2.onCreateView(name, context, attrs);
                onViewCreated(attrs, view);
                return view;
            }
        }


        /**
         * Wraps {@link android.view.LayoutInflater.Factory} so that views created by the original factory will still reach
         * {@link RainbowLayoutInflater#onViewCreated(AttributeSet, View)}
         */
        private class FactoryWrapper
                implements Factory {

            private final Factory mFactory;

            FactoryWrapper(Factory factory) {
                mFactory = factory;
            }

            @Override
            public View onCreateView(String name, Context context, AttributeSet attrs) {
                View view = mFactory.onCreateView(name, context, attrs);
                onViewCreated(attrs, view);
                return view;
            }
        }
    }
}
