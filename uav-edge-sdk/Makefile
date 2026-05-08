# UAV Edge SDK - Makefile
# ========================

.PHONY: help build test lint format clean install dev test-cov docs

# Default target
help:
	@echo "UAV Edge SDK - Available targets:"
	@echo ""
	@echo "  make build       - Build C++ module"
	@echo "  make test        - Run tests"
	@echo "  make test-cov    - Run tests with coverage"
	@echo "  make lint        - Run linting"
	@echo "  make format      - Format code"
	@echo "  make typecheck   - Run type checking"
	@echo "  make clean       - Clean build artifacts"
	@echo "  make install     - Install package"
	@echo "  make dev         - Install in development mode"
	@echo "  make docs        - Build documentation"
	@echo "  make all         - Run everything (lint, test)"
	@echo ""

# Build C++ module
build:
	@echo "Building C++ module..."
	@if [ "$(shell uname)" = "Darwin" ] || [ "$(shell uname -s)" = "Linux" ]; then \
		chmod +x build.sh && ./build.sh; \
	else \
		./build.bat; \
	fi

# Run tests
test:
	@echo "Running tests..."
	@cd tests && python test_edge_sdk.py

# Run tests with coverage
test-cov:
	@echo "Running tests with coverage..."
	pytest --cov=python --cov-report=html --cov-report=term

# Lint code
lint:
	@echo "Running flake8..."
	flake8 python/ --count --show-source --statistics

# Format code
format:
	@echo "Formatting code..."
	black python/
	isort python/

# Type checking
typecheck:
	@echo "Running mypy..."
	mypy python/ --ignore-missing-imports

# Clean build artifacts
clean:
	@echo "Cleaning..."
	@rm -rf build/
	@rm -rf dist/
	@rm -rf *.egg-info/
	@rm -rf .pytest_cache/
	@rm -rf htmlcov/
	@rm -rf .coverage
	@rm -rf .mypy_cache/
	@rm -rf python/*.pyd
	@rm -rf python/*.so
	@rm -rf python/*.dll
	find . -type d -name __pycache__ -exec rm -rf {} + 2>/dev/null || true
	find . -type f -name "*.pyc" -delete 2>/dev/null || true
	find . -type f -name "*.pyo" -delete 2>/dev/null || true

# Install package
install:
	@echo "Installing package..."
	pip install .

# Install in development mode
dev:
	@echo "Installing in development mode..."
	pip install -e .[dev]

# Build documentation
docs:
	@echo "Building documentation..."
	@mkdir -p docs
	@echo "Documentation generation not implemented yet"

# Run all checks
all: lint typecheck test

# CI target (used by GitHub Actions)
ci: lint typecheck test
