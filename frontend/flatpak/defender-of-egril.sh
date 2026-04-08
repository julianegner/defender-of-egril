#!/bin/sh
# Wrapper script for Defender of Egril inside a Flatpak sandbox.
exec /app/opt/defender-of-egril/bin/defender-of-egril "$@"
