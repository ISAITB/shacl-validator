FROM eclipse-temurin:17-jre-alpine

RUN mkdir /validator
COPY validator.jar /validator
RUN sh -c 'touch /validator/validator.jar'
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/validator/validator.jar"]
EXPOSE 8080
WORKDIR /validator