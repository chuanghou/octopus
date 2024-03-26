package com.bilanee.octopus.adapter.tunnel;

import lombok.CustomLog;
import lombok.SneakyThrows;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ExecutionException;


@CustomLog
public class Ssh {

    @SneakyThrows
    public static void exec(String command) {
        long l = System.currentTimeMillis();
        final SSHClient ssh = new SSHClient();
        ssh.addHostKeyVerifier(new PromiscuousVerifier());
        Session session = null;
        long s;
        try {
            ssh.connect("127.0.0.1");
            ssh.authPassword("administrator", "gjdtsjtu@public0313");
            session = ssh.startSession();
            final Command cmd0 = session.exec("cd C:\\Users\\Administrator\\Desktop\\PowerMarketExperimentv2 & " + command);
            System.out.println(IOUtils.toString(cmd0.getInputStream(), "GBK"));
        } finally {
            try {
                if (session != null) {
                    session.close();
                }
            } catch (IOException e) {
                log.error(Objects.toString(e), e);
            }
            ssh.disconnect();
        }
    }

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        try {
            exec("python manage.py annual_default_bid");
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

}