import java.sql.*;
var url = "jdbc:mysql://db-mysql-saas-dev.cfq0uo8ms95m.ap-southeast-1.rds.amazonaws.com:3306/thomastone?serverTimezone=Asia/Seoul&zeroDateTimeBehavior=convertToNull";
var user = "thomastone";
var password = "dPqkd0923!";
Class.forName("com.mysql.cj.jdbc.Driver");
try (var conn = DriverManager.getConnection(url, user, password);
     var ps = conn.prepareStatement("select questionnaire_id, user_id from questionnaire order by created desc limit 5");
     var rs = ps.executeQuery()) {
  while (rs.next()) {
    System.out.println(rs.getLong(1) + "\t" + rs.getLong(2));
  }
}
/exit
