#!/usr/bin/env python3
"""将一个真实 plus-ui App 注册到 release-artifacts 的 LB + 独立 Nginx 体系。"""

from __future__ import annotations

import argparse
import json
import re
import secrets
import string
from pathlib import Path


RESERVED_PREFIXES = {
    "admin",
    "monitor",
    "snail-job",
    "snail-ai",
    "dev-api",
    "prod-api",
    "actuator",
}
RESERVED_PORTS = {
    40080,
    40443,
    42080,
    42081,
    43000,
    43080,
    43081,
    43306,
    46379,
    47888,
    48080,
    48081,
    48800,
    48888,
    48900,
    49000,
    49001,
    49090,
    49091,
    49200,
}
UPSTREAM_MARKER = "# APP_UPSTREAMS: add_app.py 在此处追加 App upstream。"
ROUTE_MARKER = "    # APP_ROUTES: add_app.py 在此处追加 App 路由。"


def release_root(repo: Path) -> Path:
    nested = repo / "release-artifacts"
    if nested.is_dir():
        return nested
    if (repo / "docker" / "docker-compose-frontend.yml").is_file():
        return repo
    return nested


def docker_root(repo: Path) -> Path:
    return release_root(repo) / "docker"


def compose_path(repo: Path) -> Path:
    return docker_root(repo) / "docker-compose-frontend.yml"


def nginx_root(repo: Path) -> Path:
    return docker_root(repo) / "frontend" / "nginx"


def lb_templates(repo: Path) -> tuple[Path, Path]:
    root = nginx_root(repo) / "lb"
    return root / "nginx-lb-http.conf.template", root / "nginx-lb-tls.conf.template"


def env_key(app: str, suffix: str) -> str:
    normalized = re.sub(r"[^A-Za-z0-9]", "_", app).upper()
    return f"{normalized}_{suffix}"


def upstream_key(app: str) -> str:
    return re.sub(r"[^a-z0-9]", "_", app.lower()).strip("_")


def generate_sensitive_prefix() -> str:
    alphabet = string.ascii_lowercase + string.digits
    return "".join(secrets.choice(alphabet) for _ in range(10))


def parse_env(path: Path) -> dict[str, str]:
    if not path.exists():
        return {}
    result: dict[str, str] = {}
    for raw in path.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        result[key.strip()] = value.strip().strip("'\"")
    return result


def used_ports(repo: Path, exclude_key: str | None = None) -> set[int]:
    text = compose_path(repo).read_text(encoding="utf-8")
    values = {
        int(value)
        for key, value in re.findall(r"\$\{([A-Z0-9_]+):-([0-9]+)\}:[0-9]+", text)
        if key != exclude_key
    }
    return values | RESERVED_PORTS


def allocate_port(repo: Path) -> int:
    used = used_ports(repo)
    port = 41080
    while port in used:
        port += 1
    if port >= 42000:
        raise ValueError("41080-41999 App 端口段已用完")
    return port


def configured_apps(repo: Path) -> list[str]:
    document = json.loads((release_root(repo) / "apps.json").read_text())
    return sorted(app['id'] for app in document['apps'] if app['shipped'])


class Writer:
    def __init__(self, dry_run: bool) -> None:
        self.dry_run = dry_run
        self.changes: list[str] = []

    def write(self, path: Path, content: str) -> None:
        current = path.read_text(encoding="utf-8") if path.exists() else None
        if current == content:
            print(f"[SKIP] {path}")
            return
        self.changes.append(str(path))
        if self.dry_run:
            print(f"[DRY] write {path}")
            return
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(content, encoding="utf-8")
        print(f"[WRITE] {path}")

    def ensure_dir(self, path: Path) -> None:
        if self.dry_run:
            print(f"[DRY] mkdir {path}")
            return
        path.mkdir(parents=True, exist_ok=True)


def upsert_env(path: Path, key: str, value: str, writer: Writer) -> None:
    text = path.read_text(encoding="utf-8") if path.exists() else ""
    line = f"{key}={value}"
    pattern = re.compile(rf"(?m)^{re.escape(key)}=.*$")
    if pattern.search(text):
        updated = pattern.sub(line, text)
    else:
        separator = "" if not text or text.endswith("\n") else "\n"
        updated = f"{text}{separator}{line}\n"
    writer.write(path, updated)


