#!/bin/bash
# Expenses on Cloudflare: linked beside the Worker's entry, then `wrangler dev` (the default) or `deploy`
# (a token in the environment: CLOUDFLARE_API_TOKEN, CLOUDFLARE_ACCOUNT_ID)
set -euo pipefail
cd "$(dirname "$0")/../.."
sbt -Dplatforms=jvm,js referenceCloudflareJS/worker
cd reference/cloudflare/worker
npx -y wrangler@4 "${1:-dev}" "${@:2}"
