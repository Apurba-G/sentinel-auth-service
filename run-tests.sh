#!/usr/bin/env bash
# ==============================================================================
# Linux/Mac Automated Test Execution Script
# ==============================================================================

set -e

GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo "=============================================================================="
echo "                 AUTOMATED TEST SUITE RUNNER"
echo "=============================================================================="
echo "Starting Maven Test Suite for ecommerce-auth-service..."
echo ""

if mvn clean test; then
    echo ""
    echo -e "${GREEN}==============================================================================${NC}"
    echo -e "${GREEN}[SUCCESS] ALL TESTS PASSED SUCCESSFULLY!${NC}"
    echo -e "${GREEN}==============================================================================${NC}"
else
    echo ""
    echo -e "${RED}==============================================================================${NC}"
    echo -e "${RED}[ERROR] TEST SUITE FAILED! Check logs above.${NC}"
    echo -e "${RED}==============================================================================${NC}"
    exit 1
fi
