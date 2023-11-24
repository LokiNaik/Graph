FROM openjdk:8
ADD target/Graph-0.0.1-SNAPSHOT.jar graph.jar
ENTRYPOINT ["java","-jar","graph.jar","--spring.config.location=file:/config/graph_application.properties"]