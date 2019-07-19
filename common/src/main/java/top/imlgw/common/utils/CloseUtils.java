package top.imlgw.common.utils;

import java.io.Closeable;
import java.io.IOException;

/**
 * @author imlgw.top
 * @date 2019/7/17 12:05
 */
public class CloseUtils {
    public  static void close(Closeable...closeables){
        if(closeables==null){
            return;
        }
        for (Closeable closeable : closeables) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
