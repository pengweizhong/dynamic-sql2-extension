package com.dynamic.sql.ext;

import com.dynamic.sql.utils.CollectionUtils;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.WithItem;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

public class SqlParserTest {
    String sql = "WITH \n" +
            "-- 最近注册用户\n" +
            "recent_users AS (\n" +
            "    SELECT *\n" +
            "    FROM users\n" +
            "    WHERE registration_date >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)\n" +
            "),\n" +
            "-- 账户余额大于 100 的活跃用户\n" +
            "rich_active_users AS (\n" +
            "    SELECT user_id, name, account_balance\n" +
            "    FROM users\n" +
            "    WHERE status = 'Active' AND account_balance > 100\n" +
            "),\n" +
            "-- 按性别统计活跃用户数量\n" +
            "gender_stats AS (\n" +
            "    SELECT gender, COUNT(*) AS count_per_gender\n" +
            "    FROM users\n" +
            "    WHERE status = 'Active'\n" +
            "    GROUP BY gender\n" +
            "),\n" +
            "-- 最近注册用户的账户信息\n" +
            "recent_user_balances AS (\n" +
            "    SELECT r.user_id, r.name, u.account_balance\n" +
            "    FROM recent_users r\n" +
            "    JOIN users u ON r.user_id = u.user_id\n" +
            "    WHERE u.account_balance > 50\n" +
            "),\n" +
            "-- 综合信息：最近注册且账户余额大于 100 的活跃用户\n" +
            "target_users AS (\n" +
            "    SELECT r.user_id, r.name, r.registration_date, u.account_balance\n" +
            "    FROM recent_users r\n" +
            "    JOIN rich_active_users u ON r.user_id = u.user_id\n" +
            "    WHERE r.status = 'Active'\n" +
            ")\n" +
            "-- 最终查询\n" +
            "SELECT \n" +
            "    t.user_id,\n" +
            "    t.name,\n" +
            "    t.registration_date,\n" +
            "    t.account_balance,\n" +
            "    g.count_per_gender\n" +
            "FROM target_users t\n" +
            "LEFT JOIN gender_stats g ON g.gender = 'Male'\n" +
            "WHERE t.account_balance > 150\n" +
            "ORDER BY t.registration_date DESC\n" +
            "LIMIT 10 OFFSET 0;\n";

    String sql2 = "WITH u AS (SELECT id FROM user) SELECT * FROM u WHERE id > 1";
    String sql3 = "WITH RECURSIVE temp AS (SELECT id, parent_id FROM category WHERE parent_id IS NULL UNION ALL SELECT c.id, c.parent_id FROM category c INNER JOIN temp t ON c.parent_id = t.id) SELECT * FROM temp";
    String sql4 = "WITH cte AS (SELECT id, name, manager_id FROM employees WHERE manager_id IS NULL UNION ALL SELECT e.id, e.name, e.manager_id FROM employees e INNER JOIN cte ON e.manager_id = cte.id) SELECT * FROM cte";
    String sql5 = "    SELECT\n" +
            "        u1.user_id,\n" +
            "        u1.name,\n" +
            "        u1.gender,\n" +
            "        u1.registration_date,\n" +
            "        u1.account_balance,\n" +
            "        u2.email,\n" +
            "        u2.phone_number,\n" +
            "        g_stats.count_per_gender\n" +
            "    FROM users u1\n" +
            "    -- 自连接模拟活跃用户和最近注册用户关联\n" +
            "    LEFT JOIN users u2 ON u1.user_id = u2.user_id AND u2.status = 'Active'\n" +
            "    -- 性别统计子查询\n" +
            "    LEFT JOIN (\n" +
            "        SELECT gender, COUNT(*) AS count_per_gender\n" +
            "        FROM users\n" +
            "        WHERE status = 'Active'\n" +
            "        GROUP BY gender\n" +
            "    ) g_stats ON g_stats.gender = u1.gender\n" +
            "    WHERE u1.registration_date >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)\n" +
            "      AND u1.account_balance > 100\n" +
            "      AND u1.status = 'Active'\n" +
            "    ORDER BY u1.registration_date DESC, u1.account_balance DESC";

    @Test
    void testCountQuery() throws JSQLParserException {
        // 解析原始SQL
        Statement stmt = CCJSqlParserUtil.parse(sql);
        Select select = (Select) stmt;
        PlainSelect plainSelect = select.getPlainSelect();
        List<WithItem> withItemsList = plainSelect.getWithItemsList();
        StringBuilder withSqlBuilder = new StringBuilder();
        if (CollectionUtils.isNotEmpty(withItemsList)) {
            withSqlBuilder.append("WITH ");
            // 拼接 WITH 子句
            for (int i = 0; i < withItemsList.size(); i++) {
                WithItem withItem = withItemsList.get(i);
                withSqlBuilder.append(withItem.toString());
                if (i < withItemsList.size() - 1) {
                    withSqlBuilder.append(", ");
                }
            }
            // 移除 WITH 子句
            select.setWithItemsList(null);
        }
        //拼接 COUNT 查询
        String countSql = withSqlBuilder + " SELECT COUNT(1) FROM (" + select + ") AS _COUNT_PAGE_TEMP";
        // 创建新的 PlainSelect 用于 COUNT 查询
        System.out.println("Count SQL: " + countSql);
    }

    @Test
    void testPageQuery() throws JSQLParserException {
        // 解析原始SQL
        Statement stmt = CCJSqlParserUtil.parse(sql);
        Select select = (Select) stmt;
        PlainSelect plainSelect = select.getPlainSelect();
        List<WithItem> withItemsList = plainSelect.getWithItemsList();
        StringBuilder withSqlBuilder = new StringBuilder();
        if (CollectionUtils.isNotEmpty(withItemsList)) {
            withSqlBuilder.append("WITH ");
            // 拼接 WITH 子句
            for (int i = 0; i < withItemsList.size(); i++) {
                WithItem withItem = withItemsList.get(i);
                withSqlBuilder.append(withItem.toString());
                if (i < withItemsList.size() - 1) {
                    withSqlBuilder.append(", ");
                }
            }
            // 移除 WITH 子句
            select.setWithItemsList(null);
        }
        //拼接 COUNT 查询
        String countSql = withSqlBuilder + " SELECT COUNT(1) FROM (" + select + ") AS _COUNT_PAGE_TEMP";
        // 创建新的 PlainSelect 用于 COUNT 查询
        System.out.println("Count SQL: " + countSql);
    }

    @Test
    void testSelectBody() throws JSQLParserException {
        Select stmt = (Select) CCJSqlParserUtil.parse(
                "with u as (select id from user) select * from u where id > 1"
        );
        stmt.setWithItemsList(null);
        System.out.println("withItemsList: " + stmt.getWithItemsList());
        System.out.println("selectBody   : " + stmt.getPlainSelect().toString());
    }

    @Test
    void test() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4);
        StringBuilder withSqlBuilder = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            withSqlBuilder.append(list.get(i).toString());
            if (i < list.size() - 1) {
                withSqlBuilder.append(", ");
            }
        }
        System.out.println(withSqlBuilder.toString());
    }
}
