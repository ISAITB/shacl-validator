FROM eclipse-temurin:21-jre-jammy

RUN mkdir /validator
COPY validator.jar /validator
COPY validator-entrypoint.sh /validator
RUN chmod +x /validator/validator-entrypoint.sh && sh -c 'touch /validator/validator.jar'
EXPOSE 8080
WORKDIR /validator
ENTRYPOINT ["/validator/validator-entrypoint.sh"]