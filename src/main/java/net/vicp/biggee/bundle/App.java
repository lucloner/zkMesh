package net.vicp.biggee.bundle;

import org.apache.zookeeper.server.ServerConfig;
import org.apache.zookeeper.server.ZooKeeperServerMain;
import org.apache.zookeeper.server.admin.AdminServer;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Hello world!
 *
 */
public class App extends ZooKeeperServerMain {
    public static void main(String[] args) {
        Properties properties;
        try {
            properties = loadProperties();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<Integer> meshList = countMesh(properties);
        String dataDir = properties.getProperty("dataDir");
        int clientPort = Integer.parseInt(properties.getProperty("clientPort"));
        boolean adminServerEnable = Boolean.parseBoolean(properties.getProperty("admin.enableServer"));

        int count = meshList.size();
        if (count < 2) {
            count = 1;
        }

        App[] apps = new App[count];
        AtomicBoolean leader = new AtomicBoolean(true);
        IntStream.rangeClosed(1, count)
                .parallel()
                .forEach(i -> {
                    int index = meshList.get(i - 1);
                    boolean thisAdminServerEnable = false;
                    int port = clientPort + i - 1;
                    if (leader.compareAndSet(true, false)) {
                        thisAdminServerEnable = adminServerEnable;
                        System.out.println("server " + index + " is leader ahead mesh!");
                    }
                    apps[i - 1] = startZK(properties, index, dataDir + i, port, thisAdminServerEnable);
                });

        System.out.println("apps exited " + Arrays.toString(apps));
    }

    public static App startZK(Properties properties, int index, String dataDir, int clientPort, boolean adminServer) {
        App app = new App();
        Properties cloned = (Properties) properties.clone();
        cloned.setProperty("dataDir", dataDir);
        cloned.setProperty("clientPort", String.valueOf(clientPort));
        cloned.setProperty("admin.enableServer", String.valueOf(adminServer));

        try {
            createMyId(dataDir, index);
            app.runFromConfig(parseConfig(cloned));
        } catch (IOException | AdminServer.AdminServerException | QuorumPeerConfig.ConfigException e) {
            throw new RuntimeException(e);
        }
        return app;
    }

    public static List<Integer> countMesh(Properties properties) {
        List<Integer> list = properties.keySet().stream()
                .map(String::valueOf)
                .filter(k -> k.startsWith("server."))
                .map(k -> k.substring(7))
                .map(k -> {
                    try {
                        return Integer.parseInt(k);
                    } catch (NumberFormatException ignored) {
                    }
                    return -1;
                })
                .filter(i -> i > 0)
                .collect(Collectors.toList());

        return list.stream()
                .filter(i -> {
                    String property = properties.getProperty("server." + i);
                    String host = property.split(":")[0];
                    try {
                        return InetAddress.getByName(host).isLoopbackAddress();
                    } catch (UnknownHostException ignored) {
                        return false;
                    }
                })
                .collect(Collectors.toList());
    }


    public static Properties loadProperties() throws IOException {
        Properties properties = new Properties();
        properties.load(Files.newBufferedReader(new File("conf/zoo.cfg").toPath()));
        return properties;
    }

    public static void createMyId(String dataDir,int id) throws IOException {
        File base = new File(dataDir);
        //noinspection ResultOfMethodCallIgnored
        base.mkdirs();
        File myId=new File(base,"myid");
        if(myId.isFile()){
            //noinspection ResultOfMethodCallIgnored
            myId.delete();
        }
        Files.writeString(myId.toPath(),String.valueOf(id));
    }

    public static ServerConfig parseConfig(Properties properties) throws QuorumPeerConfig.ConfigException, IOException {
        QuorumPeerConfig quorumConfiguration = new QuorumPeerConfig();
        quorumConfiguration.parseProperties(properties);
        ServerConfig configuration = new ServerConfig();
        configuration.readFrom(quorumConfiguration);
        return configuration;
    }

    @Override
    public void runFromConfig(ServerConfig config) throws IOException, AdminServer.AdminServerException {
        super.runFromConfig(config);
    }
}
