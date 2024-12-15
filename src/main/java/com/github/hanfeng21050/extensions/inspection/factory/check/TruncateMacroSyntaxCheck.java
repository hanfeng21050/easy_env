package com.github.hanfeng21050.extensions.inspection.factory.check;

import com.github.hanfeng21050.extensions.inspection.factory.MacroSyntaxCheck;
import com.github.hanfeng21050.extensions.inspection.factory.SyntaxChecker;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.regex.Pattern;

public class TruncateMacroSyntaxCheck extends MacroSyntaxCheck implements SyntaxChecker {
    private static final String TEMPLATE = "[truncate][表名]";
    private static final String DOC = """
            宏定义说明:
            1. [表名] - 必填参数
               - 数据库表名
               - 只能包含字母、数字和下划线
               - 必须以字母开头
            
            注意事项:
            - truncate 操作会清空整个表的数据
            - 此操作不可回滚，请谨慎使用
            - 执行此操作需要相应的数据库权限
            
            示例:
            @JRESMacro("[truncate][ses_sys_arg]")
            int truncateTable();
            """;

    private static final Pattern TABLE_NAME_PATTERN = Pattern.compile("^[a-zA-Z]\\w*$");

    public TruncateMacroSyntaxCheck(String value) {
        super(value);
    }

    @Override
    public String performSyntaxCheck(List<String> params) {
        if (params.size() < 2) {
            return generateError("错误：缺少表名参数", TEMPLATE, DOC);
        }

        // 检查表名
        String tableName = handleException(() -> params.get(1));
        if (StringUtils.isBlank(tableName)) {
            return generateError("错误：[表名]不能为空", TEMPLATE, DOC);
        }

        // 验证表名格式
        if (!TABLE_NAME_PATTERN.matcher(tableName).matches()) {
            return generateError("错误：[表名]格式无效，只能包含字母、数字和下划线，且必须以字母开头", TEMPLATE, DOC);
        }

        // 检查是否有多余的参数
        if (params.size() > 2) {
            return generateError("错误：truncate 宏只接受一个表名参数", TEMPLATE, DOC);
        }

        return null;
    }
}
