package ftpFileSystem;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by xulingo on 16/3/22.
 */
public class JsonUtil {



    public static Map<String,String> getJson (String json){
        Map<String,String > map = new HashMap<String,String>();


        String temp =  json.substring(1,json.length()-1);

        String [] KV = temp.split(",");


        for (int i = 0; i <KV.length ; i++) {

            String[] kvs =  KV[i].split(":");

            map.put(kvs[0].substring(1,kvs[0].length()-1),kvs[1].substring(1,kvs[1].length()-1));

        }

        return map;

    }

    public static void main(String[] args) {



        Map<String,String> map= getJson("{\"code\":\"0\",\"message\":\"secesss\",\"content\":\"aaaaaa\"}");


        System.out.println( map.get("code"));



    }
}
