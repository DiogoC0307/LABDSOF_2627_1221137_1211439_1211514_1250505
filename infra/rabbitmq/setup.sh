#!/bin/sh
# Runs inside the RabbitMQ container right after the broker has started (see compose.yaml and
# infra/k8s/<env>/shared/rabbitmq.yaml). It:
#   1. creates ONE login per service and limits it to that service's own queues,
#   2. imports the topology (exchanges, queues, retry queues, dead-letter queues).
# Passwords come from the environment (Kubernetes Secrets / .env), never from this file.
# It is safe to run again: existing users get their password and permissions re-applied.
set -e
# Wait for the broker to create its Erlang cookie. rabbitmqctl runs here as root: if it created the cookie
# first, the broker (which runs as the rabbitmq user) could not read it and would fail to boot ("eacces").
until [ -s /var/lib/rabbitmq/.erlang.cookie ]; do sleep 1; done
until rabbitmqctl await_startup >/dev/null 2>&1; do sleep 2; done

# create_user <name> <password> <configure-regex> <write-regex> <read-regex>
#   configure = create/delete queues      write = publish to exchanges      read = consume from queues
create_user() {
  if rabbitmqctl list_users -q | cut -f1 | grep -qx "$1"; then
    rabbitmqctl change_password "$1" "$2" >/dev/null
  else
    rabbitmqctl add_user "$1" "$2" >/dev/null
  fi
  rabbitmqctl set_user_tags "$1" >/dev/null          # no tags: cannot log in to the management UI
  rabbitmqctl set_permissions -p / "$1" "$3" "$4" "$5" >/dev/null
  echo "rabbitmq user '$1' ready"
}

# Exchanges a service may publish to (the three exchanges of the topology)
EXCHANGES='^(escala\.events|escala\.retry|escala\.dlx)$'

#           user                          password                          configure   write         read
create_user "$ESCALA_API_USER"            "$ESCALA_API_PASSWORD"            '^api\.'    "$EXCHANGES"  '^api\.'
create_user "$ELEGIBILIDADE_WORKER_USER"  "$ELEGIBILIDADE_WORKER_PASSWORD"  '^worker\.' "$EXCHANGES"  '^worker\.'

rabbitmqctl import_definitions /etc/rabbitmq/definitions.json
echo "rabbitmq topology imported"
