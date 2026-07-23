package com.kaii.dentix.domain.daeguChain.application;

import java.util.function.Supplier;

public final class DaeguChainApiLogContext {

    private static final ThreadLocal<Actor> CURRENT = new ThreadLocal<>();

    private DaeguChainApiLogContext() {
    }

    public static Actor current() {
        return CURRENT.get();
    }

    public static <T> T withUser(Long userId, String feature, Supplier<T> action) {
        Actor previous = CURRENT.get();
        CURRENT.set(new Actor(userId, feature));
        try {
            return action.get();
        } finally {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }

    public record Actor(Long userId, String feature) {
    }
}
