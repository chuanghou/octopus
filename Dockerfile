#拉取基础镜像
FROM centos:7
#设置用户名（非必选）
MAINTAINER houchuang <houchuang@outlook.com>
#设置环境变量
#设置进入后的目录
#安装vim编辑器
RUN yum -y install  vim
#安装ifconfig命令查看网络IP
RUN yum -y install net-tools
#安装java8及lib库
#RUN #yum -y install glibc.i686
#将安装包添加到容器中
#ADD jdk-8u192-linux-x64.tar.gz   /usr/local/java/
##配置java环境变量
#ENV JAVA_HOME /usr/local/java/jdk1.8.0_192
#ENV PATH $JAVA_HOME/bin:$PATH
##暴露端口号
#EXPOSE 80
#CMD echo "success--------------ok"
#CMD /bin/bash
