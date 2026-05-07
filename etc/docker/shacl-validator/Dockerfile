FROM eclipse-temurin:25-jre-jammy

RUN mkdir /validator
COPY validator.jar /validator
RUN sh -c 'touch /validator/validator.jar'
EXPOSE 8080
WORKDIR /validator
ENTRYPOINT ["java","-XX:+ExitOnOutOfMemoryError", "-Djava.security.egd=file:/dev/./urandom","-jar","/validator/validator.jar"]