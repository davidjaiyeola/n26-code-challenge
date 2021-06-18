FROM adoptopenjdk/openjdk8
LABEL maintainer="David Jaiyeola<david.jaiyeola@gmail.com>"
# Add the service itself
ARG JAR_FILE
COPY target/coding-challenge-1.0.2.jar /usr/share/coding-challenge/coding-challenge-1.0.2.jar
ENV JAVA_OPTS=""
ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /usr/share/coding-challenge/coding-challenge-1.0.2.jar" ]
