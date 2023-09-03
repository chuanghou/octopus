
pid=$(ps -ef | grep java | grep MilkyDemoApplication | awk '{print $2}')
if [ -n "$pid"  ]; then
    kill -15 "$pid"
fi

script_path=$(cd $(dirname $0);pwd)

nohup java -cp "./bin/*:./lib/*" -Dstatic.resource="${script_path}/static/" com.bilanee.octopus.Application > console.log 2>&1 &
