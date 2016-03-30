package ftpFileSystem;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

/**
 * Created by xulingo on 16/3/22.
 */
public class FileListener  {

    private  String rootpath;

    private  BlockingQueue<String> deleteFile =  new LinkedBlockingDeque<String>();
    private  BlockingQueue<String> faithFile = new LinkedBlockingDeque<String>();
    private  ConcurrentHashMap<String,Long> modifyTime ;

    public FileListener(String rootpath){

        this.rootpath=rootpath;
        initMap();

    }


    public  void LisnterStart (long inervalTime,long initTime ){

        ScheduledExecutorService scheduledExecutorService = new ScheduledThreadPoolExecutor(1);

        scheduledExecutorService.scheduleWithFixedDelay(new FileRunnable(this.rootpath),initTime,inervalTime, TimeUnit.SECONDS);

        ScheduledExecutorService cache = new ScheduledThreadPoolExecutor(1);

        cache.scheduleWithFixedDelay(new Ceche(),inervalTime+initTime,2*inervalTime,TimeUnit.SECONDS);

    }


    private void initMap(){
        ConcurrentHashMap<String,Long> temp = new ConcurrentHashMap<String, Long>();

        //获取云服务器地址
        String http ="";

        try {
            http= PropsUtil.get("host");
        } catch (IOException e) {
            e.printStackTrace();
        }

        //获取服务器上的文件信息
        String response = RequestCaseUtil.requestGetCase(http);

        //将响应转化为json数组

        JSONArray jsonArray = JSONObject.parseArray(response);


        if (jsonArray.size()>0){
            //便利json数组

            for (int i = 0; i <jsonArray.size() ; i++) {

                JSONObject fileMessage = (JSONObject) jsonArray.get(i);

                String name = (String) fileMessage.get("name");

                if (fileMessage.get("time")==null){
                    temp.put(name,null);
                }else {
                    Long time = (long) fileMessage.get("time");
                    temp.put(name,time);
                }
            }

            modifyTime = temp;
        }

    }
    class Ceche implements Runnable{

        @Override
        public void run() {
            //获取云服务器地址
            String http ="";

            try {
                http= PropsUtil.get("host");
            } catch (IOException e) {
                e.printStackTrace();
            }

            //获取服务器上的文件信息
            String response = RequestCaseUtil.requestGetCase(http);

            //将响应转化为json数组

            JSONArray jsonArray = JSONObject.parseArray(response);

            for (int i = 0; i <jsonArray.size() ; i++) {

                JSONObject fileMessage = (JSONObject) jsonArray.get(i);

                String name = (String) fileMessage.get("name");

                if (!modifyTime.containsKey(name)){
                    modifyTime.put(name,null);
                }
            }
        }
    }


    class FileRunnable implements Runnable{


        private String rootpath;
        private File file;
        File log;
        StringBuilder log_str;

        public FileRunnable(String rootpath) {
            this.rootpath = rootpath;
            file=new File(rootpath);
            log = new File(rootpath+File.separator+"log.txt");
            log_str = new StringBuilder();
        }

        @Override
        public void run() {
            FileOutputStream fileoutput=null;
            try {
                fileoutput= new FileOutputStream(log,true);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }




            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yy-MM-dd HH:mm:ss");
            log_str.append("==================================================================\n");
            log_str.append("==================================================================\n");
            log_str.append("本次扫描开始 开始时间为 : "+simpleDateFormat.format(System.currentTimeMillis())+"\n");


            if(!file.exists()){
                file.mkdir();
            }

            File []  accountDirs = file.listFiles();

            for (File account: accountDirs ) {
                Set<String> keySet= modifyTime.keySet();
                boolean flag = true;
                for (String key:keySet) {
                    if(key.contains(account.getName())){
                        flag=false;
                    }
                }
                if (flag){
                    continue;
                }

                if (".DS_Store".equals(account.getName()) || "log.txt".equals(account.getName())){
                    continue;
                }
                log_str.append(account.getName()+"  用户验证完毕,开始扫描文件夹下的db文件\n");

                for (File dbfile : account.listFiles()) {

                    if (".DS_Store".equals(dbfile.getName())){
                        continue;
                    }
                    log_str.append("**************************************************\n");
                    log_str.append(dbfile.getName()+"  开始扫描并读取文件信息\n");


                    String lastmodified  = simpleDateFormat.format(dbfile.lastModified());

                    String relativePath = dbfile.getParentFile().getName()+File.separator+dbfile.getName();

                    Long temDate = modifyTime.get(relativePath);
                    if (temDate==null){
                        log_str.append(dbfile.getName()+"  在远程服务器上的最无历史纪录 \n");
                    }else {
                        log_str.append(dbfile.getName()+"  在远程服务器上的最后修改时间为 : "+simpleDateFormat.format(new Date(temDate))+"\n");
                    }

                    log_str.append(dbfile.getName()+"  在本地服务器上的最后修改时间为 : "+simpleDateFormat.format(new Date(dbfile.lastModified()))+"\n");

                    String serverUrl="";

                    try {
                       serverUrl= PropsUtil.get("host");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    //当缓冲中存在对应的文件信息,并且缓存中的文件修改时间等于本地文件,不上传,不更新缓存;
                    //当缓存中存在对应的文件信息,并且缓存中的文件修改时间晚于本地文件最后修改时间(云服务器的数据是最新的,本地上传数据是旧的),不上传,不更新缓存
                    if(temDate!=null && (dbfile.lastModified() <= temDate)){

                        log_str.append("本地服务器上的文件不是最新的,不上传\n");
                          continue;

                    }else {
                       //只要缓存中没有文件信息,就表示该文件一定是最新的,上传数据,更新缓存
                        //如果缓存中有文件信息,只有在缓存文件的修改时间早于本地文件的最后修改时间(云服务器的数据是旧的,本地上传的数据是最新的,上传数据,更新缓存)
                            String response = RequestCaseUtil.requestPostCase(serverUrl,dbfile.getAbsolutePath(),dbfile.lastModified());
                            Map<String,String> map = JsonUtil.getJson(response);

                        modifyTime.put(relativePath,dbfile.lastModified());
                        log_str.append("本地服务器上的文件是最新的,上传\n");
                        log_str.append("**************************************************\n");
                    }

                }

                log_str.append(account.getName()+"  该文件下的db文件扫描完毕\n");
                log_str.append("==================================================================\n");
            }
            log_str.append("本次扫描完毕,结束时间为:"+simpleDateFormat.format(System.currentTimeMillis())+"\n");
            log_str.append("==================================================================\n");
            log_str.append("==================================================================\n");

            if (fileoutput!=null){
                try {
                    fileoutput.write(log_str.toString().getBytes("UTF-8"));
                    fileoutput.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }


        }


    }



    public static void main(String[] args) {

        String rootPath;
        String initTime;
        String interval;
        try {
            rootPath = PropsUtil.get("root");
            initTime = PropsUtil.get("initTime");
            interval = PropsUtil.get("interval");
            FileListener fileListener = new FileListener(rootPath);
            long init = Long.parseLong(initTime);
            long intervarTime = Long.parseLong(interval);
            fileListener.LisnterStart(init,intervarTime);
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}
