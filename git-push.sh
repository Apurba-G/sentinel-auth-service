#!/usr/bin/env bash
# ==============================================================================
# Linux/Mac Automated Git Commit & Push Script
# ==============================================================================

set -e

GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

COMMIT_MSG="${1:-Auto-commit and push updates: $(date)}"

echo "=============================================================================="
echo "                 AUTOMATED GIT COMMIT & PUSH SCRIPT"
echo "=============================================================================="
echo ""

echo "Adding modified files to Git staging..."
git add .

echo ""
echo "Committing changes with message: \"$COMMIT_MSG\""
git commit -m "$COMMIT_MSG" || echo "No changes to commit."

echo ""
echo "Pushing changes to remote repository..."
if git push; then
    echo ""
    echo -e "${GREEN}==============================================================================${NC}"
    echo -e "${GREEN}[SUCCESS] CODE SUCCESSFULLY COMMITTED AND PUSHED TO REMOTE REPOSITORY!${NC}"
    echo -e "${GREEN}==============================================================================${NC}"
else
    echo ""
    echo -e "${RED}==============================================================================${NC}"
    echo -e "${RED}[ERROR] GIT PUSH FAILED! Check credentials or remote connection.${NC}"
    echo -e "${RED}==============================================================================${NC}"
    exit 1
fi
