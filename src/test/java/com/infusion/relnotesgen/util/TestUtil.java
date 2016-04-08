package com.infusion.relnotesgen.util;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.infusion.relnotesgen.Commit;

import java.util.Set;

public final class TestUtil {
    private TestUtil() {
    }

    public static Set<String> getMessages(Iterable<Commit> commits) {
        return FluentIterable
                .from(commits)
                .transform(new Function<Commit, String>() {
                    @Override
                    public String apply(Commit commit) {
                        return commit.getMessage();
                    }
                })
                .toImmutableSet();
    }
}
