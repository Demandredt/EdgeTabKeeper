package xyz.maphaze.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EdgeTabKeeperUtils {
    MinioConfig minioConfig = new MinioConfig();


    /**
     * 更新或者添加当前本地Tabs配置到MINIO服务器上
     */
    public void saveOrAddSet( ){
        minioConfig.updateKitSetFilename();
        MinioConfig.kitSetFilenames.put(MinioConfig.currentSet,MinioConfig.tabs
                .stream()
                .map(path -> path.getFileName().toString())
                .collect(Collectors.toList())
        );


        //        存储当前默认的set组
        List<String> temp = new ArrayList<>();
        temp.add(MinioConfig.currentSet);
        MinioConfig.kitSetFilenames.put("currentSet",temp);



        minioConfig.uploadTabKit(MinioConfig.currentSet, MinioConfig.tabs
                        .stream()
                        .map(path -> path.getFileName().toString())
                        .collect(Collectors.toList())
                , MinioConfig.tabs.stream()
                        .map(path -> {
                            try {
                                return Files.newInputStream(path);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }

                        })
                        .collect(Collectors.toList())
        );


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

            tabs.add(latestSession.get());
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
        }catch (AccessDeniedException e){
                return false;
        }catch (IOException e){
            return false;
        }

    }
}
