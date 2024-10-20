package com.github.hanfeng21050.extensions.inspection.factory.check;

import com.github.hanfeng21050.extensions.inspection.factory.MacroSyntaxCheck;
import com.github.hanfeng21050.extensions.inspection.factory.SyntaxChecker;
import org.apache.commons.lang.StringUtils;

import java.util.List;

public class GetSequenceMacroSyntaxCheck extends MacroSyntaxCheck implements SyntaxChecker {
    String template = "[getSequence][表名][序列名]";
    String doc = """
            @JRESMacro("[getSequence][sps_sequence][entrustseq]")
            Long getFastCounter();
            """;
    public GetSequenceMacroSyntaxCheck(String value) {
        super(value);
    }

    @Override
    public String performSyntaxCheck(List<String> params) {
        // [表名]
        String param1 = handleException(() -> params.get(1));
        // [条件语句]
        String param2 = handleException(() -> params.get(2));

        if ((StringUtils.isNotBlank(param1) && !param1.matches("\\w+")) || StringUtils.isBlank(param1)) {
            return generateError("错误：无法解析[表名].", template, doc);

        }

        if ((StringUtils.isNotBlank(param2) && !param2.matches("\\w+")) || StringUtils.isBlank(param2)) {
            return generateError("错误：无法解析[序列名].", template, doc);

        }

        return "";
    }
}
