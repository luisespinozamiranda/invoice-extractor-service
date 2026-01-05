#!/bin/bash

echo "========================================"
echo " Invoice Extractor - Stop Services"
echo "========================================"
echo ""
echo "Stopping Docker Compose services..."
echo ""

docker-compose down

echo ""
echo "========================================"
echo " Services stopped!"
echo "========================================"
echo ""
echo "Data has been preserved."
echo "To start again: ./start.sh"
echo ""
