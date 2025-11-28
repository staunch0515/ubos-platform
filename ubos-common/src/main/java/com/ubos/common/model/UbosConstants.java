package com.ubos.common.util;

import com.ubos.common.model.AuthContext;

// This class holds static keys needed by all modules (Kernel, Engine, Server)
public final class UbosConstants {
    private UbosConstants() {}

    // The key used to store AuthContext in the Reactor Context
    public static final Class<AuthContext> AUTH_CONTEXT_KEY = AuthContext.class;
}