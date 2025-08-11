package com.github.hanfeng21050.extensions.settings;

import com.github.hanfeng21050.config.EasyEnvConfig;
import com.github.hanfeng21050.extensions.EasyEnvConfigComponent;
import com.github.hanfeng21050.utils.ServiceUtils;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.NlsContexts;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @Author hanfeng32305
 * @Date 2023/10/30 17:14
 */
public class EasyEnvSettingsConfigurable implements Configurable {
    private final EasyEnvConfig config = ServiceUtils.getService(EasyEnvConfigComponent.class).getState();
    private final EasyEnvSettingsView view = new EasyEnvSettingsView(config);

    @Override
    public @NlsContexts.ConfigurableName String getDisplayName() {
        return "EasyEnv";
    }

    @Override
    public @Nullable JComponent createComponent() {
        return view.getComponent();
    }

    @Override
    public boolean isModified() {
        return view.isModified();
    }

    @Override
    public void apply() throws ConfigurationException {
        view.apply();
    }

    @Override
    public void reset() {
        view.reset();
    }
}
