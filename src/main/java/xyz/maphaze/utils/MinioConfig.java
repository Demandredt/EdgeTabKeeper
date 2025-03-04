package xyz.maphaze.utils;

import com.alibaba.fastjson2.JSON;
import io.minio.*;
import io.minio.messages.Item;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class MinioConfig {
    static MinioClient minioClient;
// 初始化
    static {
        try {
            minioClient = readProperties();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    //存储场景和文件名的对应
    public static HashMap<String,List<String>> kitSetFilenames = new HashMap<>();

//  当前set
    public static String currentSet = "default";

//    当前的Tabs文件
    public static List<Path> tabs = EdgeTabKeeperUtils.getLatestTabs();
//    储存总对应关系的json文件
    static Path kitSetFilenamePath;

    static {
        try {
            minioClient = readProperties();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        kitSetFilenamePath = Paths.get(System.getProperty("user.home"),".EdgeTabKeeper","kitSetFilename.json");
    }

    /**
     * 创建MINIO客户端，建立连接 static初始化时执行
     * @return
     * @throws IOException
     */
    public static MinioClient readProperties() throws IOException {
        Properties prop = new Properties();
        try (InputStream in = MinioConfig.class.getClassLoader().getResourceAsStream("minio.properties")){
        prop.load(in);
        }catch (IOException e){
            throw new IOException("读取配置出现异常");
        }

        return MinioClient.builder()
                .endpoint(prop.getProperty("minio.endpoint"))
                .credentials(prop.getProperty("minio.accesskey"), prop.getProperty("minio.secretkey") )
                .build();

    }

    /**
     * 读取本地映射json文件
     */
    public void updateKitSetFilename(){

        try {
            String s = Files.readString(kitSetFilenamePath,StandardCharsets.UTF_8);
            kitSetFilenames = JSON.parseObject(s,kitSetFilenames.getClass());
            if (kitSetFilenames.get("currentSet") == null){
                List<String> list = new ArrayList<>();
                list.add("default");
                kitSetFilenames.put("currentSet",list);
            }
        }catch (IOException e){
            e.printStackTrace();
        }

        MinioConfig.currentSet = MinioConfig.kitSetFilenames.get("currentSet").get(0);

    }

    /**
     * 生成json映射文件到本地
     */
    public void saveKitSetFilename(){
        String s = JSON.toJSONString(kitSetFilenames);
        try {
            Files.createDirectories(kitSetFilenamePath.getParent());
            Files.write(kitSetFilenamePath,s.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
        }catch (IOException e){
            e.printStackTrace();
        }

    }



    /**
     * 上传文件到minio
     * @param tabSet 当前页面所属的组
     * @param fileNames 文件名
     * @param tabKits tab文件的流
     * @return
     */
    public int uploadTabKit(String tabSet,List<String> fileNames,List<InputStream> tabKits,List<Long> sizes){


    try {
//        先删除set下的原有文件
        for (Result<Item> item:minioClient.listObjects(ListObjectsArgs.builder()
                .bucket("edgetabsync")
                .prefix(tabSet + "/")
                .build()
        )){
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket("edgetabsync")
                            .object(item.get().objectName())
                            .build()
            );


        }


        minioClient.putObject(PutObjectArgs.builder()
                .bucket("edgetabsync")
                .object(tabSet+"/"+fileNames.get(0))
                .stream(tabKits.get(0),sizes.get(0),-1)
                .build()
        );
        minioClient.putObject(PutObjectArgs.builder()
                .bucket("edgetabsync")
                .object(tabSet+"/"+fileNames.get(1))
                .stream(tabKits.get(1),sizes.get(1),-1)
                .build()
        );




    }catch (Exception e){
        e.printStackTrace();
    }




    return 1;
    }


    /**
     * 从服务器下载tabs到本地并且覆盖
     * @param tabSet
     */
    public void downloadTabs(String tabSet){
        Iterable<Result<Item>> objects = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket("edgetabsync")
                        .prefix(tabSet+"/")
                        .build()

        );

        try{
            for (Result<Item> result: objects){
                Item item = result.get();
                String objectName = item.objectName();

                Path localPath = Paths.get(
                        System.getProperty("user.home"),
                        ".EdgeTabKeeper",
                        objectName
                );

                Files.createDirectories(localPath.getParent());

                try (InputStream inputStream = minioClient.getObject(
                        GetObjectArgs.builder()
                                .bucket("edgetabsync")
                                .object(objectName)
                                .build()
                )){
                    Files.copy(
                            inputStream,
                            localPath,
                            StandardCopyOption.REPLACE_EXISTING
                    );
                    System.out.println("成功下载"+objectName);

                    tabs = EdgeTabKeeperUtils.getLatestTabs();

                }

            }

        }catch (Exception e){
            e.printStackTrace();
        }


    }



}