def patch_compose(repo: Path, app: str, port: int, registration: dict, writer: Writer) -> None:
    path = compose_path(repo)
    text = path.read_text(encoding="utf-8")
    prefix_key = env_key(app, "PREFIX")
    port_key = env_key(app, "PORT")
    lb_env_key = f"APP_{prefix_key}"

    if registration['ingress'] == 'lb' and f"      {lb_env_key}:" not in text:
        anchor = '      APP_ADMIN_WEB_PREFIX: "${ADMIN_WEB_PREFIX:?ADMIN_WEB_PREFIX is required}"'
        if text.count(anchor) != 2:
            raise ValueError("Compose LB env 锚点数量异常，拒绝自动修改")
        env_line = f'      {lb_env_key}: "${{{prefix_key}:?{prefix_key} is required}}"'
        text = text.replace(anchor, f"{anchor}\n{env_line}")

    service_name = f"namewta-nginx-{app}"
    if f"  {service_name}:\n" not in text:
        marker = "\nnetworks:\n"
        if marker not in text:
            raise ValueError("Compose 缺少 networks 锚点，拒绝自动修改")
        service = f'''\n  {service_name}:
    image: "${{NGINX_IMAGE:-nginx:1.31.1}}"
    container_name: namewta-nginx-{app}
    environment:
      TZ: Asia/Shanghai
      APP_PREFIX: "${{{prefix_key}:?{prefix_key} is required}}"
      BACKEND_SERVER1: "${{BACKEND_SERVER1:-namewta-server1:8080}}"
      BACKEND_SERVER2: "${{BACKEND_SERVER2:-namewta-server2:8080}}"
      NGINX_ENVSUBST_FILTER: "^(APP_|BACKEND_|LB_)"
    ports:
      - "${{NAMEWTA_BIND_HOST:-127.0.0.1}}:${{{port_key}:-{port}}}:80"
    volumes:
      - ./frontend/nginx/apps/nginx-{app}.conf.template:/etc/nginx/templates/default.conf.template:ro
      - ./frontend/nginx/html/{app}:/usr/share/nginx/html:ro
      - ${{NAMEWTA_DATA_ROOT:-./runtime}}/nginx/log/{app}:/var/log/nginx
    healthcheck:
      test: ["CMD", "curl", "--fail", "--silent", "http://127.0.0.1/healthz"]
      interval: 15s
      timeout: 5s
      retries: 3
    logging: *nginx-logging
    restart: unless-stopped
    networks: [namewta]
'''
        text = text.replace(marker, f"{service}{marker}", 1)

    tls = registration.get('tls')
    if tls and f"  {tls['composeService']}:\n" not in text:
        service = f'''
  {tls['composeService']}:
    image: "${{NGINX_IMAGE:-nginx:1.31.1}}"
    container_name: {tls['composeService']}
    profiles: [tls]
    environment:
      TZ: Asia/Shanghai
      APP_PREFIX: "${{{prefix_key}:?{prefix_key} is required}}"
      BACKEND_SERVER1: "${{BACKEND_SERVER1:-namewta-server1:8080}}"
      BACKEND_SERVER2: "${{BACKEND_SERVER2:-namewta-server2:8080}}"
      NGINX_ENVSUBST_FILTER: "^(APP_|BACKEND_|LB_)"
    ports:
      - "${{NAMEWTA_BIND_HOST:-127.0.0.1}}:${{{tls['portEnv']}:-{tls['defaultPort']}}}:443"
    volumes:
      - ./{tls['nginxTemplate'].removeprefix('docker/')}:/etc/nginx/templates/default.conf.template:ro
      - ./frontend/nginx/html/{app}:/usr/share/nginx/html:ro
      - ${{NAMEWTA_CERT_ROOT:-./frontend/nginx/cert}}/{app}:/etc/nginx/cert/{app}:ro
      - ${{NAMEWTA_DATA_ROOT:-./runtime}}/nginx/log/{app}-tls:/var/log/nginx
    healthcheck:
      test: ["CMD", "curl", "--fail", "--silent", "http://127.0.0.1/healthz"]
      interval: 15s
      timeout: 5s
      retries: 3
    logging: *nginx-logging
    restart: unless-stopped
    networks: [namewta]
'''
        text = text.replace('\nnetworks:\n', service + '\nnetworks:\n', 1)

    def bind_app_environment(match: re.Match) -> str:
        block = match[0]
        if not re.search(r'^      APP_PREFIX:', block, re.M):
            return block
        bindings = {'BACKEND_SERVER1': '"${BACKEND_SERVER1:-namewta-server1:8080}"',
                    'BACKEND_SERVER2': '"${BACKEND_SERVER2:-namewta-server2:8080}"'}
        if registration['apiKind'] == 'sso' and f'${{{prefix_key}:?' in block:
            bindings['APP_ORIGIN'] = f'"${{{registration["originEnv"]}:?{registration["originEnv"]} is required}}"'
        for key, default in bindings.items():
            pattern = re.compile(r'^      ' + key + r': ([^\n]+)\n', re.M)
            existing = pattern.findall(block)
            if len(set(existing)) > 1:
                raise ValueError(f'Compose contains conflicting {key} bindings')
            # Normalize only this known binding; preserve an existing operator-supplied expression.
            value = existing[0] if existing else default
            block = pattern.sub('', block)
            block = re.sub(r'(^      APP_PREFIX: [^\n]+\n)',
                           lambda item: item[0] + f'      {key}: {value}\n', block, count=1, flags=re.M)
        return block

    text = re.sub(r'^  namewta-nginx-[a-z0-9-]+:\n.*?(?=^  [a-z0-9-]+:\n|^networks:\n|\Z)',
                  bind_app_environment, text, flags=re.M | re.S)
    text = re.sub(re.escape('${' + port_key + ':-') + r'\d+}', '${' + port_key + ':-' + str(port) + '}', text)

    writer.write(path, text)


