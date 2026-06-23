#!/usr/bin/env bash
# docs/server-setup.sh
# Run ONCE manually on first server provision as root or a sudo-capable user.
# After this script completes, all subsequent deploys are handled by CD.
#
# Usage:
#   SSH_PATH=/opt/baundang REPO_URL=git@github.com:jeimandei/DigitalInvitationStore.git \
#     bash docs/server-setup.sh

set -euo pipefail

# ── Config ────────────────────────────────────────────────────────────────────

DEPLOY_USER="${DEPLOY_USER:-baundang}"
DEPLOY_PATH="${SSH_PATH:-/opt/baundang}"
REPO_URL="${REPO_URL:-git@github.com:jeimandei/DigitalInvitationStore.git}"
JAVA_VERSION="25"
MAVEN_VERSION="3.9.9"

# ── 1. System packages ────────────────────────────────────────────────────────

echo "==> Updating package index"
apt-get update -qq

echo "==> Installing prerequisites"
apt-get install -y -qq \
  curl wget gnupg ca-certificates \
  git \
  podman \
  python3-pip   # podman compose needs python3-podman-compose or the Go binary

# ── 2. Java 25 (Temurin) ─────────────────────────────────────────────────────

echo "==> Installing Eclipse Temurin $JAVA_VERSION"
CODENAME=$(. /etc/os-release && echo "$VERSION_CODENAME")
wget -qO /etc/apt/keyrings/adoptium.asc \
  https://packages.adoptium.net/artifactory/api/gpg/key/public
echo "deb [signed-by=/etc/apt/keyrings/adoptium.asc] \
https://packages.adoptium.net/artifactory/deb ${CODENAME} main" \
  > /etc/apt/sources.list.d/adoptium.list
apt-get update -qq
apt-get install -y -qq "temurin-${JAVA_VERSION}-jdk"
java -version

# ── 3. Maven ─────────────────────────────────────────────────────────────────

echo "==> Installing Maven $MAVEN_VERSION"
MVN_URL="https://dlcdn.apache.org/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz"
wget -qO /tmp/maven.tar.gz "$MVN_URL"
tar -xzf /tmp/maven.tar.gz -C /opt
ln -sf "/opt/apache-maven-${MAVEN_VERSION}/bin/mvn" /usr/local/bin/mvn
rm /tmp/maven.tar.gz
mvn -version

# ── 4. podman-compose ────────────────────────────────────────────────────────

echo "==> Installing podman-compose"
pip3 install -q podman-compose
# Alternatively, install the upstream Go binary:
# curl -fsSL https://github.com/containers/podman-compose/releases/latest/... | install -m755 /usr/local/bin/podman

# ── 5. Deploy user ────────────────────────────────────────────────────────────

echo "==> Creating deploy user: $DEPLOY_USER"
id "$DEPLOY_USER" &>/dev/null || useradd -m -s /bin/bash "$DEPLOY_USER"

# Allow podman (rootless) for the deploy user
loginctl enable-linger "$DEPLOY_USER" || true

# ── 6. SSH authorised key ────────────────────────────────────────────────────

echo "==> Setting up SSH authorized_keys for $DEPLOY_USER"
DEPLOY_HOME=$(eval echo "~$DEPLOY_USER")
mkdir -p "$DEPLOY_HOME/.ssh"
chmod 700 "$DEPLOY_HOME/.ssh"
touch "$DEPLOY_HOME/.ssh/authorized_keys"
chmod 600 "$DEPLOY_HOME/.ssh/authorized_keys"
chown -R "$DEPLOY_USER:$DEPLOY_USER" "$DEPLOY_HOME/.ssh"

echo ""
echo ">>> Paste the CD deploy public key into $DEPLOY_HOME/.ssh/authorized_keys"
echo ">>> (This is the public half of the SSH_KEY GitHub Actions secret)"
echo ""

# ── 7. Clone repository ───────────────────────────────────────────────────────

echo "==> Cloning repository to $DEPLOY_PATH"
mkdir -p "$DEPLOY_PATH"
chown "$DEPLOY_USER:$DEPLOY_USER" "$DEPLOY_PATH"

sudo -u "$DEPLOY_USER" git clone "$REPO_URL" "$DEPLOY_PATH"

# Install the Maven wrapper so ./mvnw works in CD
sudo -u "$DEPLOY_USER" bash -c "cd $DEPLOY_PATH && mvn -q wrapper:wrapper"

# ── 8. Environment file ───────────────────────────────────────────────────────

echo "==> Creating .env from .env.example"
cp "$DEPLOY_PATH/.env.example" "$DEPLOY_PATH/.env"
chown "$DEPLOY_USER:$DEPLOY_USER" "$DEPLOY_PATH/.env"
chmod 600 "$DEPLOY_PATH/.env"

echo ""
echo ">>> IMPORTANT: Edit $DEPLOY_PATH/.env and fill in all real values before starting."
echo ">>> Run:  nano $DEPLOY_PATH/.env"
echo ""

# ── 9. First build & start ────────────────────────────────────────────────────

read -r -p "Have you filled in .env? Start containers now? [y/N] " CONFIRM
if [[ "$CONFIRM" =~ ^[Yy]$ ]]; then
  echo "==> Building all modules"
  sudo -u "$DEPLOY_USER" bash -c "cd $DEPLOY_PATH && ./mvnw -q -DskipTests clean package"

  echo "==> Pulling images and starting services"
  sudo -u "$DEPLOY_USER" bash -c "
    cd $DEPLOY_PATH
    podman compose -f podman-compose.yml pull
    podman compose -f podman-compose.yml up -d
  "
  echo "==> Services started. Check status with:"
  echo "    sudo -u $DEPLOY_USER podman compose -f $DEPLOY_PATH/podman-compose.yml ps"
else
  echo "Skipped. When ready, run as $DEPLOY_USER:"
  echo "  cd $DEPLOY_PATH && ./mvnw -q -DskipTests clean package"
  echo "  podman compose -f podman-compose.yml up -d"
fi

echo ""
echo "==> Server setup complete."
echo "    Add the following GitHub Actions secrets to your repository:"
echo "      SSH_URL   = $(hostname -I | awk '{print $1}')"
echo "      SSH_USER  = $DEPLOY_USER"
echo "      SSH_KEY   = <private key paired with the public key in authorized_keys>"
echo "      SSH_PATH  = $DEPLOY_PATH"
