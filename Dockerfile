FROM maven:3.3-jdk-8
COPY . /app
WORKDIR /app
RUN mvn clean compile assembly:single
RUN mvn test
ENV PORT 80
ENV LOG_LEVEL info
EXPOSE 80
CMD ["sh", "/app/start.sh"]
