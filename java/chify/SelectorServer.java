package chify;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;

public class SelectorServer {
    public static void main(String[] args) {
        try {
            // 1. 打开一个 Selector (选择器)，相当于 Tomcat 的 Poller
            Selector selector = Selector.open();

            // 2. 打开 ServerSocketChannel (服务端通道)，相当于 Tomcat 的 Acceptor 监听端口
            ServerSocketChannel serverChannel = ServerSocketChannel.open();
            serverChannel.bind(new InetSocketAddress(8080));
            serverChannel.configureBlocking(false); // 必须配置为非阻塞，才能注册到 Selector

            // 3. 将 ServerSocketChannel 注册到 Selector 上，监听 ACCEPT (连接) 事件
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("🚀 NIO 服务端已启动，监听 8080 端口...");

            // 4. 死循环，轮询处理网络事件
            while (true) {
                // select() 是阻塞的，只有当注册的通道有事件发生时才会返回
                if (selector.select() == 0) {
                    continue;
                }

                // 获取所有发生事件的 SelectionKey 集合
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectedKeys.iterator();

                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove(); // 💡 必须手动移除，防止重复处理

                    // 处理不同的事件
                    if (key.isAcceptable()) {
                        // 🟢 连接事件：有新客户端连进来了
                        handleAccept(key, selector);
                    } else if (key.isReadable()) {
                        // 🔵 可读事件：客户端发数据过来了 (Tomcat 读数据触发点)
                        handleRead(key);
                    } else if (key.isWritable()) {
                        // 🟡 可写事件：网络缓冲区满了之后变为空闲，可以向客户端写数据了
                        handleWrite(key);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 处理新连接 (对应 Tomcat Acceptor 接收到连接后丢给 Poller)
    private static void handleAccept(SelectionKey key, Selector selector) throws IOException {
        ServerSocketChannel server = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = server.accept();
        clientChannel.configureBlocking(false); // 客户端通道也必须是非阻塞

        // 注册可读事件，开始监听客户端发来的数据，并绑定一个 ByteBuffer 作为附件（缓冲区）
        clientChannel.register(selector, SelectionKey.OP_READ, ByteBuffer.allocate(1024));
        System.out.println("📱 收到新连接，来自: " + clientChannel.getRemoteAddress());
    }

    // 处理读事件 (对应 Tomcat InternalNioInputBuffer 突然把 buf 读出来的瞬间)
    private static void handleRead(SelectionKey key) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        ByteBuffer buffer = (ByteBuffer) key.attachment(); // 取出注册时绑定的缓冲区

        buffer.clear();
        // 🚀 核心：非阻塞读取。网卡里的 TCP 数据包被抓到 Java 内存的 buffer 里
        int bytesRead = clientChannel.read(buffer);

        if (bytesRead > 0) {
            buffer.flip(); // 切换为读模式
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            String msg = new String(data).trim();
            System.out.println("📥 收到客户端 [ " + clientChannel.getRemoteAddress() + " ] 数据: " + msg);

            // 业务逻辑处理完后，如果想给客户端回话，我们修改关心事件为「可写」
            // 绑定回复内容，准备在 handleWrite 里发回去
            ByteBuffer responseBuffer = ByteBuffer.wrap(("Echo: " + msg + "\n").getBytes());
            key.interestOps(SelectionKey.OP_WRITE);
            key.attach(responseBuffer);

        } else if (bytesRead == -1) {
            // 客户端主动断开连接
            System.out.println("❌ 客户端断开连接: " + clientChannel.getRemoteAddress());
            clientChannel.close();
            key.cancel();
        }
    }

    // 处理写事件 (向客户端发送数据)
    private static void handleWrite(SelectionKey key) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        ByteBuffer buffer = (ByteBuffer) key.attachment();

        // 🚀 核心：非阻塞写入。将内存中的数据写入操作系统的网络发送缓冲区
        if (buffer.hasRemaining()) {
            clientChannel.write(buffer);
        }

        // 如果数据已经写完，记得把关注的事件改回「可读」，否则这个线程会因为一直「可写」而陷入死循环死忙
        if (!buffer.hasRemaining()) {
            System.out.println("📤 数据成功回复给: " + clientChannel.getRemoteAddress());
            key.interestOps(SelectionKey.OP_READ);
            key.attach(ByteBuffer.allocate(1024)); // 重新挂载干净的读缓冲区
        }
    }
}
