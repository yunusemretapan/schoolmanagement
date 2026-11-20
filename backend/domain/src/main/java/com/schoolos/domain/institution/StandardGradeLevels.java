package com.schoolos.domain.institution;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/** The one-click standard list (spec 0005, Req. 5): Anaokulu, 1.–12. sınıf, Hazırlık — 14 levels, in this order. */
public final class StandardGradeLevels {

    public static final List<String> NAMES = Stream.concat(
                    Stream.concat(
                            Stream.of("Anaokulu"), IntStream.rangeClosed(1, 12).mapToObj(n -> n + ". sınıf")),
                    Stream.of("Hazırlık"))
            .toList();

    private StandardGradeLevels() {}
}
