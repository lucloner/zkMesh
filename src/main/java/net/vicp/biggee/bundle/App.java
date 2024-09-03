package net.vicp.biggee.bundle;

import org.apache.zookeeper.server.admin.AdminServer;
import org.apache.zookeeper.server.quorum.QuorumPeer;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig;
import org.apache.zookeeper.server.quorum.QuorumPeerMain;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Hello world!
 *
 */
public class App extends QuorumPeerMain {
    public static void main(String[] args) {
        Properties properties;
        try {
            properties = loadProperties();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        loadJaas();

        List<Integer> meshList = countMesh(properties);
        String dataDir = properties.getProperty("dataDir");
        int clientPort = Integer.parseInt(properties.getProperty("clientPort"));
        boolean adminServerEnable = Boolean.parseBoolean(String.valueOf(properties.getOrDefault("admin.enableServer", "false")));
        int serverPort = Integer.parseInt(properties.getProperty("admin.serverPort"));
        String metricServer = properties.getProperty("metricsProvider.className");

        int count = meshList.size();
        if (count < 2) {
            count = 1;
            meshList.add(1);
        }

        App[] apps = new App[count];
        AtomicInteger startedCount = new AtomicInteger();
        IntStream.rangeClosed(1, count)
                .parallel()
                .forEach(i -> {
                    boolean admin = false;
                    String metricsProvider = null;
                    if (i == 1) {
                        int time = 0;
                        while (time++ == 0 || startedCount.get() < apps.length - 1) {
                            try {
                                //noinspection BusyWait
                                Thread.sleep(1000);
                            } catch (InterruptedException ignored) {
                            }
                        }
                        admin = adminServerEnable;
                        metricsProvider = metricServer;
                    }
                    int index = meshList.get(i - 1);
                    startedCount.incrementAndGet();
                    apps[i - 1] = startZK(args,
                            properties,
                            index,
                            dataDir + index,
                            clientPort + i - 1,
                            admin,
                            serverPort + i - 1,
                            metricsProvider);
                });

        System.out.println("apps exited " + Arrays.toString(apps));
    }

    public static App startZK(String[] args, Properties properties, int index, String dataDir, int clientPort, boolean adminServer, int serverPort, String metricServer) {
        App app = new App();
        Properties cloned = (Properties) properties.clone();
        cloned.setProperty("dataDir", dataDir);
        cloned.setProperty("log.dir", dataDir);
        cloned.setProperty("clientPort", String.valueOf(clientPort));
        cloned.setProperty("admin.enableServer", String.valueOf(adminServer));
        cloned.setProperty("admin.serverPort", String.valueOf(serverPort));
        if (metricServer == null) {
            cloned.remove("metricsProvider.className");

        }

        try {
            createMyId(dataDir, index);
            app.runFromConfig(parseConfig(args, cloned));
        } catch (IOException | AdminServer.AdminServerException | QuorumPeerConfig.ConfigException e) {
            throw new RuntimeException(e);
        }
        return app;
    }

    public static void loadJaas() {
        File file = new File("conf", "jaas.conf");
        if (!file.isFile()) {
            return;
        }
        System.setProperty("java.security.auth.login.config", file.getAbsolutePath());
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

    public static void createMyId(String dataDir, int id) throws IOException {
        File base = new File(dataDir);
        //noinspection ResultOfMethodCallIgnored
        base.mkdirs();
        File myId = new File(base, "myid");
        if (myId.isFile()) {
            //noinspection ResultOfMethodCallIgnored
            myId.delete();
        }
        Files.writeString(myId.toPath(), String.valueOf(id));
    }

    public static QuorumPeerConfig parseConfig(String[] args, Properties properties) throws QuorumPeerConfig.ConfigException, IOException {
        System.setProperty(QuorumPeer.CONFIG_KEY_MULTI_ADDRESS_ENABLED, String.valueOf(properties.getOrDefault("multiAddress.enabled", "false")));
        QuorumPeerConfig quorumConfiguration = new QuorumPeerConfig();
        if (args.length > 1) {
            quorumConfiguration.parse(args[0]);
        }
        quorumConfiguration.parseProperties(properties);
        return quorumConfiguration;
    }

    public void runFromConfig(QuorumPeerConfig config) throws IOException, AdminServer.AdminServerException {
        super.runFromConfig(config);
    }
}
