#!/bin/bash
source ~/.bashrc

echo "Building"

docker build -t coding-challenge:1.0.2 . -f Dockerfile
