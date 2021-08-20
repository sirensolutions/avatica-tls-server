FROM openjdk:11-jre
LABEL io.siren.avatica.maintainer="Sindice LTD <info@siren.io>"

RUN useradd -m avatica
RUN mkdir -p /home/avatica/server && \
    mkdir -p /home/avatica/jdbc && \
    mkdir -p /home/avatica/pki && \
    chown -R avatica /home/avatica

ADD --chown=avatica target/avatica-tls-server-*.jar /home/avatica/server

EXPOSE 8765

USER avatica

ENTRYPOINT ["java", "-cp", "/home/avatica/server/*:/home/avatica/jdbc/*", "io.siren.avatica.TlsServer", "-p", "8765"]
