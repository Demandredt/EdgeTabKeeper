package xyz.maphaze;

import xyz.maphaze.utils.EdgeTabKeeperUtils;
import xyz.maphaze.utils.MinioConfig;

import java.nio.file.Path;
import java.util.Scanner;
import java.util.stream.Collectors;

public class Application {

    public static void main(String[] args) {
        MinioConfig minioConfig = new MinioConfig();
        EdgeTabKeeperUtils edgeTabKeeperUtils = new EdgeTabKeeperUtils();

        //从服务器拉取json
        edgeTabKeeperUtils.pullJson();
        //读取当前本地映射文件
        minioConfig.updateKitSetFilename();
        String currentSet = MinioConfig.kitSetFilenames.get("currentSet").get(0);
//        主循环
        while (true){
        System.out.println("当前SET列表\n--------------------");

        MinioConfig.kitSetFilenames.forEach(
                (key,value) ->{
                    if (key.equals("currentSet")){
                        return;
                    }else {
                        if (key.equals(MinioConfig.currentSet) && value.get(0).equals(MinioConfig.tabs.get(0).getFileName().toString())){
                            System.out.printf("| %s --当前选中\n",key);
                        }else {
                            System.out.printf("| %s \n",key);
                        }

                    }
                }
        );



            System.out.printf("输入想要切换到的配置组(输入Add SetName添加本地Set为新Set)：");
            Scanner scanner = new Scanner(System.in);
            String setSelected = scanner.nextLine();

            //需要从服务器获取最新的tabs有哪些
            if(MinioConfig.kitSetFilenames.containsKey(setSelected)){
                //从服务器下载对应的tabs
                minioConfig.downloadTabs(setSelected);
                MinioConfig.currentSet = setSelected;
                MinioConfig.kitSetFilenames.put(MinioConfig.currentSet,MinioConfig.tabs
                        .stream()
                        .map(path -> path.getFileName().toString())
                        .collect(Collectors.toList())
                );
                minioConfig.saveKitSetFilename();
                System.out.println("切换成功");



            }else {
                if (setSelected.substring(0,4).equalsIgnoreCase("Add ")){
                    String newSetName = setSelected.substring(4);
                    MinioConfig.kitSetFilenames.put(newSetName,EdgeTabKeeperUtils.getLatestTabs()
                            .stream()
                            .map(path -> path.getFileName().toString())
                            .collect(Collectors.toList())
                    );
                    MinioConfig.currentSet = newSetName;
                    currentSet = newSetName;
                    edgeTabKeeperUtils.saveOrAddSet();

                    minioConfig.saveKitSetFilename();
                    System.out.println("添加成功！");
                }else {
                    System.out.println("输入有误，请重新输入");
                }

            }


        }


    }
}
