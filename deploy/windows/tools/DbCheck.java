import java.sql.Connection;
import java.sql.DriverManager;

/**
 * 설치 스크립트용 MySQL 연결 확인 도구.
 * 사용: java -cp "tools;mysql-connector-j.jar;protobuf.jar" DbCheck <host> <port> <user>
 * 비밀번호는 환경변수 OUTDOOR_DB_PASSWORD 로 받는다 (특수문자 안전).
 * 출력(한 줄): OK / AUTH / CONN / OTHER:<메시지>
 */
public class DbCheck {
    public static void main(String[] a) {
        String url = "jdbc:mysql://" + a[0] + ":" + a[1] + "/outdoor_index"
                + "?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Seoul"
                + "&createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true&useSSL=false&connectTimeout=5000";
        String pw = System.getenv("OUTDOOR_DB_PASSWORD");
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection c = DriverManager.getConnection(url, a[2], pw == null ? "" : pw)) {
                c.createStatement().execute("CREATE TABLE IF NOT EXISTS install_check (id INT)");
                c.createStatement().execute("DROP TABLE install_check");
            }
            System.out.println("OK");
        } catch (Throwable e) {
            StringBuilder all = new StringBuilder();
            for (Throwable t = e; t != null; t = t.getCause()) all.append(t.getMessage()).append(' ');
            String m = all.toString();
            if (m.contains("Access denied")) System.out.println("AUTH");
            else if (m.contains("Communications link failure") || m.contains("Connection refused")) System.out.println("CONN");
            else System.out.println("OTHER:" + m.replace('\n', ' ').replace('\r', ' ').trim());
        }
    }
}
