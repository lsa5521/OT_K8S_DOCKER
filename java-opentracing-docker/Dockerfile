FROM openjdk:8-jre-slim
WORKDIR /opentracing
# Copy of binaries
COPY   ./target/otdockerexample.jar ./
COPY ./target/dependency ./
ENTRYPOINT ["java","-DJAEGER_AGENT_HOST=192.168.1.113", "-cp", "./otdockerexample.jar:./dependency/*"]
