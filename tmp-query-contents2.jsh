import java.sql.*;
String url = "jdbc:mysql://db-mysql-saas-dev.cfq0uo8ms95m.ap-southeast-1.rds.amazonaws.com:3306/thomastone?serverTimezone=Asia/Seoul&zeroDateTimeBehavior=convertToNull";
try (Connection conn = DriverManager.getConnection(url, "thomastone", "dPqkd0923!")) {
    String sql = """
        select u.user_id, u.user_login_identifier,
               coalesce(os_plan.plan_name, org_plan.plan_name) as plan_name,
               (select count(*) from questionnaire q where q.user_id = u.user_id) as questionnaire_count
        from user u
        join organization o on o.organization_id = u.organization_id
        left join organization_subscription os on os.organization_id = o.organization_id
        left join subscription_plan os_plan on os_plan.subscription_plan_id = os.subscription_plan_id
        left join subscription_plan org_plan on org_plan.subscription_plan_id = o.subscription_plan_id
        where u.deleted is null
          and exists (select 1 from questionnaire q where q.user_id = u.user_id)
          and coalesce(os_plan.plan_name, org_plan.plan_name) is not null
        order by u.user_id desc
        limit 30
        """;
    try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
            System.out.println(rs.getLong("user_id") + "\t" + rs.getString("user_login_identifier") + "\t" + rs.getString("plan_name") + "\tq=" + rs.getInt("questionnaire_count"));
        }
    }
}
/exit
