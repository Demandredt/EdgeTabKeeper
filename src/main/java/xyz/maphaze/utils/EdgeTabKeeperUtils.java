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
     * 更新或者添加Tabs配置
     */
    public void saveOrAddSet( ){
        minioConfig.updateKitSetFilename();
        MinioConfig.kitSetFilenames.put(MinioConfig.currentSet,MinioConfig.tabs
                .stream()
                .map(path -> path.getFileName().toString())
                .collect(Collectors.toList())
        );

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
     * 返回最新的tabs组的文件名List
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
     * 检测该文件是否被占用
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
