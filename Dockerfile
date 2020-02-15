FROM openjdk:11-slim

COPY target/kotlin-authoriser-1.0-SNAPSHOT.jar /home/kotlin-authoriser.jar

CMD ["java", "-jar", "/home/kotlin-authoriser.jar"]

