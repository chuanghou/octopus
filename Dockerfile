# Second stage: minimal runtime environment
From demo-registry-vpc.cn-hangzhou.cr.aliyuncs.com/demo/openjdk:8-jre-alpine
# copy jar from the first stage
EXPOSE 8080
