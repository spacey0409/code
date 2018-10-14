package cn.morethink.nettyexample.handler;

import cn.morethink.nettyexample.util.GeneralResponse;
import cn.morethink.nettyexample.util.ResponseUtil;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.stream.ChunkedFile;
import lombok.extern.slf4j.Slf4j;

import javax.activation.MimetypesFileTypeMap;
import java.io.*;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * @author 李文浩
 * @date 2018/9/18
 */
@Slf4j
public class HttpDownloadHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    public HttpDownloadHandler() {
        super(false);
    }

    /**
     * 得到文件 mimeType
     */
    MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();

    /**
     * 分块大小
     */
    private static final int CHUNK_SIZE = 1024 * 10;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
        System.out.println(Thread.currentThread().getName());
        String uri = request.uri();
        if (uri.startsWith("/download") && request.method().equals(HttpMethod.GET)) {
            String filePath = "/data/jdk-8u172-linux-x64.tar.gz";
            GeneralResponse generalResponse = null;
            File file = new File(filePath);
            try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r")) {
                long fileLength = randomAccessFile.length();
                HttpResponse response = new DefaultHttpResponse(HTTP_1_1, HttpResponseStatus.OK);
                response.headers().set(HttpHeaderNames.CONTENT_LENGTH, fileLength);
                response.headers().set(HttpHeaderNames.CONTENT_TYPE, mimeTypesMap.getContentType(file.getPath()));
                response.headers().add(HttpHeaderNames.CONTENT_DISPOSITION, String.format("attachment; filename=\"%s\"", file.getName()));

                ctx.write(response);
                ChannelFuture sendFileFuture = null;
                sendFileFuture = ctx.write(new ChunkedFile(randomAccessFile, 0, fileLength, CHUNK_SIZE), ctx.newProgressivePromise());
                sendFileFuture.addListener(new ChannelProgressiveFutureListener() {
                    @Override
                    public void operationComplete(ChannelProgressiveFuture future)
                            throws Exception {
                        log.info("file {} transfer complete.", file.getName());
                    }

                    @Override
                    public void operationProgressed(ChannelProgressiveFuture future,
                                                    long progress, long total) throws Exception {
                        if (total < 0) {
                            log.warn("file {} transfer progress: {}", file.getName(), progress);
                        } else {
                            log.info("file {} transfer progress: {}/{}", file.getName(), progress, total);
                        }
                    }
                });
                ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
            } catch (FileNotFoundException e) {
                log.warn("file {} not found", file.getPath());
                generalResponse = new GeneralResponse(HttpResponseStatus.NOT_FOUND, String.format("file %s not found", file.getPath()), null);
                ResponseUtil.response(ctx, request, generalResponse);
            } catch (IOException e) {
                log.warn("file {} has a IOException: {}", file.getName(), e.getMessage());
                generalResponse = new GeneralResponse(HttpResponseStatus.INTERNAL_SERVER_ERROR, String.format("读取 file %s 发生异常", filePath), null);
                ResponseUtil.response(ctx, request, generalResponse);
            }
        } else {
            ctx.fireChannelRead(request);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) {
        log.warn("{}", e);
        ctx.close();

    }
}
