package com.bilanee.octopus.adapter.tunnel;

import com.bilanee.octopus.config.OctopusProperties;
import com.stellariver.milky.common.base.BeanUtil;
import lombok.CustomLog;
import lombok.SneakyThrows;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;


@CustomLog
public class Ssh {

    @SneakyThrows
    public static void exec(String command) {
        log.info("begin " + command );
        final SSHClient ssh = new SSHClient();
        final SSHClient ssh1 = new SSHClient();

        ssh.addHostKeyVerifier(new PromiscuousVerifier());
        Session session = null;
        long s;
        try {
            OctopusProperties octopusProperties = BeanUtil.getBean(OctopusProperties.class);
            ssh.connect(octopusProperties.getIp());
            ssh.authPassword(octopusProperties.getUsername(), octopusProperties.getPassword());
            session = ssh.startSession();
            final Command cmd0 = session.exec("source ~/.bashrc; conda activate powermarket; cd /home/sjtu/PowerMarketExperiment; " + command);
            System.out.println(IOUtils.toString(cmd0.getInputStream(), StandardCharsets.UTF_8));
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
        exec("echo $PATH");
    }


}