def patch_lb_template(path: Path, app: str, writer: Writer) -> None:
    text = path.read_text(encoding="utf-8")
    key = upstream_key(app)
    prefix_key = env_key(app, "PREFIX")
    lb_prefix_key = f"APP_{prefix_key}"

    if f"upstream app_{key} " not in text:
        if UPSTREAM_MARKER not in text:
            raise ValueError(f"{path} 缺少 upstream 锚点")
        upstream = (
            f"upstream app_{key} {{\n"
            f"    server namewta-nginx-{app}:80;\n"
            "    keepalive 32;\n"
            "}\n\n"
        )
        text = text.replace(UPSTREAM_MARKER, f"{upstream}{UPSTREAM_MARKER}", 1)

    if f"location /${{{lb_prefix_key}}}/" not in text:
        if ROUTE_MARKER not in text:
            raise ValueError(f"{path} 缺少 route 锚点")
        route = (
            f"    location = /${{{lb_prefix_key}}} {{\n"
            f"        return 302 /${{{lb_prefix_key}}}/;\n"
            "    }\n\n"
            f"    location /${{{lb_prefix_key}}}/ {{\n"
            f"        proxy_pass http://app_{key}/;\n"
            "        proxy_http_version 1.1;\n"
            "        proxy_set_header Host $http_host;\n"
            "        proxy_set_header X-Real-IP $remote_addr;\n"
            "        proxy_set_header X-Forwarded-For $remote_addr;\n"
            "        proxy_set_header Forwarded \"\";\n"
            "        proxy_set_header X-Forwarded-Proto $scheme;\n"
            f"        proxy_set_header X-Forwarded-Prefix /${{{lb_prefix_key}}};\n"
            "        proxy_set_header Upgrade $http_upgrade;\n"
            "        proxy_set_header Connection $connection_upgrade;\n"
            "        proxy_read_timeout 86400s;\n"
            "        proxy_buffering off;\n"
            "    }\n\n"
        )
        text = text.replace(ROUTE_MARKER, f"{route}{ROUTE_MARKER}", 1)

    writer.write(path, text)


