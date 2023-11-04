package com.bilanee.octopus.adapter.tunnel;

import lombok.CustomLog;
import lombok.SneakyThrows;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.util.concurrent.ExecutionException;


@CustomLog
public class Ssh {

    @SneakyThrows
    public static void exec(String command) {
        long l = System.currentTimeMillis();
        final SSHClient ssh = new SSHClient();
        ssh.addHostKeyVerifier(new PromiscuousVerifier());
        ssh.loadKnownHosts();
        Session session = null;
        long s;
        try {
            ssh.connect("118.184.179.116");
            ssh.authPassword("administrator", "co188.com");
            session = ssh.startSession();
            final Command cmd0 = session.exec("cd C:\\Users\\Administrator\\Desktop\\PowerMarketExperiment & " + command);
            System.out.println(IOUtils.toString(cmd0.getInputStream(), "GBK"));
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

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        exec("python manage.py intra_da_market_clearing 2 1");
    }

}