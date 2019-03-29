FROM openjdk:11.0.2-jre

RUN mkdir /validator
COPY validator.war /validator
RUN sh -c 'touch /validator/validator.jar'
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/validator/validator.war"]
EXPOSE 8080
WORKDIR /validator