def create_app_assets(repo: Path, app: str, writer: Writer) -> None:
    nginx = nginx_root(repo)
    source_template = nginx / "apps" / "nginx-admin-web.conf.template"
    target_template = nginx / "apps" / f"nginx-{app}.conf.template"
    if not target_template.exists():
        writer.write(target_template, source_template.read_text(encoding="utf-8"))

    gitignore = "*\n!.gitignore\n"
    writer.write(nginx / "html" / app / ".gitignore", gitignore)
    writer.write(nginx / "cert" / app / ".gitignore", gitignore)
    writer.ensure_dir(nginx / "log" / app)


def update_template_contracts(repo: Path, registry: dict, writer: Writer) -> None:
    # All registered business Apps share the same transport contract; retain their own routes.
    for app in registry['apps']:
        template = release_root(repo) / app['nginxTemplate']
        if app['apiKind'] != 'business' or not template.is_file():
            continue
        text = template.read_text()
        text = text.replace('server namewta-server1:8080', 'server ${BACKEND_SERVER1}')
        text = text.replace('server namewta-server2:8080', 'server ${BACKEND_SERVER2}')
        if 'log_format app_access ' not in text:
            text = "log_format app_access '$remote_addr $request_method $uri $status';\n\n" + text
            text = text.replace('    index index.html;', '    index index.html;\n    access_log /var/log/nginx/access.log app_access;')
        if 'location = /healthz' not in text:
            text = text.replace('    location ~ ^(/[^/]*)?/actuator', '''    location = /healthz {
        access_log off;
        default_type text/plain;
        return 200 'ok\\n';
    }

    location ~ ^(/[^/]*)?/actuator''')
        writer.write(template, text)
    # OAuth callback queries contain one-time codes; access logs retain only the URI path.
    for template in lb_templates(repo):
        text = template.read_text()
        if 'log_format lb_access ' not in text:
            text = "log_format lb_access '$remote_addr $request_method $uri $status';\n\n" + text
            text = text.replace('    server_tokens off;', '    server_tokens off;\n    access_log /var/log/nginx/access.log lb_access;')
        writer.write(template, text)


def check_prefix_collision(repo: Path, prefix: str, current_key: str) -> None:
    for env_path in (release_root(repo) / ".env.example", release_root(repo) / ".env"):
        for key, value in parse_env(env_path).items():
            if key.endswith("_PREFIX") and key != current_key and value == prefix:
                raise ValueError(f"前缀 {prefix!r} 已被 {key} 使用")


def cmd_list(repo: Path) -> None:
    compose = compose_path(repo)
    example = parse_env(release_root(repo) / ".env.example")
    local = parse_env(release_root(repo) / ".env")
    print(f"Compose: {compose}")
    print("App 台账:")
    for app in configured_apps(repo):
        prefix_key = env_key(app, "PREFIX")
        port_key = env_key(app, "PORT")
        prefix = local.get(prefix_key, example.get(prefix_key, "<missing>"))
        port = local.get(port_key, example.get(port_key, "<missing>"))
        print(f"  {app:24} prefix={prefix!r:32} port={port}")
    print(f"下一个端口: {allocate_port(repo)}")


