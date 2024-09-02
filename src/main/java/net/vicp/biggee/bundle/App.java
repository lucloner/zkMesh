package net.vicp.biggee.bundle;

import org.apache.zookeeper.server.ServerConfig;
import org.apache.zookeeper.server.ZooKeeperServerMain;
import org.apache.zookeeper.server.admin.AdminServer;
import org.apache.zookeeper.server.embedded.ZooKeeperServerEmbedded;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig;
import org.apache.zookeeper.server.quorum.QuorumPeerMain;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.Executors;

/**
 * Hello world!
 *
 */
public class App extends ZooKeeperServerMain {
    public static void main(String[] args) {
        Properties properties = new Properties();
        try {
            properties.load(Files.newBufferedReader(new File("conf/zoo.cfg").toPath()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        String dataDir = properties.getProperty("dataDir");
        int clientPort = Integer.parseInt(properties.getProperty("clientPort"));
//        boolean adminServer = Boolean.parseBoolean(properties.getProperty("admin.enableServer"));
        int adminServerPort = Integer.parseInt(properties.getProperty("admin.serverPort"));

        App app1 = new App();
        //noinspection resource
        Executors.newWorkStealingPool().execute(()->{
            try {
                createMyId(dataDir,1);
                app1.runFromConfig(parseConfig(properties));
            } catch (IOException | AdminServer.AdminServerException | QuorumPeerConfig.ConfigException e) {
                throw new RuntimeException(e);
            }
        });

        App app2 = new App();
        Properties properties2 = (Properties)properties.clone();
        String dataDir2 = dataDir+2;
        properties2.setProperty("dataDir",dataDir2);
        properties2.setProperty("clientPort", String.valueOf(clientPort+1));
        properties2.setProperty("admin.enableServer", "false");
        properties2.setProperty("admin.serverPort", String.valueOf(adminServerPort+1));

        Executors.newWorkStealingPool().execute(()->{
            try {
                createMyId(dataDir2,2);
                app2.runFromConfig(parseConfig(properties2));
            } catch (IOException | AdminServer.AdminServerException | QuorumPeerConfig.ConfigException e) {
                throw new RuntimeException(e);
            }
        });

        App app3 = new App();
        Properties properties3 = (Properties)properties.clone();
        String dataDir3 = dataDir+3;
        properties3.setProperty("dataDir",dataDir3);
        properties3.setProperty("clientPort", String.valueOf(clientPort+2));
        properties3.setProperty("admin.enableServer", "false");
        properties3.setProperty("admin.serverPort", String.valueOf(adminServerPort+1));

        try {
            createMyId(dataDir3,3);
            app3.runFromConfig(parseConfig(properties3));
        } catch (IOException | AdminServer.AdminServerException | QuorumPeerConfig.ConfigException e) {
            throw new RuntimeException(e);
        }
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
