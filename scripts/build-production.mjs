import { cpSync, existsSync, rmSync, writeFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { execFileSync } from 'node:child_process';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const frontendDistCandidates = [
  resolve(root, 'frontend/dist/frontend/browser'),
  resolve(root, 'frontend/dist/frontend')
];
const staticTarget = resolve(root, 'backend/src/main/resources/META-INF/resources');

const run = (command, args, options = {}) => {
  execFileSync(command, args, {
    cwd: root,
    stdio: 'inherit',
    ...options
  });
};

run('npm', ['--prefix', 'frontend', 'run', 'build']);

rmSync(staticTarget, { recursive: true, force: true });

const frontendDist = frontendDistCandidates.find((candidate) => existsSync(resolve(candidate, 'index.html')));
if (!frontendDist) {
  throw new Error(`Angular build output not found. Checked: ${frontendDistCandidates.join(', ')}`);
}

cpSync(frontendDist, staticTarget, { recursive: true });
writeFileSync(resolve(staticTarget, 'build-info.json'), `${JSON.stringify({
  product: 'CareFlow',
  builtAt: new Date().toISOString(),
  frontend: 'Angular 22',
  backend: 'Jakarta REST / Quarkus'
}, null, 2)}\n`);

run('mvn', ['-f', 'backend/pom.xml', '-B', 'test', 'package']);
