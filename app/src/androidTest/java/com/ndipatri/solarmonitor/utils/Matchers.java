package com.ndipatri.solarmonitor.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.test.espresso.matcher.BoundedMatcher;
import android.view.View;
import android.widget.ImageView;

import org.hamcrest.Description;
import org.hamcrest.Matcher;

public class Matchers {

    public static Matcher<View> isBitmapTheSame(final int resourceId) {
        return new BoundedMatcher<View, ImageView>(ImageView.class) {

            @Override
            public void describeTo(Description description) {
                description.appendText("has the same Bitmap as the resource with id=");
                description.appendValue(resourceId);
                description.appendText(" has.");
            }

            @Override
            public boolean matchesSafely(ImageView view) {
                Drawable otherDrawable = view.getResources().getDrawable(resourceId);
                return sameBitmap(view.getDrawable(), otherDrawable);
            }
        };
    }

    public static Matcher<View> isBitmapTheSame(final Drawable drawable) {
        return new BoundedMatcher<View, ImageView>(ImageView.class) {

            @Override
            public void describeTo(Description description) {
                description.appendText("has the same Bitmap as this drawable: ");
                description.appendValue(drawable);
                description.appendText(" has.");
            }

            @Override
            public boolean matchesSafely(ImageView view) {
                return sameBitmap(view.getDrawable(), drawable);
            }
        };
    }

    private static boolean sameBitmap(Drawable drawable, Drawable otherDrawable) {
        if (drawable == null || otherDrawable == null) {
            return false;
        }

        Bitmap bitmap = convertBitmap(drawable);
        Bitmap otherBitmap = convertBitmap(otherDrawable);
        //NOTE: sameAs requires API 12
        return bitmap.sameAs(otherBitmap);
    }

    private static Bitmap convertBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }
}
