package com.bilanee.octopus.adapter.tunnel;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Properties;

import static java.lang.System.in;

@Component
public class SshClient {

    @SneakyThrows
    public String exec(String command) {
        JSch jSch = new JSch();
        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        Session session = jSch.getSession("administrator", "118.184.179.116", 22);
        session.setConfig(config);
        String password = "co188.com";
        session.setPassword(password);
        session.connect(30000);
        ChannelShell channelShell = (ChannelShell) session.openChannel("shell");
        channelShell.sendSignal("python --version");
        return IOUtils.toString(in, "GBK");
    }

    public static void main(String[] args) {
        System.out.println(new SshClient().exec("python C:\\Users\\Administrator\\Desktop\\PowerMarketExperiment\\manage.py intra_pre_clearing 1"));
    }

}
