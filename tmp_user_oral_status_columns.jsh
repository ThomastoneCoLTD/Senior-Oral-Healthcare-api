import java.sql.*;
var url = "jdbc:mysql://db-mysql-saas-dev.cfq0uo8ms95m.ap-southeast-1.rds.amazonaws.com:3306/thomastone?serverTimezone=Asia/Seoul&zeroDateTimeBehavior=convertToNull";
var user = "thomastone";
var password = "dPqkd0923!";
Class.forName("com.mysql.cj.jdbc.Driver");
try (var conn = DriverManager.getConnection(url, user, password);
     var ps = conn.prepareStatement("select column_name, data_type, column_key from information_schema.columns where table_schema = 'thomastone' and table_name = 'user_oral_status' order by ordinal_position");
     var rs = ps.executeQuery()) {
  while (rs.next()) {
    System.out.println(rs.getString(1) + "\t" + rs.getString(2) + "\t" + rs.getString(3));
  }
}
/exit
