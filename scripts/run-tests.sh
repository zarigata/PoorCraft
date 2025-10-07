#!/bin/bash

set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="${SCRIPT_DIR}/.."
REPORT_DIR="${PROJECT_ROOT}/target/test-reports"

print_banner() {
  echo "==============================================="
  echo "     PoorCraft Automated Test Suite Runner"
  echo "==============================================="
}

open_latest_report() {
  if [[ ! -d "${REPORT_DIR}" ]]; then
    echo "No reports found. Run tests first."
    return 1
  fi

  local latest
  latest=$(ls -t "${REPORT_DIR}"/test-report-*.html 2>/dev/null | head -n 1 || true)
  if [[ -z "${latest}" ]]; then
    echo "No HTML reports available."
    return 1
  fi

  echo "Opening ${latest}"
  if command -v xdg-open >/dev/null 2>&1; then
    xdg-open "${latest}" >/dev/null 2>&1 &
  elif command -v open >/dev/null 2>&1; then
    open "${latest}"
  else
    echo "Cannot detect a default browser opener. Please open manually: ${latest}"
  fi
}

print_report_summary() {
  if [[ ! -d "${REPORT_DIR}" ]]; then
    echo "Reports directory not found: ${REPORT_DIR}"
    return
  fi

  local latest
  latest=$(ls -t "${REPORT_DIR}"/test-report-*.html 2>/dev/null | head -n 1 || true)
  if [[ -z "${latest}" ]]; then
    echo "No HTML reports found in ${REPORT_DIR}"
    return
  fi

  local base
  base=$(basename "${latest}" ".html")
  echo "Latest HTML report: ${latest}"
  echo "Latest Markdown report: ${REPORT_DIR}/${base}.md"
}

main() {
  cd "${PROJECT_ROOT}"
  print_banner

  if ! command -v mvn >/dev/null 2>&1; then
    echo "[ERROR] Maven not found in PATH. Install Maven 3.6+ and ensure 'mvn' is available."
    exit 1
  fi

  local args=()
  local maven_args=()
  local exclude_tags=""
  local report_only="false"

  while [[ $# -gt 0 ]]; do
    case "$1" in
      --quick)
        if [[ -n "${exclude_tags}" ]]; then
          exclude_tags+=""
        fi
        exclude_tags="networking,rendering"
        shift
        ;;
      --report-only)
        report_only="true"
        shift
        ;;
      *)
        maven_args+=("$1")
        shift
        ;;
    esac
  done

  if [[ "${report_only}" == "true" ]]; then
    open_latest_report
    exit $?
  fi

  if [[ -n "${exclude_tags}" ]]; then
    maven_args+=("-Djunit.jupiter.tags.exclude=${exclude_tags}")
  fi

  echo "Running: mvn -B clean test ${maven_args[*]}"
  if mvn -B clean test "${maven_args[@]}"; then
    echo
    echo "[SUCCESS] Test suite completed successfully."
    print_report_summary
    exit 0
  else
    local exit_code=$?
    echo
    echo "[FAILURE] Test suite reported failures. See reports below."
    print_report_summary
    exit ${exit_code}
  fi
}

main "$@"
