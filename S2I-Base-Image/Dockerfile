FROM fedora:26

ADD ./build/amq7-broker.tar.gz /opt

COPY ./utils/run_amq.sh ./utils/get_inst.py ./broker_configs /opt/amq-broker-7.0.2/bin/

RUN mkdir /var/run/amq && \
    touch /var/run/amq/x && \
    mkdir /etc/qpid-dispatch && \
    touch /etc/qpid-dispatch/x && \
    chmod -R 777 /var/run/amq /etc/qpid-dispatch && \
    chmod +x /opt/amq-broker-7.0.2/bin/run_amq.sh /opt/amq-broker-7.0.2/bin/get_inst.py

VOLUME /var/run/amq /etc/qpid-dispatch

RUN dnf -y install java-1.8.0-openjdk libaio python gettext hostname procps qpid-dispatch-router qpid-dispatch-tools

ENV AMQ_HOME=/opt/amq-broker-7.0.2 PATH=$AMQ_HOME/bin:$PATH

CMD ["/opt/amq-broker-7.0.2/bin/run_amq.sh"]