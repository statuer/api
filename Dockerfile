FROM maven:3.3-jdk-8
COPY . /app
WORKDIR /app
RUN mvn clean compile assembly:single
ENV PORT 80
ENV LOG_LEVEL info
EXPOSE 80
CMD "java -jar target/$(ls -p target | grep -v /)"
