#!/usr/bin/env bash
# Run ON the EC2 box as root. Serves jansho.com / www / api alongside the legacy
# pansariwala.shop hosts and expands the existing Let's Encrypt cert to cover both.
set -euo pipefail

export OLD="${OLD_DOMAIN:-pansariwala.shop}"
export NEW="${NEW_DOMAIN:-jansho.com}"
CONF="${NGINX_CONF:-/etc/nginx/conf.d/pansariwala.conf}"

[[ -f "$CONF" ]] || { echo "nginx config not found: $CONF" >&2; exit 1; }
cp "$CONF" "${CONF}.bak.$(date +%s)"

# server_name: add the NEW twin of every OLD host (idempotent).
perl -pi -e '
  s{(server_name\s+)([^;]*);}{
    my ($kw, @n) = ($1, split(/\s+/, $2));
    my %have = map { $_ => 1 } @n;
    for my $h (@n) { (my $t = $h) =~ s/\Q$ENV{OLD}\E$/$ENV{NEW}/; push @n, $t unless $have{$t}++; }
    $kw . join(" ", @n) . ";"
  }e;
' "$CONF"

# CORS origin regex: accept both web origins (idempotent).
perl -pi -e '
  next unless /http_origin/;
  my ($o, $n) = (quotemeta(quotemeta $ENV{OLD}), quotemeta(quotemeta $ENV{NEW}));
  s{$o}{"(" . quotemeta($ENV{OLD}) . "|" . quotemeta($ENV{NEW}) . ")"}e unless /$n/;
' "$CONF"

echo "--- server_name / origin lines now:"
grep -nE "server_name|http_origin" "$CONF"

nginx -t
systemctl reload nginx

CERT_NAME="$(certbot certificates 2>/dev/null | awk -v d="$OLD" '/Certificate Name:/ {name=$3} /Domains:/ && index($0, d) {print name; exit}')"
CERT_NAME="${CERT_NAME:-$OLD}"
echo "Expanding certificate '$CERT_NAME'"
certbot --nginx --non-interactive --agree-tos --expand --cert-name "$CERT_NAME" \
  -d "$OLD" -d "www.$OLD" -d "api.$OLD" \
  -d "$NEW" -d "www.$NEW" -d "api.$NEW"

nginx -t
systemctl reload nginx

for h in "api.$NEW" "api.$OLD"; do
  printf '%s /health -> ' "$h"
  curl -sS -m 10 -o /dev/null -w '%{http_code}\n' "https://$h/health" || true
done
