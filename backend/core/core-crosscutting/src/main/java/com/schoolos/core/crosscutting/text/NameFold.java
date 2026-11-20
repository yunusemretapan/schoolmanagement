package com.schoolos.core.crosscutting.text;

import java.util.Locale;

/**
 * The case folding every name comparison uses (spec 0004 review 1 L-1; spec 0005, Req. 7, plan R4): the Turkish I
 * letters (I, İ, ı, i) all become {@code i} first, then the rest is lower-cased. Java's and PostgreSQL's {@code lower}
 * disagree on these letters (PostgreSQL's depends on the collation), so queries apply the same replacements:
 * {@link #jpql(String)}.
 */
public final class NameFold {

    private NameFold() {}

    public static String fold(String value) {
        return value.replace('İ', 'i').replace('I', 'i').replace('ı', 'i').toLowerCase(Locale.ROOT);
    }

    /** The same folding as a JPQL expression over {@code path}, e.g. {@code u.fullName}. */
    public static String jpql(String path) {
        return "lower(replace(replace(replace(" + path + ", 'İ', 'i'), 'I', 'i'), 'ı', 'i'))";
    }
}
