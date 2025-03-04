package xyz.maphaze.utils;

import io.minio.GetObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.PutObjectArgs;
import io.minio.Result;
import io.minio.errors.*;
import io.minio.messages.Item;

import java.io.*;
import java.nio.file.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EdgeTabKeeperUtils {
    MinioConfig minioConfig = new MinioConfig();


    /**
     * 更新或者添加当前本地Tabs配置到MINIO服务器上
     * 以及json映射文件
     */
    public void saveOrAddSet( ){
        MinioConfig.kitSetFilenames.put(MinioConfig.currentSet,MinioConfig.tabs
                .stream()
                .map(path -> path.getFileName().toString())
                .collect(Collectors.toList())
        );




        //        存储当前默认的set组
        List<String> temp = new ArrayList<>();
        temp.add(MinioConfig.currentSet);
        MinioConfig.kitSetFilenames.put("currentSet",temp);

        List<Long> sizes = new ArrayList<>();

        minioConfig.uploadTabKit(MinioConfig.currentSet, MinioConfig.tabs
                        .stream()
                        .map(path -> path.getFileName().toString())
                        .collect(Collectors.toList())
                , MinioConfig.tabs.stream()
                        .map(path -> {
                            try {
                                InputStream in = Files.newInputStream(path);
                                sizes.add(Files.size(path));
                                return in;
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }

                        })
                        .collect(Collectors.toList())
        ,sizes);

        //上传json映射文件到服务器
        Path jsonPath = Paths.get(System.getProperty("user.home"),".EdgeTabKeeper","kitSetFilename.json");
        try {
            InputStream jsonStream = Files.newInputStream(jsonPath, StandardOpenOption.READ);
            MinioConfig.minioClient.putObject(
                    PutObjectArgs.builder()
                            .stream(jsonStream, Files.size(jsonPath), -1)
                            .bucket("edgetabsync")
                            .object("kitSetFilename.json")
                            .build()
        );
            jsonStream.close();
        }catch (IOException e){
            e.printStackTrace();
        } catch (ServerException e) {
            throw new RuntimeException(e);
        } catch (InsufficientDataException e) {
            throw new RuntimeException(e);
        } catch (ErrorResponseException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        } catch (InvalidResponseException e) {
            throw new RuntimeException(e);
        } catch (XmlParserException e) {
            throw new RuntimeException(e);
        } catch (InternalException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * 查询本地最新的tabs组的文件名
     * @return
     */
    public static List<Path> getLatestTabs(){
        Path tabsPath = Paths.get(System.getProperty("user.home"), "AppData", "Local", "Microsoft", "Edge", "User Data", "Default", "Sessions");
        String patternSession = "Session_[0-9]{17}";
        String patternTab = "Tabs_[0-9]{17}";
        List<Path> tabs = new ArrayList<>();

        try (Stream<Path> stream = Files.list(tabsPath)){
            Optional<Path> latestSession = stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().matches(patternSession))
                    .filter(EdgeTabKeeperUtils::isFileAvailable)
                    .max(Comparator.comparingLong(
                            p -> {
                                try {
                                    return Files.getLastModifiedTime(p).toMillis();
                                } catch (IOException e) {
                                    return 0L;
                                }
                            }
                    ));
            tabs.add(latestSession.get());

        }catch (IOException e){
            e.printStackTrace();
        }
        try (Stream<Path> stream = Files.list(tabsPath)){
            Optional<Path> latestTabs = stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().matches(patternTab))
                    .filter(EdgeTabKeeperUtils::isFileAvailable)
                    .max(Comparator.comparingLong(
                            p -> {
                                try {
                                    return Files.getLastModifiedTime(p).toMillis();
                                } catch (IOException e) {
                                    return 0L;
                                }
                            }
                    ));
            tabs.add(latestTabs.get());
        }catch (IOException e){
            e.printStackTrace();
        }




        return tabs;
    }

    /**
     * 检测本地文件是否被占用
     * @param path
     * @return
     */
    public static boolean isFileAvailable(Path path){
        try {
            Files.newByteChannel(path, StandardOpenOption.WRITE).close();
            return true;
        } catch (IOException e){
                return false;
        }

    }


    /**
     * 从服务器拉取json配置文件并替换
     */
    public void pullJson(){
        Iterable<Result<Item>> objects = MinioConfig.minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket("edgetabsync")
                        .prefix("kitSetFilename.json")
                        .build()
        );

        for (Result<Item> result : objects){
            try {
                Item item = result.get();
                Path jsonPath = Paths.get(System.getProperty("user.home"),".EdgeTabKeeper","kitSetFilename.json");
                Files.createDirectories(jsonPath.getParent());
                try (InputStream in = MinioConfig.minioClient.getObject(
                        GetObjectArgs.builder()
                                .bucket("edgetabsync")
                                .object("kitSetFilename.json")
                                .build()
                )){
                    Files.copy(
                            in,
                            jsonPath,
                            StandardCopyOption.REPLACE_EXISTING
                    );

                }




            } catch (ErrorResponseException e) {
                throw new RuntimeException(e);
            } catch (InsufficientDataException e) {
                throw new RuntimeException(e);
            } catch (InternalException e) {
                throw new RuntimeException(e);
            } catch (InvalidKeyException e) {
                throw new RuntimeException(e);
            } catch (InvalidResponseException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            } catch (ServerException e) {
                throw new RuntimeException(e);
            } catch (XmlParserException e) {
                throw new RuntimeException(e);
            }

        }
    }
}
