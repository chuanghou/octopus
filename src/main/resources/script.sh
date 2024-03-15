
# 准备依赖
mvn dependency:copy-dependencies -DoutputDirectory=lib -DincludeScope=compile


java -cp ".\bin\*;.\lib\*" -Xms1024m -Xmx1024m "-Dstatic.resource=C:\Users\Administrator\octopus\static\" "-Dspring.profiles.active=prod" com.bilanee.octopus.Application

