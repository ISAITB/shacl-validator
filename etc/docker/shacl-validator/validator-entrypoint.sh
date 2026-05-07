#!/bin/sh
# Default JVM options. Any option in JVM_OPTIONS that duplicates a default will
# take precedence because it appears later on the command line (HotSpot resolves
# duplicate -XX flags and -D system-properties in favour of the last occurrence).
DEFAULT_JVM_OPTIONS="-XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"

exec java ${DEFAULT_JVM_OPTIONS} ${JVM_OPTIONS} -jar /validator/validator.jar "$@"