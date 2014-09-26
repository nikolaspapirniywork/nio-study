package server;

import java.io.IOException;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;

public class Server {
    private ServerSocketChannel server;
    private int curSlave = 0;
    private SlaveSelector[] slaveSelectors;

    public Server() throws IOException {

        int selectorCount = 1;
        slaveSelectors = new SlaveSelector[selectorCount];
        for (int i = 0; i < selectorCount; i++) {
            SlaveSelector slave = new SlaveSelector();
            slave.start();
            slaveSelectors[i] = slave;
        }
    }

    public void startServer() throws IOException {
        server = ServerSocketChannel.open();
        server.configureBlocking(false);
        server.setOption(StandardSocketOptions.SO_RCVBUF, 128 * 1024);
        server.setOption(StandardSocketOptions.SO_REUSEADDR, true);

        server.socket().bind(new java.net.InetSocketAddress("localhost", 9999));
        System.out.println("server.Server started");
        Selector selector = Selector.open();
        server.register(selector, SelectionKey.OP_ACCEPT);


        while (true) {
            if (selector.select() > 0) {
                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

                while (keys.hasNext()) {
                    SelectionKey key = keys.next();
                    if (!key.isValid())
                        continue;

                    if (key.isAcceptable()) {
                        acceptOp(key, selector);
                    } else {
                        System.out.println("Unrecognized request type = " + key.interestOps());
                    }
                    keys.remove();
                }
            }
        }
    }


    private void acceptOp(SelectionKey key, Selector selector) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel socketChannel = serverChannel.accept();
        /*clientListener.addConnection(socketChannel, server);*/

        if (socketChannel != null) {
            socketChannel.write(ByteBuffer.wrap("Hello!\n".getBytes("UTF-8")));

            SlaveSelector slave = getSlaveSelector();
            slave.addChannel(socketChannel);
        }
    }

    private synchronized SlaveSelector getSlaveSelector() {
        if (curSlave == slaveSelectors.length)
            curSlave = 0;
        System.out.println("Selected = " + (curSlave));
        return slaveSelectors[curSlave++];
    }

    public static void main(String[] args) throws IOException {
        new Server().startServer();

    }
}
