package server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.*;

public class SlaveSelector extends Thread {

    private Map<SocketChannel, List<byte[]>> keepDataTrack = new HashMap<>();
    private Selector selector;

    public SlaveSelector() throws IOException {
        selector = Selector.open();
    }

    ArrayList channels = new ArrayList();

    @Override
    public void run() {
        try {
            while (true) {
                if (selector.select() > 0) {
                    Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
                    while (keys.hasNext()) {
                        SelectionKey key = keys.next();
                        if (!key.isValid())
                            continue;

                        if (key.isReadable()) {
                            readOp(key);
                        } else if (key.isWritable()) {
                            writeOp(key);
                        } else {
                            System.out.println("Unrecognized request type = " + key.interestOps());
                        }
                        keys.remove();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeOp(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        List<byte[]> channelData = keepDataTrack.get(socketChannel);
        Iterator<byte[]> iterator = channelData.iterator();
        while (iterator.hasNext()) {
            byte[] it = iterator.next();
            socketChannel.write(ByteBuffer.wrap(it));
            iterator.remove();
        }
        key.interestOps(SelectionKey.OP_READ);
    }

    private void readOp(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();

        ByteBuffer buffer = ByteBuffer.allocate(2 * 1024);
        buffer.clear();

        int numRead = -1;
        try {
            numRead = socketChannel.read(buffer);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (numRead == -1) {
            keepDataTrack.remove(socketChannel);
            System.out.println("Connection closed by " + socketChannel.getRemoteAddress());
            socketChannel.close();
            key.cancel();
            return;
        }

        byte[] data = new byte[numRead];
        System.arraycopy(buffer.array(), 0, data, 0, numRead);
        System.out.println(new String(data, "UTF-8") + " from " + socketChannel.getRemoteAddress());
        doEchoJob(key, data);
    }

    private void doEchoJob(SelectionKey key, byte[] data) {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        List<byte[]> channelData = keepDataTrack.get(socketChannel);
        channelData.add(data);

        key.interestOps(SelectionKey.OP_WRITE);
    }

    public synchronized void addChannel(SocketChannel channel)
            throws IOException {
        keepDataTrack.put(channel, new ArrayList<byte[]>());
        channels.add(channel);
        selector.wakeup();
        registerNewChannels();
    }

    private synchronized void registerNewChannels() throws IOException {
        int size = channels.size();
        for (int i = 0; i < size; i++) {
            SocketChannel sc = (SocketChannel) channels.get(i);
            sc.configureBlocking(false);
            try {
                sc.register(selector, SelectionKey.OP_READ);
                /*setSocketOptions(((SocketChannel)
                        readKey.channel()).socket());*/
            } catch (ClosedChannelException cce) {
            }
        }
        channels.clear();
    }
}
