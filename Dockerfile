FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app
RUN addgroup --system app && adduser --system --no-create-home --ingroup app appuser
COPY target/*.jar app.jar
RUN mkdir -p /app/data && chown -R appuser:app /app
USER appuser
EXPOSE 8088
ENTRYPOINT ["java", "-jar", "app.jar"]
