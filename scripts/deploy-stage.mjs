import { cpSync, existsSync, mkdirSync, rmSync, writeFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { execFileSync } from 'node:child_process';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const quarkusApp = resolve(root, 'backend/target/quarkus-app');
const releaseRoot = resolve(root, '.deploy/stage');
const remote = process.env.STAGE_REMOTE ?? 'ssh://stage.dev/srv/git/careflow-release.git';

const run = (command, args, options = {}) => {
  execFileSync(command, args, {
    cwd: options.cwd ?? root,
    stdio: 'inherit',
    ...options
  });
};

if (!existsSync(resolve(quarkusApp, 'quarkus-run.jar'))) {
  throw new Error('Missing backend/target/quarkus-app/quarkus-run.jar. Run npm run build first.');
}

rmSync(releaseRoot, { recursive: true, force: true });
mkdirSync(releaseRoot, { recursive: true });
cpSync(quarkusApp, resolve(releaseRoot, 'quarkus-app'), { recursive: true });

let revision = 'unknown';
try {
  revision = execFileSync('git', ['rev-parse', '--short', 'HEAD'], { cwd: root, encoding: 'utf8' }).trim();
} catch {
  revision = 'uncommitted';
}

writeFileSync(resolve(releaseRoot, 'release.json'), `${JSON.stringify({
  product: 'CareFlow',
  revision,
  deployedAt: new Date().toISOString(),
  runtime: 'Java 21 / Quarkus',
  port: 8097
}, null, 2)}\n`);

run('git', ['init', '-b', 'main'], { cwd: releaseRoot });
run('git', ['add', '.'], { cwd: releaseRoot });
run('git', ['commit', '-m', `CareFlow stage release ${revision}`], { cwd: releaseRoot });
run('git', ['remote', 'add', 'stage', remote], { cwd: releaseRoot });
run('git', ['push', '--force', 'stage', 'main'], { cwd: releaseRoot });
