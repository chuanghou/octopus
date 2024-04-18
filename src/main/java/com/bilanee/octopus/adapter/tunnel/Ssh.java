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


@CustomLog
public class Ssh {

    @SneakyThrows
    public static void exec(String command) {
        log.info("begin " + command );
        final SSHClient ssh = new SSHClient();
        ssh.addHostKeyVerifier(new PromiscuousVerifier());
        Session session = null;
        long s;
        try {
            ssh.connect("106.15.54.213");
            ssh.authPassword("root", "SJTU2024");
            session = ssh.startSession();
            final Command cmd0 = session.exec("cd /home/sjtu/PowerMarketExperiment && " + command);
            System.out.println(IOUtils.toString(cmd0.getInputStream(), "GBK"));
        } finally {
            log.info("end " + command );
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

    public static void main(String[] args) {
        exec("pwd");
    }


}