# Stage runbook

CareFlow is deployed to Stage as its own service to avoid impacting existing applications.

## Reserved resources

- Hostname: `careflow.stage.dev`
- Local port: `8097`
- systemd service: `careflow.service`
- Bare release repository: `/srv/git/careflow-release.git`
- Runtime directory: `/srv/www/careflow.stage.dev/current`

## Safety checks

Before deployment, verify that the port and service name are not in use:

```bash
ssh stage.dev 'ss -ltnp | grep :8097 || true'
ssh stage.dev 'systemctl status careflow.service --no-pager || true'
```

Do not change global Java, Node, Apache or existing application services while deploying CareFlow.

## Deploy

```bash
npm run deploy:stage
```

The command builds the app locally, packages Quarkus and pushes a release artifact through Git to the stage bare repository. The server-side `post-receive` hook checks out the release and restarts only `careflow.service`.
