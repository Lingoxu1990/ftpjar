package ftpFileSystem;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

/**
 * Created by surfacepc on 2016/3/16.
 */
public class PropsUtil {

    private static Properties props=new Properties();
    private static InputStream isp=PropsUtil.class.getClassLoader().getResourceAsStream("ftpFileSystem/config.properties");
    public static String get(String arg) throws IOException {
        props.load(new InputStreamReader(isp, "utf-8"));
        String result = props.getProperty(arg);
        return result;
    }
}
