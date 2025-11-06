#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
ORIGINAL_DIR="$(pwd)"

cd "${PROJECT_ROOT}" || exit 1
trap 'cd "${ORIGINAL_DIR}"' EXIT

MODE="dev"
RUN_TESTS=1
RUN_FULL_SUITE=1
TEST_ONLY=0
SKIP_BUILD=0

if [[ -t 1 ]]; then
  COLOR_INFO=$'\033[96m'
  COLOR_OK=$'\033[92m'
  COLOR_WARN=$'\033[93m'
  COLOR_ERROR=$'\033[91m'
  COLOR_RESET=$'\033[0m'
else
  COLOR_INFO=""
  COLOR_OK=""
  COLOR_WARN=""
  COLOR_ERROR=""
  COLOR_RESET=""
fi

banner() {
  printf "%b==========================================%b\n" "${COLOR_INFO}" "${COLOR_RESET}"
  printf "%b   PoorCraft Unified Test & Run%b\n" "${COLOR_INFO}" "${COLOR_RESET}"
  printf "%b==========================================%b\n" "${COLOR_INFO}" "${COLOR_RESET}"
}

info() {
  printf "%b[INFO] %s%b\n" "${COLOR_INFO}" "$*" "${COLOR_RESET}"
}

success() {
  printf "%b[OK] %s%b\n" "${COLOR_OK}" "$*" "${COLOR_RESET}"
}

warn() {
  printf "%b[WARN] %s%b\n" "${COLOR_WARN}" "$*" "${COLOR_RESET}" >&2
}

error() {
  printf "%b[ERROR] %s%b\n" "${COLOR_ERROR}" "$*" "${COLOR_RESET}" >&2
}

usage() {
  cat <<'EOF'
Usage: unified-test-and-run.sh --mode <dev|prod> [options]

Options:
  --mode dev        Build JAR and launch via java -jar (default)
  --mode prod       Build production artifacts (Launch4j profile)
  --quick-tests     Run pre-flight suite only (uses -Pquick-tests)
  --skip-tests      Skip all automated tests
  --test-only       Run tests/build without launching game
  --skip-build      Run tests but reuse existing artifacts
  --help            Show this help message
EOF
}

parse_args() {
  while [[ $# -gt 0 ]]; do
    case "$1" in
      --mode)
        if [[ $# -lt 2 ]]; then
          error "--mode requires an argument (dev or prod)."
          usage
          exit 1
        fi
        MODE="$(printf '%s' "$2" | tr '[:upper:]' '[:lower:]')"
        if [[ "${MODE}" != "dev" && "${MODE}" != "prod" ]]; then
          error "Invalid mode '${MODE}'. Use dev or prod."
          exit 1
        fi
        shift 2
        ;;
      --quick-tests)
        RUN_FULL_SUITE=0
        shift
        ;;
      --skip-tests)
        RUN_TESTS=0
        RUN_FULL_SUITE=0
        shift
        ;;
      --test-only)
        TEST_ONLY=1
        shift
        ;;
      --skip-build)
        SKIP_BUILD=1
        shift
        ;;
      --help|-h)
        usage
        exit 0
        ;;
      *)
        warn "Unknown option: $1"
        shift
        ;;
    esac
  done
}

check_command() {
  local cmd="$1"
  local name="$2"
  if ! command -v "${cmd}" >/dev/null 2>&1; then
    error "${name} not found in PATH."
    exit 1
  fi
  success "${name} detected."
}

ensure_artifact_exists() {
  local path="$1"
  local description="$2"
  if [[ ! -f "${path}" ]]; then
    error "${description} not found at ${path}."
    exit 1
  fi
}

launch_dev() {
  info "Launching PoorCraft (dev mode)..."
  java -jar "target/PoorCraft.jar" &
  local pid=$!
  success "Launch command issued (java PID ${pid})."
}

launch_prod() {
  info "Launching PoorCraft (prod mode)..."
  if [[ -x "target/PoorCraft.exe" ]]; then
    "./target/PoorCraft.exe" &
    local pid=$!
    success "Launch command issued (exe PID ${pid})."
    return
  fi

  warn "target/PoorCraft.exe is not executable on this platform. Launching JAR fallback."
  java -jar "target/PoorCraft.jar" &
  local pid=$!
  success "Launch command issued (java PID ${pid})."
}

main() {
  parse_args "$@"

  banner
  info "Mode      : ${MODE}"
  if [[ ${RUN_TESTS} -eq 1 ]]; then
    if [[ ${RUN_FULL_SUITE} -eq 1 ]]; then
      info "Tests     : pre-flight + full suite"
    else
      info "Tests     : pre-flight only"
    fi
  else
    warn "Tests     : skipped (--skip-tests)"
  fi

  if [[ ${TEST_ONLY} -eq 1 ]]; then
    info "Launch    : disabled (--test-only)"
  else
    info "Launch    : enabled"
  fi

  info "Checking prerequisites..."
  check_command java "Java"
  check_command mvn "Maven"

  if [[ ${RUN_TESTS} -eq 1 ]]; then
    info "Running pre-flight tests (quick-tests profile)..."
    if mvn -B -Pquick-tests test; then
      success "Pre-flight suite passed."
    else
      error "Pre-flight tests failed. See target/test-reports for details."
      exit 1
    fi

    if [[ ${RUN_FULL_SUITE} -eq 1 ]]; then
      info "Running full test suite (clean verify)..."
      if mvn -B clean verify; then
        success "Full test suite passed."
      else
        error "Full test suite failed. See target/test-reports for details."
        exit 1
      fi
    else
      info "Skipping full suite (--quick-tests)."
    fi
  else
    warn "Tests skipped by user request."
  fi

  if [[ ${SKIP_BUILD} -eq 1 ]]; then
    warn "Build skipped (--skip-build)."
  else
    if [[ "${MODE}" == "dev" ]]; then
      info "Building development artifacts (dev build)..."
      if mvn -B package -Pdev-build -DskipTests; then
        ensure_artifact_exists "target/PoorCraft.jar" "Development JAR"
        success "Development build complete."
      else
        error "Development build failed."
        exit 1
      fi
    else
      info "Building production artifacts (prod build)..."
      if mvn -B package -Pprod-build -DskipTests; then
        ensure_artifact_exists "target/PoorCraft.exe" "Production EXE"
        success "Production build complete."
      else
        error "Production build failed."
        exit 1
      fi
    fi
  fi

  if [[ ${TEST_ONLY} -eq 1 ]]; then
    info "Test-only mode complete."
    success "All steps finished."
    return
  fi

  if [[ ${SKIP_BUILD} -eq 1 ]]; then
    if [[ "${MODE}" == "dev" ]]; then
      ensure_artifact_exists "target/PoorCraft.jar" "Development JAR"
    else
      ensure_artifact_exists "target/PoorCraft.exe" "Production EXE"
    fi
  fi

  if [[ "${MODE}" == "dev" ]]; then
    launch_dev
  else
    launch_prod
  fi

  success "All steps finished."
}

main "$@"
