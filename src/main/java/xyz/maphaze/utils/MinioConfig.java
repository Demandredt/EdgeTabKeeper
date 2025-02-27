package xyz.maphaze.utils;

import com.alibaba.fastjson2.JSON;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

public class MinioConfig {
    static MinioClient minioClient;
    //存储场景和文件名的对应
    public static HashMap<String,List<String>> kitSetFilenames = new HashMap<>();
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
     * 创建MINIO客户端，建立连接
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
     * 读取映射文件
     */
    public void updateKitSetFilename(){

        try {
            String s = Files.readString(kitSetFilenamePath,StandardCharsets.UTF_8);
            kitSetFilenames = JSON.parseObject(s,kitSetFilenames.getClass());
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    /**
     * 存储映射文件
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
     * 上传到minio
     * @param tabSet 当前页面所属的组
     * @param fileNames 文件名
     * @param tabKits tab文件的流
     * @return
     */
    public int uploadTabKit(String tabSet,List<String> fileNames,List<InputStream> tabKits){


    try {
        minioClient.putObject(PutObjectArgs.builder()
                .bucket("edgetabsync")
                .object(tabSet+"/"+fileNames.get(0))
                .build()


        );
    }catch (Exception e){
        e.printStackTrace();
    }




    return 1;
    }


    public static HashMap<String, List<String>> getKitSetFilenames() {
        return kitSetFilenames;
    }

    public static void setKitSetFilenames(HashMap<String, List<String>> kitSetFilenames) {
        MinioConfig.kitSetFilenames = kitSetFilenames;
    }
}
