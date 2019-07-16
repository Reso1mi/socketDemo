package base_demo.TcpDemo2;

/**
 * @author imlgw.top
 * @date 2019/7/11 13:23
 */
public class ByteTools {
    public static byte[] int2byte(int a) {
        //1.移位是为了将4个字节分开 分为单独的一个个字节
        //2. & 0xff是为了保证二进制的一致性，因为java(计算机)中存放的数据是采用的补码
        //这里右移会在高位补1,保证了十进制的一致,这样二进制的数据就发生了变化
        // 所以为了保证二进制数据的一致.&上0xff就可以将高位都置为0而不影响低8位
        // 其实也可以理解为截取了后8位作为一个无符号数
        /*return new byte[]{
                (byte) ((a >> 24) & 0xff),
                (byte) ((a >> 16) & 0xff),
                (byte) ((a >> 8) & 0xff),
                (byte) ((a) & 0xff)
        };*/
        //其实这样也可以,无符号右移,高位补0不管正负
        return new byte[]{
                (byte) (a >>> 24),
                (byte) (a >>> 16),
                (byte) (a >>> 8),
                (byte) (a)
        };
    }

    public static byte[] int2byte2(int a) {
        return new byte[]{
                (byte) (a >> 24),
                (byte) (a >> 16),
                (byte) (a >> 8),
                (byte) (a)
        };
    }

    public static int byte2int(byte[] a) {
        //&0xff-->转换为int 将高位补0,低8位不变
        //-127 ：10000001(补) &0xff --> 00000000 00000000 00000000 10000001
        return a[3] & 0xff | (a[2] & 0xff) << 8 | (a[1] & 0xff) << 16 | (a[0] & 0xff) << 24;
    }

    public static void main(String[] args) {
        String []str={"123","234"};
        int a=-127;
        byte [] aa=int2byte(a);
        System.out.println(Integer.toBinaryString(aa[0]));
        System.out.println(Integer.toBinaryString(aa[1]));
        System.out.println(Integer.toBinaryString(aa[2]));
        System.out.println(Integer.toBinaryString(aa[3]));
        System.out.println("----------------------------");

        int b=byte2int(aa);
        System.out.println(b);
    }
}
