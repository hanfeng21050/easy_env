package com.github.hanfeng21050.view.settings;

import com.github.hanfeng21050.config.EasyEnvConfig;
import com.github.hanfeng21050.config.EasyEnvConfigComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.NlsContexts;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class EasyEnvRuleSettingsConfigurable implements Configurable {
    private final EasyEnvConfig config = ServiceManager.getService(EasyEnvConfigComponent.class).getState();
    final EasyEnvRuleSettingsView easyEnvRuleSettingsView = EasyEnvRuleSettingsView.getInstance(config);
    @Override
    public @NlsContexts.ConfigurableName String getDisplayName() {
        return "EasyEnvRule";
    }

    @Override
    public @Nullable JComponent createComponent() {
        return easyEnvRuleSettingsView.getComponent();
    }

    @Override
    public boolean isModified() {
        return easyEnvRuleSettingsView.isModified();
    }

    @Override
    public void apply() throws ConfigurationException {
        easyEnvRuleSettingsView.apply();
    }

    @Override
    public void reset() {
        easyEnvRuleSettingsView.reset();
    }
}
