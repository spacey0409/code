package util;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Paths;

import static util.ZipUtil.createDirectory;
import static util.ZipUtil.writeFile;

/**
 * @author 李文浩
 * @date 2018/8/4
 */
public class FileUtil {


    /**
     * 解压 tar.gz 文件
     *
     * @param file      要解压的tar.gz文件对象
     * @param outputDir 要解压到某个指定的目录下
     * @throws IOException
     */
    public static void decompressTarGz2(File file, String outputDir) throws IOException {
        try (TarArchiveInputStream tarIn = new TarArchiveInputStream(
                new GzipCompressorInputStream(
                        new FileInputStream(file)))) {
            //创建输出目录
            createDirectory(outputDir, null);
            TarArchiveEntry entry = null;
            ReadableByteChannel inChannel = Channels.newChannel(tarIn);
            while ((entry = tarIn.getNextTarEntry()) != null) {
                //是目录
                if (entry.isDirectory()) {
                    //创建空目录
                    createDirectory(outputDir, entry.getName());
                } else {
                    //是文件
                    try (FileChannel out = new RandomAccessFile(Paths.get(outputDir + File.separator + entry.getName()).toFile(), "rw").getChannel()) {
                        out.transferFrom(inChannel, 0, entry.getSize());
                    }
                }

            }
        }
    }

    public static void nioTransferCopy(File source, File target) throws IOException {
        try (FileChannel in = new FileInputStream(source).getChannel()) {
            try (FileChannel out = new FileOutputStream(target).getChannel()) {
                in.transferTo(0, in.size(), out);
            }
        }
    }


    public static void main(String[] args) throws IOException {
        long start = System.currentTimeMillis();
        String input = "/data/cps/temp/xj_restaurant_id.csv";
        String outputDir = "/data/cps/temp/test.csv";
        FileUtils.deleteQuietly(new File(outputDir));
        //ZipUtil.decompressTarGz(new File(input), outputDir);
        //ZipUtil.decompressTarGz2(new File(input), outputDir);
        nioTransferCopy(new File(input), new File(outputDir));
        System.out.println("time is " + (System.currentTimeMillis() - start) + "ms");
        start = System.currentTimeMillis();
        FileUtils.deleteQuietly(new File(outputDir));

        try (InputStream fis = new FileInputStream(new File(input))) {
            try (OutputStream fos = new FileOutputStream(new File(outputDir))) {
                writeFile(fis, fos);
            }
        }

        System.out.println("time is " + (System.currentTimeMillis() - start) + "ms");

    }
}