def main() -> None:
    parser = argparse.ArgumentParser(description="新增前端 App 到 NAMEWTA LB + 独立 Nginx 体系")
    parser.add_argument(
        "--repo-root",
        default=str(Path(__file__).resolve().parents[4]),
        help="WTA-plus 根目录",
    )
    parser.add_argument("--app", help="frontend/apps 下的 App 目录名")
    parser.add_argument("--prefix", help="URL 路径前缀，不含首尾斜杠")
    parser.add_argument("--sensitive", action="store_true", help="生成 10 位私有前缀")
    parser.add_argument("--port", type=int, help="独立 Nginx 宿主机端口")
    parser.add_argument("--list", action="store_true", help="显示当前台账")
    parser.add_argument("--dry-run", action="store_true", help="只预览，不写文件")
    args = parser.parse_args()

    repo = Path(args.repo_root).resolve()
    if args.list:
        cmd_list(repo)
        return
    if not args.app:
        parser.error("--app 必填（或使用 --list）")
    if not re.fullmatch(r"[a-z0-9][a-z0-9-]*", args.app):
        parser.error("--app 只允许小写字母、数字和连字符")
    package_json = repo / "frontend" / "apps" / args.app / "package.json"
    if not package_json.is_file():
        parser.error(f"App 尚不可构建或不存在 package.json: {package_json}")
    if args.sensitive and args.prefix:
        parser.error("--sensitive 与 --prefix 不能同时使用")

    prefix = generate_sensitive_prefix() if args.sensitive else args.prefix
    if not prefix:
        parser.error("必须提供 --prefix 或 --sensitive")
    if not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9-]*", prefix):
        parser.error("prefix 只允许字母、数字和连字符")
    if prefix in RESERVED_PREFIXES:
        parser.error(f"prefix {prefix!r} 是保留路由")

    prefix_key = env_key(args.app, "PREFIX")
    port_key = env_key(args.app, "PORT")
    check_prefix_collision(repo, prefix, prefix_key)
    registry_file = release_root(repo) / 'apps.json'
    registry = json.loads(registry_file.read_text())
    registration = next((row for row in registry['apps'] if row['id'] == args.app), None)
    port = args.port or (registration['defaultPort'] if registration else allocate_port(repo))
    if not 41080 <= port < 42000 or port in used_ports(repo, port_key):
        parser.error(f"端口 {port} 不可用；App 端口必须位于 41080-41999 且未占用")
    if registration is None:
        package = json.loads(package_json.read_text())
        registration = {'id': args.app, 'package': package['name'], 'shipped': True,
                        'prefixEnv': prefix_key, 'originEnv': env_key(args.app, 'ORIGIN'), 'portEnv': port_key,
                        'defaultPort': port, 'apiKind': 'business', 'callbackPath': '/sso/callback',
                        'composeService': 'namewta-nginx-' + args.app,
                        'nginxTemplate': f'docker/frontend/nginx/apps/nginx-{args.app}.conf.template',
                        'ingress': 'lb', 'healthPath': '/healthz'}
        registry['apps'].append(registration)
    registration['defaultPort'] = port
    for row in registry['apps']:
        row['apiPath'] = '/sso' if row['apiKind'] == 'sso' else '/{prefix}/{environment}-api'
    if registration['apiKind'] == 'sso' and prefix == 'sso':
        parser.error('SSO static prefix cannot collide with the /sso API namespace')
    if registration['apiKind'] == 'sso':
        for endpoint in [registration] + ([registration['tls']] if registration.get('tls') else []):
            if not (release_root(repo) / endpoint['nginxTemplate']).is_file():
                parser.error('SSO 专用模板缺失：先恢复已登记模板，不能用业务 App 模板代替')

    print(
        f"App={args.app} prefix=/{prefix}/ port={port} "
        f"env={prefix_key},{port_key}{' [sensitive]' if args.sensitive else ''}"
    )
    writer = Writer(args.dry_run)
    writer.write(registry_file, json.dumps(registry, ensure_ascii=False, indent=2) + '\n')
    update_template_contracts(repo, registry, writer)
    create_app_assets(repo, args.app, writer)
    patch_compose(repo, args.app, port, registration, writer)
    if registration['ingress'] == 'lb':
        for template in lb_templates(repo):
            patch_lb_template(template, args.app, writer)

    example_prefix = "replace-with-private-prefix" if args.sensitive else prefix
    upsert_env(release_root(repo) / ".env.example", prefix_key, example_prefix, writer)
    upsert_env(release_root(repo) / ".env.example", port_key, str(port), writer)
    for row in registry['apps']:
        example = parse_env(release_root(repo) / '.env.example')
        if row['originEnv'] not in example:
            upsert_env(release_root(repo) / '.env.example', row['originEnv'], f'https://{row["id"]}.example.invalid', writer)
        if row.get('tls'):
            upsert_env(release_root(repo) / '.env.example', row['tls']['portEnv'], str(row['tls']['defaultPort']), writer)
    # Runtime configuration is operator-owned; registering an App must never rewrite it.
    print(f"[NEXT] 在未入库的运行 env 中设置 {prefix_key}、{port_key} 和 {registration['originEnv']}")

    print(f"完成：{len(writer.changes)} 个文件需要变更")
    print("下一步：运行 release-artifacts/scripts/verify-release.sh 和 Compose config")


if __name__ == "__main__":
    main()
