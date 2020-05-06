All spans will be sent to the Jaeger, which can be started like the following
docker run   --rm  --name=jaeger   -p 6831:6831/udp   -p 6832:6832/udp   -p 16686:16686   jaegertracing/all-in-one:1.7   --log-level=debug
 Make sure that Maven version 3.6+ is installed.

git clone https://github.com/lsa5521/OT_K8S_DOCKER.git

To create the container with Publisher and Formatter components.
 Run    mvn clean package   in java-opentracing-docker directory
Build image -   docker build  -t otexample_image . 

The image can be checked by running      
docker run -it --rm --name otexample otexample_image:latest lesson05.solution.Formatter bob 99f9848e8fd25a9e:99f9848e8fd25a9e:0:1 38c18dc3-7cda-49f6-9273-7c0d9120523f
The second parameter here is the parent span context and the third is some diagnostic ID which can be used for tagging the spans.

Build the Controller application 
cd k8scontroller
mvn clean package
and then run it
java -cp ./target/k8scontroller.jar:./target/dependency/* github.lsa5521.k8scontroller.K8sController bob hello 38c18dc3-7cda-49f6-9273-7c0d9120523f
Check results in the Jaeger UI

