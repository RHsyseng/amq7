#!/bin/bash

function do_amq_setup() {

    if [ ! -d "$INSTANCE" ]; then

        echo -e "\n >>> creating artemis broker instance..."
        [[ "$AMQ_MODE" = 'single' ]] && NOWEB="" || NOWEB="--no-web"
        ${AMQ_HOME:="/opt/amq-broker-7.0.2"}/bin/artemis create $INSTANCE --user admin --password admin --role admin --allow-anonymous $NOWEB

        echo -e "\n >>> adding broker.xml for mode $AMQ_MODE configuration file..."
        cp $AMQ_HOME/bin/broker_$AMQ_MODE.xml /tmp/broker.xml

        echo -e "\n >>> adding logging.properties..."
        cp $AMQ_HOME/bin/logging.properties /tmp/logging.properties

        echo -e "\n >>> adding bootstrap.xml for jolokia..."
        cp $AMQ_HOME/bin/bootstrap.xml /tmp/bootstrap.xml

        echo -e "\n >>> swapping tmp broker config to $INSTANCE/etc/broker.xml..."
        envsubst < /tmp/broker.xml > $INSTANCE/etc/broker.xml

        echo -e "\n >>> swapping tmp logging config to $INSTANCE/etc/logging.properties..."
        envsubst < /tmp/logging.properties > $INSTANCE/etc/logging.properties

        echo -e "\n >>> swapping tmp bootstrap config to $INSTANCE/etc/bootstrap.xml..."
        envsubst < /tmp/bootstrap.xml > $INSTANCE/etc/bootstrap.xml

        CONTAINER_ID=$(basename $INSTANCE)
        export CONTAINER_ID

        exec $INSTANCE/bin/artemis run &
    fi
}

export BROKER_IP=`hostname -I | cut -f 1 -d ' '`
VOLUME="/var/run/amq/"
BASE=$(dirname $0)

case "${AMQ_MODE:="single"}" in

    'single' )
        export ARTEMIS_PORT=61616
        export JOLOKIA_PORT=8161
        INSTANCE=$($BASE/get_inst.py $VOLUME artemis $HOSTNAME)
        do_amq_setup
        wait $!
        exec tail -f /var/run/amq/artemis_1/log/artemis.log
    ;;
    'symmetric' )
        for i in 1 2 3
        do
            INSTANCE=$($BASE/get_inst.py $VOLUME artemis $HOSTNAME)
            export ARTEMIS_PORT=6161$i
            export JOLOKIA_PORT=816$i
            do_amq_setup
        done
        wait $!
        exec tail -f /var/run/amq/artemis_1/log/artemis.log /var/run/amq/artemis_2/log/artemis.log /var/run/amq/artemis_3/log/artemis.log
    ;;
    'replicated' )
        for i in 1 2 3
        do
            INSTANCE=$($BASE/get_inst.py $VOLUME artemis $HOSTNAME)
            export AMQ_MODE='replicated_master'
            export ARTEMIS_PORT=6161$i
            export JOLOKIA_PORT=816$i
            do_amq_setup

            INSTANCE=$($BASE/get_inst.py $VOLUME artemis $HOSTNAME)
            export AMQ_MODE='replicated_slave'
            export ARTEMIS_PORT=6161$i
            export JOLOKIA_PORT=816$(($i+3))
            do_amq_setup

            sleep 3
        done

        wait $!
        exec tail -f /var/run/amq/artemis_1/log/artemis.log /var/run/amq/artemis_2/log/artemis.log /var/run/amq/artemis_3/log/artemis.log /var/run/amq/artemis_4/log/artemis.log /var/run/amq/artemis_5/log/artemis.log /var/run/amq/artemis_6/log/artemis.log
    ;;
    'interconnect' )
        cp $AMQ_HOME/bin/qdrouterd_${LOCALE:="us"}.conf /etc/qpid-dispatch/qdrouterd.conf
        exec qdrouterd
    ;;
esac
