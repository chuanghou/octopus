package com.bilanee.octopus.adapter.tunnel;

import lombok.CustomLog;
import lombok.SneakyThrows;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;
import org.apache.commons.io.IOUtils;

import java.io.IOException;


@CustomLog
public class Ssh {

    @SneakyThrows
    public static void exec(String command) {
        final SSHClient ssh = new SSHClient();
        ssh.loadKnownHosts();
        Session session = null;
        try {
            ssh.connect("118.184.179.116");
            ssh.authPassword("administrator", "co188.com");
            session = ssh.startSession();
            final Command cmd0 = session.exec("cd C:\\Users\\Administrator\\Desktop\\PowerMarketExperiment & " + command);
            String result = IOUtils.toString(cmd0.getInputStream(), "GBK");
            log.info(result);
        } finally {
            try {
                if (session != null) {
                    session.close();
                }
            } catch (IOException e) {
                // Do Nothing   
            }
            
            ssh.disconnect();
        }
    }

}