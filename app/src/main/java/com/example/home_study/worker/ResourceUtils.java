package com.example.home_study;

public final class ResourceUtils {
    private ResourceUtils() {}

    public static int chooseDrawableForBook(String book) {
        if (book == null) return R.drawable.math;
        String k = book.trim().toLowerCase();
        if (k.contains("english")) return R.drawable.english;
        if (k.contains("math") || k.contains("mathematics")) return R.drawable.math;
        if (k.contains("physics")) return R.drawable.physics;
        if (k.contains("biology")) return R.drawable.biology;
        if (k.contains("chemistry")) return R.drawable.chemistry;
        if (k.contains("geography")) return R.drawable.geography;
        if (k.contains("history")) return R.drawable.history;
        if (k.contains("ict")) return R.drawable.ict;
        if (k.contains("physical")) return R.drawable.hpe;
        if (k.contains("language") || k.contains("oromifa")) return R.drawable.language;
        return R.drawable.math;
    }
}