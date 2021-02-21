/*******************************************************************************
 * Copyright (c) 2021 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.telemetry.core.configuration;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.util.messages.Topic;
import com.redhat.devtools.intellij.telemetry.core.util.Lazy;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class TelemetryConfiguration extends CompositeConfiguration {

    public static final String KEY_MODE = "mode";

    public static final TelemetryConfiguration INSTANCE = new TelemetryConfiguration();

    public static final FileConfiguration GLOBAL_FILE = new FileConfiguration(Paths.get(
            System.getProperty("user.home"),
            ".redhat",
            "com.redhat.devtools.intellij.telemetry"));
    private final Lazy<ConfigurationChangedListener> notifier = new Lazy<>(() ->
            ApplicationManager.getApplication().getMessageBus()
                    .syncPublisher(ConfigurationChangedListener.CONFIGURATION_CHANGED));

    public void setMode(Mode mode) {
        put(KEY_MODE, mode.toString());
    }

    public Mode getMode() {
        return Mode.safeValueOf(get(KEY_MODE));
    }

    public boolean isEnabled() {
        return getMode().isEnabled();
    }

    public void setEnabled(boolean enabled) {
        setMode(Mode.valueOf(enabled));
    }

    public boolean isDebug() {
        return getMode() == Mode.DEBUG;
    }

    public boolean isConfigured() {
        return getMode().isConfigured();
    }

    @Override
    public void put(String key, String value) {
        GLOBAL_FILE.properties.get().put(key, value);
        notifier.get().configurationChanged(key, value);
    }

    public void save() throws IOException {
        GLOBAL_FILE.save();
    }

    @Override
    protected List<IConfiguration> getConfigurations() {
        return Arrays.asList(
                new SystemProperties(),
                GLOBAL_FILE);
    }

    public enum Mode {
        NORMAL {
            @Override
            public boolean isEnabled() {
                return true;
            }

            @Override
            public boolean isConfigured() {
                return true;
            }
        }
        , DEBUG {
            @Override
            public boolean isEnabled() {
                return true;
            }

            @Override
            public boolean isConfigured() {
                return true;
            }
        }, DISABLED {
            @Override
            public boolean isEnabled() {
                return false;
            }

            @Override
            public boolean isConfigured() {
                return true;
            }

        }, UNKNOWN {
            @Override
            public boolean isEnabled() {
                return false;
            }

            @Override
            public boolean isConfigured() {
                return false;
            }

        };

        public abstract boolean isEnabled();

        public abstract boolean isConfigured();

        public static Mode safeValueOf(String value) {
            try {
                if (value == null) {
                    return UNKNOWN;
                }
                return Mode.valueOf(value);
            } catch (IllegalArgumentException e) {
                return UNKNOWN;
            }
        }

        public static Mode valueOf(boolean enabled) {
            if (enabled) {
                return Mode.NORMAL;
            } else {
                return Mode.DISABLED;
            }
        }
    }

    public interface ConfigurationChangedListener {
        Topic<ConfigurationChangedListener> CONFIGURATION_CHANGED =
                Topic.create("Telemetry Configuration Changed", ConfigurationChangedListener.class);

        void configurationChanged(String property, String value);
    }

}
