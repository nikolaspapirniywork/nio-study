package client;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;

public class Client {
    ByteBuffer buffer = ByteBuffer.allocate(2 * 1024);
    ByteBuffer randomBuffer;
    CharBuffer charBuffer;
    private CharsetDecoder decoder = Charset.defaultCharset().newDecoder();

    public void SocketChannel() throws Exception {
        Selector selector = Selector.open();
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        socketChannel.setOption(StandardSocketOptions.SO_RCVBUF, 128 * 1024);
        socketChannel.setOption(StandardSocketOptions.SO_SNDBUF, 128 * 1024);
        socketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);

        socketChannel.register(selector, SelectionKey.OP_CONNECT);

        socketChannel.connect(new java.net.InetSocketAddress("localhost", 9999));


        while (selector.select(1000) > 0) {
            Iterator<SelectionKey> itr = selector.selectedKeys().iterator();

            while (itr.hasNext()) {
                SelectionKey key = itr.next();

                itr.remove();

                SocketChannel keySocketChannel = (SocketChannel) key.channel();

                if (key.isConnectable()) {
                    System.out.println("server.Server Found");

                    if (keySocketChannel.isConnectionPending()) {
                        keySocketChannel.finishConnect();
                    }
                    while (keySocketChannel.read(buffer) != -1) {
                        buffer.flip();

                        charBuffer = decoder.decode(buffer);
                        if(!charBuffer.toString().isEmpty())
                            System.out.println(charBuffer.toString());

                        if (buffer.hasRemaining()) {
                            buffer.compact();
                        } else {
                            buffer.clear();
                        }

                        int r = new Random().nextInt(100);
                        randomBuffer = ByteBuffer.wrap("Random number".concat(String.valueOf(r)).getBytes("UTF-8"));
                        keySocketChannel.write(randomBuffer);

                        Thread.sleep(1500);
                    }

                } else if (key.isReadable()) {
                    System.out.println("Read");
                }
            }
        }
    }

    public void Channel_to_Channel() throws IOException {
        RandomAccessFile from = new RandomAccessFile("file2.txt", "rw");
        FileChannel fromChannel = from.getChannel();

        RandomAccessFile to = new RandomAccessFile("file3.txt", "rw");
        FileChannel toChannel = to.getChannel();

        long position = 0;
        long count = fromChannel.size();

        toChannel.transferFrom(fromChannel, position, count);
    }

    public void Scatter_Gather() throws IOException {
        RandomAccessFile fileWrite = new RandomAccessFile("file2.txt", "rw");
        FileChannel channelWrite = fileWrite.getChannel();

        ByteBuffer headerWriter = ByteBuffer.allocate(128);
        headerWriter.put(getAmountOfBytes(stringTimes("Header goes here", 10).getBytes(), 128));
        headerWriter.flip();

        ByteBuffer bodyWriter = ByteBuffer.allocate(1024);
        bodyWriter.put(getAmountOfBytes(stringTimes("Body goes here", 100).getBytes(), 1024));
        bodyWriter.flip();

        ByteBuffer[] array = {headerWriter, bodyWriter};

        channelWrite.write(array);

        channelWrite.close();

        // read
        RandomAccessFile fileRead = new RandomAccessFile("file2.txt", "rw");
        FileChannel channelRead = fileRead.getChannel();

        ByteBuffer headerReader = ByteBuffer.allocate(128);
        ByteBuffer bodyReader = ByteBuffer.allocate(1024);

        channelRead.read(new ByteBuffer[]{headerReader, bodyReader});

        headerReader.flip();
        while (headerReader.hasRemaining()) {
            System.out.print((char) headerReader.get());
        }
        System.out.println();

        bodyReader.flip();
        while (bodyReader.hasRemaining()) {
            System.out.print((char) bodyReader.get());
        }

        channelRead.close();
    }

    private byte[] getAmountOfBytes(byte[] message, int max) {
        if (message.length > max)
            return Arrays.copyOf(message, max);
        else
            return message;
    }

    private String stringTimes(String s, int times) {
        String result = "";
        for (int i = 0; i < times; ++i) {
            result += s;
        }
        return result;
    }

    public void FC2() throws IOException {
        RandomAccessFile file = new RandomAccessFile("file2.txt", "rw");
        FileChannel channel = file.getChannel();

        ByteBuffer buf = ByteBuffer.allocate(48);
        buf.put("Hello world".getBytes());
        buf.flip();

        channel.write(buf);
        buf.compact();
        file.close();
    }

    public void FC() throws IOException {
        RandomAccessFile file = new RandomAccessFile("file.txt", "r");
        FileChannel channel = file.getChannel();

        ByteBuffer buf = ByteBuffer.allocate(48);

        int countOfRead = channel.read(buf);
        while (countOfRead != -1) {
            System.out.println("Read = " + countOfRead);
            buf.flip();
            while (buf.hasRemaining()) {
                System.out.print((char) buf.get());
            }
            buf.clear();
            countOfRead = channel.read(buf);
        }
        file.close();
    }


    public static void main(String[] args) throws Exception {
        new Client().SocketChannel();
    }
}
