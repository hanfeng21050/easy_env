package com.github.hanfeng21050.config;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.TreeMap;

/**
 * @Author hanfeng32305
 * @Date 2023/10/30 17:15
 */
@State(name = "easyEnv", storages = {@Storage("easyEnv.xml")})
public class EasyEnvConfigComponent implements PersistentStateComponent<EasyEnvConfig> {
    private EasyEnvConfig config;

    @Override
    public @Nullable EasyEnvConfig getState() {
        if (config == null) {
            config = new EasyEnvConfig();
            config.setSeeConnectInfoMap(new TreeMap<>());
            config.setConfigReplaceRuleMap(new TreeMap<>());
            config.setExcludedFileMap(new TreeMap<>());
        }
        return config;
    }

    @Override
    public void loadState(@NotNull EasyEnvConfig state) {
        XmlSerializerUtil.copyBean(state, Objects.requireNonNull(getState()));
    }
}
